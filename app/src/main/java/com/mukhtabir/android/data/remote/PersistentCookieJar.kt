package com.mukhtabir.android.data.remote

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

/** Encrypted persistent storage for Django's session and CSRF cookies. */
class PersistentCookieJar(context: Context) : CookieJar {
    private val json = Json { ignoreUnknownKeys = true }
    private val preferences = EncryptedSharedPreferences.create(
        PREFERENCES_NAME,
        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
        context.applicationContext,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    private val cookies = loadCookies().toMutableList()
    private var generation = 0L

    /** Discard late cookies from calls started before logout/account/server cleanup. */
    @Synchronized
    fun forCurrentSession(): CookieJar {
        val owner = this
        val expectedGeneration = generation
        return object : CookieJar {
            override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
                synchronized(owner) {
                    if (owner.generation == expectedGeneration) owner.saveFromResponse(url, cookies)
                }
            }

            override fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(owner) {
                if (owner.generation == expectedGeneration) owner.loadForRequest(url) else emptyList()
            }
        }
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        val now = System.currentTimeMillis()
        this.cookies.removeAll { stored ->
            stored.expiresAt < now || cookies.any { incoming -> incoming.sameIdentityAs(stored) }
        }
        this.cookies += cookies.filter { it.expiresAt >= now }
        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val removed = cookies.removeAll { it.expiresAt < now }
        if (removed) persist()
        return cookies.filter { it.matches(url) }
    }

    @Synchronized
    fun hasSessionCookie(): Boolean = cookies.any { cookie ->
        cookie.expiresAt >= System.currentTimeMillis() && SESSION_COOKIE_NAME.matches(cookie.name)
    }

    @Synchronized
    fun clear() {
        generation++
        cookies.clear()
        preferences.edit().remove(COOKIES_KEY).apply()
    }

    @Synchronized
    fun clearForServerChange(): Result<Unit> = runCatching {
        generation++
        cookies.clear()
        check(preferences.edit().remove(COOKIES_KEY).commit())
    }

    private fun loadCookies(): List<Cookie> {
        val stored = preferences.getString(COOKIES_KEY, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(StoredCookie.serializer()), stored)
                .mapNotNull(StoredCookie::toCookie)
                .filter { it.expiresAt >= System.currentTimeMillis() }
        }.getOrDefault(emptyList())
    }

    private fun persist() {
        val stored = cookies.map { StoredCookie.fromCookie(it) }
        preferences.edit()
            .putString(
                COOKIES_KEY,
                json.encodeToString(ListSerializer(StoredCookie.serializer()), stored),
            )
            .apply()
    }

    private fun Cookie.sameIdentityAs(other: Cookie): Boolean =
        name == other.name && domain == other.domain && path == other.path

    private companion object {
        const val PREFERENCES_NAME = "mukhtabir_secure_http"
        const val COOKIES_KEY = "cookies"
        // Matches current Django defaults and isolated SQLite's 12-hex instance suffix.
        val SESSION_COOKIE_NAME = Regex("(?:sessionid|sqlite_sessionid(?:_[0-9a-f]{12})?)")
    }
}

@Serializable
private data class StoredCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean,
) {
    fun toCookie(): Cookie? = runCatching {
        Cookie.Builder()
            .name(name)
            .value(value)
            .expiresAt(expiresAt)
            .apply {
                if (hostOnly) hostOnlyDomain(domain) else domain(domain)
                path(path)
                if (secure) secure()
                if (httpOnly) httpOnly()
            }
            .build()
    }.getOrNull()

    companion object {
        fun fromCookie(cookie: Cookie) = StoredCookie(
            name = cookie.name,
            value = cookie.value,
            expiresAt = cookie.expiresAt,
            domain = cookie.domain,
            path = cookie.path,
            secure = cookie.secure,
            httpOnly = cookie.httpOnly,
            hostOnly = cookie.hostOnly,
        )
    }
}
