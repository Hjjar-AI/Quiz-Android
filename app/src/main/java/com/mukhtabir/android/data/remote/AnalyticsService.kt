package com.mukhtabir.android.data.remote

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.put

/**
 * Analytics endpoints.
 *
 * Param corrections from the previous revision:
 *   • `categoryMastery` used to send `page` / `per_page`. The backend
 *     reads `min_attempts` (1-50) and `top` (1-50); the old params
 *     were silently ignored.
 *   • `streakHistory` and `categoryMastery` returned `JsonObject`.
 *     Both now return typed DTOs.
 */
class AnalyticsService(private val api: ApiClient) {
    suspend fun verificationStats(): VerificationStatsDto = api.decode(
        VerificationStatsDto.serializer(), api.get(ApiEndpoints.Analytics.VERIFICATION_STATS),
    )

    suspend fun summary(days: Int = 30): AnalyticsSummaryDto = api.decode(
        AnalyticsSummaryDto.serializer(),
        api.get(
            ApiEndpoints.Analytics.SUMMARY,
            mapOf("days" to days.coerceIn(1, 90).toString()),
        ),
    )

    suspend fun activeUsers(days: Int = 30): List<ActiveUsersPointDto> = api.decode(
        ListSerializer(ActiveUsersPointDto.serializer()),
        api.get(
            ApiEndpoints.Analytics.ACTIVE_USERS,
            mapOf("days" to days.coerceIn(1, 90).toString()),
        ),
    )

    suspend fun categoryMastery(
        minAttempts: Int? = null,
        top: Int? = null,
    ): CategoryMasteryDto = api.decode(
        CategoryMasteryDto.serializer(),
        api.get(
            ApiEndpoints.Analytics.CATEGORY_MASTERY,
            buildMap {
                minAttempts?.let { put("min_attempts", it.coerceIn(1, 50).toString()) }
                top?.let { put("top", it.coerceIn(1, 50).toString()) }
            },
        ),
    )

    suspend fun streakHistory(days: Int = 30): StreakHistoryDto = api.decode(
        StreakHistoryDto.serializer(),
        api.get(
            ApiEndpoints.Analytics.STREAK_HISTORY,
            mapOf("days" to days.coerceIn(7, 365).toString()),
        ),
    )

    suspend fun difficultyCalibration(): ReportRowsDto = report(ApiEndpoints.Analytics.DIFFICULTY_CALIBRATION)
    suspend fun authorFlagRate(): ReportRowsDto = report(ApiEndpoints.Analytics.AUTHOR_FLAG_RATE)
    suspend fun cohortComparison(): ReportRowsDto = report(ApiEndpoints.Analytics.COHORT_COMPARISON)
    suspend fun retention(): ReportRowsDto = report(ApiEndpoints.Analytics.RETENTION)
    suspend fun examDuration(): ExamDurationReportDto = api.decode(
        ExamDurationReportDto.serializer(), api.get(ApiEndpoints.Analytics.EXAM_DURATION),
    )

    private suspend fun report(path: String): ReportRowsDto =
        api.decode(ReportRowsDto.serializer(), api.get(path))
}
