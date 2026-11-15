package com.mukhtabir.android.ui.demo

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mukhtabir.android.testing.ContractTestTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.testing.singleDisplayed
import com.mukhtabir.android.testing.answerChoices
import com.mukhtabir.android.testing.readingViews
import com.mukhtabir.android.util.formatNumber
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "en-rUS-h1000dp")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class DemoScreenInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun text(id: Int) = compose.activity.getString(id)
    private fun click(id: Int) {
        compose.onAllNodes(hasText(text(id)) and hasClickAction()).singleDisplayed().performClick()
    }
    private fun scrollClick(id: Int) {
        compose.onNode(hasText(text(id)) and hasClickAction()).performScrollTo().performClick()
    }
    private fun openDestination(id: Int) {
        compose.onNodeWithContentDescription(text(R.string.demo_menu)).performClick()
        val drawer = hasScrollAction() and hasAnyDescendant(hasText(text(R.string.demo_learner)))
        compose.onNode(hasText(text(id)) and hasClickAction() and hasAnyAncestor(drawer))
            .performScrollTo().performClick()
    }
    private fun questionCard(id: Int) = compose.onNode(hasText(text(id)) and hasClickAction() and !hasSetTextAction())
    private fun launch() { compose.setContent { ContractTestTheme { DemoScreen {} } } }
    // Use Robolectric's default width for text-field dialogs (upstream #8460).
    // Explicit RTL and wide qualifiers remain on the non-dialog cases below.
    private fun quickStudy() {
        click(R.string.study_now_start)
        click(R.string.flow_start)
    }
    private fun hasNativeText(value: String) = readingViews(compose.activity.window.decorView).any { it.text.toString() == value }

    @Test fun `library search and empty-filter reset operate on the rendered screen`() {
        launch(); click(R.string.nav_library_short)
        compose.onNode(hasSetTextAction()).performTextInput(text(R.string.demo_q2))
        compose.onNode(hasText(text(R.string.demo_q2)) and !hasSetTextAction()).assertExists()
        compose.onNodeWithText(text(R.string.demo_q1)).assertDoesNotExist()
        compose.onNode(hasSetTextAction()).performTextReplacement("no matching fixture")
        compose.onNodeWithText(text(R.string.no_questions_found)).assertExists()
        click(R.string.library_reset)
        compose.onNodeWithText(text(R.string.demo_q1)).assertExists()
    }
    @Test fun `bookmark changes carry from library to bookmarks and remove from that list`() {
        launch(); click(R.string.nav_library_short)
        compose.onNode(hasSetTextAction()).performTextInput(text(R.string.demo_q1))
        click(R.string.demo_add_bookmark)
        click(R.string.bookmarks)
        compose.onNodeWithText(text(R.string.demo_q1)).assertExists()
        compose.onAllNodesWithText(text(R.string.remove_bookmark)).onFirst().performClick()
        compose.onNodeWithText(text(R.string.demo_q1)).assertDoesNotExist()
    }
    @Test fun `study answer feedback is rendered and cannot be changed after confirmation`() {
        val repository = DemoRepository().apply { start(DemoMode.STUDY, listOf(1)) }
        compose.setContent { ContractTestTheme { DemoPracticeScreen(repository) {} } }
        compose.onAllNodes(answerChoices).onFirst().performClick()
        compose.runOnIdle {
            assertEquals(1, repository.state.session.answered)
            assertTrue(hasNativeText(text(R.string.demo_q1_explanation)))
        }
        compose.onAllNodes(answerChoices).onFirst().assertIsNotEnabled()
    }
    @Test fun `exam choice can change without exposing explanation or correctness labels`() {
        val repository = DemoRepository().apply { start(DemoMode.EXAM, listOf(1)) }
        compose.setContent { ContractTestTheme { DemoPracticeScreen(repository) {} } }
        compose.onAllNodes(answerChoices)[0].performClick()
        compose.onAllNodes(answerChoices)[1].performClick()
        compose.runOnIdle {
            assertEquals(1, repository.state.session.answers.single())
            assertFalse(hasNativeText(text(R.string.demo_q1_explanation)))
        }
        compose.onNodeWithText(text(R.string.answer_status_correct)).assertDoesNotExist()
    }
    @Test fun `recall typing precedes reveal and answer controls`() {
        val repository = DemoRepository().apply { start(DemoMode.RECALL, listOf(1)) }
        compose.setContent { ContractTestTheme { DemoPracticeScreen(repository) {} } }
        compose.onAllNodes(answerChoices).assertCountEquals(0)
        compose.onNodeWithText(text(R.string.demo_reveal)).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextInput("   ")
        compose.onNodeWithText(text(R.string.demo_reveal)).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performTextClearance()
        compose.onNode(hasSetTextAction()).performTextInput("Cell")
        click(R.string.demo_reveal)
        compose.onAllNodes(answerChoices).assertCountEquals(4)
        compose.onAllNodes(answerChoices).onFirst().performClick()
        compose.runOnIdle {
            assertEquals("Cell", repository.state.session.recalled.single())
            assertEquals(1, repository.state.session.answered)
        }
    }
    @Test fun `pause and resume keep a selected answer through the real shell`() {
        launch(); quickStudy()
        compose.onAllNodes(answerChoices).onFirst().performClick()
        compose.onNodeWithContentDescription(text(R.string.back)).performClick()
        click(R.string.demo_pause)
        click(R.string.flow_resume)
        compose.onAllNodes(answerChoices).onFirst().assertIsSelected()
    }
    @Test fun `finishing through confirmation adds a reviewable history row`() {
        launch(); quickStudy()
        compose.onAllNodes(answerChoices).onFirst().performClick()
        scrollClick(R.string.confidence_certain)
        click(R.string.finish)
        click(R.string.finish_session_action)
        click(R.string.demo_return)
        click(R.string.quizzes)
        openDestination(R.string.history)
        compose.onAllNodesWithText(text(R.string.demo_review)).assertCountEquals(3)
    }
    @Test fun `saved-state restoration keeps the selected tab and changed bookmarks`() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { ContractTestTheme { DemoScreen {} } }
        click(R.string.nav_library_short)
        compose.onNode(hasSetTextAction()).performTextInput(text(R.string.demo_q1))
        click(R.string.demo_add_bookmark)
        click(R.string.bookmarks)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(text(R.string.demo_q1)).assertExists()
        compose.onAllNodes(hasText(text(R.string.bookmarks)) and isSelected()).singleDisplayed().assertExists()
    }
    @Test
    @Config(qualifiers = "ar-rSA-ldrtl-w480dp-h1000dp")
    fun `Arabic RTL and large text keep library reset reachable`() {
        compose.setContent { ContractTestTheme {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
                LocalDensity provides Density(LocalDensity.current.density, 2f)) { DemoScreen {} }
        } }
        click(R.string.nav_library_short)
        compose.onNode(hasSetTextAction()).performTextInput("no matching fixture")
        compose.onNodeWithText(text(R.string.library_reset)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.demo_q1)).assertExists()
    }
    @Test
    @Config(qualifiers = "en-rUS-w900dp-h1000dp")
    fun `wide screen rail opens the same library and bookmark destinations`() {
        launch(); click(R.string.nav_library_short)
        compose.onNode(hasSetTextAction()).performTextInput(text(R.string.demo_q1))
        click(R.string.demo_add_bookmark)
        click(R.string.bookmarks)
        compose.onNodeWithText(text(R.string.demo_q1)).assertExists()
        compose.onAllNodes(hasText(text(R.string.bookmarks)) and isSelected()).singleDisplayed().assertExists()
    }
    @Test fun `leaving the demo requires confirmation and cancel preserves its screen`() {
        var exits = 0
        compose.setContent { ContractTestTheme { DemoScreen { exits++ } } }
        compose.onAllNodesWithText(text(R.string.demo_exit)).onFirst().performScrollTo().performClick()
        click(R.string.cancel)
        compose.runOnIdle { assertEquals(0, exits) }
        compose.onAllNodesWithText(text(R.string.demo_exit)).onFirst().performScrollTo().performClick()
        compose.onAllNodes(hasText(text(R.string.demo_exit)) and hasClickAction()).onLast().performClick()
        compose.runOnIdle { assertEquals(1, exits) }
    }

    @Test fun `learning next and finish wait for explicit confidence before leaving a confirmed answer`() {
        val repository = DemoRepository().apply { start(DemoMode.STUDY, listOf(1, 2, 3)) }
        compose.setContent { ContractTestTheme { DemoPracticeScreen(repository) {} } }
        compose.onAllNodes(answerChoices).onFirst().performClick()
        compose.onNodeWithText(text(R.string.next)).assertIsNotEnabled()
        compose.onNodeWithText(text(R.string.finish)).assertIsNotEnabled()
        compose.runOnIdle { assertTrue(repository.state.session.confidence.isEmpty()) }
        scrollClick(R.string.confidence_uncertain)
        compose.onNodeWithText(text(R.string.next)).assertIsEnabled().performClick()
        compose.runOnIdle {
            assertEquals(1, repository.state.session.index)
            assertEquals(mapOf(0 to 2), repository.state.session.confidence)
            assertEquals(listOf(0, -1, -1), repository.state.session.answers)
        }
        compose.onNodeWithText(text(R.string.next)).assertIsNotEnabled()
    }

    @Test fun `setup filters an empty pool and intersects category tag and localized count before starting`() {
        val repository = DemoRepository(DemoData(bookmarks = emptySet()))
        var mode by mutableStateOf(TestMode.STUDY)
        compose.setContent { ContractTestTheme {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                DemoPracticeSetup(repository, mode, { mode = it },
                    onStart = { chosen, ids -> repository.start(chosen, ids) }, onDownload = {})
            }
        } }
        scrollClick(R.string.bookmarks)
        compose.onNodeWithText(text(R.string.no_questions_found)).assertExists()
        compose.onNodeWithText(text(R.string.flow_start)).assertIsNotEnabled()
        scrollClick(R.string.quiz_pool_all)
        scrollClick(R.string.demo_category_circulation)
        scrollClick(R.string.demo_tag_case_context)
        compose.onNodeWithText(text(R.string.flow_start)).assertIsNotEnabled()
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("٢")
        scrollClick(R.string.exam)
        scrollClick(R.string.flow_start)
        compose.runOnIdle {
            assertEquals(listOf(2, 4), repository.state.session.questionIds)
            assertEquals(DemoMode.EXAM, repository.state.session.mode)
            assertFalse(repository.state.session.downloaded)
        }
    }

    @Test fun `library page survives detail navigation tab changes and saved-state restoration`() {
        val restoration = StateRestorationTester(compose)
        restoration.setContent { ContractTestTheme { DemoScreen {} } }
        click(R.string.nav_library_short)
        scrollClick(R.string.next)
        compose.onNodeWithText(text(R.string.demo_q1)).assertDoesNotExist()
        questionCard(R.string.demo_q5).performScrollTo().performClick()
        compose.runOnIdle { assertTrue(hasNativeText(text(R.string.demo_q5))) }
        compose.onNodeWithContentDescription(text(R.string.back)).performClick()
        compose.onNodeWithText(text(R.string.demo_q5)).assertExists()
        compose.onNodeWithText(text(R.string.demo_q1)).assertDoesNotExist()
        click(R.string.quizzes)
        click(R.string.nav_library_short)
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText(text(R.string.demo_q5)).assertExists()
        compose.onNodeWithText(text(R.string.demo_q1)).assertDoesNotExist()
    }

    @Test fun `unsaved planner target and categories survive leaving the destination`() {
        launch()
        openDestination(R.string.nav_planner)
        compose.onNode(hasSetTextAction()).performScrollTo().performTextReplacement("٤٢")
        scrollClick(R.string.demo_category_circulation)
        click(R.string.nav_library_short)
        openDestination(R.string.nav_planner)
        compose.onNode(hasSetTextAction()).assertTextContains("42")
        compose.onNodeWithText(text(R.string.demo_category_circulation)).assertIsOn()
        scrollClick(R.string.save)
        compose.onNodeWithText(text(R.string.saved)).assertExists()
    }

    @Test fun `failed sample refresh keeps library query and rows through reconnect and explicit retry`() {
        launch(); click(R.string.nav_library_short)
        compose.onNode(hasSetTextAction()).performTextInput(text(R.string.demo_q2))
        openDestination(R.string.demo_connections)
        scrollClick(R.string.demo_connection_switch)
        scrollClick(R.string.refresh)
        compose.onNodeWithText(text(R.string.demo_read_failed)).assertExists()
        click(R.string.nav_library_short)
        compose.onNode(hasSetTextAction()).assertTextContains(text(R.string.demo_q2))
        questionCard(R.string.demo_q2).assertExists()
        compose.onNodeWithText(text(R.string.flow_practice)).assertIsNotEnabled()
        openDestination(R.string.demo_connections)
        scrollClick(R.string.demo_connection_switch)
        compose.onNodeWithText(text(R.string.demo_read_failed)).assertExists()
        scrollClick(R.string.retry)
        compose.onNodeWithText(text(R.string.demo_read_failed)).assertDoesNotExist()
        click(R.string.nav_library_short)
        compose.onNode(hasSetTextAction()).assertTextContains(text(R.string.demo_q2))
        questionCard(R.string.demo_q2).assertExists()
        compose.onNodeWithText(text(R.string.flow_practice)).assertIsEnabled()
    }

    @Test fun `downloaded study completes offline and enters sample history only after explicit upload`() {
        launch()
        openDestination(R.string.demo_connections)
        scrollClick(R.string.demo_connection_switch)
        openDestination(R.string.offline_title)
        scrollClick(R.string.study)
        compose.onAllNodes(answerChoices).onFirst().performClick()
        scrollClick(R.string.confidence_certain)
        click(R.string.finish)
        click(R.string.finish_session_action)
        compose.onNodeWithText(text(R.string.demo_pending_sync)).assertExists()
        compose.onNodeWithText(text(R.string.offline_sync)).assertIsNotEnabled()
        scrollClick(R.string.demo_return)
        openDestination(R.string.history)
        compose.onAllNodesWithText(text(R.string.demo_review)).assertCountEquals(2)
        openDestination(R.string.demo_connections)
        scrollClick(R.string.demo_connection_switch)
        openDestination(R.string.offline_title)
        scrollClick(R.string.offline_sync)
        compose.onNodeWithText(text(R.string.demo_no_pending)).assertExists()
        compose.onNodeWithText(text(R.string.offline_sync)).assertDoesNotExist()
        openDestination(R.string.history)
        compose.onAllNodesWithText(text(R.string.demo_review)).assertCountEquals(3)
    }

    @Test fun `sample profile renders real profile identity and sign out still requires demo exit confirmation`() {
        var exits = 0
        compose.setContent { ContractTestTheme { DemoScreen { exits++ } } }
        openDestination(R.string.nav_profile)
        compose.onNodeWithText("@sample_learner").assertExists()
        compose.onNodeWithText(text(R.string.profile_details)).assertExists()
        scrollClick(R.string.sign_out)
        compose.onNodeWithText(text(R.string.demo_exit_message)).assertExists()
        click(R.string.cancel)
        compose.runOnIdle { assertEquals(0, exits) }
        compose.onNodeWithText("@sample_learner").assertExists()
    }

    @Test fun `question rating persists across details and is locked while the sample connection is offline`() {
        launch(); click(R.string.nav_library_short)
        compose.onNode(hasSetTextAction()).performTextInput(text(R.string.demo_q1))
        questionCard(R.string.demo_q1).performScrollTo().performClick()
        val rating = compose.activity.getString(R.string.library_rating_value, formatNumber(3))
        compose.onNode(hasText(rating) and hasClickAction()).performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithContentDescription(text(R.string.back)).performClick()
        openDestination(R.string.demo_connections)
        scrollClick(R.string.demo_connection_switch)
        click(R.string.nav_library_short)
        questionCard(R.string.demo_q1).performScrollTo().performClick()
        compose.onNode(hasText(rating) and hasClickAction()).assertIsSelected().assertIsNotEnabled()
    }
}
