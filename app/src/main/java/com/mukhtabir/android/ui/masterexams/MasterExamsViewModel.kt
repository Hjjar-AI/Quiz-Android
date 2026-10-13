package com.mukhtabir.android.ui.masterexams

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.R
import com.mukhtabir.android.core.LearnerSavedState
import com.mukhtabir.android.core.SessionDraftStore
import com.mukhtabir.android.core.MasterExamRecovery
import com.mukhtabir.android.data.remote.MasterExamAnswerRequest
import com.mukhtabir.android.data.remote.MasterExamAttemptDto
import com.mukhtabir.android.data.remote.MasterExamDto
import com.mukhtabir.android.data.remote.MasterExamService
import com.mukhtabir.android.data.remote.MasterExamStatusDto
import com.mukhtabir.android.data.remote.SessionQuestionPageDto
import java.time.Duration
import java.time.Instant
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

// Unresolved answers use the existing encrypted account store and are never silently resubmitted.
data class MasterExamsState(
    val exams: List<MasterExamDto> = emptyList(),
    val notices: Set<Long> = emptySet(),
    val examId: Long? = null,
    val exam: MasterExamDto? = null,
    val status: MasterExamStatusDto? = null,
    val result: MasterExamAttemptDto? = null,
    val page: SessionQuestionPageDto? = null,
    val draft: MasterExamAnswerRequest? = null,
    val draftPage: SessionQuestionPageDto? = null,
    val draftSessionId: String? = null,
    val draftConflict: Boolean = false,
    val busy: Boolean = false,
    val needsSync: Boolean = false,
    val remainingSeconds: Long? = null,
    val graceRemainingSeconds: Long? = null,
    val error: Throwable? = null,
    val noticesError: Throwable? = null,
    val message: Int? = null,
    val reported: Set<Long> = emptySet(),
    val statusFilter: String = "all",
    val restoredNavigation: Boolean = false,
    val storageFailed: Boolean = false,
    val finishPending: Boolean = false,
    val finishSessionId: String? = null,
)

