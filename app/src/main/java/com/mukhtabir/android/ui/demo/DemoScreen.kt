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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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
    PROFILE(R.string.nav_profile, Icons.Filled.Person),
}
private val demoTabs = listOf(DemoDestination.HOME, DemoDestination.LIBRARY, DemoDestination.QUIZZES, DemoDestination.BOOKMARKS)

/** Offline learner shell. Dependencies are deliberately limited to the local fake repository. */
@Composable
fun DemoScreen(onExit: () -> Unit) {
    val repository = rememberSaveable(saver = DemoRepository.StateSaver) { DemoRepository() }
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
    launchIds?.let { ids -> PracticeLauncher(stringResource(R.string.demo_collection), ids.map(Int::toLong), repository.ids.size, true,
        onDismiss = { launchIds = null }, initialMode = mode,
        onStart = { chosen, selected, _ ->
            repository.start(DemoMode.valueOf(chosen.name), selected.map(Long::toInt))
            launchIds = null; practicing = true
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
                DemoDestination.values().forEach { target ->
                    NavigationDrawerItem(label = { Text(stringResource(target.label)) }, selected = destination == target,
                        icon = { Icon(target.icon, null) }, onClick = {
                            navigate(target); scope.launch { drawer.close() }
                        })
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
                    key(destination, detail) {
                        Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).imePadding()
                            .verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                            Text(stringResource(R.string.demo_scope), style = MaterialTheme.typography.bodySmall)
                            if (active) AppCard(Modifier.fillMaxWidth()) { AppCardBody {
                                Text(stringResource(R.string.demo_paused_hint))
                                AppButton({ practicing = true }) { Text(stringResource(R.string.flow_resume)) }
                            } }
                            val questionId = detail
                            if (questionId != null) {
                                DemoQuestionDetail(repository, questionId) { practice(listOf(questionId)) }
                            } else when (destination) {
                                DemoDestination.HOME -> DemoDashboard(repository, ::navigate, { detail = it }, { ids -> practice(ids) })
                                DemoDestination.LIBRARY -> DemoLibrary(repository, false, { detail = it }, { practice(it) })
                                DemoDestination.BOOKMARKS -> DemoLibrary(repository, true, { detail = it }, { practice(it) })
                                DemoDestination.QUIZZES -> {
                                    AppPageHeading(stringResource(R.string.home_study_modes), stringResource(R.string.demo_collection_hint))
                                    listOf(Triple(TestMode.STUDY, R.string.study, R.string.demo_study_hint),
                                        Triple(TestMode.EXAM, R.string.exam, R.string.demo_exam_hint),
                                        Triple(TestMode.RECALL, R.string.recall, R.string.demo_recall_hint)).forEach { (option, title, hint) ->
                                        AppCard(onClick = { practice(repository.ids, option) }, modifier = Modifier.fillMaxWidth()) { AppCardBody {
                                            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
                                            Text(stringResource(hint))
                                        } }
                                    }
                                }
                                DemoDestination.HISTORY -> DemoHistory(repository, active) { record ->
                                    if (repository.review(record)) practicing = true
                                }
                                DemoDestination.ANALYTICS -> DemoAnalytics(repository)
                                DemoDestination.PLANNER -> DemoPlanner(repository) { practice(repository.ids.take(3)) }
                                DemoDestination.PROFILE -> {
                                    AppPageHeading(stringResource(R.string.demo_learner), stringResource(R.string.demo_profile_hint))
                                    DemoMetrics(repository)
                                    Text(stringResource(R.string.demo_account_limits))
                                }
                            }
                            AppTextButton({ exitDialog = true }) { Text(stringResource(R.string.demo_exit)) }
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
        AppHeroRow(decoration = { AppProgressRing(repository.todayAnswered.toFloat() / repository.state.target,
            formatNumber(repository.todayAnswered), stringResource(R.string.flow_daily)) }, content = {
            Column {
                Text(stringResource(R.string.study_now_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.study_now_description))
            }
        })
        AppButton({ practice(repository.ids.take(3)) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.study_now_start)) }
        AppTextButton({ navigate(DemoDestination.PLANNER) }) { Text(stringResource(R.string.nav_planner)) }
    }
    DemoMetrics(repository)
    AppActionRow {
        AppOutlinedButton({ navigate(DemoDestination.HISTORY) }) { Text(stringResource(R.string.history)) }
        AppOutlinedButton({ navigate(DemoDestination.ANALYTICS) }) { Text(stringResource(R.string.analytics)) }
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
private fun DemoLibrary(repository: DemoRepository, bookmarksOnly: Boolean, open: (Int) -> Unit, practice: (List<Int>) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(0) }
    AppTextField(query, { query = it }, Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.search_questions)) }, singleLine = true)
    AppActionRow {
        listOf(0 to R.string.all, 1 to R.string.demo_category_circulation, 2 to R.string.demo_category_systems).forEach { (id, label) ->
            AppFilterChip(category == id, { category = id }, label = { Text(stringResource(label)) })
        }
    }
    val rows = repository.ids.filter { id ->
        (!bookmarksOnly || id in repository.state.bookmarks) && (category == 0 || repository.category(id) == category) &&
            stringResource(repository.question(id).stem).contains(query.trim(), ignoreCase = true)
    }
    Text(stringResource(R.string.flow_material_count, formatNumber(rows.size)))
    if (rows.isEmpty()) {
        EmptyState(stringResource(R.string.no_questions_found))
        if (query.isNotBlank() || category != 0) AppTextButton({ query = ""; category = 0 }) { Text(stringResource(R.string.library_reset)) }
    } else {
        AppButton({ practice(rows) }) { Text(stringResource(R.string.flow_practice)) }
        rows.forEach { id -> DemoQuestionRow(repository, id) { open(id) } }
    }
}

