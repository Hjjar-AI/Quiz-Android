package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class MasterExamDto(
    val id: Long,
    val name: String,
    @SerialName("is_owner") val isOwner: Boolean = false,
    val description: String = "",
    val instructions: String = "",
    val status: String = "",
    @SerialName("stored_status") val storedStatus: String? = null,
    @SerialName("opens_at") val opensAt: String,
    @SerialName("closes_at") val closesAt: String,
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("question_count") val questionCount: Int = 0,
    @SerialName("allow_makeup") val allowMakeup: Boolean = false,
    @SerialName("has_acknowledged") val hasAcknowledged: Boolean = false,
    @SerialName("my_attempt") val myAttempt: MasterExamAttemptDto? = null,
)

@Serializable
data class MasterExamListDto(val items: List<MasterExamDto> = emptyList(), val total: Int = 0)

@Serializable
data class MasterExamNoticesDto(val items: List<MasterExamDto> = emptyList(), val count: Int = 0)

@Serializable
data class MasterExamAttemptDto(
    val id: Long,
    @SerialName("exam_name") val examName: String = "",
    @SerialName("session_id") val sessionId: String,
    @SerialName("is_complete") val isComplete: Boolean = false,
    @SerialName("is_makeup") val isMakeup: Boolean = false,
    @SerialName("forced_finish") val forcedFinish: Boolean = false,
    @SerialName("correct_count") val correctCount: Int = 0,
    @SerialName("total_questions") val totalQuestions: Int = 0,
    @SerialName("answered_count") val answeredCount: Int = 0,
    val accuracy: Double = 0.0,
    @SerialName("weighted_score") val weightedScore: Double = 0.0,
    @SerialName("started_at") val startedAt: String,
    @SerialName("finished_at") val finishedAt: String? = null,
)

@Serializable
data class MasterExamStatusDto(
    @SerialName("attempt_id") val attemptId: Long,
    @SerialName("session_id") val sessionId: String,
    @SerialName("is_complete") val isComplete: Boolean,
    @SerialName("is_makeup") val isMakeup: Boolean = false,
    @SerialName("question_ids") val questionIds: List<Long> = emptyList(),
    val answers: Map<String, JsonObject> = emptyMap(),
    @SerialName("current_question_id") val currentQuestionId: Long? = null,
    @SerialName("deadline_at") val deadlineAt: String,
    @SerialName("server_now") val serverNow: String,
    @SerialName("grace_seconds") val graceSeconds: Int = 180,
)

@Serializable
data class MasterExamAnswerRequest(
    @SerialName("question_id") val questionId: Long,
    val answer: Int,
    val confidence: Int = 3,
    @SerialName("expected_slot") val expectedSlot: JsonObject? = null,
    @SerialName("session_id") val sessionId: String? = null,
)
