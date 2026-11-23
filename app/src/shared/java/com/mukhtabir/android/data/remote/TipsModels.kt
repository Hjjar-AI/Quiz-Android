package com.mukhtabir.android.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class TipsResponseDto(
    val tips: List<String> = emptyList(),
    val locale: String = "ar",
)
