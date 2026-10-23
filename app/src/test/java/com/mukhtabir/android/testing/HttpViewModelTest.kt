package com.mukhtabir.android.testing

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.mukhtabir.android.data.remote.ApiClient
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.*
import org.junit.After
import org.junit.Before

/** Real services and ViewModels; only HTTP transport is local and encrypted preferences are shadowed. */
@OptIn(ExperimentalCoroutinesApi::class)
abstract class HttpViewModelTest {
    protected lateinit var server: MockWebServer
    protected lateinit var api: ApiClient
    protected val requests = CopyOnWriteArrayList<RecordedRequest>()
    private val owners = ViewModelStore()
    private val gates = mutableListOf<ResponseGate>()
    protected class ResponseGate {
        private val arrived = CountDownLatch(1)
        private val released = CountDownLatch(1)
        fun awaitRequest(): Boolean = arrived.await(2, TimeUnit.SECONDS)
        fun release() { released.countDown() }
        internal fun block() {
            arrived.countDown()
            check(released.await(5, TimeUnit.SECONDS)) { "Test did not release HTTP response" }
        }
    }
    protected fun holdResponse(response: MockResponse): ResponseGate = ResponseGate().also { gate ->
        gates += gate
        respond = { gate.block(); response }
    }
    @Volatile protected var respond: (RecordedRequest) -> MockResponse = { errorResponse() }
    @Before fun startHttp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return if (request.requestUrl?.encodedPath == "/api/v1/auth/csrf/") envelope("""{"token":"csrf"}""") else respond(request)
            }
        }
        server.start()
        api = ApiClient(ApplicationProvider.getApplicationContext<Context>(), server.url("/api/v1/").toString())
    }
    @After fun stopHttp() {
        gates.forEach { it.release() }
        try { owners.clear() } finally { try { server.shutdown() } finally { Dispatchers.resetMain() } }
    }
    protected fun <T : ViewModel> own(value: T): T = value.also { owners.put("model-${System.identityHashCode(it)}", it) }
    protected fun <T> await(flow: StateFlow<T>, predicate: (T) -> Boolean): T = runBlocking { withTimeout(5000) { flow.first(predicate) } }
    protected fun envelope(data: String) = MockResponse().setHeader("Content-Type", "application/json").setBody("""{"code":200,"data":$data}""")
    protected fun errorResponse(code: Int = 503) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody("""{"code":$code,"message":"Unavailable"}""")
}
