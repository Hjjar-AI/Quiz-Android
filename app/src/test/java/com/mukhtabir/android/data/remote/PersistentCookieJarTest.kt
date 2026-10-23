package com.mukhtabir.android.data.remote

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Storage is shadowed on the JVM; this covers cookie behavior, not device encryption. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class,
    shadows = [TransportMasterKeysShadow::class, TransportPreferencesShadow::class])
class PersistentCookieJarTest {
    private lateinit var context: Context
    private val origin = "https://study.example.invalid/api/v1/".toHttpUrl()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        preferences().edit().clear().commit()
    }

    private fun preferences() = context.getSharedPreferences(
        "transport-test-mukhtabir_secure_http", Context.MODE_PRIVATE)

    private fun cookie(name: String = "sessionid", value: String = "current", path: String = "/") =
        Cookie.Builder().name(name).value(value).hostOnlyDomain(origin.host).path(path)
            .expiresAt(System.currentTimeMillis() + 60_000).httpOnly().secure().build()

    @Test
    fun `clearing session rejects cookies from an older response generation`() {
        val jar = PersistentCookieJar(context)
        val oldRequest = jar.forCurrentSession()
        jar.clear()
        oldRequest.saveFromResponse(origin, listOf(cookie(value = "late-old-account")))
        assertFalse(jar.hasSessionCookie())
        assertTrue(PersistentCookieJar(context).loadForRequest(origin).isEmpty())
    }

    @Test
    fun `old requests cannot read or overwrite the next account cookie`() {
        val jar = PersistentCookieJar(context)
        val oldRequest = jar.forCurrentSession()
        jar.clear()
        jar.forCurrentSession().saveFromResponse(origin, listOf(cookie(value = "new-account")))
        assertTrue(oldRequest.loadForRequest(origin).isEmpty())
        oldRequest.saveFromResponse(origin, listOf(cookie(value = "old-account")))
        assertEquals(listOf("new-account"), PersistentCookieJar(context).loadForRequest(origin).map { it.value })
    }

    @Test
    fun `persisted cookies retain host path expiry and security flags`() {
        val expected = cookie(path = "/api/")
        PersistentCookieJar(context).saveFromResponse(origin, listOf(expected))
        val restored = PersistentCookieJar(context).loadForRequest(origin).single()
        assertEquals(expected, restored)
        assertTrue(restored.hostOnly)
        assertTrue(restored.secure)
        assertTrue(restored.httpOnly)
        assertEquals(expected.expiresAt, restored.expiresAt)
    }

    @Test
    fun `cookies stay within their host path and https restrictions`() {
        val jar = PersistentCookieJar(context)
        jar.saveFromResponse(origin, listOf(cookie(path = "/api/")))
        for (url in listOf("https://other.example.invalid/api/v1/", "https://study.example.invalid/outside/",
                "http://study.example.invalid/api/v1/")) {
            assertTrue(url, jar.loadForRequest(url.toHttpUrl()).isEmpty())
        }
        assertEquals(1, jar.loadForRequest(origin).size)
    }

    @Test
    fun `expired replacement removes matching persisted session`() {
        val jar = PersistentCookieJar(context)
        jar.saveFromResponse(origin, listOf(cookie()))
        val expired = Cookie.Builder().name("sessionid").value("deleted").hostOnlyDomain(origin.host)
            .path("/").expiresAt(0).build()
        jar.saveFromResponse(origin, listOf(expired))
        assertFalse(jar.hasSessionCookie())
        assertTrue(PersistentCookieJar(context).loadForRequest(origin).isEmpty())
    }

    @Test
    fun `session detection accepts only configured Django cookie names`() {
        val jar = PersistentCookieJar(context)
        for ((name, isSession) in listOf("sessionid" to true, "sqlite_sessionid" to true,
                "sqlite_sessionid_012345abcdef" to true, "csrftoken" to false,
                "sqlite_sessionid_invalid" to false)) {
            jar.clear()
            jar.saveFromResponse(origin, listOf(cookie(name = name)))
            assertEquals(name, isSession, jar.hasSessionCookie())
        }
    }

    @Test
    fun `server change clears persisted cookies and invalidates pending responses`() {
        val jar = PersistentCookieJar(context)
        jar.saveFromResponse(origin, listOf(cookie()))
        val pending = jar.forCurrentSession()
        assertTrue(jar.clearForServerChange().isSuccess)
        pending.saveFromResponse(origin, listOf(cookie(value = "late")))
        assertTrue(pending.loadForRequest(origin).isEmpty())
        assertTrue(PersistentCookieJar(context).loadForRequest(origin).isEmpty())
    }

    @Test
    fun `same-name replacement retains a cookie for another path`() {
        val jar = PersistentCookieJar(context)
        jar.saveFromResponse(origin, listOf(cookie(value = "root"), cookie(value = "api", path = "/api/")))
        jar.saveFromResponse(origin, listOf(cookie(value = "new-root")))
        assertEquals(setOf("api", "new-root"), jar.loadForRequest(origin).map { it.value }.toSet())
        assertEquals(listOf("new-root"), jar.loadForRequest("https://study.example.invalid/".toHttpUrl()).map { it.value })
    }

    @Test
    fun `corrupt persisted json does not restore an authenticated session`() {
        preferences().edit().putString("cookies", "{invalid persisted data").commit()
        val jar = PersistentCookieJar(context)
        assertFalse(jar.hasSessionCookie())
        assertTrue(jar.loadForRequest(origin).isEmpty())
    }
}
