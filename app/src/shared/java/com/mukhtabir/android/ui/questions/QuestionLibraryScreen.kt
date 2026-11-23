@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.questions

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.LearnerCategories
import com.mukhtabir.android.data.remote.LearnerImages
import com.mukhtabir.android.data.remote.LearnerQuestions
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.ActiveFilter
import com.mukhtabir.android.ui.components.ActiveFilterSummary
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppPagination
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.PracticeLauncher
import com.mukhtabir.android.ui.components.PullToRefresh
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.components.difficultyLabel
import com.mukhtabir.android.ui.theme.AppTheme
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun QuestionLibraryRoute(
    service: LearnerQuestions,
    defaultDifficulty: String,
    categoryService: LearnerCategories,
    imageRepository: LearnerImages,
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
    navigationStore: com.mukhtabir.android.core.LearnerStorage? = null,
    detailContent: (@Composable (Long, (Long?) -> Unit, (Boolean) -> Unit) -> Unit)? = null,
    detailStateOwner: String = "",
) {
    val viewModel: QuestionLibraryViewModel = viewModel(
        key = "question-library-$userId",
        factory = QuestionLibraryViewModel.Factory(service, categoryService, pageSize, maxQuestions, canReview, userId, defaultDifficulty),
    )
    val state by viewModel.state.collectAsLifecycleState()
    var recent by remember(userId,navigationStore) {mutableStateOf(com.mukhtabir.android.core.AuthorNavigation())}
    val navigationScope=androidx.compose.runtime.rememberCoroutineScope()
    val readNavigation: () -> Unit = {navigationScope.launch {
        val value=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {navigationStore?.authorNavigation(userId)?.getOrNull()}
        ensureActive();if(value!=null) recent=value
    }}
    RefreshOnResume(readNavigation)
    val ratingsVm: LibraryRatingsViewModel = viewModel(key = "library-ratings-$userId", factory = LibraryRatingsViewModel.Factory(service))
    val ratings by ratingsVm.state.collectAsLifecycleState()
    var ratingQuestionId by rememberSaveable(userId) { mutableStateOf<Long?>(null) }
    LaunchedEffect(state.items, state.isLoading) {
        if (!state.isLoading && state.error == null) ratingsVm.visible(state.items.map { it.id })
    }
    var showPractice by rememberSaveable(userId) { mutableStateOf(false) }
    var showFilters by rememberSaveable(userId) { mutableStateOf(false) }
    LaunchedEffect(pageSize, maxQuestions, canReview) {
        viewModel.applyConfiguration(pageSize, maxQuestions, canReview)
    }
    RefreshOnResume {viewModel.loadOptions()}
    LaunchedEffect(bankRevision) { if (bankRevision > 0) viewModel.refresh(removedQuestionIds) }
    val defaultSessionLabel = stringResource(R.string.question_library)
    val last=recent.lastQuestionId?.takeUnless {it in removedQuestionIds}
    val sessionIds = if (state.selectedIds.isNotEmpty()) state.selectedIds.toList() else state.items.map { it.id }
    val detailSaver = remember(userId, detailStateOwner) {
        androidx.compose.runtime.saveable.Saver<Long?, List<String>>(
            save = { listOf(detailStateOwner, userId.toString(), it?.toString().orEmpty()) },
            restore = { saved ->
                if (saved.getOrNull(0) == detailStateOwner && saved.getOrNull(1) == userId.toString())
                    saved.getOrNull(2)?.toLongOrNull()?.takeIf { it > 0 } else null
            })
    }
    var detailId by rememberSaveable(userId, detailStateOwner, stateSaver = detailSaver) { mutableStateOf<Long?>(null) }
    var detailLocked by remember(userId) { mutableStateOf(false) }
    LaunchedEffect(removedQuestionIds) { if (detailId?.let { it in removedQuestionIds } == true) detailId = null }
    BackHandler(enabled = detailId != null) { if (!detailLocked) detailId = null }
    val hasActiveFilters = state.mode == LibraryMode.ALL && (state.query.isNotBlank() ||
        state.categoryId != null || state.difficulty != null || state.tag != null ||
        state.verifiedOnly || state.customPageSize != null)
    val canLaunch = !detailLocked && !state.isLoading && state.error == null && sessionIds.size in 1..maxQuestions

    BoxWithConstraints(Modifier.fillMaxSize()) {
    val split = detailContent != null && maxWidth >= AppLayoutTokens.detailPaneBreakpoint * LocalDensity.current.fontScale.coerceAtLeast(1f)
    val openQuestion: (Long) -> Unit = { id ->
        if (!detailLocked) {
            if (split) detailId = id else onOpenQuestion(id)
        }
    }
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
    if (split || detailId == null) Column(Modifier.weight(if (split) AppLayoutTokens.detailListFraction else 1f).fillMaxHeight()) {
    Box(Modifier.weight(1f)) {
    PullToRefresh(state.isLoading, viewModel::retry) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = androidx.compose.foundation.layout.PaddingValues(AppLayoutTokens.libraryPadding),
            verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.libraryGap)) {
            item {
                AppPageHeading(stringResource(R.string.question_library),
                    action = { if (canCreate) AppTextButton(onCreateQuestion, enabled = !detailLocked) { Text(stringResource(R.string.create)) } })
                if (canReview) AppActionRow() {
                    AppFilterChip(state.mode == LibraryMode.ALL, { viewModel.setMode(LibraryMode.ALL) }, label = { Text(stringResource(R.string.all_questions)) })
                    AppFilterChip(state.mode == LibraryMode.REVIEW, { viewModel.setMode(LibraryMode.REVIEW) }, label = { Text(stringResource(R.string.review_queue)) })
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                    if (state.mode == LibraryMode.ALL) AppTextField(state.query, viewModel::setQuery,
                        label = { Text(stringResource(R.string.search_questions)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    AppActionRow() {
                        if (state.mode == LibraryMode.ALL) {
                            AppOutlinedButton({ showFilters = true }) { Text(stringResource(R.string.library_active_filters, com.mukhtabir.android.util.formatNumber(listOf(state.categoryId != null, state.difficulty != null, state.tag != null,
                                    state.verifiedOnly, state.customPageSize != null).count { it }))) }
                            if (hasActiveFilters) AppTextButton(viewModel::resetFilters) { Text(stringResource(R.string.library_reset)) }
                        }
                        if (canStartSessions && state.items.isNotEmpty()) AppTextButton(viewModel::selectPage, enabled = !state.isLoading) {
                            Text(stringResource(if (state.items.all { it.id in state.selectedIds }) R.string.deselect_page else R.string.select_page))
                        }
                        if (canStartSessions && (state.items.isNotEmpty() || state.selectedIds.isNotEmpty())) {
                            AppButton({ showPractice = true }, enabled = canLaunch) { Text(stringResource(R.string.library_practice, com.mukhtabir.android.util.formatNumber(sessionIds.size))) }
                        }
                    }
                    if (state.mode == LibraryMode.ALL) ActiveFilterSummary(
                        buildList {
                            state.categoryId?.let { id -> add(ActiveFilter("category",
                                stringResource(R.string.quiz_filter_category, state.categories.firstOrNull { it.id == id }?.name ?: com.mukhtabir.android.util.formatNumber(id)),
                                { viewModel.removeFilter("category") })) }
                            state.tag?.let { tag -> add(ActiveFilter("tag", stringResource(R.string.quiz_filter_tag, tag), { viewModel.removeFilter("tag") })) }
                            state.difficulty?.let { value -> add(ActiveFilter("difficulty", difficultyLabel(value), { viewModel.removeFilter("difficulty") })) }
                            if (state.verifiedOnly) add(ActiveFilter("verified", stringResource(R.string.verified), { viewModel.removeFilter("verified") }))
                            state.customPageSize?.let { size -> add(ActiveFilter("page", stringResource(R.string.preferences_page) + ": " + com.mukhtabir.android.util.formatNumber(size), { viewModel.removeFilter("page") })) }
                        }, onClear = viewModel::resetFilters, enabled = !state.isLoading)
                    state.mutationError?.let { AppStatus(it) }
                    if (state.selectionLimitReached || sessionIds.size > maxQuestions) {
                        AppStatus(stringResource(R.string.selection_limit, com.mukhtabir.android.util.formatNumber(maxQuestions)))
                    }
                }
            }
            if (state.isLoading) item { AppLoadingIndicator(Modifier.fillMaxWidth()) }
            state.error?.let { error -> item { ErrorState(throwable = error, onRetry = if (state.isLoading) null else viewModel::retry) } }
            if(last!=null) item {
                AppOutlinedButton({openQuestion(last)},enabled=!state.isLoading){Text(stringResource(R.string.library_last_question, com.mukhtabir.android.util.formatNumber(last)))}
                AppTextButton({navigationScope.launch {
                    val cleared=kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {navigationStore?.recordLastQuestion(userId,null)?.isSuccess==true}
                    ensureActive();if(cleared) recent=recent.copy(lastQuestionId=null)
                }},enabled=!state.isLoading){Text(stringResource(R.string.library_clear_last))}
            }
            if (state.items.isEmpty() && !state.isLoading && state.error == null) item {
                EmptyState(stringResource(R.string.no_questions_found),
                    actionLabel = if (hasActiveFilters) stringResource(R.string.library_reset) else null,
                    onAction = if (hasActiveFilters) viewModel::resetFilters else null)
            }
            if (ratings.loading) item { Text(stringResource(R.string.library_ratings_loading)) }
            ratings.error?.let { failure -> item {
                Text(stringResource(R.string.library_ratings_failed))
                ErrorState(throwable = failure, onRetry = if (ratings.loading || ratings.savingId != null) null else ratingsVm::refresh)
            } }
            items(state.items, key = { it.id }) { question ->
                Column(Modifier.fillMaxWidth().then(if (detailId == question.id) Modifier.border(
                    AppLayoutTokens.selectedBorderWidth, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.medium) else Modifier)) {
                QuestionLibraryCard(question, imageRepository, onClick = { openQuestion(question.id) },
                    onVerify = ({ viewModel.verify(question.id) }).takeIf { canReview && !question.verified },
                    verifyEnabled = !detailLocked && !state.isLoading && question.id !in state.verifyingIds,
                    selected = question.id in state.selectedIds,
                    onSelect = ({ viewModel.toggleSelection(question.id) }).takeIf { canStartSessions },
                    selectionEnabled = !state.isLoading,
                    rating = ratings.rows[question.id],
                    ratingLoading = ratings.loading,
                    onRating = ({ ratingQuestionId = question.id }).takeIf { !detailLocked })
                }
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
                AppTextButton(viewModel::clearSelection, enabled = !state.isLoading) { Text(stringResource(R.string.clear_selection)) }
                AppButton({ showPractice = true }, enabled = canLaunch) { Text(stringResource(R.string.library_practice, com.mukhtabir.android.util.formatNumber(sessionIds.size))) }
            }
        }
    }
    }
    if (detailContent != null && (split || detailId != null)) {
        Column(Modifier.weight(if (split) 1f - AppLayoutTokens.detailListFraction else 1f).fillMaxHeight()) {
            val activeId = detailId
            if (activeId == null) EmptyState(stringResource(R.string.ui_select_detail))
            else {
                AppTextButton({ if (!detailLocked) detailId = null }, enabled = !detailLocked) {
                    Text(stringResource(R.string.close))
                }
                Box(Modifier.weight(1f)) {
                    androidx.compose.runtime.key(userId, activeId, bankRevision) {
                        detailContent(activeId, { detailId = it }, { detailLocked = it })
                    }
                }
            }
        }
    }
    }
    }
    if (showPractice && canStartSessions) {
        val ids = remember { sessionIds.toList() }
        val label = remember { state.query.ifBlank { defaultSessionLabel } }
        PracticeLauncher(label, ids, maxQuestions,
            enabled = !detailLocked && !state.isLoading && state.error == null, onDismiss = { showPractice = false },
            onStart = { mode, selected, name -> if (!detailLocked) { showPractice = false; onStartQuestions(mode, selected, name) } },
            onDownload = onDownloadOffline?.let { download -> ({ selected -> if (!detailLocked) { showPractice = false; download(selected) } }) })
    }
    ratingQuestionId?.let { id -> LibraryRatingDialog(id, ratings, ratingsVm) { ratingQuestionId = null } }
    if (showFilters && state.mode == LibraryMode.ALL) LibraryFiltersSheet(
        state = state.copy(categories=state.categories.sortedBy {recent.categories.indexOf(it.id).takeIf {index->index>=0} ?: Int.MAX_VALUE},
            tags=state.tags.sortedBy {recent.tags.indexOf(it).takeIf {index->index>=0} ?: Int.MAX_VALUE}), defaultPageSize = pageSize, onRetry = viewModel::loadOptions,
        onDismiss = { showFilters = false },
        onApply = { category, difficulty, tag, verified, size ->
            viewModel.setFilters(category, difficulty, tag, verified, size)
            showFilters = false
        },
    )
}
