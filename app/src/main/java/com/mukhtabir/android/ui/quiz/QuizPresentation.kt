package com.mukhtabir.android.ui.quiz

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sync
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.SessionResultsDto
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppIconButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.ExplanationCard
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatDecimal
import com.mukhtabir.android.util.formatNumber

/** Short status at rest; the tools menu retains deadline/synchronization guidance. */
@Composable
internal fun SessionSyncStatus(state: QuizUiState, viewModel: QuizViewModel) {
    val status = when {
        state.draftStorageFailed -> R.string.quiz_save_unconfirmed
        state.syncing -> R.string.quiz_syncing
        state.syncError != null -> R.string.quiz_sync_failed
        state.queuedAnswers > 0 -> R.string.quiz_saved_pending
        state.syncRequired -> R.string.quiz_sync_needed
        state.answeredIndices.isNotEmpty() -> R.string.quiz_synced
        else -> R.string.quiz_download_ready
    }
    com.mukhtabir.android.ui.components.AppSaveStatus(
        stringResource(status,formatNumber(state.queuedAnswers)),failed=state.draftStorageFailed || state.syncError!=null) {
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
    if (selectedBook == null && state.selectedCategoryIds.isEmpty() && tags.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.tight)) {
        Text(stringResource(R.string.quiz_active_filters), style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        AppActionRow {
            selectedBook?.let { book ->
                RemovableFilter(stringResource(R.string.quiz_filter_book, book), !state.isLoading, viewModel::useWholeBank)
            }
            state.selectedCategoryIds.forEach { id ->
                val name = (state.categories + state.bookCategories).firstOrNull { it.id == id }?.name ?: formatNumber(id)
                RemovableFilter(stringResource(R.string.quiz_filter_category, name), !state.isLoading,
                    onRemove = { viewModel.removeCategoryFilter(id) })
            }
            tags.forEach { name ->
                RemovableFilter(stringResource(R.string.quiz_filter_tag, name), !state.isLoading,
                    onRemove = { viewModel.removeTagFilter(name) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RemovableFilter(label: String, enabled: Boolean, onRemove: () -> Unit) {
    val removeLabel = stringResource(R.string.quiz_remove_filter, label)
    FilterChip(selected = true, onClick = onRemove, enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.semantics { contentDescription = removeLabel },
        label = { Text(label) }, trailingIcon = { Icon(Icons.Filled.Close, contentDescription = null) })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ResultsOverview(results: SessionResultsDto) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.content)) {
            val values=listOf(
                com.mukhtabir.android.util.bidi("${formatNumber(results.correctCount)} / ${formatNumber(results.totalQuestions)}"),
                com.mukhtabir.android.util.bidi("${formatDecimal(results.accuracy)}%"),
                formatNumber(results.answeredCount),
                formatNumber((results.totalQuestions-results.answeredCount).coerceAtLeast(0)))
            val labels=listOf(stringResource(R.string.quiz_metric_score),stringResource(R.string.quiz_metric_accuracy),
                stringResource(R.string.quiz_metric_answered),stringResource(R.string.unanswered))
            com.mukhtabir.android.ui.components.AppTileGrid(values.size) { index,tileModifier ->
                com.mukhtabir.android.ui.components.AppMetricTile(values[index],labels[index],tileModifier)
            }
            com.mukhtabir.android.ui.components.AppMetadataRow(listOf(
                stringResource(R.string.total_time,formatDuration(results.totalTime))))
            if (results.confidenceFragile > 0) Text(stringResource(R.string.fragile_result_count, results.confidenceFragile),
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
