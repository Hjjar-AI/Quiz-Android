@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.quiz

import com.mukhtabir.android.ui.components.AppSectionHeading
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber

@Composable
internal fun BookContentsDialog(state: QuizUiState, viewModel: QuizViewModel, onDismiss: () -> Unit) {
    var choosing by remember { mutableStateOf(state.selectedBook == null) }
    val scroll = rememberScrollState()
    LaunchedEffect(state.selectedBook, choosing) { scroll.scrollTo(0) }
    val selected = state.selectedBook
    val valid = selected != null && state.books.any { it.name == selected }
    val facetsReady = state.bookFiltersFor == selected && !state.bookFiltersLoading && state.bookFiltersError == null
    AppConfirmationDialog(onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.flow_book_contents)) },
        text = { Column(Modifier.heightIn(max = AppLayoutTokens.menuMaxHeight).verticalScroll(scroll),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
            if (choosing || !valid) {
                Text(stringResource(R.string.flow_book_choose))
                if (state.metadataLoading) AppLoadingIndicator()
                state.metadataError?.let { ErrorState(throwable = it, onRetry = if (state.metadataLoading) null else viewModel::refreshMetadata) }
                state.books.forEach { book -> AppCard(onClick = { viewModel.selectBook(book.name); choosing = false },
                    enabled = !state.isLoading, modifier = Modifier.fillMaxWidth()) {
                    AppCardBody { Text(book.name, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.flow_book_count, formatNumber(book.count))) }
                } }
            } else {
                Text(selected.orEmpty(), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.flow_book_refine))
                Text(stringResource(R.string.quiz_filter_matching), style = MaterialTheme.typography.bodySmall)
                AppTextButton({ choosing = true }) { Text(stringResource(R.string.flow_book_choose)) }
                if (state.bookFiltersLoading) AppLoadingIndicator()
                state.bookFiltersError?.let { ErrorState(throwable = it, onRetry = if (state.bookFiltersLoading) null else viewModel::refreshBookFilters) }
                if (facetsReady) {
                    AppSectionHeading(stringResource(R.string.select_categories))
                    if (state.bookCategories.isEmpty()) Text(stringResource(R.string.quiz_book_no_categories))
                    AppActionRow { state.bookCategories.forEach { category -> AppFilterChip(category.id in state.selectedCategoryIds,
                        { viewModel.toggleCategory(category.id) }, enabled = !state.isLoading, label = { Text(category.name) }) } }
                    AppSectionHeading(stringResource(R.string.ux_tags_any))
                    if (state.bookTags.isEmpty()) Text(stringResource(R.string.quiz_book_no_tags))
                    AppActionRow { state.bookTags.forEach { tag -> AppFilterChip(tag.name in state.selectedTags,
                        { viewModel.toggleTag(tag.name) }, enabled = !state.isLoading, label = { Text(tag.name + " · " + formatNumber(tag.count)) }) } }
                }
                ActiveFilterSummary(state, viewModel, selected)
                if (state.isCountLoading) AppLoadingIndicator()
                else if (state.countError == null) Text(stringResource(R.string.flow_book_count, formatNumber(state.availableCount)))
                state.countError?.let { ErrorState(throwable = it, onRetry = viewModel::refreshMetadata) }
            }
        } },
        confirmButton = { AppButton(onDismiss, enabled = valid && !choosing && !state.isLoading &&
            (!state.selectedCategoryIds.any() && !state.selectedTags.any() || facetsReady)) { Text(stringResource(R.string.flow_book_use)) } },
        dismissButton = { AppTextButton(onDismiss) { Text(stringResource(R.string.close)) } })
}
