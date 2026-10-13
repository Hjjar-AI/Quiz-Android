@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.ui.components.ExplanationCard
import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPagination

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerTextButton

import com.mukhtabir.android.ui.components.AppActionRow

import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import androidx.compose.material3.Checkbox
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.BookmarkService
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.QuestionContext
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.QuestionRatingRowDto
import com.mukhtabir.android.ui.profile.AuthorRank
import com.mukhtabir.android.ui.profile.AuthorRankBadge
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.QuestionImage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class LibraryMode { ALL, REVIEW }

data class QuestionLibraryUiState(
    val items: List<QuestionDto> = emptyList(),
    val query: String = "",
    val categoryId: Long? = null,
    val difficulty: String? = null,
    val tag: String? = null,
    val verifiedOnly: Boolean = false,
    val customPageSize: Int? = null,
    val categories: List<CategoryDto> = emptyList(),
    val tags: List<String> = emptyList(),
    val optionsLoading: Boolean = false,
    val optionsError: Throwable? = null,
    val mode: LibraryMode = LibraryMode.ALL,
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val selectedIds: Set<Long> = emptySet(),
    val selectionLimitReached: Boolean = false,
    val verifyingIds: Set<Long> = emptySet(),
    val mutationError: String? = null,
    val isLoading: Boolean = false,
    val error: Throwable? = null,
)

