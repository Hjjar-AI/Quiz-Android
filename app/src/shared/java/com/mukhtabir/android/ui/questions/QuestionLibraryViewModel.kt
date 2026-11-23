@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.questions

import android.os.Bundle
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.data.remote.LearnerCategories
import com.mukhtabir.android.data.remote.LearnerQuestions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class QuestionLibraryViewModel(
    private val service: LearnerQuestions,
    private val categoryService: LearnerCategories,
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

    fun removeFilter(key: String) {
        val s = _state.value
        if (s.isLoading) return
        setFilters(if (key == "category") null else s.categoryId,
            if (key == "difficulty") null else s.difficulty,
            if (key == "tag") null else s.tag,
            if (key == "verified") false else s.verifiedOnly,
            if (key == "page") null else s.customPageSize)
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
        private val service: LearnerQuestions,
        private val categoryService: LearnerCategories,
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
