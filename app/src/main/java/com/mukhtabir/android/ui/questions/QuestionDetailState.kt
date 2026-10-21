@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.data.remote.QuestionDto

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
