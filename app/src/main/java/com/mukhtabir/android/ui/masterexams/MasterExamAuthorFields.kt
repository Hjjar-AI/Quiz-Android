@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.masterexams

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.AppToggleRow
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownField
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatLocalDateTime
import com.mukhtabir.android.util.formatNumber
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

@Composable
internal fun ExamMetadataFields(form: ExamForm, exam: com.mukhtabir.android.data.remote.MasterExamAuthorDto?, enabled: Boolean,
    change: ((ExamForm) -> ExamForm) -> Unit,
    canLookUp: ((ExamTargetKind) -> Boolean)? = null, openTargets: ((ExamTargetKind) -> Unit)? = null,
    names: Map<ExamTargetKind, Map<Long, String>> = emptyMap()) {
    Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppTextField(form.name, { value -> change { it.copy(name = value) } }, enabled = enabled,
            label = { Text(stringResource(R.string.exam_author_name)) }, modifier = Modifier.fillMaxWidth())
        MarkdownField(form.description, { value -> change { it.copy(description = value) } }, stringResource(R.string.exam_author_description), enabled, minLines = 2)
        MarkdownField(form.instructions, { value -> change { it.copy(instructions = value) } }, stringResource(R.string.exam_author_instructions), enabled, minLines = 2)
        AppTextField(form.topic, { value -> change { it.copy(topic = value) } }, enabled = enabled,
            label = { Text(stringResource(R.string.exam_author_topic)) }, modifier = Modifier.fillMaxWidth())
        Text(stringResource(R.string.exam_author_time_zone, ZoneId.systemDefault().id))
        ExamDateTime(form.opens, R.string.exam_author_opens, enabled) { value -> change { it.copy(opens = value) } }
        ExamDateTime(form.closes, R.string.exam_author_closes, enabled) { value -> change { it.copy(closes = value) } }
        AppTextField(form.duration, { value -> change { it.copy(duration = value) } }, enabled = enabled,
            label = { Text(stringResource(R.string.exam_author_duration)) }, modifier = Modifier.fillMaxWidth())
        ExamCheck(form.allDoctors, R.string.exam_author_all_doctors, enabled) { value -> change { it.copy(allDoctors = value) } }
        ExamTargetFields(form, exam, enabled, change, canLookUp, openTargets, names)
        AppTextField(form.easy, { value -> change { it.copy(easy = value) } }, enabled = enabled,
            label = { Text(stringResource(R.string.exam_author_easy)) }, modifier = Modifier.fillMaxWidth())
        AppTextField(form.medium, { value -> change { it.copy(medium = value) } }, enabled = enabled,
            label = { Text(stringResource(R.string.exam_author_medium)) }, modifier = Modifier.fillMaxWidth())
        AppTextField(form.hard, { value -> change { it.copy(hard = value) } }, enabled = enabled,
            label = { Text(stringResource(R.string.exam_author_hard)) }, modifier = Modifier.fillMaxWidth())
        ExamCheck(form.shuffleQuestions, R.string.exam_author_shuffle_questions, enabled) { value -> change { it.copy(shuffleQuestions = value) } }
        ExamCheck(form.shuffleChoices, R.string.exam_author_shuffle_choices, enabled) { value -> change { it.copy(shuffleChoices = value) } }
        ExamCheck(form.makeup, R.string.exam_author_makeup, enabled) { value -> change { it.copy(makeup = value) } }
    }
}

@Composable
private fun ExamCheck(checked: Boolean, label: Int, enabled: Boolean, onChange: (Boolean) -> Unit) {
    // A labeled checkbox row gives TalkBack the same caption as sighted users.
    AppToggleRow(Modifier.fillMaxWidth()
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
    AppOutlinedButton(onClick = {
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
    if (invalid) AppStatus(stringResource(R.string.exam_author_invalid_time))
}

@Composable
internal fun ExamBankPicker(s: ExamEditorState, vm: MasterExamEditorViewModel) {
    var query by remember { mutableStateOf(s.query) }
    AppConfirmationDialog(onDismissRequest = vm::closePicker,
        title = { Text(stringResource(R.string.exam_author_add_bank)) },
        text = {
            LazyColumn(Modifier.heightIn(max = AppLayoutTokens.pickerMaxHeight), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                item {
                    AppTextField(query, { query = it }, enabled = !s.busy, label = { Text(stringResource(R.string.search)) })
                    AppOutlinedButton({ vm.search(query, 1) }, enabled = !s.busy && !s.pickerBusy) { Text(stringResource(R.string.search)) }
                    if (s.pickerBusy || s.busy) AppLoadingIndicator()
                    s.pickerError?.let { ErrorState(throwable = it, onRetry = if (s.pickerBusy || s.busy) null else ({ vm.search(s.query, s.pickerRequestedPage) })) }
                    s.error?.let { ErrorState(throwable = it, onRetry = if (s.needsSync && !s.busy) vm::refresh else null) }
                    Text(stringResource(R.string.exam_author_selected, formatNumber(s.selected.size)))
                    if (!s.pickerBusy && s.pickerError == null && s.pickerPage.items.isEmpty()) EmptyState(stringResource(R.string.exam_author_no_questions))
                }
                items(s.pickerPage.items, key = { it.id }) { row ->
                    AppCard(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(AppLayoutTokens.denseCardInsets)) {
                            AuthorQuestionPreview(row)
                            val already = row.id in s.exam?.questionIds.orEmpty()
                            if (already) Text(stringResource(R.string.exam_author_already_added))
                            ExamCheck(row.id in s.selected || already, R.string.exam_author_select, !already && !s.busy && !s.pickerBusy && s.pickerError == null && !s.needsSync) { vm.toggle(row.id) }
                        }
                    }
                }
                item { AppActionRow() {
                    AppOutlinedButton({ vm.search(s.query, s.pickerPage.page - 1) }, enabled = !s.busy && !s.needsSync && !s.pickerBusy && s.pickerError == null && s.pickerPage.page > 1) { Text(stringResource(R.string.previous)) }
                    AppOutlinedButton({ vm.search(s.query, s.pickerPage.page + 1) }, enabled = !s.busy && !s.needsSync && !s.pickerBusy && s.pickerError == null && s.pickerPage.page < s.pickerPage.totalPages) { Text(stringResource(R.string.next)) }
                } }
            }
        }, confirmButton = { AppTextButton(vm::addSelected, enabled = vm.editable(s) && !s.dirty && !s.pickerBusy && s.pickerError == null && s.selected.isNotEmpty()) { Text(stringResource(R.string.exam_author_add_selected)) } },
        dismissButton = { AppTextButton(vm::closePicker, enabled = !s.busy) { Text(stringResource(R.string.cancel)) } })
}
