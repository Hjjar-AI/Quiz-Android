package com.mukhtabir.android.ui.quiz

import android.app.Application
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mukhtabir.android.testing.ContractTestTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import com.mukhtabir.android.R
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.core.DownloadedSession
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.testing.answerChoices
import com.mukhtabir.android.testing.readingViews
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "en-rUS-w480dp-h1000dp",
    shadows = [TransportMasterKeysShadow::class, TransportPreferencesShadow::class])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class QuizQuestionInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val owners = ViewModelStore()
    private var server: MockWebServer? = null
    private lateinit var vm: QuizViewModel
    @After fun tearDown() { compose.runOnIdle { owners.clear() }; server?.shutdown() }
    private fun launch(mode: TestMode, selected: Int? = null, confidence: Int? = null) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SessionDraftStore(context).also { it.bind(41).getOrThrow() }
        val page = SessionQuestionPageDto(0, 1, SessionQuestionDto(13, "Question body sample", listOf("First choice", "Second choice"),
            correctAnswer = 1, explanation = "Explanation body sample", case = ClinicalCaseContextDto(stem = "Case context sample")),
            savedAnswer = selected, savedConfidence = confidence)
        store.writeDownloadedSession(41, DownloadedSession(ExamSessionDto("reader-session", mode.path, listOf(13)), listOf(page), timerDisabled = true)).getOrThrow()
        val backend = MockWebServer().apply {
            dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
                override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest) = MockResponse()
                    .setHeader("Content-Type", "application/json").setBody("""{"code":200,"data":{"bookmarked":false}}""")
            }
            start()
        }
        server = backend
        val api = ApiClient(context, backend.url("/api/v1/").toString())
        val images = ImageRepository(api)
        vm = QuizViewModel(QuestionService(api), TestSessionService(api), BookmarkService(api), CategoryService(api), BlueprintService(api),
            false, 41, SavedStateHandle(), store, AppPreferences(context), images)
        owners.put("quiz", vm)
        compose.setContent { ContractTestTheme {
            val state by vm.state.collectAsState()
            Column(Modifier.verticalScroll(rememberScrollState())) { QuizQuestionContent(state, images, vm) }
        } }
    }
    @Test fun `production reader lays out case before question and retains minimum answer targets`() {
        launch(TestMode.STUDY)
        compose.onAllNodes(answerChoices).onFirst().assertHeightIsAtLeast(48.dp)
        compose.runOnIdle {
            val views = readingViews(compose.activity.window.decorView)
            val case = views.single { it.text.toString() == "Case context sample" }
            val question = views.single { it.text.toString() == "Question body sample" }
            val caseLocation = IntArray(2); val questionLocation = IntArray(2)
            case.getLocationOnScreen(caseLocation); question.getLocationOnScreen(questionLocation)
            assertTrue(caseLocation[1] < questionLocation[1])
        }
    }
    @Test fun `production study reader saves an answer and disables further selection after feedback`() {
        launch(TestMode.STUDY)
        compose.onAllNodes(answerChoices).onFirst().performScrollTo().performClick()
        compose.onAllNodes(answerChoices).onFirst().assertIsNotEnabled()
        compose.runOnIdle {
            assertEquals(1, vm.state.value.selectedAnswer)
            assertEquals(1, vm.state.value.queuedAnswers)
            assertTrue(vm.state.value.showFeedback)
        }
        compose.onAllNodesWithText(compose.activity.getString(R.string.explanation)).onFirst().assertExists()
    }
    @Test fun `production exam reader permits edits and keeps explanation hidden`() {
        launch(TestMode.EXAM)
        compose.onAllNodes(answerChoices)[0].performScrollTo().performClick()
        compose.onAllNodes(answerChoices)[1].performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(2, vm.state.value.selectedAnswer)
            assertFalse(vm.state.value.showFeedback)
            assertFalse(readingViews(compose.activity.window.decorView).any { it.text.toString() == "Explanation body sample" })
        }
        compose.onNodeWithText(compose.activity.getString(R.string.answer_status_correct)).assertDoesNotExist()
    }
    @Test fun `restored confirmed study answer renders feedback and remains locked`() {
        launch(TestMode.STUDY, selected = 1, confidence = 3)
        compose.onAllNodes(answerChoices).onFirst().assertIsSelected()
        compose.onAllNodes(answerChoices).onFirst().assertIsNotEnabled()
        compose.onAllNodesWithText(compose.activity.getString(R.string.explanation)).onFirst().assertExists()
    }
}
