package com.mukhtabir.android.ui.demo

import android.app.Application
import androidx.activity.ComponentActivity
import com.mukhtabir.android.testing.ContractTestTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mukhtabir.android.R
import com.mukhtabir.android.testing.singleDisplayed
import com.mukhtabir.android.testing.answerChoices
import com.mukhtabir.android.testing.readingViews
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
        println("Demo action: ${compose.activity.resources.getResourceEntryName(id)}")
        compose.onAllNodes(hasText(text(id)) and hasClickAction()).singleDisplayed().performClick()
    }
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
        click(R.string.finish)
        click(R.string.finish_session_action)
        click(R.string.demo_return)
        click(R.string.quizzes)
        compose.onNodeWithContentDescription(text(R.string.demo_menu)).performClick()
        click(R.string.history)
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
}
