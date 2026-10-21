package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

// Shared by live questions and frozen session/history case blocks. Older
// snapshots can omit title, and the backend normalizes missing fields to null.
@Serializable
data class ClinicalCaseContextDto(
    val id: Long? = null,
    val key: String? = null,
    val title: String? = null,
    val stem: String? = null,
    val translations: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class QuestionAuthorRankDto(val key: String? = null, val label: String? = null)

@Serializable
data class QuestionDto(
    val id: Long,
    val question: String,
    val choices: List<String> = emptyList(),
    @SerialName("correct_answer") val correctAnswer: Int? = null,
    val explanation: String? = null,
    val translations: JsonObject = JsonObject(emptyMap()),
    @SerialName("answers_hidden") val answersHidden: Boolean = false,
    val source: String? = null,
    @SerialName("source_document") val sourceDocument: String? = null,
    @SerialName("source_page") val sourcePage: Int? = null,
    @SerialName("knowledge_object") val knowledgeObject: Long? = null,
    @SerialName("case_order") val caseOrder: Int? = null,
    @SerialName("knowledge_object_title") val knowledgeObjectTitle: String? = null,
    @SerialName("knowledge_object_objective") val knowledgeObjectObjective: String? = null,
    val tags: List<String> = emptyList(),
    val difficulty: String = "",
    val category: Long? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("category_color") val categoryColor: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("verified_by") val verifiedBy: String? = null,
    @SerialName("verified_at") val verifiedAt: String? = null,
    @SerialName("verification_notes") val verificationNotes: String? = null,
    val verified: Boolean = false,
    val version: Int = 1,
    @SerialName("authored_by") val authoredBy: Long? = null,
    @SerialName("authored_by_username") val authoredByUsername: String? = null,
    @SerialName("authored_by_rank") val authoredByRank: QuestionAuthorRankDto? = null,
    @SerialName("owned_by_username") val ownedByUsername: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_by") val updatedBy: Long? = null,
    @SerialName("times_answered") val timesAnswered: Long = 0,
    @SerialName("times_correct") val timesCorrect: Long = 0,
    @SerialName("owned_by") val ownedBy: Long? = null,
    @SerialName("is_draft") val isDraft: Boolean = false,
    @SerialName("draft_owner") val draftOwner: Long? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("last_revised_at") val lastRevisedAt: String? = null,
    val case: ClinicalCaseContextDto? = null,
)

@Serializable
data class QuestionPageDto(
    val items: List<QuestionDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
    @SerialName("total_pages") val totalPages: Int = 1,
)

@Serializable
data class CountDto(val count: Int = 0)

@Serializable
data class TagDto(
    val name: String,
    val count: Int = 0,
    @SerialName("verified_count") val verifiedCount: Int = 0,
)

@Serializable
data class TagListDto(
    val items: List<TagDto> = emptyList(),
    val total: Int = 0,
)

@Serializable
data class SourceBookDto(val name: String, val count: Int = 0)

@Serializable
data class SourceBookFiltersDto(
    @SerialName("source_document") val sourceDocument: String,
    val categories: List<CategoryDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
)

@Serializable
data class SourceBookListDto(val items: List<SourceBookDto> = emptyList())
