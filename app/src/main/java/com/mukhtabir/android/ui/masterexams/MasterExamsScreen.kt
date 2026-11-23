@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.masterexams

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.data.remote.ImageRepository
import com.mukhtabir.android.data.remote.MasterExamService
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.AnswerChoice
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppConfirmationButton
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerOutlinedButton
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppSectionHeading
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppStatusKind
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.ConfidenceSelector
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.LearningQuestionChips
import com.mukhtabir.android.ui.components.LearningResultsSummary
import com.mukhtabir.android.ui.components.LearningSaveState
import com.mukhtabir.android.ui.components.LearningSaveStatus
import com.mukhtabir.android.ui.components.LearningSessionHeader
import com.mukhtabir.android.ui.components.LearningSessionNavigation
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.QuestionImage
import com.mukhtabir.android.ui.components.QuestionLanguageSelector
import com.mukhtabir.android.ui.components.QuestionTextCard
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.quiz.KeepSessionScreenOn
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatLocalDateTime

@Composable
fun MasterExamsRoute(userId: Long, service: MasterExamService, images: ImageRepository, draftStore: SessionDraftStore, onManage: (() -> Unit)? = null, initialExamId: Long? = null, onShare: ((Long) -> Unit)? = null, onBack: () -> Unit) {
    val vm: MasterExamsViewModel = viewModel(key="master-exams-$userId",factory=MasterExamsViewModel.Factory(service,userId,draftStore))
    val s by vm.state.collectAsLifecycleState()
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

    val status = s.status
    val active = status?.isComplete == false
    val ids = status?.questionIds.orEmpty()
    val currentIndex = ids.indexOf(s.page?.question?.id)
    val answeredIndices = ids.mapIndexedNotNull { index, id ->
        index.takeIf { id.toString() in status?.answers.orEmpty() }
    }.toSet()
    val canNavigate = !s.busy && !s.finishPending && !s.storageFailed && !s.needsSync && s.draft == null &&
        (s.graceRemainingSeconds ?: 0) > 0
    var showMap by remember(status?.sessionId) { mutableStateOf(false) }
    if (showMap && active) AppConfirmationDialog(onDismissRequest = { showMap = false },
        title = { Text(stringResource(R.string.session_question_navigation)) },
        text = {
            Column(Modifier.heightIn(max = AppLayoutTokens.menuMaxHeight).verticalScroll(rememberScrollState())) {
                Text(stringResource(R.string.master_navigation_hint))
                LearningQuestionChips(currentIndex, ids.size,
                    answeredIndices, emptySet(), canNavigate, onChoose = { index ->
                        vm.goto(ids[index]); showMap = false
                    })
            }
        }, confirmButton = { AppTextButton({ showMap = false }) { Text(stringResource(R.string.done)) } })
    val sessionHeader: @Composable () -> Unit = {
        val grace = s.remainingSeconds == 0L && (s.graceRemainingSeconds ?: 0) > 0
        val seconds = (if (grace) s.graceRemainingSeconds else s.remainingSeconds) ?: 0L
        LearningSessionHeader(currentIndex, ids.size, answeredIndices.size,
            seconds = seconds.coerceIn(0L, Int.MAX_VALUE.toLong()).toInt(), overtime = grace)
    }
    val sessionNavigation: @Composable () -> Unit = {
        LearningSessionNavigation(
            onPrevious = { vm.move(-1) }, onNext = { vm.move(1) },
            previousEnabled = canNavigate && currentIndex > 0,
            nextEnabled = canNavigate && currentIndex >= 0 && currentIndex < ids.lastIndex,
            nextLabel = stringResource(R.string.next), onQuestions = { showMap = true })
    }
    val fontScale = androidx.compose.ui.platform.LocalDensity.current.fontScale.coerceAtLeast(1f)
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val pinned = maxHeight >= AppLayoutTokens.sessionPinnedMinHeight * fontScale
    Column(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
    if (active && pinned) sessionHeader()
    Box(Modifier.weight(1f)) {
    LazyColumn(Modifier.fillMaxSize(),state=listState,verticalArrangement=Arrangement.spacedBy(AppTheme.spacing.section)) {
        if (active && !pinned) item { sessionHeader() }
        item {
            AppActionRow() {
                AppOutlinedButton(leave,enabled=!s.busy) { Text(stringResource(R.string.back)) }
                AppOutlinedButton(vm::refresh,enabled=!s.busy) { Text(stringResource(R.string.refresh)) }
                if (s.examId == null && onManage != null) AppOutlinedButton(onManage, enabled = !s.busy) { Text(stringResource(R.string.exam_author_manage)) }
            }
        }
        item { AppPageHeading(s.result?.examName?.takeIf { it.isNotBlank() } ?: s.exam?.name ?: stringResource(R.string.master_exams_title)) }
        s.exam?.id?.let {id -> onShare?.let {share -> item {
            AppOutlinedButton({share(id)},enabled=!s.busy) {Text(stringResource(R.string.link_share_exam))}
        } } }
        if(s.busy && s.page == null) item { AppLoadingIndicator() }
        s.error?.let { item { ErrorState(throwable=it,onRetry=if(s.busy) null else vm::refresh) } }
        s.message?.let { item { Text(stringResource(it)) } }
        if (!requestHandled && s.examId != null && s.examId != initialExamId) item {
            Text(stringResource(R.string.notice_existing_exam))
        }
        if(s.storageFailed) item {
            AppStatus(stringResource(R.string.master_recovery_storage),
                modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
            AppOutlinedButton(vm::retryStorage,enabled=!s.busy) { Text(stringResource(R.string.retry)) }
            if(s.draft==null) AppDangerOutlinedButton({confirmation="discard-recovery"},enabled=!s.busy) {
                Text(stringResource(R.string.ux_discard_local_recovery))
            }
        }
        if(s.finishPending && s.status?.isComplete!=true) item {
            AppStatus(stringResource(R.string.master_finish_uncertain),
                modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }, kind = AppStatusKind.WARNING)
        }
        if(s.restoredNavigation) item {
            Text(stringResource(R.string.master_restored_navigation),modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
            AppTextButton(vm::dismissRestorationNotice) { Text(stringResource(R.string.master_restoration_dismiss)) }
        }
        if(s.examId==null) {
            item {
                if(s.notices.isNotEmpty()) Text(stringResource(R.string.master_notices_count, com.mukhtabir.android.util.formatNumber(s.notices.size)))
                AppActionRow() {
                    listOf("all","active","scheduled","completed").forEach { filter ->
                        AppFilterChip(selected=s.statusFilter==filter,onClick={vm.filter(filter)},enabled=!s.busy,
                            label={Text(if(filter=="all") stringResource(R.string.master_list_all) else masterStatus(filter))})
                    }
                }
            }
            s.noticesError?.let { item { ErrorState(throwable=it,title=stringResource(R.string.master_notices_failed),onRetry=if(s.busy) null else vm::refresh) } }
            if(s.exams.none { s.statusFilter=="all" || it.status==s.statusFilter } && !s.busy && s.error==null) item { EmptyState(stringResource(R.string.master_empty)) }
            items(s.exams.filter { s.statusFilter=="all" || it.status==s.statusFilter },key={it.id}) { exam ->
                AppCard(onClick={vm.open(exam.id)},enabled=!s.busy && !s.storageFailed,modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                        MasterExamSummary(exam, notices = {
                            if (exam.id in s.notices) Text(stringResource(R.string.master_ack_required), color = MaterialTheme.colorScheme.primary)
                        }, actions = {
                            exam.myAttempt?.let { attempt ->
                                Text(stringResource(if (attempt.isComplete) R.string.master_result else R.string.master_resume))
                            }
                        })
                    }
                }
            }
        } else {
            s.exam?.let { exam ->
                if(s.status==null && s.result==null) {
                    item { MarkdownText(exam.description) }
                    if(exam.instructions.isNotBlank()) item { MarkdownText(exam.instructions) }
                    item { Text(masterStatus(exam.status)); Text(stringResource(R.string.master_summary, com.mukhtabir.android.util.formatNumber(exam.questionCount), com.mukhtabir.android.util.formatNumber(exam.durationMinutes))); Text(stringResource(R.string.master_window,formatLocalDateTime(exam.opensAt),formatLocalDateTime(exam.closesAt))) }
                    item { Text(stringResource(R.string.master_time_rule)) }
                    if(!exam.hasAcknowledged) item {
                        AppButton(vm::acknowledge,enabled=!s.busy && !s.finishPending && !s.storageFailed && s.draft==null && !s.needsSync) { Text(stringResource(R.string.master_acknowledge)) }
                    } else item { Text(stringResource(R.string.master_acknowledged)) }
                    if(MasterExamsViewModel.canStart(exam)) item {
                        AppButton({confirmation="start"},enabled=!s.busy && !s.storageFailed && !s.needsSync && !s.finishPending && s.draft==null && exam.hasAcknowledged) { Text(stringResource(R.string.master_start)) }
                    }
                }
            }
                if(s.draft!=null) item {
                    if(s.draftConflict) {
                        AppStatus(stringResource(R.string.master_draft_conflict), kind = AppStatusKind.WARNING)
                        Text(stringResource(R.string.master_server_answer,vm.serverDraftAnswer()?.toString() ?: stringResource(R.string.none)))
                        AppButton(vm::rebaseDraft,enabled=!s.busy && !s.storageFailed && !s.needsSync && !s.finishPending && s.status?.sessionId==s.draftSessionId && (s.graceRemainingSeconds ?: 0)>0) {Text(stringResource(R.string.content_keep_draft))}
                    }
                    Text(stringResource(R.string.master_unsaved))
                    s.draftPage?.let { draftPage ->
                        val pending=QuestionDto(id=draftPage.question.id,question=draftPage.question.text,choices=draftPage.question.choices,
                            translations=draftPage.question.translations,answersHidden=true).localizedForDisplay(readingLocale ?: java.util.Locale.getDefault().toLanguageTag())
                        Text(stringResource(R.string.master_pending_question, com.mukhtabir.android.util.formatNumber(draftPage.index+1)))
                        QuestionTextCard(pending.question)
                        pending.choices.getOrNull(s.draft!!.answer-1)?.let { MarkdownText(it) }
                    }
                    AppButton(vm::saveAnswer,enabled=!s.busy && !s.storageFailed && !s.finishPending && !s.needsSync && !s.draftConflict && (s.graceRemainingSeconds ?: 0)>0) { Text(stringResource(R.string.master_save_answer)) }
                    AppOutlinedButton({confirmation="discard"},enabled=!s.busy) { Text(stringResource(R.string.master_discard_draft)) }
                }
            s.result?.let { result ->
                item { AppSectionHeading(stringResource(R.string.master_result)) }
                item {
                    LearningSaveStatus(LearningSaveState.SERVER)
                    LearningResultsSummary(result.correctCount, result.totalQuestions,
                        result.answeredCount, result.accuracy)
                }
                item { Text(stringResource(R.string.report_weighted_score) + ": " +
                    com.mukhtabir.android.util.bidi(com.mukhtabir.android.util.formatDecimal(result.weightedScore) + "%")) }
                item { Text(stringResource(R.string.master_result_dates,formatLocalDateTime(result.startedAt),result.finishedAt?.let(::formatLocalDateTime).orEmpty())) }
                if(result.isMakeup) item { Text(stringResource(R.string.master_makeup)) }
                if(result.forcedFinish) item { Text(stringResource(R.string.master_forced_finish)) }
            }
            if (s.storageFailed || s.needsSync || s.finishPending || s.draftConflict) item {
                LearningSaveStatus(LearningSaveState.REVIEW)
            }
            if(s.status?.isComplete==false) {
                item {
                    if(s.remainingSeconds==0L && (s.graceRemainingSeconds ?: 0)>0) {
                        Text(stringResource(R.string.master_grace_notice),modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite })
                    }
                    Text(stringResource(R.string.master_time_rule))
                }
                if(s.needsSync) item { AppStatus(stringResource(R.string.master_sync_required), kind = AppStatusKind.WARNING) }
                s.page?.let { page ->
                    val question=QuestionDto(id=page.question.id,question=page.question.text,choices=page.question.choices,
                        translations=page.question.translations,case=page.question.case,answersHidden=true).localizedForDisplay(readingLocale ?: java.util.Locale.getDefault().toLanguageTag())
                    val canEdit=!s.busy && !s.finishPending && !s.storageFailed && !s.needsSync && !s.draftConflict && (s.graceRemainingSeconds ?: 0)>0 &&
                        (s.draft==null || s.draft?.questionId==page.question.id)
                    val displayDraft=s.draft?.takeIf { !s.draftConflict && it.questionId==page.question.id }
                    val selected=displayDraft?.answer ?: page.savedAnswer
                    item { QuestionLanguageSelector(page.question.translations,
                        page.question.choices.size, readingLocale, { readingLocale = it },
                        hasCase = page.question.case?.stem?.isNotBlank() == true, caseTranslations = page.question.case?.translations ?: kotlinx.serialization.json.JsonObject(emptyMap())) }
                    item { ClinicalCaseContext(question.case) }
                    item { QuestionTextCard(question.question) }
                    item { QuestionImage(page.question.imageUrl,images) }
                    item(key = "answer-choices") {
                        Column(Modifier.fillMaxWidth().selectableGroup(),
                            verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                            question.choices.forEachIndexed { index, choice ->
                                AnswerChoice(number=index+1, text=choice, selected=selected==index+1,
                                    enabled=canEdit, neutralSelection=true, onClick={vm.choose(index+1)})
                            }
                        }
                    }
                    item {
                        androidx.compose.runtime.key(page.question.id) {
                            ConfidenceSelector(displayDraft?.confidence ?: page.savedConfidence ?: 3,
                                vm::confidence, enabled = canEdit && selected != null)
                        }
                    }
                    item {
                        AppOutlinedButton({confirmation="report"},enabled=!s.busy && !s.storageFailed && !s.needsSync && !s.finishPending && page.question.id !in s.reported) { Text(stringResource(R.string.master_report)) }
                        if(page.question.id in s.reported) Text(stringResource(R.string.master_report_attempted))
                    }
                }
                item {
                    AppButton({confirmation="finish"},enabled=!s.busy && !s.storageFailed && !s.needsSync && (!s.finishPending || s.finishSessionId==s.status?.sessionId) &&
                        (s.draft==null || s.graceRemainingSeconds==0L)) { Text(stringResource(R.string.finish)) }
                }
            }
        }
        if (active && !pinned) item { sessionNavigation() }
    }
    if (s.busy && s.page != null) {
        AppLoadingIndicator(Modifier.align(Alignment.TopEnd), compact = true)
    }
    }
    if (active && pinned) sessionNavigation()
    }
    }
    confirmation?.let { action ->
        AppConfirmationDialog(
            onDismissRequest={if(!s.busy) confirmation=null},
            title={Text(stringResource(when(action){"discard-recovery"->R.string.ux_discard_local_recovery;"start"->R.string.master_start;"finish"->R.string.finish;"report"->R.string.master_report;"discard"->R.string.master_discard_draft;else->R.string.back}))},
            text={if(action=="report") AppTextField(reportReason,{reportReason=it},label={Text(stringResource(R.string.master_report_reason))},modifier=Modifier.fillMaxWidth())
                else Text(stringResource(when(action){"discard-recovery"->R.string.master_recovery_discard;"start"->R.string.master_start_confirm;"finish"->R.string.master_finish_confirm;"discard"->R.string.master_discard_confirm;else->R.string.master_leave_confirm}))},
            confirmButton={AppConfirmationButton(
                destructive = action == "discard" || action == "discard-recovery", onClick = {
                confirmation=null
                when(action){"discard-recovery"->vm.discardUnreadableRecovery();"start"->vm.start();"finish"->vm.finish();"report"->vm.flag(reportReason);"discard"->vm.discardDraft();else->vm.back()}
            },enabled=!s.busy){Text(stringResource(when(action){"discard-recovery"->R.string.ux_discard_local_recovery;"discard"->R.string.master_discard_draft;"start"->R.string.master_start;"finish"->R.string.finish_exam_action;"report"->R.string.master_report;else->R.string.back_to_exams_action}))}},
            dismissButton={AppTextButton({confirmation=null},enabled=!s.busy){Text(stringResource(R.string.cancel))}},
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
