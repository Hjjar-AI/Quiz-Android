@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.QuestionDto

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
