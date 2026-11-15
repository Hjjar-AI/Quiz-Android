@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.planner.PlannerDateField
import com.mukhtabir.android.ui.profile.ProfileScreen
import com.mukhtabir.android.util.formatCalendarDate
import com.mukhtabir.android.util.formatNumber
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun demoDigits(value: String) = value.map { c ->
    Character.digit(c, 10).takeIf { it >= 0 }?.digitToChar() ?: c
}.joinToString("")

@Composable
internal fun demoCategoryLabel(category: Int) = stringResource(when (category) {
    1 -> R.string.demo_category_circulation
    2 -> R.string.demo_category_systems
    else -> R.string.all
})

@Composable
internal fun demoPoolLabel(pool: DemoPool) = stringResource(when (pool) {
    DemoPool.ALL -> R.string.quiz_pool_all
    DemoPool.BOOKMARKS -> R.string.bookmarks
    DemoPool.MISTAKES -> R.string.nav_mistakes
    DemoPool.FRAGILE -> R.string.nav_fragile
})

@Composable
internal fun DemoPracticeSetup(repository: DemoRepository, mode: TestMode, onMode: (TestMode) -> Unit,
    onStart: (DemoMode, List<Int>) -> Unit, onDownload: () -> Unit) {
    var pool by rememberSaveable { mutableStateOf(DemoPool.ALL) }
    var category by rememberSaveable { mutableStateOf(0) }
    var tag by rememberSaveable { mutableStateOf(0) }
    var count by rememberSaveable { mutableStateOf("3") }
    val ids = repository.poolIds(pool).filter { id ->
        (category == 0 || repository.category(id) == category) && (tag == 0 || tag in repository.question(id).tags)
    }
    val number = count.toIntOrNull()?.takeIf { it in 1..ids.size }
    val enabled = repository.state.connected && repository.state.session.page != DemoPage.SESSION
    AppPageHeading(stringResource(R.string.home_study_modes), stringResource(R.string.demo_setup_hint))
    AppSection(stringResource(R.string.quiz_mode), initiallyExpanded = true) {
        AppActionRow {
            TestMode.values().forEach { option -> AppFilterChip(mode == option, { onMode(option) }, label = {
                Text(stringResource(when (option) { TestMode.STUDY -> R.string.study; TestMode.EXAM -> R.string.exam; TestMode.RECALL -> R.string.recall }))
            }) }
        }
        Text(stringResource(when (mode) { TestMode.STUDY -> R.string.demo_study_hint; TestMode.EXAM -> R.string.demo_exam_hint; TestMode.RECALL -> R.string.demo_recall_hint }))
    }
    AppSection(stringResource(R.string.quiz_pool_title), initiallyExpanded = true) {
        AppActionRow {
            DemoPool.values().forEach { option -> AppFilterChip(pool == option, { pool = option }, label = { Text(demoPoolLabel(option)) }) }
        }
        AppActionRow {
            (0..2).forEach { option -> AppFilterChip(category == option, { category = option }, label = { Text(demoCategoryLabel(option)) }) }
        }
        Text(stringResource(R.string.source_tags))
        AppActionRow {
            listOf(0, R.string.demo_tag_foundations, R.string.demo_tag_case_context).forEach { option ->
                AppFilterChip(tag == option, { tag = option }, label = { Text(stringResource(if (option == 0) R.string.all else option)) })
            }
        }
        Text(stringResource(R.string.flow_material_count, formatNumber(ids.size)))
    }
    AppSection(stringResource(R.string.flow_count), initiallyExpanded = true) {
        AppTextField(count, { count = demoDigits(it) }, Modifier.fillMaxWidth(), singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text(stringResource(R.string.flow_count)) }, isError = number == null,
            supportingText = {
                Text(if (ids.isEmpty()) stringResource(R.string.no_questions_found)
                    else stringResource(R.string.quiz_count_range, formatNumber(ids.size)))
            })
        AppActionRow {
            listOf(2, 4, ids.size).filter { it in 1..ids.size }.distinct().forEach { value ->
                AppFilterChip(number == value, { count = value.toString() }, label = { Text(formatNumber(value)) })
            }
        }
    }
    if (!repository.state.connected) AppStatus(stringResource(R.string.demo_online_required), kind = AppStatusKind.WARNING)
    AppButton({ number?.let { onStart(DemoMode.valueOf(mode.name), ids.take(it)) } }, enabled = enabled && number != null,
        modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.flow_start)) }
    AppOutlinedButton({ number?.let { repository.download(ids.take(it)); onDownload() } }, enabled = enabled && number != null) {
        Text(stringResource(R.string.offline_download))
    }
}

