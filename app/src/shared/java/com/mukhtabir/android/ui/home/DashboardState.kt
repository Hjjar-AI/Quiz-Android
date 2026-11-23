package com.mukhtabir.android.ui.home

import com.mukhtabir.android.data.remote.AttemptSummaryDto
import com.mukhtabir.android.data.remote.QuestionPageDto
import com.mukhtabir.android.data.remote.PlannerDto
import com.mukhtabir.android.data.remote.MasterExamDto
import com.mukhtabir.android.data.remote.StreakHistoryDto

enum class DashboardSection(val requiresData: Boolean = true) {
    TODAY, MODES(false), OVERVIEW, RECENT, ACTIVITY, EXAMS, TIPS,
}

data class DashboardUiState(
    val bank: QuestionPageDto? = null,
    val bankTotal: Int? = null,
    val verifiedTotal: Int? = null,
    val planner: PlannerDto? = null,
    val activity: StreakHistoryDto? = null,
    val notices: List<MasterExamDto>? = null,
    val currentStreak: Int? = null,
    val bookmarkCount: Int = 0,
    val srsDueCount: Int = 0,
    val attempts: AttemptSummaryDto = AttemptSummaryDto(),
    val todayAttempts: AttemptSummaryDto = AttemptSummaryDto(),
    val tips: List<String> = emptyList(),
    val currentTipIndex: Int = 0,
    val expanded: Set<DashboardSection> = setOf(DashboardSection.TIPS),
    val loading: Set<DashboardSection> = emptySet(),
    val loaded: Set<DashboardSection> = emptySet(),
    val errors: Map<DashboardSection, Throwable> = emptyMap(),
    val stale: Set<DashboardSection> = emptySet(),
    val studyLoading: Boolean = false,
    val studyError: Throwable? = null,
    val studyEmpty: Boolean = false,
) {
    val isLoading: Boolean get() = loading.any { it in expanded }
    val currentTip: String?
        get() = tips.getOrNull(currentTipIndex).takeUnless { it.isNullOrBlank() }
}
