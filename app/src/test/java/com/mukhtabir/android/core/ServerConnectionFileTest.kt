package com.mukhtabir.android.core

import java.io.ByteArrayInputStream
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ServerConnectionFileTest {
    private fun read(value: String) = ServerConnectionFile.read(ByteArrayInputStream(value.toByteArray(Charsets.UTF_8)))
    private fun file(address: String = "https://example.org") = """{"format":"mukhtabir.connection","version":1,"bundle":{"connection":{"endpoint":{"api":"$address"}}}}"""
    @Test fun `root and prefixed API addresses normalize consistently`() {
        mapOf(" https://EXAMPLE.org " to "https://example.org/api/v1/",
            "http://localhost:8000" to "http://localhost:8000/api/v1/",
            "https://example.org/prefix/api/v1" to "https://example.org/prefix/api/v1/",
            "https://example.org/api/v1/" to "https://example.org/api/v1/").forEach { (input, expected) ->
            assertEquals(input, expected, ServerSettings.normalize(input))
        }
    }
    @Test fun `credentials query fragments unsupported paths and schemes are rejected`() {
        listOf("", "example.org", "ftp://example.org", "https://u:p@example.org", "https://example.org?token=x",
            "https://example.org#secret", "https://example.org/admin/", "https://exam ple.org", "https://example.org/\napi/v1/",
            "https://example.org/" + "a".repeat(2048)).forEach { assertNull(it, ServerSettings.normalize(it)) }
    }
    @Test fun `private HTTP IPv4 and IPv6 origins remain supported`() {
        assertEquals("http://192.168.1.20:8000/api/v1/", ServerSettings.normalize("http://192.168.1.20:8000"))
        assertEquals("http://[::1]:8000/api/v1/", ServerSettings.normalize("http://[::1]:8000"))
    }
    @Test fun `export import round trip preserves normalized endpoint only`() {
        val exported = ServerConnectionFile.export("https://example.org/prefix/api/v1")
        assertEquals("https://example.org/prefix/api/v1/", read(exported))
        val root = Json.parseToJsonElement(exported).jsonObject
        assertEquals("mukhtabir.connection", root.getValue("format").jsonPrimitive.content)
        assertFalse(exported.contains("password")); assertFalse(exported.contains("cookie"))
    }
    @Test fun `each export creates distinct public metadata identities`() {
        fun bundle() = Json.parseToJsonElement(ServerConnectionFile.export("https://example.org")).jsonObject.getValue("bundle").jsonObject
        val first = bundle(); val second = bundle()
        assertNotEquals(first["identity"], second["identity"])
        assertNotEquals(first.getValue("connection").jsonObject["identity"], second.getValue("connection").jsonObject["identity"])
    }
    @Test fun `UTF8 BOM and unrelated metadata are accepted without authenticating anything`() {
        assertEquals("https://example.org/api/v1/", read("\uFEFF" + file().replace("\"bundle\":", "\"credentials\":{\"token\":\"untrusted\"},\"bundle\":")))
    }
    @Test fun `wrong format version shape and invalid endpoints are rejected`() {
        listOf("[]", "null", "broken", "{}", file().replace("mukhtabir.connection", "other"),
            file().replace("\"version\":1", "\"version\":2"), file().replace("\"endpoint\"", "\"missing\""),
            file("https://u:p@example.org")).forEach { value -> assertThrows(Exception::class.java) { read(value) } }
    }
    @Test fun `connection file byte boundary is inclusive`() {
        val value = file()
        assertEquals("https://example.org/api/v1/", read(value + " ".repeat(65536 - value.toByteArray().size)))
        assertThrows(IllegalArgumentException::class.java) { read(value + " ".repeat(65537 - value.toByteArray().size)) }
    }
    @Test fun `export refuses an invalid connection`() {
        assertThrows(IllegalArgumentException::class.java) { ServerConnectionFile.export("https://example.org?password=secret") }
    }
}
