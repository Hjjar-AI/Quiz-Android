@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.masterexams

import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerTextButton

import com.mukhtabir.android.ui.components.AppActionRow

import com.mukhtabir.android.core.has
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.data.remote.GroupService
import com.mukhtabir.android.data.remote.MasterExamDto
import com.mukhtabir.android.data.remote.MasterExamService
import com.mukhtabir.android.data.remote.QuestionDto
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.ClinicalCaseContext
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownField
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.util.formatLocalDateTime
import com.mukhtabir.android.util.formatNumber
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

@Composable
fun MasterExamManagementRoute(user: UserDto, service: MasterExamService, onCreate: () -> Unit, onEdit: (Long) -> Unit) {
    var exams by remember(user.id) { mutableStateOf(emptyList<MasterExamDto>()) }
    var loading by remember(user.id) { mutableStateOf(false) }
    var error by remember(user.id) { mutableStateOf<Throwable?>(null) }
    val scope = rememberCoroutineScope()
    val refresh: () -> Unit = {
        if (!loading) {
            loading = true
            error = null
            scope.launch {
                try {
                    val response = service.list()
                    ensureActive()
                    exams = response.items.filter { user.has("master_exams.manage_any") || it.isOwner }
                } catch (cancel: CancellationException) { throw cancel }
                catch (failure: Exception) { ensureActive(); error = failure }
                finally { loading = false }
            }
        }
    }
    RefreshOnResume(refresh)
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.exam_author_manage))
            AppActionRow() {
                if (user.has("master_exams.create")) Button(onCreate, enabled = !loading) { Text(stringResource(R.string.exam_author_create)) }
                OutlinedButton(refresh, enabled = !loading) { Text(stringResource(R.string.refresh)) }
            }
            if (loading) AppLoadingIndicator()
            error?.let { ErrorState(throwable = it, onRetry = if (loading) null else refresh) }
            if (!loading && error == null && exams.isEmpty()) Text(stringResource(R.string.master_empty))
        }
        items(exams, key = { it.id }) { exam ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                    Text(exam.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
                    Text(masterStatus(exam.status))
                    Text(stringResource(R.string.master_window, formatLocalDateTime(exam.opensAt), formatLocalDateTime(exam.closesAt)))
                    Text(stringResource(R.string.master_summary, exam.questionCount, exam.durationMinutes))
                    if (user.has("master_exams.manage_any") || user.has("master_exams.manage_own")) {
                        OutlinedButton({ onEdit(exam.id) }, enabled = !loading) { Text(stringResource(R.string.exam_author_edit)) }
                    }
                }
            }
        }
    }
}

