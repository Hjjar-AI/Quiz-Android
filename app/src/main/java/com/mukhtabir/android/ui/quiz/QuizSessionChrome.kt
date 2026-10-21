package com.mukhtabir.android.ui.quiz


import androidx.compose.foundation.layout.Column
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.TestMode

/** Stable presentation values; session DTOs and recovery rules stay in the ViewModel. */
internal data class SessionPresentation(
    val sessionId: String?,
    val index: Int,
    val total: Int,
    val answered: Set<Int>,
    val flagged: Set<Int>,
    val seconds: Int,
    val overtime: Boolean,
    val critical: Boolean,
    val canNavigate: Boolean,
    val canPause: Boolean,
    val canRequestPause: Boolean,
    val locallyFinished: Boolean,
    val canFinishLocally: Boolean,
)

internal fun QuizUiState.toSessionPresentation(): SessionPresentation {
    val total = questionPage?.total ?: session?.questionIds?.size ?: 0
    val busy = isLoading || recoveryRequired || draftStorageFailed || (mode != TestMode.RECALL && !downloaded)
    val pauseBlocked = isLoading || recoveryRequired || draftStorageFailed || pendingDraft != null ||
        finishPending || finishStorageFailed || syncConflict != null
    val needsConfidence = mode.isLearningMode() && selectedAnswer != null && confidence == null
    val needsRecallAnswer = mode == TestMode.RECALL && (!choicesRevealed || selectedAnswer == null)
    val durationSeconds = session?.durationMinutes?.toLong()?.times(60)
    return SessionPresentation(
        sessionId = session?.sessionId,
        index = (questionPage?.index ?: session?.currentIndex ?: 0).coerceIn(0, (total - 1).coerceAtLeast(0)),
        total = total.coerceAtLeast(0),
        answered = answeredIndices.filter { it in 0 until total }.toSet(),
        flagged = flaggedIndices.filter { it in 0 until total }.toSet(),
        seconds = timerSeconds,
        overtime = timerOvertime,
        critical = mode == TestMode.EXAM && durationSeconds != null && durationSeconds > 0 &&
            timerSeconds.toLong() * 10 <= durationSeconds,
        canNavigate = !busy && pendingDraft == null && !needsConfidence && !needsRecallAnswer,
        canPause = !pauseBlocked && !syncing && (queuedAnswers == 0 || locallyFinished),
        canRequestPause = !pauseBlocked && !syncing,
        locallyFinished = locallyFinished,
        canFinishLocally = mode == TestMode.STUDY && downloaded && !busy && pendingDraft == null && !locallyFinished,
    )
}

@Composable
internal fun KeepSessionScreenOn(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, active) {
        val previous = view.keepScreenOn
        if (active) view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
}

@Composable
internal fun SessionHeader(ui: SessionPresentation, tools: @Composable () -> Unit) {
    com.mukhtabir.android.ui.components.LearningSessionHeader(ui.index, ui.total, ui.answered.size,
        ui.seconds, ui.overtime, ui.critical, tools)
}

@Composable
internal fun SessionNavigation(ui: SessionPresentation, viewModel: QuizViewModel) {
    var confirmFinish by rememberSaveable(ui.sessionId) { mutableStateOf(false) }
    var showQuestions by rememberSaveable(ui.sessionId) { mutableStateOf(false) }
    if (showQuestions) com.mukhtabir.android.ui.components.AppBottomSheet(
        title = stringResource(R.string.session_question_navigation), onDismiss = { showQuestions = false },
        actions = { TextButton({ showQuestions = false }) { Text(stringResource(R.string.done)) } },
    ) {
        Text(stringResource(R.string.flow_final_counts,
            com.mukhtabir.android.util.formatNumber(ui.answered.size),
            com.mukhtabir.android.util.formatNumber((ui.total - ui.answered.size).coerceAtLeast(0)),
            com.mukhtabir.android.util.formatNumber(ui.flagged.size)))
        Text(stringResource(R.string.flow_map_hint))
        com.mukhtabir.android.ui.components.LearningQuestionChips(ui.index, ui.total, ui.answered, ui.flagged, ui.canNavigate) {
            viewModel.goTo(it); showQuestions = false
        }
    }
    if (confirmFinish) AlertDialog(
        onDismissRequest = { confirmFinish = false },
        title = { Text(stringResource(R.string.finish_session_title)) },
        text = { Column {
            Text(stringResource(R.string.flow_final_review))
            Text(stringResource(R.string.finish_session_message))
            SessionQuestionPicker(ui, viewModel, onSelected = { confirmFinish = false })
        } },
        confirmButton = { com.mukhtabir.android.ui.components.AppButton({ confirmFinish = false; viewModel.next() }, enabled = ui.canNavigate && ui.canPause) { Text(stringResource(R.string.finish)) } },
        dismissButton = { Column {
            if (ui.canFinishLocally) TextButton({ confirmFinish = false; viewModel.finishStudyLocally() }) {
                Text(stringResource(R.string.offline_finish_device))
            }
            TextButton({ confirmFinish = false }) { Text(stringResource(R.string.cancel)) }
        } },
    )
    com.mukhtabir.android.ui.components.LearningSessionNavigation(
        onPrevious = viewModel::previous,
        onQuestions = { showQuestions = true },
        previousEnabled = ui.index > 0 && ui.canNavigate, nextEnabled = ui.canNavigate,
        onNext = { if (ui.index + 1 >= ui.total) {
            if (ui.locallyFinished) viewModel.syncDownloadedAnswers() else confirmFinish = true
        } else viewModel.next() },
        nextLabel = stringResource(if (ui.index + 1 >= ui.total) {
            if (ui.locallyFinished) R.string.offline_sync_finish else R.string.finish
        } else R.string.next),
    )
}



@Composable
internal fun SessionQuestionPicker(ui: SessionPresentation, viewModel: QuizViewModel, onSelected: () -> Unit) {
    com.mukhtabir.android.ui.components.LearningQuestionMap(ui.index, ui.total, ui.answered, ui.flagged, ui.canNavigate) {
        viewModel.goTo(it); onSelected()
    }
}
