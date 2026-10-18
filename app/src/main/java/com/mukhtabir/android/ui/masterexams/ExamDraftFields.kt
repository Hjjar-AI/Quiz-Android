@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.masterexams

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.questions.QuestionChoiceDraft
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber

internal fun QuestionDto.examDraftForm(): EmbeddedExamDraftForm {
    val rows = choices.map { QuestionChoiceDraft(text = it) }
    return EmbeddedExamDraftForm(question, rows, rows.getOrNull((correctAnswer ?: 0) - 1)?.id,
        explanation.orEmpty(), source.orEmpty(), difficulty, category, case?.key.orEmpty(),
        case?.stem.orEmpty(), caseOrder?.toString().orEmpty())
}

/** The library uses a column; the embedded dialog reuses these groups in keyed lazy items. */
@Composable
internal fun ExamDraftFields(form: EmbeddedExamDraftForm, enabled: Boolean, maxChoices: Int,
    categories: List<CategoryDto>, caseStemLocked: Boolean, change: ((EmbeddedExamDraftForm) -> EmbeddedExamDraftForm) -> Unit,
    validation: DraftValidationUi = DraftValidationUi()) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        ExamDraftQuestionFields(form, enabled, change, validation)
        form.choices.forEachIndexed { index, choice ->
            key(choice.id) { ExamDraftChoiceFields(form, choice, index, enabled, change, validation) }
        }
        ExamDraftDetailsFields(form, enabled, maxChoices, categories, caseStemLocked,
            onCaseKeyChange = { value -> change { it.copy(caseKey = value, caseStem = "", caseOrder = "") } },
            change = change, validation = validation)
    }
}

