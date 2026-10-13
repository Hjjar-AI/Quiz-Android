package com.mukhtabir.android.ui.knowledge

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppPagination

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mukhtabir.android.ui.components.AppCard as Card
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import com.mukhtabir.android.core.LearnerSavedState
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ClinicalCaseDetailDto
import com.mukhtabir.android.data.remote.ClinicalCaseListDto
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.data.remote.localizedForDisplay
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.MarkdownText
import com.mukhtabir.android.ui.components.RefreshOnResume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal data class CaseBrowserState(val draft: String = "",val query: String = "",
    val requestedQuery: String = "",val requestedPage: Int = 1,val data: ClinicalCaseListDto? = null,val loading: Boolean = false,val error: Throwable? = null)

internal class CaseBrowserViewModel(private val service: QuestionService,private val pageSize: Int,userId: Long,handle: SavedStateHandle): ViewModel() {
    private val saved=LearnerSavedState(handle,userId,"case_browser")
    private val _state=MutableStateFlow(CaseBrowserState(draft=saved.draftFilter().orEmpty(),query=saved.filter().orEmpty()))
    val state=_state.asStateFlow()
    init { refresh() }
    fun draft(value: String) { _state.update { it.copy(draft=value) };saved.saveDraftFilter(value) }
    fun search() = load(_state.value.draft.trim(),1)
    fun refresh() = load(_state.value.query,_state.value.data?.page ?: 1)
    fun retry() = load(_state.value.requestedQuery,_state.value.requestedPage)
    fun page(value: Int) { val data=_state.value.data ?: return;load(_state.value.query,value.coerceIn(1,data.totalPages.coerceAtLeast(1))) }
    private fun load(query: String,page: Int) {
        if(_state.value.loading) return
        _state.update { it.copy(requestedQuery=query,requestedPage=page,loading=true,error=null) }
        viewModelScope.launch {
            try {
                var data=service.cases(query,page,pageSize)
                if(data.page>data.totalPages) data=service.cases(query,data.totalPages.coerceAtLeast(1),pageSize)
                coroutineContext.ensureActive()
                saved.saveFilter(query)
                _state.update { it.copy(query=query,data=data,loading=false) }
            } catch(failure: CancellationException) { throw failure }
            catch(failure: Exception) {
                coroutineContext.ensureActive()
                _state.update { it.copy(loading=false,error=failure) }
            }
        }
    }
    class Factory(private val service: QuestionService,private val userId: Long,private val pageSize: Int): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T: ViewModel> create(modelClass: Class<T>,extras: CreationExtras): T = CaseBrowserViewModel(service,pageSize.coerceIn(1,500),userId,extras.createSavedStateHandle()) as T
    }
}

@Composable
fun ClinicalCasesRoute(userId: Long,service: QuestionService,pageSize: Int,onBack: () -> Unit,onOpen: (String) -> Unit) {
    val vm: CaseBrowserViewModel=viewModel(key="case-browser-$userId-$pageSize",factory=CaseBrowserViewModel.Factory(service,userId,pageSize))
    val state by vm.state.collectAsState()
    RefreshOnResume(vm::refresh)
    com.mukhtabir.android.ui.components.PullToRefresh(state.loading,vm::refresh) {
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.explore_cases))
            TextButton(onBack){Text(stringResource(R.string.back))}
            OutlinedTextField(state.draft,vm::draft,label={Text(stringResource(R.string.search))},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedButton(vm::search,enabled=!state.loading){Text(stringResource(R.string.search))}
            OutlinedButton(vm::refresh,enabled=!state.loading){Text(stringResource(R.string.refresh))}
            if(state.loading) AppLoadingIndicator()
            state.error?.let { ErrorState(throwable=it,onRetry=if(state.loading) null else vm::retry) }
            if(state.error!=null && state.data!=null) Text(stringResource(R.string.explore_stale))
            state.data?.let { data ->
                Text(stringResource(R.string.editor_lookup_count,data.items.size,data.total))
                AppPagination(data.page, data.totalPages, onPrevious = {vm.page(data.page-1)}, onNext = {vm.page(data.page+1)}, previousEnabled = !state.loading, nextEnabled = !state.loading)
                if(data.items.isEmpty() && !state.loading && state.error==null) Text(stringResource(R.string.no_items))
            }
        }
        items(state.data?.items.orEmpty(),key={it.key ?: it.id.toString()}) { case ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                    Text(case.title?.takeIf(String::isNotBlank) ?: case.key.orEmpty())
                    Text(case.key.orEmpty())
                    OutlinedButton({case.key?.let(onOpen)},enabled=!case.key.isNullOrBlank() && !state.loading && state.error==null) {
                        Text(stringResource(R.string.explore_open_case))
                    }
                }
            }
        }
    }
    }
}

