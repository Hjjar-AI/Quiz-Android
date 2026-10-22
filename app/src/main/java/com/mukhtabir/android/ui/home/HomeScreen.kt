package com.mukhtabir.android.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.data.remote.AnalyticsService
import com.mukhtabir.android.data.remote.BookmarkService
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.MasterExamService
import com.mukhtabir.android.data.remote.PlannerService
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.StreakHistoryDto
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.data.remote.TipsService
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppActivityHeatmap
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppCardBody
import com.mukhtabir.android.ui.components.AppHero
import com.mukhtabir.android.ui.components.AppHeroRow
import com.mukhtabir.android.ui.components.AppIconButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppListRow
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppMetricTile
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppProgressRing
import com.mukhtabir.android.ui.components.AppRecoveryPanel
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTileGrid
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.PullToRefresh
import com.mukhtabir.android.ui.components.QuestionListCard
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.activityCalendarCells
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.components.directionalIcon
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val state by viewModel.state.collectAsLifecycleState()
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

    val needsReview = answerRecovery.isFailure || answerRecovery.getOrNull() != null ||
        finishRecovery.isFailure || finishRecovery.getOrNull() != null
    val continuing = needsReview || pendingStudy || hasLocalSession || pendingPacks > 0 || completionReadError != null
    val todayFresh = DashboardSection.TODAY in state.loaded && DashboardSection.TODAY !in state.loading &&
        DashboardSection.TODAY !in state.errors && DashboardSection.TODAY !in state.stale
    val plan = state.planner?.takeIf { todayFresh }

    PullToRefresh(state.isLoading, refreshHome, enabled = !state.studyLoading) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
        val scale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(
                if (maxWidth >= AppLayoutTokens.homeColumnMinWidth * scale * 2) 2 else 1),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
            contentPadding = AppLayoutTokens.pageInsets,
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
        ) {
            item(key = "greeting", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
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
            item(key = "learning-hero", span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                AppHero {
                    val introduction: @Composable () -> Unit = {
                        Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                            Text(stringResource(R.string.home_next_action), style = MaterialTheme.typography.labelMedium)
                            Text(stringResource(if (continuing || !canStartTests) R.string.flow_continue else R.string.study_now_title),
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.semantics { heading() })
                            Text(stringResource(if (continuing || !canStartTests) R.string.flow_continue_hint else R.string.study_now_description))

                        }
                    }
                    if (plan != null && plan.targetQuestionsPerDay > 0) {
                        val done = (plan.dailyProgress[plan.today] ?: 0).coerceAtLeast(0)
                        AppHeroRow(decoration = {
                            AppProgressRing(
                                done.toFloat() / plan.targetQuestionsPerDay,
                                formatNumber(done), stringResource(R.string.flow_daily))
                        }, content = introduction)
                    } else introduction()
                    val primary = when {
                        !canStartTests -> HomePrimaryAction(R.string.question_library, onOpenLibrary)
                        completionReadError != null -> HomePrimaryAction(R.string.retry, refreshCompletions)
                        needsReview -> HomePrimaryAction(R.string.flow_resolve, onOpenQuizzes)
                        pendingStudy -> HomePrimaryAction(R.string.flow_review_sync, onOpenQuizzes)
                        hasLocalSession -> HomePrimaryAction(R.string.flow_resume, onOpenQuizzes)
                        pendingPacks > 0 -> HomePrimaryAction(R.string.flow_review_sync, onOpenOfflineStudy)
                        else -> HomePrimaryAction(R.string.study_now_start) {
                            scope.launch {
                                val ids = viewModel.fetchStudyNow(minOf(20, maxQuizQuestions), canStartTests)
                                if (!ids.isNullOrEmpty()) onStartStudyNow(ids)
                            }
                        }
                    }
                    AppButton(primary.onClick, Modifier.fillMaxWidth(), enabled = !state.studyLoading) {
                        Text(stringResource(primary.label))
                    }
                    if (state.studyLoading) AppLoadingIndicator()
                    state.studyError?.let { ErrorState(throwable = it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                    if (state.studyEmpty) Text(stringResource(R.string.study_now_empty), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    if (needsReview) Text(stringResource(if (finishRecovery.isFailure || answerRecovery.isFailure)
                        R.string.quiz_finish_storage_error else R.string.quiz_finish_home_hint))
                    completionReadError?.let { AppRecoveryPanel(
                        message = stringResource(R.string.home_pending_completion_read_error), throwable = it, onRetry = refreshCompletions) }

                }
            }
            item(key = "learning-details") {
                AppCard(Modifier.fillMaxWidth()) {
                    AppCardBody {
                            if (plan != null) {
                                val done = plan.dailyProgress[plan.today] ?: 0
                                Text(stringResource(R.string.flow_daily_counts, formatNumber(done.coerceAtLeast(0)),
                                    formatNumber(plan.targetQuestionsPerDay.coerceAtLeast(0)),
                                    formatNumber((plan.targetQuestionsPerDay - done).coerceAtLeast(0))))
                                if (plan.targetQuestionsPerDay > 0 && done >= plan.targetQuestionsPerDay)
                                    Text(stringResource(R.string.flow_daily_optional))
                            }
                            state.currentStreak?.takeIf { todayFresh }?.let {
                                Text(stringResource(R.string.home_streak, formatNumber(it)))
                            }
                    if (collectionsCount > 0) Text(stringResource(R.string.flow_downloaded_count, formatNumber(collectionsCount)))
                    if (pendingPacks > 0) Text(stringResource(R.string.flow_ready_count, formatNumber(pendingPacks)))
                    AppActionRow {
                        if (canStartTests && !needsReview && !pendingStudy && !hasLocalSession)
                            AppTextButton(onOpenQuizzes) { Text(stringResource(R.string.flow_check_sessions)) }
                        if (collectionsCount > 0 || completionReadError != null)
                            AppTextButton(onOpenOfflineStudy) { Text(stringResource(R.string.offline_open_cached)) }
                        if (masterRecovery.getOrNull() != null || masterRecovery.isFailure)
                            AppTextButton(onOpenAssignedExam) { Text(stringResource(R.string.master_reopen_home)) }
                        AppTextButton(onOpenPlanner) { Text(stringResource(R.string.nav_planner)) }
                    }
                    if (masterRecovery.getOrNull() != null || masterRecovery.isFailure) Text(stringResource(
                        if (masterRecovery.isFailure) R.string.master_recovery_storage else R.string.master_reopen_hint))
                    if (canStartTests) {
                        if (todayFresh) Text(stringResource(R.string.smart_review_counts, com.mukhtabir.android.util.formatNumber(state.srsDueCount), com.mukhtabir.android.util.formatNumber(state.todayAttempts.wrongOpen), com.mukhtabir.android.util.formatNumber(state.todayAttempts.fragileCorrect)), style = MaterialTheme.typography.bodySmall)
                        else {
                            Text(stringResource(if (DashboardSection.TODAY in state.loading) R.string.loading else R.string.ui_summary_unavailable))
                            state.errors[DashboardSection.TODAY]?.let { ErrorState(throwable = it) }
                            AppTextButton({ viewModel.load(DashboardSection.TODAY, force = true) },
                                enabled = DashboardSection.TODAY !in state.loading) { Text(stringResource(R.string.refresh)) }
                        }
                    }
                    }
                }
            }
            item(key = "modes") {
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
            item(key = "overview") {
                DashboardCard(DashboardSection.OVERVIEW, stringResource(R.string.home_overview), state, viewModel,
                    subtitle = stringResource(R.string.home_progress_description)) {
                    val unavailable = stringResource(R.string.dashboard_unavailable)
                    fun overviewMetric(value: Number): String =
                        if (DashboardSection.OVERVIEW in state.loaded) formatNumber(value.toLong()) else unavailable
                    HomeMetrics(listOf(
                        HomeMetricData(overviewMetric(state.attempts.wrongOpen), stringResource(R.string.nav_mistakes), onOpenMistakes),
                        HomeMetricData(overviewMetric(state.attempts.fragileCorrect), stringResource(R.string.nav_fragile), onOpenFragile),
                        HomeMetricData(overviewMetric(state.attempts.everCorrect), stringResource(R.string.home_correct_once)),
                        HomeMetricData(overviewMetric(state.attempts.totalSeen), stringResource(R.string.home_questions_seen)),
                    ))
                    HomeMetrics(listOf(
                        HomeMetricData(state.bankTotal?.let(::formatNumber) ?: stringResource(R.string.dashboard_unavailable), stringResource(R.string.home_bank_total), onOpenLibrary),
                        HomeMetricData(state.verifiedTotal?.let(::formatNumber) ?: stringResource(R.string.dashboard_unavailable), stringResource(R.string.verified)),
                        HomeMetricData(state.bankTotal?.let { total -> state.verifiedTotal?.let { verified -> formatNumber((total - verified).coerceAtLeast(0)) } } ?: stringResource(R.string.dashboard_unavailable), stringResource(R.string.unverified)),
                        HomeMetricData(overviewMetric(state.bookmarkCount), stringResource(R.string.bookmarks), onOpenBookmarks),
                    ))
                    if (canViewHistory) HomeLink(stringResource(R.string.history), onOpenHistory)
                    HomeLink(stringResource(R.string.analytics), onOpenAnalytics)
                }
            }
            item(key = "recent") {
                DashboardCard(DashboardSection.RECENT, stringResource(R.string.home_recent_questions), state, viewModel,
                    subtitle = stringResource(R.string.home_recent_description)) {
                    val bank = state.bank
                    if (bank != null && bank.items.isEmpty() && DashboardSection.RECENT !in state.loading && DashboardSection.RECENT !in state.errors) EmptyState(stringResource(R.string.no_questions_found))
                    bank?.items?.forEach { question ->
                        QuestionListCard(question, imageRepository,
                            onOpen = { onOpenQuestion(question.id) })
                    }
                    AppTextButton(onOpenLibrary) { Text(stringResource(R.string.all_questions)) }
                }
            }
            item(key = "activity") {
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
            item(key = "exams") {
                DashboardCard(DashboardSection.EXAMS, stringResource(R.string.master_exams_title), state, viewModel) {
                    val exams = state.notices.orEmpty()
                    if (DashboardSection.EXAMS in state.loaded && exams.isEmpty()) EmptyState(stringResource(R.string.master_empty))
                    exams.forEach { exam -> HomeLink(exam.name) { onOpenExam(exam.id) } }
                }
            }

            item(key = "tips") {
                DashboardCard(DashboardSection.TIPS, stringResource(R.string.tips_title), state, viewModel) {
                    state.currentTip?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                    if (DashboardSection.TIPS in state.loaded && DashboardSection.TIPS !in state.loading &&
                        DashboardSection.TIPS !in state.errors && state.currentTip == null) Text(stringResource(R.string.home_no_tips))
                }
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
            section == DashboardSection.TODAY -> stringResource(R.string.smart_review_counts, com.mukhtabir.android.util.formatNumber(state.srsDueCount), com.mukhtabir.android.util.formatNumber(state.todayAttempts.wrongOpen), com.mukhtabir.android.util.formatNumber(state.todayAttempts.fragileCorrect))
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
    AppTileGrid(metrics.size) { index, tileModifier ->
        val metric=metrics[index]
        AppMetricTile(metric.value,metric.label,tileModifier,onClick=metric.onClick)
    }
}

private data class HomeModeData(val title: String, val description: String, val mode: TestMode)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeModeGrid(modes: List<HomeModeData>, onOpen: (TestMode) -> Unit) {
    AppTileGrid(modes.size) { index, tileModifier ->
        val mode=modes[index]
        val accent=when(mode.mode) {
            TestMode.STUDY -> MaterialTheme.colorScheme.primary
            TestMode.EXAM -> MaterialTheme.colorScheme.secondary
            else -> MaterialTheme.colorScheme.tertiary
        }
        AppMetricTile(mode.title,mode.description,tileModifier,
            onClick={onOpen(mode.mode)},accent=accent,
            container=androidx.compose.ui.graphics.lerp(MaterialTheme.colorScheme.background,accent,.07f),
            valueStyle=MaterialTheme.typography.titleMedium,
            icon = when (mode.mode) {
                TestMode.STUDY -> Icons.Outlined.MenuBook
                TestMode.EXAM -> Icons.Outlined.Quiz
                TestMode.RECALL -> Icons.Outlined.Psychology
            })
    }
}

@Composable
private fun HomeActivity(activity: StreakHistoryDto) {
    AppActivityHeatmap(
        activityCalendarCells(activity.days))
}

@Composable
private fun HomeLink(label: String, onClick: () -> Unit) {
    AppListRow(Modifier.clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onClick)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        androidx.compose.material3.Icon(androidx.compose.material.icons.Icons.Filled.ChevronRight,
            modifier = Modifier.directionalIcon(),
            contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private data class HomePrimaryAction(val label: Int, val onClick: () -> Unit)
