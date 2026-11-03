package com.mukhtabir.android.ui.questions

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppSearchPicker
import com.mukhtabir.android.ui.components.AppSectionHeading
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.MarkdownField
import com.mukhtabir.android.ui.components.MarkdownText

@Composable
internal fun QuestionAssociationFields(s: QuestionEditorState, vm: QuestionEditorViewModel, enabled: Boolean) {
    val associationError = if (s.validationError == QuestionEditorValidationError.INVALID_ASSOCIATION) stringResource(R.string.editor_association_invalid) else null
    var detach by rememberSaveable { mutableStateOf(false) }
    if(detach) AppConfirmationDialog(
        onDismissRequest={detach=false},
        title={Text(stringResource(R.string.editor_detach_case))},
        text={Text(stringResource(R.string.editor_detach_case_confirm))},
        confirmButton={AppTextButton({vm.detachCase();detach=false},enabled=enabled){Text(stringResource(R.string.confirm))}},
        dismissButton={AppTextButton({detach=false}){Text(stringResource(R.string.cancel))}},
    )
    AppSectionHeading(stringResource(R.string.editor_case))
    AppOutlinedButton({vm.openLookup("case")},enabled=enabled){Text(stringResource(R.string.editor_choose_case))}
    AppTextField(s.caseKey,{vm.field("caseKey",it)},enabled=enabled,singleLine=true,
        label={Text(stringResource(R.string.editor_case_key))},isError=associationError!=null, supportingText=associationError?.let { message -> { Text(message) } },modifier=Modifier.fillMaxWidth())
    Text(stringResource(R.string.editor_case_stem_hint))
    if(s.caseStemLocked) {
        MarkdownText(s.caseStem)
    } else MarkdownField(s.caseStem,{vm.field("caseStem",it)},enabled=enabled,
        label=stringResource(R.string.editor_case_stem),error=associationError,minLines=2,modifier=Modifier.fillMaxWidth())
    AppTextField(s.caseOrder,{vm.field("caseOrder",it)},enabled=enabled && s.caseKey.isNotBlank(),singleLine=true,
        label={Text(stringResource(R.string.editor_case_order))},isError=associationError!=null, supportingText=associationError?.let { message -> { Text(message) } },modifier=Modifier.fillMaxWidth())
    AppOutlinedButton({detach=true},enabled=enabled && s.caseKey.isNotBlank()){Text(stringResource(R.string.editor_detach_case))}
    AppSectionHeading(stringResource(R.string.editor_knowledge))
    s.knowledgeObject?.let { id ->
        Text(if(s.knowledgeTitle.isBlank()) stringResource(R.string.editor_knowledge_id,id) else s.knowledgeTitle)
        if(s.knowledgeObjective.isNotBlank()) MarkdownText(s.knowledgeObjective)
    } ?: Text(stringResource(R.string.none))
    AppOutlinedButton({vm.openLookup("knowledge")},enabled=enabled){Text(stringResource(R.string.editor_choose_knowledge))}
    AppOutlinedButton(vm::clearKnowledge,enabled=enabled && s.knowledgeObject!=null){Text(stringResource(R.string.editor_clear_knowledge))}
    if(vm.canCreateKnowledge) AppOutlinedButton(vm::openKnowledgeCreator,enabled=enabled) {
        Text(stringResource(R.string.knowledge_create_title))
    }

}

@Composable
internal fun QuestionAssociationPicker(s: QuestionEditorState, vm: QuestionEditorViewModel) {
    val kind=s.lookupKind ?: return
    var query by rememberSaveable(kind) { mutableStateOf("") }
    AppSearchPicker(stringResource(if (kind == "case") R.string.editor_choose_case else R.string.editor_choose_knowledge),
        vm::closeLookup, query, { query = it }, onSearch = { vm.searchLookup(query) },
        queryEnabled = true, searchEnabled = !s.lookupBusy, loading = s.lookupBusy,
        error = s.lookupError, onRetry = { vm.searchLookup(s.lookupQuery) },
        status = {
            if (!s.lookupBusy && s.lookupError == null) {
                val shown = if (kind == "case") s.caseResults.size else s.knowledgeResults.size
                Text(stringResource(R.string.editor_lookup_count, com.mukhtabir.android.util.formatNumber(shown),
                    com.mukhtabir.android.util.formatNumber(s.lookupTotal)))
            }
        }, confirmButton = { AppTextButton(vm::closeLookup) { Text(stringResource(R.string.cancel)) } }) {
        if (!s.lookupBusy && s.lookupError == null) {
            if (kind == "case") items(s.caseResults) { row ->
                AppOutlinedButton({ vm.selectCase(row) }, enabled = !row.key.isNullOrBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(listOfNotNull(row.title?.takeIf(String::isNotBlank), row.key).joinToString(" · "))
                }
            } else items(s.knowledgeResults, key = { it.id }) { row ->
                AppOutlinedButton({ vm.selectKnowledge(row) }, modifier = Modifier.fillMaxWidth()) { Text(row.title) }
            }
        }
    }
}
