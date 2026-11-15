@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.demo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.StreakDayDto
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatCalendarDate
import com.mukhtabir.android.util.formatNumber
import kotlinx.coroutines.launch
import java.time.LocalDate

private enum class DemoDestination(val label: Int, val icon: ImageVector) {
    HOME(R.string.home, Icons.Filled.Home),
    LIBRARY(R.string.nav_library_short, Icons.Filled.ListAlt),
    QUIZZES(R.string.quizzes, Icons.Filled.Quiz),
    BOOKMARKS(R.string.bookmarks, Icons.Filled.Bookmark),
    HISTORY(R.string.history, Icons.Filled.History),
    ANALYTICS(R.string.analytics, Icons.Filled.BarChart),
    PLANNER(R.string.nav_planner, Icons.Filled.DateRange),
    MISTAKES(R.string.nav_mistakes, Icons.Filled.ErrorOutline),
    FRAGILE(R.string.nav_fragile, Icons.Filled.WarningAmber),
    OFFLINE(R.string.offline_title, Icons.Filled.Download),
    PROFILE(R.string.nav_profile, Icons.Filled.Person),
    CONNECTIONS(R.string.demo_connections, Icons.Filled.Settings),
}
private val demoTabs = listOf(DemoDestination.HOME, DemoDestination.LIBRARY, DemoDestination.QUIZZES, DemoDestination.BOOKMARKS)

