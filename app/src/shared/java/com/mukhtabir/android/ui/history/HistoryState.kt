package com.mukhtabir.android.ui.history

import com.mukhtabir.android.data.remote.HistoryItemDto
import com.mukhtabir.android.data.remote.HistoryDetailDto

data class HistoryUiState(
    val items: List<HistoryItemDto> = emptyList(),
    val page: Int = 1,
    val totalPages: Int = 1,
    val total: Int = 0,
    val expandedId: Long? = null,
    val detail: HistoryDetailDto? = null,
    val detailLoading: Boolean = false,
    val isLoading: Boolean = false,
    val userFilter: String = "",
    val appliedUserFilter: String = "",
    val invalidUserFilter: Boolean = false,
    val error: Throwable? = null,
    val detailError: Throwable? = null,
)

enum class HistoryScope { OWN, ALL }
