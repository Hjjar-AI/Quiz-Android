package com.mukhtabir.android.ui.offline

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.mukhtabir.android.core.ServerSettings
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.testing.OfflineFileBuilderShadow
import com.mukhtabir.android.testing.OfflineFileShadow
import java.io.File
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import okhttp3.mockwebserver.Dispatcher
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
import org.robolectric.shadows.ShadowStatFs

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, shadows = [TransportMasterKeysShadow::class,
    TransportPreferencesShadow::class, OfflineFileBuilderShadow::class, OfflineFileShadow::class])
class OfflineStudyFlowTest {
    private lateinit var context: Context
    private lateinit var server: MockWebServer
    private lateinit var store: SessionDraftStore
    private lateinit var service: OfflineStudyService
    private val owners = ViewModelStore()
    private var modelId = 0
    private val uploads = CopyOnWriteArrayList<JsonObject>()
    private var reply: ((JsonObject) -> MockResponse)? = null
    private val packId = "11111111-1111-4111-8111-111111111111"

    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        OfflineFileShadow.rejectWrites = false
        context = ApplicationProvider.getApplicationContext()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse = when {
                request.path == "/api/v1/auth/me/" -> envelope("""{"id":41,"username":"sample","capabilities":["tests.start"]}""")
                request.path == "/api/v1/auth/csrf/" -> envelope("""{"token":"sample-token"}""")
                request.path == "/api/v1/exam/offline/completions/" -> {
                    val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
                    uploads += body
                    reply?.invoke(body) ?: accepted(body)
                }
                else -> MockResponse().setResponseCode(404)
            }
        }
        server.start()
        ServerSettings(context).save(server.url("/api/v1/").toString()).getOrThrow()
        store = SessionDraftStore(context)
        store.bind(41, "sample-owner").getOrThrow()
        val directory = File(context.noBackupFilesDir, "offline-${ServerSettings(context).profile()}")
        directory.mkdirs()
        ShadowStatFs.registerStats(directory.path, 100_000, 100_000, 100_000)
        service = OfflineStudyService(ApiClient(context, server.url("/api/v1/").toString()))
        store.writeOffline(41, false, listOf(samplePack().newMaterial()), null).getOrThrow()
    }
    @After fun tearDown() {
        owners.clear()
        OfflineFileShadow.rejectWrites = false
        server.shutdown()
        Dispatchers.resetMain()
    }
    private fun envelope(data: String) = MockResponse().setHeader("Content-Type", "application/json")
        .setBody("""{"code":200,"data":$data}""")
    private fun accepted(body: JsonObject) = envelope("""{"correct_count":1,"total_questions":2,"accuracy":50.0,"total_time":0,"answered_count":2,"source_session_id":${body.getValue("completion_id")},"history_id":44,"mode":"study"}""")
    private fun samplePack() = OfflinePack(packId, "signed-sample-token", listOf(
        QuestionDto(13, "First", listOf("A", "B"), correctAnswer = 1),
        QuestionDto(14, "Second", listOf("C", "D"), correctAnswer = 2)), issuedAt = Instant.now().toString(), imagesIncluded = false)
    private fun model(localOnly: Boolean = false, canSync: Boolean = true): OfflineStudyViewModel {
        val value = OfflineStudyViewModel(service, store, 41, localOnly, canSync, 200, "")
        owners.put("model-${modelId++}", value)
        settle(value)
        return value
    }
    private fun settle(vm: OfflineStudyViewModel): OfflineStudyState = runBlocking {
        withTimeout(5_000) { vm.state.first { !it.busy && it.syncingIds.isEmpty() } }
    }
    private fun open(vm: OfflineStudyViewModel) { vm.open(packId); settle(vm) }
    private fun complete(vm: OfflineStudyViewModel): OfflineMaterial {
        open(vm); vm.start(); settle(vm)
        vm.answer(1, 3); settle(vm)
        vm.go(1); settle(vm)
        vm.answer(1, 2); settle(vm)
        vm.finish(); settle(vm)
        return requireNotNull(vm.state.value.current)
    }
    private fun persisted(localOnly: Boolean = false) = store.readOfflinePack(41, localOnly, packId).getOrThrow()

    @Test fun `local practice finishes and restores without any HTTP connection`() {
        val vm = model(localOnly = true)
        val finished = complete(vm)
        assertNotNull(finished.completionId)
        assertNotNull(finished.frozenTime)
        assertNotNull(finished.completedAt)
        assertNull(finished.result)
        assertEquals(2, finished.answers.size)
        assertEquals(0, server.requestCount)
        val reopened = model(localOnly = true)
        open(reopened)
        assertEquals(finished, reopened.state.value.current)
        assertEquals(0, server.requestCount)
    }
    @Test fun `unanswered completion and repeated answer cannot change a frozen body`() {
        val vm = model(localOnly = true)
        open(vm); vm.start(); settle(vm)
        vm.finish(); settle(vm)
        assertNull(vm.state.value.current?.frozenTime)
        vm.answer(1, 3); settle(vm)
        val answer = vm.state.value.current?.answers
        vm.answer(2, 1); settle(vm)
        assertEquals(answer, vm.state.value.current?.answers)
        vm.go(1); settle(vm); vm.answer(1, 2); settle(vm); vm.finish(); settle(vm)
        val frozen = persisted(true)
        vm.answer(2, 3); vm.finish(); settle(vm)
        assertEquals(frozen, persisted(true))
    }
    @Test fun `completion uploads only after explicit sync with frozen identity timestamps and answers`() {
        val vm = model()
        val frozen = complete(vm)
        assertTrue(uploads.isEmpty())
        vm.sync(); settle(vm)
        val body = uploads.single()
        assertEquals(frozen.completionId, body.getValue("completion_id").jsonPrimitive.content)
        assertEquals(frozen.frozenTime, body.getValue("time_spent").jsonPrimitive.int)
        assertEquals(frozen.completedAt, body.getValue("occurred_at").jsonPrimitive.content)
        assertEquals(listOf(13L, 14L), body.getValue("answers").jsonArray.map { it.jsonObject.getValue("question_id").jsonPrimitive.long })
        assertEquals(frozen.answers.map { it.answeredAt }, body.getValue("answers").jsonArray.map { it.jsonObject.getValue("answered_at").jsonPrimitive.content })
        assertEquals(44L, persisted().result?.historyId)
        vm.sync(); settle(vm)
        assertEquals(1, uploads.size)
    }
    @Test fun `lost upload response preserves frozen data and only explicit retry sends the same body`() {
        val vm = model()
        val frozen = complete(vm)
        reply = { MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST) }
        vm.sync(); settle(vm)
        assertEquals(1, uploads.size)
        assertTrue(packId in vm.state.value.syncErrors)
        assertEquals(frozen, persisted())
        val reopened = model()
        open(reopened)
        assertEquals(1, uploads.size)
        assertEquals(frozen, reopened.state.value.current)
        reply = null
        reopened.sync(); settle(reopened)
        assertEquals(2, uploads.size)
        assertEquals(uploads[0], uploads[1])
        assertNotNull(persisted().result)
    }
    @Test fun `mismatched acknowledgement never marks completion synced`() {
        val vm = model()
        val frozen = complete(vm)
        reply = { accepted(it.toMutableMap().apply { put("completion_id", JsonPrimitive("different-completion")) }.let(::JsonObject)) }
        vm.sync(); settle(vm)
        assertTrue(packId in vm.state.value.syncErrors)
        assertNull(persisted().result)
        assertEquals(frozen.completionId, persisted().completionId)
        assertEquals(1, uploads.size)
    }
    @Test fun `local-only and revoked sync capability cannot upload`() {
        val local = model(localOnly = true)
        complete(local); local.sync(); settle(local)
        val forbidden = model(canSync = false)
        open(forbidden); forbidden.sync(); settle(forbidden)
        assertTrue(uploads.isEmpty())
    }
    @Test fun `storage failure retains answer draft and retries storage without an upload`() {
        val vm = model(localOnly = true)
        open(vm); vm.start(); settle(vm)
        OfflineFileShadow.rejectWrites = true
        vm.answer(1, 3); settle(vm)
        assertTrue(vm.state.value.storageFailed)
        assertTrue(vm.state.value.pendingStorage)
        assertEquals(1, vm.state.value.current?.answers?.size)
        assertTrue(persisted(true).answers.isEmpty())
        OfflineFileShadow.rejectWrites = false
        vm.retryStorage(); settle(vm)
        assertFalse(vm.state.value.storageFailed)
        assertEquals(1, persisted(true).answers.size)
        assertTrue(uploads.isEmpty())
    }
    @Test fun `successful upload followed by storage failure retries only persistence`() {
        val vm = model()
        complete(vm)
        OfflineFileShadow.rejectWrites = true
        vm.sync(); settle(vm)
        assertEquals(1, uploads.size)
        assertTrue(vm.state.value.pendingStorage)
        assertNotNull(vm.state.value.current?.result)
        assertNull(persisted().result)
        OfflineFileShadow.rejectWrites = false
        vm.retryStorage(); settle(vm)
        assertNotNull(persisted().result)
        assertEquals(1, uploads.size)
    }
    @Test fun `a changed account cannot read or upload the previous owner pack`() {
        val vm = model(localOnly = true)
        complete(vm)
        store.bind(42, "different-owner").getOrThrow()
        assertTrue(store.readOfflinePack(41, true, packId).isFailure)
        assertTrue(store.readOffline(42, false).getOrThrow().isEmpty())
        assertTrue(uploads.isEmpty())
    }
    @Test fun `login expiry retains local study but server changes revoke local access`() {
        val original = persisted()
        store.expireOnlineSession()
        assertTrue(store.readOffline(41, false).isFailure)
        assertEquals(original, store.readOfflinePack(41, true, packId).getOrThrow())
        ServerSettings(context).save("https://another.example/api/v1/").getOrThrow()
        assertTrue(store.readOffline(41, true).isFailure)
        assertTrue(uploads.isEmpty())
    }
    @Test fun `corrupted pack fails open instead of becoming a successful empty session`() {
        val directory = File(context.noBackupFilesDir, "offline-${ServerSettings(context).profile()}")
        requireNotNull(directory.listFiles()?.single()).writeText("corrupted fixture")
        val vm = model(localOnly = true)
        vm.open(packId); settle(vm)
        assertNotNull(vm.state.value.error)
        assertNull(vm.state.value.selected)
        assertEquals(1, vm.state.value.materials.size)
        assertTrue(uploads.isEmpty())
    }
}
