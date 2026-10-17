package com.mukhtabir.android.ui.home

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.directionalIcon

import com.mukhtabir.android.ui.components.AppTextButton

import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppIconButton

import com.mukhtabir.android.ui.components.AppActionRow

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.R
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.util.formatNumber
import com.mukhtabir.android.data.remote.AnalyticsService
import com.mukhtabir.android.data.remote.AttemptSummaryDto
import com.mukhtabir.android.data.remote.BookmarkService
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.TipsService
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.data.remote.QuestionPageDto
import com.mukhtabir.android.data.remote.PlannerDto
import com.mukhtabir.android.data.remote.PlannerService
import com.mukhtabir.android.data.remote.MasterExamService
import com.mukhtabir.android.data.remote.MasterExamDto
import com.mukhtabir.android.data.remote.StreakHistoryDto
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.data.remote.localizedForDisplay
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
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

class DashboardViewModel(
    private val bookmarkService: BookmarkService,
    private val questionService: QuestionService,
    private val tipsService: TipsService,
    private val analyticsService: AnalyticsService,
    private val plannerService: PlannerService,
    private val masterExamService: MasterExamService,
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
        private val bookmarkService: BookmarkService,
        private val questionService: QuestionService,
        private val tipsService: TipsService,
        private val analyticsService: AnalyticsService,
        private val plannerService: PlannerService,
        private val masterExamService: MasterExamService,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            DashboardViewModel(bookmarkService, questionService, tipsService, analyticsService, plannerService, masterExamService) as T
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeRoute(
    user: UserDto,
    bookmarkService: BookmarkService,
    questionService: QuestionService,
    tipsService: TipsService,
    analyticsService: AnalyticsService,
    plannerService: PlannerService,
    masterExamService: MasterExamService,
    imageRepository: ImageRepository,
    onOpenLibrary: () -> Unit,
    onOpenQuestion: (Long) -> Unit,
    onOpenPlanner: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenStudyMode: (TestMode) -> Unit,
    onOpenExam: (Long) -> Unit,
    onCreateQuestion: (() -> Unit)?,
    canStartTests: Boolean,
    canViewHistory: Boolean,
    maxQuizQuestions: Int,
    onStartStudyNow: (List<Long>) -> Unit,
    onOpenQuizzes: () -> Unit,
    onOpenBookmarks: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenMistakes: () -> Unit,
    onOpenFragile: () -> Unit,
    draftStore: SessionDraftStore,
    onOpenAssignedExam: () -> Unit,
    onOpenOfflineStudy: () -> Unit,
) {
    val viewModel: DashboardViewModel = viewModel(
        key = "dashboard-${user.id}",
        factory = DashboardViewModel.Factory(bookmarkService, questionService, tipsService, analyticsService, plannerService, masterExamService),
    )
    val state by viewModel.state.collectAsState()
    var masterRecovery by remember(user.id) { mutableStateOf(draftStore.readMaster(user.id)) }
    var finishRecovery by remember(user.id) { mutableStateOf(draftStore.readFinish(user.id)) }
    val scope = rememberCoroutineScope()
    var answerRecovery by remember(user.id) { mutableStateOf(draftStore.read(user.id)) }
    var hasLocalSession by remember(user.id, draftStore) { mutableStateOf(false) }
    var collectionsCount by remember(user.id, draftStore) { mutableStateOf(0) }
    var pendingStudy by remember(user.id, draftStore) { mutableStateOf(false) }
    var pendingPacks by remember(user.id, draftStore) { mutableStateOf(0) }
    var completionReadError by remember(user.id, draftStore) { mutableStateOf<Throwable?>(null) }
    var completionReadJob by remember(user.id, draftStore) { mutableStateOf<Job?>(null) }
    val refreshCompletions: () -> Unit = {
        completionReadJob?.cancel()
        completionReadJob = scope.launch {
            val (session, packs) = withContext(Dispatchers.IO) {
                val session = draftStore.readDownloadedSession(user.id)
                val packs = draftStore.readOffline(user.id, localOnly = false)
                session to packs
            }
            ensureActive()
            session.onSuccess { saved ->
                hasLocalSession = saved != null && saved.session.isActive
                pendingStudy = saved != null && saved.locallyFinished && saved.session.mode == TestMode.STUDY.path
            }
            packs.onSuccess { rows ->
                collectionsCount = rows.size
                pendingPacks = rows.count { row -> row.summary?.let { it.finished && !it.synced }
                    ?: (row.frozenTime != null && row.result == null) }
            }
            completionReadError = session.exceptionOrNull() ?: packs.exceptionOrNull()
        }
    }
    val refreshHome: () -> Unit = {
        viewModel.refresh()
        masterRecovery=draftStore.readMaster(user.id)
        finishRecovery=draftStore.readFinish(user.id)
        answerRecovery=draftStore.read(user.id)
        refreshCompletions()
    }
    androidx.compose.runtime.LaunchedEffect(viewModel) { viewModel.load(DashboardSection.TIPS) }
    androidx.compose.runtime.LaunchedEffect(viewModel, canStartTests) {
        viewModel.initializeToday(canStartTests)
    }
    androidx.compose.runtime.DisposableEffect(user.id, draftStore) {
        onDispose { completionReadJob?.cancel() }
    }
    RefreshOnResume(refreshHome)

    com.mukhtabir.android.ui.components.PullToRefresh(state.isLoading, refreshHome, enabled = !state.studyLoading) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = AppLayoutTokens.pageInsets,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
        ) {
            item {
                AppPageHeading(
                    stringResource(R.string.welcome, user.displayName), stringResource(R.string.home_intro),
                    keepActionInline = true,
                    titleAnimationDurationMillis = androidx.compose.ui.res.integerResource(R.integer.startup_reveal_duration) * 3,
                    action = {
                        AppIconButton(onClick = refreshHome, enabled = !state.isLoading) {
                            androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Default.Refresh,
                                contentDescription = stringResource(R.string.refresh))
                        }
                    },
                )
            }
            item {
                DashboardCard(DashboardSection.TIPS, stringResource(R.string.tips_title), state, viewModel) {
                    state.currentTip?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    if (DashboardSection.TIPS in state.loaded && DashboardSection.TIPS !in state.loading &&
                        DashboardSection.TIPS !in state.errors && state.currentTip == null) Text(stringResource(R.string.home_no_tips))
                }
            }
            item {
                com.mukhtabir.android.ui.components.AppPendingSessionCard(stringResource(R.string.flow_continue)) {
                    val sessionNeedsReview = answerRecovery.isFailure || answerRecovery.getOrNull() != null ||
                        finishRecovery.isFailure || finishRecovery.getOrNull() != null
                    AppActionRow {
                        if (canStartTests) Button(onOpenQuizzes) { Text(stringResource(when {
                            sessionNeedsReview -> R.string.flow_resolve
                            pendingStudy -> R.string.flow_review_sync
                            hasLocalSession -> R.string.flow_resume
                            else -> R.string.flow_check_sessions
                        })) }
                        if (collectionsCount > 0 || completionReadError != null) OutlinedButton(onOpenOfflineStudy) { Text(stringResource(R.string.flow_review_sync)) }
                        if (masterRecovery.getOrNull() != null || masterRecovery.isFailure) OutlinedButton(onOpenAssignedExam) {
                            Text(stringResource(R.string.master_reopen_home))
                        }
                    }
                    if (collectionsCount > 0) Text(stringResource(R.string.flow_downloaded_count, formatNumber(collectionsCount)))
                    if (pendingPacks > 0) Text(stringResource(R.string.flow_ready_count, formatNumber(pendingPacks)))
                    if (sessionNeedsReview) Text(stringResource(if (finishRecovery.isFailure || answerRecovery.isFailure)
                        R.string.quiz_finish_storage_error else R.string.quiz_finish_home_hint))
                    if (masterRecovery.getOrNull() != null || masterRecovery.isFailure) Text(stringResource(
                        if (masterRecovery.isFailure) R.string.master_recovery_storage else R.string.master_reopen_hint))
                    completionReadError?.let { com.mukhtabir.android.ui.components.AppRecoveryPanel(
                        message = stringResource(R.string.home_pending_completion_read_error), throwable = it, onRetry = refreshCompletions) }
                }
            }
            if (canStartTests) item {
                DashboardCard(DashboardSection.TODAY, stringResource(R.string.home_today), state, viewModel,
                    subtitle = stringResource(R.string.home_today_description)) {
                    androidx.compose.material3.Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                        Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                            val needsReview = answerRecovery.isFailure || answerRecovery.getOrNull() != null || finishRecovery.isFailure || finishRecovery.getOrNull() != null
                            val continuing = needsReview || pendingStudy || hasLocalSession || pendingPacks > 0 || completionReadError != null
                            Text(stringResource(R.string.home_next_action), style = MaterialTheme.typography.labelMedium)
                            Text(stringResource(if (continuing) R.string.flow_continue else R.string.study_now_title), style = MaterialTheme.typography.titleLarge)
                            Text(stringResource(if (continuing) R.string.flow_continue_hint else R.string.study_now_description), style = MaterialTheme.typography.bodyMedium)
                            val todayFresh = DashboardSection.TODAY in state.loaded && DashboardSection.TODAY !in state.loading &&
                                DashboardSection.TODAY !in state.errors && DashboardSection.TODAY !in state.stale
                            val targetDone = todayFresh && state.planner?.let { (it.dailyProgress[it.today] ?: 0) >= it.targetQuestionsPerDay && it.targetQuestionsPerDay > 0 } == true
                            if (targetDone) Text(stringResource(R.string.flow_daily_optional))
                            Button({ when {
                                completionReadError != null -> refreshCompletions()
                                needsReview || pendingStudy || hasLocalSession -> onOpenQuizzes()
                                pendingPacks > 0 -> onOpenOfflineStudy()
                                else -> scope.launch {
                                    val ids = viewModel.fetchStudyNow(minOf(20, maxQuizQuestions), canStartTests)
                                    if (!ids.isNullOrEmpty()) onStartStudyNow(ids)
                                }
                            } }, Modifier.fillMaxWidth(), enabled = !state.studyLoading) { Text(stringResource(when {
                                completionReadError != null -> R.string.retry
                                needsReview -> R.string.flow_resolve
                                pendingStudy -> R.string.flow_review_sync
                                hasLocalSession -> R.string.flow_resume
                                pendingPacks > 0 -> R.string.flow_review_sync
                                else -> R.string.study_now_start
                            })) }
                            if (state.studyLoading) AppLoadingIndicator(Modifier.fillMaxWidth())
                            state.studyError?.let { ErrorState(throwable = it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                            if (state.studyEmpty) Text(stringResource(R.string.study_now_empty), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                            if (DashboardSection.TODAY in state.loaded) Text(stringResource(R.string.smart_review_counts, state.srsDueCount,
                                state.todayAttempts.wrongOpen, state.todayAttempts.fragileCorrect), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (DashboardSection.TODAY in state.errors || DashboardSection.TODAY in state.stale)
                        Text(stringResource(R.string.ui_summary_unavailable), style = MaterialTheme.typography.bodySmall)
                    state.planner?.takeIf { DashboardSection.TODAY in state.loaded && DashboardSection.TODAY !in state.loading &&
                        DashboardSection.TODAY !in state.errors && DashboardSection.TODAY !in state.stale }?.let { plan ->
                        val done = plan.dailyProgress[plan.today] ?: 0
                        Text(stringResource(R.string.flow_daily), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.flow_daily_counts, formatNumber(done.coerceAtLeast(0)),
                            formatNumber(plan.targetQuestionsPerDay.coerceAtLeast(0)), formatNumber((plan.targetQuestionsPerDay - done).coerceAtLeast(0))))
                        if (plan.targetQuestionsPerDay > 0 && done >= plan.targetQuestionsPerDay) Text(stringResource(R.string.flow_daily_done))
                        Text(stringResource(R.string.flow_due_separate), style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator((done.toFloat() / plan.targetQuestionsPerDay.coerceAtLeast(1)).coerceIn(0f, 1f), Modifier.fillMaxWidth())
                    }
                    state.currentStreak?.let { Text(stringResource(R.string.home_streak, formatNumber(it)), style = MaterialTheme.typography.bodyMedium) }
                    AppTextButton(onOpenPlanner) { Text(stringResource(R.string.nav_planner)) }
                }
            }
            item {
                DashboardCard(DashboardSection.MODES, stringResource(R.string.home_study_modes), state, viewModel,
                    subtitle = stringResource(R.string.home_modes_description)) {
                    if (canStartTests) {
                        HomeModeGrid(listOf(
                            HomeModeData(stringResource(R.string.study), stringResource(R.string.home_mode_study), TestMode.STUDY),
                            HomeModeData(stringResource(R.string.exam), stringResource(R.string.home_mode_exam), TestMode.EXAM),
                            HomeModeData(stringResource(R.string.recall), stringResource(R.string.home_mode_recall), TestMode.RECALL),
                        ), onOpenStudyMode)
                    }
                    HomeLink(stringResource(R.string.question_library), onOpenLibrary)
                    onCreateQuestion?.let { HomeLink(stringResource(R.string.create_question), it) }
                    HomeLink(stringResource(R.string.bookmarks), onOpenBookmarks)
                }
            }
            item {
                DashboardCard(DashboardSection.OVERVIEW, stringResource(R.string.home_overview), state, viewModel,
                    subtitle = stringResource(R.string.home_progress_description)) {
                    HomeMetrics(listOf(
                        HomeMetricData(if (DashboardSection.OVERVIEW in state.loaded) formatNumber(state.attempts.wrongOpen) else stringResource(R.string.dashboard_unavailable), stringResource(R.string.nav_mistakes), onOpenMistakes),
                        HomeMetricData(if (DashboardSection.OVERVIEW in state.loaded) formatNumber(state.attempts.fragileCorrect) else stringResource(R.string.dashboard_unavailable), stringResource(R.string.nav_fragile), onOpenFragile),
                        HomeMetricData(if (DashboardSection.OVERVIEW in state.loaded) formatNumber(state.attempts.everCorrect) else stringResource(R.string.dashboard_unavailable), stringResource(R.string.home_correct_once)),
                        HomeMetricData(if (DashboardSection.OVERVIEW in state.loaded) formatNumber(state.attempts.totalSeen) else stringResource(R.string.dashboard_unavailable), stringResource(R.string.home_questions_seen)),
                    ))
                    HomeMetrics(listOf(
                        HomeMetricData(state.bankTotal?.let(::formatNumber) ?: stringResource(R.string.dashboard_unavailable), stringResource(R.string.home_bank_total), onOpenLibrary),
                        HomeMetricData(state.verifiedTotal?.let(::formatNumber) ?: stringResource(R.string.dashboard_unavailable), stringResource(R.string.verified)),
                        HomeMetricData(state.bankTotal?.let { total -> state.verifiedTotal?.let { verified -> formatNumber((total - verified).coerceAtLeast(0)) } } ?: stringResource(R.string.dashboard_unavailable), stringResource(R.string.unverified)),
                        HomeMetricData(if (DashboardSection.OVERVIEW in state.loaded) formatNumber(state.bookmarkCount) else stringResource(R.string.dashboard_unavailable), stringResource(R.string.bookmarks), onOpenBookmarks),
                    ))
                    if (canViewHistory) HomeLink(stringResource(R.string.history), onOpenHistory)
                    HomeLink(stringResource(R.string.analytics), onOpenAnalytics)
                }
            }
            item {
                DashboardCard(DashboardSection.RECENT, stringResource(R.string.home_recent_questions), state, viewModel,
                    subtitle = stringResource(R.string.home_recent_description)) {
                    val bank = state.bank
                    if (bank != null && bank.items.isEmpty() && DashboardSection.RECENT !in state.loading && DashboardSection.RECENT !in state.errors) com.mukhtabir.android.ui.components.EmptyState(stringResource(R.string.no_questions_found))
                    bank?.items?.forEach { question ->
                        Card(onClick = { onOpenQuestion(question.id) }, modifier = Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                                com.mukhtabir.android.ui.components.ClinicalCaseContext(question.localizedForDisplay().case)
                                com.mukhtabir.android.ui.components.MarkdownText(question.localizedForDisplay().question)
                                com.mukhtabir.android.ui.components.QuestionImage(question.imageUrl, imageRepository)
                                question.categoryName?.let { com.mukhtabir.android.ui.components.AppMetadataRow(listOf(it)) }
                            }
                        }
                    }
                    AppTextButton(onOpenLibrary) { Text(stringResource(R.string.all_questions)) }
                }
            }
            item {
                DashboardCard(DashboardSection.ACTIVITY, stringResource(R.string.activity_title), state, viewModel,
                    subtitle = stringResource(R.string.activity_days, formatNumber(90))) {
                    state.activity?.let { activity ->
                        Text(stringResource(R.string.activity_summary, formatNumber(activity.currentStreak), formatNumber(activity.longestStreak),
                            formatNumber(activity.activeDays), formatNumber(activity.totalQuestions)))
                        HomeActivity(activity)
                    }
                    AppTextButton(onOpenActivity) { Text(stringResource(R.string.home_all_activity)) }
                }
            }
            item {
                DashboardCard(DashboardSection.EXAMS, stringResource(R.string.master_exams_title), state, viewModel) {
                    val exams = state.notices.orEmpty()
                    if (DashboardSection.EXAMS in state.loaded && exams.isEmpty()) Text(stringResource(R.string.master_empty))
                    exams.forEach { exam -> HomeLink(exam.name) { onOpenExam(exam.id) } }
                }
            }

        }
    }
}

/** Controlled expansion keeps server reads and image composition behind an explicit open. */
@Composable
private fun DashboardCard(
    section: DashboardSection,
    title: String,
    state: DashboardUiState,
    viewModel: DashboardViewModel,
    subtitle: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val expanded = section in state.expanded
    androidx.compose.foundation.layout.Box(Modifier.fillMaxWidth()) {
        val summary = when {
            section in state.loading -> stringResource(R.string.loading)
            section in state.errors || section in state.stale -> stringResource(R.string.ui_summary_unavailable)
            !section.requiresData -> listOf(stringResource(R.string.study), stringResource(R.string.exam), stringResource(R.string.recall)).joinToString(" · ")
            section !in state.loaded -> stringResource(R.string.ui_loading_summary)
            section == DashboardSection.TODAY -> stringResource(R.string.smart_review_counts,
                state.srsDueCount, state.todayAttempts.wrongOpen, state.todayAttempts.fragileCorrect)
            section == DashboardSection.OVERVIEW -> state.bankTotal?.let { stringResource(R.string.ui_summary_count, formatNumber(it)) }
            section == DashboardSection.ACTIVITY -> state.activity?.let { stringResource(R.string.activity_summary,
                formatNumber(it.currentStreak), formatNumber(it.longestStreak), formatNumber(it.activeDays), formatNumber(it.totalQuestions)) }
            section == DashboardSection.RECENT -> state.bank?.items?.size?.let { stringResource(R.string.ui_summary_count, formatNumber(it)) }
            section == DashboardSection.EXAMS -> state.notices?.size?.let { stringResource(R.string.ui_summary_count, formatNumber(it)) }
            section == DashboardSection.TIPS -> stringResource(R.string.ui_summary_count, formatNumber(state.tips.size))
            else -> subtitle
        }
        AppSection(title, subtitle = if (expanded) subtitle else summary, expandedOverride = expanded,
            onExpandedChange = { viewModel.setExpanded(section, it) }, composeCollapsedContent = false) {
            if (section in state.loading) AppLoadingIndicator(Modifier.fillMaxWidth())
            state.errors[section]?.let { error ->
                ErrorState(throwable = error, onRetry = if (section in state.loading) null else ({ viewModel.load(section, force = true) }),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            if (!section.requiresData || section == DashboardSection.TODAY || section in state.loaded) content()
        }
    }
}

private data class HomeMetricData(val value: String, val label: String, val onClick: (() -> Unit)? = null)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeMetrics(metrics: List<HomeMetricData>) {
    com.mukhtabir.android.ui.components.AppTileGrid(metrics.size) { index, tileModifier ->
        val metric=metrics[index]
        com.mukhtabir.android.ui.components.AppMetricTile(metric.value,metric.label,tileModifier,onClick=metric.onClick)
    }
}

private data class HomeModeData(val title: String, val description: String, val mode: TestMode)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeModeGrid(modes: List<HomeModeData>, onOpen: (TestMode) -> Unit) {
    com.mukhtabir.android.ui.components.AppTileGrid(modes.size) { index, tileModifier ->
        val mode=modes[index]
        val accent=when(mode.mode) {
            TestMode.STUDY -> MaterialTheme.colorScheme.primary
            TestMode.EXAM -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.tertiary
        }
        com.mukhtabir.android.ui.components.AppMetricTile(mode.title,mode.description,tileModifier,
            onClick={onOpen(mode.mode)},accent=accent,
            container=androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background,accent,.07f),
            valueStyle=MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun HomeActivity(activity: StreakHistoryDto) {
    val days = activity.days.sortedBy { it.date }
    val leading = days.firstOrNull()?.date?.let { runCatching { java.time.LocalDate.parse(it).dayOfWeek.value - 1 }.getOrDefault(0) } ?: 0
    val cells = List<com.mukhtabir.android.data.remote.StreakDayDto?>(leading) { null } + days
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                (week + List<com.mukhtabir.android.data.remote.StreakDayDto?>(7 - week.size) { null }).forEach { day ->
                    val label = day?.let { stringResource(R.string.activity_day, com.mukhtabir.android.util.formatCalendarDate(it.date), formatNumber(it.sessions), formatNumber(it.questions)) }
                    androidx.compose.material3.Surface(Modifier.weight(1f).semantics { label?.let { contentDescription = it } },
                        shape = MaterialTheme.shapes.extraSmall,
                        color = if (day != null && day.questions > 0) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant) {
                        Text(day?.questions?.let(::formatNumber) ?: "", Modifier.padding(vertical = 6.dp),
                            style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeLink(label: String, onClick: () -> Unit) {
    com.mukhtabir.android.ui.components.AppListRow(Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.ChevronRight,
            modifier = Modifier.directionalIcon(),
            contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