class QuestionLibraryViewModel(
    private val service: QuestionService,
    private val categoryService: CategoryService,
    private var pageSize: Int,
    private var maxQuestions: Int,
    private var canReview: Boolean,
    private val userId: Long,
    private val savedStateHandle: SavedStateHandle,
    private val defaultDifficulty: String,
) : ViewModel() {
    private val _state = MutableStateFlow(restoreFilters())
    val state = _state.asStateFlow()
    private var searchJob: Job? = null
    private var loadJob: Job? = null
    private var optionsJob: Job? = null
    private var requestedPage = _state.value.page

    init { load(_state.value.page); loadOptions() }

    fun loadOptions() {
        optionsJob?.cancel()
        _state.update { it.copy(optionsLoading = true, optionsError = null) }
        optionsJob = viewModelScope.launch {
            try {
                val categories = categoryService.list().items
                val tags = service.tags().items.map { it.name }
                ensureActive()
                val before=_state.value
                updateFilters { current -> current.copy(categories=categories,tags=tags,optionsLoading=false,
                    categoryId=current.categoryId?.takeIf {id -> categories.any {it.id==id}},
                    tag=current.tag?.takeIf {it in tags}) }
                if(before.categoryId!=_state.value.categoryId || before.tag!=_state.value.tag) load(1)
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                ensureActive()
                _state.update { it.copy(optionsLoading = false, optionsError = failure) }
            }
        }
    }

    fun setFilters(categoryId: Long?, difficulty: String?, tag: String?, verifiedOnly: Boolean, size: Int?) {
        if (difficulty != null && difficulty !in setOf("easy", "medium", "hard")) return
        if (size != null && size !in 1..500) return
        searchJob?.cancel()
        loadJob?.cancel()
        updateFilters { it.copy(categoryId = categoryId?.takeIf { id -> id > 0 },
            difficulty = difficulty, tag = tag?.takeIf(String::isNotBlank), verifiedOnly = verifiedOnly,
            customPageSize = size, page = 1, items = emptyList(), total = 0, totalPages = 1,
            selectedIds = emptySet(), selectionLimitReached = false) }
        load(1)
    }

    fun resetFilters() {
        updateFilters { it.copy(query = "") }
        setFilters(null, null, null, false, null)
    }

    fun applyConfiguration(pageSize: Int, maxQuestions: Int, canReview: Boolean) {
        val size = pageSize.coerceAtLeast(1)
        val maximum = maxQuestions.coerceAtLeast(1)
        if (this.pageSize == size && this.maxQuestions == maximum && this.canReview == canReview) return
        this.pageSize = size
        this.maxQuestions = maximum
        this.canReview = canReview
        searchJob?.cancel()
        updateFilters {
            it.copy(
                mode = if (it.mode == LibraryMode.REVIEW && !canReview) LibraryMode.ALL else it.mode,
                selectedIds = it.selectedIds.take(maximum).toSet(),
                selectionLimitReached = false,
            )
        }
        load()
    }

    fun setMode(mode: LibraryMode) {
        if (_state.value.mode == mode || (mode == LibraryMode.REVIEW && !canReview)) return
        searchJob?.cancel()
        _state.update { it.copy(mode = mode, page = 1, items = emptyList(), total = 0,
            totalPages = 1, selectedIds = emptySet(), selectionLimitReached = false) }
        persistFilters()
        load(1)
    }

    fun setQuery(query: String) {
        searchJob?.cancel()
        loadJob?.cancel()
        _state.update { it.copy(query = query, page = 1, items = emptyList(), total = 0,
            totalPages = 1, selectedIds = emptySet(), selectionLimitReached = false, isLoading = true, error = null) }
        persistFilters()
        searchJob = viewModelScope.launch {
            delay(350)
            load(1)
        }
    }

    fun load(page: Int = _state.value.page) {
        val snapshot = _state.value
        loadJob?.cancel()
        val targetPage = page.coerceAtLeast(1)
        requestedPage = targetPage
        _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            runCatching {
                when (snapshot.mode) {
                    LibraryMode.ALL -> service.list(
                        page = targetPage,
                        perPage = snapshot.customPageSize ?: pageSize,
                        search = snapshot.query.trim().takeIf(String::isNotEmpty),
                        categoryId = snapshot.categoryId,
                        difficulty = snapshot.difficulty,
                        tag = snapshot.tag,
                        verifiedOnly = snapshot.verifiedOnly,
                    )
                    LibraryMode.REVIEW -> service.unverified(targetPage, snapshot.customPageSize ?: pageSize)
                }
            }.onSuccess { response ->
                ensureActive()
                if (response.page > response.totalPages) {
                    load(response.totalPages.coerceAtLeast(1))
                    return@onSuccess
                }
                _state.update {
                    it.copy(
                        items = response.items,
                        page = response.page,
                        totalPages = response.totalPages,
                        total = response.total,
                        isLoading = false,
                    )
                }
                persistFilters()
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                ensureActive()
                _state.update { it.copy(isLoading = false, error = failure) }
            }
        }
    }

    fun retry() = load(requestedPage)

    fun refresh(removedIds: Set<Long>) {
        loadOptions()
        searchJob?.cancel()
        _state.update { it.copy(selectedIds = it.selectedIds - removedIds,
            selectionLimitReached = false, mutationError = null) }
        persistFilters()
        load()
    }

    fun previousPage() = _state.value.let { if (it.page > 1) load(it.page - 1) else Unit }
    fun nextPage() = _state.value.let { if (it.page < it.totalPages) load(it.page + 1) else Unit }

    fun toggleSelection(id: Long) = updateFilters {
        when {
            it.isLoading || it.items.none { question -> question.id == id } -> it
            id in it.selectedIds -> it.copy(selectedIds = it.selectedIds - id, selectionLimitReached = false)
            it.selectedIds.size >= maxQuestions -> it.copy(selectionLimitReached = true)
            else -> it.copy(selectedIds = it.selectedIds + id, selectionLimitReached = false)
        }
    }

    fun selectPage() = updateFilters { state ->
        if (state.isLoading) state else {
            val pageIds = state.items.map { it.id }
            if (pageIds.all { it in state.selectedIds }) state.copy(
                selectedIds = state.selectedIds - pageIds.toSet(), selectionLimitReached = false,
            ) else {
                val available = pageIds.filterNot { it in state.selectedIds }
                val remaining = (maxQuestions - state.selectedIds.size).coerceAtLeast(0)
                state.copy(selectedIds = state.selectedIds + available.take(remaining),
                    selectionLimitReached = available.size > remaining)
            }
        }
    }

    fun clearSelection() = updateFilters { it.copy(selectedIds = emptySet(), selectionLimitReached = false) }

    fun verify(id: Long) {
        val currentState = _state.value
        if (!canReview || id in currentState.verifyingIds || currentState.isLoading ||
            currentState.items.none { it.id == id && !it.verified }) return
        _state.update { it.copy(verifyingIds = it.verifyingIds + id, mutationError = null) }
        viewModelScope.launch {
            try {
                // The endpoint toggles. Read before every attempt so a lost successful
                // response does not blindly toggle the question back on retry.
                val current = service.get(id)
                ensureActive()
                if (!canReview) return@launch
                if (!current.verified) service.verify(id)
                ensureActive()
                load()
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                ensureActive()
                _state.update { it.copy(mutationError = failure.message) }
                // Read only; an uncertain toggle must never be automatically replayed.
                load()
            } finally {
                _state.update { it.copy(verifyingIds = it.verifyingIds - id) }
            }
        }
    }

    private fun restoreFilters(): QuestionLibraryUiState {
        val saved = savedStateHandle.get<Bundle>("library_filters")
            ?.takeIf { it.getLong("user_id", -1L) == userId }
            ?: return QuestionLibraryUiState(difficulty = defaultDifficulty.takeIf { it.isNotBlank() }).also { savedStateHandle.remove<Bundle>("library_filters") }
        val mode = LibraryMode.values().firstOrNull { it.name == saved.getString("mode") }
            ?.takeUnless { it == LibraryMode.REVIEW && !canReview } ?: LibraryMode.ALL
        return QuestionLibraryUiState(
            query = saved.getString("query").orEmpty(),
            categoryId = saved.getLong("category", -1).takeIf { it > 0 },
            difficulty = saved.getString("difficulty")?.takeIf { it in setOf("easy", "medium", "hard") },
            tag = saved.getString("tag")?.takeIf(String::isNotBlank),
            verifiedOnly = saved.getBoolean("verified"),
            customPageSize = saved.getInt("page_size", 0).takeIf { it in 1..500 },
            mode = mode,
            page = saved.getInt("page", 1).coerceAtLeast(1),
            selectedIds = saved.getLongArray("selection")?.filter { it > 0 }
                ?.take(maxQuestions)?.toSet().orEmpty(),
        )
    }

    private fun updateFilters(transform: (QuestionLibraryUiState) -> QuestionLibraryUiState) {
        _state.update(transform)
        persistFilters()
    }

    private fun persistFilters() {
        val current = _state.value
        savedStateHandle["library_filters"] = Bundle().apply {
            putLong("user_id", userId)
            putString("query", current.query)
            putLong("category", current.categoryId ?: -1)
            putString("difficulty", current.difficulty)
            putString("tag", current.tag)
            putBoolean("verified", current.verifiedOnly)
            putInt("page_size", current.customPageSize ?: 0)
            putString("mode", current.mode.name)
            putInt("page", current.page)
            putLongArray("selection", current.selectedIds.toLongArray())
        }
    }

    class Factory(
        private val service: QuestionService,
        private val categoryService: CategoryService,
        private val pageSize: Int,
        private val maxQuestions: Int,
        private val canReview: Boolean,
        private val userId: Long, private val defaultDifficulty: String,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            QuestionLibraryViewModel(service, categoryService, pageSize.coerceAtLeast(1), maxQuestions.coerceAtLeast(1),
                canReview, userId, extras.createSavedStateHandle(), defaultDifficulty) as T
    }
}

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
                        if (canStartSessions && (state.items.isNotEmpty() || state.selectedIds.isNotEmpty())) {
                            Button({ showPractice = true }) { Text(stringResource(R.string.library_practice, sessionIds.size)) }
                        }
                    }
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
    if (showPractice && canStartSessions) AlertDialog(
        onDismissRequest = { showPractice = false },
        title = { Text(stringResource(R.string.library_practice_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                Text(if (state.selectedIds.isEmpty()) stringResource(R.string.selection_page_fallback)
                    else stringResource(R.string.selected_question_count, state.selectedIds.size))
                AppActionRow() {
                    TextButton(viewModel::selectPage, enabled = !state.isLoading && state.items.isNotEmpty()) {
                        Text(stringResource(if (state.items.isNotEmpty() && state.items.all { it.id in state.selectedIds }) R.string.deselect_page else R.string.select_page))
                    }
                    TextButton(viewModel::clearSelection, enabled = state.selectedIds.isNotEmpty() && !state.isLoading) { Text(stringResource(R.string.clear_selection)) }
                }
                if (state.selectionLimitReached || sessionIds.size > maxQuestions) Text(stringResource(R.string.selection_limit, maxQuestions), color = MaterialTheme.colorScheme.error)
                Button({
                    showPractice = false
                    onStartQuestions(TestMode.STUDY, sessionIds, state.query.ifBlank { defaultSessionLabel })
                }, enabled = canLaunch, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_study)) }
                OutlinedButton({
                    showPractice = false
                    onStartQuestions(TestMode.EXAM, sessionIds, state.query.ifBlank { defaultSessionLabel })
                }, enabled = canLaunch, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_exam)) }
                onDownloadOffline?.let { download ->
                    OutlinedButton({ showPractice = false; download(sessionIds) }, enabled = canLaunch,
                        modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.offline_download)) }
                }
            }
        },
        confirmButton = { TextButton({ showPractice = false }) { Text(stringResource(R.string.close)) } },
    )
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

