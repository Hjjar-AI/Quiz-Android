@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.masterexams

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.components.AppTextField
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
        QuestionDraftPreview(form.question, form.choices.map { it.text },
            form.choices.indexOfFirst { it.id == form.correctId } + 1, form.explanation,
            com.mukhtabir.android.data.remote.ClinicalCaseContextDto(key = form.caseKey, stem = form.caseStem))
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
    val editText: (String) -> Unit = { value -> change { f -> f.copy(choices = f.choices.map {
        if (it.id == choice.id) it.copy(text = value) else it }) } }
    val selectCorrect = { change { it.copy(correctId = choice.id) } }
    AuthoringChoiceEditor(index + 1, choice.text, choice.id == form.correctId, enabled,
        onTextChange = editText, onSelectCorrect = selectCorrect,
        onMoveUp = { change { it.moveChoice(choice.id, -1) } },
        onMoveDown = { change { it.moveChoice(choice.id, 1) } },
        onRemove = { change { it.copy(choices = it.choices.filterNot { row -> row.id == choice.id },
            correctId = it.correctId.takeUnless { selected -> selected == choice.id }) } },
        canMoveUp = index > 0, canMoveDown = index < form.choices.lastIndex, canRemove = form.choices.size > 2,
        textField = {
            DraftValidatedField("choice:${choice.id}", validation, showMessage = false) { error, focus ->
                AuthoringChoiceTextField(index + 1, choice.text, editText, enabled, error, focus)
            }
        }, correctControl = {
            DraftValidatedField(if (index == 0) "correct" else "correct:${choice.id}", validation) { _, focus ->
                CorrectChoiceControl(choice.id == form.correctId, selectCorrect, enabled, Modifier.focusRequester(focus))
            }
        })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ExamDraftDetailsFields(form: EmbeddedExamDraftForm, enabled: Boolean, maxChoices: Int,
    categories: List<CategoryDto>, caseStemLocked: Boolean, onCaseKeyChange: (String) -> Unit,
    categoryEnabled: Boolean = enabled, revealError: Boolean = false, categoryStatus: @Composable () -> Unit = {},
    change: ((EmbeddedExamDraftForm) -> EmbeddedExamDraftForm) -> Unit, validation: DraftValidationUi = DraftValidationUi()) {
    var menu by remember { mutableStateOf(false) }
    DraftValidatedField("choices", validation) { _, focus ->
        Text(stringResource(R.string.maximum_choices, com.mukhtabir.android.util.formatNumber(maxChoices.coerceAtLeast(2))), Modifier.focusRequester(focus).focusable())
        AppOutlinedButton({ change { if (it.choices.size < maxChoices.coerceAtLeast(2))
            it.copy(choices = it.choices + QuestionChoiceDraft()) else it } },
            enabled = enabled && form.choices.size < maxChoices.coerceAtLeast(2)) { Text(stringResource(R.string.add_choice)) }
    }
    DraftValidatedField("explanation", validation, showMessage = false) { error, focus ->
        MarkdownField(form.explanation, { value -> change { it.copy(explanation = value) } },
            stringResource(R.string.explanation), enabled, minLines = 2, error = error, fieldFocusRequester = focus)
    }
    AppSection(stringResource(R.string.section_organization), revealError = revealError || validation.hasAny("source", "difficulty", "category"), revealRequest = validation.attempt) {
        DraftValidatedField("source", validation) { error, focus ->
            AppTextField(form.source, { value -> change { it.copy(source = value) } }, enabled = enabled,
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
            AppTextField(form.caseKey, onCaseKeyChange, enabled = enabled, isError = error != null,
                label = { Text(stringResource(R.string.editor_case_key)) }, modifier = Modifier.fillMaxWidth().focusRequester(focus))
        }
        DraftValidatedField("caseStem", validation, showMessage = false) { error, focus ->
            MarkdownField(form.caseStem, { value -> change { it.copy(caseStem = value) } },
                stringResource(R.string.editor_case_stem), enabled && !caseStemLocked, minLines = 2,
                error = error, fieldFocusRequester = focus)
        }
        DraftValidatedField("caseOrder", validation) { error, focus ->
            AppTextField(form.caseOrder, { value -> change { it.copy(caseOrder = value) } }, enabled = enabled, isError = error != null,
                label = { Text(stringResource(R.string.editor_case_order)) }, modifier = Modifier.fillMaxWidth().focusRequester(focus))
        }
    }
}
