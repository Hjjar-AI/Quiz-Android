@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.ui.components.AppSuccessText

import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.QuestionRatingDto
import com.mukhtabir.android.data.remote.QuestionService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import com.mukhtabir.android.core.isRejectedWrite
import com.mukhtabir.android.ui.components.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@kotlinx.serialization.Serializable
internal data class FeedbackDraft(val selected: Int = 0, val reason: String = "", val pending: String? = null)
data class QuestionFeedbackState(
    val rating: QuestionRatingDto? = null, val selectedRating: Int = 0, val lastSavedRating: Int? = null,
    val ratingLoading: Boolean = false, val ratingSaving: Boolean = false, val ratingSaved: Boolean = false,
    val ratingError: String? = null, val reportReason: String = "", val reporting: Boolean = false,
    val reported: Boolean = false, val reportError: String? = null, val pending: String? = null,
    val reviewed: Boolean = false, val uncertain: Boolean = false,
)
internal class QuestionFeedbackViewModel(private val questionId: Long, private val service: QuestionService,
    store: com.mukhtabir.android.core.SessionDraftStore, userId: Long) : ViewModel() {
    private val _state = MutableStateFlow(QuestionFeedbackState()); val state = _state.asStateFlow()
    val recovery = com.mukhtabir.android.core.EditorRecovery(viewModelScope, store, userId,
        "feedback-$questionId", FeedbackDraft.serializer())
    private fun draft(): FeedbackDraft? = _state.value.let { s ->
        FeedbackDraft(s.selectedRating, s.reportReason, s.pending).takeIf {
            s.pending != null || (!s.reported && s.reportReason.isNotBlank()) || (s.selectedRating in 1..5 && s.selectedRating != s.lastSavedRating)
        }
    }
    init { recovery.observe(state) { draft() }; loadRating() }
    fun restore(d: FeedbackDraft) { _state.update { it.copy(selectedRating=d.selected,reportReason=d.reason,pending=d.pending,uncertain=d.pending!=null,reviewed=false) } }
    private fun busy() = _state.value.ratingLoading || _state.value.ratingSaving || _state.value.reporting
    fun selectRating(value: Int) { if(value in 1..5 && !busy() && !recovery.blocked && _state.value.pending==null)
        _state.update {it.copy(selectedRating=value,ratingSaved=false,ratingError=null)} }
    fun reportReason(value: String) { if(!busy() && !recovery.blocked && _state.value.pending==null && !_state.value.reported)
        _state.update {it.copy(reportReason=value,reportError=null)} }
    fun loadRating() {
        if(busy()) return
        _state.update {it.copy(ratingLoading=true,ratingError=null,reviewed=false)}
        viewModelScope.launch {
            try {
                val rating=service.rating(questionId)
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                val before=_state.value
                val ratingConfirmed=before.pending=="rating" && rating.userRating==before.selectedRating
                _state.update {it.copy(rating=rating,lastSavedRating=rating.userRating.takeIf {v->v in 1..5},
                    selectedRating=it.selectedRating.takeIf {v->v in 1..5} ?: rating.userRating,
                    reviewed=it.pending!="report",pending=if(ratingConfirmed) null else it.pending,
                    uncertain=if(ratingConfirmed) false else it.uncertain,ratingSaved=it.ratingSaved || ratingConfirmed)}
                try {
                    val report=service.reportState(questionId)
                    kotlinx.coroutines.currentCoroutineContext().ensureActive()
                    _state.update {s->val confirmed=s.pending=="report" && report.reported
                        s.copy(reported=report.reported,reportError=null,reviewed=true,
                            pending=if(confirmed) null else s.pending,uncertain=if(confirmed) false else s.uncertain)}
                } catch(cancel: CancellationException) {throw cancel}
                catch(failure: Exception) {_state.update {it.copy(reportError=failure.message)}}
                if(!recovery.blocked) recovery.persist(draft())
            } catch(cancel: CancellationException) {throw cancel}
            catch(failure: Exception) {_state.update {it.copy(ratingError=failure.message)}}
            finally {_state.update {it.copy(ratingLoading=false)}}
        }
    }
    fun acknowledge() {if(!busy() && !recovery.blocked && _state.value.reviewed) _state.update {it.copy(pending=null,uncertain=false,reviewed=false)} }
    fun saveRating() { val s=_state.value; if(s.selectedRating !in 1..5 || s.selectedRating==s.lastSavedRating) return; write("rating") }
    fun report() {if(!_state.value.reported) write("report")}
    private fun write(kind: String) {
        val s=_state.value
        if(busy() || recovery.blocked || s.pending!=null) return
        _state.update {it.copy(pending=kind,ratingSaving=kind=="rating",reporting=kind=="report",ratingError=null,reportError=null,reviewed=false)}
        viewModelScope.launch {
            var sent=false
            try {
                if(!recovery.persist(draft())) return@launch
                sent=true
                if(kind=="rating") service.rate(questionId,s.selectedRating) else service.report(questionId,s.reportReason)
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                _state.update {it.copy(pending=null,uncertain=false,ratingSaved=it.ratingSaved || kind=="rating",
                    lastSavedRating=if(kind=="rating") s.selectedRating else it.lastSavedRating,reported=it.reported || kind=="report")}
                recovery.persist(draft())
                if(kind=="rating") {
                    try { val refreshed=service.rating(questionId); _state.update {it.copy(rating=refreshed)} }
                    catch(cancel: CancellationException) {throw cancel}
                    catch(failure: Exception) {_state.update {it.copy(ratingError=failure.message)}}
                }
            } catch(cancel: CancellationException) {throw cancel}
            catch(failure: Exception) {
                val unknown=sent && !failure.isRejectedWrite()
                _state.update {it.copy(pending=if(unknown) kind else null,uncertain=unknown,
                    ratingError=if(kind=="rating") failure.message else it.ratingError,reportError=if(kind=="report") failure.message else it.reportError)}
            } finally {_state.update {it.copy(ratingSaving=false,reporting=false)}}
        }
    }
    class Factory(private val id: Long,private val service: QuestionService,private val store: com.mukhtabir.android.core.SessionDraftStore,private val user: Long): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>): T = QuestionFeedbackViewModel(id,service,store,user) as T
    }
}

