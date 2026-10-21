@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppPagination

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppActionRow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun QuestionLibraryRoute(
    service: QuestionService,
    defaultDifficulty: String,
    categoryService: CategoryService,
    imageRepository: ImageRepository,
    canReview: Boolean,
    canCreate: Boolean,
    canStartSessions: Boolean,
    pageSize: Int,
    maxQuestions: Int,
    bankRevision: Int,
    userId: Long,
    removedQuestionIds: Set<Long>,
    onOpenQuestion: (Long) -> Unit,
    onCreateQuestion: () -> Unit,
    onStartQuestions: (TestMode, List<Long>, String) -> Unit,
    onDownloadOffline: ((List<Long>) -> Unit)? = null,
    navigationStore: com.mukhtabir.android.core.SessionDraftStore? = null,
) {
    val viewModel: QuestionLibraryViewModel = viewModel(
        key = "question-library-$userId",
        factory = QuestionLibraryViewModel.Factory(service, categoryService, pageSize, maxQuestions, canReview, userId, defaultDifficulty),
    )
    val state by viewModel.state.collectAsState()
    var recent by remember(userId,navigationStore) {mutableStateOf(com.mukhtabir.android.core.AuthorNavigation())}
    val navigationScope=androidx.compose.runtime.rememberCoroutineScope()
    val readNavigation: () -> Unit = {navigationScope.launch {
        val value=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {navigationStore?.authorNavigation(userId)?.getOrNull()}
        ensureActive();if(value!=null) recent=value
    }}
    com.mukhtabir.android.ui.components.RefreshOnResume(readNavigation)
    val ratingsVm: LibraryRatingsViewModel = viewModel(key = "library-ratings-$userId", factory = LibraryRatingsViewModel.Factory(service))
    val ratings by ratingsVm.state.collectAsState()
    var ratingQuestionId by rememberSaveable(userId) { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.items, state.isLoading) {
        if (!state.isLoading && state.error == null) ratingsVm.visible(state.items.map { it.id })
    }
    var showPractice by rememberSaveable(userId) { mutableStateOf(false) }
    var showFilters by rememberSaveable(userId) { mutableStateOf(false) }
    LaunchedEffect(pageSize, maxQuestions, canReview) {
        viewModel.applyConfiguration(pageSize, maxQuestions, canReview)
    }
    com.mukhtabir.android.ui.components.RefreshOnResume {viewModel.loadOptions()}
    LaunchedEffect(bankRevision) { if (bankRevision > 0) viewModel.refresh(removedQuestionIds) }
    val defaultSessionLabel = stringResource(R.string.question_library)
    val last=recent.lastQuestionId?.takeUnless {it in removedQuestionIds}
    val sessionIds = if (state.selectedIds.isNotEmpty()) state.selectedIds.toList() else state.items.map { it.id }
    val canLaunch = !state.isLoading && state.error == null && sessionIds.size in 1..maxQuestions

    Column(Modifier.fillMaxSize()) {
    Box(Modifier.weight(1f)) {
    com.mukhtabir.android.ui.components.PullToRefresh(state.isLoading, viewModel::retry) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(AppLayoutTokens.libraryPadding),
            verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.libraryGap)) {
            item {
                AppPageHeading(stringResource(R.string.question_library),
                    action = { if (canCreate) TextButton(onCreateQuestion) { Text(stringResource(R.string.create)) } })
                if (canReview) AppActionRow() {
                    FilterChip(state.mode == LibraryMode.ALL, { viewModel.setMode(LibraryMode.ALL) }, label = { Text(stringResource(R.string.all_questions)) })
                    FilterChip(state.mode == LibraryMode.REVIEW, { viewModel.setMode(LibraryMode.REVIEW) }, label = { Text(stringResource(R.string.review_queue)) })
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                    if (state.mode == LibraryMode.ALL) OutlinedTextField(state.query, viewModel::setQuery,
                        label = { Text(stringResource(R.string.search_questions)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    AppActionRow() {
                        if (state.mode == LibraryMode.ALL) {
                            OutlinedButton({ showFilters = true }) { Text(stringResource(R.string.library_active_filters,
                                listOf(state.categoryId != null, state.difficulty != null, state.tag != null,
                                    state.verifiedOnly, state.customPageSize != null).count { it })) }
                            TextButton(viewModel::resetFilters) { Text(stringResource(R.string.library_reset)) }
                        }
                        if (canStartSessions && state.items.isNotEmpty()) TextButton(viewModel::selectPage, enabled = !state.isLoading) {
                            Text(stringResource(if (state.items.all { it.id in state.selectedIds }) R.string.deselect_page else R.string.select_page))
                        }
                        if (canStartSessions && (state.items.isNotEmpty() || state.selectedIds.isNotEmpty())) {
                            Button({ showPractice = true }) { Text(stringResource(R.string.library_practice, sessionIds.size)) }
                        }
                    }
                    if (state.mode == LibraryMode.ALL) com.mukhtabir.android.ui.components.ActiveFilterSummary(
                        buildList {
                            state.categoryId?.let { id -> add(com.mukhtabir.android.ui.components.ActiveFilter("category",
                                stringResource(R.string.quiz_filter_category, state.categories.firstOrNull { it.id == id }?.name ?: com.mukhtabir.android.util.formatNumber(id)),
                                { viewModel.removeFilter("category") })) }
                            state.tag?.let { tag -> add(com.mukhtabir.android.ui.components.ActiveFilter("tag", stringResource(R.string.quiz_filter_tag, tag), { viewModel.removeFilter("tag") })) }
                            state.difficulty?.let { value -> add(com.mukhtabir.android.ui.components.ActiveFilter("difficulty", com.mukhtabir.android.ui.components.difficultyLabel(value), { viewModel.removeFilter("difficulty") })) }
                            if (state.verifiedOnly) add(com.mukhtabir.android.ui.components.ActiveFilter("verified", stringResource(R.string.verified), { viewModel.removeFilter("verified") }))
                            state.customPageSize?.let { size -> add(com.mukhtabir.android.ui.components.ActiveFilter("page", stringResource(R.string.preferences_page) + ": " + com.mukhtabir.android.util.formatNumber(size), { viewModel.removeFilter("page") })) }
                        }, onClear = viewModel::resetFilters, enabled = !state.isLoading)
                    state.mutationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (state.selectionLimitReached || sessionIds.size > maxQuestions) {
                        Text(stringResource(R.string.selection_limit, maxQuestions), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (state.isLoading) item { AppLoadingIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item { ErrorState(throwable = error, onRetry = if (state.isLoading) null else viewModel::retry) } }
            if(last!=null) item {
                OutlinedButton({onOpenQuestion(last)},enabled=!state.isLoading){Text(stringResource(R.string.library_last_question,last))}
                TextButton({navigationScope.launch {
                    val cleared=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {navigationStore?.recordLastQuestion(userId,null)?.isSuccess==true}
                    ensureActive();if(cleared) recent=recent.copy(lastQuestionId=null)
                }},enabled=!state.isLoading){Text(stringResource(R.string.library_clear_last))}
            }
            if (state.items.isEmpty() && !state.isLoading && state.error == null) item { EmptyState(stringResource(R.string.no_questions_found)) }
            if (ratings.loading) item { Text(stringResource(R.string.library_ratings_loading)) }
            ratings.error?.let { failure -> item {
                Text(stringResource(R.string.library_ratings_failed))
                ErrorState(throwable = failure, onRetry = if (ratings.loading || ratings.savingId != null) null else ratingsVm::refresh)
            } }
            items(state.items, key = { it.id }) { question ->
                QuestionLibraryCard(question, imageRepository, onClick = { onOpenQuestion(question.id) },
                    onVerify = ({ viewModel.verify(question.id) }).takeIf { canReview && !question.verified },
                    verifyEnabled = !state.isLoading && question.id !in state.verifyingIds,
                    selected = question.id in state.selectedIds,
                    onSelect = ({ viewModel.toggleSelection(question.id) }).takeIf { canStartSessions },
                    selectionEnabled = !state.isLoading,
                    rating = ratings.rows[question.id],
                    ratingLoading = ratings.loading,
                    onRating = { ratingQuestionId = question.id })
            }
            if (state.totalPages > 1) item {
                AppPagination(state.page, state.totalPages, onPrevious = viewModel::previousPage, onNext = viewModel::nextPage, previousEnabled = state.page > 1 && !state.isLoading, nextEnabled = state.page < state.totalPages && !state.isLoading, modifier = Modifier.fillMaxWidth())
            }
        }
    }
    }
    if (canStartSessions && state.selectedIds.isNotEmpty()) androidx.compose.material3.Surface(
        color = MaterialTheme.colorScheme.surfaceVariant) {
        Column(Modifier.fillMaxWidth().padding(AppLayoutTokens.cardInsets)) {
            Text(stringResource(R.string.ui_selected_count, com.mukhtabir.android.util.formatNumber(state.selectedIds.size)),
                style = MaterialTheme.typography.labelLarge)
            AppActionRow {
                TextButton(viewModel::clearSelection, enabled = !state.isLoading) { Text(stringResource(R.string.clear_selection)) }
                Button({ showPractice = true }, enabled = canLaunch) { Text(stringResource(R.string.library_practice, sessionIds.size)) }
            }
        }
    }
    }
    if (showPractice && canStartSessions) {
        val ids = remember { sessionIds.toList() }
        val label = remember { state.query.ifBlank { defaultSessionLabel } }
        com.mukhtabir.android.ui.components.PracticeLauncher(label, ids, maxQuestions,
            enabled = !state.isLoading && state.error == null, onDismiss = { showPractice = false },
            onStart = { mode, selected, name -> showPractice = false; onStartQuestions(mode, selected, name) },
            onDownload = onDownloadOffline?.let { download -> ({ selected -> showPractice = false; download(selected) }) })
    }
    ratingQuestionId?.let { id -> LibraryRatingDialog(id, ratings, ratingsVm) { ratingQuestionId = null } }
    if (showFilters && state.mode == LibraryMode.ALL) LibraryFiltersDialog(
        state = state.copy(categories=state.categories.sortedBy {recent.categories.indexOf(it.id).takeIf {index->index>=0} ?: Int.MAX_VALUE},
            tags=state.tags.sortedBy {recent.tags.indexOf(it).takeIf {index->index>=0} ?: Int.MAX_VALUE}), defaultPageSize = pageSize, onRetry = viewModel::loadOptions,
        onDismiss = { showFilters = false },
        onApply = { category, difficulty, tag, verified, size ->
            viewModel.setFilters(category, difficulty, tag, verified, size)
            showFilters = false
        },
    )
}
