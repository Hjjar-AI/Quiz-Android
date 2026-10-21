package com.mukhtabir.android.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class BookmarkListDto(
    val items: List<QuestionDto> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class BookmarkToggleDto(val added: Boolean)
