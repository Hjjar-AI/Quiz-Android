@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.analytics

import com.mukhtabir.android.data.remote.AnalyticsSummaryDto
import com.mukhtabir.android.data.remote.CategoryMasteryDto
import com.mukhtabir.android.data.remote.ExamDurationReportDto
import com.mukhtabir.android.data.remote.ActiveUsersPointDto
import com.mukhtabir.android.data.remote.StreakHistoryDto
import kotlinx.serialization.json.JsonObject

data class AnalyticsUiState(
    val summary: AnalyticsSummaryDto? = null,
    val mastery: CategoryMasteryDto? = null,
    val streak: StreakHistoryDto? = null,
    val adminReports: Map<String, List<JsonObject>> = emptyMap(),
    val duration: ExamDurationReportDto? = null,
    val activeUsers: List<ActiveUsersPointDto>? = null,
    val activeUsersDays: Int? = null,
    val days: Int = 30,
    val summaryDays: Int? = null,
    val loading: Set<String> = emptySet(),
    val errors: Map<String, Throwable> = emptyMap(),
) {
    val isLoading: Boolean get() = loading.isNotEmpty()
}
