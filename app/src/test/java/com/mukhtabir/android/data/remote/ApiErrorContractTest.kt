package com.mukhtabir.android.data.remote

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Current backend error contracts; no server, device or old-version fixtures. */
class ApiErrorContractTest {
    @Test
    fun `typed csrf rejection survives sanitization and translated summaries`() {
        val error = ApiException(403, "تعذر التحقق من الطلب", details = buildJsonObject {
            put("reason", "CSRF_FAILED")
        })

        assertEquals("CSRF_FAILED", error.reason)
        assertTrue(error.isCsrfFailure)
        assertTrue(error.fieldErrors.isEmpty())
    }

    @Test
    fun `permission rejection mentioning csrf is not retryable`() {
        val error = ApiException(403, "Permission denied by the CSRF administrator", details = buildJsonObject {
            put("reason", "PERMISSION_DENIED")
        })

        assertFalse(error.isCsrfFailure)
    }

    @Test
    fun `message text without a typed reason does not authorize a retry`() {
        assertFalse(ApiException(403, "CSRF Failed").isCsrfFailure)
    }

    @Test
    fun `csrf token on another status does not authorize a retry`() {
        val error = ApiException(409, "Refresh the saved state", details = buildJsonObject {
            put("reason", "CSRF_FAILED")
        })

        assertFalse(error.isCsrfFailure)
    }

    @Test
    fun `session and offline reasons stay machine readable without becoming field text`() {
        for (reason in listOf("SESSION_PROGRESS_CHANGED", "ATTEMPT_PROGRESS_CHANGED", "OFFLINE_DOWNLOAD_LIMIT")) {
            val error = ApiException(409, "Review required", details = buildJsonObject {
                put("reason", reason)
                put("question", "Check this question")
            })

            assertEquals(reason, error.reason)
            assertEquals(listOf("question" to "Check this question"), error.fieldErrors)
            assertFalse(error.message.contains(reason))
        }
    }

    @Test
    fun `malformed or oversized reason tokens are discarded`() {
        for (reason in listOf("csrf failed", "<internal-path>", "A".repeat(81))) {
            val error = ApiException(400, "Invalid data", details = buildJsonObject {
                put("reason", reason)
            })

            assertNull(error.reason)
            assertTrue(error.fieldErrors.isEmpty())
        }
    }

    @Test
    fun `English field fallback retains raw structured details and field identity`() {
        val raw = "اسم المستخدم مستخدم"
        val error = ApiException(400, "Invalid data", details = buildJsonObject {
            put("reason", "VALIDATION_ERROR")
            put("username", JsonArray(listOf(JsonPrimitive(raw))))
            put("password", "Use a longer password")
        }, fieldErrorFallback = "Check the entered value")

        assertEquals(listOf("username" to "Check the entered value", "password" to "Use a longer password"), error.fieldErrors)
        assertEquals(JsonArray(listOf(JsonPrimitive(raw))), error.details?.get("username"))
        assertFalse(error.message.contains(raw))
    }

    @Test
    fun `Arabic field messages remain available without an English fallback`() {
        val raw = "اسم المستخدم مستخدم"
        val error = ApiException(400, "بيانات غير صالحة", details = buildJsonObject {
            put("reason", "VALIDATION_ERROR")
            put("username", raw)
        })

        assertEquals(listOf("username" to raw), error.fieldErrors)
        assertTrue(error.message.contains(raw))
    }
}
