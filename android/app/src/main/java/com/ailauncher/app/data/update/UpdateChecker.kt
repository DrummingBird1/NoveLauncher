package com.ailauncher.app.data.update

import android.content.Context
import com.ailauncher.app.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * v10: "is there a newer release?" for people who installed the APK from GitHub
 * or another store without auto-updates.
 *
 * Opt-in (Settings → About), and even then at most one request a day, to
 * GitHub's public releases API. Nothing about the user or device is sent — only
 * what any HTTPS request carries (IP address, a User-Agent naming the app
 * version). The result just links to the release page; nothing is downloaded
 * or installed by the app itself.
 */
@Singleton
class UpdateChecker @Inject constructor(@ApplicationContext private val context: Context) {

    data class UpdateInfo(val version: String, val url: String)

    // internal + var: test-only override, same pattern as WeatherService.baseUrl.
    internal var apiUrl = "https://api.github.com/repos/DrummingBird1/NoveLauncher/releases/latest"

    private val prefs by lazy { context.getSharedPreferences("update_check", Context.MODE_PRIVATE) }
    private val json = Json { ignoreUnknownKeys = true }

    /** Cached result of the last check, if it found something newer than this build. */
    fun cached(): UpdateInfo? {
        val version = prefs.getString(KEY_VERSION, null) ?: return null
        val url = prefs.getString(KEY_URL, null) ?: return null
        return UpdateInfo(version, url).takeIf { isNewer(version, BuildConfig.VERSION_NAME) }
    }

    /**
     * Returns the newer release, or null if up to date / offline. Without [force],
     * a check is skipped (and the cached answer returned) within 24 h of the last one.
     */
    suspend fun check(force: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong(KEY_LAST_CHECK, 0L) < DAY_MS) return@withContext cached()
        try {
            val conn = URL(apiUrl).openConnection() as HttpURLConnection
            try {
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                conn.setRequestProperty("Accept", "application/vnd.github+json")
                conn.setRequestProperty("User-Agent", "NoveLauncher/${BuildConfig.VERSION_NAME}")
                if (conn.responseCode !in 200..299) return@withContext cached()
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val root = json.parseToJsonElement(body).jsonObject
                val tag = root["tag_name"]?.jsonPrimitive?.content?.removePrefix("v") ?: return@withContext cached()
                val url = root["html_url"]?.jsonPrimitive?.content ?: RELEASES_PAGE
                prefs.edit().putLong(KEY_LAST_CHECK, now).putString(KEY_VERSION, tag).putString(KEY_URL, url).apply()
                UpdateInfo(tag, url).takeIf { isNewer(tag, BuildConfig.VERSION_NAME) }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Timber.w(e, "Update check failed")
            cached()
        }
    }

    fun dismiss(version: String) = prefs.edit().putString(KEY_DISMISSED, version).apply()
    fun isDismissed(version: String) = prefs.getString(KEY_DISMISSED, null) == version

    companion object {
        const val RELEASES_PAGE = "https://github.com/DrummingBird1/NoveLauncher/releases/latest"
        private const val DAY_MS = 24 * 60 * 60 * 1000L
        private const val KEY_LAST_CHECK = "last_check"
        private const val KEY_VERSION = "latest_version"
        private const val KEY_URL = "latest_url"
        private const val KEY_DISMISSED = "dismissed_version"

        /** Numeric dot-separated comparison ("10.0.1" > "9.3.0"); any suffix after '-' is ignored. */
        fun isNewer(candidate: String, current: String): Boolean {
            fun parts(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
            val a = parts(candidate)
            val b = parts(current)
            for (i in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(i) { 0 }
                val y = b.getOrElse(i) { 0 }
                if (x != y) return x > y
            }
            return false
        }
    }
}
