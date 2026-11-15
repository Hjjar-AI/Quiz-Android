package com.mukhtabir.android.ui.components

import android.app.Application
import android.app.DatePickerDialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ClinicalCaseContextDto
import com.mukhtabir.android.testing.ContractTestTheme
import com.mukhtabir.android.testing.readingViews
import com.mukhtabir.android.ui.masterexams.EmbeddedExamDraftForm
import com.mukhtabir.android.ui.masterexams.ExamDraftChoiceFields
import com.mukhtabir.android.ui.questions.QuestionChoiceDraft
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class, qualifiers = "en-rUS")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@LooperMode(LooperMode.Mode.PAUSED)
class SharedComponentInteractionTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private fun text(id: Int) = compose.activity.getString(id)
    private val radio = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton)

    @Test fun `exam choice edit and reorder retain the selected choice identity`() {
        var form by mutableStateOf(EmbeddedExamDraftForm(choices = listOf(
            QuestionChoiceDraft("a", "First"), QuestionChoiceDraft("b", "Second"), QuestionChoiceDraft("c", "Third")), correctId = "a"))
        compose.setContent { ContractTestTheme {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                form.choices.forEachIndexed { index, choice -> key(choice.id) {
                    Column(Modifier.testTag("choice-${choice.id}")) {
                        ExamDraftChoiceFields(form, choice, index, true, change = { form = it(form) })
                    }
                } }
            }
        } }
        val second = hasAnyAncestor(hasTestTag("choice-b"))
        compose.onNode(second and hasSetTextAction()).performScrollTo().performTextReplacement("Edited second")
        compose.onNode(second and hasText(text(R.string.correct_answer)) and hasClickAction()).performScrollTo().performClick()
        compose.onNode(second and hasText(text(R.string.move_choice_up))).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals(listOf("b", "a", "c"), form.choices.map { it.id })
            assertEquals("b", form.correctId)
            assertEquals("Edited second", form.choices.first().text)
        }
        compose.onNode(hasAnyAncestor(hasTestTag("choice-b")) and hasText(text(R.string.remove_choice)))
            .performScrollTo().performClick()
        compose.runOnIdle { assertEquals(2, form.choices.size); assertNull(form.correctId) }
        compose.onAllNodesWithText(text(R.string.remove_choice)).onFirst().assertIsNotEnabled()
    }

    @Test fun `disabled authoring controls do not dispatch mutations`() {
        var calls = 0
        compose.setContent { ContractTestTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            AuthoringChoiceEditor(1, "Retained choice", true, false,
                onTextChange = { calls++ }, onSelectCorrect = { calls++ }, onMoveUp = { calls++ },
                onMoveDown = { calls++ }, onRemove = { calls++ },
                canMoveUp = true, canMoveDown = true, canRemove = true)
        } } }
        compose.onNode(hasSetTextAction()).assertIsNotEnabled()
        listOf(R.string.correct_answer, R.string.move_choice_up, R.string.move_choice_down, R.string.remove_choice).forEach {
            compose.onNodeWithText(text(it)).performScrollTo().assertIsNotEnabled().performTouchInput { click() }
        }
        compose.runOnIdle { assertEquals(0, calls) }
    }

    @Test fun `exact tags retain commas and input when an add is rejected`() {
        var input by mutableStateOf("Kidney, acute")
        var validation by mutableStateOf<String?>(null)
        var submitted: String? = null
        var removed: String? = null
        compose.setContent { ContractTestTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            ExactTagEditor(listOf("Existing, exact"), input, { input = it },
                onAdd = { submitted = it; validation = "Rejected add" }, onRemove = { removed = it },
                enabled = true, validation = validation)
        } } }
        compose.onNodeWithText(text(R.string.editor_tag_add)).performScrollTo().performClick()
        compose.onNode(hasSetTextAction()).assertTextContains("Kidney, acute")
        compose.onNodeWithText("Rejected add").assertExists()
        compose.onNodeWithText(compose.activity.getString(R.string.editor_tag_remove, "Existing, exact")).performScrollTo().performClick()
        compose.runOnIdle { assertEquals("Kidney, acute", submitted); assertEquals("Existing, exact", removed) }
    }

    @Test fun `tag clearing requires confirmation and disabled suggestions cannot add`() {
        var clears = 0
        var adds = 0
        compose.setContent { ContractTestTheme { Column(Modifier.verticalScroll(rememberScrollState())) {
            ExactTagEditor(listOf("Exact tag"), "", {}, { adds++ }, {}, enabled = true,
                suggestions = listOf("Unavailable suggestion"), suggestionsEnabled = false, onClear = { clears++ })
        } } }
        compose.onNodeWithText("Unavailable suggestion").performScrollTo().assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithText(text(R.string.editor_tags_clear)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(0, clears); assertEquals(0, adds) }
        compose.onNodeWithText(text(R.string.cancel)).performClick()
        compose.runOnIdle { assertEquals(0, clears) }
        compose.onNodeWithText(text(R.string.editor_tags_clear)).performScrollTo().performClick()
        compose.onNodeWithText(text(R.string.confirm)).performClick()
        compose.runOnIdle { assertEquals(1, clears) }
    }

    @Test fun `required confidence remains unselected until an explicit choice and respects locks`() {
        var value by mutableStateOf<Int?>(null)
        var enabled by mutableStateOf(true)
        var selections = 0
        compose.setContent { ContractTestTheme { Column {
            ConfidenceSelector(value, { value = it; selections++ }, enabled, required = true)
        } } }
        compose.onNodeWithText(text(R.string.confidence_required_navigation)).assertExists()
        compose.runOnIdle { assertNull(value); assertEquals(0, selections) }
        compose.onNode(radio and hasText(text(R.string.confidence_certain))).performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(3, value); enabled = false }
        compose.onNode(radio and hasText(text(R.string.confidence_guessing))).assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(3, value); assertEquals(1, selections) }
    }

    @Test fun `rating options show localized values and retain the caller lock`() {
        var rating by mutableStateOf(2)
        var enabled by mutableStateOf(true)
        compose.setContent { ContractTestTheme { Column {
            QuestionRatingSelector(rating, { rating = it }, enabled)
        } } }
        fun label(value: Int) = compose.activity.getString(R.string.library_rating_value, value.toString())
        compose.onNode(radio and hasText(label(4))).performClick().assertIsSelected()
        compose.runOnIdle { assertEquals(4, rating); enabled = false }
        compose.onNode(radio and hasText(label(1))).assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(4, rating) }
    }

    @Test fun `locale selectors keep custom document keys and separate app language callbacks`() {
        var appLanguage: String? = null
        var locale by mutableStateOf("en")
        compose.setContent { ContractTestTheme { Column {
            AppLanguageChoices("en", { appLanguage = it })
            TranslationLocaleChoices(locale, listOf("pt_BR"), { locale = it }, enabled = true)
        } } }
        compose.onNode(radio and hasText("pt_BR")).performClick().assertIsSelected()
        compose.runOnIdle { assertEquals("pt_BR", locale); assertNull(appLanguage) }
        compose.onNode(radio and hasText("العربية")).performClick()
        compose.runOnIdle { assertEquals("ar", appLanguage); assertEquals("pt_BR", locale) }
    }

    @Test fun `recovery prompt keeps restore and discard permissions independent`() {
        var restored = 0
        var discarded = 0
        var visible by mutableStateOf(true)
        compose.setContent { ContractTestTheme {
            if (visible) AuthoringRecoveryPrompt({ restored++ }, { discarded++; visible = false },
                restoreEnabled = false, discardEnabled = true)
        } }
        compose.onNodeWithText(text(R.string.author_restore)).assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithText(text(R.string.ux_discard_local_recovery)).performClick()
        compose.runOnIdle { assertEquals(0, restored); assertEquals(1, discarded) }
        compose.onNodeWithText(text(R.string.author_recover_title)).assertDoesNotExist()
    }

    @Test fun `picker failed read retains query and staged rows until explicit retry`() {
        var query by mutableStateOf("Draft query")
        var error by mutableStateOf<Throwable?>(IllegalStateException("Read failed"))
        var searches = 0
        var applied = 0
        var page by mutableStateOf(2)
        compose.setContent { ContractTestTheme {
            AppSearchPicker("Choose target", {}, query, { query = it }, { searches++ },
                queryEnabled = true, searchEnabled = error == null, loading = false, error = error,
                onRetry = { error = null },
                confirmButton = { AppTextButton({ applied++ }, enabled = error == null) { androidx.compose.material3.Text("Apply targets") } }) {
                item { androidx.compose.material3.Text("Retained selected target") }
                item { AppPagination(page, 3, { page-- }, { page++ },
                    previousEnabled = error == null, nextEnabled = error == null) }
            }
        } }
        compose.onNode(hasSetTextAction()).performTextReplacement("Updated draft")
        compose.onNodeWithText("Apply targets").assertIsNotEnabled()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(text(R.string.next)))
        compose.onNodeWithText(text(R.string.next)).assertIsNotEnabled()
        compose.onNodeWithText("Retained selected target").assertExists()
        compose.runOnIdle { assertEquals(0, searches); assertEquals(0, applied) }
        compose.onNode(hasScrollToIndexAction()).performScrollToIndex(0)
        compose.onNodeWithText(text(R.string.retry)).performClick()
        compose.onNode(hasSetTextAction()).assertTextContains("Updated draft")
        compose.onNodeWithText(text(R.string.next)).performScrollTo().performClick()
        compose.runOnIdle { assertEquals(3, page) }
        compose.onNodeWithText("Apply targets").performClick()
        compose.runOnIdle { assertEquals(1, applied); assertEquals(0, searches) }
    }

    @Test fun `bounded picker scrolls long results while its action stays reachable`() {
        var applied = 0
        compose.setContent { ContractTestTheme {
            AppSearchPicker("Long lookup", {}, "", {}, {}, true, true, false, null, null,
                confirmButton = { AppTextButton({ applied++ }) { androidx.compose.material3.Text("Apply selection") } }) {
                items((1..30).toList()) { value -> AppRadioRow("Target $value", value == 30, {}) }
            }
        } }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Target 30"))
        compose.onNodeWithText("Target 30").assertIsDisplayed()
        compose.onNodeWithText("Apply selection").assertIsDisplayed().performClick()
        compose.runOnIdle { assertEquals(1, applied) }
    }

    @Test fun `export requires a saved URI to share and disables actions while busy`() {
        var uri by mutableStateOf<Uri?>(null)
        var enabled by mutableStateOf(true)
        var saves = 0
        compose.setContent { ContractTestTheme { Column {
            ExportArtifactActions("review.pdf", "application/pdf", uri, enabled, { saves++ }, {})
        } } }
        compose.onNodeWithText(text(R.string.transfer_share)).assertDoesNotExist()
        compose.onNodeWithText(text(R.string.save)).performClick()
        compose.runOnIdle { assertEquals(1, saves); uri = Uri.parse("content://documents/review"); enabled = false }
        compose.onNodeWithText(text(R.string.save)).assertIsNotEnabled().performTouchInput { click() }
        compose.onNodeWithText(text(R.string.transfer_share)).assertIsNotEnabled().performTouchInput { click() }
        compose.runOnIdle { assertEquals(1, saves); assertNull(shadowOf(compose.activity).nextStartedActivity) }
    }

    @Test fun `export chooser grants read access only to the saved document`() {
        val uri = Uri.parse("content://documents/exported-review")
        var failures = 0
        compose.setContent { ContractTestTheme { Column {
            ExportArtifactActions("review.pdf", "application/pdf", uri, true, {}, { failures++ })
        } } }
        compose.onNodeWithText(text(R.string.transfer_share)).performClick()
        compose.runOnIdle {
            assertEquals(0, failures)
            val chooser = shadowOf(compose.activity).nextStartedActivity
            assertEquals(Intent.ACTION_CHOOSER, chooser.action)
            @Suppress("DEPRECATION") val send = chooser.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)!!
            assertEquals(Intent.ACTION_SEND, send.action)
            assertEquals("application/pdf", send.type)
            @Suppress("DEPRECATION") val stream = send.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
            assertEquals(uri, stream)
            assertEquals(uri, send.clipData!!.getItemAt(0).uri)
            assertTrue(send.flags and Intent.FLAG_GRANT_READ_URI_PERMISSION != 0)
            assertEquals(0, send.flags and Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
    }

    @Test fun `busy effect reports changes and clears the current callback on disposal`() {
        var visible by mutableStateOf(true)
        var busy by mutableStateOf(false)
        var owner by mutableStateOf(1)
        val events = mutableListOf<Pair<Int, Boolean>>()
        compose.setContent {
            if (visible) {
                val currentOwner = owner
                ScreenBusyEffect(busy) { events += currentOwner to it }
            }
        }
        compose.runOnIdle { assertEquals(false, events.last().second); busy = true }
        compose.runOnIdle { assertEquals(1 to true, events.last()); owner = 2 }
        compose.runOnIdle { visible = false }
        compose.runOnIdle { assertEquals(2 to false, events.last()) }
    }

    @Test fun `date picker evaluates its initial date at click time and returns the chosen date`() {
        var initial = LocalDate.of(2026, 11, 3)
        var selected: LocalDate? = null
        compose.setContent { ContractTestTheme {
            AppDateField("Review date", { initial }, true, { selected = it })
        } }
        compose.runOnIdle { initial = LocalDate.of(2026, 11, 4) }
        compose.onNodeWithText("Review date").performClick()
        compose.runOnIdle {
            val dialog = ShadowDialog.getLatestDialog() as DatePickerDialog
            assertEquals(4, dialog.datePicker.dayOfMonth)
            dialog.datePicker.updateDate(2026, 10, 9)
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).performClick()
        }
        compose.runOnIdle {
            assertEquals(LocalDate.of(2026, 11, 9), selected)
        }
    }

    @Test fun `authoring preview places case before question and exposes only the supplied key`() {
        compose.setContent { ContractTestTheme {
            Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
                QuestionDraftPreview("Question preview body", listOf("First preview", "Second preview"), 2,
                    "Preview explanation", ClinicalCaseContextDto(stem = "Case preview context"))
            }
        } }
        compose.onNodeWithText(text(R.string.correct_answer)).assertExists()
        compose.runOnIdle {
            val views = readingViews(compose.activity.window.decorView)
            val case = views.single { it.text.toString() == "Case preview context" }
            val question = views.single { it.text.toString() == "Question preview body" }
            val casePosition = IntArray(2); val questionPosition = IntArray(2)
            case.getLocationOnScreen(casePosition); question.getLocationOnScreen(questionPosition)
            assertTrue(casePosition[1] < questionPosition[1])
        }
    }
}
