@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.data.remote.LearnerAnalytics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.Job
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.mukhtabir.android.core.LearnerSavedState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AnalyticsViewModel(
    private val service: LearnerAnalytics,
    private val canViewAll: Boolean,
    private val canViewActiveUsers: Boolean,
    savedStateHandle: SavedStateHandle,
    userId: Long,
) : ViewModel() {
    private val saved = LearnerSavedState(savedStateHandle, userId, "analytics-period")
    private val _state = MutableStateFlow(AnalyticsUiState(days = saved.filter()?.toIntOrNull()?.takeIf { it in setOf(7, 30, 90) } ?: 30))
    val state = _state.asStateFlow()
    private val jobs = mutableMapOf<String, Job>()
    private val adminKeys = listOf("difficulty_calibration", "author_flag_rate", "cohort_comparison", "retention", "exam_duration")

    init { load() }

    fun selectDays(days: Int) {
        if (days !in setOf(7, 30, 90) || days == _state.value.days) return
        saved.saveFilter(days.toString())
        (listOf("summary") + if (canViewActiveUsers) listOf("active_users") else emptyList()).forEach { key ->
            jobs.remove(key)?.cancel()
            _state.update { it.copy(days = days, loading = it.loading - key, errors = it.errors - key) }
            retry(key)
        }
    }

    fun load() {
        (listOf("summary", "mastery", "streak") + (if (canViewAll) adminKeys else emptyList()) +
            (if (canViewActiveUsers) listOf("active_users") else emptyList())).forEach(::retry)
    }

    fun retry(key: String) {
        if (key !in listOf("summary", "mastery", "streak") && !(canViewAll && key in adminKeys) &&
            !(canViewActiveUsers && key == "active_users")) return
        if (key in _state.value.loading) return
        val days = _state.value.days
        _state.update { it.copy(loading = it.loading + key, errors = it.errors - key) }
        jobs[key] = viewModelScope.launch {
            try {
                when (key) {
                    "summary" -> {
                        val result = service.summary(days); ensureActive()
                        _state.update { it.copy(summary = result, summaryDays = days) }
                    }
                    "active_users" -> {
                        val result = service.activeUsers(days); ensureActive()
                        _state.update { it.copy(activeUsers = result, activeUsersDays = days) }
                    }
                    "exam_duration" -> {
                        val result = service.examDuration(); ensureActive()
                        _state.update { it.copy(duration = result, adminReports = it.adminReports + (key to result.buckets)) }
                    }
                    "mastery" -> {
                        val result = service.categoryMastery(); ensureActive()
                        _state.update { it.copy(mastery = result) }
                    }
                    "streak" -> {
                        val result = service.streakHistory(); ensureActive()
                        _state.update { it.copy(streak = result) }
                    }
                    else -> {
                        val rows = when (key) {
                            "difficulty_calibration" -> service.difficultyCalibration().rows
                            "author_flag_rate" -> service.authorFlagRate().rows
                            "cohort_comparison" -> service.cohortComparison().rows
                            "retention" -> service.retention().rows
                            else -> error("Unsupported analytics report")
                        }
                        ensureActive()
                        _state.update { it.copy(adminReports = it.adminReports + (key to rows)) }
                    }
                }
                _state.update { it.copy(loading = it.loading - key) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                ensureActive()
                val denied = failure is com.mukhtabir.android.data.remote.ApiException && failure.statusCode in setOf(401, 403, 404)
                _state.update { it.copy(loading = it.loading - key, errors = it.errors + (key to failure),
                    summary = if (denied && key == "summary") null else it.summary,
                    mastery = if (denied && key == "mastery") null else it.mastery,
                    streak = if (denied && key == "streak") null else it.streak,
                    activeUsers = if (denied && key == "active_users") null else it.activeUsers,
                    duration = if (denied && key == "exam_duration") null else it.duration,
                    adminReports = if (denied) it.adminReports - key else it.adminReports) }
            }
        }
    }

    class Factory(private val service: LearnerAnalytics, private val canViewAll: Boolean, private val canViewActiveUsers: Boolean, private val userId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            AnalyticsViewModel(service, canViewAll, canViewActiveUsers, extras.createSavedStateHandle(), userId) as T
    }
}
