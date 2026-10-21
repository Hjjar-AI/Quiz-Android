package com.mukhtabir.android.ui.history

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.core.LearnerSavedState
import com.mukhtabir.android.data.remote.HistoryService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val service: HistoryService,
    private val scope: HistoryScope,
    private val signedInUserId: Long,
    private var pageSize: Int,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val saved = LearnerSavedState(savedStateHandle, signedInUserId, "history_${scope.name}_navigation")
    private val _state = MutableStateFlow(HistoryUiState(
        page = saved.page(),
        expandedId = saved.expandedId(),
        userFilter = if (scope == HistoryScope.ALL) saved.draftFilter().orEmpty() else "",
        appliedUserFilter = if (scope == HistoryScope.ALL) saved.filter().orEmpty() else "",
    ))
    val state = _state.asStateFlow()
    private var requestedPage = saved.page()
    private var loadJob: Job? = null
    private var detailJob: Job? = null

    init { load() }

    fun applyPageSize(value: Int) {
        val normalized = value.coerceAtLeast(1)
        if (pageSize == normalized) return
        pageSize = normalized
        loadJob?.cancel()
        _state.update { it.copy(isLoading = false) }
        load(requestedPage)
    }

    fun load(page: Int = requestedPage) {
        if (_state.value.isLoading) return
        // Pagination/retry use the applied filter, never an unsubmitted draft.
        val requestedUserId = when (scope) {
            HistoryScope.OWN -> signedInUserId
            HistoryScope.ALL -> {
                val filter = _state.value.appliedUserFilter
                if (filter.isBlank()) null else filter.toLongOrNull()?.takeIf { it > 0 } ?: run {
                    _state.update { it.copy(invalidUserFilter = true) }
                    return
                }
            }
        }
        requestedPage = page.coerceAtLeast(1)
        saved.savePage(requestedPage)
        val size = pageSize
        _state.update { it.copy(isLoading = true, error = null) }
        loadJob = viewModelScope.launch {
            runCatching {
                var response = service.list(page = requestedPage, perPage = size, userId = requestedUserId)
                while (response.page > response.totalPages.coerceAtLeast(1)) {
                    ensureActive()
                    requestedPage = response.totalPages.coerceAtLeast(1)
                    saved.savePage(requestedPage)
                    response = service.list(page = requestedPage, perPage = size, userId = requestedUserId)
                }
                response
            }.onSuccess { response ->
                ensureActive()
                val expanded = _state.value.expandedId?.takeIf { id -> response.items.any { it.id == id } }
                if (expanded == null) detailJob?.cancel()
                saved.savePage(response.page)
                saved.saveExpandedId(expanded)
                _state.update {
                    it.copy(items = response.items, page = response.page,
                        totalPages = response.totalPages, total = response.total,
                        isLoading = false, expandedId = expanded,
                        detail = it.detail.takeIf { expanded != null },
                        detailError = it.detailError.takeIf { expanded != null },
                        detailLoading = expanded != null && it.detailLoading)
                }
                if (expanded != null && _state.value.detail == null && !_state.value.detailLoading &&
                    _state.value.detailError == null
                ) loadDetail(expanded)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                ensureActive()
                _state.update { it.copy(isLoading = false, error = error) }
            }
        }
    }

    fun retry() = load(requestedPage)

    fun setUserFilter(value: String) {
        if (scope != HistoryScope.ALL) return
        _state.update { it.copy(userFilter = value, invalidUserFilter = false) }
        saved.saveDraftFilter(value)
    }

    fun applyUserFilter() {
        if (scope != HistoryScope.ALL || _state.value.isLoading) return
        val raw = _state.value.userFilter.trim()
        val id = raw.toLongOrNull()?.takeIf { it > 0 }
        if (raw.isNotEmpty() && id == null) {
            _state.update { it.copy(invalidUserFilter = true) }
            return
        }
        val filter = id?.toString().orEmpty()
        closeDetails()
        // Rows for another filter must not appear under the new filter label.
        _state.update { it.copy(appliedUserFilter = filter, invalidUserFilter = false,
            items = emptyList(), page = 1, total = 0, totalPages = 1) }
        saved.saveFilter(filter)
        load(1)
    }

    fun clearUserFilter() {
        if (scope != HistoryScope.ALL || _state.value.isLoading) return
        setUserFilter("")
        applyUserFilter()
    }

    fun previousPage() = _state.value.let { if (it.page > 1) load(it.page - 1) else Unit }
    fun nextPage() = _state.value.let { if (it.page < it.totalPages) load(it.page + 1) else Unit }

    fun toggleDetails(id: Long) {
        val snapshot = _state.value
        if (snapshot.isLoading || snapshot.items.none { it.id == id }) return
        if (snapshot.expandedId == id) {
            closeDetails()
        } else {
            closeDetails()
            _state.update { it.copy(expandedId = id) }
            saved.saveExpandedId(id)
            loadDetail(id)
        }
    }

    fun retryDetails() {
        val snapshot = _state.value
        val id = snapshot.expandedId ?: return
        if (snapshot.isLoading || snapshot.detailLoading || snapshot.items.none { it.id == id }) return
        loadDetail(id)
    }

    private fun closeDetails() {
        detailJob?.cancel()
        _state.update { it.copy(expandedId = null, detail = null,
            detailLoading = false, detailError = null) }
        saved.saveExpandedId(null)
    }

    private fun loadDetail(id: Long) {
        detailJob?.cancel()
        _state.update { it.copy(detailLoading = true, detailError = null) }
        detailJob = viewModelScope.launch {
            runCatching { service.detail(id).also { check(it.id == id) } }
                .onSuccess { detail ->
                    ensureActive()
                    if (_state.value.expandedId != id) return@onSuccess
                    _state.update { it.copy(detail = detail, detailLoading = false) }
                }.onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    ensureActive()
                    if (_state.value.expandedId != id) return@onFailure
                    _state.update { it.copy(detailLoading = false, detailError = failure) }
                }
        }
    }

    class Factory(
        private val service: HistoryService,
        private val scope: HistoryScope,
        private val signedInUserId: Long,
        private val pageSize: Int,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            HistoryViewModel(service, scope, signedInUserId, pageSize.coerceAtLeast(1),
                extras.createSavedStateHandle()) as T
    }
}

/**
 * History screen.
 *
 * Per-screen Back button removed in Phase 0.
 */
