package com.mukhtabir.android.ui.masterexams

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.MasterExamAuthorDto
import com.mukhtabir.android.ui.components.AppCheckboxRow
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPagination
import com.mukhtabir.android.ui.components.AppSearchPicker
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.util.formatNumber

internal enum class ExamTargetKind(val label: Int, val pickerLabel: Int) {
    GROUPS(R.string.exam_author_group_ids, R.string.exam_target_groups),
    USERS(R.string.exam_author_user_ids, R.string.exam_target_users),
    COAUTHORS(R.string.exam_author_coauthors, R.string.exam_target_coauthors);

    fun ids(form: ExamForm): String = when (this) {
        GROUPS -> form.groups
        USERS -> form.users
        COAUTHORS -> form.coAuthors
    }
    fun replace(form: ExamForm, ids: String): ExamForm = when (this) {
        GROUPS -> form.copy(groups = ids)
        USERS -> form.copy(users = ids)
        COAUTHORS -> form.copy(coAuthors = ids)
    }
}

internal data class ExamTargetRow(val id: Long, val name: String)
internal data class ExamTargetPickerState(
    val kind: ExamTargetKind,
    val selected: Set<Long> = emptySet(),
    val query: String = "", val requestedPage: Int = 1, val page: Int = 1, val totalPages: Int = 1,
    val rows: List<ExamTargetRow> = emptyList(), val groups: List<ExamTargetRow>? = null,
    val busy: Boolean = false, val error: Throwable? = null,
)

private fun targetNames(kind: ExamTargetKind, exam: MasterExamAuthorDto?, cache: Map<ExamTargetKind, Map<Long, String>>): Map<Long, String> {
    val saved = when (kind) {
        ExamTargetKind.GROUPS -> exam?.audienceGroups.orEmpty().associate { it.id to it.name }
        ExamTargetKind.USERS -> exam?.audienceUsers.orEmpty().associate { it.id to it.fullName.ifBlank { it.username } }
        ExamTargetKind.COAUTHORS -> exam?.coAttendings.orEmpty().associate { it.id to it.fullName.ifBlank { it.username } }
    }
    return saved + cache[kind].orEmpty()
}

private fun targetLabel(id: Long, names: Map<Long, String>): String =
    names[id]?.let { "$it (${formatNumber(id)})" } ?: formatNumber(id)

@Composable
internal fun ExamTargetFields(form: ExamForm, exam: MasterExamAuthorDto?, enabled: Boolean,
    change: ((ExamForm) -> ExamForm) -> Unit, canLookUp: ((ExamTargetKind) -> Boolean)?,
    open: ((ExamTargetKind) -> Unit)?, cache: Map<ExamTargetKind, Map<Long, String>>) {
    Text(stringResource(R.string.exam_author_ids_hint))
    ExamTargetKind.entries.forEach { kind ->
        AppTextField(kind.ids(form), { value -> change { kind.replace(it, value) } }, enabled = enabled,
            label = { Text(stringResource(kind.label)) }, modifier = Modifier.fillMaxWidth())
        val ids = parseExamTargetIds(kind.ids(form)).orEmpty()
        val names = targetNames(kind, exam, cache)
        if (ids.isNotEmpty()) Text(ids.joinToString(" · ") { targetLabel(it, names) })
        if (open != null) {
            if (canLookUp?.invoke(kind) == true) AppOutlinedButton({ open(kind) }, enabled = enabled) {
                Text(stringResource(kind.pickerLabel))
            } else Text(stringResource(if (kind == ExamTargetKind.GROUPS) R.string.exam_target_groups_permission else R.string.exam_target_users_permission))
        }
    }
}

@Composable
internal fun ExamTargetPicker(p: ExamTargetPickerState, s: ExamEditorState, vm: MasterExamEditorViewModel) {
    var query by remember(p.kind) { mutableStateOf(p.query) }
    val enabled = vm.editable(s) && vm.canLookUp(p.kind) && !p.busy && p.error == null
    val names = targetNames(p.kind, s.exam, s.targetNames)
    AppSearchPicker(stringResource(p.kind.pickerLabel), vm::closeTargets,
        query, { query = it }, onSearch = { vm.searchTargets(query, 1) },
        queryEnabled = !s.busy, searchEnabled = vm.editable(s) && !p.busy,
        loading = p.busy, error = p.error,
        onRetry = if (p.busy || !vm.editable(s)) null else ({ vm.searchTargets(p.query, p.requestedPage) }),
        beforeSearch = {
            Text(stringResource(R.string.exam_target_staged))
            if (p.kind != ExamTargetKind.GROUPS) Text(stringResource(R.string.exam_target_member_filter))
        }, status = {
            Text(stringResource(R.string.exam_author_selected, formatNumber(p.selected.size)))
            Text(stringResource(R.string.exam_target_keep_missing))
        },
        confirmButton = { AppTextButton(vm::applyTargets, enabled = enabled) { Text(stringResource(R.string.exam_target_apply)) } },
        dismissButton = { AppTextButton(vm::closeTargets) { Text(stringResource(R.string.cancel)) } }) {
        // Selections absent from a lookup remain visible and explicitly removable.
        items(p.selected.sorted(), key = { "selected-$it" }) { id ->
            Column {
                Text(targetLabel(id, names))
                AppTextButton({ vm.removeTarget(id) }, enabled = enabled) {
                    Text(stringResource(R.string.exam_target_remove, targetLabel(id, names)))
                }
            }
        }
        item {
            Text(stringResource(R.string.exam_target_matches))
            if (!p.busy && p.error == null && p.rows.isEmpty()) EmptyState(stringResource(R.string.exam_target_empty))
        }
        items(p.rows, key = { "match-${it.id}" }) { row ->
            AppCheckboxRow(targetLabel(row.id, names), row.id in p.selected,
                onCheckedChange = { vm.toggleTarget(row.id) }, enabled = enabled)
        }
        item {
            AppPagination(p.page, p.totalPages,
                onPrevious = { vm.searchTargets(p.query, p.page - 1) },
                onNext = { vm.searchTargets(p.query, p.page + 1) },
                previousEnabled = enabled, nextEnabled = enabled,
                statusLabel = stringResource(R.string.exam_target_page, formatNumber(p.page), formatNumber(p.totalPages)))
        }
    }
}
