package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CategoryDto(
    val id: Long,
    val version: Int = 1,
    val name: String,
    val description: String? = null,
    val color: String? = null,
    val icon: String? = null,
)

@Serializable
data class CategoryListDto(
    val items: List<CategoryDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class BlueprintDto(
    val id: Long,
    val name: String,
    val description: String? = null,
    @SerialName("is_active") val isActive: Boolean = false,
)

@Serializable
data class BlueprintListDto(
    val items: List<BlueprintDto> = emptyList(),
    val total: Int = 0,
)
