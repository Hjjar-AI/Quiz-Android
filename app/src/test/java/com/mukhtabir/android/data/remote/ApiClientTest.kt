package com.mukhtabir.android.data.remote

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.mukhtabir.android.R
import com.mukhtabir.android.core.AppPreferences
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Current transport contracts, including pre-dispatch rejection versus uncertain writes. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class,
    shadows = [TransportMasterKeysShadow::class, TransportPreferencesShadow::class])
class ApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var context: Context
    private lateinit var previousLocale: Locale

    @Before
    fun setUp() {
        previousLocale = Locale.getDefault()
        server = MockWebServer()
        server.start()
        context = ApplicationProvider.getApplicationContext()
        language("en")
    }

    @After
    fun tearDown() {
        server.shutdown()
        context.getSharedPreferences("mukhtabir_preferences", Context.MODE_PRIVATE).edit().clear().commit()
        Locale.setDefault(previousLocale)
    }

    private fun language(code: String) {
        context.getSharedPreferences("mukhtabir_preferences", Context.MODE_PRIVATE)
            .edit().putString("language", code).commit()
        AppPreferences.wrapContext(context)
    }

    private fun client(onExpired: () -> Unit = {}): ApiClient = ApiClient(context,
        server.url("/api/v1/").toString(), onSessionExpired = onExpired)

    private fun response(status: Int, body: String) = server.enqueue(MockResponse()
        .setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body))

    private fun token(value: String) = response(200, """{"code":200,"data":{"token":"$value"}}""")

    private fun rejected(reason: String, message: String = "رفض الطلب") = response(403,
        """{"code":403,"message":"$message","details":{"reason":"$reason"}}""")

    private fun request(): RecordedRequest = requireNotNull(server.takeRequest(2, TimeUnit.SECONDS)) {
        "Expected request was not received"
    }

    @Test
    fun `unwraps success envelope to data`() = runTest {
        response(200, """{"code":200,"message":"ok","data":{"id":1,"username":"alice"}}""")
        assertTrue(client().get(ApiEndpoints.Auth.ME).toString().contains("\"id\":1"))
        assertEquals("/api/v1/auth/me/", request().path)
    }

    @Test
    fun `non-2xx envelope retains status and specific English message`() = runTest {
        response(400, """{"code":400,"message":"bad payload","details":null}""")
        val error = runCatching { client().get(ApiEndpoints.Questions.BASE) }.exceptionOrNull() as ApiException
        assertEquals(400, error.statusCode)
        assertEquals("bad payload", error.message)
    }

    @Test
    fun `401 clears session and emits expiry exactly once`() = runTest {
        var expirations = 0
        val api = client { expirations++ }
        val event = CompletableDeferred<String>()
        val collector = launch(Dispatchers.Unconfined) { api.sessionExpired.collect { event.complete(it) } }
        try {
            response(401, """{"code":401,"message":"unauth","details":{"reason":"AUTHENTICATION_REQUIRED"}}""")
            val error = runCatching { api.get(ApiEndpoints.Auth.ME) }.exceptionOrNull() as ApiException
            assertEquals(401, error.statusCode)
            assertFalse(api.hasPersistedSession())
            assertTrue(event.isCompleted)
            assertEquals("session_expired", event.await())
            assertEquals(1, expirations)
        } finally {
            collector.cancel()
        }
    }

    @Test
    fun `typed csrf rejection refreshes token and retries identical mutation once`() = runTest {
        token("first-token")
        rejected("CSRF_FAILED")
        token("fresh-token")
        response(200, """{"code":200,"data":{"id":42}}""")
        val body = buildJsonObject {}
        assertTrue(client().post(ApiEndpoints.Questions.BASE, body).toString().contains("\"id\":42"))
        assertEquals("/api/v1/auth/csrf/", request().path)
        val first = request()
        assertEquals("/api/v1/auth/csrf/", request().path)
        val retry = request()
        assertEquals(first.path, retry.path)
        assertEquals(first.method, retry.method)
        assertEquals(first.body.readUtf8(), retry.body.readUtf8())
        assertEquals("first-token", first.getHeader("X-CSRFToken"))
        assertEquals("fresh-token", retry.getHeader("X-CSRFToken"))
        assertEquals(server.url("/").toString().removeSuffix("/"), retry.getHeader("Origin"))
        assertEquals(4, server.requestCount)
    }

    @Test
    fun `second csrf rejection stops without a third mutation`() = runTest {
        token("first-token")
        rejected("CSRF_FAILED")
        token("fresh-token")
        rejected("CSRF_FAILED")
        val error = runCatching { client().post(ApiEndpoints.Questions.BASE) }.exceptionOrNull() as ApiException
        assertEquals("CSRF_FAILED", error.reason)
        assertEquals(4, server.requestCount)
    }

    @Test
    fun `permission failure never retries even when message mentions csrf`() = runTest {
        token("token")
        rejected("PERMISSION_DENIED", "CSRF administrator permission missing")
        val error = runCatching { client().post(ApiEndpoints.Questions.BASE) }.exceptionOrNull() as ApiException
        assertEquals("PERMISSION_DENIED", error.reason)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `uncertain server failure does not replay mutation`() = runTest {
        token("token")
        response(500, """{"code":500,"message":"failure","details":{"reason":"SERVER_ERROR"}}""")
        val error = runCatching { client().post(ApiEndpoints.Questions.BASE) }.exceptionOrNull() as ApiException
        assertEquals(500, error.statusCode)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `login credential rejection does not expire another session flow`() = runTest {
        token("token")
        response(401, """{"code":401,"message":"رفض","details":{"reason":"INVALID_CREDENTIALS"}}""")
        var expirations = 0
        val error = runCatching { client { expirations++ }.login("sample", "wrong") }.exceptionOrNull() as ApiException
        assertEquals("INVALID_CREDENTIALS", error.reason)
        assertEquals(0, expirations)
        assertEquals(2, server.requestCount)
    }

    @Test
    fun `app language overrides device locale in headers and error display`() = runTest {
        Locale.setDefault(Locale.forLanguageTag("ar"))
        // Apply the existing English app preference, as Activity startup does.
        val localized = AppPreferences.wrapContext(context)
        rejected("INVALID_DATA")
        val error = runCatching { client().get(ApiEndpoints.Questions.BASE) }.exceptionOrNull() as ApiException
        assertEquals("en", request().getHeader("Accept-Language"))
        assertEquals(localized.getString(R.string.api_reason_invalid_data), error.summary)
        assertFalse(error.summary.any { it in '\u0600'..'\u06ff' })
    }

    @Test
    fun `Arabic app language renders reason with Arabic resources`() = runTest {
        language("ar")
        rejected("INVALID_DATA", "server text")
        val error = runCatching { client().get(ApiEndpoints.Questions.BASE) }.exceptionOrNull() as ApiException
        assertEquals("ar", request().getHeader("Accept-Language"))
        assertTrue(error.summary.any { it in '\u0600'..'\u06ff' })
    }

    @Test
    fun `export csrf rejection refreshes token and writes only confirmed artifact`() = runTest {
        val target = File.createTempFile("api-export-", ".json", context.cacheDir)
        try {
            token("first-token")
            rejected("CSRF_FAILED")
            token("fresh-token")
            server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json")
                .setHeader("Content-Disposition", "attachment; filename=questions.json").setBody("confirmed export"))
            val info = client().download(ApiEndpoints.Database.export("json", false), target, body = buildJsonObject {})
            assertEquals("confirmed export", target.readText())
            assertEquals("questions.json", info.filename)
            request()
            val first = request()
            request()
            val retry = request()
            assertEquals(first.body.readUtf8(), retry.body.readUtf8())
            assertEquals("POST", retry.method)
            assertEquals("fresh-token", retry.getHeader("X-CSRFToken"))
            assertEquals(4, server.requestCount)
        } finally {
            target.delete()
        }
    }

    @Test
    fun `repeated export rejection stops and removes partial artifact`() = runTest {
        val target = File.createTempFile("api-export-", ".json", context.cacheDir)
        try {
            target.writeText("partial")
            token("first-token")
            rejected("CSRF_FAILED")
            token("fresh-token")
            rejected("CSRF_FAILED")
            val error = runCatching {
                client().download(ApiEndpoints.Database.export("json", false), target, body = buildJsonObject {})
            }.exceptionOrNull() as ApiException
            assertEquals("CSRF_FAILED", error.reason)
            assertEquals(4, server.requestCount)
            assertFalse(target.exists())
        } finally {
            target.delete()
        }
    }

    @Test
    fun `permission denied export is not retried and leaves no artifact`() = runTest {
        val target = File.createTempFile("api-export-", ".json", context.cacheDir)
        try {
            token("token")
            rejected("PERMISSION_DENIED", "CSRF administrator permission missing")
            val error = runCatching {
                client().download(ApiEndpoints.Database.export("json", false), target, body = buildJsonObject {})
            }.exceptionOrNull() as ApiException
            assertEquals("PERMISSION_DENIED", error.reason)
            assertEquals(2, server.requestCount)
            assertFalse(target.exists())
        } finally {
            target.delete()
        }
    }

    @Test
    fun `response loss after server receives a mutation does not replay it`() = runTest {
        token("token")
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        val error = runCatching { client().post(ApiEndpoints.Questions.BASE) }.exceptionOrNull()
        assertTrue("Expected uncertain transport failure, got $error", error is ApiException)
        assertNull((error as ApiException).statusCode)
        request()
        assertEquals("POST", request().method)
        assertEquals(2, server.requestCount)
        assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `interrupted export removes partial output without replaying the export`() = runTest {
        val target = File.createTempFile("api-export-", ".json", context.cacheDir)
        try {
            token("token")
            server.enqueue(MockResponse().setBody("x".repeat(64 * 1024))
                .setSocketPolicy(SocketPolicy.DISCONNECT_DURING_RESPONSE_BODY))
            val error = runCatching {
                client().download(ApiEndpoints.Database.export("json", false), target, body = buildJsonObject {})
            }.exceptionOrNull()
            assertTrue("Expected incomplete download failure, got $error", error is ApiException)
            assertNull((error as ApiException).statusCode)
            assertFalse(target.exists())
            assertEquals(2, server.requestCount)
            assertEquals("/api/v1/auth/csrf/", request().path)
            val export = request()
            assertEquals("POST", export.method)
            assertEquals("/api/v1/" + ApiEndpoints.Database.export("json", false), export.path)
            assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS))
        } finally {
            target.delete()
        }
    }

    @Test
    fun `successful login rotates session cookie and fetches a new token before next write`() = runTest {
        token("before-login")
        server.enqueue(MockResponse().setBody("""{"code":200,"data":{"user":{"id":7,"username":"alice"}}}""")
            .addHeader("Set-Cookie", "sessionid=new-session; Path=/; Max-Age=3600; HttpOnly"))
        token("after-login")
        response(200, """{"code":200,"data":{"saved":true}}""")
        val api = client()
        assertEquals("alice", api.login("alice", "password").username)
        assertTrue(api.hasPersistedSession())
        api.post(ApiEndpoints.Questions.BASE)
        request()
        val login = request()
        assertEquals("/api/v1/auth/login/", login.path)
        assertEquals("before-login", login.getHeader("X-CSRFToken"))
        val refreshed = request()
        assertEquals("/api/v1/auth/csrf/", refreshed.path)
        assertTrue(refreshed.getHeader("Cookie").orEmpty().contains("sessionid=new-session"))
        assertEquals("after-login", request().getHeader("X-CSRFToken"))
        assertEquals(4, server.requestCount)
    }

    @Test
    fun `confirmed logout clears cookie storage before subsequent reads`() = runTest {
        var cleared = 0
        val api = ApiClient(context, server.url("/api/v1/").toString(), onSessionCleared = { cleared++ })
        server.enqueue(MockResponse().setBody("""{"code":200,"data":{}}""")
            .addHeader("Set-Cookie", "sessionid=old-session; Path=/; Max-Age=3600; HttpOnly"))
        api.get(ApiEndpoints.Auth.ME)
        assertTrue(api.hasPersistedSession())
        token("token")
        response(200, """{"code":200,"data":{}}""")
        api.logout()
        assertFalse(api.hasPersistedSession())
        assertEquals(1, cleared)
        response(200, """{"code":200,"data":{}}""")
        api.get(ApiEndpoints.Auth.ME)
        request(); request()
        assertEquals("/api/v1/auth/logout/", request().path)
        assertNull(request().getHeader("Cookie"))
    }

    @Test
    fun `failed logout retains session cookie for explicit recovery`() = runTest {
        var cleared = 0
        val api = ApiClient(context, server.url("/api/v1/").toString(), onSessionCleared = { cleared++ })
        server.enqueue(MockResponse().setBody("""{"code":200,"data":{}}""")
            .addHeader("Set-Cookie", "sessionid=current; Path=/; Max-Age=3600; HttpOnly"))
        api.get(ApiEndpoints.Auth.ME)
        token("token")
        response(500, """{"code":500,"details":{"reason":"SERVER_ERROR"}}""")
        val error = runCatching { api.logout() }.exceptionOrNull()
        assertTrue(error is ApiException)
        assertTrue(api.hasPersistedSession())
        assertEquals(0, cleared)
        assertEquals(3, server.requestCount)
    }

    @Test
    fun `cancelled read cancels the call without retrying or expiring session`() = runTest {
        var expirations = 0
        val api = client { expirations++ }
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        val pending = async(Dispatchers.IO) { api.get(ApiEndpoints.Auth.ME) }
        assertEquals("/api/v1/auth/me/", request().path)
        pending.cancelAndJoin()
        assertTrue(pending.isCancelled)
        assertEquals(0, expirations)
        assertEquals(1, server.requestCount)
        assertNull(server.takeRequest(200, TimeUnit.MILLISECONDS))
    }

    @Test
    fun `non-json proxy 401 preserves unauthorized status and expires session`() = runTest {
        var expirations = 0
        server.enqueue(MockResponse().setResponseCode(401).setBody("<html>internal proxy detail</html>"))
        val error = runCatching { client { expirations++ }.get(ApiEndpoints.Auth.ME) }.exceptionOrNull()
        assertTrue(error is ApiException)
        assertEquals(401, (error as ApiException).statusCode)
        assertFalse(error.message.contains("internal proxy detail"))
        assertEquals(1, expirations)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `session revocation sends opaque string handle and password body`() = runTest {
        val handle = "a".repeat(64)
        token("token")
        response(200, """{"code":200,"data":{"revoked":true,"is_current":false}}""")
        val result = DeviceSessionsService(client()).revoke(handle, "current-password")
        assertTrue(result.revoked)
        assertFalse(result.current)
        request()
        val revoke = request()
        assertEquals("/api/v1/auth/sessions/$handle/revoke/", revoke.path)
        assertEquals("POST", revoke.method)
        assertTrue(revoke.body.readUtf8().contains("\"current_password\":\"current-password\""))
    }

    @Test
    fun `discard uses query identity and no DELETE body`() = runTest {
        token("token")
        response(200, """{"code":200,"data":{"deleted_count":1}}""")
        TestSessionService(client()).discard(TestMode.STUDY, "session with spaces")
        request()
        val discard = request()
        assertEquals("DELETE", discard.method)
        assertEquals("session with spaces", discard.requestUrl?.queryParameter("session_id"))
        assertEquals(0L, discard.bodySize)
        assertEquals(2, server.requestCount)
    }
}
