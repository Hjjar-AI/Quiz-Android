package com.mukhtabir.android.ui.quiz

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.util.formatNumber

internal data class SetupReadiness(val maximum: Int, val countValid: Boolean, val bookExists: Boolean,
    val sourceReady: Boolean, val filtersReady: Boolean, val canStart: Boolean)

/** Display eligibility mirrors the existing start contract; the ViewModel still independently guards dispatch. */
internal fun QuizUiState.toSetupReadiness(canUseBlueprint: Boolean): SetupReadiness {
    val bank = source in setOf(QuizSource.BOOK, QuizSource.TAG, QuizSource.CATEGORY)
    val book = selectedBook.takeIf { bank }
    val exists = book == null || books.any { it.name == book }
    val filters = book == null || (selectedCategoryIds.isEmpty() && selectedTags.isEmpty()) ||
        (bookFiltersFor == book && !bookFiltersLoading && bookFiltersError == null)
    val sourceReady = when (source) {
        QuizSource.BOOK, QuizSource.TAG, QuizSource.CATEGORY -> exists
        QuizSource.BOOKMARKS -> bookmarkCount > 0
        QuizSource.SRS -> srsDueCount > 0
        QuizSource.BLUEPRINT -> canUseBlueprint && blueprints.any { it.id == selectedBlueprintId && it.isActive }
        null -> false
    }
    val maximum = minOf(availableCount, maxQuizQuestions).coerceAtLeast(0)
    val valid = requestedCount.toIntOrNull()?.let { it in 1..maximum } == true
    return SetupReadiness(maximum, valid, exists, sourceReady, filters,
        setupLoaded && session == null && results == null && !isLoading && pendingDraft == null && !draftStorageFailed &&
            !recoveryRequired && !finishPending && !finishStorageFailed && !syncing && sourceReady && filters &&
            !isCountLoading && countError == null && valid)
}

@Composable
internal fun QuizSetupActions(state: QuizUiState, canUseBlueprint: Boolean, viewModel: QuizViewModel) {
    val ready = state.toSetupReadiness(canUseBlueprint)
    val hint = when {
        state.recoveryRequired || state.pendingDraft != null || state.draftStorageFailed || state.finishPending || state.finishStorageFailed -> stringResource(R.string.session_recovery_required)
        state.isLoading -> null
        state.metadataError != null -> null
        !ready.bookExists -> stringResource(R.string.quiz_book_unavailable)
        !ready.sourceReady -> stringResource(R.string.quiz_select_pool_item)
        !ready.filtersReady -> stringResource(R.string.quiz_filters_need_review)
        state.isCountLoading -> stringResource(R.string.checking_available)
        state.countError != null -> null
        state.error != null || state.hasUnknownError -> state.error ?: stringResource(R.string.error_unknown)
        state.availableCount <= 0 -> stringResource(R.string.quiz_no_matching_questions)
        !ready.countValid -> stringResource(R.string.quiz_count_range, com.mukhtabir.android.util.formatNumber(ready.maximum))
        else -> null
    }
    val bank = state.source in setOf(QuizSource.BOOK, QuizSource.TAG, QuizSource.CATEGORY)
    val pool = if (bank) state.selectedBook ?: stringResource(R.string.quiz_no_book_restriction) else stringResource(when (state.source) {
        QuizSource.BOOKMARKS -> R.string.bookmarks
        QuizSource.SRS -> R.string.source_srs
        QuizSource.BLUEPRINT -> R.string.source_blueprint
        else -> R.string.choose_source
    })
    Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = AppLayoutTokens.heroElevation) {
        Column(Modifier.fillMaxWidth().padding(AppLayoutTokens.cardInsets),
            verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            Text(stringResource(R.string.ui_setup_summary, quizModeLabel(state.mode),
                state.requestedCount.toIntOrNull()?.let(::formatNumber) ?: state.requestedCount, pool),
                style = MaterialTheme.typography.bodySmall)
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            AppButton(viewModel::start, Modifier.fillMaxWidth(), enabled = ready.canStart) {
                Text(stringResource(when (state.mode) {
                    TestMode.EXAM -> R.string.start_exam
                    TestMode.STUDY -> R.string.start_study
                    TestMode.RECALL -> R.string.start_recall
                }))
            }
        }
    }
}
