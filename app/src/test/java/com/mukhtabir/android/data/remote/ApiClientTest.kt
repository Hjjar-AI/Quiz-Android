package com.mukhtabir.android.data.remote

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.buildJsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.HttpURLConnection

/**
 * ApiClient behaviour tests.
 *
 * Scope (Phase 0, item 97):
 *
 *   1. Envelope unwrapping — an `ApiEnvelope` response is decoded to
 *      its `data` field, not the outer object.
 *   2. Error envelope — a non-2xx response carrying an envelope is
 *      surfaced as an `ApiException` with the envelope's `message`
 *      and the HTTP status code.
 *   3. 401 handling — a 401 response from a non-login endpoint clears
 *      the session and emits on `sessionExpired`.
 *   4. CSRF retry — a 403 whose message mentions CSRF causes the
 *      client to re-fetch the CSRF endpoint and retry the original
 *      request exactly once.
 *   5. Non-CSRF 403 — a 403 whose message does NOT mention CSRF is
 *      NOT retried.
 *
 * The tests use Robolectric because `PersistentCookieJar` uses
 * `EncryptedSharedPreferences`, which needs an Android context.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class ApiClientTest {

    private lateinit var server: MockWebServer
    private lateinit var context: Context

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun newClient(): ApiClient =
        ApiClient(context = context, baseUrl = server.url("/api/v1/").toString())

    // ── 1. Envelope unwrapping ─────────────────────────────────────

    @Test
    fun `unwraps success envelope to data`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(
                    """
                    {
                      "code": 200,
                      "message": "ok",
                      "data": {"id": 1, "username": "alice"}
                    }
                    """.trimIndent(),
                )
                .addHeader("Content-Type", "application/json"),
        )

        val client = newClient()
        val payload = client.get("me/")

        val obj = payload.toString()
        assertTrue("payload should contain id=1: $obj", obj.contains("\"id\":1"))

        val request = server.takeRequest()
        assertEquals("/api/v1/me/", request.path)
    }

    // ── 2. Error envelope ──────────────────────────────────────────

    @Test
    fun `non-2xx envelope surfaces as ApiException`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_BAD_REQUEST)
                .setBody(
                    """
                    {
                      "code": 400,
                      "message": "bad payload",
                      "details": null
                    }
                    """.trimIndent(),
                )
                .addHeader("Content-Type", "application/json"),
        )

        val client = newClient()
        val error = runCatching { client.get("questions/") }.exceptionOrNull()
        assertNotNull("expected an exception", error)
        assertTrue("expected ApiException, got ${error!!::class.simpleName}", error is ApiException)
        assertEquals(400, (error as ApiException).statusCode)
        assertEquals("bad payload", error.message)
    }

    // ── 3. 401 handling ────────────────────────────────────────────

    @Test
    fun `401 emits sessionExpired and clears cookies`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_UNAUTHORIZED)
                .setBody(
                    """
                    {"code": 401, "message": "unauth", "details": null}
                    """.trimIndent(),
                )
                .addHeader("Content-Type", "application/json"),
        )

        val client = newClient()

        // Start collecting the flow first, in a separate coroutine,
        // so nothing is lost between the 401 and the assertion.
        val expired = async(Dispatchers.Unconfined) {
            withTimeoutOrNull(2_000) {
                client.sessionExpired.first()
            }
        }

        // Give the collector a moment to subscribe before the request.
        delay(50)

        val error = runCatching { client.get("me/") }.exceptionOrNull()
        assertNotNull(error)
        assertTrue(error is ApiException)
        assertEquals(401, (error as ApiException).statusCode)
        assertFalse(
            "session cookie should be cleared after 401",
            client.hasPersistedSession(),
        )

        val emitted = expired.await()
        assertNotNull("sessionExpired should have emitted", emitted)
    }

    // ── 4. CSRF retry ──────────────────────────────────────────────

    @Test
    fun `403 with csrf message retries after refetching token`() = runTest {
        // 1. First mutation returns 403 CSRF failure.
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_FORBIDDEN)
                .setBody(
                    """
                    {"code": 403, "message": "CSRF Failed: token invalid", "details": null}
                    """.trimIndent(),
                )
                .addHeader("Content-Type", "application/json"),
        )
        // 2. The client refetches the CSRF token.
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(
                    """
                    {"code": 200, "message": "ok", "data": {"token": "fresh-token"}}
                    """.trimIndent(),
                )
                .addHeader("Content-Type", "application/json"),
        )
        // 3. The retried mutation succeeds.
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_OK)
                .setBody(
                    """
                    {"code": 200, "message": "ok", "data": {"id": 42}}
                    """.trimIndent(),
                )
                .addHeader("Content-Type", "application/json"),
        )

        val client = newClient()
        val result = client.post("questions/", body = buildJsonObject {})
        assertTrue(result.toString().contains("\"id\":42"))

        val first = server.takeRequest()
        val second = server.takeRequest()
        val third = server.takeRequest()
        assertEquals("/api/v1/questions/", first.path)
        assertEquals("/api/v1/auth/csrf/", second.path)
        assertEquals("/api/v1/questions/", third.path)

        val retriedHeader = third.getHeader("X-CSRFToken")
        assertEquals("fresh-token", retriedHeader)
    }

    // ── 5. Non-CSRF 403 is NOT retried ─────────────────────────────

    @Test
    fun `403 without csrf message does not retry`() = runTest {
        server.enqueue(
            MockResponse()
                .setResponseCode(HttpURLConnection.HTTP_FORBIDDEN)
                .setBody(
                    """
                    {"code": 403, "message": "forbidden", "details": null}
                    """.trimIndent(),
                )
                .addHeader("Content-Type", "application/json"),
        )

        val client = newClient()
        val error = runCatching {
            client.post("questions/", body = buildJsonObject {})
        }.exceptionOrNull()

        assertNotNull(error)
        assertTrue(error is ApiException)
        assertEquals(403, (error as ApiException).statusCode)

        val first = server.takeRequest()
        assertEquals("/api/v1/questions/", first.path)

        // Only one request should have been issued. If a retry ran,
        // a second request would be sitting in the queue.
        assertEquals(
            "no second request should have been issued",
            1,
            server.requestCount,
        )
    }
}