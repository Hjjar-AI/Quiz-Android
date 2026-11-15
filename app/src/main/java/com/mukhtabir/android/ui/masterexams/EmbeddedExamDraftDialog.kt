package com.mukhtabir.android.ui.masterexams

import com.mukhtabir.android.util.QuestionContentLimits

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.MasterExamDraftWrite
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.QuestionDraftPreview
import com.mukhtabir.android.ui.questions.QuestionChoiceDraft
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber
import java.util.Locale

@kotlinx.serialization.Serializable
internal data class EmbeddedExamDraftForm(
    val question: String = "", val choices: List<QuestionChoiceDraft> = List(2) { QuestionChoiceDraft() },
    val correctId: String? = null, val explanation: String = "", val source: String = "",
    val difficulty: String = "medium", val category: Long? = null,
    val caseKey: String = "", val caseStem: String = "", val caseOrder: String = "",
) {
    internal fun parsedCaseOrder(): Int? = caseOrder.trim().map { char ->
        val digit = Character.digit(char, 10)
        if (digit >= 0) ('0'.code + digit).toChar() else char
    }.joinToString("").toIntOrNull()?.takeIf { it > 0 }

    fun validationErrors(maxChoices: Int): List<DraftFieldError> = buildList {
        fun length(value: String) = value.codePointCount(0, value.length)
        fun text(key: String, value: String, limit: Int, required: Boolean = false) {
            if (required && value.trim().isEmpty()) add(DraftFieldError(key, R.string.draft_field_required))
            else if (length(value.trim()) > limit) add(DraftFieldError(key, R.string.draft_field_too_long, limit))
        }
        text("question", question, QuestionContentLimits.QUESTION, true)
        text("explanation", explanation, QuestionContentLimits.EXPLANATION)
        if (choices.size !in 2..maxChoices.coerceAtLeast(2)) add(DraftFieldError("choices", R.string.draft_choice_count, maxChoices.coerceAtLeast(2)))
        val counts = choices.groupingBy { it.text.trim().lowercase(Locale.ROOT) }.eachCount()
        choices.forEach { choice ->
            text("choice:${choice.id}", choice.text, QuestionContentLimits.CHOICE, true)
            if (choice.text.isNotBlank() && counts[choice.text.trim().lowercase(Locale.ROOT)] != 1)
                add(DraftFieldError("choice:${choice.id}", R.string.draft_choice_duplicate))
        }
        if (choices.none { it.id == correctId }) add(DraftFieldError("correct", R.string.draft_correct_required))
        text("source", source, 200)
        if (difficulty !in setOf("easy", "medium", "hard")) add(DraftFieldError("difficulty", R.string.draft_difficulty_invalid))
        if (category != null && category <= 0) add(DraftFieldError("category", R.string.draft_category_invalid))
        text("caseKey", caseKey, 64)
        if (caseKey.isBlank() && (caseStem.isNotBlank() || caseOrder.isNotBlank()))
            add(DraftFieldError("caseKey", R.string.draft_case_key_required))
        text("caseStem", caseStem, QuestionContentLimits.CASE_STEM)
        if (caseOrder.isNotBlank() && parsedCaseOrder() == null) add(DraftFieldError("caseOrder", R.string.draft_case_order_invalid))
    }

    fun payload(maxChoices: Int): MasterExamDraftWrite? {
        if (validationErrors(maxChoices).isNotEmpty()) return null
        return MasterExamDraftWrite(question.trim(), choices.map { it.text.trim() },
            choices.indexOfFirst { it.id == correctId } + 1, explanation.trim(), source.trim(), difficulty, category,
            caseKey.trim().takeIf { it.isNotEmpty() }, caseStem.trim().takeIf { it.isNotEmpty() }, parsedCaseOrder())
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
    val validation: Boolean = false, val validationAttempt: Int = 0, val error: Throwable? = null,
    val uncertain: Boolean = false, val reconciled: Boolean = false, val createdId: Long? = null,
) { val dirty: Boolean get() = form != baseline }

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
internal fun EmbeddedExamDraftDialog(draft: EmbeddedExamDraftState, s: ExamEditorState, vm: MasterExamEditorViewModel) {
    val form = draft.form
    val enabled = vm.editable(s)
    val validation = DraftValidationUi(if (draft.validation) form.validationErrors(vm.maxChoices) else emptyList(), draft.validationAttempt)
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    androidx.compose.runtime.LaunchedEffect(validation.attempt) {
        val field = validation.errors.firstOrNull()?.key ?: return@LaunchedEffect
        val base = 1 + if (draft.reconciled) (s.exam?.questionIds.orEmpty() - draft.beforeIds.toSet()).size else 0
        val item = when {
            field == "question" -> base
            field == "correct" && form.choices.isNotEmpty() -> base + 1
            field.startsWith("choice:") -> base + 1 + form.choices.indexOfFirst { "choice:${it.id}" == field }.coerceAtLeast(0)
            else -> base + 1 + form.choices.size
        }
        listState.scrollToItem(item)
    }
    var confirmClose by remember { mutableStateOf(false) }
    val canClose = !s.busy && (!(draft.uncertain || draft.createdId != null) || draft.reconciled)
    val close: () -> Unit = {
        if (canClose) {
            if (draft.uncertain || (draft.dirty && draft.createdId == null)) confirmClose = true
            else vm.closeEmbeddedDraft()
        }
    }
    if (confirmClose) AppConfirmationDialog(onDismissRequest = { confirmClose = false },
        title = { Text(stringResource(R.string.exam_author_leave_title)) },
        text = { Text(stringResource(if (draft.uncertain) R.string.exam_draft_close_uncertain else R.string.exam_draft_discard)) },
        confirmButton = { AppDangerTextButton({ confirmClose = false; vm.closeEmbeddedDraft() }, enabled = canClose) { Text(stringResource(R.string.ux_discard_question_draft)) } },
        dismissButton = { AppTextButton({ confirmClose = false }) { Text(stringResource(R.string.keep_editing)) } })
    AppConfirmationDialog(onDismissRequest = close,
        title = { Text(stringResource(R.string.exam_draft_create)) },
        text = {
            LazyColumn(Modifier.heightIn(max = AppLayoutTokens.pickerMaxHeight), state = listState, verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                item {
                    Text(stringResource(R.string.exam_draft_scope))
                    if (s.busy) AppLoadingIndicator()
                    draft.error?.let { ErrorState(throwable = it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                    s.error?.let { ErrorState(throwable = it, onRetry = if (s.busy) null else vm::refresh) }
                    s.message?.let { Text(stringResource(it), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                    if (s.denied) Text(stringResource(R.string.error_forbidden))
                    if (s.exam?.canEditNow == false) Text(stringResource(R.string.exam_author_frozen))
                    if (draft.validation) DraftValidationSummary(validation)
                    if (draft.uncertain) Text(stringResource(R.string.exam_draft_uncertain))
                    draft.createdId?.let { Text(stringResource(R.string.exam_draft_created_id, formatNumber(it))) }
                    if (draft.uncertain || draft.createdId != null) {
                        AppOutlinedButton(vm::refresh, enabled = !s.busy) { Text(stringResource(R.string.refresh)) }
                        Text(stringResource(R.string.exam_draft_review))
                        s.questionError?.let { ErrorState(throwable = it, onRetry = if (s.busy) null else vm::retryQuestions) }
                    }
                }
                if (draft.reconciled) items(s.exam?.questionIds.orEmpty() - draft.beforeIds.toSet(), key = { "review-$it" }) { id ->
                    Column {
                        Text(stringResource(R.string.exam_draft_created_id, formatNumber(id)))
                        s.questions[id]?.let { question ->
                            val display = question.localizedForDisplay()
                            QuestionDraftPreview(display.question, display.choices, display.correctAnswer,
                                display.explanation.orEmpty(), display.case)
                            Text(stringResource(R.string.question_source) + ": " + question.source.orEmpty())
                            Text(stringResource(R.string.nav_categories) + ": " + question.categoryName.orEmpty())
                        }
                    }
                }
                item { ExamDraftQuestionFields(form, enabled, vm::editEmbeddedDraft, validation) }
                items(form.choices, key = { it.id }) { choice ->
                    ExamDraftChoiceFields(form, choice, form.choices.indexOf(choice), enabled, vm::editEmbeddedDraft, validation)
                }
                item {
                    ExamDraftDetailsFields(form, enabled, vm.maxChoices, s.draftCategories, caseStemLocked = false,
                        onCaseKeyChange = { value -> vm.editEmbeddedDraft { it.copy(caseKey = value) } },
                        categoryEnabled = !s.draftCategoriesBusy && s.draftCategoriesError == null,
                        revealError = s.draftCategoriesError != null,
                        categoryStatus = {
                            if (s.draftCategoriesBusy) AppLoadingIndicator()
                            s.draftCategoriesError?.let { ErrorState(throwable = it,
                                onRetry = if (s.busy || s.draftCategoriesBusy) null else vm::loadDraftCategories) }
                        }, change = vm::editEmbeddedDraft, validation = validation)
                }
            }
        }, confirmButton = { AppTextButton(vm::createEmbeddedDraft, enabled = enabled) { Text(stringResource(R.string.exam_draft_create)) } },
        dismissButton = { AppTextButton(close, enabled = canClose) { Text(stringResource(if (draft.createdId != null) R.string.back else R.string.cancel)) } })
}
