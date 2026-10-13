package com.mukhtabir.android.ui.quiz

import com.mukhtabir.android.ui.components.AppIconButton

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.List
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppCompactButton as Button
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.util.formatNumber

/** Stable presentation values; session DTOs and recovery rules stay in the ViewModel. */
internal data class SessionPresentation(
    val sessionId: String?,
    val index: Int,
    val total: Int,
    val answered: Set<Int>,
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SessionHeader(ui: SessionPresentation, tools: @Composable () -> Unit) {
    Surface(
        color = if (ui.critical) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium,
    ) {
        BoxWithConstraints {
            val stackedClock = maxWidth < com.mukhtabir.android.ui.components.AppLayoutTokens.sessionHeaderBreakpoint ||
                LocalDensity.current.fontScale >= 1.3f
            Column(Modifier.fillMaxWidth().padding(horizontal = com.mukhtabir.android.ui.components.AppLayoutTokens.pagePadding,
                vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(stringResource(R.string.session_position, formatNumber(ui.index + 1), formatNumber(ui.total)),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f).semantics { heading() })
                    if (!stackedClock) Column(Modifier.weight(1f), horizontalAlignment = androidx.compose.ui.Alignment.End) {
                        Text(stringResource(if (ui.overtime) R.string.session_clock_additional else R.string.session_clock_remaining),
                            style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        StableSessionClock(ui.seconds)
                    }
                    tools()
                }
                if (stackedClock) Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(stringResource(if (ui.overtime) R.string.session_clock_additional else R.string.session_clock_remaining),
                        modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    StableSessionClock(ui.seconds)
                }
                if (ui.critical) Text(stringResource(R.string.session_time_critical), color = MaterialTheme.colorScheme.error)
                LinearProgressIndicator(progress = if (ui.total > 0) ui.answered.size.toFloat() / ui.total else 0f,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 2.dp, max = 2.dp))
            }
        }
    }
}

/** Reserve an hours-capable clock box; invisible sizing text has no accessibility node. */
@Composable
private fun StableSessionClock(seconds: Int) {
    val style = MaterialTheme.typography.bodySmall.copy(fontFeatureSettings = "tnum")
    val hours = (seconds.coerceAtLeast(0) / 3600).toString().length.coerceAtLeast(2)
    Box(contentAlignment = androidx.compose.ui.Alignment.CenterEnd) {
        (0..9).forEach { value ->
            val digit = formatNumber(value)
            val sample = "\u2066${digit.repeat(hours)}:${digit.repeat(2)}:${digit.repeat(2)}\u2069"
            Text(sample, Modifier.alpha(0f).clearAndSetSemantics { }, style = style, maxLines = 1, softWrap = false)
        }
        Text(formatDuration(seconds), style = style, maxLines = 1, softWrap = false, textAlign = TextAlign.End)
    }
}

@Composable
internal fun SessionNavigation(ui: SessionPresentation, viewModel: QuizViewModel) {
    var confirmFinish by rememberSaveable(ui.sessionId) { mutableStateOf(false) }
    var showQuestions by rememberSaveable(ui.sessionId) { mutableStateOf(false) }
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    if (showQuestions) AlertDialog(
        onDismissRequest = { showQuestions = false },
        title = { Text(stringResource(R.string.session_question_navigation)) },
        text = { SessionQuestionPicker(ui, viewModel, onSelected = { showQuestions = false }) },
        confirmButton = { TextButton({ showQuestions = false }) { Text(stringResource(R.string.done)) } },
    )
    if (confirmFinish) AlertDialog(
        onDismissRequest = { confirmFinish = false },
        title = { Text(stringResource(R.string.finish_session_title)) },
        text = { Text(stringResource(R.string.finish_session_message)) },
        confirmButton = { TextButton({ confirmFinish = false; viewModel.next() }, enabled = ui.canNavigate && ui.canPause) { Text(stringResource(R.string.finish)) } },
        dismissButton = { Column {
            if (ui.canFinishLocally) TextButton({ confirmFinish = false; viewModel.finishStudyLocally() }) {
                Text(stringResource(R.string.offline_finish_device))
            }
            TextButton({ confirmFinish = false }) { Text(stringResource(R.string.cancel)) }
        } },
    )
    Surface(color = MaterialTheme.colorScheme.surface) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                AppIconButton(viewModel::previous, enabled = ui.index > 0 && ui.canNavigate) {
                    Icon(if (rtl) Icons.Filled.ChevronRight else Icons.Filled.ChevronLeft,
                        contentDescription = stringResource(R.string.previous))
                }
                AppIconButton({ showQuestions = true }) {
                    Icon(Icons.Filled.List, contentDescription = stringResource(R.string.session_question_navigation))
                }
                Button({ if (ui.index + 1 >= ui.total) {
                    if (ui.locallyFinished) viewModel.syncDownloadedAnswers() else confirmFinish = true
                } else viewModel.next() },
                    enabled = ui.canNavigate, modifier = Modifier.weight(1f)) {
                    Text(stringResource(if (ui.index + 1 >= ui.total) {
                        if (ui.locallyFinished) R.string.offline_sync_finish else R.string.finish
                    } else R.string.next))
                }
            }
    }
}


@Composable
internal fun SessionQuestionPicker(ui: SessionPresentation, viewModel: QuizViewModel, onSelected: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
            Text(stringResource(R.string.session_answered_progress, formatNumber(ui.answered.size), formatNumber(ui.total)),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val listState = rememberLazyListState()
            LaunchedEffect(ui.index, ui.total) { if (ui.total > 0) listState.scrollToItem(ui.index) }
            LazyRow(state = listState, horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                items(ui.total, key = { it }) { index ->
                    val answered = index in ui.answered
                    val status = stringResource(if (answered) R.string.session_question_answered else R.string.session_question_unanswered)
                    OutlinedButton(
                        onClick = { viewModel.goTo(index); onSelected() },
                        enabled = ui.canNavigate,
                        modifier = Modifier.widthIn(min = 48.dp).heightIn(min = 48.dp).semantics {
                            selected = index == ui.index
                            stateDescription = status
                        },
                    ) {
                        if (answered) Icon(Icons.Filled.Check, contentDescription = null)
                        Text(formatNumber(index + 1), style = if (index == ui.index)
                            MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
                    }
                }
            }
    }
}
