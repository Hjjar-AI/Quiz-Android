package com.mukhtabir.android.ui.quiz

import com.mukhtabir.android.ui.components.ReviewFilter
import com.mukhtabir.android.ui.components.ResultReviewFilters

import com.mukhtabir.android.ui.components.AppLayoutTokens


import com.mukhtabir.android.ui.theme.AppTheme
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.material3.LinearProgressIndicator

import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppDangerTextButton

import com.mukhtabir.android.ui.components.AppDangerOutlinedButton

import com.mukhtabir.android.ui.components.AppActionRow

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.core.QuizPresetHolder
import com.mukhtabir.android.data.remote.BlueprintService
import com.mukhtabir.android.data.remote.BookmarkService
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.SessionResultItemDto
import com.mukhtabir.android.data.remote.SessionResultsDto
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.data.remote.TestSessionService
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.SessionResultReview
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.QuestionLanguageSelector
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.QuestionImage
import java.util.Locale

@Composable
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
fun QuizRoute(
    questionService: QuestionService,
    sessionService: TestSessionService,
    bookmarkService: BookmarkService,
    categoryService: CategoryService,
    blueprintService: BlueprintService,
    imageRepository: ImageRepository,
    quizPresetHolder: QuizPresetHolder,
    canUseBlueprint: Boolean,
    maxQuizQuestions: Int,
    userId: Long,
    draftStore: SessionDraftStore,
    preferences: com.mukhtabir.android.core.AppPreferences,
    onBack: () -> Unit,
    onOpenHistory: (() -> Unit)? = null,
    onSessionActiveChanged: (Boolean) -> Unit,
) {
    val viewModel: QuizViewModel = viewModel(
        key = "quiz-$userId",
        factory = QuizViewModel.Factory(
            questionService,
            sessionService,
            bookmarkService,
            categoryService,
            blueprintService,
            canUseBlueprint,
            userId,
            draftStore,
            preferences,
            imageRepository,
        ),
    )
    val state by viewModel.state.collectAsState()
    var confirmFinishRecovery by remember { mutableStateOf(false) }
    var confirmRecoveredFinish by remember { mutableStateOf(false) }
    var confirmDiscardFinishStorage by remember { mutableStateOf(false) }
    if(confirmDiscardFinishStorage) AlertDialog(onDismissRequest={confirmDiscardFinishStorage=false},
        title={Text(stringResource(R.string.quiz_finish_recovery_title))},
        text={Text(stringResource(R.string.quiz_finish_storage_discard))},
        confirmButton={AppDangerTextButton({confirmDiscardFinishStorage=false;viewModel.discardUnreadableFinishRecovery()},enabled=!state.isLoading) { Text(stringResource(R.string.ux_discard_finish_recovery)) }},
        dismissButton={TextButton({confirmDiscardFinishStorage=false}) { Text(stringResource(R.string.cancel)) }})
    if(confirmFinishRecovery) AlertDialog(onDismissRequest={confirmFinishRecovery=false},
        title={Text(stringResource(R.string.quiz_finish_recovery_title))},
        text={Text(stringResource(R.string.quiz_finish_review_confirm))},
        confirmButton={TextButton({confirmFinishRecovery=false;viewModel.acknowledgeFinishRecovery()},enabled=!state.isLoading) { Text(stringResource(R.string.quiz_finish_review_done)) }},
        dismissButton={TextButton({confirmFinishRecovery=false}) { Text(stringResource(R.string.cancel)) }})
    if(confirmRecoveredFinish) AlertDialog(onDismissRequest={confirmRecoveredFinish=false},
        title={Text(stringResource(R.string.finish_session_title))},
        text={Text(stringResource(R.string.finish_session_message))},
        confirmButton={TextButton({confirmRecoveredFinish=false;viewModel.finish()},enabled=!state.isLoading && !state.recoveryRequired) { Text(stringResource(R.string.finish_session_action)) }},
        dismissButton={TextButton({confirmRecoveredFinish=false}) { Text(stringResource(R.string.cancel)) }})
    val display by preferences.display.collectAsState()
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    var foreground by remember { mutableStateOf(owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)) }
    DisposableEffect(owner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, _ -> foreground = owner.lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED) }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    var handledAdvanceToken by remember { mutableStateOf(state.autoAdvanceToken) }
    LaunchedEffect(state.autoAdvanceToken, foreground, display.autoAdvance, state.questionPage?.index, state.questionPage?.savedAnswer, state.confidence,
        state.errorReason, state.isLoading, state.pendingDraft, state.recoveryRequired, state.draftStorageFailed) {
        val page = state.questionPage
        val answer = state.selectedAnswer
        if (state.autoAdvanceToken > handledAdvanceToken && foreground && display.autoAdvance && !state.isLoading && page != null && answer != null && page.savedAnswer == answer) {
            handledAdvanceToken = state.autoAdvanceToken
            kotlinx.coroutines.delay(600)
            viewModel.autoAdvance(page.index, answer)
        }
    }
    LaunchedEffect(maxQuizQuestions) {
        viewModel.applyMaxQuizQuestions(maxQuizQuestions)
    }
    LaunchedEffect(canUseBlueprint) {viewModel.configureBlueprintPermission(canUseBlueprint)}
    com.mukhtabir.android.ui.components.RefreshOnResume {viewModel.refreshMetadata()}
    LaunchedEffect(Unit) { if (!state.setupLoaded && !state.isLoading) viewModel.initialize() }
    val sessionActive = state.session != null && state.results == null
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(sessionActive) {
        if (sessionActive) {
            focusManager.clearFocus()
            keyboard?.hide()
        }
    }
    val sessionUi = state.toSessionPresentation()
    ConfirmedAnswerHaptics(state.confirmedSaveToken, display.haptics, foreground)
    var confirmPause by rememberSaveable(state.session?.sessionId) { mutableStateOf(false) }
    DisposableEffect(sessionActive) {
        onSessionActiveChanged(sessionActive)
        onDispose { onSessionActiveChanged(false) }
    }
    KeepSessionScreenOn(sessionActive)
    val contentScroll = rememberScrollState()
    LaunchedEffect(state.session?.sessionId, state.questionPage?.index) { contentScroll.scrollTo(0) }
    BackHandler {
        if (sessionActive) confirmPause = true else viewModel.pauseAndExit(onBack)
    }
    if (confirmPause && sessionActive) AlertDialog(
        onDismissRequest = { if (!state.isLoading && !state.syncing) confirmPause = false },
        title = { Text(stringResource(R.string.session_pause_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            Text(stringResource(if (state.locallyFinished) R.string.offline_finish_leave
                else if (state.queuedAnswers > 0) R.string.session_pause_sync_first else R.string.session_pause_message))
            if (state.syncing) com.mukhtabir.android.ui.components.AppSaveStatus(stringResource(R.string.quiz_syncing))
            if (state.syncError != null || state.error != null) ErrorState(throwable=state.syncError,message=state.error)
            if (!sessionUi.canRequestPause && !state.isLoading && !state.syncing) {
                Text(stringResource(R.string.session_pause_recovery_required))
            }
        } },
        confirmButton = {
            TextButton({ viewModel.pauseAndExit { confirmPause = false; onBack() } }, enabled = sessionUi.canRequestPause) {
                Text(stringResource(if(state.queuedAnswers>0 && !state.locallyFinished)
                    R.string.session_sync_pause_leave else R.string.pause_and_leave))
            }
        },
        dismissButton = { TextButton({ confirmPause = false },enabled=!state.isLoading && !state.syncing) { Text(stringResource(R.string.cancel)) } },
    )

    // Consume any preset dropped by the Home / Mistakes / Fragile
    // screens. The holder's StateFlow is the signal: as long as a
    // preset is pending, the effect fires; consuming it clears the
    // slot, the flow emits null, and the effect ends. That handles
    // the case where this composable is restored (rather than
    // re-created) by the tab-navigation save/restore pattern.
    var retryLaunch by remember { mutableStateOf(false) }
    if (retryLaunch) state.launchPreset?.let { pending ->
        com.mukhtabir.android.ui.components.PracticeLauncher(pending.sessionLabel, pending.questionIds, state.maxQuizQuestions,
            enabled = !state.isLoading && !state.recoveryRequired && state.resumable == null && state.session == null,
            initialMode = pending.mode, onDismiss = { retryLaunch = false }, onStart = { mode, ids, title ->
                retryLaunch = false; viewModel.startPreset(com.mukhtabir.android.core.QuizPreset(mode, ids, title))
            })
    }
    val pendingSetupMode by quizPresetHolder.setupMode.collectAsState()
    LaunchedEffect(pendingSetupMode, state.setupLoaded, state.isLoading, state.recoveryRequired, state.session, state.results, state.pendingDraft, state.draftStorageFailed) {
        if (pendingSetupMode != null && state.setupLoaded && !state.isLoading && !state.recoveryRequired &&
            state.session == null && state.results == null && state.pendingDraft == null && !state.draftStorageFailed) {
            quizPresetHolder.consumeSetupMode()?.let(viewModel::setMode)
        }
    }
    val pendingPreset by quizPresetHolder.preset.collectAsState()
    LaunchedEffect(pendingPreset, state.setupLoaded, state.isLoading, state.recoveryRequired, state.session, state.results, state.pendingDraft, state.draftStorageFailed) {
        if (pendingPreset != null && state.setupLoaded && !state.isLoading && !state.recoveryRequired
            && state.session == null && state.results == null && state.pendingDraft == null && !state.draftStorageFailed) {
            quizPresetHolder.consume()?.let(viewModel::startPreset)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(com.mukhtabir.android.ui.components.AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap),
    ) {
        if (sessionActive) SessionHeader(sessionUi) {
            SessionToolsMenu(state, sessionUi, viewModel, preferences, onPause = { confirmPause = true })
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(contentScroll),
            verticalArrangement = Arrangement.spacedBy(if (sessionActive) AppTheme.spacing.content else AppTheme.spacing.section),
        ) {
            state.syncConflict?.let { conflict ->
                Text(stringResource(R.string.session_sync_conflict, conflict.index + 1,
                    state.syncConflictAnswer ?: 0, conflict.savedAnswer ?: 0))
                SelectionRow {
                    OutlinedButton(onClick = { viewModel.resolveDownloadedConflict(true) }, enabled = !state.isLoading) {
                        Text(stringResource(R.string.session_keep_device))
                    }
                    OutlinedButton(onClick = { viewModel.resolveDownloadedConflict(false) }, enabled = !state.isLoading) {
                        Text(stringResource(R.string.session_keep_server))
                    }
                }
            }
            if (sessionActive && state.draftStorageFailed && state.pendingDraft == null) {
                com.mukhtabir.android.ui.components.AppRecoveryPanel(
                    message=stringResource(R.string.quiz_finish_storage_error),
                    onRetry=viewModel::retryDownloadedStorage,retryEnabled=!state.isLoading)
            }
            if (sessionActive && state.mode != TestMode.RECALL) {
                if (!state.downloaded) {
                    com.mukhtabir.android.ui.components.AppRecoveryPanel(
                        message=stringResource(R.string.session_download_required),
                        onRetry={viewModel.downloadSession()},retryEnabled=!state.isLoading && !state.draftStorageFailed)
                } else {
                    SessionSyncStatus(state, viewModel)
                }
            }
            if (!sessionActive) Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppPageHeading(stringResource(R.string.quizzes))
            }

            if (state.isLoading && state.questionPage == null) AppLoadingIndicator()
            if (state.locallyFinished) Text(stringResource(R.string.offline_finished_session),
                style = MaterialTheme.typography.bodySmall)
            if (state.error != null || state.hasUnknownError) {
                Text(
                    state.error ?: stringResource(R.string.error_unknown),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    color = MaterialTheme.colorScheme.error,
                )
                if (state.recoveryRequired) Text(stringResource(R.string.session_recovery_required))
                if (state.recoveryRequired || (state.questionPage == null && state.results == null)) {
                    OutlinedButton(onClick = viewModel::initialize, enabled = !state.isLoading) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            if(state.finishStorageFailed) {
                com.mukhtabir.android.ui.components.AppRecoveryPanel(
                    message=stringResource(R.string.quiz_finish_storage_error),
                    onRetry=viewModel::retryFinishStorage,retryEnabled=!state.isLoading)
                if(state.finishRecovery==null) AppDangerOutlinedButton({confirmDiscardFinishStorage=true},enabled=!state.isLoading) {
                    Text(stringResource(R.string.ux_discard_finish_recovery))
                }
            }
            if(state.finishPending) {
                Text(stringResource(R.string.quiz_finish_recovery_title),style=MaterialTheme.typography.titleMedium)
                Text(stringResource(if(state.finishStatusChecked && state.finishedSessionMissing)
                    R.string.quiz_finish_session_missing else R.string.quiz_finish_uncertain))
                OutlinedButton(viewModel::initialize,enabled=!state.isLoading) { Text(stringResource(R.string.refresh)) }
                onOpenHistory?.let { history ->
                    OutlinedButton(history,enabled=!state.isLoading) { Text(stringResource(R.string.history)) }
                }
                if(state.finishStatusChecked && !state.finishStorageFailed) OutlinedButton({confirmFinishRecovery=true},enabled=!state.isLoading) {
                    Text(stringResource(R.string.quiz_finish_review_done))
                }
            }
            if(state.session!=null && state.questionPage==null && !state.finishStorageFailed && !state.finishPending && !state.recoveryRequired && state.results==null) {
                OutlinedButton({confirmRecoveredFinish=true},enabled=!state.isLoading) { Text(stringResource(R.string.finish)) }
            }
            if (state.draftStorageFailed) {
                com.mukhtabir.android.ui.components.AppRecoveryPanel(
                    message=stringResource(R.string.session_draft_storage_error),
                    onRetry=viewModel::retryDraftStorage,retryEnabled=!state.isLoading)
                if (state.pendingDraft == null) {
                    // A malformed draft may not decode, but explicit discard can still
                    // remove its encrypted value without interpreting it.
                    var confirmUnreadableDiscard by rememberSaveable(userId) { mutableStateOf(false) }
                    AppDangerOutlinedButton({ confirmUnreadableDiscard = true }, enabled = !state.isLoading) {
                        Text(stringResource(R.string.ux_discard_answer_draft))
                    }
                    if (confirmUnreadableDiscard) AlertDialog(
                        onDismissRequest = { confirmUnreadableDiscard = false },
                        title = { Text(stringResource(R.string.session_draft_discard_title)) },
                        text = { Text(stringResource(R.string.session_draft_discard_message)) },
                        confirmButton = { AppDangerTextButton({ confirmUnreadableDiscard = false; viewModel.discardLocalDraft() }, enabled = !state.isLoading) { Text(stringResource(R.string.ux_discard_answer_draft)) } },
                        dismissButton = { TextButton({ confirmUnreadableDiscard = false }) { Text(stringResource(R.string.cancel)) } },
                    )
                }
            }
            state.pendingDraft?.takeIf { !state.isLoading }?.let { draft ->
                var confirmDiscard by rememberSaveable(draft.sessionId, draft.index) { mutableStateOf(false) }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                        Text(stringResource(R.string.session_draft_title), modifier = Modifier.semantics { heading() })
                        Text(stringResource(when {
                            state.recoveryRequired -> R.string.session_recovery_required
                            state.draftStorageFailed -> R.string.session_draft_storage_error
                            state.draftCanRetry -> R.string.session_draft_retry_message
                            state.resumable?.progress?.sessionId == draft.sessionId -> R.string.session_draft_resume_message
                            else -> R.string.session_draft_conflict_message
                        }))
                        draft.answer?.let { Text(stringResource(R.string.your_answer, it.toString())) }
                        if (draft.preAnswer.isNotBlank()) Text(draft.preAnswer)
                        if (state.draftCanRetry) Button(viewModel::retryPendingDraft,
                            enabled = !state.isLoading && !state.recoveryRequired && !state.draftStorageFailed) { Text(stringResource(R.string.retry)) }
                        AppDangerOutlinedButton({ confirmDiscard = true }, enabled = !state.isLoading) { Text(stringResource(R.string.ux_discard_answer_draft)) }
                    }
                }
                if (confirmDiscard) AlertDialog(onDismissRequest = { confirmDiscard = false },
                    title = { Text(stringResource(R.string.session_draft_discard_title)) },
                    text = { Text(stringResource(R.string.session_draft_discard_message)) },
                    confirmButton = { AppDangerTextButton({ confirmDiscard = false; viewModel.discardLocalDraft() }, enabled = !state.isLoading) { Text(stringResource(R.string.ux_discard_answer_draft)) } },
                    dismissButton = { TextButton({ confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
                )
            }

            if (state.launchPreset != null && state.session == null && state.results == null) {
                Text(stringResource(R.string.flow_target_retained))
                SelectionRow {
                    OutlinedButton({ retryLaunch = true }, enabled = !state.isLoading && !state.recoveryRequired && state.resumable == null) { Text(stringResource(R.string.flow_practice)) }
                    TextButton(viewModel::discardLaunchPreset, enabled = !state.isLoading) { Text(stringResource(R.string.ux_clear_practice_selection)) }
                }
            }
            when {
                state.results != null -> ResultsContent(
                    results = requireNotNull(state.results),
                    reviewLoading = state.reviewLoading,
                    reviewFailed = state.reviewFailed,
                    reviewError = state.reviewError,
                    onRefreshReview = viewModel::refreshCompletedReview,
                    imageRepository = imageRepository,
                    onNewSession = viewModel::returnToSetup,
                    maximum = state.maxQuizQuestions,
                    canPractice = !state.isLoading && !state.recoveryRequired && !state.finishStorageFailed && !state.syncing,
                    categories = state.categories.map { it.id }.toSet(),
                    onPractice = { mode, ids, label -> viewModel.practiceResults(com.mukhtabir.android.core.QuizPreset(mode, ids, label)) },
                    onCategory = viewModel::studyResultCategory,
                    onDone = onBack,
                )
                state.questionPage != null -> QuestionContent(state.copy(isLoading = state.isLoading || state.locallyFinished || state.recoveryRequired || state.pendingDraft != null || state.draftStorageFailed ||
                    (state.mode != TestMode.RECALL && !state.downloaded) ||
                    (state.syncConflict != null && state.syncConflict?.index == state.questionPage?.index)), imageRepository, viewModel)
                state.setupLoaded -> SetupContent(state.copy(isLoading = state.isLoading || state.recoveryRequired || state.draftStorageFailed), canUseBlueprint, viewModel)
            }
        }
        if (state.isLoading && state.questionPage != null) {
            AppLoadingIndicator(Modifier.align(Alignment.TopEnd), compact = true)
        }
        }
        if (sessionActive && state.questionPage != null) SessionNavigation(sessionUi, viewModel)
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun SetupContent(
    state: QuizUiState,
    canUseBlueprint: Boolean,
    viewModel: QuizViewModel,
) {
    var bookContents by rememberSaveable { mutableStateOf(false) }
    if (bookContents) BookContentsDialog(state, viewModel, onDismiss = { bookContents = false })
    var confirmDiscard by rememberSaveable(state.resumable?.progress?.sessionId) { mutableStateOf(false) }
    if (confirmDiscard) AlertDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text(stringResource(R.string.discard_saved_session_title)) },
        text = { Text(stringResource(R.string.discard_saved_session_message)) },
        confirmButton = { AppDangerTextButton({ confirmDiscard = false; viewModel.discardSaved() }, enabled = !state.isLoading) { Text(stringResource(R.string.ux_discard_saved_session)) } },
        dismissButton = { TextButton({ confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
    )
    state.resumable?.let { saved ->
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.cardInsets),
                verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap),
            ) {
                Text(stringResource(R.string.quiz_saved_mode, modeLabel(saved.mode)),
                    style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                val total = saved.progress.questionIds.size
                val answered = saved.progress.answers.entries.count { (index, slot) ->
                    val answer = if (slot is kotlinx.serialization.json.JsonObject) slot["answer"] else slot
                    index.toIntOrNull()?.let { it in 0 until total } == true &&
                        (answer as? kotlinx.serialization.json.JsonPrimitive)?.content?.toIntOrNull()?.let { it > 0 } == true
                }
                Text(stringResource(R.string.session_position,
                    com.mukhtabir.android.util.formatNumber((saved.progress.currentIndex + 1).coerceIn(1, total.coerceAtLeast(1))),
                    com.mukhtabir.android.util.formatNumber(total)), style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.session_answered_progress,
                    com.mukhtabir.android.util.formatNumber(answered), com.mukhtabir.android.util.formatNumber(total)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LinearProgressIndicator(progress = if (total > 0) answered.toFloat() / total else 0f,
                    modifier = Modifier.fillMaxWidth())
                val pending = if (state.pendingDraft?.sessionId == saved.progress.sessionId) 1 else 0
                Text(if (state.draftStorageFailed) stringResource(R.string.quiz_resume_pending_unknown)
                    else stringResource(R.string.quiz_resume_pending, com.mukhtabir.android.util.formatNumber(pending)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                SelectionRow {
                    Button(onClick = viewModel::resumeSaved, enabled = !state.isLoading) {
                        Text(stringResource(R.string.quiz_continue_session))
                    }
                    AppDangerTextButton(onClick = { confirmDiscard = true }, enabled = !state.isLoading) {
                        Text(stringResource(R.string.ux_discard_saved_session))
                    }
                }
            }
        }
    }

    SetupStepCard(stringResource(R.string.quiz_mode),
        subtitle = modeLabel(state.mode)) {
        SelectionRow {
            ModeButton(TestMode.STUDY, R.string.study, state, viewModel)
            ModeButton(TestMode.EXAM, R.string.exam, state, viewModel)
            ModeButton(TestMode.RECALL, R.string.recall, state, viewModel)
        }
    }
    val bankPool = state.source in setOf(QuizSource.BOOK, QuizSource.TAG, QuizSource.CATEGORY)
    val selectedBook = state.selectedBook.takeIf { bankPool }
    val bookExists = selectedBook == null || state.books.any { it.name == selectedBook }
    val scopedFilters = state.selectedCategoryIds.isNotEmpty() || state.selectedTags.isNotEmpty()
    val bookFiltersReady = selectedBook == null || !scopedFilters ||
        (state.bookFiltersFor == selectedBook && !state.bookFiltersLoading && state.bookFiltersError == null)
    val extraTags = state.additionalTags.split(',').map { it.trim() }.filter { it.isNotEmpty() }
    val selectedTagCount = (state.selectedTags + extraTags).distinct().size
    val hasFilters = scopedFilters || extraTags.isNotEmpty()
    val poolLabel = stringResource(when (state.source) {
        QuizSource.BOOK, QuizSource.TAG, QuizSource.CATEGORY -> R.string.quiz_pool_all
        QuizSource.BOOKMARKS -> R.string.bookmarks
        QuizSource.SRS -> R.string.source_srs
        QuizSource.BLUEPRINT -> R.string.source_blueprint
        null -> R.string.choose_source
    })
    SetupStepCard(stringResource(R.string.quiz_pool_title), subtitle = poolLabel) {
        state.metadataError?.let {
            ErrorState(throwable = it, onRetry = if (state.metadataLoading || state.isLoading) null else viewModel::refreshMetadata)
        }
        SelectionRow {
            SourceButton(QuizSource.BOOK, R.string.quiz_pool_all, state, viewModel, true)
            SourceButton(QuizSource.BOOKMARKS, R.string.bookmarks, state, viewModel, state.bookmarkCount > 0)
            SourceButton(QuizSource.SRS, R.string.source_srs, state, viewModel, state.srsDueCount > 0)
            if (canUseBlueprint) SourceButton(QuizSource.BLUEPRINT, R.string.source_blueprint,
                state, viewModel, state.blueprints.isNotEmpty())
        }
        when (state.source) {
            QuizSource.BOOKMARKS -> if (state.metadataError == null) Text(stringResource(R.string.source_bookmarks_count, state.bookmarkCount))
            QuizSource.SRS -> if (state.metadataError == null) Text(stringResource(R.string.source_srs_count, state.srsDueCount))
            QuizSource.BLUEPRINT -> SetupPicker(title = stringResource(R.string.select_blueprint),
                options = state.blueprints.map { it.id to it.name },
                selected = state.selectedBlueprintId?.let(::setOf).orEmpty(),
                onToggle = viewModel::selectBlueprint, enabled = !state.isLoading)
            else -> Unit
        }
    }
    if (bankPool) {
        OutlinedButton({ bookContents = true }, enabled = !state.isLoading) { Text(stringResource(R.string.flow_book_contents)) }
        AppSection(title = stringResource(R.string.quiz_optional_book),
            subtitle = selectedBook ?: stringResource(R.string.quiz_no_book_restriction), initiallyExpanded = false) {
            Text(stringResource(R.string.quiz_book_hint), style = MaterialTheme.typography.bodySmall)
            SelectionRow {
                SetupChoiceChip(selected = selectedBook == null, enabled = !state.isLoading,
                    onClick = viewModel::useWholeBank, text = stringResource(R.string.quiz_no_book_restriction))
            }
            SetupPicker(title = stringResource(R.string.select_source_book),
                options = state.books.map { it.name to "${com.mukhtabir.android.util.bidi(it.name)} (${com.mukhtabir.android.util.formatNumber(it.count)})" },
                selected = selectedBook?.let(::setOf).orEmpty(), onToggle = viewModel::selectBook,
                enabled = !state.isLoading)
        }
        if (!bookExists) Text(stringResource(R.string.quiz_book_unavailable), color = MaterialTheme.colorScheme.error)
    }
    val filtersHint = if (selectedBook != null) stringResource(R.string.quiz_scoped_filters_optional, com.mukhtabir.android.util.bidi(selectedBook))
        else stringResource(R.string.quiz_pool_filters_optional)
    val selectionHint = if (!hasFilters) filtersHint else filtersHint + "\n" +
        stringResource(R.string.quiz_filters_selected, state.selectedCategoryIds.size, selectedTagCount)
    ActiveFilterSummary(state, viewModel, selectedBook)
    if (selectedBook != null && state.bookFiltersLoading) AppLoadingIndicator()
    if (selectedBook != null) state.bookFiltersError?.let {
        ErrorState(throwable = it, title = stringResource(R.string.quiz_optional_filters),
            onRetry = if (state.isLoading || state.bookFiltersLoading) null else viewModel::refreshBookFilters)
    }
    AppSection(title = stringResource(R.string.quiz_optional_filters), subtitle = selectionHint, initiallyExpanded = false) {
        Text(stringResource(R.string.quiz_filter_matching), style = MaterialTheme.typography.bodySmall)
        val pickerReady = selectedBook == null || (state.bookFiltersFor == selectedBook &&
            !state.bookFiltersLoading && state.bookFiltersError == null)
        if (pickerReady) {
            val categories = if (selectedBook == null) state.categories else state.bookCategories
            val tags = if (selectedBook == null) state.tags else state.bookTags
            if (selectedBook != null && categories.isEmpty()) Text(stringResource(R.string.quiz_book_no_categories))
            SetupPicker(title = stringResource(R.string.select_categories), options = categories.map { it.id to it.name },
                selected = state.selectedCategoryIds, onToggle = viewModel::toggleCategory, enabled = !state.isLoading)
            if (selectedBook != null && tags.isEmpty()) Text(stringResource(R.string.quiz_book_no_tags))
            SetupPicker(title = stringResource(R.string.ux_tags_any), options = tags.map { it.name to it.name },
                selected = state.selectedTags, onToggle = viewModel::toggleTag, enabled = !state.isLoading)
        }
        // Preserve legacy free-text filters visibly, including after pool/book changes.
        if (state.additionalTags.isNotBlank()) OutlinedTextField(value = state.additionalTags,
            onValueChange = viewModel::setAdditionalTags, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.additional_tags)) },
            supportingText = { Text(stringResource(R.string.comma_separated)) }, enabled = !state.isLoading)
        if (hasFilters) OutlinedButton(onClick = viewModel::clearFilters, enabled = !state.isLoading) {
            Text(stringResource(R.string.quiz_clear_filters))
        }
    }
    AppSection(title = stringResource(R.string.quiz_other_options), initiallyExpanded = false,
        subtitle = stringResource(R.string.quiz_refinement_summary,
            stringResource(when (state.difficulty) { "easy" -> R.string.difficulty_easy; "medium" -> R.string.difficulty_medium;
                "hard" -> R.string.difficulty_hard; else -> R.string.difficulty_any }),
            if (state.verifiedOnly) stringResource(R.string.verified_only) else stringResource(R.string.quiz_verification_any)) +
            (if (state.source != QuizSource.SRS) " · " + stringResource(when (state.selectionStrategy) {
                "balanced" -> R.string.selection_balanced; "review" -> R.string.selection_review; else -> R.string.selection_coverage
            }) else "")) {
        Text(stringResource(R.string.difficulty))
        SelectionRow {
            DifficultyButton(null, R.string.difficulty_any, state, viewModel)
            DifficultyButton("easy", R.string.difficulty_easy, state, viewModel)
            DifficultyButton("medium", R.string.difficulty_medium, state, viewModel)
            DifficultyButton("hard", R.string.difficulty_hard, state, viewModel)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = state.verifiedOnly, onCheckedChange = viewModel::setVerifiedOnly, enabled = !state.isLoading)
            Text(stringResource(R.string.verified_only))
        }
        if (state.source != QuizSource.SRS) {
            Text(stringResource(R.string.selection_strategy))
            SelectionRow {
                listOf("coverage" to R.string.selection_coverage, "balanced" to R.string.selection_balanced,
                    "review" to R.string.selection_review).forEach { (value, label) ->
                    SetupChoiceChip(selected = state.selectionStrategy == value,
                        onClick = { viewModel.setSelectionStrategy(value) }, enabled = !state.isLoading,
                        text = stringResource(label))
                }
            }
        }
    }
    val count = state.requestedCount.toIntOrNull()
    val effectiveMaximum = minOf(state.availableCount, state.maxQuizQuestions)
    val sourceReady = when (state.source) {
        QuizSource.BOOK, QuizSource.TAG, QuizSource.CATEGORY -> bookExists
        QuizSource.BOOKMARKS -> state.bookmarkCount > 0
        QuizSource.SRS -> state.srsDueCount > 0
        QuizSource.BLUEPRINT -> canUseBlueprint && state.blueprints.any { it.id == state.selectedBlueprintId && it.isActive }
        null -> false
    }
    val countValid = count != null && count in 1..effectiveMaximum
    val blockedHint = when {
        state.recoveryRequired || state.pendingDraft != null || state.draftStorageFailed -> stringResource(R.string.session_recovery_required)
        state.isLoading -> null
        state.metadataError != null -> null // The pool section shows the failed read and Retry.
        !bookExists -> stringResource(R.string.quiz_book_unavailable)
        !sourceReady -> stringResource(R.string.quiz_select_pool_item)
        !bookFiltersReady -> stringResource(R.string.quiz_filters_need_review)
        state.isCountLoading -> stringResource(R.string.checking_available)
        state.countError != null -> null // The count section owns its error and Retry.
        state.error != null || state.hasUnknownError -> state.error ?: stringResource(R.string.error_unknown)
        state.availableCount <= 0 -> stringResource(R.string.quiz_no_matching_questions)
        !countValid -> stringResource(R.string.quiz_count_range, effectiveMaximum)
        else -> null
    }
    SetupStepCard(stringResource(R.string.number_of_questions)) {
        state.countError?.let {
            ErrorState(throwable = it, onRetry = if (state.isLoading || state.isCountLoading) null else viewModel::refreshAvailableCount)
        }
        OutlinedTextField(value = state.requestedCount, onValueChange = viewModel::setRequestedCount,
            modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.number_of_questions)) },
            supportingText = { if (state.countError == null && state.metadataError == null) Text(if (state.isCountLoading) stringResource(R.string.checking_available)
                else stringResource(R.string.available_questions_with_limit, state.availableCount, state.maxQuizQuestions)) },
            isError = !state.isCountLoading && state.availableCount > 0 && !countValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), enabled = !state.isLoading,
            singleLine = true, textStyle = com.mukhtabir.android.util.ltrInputStyle())
        blockedHint?.let { Text(it, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Button(onClick = viewModel::start, modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && state.pendingDraft == null && !state.draftStorageFailed &&
                !state.recoveryRequired && sourceReady && bookFiltersReady && !state.isCountLoading && state.countError == null && countValid) {
            Text(stringResource(when (state.mode) { TestMode.EXAM -> R.string.start_exam;
                TestMode.STUDY -> R.string.start_study; TestMode.RECALL -> R.string.start_recall }))
        }
    }
}