@Composable
private fun DemoQuestionRow(repository: DemoRepository, id: Int, open: () -> Unit) {
    AppCard(onClick = open, modifier = Modifier.fillMaxWidth()) { AppCardBody {
        Text(stringResource(if (repository.category(id) == 1) R.string.demo_category_circulation else R.string.demo_category_systems),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(stringResource(repository.question(id).stem), style = MaterialTheme.typography.titleMedium)
        AppTextButton({ repository.toggleBookmark(id) }) { Text(stringResource(if (id in repository.state.bookmarks) R.string.remove_bookmark else R.string.demo_add_bookmark)) }
    } }
}

@Composable
private fun DemoQuestionDetail(repository: DemoRepository, id: Int, practice: () -> Unit) {
    val question = repository.question(id)
    question.caseStem?.let { QuestionTextCard(stringResource(it)) }
    QuestionTextCard(stringResource(question.stem))
    question.choices.forEachIndexed { index, text -> AnswerChoice(number = index + 1, text = stringResource(text),
        selected = false, enabled = false, onClick = null, answerCorrect = if (index == question.correct) true else null, reviewMode = true, interactiveText = true) }
    ExplanationCard(stringResource(question.explanation), title = stringResource(R.string.explanation))
    AppTextButton({ repository.toggleBookmark(id) }) { Text(stringResource(if (id in repository.state.bookmarks) R.string.remove_bookmark else R.string.demo_add_bookmark)) }
    AppButton(practice) { Text(stringResource(R.string.flow_practice)) }
}

@Composable
private fun DemoHistory(repository: DemoRepository, active: Boolean, review: (DemoRecord) -> Unit) {
    Text(stringResource(R.string.demo_history_hint))
    repository.state.records.forEach { record ->
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
        val records = repository.state.records.filter { it.date == date }
        StreakDayDto(date, records.size, records.sumOf { it.session.answered })
    }
    AppActivityHeatmap(activityCalendarCells(days))
    AppPageHeading(stringResource(R.string.category_mastery))
    listOf(1 to R.string.demo_category_circulation, 2 to R.string.demo_category_systems).forEach { (category, label) ->
        val attempts = repository.state.records.flatMap { record -> record.session.questionIds.mapIndexedNotNull { i, id ->
            if (repository.category(id) == category && record.session.answers[i] >= 0) record.session.answers[i] == repository.question(id).correct else null
        } }
        AppCard(Modifier.fillMaxWidth()) { AppCardBody {
            Text(stringResource(label), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.demo_score, formatNumber(attempts.count { it }), formatNumber(attempts.size)))
        } }
    }
}

@Composable
private fun DemoPlanner(repository: DemoRepository, practice: () -> Unit) {
    var target by rememberSaveable { mutableStateOf(repository.state.target.toString()) }
    var saved by rememberSaveable { mutableStateOf(false) }
    val number = target.toIntOrNull()?.takeIf { it in 1..100 }
    AppPageHeading(stringResource(R.string.nav_planner), stringResource(R.string.demo_planner_hint))
    AppTextField(target, { value -> target = value.map { c -> Character.digit(c, 10).takeIf { it >= 0 }?.digitToChar() ?: c }.joinToString(""); saved = false },
        Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.demo_daily_target)) }, isError = number == null,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number), singleLine = true)
    AppButton({ number?.let(repository::setTarget); saved = true }, enabled = number != null) { Text(stringResource(R.string.save)) }
    if (saved) Text(stringResource(R.string.saved), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    Text(stringResource(R.string.flow_daily_counts, formatNumber(repository.todayAnswered), formatNumber(repository.state.target),
        formatNumber((repository.state.target - repository.todayAnswered).coerceAtLeast(0))))
    if (repository.todayAnswered >= repository.state.target) Text(stringResource(R.string.flow_daily_optional))
    AppButton(practice) { Text(stringResource(R.string.study_now_start)) }
}
