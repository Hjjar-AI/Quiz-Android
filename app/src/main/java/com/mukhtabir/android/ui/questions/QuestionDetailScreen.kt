@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.ui.components.ExplanationCard
import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerTextButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import com.mukhtabir.android.ui.components.AppButton as Button
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.BookmarkService
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.QuestionContext
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.QuestionImage

@Composable
fun QuestionDetailRoute(
    questionId: Long,
    questionService: QuestionService,
    bookmarkService: BookmarkService,
    store: com.mukhtabir.android.core.SessionDraftStore,
    imageRepository: ImageRepository,
    canStartSessions: Boolean,
    canEditAny: Boolean,
    canEditOwn: Boolean,
    canUploadImageOwn: Boolean,
    canReviewQuestion: Boolean,
    canDuplicate: Boolean,
    canDeleteOwn: Boolean,
    canDeleteAny: Boolean,
    bankRevision: Int,
    userId: Long,
    onStart: (TestMode, List<Long>, String) -> Unit,
    onEdit: (Long) -> Unit,
    onManageImage: (Long) -> Unit,
    onOpenKnowledge: (Long) -> Unit,
    onOpenCase: (String) -> Unit,
    onDuplicated: (Long) -> Unit,
    onDeleted: (Long) -> Unit,
    onMutationBusyChanged: (Boolean) -> Unit,
    onShare: ((Long) -> Unit)? = null,
    onPaneLockChanged: (Boolean) -> Unit = {},
) {
    val viewModel: QuestionDetailViewModel = viewModel(
        key = "question-$userId-$questionId-$bankRevision",
        factory = QuestionDetailViewModel.Factory(questionId, questionService, bookmarkService, userId, canDuplicate, canDeleteOwn, canDeleteAny, store),
    )
    viewModel.permissions(canDuplicate,canDeleteOwn,canDeleteAny)
    val state by viewModel.state.collectAsState()
    val recoveryBlocked = com.mukhtabir.android.ui.components.editorRecoveryBlocked(viewModel.recovery)
    DisposableEffect(state.mutationBusy) {
        onMutationBusyChanged(state.mutationBusy)
        onDispose { onMutationBusyChanged(false) }
    }
    val paneLocked = state.mutationBusy || state.bookmarkBusy || recoveryBlocked
    DisposableEffect(paneLocked) {
        onPaneLockChanged(paneLocked)
        onDispose { onPaneLockChanged(false) }
    }
    val detailSessionLabel = stringResource(R.string.question_details)
    var readingLocale by rememberSaveable(questionId) { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable(questionId) { mutableStateOf(false) }
    var deleteVersion by rememberSaveable(questionId) { mutableStateOf<Int?>(null) }
    var confirmDuplicate by rememberSaveable(questionId) { mutableStateOf(false) }
    LaunchedEffect(state.duplicatedId, state.deleted) {
        state.duplicatedId?.let { id -> viewModel.consumeMutation(); onDuplicated(id) }
        if (state.deleted) { viewModel.consumeMutation(); onDeleted(questionId) }
    }
    com.mukhtabir.android.ui.components.BlockedBackHandler(enabled = state.mutationBusy)
    if (confirmDelete || confirmDuplicate) AlertDialog(
        onDismissRequest = { confirmDelete = false; confirmDuplicate = false },
        title = { Text(stringResource(if (confirmDelete) R.string.delete_question else R.string.duplicate_question)) },
        text = { Text(stringResource(if (confirmDelete) R.string.delete_question_confirmation else R.string.duplicate_question_confirmation)) },
        confirmButton = {
            AppDangerTextButton({
                if (confirmDelete) deleteVersion?.let(viewModel::delete) else viewModel.duplicate()
                confirmDelete = false; confirmDuplicate = false
            }, enabled = !recoveryBlocked && !state.mutationBusy && !state.bookmarkBusy && !state.isLoading) {
                Text(stringResource(if (confirmDelete) R.string.delete_question else R.string.duplicate_question))
            }
        },
        dismissButton = { TextButton({ confirmDelete = false; confirmDuplicate = false }) { Text(stringResource(R.string.cancel)) } },
    )

    Column(Modifier.fillMaxSize()) {
        com.mukhtabir.android.ui.components.EditorRecoveryControls(viewModel.recovery, state.mutationBusy || state.bookmarkBusy, viewModel::restore)
        if(state.question == null && state.duplicateUncertain && canDuplicate) {
            OutlinedButton(viewModel::duplicate, enabled = !recoveryBlocked && !state.mutationBusy) { Text(stringResource(R.string.check_previous_copy)) }
            state.actionError?.let { Text(it, color=MaterialTheme.colorScheme.error) }
        }
        Box(Modifier.weight(1f)) {
        when {
        state.isLoading -> Column(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets)) { AppLoadingIndicator() }
        state.error != null -> ErrorState(
            message = state.error,
            onRetry = viewModel::load,
            modifier = Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),
        )
        state.question != null -> {
            val question = requireNotNull(state.question)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(AppLayoutTokens.libraryPadding),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
            ) {
                item { AppPageHeading(stringResource(R.string.question_details)) }
                val display = question.localizedForDisplay(readingLocale ?: java.util.Locale.getDefault().toLanguageTag())
                item { ClinicalCaseContext(display.case) }
                item { com.mukhtabir.android.ui.components.QuestionLanguageSelector(question.translations, question.choices.size,
                    readingLocale, { readingLocale = it }, hasCase = question.case?.stem?.isNotBlank() == true, caseTranslations = question.case?.translations ?: kotlinx.serialization.json.JsonObject(emptyMap()),
                    explanationAvailable = !question.explanation.isNullOrBlank() && !question.answersHidden,
                    explanationTranslated = !display.explanation.isNullOrBlank()) }
                item { com.mukhtabir.android.ui.components.QuestionTextCard(display.question) }
                item { QuestionImage(question.imageUrl, imageRepository) }
                items(display.choices.withIndex().toList()) { (index, choice) ->
                    com.mukhtabir.android.ui.components.QuestionChoiceCard {
                        Text("${com.mukhtabir.android.util.formatNumber(index + 1)}.")
                        Column(Modifier.weight(1f)) {
                            MarkdownText(choice)
                            if(canReviewQuestion && display.correctAnswer==index+1) Text(stringResource(R.string.correct_answer))
                        }
                    }
                }
                item { QuestionContext(question) }
                item { com.mukhtabir.android.ui.components.BilingualQuestionComparison(question, choicesVisible = true,
                    explanationVisible = !question.answersHidden && canReviewQuestion) }
                if(!question.answersHidden) {
                    question.knowledgeObject?.let { id -> item {
                        OutlinedButton({onOpenKnowledge(id)},enabled=!state.isLoading && !state.mutationBusy && !state.bookmarkBusy) { Text(stringResource(R.string.explore_knowledge_detail)) }
                    } }
                    question.case?.key?.takeIf(String::isNotBlank)?.let { key -> item {
                        OutlinedButton({onOpenCase(key)},enabled=!state.isLoading && !state.mutationBusy && !state.bookmarkBusy) { Text(stringResource(R.string.explore_open_case)) }
                    } }
                }

                item {
                    AppSection(stringResource(R.string.section_verification)) {
                    Text(stringResource(if(question.verified) R.string.verified else R.string.unverified))
                    question.verifiedBy?.takeIf(String::isNotBlank)?.let { Text(stringResource(R.string.moderation_verified_by,it)) }
                    question.verifiedAt?.let { Text(it,style=MaterialTheme.typography.bodySmall) }
                    if(!question.answersHidden) question.verificationNotes?.takeIf(String::isNotBlank)?.let {
                        Text(stringResource(R.string.moderation_existing_notes,it))
                    }
                    }
                }
                if(canReviewQuestion && !question.answersHidden) item {
                    AppSection(stringResource(R.string.moderation_question_metadata)) {
                        Text(stringResource(R.string.moderation_original_author, question.authoredByUsername ?: question.authoredBy?.toString() ?: stringResource(R.string.none)))
                        Text(stringResource(R.string.moderation_current_owner, question.ownedByUsername ?: question.ownedBy?.toString() ?: stringResource(R.string.none)))
                        Text(stringResource(R.string.moderation_version, question.version))
                        question.createdAt?.let { Text(stringResource(R.string.moderation_created, com.mukhtabir.android.util.formatLocalDateTime(it))) }
                        question.updatedAt?.let { Text(stringResource(R.string.moderation_updated, com.mukhtabir.android.util.formatLocalDateTime(it))) }
                        question.updatedBy?.let { Text(stringResource(R.string.moderation_updated_by, it.toString())) }
                        question.lastRevisedAt?.let { Text(stringResource(R.string.moderation_last_revised, it)) }
                        Text(stringResource(R.string.moderation_answer_totals, question.timesAnswered, question.timesCorrect))
                    }
                }
                display.explanation?.takeIf(String::isNotBlank)?.let { explanation ->
                    item {
                        AppSection(stringResource(R.string.explanation), initiallyExpanded = true) {
                            ExplanationCard(explanation)
                        }
                    }
                }
                item {
                    com.mukhtabir.android.ui.components.AppMetadataRow(listOfNotNull(
                        question.categoryName,question.difficulty,question.tags.joinToString().takeIf(String::isNotBlank)))
                }
                item {
                    AppSection(stringResource(R.string.section_question_actions)) {
                        val idle = !recoveryBlocked && !state.mutationBusy && !state.bookmarkBusy
                        onShare?.let { share -> OutlinedButton({ share(question.id) }, enabled = idle) { Text(stringResource(R.string.link_share_question)) } }
                        if (canStartSessions) {
                            Button({ onStart(TestMode.STUDY, listOf(question.id), "$detailSessionLabel #${question.id}") }, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_study)) }
                            OutlinedButton({ onStart(TestMode.EXAM, listOf(question.id), "$detailSessionLabel #${question.id}") }, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_exam)) }
                        }
                        if (canEditAny || (canEditOwn && question.ownedBy == userId)) Button({ onEdit(question.id) }, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.edit_question)) }
                        if (canEditAny || (canUploadImageOwn && question.ownedBy == userId)) OutlinedButton({ onManageImage(question.id) }, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.question_manage_image)) }
                        if (state.mutationBusy) AppLoadingIndicator()
                        if (canDuplicate) OutlinedButton({ if (state.duplicateUncertain) viewModel.duplicate() else confirmDuplicate = true }, enabled = idle) { Text(stringResource(if (state.duplicateUncertain) R.string.check_previous_copy else R.string.duplicate_question)) }
                        if (canDuplicate && state.duplicateUncertain) OutlinedButton(viewModel::retryDuplicate, enabled = idle) { Text(stringResource(R.string.retry_same_request)) }
                        if (canDeleteAny || (canDeleteOwn && question.ownedBy == userId)) OutlinedButton({ deleteVersion=question.version;confirmDelete = true }, enabled = idle) { Text(stringResource(R.string.delete_question), color = MaterialTheme.colorScheme.error) }
                        if (state.deleteConflict) {
                            Text(stringResource(R.string.content_conflict), color = MaterialTheme.colorScheme.error)
                            OutlinedButton(viewModel::load,enabled=idle) {Text(stringResource(R.string.refresh))}
                        }
                    }
                }
                item { QuestionFeedback(questionId, userId, questionService, store) }
                item {
                    OutlinedButton(onClick = viewModel::toggleBookmark, enabled = !recoveryBlocked && !state.bookmarkBusy && !state.mutationBusy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(if (viewModel.hasPendingBookmark) R.string.quiz_bookmark_check else R.string.toggle_bookmark))
                    }
                    state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    state.bookmarkChange?.let { change ->
                        Text(stringResource(if (change == BookmarkChange.ADDED) R.string.bookmark_added else R.string.bookmark_removed))
                    }
                }
            }
        }
    }
        }
    }
}