/** Offline learner shell. Dependencies are deliberately limited to the local fake repository. */
@Composable
fun DemoScreen(onExit: () -> Unit) {
    val repository = rememberSaveable(saver = DemoRepository.StateSaver) { DemoRepository() }
    val screenState = rememberSaveableStateHolder()
    var destination by rememberSaveable { mutableStateOf(DemoDestination.HOME) }
    var detail by rememberSaveable { mutableStateOf<Int?>(null) }
    var practicing by rememberSaveable { mutableStateOf(false) }
    var launchIds by rememberSaveable { mutableStateOf<List<Int>?>(null) }
    var exitDialog by rememberSaveable { mutableStateOf(false) }
    var mode by rememberSaveable { mutableStateOf(TestMode.STUDY) }
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val active = repository.state.session.page == DemoPage.SESSION
    fun navigate(target: DemoDestination) { destination = target; detail = null }
    fun practice(ids: List<Int>, initial: TestMode = TestMode.STUDY) {
        if (active) practicing = true
        else if (!repository.state.connected) navigate(DemoDestination.OFFLINE)
        else { mode = initial; launchIds = ids }
    }
    BackHandler {
        when {
            drawer.isOpen -> scope.launch { drawer.close() }
            detail != null -> detail = null
            destination != DemoDestination.HOME -> navigate(DemoDestination.HOME)
            else -> exitDialog = true
        }
    }
    if (exitDialog) AppConfirmationDialog(onDismissRequest = { exitDialog = false },
        title = { Text(stringResource(R.string.demo_exit)) }, text = { Text(stringResource(R.string.demo_exit_message)) },
        confirmButton = { AppDangerTextButton(onExit) { Text(stringResource(R.string.demo_exit)) } },
        dismissButton = { AppTextButton({ exitDialog = false }) { Text(stringResource(R.string.cancel)) } })
    launchIds?.let { ids -> PracticeLauncher(stringResource(R.string.demo_collection), ids.map(Int::toLong), repository.ids.size, repository.state.connected && !active,
        onDismiss = { launchIds = null }, initialMode = mode,
        onStart = { chosen, selected, _ ->
            repository.start(DemoMode.valueOf(chosen.name), selected.map(Long::toInt))
            launchIds = null; practicing = true
        }, onDownload = { selected ->
            repository.download(selected.map(Long::toInt)); launchIds = null; navigate(DemoDestination.OFFLINE)
        }) }
    if (practicing && repository.state.session.total > 0) {
        key(repository.state.session.id) { DemoPracticeScreen(repository) { practicing = false } }
        return
    }
    ModalNavigationDrawer(drawerState = drawer, drawerContent = {
        ModalDrawerSheet {
            Column(Modifier.verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.content)) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.demo_learner))
                Text(stringResource(R.string.demo_badge), style = MaterialTheme.typography.labelMedium)
                listOf(
                    R.string.more_learn to listOf(DemoDestination.HOME, DemoDestination.LIBRARY, DemoDestination.QUIZZES, DemoDestination.PLANNER, DemoDestination.OFFLINE),
                    R.string.more_review to listOf(DemoDestination.BOOKMARKS, DemoDestination.HISTORY, DemoDestination.MISTAKES, DemoDestination.FRAGILE, DemoDestination.ANALYTICS),
                    R.string.more_account to listOf(DemoDestination.PROFILE, DemoDestination.CONNECTIONS),
                ).forEach { (section, targets) ->
                    Text(stringResource(section), style = MaterialTheme.typography.labelLarge)
                    targets.forEach { target ->
                        NavigationDrawerItem(label = { Text(stringResource(target.label)) }, selected = destination == target,
                            icon = { Icon(target.icon, null) }, onClick = {
                                navigate(target); scope.launch { drawer.close() }
                            })
                    }
                }
                AppOutlinedButton({ exitDialog = true; scope.launch { drawer.close() } }) { Text(stringResource(R.string.demo_exit)) }
            }
        }
    }) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val rail = maxWidth >= AppLayoutTokens.navigationRailBreakpoint * androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
            Row(Modifier.fillMaxSize()) {
                if (rail) NavigationRail(Modifier.fillMaxHeight().verticalScroll(rememberScrollState())) {
                    demoTabs.forEach { target -> NavigationRailItem(selected = destination == target,
                        onClick = { navigate(target) }, icon = { Icon(target.icon, null) }, label = { Text(stringResource(target.label)) }) }
                }
                Scaffold(modifier = Modifier.weight(1f), topBar = {
                    AppScreenBar(stringResource(if (detail != null) R.string.question_library else destination.label) +
                        " · " + stringResource(R.string.demo_badge), navigation = {
                        if (detail != null) AppIconButton({ detail = null }) { Icon(Icons.Filled.ArrowBack, stringResource(R.string.back), modifier = Modifier.directionalIcon()) }
                        else AppIconButton({ scope.launch { drawer.open() } }) { Icon(Icons.Filled.Menu, stringResource(R.string.demo_menu)) }
                    })
                }, bottomBar = {
                    if (!rail) NavigationBar {
                        demoTabs.forEach { target -> NavigationBarItem(selected = destination == target,
                            onClick = { navigate(target) }, icon = { Icon(target.icon, null) }, label = { Text(stringResource(target.label)) }) }
                    }
                }) { insets ->
                    screenState.SaveableStateProvider("${destination.name}-${detail ?: 0}") {
                        if (destination == DemoDestination.PROFILE && detail == null) {
                            Box(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets)) {
                                DemoProfile(repository) { exitDialog = true }
                            }
                        } else {
                            Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).imePadding()
                                .verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
                                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                                Text(stringResource(R.string.demo_scope), style = MaterialTheme.typography.bodySmall)
                                if (!repository.state.connected) {
                                    AppStatus(stringResource(R.string.demo_disconnected), kind = AppStatusKind.WARNING)
                                    if (destination != DemoDestination.CONNECTIONS) AppTextButton({ navigate(DemoDestination.CONNECTIONS) }) {
                                        Text(stringResource(R.string.demo_connections))
                                    }
                                }
                                if (repository.state.readFailed && destination != DemoDestination.CONNECTIONS) AppStatus(stringResource(R.string.demo_read_failed))
                                if (active) AppCard(Modifier.fillMaxWidth()) { AppCardBody {
                                    Text(stringResource(R.string.demo_paused_hint))
                                    AppButton({ practicing = true }) { Text(stringResource(R.string.flow_resume)) }
                                } }
                                val questionId = detail
                                if (questionId != null) {
                                    DemoQuestionDetail(repository, questionId) { practice(listOf(questionId)) }
                                } else when (destination) {
                                    DemoDestination.HOME -> DemoDashboard(repository, ::navigate, { detail = it }, { ids -> practice(ids) })
                                    DemoDestination.LIBRARY, DemoDestination.BOOKMARKS, DemoDestination.MISTAKES, DemoDestination.FRAGILE ->
                                        DemoLibrary(repository, when (destination) {
                                            DemoDestination.BOOKMARKS -> DemoPool.BOOKMARKS
                                            DemoDestination.MISTAKES -> DemoPool.MISTAKES
                                            DemoDestination.FRAGILE -> DemoPool.FRAGILE
                                            else -> DemoPool.ALL
                                        }, { detail = it }, { practice(it) })
                                    DemoDestination.QUIZZES -> DemoPracticeSetup(repository, mode, { mode = it },
                                        onStart = { chosen, selected -> repository.start(chosen, selected); practicing = repository.state.session.page == DemoPage.SESSION },
                                        onDownload = { navigate(DemoDestination.OFFLINE) })
                                    DemoDestination.HISTORY -> DemoHistory(repository, active) { record ->
                                        if (repository.review(record)) practicing = true
                                    }
                                    DemoDestination.ANALYTICS -> DemoAnalytics(repository)
                                    DemoDestination.PLANNER -> DemoStudyPlanner(repository) { practice(it) }
                                    DemoDestination.OFFLINE -> DemoDownloads(repository) {
                                        repository.start(DemoMode.STUDY, repository.state.downloadedIds, downloaded = true)
                                        practicing = repository.state.session.page == DemoPage.SESSION
                                    }
                                    DemoDestination.CONNECTIONS -> DemoConnections(repository)
                                    DemoDestination.PROFILE -> Unit
                                }
                                AppTextButton({ exitDialog = true }) { Text(stringResource(R.string.demo_exit)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DemoDashboard(repository: DemoRepository, navigate: (DemoDestination) -> Unit, open: (Int) -> Unit, practice: (List<Int>) -> Unit) {
    AppPageHeading(stringResource(R.string.demo_greeting), stringResource(R.string.home_next_action))
    AppHero {
        AppHeroRow(decoration = { if (repository.planActive) AppProgressRing(repository.planTodayAnswered.toFloat() / repository.state.target,
            formatNumber(repository.planTodayAnswered), stringResource(R.string.flow_daily)) }, content = {
            Column {
                Text(stringResource(R.string.study_now_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.study_now_description))
            }
        })
        AppButton({ practice(repository.planIds.take(minOf(repository.state.target, 8))) }, Modifier.fillMaxWidth(),
            enabled = repository.planActive && repository.state.connected) { Text(stringResource(R.string.study_now_start)) }
        AppTextButton({ navigate(DemoDestination.PLANNER) }) { Text(stringResource(R.string.nav_planner)) }
    }
    DemoMetrics(repository)
    AppActionRow {
        AppOutlinedButton({ navigate(DemoDestination.HISTORY) }) { Text(stringResource(R.string.history)) }
        AppOutlinedButton({ navigate(DemoDestination.ANALYTICS) }) { Text(stringResource(R.string.analytics)) }
    }
    AppSection(stringResource(R.string.more_review), initiallyExpanded = true) {
        AppActionRow {
            AppOutlinedButton({ navigate(DemoDestination.MISTAKES) }) { Text(stringResource(R.string.nav_mistakes)) }
            AppOutlinedButton({ navigate(DemoDestination.FRAGILE) }) { Text(stringResource(R.string.nav_fragile)) }
            AppOutlinedButton({ navigate(DemoDestination.OFFLINE) }) { Text(stringResource(R.string.offline_title)) }
        }
        if (repository.pendingRecords.isNotEmpty()) AppStatus(stringResource(R.string.demo_pending_sync), kind = AppStatusKind.INFO)
    }
    AppPageHeading(stringResource(R.string.home_recent_questions))
    repository.ids.take(3).forEach { id -> DemoQuestionRow(repository, id) { open(id) } }
    AppOutlinedButton({ navigate(DemoDestination.LIBRARY) }) { Text(stringResource(R.string.all_questions)) }
}

@Composable
private fun DemoMetrics(repository: DemoRepository) {
    val metrics = listOf(
        formatNumber(repository.ids.size) to stringResource(R.string.home_bank_total),
        formatNumber(repository.state.bookmarks.size) to stringResource(R.string.bookmarks),
        formatNumber(repository.answered) to stringResource(R.string.demo_answered_total),
        stringResource(R.string.demo_percentage, formatNumber(repository.accuracy)) to stringResource(R.string.demo_accuracy),
    )
    AppTileGrid(metrics.size) { index, modifier ->
        AppMetricTile(metrics[index].first, metrics[index].second, modifier)
    }
}

@Composable
private fun DemoLibrary(repository: DemoRepository, pool: DemoPool, open: (Int) -> Unit, practice: (List<Int>) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(0) }
    var tag by rememberSaveable { mutableStateOf(0) }
    var page by rememberSaveable { mutableStateOf(1) }
    AppPageHeading(demoPoolLabel(pool))
    AppSearchRow(field = { modifier ->
        AppTextField(query, { query = it; page = 1 }, modifier, label = { Text(stringResource(R.string.search_questions)) }, singleLine = true)
    }, action = { AppOutlinedButton(repository::refresh) { Text(stringResource(if (repository.state.readFailed) R.string.retry else R.string.refresh)) } })
    Text(stringResource(R.string.source_categories))
    AppActionRow {
        listOf(0 to R.string.all, 1 to R.string.demo_category_circulation, 2 to R.string.demo_category_systems).forEach { (id, label) ->
            AppFilterChip(category == id, { category = id; page = 1 }, label = { Text(stringResource(label)) })
        }
    }
    Text(stringResource(R.string.source_tags))
    AppActionRow {
        listOf(0, R.string.demo_tag_foundations, R.string.demo_tag_case_context).forEach { option ->
            AppFilterChip(tag == option, { tag = option; page = 1 }, label = { Text(stringResource(if (option == 0) R.string.all else option)) })
        }
    }
    val rows = repository.poolIds(pool).filter { id ->
        (category == 0 || repository.category(id) == category) && (tag == 0 || tag in repository.question(id).tags) &&
            stringResource(repository.question(id).stem).contains(query.trim(), ignoreCase = true)
    }
    Text(stringResource(R.string.flow_material_count, formatNumber(rows.size)))
    if (rows.isEmpty()) {
        EmptyState(stringResource(R.string.no_questions_found))
        if (query.isNotBlank() || category != 0 || tag != 0) AppTextButton({ query = ""; category = 0; tag = 0; page = 1 }) { Text(stringResource(R.string.library_reset)) }
    } else {
        AppButton({ practice(rows) }, enabled = repository.state.connected || repository.state.session.page == DemoPage.SESSION) { Text(stringResource(R.string.flow_practice)) }
        val totalPages = (rows.size + 3) / 4
        val currentPage = page.coerceIn(1, totalPages)
        rows.drop((currentPage - 1) * 4).take(4).forEach { id -> DemoQuestionRow(repository, id) { open(id) } }
        AppPagination(currentPage, totalPages, { page = currentPage - 1 }, { page = currentPage + 1 })
    }
}

@Composable
private fun DemoQuestionRow(repository: DemoRepository, id: Int, open: () -> Unit) {
    AppCard(onClick = open, modifier = Modifier.fillMaxWidth()) { AppCardBody {
        Text(stringResource(if (repository.category(id) == 1) R.string.demo_category_circulation else R.string.demo_category_systems),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(repository.question(id).stem), style = MaterialTheme.typography.titleMedium)
        AppMetadataRow(repository.question(id).tags.map { stringResource(it) })
        AppTextButton({ repository.toggleBookmark(id) }) { Text(stringResource(if (id in repository.state.bookmarks) R.string.remove_bookmark else R.string.demo_add_bookmark)) }
    } }
}

@Composable
private fun DemoQuestionDetail(repository: DemoRepository, id: Int, practice: () -> Unit) {
    val question = repository.question(id)
    AppMetadataRow(listOf(demoCategoryLabel(question.category)) + question.tags.map { stringResource(it) })
    question.caseStem?.let { QuestionTextCard(stringResource(it)) }
    QuestionTextCard(stringResource(question.stem))
    question.choices.forEachIndexed { index, text -> AnswerChoice(number = index + 1, text = stringResource(text),
        selected = false, enabled = false, onClick = null, answerCorrect = if (index == question.correct) true else null, reviewMode = true, interactiveText = true) }
    ExplanationCard(stringResource(question.explanation), title = stringResource(R.string.explanation))
    QuestionRatingSelector(repository.state.ratings[id] ?: 0, { repository.rate(id, it) }, enabled = repository.state.connected)
    if (!repository.state.connected) AppStatus(stringResource(R.string.demo_online_required), kind = AppStatusKind.WARNING)
    AppTextButton({ repository.toggleBookmark(id) }) { Text(stringResource(if (id in repository.state.bookmarks) R.string.remove_bookmark else R.string.demo_add_bookmark)) }
    AppButton(practice, enabled = repository.state.connected || repository.state.session.page == DemoPage.SESSION) { Text(stringResource(R.string.flow_practice)) }
}

@Composable
private fun DemoHistory(repository: DemoRepository, active: Boolean, review: (DemoRecord) -> Unit) {
    Text(stringResource(R.string.demo_history_hint))
    var mode by rememberSaveable { mutableStateOf<DemoMode?>(null) }
    AppActionRow {
        AppFilterChip(mode == null, { mode = null }, label = { Text(stringResource(R.string.all)) })
        DemoMode.values().forEach { option -> AppFilterChip(mode == option, { mode = option }, label = {
            Text(stringResource(when (option) { DemoMode.STUDY -> R.string.study; DemoMode.EXAM -> R.string.exam; DemoMode.RECALL -> R.string.recall }))
        }) }
    }
    val records = repository.state.records.filter { it.synced && (mode == null || it.session.mode == mode) }
    if (records.isEmpty()) EmptyState(stringResource(R.string.no_items))
    if (repository.pendingRecords.isNotEmpty()) AppStatus(stringResource(R.string.demo_pending_sync), kind = AppStatusKind.INFO)
    records.forEach { record ->
        val session = record.session
        AppCard(onClick = { review(record) }, modifier = Modifier.fillMaxWidth(), enabled = !active) { AppCardBody {
            Text(stringResource(when (session.mode) { DemoMode.STUDY -> R.string.study; DemoMode.EXAM -> R.string.exam; DemoMode.RECALL -> R.string.recall }), style = MaterialTheme.typography.titleMedium)
            Text(formatCalendarDate(record.date))
            Text(stringResource(R.string.demo_score, formatNumber(repository.correct(session)), formatNumber(session.total)))
            Text(stringResource(R.string.history_answered, formatNumber(session.answered), formatNumber(session.total)))
            Text(stringResource(R.string.demo_review), color = MaterialTheme.colorScheme.primary)
        } }
    }
}

@Composable
private fun DemoAnalytics(repository: DemoRepository) {
    DemoMetrics(repository)
    AppPageHeading(stringResource(R.string.activity_title))
    val today = LocalDate.now()
    val days = (27L downTo 0L).map { offset ->
        val date = today.minusDays(offset).toString()
        val records = repository.state.records.filter { it.synced && it.date == date }
        StreakDayDto(date, records.size, records.sumOf { it.session.answered })
    }
    AppActivityHeatmap(activityCalendarCells(days))
    AppPageHeading(stringResource(R.string.category_mastery))
    listOf(1 to R.string.demo_category_circulation, 2 to R.string.demo_category_systems).forEach { (category, label) ->
        val attempts = repository.state.records.filter { it.synced }.flatMap { record -> record.session.questionIds.mapIndexedNotNull { i, id ->
            if (repository.category(id) == category && record.session.answers[i] >= 0) record.session.answers[i] == repository.question(id).correct else null
        } }
        AppCard(Modifier.fillMaxWidth()) { AppCardBody {
            Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.demo_score, formatNumber(attempts.count { it }), formatNumber(attempts.size)))
        } }
    }
}