@Composable
internal fun DemoStudyPlanner(repository: DemoRepository, practice: (List<Int>) -> Unit) {
    var target by rememberSaveable { mutableStateOf(repository.state.target.toString()) }
    var categories by rememberSaveable { mutableStateOf(repository.state.planCategories.toList()) }
    var start by rememberSaveable { mutableStateOf(repository.state.planStart) }
    var end by rememberSaveable { mutableStateOf(repository.state.planEnd) }
    var saved by rememberSaveable { mutableStateOf(false) }
    val number = target.toIntOrNull()?.takeIf { it in 1..1000 }
    val validDates = runCatching {
        val first = LocalDate.parse(start)
        end.isBlank() || LocalDate.parse(end) >= first
    }.getOrDefault(false)
    val enabled = repository.state.connected
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(java.util.Locale.getDefault())
    AppPageHeading(stringResource(R.string.nav_planner), stringResource(R.string.demo_planner_hint))
    Text(stringResource(when {
        LocalDate.now().toString() < repository.state.planStart -> R.string.planner_scheduled
        !repository.planActive -> R.string.planner_ended
        else -> R.string.planner_active
    }))
    AppTextField(target, { target = demoDigits(it); saved = false }, Modifier.fillMaxWidth(), enabled = enabled,
        label = { Text(stringResource(R.string.daily_target)) }, isError = number == null,
        supportingText = { if (number == null) Text(stringResource(R.string.planner_invalid_target)) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
    AppSection(stringResource(R.string.source_categories), initiallyExpanded = true) {
        if (categories.isEmpty()) Text(stringResource(R.string.planner_all_categories))
        (1..2).forEach { id -> AppCheckboxRow(demoCategoryLabel(id), id in categories, { checked ->
            categories = if (checked) categories + id else categories - id; saved = false
        }, enabled = enabled) }
    }
    PlannerDateField(R.string.start_date, start, LocalDate.now().toString(), enabled, formatter) { start = it; saved = false }
    PlannerDateField(R.string.end_date, end, start, enabled, formatter) { end = it; saved = false }
    if (end.isNotBlank()) AppTextButton({ end = ""; saved = false }, enabled = enabled) { Text(stringResource(R.string.planner_clear_end)) }
    if (!validDates) AppStatus(stringResource(R.string.planner_invalid_date_order))
    AppButton({ number?.let { saved = repository.savePlan(it, categories.toSet(), start, end) } }, enabled = enabled && number != null && validDates) {
        Text(stringResource(R.string.save))
    }
    if (saved) AppStatus(stringResource(R.string.saved), kind = AppStatusKind.SUCCESS)
    if (!enabled) AppStatus(stringResource(R.string.demo_online_required), kind = AppStatusKind.WARNING)
    Text(stringResource(R.string.flow_daily_counts, formatNumber(repository.planTodayAnswered), formatNumber(repository.state.target),
        formatNumber((repository.state.target - repository.planTodayAnswered).coerceAtLeast(0))))
    AppButton({ practice(repository.planIds.take(repository.state.target.coerceAtMost(8))) }, enabled = enabled && repository.planActive) {
        Text(stringResource(R.string.study_now_start))
    }
}

@Composable
internal fun DemoDownloads(repository: DemoRepository, onStart: () -> Unit) {
    val active = repository.state.session.page == DemoPage.SESSION
    AppPageHeading(stringResource(R.string.offline_title), stringResource(R.string.demo_downloads_hint))
    AppCard(Modifier.fillMaxWidth()) { AppCardBody {
        Text(stringResource(R.string.demo_collection), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.flow_material_count, formatNumber(repository.state.downloadedIds.size)))
        Text(stringResource(R.string.offline_local))
        AppButton(onStart, enabled = !active && repository.state.downloadedIds.isNotEmpty() && repository.pendingRecords.size < 10) {
            Text(stringResource(R.string.study))
        }
    } }
    if (repository.pendingRecords.isEmpty()) Text(stringResource(R.string.demo_no_pending))
    if (repository.pendingRecords.size >= 10) AppStatus(stringResource(R.string.demo_pending_limit), kind = AppStatusKind.WARNING)
    repository.pendingRecords.forEach { record -> AppCard(Modifier.fillMaxWidth()) { AppCardBody {
        Text(formatCalendarDate(record.date))
        LearningResultsSummary(repository.correct(record.session), record.session.total, record.session.answered)
        Text(stringResource(R.string.demo_pending_sync))
    } } }
    if (repository.pendingRecords.isNotEmpty()) AppButton(repository::sync, enabled = repository.state.connected) {
        Text(stringResource(R.string.offline_sync))
    }
    if (!repository.state.connected) AppStatus(stringResource(R.string.demo_offline_downloads), kind = AppStatusKind.INFO)
}

@Composable
internal fun DemoConnections(repository: DemoRepository) {
    AppPageHeading(stringResource(R.string.demo_connections), stringResource(R.string.demo_connections_hint))
    AppCard(Modifier.fillMaxWidth()) { AppCardBody {
        Text(stringResource(R.string.demo_server), style = MaterialTheme.typography.titleMedium)
        Text("https://academy.example.invalid")
        AppStatus(stringResource(if (repository.state.connected) R.string.demo_connected else R.string.demo_disconnected),
            kind = if (repository.state.connected) AppStatusKind.SUCCESS else AppStatusKind.WARNING)
        AppSwitchRow(stringResource(R.string.demo_connection_switch), repository.state.connected, repository::setConnected,
            enabled = repository.state.session.page != DemoPage.SESSION || repository.state.session.downloaded)
        if (repository.state.session.page == DemoPage.SESSION && !repository.state.session.downloaded) Text(stringResource(R.string.demo_connection_locked))
        AppOutlinedButton(repository::refresh) { Text(stringResource(if (repository.state.readFailed) R.string.retry else R.string.refresh)) }
        if (repository.state.readFailed) AppStatus(stringResource(R.string.demo_read_failed))
    } }
    Text(stringResource(R.string.demo_scope))
}

@Composable
internal fun DemoProfile(repository: DemoRepository, onExit: () -> Unit) {
    var information by rememberSaveable { mutableStateOf(false) }
    val user = UserDto(id = 1, username = "sample_learner", fullName = stringResource(R.string.demo_learner),
        role = "member", capabilities = listOf("tests.start", "tests.view_own_history"),
        createdAt = LocalDate.now().minusDays(45).atStartOfDay(java.time.ZoneId.systemDefault()).toOffsetDateTime().toString(), currentStreak = repository.currentStreak,
        longestStreak = maxOf(2, repository.currentStreak))
    ProfileScreen(user, onChangePassword = { information = true }, onAbout = { information = true }, onSignOut = onExit,
        onAdminUsers = {}, onAdminSettings = {}, onAdminPermissions = {}, onRefresh = repository::refresh,
        refreshing = false, refreshError = if (repository.state.readFailed) IllegalStateException() else null)
    if (information) AppConfirmationDialog(onDismissRequest = { information = false },
        title = { Text(stringResource(R.string.demo_badge)) }, text = { Text(stringResource(R.string.demo_account_limits)) },
        confirmButton = { AppTextButton({ information = false }) { Text(stringResource(R.string.done)) } })
}
