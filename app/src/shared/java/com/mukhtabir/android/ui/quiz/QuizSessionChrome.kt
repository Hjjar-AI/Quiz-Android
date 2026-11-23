package com.mukhtabir.android.ui.quiz


import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppBottomSheet
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppSheetSize
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.LearningQuestionChips
import com.mukhtabir.android.ui.components.LearningSessionHeader
import com.mukhtabir.android.ui.components.LearningSessionNavigation
import com.mukhtabir.android.ui.components.collectAsLifecycleState

/** Stable presentation values; session DTOs and recovery rules stay in the ViewModel. */
internal data class SessionPresentation(
    val sessionId: String?,
    val index: Int,
    val total: Int,
    val answered: Set<Int>,
    val flagged: Set<Int>,
    val examDurationSeconds: Long?,
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
        examDurationSeconds = durationSeconds?.takeIf { mode == TestMode.EXAM && it > 0 },
        canNavigate = !busy && pendingDraft == null && !needsConfidence && !needsRecallAnswer,
        canPause = !pauseBlocked && !syncing && (queuedAnswers == 0 || locallyFinished),
        canRequestPause = !pauseBlocked && !syncing,
        locallyFinished = locallyFinished,
        canFinishLocally = mode == TestMode.STUDY && downloaded && !busy && pendingDraft == null && !locallyFinished,
    )
}

@Composable
fun KeepSessionScreenOn(active: Boolean) {
    val view = LocalView.current
    DisposableEffect(view, active) {
        val previous = view.keepScreenOn
        if (active) view.keepScreenOn = true
        onDispose { view.keepScreenOn = previous }
    }
}

internal data class QuizClock(val seconds: Int = 0, val overtime: Boolean = false)

@Composable
internal fun SessionHeader(ui: SessionPresentation, clock: kotlinx.coroutines.flow.StateFlow<QuizClock>, tools: @Composable () -> Unit) {
    val time by clock.collectAsLifecycleState()
    LearningSessionHeader(ui.index, ui.total, ui.answered.size,
        time.seconds, time.overtime,
        ui.examDurationSeconds?.let { time.seconds.toLong() * 10 <= it } == true, tools)
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun SessionNavigation(ui: SessionPresentation, viewModel: QuizViewModel) {
    var confirmFinish by rememberSaveable(ui.sessionId) { mutableStateOf(false) }
    var showQuestions by rememberSaveable(ui.sessionId) { mutableStateOf(false) }
    if (showQuestions) AppBottomSheet(
        title = stringResource(R.string.session_question_navigation), onDismiss = { showQuestions = false },
        size = AppSheetSize.FULL,
        actions = { AppTextButton({ showQuestions = false }) { Text(stringResource(R.string.done)) } },
    ) {
        Text(stringResource(R.string.flow_final_counts,
            com.mukhtabir.android.util.formatNumber(ui.answered.size),
            com.mukhtabir.android.util.formatNumber((ui.total - ui.answered.size).coerceAtLeast(0)),
            com.mukhtabir.android.util.formatNumber(ui.flagged.size)))
        Text(stringResource(R.string.flow_map_hint))
        LearningQuestionChips(ui.index, ui.total, ui.answered, ui.flagged, ui.canNavigate) {
            viewModel.goTo(it); showQuestions = false
        }
    }
    if (confirmFinish) AppBottomSheet(
        title = stringResource(R.string.finish_session_title), onDismiss = { confirmFinish = false },
        actions = {
            AppActionRow {
                AppButton({ confirmFinish = false; viewModel.next() }, enabled = ui.canNavigate && ui.canPause) { Text(stringResource(R.string.finish)) }
                if (ui.canFinishLocally) AppTextButton({ confirmFinish = false; viewModel.finishStudyLocally() }) {
                    Text(stringResource(R.string.offline_finish_device))
                }
                AppTextButton({ confirmFinish = false }) { Text(stringResource(R.string.cancel)) }
            }
        },
    ) {
        Text(stringResource(R.string.flow_final_review))
        Text(stringResource(R.string.finish_session_message))
        SessionQuestionPicker(ui, viewModel, onSelected = { confirmFinish = false })
    }
    LearningSessionNavigation(
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
    Text(stringResource(R.string.flow_final_counts,
        com.mukhtabir.android.util.formatNumber(ui.answered.size),
        com.mukhtabir.android.util.formatNumber((ui.total - ui.answered.size).coerceAtLeast(0)),
        com.mukhtabir.android.util.formatNumber(ui.flagged.size)))
    Text(stringResource(R.string.flow_map_hint))
    LearningQuestionChips(ui.index, ui.total, ui.answered, ui.flagged, ui.canNavigate) {
        viewModel.goTo(it); onSelected()
    }
}