/** Setup sections stay stacked and visible in their selection order. */
@Composable
private fun SetupStepCard(title: String, subtitle: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.cardInsets),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun SelectionRow(content: @Composable () -> Unit) {
    AppActionRow(Modifier.fillMaxWidth()) { content() }
}

@Composable
private fun ModeButton(mode: TestMode, label: Int, state: QuizUiState, viewModel: QuizViewModel) {
    SetupChoiceChip(
        selected = state.mode == mode,
        enabled = !state.isLoading,
        onClick = { viewModel.setMode(mode) },
        text = stringResource(label),
    )
}

@Composable
private fun SourceButton(
    source: QuizSource,
    label: Int,
    state: QuizUiState,
    viewModel: QuizViewModel,
    available: Boolean,
) {
    SetupChoiceChip(
        selected = state.source == source,
        enabled = available && !state.isLoading,
        onClick = { viewModel.setSource(source) },
        text = stringResource(label),
    )
}

@Composable
private fun DifficultyButton(
    difficulty: String?,
    label: Int,
    state: QuizUiState,
    viewModel: QuizViewModel,
) {
    SetupChoiceChip(
        selected = state.difficulty == difficulty,
        enabled = !state.isLoading,
        onClick = { viewModel.setDifficulty(difficulty) },
        text = stringResource(label),
    )
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
private fun SetupChoiceChip(selected: Boolean, enabled: Boolean, onClick: () -> Unit, text: String) {
    FilterChip(selected = selected, enabled = enabled, onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelLarge) })
}

