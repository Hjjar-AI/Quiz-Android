package com.mukhtabir.android.ui.quiz

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppProgressBar
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppSwitchRow
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.difficultyLabel
import com.mukhtabir.android.ui.theme.AppTheme

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun QuizSetupContent(
    state: QuizUiState,
    canUseBlueprint: Boolean,
    viewModel: QuizViewModel,
) {
    var bookContents by rememberSaveable { mutableStateOf(false) }
    if (bookContents) BookContentsDialog(state, viewModel, onDismiss = { bookContents = false })
    var confirmDiscard by rememberSaveable(state.resumable?.progress?.sessionId) { mutableStateOf(false) }
    if (confirmDiscard) AppConfirmationDialog(
        onDismissRequest = { confirmDiscard = false },
        title = { Text(stringResource(R.string.discard_saved_session_title)) },
        text = { Text(stringResource(R.string.discard_saved_session_message)) },
        confirmButton = { AppDangerTextButton({ confirmDiscard = false; viewModel.discardSaved() }, enabled = !state.isLoading) { Text(stringResource(R.string.ux_discard_saved_session)) } },
        dismissButton = { AppTextButton({ confirmDiscard = false }) { Text(stringResource(R.string.cancel)) } },
    )
    state.resumable?.let { saved ->
        AppCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(AppLayoutTokens.cardInsets),
                verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap),
            ) {
                Text(stringResource(R.string.quiz_saved_mode, quizModeLabel(saved.mode)),
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
                AppProgressBar(progress = if (total > 0) answered.toFloat() / total else 0f,
                    modifier = Modifier.fillMaxWidth())
                val pending = if (state.pendingDraft?.sessionId == saved.progress.sessionId) 1 else 0
                Text(if (state.draftStorageFailed) stringResource(R.string.quiz_resume_pending_unknown)
                    else stringResource(R.string.quiz_resume_pending, com.mukhtabir.android.util.formatNumber(pending)),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                QuizSelectionRow {
                    AppButton(onClick = viewModel::resumeSaved, enabled = !state.isLoading) {
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
        subtitle = quizModeLabel(state.mode)) {
        QuizSelectionRow {
            ModeButton(TestMode.STUDY, R.string.study, state, viewModel)
            ModeButton(TestMode.EXAM, R.string.exam, state, viewModel)
            ModeButton(TestMode.RECALL, R.string.recall, state, viewModel)
        }
    }
    val bankPool = state.source in setOf(QuizSource.BOOK, QuizSource.TAG, QuizSource.CATEGORY)
    val selectedBook = state.selectedBook.takeIf { bankPool }
    val bookExists = selectedBook == null || state.books.any { it.name == selectedBook }
    val scopedFilters = state.selectedCategoryIds.isNotEmpty() || state.selectedTags.isNotEmpty()
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
        QuizSelectionRow {
            SourceButton(QuizSource.BOOK, R.string.quiz_pool_all, state, viewModel, true)
            SourceButton(QuizSource.BOOKMARKS, R.string.bookmarks, state, viewModel, state.bookmarkCount > 0)
            SourceButton(QuizSource.SRS, R.string.source_srs, state, viewModel, state.srsDueCount > 0)
            if (canUseBlueprint) SourceButton(QuizSource.BLUEPRINT, R.string.source_blueprint,
                state, viewModel, state.blueprints.isNotEmpty())
        }
        when (state.source) {
            QuizSource.BOOKMARKS -> if (state.metadataError == null) Text(stringResource(R.string.source_bookmarks_count, com.mukhtabir.android.util.formatNumber(state.bookmarkCount)))
            QuizSource.SRS -> if (state.metadataError == null) Text(stringResource(R.string.source_srs_count, com.mukhtabir.android.util.formatNumber(state.srsDueCount)))
            QuizSource.BLUEPRINT -> SetupPicker(title = stringResource(R.string.select_blueprint),
                options = state.blueprints.map { it.id to it.name },
                selected = state.selectedBlueprintId?.let(::setOf).orEmpty(),
                onToggle = viewModel::selectBlueprint, enabled = !state.isLoading)
            else -> Unit
        }
    }
    if (bankPool) {
        AppOutlinedButton({ bookContents = true }, enabled = !state.isLoading) { Text(stringResource(R.string.flow_book_contents)) }
        AppSection(title = stringResource(R.string.quiz_optional_book),
            subtitle = selectedBook ?: stringResource(R.string.quiz_no_book_restriction), initiallyExpanded = false) {
            Text(stringResource(R.string.quiz_book_hint), style = MaterialTheme.typography.bodySmall)
            QuizSelectionRow {
                SetupChoiceChip(selected = selectedBook == null, enabled = !state.isLoading,
                    onClick = viewModel::useWholeBank, text = stringResource(R.string.quiz_no_book_restriction))
            }
            SetupPicker(title = stringResource(R.string.select_source_book),
                options = state.books.map { it.name to "${com.mukhtabir.android.util.bidi(it.name)} (${com.mukhtabir.android.util.formatNumber(it.count)})" },
                selected = selectedBook?.let(::setOf).orEmpty(), onToggle = viewModel::selectBook,
                enabled = !state.isLoading)
        }
        if (!bookExists) AppStatus(stringResource(R.string.quiz_book_unavailable))
    }
    val filtersHint = if (selectedBook != null) stringResource(R.string.quiz_scoped_filters_optional, com.mukhtabir.android.util.bidi(selectedBook))
        else stringResource(R.string.quiz_pool_filters_optional)
    val selectionHint = if (!hasFilters) filtersHint else filtersHint + "\n" +
        stringResource(R.string.quiz_filters_selected, com.mukhtabir.android.util.formatNumber(state.selectedCategoryIds.size), com.mukhtabir.android.util.formatNumber(selectedTagCount))
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
        if (state.additionalTags.isNotBlank()) AppTextField(value = state.additionalTags,
            onValueChange = viewModel::setAdditionalTags, modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.additional_tags)) },
            supportingText = { Text(stringResource(R.string.comma_separated)) }, enabled = !state.isLoading)
        if (hasFilters) AppOutlinedButton(onClick = viewModel::clearFilters, enabled = !state.isLoading) {
            Text(stringResource(R.string.quiz_clear_filters))
        }
    }
    AppSection(title = stringResource(R.string.quiz_other_options), initiallyExpanded = false,
        subtitle = stringResource(R.string.quiz_refinement_summary,
            state.difficulty?.let { difficultyLabel(it) } ?: stringResource(R.string.difficulty_any),
            if (state.verifiedOnly) stringResource(R.string.verified_only) else stringResource(R.string.quiz_verification_any)) +
            (if (state.source != QuizSource.SRS) " · " + stringResource(when (state.selectionStrategy) {
                "balanced" -> R.string.selection_balanced; "review" -> R.string.selection_review; else -> R.string.selection_coverage
            }) else "")) {
        Text(stringResource(R.string.difficulty))
        QuizSelectionRow {
            DifficultyButton(null, R.string.difficulty_any, state, viewModel)
            DifficultyButton("easy", R.string.difficulty_easy, state, viewModel)
            DifficultyButton("medium", R.string.difficulty_medium, state, viewModel)
            DifficultyButton("hard", R.string.difficulty_hard, state, viewModel)
        }
        AppSwitchRow(stringResource(R.string.verified_only), state.verifiedOnly,
            viewModel::setVerifiedOnly, enabled = !state.isLoading)
        if (state.source != QuizSource.SRS) {
            Text(stringResource(R.string.selection_strategy))
            QuizSelectionRow {
                listOf("coverage" to R.string.selection_coverage, "balanced" to R.string.selection_balanced,
                    "review" to R.string.selection_review).forEach { (value, label) ->
                    SetupChoiceChip(selected = state.selectionStrategy == value,
                        onClick = { viewModel.setSelectionStrategy(value) }, enabled = !state.isLoading,
                        text = stringResource(label))
                }
            }
        }
    }
    val readiness = state.toSetupReadiness(canUseBlueprint)
    val effectiveMaximum = readiness.maximum
    val countValid = readiness.countValid
    SetupStepCard(stringResource(R.string.number_of_questions)) {
        state.countError?.let {
            ErrorState(throwable = it, onRetry = if (state.isLoading || state.isCountLoading) null else viewModel::refreshAvailableCount)
        }
        AppTextField(value = state.requestedCount, onValueChange = viewModel::setRequestedCount,
            modifier = Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.number_of_questions)) },
            supportingText = { if (state.countError == null && state.metadataError == null) Text(if (state.isCountLoading) stringResource(R.string.checking_available)
                else stringResource(R.string.available_questions_with_limit, com.mukhtabir.android.util.formatNumber(state.availableCount), com.mukhtabir.android.util.formatNumber(state.maxQuizQuestions))) },
            isError = !state.isCountLoading && state.availableCount > 0 && !countValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done), enabled = !state.isLoading,
            singleLine = true, textStyle = com.mukhtabir.android.util.ltrInputStyle())
        QuizSelectionRow {
            (listOf(10, 20, 40).filter { it <= effectiveMaximum } + listOfNotNull(effectiveMaximum.takeIf { it > 0 })).distinct().forEach { amount ->
                SetupChoiceChip(selected = state.requestedCount.toIntOrNull() == amount,
                    enabled = !state.isLoading && !state.isCountLoading && state.countError == null,
                    onClick = { viewModel.setRequestedCount(amount.toString()) },
                    text = if (amount == effectiveMaximum) stringResource(R.string.ui_count_max, com.mukhtabir.android.util.formatNumber(amount))
                        else com.mukhtabir.android.util.formatNumber(amount))
            }
        }

    }
}

/** Setup sections stay stacked and visible in their selection order. */
@Composable
private fun SetupStepCard(title: String, subtitle: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    AppCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(AppLayoutTokens.cardInsets),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant) }
            content()
        }
    }
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
    AppFilterChip(selected = selected, enabled = enabled, onClick = onClick,
        label = { Text(text, style = MaterialTheme.typography.labelLarge) })
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
    QuizSelectionRow {
        options.forEach { (value, label) ->
            SetupChoiceChip(selected = value in selected, enabled = enabled,
                onClick = { onToggle(value) }, text = label)
        }
    }
}
