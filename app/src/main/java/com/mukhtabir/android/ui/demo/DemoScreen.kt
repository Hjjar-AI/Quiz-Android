package com.mukhtabir.android.ui.demo

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.theme.AppTheme
import kotlinx.coroutines.delay

private data class DemoQuestion(val stem: Int, val choices: List<Int>, val correct: Int, val explanation: Int)

private val demoQuestions = listOf(
    DemoQuestion(R.string.demo_q1, listOf(R.string.demo_q1_c1, R.string.demo_q1_c2, R.string.demo_q1_c3, R.string.demo_q1_c4), 0, R.string.demo_q1_explanation),
    DemoQuestion(R.string.demo_q2, listOf(R.string.demo_q2_c1, R.string.demo_q2_c2, R.string.demo_q2_c3, R.string.demo_q2_c4), 1, R.string.demo_q2_explanation),
    DemoQuestion(R.string.demo_q3, listOf(R.string.demo_q3_c1, R.string.demo_q3_c2, R.string.demo_q3_c3, R.string.demo_q3_c4), 2, R.string.demo_q3_explanation),
    DemoQuestion(R.string.demo_q4, listOf(R.string.demo_q4_c1, R.string.demo_q4_c2, R.string.demo_q4_c3, R.string.demo_q4_c4), 3, R.string.demo_q4_explanation),
    DemoQuestion(R.string.demo_q5, listOf(R.string.demo_q5_c1, R.string.demo_q5_c2, R.string.demo_q5_c3, R.string.demo_q5_c4), 0, R.string.demo_q5_explanation),
    DemoQuestion(R.string.demo_q6, listOf(R.string.demo_q6_c1, R.string.demo_q6_c2, R.string.demo_q6_c3, R.string.demo_q6_c4), 1, R.string.demo_q6_explanation),
    DemoQuestion(R.string.demo_q7, listOf(R.string.demo_q7_c1, R.string.demo_q7_c2, R.string.demo_q7_c3, R.string.demo_q7_c4), 2, R.string.demo_q7_explanation),
    DemoQuestion(R.string.demo_q8, listOf(R.string.demo_q8_c1, R.string.demo_q8_c2, R.string.demo_q8_c3, R.string.demo_q8_c4), 3, R.string.demo_q8_explanation),
)