@Composable
internal fun SelectableButton(
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    text: String,
    markdown: Boolean = false,
    modifier: Modifier = Modifier,
    accessibilityLabel: String? = null,
    choiceNumber: Int? = null,
    answerCorrect: Boolean? = null,
    neutralSelection: Boolean = false,
) {
    val selectionModifier = modifier.heightIn(min = com.mukhtabir.android.ui.components.AppLayoutTokens.touchTarget).semantics {
        this.selected = selected
        accessibilityLabel?.let { contentDescription = it }
    }
    if (choiceNumber != null) {
        com.mukhtabir.android.ui.components.AnswerChoice(
            number = choiceNumber, text = text, modifier = modifier,
            selected = selected, enabled = enabled, onClick = onClick, answerCorrect = answerCorrect,
            neutralSelection = neutralSelection, markdown = markdown, accessibilityLabel = accessibilityLabel)
    } else if (selected) {
        Button(onClick = onClick, enabled = enabled, modifier = selectionModifier) {
            if (markdown) MarkdownText(text, Modifier.weight(1f), interactive = false) else Text(text)
        }
    } else {
        OutlinedButton(onClick = onClick, enabled = enabled, modifier = selectionModifier) {
            if (markdown) MarkdownText(text, Modifier.weight(1f), interactive = false) else Text(text)
        }
    }
    answerCorrect?.takeIf { choiceNumber == null }?.let { correct ->
        Text(stringResource(if (correct) R.string.correct_answer else R.string.incorrect_answer),
            color = if (correct) com.mukhtabir.android.ui.theme.AppTheme.feedback.success else MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun <T> SetupPicker(
    title: String,
    options: List<Pair<T, String>>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    enabled: Boolean,
) {
    Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
    SelectionRow {
        options.forEach { (value, label) ->
            SetupChoiceChip(selected = value in selected, enabled = enabled,
                onClick = { onToggle(value) }, text = label)
        }
    }
}

@Composable
private fun QuestionContent(
    state: QuizUiState,
    imageRepository: ImageRepository,
    viewModel: QuizViewModel,
) {
    val nativeView = androidx.compose.ui.platform.LocalView.current

    val page = requireNotNull(state.questionPage)
    var explanationShown by remember(state.session?.sessionId, page.question.id) { mutableStateOf(false) }
    var readingLocale by rememberSaveable(state.session?.sessionId) { mutableStateOf<String?>(null) }
    val showExplanation = state.mode.isLearningMode() && (state.showFeedback || state.locallyFinished)
    val localized = page.question.localizedForDisplay(
        localeTag = readingLocale ?: Locale.getDefault().toLanguageTag(),
        feedbackTranslations = state.feedbackTranslations,
        feedbackExplanation = state.explanation,
        showExplanation = showExplanation,
    )
    QuestionLanguageSelector(page.question.translations, page.question.choices.size, readingLocale,
        { readingLocale = it }, hasCase = page.question.case?.stem?.isNotBlank() == true, caseTranslations = page.question.case?.translations ?: kotlinx.serialization.json.JsonObject(emptyMap()),
        explanationAvailable = showExplanation && (!state.explanation.isNullOrBlank() || !page.question.explanation.isNullOrBlank()),
        explanationTranslated = !localized.explanation.isNullOrBlank(), action = { QuestionBookmark(state,viewModel) })
    TextButton(viewModel::toggleFlag, enabled = !state.isLoading) {
        Text(stringResource(if (page.index in state.flaggedIndices) R.string.flow_unflag else R.string.flow_flag))
    }
    if (state.flagError) {
        Text(stringResource(R.string.flow_flag_error), color = MaterialTheme.colorScheme.error)
        TextButton(viewModel::retryFlags) { Text(stringResource(R.string.retry)) }
    }
    ClinicalCaseContext(localized.case)
    com.mukhtabir.android.ui.components.QuestionTextCard(localized.text, Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.titleMedium)
    if (page.question.id in state.bookmarkErrors) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.quiz_bookmark_error), Modifier.weight(1f).semantics { liveRegion = LiveRegionMode.Polite },
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            TextButton({ viewModel.refreshQuestionBookmark(page.question.id, force = true) },
                enabled = page.question.id !in state.bookmarkBusyIds) { Text(stringResource(R.string.retry)) }
        }
    }
    QuestionImage(page.question.imageUrl, imageRepository,
        downloadedImage = viewModel.downloadedImage(page.question.imageUrl))

    if (state.mode == TestMode.RECALL && !state.choicesRevealed) {
        Text(stringResource(R.string.recall_instruction))
        OutlinedTextField(
            value = state.preAnswer,
            onValueChange = viewModel::updatePreAnswer,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.recall_your_answer)) },
            minLines = 3,
            enabled = !state.isLoading,
        )
        Button(
            onClick = viewModel::revealRecallChoices,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.preAnswer.isNotBlank() && !state.isLoading,
        ) { Text(stringResource(R.string.reveal_choices)) }
    } else {
        localized.choices.forEachIndexed { index, choice ->
            val answer = index + 1
            SelectableButton(
                selected = state.selectedAnswer == answer,
                enabled = !state.isLoading && !(state.mode.isLearningMode() && state.showFeedback),
                onClick = {
                    if (viewModel.selectAnswer(answer)) {
                        if (viewModel.soundEnabled) nativeView.playSoundEffect(android.view.SoundEffectConstants.CLICK)
                        if (viewModel.hapticsEnabled) com.mukhtabir.android.ui.components.performAppHaptic(nativeView)
                    }
                },
                text = choice,
                choiceNumber = answer,
                neutralSelection = state.mode == TestMode.EXAM,
                answerCorrect = when {
                    !showExplanation -> null
                    page.question.correctAnswer == answer -> true
                    state.selectedAnswer == answer -> state.isCorrect
                    else -> null
                },
                markdown = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (state.mode.isLearningMode()) com.mukhtabir.android.ui.components.BilingualQuestionComparison(
        com.mukhtabir.android.data.remote.QuestionDto(id = page.question.id, question = page.question.text,
            choices = page.question.choices, translations = page.question.translations, case = page.question.case,
            explanation = state.explanation ?: page.question.explanation),
        choicesVisible = state.mode != TestMode.RECALL || state.choicesRevealed,
        explanationVisible = showExplanation, feedbackTranslations = state.feedbackTranslations)

    if (showExplanation) {
        localized.explanation?.takeIf { it.isNotBlank() }?.let {
            StudyExplanation(it, page.question.id, explanationShown) { explanationShown = true }
        }
    }

    if (state.mode.isLearningMode() && state.selectedAnswer != null) {
        androidx.compose.runtime.key(page.question.id) {
            AppSection(stringResource(R.string.confidence_prompt),
                subtitle = stringResource(when (state.confidence ?: 3) {
                    1 -> R.string.confidence_guessing
                    2 -> R.string.confidence_uncertain
                    else -> R.string.confidence_certain
                })) {
                SelectionRow {
                    ConfidenceButton(1, R.string.confidence_guessing, state, viewModel)
                    ConfidenceButton(2, R.string.confidence_uncertain, state, viewModel)
                    ConfidenceButton(3, R.string.confidence_certain, state, viewModel)
                }
            }
        }
    }

    if (state.mode.isLearningMode() && state.isCorrect == false) {
        androidx.compose.runtime.key(page.question.id) {
            AppSection(stringResource(R.string.reflection_title),
                subtitle = stringResource(when (state.errorReason ?: "unknown") {
                    "misread" -> R.string.reflection_misread
                    "confused" -> R.string.reflection_confused
                    "guessed" -> R.string.reflection_guessed
                    else -> R.string.reflection_unknown
                })) {
                ReflectionButtons(viewModel, state.isLoading || state.errorReason != null, state.errorReason ?: "unknown")
            }
        }
    }


}

@Composable
private fun ConfidenceButton(
    value: Int,
    label: Int,
    state: QuizUiState,
    viewModel: QuizViewModel,
) {
    SelectableButton(
        selected = state.confidence == value,
        enabled = !state.isLoading,
        onClick = { viewModel.selectConfidence(value) },
        text = stringResource(label),
    )
}

@Composable
private fun ReflectionButtons(viewModel: QuizViewModel, disabled: Boolean, selectedReason: String) {
    val reasons = listOf(
        "unknown" to R.string.reflection_unknown,
        "misread" to R.string.reflection_misread,
        "confused" to R.string.reflection_confused,
        "guessed" to R.string.reflection_guessed,
    )
    reasons.forEach { (reason, label) ->
        SelectableButton(selected = selectedReason == reason, enabled = !disabled,
            onClick = { viewModel.recordReflection(reason) }, text = stringResource(label),
            modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun ResultsContent(
    results: SessionResultsDto,
    reviewLoading: Boolean,
    reviewFailed: Boolean,
    reviewError: String?,
    onRefreshReview: () -> Unit,
    imageRepository: ImageRepository,
    onNewSession: () -> Unit,
    maximum: Int,
    canPractice: Boolean,
    categories: Set<Long>,
    onPractice: (TestMode, List<Long>, String) -> Unit,
    onCategory: (Long) -> Unit,
    onDone: () -> Unit,
) {
    var practiceIds by remember { mutableStateOf<List<Long>?>(null) }
    practiceIds?.let { ids -> com.mukhtabir.android.ui.components.PracticeLauncher(stringResource(R.string.results), ids,
        maximum, enabled = canPractice, onDismiss = { practiceIds = null },
        onStart = { mode, selected, label -> practiceIds = null; onPractice(mode, selected, label) }) }
    var reviewFilter by rememberSaveable(results.sourceSessionId, results.historyId) { mutableStateOf(ReviewFilter.ALL) }
    AppPageHeading(stringResource(R.string.results))
    ResultsOverview(results)
    if (results.reviewWithheld) {
        Text(stringResource(R.string.quiz_review_withheld))
        if (reviewFailed) Text(reviewError ?: stringResource(R.string.quiz_review_read_failed),
            color = MaterialTheme.colorScheme.error)
        OutlinedButton(onClick = onRefreshReview, enabled = !reviewLoading,
            modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.quiz_refresh_review)) }
    } else {
        val incorrect = results.results.filter { it.userAnswer != null && !it.isCorrect }.map { it.questionId }.filter { it > 0 }.distinct()
        val unanswered = results.results.filter { it.userAnswer == null }.map { it.questionId }.filter { it > 0 }.distinct()
        SelectionRow {
            if (incorrect.isNotEmpty()) Button({ practiceIds = incorrect }, enabled = canPractice) { Text(stringResource(R.string.flow_incorrect)) }
            if (unanswered.isNotEmpty()) OutlinedButton({ practiceIds = unanswered }, enabled = canPractice) { Text(stringResource(R.string.flow_unanswered)) }
        }
        AppSection(stringResource(R.string.flow_category)) {
            results.results.filter { it.categoryId != null }.distinctBy { it.categoryId }.forEach { row ->
                OutlinedButton({ row.categoryId?.let(onCategory) }, enabled = canPractice && row.categoryId?.let { it in categories } == true) {
                    Text(row.categoryName ?: com.mukhtabir.android.util.formatNumber(requireNotNull(row.categoryId)))
                }
            }
        }
        com.mukhtabir.android.ui.components.AppSectionHeading(stringResource(R.string.review_details))
        val reviews = results.results.withIndex().filter { (_, result) ->
            reviewFilter.includes(result.userAnswer != null, result.isCorrect)
        }
        ResultReviewFilters(reviewFilter, { reviewFilter = it }, reviews.size, results.results.size)
        reviews.forEach { (index, result) ->
            AppSection(title = if (result.userAnswer == null) stringResource(R.string.quiz_review_unanswered,
                com.mukhtabir.android.util.formatNumber(index + 1)) else stringResource(
                    if (result.isCorrect) R.string.review_correct else R.string.review_wrong, index + 1),
                subtitle = stringResource(R.string.your_answer,
                    result.userAnswer?.let { com.mukhtabir.android.util.formatNumber(it) } ?: stringResource(R.string.unanswered)),
                composeCollapsedContent = false) {
                SessionResultReview(index + 1, result, imageRepository, showHeading = false)
            }
        }
        AppSection(stringResource(R.string.quiz_result_breakdowns), composeCollapsedContent = false) {
            ResultBreakdowns(results.results)
        }
    }

    Button(onClick = onNewSession, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.new_session))
    }
    OutlinedButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.done))
    }
}


@Composable
private fun ResultBreakdowns(results: List<SessionResultItemDto>) {
    val categories = results.groupBy { it.categoryName ?: "—" }
    val difficulties = results.groupBy { it.difficulty ?: "—" }
    if (categories.isNotEmpty()) {
        Text(stringResource(R.string.breakdown_category), style = MaterialTheme.typography.titleMedium)
        categories.forEach { (name, items) ->
            Text(stringResource(R.string.breakdown_value, name, items.count { it.isCorrect }, items.size),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
    if (difficulties.isNotEmpty()) {
        Text(stringResource(R.string.breakdown_difficulty), style = MaterialTheme.typography.titleMedium)
        difficulties.forEach { (name, items) ->
            Text(stringResource(R.string.breakdown_value, name, items.count { it.isCorrect }, items.size),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun modeLabel(mode: TestMode): String = stringResource(
    when (mode) {
        TestMode.EXAM -> R.string.exam
        TestMode.STUDY -> R.string.study
        TestMode.RECALL -> R.string.recall
    },
)

internal fun formatDuration(seconds: Int): String {
    return com.mukhtabir.android.util.formatSessionDuration(seconds.toLong())
}
