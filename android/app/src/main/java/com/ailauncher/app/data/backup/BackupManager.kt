package com.ailauncher.app.data.backup

import android.accounts.Account
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import com.ailauncher.app.R
import com.ailauncher.app.data.SettingsRepository
import com.ailauncher.app.domain.models.BackupDestination
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackupManager(private val context: Context, private val settingsRepo: SettingsRepository) {
    sealed class BackupResult {
        data class Success(val path: String) : BackupResult()
        data class Error(val message: String) : BackupResult()
    }

    /**
     * v8: Opaque reference to a local backup. Pre-Q backups are File-backed,
     * Q+ backups are MediaStore Uri-backed. UI only needs [displayName].
     */
    data class BackupFile(
        val displayName: String,
        val timestamp: Long,
        internal val file: File? = null,
        internal val uri: Uri? = null
    )

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)

    /** [password] blank = plain-JSON export (default); non-blank = AES-256-GCM
     *  envelope only that password can open (see PortableBackupCrypto). */
    suspend fun backup(destination: BackupDestination, password: String = ""): BackupResult = withContext(Dispatchers.IO) {
        try {
            val jsonData = if (password.isNotBlank()) settingsRepo.exportAllSettingsEncrypted(password) else settingsRepo.exportAllSettings()
            val fileName = "novelauncher_backup_${dateFormat.format(Date())}.json"
            when (destination) {
                BackupDestination.LOCAL -> backupToLocal(fileName, jsonData)
                BackupDestination.GOOGLE_DRIVE -> backupToGoogleDrive(fileName, jsonData)
                // OneDrive: graph.microsoft.com path-upload requires a Microsoft OAuth flow
                // we haven't wired up yet. Returns a clear error rather than silently
                // failing on whatever happens to be in the legacy oauth_token slot.
                BackupDestination.ONEDRIVE -> BackupResult.Error(
                    context.getString(R.string.backup_error_onedrive)
                )
                // Box: upload.box.com/api/2.0/files/content requires a multipart upload
                // with a parent_id and a Box OAuth flow. Disabled until properly implemented.
                BackupDestination.BOX -> BackupResult.Error(
                    context.getString(R.string.backup_error_box)
                )
                BackupDestination.NAS -> backupToNas(fileName, jsonData, settingsRepo.backupFlow.first())
            }
        } catch (e: Exception) { BackupResult.Error(e.message ?: "Unknown error") }
    }

    /** [password] is only used if the file turns out to be a [PortableBackupCrypto]
     *  envelope — a plain-JSON backup restores fine with an empty password. */
    suspend fun restoreFromLocal(backup: BackupFile, password: String = ""): BackupResult = withContext(Dispatchers.IO) {
        try {
            val text = when {
                backup.uri != null -> context.contentResolver.openInputStream(backup.uri)
                    ?.bufferedReader()?.use { it.readText() }
                    ?: return@withContext BackupResult.Error("Could not open backup")
                backup.file != null -> {
                    if (!backup.file.exists()) return@withContext BackupResult.Error("File not found")
                    backup.file.readText()
                }
                else -> return@withContext BackupResult.Error("Invalid backup reference")
            }
            val success = settingsRepo.importAllSettingsEncrypted(text, password)
            if (success) BackupResult.Success(backup.displayName) else BackupResult.Error("Invalid format")
        } catch (e: Exception) { BackupResult.Error(e.message ?: "Restore failed") }
    }

    suspend fun restoreFromJson(json: String, password: String = ""): BackupResult = withContext(Dispatchers.IO) {
        if (settingsRepo.importAllSettingsEncrypted(json, password)) BackupResult.Success("OK") else BackupResult.Error("Invalid format")
    }

    private fun backupToLocal(fileName: String, data: String): BackupResult {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            backupToMediaStore(fileName, data)
        } else {
            backupToLegacyDownloads(fileName, data)
        }
    }

    @Suppress("DEPRECATION")
    private fun backupToLegacyDownloads(fileName: String, data: String): BackupResult {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NoveLauncher")
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, fileName); file.writeText(data)
        return BackupResult.Success(file.absolutePath)
    }

    /**
     * v8 FIX: Write via MediaStore.Downloads on API 29+ so the backup respects
     * scoped storage. The legacy Environment.getExternalStoragePublicDirectory
     * path silently fails on most Android 11+ devices.
     *
     * v9: @RequiresApi documents the API-29 contract that [backupToLocal] already
     * enforces with its SDK_INT >= Q guard. Lint's interprocedural analysis
     * doesn't follow that guard across the method boundary, so without this
     * annotation it flags MediaStore.Downloads.EXTERNAL_CONTENT_URI as a NewApi
     * error and (with abortOnError) fails the build.
     */
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun backupToMediaStore(fileName: String, data: String): BackupResult {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/NoveLauncher")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: return BackupResult.Error("Could not create file in Downloads")
        return try {
            resolver.openOutputStream(uri)?.use { it.write(data.toByteArray()) }
                ?: return BackupResult.Error("Could not open output stream")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            BackupResult.Success("Downloads/NoveLauncher/$fileName")
        } catch (e: Exception) {
            try { resolver.delete(uri, null, null) } catch (_: Exception) {}
            BackupResult.Error("Local backup: ${e.message}")
        }
    }

    /**
     * v8 FIX: Drive backup now obtains a real OAuth2 *access token* via GoogleAuthUtil,
     * keyed on the signed-in account email. The previous flow stored an ID token (or
     * a serverAuthCode) as `oauth_token`, which Drive's REST API rejects with 401.
     *
     * Requires:
     * - The user signed in via GoogleSignIn with the `drive.file` scope (done in
     *   LauncherActivity.initiateGoogleSignIn()).
     * - The signed-in account email persisted under cloud_auth/account_email.
     *
     * Token is fetched fresh each call (Google tokens are short-lived).
     */
    private fun backupToGoogleDrive(fileName: String, data: String): BackupResult {
        val token = fetchDriveAccessToken()
            ?: return BackupResult.Error(context.getString(R.string.backup_error_not_connected))

        val url = URL("https://www.googleapis.com/upload/drive/v3/files?uploadType=media")
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $token")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("X-Upload-Content-Type", "application/json")
            conn.doOutput = true
            OutputStreamWriter(conn.outputStream).use { it.write(data) }

            when {
                conn.responseCode in 200..299 -> BackupResult.Success("Google Drive: $fileName")
                conn.responseCode == 401 -> {
                    // Invalidate cached token so the next attempt re-fetches.
                    try { GoogleAuthUtil.clearToken(context, token) } catch (_: Exception) {}
                    BackupResult.Error(context.getString(R.string.backup_error_token_expired))
                }
                else -> BackupResult.Error("Google Drive error: ${conn.responseCode} ${conn.responseMessage}")
            }
        } catch (e: Exception) {
            BackupResult.Error("Google Drive: ${e.message}")
        } finally { conn.disconnect() }
    }

    private fun fetchDriveAccessToken(): String? {
        val email = context.getSharedPreferences("cloud_auth", Context.MODE_PRIVATE)
            .getString("account_email", null) ?: return null
        return try {
            GoogleAuthUtil.getToken(
                context,
                Account(email, "com.google"),
                "oauth2:https://www.googleapis.com/auth/drive.file"
            )
        } catch (_: UserRecoverableAuthException) {
            // User must re-authorize. UI flow should call LauncherActivity.initiateGoogleSignIn() again.
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * v9: NAS now reads its settings (address/path/credentials) from the same
     * BackupSettings instance the UI writes to. Previously the UI saved to
     * DataStore while this method read from a separate SharedPreferences file
     * called "nas_config" that nothing populated — so NAS backups silently
     * always failed with "NAS not configured".
     *
     * Also: HTTPS-only. The backup payload contains every PBKDF2 hash and
     * every locked-app package name; sending that to a plaintext NAS over LAN
     * is the same as publishing it. Self-signed certs on a private network
     * still work via the user's trust store (Network Security Config).
     */
    private fun backupToNas(fileName: String, data: String, backup: com.ailauncher.app.domain.models.BackupSettings): BackupResult {
        val addr = backup.nasAddress.trim()
        if (addr.isEmpty()) return BackupResult.Error(context.getString(R.string.backup_error_nas_not_configured))
        if (!addr.startsWith("https://", ignoreCase = true)) {
            return BackupResult.Error(context.getString(R.string.backup_error_nas_http_refused))
        }
        val path = backup.nasPath.ifBlank { "/backup/novelauncher" }
        val url = URL("$addr$path/$fileName")
        val conn = url.openConnection() as HttpURLConnection
        return try {
            conn.requestMethod = "PUT"
            conn.setRequestProperty("Content-Type", "application/json")
            // Basic Auth header — only attached if the user provided credentials.
            if (backup.nasUsername.isNotEmpty()) {
                val creds = "${backup.nasUsername}:${backup.nasPassword}"
                val encoded = android.util.Base64.encodeToString(
                    creds.toByteArray(Charsets.UTF_8),
                    android.util.Base64.NO_WRAP
                )
                conn.setRequestProperty("Authorization", "Basic $encoded")
            }
            conn.doOutput = true; conn.connectTimeout = 10000; conn.readTimeout = 10000
            OutputStreamWriter(conn.outputStream).use { it.write(data) }
            if (conn.responseCode in 200..299) BackupResult.Success("NAS: $addr")
            else BackupResult.Error(context.getString(R.string.backup_error_nas_http_code, conn.responseCode))
        } catch (e: Exception) {
            BackupResult.Error(context.getString(R.string.backup_error_nas_io, e.message ?: ""))
        } finally { conn.disconnect() }
    }

    fun listLocalBackups(): List<BackupFile> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            listMediaStoreBackups()
        } else {
            listLegacyBackups()
        }
    }

    @Suppress("DEPRECATION")
    private fun listLegacyBackups(): List<BackupFile> {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "NoveLauncher")
        return dir.listFiles { f -> f.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { BackupFile(it.name, it.lastModified(), file = it) }
            ?: emptyList()
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun listMediaStoreBackups(): List<BackupFile> {
        val results = mutableListOf<BackupFile>()
        val projection = arrayOf(
            MediaStore.Downloads._ID,
            MediaStore.Downloads.DISPLAY_NAME,
            MediaStore.Downloads.DATE_MODIFIED
        )
        val selection = "${MediaStore.Downloads.RELATIVE_PATH} LIKE ? AND ${MediaStore.Downloads.MIME_TYPE} = ?"
        val args = arrayOf("${Environment.DIRECTORY_DOWNLOADS}/NoveLauncher/%", "application/json")
        try {
            context.contentResolver.query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                projection, selection, args,
                "${MediaStore.Downloads.DATE_MODIFIED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DISPLAY_NAME)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Downloads.DATE_MODIFIED)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val name = cursor.getString(nameCol)
                    val date = cursor.getLong(dateCol) * 1000L
                    val uri = MediaStore.Downloads.EXTERNAL_CONTENT_URI.buildUpon()
                        .appendPath(id.toString()).build()
                    results.add(BackupFile(name, date, uri = uri))
                }
            }
        } catch (_: Exception) {}
        return results
    }

    fun isGoogleConnected(): Boolean = getConnectedEmail() != null

    fun getConnectedEmail(): String? {
        return context.getSharedPreferences("cloud_auth", Context.MODE_PRIVATE).getString("account_email", null)
    }
}
