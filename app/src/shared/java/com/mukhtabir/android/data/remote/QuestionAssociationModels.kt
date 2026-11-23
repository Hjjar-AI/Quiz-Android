package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClinicalCaseListDto(val items: List<ClinicalCaseContextDto> = emptyList(), val total: Int = 0, val page: Int = 1, @SerialName("total_pages") val totalPages: Int = 1)

@Serializable
data class KnowledgeAssociationDto(
    val id: Long,
    val title: String,
    @SerialName("learning_objective") val learningObjective: String = "",
)

@Serializable
data class KnowledgeAssociationListDto(val items: List<KnowledgeAssociationDto> = emptyList(), val count: Int = 0)

@Serializable
data class QuestionImageResultDto(@SerialName("image_url") val imageUrl: String? = null)


@Serializable
data class KnowledgeDetailDto(
    val category: Long? = null,
    val status: String = "active", val version: Int = 1,
    @SerialName("created_by") val createdBy: Long? = null,
    @SerialName("created_by_username") val createdByUsername: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("last_revised_at") val lastRevisedAt: String? = null,
    @SerialName("question_count") val questionCount: Int = 0,
    val id: Long,
    val title: String,
    @SerialName("learning_objective") val learningObjective: String = "",
    @SerialName("canonical_answer") val canonicalAnswer: String = "",
    @SerialName("key_facts") val keyFacts: List<String> = emptyList(),
    val misconceptions: List<String> = emptyList(),
    @SerialName("category_name") val categoryName: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("source_document") val sourceDocument: String? = null,
    @SerialName("source_page") val sourcePage: Int? = null,
    val translations: kotlinx.serialization.json.JsonObject = kotlinx.serialization.json.JsonObject(emptyMap()),
)

@Serializable
data class ClinicalCaseDetailDto(
    val version: Int = 1,
    val key: String,
    val title: String? = null,
    val stem: String? = null,
    val translations: kotlinx.serialization.json.JsonObject = kotlinx.serialization.json.JsonObject(emptyMap()),
    val questions: List<QuestionDto> = emptyList(),
)


@Serializable
data class CaseStemResultDto(
    val version: Int = 1,
    val updated: Int,
    @SerialName("case_stem") val caseStem: String?,
)