/** Public sample content only. No services, account state, storage or synchronization. */
@Composable
fun DemoInvitation(onOpen: () -> Unit, enabled: Boolean = true) {
    DemoHero {
        Text(stringResource(R.string.demo_title), style = MaterialTheme.typography.titleLarge)
        Text(stringResource(R.string.demo_intro), style = MaterialTheme.typography.bodyMedium)
        AppButton(onOpen, Modifier.fillMaxWidth(), enabled = enabled) { Text(stringResource(R.string.demo_open)) }
    }
}

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
fun DemoScreen(onExit: () -> Unit) {
    var session by rememberSaveable { mutableStateOf(DemoSession()) }
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
    fun start(mode: DemoMode, count: Int = demoQuestions.size) {
        session = DemoSession.start(mode, count); filter = ReviewFilter.ALL; dialog = null
    }
    fun back() {
        when (session.page) {
            DemoPage.HOME -> onExit()
            DemoPage.SESSION -> dialog = DemoDialog.DISCARD
            DemoPage.REVIEW -> session = session.copy(page = DemoPage.RESULTS)
            DemoPage.RESULTS -> session = DemoSession()
        }
    }
    BackHandler { back() }
    LaunchedEffect(index, session.page, filter) { focus.clearFocus(); scroll.scrollTo(0) }
    LaunchedEffect(session.mode, session.page, session.deadline) {
        if (active && session.mode == DemoMode.EXAM) while (true) {
            if (session.deadlineReached()) { session = session.finish(true); dialog = null; break }
            remaining = ((session.remainingMillis() + 999) / 1000).coerceIn(0, 480).toInt()
            delay(250)
        }
    }
    if (showMap && (active || reviewing)) AppConfirmationDialog(onDismissRequest = { showMap = false },
        title = { Text(stringResource(R.string.session_question_navigation)) },
        text = { LearningQuestionMap(index, session.total, session.answers.indices.filter { session.answers[it] >= 0 }.toSet(),
            session.flagged, reviewing || session.mode == DemoMode.EXAM || session.answers[index] >= 0) { target ->
                filter = ReviewFilter.ALL; session = session.copy(index = target); showMap = false
            } }, confirmButton = { AppTextButton({ showMap = false }) { Text(stringResource(R.string.done)) } })
    dialog?.let { current -> AppConfirmationDialog(
        onDismissRequest = { dialog = null },
        title = { Text(stringResource(if (current == DemoDialog.FINISH) R.string.finish_session_title else R.string.demo_leave_title)) },
        text = { Column {
            Text(stringResource(if (current == DemoDialog.FINISH) R.string.demo_finish_message else R.string.demo_leave_message))
            if (current == DemoDialog.FINISH) LearningQuestionMap(index, session.total,
                session.answers.indices.filter { session.answers[it] >= 0 }.toSet(), session.flagged, true) { target ->
                    session = session.copy(index = target); dialog = null
                }
        } },
        confirmButton = {
            if (current == DemoDialog.DISCARD) AppDangerTextButton({ session = DemoSession(); dialog = null }) {
                Text(stringResource(R.string.ui_demo_home))
            } else AppTextButton({ session = session.finish(session.deadlineReached()); dialog = null }) { Text(stringResource(R.string.finish_session_action)) }
        },
        dismissButton = { AppTextButton({ dialog = null }) { Text(stringResource(R.string.cancel)) } },
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
                    AppIconButton({ session = session.copy(flagged = if (index in session.flagged) session.flagged - index else session.flagged + index) }) {
                        Icon(Icons.Filled.Flag, stringResource(if (index in session.flagged) R.string.flow_unflag else R.string.flow_flag))
                    }
                    AppTextButton({ dialog = DemoDialog.FINISH }) { Text(stringResource(R.string.finish)) }
                } })
        } },
        bottomBar = {
            if (active || (reviewing && visible.isNotEmpty())) {
                val position = visible.indexOf(index)
                val last = if (reviewing) position == visible.lastIndex else index == session.total - 1
                LearningSessionNavigation(onQuestions = { showMap = true },
                    onPrevious = { session = session.copy(index = if (reviewing) visible[position - 1] else index - 1) },
                    previousEnabled = if (reviewing) position > 0 else index > 0,
                    nextEnabled = reviewing || session.mode == DemoMode.EXAM || session.answers[index] >= 0,
                    nextLabel = stringResource(if (!last) R.string.next else if (reviewing) R.string.results else R.string.finish),
                    onNext = {
                        if (last) { if (reviewing) session = session.copy(page = DemoPage.RESULTS) else dialog = DemoDialog.FINISH }
                        else session = session.copy(index = if (reviewing) visible[position + 1] else index + 1)
                    })
            }
        },
    ) { insets ->
        Column(Modifier.fillMaxSize().padding(insets).consumeWindowInsets(insets).imePadding()
            .verticalScroll(scroll).padding(AppLayoutTokens.pageInsets),
            verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
            when (session.page) {
                DemoPage.HOME -> {
                    DemoHero {
                        Text(stringResource(R.string.demo_title), style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.semantics { heading() })
                        Text(stringResource(R.string.demo_intro))
                        Text(stringResource(R.string.ui_quick_hint))
                        AppButton({ start(DemoMode.STUDY, 3) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_quick_study)) }
                    }
                    AppPageHeading(stringResource(R.string.demo_collection), stringResource(R.string.demo_collection_hint))
                    listOf(Triple(DemoMode.STUDY, R.string.ui_full_study, R.string.demo_study_hint),
                        Triple(DemoMode.EXAM, R.string.exam, R.string.demo_exam_hint),
                        Triple(DemoMode.RECALL, R.string.recall, R.string.demo_recall_hint)).forEach { (mode, title, hint) ->
                        AppCard(onClick = { start(mode) }, modifier = Modifier.fillMaxWidth()) {
                            AppCardBody {
                                Icon(when (mode) { DemoMode.EXAM -> Icons.Filled.Quiz; DemoMode.RECALL -> Icons.Filled.Lightbulb; else -> Icons.Filled.School }, null,
                                    tint = MaterialTheme.colorScheme.primary)
                                Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
                                Text(stringResource(hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Text(stringResource(R.string.demo_local_notice), style = MaterialTheme.typography.bodySmall)
                    AppOutlinedButton(onExit, Modifier.fillMaxWidth()) { Text(stringResource(R.string.demo_exit)) }
                }
                DemoPage.RESULTS -> {
                    DemoHero { Text(stringResource(R.string.demo_result_title), style = MaterialTheme.typography.headlineSmall) }
                    LearningResultsSummary(session.answers.indices.count { session.answers[it] == demoQuestions[it].correct }, session.total, session.answered)
                    if (session.expired) Text(stringResource(R.string.demo_expired), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                    AppButton({ filter = ReviewFilter.ALL; session = session.copy(index = 0, page = DemoPage.REVIEW) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.demo_review)) }
                    AppOutlinedButton({ start(session.mode, session.total) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.demo_retry)) }
                    AppOutlinedButton({ session = DemoSession() }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.ui_demo_home)) }
                    Text(stringResource(R.string.demo_local_notice), style = MaterialTheme.typography.bodySmall)
                }
                DemoPage.SESSION, DemoPage.REVIEW -> {
                    if (reviewing) ResultReviewFilters(filter, { filter = it }, visible.size, session.total)
                    if (!reviewing || displayIndex != null) {
                        val question = demoQuestions[index]
                        val answer = session.answers[index]
                        val feedback = session.finished || (session.mode != DemoMode.EXAM && answer >= 0)
                        QuestionTextCard(stringResource(question.stem))
                        if (session.mode == DemoMode.RECALL) {
                            if (index !in session.revealed && active) {
                                Text(stringResource(R.string.demo_recall_prompt))
                                OutlinedTextField(session.recalled[index], { value -> session = session.copy(recalled = session.recalled.mapIndexed { i, old -> if (i == index) value else old }) },
                                    Modifier.fillMaxWidth(), label = { Text(stringResource(R.string.demo_recalled)) })
                                AppButton({ session = session.copy(revealed = session.revealed + index); focus.clearFocus() }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.demo_reveal)) }
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
                                    enabled = active && !feedback, onClick = if (active) ({ session = session.copy(index = index).answer(choice) }) else null,
                                    answerCorrect = status, neutralSelection = session.mode == DemoMode.EXAM && active, interactiveText = reviewing, reviewMode = reviewing)
                            }
                        }
                        if (feedback) ExplanationCard(stringResource(question.explanation), title = stringResource(R.string.explanation))
                        if (reviewing && answer < 0) Text(stringResource(R.string.unanswered))
                        if (active && session.mode != DemoMode.EXAM && answer < 0) Text(stringResource(R.string.demo_answer_hint), style = MaterialTheme.typography.bodySmall)
                    }
                    if (reviewing) AppTextButton({ session = session.copy(page = DemoPage.RESULTS) }) { Text(stringResource(R.string.results)) }
                }
            }
        }
    }
}
