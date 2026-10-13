package com.mukhtabir.android.ui.masterexams

import com.mukhtabir.android.ui.components.ExplanationCard

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppDangerTextButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.MasterExamDraftWrite
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownField
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.questions.QuestionChoiceDraft
import com.mukhtabir.android.util.formatNumber
import java.util.Locale

@kotlinx.serialization.Serializable
internal data class EmbeddedExamDraftForm(
    val question: String = "", val choices: List<QuestionChoiceDraft> = List(2) { QuestionChoiceDraft() },
    val correctId: String? = null, val explanation: String = "", val source: String = "",
    val difficulty: String = "medium", val category: Long? = null,
    val caseKey: String = "", val caseStem: String = "", val caseOrder: String = "",
) {
    fun payload(maxChoices: Int): MasterExamDraftWrite? {
        fun length(value: String) = value.codePointCount(0, value.length)
        val text = question.trim()
        val answers = choices.map { it.text.trim() }
        val correct = choices.indexOfFirst { it.id == correctId } + 1
        val order = if (caseOrder.isBlank()) null else caseOrder.trim().map { char ->
            val digit = Character.digit(char, 10)
            if (digit >= 0) ('0'.code + digit).toChar() else char
        }.joinToString("").toIntOrNull()?.takeIf { it > 0 } ?: return null
        if (text.isEmpty() || length(text) > 3000 || answers.size !in 2..maxChoices.coerceAtLeast(2) ||
            answers.any { it.isEmpty() || length(it) > 300 } || correct !in 1..answers.size ||
            answers.map { it.lowercase(Locale.ROOT) }.distinct().size != answers.size ||
            length(source.trim()) > 200 || length(caseKey.trim()) > 64 || length(caseStem.trim()) > 3000 ||
            (category != null && category <= 0) || difficulty !in setOf("easy", "medium", "hard") ||
            (caseKey.isBlank() && (caseStem.isNotBlank() || caseOrder.isNotBlank()))) return null
        return MasterExamDraftWrite(text, answers, correct, explanation.trim(), source.trim(), difficulty, category,
            caseKey.trim().takeIf { it.isNotEmpty() }, caseStem.trim().takeIf { it.isNotEmpty() }, order)
    }

    fun moveChoice(id: String, offset: Int): EmbeddedExamDraftForm {
        val from = choices.indexOfFirst { it.id == id }
        if (from < 0 || from + offset !in choices.indices) return this
        val reordered = choices.toMutableList()
        reordered.add(from + offset, reordered.removeAt(from))
        return copy(choices = reordered)
    }
}

