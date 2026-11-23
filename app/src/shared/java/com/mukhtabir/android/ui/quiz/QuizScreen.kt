package com.mukhtabir.android.ui.quiz

import com.mukhtabir.android.ui.components.AppSectionHeading
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.core.QuizPresetHolder
import com.mukhtabir.android.core.LearnerStorage
import com.mukhtabir.android.data.remote.LearnerBlueprints
import com.mukhtabir.android.data.remote.LearnerBookmarks
import com.mukhtabir.android.data.remote.LearnerCategories
import com.mukhtabir.android.data.remote.LearnerImages
import com.mukhtabir.android.data.remote.LearnerQuestions
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.data.remote.LearnerSessions
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerOutlinedButton
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppRecoveryPanel
import com.mukhtabir.android.ui.components.AppSaveStatus
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.PracticeLauncher
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme

@Composable
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
fun QuizRoute(
    questionService: LearnerQuestions,
    sessionService: LearnerSessions,
    bookmarkService: LearnerBookmarks,
    categoryService: LearnerCategories,
    blueprintService: LearnerBlueprints,
    imageRepository: LearnerImages,
    quizPresetHolder: QuizPresetHolder,
    canUseBlueprint: Boolean,
    maxQuizQuestions: Int,
    userId: Long,
    draftStore: LearnerStorage,
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
    val state by viewModel.state.collectAsLifecycleState()
    var confirmFinishRecovery by remember { mutableStateOf(false) }
    var confirmRecoveredFinish by remember { mutableStateOf(false) }
    var confirmDiscardFinishStorage by remember { mutableStateOf(false) }
    if(confirmDiscardFinishStorage) AppConfirmationDialog(onDismissRequest={confirmDiscardFinishStorage=false},
        title={Text(stringResource(R.string.quiz_finish_recovery_title))},
        text={Text(stringResource(R.string.quiz_finish_storage_discard))},
        confirmButton={AppDangerTextButton({confirmDiscardFinishStorage=false;viewModel.discardUnreadableFinishRecovery()},enabled=!state.isLoading) { Text(stringResource(R.string.ux_discard_finish_recovery)) }},
        dismissButton={AppTextButton({confirmDiscardFinishStorage=false}) { Text(stringResource(R.string.cancel)) }})
    if(confirmFinishRecovery) AppConfirmationDialog(onDismissRequest={confirmFinishRecovery=false},
        title={Text(stringResource(R.string.quiz_finish_recovery_title))},
        text={Text(stringResource(R.string.quiz_finish_review_confirm))},
        confirmButton={AppTextButton({confirmFinishRecovery=false;viewModel.acknowledgeFinishRecovery()},enabled=!state.isLoading) { Text(stringResource(R.string.quiz_finish_review_done)) }},
        dismissButton={AppTextButton({confirmFinishRecovery=false}) { Text(stringResource(R.string.cancel)) }})
    if(confirmRecoveredFinish) AppConfirmationDialog(onDismissRequest={confirmRecoveredFinish=false},
        title={Text(stringResource(R.string.finish_session_title))},
        text={Text(stringResource(R.string.finish_session_message))},
        confirmButton={AppTextButton({confirmRecoveredFinish=false;viewModel.finish()},enabled=!state.isLoading && !state.recoveryRequired) { Text(stringResource(R.string.finish_session_action)) }},
        dismissButton={AppTextButton({confirmRecoveredFinish=false}) { Text(stringResource(R.string.cancel)) }})
    val display by preferences.display.collectAsLifecycleState()
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
    RefreshOnResume {viewModel.refreshMetadata()}
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
    if (confirmPause && sessionActive) AppConfirmationDialog(
        onDismissRequest = { if (!state.isLoading && !state.syncing) confirmPause = false },
        title = { Text(stringResource(R.string.session_pause_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            Text(stringResource(if (state.locallyFinished) R.string.offline_finish_leave
                else if (state.queuedAnswers > 0) R.string.session_pause_sync_first else R.string.session_pause_message))
            if (state.syncing) AppSaveStatus(stringResource(R.string.quiz_syncing))
            if (state.syncError != null || state.error != null) ErrorState(throwable=state.syncError,message=state.error)
            if (!sessionUi.canRequestPause && !state.isLoading && !state.syncing) {
                Text(stringResource(R.string.session_pause_recovery_required))
            }
        } },
        confirmButton = {
            AppTextButton({ viewModel.pauseAndExit { confirmPause = false; onBack() } }, enabled = sessionUi.canRequestPause) {
                Text(stringResource(if(state.queuedAnswers>0 && !state.locallyFinished)
                    R.string.session_sync_pause_leave else R.string.pause_and_leave))
            }
        },
        dismissButton = { AppTextButton({ confirmPause = false },enabled=!state.isLoading && !state.syncing) { Text(stringResource(R.string.cancel)) } },
    )

    // Consume any preset dropped by the Home / Mistakes / Fragile
    // screens. The holder's StateFlow is the signal: as long as a
    // preset is pending, the effect fires; consuming it clears the
    // slot, the flow emits null, and the effect ends. That handles
    // the case where this composable is restored (rather than
    // re-created) by the tab-navigation save/restore pattern.
    var retryLaunch by remember { mutableStateOf(false) }
    if (retryLaunch) state.launchPreset?.let { pending ->
        PracticeLauncher(pending.sessionLabel, pending.questionIds, state.maxQuizQuestions,
            enabled = !state.isLoading && !state.recoveryRequired && state.resumable == null && state.session == null,
            initialMode = pending.mode, onDismiss = { retryLaunch = false }, onStart = { mode, ids, title ->
                retryLaunch = false; viewModel.startPreset(com.mukhtabir.android.core.QuizPreset(mode, ids, title))
            })
    }
    val pendingSetupMode by quizPresetHolder.setupMode.collectAsLifecycleState()
    LaunchedEffect(pendingSetupMode, state.setupLoaded, state.isLoading, state.recoveryRequired, state.session, state.results, state.pendingDraft, state.draftStorageFailed) {
        if (pendingSetupMode != null && state.setupLoaded && !state.isLoading && !state.recoveryRequired &&
            state.session == null && state.results == null && state.pendingDraft == null && !state.draftStorageFailed) {
            quizPresetHolder.consumeSetupMode()?.let(viewModel::setMode)
        }
    }
    val pendingPreset by quizPresetHolder.preset.collectAsLifecycleState()
    LaunchedEffect(pendingPreset, state.setupLoaded, state.isLoading, state.recoveryRequired, state.session, state.results, state.pendingDraft, state.draftStorageFailed) {
        if (pendingPreset != null && state.setupLoaded && !state.isLoading && !state.recoveryRequired
            && state.session == null && state.results == null && state.pendingDraft == null && !state.draftStorageFailed) {
            quizPresetHolder.consume()?.let(viewModel::startPreset)
        }
    }

    androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
    val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    val pinSetup = state.session == null && state.results == null && state.questionPage == null && state.setupLoaded &&
        maxHeight >= AppLayoutTokens.sessionPinnedMinHeight * scale
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap),
    ) {
        if (sessionActive) SessionHeader(sessionUi, viewModel.clock) {
            SessionToolsMenu(state, sessionUi, viewModel, preferences, onPause = { confirmPause = true })
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(contentScroll),
            verticalArrangement = Arrangement.spacedBy(if (sessionActive) AppTheme.spacing.content else AppTheme.spacing.section),
        ) {
            state.syncConflict?.let { conflict ->
                Text(stringResource(R.string.session_sync_conflict, com.mukhtabir.android.util.formatNumber(conflict.index + 1), com.mukhtabir.android.util.formatNumber(state.syncConflictAnswer ?: 0), com.mukhtabir.android.util.formatNumber(conflict.savedAnswer ?: 0)))
                QuizSelectionRow {
                    AppOutlinedButton(onClick = { viewModel.resolveDownloadedConflict(true) }, enabled = !state.isLoading) {
                        Text(stringResource(R.string.session_keep_device))
                    }
                    AppOutlinedButton(onClick = { viewModel.resolveDownloadedConflict(false) }, enabled = !state.isLoading) {
                        Text(stringResource(R.string.session_keep_server))
                    }
                }
            }
            if (sessionActive && state.draftStorageFailed && state.pendingDraft == null) {
                AppRecoveryPanel(
                    message=stringResource(R.string.quiz_finish_storage_error),
                    onRetry=viewModel::retryDownloadedStorage,retryEnabled=!state.isLoading)
            }
            if (sessionActive && state.mode != TestMode.RECALL) {
                if (!state.downloaded) {
                    AppRecoveryPanel(
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
                AppStatus(
                    state.error ?: stringResource(R.string.error_unknown),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
                if (state.recoveryRequired) Text(stringResource(R.string.session_recovery_required))
                if (state.recoveryRequired || (state.questionPage == null && state.results == null)) {
                    AppOutlinedButton(onClick = viewModel::initialize, enabled = !state.isLoading) {
                        Text(stringResource(R.string.retry))
                    }
                }
            }

            if(state.finishStorageFailed) {
                AppRecoveryPanel(
                    message=stringResource(R.string.quiz_finish_storage_error),
                    onRetry=viewModel::retryFinishStorage,retryEnabled=!state.isLoading)
                if(state.finishRecovery==null) AppDangerOutlinedButton({confirmDiscardFinishStorage=true},enabled=!state.isLoading) {
                    Text(stringResource(R.string.ux_discard_finish_recovery))
                }
            }
            if(state.finishPending) {
                AppSectionHeading(stringResource(R.string.quiz_finish_recovery_title))
                Text(stringResource(if(state.finishStatusChecked && state.finishedSessionMissing)
                    R.string.quiz_finish_session_missing else R.string.quiz_finish_uncertain))
                AppOutlinedButton(viewModel::initialize,enabled=!state.isLoading) { Text(stringResource(R.string.refresh)) }
                onOpenHistory?.let { history ->
                    AppOutlinedButton(history,enabled=!state.isLoading) { Text(stringResource(R.string.history)) }
                }
                if(state.finishStatusChecked && !state.finishStorageFailed) AppOutlinedButton({confirmFinishRecovery=true},enabled=!state.isLoading) {
                    Text(stringResource(R.string.quiz_finish_review_done))
                }
            }
            if(state.session!=null && state.questionPage==null && !state.finishStorageFailed && !state.finishPending && !state.recoveryRequired && state.results==null) {
                AppOutlinedButton({confirmRecoveredFinish=true},enabled=!state.isLoading) { Text(stringResource(R.string.finish)) }
            }
            if (state.draftStorageFailed) {
                AppRecoveryPanel(
                    message=stringResource(R.string.session_draft_storage_error),
                    onRetry=viewModel::retryDraftStorage,retryEnabled=!state.isLoading)
                if (state.pendingDraft == null) {
                    // A malformed draft may not decode, but explicit discard can still
                    // remove its encrypted value without interpreting it.
                    var confirmUnreadableDiscard by rememberSaveable(userId) { mutableStateOf(false) }
                    AppDangerOutlinedButton({ confirmUnreadableDiscard = true }, enabled = !state.isLoading) {
                        Text(stringResource(R.string.ux_discard_answer_draft))
                    }
                    if (confirmUnreadableDiscard) AppConfirmationDialog(
                        onDismissRequest = { confirmUnreadableDiscard = false },
                        title = { Text(stringResource(R.string.session_draft_discard_title)) },
                        text = { Text(stringResource(R.string.session_draft_discard_message)) },
                        confirmButton = { AppDangerTextButton({ confirmUnreadableDiscard = false; viewModel.discardLocalDraft() }, enabled = !state.isLoading) { Text(stringResource(R.string.ux_discard_answer_draft)) } },
                        dismissButton = { AppTextButton({ confirmUnreadableDiscard = false }) { Text(stringResource(R.string.cancel)) } },
                    )
                }
            }
            state.pendingDraft?.takeIf { !state.isLoading }?.let { draft ->
                var confirmDiscard by rememberSaveable(draft.sessionId, draft.index) { mutableStateOf(false) }
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
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
                        if (state.draftCanRetry) AppButton(viewModel::retryPendingDraft,
                            enabled = !state.isLoading && !state.recoveryRequired && !state.draftStorageFailed) { Text(stringResource(R.string.retry)) }
                        AppDangerOutlinedButton({ confirmDiscard = true }, enabled = !state.isLoading) { Text(stringResource(R.string.ux_discard_answer_draft)) }
                    }
                }
                if (confirmDiscard) AppConfirmationDialog(onDismissRequest = { confirmDiscard = false },
                    title = { Text(stringResource(R.string.session_draft_discard_title)) },
                    text = { Text(stringResource(R.string.session_draft_discard_message)) },
                    confirmButton = { AppDangerTextButton({ confirmDiscard = false; viewModel.discardLocalDraft() }, enabled = !state.isLoading) { Text(stringResource(R.string.ux_discard_answer_draft)) } },
                    dismissButton = { AppTextButton({ confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
                )
            }

            if (state.launchPreset != null && state.session == null && state.results == null) {
                Text(stringResource(R.string.flow_target_retained))
                QuizSelectionRow {
                    AppOutlinedButton({ retryLaunch = true }, enabled = !state.isLoading && !state.recoveryRequired && state.resumable == null) { Text(stringResource(R.string.flow_practice)) }
                    AppTextButton(viewModel::discardLaunchPreset, enabled = !state.isLoading) { Text(stringResource(R.string.ux_clear_practice_selection)) }
                }
            }
            when {
                state.results != null -> QuizResultsContent(
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
                state.questionPage != null -> QuizQuestionContent(state.copy(isLoading = state.isLoading || state.locallyFinished || state.recoveryRequired || state.pendingDraft != null || state.draftStorageFailed ||
                    (state.mode != TestMode.RECALL && !state.downloaded) ||
                    (state.syncConflict != null && state.syncConflict?.index == state.questionPage?.index)), imageRepository, viewModel)
                state.setupLoaded -> {
                    QuizSetupContent(state.copy(isLoading = state.isLoading || state.recoveryRequired || state.draftStorageFailed), canUseBlueprint, viewModel)
                    if (!pinSetup) QuizSetupActions(state, canUseBlueprint, viewModel)
                }
            }
        }
        if (state.isLoading && state.questionPage != null) {
            AppLoadingIndicator(Modifier.align(Alignment.TopEnd), compact = true)
        }
        }
        if (pinSetup) QuizSetupActions(state, canUseBlueprint, viewModel)
        if (sessionActive && state.questionPage != null) SessionNavigation(sessionUi, viewModel)
    }
    }
}
