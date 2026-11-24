package com.mukhtabir.android.ui.quiz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.data.remote.SessionResultsDto
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.ActiveFilter
import com.mukhtabir.android.ui.components.ActiveFilterSummary
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppHero
import com.mukhtabir.android.ui.components.AppHeroRow
import com.mukhtabir.android.ui.components.AppIconButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppMenu
import com.mukhtabir.android.ui.components.AppMenuItem
import com.mukhtabir.android.ui.components.AppMenuItemStyle
import com.mukhtabir.android.ui.components.AppMotion
import com.mukhtabir.android.ui.components.AppProgressRing
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.ExplanationCard
import com.mukhtabir.android.ui.components.LearningSaveState
import com.mukhtabir.android.ui.components.LearningSaveStatus
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.components.performAppHaptic
import com.mukhtabir.android.ui.components.rememberMotionEnabled
import com.mukhtabir.android.util.formatNumber

/** Short status at rest; the tools menu retains deadline/synchronization guidance. */
@Composable
internal fun SessionSyncStatus(state: QuizUiState, viewModel: QuizViewModel) {
    val (saveState, status) = when {
        state.draftStorageFailed -> LearningSaveState.REVIEW to R.string.quiz_save_unconfirmed
        state.syncConflict != null || state.recoveryRequired -> LearningSaveState.REVIEW to R.string.ui_needs_review
        state.syncing -> LearningSaveState.SYNCING to R.string.quiz_syncing
        state.syncError != null -> LearningSaveState.REVIEW to R.string.quiz_sync_failed
        state.queuedAnswers > 0 -> LearningSaveState.WAITING to R.string.quiz_saved_pending
        state.syncRequired -> LearningSaveState.WAITING to R.string.quiz_sync_needed
        state.answeredIndices.isNotEmpty() -> LearningSaveState.SERVER to R.string.quiz_synced
        else -> LearningSaveState.DEVICE to R.string.quiz_download_ready
    }
    LearningSaveStatus(saveState, detail = stringResource(status, formatNumber(state.queuedAnswers))) {
        if (state.syncRequired || state.queuedAnswers > 0) AppTextButton(
            onClick = viewModel::syncDownloadedAnswers,
            enabled = !state.isLoading && !state.syncing && !state.draftStorageFailed && state.syncConflict == null) {
            Icon(Icons.Filled.Sync, contentDescription = null)
            Text(stringResource(R.string.quiz_sync), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** One stable header action; secondary controls preserve their existing handler guards. */
@Composable
internal fun SessionToolsMenu(state: QuizUiState, ui: SessionPresentation, viewModel: QuizViewModel,
    preferences: AppPreferences, onPause: () -> Unit) {
    var expanded by rememberSaveable(ui.sessionId) { mutableStateOf(false) }
    var showDetails by rememberSaveable(ui.sessionId) { mutableStateOf(false) }
    val display by preferences.display.collectAsLifecycleState()
    if (showDetails) AppConfirmationDialog(
        onDismissRequest = { showDetails = false },
        title = { Text(stringResource(R.string.quiz_download_details)) },
        text = { Text(stringResource(if (state.queuedAnswers > 0) R.string.session_local_answers
            else R.string.session_download_ready, state.queuedAnswers)) },
        confirmButton = { AppTextButton({ showDetails = false }) { Text(stringResource(R.string.done)) } },
    )
    Box {
        AppIconButton({ expanded = true }) {
            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.quiz_tools))
        }
        AppMenu(expanded, onDismissRequest = { expanded = false }) {
            AppMenuItem(stringResource(R.string.pause_and_leave),
                onClick = { expanded = false; onPause() },
                style = AppMenuItemStyle.COMPACT, enabled = ui.canRequestPause)
            if (state.downloaded) {
                if (state.syncRequired || state.queuedAnswers > 0) AppMenuItem(
                    stringResource(R.string.session_sync_answers),
                    onClick = { expanded = false; viewModel.syncDownloadedAnswers() },
                    style = AppMenuItemStyle.COMPACT,
                    enabled = !state.isLoading && !state.draftStorageFailed && state.syncConflict == null)
                AppMenuItem(stringResource(R.string.quiz_download_details),
                    onClick = { expanded = false; showDetails = true },
                    style = AppMenuItemStyle.COMPACT)
            }
            AppMenuItem(stringResource(R.string.preferences_sound),
                onClick = {
                    preferences.setDisplay(preferences.display.value.copy(sound = !display.sound))
                    expanded = false
                },
                style = AppMenuItemStyle.COMPACT, isSelected = display.sound, showSelectionIndicator = true)
            AppMenuItem(stringResource(R.string.preferences_haptics),
                onClick = {
                    preferences.setDisplay(preferences.display.value.copy(haptics = !display.haptics))
                    expanded = false
                },
                style = AppMenuItemStyle.COMPACT, isSelected = display.haptics, showSelectionIndicator = true)
        }
    }
}

/** Consume confirmations even in the background, so reopening never replays feedback. */
@Composable
internal fun ConfirmedAnswerHaptics(token: Long, enabled: Boolean, foreground: Boolean) {
    val view = LocalView.current
    var handled by remember { mutableStateOf(token) }
    LaunchedEffect(token, foreground) {
        if (token > handled) {
            handled = token
            if (enabled && foreground) performAppHaptic(view,confirmed=true)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun QuestionBookmark(state: QuizUiState, viewModel: QuizViewModel) {
    val id = state.questionPage?.question?.id ?: return
    LaunchedEffect(state.session?.sessionId, id) { viewModel.refreshQuestionBookmark(id, force = true) }
    val saved = state.questionBookmarks[id]
    val uncertain = id in state.bookmarkUncertainIds
    val label = stringResource(when {
        saved == null || uncertain -> R.string.quiz_bookmark_check
        saved == true -> R.string.remove_bookmark
        else -> R.string.quiz_add_bookmark
    })
    val status = stringResource(when {
        id in state.bookmarkBusyIds -> R.string.quiz_bookmark_loading
        saved == null || uncertain -> R.string.quiz_bookmark_unknown
        saved == true -> R.string.quiz_bookmark_saved
        else -> R.string.quiz_bookmark_not_saved
    })
    AppIconButton(
        onClick = {
            if (saved == null || uncertain) viewModel.refreshQuestionBookmark(id, force = true)
            else viewModel.changeQuestionBookmark(id)
        }, enabled=id !in state.bookmarkBusyIds && !state.recoveryRequired,
        modifier=Modifier.semantics { contentDescription=label; selected=saved==true && !uncertain; stateDescription=status }) {
        Box(Modifier.size(AppLayoutTokens.secondaryIconSize),contentAlignment=Alignment.Center) {
            if(id in state.bookmarkBusyIds) AppLoadingIndicator(compact=true)
            else Icon(if(saved==true && !uncertain) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription=null,tint=if(saved==true && !uncertain) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Fade only the appended card: no size animation or movement of the question/choices. */
@Composable
internal fun StudyExplanation(text: String, questionId: Long, alreadyRevealed: Boolean, onRevealed: () -> Unit) {
    key(questionId) {
        val motionEnabled = rememberMotionEnabled()
        var revealed by remember { mutableStateOf(alreadyRevealed) }
        LaunchedEffect(Unit) { revealed = true; onRevealed() }
        val opacity = animateFloatAsState(targetValue = if (revealed || !motionEnabled) 1f else 0f,
            animationSpec = tween(durationMillis = if (motionEnabled) AppMotion.revealMillis else 0), label = "Study explanation fade")
        ExplanationCard(text, title = stringResource(R.string.explanation),
            modifier = Modifier.graphicsLayer { alpha = if (motionEnabled) opacity.value else 1f })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActiveFilterSummary(state: QuizUiState, viewModel: QuizViewModel, selectedBook: String?) {
    val tags = (state.selectedTags + state.additionalTags.split(',').map(String::trim).filter(String::isNotEmpty)).distinct()
    val filters = buildList {
        selectedBook?.let { book -> add(ActiveFilter("book", stringResource(R.string.quiz_filter_book, book), viewModel::useWholeBank)) }
        state.selectedCategoryIds.forEach { id ->
            val name = (state.categories + state.bookCategories).firstOrNull { it.id == id }?.name ?: formatNumber(id)
            add(ActiveFilter("category-$id", stringResource(R.string.quiz_filter_category, name), { viewModel.removeCategoryFilter(id) }))
        }
        tags.forEach { name -> add(ActiveFilter("tag-$name", stringResource(R.string.quiz_filter_tag, name), { viewModel.removeTagFilter(name) })) }
    }
    ActiveFilterSummary(filters, onClear = viewModel::clearActiveFilters, enabled = !state.isLoading)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ResultsOverview(results: SessionResultsDto) {
    val accuracy = results.accuracy.takeIf { it.isFinite() }
        ?: if (results.totalQuestions > 0) results.correctCount * 100.0 / results.totalQuestions else 0.0
    AppHero {
        AppHeroRow(decoration = {
            AppProgressRing((accuracy / 100).toFloat(),
                com.mukhtabir.android.util.bidi("${com.mukhtabir.android.util.formatDecimal(accuracy)}%"),
                stringResource(R.string.quiz_metric_accuracy))
        }) {
            Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                Text(com.mukhtabir.android.util.bidi("${formatNumber(results.correctCount)} / ${formatNumber(results.totalQuestions)}"),
                    style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.ui_result_summary, formatNumber(results.correctCount),
                    formatNumber(results.answeredCount), formatNumber((results.totalQuestions - results.answeredCount).coerceAtLeast(0))))
                Text(stringResource(R.string.total_time, formatDuration(results.totalTime)), style = MaterialTheme.typography.bodySmall)
                if (results.confidenceFragile > 0) Text(stringResource(R.string.fragile_result_count, com.mukhtabir.android.util.formatNumber(results.confidenceFragile)),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
internal fun quizModeLabel(mode: TestMode): String = stringResource(
    when (mode) {
        TestMode.EXAM -> R.string.exam
        TestMode.STUDY -> R.string.study
        TestMode.RECALL -> R.string.recall
    },
)

internal fun formatDuration(seconds: Int): String {
    return com.mukhtabir.android.util.formatSessionDuration(seconds.toLong())
}