@Composable
fun MasterExamEditorRoute(id: Long?, user: UserDto, service: MasterExamService, questions: QuestionService,
    pageSize: Int, admin: AdminService, groups: GroupService, categories: CategoryService, maxChoices: Int, draftStore: com.mukhtabir.android.core.SessionDraftStore, onBack: () -> Unit, onBusyChanged: (Boolean) -> Unit, onDraft: (Long) -> Unit, onPreview: (Long) -> Unit, onResults: (Long) -> Unit) {
    val vm: MasterExamEditorViewModel = viewModel(key = "exam-author-${user.id}-${id ?: "new"}",
        factory = MasterExamEditorViewModel.Factory(id, user, service, questions, pageSize, admin, groups, categories, maxChoices, draftStore))
    val s by vm.state.collectAsState()
    val recovery by vm.recovery.collectAsState()
    val storageError by vm.recoveryError.collectAsState()
    if (recovery != null && s.loaded) AlertDialog(onDismissRequest = {}, title = { Text(stringResource(R.string.author_recover_title)) },
        text = { Text(stringResource(R.string.author_recover_body)) },
        confirmButton = { TextButton(vm::restoreDraft, enabled = !s.busy && !s.denied) { Text(stringResource(R.string.author_restore)) } },
        dismissButton = { AppDangerTextButton(vm::discardRecovery, enabled = !s.busy) { Text(stringResource(R.string.ux_discard_local_recovery)) } })
    RefreshOnResume(vm::refresh)
    var leave by rememberSaveable { mutableStateOf(false) }
    var removeId by remember { mutableStateOf<Long?>(null) }
    var transition by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf(false) }
    var deleteMode by remember { mutableStateOf("keep_drafts") }
    val back: () -> Unit = { if (!s.busy) { if (s.dirty || s.needsSync || s.plannedIds != null || s.embedded?.dirty == true || s.embedded?.uncertain == true) leave = true else onBack() } }
    BackHandler(onBack = back)
    DisposableEffect(s.busy) { onBusyChanged(s.busy); onDispose { onBusyChanged(false) } }
    LaunchedEffect(s.deleted) { if (s.deleted) onBack() }
    val enabled = vm.editable(s)
    if (leave) AlertDialog(onDismissRequest = { leave = false },
        title = { Text(stringResource(R.string.exam_author_leave_title)) },
        text = { Text(stringResource(if (s.needsSync || s.embedded?.uncertain == true) R.string.author_leave_unknown else R.string.exam_author_leave)) },
        confirmButton = { Column {
            TextButton({ leave = false; vm.keepDraft(onBack) }, enabled = !s.busy) { Text(stringResource(R.string.author_save_later)) }
            AppDangerTextButton({ leave = false; vm.discardDraft(onBack) }, enabled = !s.busy) { Text(stringResource(R.string.ux_discard_exam_changes)) }
        } },
        dismissButton = { TextButton({ leave = false }) { Text(stringResource(R.string.keep_editing)) } })
    transition?.let { action -> AlertDialog(onDismissRequest = { transition = null },
        title = { Text(stringResource(examActionLabel(action))) },
        text = { Text(stringResource(examActionWarning(action))) },
        confirmButton = { TextButton({ transition = null; vm.transition(action) }, enabled = vm.canTransition(action)) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton({ transition = null }) { Text(stringResource(R.string.cancel)) } }) }
    if (deleting) AlertDialog(onDismissRequest = { if (!s.busy) deleting = false },
        title = { Text(stringResource(R.string.exam_author_delete)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
            Text(stringResource(R.string.exam_author_delete_warning))
            listOf("keep_drafts" to R.string.exam_delete_keep, "delete_drafts" to R.string.exam_delete_drafts,
                "publish_drafts" to R.string.exam_delete_publish).forEach { (mode, label) ->
                OutlinedButton({ deleteMode = mode }, enabled = !s.busy, modifier = Modifier.semantics { selected = deleteMode == mode }) {
                    Text((if (deleteMode == mode) "✓ " else "") + stringResource(label))
                }
            }
        } }, confirmButton = { AppDangerTextButton({ deleting = false; vm.delete(deleteMode) }, enabled = vm.mayDelete(s)) {
            Text(stringResource(R.string.exam_author_delete))
        } }, dismissButton = { TextButton({ deleting = false }, enabled = !s.busy) { Text(stringResource(R.string.cancel)) } })
    removeId?.let { questionId -> AlertDialog(onDismissRequest = { removeId = null },
        title = { Text(stringResource(R.string.exam_author_remove)) },
        text = { Text(stringResource(R.string.exam_author_remove_warning)) },
        confirmButton = { TextButton({ removeId = null; vm.removeQuestion(questionId) }, enabled = enabled && !s.dirty) { Text(stringResource(R.string.exam_author_remove)) } },
        dismissButton = { TextButton({ removeId = null }) { Text(stringResource(R.string.cancel)) } }) }

    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(if (id == null) R.string.exam_author_create else R.string.exam_author_edit))
            AppActionRow() {
                OutlinedButton(back, enabled = !s.busy) { Text(stringResource(R.string.back)) }
                if (s.exam != null || id != null) OutlinedButton(vm::refresh, enabled = !s.busy) { Text(stringResource(R.string.refresh)) }
            }
            if (s.busy) AppLoadingIndicator()
            storageError?.let { Text(stringResource(R.string.author_storage_failed)); ErrorState(throwable = it) }
            if (s.needsSync && s.exam == null) {
                Text(stringResource(R.string.author_write_unknown))
                OutlinedButton(vm::reviewCreation, enabled = !s.busy) { Text(stringResource(R.string.refresh)) }
                if (s.plannedIds != null) OutlinedButton(vm::acknowledgeCreationReview, enabled = !s.busy) { Text(stringResource(R.string.tools_reviewed)) }
            }
            s.error?.let { ErrorState(throwable = it, onRetry = if (!s.busy && (s.exam != null || id != null)) vm::refresh else null,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            s.message?.let { Text(stringResource(it), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            if (s.denied) Text(stringResource(R.string.error_forbidden), color = MaterialTheme.colorScheme.error)
            if (s.needsSync && s.exam != null) Text(stringResource(R.string.exam_author_sync_required))
            if (s.loaded && s.exam?.canEditNow == false) Text(stringResource(R.string.exam_author_frozen))
            if (s.validation) Text(stringResource(R.string.exam_author_invalid), color = MaterialTheme.colorScheme.error)
            if (s.conflict) {
                Text(stringResource(R.string.exam_author_conflict))
                s.latest?.let { latest ->
                    Text(stringResource(R.string.exam_author_server_revision, formatNumber(latest.version)))
                    // Full server values remain inspectable before keeping or replacing the local draft.
                    ExamMetadataFields(latest.form(), latest, false, change = { })
                }
                OutlinedButton({ vm.resolveConflict(false) }, enabled = !s.busy) { Text(stringResource(R.string.use_server_question)) }
                OutlinedButton({ vm.resolveConflict(true) }, enabled = !s.busy) { Text(stringResource(R.string.keep_question_draft)) }
                Text(stringResource(R.string.your_question_draft))
            }
            s.plannedIds?.takeIf { !s.needsSync }?.let { planned ->
                Text(stringResource(R.string.exam_author_planned_order, planned.joinToString(", ") { formatNumber(it) }))
                OutlinedButton(vm::acknowledgePlan, enabled = !s.busy) { Text(stringResource(R.string.exam_author_reviewed)) }
            }
        }
        if (s.loaded) item {
            AppSection(stringResource(R.string.section_exam_details), initiallyExpanded = true) {
                ExamMetadataFields(s.form, s.exam, enabled, vm::edit, vm::canLookUp, vm::openTargets, s.targetNames)
            }
            Button(vm::save, enabled = enabled && (s.exam == null || s.dirty), modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.save))
            }
        }
        s.exam?.let { exam ->
            item {
                Text(stringResource(R.string.exam_author_questions), style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.exam_author_composition_hint))
                if (s.dirty) Text(stringResource(R.string.exam_author_save_first))
                OutlinedButton(vm::openPicker, enabled = enabled && !s.dirty) { Text(stringResource(R.string.exam_author_add_bank)) }
                OutlinedButton(vm::openEmbeddedDraft, enabled = enabled && !s.dirty) { Text(stringResource(R.string.exam_draft_create)) }
                s.questionError?.let { ErrorState(throwable = it, onRetry = if (s.busy) null else vm::retryQuestions) }
                if (exam.questionIds.isEmpty()) Text(stringResource(R.string.exam_author_no_questions))
            }
            items(exam.questionIds, key = { it }) { questionId ->
                val index = exam.questionIds.indexOf(questionId)
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets), verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                        Text(stringResource(R.string.exam_author_question_position, formatNumber(index + 1), formatNumber(questionId)))
                        s.questions[questionId]?.let { AuthorQuestionPreview(it) }
                        AppActionRow() {
                            OutlinedButton({ vm.moveQuestion(questionId, -1) }, enabled = enabled && !s.dirty && index > 0) { Text(stringResource(R.string.move_choice_up)) }
                            OutlinedButton({ vm.moveQuestion(questionId, 1) }, enabled = enabled && !s.dirty && index < exam.questionIds.lastIndex) { Text(stringResource(R.string.move_choice_down)) }
                            OutlinedButton({vm.moveQuestion(questionId,-index)},enabled=enabled && !s.dirty && index>0){Text(stringResource(R.string.exam_move_top))}
                            OutlinedButton({vm.moveQuestion(questionId,exam.questionIds.lastIndex-index)},enabled=enabled && !s.dirty && index<exam.questionIds.lastIndex){Text(stringResource(R.string.exam_move_bottom))}
                            s.questions[questionId]?.takeIf { it.isDraft && user.has("master_exams.drafts_library") &&
                                (user.has("master_exams.manage_any") || it.draftOwner == user.id) }?.let {
                                OutlinedButton({ onDraft(questionId) }, enabled = enabled && !s.dirty && s.embedded == null) { Text(stringResource(R.string.exam_library_edit)) }
                            }
                            OutlinedButton({ removeId = questionId }, enabled = enabled && !s.dirty) { Text(stringResource(R.string.exam_author_remove)) }
                        }
                    }
                }
            }
            item {
                AppSection(stringResource(R.string.exam_lifecycle_title)) {
                    Text(masterStatus(exam.status))
                    listOf("publish", "cancel", "publish-to-bank").forEach { action ->
                        if (action != "publish-to-bank" || user.has("master_exams.publish_to_bank"))
                            OutlinedButton({ transition = action }, enabled = vm.canTransition(action)) { Text(stringResource(examActionLabel(action))) }
                    }
                    val clean = !s.busy && !s.dirty && !s.needsSync && !s.conflict && s.embedded == null && s.targets == null && s.plannedIds == null
                    if (user.has("master_exams.preview") && vm.mayManage(exam)) OutlinedButton({ onPreview(exam.id) }, enabled = clean) { Text(stringResource(R.string.exam_preview_title)) }
                    if (com.mukhtabir.android.data.remote.canViewExamResults(user, exam)) OutlinedButton({ onResults(exam.id) }, enabled = clean) { Text(stringResource(R.string.exam_results_title)) }
                }
            }
            item { OutlinedButton({ deleting = true }, enabled = vm.mayDelete(s)) { Text(stringResource(R.string.exam_author_delete)) } }
        }
    }
    if (s.pickerOpen) ExamBankPicker(s, vm)
    s.targets?.let { ExamTargetPicker(it, s, vm) }
    s.embedded?.let { EmbeddedExamDraftDialog(it, s, vm) }
}

