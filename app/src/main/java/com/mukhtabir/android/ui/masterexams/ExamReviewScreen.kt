package com.mukhtabir.android.ui.masterexams

import android.content.Context
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.has
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppSuccessText
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ExplanationCard
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

internal data class PreviewAnswer(val answer: Int? = null, val confidence: Int = 3, val submitted: Boolean = false)

internal data class ExamReviewState(val busy: Boolean = false, val error: Throwable? = null,
    val exam: MasterExamAuthorDto? = null, val preview: ExamPreviewDto? = null,
    val results: JsonObject? = null, val matrix: List<List<String>> = emptyList(),
    val rehearsal: Map<Long, PreviewAnswer> = emptyMap(), val matrixLoadedAt: String? = null, val publishSaved: Boolean = false, val publishUnknown: Boolean = false, val publishReviewed: Boolean = false)

internal class ExamReviewViewModel(private val id: Long, private val user: UserDto, private val service: MasterExamService,
    private val transfer: TransferService, private val previewMode: Boolean, context: Context) : ViewModel() {
    private val app = context.applicationContext
    private val mutable = MutableStateFlow(ExamReviewState())
    val state = mutable.asStateFlow()
    private var readJob: Job? = null
    init { load() }
    fun cancelRead() {val job=readJob;readJob=null;if(job?.isActive==true) {job.cancel();mutable.update {it.copy(busy=false)}}}
    fun resumeRead() {if(!previewMode || mutable.value.preview==null) load()}
    fun refreshIfActive() {if(!previewMode && mutable.value.exam?.status=="active") load()}
    fun choose(qid: Long, answer: Int) {
        val s=mutable.value;val q=s.preview?.questions?.firstOrNull {it.id==qid} ?: return
        if(!previewMode || s.busy || answer !in 1..q.choices.size) return
        mutable.update {it.copy(rehearsal=it.rehearsal+(qid to (it.rehearsal[qid] ?: PreviewAnswer()).copy(answer=answer,submitted=false)))}
    }
    fun confidence(qid: Long,value: Int) {
        if(!previewMode || mutable.value.busy || value !in 1..3 || mutable.value.preview?.questions?.none {it.id==qid}!=false) return
        mutable.update {it.copy(rehearsal=it.rehearsal+(qid to (it.rehearsal[qid] ?: PreviewAnswer()).copy(confidence=value,submitted=false)))}
    }
    fun submitPreview(qid: Long) {
        val s=mutable.value;val saved=s.rehearsal[qid] ?: return
        if(!previewMode || s.busy || saved.answer==null || s.preview?.questions?.none {it.id==qid}!=false) return
        mutable.update {it.copy(rehearsal=it.rehearsal+(qid to saved.copy(submitted=true)))}
    }
    fun canPublish(): Boolean = !previewMode && !mutable.value.busy && !mutable.value.publishUnknown &&
        mutable.value.exam?.let {canManageExam(user,it) && user.has("master_exams.publish_to_bank") && it.status=="completed" && it.storedStatus!="published_to_bank"}==true
    fun publish() {
        if(!canPublish()) return
        mutable.update {it.copy(busy=true,error=null,publishReviewed=false,publishSaved=false)}
        viewModelScope.launch {
            var dispatched=false
            try {
                val fresh=service.authorDetail(id);ensureActive()
                if(!canManageExam(user,fresh) || !user.has("master_exams.publish_to_bank") || fresh.status!="completed") throw ApiException(403,app.getString(R.string.error_forbidden))
                dispatched=true
                val saved=service.lifecycle(id,"publish-to-bank");ensureActive()
                mutable.update {it.copy(exam=saved,busy=false,publishSaved=true)}
                load()
            } catch(cancel: CancellationException) {throw cancel}
            catch(failure: Exception) {
                val rejected=failure is ApiException && failure.statusCode in setOf(400,401,403,404,409,422,429)
                mutable.update {it.copy(busy=false,error=failure,publishUnknown=dispatched && !rejected)}
            }
        }
    }
    fun acknowledgePublish() {if(!mutable.value.busy && mutable.value.publishReviewed) mutable.update {it.copy(publishUnknown=false,publishReviewed=false)}}
    fun load() {
        if (mutable.value.busy || !(if (previewMode) user.has("master_exams.preview") else canExportExamResults(user))) return
        mutable.update { it.copy(busy = true, error = null) }
        readJob=viewModelScope.launch {
            try {
                if (previewMode) {
                    val exam = service.authorDetail(id)
                    ensureActive()
                    if (!canManageExam(user, exam) || !user.has("master_exams.preview")) throw ApiException(403, app.getString(R.string.error_forbidden))
                    val preview = service.preview(id)
                    ensureActive()
                    check(preview.isPreview && preview.examId == id)
                    mutable.update { it.copy(exam = exam, preview = preview, rehearsal=emptyMap(), busy = false) }
                } else {
                    val results = service.results(id)
                    ensureActive()
                    val exam = service.resultsExam(results)
                    if (!canViewExamResults(user, exam)) throw ApiException(403, app.getString(R.string.error_forbidden))
                    mutable.update { it.copy(exam = exam, results = results, busy = false, publishReviewed=it.publishUnknown) }
                }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { ensureActive(); mutable.update { it.copy(busy = false, error = failure,
                // Revoke private content after an authoritative denial.
                exam = if (failure is ApiException && failure.statusCode in setOf(403, 404)) null else it.exam,
                rehearsal = if (failure is ApiException && failure.statusCode in setOf(403, 404)) emptyMap() else it.rehearsal,
                results = if (failure is ApiException && failure.statusCode in setOf(403, 404)) null else it.results,
                preview = if (failure is ApiException && failure.statusCode in setOf(403, 404)) null else it.preview,
                matrix = if (failure is ApiException && failure.statusCode in setOf(403, 404)) emptyList() else it.matrix) } }
        }
    }
    fun matrix() {
        if (previewMode || mutable.value.busy || mutable.value.exam?.let { canViewExamResults(user, it) } != true) return
        mutable.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            var file: File? = null
            try {
                val exam = service.resultsExam(service.results(id))
                ensureActive()
                if (!canViewExamResults(user, exam)) throw ApiException(403, app.getString(R.string.error_forbidden))
                val target = withContext(Dispatchers.IO) { File.createTempFile("exam-matrix-", ".csv", app.cacheDir) }.also { file = it }
                transfer.exportExam(target, id, "matrix")
                val rows = withContext(Dispatchers.IO) {
                    if (target.length() > 10 * 1024 * 1024) throw ApiException(null, app.getString(R.string.exam_matrix_large))
                    readExamMatrix(target.readText(Charsets.UTF_8))
                }
                ensureActive()
                mutable.update { it.copy(matrix = rows, matrixLoadedAt=java.time.Instant.now().toString(), busy = false) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { ensureActive(); mutable.update { it.copy(busy = false, error = failure,
                exam = if (failure is ApiException && failure.statusCode in setOf(403, 404)) null else it.exam,
                matrix = if (failure is ApiException && failure.statusCode in setOf(403, 404)) emptyList() else it.matrix,
                results = if (failure is ApiException && failure.statusCode in setOf(403, 404)) null else it.results) } }
            finally { file?.delete() }
        }
    }
    class Factory(private val id: Long, private val user: UserDto, private val service: MasterExamService,
        private val transfer: TransferService, private val preview: Boolean, private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = ExamReviewViewModel(id, user, service, transfer, preview, context) as T
    }
}

/** RFC4180 fields, including escaped quotes and embedded newlines; no CSV formula evaluation. */
internal fun readExamMatrix(text: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val row = mutableListOf<String>()
    val cell = StringBuilder()
    var quoted = false
    var closed = false
    var i = if (text.startsWith('\uFEFF')) 1 else 0
    fun field() { row.add(cell.toString()); cell.setLength(0); closed = false }
    while (i < text.length) {
        val char = text[i++]
        when {
            quoted && char == '"' -> if (i < text.length && text[i] == '"') { cell.append('"'); i++ } else { quoted = false; closed = true }
            quoted -> cell.append(char)
            char == '"' -> { require(cell.isEmpty() && !closed); quoted = true }
            char == ',' -> field()
            char == '\n' || char == '\r' -> { if (char == '\r' && i < text.length && text[i] == '\n') i++; field(); rows.add(row.toList()); row.clear() }
            else -> { require(!closed); cell.append(char) }
        }
    }
    require(!quoted)
    if (cell.isNotEmpty() || row.isNotEmpty() || closed) { field(); rows.add(row.toList()) }
    require(rows.isNotEmpty() && rows.first().take(2) == listOf("full_name", "username"))
    val width = rows.first().size
    require(rows.all { it.size == width })
    return rows
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExamReviewRoute(id: Long, user: UserDto, service: MasterExamService, transfer: TransferService,
    images: ImageRepository, preview: Boolean, onBack: () -> Unit, onCsv: () -> Unit,onBusyChanged: (Boolean) -> Unit = {}) {
    if (!(if (preview) user.has("master_exams.preview") else canExportExamResults(user))) { Text(stringResource(R.string.error_forbidden)); return }
    val context = androidx.compose.ui.platform.LocalContext.current
    val vm: ExamReviewViewModel = viewModel(key = "exam-review-${user.id}-$id-$preview", factory = ExamReviewViewModel.Factory(id, user, service, transfer, preview, context))
    val s by vm.state.collectAsLifecycleState()
    val owner=androidx.compose.ui.platform.LocalLifecycleOwner.current
    LaunchedEffect(owner,vm) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            try {vm.resumeRead();while(isActive) {delay(30_000);vm.refreshIfActive()}}
            finally {vm.cancelRead()}
        }
    }
    DisposableEffect(vm) {onDispose {vm.cancelRead()}}
    DisposableEffect(s.busy) {onBusyChanged(s.busy);onDispose {onBusyChanged(false)}}
    var confirmation by remember {mutableStateOf<String?>(null)}
    androidx.activity.compose.BackHandler {if(!s.busy) {if(preview && s.rehearsal.isNotEmpty() || s.publishUnknown) confirmation="leave" else onBack()}}
    confirmation?.let {action->AppConfirmationDialog(onDismissRequest={confirmation=null},
        title={Text(stringResource(if(action=="publish") R.string.exam_publish_bank else R.string.confirm))},
        text={Text(stringResource(when(action) {"publish"->R.string.exam_bank_warning;"refresh"->R.string.preview_restart_warning;else->if(s.publishUnknown) R.string.content_unknown else R.string.preview_leave_warning}))},
        confirmButton={AppTextButton({confirmation=null;when(action){"publish"->vm.publish();"refresh"->vm.load();else->onBack()}},enabled=!s.busy){Text(stringResource(R.string.confirm))}},
        dismissButton={AppTextButton({confirmation=null}){Text(stringResource(R.string.cancel))}})}
    var readingLocale by rememberSaveable(user.id,id,preview) { mutableStateOf<String?>(null) }
    var position by rememberSaveable(user.id,id,preview) { mutableStateOf(0) }
    var reveal by rememberSaveable(user.id,id,preview) { mutableStateOf(false) }
    var participantPage by remember { mutableStateOf(1) }
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(if (preview) R.string.exam_preview_title else R.string.exam_results_title))
            Text(s.exam?.name.orEmpty(), style = MaterialTheme.typography.titleLarge)
            s.exam?.let {Text(masterStatus(it.status))}
            if(s.publishSaved) AppSuccessText(stringResource(R.string.saved))
            AppOutlinedButton({if(preview && s.rehearsal.isNotEmpty() || s.publishUnknown) confirmation="leave" else onBack()}, enabled=!s.busy) { Text(stringResource(R.string.back)) }
            AppOutlinedButton({if(preview && s.rehearsal.isNotEmpty()) confirmation="refresh" else vm.load()}, enabled = !s.busy) { Text(stringResource(R.string.refresh)) }
            if(!preview && s.exam?.status=="active") Text(stringResource(R.string.exam_results_live))
            if(s.publishUnknown) {
                Text(stringResource(R.string.content_unknown))
                AppOutlinedButton(vm::load,enabled=!s.busy){Text(stringResource(R.string.refresh))}
                AppOutlinedButton(vm::acknowledgePublish,enabled=!s.busy && s.publishReviewed){Text(stringResource(R.string.content_reviewed))}
            }
            if(vm.canPublish()) AppOutlinedButton({confirmation="publish"}){Text(stringResource(R.string.exam_publish_bank))}
            if (s.busy) AppLoadingIndicator()
            s.error?.let { ErrorState(throwable = it, onRetry = if (s.busy) null else vm::load) }
        }
        s.preview?.let { snapshot ->
            item {
                Text(stringResource(R.string.exam_preview_policy))
                val questions = snapshot.questions
                if (questions.isEmpty()) Text(stringResource(R.string.exam_author_no_questions))
                else {
                    val index = position.coerceIn(0, questions.lastIndex)
                    val original = questions[index].question()
                    val q = original.localizedForDisplay(readingLocale ?: java.util.Locale.getDefault().toLanguageTag())
                    QuestionLanguageSelector(original.translations, original.choices.size,
                        readingLocale, { readingLocale = it }, hasCase = original.case?.stem?.isNotBlank() == true, caseTranslations = original.case?.translations ?: kotlinx.serialization.json.JsonObject(emptyMap()),
                        explanationAvailable = reveal && !original.explanation.isNullOrBlank(),
                        explanationTranslated = !q.explanation.isNullOrBlank())
                    val answer=s.rehearsal[q.id] ?: PreviewAnswer()
                    Text(stringResource(R.string.report_page, com.mukhtabir.android.util.formatNumber(index + 1), com.mukhtabir.android.util.formatNumber(questions.size)))
                    ClinicalCaseContext(q.case)
                    QuestionTextCard(q.question)
                    QuestionImage(q.imageUrl, images)
                    q.choices.forEachIndexed { choice, text ->
                        com.mukhtabir.android.ui.quiz.SelectableButton(
                            selected = answer.answer == choice + 1, enabled = !s.busy,
                            onClick = { vm.choose(q.id, choice + 1) },
                            text = text, choiceNumber = choice + 1,
                            markdown = true, modifier = Modifier.fillMaxWidth())
                        if (reveal && q.correctAnswer == choice + 1) Text(stringResource(R.string.correct_answer))
                    }
                    androidx.compose.runtime.key(q.id) {
                        AppSection(stringResource(R.string.confidence_prompt),
                            subtitle = stringResource(when (answer.confidence) {
                                1 -> R.string.confidence_guessing
                                2 -> R.string.confidence_uncertain
                                else -> R.string.confidence_certain
                            })) {
                            AppActionRow {
                                listOf(1 to R.string.confidence_guessing, 2 to R.string.confidence_uncertain,
                                    3 to R.string.confidence_certain).forEach { (value, label) ->
                                    AppFilterChip(answer.confidence == value, { vm.confidence(q.id, value) },
                                        enabled = !s.busy, label = { Text(stringResource(label)) })
                                }
                            }
                        }
                    }
                    AppButton({vm.submitPreview(q.id)},enabled=!s.busy && answer.answer!=null){Text(stringResource(R.string.master_save_answer))}
                    if(answer.submitted) {
                        Text(stringResource(if(answer.answer==q.correctAnswer) R.string.correct_answer else R.string.incorrect_answer))
                        ExplanationCard(q.explanation.orEmpty())
                    }
                    Text(stringResource(R.string.preview_answered_count, com.mukhtabir.android.util.formatNumber(s.rehearsal.values.count {it.submitted}), com.mukhtabir.android.util.formatNumber(questions.size)))
                    AppOutlinedButton({confirmation="leave"},enabled=!s.busy){Text(stringResource(R.string.preview_finish))}
                    AppOutlinedButton({ reveal = !reveal }) { Text(stringResource(if (reveal) R.string.exam_preview_hide else R.string.exam_preview_reveal)) }
                    if (reveal) ExplanationCard(q.explanation.orEmpty())
                    AppOutlinedButton({ position = index - 1; reveal = false }, enabled = index > 0) { Text(stringResource(R.string.previous)) }
                    AppOutlinedButton({ position = index + 1; reveal = false }, enabled = index < questions.lastIndex) { Text(stringResource(R.string.next)) }
                }
            }
        }
        s.results?.let { results ->
            item {
                AppSection(stringResource(R.string.exam_results_summary), initiallyExpanded = true) {
                    (results["summary"] as? JsonObject)?.let { OperatorReport(it, listOf("total_assigned", "started", "finished", "in_progress", "not_started", "avg_accuracy", "avg_weighted_score")) }
                }
                AppOutlinedButton(onCsv,enabled=!s.busy) { Text(stringResource(R.string.transfer_exam_csv)) }
            }
            listOf("per_user" to R.string.exam_results_participants, "per_question" to R.string.exam_results_questions,
                "per_category" to R.string.nav_categories, "per_difficulty" to R.string.difficulty,
                "histogram" to R.string.exam_results_histogram, "flags" to R.string.exam_results_flags).forEach { (key, label) -> item {
                AppSection(stringResource(label)) {
                    val rows = (results[key] as? JsonArray)?.mapNotNull { it as? JsonObject }.orEmpty()
                    OperatorRows(rows, when (key) {
                        "per_user" -> listOf("full_name", "username", "is_complete", "is_makeup", "forced_finish", "correct_count", "total_questions", "answered_count", "accuracy", "weighted_score", "started_at", "finished_at")
                        "per_question" -> listOf("question_id", "question_text", "correct_answer", "answered_count", "correct_count", "correct_rate", "distribution")
                        "per_category" -> listOf("category_name", "total", "answered", "correct", "correct_rate")
                        "per_difficulty" -> listOf("difficulty", "total", "answered", "correct", "correct_rate")
                        "histogram" -> listOf("range", "count")
                        else -> listOf("question_id", "question_text", "flagger_username", "reason", "created_at")
                    })
                }
            } }
            item {
                AppSection(stringResource(R.string.exam_results_matrix)) {
                    Text(stringResource(R.string.exam_matrix_legend))
                    AppOutlinedButton(vm::matrix, enabled = !s.busy) { Text(stringResource(R.string.exam_matrix_load)) }
                    val matrix = s.matrix
                    if (matrix.isNotEmpty()) {
                        s.matrixLoadedAt?.let {Text(stringResource(R.string.exam_matrix_loaded_at,com.mukhtabir.android.util.formatLocalDateTime(it)))}
                        val people = matrix.drop(1)
                        val pages = ((people.size - 1).coerceAtLeast(0) / 20) + 1
                        val page = participantPage.coerceIn(1, pages)
                        if (people.isEmpty()) EmptyState(stringResource(R.string.master_empty))
                        // Each cell carries its column label; RTL and TalkBack do not depend on physical alignment.
                        people.drop((page - 1) * 20).take(20).forEach { row -> AppCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(AppLayoutTokens.denseCardInsets)) {
                                Text(row[0], style = MaterialTheme.typography.titleMedium)
                                Text(row[1])
                                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                                    row.drop(2).forEachIndexed { column, value -> Text(matrix[0][column + 2] + ": " + value) }
                                }
                            }
                        } }
                        if (pages > 1) {
                            Text(stringResource(R.string.report_page, com.mukhtabir.android.util.formatNumber(page), com.mukhtabir.android.util.formatNumber(pages)))
                            AppOutlinedButton({ participantPage = page - 1 }, enabled = page > 1) { Text(stringResource(R.string.previous)) }
                            AppOutlinedButton({ participantPage = page + 1 }, enabled = page < pages) { Text(stringResource(R.string.next)) }
                        }
                    }
                }
            }
        }
    }
}
