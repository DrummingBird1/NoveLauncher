package com.ailauncher.app

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import java.util.Locale
import java.util.WeakHashMap

/**
 * v10: applies the in-app language choice (AppearanceSettings.appLanguage,
 * "" = follow the system).
 *
 * On Android 13+ this goes straight to the framework [LocaleManager].
 * AppCompatDelegate.setApplicationLocales only reaches LocaleManager through an
 * active AppCompatActivity, and neither of ours is one, so the language picker
 * silently did nothing there. The framework persists the choice and recreates
 * the app's activities itself.
 *
 * Below 13 there is no framework support, and the AppCompat call this used to
 * make was a no-op too: it only reaches AppCompatActivity delegates. So the app
 * does it itself (see CLAUDE.md Pitfall #12): the tag is kept in a small prefs
 * file that can be read synchronously, each Activity applies it in
 * attachBaseContext ([attach]), and an Activity created before the latest pick
 * recreates itself on resume ([recreateIfStale]).
 */
object AppLocales {
    private const val PREFS = "app_locale"
    private const val KEY_TAG = "tag"

    private val modern = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Below 13: the tag each live Activity was created with. Main thread only. */
    private val attached = WeakHashMap<Activity, String>()

    fun apply(
        context: Context,
        tag: String,
    ) {
        if (modern) {
            val manager = context.getSystemService(LocaleManager::class.java) ?: return
            val wanted = if (tag.isBlank()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
            // Unchanged → don't touch it, so a cold start never causes a configuration change.
            if (manager.applicationLocales != wanted) manager.applicationLocales = wanted
        } else {
            prefs(context).edit().putString(KEY_TAG, tag).apply()
            setDefault(tag)
        }
    }

    /**
     * Below 13: [base] with the chosen language applied, also usable for a Service
     * or Worker that shows text. 13+: [base] unchanged — the framework already did it.
     */
    fun wrap(base: Context): Context {
        if (modern) return base
        val tag = storedTag(base)
        if (tag.isBlank()) return base
        setDefault(tag)
        // Only the locale is set; every other field stays "undefined", so the
        // Activity's own configuration (orientation, night mode…) still applies.
        val override = Configuration().apply { setLocales(LocaleList.forLanguageTags(tag)) }
        return base.createConfigurationContext(override)
    }

    /** For an Activity's attachBaseContext: [wrap], remembering which tag it was created with. */
    fun attach(
        activity: Activity,
        base: Context,
    ): Context {
        if (modern) return base
        attached[activity] = storedTag(base)
        return wrap(base)
    }

    /**
     * Below 13, call from onResume: recreates [activity] if the language was changed
     * after it was created (picked in Settings while Home stayed alive, or synced at
     * startup after Home was already up). Returns true when it did.
     */
    fun recreateIfStale(activity: Activity): Boolean {
        if (modern) return false
        val createdWith = attached[activity] ?: return false
        if (createdWith == storedTag(activity)) return false
        activity.recreate()
        return true
    }

    /** The language the UI is shown in, for formatting outside an Activity's resources. */
    fun locale(context: Context): Locale {
        if (!modern) {
            val tag = storedTag(context)
            if (tag.isNotBlank()) return Locale.forLanguageTag(tag)
        }
        return context.resources.configuration.locales[0] ?: Locale.getDefault()
    }

    private fun storedTag(context: Context): String = prefs(context).getString(KEY_TAG, null).orEmpty()

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /** Dates and numbers formatted with the default locale follow the choice too, as on 13+. */
    private fun setDefault(tag: String) {
        LocaleList.setDefault(
            if (tag.isBlank()) Resources.getSystem().configuration.locales else LocaleList.forLanguageTags(tag),
        )
    }
}
