@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.masterexams

import androidx.compose.foundation.layout.size

import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerOutlinedButton

import com.mukhtabir.android.ui.components.AppActionRow

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import com.mukhtabir.android.util.formatLocalDateTime
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.MasterExamService
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.QuestionImage
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.quiz.KeepSessionScreenOn
import com.mukhtabir.android.util.formatNumber

@Composable
fun MasterExamsRoute(userId: Long, service: MasterExamService, images: ImageRepository, draftStore: SessionDraftStore, onManage: (() -> Unit)? = null, initialExamId: Long? = null, onShare: ((Long) -> Unit)? = null, onBack: () -> Unit) {
    val vm: MasterExamsViewModel = viewModel(key="master-exams-$userId",factory=MasterExamsViewModel.Factory(service,userId,draftStore))
    val s by vm.state.collectAsState()
    var readingLocale by androidx.compose.runtime.saveable.rememberSaveable(userId, s.examId, s.status?.sessionId) { mutableStateOf<String?>(null) }
    var requestHandled by remember(initialExamId) { mutableStateOf(initialExamId == null) }
    LaunchedEffect(initialExamId, s.busy, s.examId, s.storageFailed) {
        if (!requestHandled && !s.busy && !s.storageFailed) {
            if (s.examId == initialExamId) requestHandled = true
            else if (s.examId == null) {
                vm.open(requireNotNull(initialExamId)); requestHandled = true
            }
        }
    }
    var confirmation by remember(s.examId) { mutableStateOf<String?>(null) }
    var reportReason by remember(s.examId,s.page?.question?.id) { mutableStateOf("") }
    val listState=rememberLazyListState()
    LaunchedEffect(s.examId,s.page?.question?.id,s.result?.id) { listState.scrollToItem(0) }
    LaunchedEffect(s.result?.id) { if(s.result!=null) confirmation=null }
    RefreshOnResume(vm::refresh)
    KeepSessionScreenOn(s.status?.isComplete==false)
    val leave: () -> Unit = {
        if(!s.busy) {
            if(s.status?.isComplete==false || s.draft!=null) confirmation="leave"
            else if(s.examId!=null) vm.back() else onBack()
        }
    }
    BackHandler(s.examId!=null || s.busy, onBack=leave)

    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),state=listState,verticalArrangement=Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppActionRow() {
                OutlinedButton(leave,enabled=!s.busy) { Text(stringResource(R.string.back)) }
                OutlinedButton(vm::refresh,enabled=!s.busy) { Text(stringResource(R.string.refresh)) }
                if (s.examId == null && onManage != null) OutlinedButton(onManage, enabled = !s.busy) { Text(stringResource(R.string.exam_author_manage)) }
            }
        }
        item { AppPageHeading(s.result?.examName?.takeIf { it.isNotBlank() } ?: s.exam?.name ?: stringResource(R.string.master_exams_title)) }
        s.exam?.id?.let {id -> onShare?.let {share -> item {
            OutlinedButton({share(id)},enabled=!s.busy) {Text(stringResource(R.string.link_share_exam))}
        } } }
        if(s.busy) item { AppLoadingIndicator() }
        s.error?.let { item { ErrorState(throwable=it,onRetry=if(s.busy) null else vm::refresh) } }
        s.message?.let { item { Text(stringResource(it)) } }
        if (!requestHandled && s.examId != null && s.examId != initialExamId) item {
            Text(stringResource(R.string.notice_existing_exam))
        }
        if(s.storageFailed) item {
            Text(stringResource(R.string.master_recovery_storage),color=MaterialTheme.colorScheme.error,
                modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
            OutlinedButton(vm::retryStorage,enabled=!s.busy) { Text(stringResource(R.string.retry)) }
            if(s.draft==null) AppDangerOutlinedButton({confirmation="discard-recovery"},enabled=!s.busy) {
                Text(stringResource(R.string.discard))
            }
        }
        if(s.finishPending && s.status?.isComplete!=true) item {
            Text(stringResource(R.string.master_finish_uncertain),color=MaterialTheme.colorScheme.error,
                modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
        }
        if(s.restoredNavigation) item {
            Text(stringResource(R.string.master_restored_navigation),modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
            TextButton(vm::dismissRestorationNotice) { Text(stringResource(R.string.master_restoration_dismiss)) }
        }
        if(s.examId==null) {
            item {
                if(s.notices.isNotEmpty()) Text(stringResource(R.string.master_notices_count,s.notices.size))
                AppActionRow() {
                    listOf("all","active","scheduled","completed").forEach { filter ->
                        FilterChip(selected=s.statusFilter==filter,onClick={vm.filter(filter)},enabled=!s.busy,
                            label={Text(if(filter=="all") stringResource(R.string.master_list_all) else masterStatus(filter))})
                    }
                }
            }
            s.noticesError?.let { item { ErrorState(throwable=it,title=stringResource(R.string.master_notices_failed),onRetry=if(s.busy) null else vm::refresh) } }
            if(s.exams.none { s.statusFilter=="all" || it.status==s.statusFilter } && !s.busy && s.error==null) item { Text(stringResource(R.string.master_empty)) }
            items(s.exams.filter { s.statusFilter=="all" || it.status==s.statusFilter },key={it.id}) { exam ->
                Card(onClick={vm.open(exam.id)},enabled=!s.busy && !s.storageFailed,modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                        Text(exam.name,style=MaterialTheme.typography.titleMedium)
                        Text(masterStatus(exam.status))
                        if(exam.id in s.notices) Text(stringResource(R.string.master_ack_required),color=MaterialTheme.colorScheme.primary)
                        Text(stringResource(R.string.master_summary,exam.questionCount,exam.durationMinutes))
                        Text(stringResource(R.string.master_window,formatLocalDateTime(exam.opensAt),formatLocalDateTime(exam.closesAt)))
                        if(exam.myAttempt!=null) Text(stringResource(if(exam.myAttempt.isComplete) R.string.master_result else R.string.master_resume))
                    }
                }
            }
        } else {
            s.exam?.let { exam ->
                if(s.status==null && s.result==null) {
                    item { MarkdownText(exam.description) }
                    if(exam.instructions.isNotBlank()) item { MarkdownText(exam.instructions) }
                    item { Text(masterStatus(exam.status)); Text(stringResource(R.string.master_summary,exam.questionCount,exam.durationMinutes)); Text(stringResource(R.string.master_window,formatLocalDateTime(exam.opensAt),formatLocalDateTime(exam.closesAt))) }
                    item { Text(stringResource(R.string.master_time_rule)) }
                    if(!exam.hasAcknowledged) item {
                        Button(vm::acknowledge,enabled=!s.busy && !s.finishPending && !s.storageFailed && s.draft==null && !s.needsSync) { Text(stringResource(R.string.master_acknowledge)) }
                    } else item { Text(stringResource(R.string.master_acknowledged)) }
                    if(MasterExamsViewModel.canStart(exam)) item {
                        Button({confirmation="start"},enabled=!s.busy && !s.storageFailed && !s.needsSync && !s.finishPending && s.draft==null && exam.hasAcknowledged) { Text(stringResource(R.string.master_start)) }
                    }
                }
            }
                if(s.draft!=null) item {
                    if(s.draftConflict) {
                        Text(stringResource(R.string.master_draft_conflict),color=MaterialTheme.colorScheme.error)
                        Text(stringResource(R.string.master_server_answer,vm.serverDraftAnswer()?.toString() ?: stringResource(R.string.none)))
                        Button(vm::rebaseDraft,enabled=!s.busy && !s.storageFailed && !s.needsSync && !s.finishPending && s.status?.sessionId==s.draftSessionId && (s.graceRemainingSeconds ?: 0)>0) {Text(stringResource(R.string.content_keep_draft))}
                    }
                    Text(stringResource(R.string.master_unsaved))
                    s.draftPage?.let { draftPage ->
                        val pending=QuestionDto(id=draftPage.question.id,question=draftPage.question.text,choices=draftPage.question.choices,
                            translations=draftPage.question.translations,answersHidden=true).localizedForDisplay(readingLocale ?: java.util.Locale.getDefault().toLanguageTag())
                        Text(stringResource(R.string.master_pending_question,draftPage.index+1))
                        com.mukhtabir.android.ui.components.QuestionTextCard(pending.question)
                        pending.choices.getOrNull(s.draft!!.answer-1)?.let { MarkdownText(it) }
                    }
                    Button(vm::saveAnswer,enabled=!s.busy && !s.storageFailed && !s.finishPending && !s.needsSync && !s.draftConflict && (s.graceRemainingSeconds ?: 0)>0) { Text(stringResource(R.string.master_save_answer)) }
                    OutlinedButton({confirmation="discard"},enabled=!s.busy) { Text(stringResource(R.string.master_discard_draft)) }
                }
            s.result?.let { result ->
                item { Text(stringResource(R.string.master_result),style=MaterialTheme.typography.titleLarge) }
                item { Text(stringResource(R.string.master_score,result.correctCount,result.totalQuestions,result.answeredCount)) }
                item { Text(stringResource(R.string.master_percentages,result.accuracy,result.weightedScore)) }
                item { Text(stringResource(R.string.master_result_dates,formatLocalDateTime(result.startedAt),result.finishedAt?.let(::formatLocalDateTime).orEmpty())) }
                if(result.isMakeup) item { Text(stringResource(R.string.master_makeup)) }
                if(result.forcedFinish) item { Text(stringResource(R.string.master_forced_finish)) }
            }
            if(s.status?.isComplete==false) {
                item {
                    Text(stringResource(R.string.master_timer,com.mukhtabir.android.util.formatSessionDuration(s.remainingSeconds ?: 0)),style=MaterialTheme.typography.titleLarge)
                    if(s.remainingSeconds==0L && (s.graceRemainingSeconds ?: 0)>0) {
                        Text(stringResource(R.string.master_grace_notice),modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
                        // The notice announces once; the ticking value is not a live region.
                        Text(stringResource(R.string.master_grace_timer,com.mukhtabir.android.util.formatSessionDuration(s.graceRemainingSeconds ?: 0)))
                    }
                    Text(stringResource(R.string.master_answered,s.status!!.answers.size,s.status!!.questionIds.size))
                    Text(stringResource(R.string.master_time_rule))
                }
                if(s.needsSync) item { Text(stringResource(R.string.master_sync_required),color=MaterialTheme.colorScheme.error) }
                s.page?.let { page ->
                    val question=QuestionDto(id=page.question.id,question=page.question.text,choices=page.question.choices,
                        translations=page.question.translations,case=page.question.case,answersHidden=true).localizedForDisplay(readingLocale ?: java.util.Locale.getDefault().toLanguageTag())
                    val canEdit=!s.busy && !s.finishPending && !s.storageFailed && !s.needsSync && !s.draftConflict && (s.graceRemainingSeconds ?: 0)>0 &&
                        (s.draft==null || s.draft?.questionId==page.question.id)
                    val displayDraft=s.draft?.takeIf { !s.draftConflict && it.questionId==page.question.id }
                    val selected=displayDraft?.answer ?: page.savedAnswer
                    item { Text(stringResource(R.string.page_of,page.index+1,page.total),style=MaterialTheme.typography.titleMedium) }
                    item { com.mukhtabir.android.ui.components.QuestionLanguageSelector(page.question.translations,
                        page.question.choices.size, readingLocale, { readingLocale = it },
                        hasCase = page.question.case?.stem?.isNotBlank() == true, caseTranslations = page.question.case?.translations ?: kotlinx.serialization.json.JsonObject(emptyMap())) }
                    item { ClinicalCaseContext(question.case) }
                    item { com.mukhtabir.android.ui.components.QuestionTextCard(question.question) }
                    item { QuestionImage(page.question.imageUrl,images) }
                    items(question.choices.indices.toList(),key={"choice-$it"}) { index ->
                        com.mukhtabir.android.ui.components.AnswerChoice(
                            number=index+1, text=question.choices[index], selected=selected==index+1,
                            enabled=canEdit, neutralSelection=true, onClick={vm.choose(index+1)})
                    }
                    item {
                        androidx.compose.runtime.key(page.question.id) {
                            com.mukhtabir.android.ui.components.AppSection(stringResource(R.string.confidence_prompt),
                                subtitle = stringResource(when (displayDraft?.confidence ?: page.savedConfidence ?: 3) {
                                    1 -> R.string.confidence_guessing
                                    2 -> R.string.confidence_uncertain
                                    else -> R.string.confidence_certain
                                })) {
                                AppActionRow() {
                                    listOf(R.string.master_confidence_low,R.string.master_confidence_medium,R.string.master_confidence_high).forEachIndexed { index,label ->
                                        FilterChip(selected=(displayDraft?.confidence ?: page.savedConfidence ?: 3)==index+1,
                                            onClick={vm.confidence(index+1)},label={Text(stringResource(label))},enabled=canEdit && selected!=null)
                                    }
                                }
                            }
                        }
                    }
                    item {
                        OutlinedButton({confirmation="report"},enabled=!s.busy && !s.storageFailed && !s.needsSync && !s.finishPending && page.question.id !in s.reported) { Text(stringResource(R.string.master_report)) }
                        if(page.question.id in s.reported) Text(stringResource(R.string.master_report_attempted))
                    }
                }
                item {
                    Text(stringResource(R.string.master_navigation_hint))
                    val ids=s.status!!.questionIds
                    val currentIndex=ids.indexOf(s.page?.question?.id)
                    val canNavigate=!s.busy && !s.finishPending && !s.storageFailed && !s.needsSync && s.draft==null && (s.graceRemainingSeconds ?: 0)>0
                    AppActionRow() {
                        OutlinedButton({vm.move(-1)},enabled=canNavigate && currentIndex>0,modifier=Modifier.heightIn(min=48.dp)) {
                            Text(stringResource(R.string.previous))
                        }
                        OutlinedButton({vm.move(1)},enabled=canNavigate && currentIndex>=0 && currentIndex<ids.lastIndex,modifier=Modifier.heightIn(min=48.dp)) {
                            Text(stringResource(R.string.next))
                        }
                    }
                    AppActionRow() {
                        ids.forEachIndexed { index,id ->
                            val label=stringResource(R.string.select_question_number,index+1)
                            val answerState=stringResource(if(id.toString() in s.status!!.answers)
                                R.string.session_question_answered else R.string.session_question_unanswered)
                            FilterChip(selected=id==s.page?.question?.id,onClick={vm.goto(id)},
                                label={Text("${formatNumber(index+1)}${if(id.toString() in s.status!!.answers) " ✓" else ""}")},
                                modifier=Modifier.heightIn(min=48.dp).semantics {
                                    contentDescription=label
                                    stateDescription=answerState
                                },enabled=canNavigate)
                        }
                    }
                }
                item {
                    Button({confirmation="finish"},enabled=!s.busy && !s.storageFailed && !s.needsSync && (!s.finishPending || s.finishSessionId==s.status?.sessionId) &&
                        (s.draft==null || s.graceRemainingSeconds==0L)) { Text(stringResource(R.string.finish)) }
                }
            }
        }
    }
    confirmation?.let { action ->
        AlertDialog(
            onDismissRequest={if(!s.busy) confirmation=null},
            title={Text(stringResource(when(action){"start"->R.string.master_start;"finish"->R.string.finish;"report"->R.string.master_report;"discard"->R.string.master_discard_draft;else->R.string.back}))},
            text={if(action=="report") OutlinedTextField(reportReason,{reportReason=it},label={Text(stringResource(R.string.master_report_reason))},modifier=Modifier.fillMaxWidth())
                else Text(stringResource(when(action){"discard-recovery"->R.string.master_recovery_discard;"start"->R.string.master_start_confirm;"finish"->R.string.master_finish_confirm;"discard"->R.string.master_discard_confirm;else->R.string.master_leave_confirm}))},
            confirmButton={TextButton({
                confirmation=null
                when(action){"discard-recovery"->vm.discardUnreadableRecovery();"start"->vm.start();"finish"->vm.finish();"report"->vm.flag(reportReason);"discard"->vm.discardDraft();else->vm.back()}
            },enabled=!s.busy){Text(stringResource(R.string.confirm))}},
            dismissButton={TextButton({confirmation=null},enabled=!s.busy){Text(stringResource(R.string.cancel))}},
        )
    }
}

@Composable
internal fun masterStatus(status: String): String = stringResource(when(status) {
    "active" -> R.string.master_status_active
    "scheduled" -> R.string.master_status_scheduled
    "completed" -> R.string.master_status_completed
    "cancelled" -> R.string.master_status_cancelled
    "draft" -> R.string.master_status_draft
    "published_to_bank" -> R.string.master_status_archived
    else -> R.string.master_status_unavailable
})
