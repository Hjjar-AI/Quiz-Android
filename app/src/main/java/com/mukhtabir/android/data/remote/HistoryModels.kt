package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ArchivedMasterMetadataDto(
    @SerialName("exam_id") val examId: Long? = null,
    @SerialName("exam_name") val examName: String? = null,
    @SerialName("weighted_score") val weightedScore: Double? = null,
    @SerialName("forced_finish") val forcedFinish: Boolean = false,
)

@Serializable
data class HistoryItemDto(
    val id: Long,
    val user: Long? = null,
    val username: String? = null,
    @SerialName("full_name") val fullName: String? = null,
    val mode: String,
    val tag: String? = null,
    @SerialName("total_questions") val totalQuestions: Int,
    @SerialName("answered_count") val answeredCount: Int,
    @SerialName("correct_count") val correctCount: Int,
    val accuracy: Double,
    @SerialName("time_spent") val timeSpent: Int,
    @SerialName("completed_at") val completedAt: String,
    @SerialName("master_metadata") val masterMetadata: ArchivedMasterMetadataDto = ArchivedMasterMetadataDto(),
)

@Serializable
data class HistoryPageDto(
    val items: List<HistoryItemDto> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    @SerialName("per_page") val perPage: Int = 20,
    @SerialName("total_pages") val totalPages: Int = 1,
)