class MasterExamsViewModel(private val service: MasterExamService, private val userId: Long, handle: SavedStateHandle,
    private val draftStore: SessionDraftStore) : ViewModel() {
    // SavedStateHandle holds metadata only; unresolved answer snapshots use encrypted recovery.
    private val saved = LearnerSavedState(handle,userId,"master_exam_navigation")
    private var pendingRecoveryWrite: MasterExamRecovery? = null
    private val _state = MutableStateFlow(restoreRecoveryState())
    val state = _state.asStateFlow()
    private var deadlineElapsed: Long? = null
    private var graceDeadlineElapsed: Long? = null
    private val automaticFinishSessions = mutableSetOf<String>()

    init {
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                val s = _state.value
                val deadline = deadlineElapsed
                if (s.status?.isComplete == false && deadline != null) {
                    val remaining = ((deadline-SystemClock.elapsedRealtime()+999)/1000).coerceAtLeast(0)
                    val graceRemaining = graceDeadlineElapsed?.let {
                        ((it-SystemClock.elapsedRealtime()+999)/1000).coerceAtLeast(0)
                    } ?: remaining
                    _state.update { it.copy(remainingSeconds=remaining,graceRemainingSeconds=graceRemaining) }
                    if (graceRemaining==0L && !s.finishPending && !s.busy && !s.storageFailed && !s.needsSync && s.status.sessionId !in automaticFinishSessions) {
                        automaticFinishSessions.add(s.status.sessionId)
                        finish(timed=true)
                    }
                }
            }
        }
    }

    fun refresh() {
        val id=_state.value.examId
        if(id==null) request {
            val exams=service.list().items
            coroutineContext.ensureActive()
            _state.update { it.copy(exams=exams) }
            try {
                val notices=service.notices()
                coroutineContext.ensureActive()
                _state.update { it.copy(notices=notices.items.map { e -> e.id }.toSet(),noticesError=null) }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) { _state.update { it.copy(noticesError=e) } }
        } else request { discover(id) }
    }

    fun open(id: Long) {
        val s=_state.value
        if(s.busy || s.storageFailed || s.examId!=null || s.draft!=null || id<=0) return
        saveRecovery(MasterExamRecovery(id))
        saved.saveExpandedId(id)
        deadlineElapsed=null
        graceDeadlineElapsed=null
        _state.update { it.copy(examId=id,exam=null,status=null,result=null,page=null,draft=null,draftPage=null,draftSessionId=null,draftConflict=false,
            remainingSeconds=null,graceRemainingSeconds=null,needsSync=true,error=null,message=null,reported=emptySet(),restoredNavigation=false) }
        request { discover(id) }
    }

    fun back() {
        if(_state.value.busy || !clearRecovery()) return
        saved.saveExpandedId(null)
        deadlineElapsed=null
        graceDeadlineElapsed=null
        _state.update { it.copy(examId=null,exam=null,status=null,result=null,page=null,draft=null,draftPage=null,draftSessionId=null,draftConflict=false,
            needsSync=false,remainingSeconds=null,graceRemainingSeconds=null,error=null,message=null,reported=emptySet(),restoredNavigation=false) }
        refresh()
    }

    fun filter(value: String) {
        if(_state.value.busy || _state.value.examId!=null || value !in setOf("all","active","scheduled","completed")) return
        saved.saveFilter(value)
        _state.update { it.copy(statusFilter=value) }
    }

    fun dismissRestorationNotice() {
        _state.update { it.copy(restoredNavigation=false) }
    }

    private suspend fun discover(id: Long) {
        val exam=service.detail(id)
        coroutineContext.ensureActive()
        _state.update { it.copy(exam=exam) }
        if(exam.myAttempt==null) {
            // Only a successful detail read establishes that there is no saved attempt.
            deadlineElapsed=null
            graceDeadlineElapsed=null
            _state.update { it.copy(status=null,page=null,result=null,draftConflict=it.draft!=null,
                needsSync=false,remainingSeconds=null,graceRemainingSeconds=null) }
        } else synchronize(id)
    }

    private suspend fun synchronize(id: Long, loadQuestion: Boolean=true) {
        val status=service.status(id)
        coroutineContext.ensureActive()
        // Keep the signed interval: clamping here would grant a fresh grace period on a late resume.
        val remaining=Duration.between(Instant.parse(status.serverNow),Instant.parse(status.deadlineAt)).toMillis()
        val deadline=SystemClock.elapsedRealtime()+remaining
        deadlineElapsed=deadline
        graceDeadlineElapsed=deadline+status.graceSeconds.coerceAtLeast(0)*1_000L
        val graceRemaining=(remaining+status.graceSeconds.coerceAtLeast(0)*1_000L).coerceAtLeast(0)
        val previous=_state.value
        val draft=previous.draft
        val sameSession=draft==null || previous.draftSessionId==status.sessionId
        val slot=draft?.let { status.answers[it.questionId.toString()] }
        val savedAnswer=slot?.get("answer")?.jsonPrimitive?.intOrNull
        val savedConfidence=if(savedAnswer==null) null else slot.get("confidence")?.jsonPrimitive?.intOrNull ?: 3
        val confirmed=draft!=null && sameSession && savedAnswer==draft.answer && savedConfidence==draft.confidence
        val unchanged=draft==null || (draft.sessionId!=null && sameSession && draft.expectedSlot==slot)
        val sameFinishSession=!previous.finishPending || previous.finishSessionId==status.sessionId
        val clearDraft=(confirmed || (status.isComplete && sameSession && sameFinishSession)) &&
            saveRecovery(MasterExamRecovery(id,sessionId=previous.finishSessionId?.takeIf { previous.finishPending && !status.isComplete },
                finishPending=previous.finishPending && (!status.isComplete || !sameFinishSession)))
        _state.update { it.copy(status=status,remainingSeconds=(remaining.coerceAtLeast(0)+999)/1000,
            graceRemainingSeconds=(graceRemaining+999)/1000,
            draft=if(clearDraft) null else it.draft,
            draftPage=if(clearDraft) null else it.draftPage,
            draftSessionId=if(clearDraft) null else it.draftSessionId,
            draftConflict=!clearDraft && !unchanged,
            reported=if(previous.status!=null && previous.status.sessionId!=status.sessionId) emptySet() else it.reported) }
        if(status.isComplete) {
            val detail=service.detail(id)
            coroutineContext.ensureActive()
            val result=detail.myAttempt
            check(result!=null && result.isComplete && result.sessionId==status.sessionId) { "Completed result is not available yet" }
            _state.update { it.copy(exam=detail,result=result,page=null,needsSync=false) }
        } else if(loadQuestion) {
            val page=service.question(id)
            coroutineContext.ensureActive()
            _state.update { it.copy(page=page,result=null,needsSync=false) }
        }
    }

    private fun request(mutation: Boolean=false, action: suspend () -> Unit) {
        if(_state.value.busy) return
        _state.update { it.copy(busy=true,error=null,message=if(mutation) null else it.message) }
        viewModelScope.launch {
            try {
                action(); coroutineContext.ensureActive()
                _state.update { it.copy(busy=false) }
            } catch(e: CancellationException) { throw e }
            catch(e: Exception) {
                val id=_state.value.examId
                _state.update { it.copy(error=e,needsSync=it.needsSync || id!=null) }
                // A lost response may follow a committed write. Read status once; never replay it.
                if(mutation && id!=null) {
                    try { discover(id) }
                    catch(cancel: CancellationException) { throw cancel }
                    catch(_: Exception) { /* Retain the original failure and require explicit refresh. */ }
                }
                _state.update { it.copy(busy=false) }
            }
        }
    }

    fun acknowledge() {
        val s=_state.value; val id=s.examId ?: return
        if(s.busy || s.storageFailed || s.finishPending || s.draft!=null || s.needsSync || s.exam==null || s.exam.hasAcknowledged) return
        request(mutation=true) {
            service.acknowledge(id); coroutineContext.ensureActive()
            _state.update { it.copy(exam=it.exam?.copy(hasAcknowledged=true),notices=it.notices-id,message=R.string.master_acknowledged) }
        }
    }

    fun start() {
        val s=_state.value; val exam=s.exam ?: return
        if(s.busy || s.storageFailed || s.finishPending || s.draft!=null || s.needsSync || s.status!=null || !exam.hasAcknowledged || !canStart(exam)) return
        request(mutation=true) { service.start(exam.id); synchronize(exam.id) }
    }

    fun choose(answer: Int) {
        val s=_state.value; val page=s.page ?: return
        if(s.busy || s.storageFailed || s.finishPending || s.needsSync || s.draftConflict || s.status?.isComplete!=false || (s.graceRemainingSeconds ?: 0)<=0 || answer !in 1..page.question.choices.size) return
        if(s.draft!=null && s.draft.questionId!=page.question.id) return
        keepAnswer(MasterExamAnswerRequest(page.question.id,answer,s.draft?.confidence ?: page.savedConfidence ?: 3, expectedSlot=if(s.draft!=null) s.draft.expectedSlot else s.status.answers[page.question.id.toString()],sessionId=s.status.sessionId),s.draftPage ?: page)
    }

    fun confidence(value: Int) {
        val s=_state.value; val page=s.page ?: return
        if(s.busy || s.storageFailed || s.finishPending || s.needsSync || s.draftConflict || s.status?.isComplete!=false || value !in 1..3 || (s.graceRemainingSeconds ?: 0)<=0) return
        if(s.draft!=null && s.draft.questionId!=page.question.id) return
        val answer=s.draft?.answer ?: page.savedAnswer ?: return
        keepAnswer(MasterExamAnswerRequest(page.question.id,answer,value,expectedSlot=if(s.draft!=null) s.draft.expectedSlot else s.status.answers[page.question.id.toString()],sessionId=s.status.sessionId),s.draftPage ?: page)
    }

    fun serverDraftAnswer(): Int? = _state.value.let {s->s.draft?.let {s.status?.answers?.get(it.questionId.toString())?.get("answer")?.jsonPrimitive?.intOrNull}}
    fun rebaseDraft() {
        val s=_state.value;val draft=s.draft ?: return;val page=s.draftPage ?: return;val status=s.status ?: return
        if(s.busy || s.storageFailed || s.needsSync || s.finishPending || !s.draftConflict || status.isComplete || s.draftSessionId!=status.sessionId || (s.graceRemainingSeconds ?: 0)<=0 || draft.questionId !in status.questionIds) return
        val slot=status.answers[draft.questionId.toString()]
        val saved=slot?.get("answer")?.jsonPrimitive?.intOrNull
        val confidence=slot?.get("confidence")?.jsonPrimitive?.intOrNull
        keepAnswer(draft.copy(expectedSlot=slot,sessionId=status.sessionId),page.copy(savedAnswer=saved,savedConfidence=confidence))
        if(!_state.value.storageFailed) _state.update {it.copy(draftConflict=false)}
    }

    fun discardDraft() {
        val s=_state.value
        if(s.busy) return
        val id=s.examId ?: return
        if(saveRecovery(MasterExamRecovery(id,sessionId=s.finishSessionId,finishPending=s.finishPending)))
            _state.update { it.copy(draft=null,draftPage=null,draftSessionId=null,draftConflict=false,message=null) }
    }

    fun saveAnswer() {
        val s=_state.value; val id=s.examId ?: return; val draft=s.draft ?: return
        if(s.busy || s.storageFailed || s.finishPending || s.needsSync || s.draftConflict || s.draftSessionId!=s.status?.sessionId || s.status?.isComplete!=false || (s.graceRemainingSeconds ?: 0)<=0 || draft.questionId !in s.status.questionIds) return
        request(mutation=true) {
            synchronize(id)
            val checked=_state.value
            if(checked.draft==null) return@request // The same answer was already confirmed by the server.
            if(checked.storageFailed || checked.draftConflict || checked.status?.isComplete!=false || checked.status.sessionId!=s.draftSessionId || (checked.graceRemainingSeconds ?: 0)<=0) {
                _state.update { it.copy(message=R.string.master_draft_conflict) }; return@request
            }
            service.answer(id,draft); coroutineContext.ensureActive()
            if(saveRecovery(MasterExamRecovery(id)))
                _state.update { it.copy(draft=null,draftPage=null,draftSessionId=null,draftConflict=false) }
            _state.update { it.copy(message=R.string.saved) }
            synchronize(id)
        }
    }

    fun goto(questionId: Long) {
        val s=_state.value; val id=s.examId ?: return
        if(s.busy || s.storageFailed || s.finishPending || s.needsSync || s.draft!=null || s.status?.isComplete!=false || (s.graceRemainingSeconds ?: 0)<=0 || questionId !in s.status.questionIds) return
        request(mutation=true) {
            synchronize(id)
            if(_state.value.storageFailed || _state.value.status?.sessionId!=s.status.sessionId || _state.value.status?.isComplete!=false) {
                _state.update { it.copy(message=R.string.master_attempt_changed) }; return@request
            }
            if((_state.value.graceRemainingSeconds ?: 0)<=0) return@request
            service.goto(id,questionId,requireNotNull(_state.value.status).sessionId,_state.value.status?.currentQuestionId); synchronize(id)
        }
    }

    fun move(offset: Int) {
        if(offset !in setOf(-1,1)) return
        val s=_state.value
        val ids=s.status?.questionIds ?: return
        val current=s.page?.question?.id ?: return
        val index=ids.indexOf(current)
        if(index<0) return
        ids.getOrNull(index+offset)?.let(::goto)
    }

    fun finish(timed: Boolean=false) {
        val s=_state.value; val id=s.examId ?: return
        if(s.busy || s.storageFailed || (timed && s.finishPending) ||
            (s.finishPending && s.finishSessionId!=s.status?.sessionId) ||
            (s.draft!=null && s.draftSessionId!=s.status?.sessionId) || s.needsSync || s.status?.isComplete!=false || (!timed && s.draft!=null && (s.graceRemainingSeconds ?: 0)>0)) return
        request(mutation=true) {
            synchronize(id,loadQuestion=false)
            if(_state.value.storageFailed || _state.value.status?.sessionId!=s.status.sessionId || _state.value.status?.isComplete!=false) {
                if(_state.value.status?.isComplete==false) synchronize(id)
                _state.update { it.copy(message=R.string.master_attempt_changed) }; return@request
            }
            if(timed && (_state.value.graceRemainingSeconds ?: 0)>0) {
                // A fresh server clock/grace setting wins over the old local countdown.
                automaticFinishSessions.remove(s.status.sessionId)
                return@request
            }
            val current=_state.value
            if(!saveRecovery(MasterExamRecovery(id,current.draftSessionId ?: current.status?.sessionId,current.draft,current.draftPage,finishPending=true))) return@request
            val result=service.finish(id); coroutineContext.ensureActive()
            deadlineElapsed=null
            graceDeadlineElapsed=null
            val cleared=saveRecovery(MasterExamRecovery(id))
            _state.update { it.copy(result=result,page=null,draft=if(cleared) null else it.draft,
                draftPage=if(cleared) null else it.draftPage,draftSessionId=if(cleared) null else it.draftSessionId,
                draftConflict=!cleared && it.draft!=null,status=it.status?.copy(isComplete=true),needsSync=false) }
        }
    }

    fun flag(reason: String) {
        val s=_state.value; val id=s.examId ?: return; val qid=s.page?.question?.id ?: return
        if(s.busy || s.storageFailed || s.finishPending || s.needsSync || s.status?.isComplete!=false || qid in s.reported) return
        // Keep disabled after an ambiguous response too; reporting is not safely replayable.
        _state.update { it.copy(reported=it.reported+qid) }
        request(mutation=true) {
            synchronize(id)
            if(_state.value.storageFailed || _state.value.status?.sessionId!=s.status.sessionId || _state.value.status?.isComplete!=false) {
                _state.update { it.copy(reported=it.reported-qid,message=R.string.master_attempt_changed) }; return@request
            }
            service.flag(id,qid,reason); coroutineContext.ensureActive()
            _state.update { it.copy(message=R.string.master_report_sent) }
        }
    }

    private fun restoreRecoveryState(): MasterExamsState {
        val read=draftStore.readMaster(userId)
        val recovery=read.getOrNull()
        val id=if(recovery!=null && (recovery.draft!=null || recovery.finishPending)) recovery.examId
            else saved.expandedId() ?: recovery?.examId
        return MasterExamsState(examId=id,draft=recovery?.draft,draftPage=recovery?.page,
            draftSessionId=recovery?.sessionId,storageFailed=read.isFailure,
            finishPending=recovery?.finishPending==true,
            finishSessionId=recovery?.takeIf { it.finishPending }?.sessionId,
            needsSync=id!=null,restoredNavigation=id!=null,
            statusFilter=saved.filter()?.takeIf { it in setOf("all","active","scheduled","completed") } ?: "all")
    }

    private fun keepAnswer(draft: MasterExamAnswerRequest, page: SessionQuestionPageDto) {
        val s=_state.value
        val id=s.examId ?: return
        val session=s.draftSessionId ?: s.status?.sessionId ?: return
        _state.update { it.copy(draft=draft,draftPage=page,draftSessionId=session,message=null) }
        saveRecovery(MasterExamRecovery(id,session,draft,page))
    }

    private fun saveRecovery(recovery: MasterExamRecovery): Boolean {
        val result=draftStore.writeMaster(userId,recovery)
        pendingRecoveryWrite=if(result.isFailure) recovery else null
        _state.update { it.copy(storageFailed=result.isFailure,
            finishPending=if(result.isSuccess) recovery.finishPending else it.finishPending,
            finishSessionId=if(result.isSuccess) recovery.sessionId?.takeIf { recovery.finishPending } else it.finishSessionId) }
        return result.isSuccess
    }

    private fun clearRecovery(): Boolean {
        val result=draftStore.clearMaster(userId)
        if(result.isSuccess) pendingRecoveryWrite=null
        _state.update { it.copy(storageFailed=result.isFailure,finishPending=if(result.isSuccess) false else it.finishPending,
            finishSessionId=if(result.isSuccess) null else it.finishSessionId) }
        return result.isSuccess
    }

    fun retryStorage() {
        if(_state.value.busy) return
        if(draftStore.retryBinding(userId).isFailure) return
        val pending=pendingRecoveryWrite
        if(pending!=null) {
            if(!saveRecovery(pending)) return
        } else {
            val read=draftStore.readMaster(userId)
            if(read.isFailure) return
            val recovery=read.getOrNull()
            if(recovery!=null && _state.value.draft==null) {
                saved.saveExpandedId(recovery.examId)
                _state.update { it.copy(examId=recovery.examId,draft=recovery.draft,draftPage=recovery.page,
                    draftSessionId=recovery.sessionId,finishPending=recovery.finishPending,
                    finishSessionId=recovery.sessionId?.takeIf { recovery.finishPending },restoredNavigation=true,needsSync=true,
                    exam=null,status=null,page=null,result=null) }
            }
            _state.update { it.copy(storageFailed=false) }
        }
        refresh()
    }

    fun discardUnreadableRecovery() {
        if(_state.value.busy || _state.value.draft!=null || !clearRecovery()) return
        back()
    }

    companion object {
        fun canStart(exam: MasterExamDto): Boolean = exam.storedStatus=="published" && exam.myAttempt?.isComplete!=true &&
            (exam.status=="active" || (exam.status=="completed" && exam.allowMakeup))
    }
    class Factory(private val service: MasterExamService,private val userId: Long,private val draftStore: SessionDraftStore): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>,extras: CreationExtras): T =
            MasterExamsViewModel(service,userId,extras.createSavedStateHandle(),draftStore) as T
    }
}
