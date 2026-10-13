package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownField
import com.mukhtabir.android.ui.components.MarkdownText

@Composable
internal fun QuestionAssociationFields(s: QuestionEditorState, vm: QuestionEditorViewModel, enabled: Boolean) {
    val associationError = if (s.validationError == QuestionEditorValidationError.INVALID_ASSOCIATION) stringResource(R.string.editor_association_invalid) else null
    var detach by rememberSaveable { mutableStateOf(false) }
    if(detach) AlertDialog(
        onDismissRequest={detach=false},
        title={Text(stringResource(R.string.editor_detach_case))},
        text={Text(stringResource(R.string.editor_detach_case_confirm))},
        confirmButton={TextButton({vm.detachCase();detach=false},enabled=enabled){Text(stringResource(R.string.confirm))}},
        dismissButton={TextButton({detach=false}){Text(stringResource(R.string.cancel))}},
    )
    Text(stringResource(R.string.editor_case),style=MaterialTheme.typography.titleLarge)
    OutlinedButton({vm.openLookup("case")},enabled=enabled){Text(stringResource(R.string.editor_choose_case))}
    OutlinedTextField(s.caseKey,{vm.field("caseKey",it)},enabled=enabled,singleLine=true,
        label={Text(stringResource(R.string.editor_case_key))},isError=associationError!=null, supportingText=associationError?.let { message -> { Text(message) } },modifier=Modifier.fillMaxWidth())
    Text(stringResource(R.string.editor_case_stem_hint))
    if(s.caseStemLocked) {
        MarkdownText(s.caseStem)
    } else MarkdownField(s.caseStem,{vm.field("caseStem",it)},enabled=enabled,
        label=stringResource(R.string.editor_case_stem),error=associationError,minLines=2,modifier=Modifier.fillMaxWidth())
    OutlinedTextField(s.caseOrder,{vm.field("caseOrder",it)},enabled=enabled && s.caseKey.isNotBlank(),singleLine=true,
        label={Text(stringResource(R.string.editor_case_order))},isError=associationError!=null, supportingText=associationError?.let { message -> { Text(message) } },modifier=Modifier.fillMaxWidth())
    OutlinedButton({detach=true},enabled=enabled && s.caseKey.isNotBlank()){Text(stringResource(R.string.editor_detach_case))}
    Text(stringResource(R.string.editor_knowledge),style=MaterialTheme.typography.titleLarge)
    s.knowledgeObject?.let { id ->
        Text(if(s.knowledgeTitle.isBlank()) stringResource(R.string.editor_knowledge_id,id) else s.knowledgeTitle)
        if(s.knowledgeObjective.isNotBlank()) MarkdownText(s.knowledgeObjective)
    } ?: Text(stringResource(R.string.none))
    OutlinedButton({vm.openLookup("knowledge")},enabled=enabled){Text(stringResource(R.string.editor_choose_knowledge))}
    OutlinedButton(vm::clearKnowledge,enabled=enabled && s.knowledgeObject!=null){Text(stringResource(R.string.editor_clear_knowledge))}
    if(vm.canCreateKnowledge) OutlinedButton(vm::openKnowledgeCreator,enabled=enabled) {
        Text(stringResource(R.string.knowledge_create_title))
    }

}

@Composable
internal fun QuestionAssociationPicker(s: QuestionEditorState, vm: QuestionEditorViewModel) {
    val kind=s.lookupKind ?: return
    var query by rememberSaveable(kind) { mutableStateOf("") }
    AlertDialog(
        onDismissRequest=vm::closeLookup,
        title={Text(stringResource(if(kind=="case") R.string.editor_choose_case else R.string.editor_choose_knowledge))},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
            OutlinedTextField(query,{query=it},singleLine=true,label={Text(stringResource(R.string.search))},modifier=Modifier.fillMaxWidth())
            OutlinedButton({vm.searchLookup(query)},enabled=!s.lookupBusy){Text(stringResource(R.string.search))}
            if(s.lookupBusy) AppLoadingIndicator()
            s.lookupError?.let { ErrorState(throwable=it,onRetry={vm.searchLookup(s.lookupQuery)}) }
            if(!s.lookupBusy && s.lookupError==null) {
                val shown=if(kind=="case") s.caseResults.size else s.knowledgeResults.size
                Text(stringResource(R.string.editor_lookup_count,shown,s.lookupTotal))
                if(kind=="case") s.caseResults.forEach { item ->
                    OutlinedButton({vm.selectCase(item)},enabled=!item.key.isNullOrBlank(),modifier=Modifier.fillMaxWidth()) {
                        Text(listOfNotNull(item.title?.takeIf(String::isNotBlank),item.key).joinToString(" · "))
                    }
                } else s.knowledgeResults.forEach { item ->
                    OutlinedButton({vm.selectKnowledge(item)},modifier=Modifier.fillMaxWidth()) { Text(item.title) }
                }
            }
        }},
        confirmButton={TextButton(vm::closeLookup){Text(stringResource(R.string.cancel))}},
    )
}
