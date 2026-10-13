package com.mukhtabir.android.core

import java.io.InputStream
import java.util.UUID
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/** Public convenience file, not a credential or proof of server identity. */
object ServerConnectionFile {
    private const val MAX_BYTES = 64 * 1024

    fun read(stream: InputStream): String {
        val buffer = java.io.ByteArrayOutputStream()
        val chunk = ByteArray(4096)
        while (true) {
            val count = stream.read(chunk)
            if (count < 0) break
            require(buffer.size() + count <= MAX_BYTES)
            buffer.write(chunk, 0, count)
        }
        val root = Json.parseToJsonElement(buffer.toString("UTF-8").removePrefix("\uFEFF")) as? JsonObject
            ?: error("Invalid connection file")
        require((root["format"] as? JsonPrimitive)?.contentOrNull == "mukhtabir.connection")
        require((root["version"] as? JsonPrimitive)?.intOrNull == 1)
        val bundle = root["bundle"] as? JsonObject ?: error("Missing bundle")
        val connection = bundle["connection"] as? JsonObject ?: error("Missing connection")
        val endpoint = connection["endpoint"] as? JsonObject ?: error("Missing endpoint")
        val address = (endpoint["api"] as? JsonPrimitive)?.contentOrNull ?: error("Missing address")
        return requireNotNull(ServerSettings.normalize(address))
    }

    fun export(address: String): String {
        val url = requireNotNull(ServerSettings.normalize(address))
        return buildJsonObject {
            put("format", "mukhtabir.connection")
            put("version", 1)
            put("bundle", buildJsonObject {
                put("identity", UUID.randomUUID().toString())
                put("connection", buildJsonObject {
                    put("identity", UUID.randomUUID().toString())
                    put("endpoint", buildJsonObject { put("api", url) })
                })
                put("metadata", buildJsonObject {
                    put("identity", UUID.randomUUID().toString())
                    put("purpose", "connection convenience")
                })
            })
        }.toString()
    }
}