@Composable
private fun LibraryFiltersDialog(
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_filters)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(verified, { verified = it })
                    Text(stringResource(R.string.verified_only), Modifier.weight(1f))
                }
                LibraryFilterChoice(stringResource(R.string.library_page_size), size,
                    listOf("" to stringResource(R.string.library_default_page_size, defaultPageSize)) +
                        listOf(10, 20, 50, 100).map { it.toString() to it.toString() }) { size = it }
            }
        },
        confirmButton = { TextButton({ onApply(category.toLongOrNull(), difficulty.takeIf(String::isNotBlank),
            tag.takeIf(String::isNotBlank), verified, size.toIntOrNull()) }) { Text(stringResource(R.string.library_apply)) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun LibraryFilterChoice(label: String, value: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton({ expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.library_filter_value, label,
                options.firstOrNull { it.first == value }?.second ?: value))
        }
        DropdownMenu(expanded, { expanded = false }, modifier = Modifier.heightIn(max = 280.dp)) {
            options.forEach { (key, text) ->
                DropdownMenuItem(text = { Text(if (key == value) "✓ $text" else text) },
                    modifier = Modifier.semantics { selected = key == value },
                    onClick = { onSelect(key); expanded = false })
            }
        }
    }
}

@Composable
private fun QuestionLibraryCard(
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
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Column(Modifier.padding(AppLayoutTokens.libraryCardPadding), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            onSelect?.let { select ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = selected, onCheckedChange = { select() }, enabled = selectionEnabled)
                    Text(stringResource(R.string.select_question_number, question.id))
                }
            }
            ClinicalCaseContext(question.localizedForDisplay().case)
            MarkdownText(question.localizedForDisplay().question, Modifier.fillMaxWidth())
            QuestionContext(question)
            QuestionImage(question.imageUrl, imageRepository)
            val metadata = listOfNotNull(
                question.categoryName,
                question.difficulty.takeIf(String::isNotBlank),
                stringResource(if (question.verified) R.string.verified else R.string.unverified),
            )
            com.mukhtabir.android.ui.components.AppMetadataRow(metadata)
            question.authoredByUsername?.takeIf(String::isNotBlank)?.let { author ->
                Row(horizontalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap), verticalAlignment = Alignment.CenterVertically) {
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
                        else stringResource(R.string.rating_average, value.average, value.count), style = MaterialTheme.typography.bodySmall)
                }
                TextButton(open, enabled = !ratingLoading) { Text(stringResource(R.string.your_rating)) }
            }
            onVerify?.let { Button(it, enabled = verifyEnabled) { Text(stringResource(R.string.verify_question)) } }
        }
    }
}

