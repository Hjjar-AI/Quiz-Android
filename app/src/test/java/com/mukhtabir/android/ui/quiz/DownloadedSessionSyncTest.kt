package com.mukhtabir.android.ui.quiz

import android.app.Application
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.core.DownloadedSession
import com.mukhtabir.android.core.SessionDraft
import com.mukhtabir.android.core.SessionDraftKind
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.data.remote.*
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
import okhttp3.mockwebserver.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class,
    shadows = [TransportMasterKeysShadow::class, TransportPreferencesShadow::class])
class DownloadedSessionSyncTest {
    private lateinit var context: Context
    private lateinit var store: SessionDraftStore
    private lateinit var server: MockWebServer
    private val owners = ViewModelStore()
    private val writes = CopyOnWriteArrayList<JsonObject>()
    @Volatile private var savedAnswer: Int? = null
    @Volatile private var savedConfidence: Int? = null
    private var loseAnswerResponse = false
    private var foreignBundle = false
    private var pauseFails = false
    private var modelId = 0
    private val draft = SessionDraft("session-1", "study", 13, 0, SessionDraftKind.ANSWER, answer = 1, confidence = 3)
    private fun page() = SessionQuestionPageDto(0, 1,
        SessionQuestionDto(13, "Saved question", listOf("A", "B"), correctAnswer = 1),
        savedAnswer = savedAnswer, savedConfidence = savedConfidence, savedIsCorrect = savedAnswer?.let { it == 1 })
    private fun snapshot(queued: Boolean = true, mode: String = "study") = DownloadedSession(
        ExamSessionDto("session-1", mode, listOf(13)), listOf(page()),
        answers = if (queued) listOf(draft.copy(mode = mode)) else emptyList(), timerDisabled = true)
    @Before fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        context = ApplicationProvider.getApplicationContext()
        store = SessionDraftStore(context).also { it.bind(41).getOrThrow() }
        store.writeDownloadedSession(41, snapshot()).getOrThrow()
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty().substringBefore('?')
                return when (path) {
                    "/api/v1/auth/csrf/" -> envelope("""{"token":"csrf"}""")
                    "/api/v1/study/questions/" -> envelope(Json.encodeToString(SessionQuestionBundleDto.serializer(),
                        SessionQuestionBundleDto(if (foreignBundle) "foreign-session" else "session-1", listOf(page()))))
                    "/api/v1/study/question/" -> envelope(Json.encodeToString(SessionQuestionPageDto.serializer(), page()))
                    "/api/v1/study/status/" -> envelope("""{"session_id":"session-1","question_ids":[13],"current_index":0,"is_active":true}""")
                    "/api/v1/study/answer/" -> {
                        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
                        writes += body
                        savedAnswer = body["answer"]?.jsonPrimitive?.intOrNull
                        savedConfidence = body["confidence"]?.jsonPrimitive?.intOrNull
                        if (loseAnswerResponse) MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST)
                        else envelope("""{"new_index":0,"is_correct":true,"explanation":"Saved"}""")
                    }
                    "/api/v1/study/pause/" -> if (pauseFails) MockResponse().setResponseCode(503)
                        .setBody("""{"code":503,"message":"unavailable"}""") else envelope("null")
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
    }
    @After fun tearDown() { owners.clear(); server.shutdown(); Dispatchers.resetMain() }
    private fun envelope(data: String) = MockResponse().setHeader("Content-Type", "application/json")
        .setBody("""{"code":200,"data":$data}""")
    private fun model(): QuizViewModel {
        val api = ApiClient(context, server.url("/api/v1/").toString())
        val value = QuizViewModel(QuestionService(api), TestSessionService(api), BookmarkService(api),
            CategoryService(api), BlueprintService(api), false, 41, SavedStateHandle(), store,
            AppPreferences(context), ImageRepository(api))
        owners.put("quiz-${modelId++}", value)
        settle(value)
        return value
    }
    private fun settle(vm: QuizViewModel): QuizUiState = runBlocking {
        withTimeout(5_000) { vm.state.first { !it.isLoading && !it.syncing } }
    }
    private fun persisted() = requireNotNull(store.readDownloadedSession(41).getOrThrow())

    @Test fun `restoring a downloaded session shows queued work without automatic requests`() {
        val vm = model()
        assertEquals(0, server.requestCount)
        assertEquals(1, vm.state.value.queuedAnswers)
        assertTrue(vm.state.value.syncRequired)
        assertEquals(1, vm.state.value.selectedAnswer)
        assertEquals(listOf(draft), persisted().answers)
    }
    @Test fun `explicit sync confirms a write with owned reads before dropping its draft`() {
        val vm = model()
        vm.syncDownloadedAnswers(); settle(vm)
        val request = writes.single()
        assertEquals("session-1", request.getValue("session_id").jsonPrimitive.content)
        assertEquals(0, request.getValue("expected_index").jsonPrimitive.int)
        assertEquals("same", request.getValue("action").jsonPrimitive.content)
        assertNotNull(request["expected_slot"])
        assertEquals(0, vm.state.value.queuedAnswers)
        assertFalse(vm.state.value.syncRequired)
        assertTrue(persisted().answers.isEmpty())
        assertEquals(1, persisted().pages.single().savedAnswer)
        assertEquals(3, persisted().pages.single().savedConfidence)
    }
    @Test fun `lost answer response retains the queue and retry reconciles without another write`() {
        val vm = model()
        loseAnswerResponse = true
        vm.syncDownloadedAnswers(); settle(vm)
        assertEquals(1, writes.size)
        assertNotNull(vm.state.value.syncError)
        assertEquals(listOf(draft), persisted().answers)
        val reopened = model()
        assertEquals(1, writes.size)
        loseAnswerResponse = false
        reopened.syncDownloadedAnswers(); settle(reopened)
        assertEquals(1, writes.size)
        assertTrue(persisted().answers.isEmpty())
        assertNull(reopened.state.value.syncError)
    }
    @Test fun `changed server slot stops sync for explicit conflict review without a write`() {
        savedAnswer = 2; savedConfidence = 1
        val vm = model()
        vm.syncDownloadedAnswers(); settle(vm)
        assertTrue(writes.isEmpty())
        assertNotNull(vm.state.value.syncConflict)
        assertEquals(2, vm.state.value.syncConflict?.savedAnswer)
        assertEquals(listOf(draft), persisted().answers)
        val requestCount = server.requestCount
        vm.syncDownloadedAnswers(); settle(vm)
        assertEquals(requestCount, server.requestCount)
    }
    @Test fun `foreign bundle identity cannot acknowledge or discard queued answers`() {
        foreignBundle = true
        val vm = model()
        vm.syncDownloadedAnswers(); settle(vm)
        assertNotNull(vm.state.value.syncError)
        assertEquals(listOf(draft), persisted().answers)
        assertTrue(writes.isEmpty())
    }
    @Test fun `failed pause preserves the session and does not invoke exit`() {
        store.writeDownloadedSession(41, snapshot(queued = false)).getOrThrow()
        pauseFails = true
        val vm = model()
        var exits = 0
        vm.pauseAndExit { exits++ }; settle(vm)
        assertEquals(0, exits)
        assertEquals("session-1", vm.state.value.session?.sessionId)
        assertEquals("session-1", persisted().session.sessionId)
        assertTrue(writes.isEmpty())
    }
    @Test fun `local study finish freezes progress without creating a server result`() {
        val vm = model()
        vm.finishStudyLocally()
        assertTrue(vm.state.value.locallyFinished)
        assertTrue(persisted().locallyFinished)
        assertNull(vm.state.value.results)
        assertEquals(0, server.requestCount)
        val restored = model()
        assertTrue(restored.state.value.locallyFinished)
        assertEquals(0, server.requestCount)
    }
    @Test fun `exam cannot claim a local study completion`() {
        store.writeDownloadedSession(41, snapshot(mode = "exam")).getOrThrow()
        val vm = model()
        vm.finishStudyLocally()
        assertFalse(vm.state.value.locallyFinished)
        assertFalse(persisted().locallyFinished)
        assertEquals(0, server.requestCount)
    }
}
