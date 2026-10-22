package com.mukhtabir.android.ui.questions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.KnowledgeAssociationDto
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppStatusKind
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownText

// Kept in the editor's account-scoped ViewModel; never persisted to SavedStateHandle.
data class KnowledgeCreationState(
    val title: String = "",
    val objective: String = "",
    val answer: String = "",
    val facts: String = "",
    val validation: Int? = null,
    val error: Throwable? = null,
    val needsReview: Boolean = false,
    val reviewed: Boolean = false,
    val matches: List<KnowledgeAssociationDto> = emptyList(),
    val matchCount: Int = 0,
) {
    val hasDraft: Boolean get() = listOf(title,objective,answer,facts).any(String::isNotEmpty)
}

@Composable
internal fun QuestionKnowledgeCreator(s: QuestionEditorState,vm: QuestionEditorViewModel) {
    val draft=s.knowledgeCreation ?: return
    var discard by rememberSaveable { mutableStateOf(false) }
    val close={if(!s.loading) { if(draft.hasDraft) discard=true else vm.closeKnowledgeCreator() }}
    AppConfirmationDialog(
        onDismissRequest=close,
        title={Text(stringResource(R.string.knowledge_create_title))},
        text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            Text(stringResource(R.string.knowledge_create_hint))
            AppTextField(draft.title,{vm.knowledgeCreationField("title",it)},enabled=!s.loading,
                label={Text(stringResource(R.string.knowledge_create_name))},singleLine=true,modifier=Modifier.fillMaxWidth())
            AppTextField(draft.objective,{vm.knowledgeCreationField("objective",it)},enabled=!s.loading,
                label={Text(stringResource(R.string.explore_objective))},minLines=2,modifier=Modifier.fillMaxWidth())
            AppTextField(draft.answer,{vm.knowledgeCreationField("answer",it)},enabled=!s.loading,
                label={Text(stringResource(R.string.explore_reference_answer))},minLines=2,modifier=Modifier.fillMaxWidth())
            AppTextField(draft.facts,{vm.knowledgeCreationField("facts",it)},enabled=!s.loading,
                label={Text(stringResource(R.string.knowledge_create_facts))},minLines=2,modifier=Modifier.fillMaxWidth())
            if(s.loading) AppLoadingIndicator()
            draft.validation?.let { AppStatus(stringResource(it)) }
            draft.error?.let { ErrorState(throwable=it) }
            if(draft.needsReview) AppStatus(stringResource(R.string.knowledge_create_review_required), kind = AppStatusKind.WARNING)
            AppOutlinedButton(vm::reviewKnowledgeCreation,enabled=!s.loading && draft.title.isNotBlank()) {
                Text(stringResource(R.string.knowledge_create_review))
            }
            if(draft.reviewed) {
                Text(stringResource(R.string.editor_lookup_count, com.mukhtabir.android.util.formatNumber(draft.matches.take(20).size), com.mukhtabir.android.util.formatNumber(draft.matchCount)))
                Text(stringResource(R.string.knowledge_create_review_hint))
                draft.matches.take(20).forEach { match ->
                    Text(match.title,style=MaterialTheme.typography.titleMedium)
                    if(match.learningObjective.isNotBlank()) MarkdownText(match.learningObjective)
                    AppOutlinedButton({vm.attachReviewedKnowledge(match.id)},enabled=!s.loading && !draft.needsReview) {
                        Text(stringResource(R.string.knowledge_create_use_existing))
                    }
                }
            }
        }},
        confirmButton={AppTextButton(vm::createKnowledge,enabled=!s.loading && !draft.needsReview && vm.canCreateKnowledge) {
            Text(stringResource(R.string.knowledge_create_attach))
        }},
        dismissButton={AppTextButton(close,enabled=!s.loading){Text(stringResource(R.string.cancel))}},
    )
    if(discard) AppConfirmationDialog(
        onDismissRequest={discard=false},
        title={Text(stringResource(R.string.knowledge_create_discard_title))},
        text={Text(stringResource(R.string.knowledge_create_discard_message))},
        confirmButton={AppDangerTextButton({discard=false;vm.closeKnowledgeCreator()},enabled=!s.loading){Text(stringResource(R.string.ux_discard_knowledge_draft))}},
        dismissButton={AppTextButton({discard=false}){Text(stringResource(R.string.keep_editing))}},
    )
}