@Composable
fun QuestionFeedback(questionId: Long, userId: Long, service: QuestionService, store: com.mukhtabir.android.core.SessionDraftStore) {
    val vm: QuestionFeedbackViewModel = viewModel(
        key = "question-feedback-$userId-$questionId", factory = QuestionFeedbackViewModel.Factory(questionId, service, store, userId),
    )
    val state by vm.state.collectAsState()
    val blocked = editorRecoveryBlocked(vm.recovery)
    var showReport by rememberSaveable(questionId, userId) { mutableStateOf(false) }
    LaunchedEffect(state.reported) { if (state.reported) showReport = false }
    AppSection(stringResource(R.string.question_feedback)) {
            EditorRecoveryControls(vm.recovery,state.ratingLoading || state.ratingSaving || state.reporting,vm::restore)
            if(state.pending!=null) {
                Text(stringResource(R.string.author_write_unknown))
                TextButton(vm::loadRating,enabled=!state.ratingLoading && !state.ratingSaving && !state.reporting && !blocked) {Text(stringResource(R.string.refresh))}
                if(state.reviewed) TextButton(vm::acknowledge,enabled=!blocked) {Text(stringResource(R.string.tools_reviewed))}
            }
            state.rating?.let {
                Text(if (it.count == 0) stringResource(R.string.no_ratings)
                    else stringResource(R.string.rating_average, it.average, it.count))
            }
            if (state.ratingLoading || state.ratingSaving) AppLoadingIndicator()
            Text(stringResource(R.string.your_rating))
            Row(horizontalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                (1..5).forEach { rating ->
                    FilterChip(selected = state.selectedRating == rating, onClick = { vm.selectRating(rating) },
                        enabled = !blocked && state.pending==null && !state.reporting && !state.ratingLoading && !state.ratingSaving, label = { Text(rating.toString()) })
                }
            }
            Button(vm::saveRating, enabled = !blocked && state.pending==null && !state.reporting && !state.ratingLoading && !state.ratingSaving
                && state.selectedRating in 1..5 && state.selectedRating != (state.lastSavedRating ?: state.rating?.userRating)) {
                Text(stringResource(R.string.save_rating))
            }
            if (state.ratingSaved) AppSuccessText(stringResource(R.string.saved))
            state.ratingError?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
                TextButton(vm::loadRating, enabled = !blocked && state.pending==null && !state.reporting && !state.ratingLoading && !state.ratingSaving) {
                    Text(stringResource(R.string.refresh_rating))
                }
            }
            state.reportError?.let { Text(it, color=MaterialTheme.colorScheme.error) }
            if (state.reported) Text(stringResource(R.string.question_reported))
            else OutlinedButton({ showReport = true }, enabled = !blocked && state.pending==null && !state.ratingLoading && !state.ratingSaving && !state.reporting) {
                Text(stringResource(R.string.report_question))
            }
    }
    if (showReport) {
        AlertDialog(
            onDismissRequest = { if (!state.reporting) showReport = false },
            title = { Text(stringResource(R.string.report_question)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                    OutlinedTextField(state.reportReason, vm::reportReason, enabled = !blocked && state.pending==null && !state.ratingLoading && !state.ratingSaving && !state.reporting,
                        label = { Text(stringResource(R.string.report_reason)) }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    state.reportError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    if (state.reporting) AppLoadingIndicator()
                }
            },
            confirmButton = {
                TextButton(vm::report, enabled = !blocked && state.pending==null && !state.ratingLoading && !state.ratingSaving && !state.reporting) { Text(stringResource(R.string.submit_report)) }
            },
            dismissButton = {
                TextButton({ showReport = false }, enabled = !state.reporting) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}