@Composable
private fun ExamMetadataFields(form: ExamForm, exam: com.mukhtabir.android.data.remote.MasterExamAuthorDto?, enabled: Boolean,
    change: ((ExamForm) -> ExamForm) -> Unit,
    canLookUp: ((ExamTargetKind) -> Boolean)? = null, openTargets: ((ExamTargetKind) -> Unit)? = null,
    names: Map<ExamTargetKind, Map<Long, String>> = emptyMap()) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        OutlinedTextField(form.name, { value -> change { it.copy(name = value) } }, enabled = enabled,
            label = { Text(stringResource(R.string.exam_author_name)) }, modifier = Modifier.fillMaxWidth())
        MarkdownField(form.description, { value -> change { it.copy(description = value) } }, stringResource(R.string.exam_author_description), enabled, minLines = 2)
        MarkdownField(form.instructions, { value -> change { it.copy(instructions = value) } }, stringResource(R.string.exam_author_instructions), enabled, minLines = 2)
        OutlinedTextField(form.topic, { value -> change { it.copy(topic = value) } }, enabled = enabled,
            label = { Text(stringResource(R.string.exam_author_topic)) }, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.exam_author_time_zone, ZoneId.systemDefault().id))
        ExamDateTime(form.opens, R.string.exam_author_opens, enabled) { value -> change { it.copy(opens = value) } }
        ExamDateTime(form.closes, R.string.exam_author_closes, enabled) { value -> change { it.copy(closes = value) } }
        OutlinedTextField(form.duration, { value -> change { it.copy(duration = value) } }, enabled = enabled,
            label = { Text(stringResource(R.string.exam_author_duration)) }, modifier = Modifier.fillMaxWidth())
        ExamCheck(form.allDoctors, R.string.exam_author_all_doctors, enabled) { value -> change { it.copy(allDoctors = value) } }
        ExamTargetFields(form, exam, enabled, change, canLookUp, openTargets, names)
        OutlinedTextField(form.easy, { value -> change { it.copy(easy = value) } }, enabled = enabled,
            label = { Text(stringResource(R.string.exam_author_easy)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(form.medium, { value -> change { it.copy(medium = value) } }, enabled = enabled,
            label = { Text(stringResource(R.string.exam_author_medium)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(form.hard, { value -> change { it.copy(hard = value) } }, enabled = enabled,
            label = { Text(stringResource(R.string.exam_author_hard)) }, modifier = Modifier.fillMaxWidth())
        ExamCheck(form.shuffleQuestions, R.string.exam_author_shuffle_questions, enabled) { value -> change { it.copy(shuffleQuestions = value) } }
        ExamCheck(form.shuffleChoices, R.string.exam_author_shuffle_choices, enabled) { value -> change { it.copy(shuffleChoices = value) } }
        ExamCheck(form.makeup, R.string.exam_author_makeup, enabled) { value -> change { it.copy(makeup = value) } }
    }
}

@Composable
private fun ExamCheck(checked: Boolean, label: Int, enabled: Boolean, onChange: (Boolean) -> Unit) {
    // A labeled checkbox row gives TalkBack the same caption as sighted users.
    com.mukhtabir.android.ui.components.AppToggleRow(Modifier.fillMaxWidth()
        .toggleable(value = checked, enabled = enabled, role = Role.Checkbox, onValueChange = onChange)
        .semantics(mergeDescendants = true) {}, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Checkbox(checked, onCheckedChange = null, enabled = enabled)
        Text(stringResource(label))
    }
}

@Composable
private fun ExamDateTime(value: String, label: Int, enabled: Boolean, onChange: (String) -> Unit) {
    val context = LocalContext.current
    var invalid by remember { mutableStateOf(false) }
    OutlinedButton(onClick = {
        val zone = ZoneId.systemDefault()
        val current = runCatching { OffsetDateTime.parse(value).atZoneSameInstant(zone) }.getOrElse { ZonedDateTime.now(zone) }
        DatePickerDialog(context, { _, year, month, day ->
            TimePickerDialog(context, { _, hour, minute ->
                val local = LocalDateTime.of(year, month + 1, day, hour, minute)
                val offsets = zone.rules.getValidOffsets(local)
                invalid = offsets.isEmpty()
                if (offsets.isNotEmpty()) onChange(local.atOffset(offsets.first()).toString())
            }, current.hour, current.minute, android.text.format.DateFormat.is24HourFormat(context)).show()
        }, current.year, current.monthValue - 1, current.dayOfMonth).show()
    }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
        val offset = runCatching { OffsetDateTime.parse(value).atZoneSameInstant(ZoneId.systemDefault()).offset.id }.getOrDefault("")
        Text(stringResource(label) + ": " + if (value.isBlank()) stringResource(R.string.exam_author_choose_date)
            else formatLocalDateTime(value) + " (" + (if (offset == "Z") "UTC" else "UTC$offset") + ")")
    }
    if (invalid) Text(stringResource(R.string.exam_author_invalid_time), color = MaterialTheme.colorScheme.error)
}

@Composable
internal fun AuthorQuestionPreview(question: QuestionDto) {
    val localized = question.localizedForDisplay()
    ClinicalCaseContext(localized.case)
    MarkdownText(localized.question, Modifier.fillMaxWidth())
}

@Composable
private fun ExamBankPicker(s: ExamEditorState, vm: MasterExamEditorViewModel) {
    var query by remember { mutableStateOf(s.query) }
    AlertDialog(onDismissRequest = vm::closePicker,
        title = { Text(stringResource(R.string.exam_author_add_bank)) },
        text = {
            LazyColumn(Modifier.heightIn(max = com.mukhtabir.android.ui.components.AppLayoutTokens.pickerMaxHeight), verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                item {
                    OutlinedTextField(query, { query = it }, enabled = !s.busy, label = { Text(stringResource(R.string.search)) })
                    OutlinedButton({ vm.search(query, 1) }, enabled = !s.busy && !s.pickerBusy) { Text(stringResource(R.string.search)) }
                    if (s.pickerBusy || s.busy) AppLoadingIndicator()
                    s.pickerError?.let { ErrorState(throwable = it, onRetry = if (s.pickerBusy || s.busy) null else ({ vm.search(s.query, s.pickerRequestedPage) })) }
                    s.error?.let { ErrorState(throwable = it, onRetry = if (s.needsSync && !s.busy) vm::refresh else null) }
                    Text(stringResource(R.string.exam_author_selected, formatNumber(s.selected.size)))
                    if (!s.pickerBusy && s.pickerError == null && s.pickerPage.items.isEmpty()) com.mukhtabir.android.ui.components.EmptyState(stringResource(R.string.exam_author_no_questions))
                }
                items(s.pickerPage.items, key = { it.id }) { row ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets)) {
                            AuthorQuestionPreview(row)
                            val already = row.id in s.exam?.questionIds.orEmpty()
                            if (already) Text(stringResource(R.string.exam_author_already_added))
                            ExamCheck(row.id in s.selected || already, R.string.exam_author_select, !already && !s.busy && !s.pickerBusy && s.pickerError == null && !s.needsSync) { vm.toggle(row.id) }
                        }
                    }
                }
                item { AppActionRow() {
                    OutlinedButton({ vm.search(s.query, s.pickerPage.page - 1) }, enabled = !s.busy && !s.needsSync && !s.pickerBusy && s.pickerError == null && s.pickerPage.page > 1) { Text(stringResource(R.string.previous)) }
                    OutlinedButton({ vm.search(s.query, s.pickerPage.page + 1) }, enabled = !s.busy && !s.needsSync && !s.pickerBusy && s.pickerError == null && s.pickerPage.page < s.pickerPage.totalPages) { Text(stringResource(R.string.next)) }
                } }
            }
        }, confirmButton = { TextButton(vm::addSelected, enabled = vm.editable(s) && !s.dirty && !s.pickerBusy && s.pickerError == null && s.selected.isNotEmpty()) { Text(stringResource(R.string.exam_author_add_selected)) } },
        dismissButton = { TextButton(vm::closePicker, enabled = !s.busy) { Text(stringResource(R.string.cancel)) } })
}

internal fun examActionLabel(action: String): Int = when (action) {
    "publish" -> R.string.exam_publish
    "cancel" -> R.string.exam_cancel
    else -> R.string.exam_publish_bank
}
internal fun examActionWarning(action: String): Int = when (action) {
    "publish" -> R.string.exam_publish_warning
    "cancel" -> R.string.exam_cancel_warning
    else -> R.string.exam_bank_warning
}