@Composable
fun ClinicalCaseDetailRoute(key: String,userId: Long,service: QuestionService,canStart: Boolean,maxQuestions: Int,
    canEditStemAny: Boolean,canEditStemOwn: Boolean,onEditStem: () -> Unit,
    onBack: () -> Unit,onOpenQuestion: (Long) -> Unit,onReview: (List<Long>,String) -> Unit) {
    val vm: ContentReader<ClinicalCaseDetailDto> = viewModel(key="case-detail-$userId-$key",
        factory=ContentReader.Factory { service.caseDetail(key) })
    val state by vm.state.collectAsState()
    RefreshOnResume(vm::refresh)
    val data=state.data?.localizedForDisplay()
    val questions=data?.questions.orEmpty().distinctBy { it.id }
        .sortedWith(compareBy({it.caseOrder ?: Int.MAX_VALUE},{it.id}))
    val label=data?.title?.takeIf(String::isNotBlank) ?: key
    val limit=maxQuestions.coerceAtLeast(1)
    com.mukhtabir.android.ui.components.PullToRefresh(state.loading,vm::refresh) {
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.explore_case_detail))
            TextButton(onBack){Text(stringResource(R.string.back))}
            OutlinedButton(vm::refresh,enabled=!state.loading){Text(stringResource(R.string.refresh))}
            if(state.loading) AppLoadingIndicator()
            state.error?.let { ErrorState(throwable=it,onRetry=if(state.loading) null else vm::refresh) }
            if(state.error!=null && data!=null) Text(stringResource(R.string.explore_stale))
        }
        data?.let { case ->
            item {
                ContentHeading(label)
                Text(case.key)
                case.stem?.takeIf(String::isNotBlank)?.let { MarkdownText(it) }
                if(case.mayEditStem(userId,canEditStemAny,canEditStemOwn)) {
                    OutlinedButton(onEditStem,enabled=!state.loading && state.error==null) {
                        Text(stringResource(R.string.case_stem_edit))
                    }
                }

                ContentHeading(stringResource(R.string.explore_linked_questions))
                Text(stringResource(R.string.explore_visible_questions,questions.size))
                if(questions.isEmpty() && state.error==null && !state.loading) Text(stringResource(R.string.no_questions_found))
                if(canStart && questions.isNotEmpty()) {
                    OutlinedButton({onReview(questions.take(limit).map { it.id },label)},enabled=!state.loading && state.error==null) {
                        Text(stringResource(R.string.review))
                    }
                    if(questions.size>limit) Text(stringResource(R.string.explore_practice_limit,limit))
                }
            }
            items(questions,key={it.id}) { question ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                        Text(stringResource(R.string.explore_question_id,question.id))
                        MarkdownText(question.localizedForDisplay().question)
                        OutlinedButton({onOpenQuestion(question.id)},enabled=!state.loading && state.error==null) {
                            Text(stringResource(R.string.question_details))
                        }
                    }
                }
            }
        }
    }
    }
}