internal data class EmbeddedExamDraftState(
    val form: EmbeddedExamDraftForm = EmbeddedExamDraftForm(), val baseline: EmbeddedExamDraftForm = form,
    val beforeIds: List<Long> = emptyList(),
    val validation: Boolean = false, val error: Throwable? = null,
    val uncertain: Boolean = false, val reconciled: Boolean = false, val createdId: Long? = null,
) { val dirty: Boolean get() = form != baseline }

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
internal fun EmbeddedExamDraftDialog(draft: EmbeddedExamDraftState, s: ExamEditorState, vm: MasterExamEditorViewModel) {
    val form = draft.form
    val enabled = vm.editable(s)
    var preview by rememberSaveable { mutableStateOf(false) }
    var confirmClose by remember { mutableStateOf(false) }
    var categoryMenu by remember { mutableStateOf(false) }
    val canClose = !s.busy && (!(draft.uncertain || draft.createdId != null) || draft.reconciled)
    val close: () -> Unit = {
        if (canClose) {
            if (draft.uncertain || (draft.dirty && draft.createdId == null)) confirmClose = true
            else vm.closeEmbeddedDraft()
        }
    }
    if (confirmClose) AlertDialog(onDismissRequest = { confirmClose = false },
        title = { Text(stringResource(R.string.exam_author_leave_title)) },
        text = { Text(stringResource(if (draft.uncertain) R.string.exam_draft_close_uncertain else R.string.exam_draft_discard)) },
        confirmButton = { AppDangerTextButton({ confirmClose = false; vm.closeEmbeddedDraft() }, enabled = canClose) { Text(stringResource(R.string.discard)) } },
        dismissButton = { TextButton({ confirmClose = false }) { Text(stringResource(R.string.keep_editing)) } })
    AlertDialog(onDismissRequest = close,
        title = { Text(stringResource(R.string.exam_draft_create)) },
        text = {
            LazyColumn(Modifier.heightIn(max = com.mukhtabir.android.ui.components.AppLayoutTokens.pickerMaxHeight), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                item {
                    Text(stringResource(R.string.exam_draft_scope))
                    if (s.busy) AppLoadingIndicator()
                    draft.error?.let { ErrorState(throwable = it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                    s.error?.let { ErrorState(throwable = it, onRetry = if (s.busy) null else vm::refresh) }
                    s.message?.let { Text(stringResource(it), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                    if (s.denied) Text(stringResource(R.string.error_forbidden))
                    if (s.exam?.canEditNow == false) Text(stringResource(R.string.exam_author_frozen))
                    if (draft.validation) Text(stringResource(R.string.exam_draft_invalid), color = MaterialTheme.colorScheme.error)
                    if (draft.uncertain) Text(stringResource(R.string.exam_draft_uncertain))
                    draft.createdId?.let { Text(stringResource(R.string.exam_draft_created_id, formatNumber(it))) }
                    if (draft.uncertain || draft.createdId != null) {
                        OutlinedButton(vm::refresh, enabled = !s.busy) { Text(stringResource(R.string.refresh)) }
                        Text(stringResource(R.string.exam_draft_review))
                        s.questionError?.let { ErrorState(throwable = it, onRetry = if (s.busy) null else vm::retryQuestions) }
                    }
                }
                if (draft.reconciled) items(s.exam?.questionIds.orEmpty() - draft.beforeIds.toSet(), key = { "review-$it" }) { id ->
                    Column {
                        Text(stringResource(R.string.exam_draft_created_id, formatNumber(id)))
                        s.questions[id]?.let { question ->
                            ClinicalCaseContext(question.localizedForDisplay().case)
                            com.mukhtabir.android.ui.components.QuestionTextCard(question.question)
                            question.choices.forEachIndexed { index, text ->
                                com.mukhtabir.android.ui.components.QuestionChoiceCard {
                                    Column(Modifier.weight(1f)) {
                                        Text(stringResource(R.string.choice_number, index + 1))
                                        MarkdownText(text)
                                        if (question.correctAnswer == index + 1) Text(stringResource(R.string.correct_answer))
                                    }
                                }
                            }
                            question.explanation?.takeIf { it.isNotBlank() }?.let { ExplanationCard(it) }
                            Text(stringResource(R.string.question_source) + ": " + question.source.orEmpty())
                            Text(stringResource(R.string.nav_categories) + ": " + question.categoryName.orEmpty())
                        }
                    }
                }
                item {
                    FilterChip(preview, { preview = !preview }, label = { Text(stringResource(R.string.question_markdown_preview)) })
                    if (preview) {
                        if (form.caseStem.isNotBlank()) MarkdownText(form.caseStem)
                        com.mukhtabir.android.ui.components.QuestionTextCard(form.question)
                        form.choices.forEachIndexed { index, choice ->
                            com.mukhtabir.android.ui.components.QuestionChoiceCard {
                                Column(Modifier.weight(1f)) {
                                    Text(stringResource(R.string.choice_number, index + 1))
                                    MarkdownText(choice.text)
                                    if (choice.id == form.correctId) Text(stringResource(R.string.correct_answer))
                                }
                            }
                        }
                        ExplanationCard(form.explanation)
                    }
                    MarkdownField(form.question, { value -> vm.editEmbeddedDraft { it.copy(question = value) } },
                        label = stringResource(R.string.question_text), enabled = enabled, minLines = 3)
                    Text(stringResource(R.string.maximum_choices, vm.maxChoices.coerceAtLeast(2)))
                }
                items(form.choices, key = { it.id }) { choice ->
                    val index = form.choices.indexOf(choice)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        MarkdownField(choice.text, { value -> vm.editEmbeddedDraft { f -> f.copy(choices = f.choices.map { if (it.id == choice.id) it.copy(text = value) else it }) } },
                            label = stringResource(R.string.choice_number, index + 1), enabled = enabled)
                        FilterChip(choice.id == form.correctId, { vm.editEmbeddedDraft { it.copy(correctId = choice.id) } }, enabled = enabled,
                            label = { Text(stringResource(R.string.correct_answer)) })
                        TextButton({ vm.editEmbeddedDraft { it.moveChoice(choice.id, -1) } }, enabled = enabled && index > 0) { Text(stringResource(R.string.move_choice_up)) }
                        TextButton({ vm.editEmbeddedDraft { it.moveChoice(choice.id, 1) } }, enabled = enabled && index < form.choices.lastIndex) { Text(stringResource(R.string.move_choice_down)) }
                        AppDangerTextButton({ vm.editEmbeddedDraft { it.copy(choices = it.choices.filterNot { row -> row.id == choice.id },
                            correctId = it.correctId.takeUnless { selected -> selected == choice.id }) } }, enabled = enabled && form.choices.size > 2) { Text(stringResource(R.string.remove_choice)) }
                    }
                }
                item {
                    OutlinedButton({ vm.editEmbeddedDraft { if (it.choices.size < vm.maxChoices.coerceAtLeast(2)) it.copy(choices = it.choices + QuestionChoiceDraft()) else it } },
                        enabled = enabled && form.choices.size < vm.maxChoices.coerceAtLeast(2)) { Text(stringResource(R.string.add_choice)) }
                    MarkdownField(form.explanation, { value -> vm.editEmbeddedDraft { it.copy(explanation = value) } },
                        label = stringResource(R.string.explanation), enabled = enabled, minLines = 2)
                    OutlinedTextField(form.source, { value -> vm.editEmbeddedDraft { it.copy(source = value) } }, enabled = enabled,
                        label = { Text(stringResource(R.string.question_source)) }, modifier = Modifier.fillMaxWidth())
                    Text(stringResource(R.string.difficulty))
                    listOf("easy" to R.string.difficulty_easy, "medium" to R.string.difficulty_medium, "hard" to R.string.difficulty_hard).forEach { (value, label) ->
                        FilterChip(form.difficulty == value, { vm.editEmbeddedDraft { it.copy(difficulty = value) } }, enabled = enabled,
                            label = { Text(stringResource(label)) })
                    }
                    if (s.draftCategoriesBusy) AppLoadingIndicator()
                    s.draftCategoriesError?.let { ErrorState(throwable = it, onRetry = if (s.busy || s.draftCategoriesBusy) null else vm::loadDraftCategories) }
                    Box {
                        OutlinedButton({ categoryMenu = true }, enabled = enabled && !s.draftCategoriesBusy && s.draftCategoriesError == null) {
                            Text(stringResource(R.string.nav_categories) + ": " + (s.draftCategories.firstOrNull { it.id == form.category }?.name ?: stringResource(R.string.none)))
                        }
                        DropdownMenu(categoryMenu && enabled, { categoryMenu = false }) {
                            DropdownMenuItem(text = { Text(stringResource(R.string.none)) }, onClick = { vm.editEmbeddedDraft { it.copy(category = null) }; categoryMenu = false })
                            s.draftCategories.forEach { category ->
                                DropdownMenuItem(text = { Text(category.name) }, onClick = { vm.editEmbeddedDraft { it.copy(category = category.id) }; categoryMenu = false })
                            }
                        }
                    }
                    Text(stringResource(R.string.editor_case_stem_hint))
                    OutlinedTextField(form.caseKey, { value -> vm.editEmbeddedDraft { it.copy(caseKey = value) } }, enabled = enabled,
                        label = { Text(stringResource(R.string.editor_case_key)) }, modifier = Modifier.fillMaxWidth())
                    MarkdownField(form.caseStem, { value -> vm.editEmbeddedDraft { it.copy(caseStem = value) } },
                        label = stringResource(R.string.editor_case_stem), enabled = enabled, minLines = 2)
                    OutlinedTextField(form.caseOrder, { value -> vm.editEmbeddedDraft { it.copy(caseOrder = value) } }, enabled = enabled,
                        label = { Text(stringResource(R.string.editor_case_order)) }, modifier = Modifier.fillMaxWidth())
                }
            }
        }, confirmButton = { TextButton(vm::createEmbeddedDraft, enabled = enabled) { Text(stringResource(R.string.exam_draft_create)) } },
        dismissButton = { TextButton(close, enabled = canClose) { Text(stringResource(if (draft.createdId != null) R.string.back else R.string.cancel)) } })
}
