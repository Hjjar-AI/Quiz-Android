package com.mukhtabir.android.ui.knowledge

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.KnowledgeDetailDto
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.RefreshOnResume

@Composable
fun KnowledgeDetailRoute(id: Long,userId: Long,service: QuestionService,canStart: Boolean,maxQuestions: Int,
    onBack: () -> Unit,onOpenQuestion: (Long) -> Unit,onReview: (List<Long>,String) -> Unit) {
    val vm: ContentReader<KnowledgeDetailDto> = viewModel(key="knowledge-detail-$userId-$id",
        factory=ContentReader.Factory { service.knowledgeDetail(id) })
    // The detail's question_count includes drafts. Use only public IDs returned by the mastery map.
    val links: ContentReader<List<Long>> = viewModel(key="knowledge-links-$userId-$id",
        factory=ContentReader.Factory { service.knowledgeMap().items.firstOrNull { it.id==id }?.questionIds.orEmpty().distinct() })
    val state by vm.state.collectAsState()
    val linked by links.state.collectAsState()
    RefreshOnResume { vm.refresh();links.refresh() }
    val data=state.data?.localizedForDisplay()
    val ids=linked.data.orEmpty()
    val limit=maxQuestions.coerceAtLeast(1)
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.explore_knowledge_detail))
            TextButton(onBack){Text(stringResource(R.string.back))}
            OutlinedButton({vm.refresh();links.refresh()},enabled=!state.loading && !linked.loading){Text(stringResource(R.string.refresh))}
            if(state.loading) AppLoadingIndicator()
            state.error?.let { ErrorState(throwable=it,onRetry=vm::refresh) }
            if(state.error!=null && data!=null) Text(stringResource(R.string.explore_stale))
        }
        data?.let { knowledge ->
            item {
                ContentHeading(knowledge.title)
                knowledge.categoryName?.takeIf(String::isNotBlank)?.let { Text(it) }
                if(knowledge.tags.isNotEmpty()) Text(knowledge.tags.joinToString(" · "))
            }
            if(knowledge.learningObjective.isNotBlank()) item {
                AppSection(stringResource(R.string.explore_objective), initiallyExpanded = true) { MarkdownText(knowledge.learningObjective) }
            }
            if(knowledge.canonicalAnswer.isNotBlank()) item {
                AppSection(stringResource(R.string.explore_reference_answer), initiallyExpanded = true) { MarkdownText(knowledge.canonicalAnswer) }
            }
            if(knowledge.keyFacts.isNotEmpty()) {
                item { AppSection(stringResource(R.string.explore_key_facts), initiallyExpanded = true) {
                    knowledge.keyFacts.forEach { MarkdownText(it) }
                } }
            }
            if(knowledge.misconceptions.isNotEmpty()) {
                item { AppSection(stringResource(R.string.explore_misconceptions)) {
                    knowledge.misconceptions.forEach { MarkdownText(it) }
                } }
            }
            knowledge.sourceDocument?.takeIf(String::isNotBlank)?.let { document -> item {
                ContentHeading(stringResource(R.string.editor_source_document));Text(document)
            } }
            knowledge.sourcePage?.let { page -> item { Text(stringResource(R.string.question_page,page)) } }
            item {
                ContentHeading(stringResource(R.string.explore_linked_questions))
                Text(stringResource(R.string.explore_public_links))
                if(linked.loading) AppLoadingIndicator()
                linked.error?.let { ErrorState(throwable=it,onRetry=links::refresh) }
                if(linked.error!=null && linked.data!=null) Text(stringResource(R.string.explore_stale))
                if(linked.data!=null && ids.isEmpty() && linked.error==null && !linked.loading) Text(stringResource(R.string.no_questions_found))
                if(canStart && ids.isNotEmpty()) {
                    OutlinedButton({onReview(ids.take(limit),knowledge.title)},enabled=!state.loading && state.error==null && !linked.loading && linked.error==null) {
                        Text(stringResource(R.string.review))
                    }
                    if(ids.size>limit) Text(stringResource(R.string.explore_practice_limit,limit))
                }
            }
            items(ids,key={it}) { questionId ->
                OutlinedButton({onOpenQuestion(questionId)},enabled=!linked.loading && linked.error==null && state.error==null,
                    modifier=Modifier.fillMaxWidth()) { Text(stringResource(R.string.explore_question_id,questionId)) }
            }
        }
    }
}

@Composable
internal fun ContentHeading(text: String) {
    Text(text,style=MaterialTheme.typography.titleLarge,modifier=Modifier.semantics { heading() })
}
