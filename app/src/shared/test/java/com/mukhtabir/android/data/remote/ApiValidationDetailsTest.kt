package com.mukhtabir.android.data.remote

import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test

class ApiValidationDetailsTest {
    private fun clean(value: String) = safeValidationDetails(Json.parseToJsonElement(value))
    @Test fun `unknown root fields and non objects are discarded`() {
        assertNull(clean("[]")); assertNull(clean("null")); assertNull(clean("""{"stack":"private","token":"secret"}"""))
    }
    @Test fun `nested serializer indices locales and known fields survive`() {
        val result = clean("""{"translations":{"en-US":{"choices":["required"]}},"question":["blank"]}""")!!
        assertEquals(listOf("translations" to "required", "question" to "blank"), validationMessages(result))
    }
    @Test fun `machine reasons are bounded and not presented as validation messages`() {
        assertEquals("VERSION_CONFLICT", clean("""{"reason":"VERSION_CONFLICT"}""")!!.getValue("reason").jsonPrimitive.content)
        listOf("lowercase", "HAS SPACE", "A".repeat(81)).forEach { assertNull(clean("""{"reason":"$it"}""")) }
        assertTrue(validationMessages(clean("""{"reason":"DENIED"}""")).isEmpty())
    }
    @Test fun `numbers booleans null and unknown nested fields are removed`() {
        assertEquals(listOf("choices" to "safe"), validationMessages(clean("""{"question":3,"password":false,"detail":null,"choices":{"secret":"hidden","0":"safe"}}""")))
    }
    @Test fun `blank overlong and unsafe control messages are dropped`() {
        val node = buildJsonObject { put("question", JsonArray(listOf("", "a".repeat(2001), "bad\u0001", "safe\nline\tend").map(::JsonPrimitive))) }
        assertEquals(listOf("question" to "safe\nline\tend"), validationMessages(safeValidationDetails(node)))
    }
    @Test fun `message limit accepts exactly two thousand characters`() {
        val text = "a".repeat(2000)
        assertEquals(listOf("detail" to text), validationMessages(safeValidationDetails(buildJsonObject { put("detail", text) })))
    }
    @Test fun `array retention is bounded at twenty entries`() {
        val details = safeValidationDetails(buildJsonObject { put("choices", JsonArray((1..25).map { JsonPrimitive("issue-$it") })) })!!
        assertEquals(20, details.getValue("choices").jsonArray.size)
        assertEquals("issue-20", validationMessages(details).last().second)
    }
    @Test fun `deep unknown payloads do not become readable errors`() {
        assertNull(clean("""{"question":{"choices":{"choices":{"choices":{"choices":["too deep"]}}}}}"""))
    }
    @Test fun `flattening deduplicates by field and caps display at thirty`() {
        val details = buildJsonObject {
            put("question", JsonArray((1..35).map { JsonPrimitive("issue-$it") }))
            put("choices", JsonArray(listOf(JsonPrimitive("same"), JsonPrimitive("same"))))
        }
        assertEquals(30, validationMessages(details).size)
        assertEquals(listOf("question" to "same", "choices" to "same"), validationMessages(clean("""{"question":["same","same"],"choices":["same"]}""")))
    }
}
