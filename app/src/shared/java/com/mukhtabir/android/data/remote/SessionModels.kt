package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class StudyNowDto(
    @SerialName("question_ids") val questionIds: List<Long> = emptyList(),
    val total: Int = 0,
    val breakdown: Map<String, Int> = emptyMap(),
)

@Serializable
data class AttemptSummaryDto(
    @SerialName("total_seen") val totalSeen: Int = 0,
    @SerialName("ever_correct") val everCorrect: Int = 0,
    @SerialName("wrong_open") val wrongOpen: Int = 0,
    @SerialName("fragile_correct") val fragileCorrect: Int = 0,
    @SerialName("due_now") val dueNow: Int = 0,
)

@Serializable
data class StartSessionRequest(
    val limit: Int,
    @SerialName("selection_strategy") val selectionStrategy: String = "coverage",
    @SerialName("category_ids") val categoryIds: List<Long> = emptyList(),
    val difficulty: String? = null,
    @SerialName("verified_only") val verifiedOnly: Boolean = false,
    @SerialName("use_bookmarks") val useBookmarks: Boolean = false,
    @SerialName("use_srs") val useSrs: Boolean = false,
    @SerialName("source_document") val sourceDocument: String? = null,
    @SerialName("tags_filter") val tagsFilter: List<String> = emptyList(),
    @SerialName("question_ids") val questionIds: List<Long> = emptyList(),
    @SerialName("session_label") val sessionLabel: String? = null,
    @SerialName("blueprint_id") val blueprintId: Long? = null,
)

@Serializable
data class ExamSessionDto(
    @SerialName("session_id") val sessionId: String,
    val mode: String,
    @SerialName("question_ids") val questionIds: List<Long> = emptyList(),
    @SerialName("current_index") val currentIndex: Int = 0,
    val tag: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("accumulated_time") val accumulatedTime: Int = 0,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("is_active") val isActive: Boolean = true,
)

@Serializable
data class SessionQuestionDto(
    val id: Long,
    val text: String,
    val choices: List<String> = emptyList(),
    @SerialName("correct_answer") val correctAnswer: Int? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    val explanation: String? = null,
    val translations: JsonObject = JsonObject(emptyMap()),
    val case: ClinicalCaseContextDto? = null,
    @SerialName("choices_hidden") val choicesHidden: Boolean = false,
)

@Serializable
data class SessionQuestionPageDto(
    val index: Int,
    val total: Int,
    val question: SessionQuestionDto,
    @SerialName("saved_answer") val savedAnswer: Int? = null,
    @SerialName("saved_is_correct") val savedIsCorrect: Boolean? = null,
    @SerialName("saved_error_reason") val savedErrorReason: String? = null,
    @SerialName("saved_confidence") val savedConfidence: Int? = null,
    @SerialName("saved_pre_answer") val savedPreAnswer: String? = null,
    val tag: String? = null,
    @SerialName("blueprint_id") val blueprintId: Long? = null,
)

@Serializable
data class SessionQuestionBundleDto(
    @SerialName("session_id") val sessionId: String,
    val questions: List<SessionQuestionPageDto>,
)

@Serializable
data class SessionAnswerBaseline(
    val answer: Int? = null,
    val confidence: Int? = null,
    @SerialName("pre_answer") val preAnswer: String? = null,
    @SerialName("error_reason") val errorReason: String? = null,
)

@Serializable
data class SubmitAnswerRequest(
    @SerialName("session_id") val sessionId: String,
    val answer: Int? = null,
    val action: String = "next",
    @SerialName("expected_index") val expectedIndex: Int? = null,
    @SerialName("expected_slot") val expectedSlot: SessionAnswerBaseline? = null,
    @SerialName("target_index") val targetIndex: Int? = null,
    val confidence: Int? = null,
    @SerialName("error_reason") val errorReason: String? = null,
    @SerialName("pre_answer") val preAnswer: String? = null,
)

@Serializable
data class AnswerSubmissionDto(
    @SerialName("new_index") val newIndex: Int,
    val explanation: String? = null,
    @SerialName("is_correct") val isCorrect: Boolean? = null,
    @SerialName("feedback_translations")
    val feedbackTranslations: JsonObject = JsonObject(emptyMap()),
)

@Serializable
data class SessionProgressDto(
    @SerialName("session_id") val sessionId: String? = null,
    @SerialName("question_ids") val questionIds: List<Long> = emptyList(),
    @SerialName("current_index") val currentIndex: Int = 0,
    val answers: JsonObject = JsonObject(emptyMap()),
    val tag: String? = null,
    @SerialName("started_at") val startedAt: String? = null,
    @SerialName("accumulated_time") val accumulatedTime: Int = 0,
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
)

@Serializable
data class SessionResultsDto(
    val results: List<SessionResultItemDto> = emptyList(),
    @SerialName("correct_count") val correctCount: Int,
    @SerialName("total_questions") val totalQuestions: Int,
    val accuracy: Double,
    @SerialName("total_time") val totalTime: Int,
    @SerialName("answered_count") val answeredCount: Int,
    @SerialName("confidence_correct") val confidenceCorrect: Int = 0,
    @SerialName("confidence_fragile") val confidenceFragile: Int = 0,
    @SerialName("source_session_id") val sourceSessionId: String? = null,
    @SerialName("history_id") val historyId: Long? = null,
    val mode: String? = null,
    @SerialName("review_withheld") val reviewWithheld: Boolean = false,
)

@Serializable
data class SessionResultItemDto(
    @SerialName("question_id") val questionId: Long,
    val question: String,
    val choices: List<String> = emptyList(),
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("correct_answer") val correctAnswer: Int? = null,
    @SerialName("user_answer") val userAnswer: Int? = null,
    @SerialName("final_answer") val finalAnswer: Int? = null,
    @SerialName("final_confidence_score") val finalConfidenceScore: Int? = null,
    @SerialName("final_error_reason") val finalErrorReason: String? = null,
    @SerialName("used_first_attempt") val usedFirstAttempt: Boolean = false,
    @SerialName("is_correct") val isCorrect: Boolean = false,
    val confidence: Boolean = false,
    @SerialName("confidence_score") val confidenceScore: Int? = null,
    @SerialName("pre_answer") val preAnswer: String? = null,
    @SerialName("error_reason") val errorReason: String? = null,
    val explanation: String? = null,
    @SerialName("category_id") val categoryId: Long? = null,
    @SerialName("category_name") val categoryName: String? = null,
    @SerialName("category_color") val categoryColor: String? = null,
    @SerialName("tag_names") val tagNames: List<String> = emptyList(),
    val difficulty: String? = null,
    val case: ClinicalCaseContextDto? = null,
)
