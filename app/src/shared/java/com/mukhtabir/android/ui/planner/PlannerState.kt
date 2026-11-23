package com.mukhtabir.android.ui.planner

import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.PlannerDto

@kotlinx.serialization.Serializable
internal data class StoredPlannerDraft(val target: String, val categories: Set<Long>, val tags: List<String>, val tagInput: String,
    val start: String, val end: String, val pending: Boolean, val version: Int? = null, val plannerId: Long? = null)

enum class PlannerValidationError { TARGET, CATEGORIES, DATE, DATE_ORDER }
enum class PlannerOutcome { SAVED, DELETED }

data class PlannerUiState(
    val planner: PlannerDto? = null,
    val baselineVersion: Int? = null, val baselineId: Long? = null,
    val target: String = "10",
    val selectedCategories: Set<Long> = emptySet(),
    val categoryOptions: List<CategoryDto> = emptyList(),
    val tagOptions: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val tagInput: String = "",
    val tagValidation: Int? = null,
    val startDate: String = "",
    val endDate: String = "",
    val loaded: Boolean = false,
    val serverToday: String = "",
    val validationError: PlannerValidationError? = null,
    val validationAttempt: Int = 0,
    val loading: Boolean = false,
    val optionsLoading: Boolean = false,
    val recoveryRequired: Boolean = false,
    val error: Throwable? = null,
    val actionError: Throwable? = null,
    val optionsError: Throwable? = null,
    val outcome: PlannerOutcome? = null,
) { val conflict: Boolean get() = loaded && planner!=null && (baselineVersion!=planner.version || baselineId!=planner.id) }
