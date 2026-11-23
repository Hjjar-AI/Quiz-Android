package com.mukhtabir.android.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.data.remote.LearnerAnalytics
import com.mukhtabir.android.data.remote.LearnerBookmarks
import com.mukhtabir.android.data.remote.LearnerQuestions
import com.mukhtabir.android.data.remote.LearnerTips
import com.mukhtabir.android.data.remote.LearnerPlanner
import com.mukhtabir.android.data.remote.LearnerNotices
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TIP_ROTATE_MILLIS = 10 * 60 * 1000L

class DashboardViewModel(
    private val bookmarkService: LearnerBookmarks,
    private val questionService: LearnerQuestions,
    private val tipsService: LearnerTips,
    private val analyticsService: LearnerAnalytics,
    private val plannerService: LearnerPlanner,
    private val masterExamService: LearnerNotices,
) : ViewModel() {
    private val _state = MutableStateFlow(DashboardUiState())
    val state = _state.asStateFlow()
    private var tipsRotationJob: kotlinx.coroutines.Job? = null
    private var todayInitialized = false

    fun initializeToday(canStartTests: Boolean) {
        if (!canStartTests) {
            setExpanded(DashboardSection.TODAY, false)
            return
        }
        if (todayInitialized) return
        todayInitialized = true
        setExpanded(DashboardSection.TODAY, true)
    }

    fun setExpanded(section: DashboardSection, expanded: Boolean) {
        _state.update { it.copy(expanded = if (expanded) it.expanded + section else it.expanded - section) }
        if (expanded) load(section) else if (section == DashboardSection.TIPS) tipsRotationJob?.cancel()
        if (expanded && section == DashboardSection.TIPS && section in _state.value.loaded) startTipsRotation()
    }

    /** Refresh only visible sections; closed sections become stale until explicitly opened. */
    fun refresh() {
        val open = _state.value.expanded
        _state.update { it.copy(stale = it.stale + (it.loaded - open)) }
        open.forEach { load(it, force = true) }
    }

    fun load(section: DashboardSection, force: Boolean = false) {
        val current = _state.value
        if (!section.requiresData || section in current.loading ||
            (!force && section in current.loaded && section !in current.stale)) return
        _state.update { it.copy(loading = it.loading + section, errors = it.errors - section) }
        viewModelScope.launch {
            try {
                // Apply a section's snapshot together, preserving readable data on failure.
                val apply: (DashboardUiState) -> DashboardUiState = coroutineScope {
                    when (section) {
                        DashboardSection.TODAY -> {
                            val due = async { questionService.srsDueCount() }
                            val attempts = async { questionService.attemptSummary() }
                            val planner = async { plannerService.get() }
                            val streak = async { analyticsService.streakHistory(90) }
                            val dueValue = due.await()
                            val attemptsValue = attempts.await()
                            val planValue = planner.await()
                            val streakValue = streak.await().currentStreak
                            val update: (DashboardUiState) -> DashboardUiState = {
                                it.copy(srsDueCount = dueValue, todayAttempts = attemptsValue,
                                    planner = planValue, currentStreak = streakValue)
                            }
                            update
                        }
                        DashboardSection.OVERVIEW -> {
                            val bookmarks = async { bookmarkService.count() }
                            val attempts = async { questionService.attemptSummary() }
                            val bank = async { questionService.list(perPage = 1).total }
                            val verified = async { questionService.list(perPage = 1, verifiedOnly = true).total }
                            val bookmarksValue = bookmarks.await()
                            val attemptsValue = attempts.await()
                            val totalValue = bank.await()
                            val verifiedValue = verified.await()
                            val update: (DashboardUiState) -> DashboardUiState = {
                                it.copy(bookmarkCount = bookmarksValue, attempts = attemptsValue,
                                    bankTotal = totalValue, verifiedTotal = verifiedValue)
                            }
                            update
                        }
                        DashboardSection.RECENT -> {
                            val value = questionService.list(perPage = 5)
                            val update: (DashboardUiState) -> DashboardUiState = { it.copy(bank = value) }
                            update
                        }
                        DashboardSection.ACTIVITY -> {
                            val value = analyticsService.streakHistory(90)
                            val update: (DashboardUiState) -> DashboardUiState = { it.copy(activity = value) }
                            update
                        }
                        DashboardSection.EXAMS -> {
                            val value = masterExamService.notices().items
                            val update: (DashboardUiState) -> DashboardUiState = { it.copy(notices = value) }
                            update
                        }
                        DashboardSection.TIPS -> {
                            val value = tipsService.forLocale(Locale.getDefault().language).tips
                            val update: (DashboardUiState) -> DashboardUiState = { it.copy(tips = value, currentTipIndex = 0) }
                            update
                        }
                        DashboardSection.MODES -> { value: DashboardUiState -> value }
                    }
                }
                ensureActive()
                _state.update { apply(it).copy(loading = it.loading - section, loaded = it.loaded + section, stale = it.stale - section) }
                if (section == DashboardSection.TIPS && section in _state.value.expanded) startTipsRotation()
            } catch (cancel: CancellationException) {
                throw cancel
            } catch (failure: Exception) {
                ensureActive()
                _state.update { it.copy(errors = it.errors + (section to failure)) }
            } finally {
                _state.update { it.copy(loading = it.loading - section) }
            }
        }
    }

    private fun startTipsRotation() {
        tipsRotationJob?.cancel()
        tipsRotationJob = viewModelScope.launch {
            while (true) {
                delay(TIP_ROTATE_MILLIS)
                _state.update { current ->
                    if (current.tips.size < 2) {
                        current
                    } else {
                        current.copy(
                            currentTipIndex = (current.currentTipIndex + 1) % current.tips.size,
                        )
                    }
                }
            }
        }
    }

    /** Called in the route's coroutine scope so leaving cancels a pending launch. */
    suspend fun fetchStudyNow(limit: Int, canStart: Boolean): List<Long>? {
        if (!canStart || _state.value.studyLoading) return null
        _state.update { it.copy(studyLoading = true, studyError = null, studyEmpty = false) }
        return try {
            val ids = questionService.studyNow(limit.coerceAtLeast(1)).questionIds
            currentCoroutineContext().ensureActive()
            _state.update { it.copy(studyEmpty = ids.isEmpty()) }
            ids
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Exception) {
            currentCoroutineContext().ensureActive()
            _state.update { it.copy(studyError = failure) }
            null
        } finally {
            _state.update { it.copy(studyLoading = false) }
        }
    }

    override fun onCleared() {
        tipsRotationJob?.cancel()
        super.onCleared()
    }

    class Factory(
        private val bookmarkService: LearnerBookmarks,
        private val questionService: LearnerQuestions,
        private val tipsService: LearnerTips,
        private val analyticsService: LearnerAnalytics,
        private val plannerService: LearnerPlanner,
        private val masterExamService: LearnerNotices,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(bookmarkService, questionService, tipsService, analyticsService, plannerService, masterExamService) as T
    }
}
