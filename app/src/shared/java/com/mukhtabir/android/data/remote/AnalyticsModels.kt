package com.mukhtabir.android.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

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
data class DifficultyStatsDto(
    val difficulty: String,
    val count: Int = 0,
    @SerialName("avg_correct_rate") val averageCorrectRate: Double = 0.0,
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
data class TagCoverageDto(
    @SerialName("tag_name") val tagName: String,
    val count: Int = 0,
    @SerialName("verified_count") val verifiedCount: Int = 0,
)
