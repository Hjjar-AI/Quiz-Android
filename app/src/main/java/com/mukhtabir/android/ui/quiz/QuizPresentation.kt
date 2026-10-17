package com.mukhtabir.android.ui.quiz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.SessionResultsDto
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.ui.components.AppIconButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.ExplanationCard
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.util.formatNumber

/** Short status at rest; the tools menu retains deadline/synchronization guidance. */
@Composable
internal fun SessionSyncStatus(state: QuizUiState, viewModel: QuizViewModel) {
    val status = when {
        state.draftStorageFailed -> R.string.quiz_save_unconfirmed
        state.syncConflict != null || state.recoveryRequired -> R.string.ui_needs_review
        state.syncing -> R.string.quiz_syncing
        state.syncError != null -> R.string.quiz_sync_failed
        state.queuedAnswers > 0 -> R.string.quiz_saved_pending
        state.syncRequired -> R.string.quiz_sync_needed
        state.answeredIndices.isNotEmpty() -> R.string.quiz_synced
        else -> R.string.quiz_download_ready
    }
    com.mukhtabir.android.ui.components.LearningSaveStatus(when {
        state.draftStorageFailed || state.syncError != null || state.syncConflict != null || state.recoveryRequired -> com.mukhtabir.android.ui.components.LearningSaveState.REVIEW
        state.syncing -> com.mukhtabir.android.ui.components.LearningSaveState.SYNCING
        state.queuedAnswers > 0 || state.syncRequired -> com.mukhtabir.android.ui.components.LearningSaveState.WAITING
        state.answeredIndices.isNotEmpty() -> com.mukhtabir.android.ui.components.LearningSaveState.SERVER
        else -> com.mukhtabir.android.ui.components.LearningSaveState.DEVICE
    }, detail = stringResource(status,formatNumber(state.queuedAnswers))) {
        if (state.syncRequired || state.queuedAnswers > 0) AppTextButton(
            onClick = viewModel::syncDownloadedAnswers,
            enabled = !state.isLoading && !state.syncing && !state.draftStorageFailed && state.syncConflict == null) {
            Icon(Icons.Filled.Sync, contentDescription = null)
            Text(stringResource(R.string.quiz_sync), style = MaterialTheme.typography.labelMedium)
        }
    }
}

/** One stable header action; secondary controls preserve their existing handler guards. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionToolsMenu(state: QuizUiState, ui: SessionPresentation, viewModel: QuizViewModel,
    preferences: AppPreferences, onPause: () -> Unit) {
    var expanded by rememberSaveable(ui.sessionId) { mutableStateOf(false) }
    var showDetails by rememberSaveable(ui.sessionId) { mutableStateOf(false) }
    val display by preferences.display.collectAsState()
    val menuColors = MenuDefaults.itemColors(disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .62f))
    if (showDetails) AlertDialog(
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
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = AppLayoutTokens.menuMaxHeight)) {
            DropdownMenuItem(text = { Text(stringResource(R.string.pause_and_leave), style = MaterialTheme.typography.labelLarge) },
                enabled = ui.canRequestPause, colors = menuColors, contentPadding = AppLayoutTokens.secondaryContentPadding,
                onClick = { expanded = false; onPause() })
            if (state.downloaded) {
                if (state.syncRequired || state.queuedAnswers > 0) DropdownMenuItem(
                    text = { Text(stringResource(R.string.session_sync_answers), style = MaterialTheme.typography.labelLarge) },
                    colors = menuColors, contentPadding = AppLayoutTokens.secondaryContentPadding,
                    enabled = !state.isLoading && !state.draftStorageFailed && state.syncConflict == null,
                    onClick = { expanded = false; viewModel.syncDownloadedAnswers() })
                DropdownMenuItem(text = { Text(stringResource(R.string.quiz_download_details), style = MaterialTheme.typography.labelLarge) },
                    contentPadding = AppLayoutTokens.secondaryContentPadding,
                    onClick = { expanded = false; showDetails = true })
            }
            DropdownMenuItem(text = { Text(stringResource(R.string.preferences_sound), style = MaterialTheme.typography.labelLarge) },
                contentPadding = AppLayoutTokens.secondaryContentPadding,
                modifier = Modifier.semantics { selected = display.sound },
                trailingIcon = { if (display.sound) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(AppLayoutTokens.secondaryIconSize)) },
                onClick = { preferences.setDisplay(preferences.display.value.copy(sound = !display.sound)); expanded = false })
            DropdownMenuItem(text = { Text(stringResource(R.string.preferences_haptics), style = MaterialTheme.typography.labelLarge) },
                contentPadding = AppLayoutTokens.secondaryContentPadding,
                modifier = Modifier.semantics { selected = display.haptics },
                trailingIcon = { if (display.haptics) Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(AppLayoutTokens.secondaryIconSize)) },
                onClick = { preferences.setDisplay(preferences.display.value.copy(haptics = !display.haptics)); expanded = false })
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
            if (enabled && foreground) com.mukhtabir.android.ui.components.performAppHaptic(view,confirmed=true)
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
    com.mukhtabir.android.ui.components.AppOutlinedButton(
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
        Text(label,style=MaterialTheme.typography.labelMedium)
    }
}

/** Fade only the appended card: no size animation or movement of the question/choices. */
@Composable
internal fun StudyExplanation(text: String, questionId: Long, alreadyRevealed: Boolean, onRevealed: () -> Unit) {
    key(questionId) {
        val motionEnabled = (rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor ?: 1f) > 0f
        var revealed by remember { mutableStateOf(alreadyRevealed) }
        LaunchedEffect(Unit) { revealed = true; onRevealed() }
        val opacity = animateFloatAsState(targetValue = if (revealed || !motionEnabled) 1f else 0f,
            animationSpec = tween(durationMillis = if (motionEnabled) 180 else 0), label = "Study explanation fade")
        ExplanationCard(text, title = stringResource(R.string.explanation),
            modifier = Modifier.graphicsLayer { alpha = if (motionEnabled) opacity.value else 1f })
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActiveFilterSummary(state: QuizUiState, viewModel: QuizViewModel, selectedBook: String?) {
    val tags = (state.selectedTags + state.additionalTags.split(',').map(String::trim).filter(String::isNotEmpty)).distinct()
    val filters = buildList {
        selectedBook?.let { book -> add(com.mukhtabir.android.ui.components.ActiveFilter("book", stringResource(R.string.quiz_filter_book, book), viewModel::useWholeBank)) }
        state.selectedCategoryIds.forEach { id ->
            val name = (state.categories + state.bookCategories).firstOrNull { it.id == id }?.name ?: formatNumber(id)
            add(com.mukhtabir.android.ui.components.ActiveFilter("category-$id", stringResource(R.string.quiz_filter_category, name), { viewModel.removeCategoryFilter(id) }))
        }
        tags.forEach { name -> add(com.mukhtabir.android.ui.components.ActiveFilter("tag-$name", stringResource(R.string.quiz_filter_tag, name), { viewModel.removeTagFilter(name) })) }
    }
    com.mukhtabir.android.ui.components.ActiveFilterSummary(filters, onClear = viewModel::clearActiveFilters, enabled = !state.isLoading)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ResultsOverview(results: SessionResultsDto) {
    com.mukhtabir.android.ui.components.LearningResultsSummary(results.correctCount, results.totalQuestions,
        results.answeredCount, results.accuracy) {
        com.mukhtabir.android.ui.components.AppMetadataRow(listOf(stringResource(R.string.total_time, formatDuration(results.totalTime))))
        if (results.confidenceFragile > 0) Text(stringResource(R.string.fragile_result_count, results.confidenceFragile),
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
