package com.ailauncher.app.data.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.Instant
import java.util.UUID

/**
 * v10: the Google Drive REST v3 calls behind Drive backup and restore — plain
 * HttpURLConnection, like the rest of the app's networking.
 *
 * Token-agnostic on purpose: [BackupManager] gets the access token from
 * [DriveAuth] and handles a 401; this class only speaks HTTP, so DriveClientTest
 * can point [baseUrl] at a MockWebServer.
 *
 * Everything stays inside the `drive.file` scope: the folder and the backup
 * files are created by the app, so files.list sees them and nothing else.
 */
class DriveClient {
    // Test-only override point, same pattern as WeatherService.baseUrl. Both the
    // metadata API (/drive/v3) and the upload API (/upload/drive/v3) live here.
    internal var baseUrl: String = "https://www.googleapis.com"

    data class DriveFile(
        val id: String,
        val name: String,
        val modifiedTime: Long,
    )

    /** A non-2xx response. [code] 401 means Google rejected the access token. */
    class HttpException(
        val code: Int,
        message: String,
    ) : IOException("$code $message")

    private val json = Json { ignoreUnknownKeys = true }

    /** Id of the app's "NoveLauncher" folder in My Drive, created on first use. */
    fun findOrCreateFolder(token: String): String {
        val q = "name = '$FOLDER_NAME' and mimeType = '$FOLDER_MIME' and trashed = false"
        val found = request(token, "GET", "/drive/v3/files?q=${enc(q)}&spaces=drive&pageSize=1&fields=${enc("files(id)")}")
        parseObject(found)["files"]
            ?.jsonArray
            ?.firstOrNull()
            ?.jsonObject
            ?.get("id")
            ?.jsonPrimitive
            ?.content
            ?.let { return it }

        val metadata =
            buildJsonObject {
                put("name", FOLDER_NAME)
                put("mimeType", FOLDER_MIME)
            }.toString()
        val created =
            request(
                token,
                "POST",
                "/drive/v3/files?fields=id",
                "application/json; charset=UTF-8",
                metadata.toByteArray(Charsets.UTF_8),
            )
        return parseObject(created).getValue("id").jsonPrimitive.content
    }

    /**
     * Multipart upload: file metadata (name, MIME type, parent folder) and content
     * in one request. The plain `uploadType=media` upload this replaces sends no
     * metadata, so Drive named every backup "Untitled".
     */
    fun upload(
        token: String,
        fileName: String,
        content: String,
        folderId: String?,
    ): DriveFile {
        val boundary = "novelauncher-${UUID.randomUUID()}"
        val response =
            request(
                token,
                "POST",
                "/upload/drive/v3/files?uploadType=multipart&fields=${enc(FILE_FIELDS)}",
                "multipart/related; boundary=$boundary",
                multipartBody(boundary, fileName, content, folderId),
            )
        return parseFile(parseObject(response))
    }

    /** The app's backup files, newest first — wherever in Drive the user moved them. */
    fun listBackups(token: String): List<DriveFile> {
        val q = "name contains '$BACKUP_NAME_PREFIX' and mimeType = '$BACKUP_MIME' and trashed = false"
        val response =
            request(
                token,
                "GET",
                "/drive/v3/files?q=${enc(q)}&spaces=drive&orderBy=${enc("modifiedTime desc")}" +
                    "&pageSize=$LIST_PAGE_SIZE&fields=${enc("files($FILE_FIELDS)")}",
            )
        return parseObject(response)["files"]?.jsonArray?.map { parseFile(it.jsonObject) } ?: emptyList()
    }

    /** Raw file content: plain JSON or a PortableBackupCrypto envelope. */
    fun download(
        token: String,
        fileId: String,
    ): String = request(token, "GET", "/drive/v3/files/${enc(fileId)}?alt=media")

    private fun multipartBody(
        boundary: String,
        fileName: String,
        content: String,
        folderId: String?,
    ): ByteArray {
        val metadata =
            buildJsonObject {
                put("name", fileName)
                put("mimeType", BACKUP_MIME)
                if (folderId != null) putJsonArray("parents") { add(folderId) }
            }
        return buildString {
            append("--").append(boundary).append(CRLF)
            append("Content-Type: application/json; charset=UTF-8").append(CRLF).append(CRLF)
            append(metadata.toString()).append(CRLF)
            append("--").append(boundary).append(CRLF)
            append("Content-Type: ").append(BACKUP_MIME).append(CRLF).append(CRLF)
            append(content).append(CRLF)
            append("--").append(boundary).append("--").append(CRLF)
        }.toByteArray(Charsets.UTF_8)
    }

    private fun request(
        token: String,
        method: String,
        path: String,
        contentType: String? = null,
        body: ByteArray? = null,
    ): String {
        val conn = URL(baseUrl + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = TIMEOUT_MS
            conn.readTimeout = TIMEOUT_MS
            conn.setRequestProperty("Authorization", "Bearer $token")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", contentType)
                conn.setFixedLengthStreamingMode(body.size)
                conn.outputStream.use { it.write(body) }
            }
            val code = conn.responseCode
            if (code !in 200..299) throw HttpException(code, conn.responseMessage.orEmpty())
            return conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseObject(body: String): JsonObject = json.parseToJsonElement(body).jsonObject

    private fun parseFile(o: JsonObject) =
        DriveFile(
            id = o.getValue("id").jsonPrimitive.content,
            name = o["name"]?.jsonPrimitive?.content.orEmpty(),
            modifiedTime =
                o["modifiedTime"]
                    ?.jsonPrimitive
                    ?.content
                    ?.let { runCatching { Instant.parse(it).toEpochMilli() }.getOrNull() }
                    ?: 0L,
        )

    // URLEncoder does form encoding (space → '+'); Google reads %20 unambiguously.
    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")

    companion object {
        const val FOLDER_NAME = "NoveLauncher"
        const val BACKUP_MIME = "application/json"

        // Drive's `name contains` matches name prefixes; BackupManager names
        // every backup "novelauncher_backup_<date>.json".
        private const val BACKUP_NAME_PREFIX = "novelauncher_backup"
        private const val FOLDER_MIME = "application/vnd.google-apps.folder"
        private const val FILE_FIELDS = "id,name,modifiedTime"
        private const val LIST_PAGE_SIZE = 50
        private const val TIMEOUT_MS = 30_000
        private const val CRLF = "\r\n"
    }
}
