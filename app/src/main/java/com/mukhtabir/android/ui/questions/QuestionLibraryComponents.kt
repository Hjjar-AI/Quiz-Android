@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.QuestionRatingRowDto
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppBottomSheet
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCheckboxRow
import com.mukhtabir.android.ui.components.AppFilterOptions
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.QuestionListCard
import com.mukhtabir.android.ui.profile.AuthorRank
import com.mukhtabir.android.ui.profile.AuthorRankBadge

@Composable
internal fun LibraryFiltersSheet(
    state: QuestionLibraryUiState,
    defaultPageSize: Int,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    onApply: (Long?, String?, String?, Boolean, Int?) -> Unit,
) {
    var category by remember { mutableStateOf(state.categoryId?.toString().orEmpty()) }
    var difficulty by remember { mutableStateOf(state.difficulty.orEmpty()) }
    var tag by remember { mutableStateOf(state.tag.orEmpty()) }
    var verified by remember { mutableStateOf(state.verifiedOnly) }
    var size by remember { mutableStateOf(state.customPageSize?.toString().orEmpty()) }
    LaunchedEffect(state.categories,state.tags,state.optionsLoading,state.optionsError) {
        if(!state.optionsLoading && state.optionsError==null) {
            if(category.isNotEmpty() && state.categories.none {it.id.toString()==category}) category=""
            if(tag.isNotEmpty() && tag !in state.tags) tag=""
        }
    }
    val any = stringResource(R.string.difficulty_any)
    AppBottomSheet(
        title = stringResource(R.string.library_filters), onDismiss = onDismiss,
        actions = {
            AppActionRow {
                AppButton({ onApply(category.toLongOrNull(), difficulty.takeIf(String::isNotBlank),
                    tag.takeIf(String::isNotBlank), verified, size.toIntOrNull()) }) { Text(stringResource(R.string.library_apply)) }
                AppTextButton(onDismiss) { Text(stringResource(R.string.cancel)) }
            }
        },
    ) {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                if (state.optionsLoading) AppLoadingIndicator()
                state.optionsError?.let { ErrorState(throwable = it,
                    onRetry = if (state.optionsLoading) null else onRetry) }
                LibraryFilterChoice(stringResource(R.string.source_categories), category,
                    listOf("" to any) + state.categories.map { it.id.toString() to it.name }) { category = it }
                LibraryFilterChoice(stringResource(R.string.difficulty), difficulty, listOf(
                    "" to any, "easy" to stringResource(R.string.difficulty_easy),
                    "medium" to stringResource(R.string.difficulty_medium),
                    "hard" to stringResource(R.string.difficulty_hard))) { difficulty = it }
                LibraryFilterChoice(stringResource(R.string.source_tags), tag,
                    listOf("" to any) + state.tags.map { it to it }) { tag = it }
                Text(stringResource(R.string.ux_filter_single_tag), style = MaterialTheme.typography.bodySmall)
                AppCheckboxRow(stringResource(R.string.verified_only), verified, { verified = it })
                LibraryFilterChoice(stringResource(R.string.library_page_size), size,
                    listOf("" to stringResource(R.string.library_default_page_size, com.mukhtabir.android.util.formatNumber(defaultPageSize))) +
                        listOf(10, 20, 50, 100).map { it.toString() to it.toString() }) { size = it }
            }
    }

}

@Composable
private fun LibraryFilterChoice(label: String, value: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    AppFilterOptions(label, options, value,
        onSelect = onSelect)

}

@Composable
internal fun QuestionLibraryCard(
    question: QuestionDto,
    imageRepository: ImageRepository,
    onClick: () -> Unit,
    onVerify: (() -> Unit)? = null,
    verifyEnabled: Boolean = true,
    selected: Boolean = false,
    onSelect: (() -> Unit)? = null,
    selectionEnabled: Boolean = true,
    rating: QuestionRatingRowDto? = null,
    ratingLoading: Boolean = false,
    onRating: (() -> Unit)? = null,
) {
    QuestionListCard(question, imageRepository, onOpen = onClick,
        selected = selected, onSelect = onSelect, selectionEnabled = selectionEnabled) {
            question.authoredByUsername?.takeIf(String::isNotBlank)?.let { author ->
                Row(horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.library_question_author, author), modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodySmall)
                    val rank = AuthorRank.fromKey(question.authoredByRank?.key)
                    AuthorRankBadge(rank)
                    if (rank == null) question.authoredByRank?.label?.takeIf(String::isNotBlank)?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            onRating?.let { open ->
                rating?.let { value ->
                    Text(if (value.count == 0) stringResource(R.string.no_ratings)
                        else stringResource(R.string.rating_average, value.average, com.mukhtabir.android.util.formatNumber(value.count)), style = MaterialTheme.typography.bodySmall)
                }
                AppTextButton(open, enabled = !ratingLoading) { Text(stringResource(R.string.your_rating)) }
            }
            onVerify?.let { AppButton(it, enabled = verifyEnabled) { Text(stringResource(R.string.verify_question)) } }
    }
}
