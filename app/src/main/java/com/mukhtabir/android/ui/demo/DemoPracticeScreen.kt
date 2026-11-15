package com.mukhtabir.android.ui.demo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.theme.AppTheme
import kotlinx.coroutines.delay

@Composable
private fun DemoHero(content: @Composable ColumnScope.() -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.large, color = scheme.primaryContainer, contentColor = scheme.onPrimaryContainer) {
        Column(Modifier.fillMaxWidth().background(Brush.linearGradient(listOf(scheme.primaryContainer, lerp(scheme.primaryContainer, scheme.surface, .25f))))
            .padding(AppLayoutTokens.authInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
            Text(stringResource(R.string.demo_badge), style = MaterialTheme.typography.labelMedium)
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun DemoPracticeScreen(repository: DemoRepository, onClose: () -> Unit) {
    val session = repository.state.session
    val demoQuestions = session.questionIds.map { repository.question(it) }
    fun change(value: DemoSession) { repository.updateSession(value) }
    var dialog by rememberSaveable { mutableStateOf<DemoDialog?>(null) }
    var showMap by rememberSaveable { mutableStateOf(false) }
    var filter by rememberSaveable { mutableStateOf(ReviewFilter.ALL) }
    var remaining by remember { mutableStateOf(480) }
    val scroll = rememberScrollState()
    val focus = androidx.compose.ui.platform.LocalFocusManager.current
    val reviewing = session.page == DemoPage.REVIEW
    val active = session.page == DemoPage.SESSION
    val visible = session.answers.indices.filter { filter.includes(session.answers[it] >= 0, session.answers[it] == demoQuestions[it].correct) }
    val displayIndex = if (reviewing) session.index.takeIf { it in visible } ?: visible.firstOrNull() else session.index
    val index = displayIndex ?: 0
    val canFinish = session.mode == DemoMode.EXAM || session.answers.getOrNull(index) == -1 || session.canAdvance(index)
    fun start(mode: DemoMode, count: Int = demoQuestions.size) {
        repository.start(mode, session.questionIds.take(count), downloaded = session.downloaded); filter = ReviewFilter.ALL; dialog = null
    }
    fun back() {
        when (session.page) {
            DemoPage.HOME -> onClose()
            DemoPage.SESSION -> dialog = DemoDialog.DISCARD
            DemoPage.REVIEW -> change(session.copy(page = DemoPage.RESULTS))
            DemoPage.RESULTS -> onClose()
        }
    }
    BackHandler { back() }
    LaunchedEffect(index, session.page, filter) { focus.clearFocus(); scroll.scrollTo(0) }
    LaunchedEffect(session.mode, session.page, session.deadline) {
        if (active && session.mode == DemoMode.EXAM) while (true) {
            val current = repository.state.session
            if (current.deadlineReached()) { change(current.finish(true)); dialog = null; break }
            remaining = ((current.remainingMillis() + 999) / 1000).coerceIn(0, 480).toInt()
            delay(250)
        }
    }
    if (showMap && (active || reviewing)) AppConfirmationDialog(onDismissRequest = { showMap = false },
        title = { Text(stringResource(R.string.session_question_navigation)) },
        text = { LearningQuestionMap(index, session.total, session.answers.indices.filter { session.answers[it] >= 0 }.toSet(),
            session.flagged, reviewing || session.canAdvance(index)) { target ->
                filter = ReviewFilter.ALL; change(session.copy(index = target)); showMap = false
            } }, confirmButton = { AppTextButton({ showMap = false }) { Text(stringResource(R.string.done)) } })
    dialog?.let { current -> AppConfirmationDialog(
        onDismissRequest = { dialog = null },
        title = { Text(stringResource(if (current == DemoDialog.FINISH) R.string.finish_session_title else R.string.demo_leave_title)) },
        text = { Column {
            Text(stringResource(if (current == DemoDialog.FINISH) R.string.demo_finish_message else R.string.demo_leave_message))
            if (current == DemoDialog.FINISH) LearningQuestionMap(index, session.total,
                session.answers.indices.filter { session.answers[it] >= 0 }.toSet(), session.flagged, true) { target ->
                    change(session.copy(index = target)); dialog = null
                }
        } },
        confirmButton = {
            if (current == DemoDialog.DISCARD) AppDangerTextButton({ repository.discard(); dialog = null; onClose() }) {
                Text(stringResource(R.string.demo_discard))
            } else AppTextButton({ change(session.finish(session.deadlineReached())); dialog = null }, enabled = canFinish) { Text(stringResource(R.string.finish_session_action)) }
        },
        dismissButton = { Column {
            if (current == DemoDialog.DISCARD) AppTextButton({ dialog = null; onClose() }) { Text(stringResource(R.string.demo_pause)) }
            AppTextButton({ dialog = null }) { Text(stringResource(R.string.cancel)) }
        } },
    ) }
    Scaffold(
        topBar = { Column {
            AppBackBar(stringResource(R.string.demo_badge) + if (active || reviewing) " · " + stringResource(when (session.mode) {
                DemoMode.STUDY -> R.string.study; DemoMode.EXAM -> R.string.exam; DemoMode.RECALL -> R.string.recall
            }) else "", enabled = true, onBack = { back() })
            if (active || (reviewing && visible.isNotEmpty())) LearningSessionHeader(index, session.total, session.answered,
                seconds = if (active && session.mode == DemoMode.EXAM) remaining else null,
                critical = active && session.mode == DemoMode.EXAM && remaining <= 48,
                tools = { if (active) {
                    AppIconButton({ change(session.copy(flagged = if (index in session.flagged) session.flagged - index else session.flagged + index)) }) {
                        Icon(Icons.Filled.Flag, stringResource(if (index in session.flagged) R.string.flow_unflag else R.string.flow_flag))
                    }
                    AppTextButton({ dialog = DemoDialog.FINISH }, enabled = canFinish) { Text(stringResource(R.string.finish)) }
                } })
        } },
        bottomBar = {
            if (active || (reviewing && visible.isNotEmpty())) {
                val position = visible.indexOf(index)
                val last = if (reviewing) position == visible.lastIndex else index == session.total - 1
                LearningSessionNavigation(onQuestions = { showMap = true },
                    onPrevious = { change(session.copy(index = if (reviewing) visible[position - 1] else index - 1)) },
                    previousEnabled = if (reviewing) position > 0 else index > 0 && (session.answers[index] < 0 || session.canAdvance(index)),
                    nextEnabled = reviewing || session.canAdvance(index),
                    nextLabel = stringResource(if (!last) R.string.next else if (reviewing) R.string.results else R.string.finish),
                    onNext = {
                        if (last) { if (reviewing) change(session.copy(page = DemoPage.RESULTS)) else dialog = DemoDialog.FINISH }
                        else change(session.copy(index = if (reviewing) visible[position + 1] else index + 1))
                    })
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).imePadding()
            .verticalScroll(scroll).padding(AppLayoutTokens.pageInsets),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
            when (session.page) {
                DemoPage.HOME -> Unit
                DemoPage.RESULTS -> {
                    DemoHero { Text(stringResource(R.string.demo_result_title), style = MaterialTheme.typography.headlineSmall) }
                    LearningResultsSummary(session.answers.indices.count { session.answers[it] == demoQuestions[it].correct }, session.total, session.answered)
                    if (session.expired) Text(stringResource(R.string.demo_expired), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    AppButton({ filter = ReviewFilter.ALL; change(session.copy(index = 0, page = DemoPage.REVIEW)) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.demo_review)) }
                    AppOutlinedButton({ start(session.mode, session.total) }, Modifier.fillMaxWidth(),
                        enabled = if (session.downloaded) repository.pendingRecords.size < 10 else repository.state.connected) { Text(stringResource(R.string.demo_retry)) }
                    if (session.downloaded) {
                        val pending = repository.pendingRecords.any { it.session.id == session.id }
                        AppStatus(stringResource(if (pending) R.string.demo_pending_sync else R.string.demo_synced), kind = if (pending) AppStatusKind.INFO else AppStatusKind.SUCCESS)
                        if (pending) AppButton(repository::sync, enabled = repository.state.connected) { Text(stringResource(R.string.offline_sync)) }
                    }
                    AppOutlinedButton({ onClose() }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.demo_return)) }
                    Text(stringResource(R.string.demo_local_notice), style = MaterialTheme.typography.bodySmall)
                }
                DemoPage.SESSION, DemoPage.REVIEW -> {
                    if (reviewing) ResultReviewFilters(filter, { filter = it }, visible.size, session.total)
                    if (!reviewing || displayIndex != null) {
                        val question = demoQuestions[index]
                        val answer = session.answers[index]
                        val feedback = session.finished || (session.mode != DemoMode.EXAM && answer >= 0)
                        question.caseStem?.let { QuestionTextCard(stringResource(it)) }
                        QuestionTextCard(stringResource(question.stem))
                        AppTextButton({ repository.toggleBookmark(session.questionIds[index]) }) {
                            Text(stringResource(if (session.questionIds[index] in repository.state.bookmarks) R.string.remove_bookmark else R.string.demo_add_bookmark))
                        }
                        if (session.mode == DemoMode.RECALL) {
                            if (index !in session.revealed && active) {
                                Text(stringResource(R.string.demo_recall_prompt))
                                AppTextField(session.recalled[index], { value -> change(session.copy(recalled = session.recalled.mapIndexed { i, old -> if (i == index) value.take(500) else old })) },
                                    Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.demo_recalled)) },
                                    supportingText = { Text(stringResource(R.string.demo_recalled_limit)) })
                                AppButton({ change(session.copy(revealed = session.revealed + index)); focus.clearFocus() }, Modifier.fillMaxWidth(),
                                    enabled = session.recalled[index].isNotBlank()) { Text(stringResource(R.string.demo_reveal)) }
                            } else if (session.recalled[index].isNotBlank()) {
                                Text(stringResource(R.string.demo_recalled), style = MaterialTheme.typography.labelLarge)
                                Text(session.recalled[index])
                            }
                        }
                        if (session.mode != DemoMode.RECALL || index in session.revealed || reviewing) Column(Modifier.selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.content)) {
                            question.choices.forEachIndexed { choice, label ->
                                val status = if (!feedback) null else when (choice) { question.correct -> true; answer -> false; else -> null }
                                AnswerChoice(number = choice + 1, text = stringResource(label), selected = answer == choice,
                                    enabled = active && !feedback, onClick = if (active) ({ change(session.copy(index = index).answer(choice)) }) else null,
                                    answerCorrect = status, neutralSelection = session.mode == DemoMode.EXAM && active, interactiveText = reviewing, reviewMode = reviewing)
                            }
                        }
                        if (feedback) ExplanationCard(stringResource(question.explanation), title = stringResource(R.string.explanation))
                        if (active && session.mode != DemoMode.EXAM && answer >= 0) {
                            key(index) { ConfidenceSelector(session.confidence[index], { change(session.withConfidence(it)) }, enabled = true, required = true) }
                        }
                        if (reviewing) session.confidence[index]?.let { Text(confidenceLabel(it)) }
                        if (reviewing && answer < 0) Text(stringResource(R.string.unanswered))
                        if (active && session.mode != DemoMode.EXAM && answer < 0) Text(stringResource(R.string.demo_answer_hint), style = MaterialTheme.typography.bodySmall)
                    }
                    if (reviewing) AppTextButton({ change(session.copy(page = DemoPage.RESULTS)) }) { Text(stringResource(R.string.results)) }
                }
            }
        }
    }
}
