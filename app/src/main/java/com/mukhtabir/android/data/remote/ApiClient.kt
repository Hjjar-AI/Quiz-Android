package com.mukhtabir.android.data.remote

import android.content.Context
import com.mukhtabir.android.R
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.buildJsonObject
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Shared Django API transport.
 *
 * It implements the same contract as the Vue Axios client: response envelopes,
 * session cookies, Accept-Language, and CSRF tokens on every mutation.
 */
class ApiClient(context: Context, baseUrl: String, private val onSessionCleared: () -> Unit = {},
    private val onSessionExpired: () -> Unit = onSessionCleared) {
    private val applicationContext = context.applicationContext
    private val normalizedBaseUrl = baseUrl.trimEnd('/') + "/"
    private val baseHttpUrl = normalizedBaseUrl.toHttpUrl()
    private val apiOrigin = baseHttpUrl.newBuilder()
        .encodedPath("/")
        .query(null)
        .fragment(null)
        .build().toString().removeSuffix("/")
    private val cookieJar = PersistentCookieJar(context)
    @OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        coerceInputValues = true
    }
    // Authenticated responses include private images, answers, and session status.
    // Keep the online transport uncached until an explicit user-scoped offline
    // storage policy exists; never override server cache directives.
    private val legacyCacheDirectory = applicationContext.cacheDir.resolve("api_http_cache")
    private val http = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .callTimeout(45, java.util.concurrent.TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .addNetworkInterceptor { chain ->
            val url = chain.request().url
            // Includes redirected requests: HTTPS selections cannot downgrade to HTTP,
            // and cleartext images/downloads cannot silently move to another server.
            if (url.scheme == "http" && (baseHttpUrl.scheme != "http" ||
                    url.host != baseHttpUrl.host || url.port != baseHttpUrl.port)) {
                throw IOException(applicationContext.getString(R.string.server_http_origin_blocked))
            }
            chain.proceed(chain.request())
        }
        .addInterceptor { chain ->
            val request = chain.request()
            try {
                chain.proceed(request)
            } catch (first: IOException) {
                // Only reads may be retried here. A failed write may have committed.
                if (request.method == "GET" && !chain.call().isCanceled()) {
                    chain.proceed(request)
                } else throw first
            }
        }
        .build()

    init {
        clearLegacyCache()
        // Transfer drafts are memory-only: no live ViewModel exists at application startup.
        applicationContext.cacheDir.listFiles()?.filter { it.name.startsWith("transfer-") }?.forEach { it.delete() }
    }

    private val csrfMutex = Mutex()
    @Volatile private var cachedCsrfToken: String? = null
    private val _sessionExpired = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val sessionExpired = _sessionExpired.asSharedFlow()
    private val _profileRefreshRequests = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val profileRefreshRequests = _profileRefreshRequests.asSharedFlow()

    fun hasPersistedSession(): Boolean = cookieJar.hasSessionCookie()

    suspend fun login(username: String, password: String): UserDto {
        // Retained server-profile recovery is unlocked/cleared only after account binding.
        clearTransportSession()
        val data = requestData(
            method = "POST",
            path = ApiEndpoints.Auth.LOGIN,
            body = json.encodeToJsonElement(
                LoginRequest.serializer(),
                LoginRequest(username, password),
            ),
        )
        // Django rotates the token on login; fetch the new token for writes.
        cachedCsrfToken = null
        return decode(LoginPayload.serializer(), data).user
    }

    suspend fun currentUser(): UserDto =
        decode(UserDto.serializer(), get(ApiEndpoints.Auth.ME))

    suspend fun changePassword(currentPassword: String, newPassword: String) {
        post(
            ApiEndpoints.Auth.CHANGE_PASSWORD,
            encode(
                ChangePasswordRequest.serializer(),
                ChangePasswordRequest(currentPassword, newPassword),
            ),
        )
    }

    suspend fun logout() {
        post(ApiEndpoints.Auth.LOGOUT)
        clearSession()
    }

    /** Address changes revoke transport without deleting the retained encrypted profile. */
    @Volatile private var serverDetached = false

    @Synchronized
    fun detachServer() {
        serverDetached = true
        clearTransportSession()
    }

    @Synchronized
    private fun clearTransportSession() {
        cachedCsrfToken = null
        cookieJar.clear()
        http.dispatcher.cancelAll()
        applicationContext.cacheDir.listFiles()?.filter { it.name.startsWith("transfer-") }?.forEach { it.delete() }
        clearLegacyCache()
    }

    @Synchronized
    fun clearSession() {
        if (serverDetached) return
        clearTransportSession()
        onSessionCleared()
    }

    /** Ordinary online expiry retains downloaded study for its last bound owner. */
    @Synchronized
    fun expireOnlineSession(reason: String? = null) {
        if (serverDetached) return
        clearTransportSession()
        if (reason == "ACCOUNT_EXPIRED") onSessionCleared() else onSessionExpired()
    }

    private fun clearLegacyCache() {
        // No client opens this directory anymore, even if filesystem cleanup fails.
        runCatching { legacyCacheDirectory.deleteRecursively() }
    }

    suspend fun loadBytes(source: String): ByteArray {
        val url = runCatching { source.toHttpUrl() }
            .getOrElse {
                baseHttpUrl.resolve(source)
                    ?: throw ApiException(
                        null,
                        applicationContext.getString(R.string.error_invalid_image_url),
                    )
            }
        val request = Request.Builder()
            .url(url)
            .header("Accept", "image/*")
            .header("Accept-Language", Locale.getDefault().language)
            .get()
            .build()
        return try {
            perform(request) { response ->
                // An external image host's rejection says nothing about our login.
                if (response.code == 401 && response.request.url.host == baseHttpUrl.host &&
                    response.request.url.port == baseHttpUrl.port &&
                    response.request.url.scheme == baseHttpUrl.scheme
                ) expireSession()
                if (!response.isSuccessful) {
                    throw ApiException(
                        response.code,
                        applicationContext.getString(errorResource(response.code)),
                    )
                }
                response.body?.bytes() ?: throw ApiException(
                    response.code,
                    applicationContext.getString(R.string.error_empty_image_response),
                )
            }
        } catch (error: ApiException) {
            throw error
        } catch (error: IOException) {
            throw ApiException(
                null,
                applicationContext.getString(networkErrorResource(error)),
                error,
            )
        }
    }

    /** Streams authenticated artifacts to a caller-owned private temporary file. */
    suspend fun download(path: String, target: java.io.File, query: Map<String, String?> = emptyMap(),
        body: JsonElement? = null, retryCsrf: Boolean = true): DownloadInfo {
        val url = (normalizedBaseUrl + path.trimStart('/')).toHttpUrl().newBuilder()
        query.forEach { (key, value) -> if (value != null) url.addQueryParameter(key, value) }
        val builder = Request.Builder().url(url.build()).header("Accept", "*/*")
            .header("Accept-Language", Locale.getDefault().language)
        if (body == null) builder.get() else builder.header("X-CSRFToken", csrfToken()).header("Origin", apiOrigin)
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
        try {
            return perform(builder.build(), transfer = true) { response ->
                if (!response.isSuccessful) {
                    val envelope = runCatching { json.decodeFromString(ApiEnvelope.serializer(JsonElement.serializer()),
                        response.body?.string().orEmpty()) }.getOrNull()
                    if(response.code==401) expireSession((safeValidationDetails(envelope?.details)?.get("reason") as? kotlinx.serialization.json.JsonPrimitive)?.content)
                    if(response.code==403) _profileRefreshRequests.tryEmit(true)
                    throw ApiException(response.code, envelope?.message?.takeIf { it.isNotBlank() }
                        ?: applicationContext.getString(errorResource(response.code)), details=envelope?.details)
                }
                val responseBody = response.body ?: throw IOException("Empty response")
                responseBody.byteStream().use { input -> target.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                    }
                } }
                val disposition = response.header("Content-Disposition").orEmpty()
                val encoded = Regex("filename\\*=UTF-8''([^;]+)", RegexOption.IGNORE_CASE).find(disposition)?.groupValues?.get(1)
                val ordinary = Regex("filename=\"?([^\";]+)", RegexOption.IGNORE_CASE).find(disposition)?.groupValues?.get(1)
                val name = (encoded?.let { android.net.Uri.decode(it) } ?: ordinary ?: "export")
                    .substringAfterLast('/').substringAfterLast('\\').replace(Regex("[\\p{Cntrl}]"), "_").take(180)
                DownloadInfo(name, responseBody.contentType()?.toString()?.substringBefore(';') ?: "application/octet-stream")
            }
        } catch (error: ApiException) {
            target.delete()
            if (body != null && retryCsrf && error.statusCode == 403 && error.summary.contains("csrf", true)) {
                cachedCsrfToken = null
                return download(path, target, query, body, false)
            }
            throw error
        } catch (error: IOException) {
            target.delete()
            throw ApiException(null, applicationContext.getString(networkErrorResource(error)), error)
        }
    }

    suspend fun get(
        path: String,
        query: Map<String, String?> = emptyMap(),
    ): JsonElement = requestData("GET", path, query = query)

    suspend fun post(
        path: String,
        body: JsonElement = buildJsonObject {},
    ): JsonElement = requestData("POST", path, body = body)

    suspend fun postMultipart(path: String, body: MultipartBody): JsonElement =
        requestData("POST", path, multipartBody=body)

    suspend fun put(
        path: String,
        body: JsonElement,
    ): JsonElement = requestData("PUT", path, body = body)

    suspend fun delete(
        path: String,
        body: JsonElement = buildJsonObject {},
    ): JsonElement = requestData("DELETE", path, body = body)

    fun <T> decode(serializer: DeserializationStrategy<T>, element: JsonElement): T =
        json.decodeFromJsonElement(serializer, element)

    fun <T> encode(serializer: kotlinx.serialization.SerializationStrategy<T>, value: T): JsonElement =
        json.encodeToJsonElement(serializer, value)

    private suspend fun requestData(
        method: String,
        path: String,
        query: Map<String, String?> = emptyMap(),
        body: JsonElement? = null,
        requiresCsrf: Boolean = method != "GET",
        allowCsrfRetry: Boolean = true,
        multipartBody: RequestBody? = null,
    ): JsonElement {
        val urlBuilder = (normalizedBaseUrl + path.trimStart('/')).toHttpUrl().newBuilder()
        query.forEach { (key, value) ->
            if (value != null) urlBuilder.addQueryParameter(key, value)
        }

        val builder = Request.Builder()
            .url(urlBuilder.build())
            .header("Accept", "application/json")
            .header("Accept-Language", Locale.getDefault().language)

        if (requiresCsrf) {
            builder.header("X-CSRFToken", csrfToken())
            // Django checks Origin (or Referer) as well as the token on HTTPS.
            builder.header("Origin", apiOrigin)
        }

        val requestBody = multipartBody ?: body?.toString()?.toRequestBody(JSON_MEDIA_TYPE)
            ?: EMPTY_JSON_BODY
        when (method) {
            "GET" -> builder.get()
            "POST" -> builder.post(requestBody)
            "PUT" -> builder.put(requestBody)
            "DELETE" -> builder.delete(requestBody)
            else -> error("Unsupported HTTP method: $method")
        }

        return try {
            execute(builder.build(), expireOnUnauthorized = path.trimStart('/') != ApiEndpoints.Auth.LOGIN)
                .data ?: JsonNull
        } catch (error: ApiException) {
            val isCsrfFailure = error.statusCode == 403 &&
                error.summary.contains("csrf", ignoreCase = true)
            if (requiresCsrf && allowCsrfRetry && isCsrfFailure) {
                cachedCsrfToken = null
                requestData(
                    method = method,
                    path = path,
                    query = query,
                    body = body,
                    requiresCsrf = true,
                    allowCsrfRetry = false,
                    multipartBody = multipartBody,
                )
            } else {
                throw error
            }
        }
    }

    private suspend fun csrfToken(): String = cachedCsrfToken ?: csrfMutex.withLock {
        cachedCsrfToken ?: run {
            val data = requestData(
                method = "GET",
                path = ApiEndpoints.Auth.CSRF,
                requiresCsrf = false,
            )
            decode(CsrfPayload.serializer(), data).token.also {
                cachedCsrfToken = it
            }
        }
    }

    private suspend fun execute(
        request: Request,
        expireOnUnauthorized: Boolean,
    ): ApiEnvelope<JsonElement> =
        try {
            perform(request, transfer = request.url.encodedPath.contains("/database/")) { response ->
                val responseBody = response.body?.string().orEmpty()
                // Status is authoritative even if a proxy returns a non-JSON body.
                // Invalid credentials belong on Login, not in the expiry flow
                // that cancels the currently running authentication job.
                val envelope = runCatching {
                    json.decodeFromString(
                        ApiEnvelope.serializer(JsonElement.serializer()),
                        responseBody,
                    )
                }.getOrElse { parsingError ->
                    if(response.code==401 && expireOnUnauthorized) expireSession()
                    throw ApiException(
                        statusCode = response.code,
                        message = applicationContext.getString(
                            if (!response.isSuccessful) errorResource(response.code) else R.string.error_unknown,
                        ),
                        cause = parsingError,
                    )
                }
                if (!response.isSuccessful) {
                    if(response.code==401 && expireOnUnauthorized) expireSession((safeValidationDetails(envelope.details)?.get("reason") as? kotlinx.serialization.json.JsonPrimitive)?.content)
                    if(response.code==403) _profileRefreshRequests.tryEmit(true)
                    throw ApiException(
                        statusCode = response.code,
                        message = envelope.message.ifBlank {
                            applicationContext.getString(errorResource(response.code))
                        },
                        details = envelope.details,
                    )
                }
                if(request.method!="GET") _profileRefreshRequests.tryEmit(false)
                envelope
            }
        } catch (error: ApiException) {
            throw error
        } catch (error: IOException) {
            throw ApiException(
                statusCode = null,
                message = applicationContext.getString(networkErrorResource(error)),
                cause = error,
            )
        }

    /** Cancel the actual HTTP call when its screen/request coroutine is canceled. */
    private suspend fun <T> perform(request: Request, transfer: Boolean = false, read: (Response) -> T): T =
        suspendCancellableCoroutine { continuation ->
            val builder = http.newBuilder().cookieJar(cookieJar.forCurrentSession())
            if (transfer) builder.readTimeout(180, java.util.concurrent.TimeUnit.SECONDS)
                .callTimeout(240, java.util.concurrent.TimeUnit.SECONDS)
            val client = builder.build()
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!continuation.isActive) {
                        response.close()
                        return
                    }
                    // Consume and close the body on OkHttp's worker before handing
                    // back a value; cancellation cannot strand an open response.
                    runCatching { response.use(read) }
                        .onSuccess { continuation.resume(it) }
                        .onFailure { continuation.resumeWithException(it) }
                }
            })
        }

    private fun networkErrorResource(error: IOException): Int =
        if (error is java.io.InterruptedIOException) R.string.error_timeout else R.string.error_network

    private fun errorResource(statusCode: Int): Int = when (statusCode) {
        400 -> R.string.error_bad_request
        401 -> R.string.error_unauthorized
        403 -> R.string.error_forbidden
        404 -> R.string.error_not_found
        409 -> R.string.error_conflict
        in 500..599 -> R.string.error_server
        else -> R.string.error_unknown
    }

    private fun expireSession(reason: String? = null) {
        expireOnlineSession(reason)
        _sessionExpired.tryEmit(if(reason=="ACCOUNT_EXPIRED") reason else "session_expired")
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
        val EMPTY_JSON_BODY = "{}".toRequestBody(JSON_MEDIA_TYPE)
    }
}

class ApiException(
    val statusCode: Int?,
    message: String,
    cause: Throwable? = null,
    details: JsonElement? = null,
) : Exception(message, cause) {
    val summary: String = message
    val details = safeValidationDetails(details)
    val fieldErrors = validationMessages(this.details)
    val reason: String? get() = (this.details?.get("reason") as? kotlinx.serialization.json.JsonPrimitive)?.content
    override val message: String get() = (listOf(summary) + fieldErrors.map {it.second}).filter {it.isNotBlank()}.distinct().joinToString("\n")
}
