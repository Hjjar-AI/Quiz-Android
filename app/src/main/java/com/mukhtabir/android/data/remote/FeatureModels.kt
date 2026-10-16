package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class PublicConfigDto(
    @SerialName("max_quiz_questions") val maxQuizQuestions: Int = 200,
    @SerialName("max_choices") val maxChoices: Int = 8,
    @SerialName("items_per_page") val itemsPerPage: Int = 20,
    val roles: Map<String, String> = mapOf(
        "admin" to "admin",
        "moderator" to "moderator",
        "member" to "member",
    ),
)

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
data class BookmarkListDto(
    val items: List<QuestionDto> = emptyList(),
    val count: Int = 0,
)

@Serializable
data class CountDto(val count: Int = 0)

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

@Serializable
data class BookmarkToggleDto(val added: Boolean)

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
data class SourceBookDto(val name: String, val count: Int = 0)

@Serializable
data class SourceBookFiltersDto(
    @SerialName("source_document") val sourceDocument: String,
    val categories: List<CategoryDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
)

@Serializable
data class SourceBookListDto(val items: List<SourceBookDto> = emptyList())

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

@Serializable
data class PerformancePointDto(
    val date: String,
    val accuracy: Double,
    @SerialName("questions_answered") val questionsAnswered: Int,
)

@Serializable
data class AnalyticsSummaryDto(
    @SerialName("user_performance") val userPerformance: List<PerformancePointDto> = emptyList(),
    @SerialName("weak_categories") val weakCategories: List<JsonObject> = emptyList(),
    @SerialName("confidence_stats") val confidenceStats: ConfidenceStatsDto = ConfidenceStatsDto(),
    @SerialName("category_coverage") val categoryCoverage: List<CategoryCoverageDto> = emptyList(),
    @SerialName("tag_coverage") val tagCoverage: List<TagCoverageDto> = emptyList(),
    @SerialName("difficulty_stats") val difficultyStats: List<DifficultyStatsDto> = emptyList(),
    @SerialName("active_users") val activeUsers: List<ActiveUsersPointDto> = emptyList(),
)

@Serializable
data class ConfidenceStatsDto(
    @SerialName("total_attempted") val totalAttempted: Int = 0,
    @SerialName("correct_confident") val correctConfident: Int = 0,
    @SerialName("correct_fragile") val correctFragile: Int = 0,
    @SerialName("wrong_open") val wrongOpen: Int = 0,
    @SerialName("fragile_ratio") val fragileRatio: Double = 0.0,
)

@Serializable
data class CategoryCoverageDto(
    @SerialName("category_id") val categoryId: Long,
    @SerialName("category_name") val categoryName: String,
    val total: Int = 0,
    val verified: Int = 0,
    @SerialName("avg_accuracy") val averageAccuracy: Double = 0.0,
    val color: String? = null,
)

@Serializable
data class TagCoverageDto(
    @SerialName("tag_name") val tagName: String,
    val count: Int = 0,
    @SerialName("verified_count") val verifiedCount: Int = 0,
)

@Serializable
data class DifficultyStatsDto(
    val difficulty: String,
    val count: Int = 0,
    @SerialName("avg_correct_rate") val averageCorrectRate: Double = 0.0,
)

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

// ═════════════════════════════════════════════════════════════════════
// Phase 1 additions
//
// These back the Analytics rewrite (items 3 and 24), the Tips card
// (item 26), and any future admin charts. The shape of each DTO
// matches the corresponding service method in
// `apps/analytics/services/member*.py` on the backend — that is the
// source of truth for field names and defaults.
// ═════════════════════════════════════════════════════════════════════

/**
 * One row of the caller's category-mastery ranking.
 *
 * `mastery_score` and `accuracy` are the same value today — the
 * backend keeps both keys on the response so older clients that
 * read `accuracy` continue to work while new clients read
 * `mastery_score`. The UI uses `masteryScore` and falls back to
 * `accuracy` only if the primary is missing (defensive).
 */
@Serializable
data class CategoryMasteryItemDto(
    @SerialName("category_id") val categoryId: Long,
    @SerialName("category_name") val categoryName: String,
    @SerialName("category_color") val categoryColor: String? = null,
    @SerialName("distinct_questions") val distinctQuestions: Int = 0,
    @SerialName("total_questions") val totalQuestions: Int = 0,
    val coverage: Double = 0.0,
    val attempts: Int = 0,
    @SerialName("wrong_count") val wrongCount: Int = 0,
    val accuracy: Double = 0.0,
    @SerialName("mastery_score") val masteryScore: Double = 0.0,
    val mastered: Boolean = false,
)

@Serializable
data class CategoryMasteryDto(
    val categories: List<CategoryMasteryItemDto> = emptyList(),
    @SerialName("total_categories") val totalCategories: Int = 0,
)

@Serializable
data class StreakDayDto(
    val date: String,
    val sessions: Int = 0,
    val questions: Int = 0,
)

@Serializable
data class StreakHistoryDto(
    val days: List<StreakDayDto> = emptyList(),
    @SerialName("current_streak") val currentStreak: Int = 0,
    @SerialName("longest_streak") val longestStreak: Int = 0,
    @SerialName("active_days") val activeDays: Int = 0,
    @SerialName("total_sessions") val totalSessions: Int = 0,
    @SerialName("total_questions") val totalQuestions: Int = 0,
    @SerialName("unique_questions") val uniqueQuestions: Int = 0,
    @SerialName("unique_concepts") val uniqueConcepts: Int = 0,
    @SerialName("due_reviews") val dueReviews: Int = 0,
    @SerialName("mastery_gain") val masteryGain: Double = 0.0,
)

@Serializable
data class ActiveUsersPointDto(
    val date: String,
    @SerialName("active_users") val activeUsers: Int = 0,
    @SerialName("new_users") val newUsers: Int = 0,
    @SerialName("total_sessions") val totalSessions: Int = 0,
    @SerialName("avg_session_duration") val avgSessionDuration: Double = 0.0,
)

@Serializable
data class TipsResponseDto(
    val tips: List<String> = emptyList(),
    val locale: String = "ar",
)