enum class BookmarkChange { ADDED, REMOVED }

data class QuestionDetailUiState(
    val question: QuestionDto? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val bookmarkChange: BookmarkChange? = null,
    val bookmarkBusy: Boolean = false,
    val actionError: String? = null,
    val mutationBusy: Boolean = false,
    val duplicatedId: Long? = null,
    val duplicateUncertain: Boolean = false,
    val deleted: Boolean = false,
    val deleteConflict: Boolean = false,
)

@kotlinx.serialization.Serializable
internal data class DetailRecovery(val bookmark: Boolean? = null, val operation: String? = null)

internal class QuestionDetailViewModel(
    private val questionId: Long,
    private val questionService: QuestionService,
    private val bookmarkService: BookmarkService,
    private val userId: Long,
    private var canDuplicate: Boolean,
    private var canDeleteOwn: Boolean,
    private var canDeleteAny: Boolean,
    private val store: com.mukhtabir.android.core.SessionDraftStore,
) : ViewModel() {
    private val _state = MutableStateFlow(QuestionDetailUiState())
    val state = _state.asStateFlow()

    fun permissions(duplicate: Boolean, deleteOwn: Boolean, deleteAny: Boolean) {canDuplicate=duplicate;canDeleteOwn=deleteOwn;canDeleteAny=deleteAny}
    private var pendingBookmark: Boolean? = null
    private var duplicateOperation: String? = null
    val recovery = com.mukhtabir.android.core.EditorRecovery(viewModelScope, store, userId,
        "question_detail-$questionId", DetailRecovery.serializer())
    init { load() }
    private fun snapshot() = if(pendingBookmark != null || duplicateOperation != null) DetailRecovery(pendingBookmark, duplicateOperation) else null
    fun restore(value: DetailRecovery) {
        pendingBookmark = value.bookmark; duplicateOperation = value.operation
        _state.update { it.copy(duplicateUncertain = value.operation != null) }
    }

    fun load() {
        if (_state.value.isLoading || _state.value.mutationBusy || _state.value.bookmarkBusy) return
        _state.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            runCatching { questionService.get(questionId) }
                .onSuccess { question ->
                    ensureActive()
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {store.recordLastQuestion(userId,question.id)}
                    ensureActive()
                    _state.value = QuestionDetailUiState(question = question, isLoading = false, duplicateUncertain = duplicateOperation != null)
                }
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    _state.update { it.copy(isLoading = false, error = failure.message) }
                }
        }
    }

    fun toggleBookmark() {
        if (recovery.blocked || duplicateOperation != null || _state.value.bookmarkBusy || _state.value.mutationBusy || _state.value.isLoading || _state.value.question == null) return
        _state.update { it.copy(bookmarkBusy = true, actionError = null, bookmarkChange = null) }
        viewModelScope.launch {
            try {
                val actual = bookmarkService.state(questionId)
                val desired = pendingBookmark ?: !actual
                pendingBookmark = desired
                if(!recovery.persist(snapshot())) return@launch
                // An explicit-state write is safe, but restored intent is first checked against live state.
                val added = if(actual == desired) actual else bookmarkService.set(questionId, desired)
                pendingBookmark = null
                recovery.persist(snapshot())
                _state.update { it.copy(bookmarkChange = if (added) BookmarkChange.ADDED else BookmarkChange.REMOVED) }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                _state.update { it.copy(actionError = failure.message) }
            } finally {
                _state.update { it.copy(bookmarkBusy = false) }
            }
        }
    }

    fun duplicate() = mutate(delete = false)
    fun retryDuplicate() { if (duplicateOperation != null) mutate(delete = false, retry = true) }
    fun delete(version: Int) = mutate(delete = true, deleteVersion=version)

    private fun mutate(delete: Boolean, retry: Boolean = false, deleteVersion: Int? = null) {
        val s = _state.value
        val question = s.question
        if (recovery.blocked || pendingBookmark != null || s.mutationBusy || s.bookmarkBusy || s.isLoading || s.deleted || s.duplicatedId != null) return
        if (delete) {
            if (question == null || (!canDeleteAny && !(canDeleteOwn && question.ownedBy == userId))) return
        } else if (!canDuplicate || (question == null && (duplicateOperation == null || retry))) return
        val reconcilingDuplicate = duplicateOperation != null
        _state.update { it.copy(mutationBusy = true, actionError = null, deleteConflict = false) }
        viewModelScope.launch {
            try {
                if (delete) {
                    questionService.delete(questionId,requireNotNull(deleteVersion))
                    _state.update { it.copy(deleted = true) }
                } else {
                    val pending = duplicateOperation
                    val copy = if (pending != null && !retry) {
                        // Resolve a lost response with a read; never blindly create again.
                        questionService.duplicateReceipt(questionId, pending)
                    } else {
                        val operation = pending ?: java.util.UUID.randomUUID().toString()
                        duplicateOperation = operation
                        if(!recovery.persist(snapshot())) return@launch
                        questionService.duplicate(questionId, operation)
                    }
                    duplicateOperation = null
                    recovery.persist(snapshot())
                    _state.update { it.copy(duplicatedId = copy.id, duplicateUncertain = false) }
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (failure: Exception) {
                if (!delete && !reconcilingDuplicate && failure is ApiException && failure.statusCode in setOf(400, 403, 404, 409, 429)) duplicateOperation = null
                recovery.persist(snapshot())
                _state.update { it.copy(actionError = failure.message, duplicateUncertain = duplicateOperation != null,
                    deleteConflict = delete && failure is ApiException && failure.statusCode == 409) }
            } finally {
                _state.update { it.copy(mutationBusy = false) }
            }
        }
    }

    fun consumeMutation() = _state.update { it.copy(duplicatedId = null, deleted = false) }

    class Factory(
        private val questionId: Long,
        private val questionService: QuestionService,
        private val bookmarkService: BookmarkService,
        private val userId: Long, private val canDuplicate: Boolean,
        private val canDeleteOwn: Boolean, private val canDeleteAny: Boolean,
        private val store: com.mukhtabir.android.core.SessionDraftStore,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            QuestionDetailViewModel(questionId, questionService, bookmarkService, userId, canDuplicate, canDeleteOwn, canDeleteAny, store) as T
    }
}

@Composable
fun QuestionDetailRoute(
    questionId: Long,
    questionService: QuestionService,
    bookmarkService: BookmarkService,
    store: com.mukhtabir.android.core.SessionDraftStore,
    imageRepository: ImageRepository,
    canStartSessions: Boolean,
    canEditAny: Boolean,
    canEditOwn: Boolean,
    canUploadImageOwn: Boolean,
    canReviewQuestion: Boolean,
    canDuplicate: Boolean,
    canDeleteOwn: Boolean,
    canDeleteAny: Boolean,
    bankRevision: Int,
    userId: Long,
    onStart: (TestMode, List<Long>, String) -> Unit,
    onEdit: (Long) -> Unit,
    onManageImage: (Long) -> Unit,
    onOpenKnowledge: (Long) -> Unit,
    onOpenCase: (String) -> Unit,
    onDuplicated: (Long) -> Unit,
    onDeleted: (Long) -> Unit,
    onMutationBusyChanged: (Boolean) -> Unit,
    onShare: ((Long) -> Unit)? = null,
) {
    val viewModel: QuestionDetailViewModel = viewModel(
        key = "question-$userId-$questionId-$bankRevision",
        factory = QuestionDetailViewModel.Factory(questionId, questionService, bookmarkService, userId, canDuplicate, canDeleteOwn, canDeleteAny, store),
    )
    viewModel.permissions(canDuplicate,canDeleteOwn,canDeleteAny)
    val state by viewModel.state.collectAsState()
    val recoveryBlocked = com.mukhtabir.android.ui.components.editorRecoveryBlocked(viewModel.recovery)
    DisposableEffect(state.mutationBusy) {
        onMutationBusyChanged(state.mutationBusy)
        onDispose { onMutationBusyChanged(false) }
    }
    val detailSessionLabel = stringResource(R.string.question_details)
    var readingLocale by rememberSaveable(questionId) { mutableStateOf<String?>(null) }
    var confirmDelete by rememberSaveable(questionId) { mutableStateOf(false) }
    var deleteVersion by rememberSaveable(questionId) { mutableStateOf<Int?>(null) }
    var confirmDuplicate by rememberSaveable(questionId) { mutableStateOf(false) }
    LaunchedEffect(state.duplicatedId, state.deleted) {
        state.duplicatedId?.let { id -> viewModel.consumeMutation(); onDuplicated(id) }
        if (state.deleted) { viewModel.consumeMutation(); onDeleted(questionId) }
    }
    com.mukhtabir.android.ui.components.BlockedBackHandler(enabled = state.mutationBusy)
    if (confirmDelete || confirmDuplicate) AlertDialog(
        onDismissRequest = { confirmDelete = false; confirmDuplicate = false },
        title = { Text(stringResource(if (confirmDelete) R.string.delete_question else R.string.duplicate_question)) },
        text = { Text(stringResource(if (confirmDelete) R.string.delete_question_confirmation else R.string.duplicate_question_confirmation)) },
        confirmButton = {
            AppDangerTextButton({
                if (confirmDelete) deleteVersion?.let(viewModel::delete) else viewModel.duplicate()
                confirmDelete = false; confirmDuplicate = false
            }, enabled = !recoveryBlocked && !state.mutationBusy && !state.bookmarkBusy && !state.isLoading) {
                Text(stringResource(if (confirmDelete) R.string.delete else R.string.duplicate_question))
            }
        },
        dismissButton = { TextButton({ confirmDelete = false; confirmDuplicate = false }) { Text(stringResource(R.string.cancel)) } },
    )

    Column(Modifier.fillMaxSize()) {
        com.mukhtabir.android.ui.components.EditorRecoveryControls(viewModel.recovery, state.mutationBusy || state.bookmarkBusy, viewModel::restore)
        if(state.question == null && state.duplicateUncertain && canDuplicate) {
            OutlinedButton(viewModel::duplicate, enabled = !recoveryBlocked && !state.mutationBusy) { Text(stringResource(R.string.check_previous_copy)) }
            state.actionError?.let { Text(it, color=MaterialTheme.colorScheme.error) }
        }
        Box(Modifier.weight(1f)) {
        when {
        state.isLoading -> Column(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets)) { AppLoadingIndicator() }
        state.error != null -> ErrorState(
            message = state.error,
            onRetry = viewModel::load,
            modifier = Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),
        )
        state.question != null -> {
            val question = requireNotNull(state.question)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
            ) {
                item { AppPageHeading(stringResource(R.string.question_details)) }
                val display = question.localizedForDisplay(readingLocale ?: java.util.Locale.getDefault().toLanguageTag())
                item { ClinicalCaseContext(display.case) }
                item { com.mukhtabir.android.ui.components.QuestionLanguageSelector(question.translations, question.choices.size,
                    readingLocale, { readingLocale = it }, hasCase = question.case?.stem?.isNotBlank() == true, caseTranslations = question.case?.translations ?: kotlinx.serialization.json.JsonObject(emptyMap()),
                    explanationAvailable = !question.explanation.isNullOrBlank() && !question.answersHidden,
                    explanationTranslated = !display.explanation.isNullOrBlank()) }
                item { com.mukhtabir.android.ui.components.QuestionTextCard(display.question) }
                item { QuestionImage(question.imageUrl, imageRepository) }
                items(display.choices.withIndex().toList()) { (index, choice) ->
                    com.mukhtabir.android.ui.components.QuestionChoiceCard {
                        Text("${com.mukhtabir.android.util.formatNumber(index + 1)}.")
                        Column(Modifier.weight(1f)) {
                            MarkdownText(choice)
                            if(canReviewQuestion && display.correctAnswer==index+1) Text(stringResource(R.string.correct_answer))
                        }
                    }
                }
                item { QuestionContext(question) }
                if(!question.answersHidden) {
                    question.knowledgeObject?.let { id -> item {
                        OutlinedButton({onOpenKnowledge(id)},enabled=!state.isLoading && !state.mutationBusy && !state.bookmarkBusy) { Text(stringResource(R.string.explore_knowledge_detail)) }
                    } }
                    question.case?.key?.takeIf(String::isNotBlank)?.let { key -> item {
                        OutlinedButton({onOpenCase(key)},enabled=!state.isLoading && !state.mutationBusy && !state.bookmarkBusy) { Text(stringResource(R.string.explore_open_case)) }
                    } }
                }

                item {
                    AppSection(stringResource(R.string.section_verification)) {
                    Text(stringResource(if(question.verified) R.string.verified else R.string.unverified))
                    question.verifiedBy?.takeIf(String::isNotBlank)?.let { Text(stringResource(R.string.moderation_verified_by,it)) }
                    question.verifiedAt?.let { Text(it,style=MaterialTheme.typography.bodySmall) }
                    if(!question.answersHidden) question.verificationNotes?.takeIf(String::isNotBlank)?.let {
                        Text(stringResource(R.string.moderation_existing_notes,it))
                    }
                    }
                }
                if(canReviewQuestion && !question.answersHidden) item {
                    AppSection(stringResource(R.string.moderation_question_metadata)) {
                        Text(stringResource(R.string.moderation_original_author, question.authoredByUsername ?: question.authoredBy?.toString() ?: stringResource(R.string.none)))
                        Text(stringResource(R.string.moderation_current_owner, question.ownedByUsername ?: question.ownedBy?.toString() ?: stringResource(R.string.none)))
                        Text(stringResource(R.string.moderation_version, question.version))
                        question.createdAt?.let { Text(stringResource(R.string.moderation_created, com.mukhtabir.android.util.formatLocalDateTime(it))) }
                        question.updatedAt?.let { Text(stringResource(R.string.moderation_updated, com.mukhtabir.android.util.formatLocalDateTime(it))) }
                        question.updatedBy?.let { Text(stringResource(R.string.moderation_updated_by, it.toString())) }
                        question.lastRevisedAt?.let { Text(stringResource(R.string.moderation_last_revised, it)) }
                        Text(stringResource(R.string.moderation_answer_totals, question.timesAnswered, question.timesCorrect))
                    }
                }
                display.explanation?.takeIf(String::isNotBlank)?.let { explanation ->
                    item {
                        AppSection(stringResource(R.string.explanation), initiallyExpanded = true) {
                            ExplanationCard(explanation)
                        }
                    }
                }
                item {
                    com.mukhtabir.android.ui.components.AppMetadataRow(listOfNotNull(
                        question.categoryName,question.difficulty,question.tags.joinToString().takeIf(String::isNotBlank)))
                }
                item {
                    AppSection(stringResource(R.string.section_question_actions)) {
                        val idle = !recoveryBlocked && !state.mutationBusy && !state.bookmarkBusy
                        onShare?.let { share -> OutlinedButton({ share(question.id) }, enabled = idle) { Text(stringResource(R.string.link_share_question)) } }
                        if (canStartSessions) {
                            Button({ onStart(TestMode.STUDY, listOf(question.id), "$detailSessionLabel #${question.id}") }, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_study)) }
                            OutlinedButton({ onStart(TestMode.EXAM, listOf(question.id), "$detailSessionLabel #${question.id}") }, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.start_exam)) }
                        }
                        if (canEditAny || (canEditOwn && question.ownedBy == userId)) Button({ onEdit(question.id) }, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.edit_question)) }
                        if (canEditAny || (canUploadImageOwn && question.ownedBy == userId)) OutlinedButton({ onManageImage(question.id) }, enabled = idle, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.question_manage_image)) }
                        if (state.mutationBusy) AppLoadingIndicator()
                        if (canDuplicate) OutlinedButton({ if (state.duplicateUncertain) viewModel.duplicate() else confirmDuplicate = true }, enabled = idle) { Text(stringResource(if (state.duplicateUncertain) R.string.check_previous_copy else R.string.duplicate_question)) }
                        if (canDuplicate && state.duplicateUncertain) OutlinedButton(viewModel::retryDuplicate, enabled = idle) { Text(stringResource(R.string.retry_same_request)) }
                        if (canDeleteAny || (canDeleteOwn && question.ownedBy == userId)) OutlinedButton({ deleteVersion=question.version;confirmDelete = true }, enabled = idle) { Text(stringResource(R.string.delete_question), color = MaterialTheme.colorScheme.error) }
                        if (state.deleteConflict) {
                            Text(stringResource(R.string.content_conflict), color = MaterialTheme.colorScheme.error)
                            OutlinedButton(viewModel::load,enabled=idle) {Text(stringResource(R.string.refresh))}
                        }
                    }
                }
                item { QuestionFeedback(questionId, userId, questionService, store) }
                item {
                    OutlinedButton(onClick = viewModel::toggleBookmark, enabled = !recoveryBlocked && !state.bookmarkBusy && !state.mutationBusy, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.toggle_bookmark))
                    }
                    state.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    state.bookmarkChange?.let { change ->
                        Text(stringResource(if (change == BookmarkChange.ADDED) R.string.bookmark_added else R.string.bookmark_removed))
                    }
                }
            }
        }
    }
        }
    }
}