@Composable
internal fun ExamDraftQuestionFields(form: EmbeddedExamDraftForm, enabled: Boolean,
    change: ((EmbeddedExamDraftForm) -> EmbeddedExamDraftForm) -> Unit, validation: DraftValidationUi = DraftValidationUi()) {
    var preview by rememberSaveable { mutableStateOf(false) }
    AppFilterChip(preview, { preview = !preview }, label = { Text(stringResource(R.string.question_markdown_preview)) })
    if (preview) {
        if (form.caseStem.isNotBlank()) MarkdownText(form.caseStem)
        QuestionTextCard(form.question)
        form.choices.forEachIndexed { index, choice ->
            QuestionChoiceCard {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.choice_number, index + 1))
                    MarkdownText(choice.text)
                    if (choice.id == form.correctId) Text(stringResource(R.string.correct_answer))
                }
            }
        }
        ExplanationCard(form.explanation)
    }
    DraftValidatedField("question", validation, showMessage = false) { error, focus ->
        MarkdownField(form.question, { value -> change { it.copy(question = value) } },
            stringResource(R.string.question_text), enabled, minLines = 3, error = error, fieldFocusRequester = focus)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExamDraftChoiceFields(form: EmbeddedExamDraftForm, choice: QuestionChoiceDraft, index: Int,
    enabled: Boolean, change: ((EmbeddedExamDraftForm) -> EmbeddedExamDraftForm) -> Unit, validation: DraftValidationUi = DraftValidationUi()) {
    Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        DraftValidatedField("choice:${choice.id}", validation, showMessage = false) { error, focus ->
            MarkdownField(choice.text, { value -> change { f -> f.copy(choices = f.choices.map {
                if (it.id == choice.id) it.copy(text = value) else it }) } },
                stringResource(R.string.choice_number, index + 1), enabled, error = error, fieldFocusRequester = focus)
        }
        DraftValidatedField(if (index == 0) "correct" else "correct:${choice.id}", validation) { _, focus ->
            AppFilterChip(choice.id == form.correctId, { change { it.copy(correctId = choice.id) } },
                modifier = Modifier.focusRequester(focus), enabled = enabled,
                label = { Text(stringResource(R.string.correct_answer)) })
        }
        AppActionRow {
            AppTextButton({ change { it.moveChoice(choice.id, -1) } }, enabled = enabled && index > 0) {
                Text(stringResource(R.string.move_choice_up))
            }
            AppTextButton({ change { it.moveChoice(choice.id, 1) } }, enabled = enabled && index < form.choices.lastIndex) {
                Text(stringResource(R.string.move_choice_down))
            }
            AppDangerTextButton({ change { it.copy(choices = it.choices.filterNot { row -> row.id == choice.id },
                correctId = it.correctId.takeUnless { selected -> selected == choice.id }) } },
                enabled = enabled && form.choices.size > 2) { Text(stringResource(R.string.remove_choice)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExamDraftDetailsFields(form: EmbeddedExamDraftForm, enabled: Boolean, maxChoices: Int,
    categories: List<CategoryDto>, caseStemLocked: Boolean, onCaseKeyChange: (String) -> Unit,
    categoryEnabled: Boolean = enabled, revealError: Boolean = false, categoryStatus: @Composable () -> Unit = {},
    change: ((EmbeddedExamDraftForm) -> EmbeddedExamDraftForm) -> Unit, validation: DraftValidationUi = DraftValidationUi()) {
    var menu by remember { mutableStateOf(false) }
    DraftValidatedField("choices", validation) { _, focus ->
        Text(stringResource(R.string.maximum_choices, maxChoices.coerceAtLeast(2)), Modifier.focusRequester(focus).focusable())
        AppOutlinedButton({ change { if (it.choices.size < maxChoices.coerceAtLeast(2))
            it.copy(choices = it.choices + QuestionChoiceDraft()) else it } },
            enabled = enabled && form.choices.size < maxChoices.coerceAtLeast(2)) { Text(stringResource(R.string.add_choice)) }
    }
    MarkdownField(form.explanation, { value -> change { it.copy(explanation = value) } },
        stringResource(R.string.explanation), enabled, minLines = 2)
    AppSection(stringResource(R.string.section_organization), revealError = revealError || validation.hasAny("source", "difficulty", "category"), revealRequest = validation.attempt) {
        DraftValidatedField("source", validation) { error, focus ->
            OutlinedTextField(form.source, { value -> change { it.copy(source = value) } }, enabled = enabled,
                isError = error != null, label = { Text(stringResource(R.string.question_source)) },
                modifier = Modifier.fillMaxWidth().focusRequester(focus))
        }
        DraftValidatedField("difficulty", validation) { _, focus ->
            AppActionRow {
                listOf("easy" to R.string.difficulty_easy, "medium" to R.string.difficulty_medium,
                    "hard" to R.string.difficulty_hard).forEach { (value, label) ->
                    AppFilterChip(form.difficulty == value, { change { it.copy(difficulty = value) } },
                        enabled = enabled, modifier = if (value == "easy") Modifier.focusRequester(focus) else Modifier,
                        label = { Text(stringResource(label)) })
                }
            }
        }
        categoryStatus()
        DraftValidatedField("category", validation) { _, focus ->
            Box {
                AppOutlinedButton({ menu = true }, modifier = Modifier.focusRequester(focus), enabled = enabled && categoryEnabled) {
                    Text(stringResource(R.string.nav_categories) + ": " + (categories.firstOrNull { it.id == form.category }?.name
                        ?: form.category?.let(::formatNumber) ?: stringResource(R.string.none)))
                }
                DropdownMenu(menu && enabled && categoryEnabled, { menu = false },
                    modifier = Modifier.heightIn(max = AppLayoutTokens.menuMaxHeight)) {
                    DropdownMenuItem({ Text(stringResource(R.string.none)) }, { change { it.copy(category = null) }; menu = false },
                        modifier = Modifier.semantics { selected = form.category == null })
                    categories.forEach { row ->
                        DropdownMenuItem({ Text(row.name) }, { change { it.copy(category = row.id) }; menu = false },
                            modifier = Modifier.semantics { selected = form.category == row.id })
                    }
                }
            }
        }
    }
    AppSection(stringResource(R.string.section_case_context), revealError = validation.hasAny("caseKey", "caseStem", "caseOrder"), revealRequest = validation.attempt) {
        Text(stringResource(R.string.editor_case_stem_hint))
        DraftValidatedField("caseKey", validation) { error, focus ->
            OutlinedTextField(form.caseKey, onCaseKeyChange, enabled = enabled, isError = error != null,
                label = { Text(stringResource(R.string.editor_case_key)) }, modifier = Modifier.fillMaxWidth().focusRequester(focus))
        }
        DraftValidatedField("caseStem", validation, showMessage = false) { error, focus ->
            MarkdownField(form.caseStem, { value -> change { it.copy(caseStem = value) } },
                stringResource(R.string.editor_case_stem), enabled && !caseStemLocked, minLines = 2,
                error = error, fieldFocusRequester = focus)
        }
        DraftValidatedField("caseOrder", validation) { error, focus ->
            OutlinedTextField(form.caseOrder, { value -> change { it.copy(caseOrder = value) } }, enabled = enabled, isError = error != null,
                label = { Text(stringResource(R.string.editor_case_order)) }, modifier = Modifier.fillMaxWidth().focusRequester(focus))
        }
    }
}
