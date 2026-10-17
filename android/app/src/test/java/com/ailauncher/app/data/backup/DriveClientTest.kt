package com.ailauncher.app.data.backup

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Instant

/**
 * Contract test for the Drive v3 requests behind Drive backup/restore — the
 * multipart upload body (the media-only upload it replaced left every backup
 * named "Untitled") and the files.list / alt=media parsing. Pure JVM:
 * DriveClient never touches Context or DriveAuth.
 */
class DriveClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: DriveClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = DriveClient().apply { baseUrl = server.url("/").toString().trimEnd('/') }
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `upload sends name, type and folder as multipart metadata`() {
        server.enqueue(
            MockResponse().setBody(
                """{"id": "f1", "name": "novelauncher_backup_2026-10-17_09-00-00.json", "modifiedTime": "2026-10-17T09:00:01.250Z"}"""
            )
        )
        val content = """{"appearance":{"themeName":"עברית"}}"""

        val file = client.upload("tok", "novelauncher_backup_2026-10-17_09-00-00.json", content, folderId = "folder9")

        val req = server.takeRequest()
        assertEquals("POST", req.method)
        assertEquals("/upload/drive/v3/files", req.requestUrl?.encodedPath)
        assertEquals("multipart", req.requestUrl?.queryParameter("uploadType"))
        assertEquals("Bearer tok", req.getHeader("Authorization"))

        val contentType = req.getHeader("Content-Type").orEmpty()
        assertTrue(contentType, contentType.startsWith("multipart/related; boundary="))
        val boundary = contentType.substringAfter("boundary=")

        val parts = multipartParts(req.body.readUtf8(), boundary)
        assertEquals(2, parts.size)

        val (metaHeaders, metaBody) = parts[0]
        assertTrue(metaHeaders, metaHeaders.contains("Content-Type: application/json"))
        val meta = Json.parseToJsonElement(metaBody).jsonObject
        assertEquals("novelauncher_backup_2026-10-17_09-00-00.json", meta["name"]?.jsonPrimitive?.content)
        assertEquals("application/json", meta["mimeType"]?.jsonPrimitive?.content)
        assertEquals(listOf("folder9"), meta["parents"]?.jsonArray?.map { it.jsonPrimitive.content })

        val (mediaHeaders, mediaBody) = parts[1]
        assertTrue(mediaHeaders, mediaHeaders.contains("Content-Type: application/json"))
        assertEquals(content, mediaBody)

        assertEquals("f1", file.id)
        assertEquals("novelauncher_backup_2026-10-17_09-00-00.json", file.name)
        assertEquals(Instant.parse("2026-10-17T09:00:01.250Z").toEpochMilli(), file.modifiedTime)
    }

    @Test
    fun `upload without a folder sends no parents`() {
        server.enqueue(MockResponse().setBody("""{"id": "f2", "name": "b.json"}"""))

        client.upload("tok", "b.json", "NVLBK1:abc", folderId = null)

        val req = server.takeRequest()
        val boundary = req.getHeader("Content-Type").orEmpty().substringAfter("boundary=")
        val parts = multipartParts(req.body.readUtf8(), boundary)
        val meta = Json.parseToJsonElement(parts[0].second).jsonObject
        assertTrue(meta.toString(), "parents" !in meta)
        assertEquals("NVLBK1:abc", parts[1].second)
    }

    @Test
    fun `existing app folder is reused`() {
        server.enqueue(MockResponse().setBody("""{"files": [{"id": "folder9"}]}"""))

        assertEquals("folder9", client.findOrCreateFolder("tok"))

        val req = server.takeRequest()
        assertEquals("GET", req.method)
        val q = req.requestUrl?.queryParameter("q").orEmpty()
        assertTrue(q, q.contains("name = 'NoveLauncher'"))
        assertTrue(q, q.contains("mimeType = 'application/vnd.google-apps.folder'"))
        assertTrue(q, q.contains("trashed = false"))
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `missing app folder is created`() {
        server.enqueue(MockResponse().setBody("""{"files": []}"""))
        server.enqueue(MockResponse().setBody("""{"id": "newFolder"}"""))

        assertEquals("newFolder", client.findOrCreateFolder("tok"))

        server.takeRequest() // the lookup
        val create = server.takeRequest()
        assertEquals("POST", create.method)
        assertEquals("/drive/v3/files", create.requestUrl?.encodedPath)
        val meta = Json.parseToJsonElement(create.body.readUtf8()).jsonObject
        assertEquals("NoveLauncher", meta["name"]?.jsonPrimitive?.content)
        assertEquals("application/vnd.google-apps.folder", meta["mimeType"]?.jsonPrimitive?.content)
    }

    @Test
    fun `listBackups asks for the app's backups newest first and parses them`() {
        server.enqueue(
            MockResponse().setBody(
                """
                {
                  "kind": "drive#fileList",
                  "incompleteSearch": false,
                  "files": [
                    {"id": "b2", "name": "novelauncher_backup_2026-10-17_09-00-00.json", "modifiedTime": "2026-10-17T09:00:01.000Z"},
                    {"id": "b1", "name": "novelauncher_backup_2026-10-01_21-30-00.json", "modifiedTime": "2026-10-01T21:30:02.000Z"}
                  ]
                }
                """.trimIndent()
            )
        )

        val files = client.listBackups("tok")

        val req = server.takeRequest()
        assertEquals("/drive/v3/files", req.requestUrl?.encodedPath)
        assertEquals("modifiedTime desc", req.requestUrl?.queryParameter("orderBy"))
        val q = req.requestUrl?.queryParameter("q").orEmpty()
        assertTrue(q, q.contains("name contains 'novelauncher_backup'"))
        assertTrue(q, q.contains("mimeType = 'application/json'"))
        assertTrue(q, q.contains("trashed = false"))
        assertTrue(req.requestUrl?.queryParameter("fields").orEmpty().contains("modifiedTime"))

        assertEquals(listOf("b2", "b1"), files.map { it.id })
        assertEquals("novelauncher_backup_2026-10-17_09-00-00.json", files[0].name)
        assertEquals(Instant.parse("2026-10-01T21:30:02Z").toEpochMilli(), files[1].modifiedTime)
    }

    @Test
    fun `listBackups with no files key returns an empty list`() {
        server.enqueue(MockResponse().setBody("""{"kind": "drive#fileList"}"""))

        assertTrue(client.listBackups("tok").isEmpty())
    }

    @Test
    fun `download fetches the raw content with alt=media`() {
        server.enqueue(MockResponse().setBody("NVLBK1:c2FsdGl2Y2lwaGVy"))

        val content = client.download("tok", "b2")

        val req = server.takeRequest()
        assertEquals("GET", req.method)
        assertEquals("/drive/v3/files/b2", req.requestUrl?.encodedPath)
        assertEquals("media", req.requestUrl?.queryParameter("alt"))
        assertEquals("Bearer tok", req.getHeader("Authorization"))
        assertEquals("NVLBK1:c2FsdGl2Y2lwaGVy", content)
    }

    @Test
    fun `a rejected token surfaces as HttpException 401`() {
        server.enqueue(MockResponse().setResponseCode(401).setBody("""{"error": {"code": 401}}"""))

        try {
            client.listBackups("stale")
            fail("expected HttpException")
        } catch (e: DriveClient.HttpException) {
            assertEquals(401, e.code)
        }
    }

    /** Splits a multipart body into (headers, body) pairs, per RFC 2046 with CRLF line breaks. */
    private fun multipartParts(body: String, boundary: String): List<Pair<String, String>> {
        assertTrue("body must end with the close delimiter", body.trimEnd().endsWith("--$boundary--"))
        return body.substringBefore("\r\n--$boundary--")
            .split("--$boundary\r\n")
            .filter { it.isNotEmpty() }
            .map { part ->
                val headers = part.substringBefore("\r\n\r\n")
                val content = part.substringAfter("\r\n\r\n").removeSuffix("\r\n")
                headers to content
            }
    }
}
