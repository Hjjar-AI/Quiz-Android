@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.knowledge

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.LearnerSavedState
import com.mukhtabir.android.data.remote.KnowledgeMapDto
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppMetadataRow
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppProgressBar
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KnowledgeUiState(
    val data: KnowledgeMapDto? = null,
    val filter: String = "all",
    val loading: Boolean = false,
    val error: Throwable? = null,
)

private val knowledgeFilters = setOf("all", "mastered", "developing", "needs_work", "unstarted")

class KnowledgeViewModel(
    private val service: QuestionService,
    userId: Long,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val saved = LearnerSavedState(savedStateHandle, userId, "knowledge_navigation")
    private val _state = MutableStateFlow(KnowledgeUiState(
        filter = saved.filter()?.takeIf { it in knowledgeFilters } ?: "all",
    ))
    val state = _state.asStateFlow()

    init { load() }

    fun load() {
        if (_state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { service.knowledgeMap() }
                .onSuccess { data ->
                    ensureActive()
                    _state.update { it.copy(data = data, loading = false) }
                }
                .onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    ensureActive()
                    _state.update { it.copy(loading = false, error = failure) }
                }
        }
    }

    fun filter(value: String) {
        if (value !in knowledgeFilters) return
        _state.update { it.copy(filter = value) }
        saved.saveFilter(value)
    }

    class Factory(
        private val service: QuestionService,
        private val userId: Long,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
            KnowledgeViewModel(service, userId, extras.createSavedStateHandle()) as T
    }
}

@Composable
fun KnowledgeMapRoute(
    service: QuestionService,
    canStartSessions: Boolean,
    userId: Long,
    onOpenDetail: (Long) -> Unit,
    onReview: (List<Long>, String) -> Unit,
) {
    val vm: KnowledgeViewModel = viewModel(
        key = "knowledge-$userId",
        factory = KnowledgeViewModel.Factory(service, userId),
    )
    RefreshOnResume(vm::load)
    val state by vm.state.collectAsLifecycleState()
    val data = state.data
    val items = data?.items.orEmpty().filter { state.filter == "all" || it.status == state.filter }
    Column(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppPageHeading(stringResource(R.string.nav_knowledge_map))
        AppOutlinedButton(vm::load, enabled = !state.loading) { Text(stringResource(R.string.refresh)) }
        data?.summary?.let { summary ->
            Text(stringResource(R.string.knowledge_summary, com.mukhtabir.android.util.formatNumber(summary.masteredObjects), com.mukhtabir.android.util.formatNumber(summary.totalObjects), com.mukhtabir.android.util.formatNumber(summary.averageMastery)))
            Text(stringResource(R.string.knowledge_started_count, com.mukhtabir.android.util.formatNumber(summary.startedObjects)))
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap),
        ) {
            knowledgeFilters.forEach { filter ->
                AppFilterChip(
                    selected = state.filter == filter,
                    onClick = { vm.filter(filter) },
                    label = { Text(knowledgeStatusLabel(filter)) },
                    modifier = Modifier.heightIn(min = AppLayoutTokens.touchTarget),
                    leadingIcon = if (state.filter == filter) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else null,
                )
            }
        }
        if (state.loading) AppLoadingIndicator()
        state.error?.let {
            ErrorState(throwable = it, onRetry = vm::load,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
        }
        if (data != null && !state.loading && state.error == null && items.isEmpty()) {
            EmptyState(stringResource(R.string.no_items))
        }
        if (items.isNotEmpty()) LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            items.groupBy { it.categoryName?.takeIf(String::isNotBlank) }.forEach { (category, rows) ->
            item(key = "category-${category.orEmpty()}") { Text(category ?: stringResource(R.string.knowledge_uncategorized), modifier = Modifier.semantics { heading() }) }
            items(rows, key = { it.id }) { item ->
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(AppLayoutTokens.denseCardInsets), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                        Text(item.title, style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.semantics { heading() })
                        if (item.learningObjective.isNotBlank()) Text(item.learningObjective)
                        AppMetadataRow(listOfNotNull(item.categoryName,knowledgeStatusLabel(item.status)))
                        AppProgressBar((item.masteryScore / 100.0).toFloat().coerceIn(0f, 1f), Modifier.fillMaxWidth())
                        Text(stringResource(R.string.knowledge_metrics, item.masteryScore, com.mukhtabir.android.util.formatNumber(item.coverage), com.mukhtabir.android.util.formatNumber(item.totalAttempts)))
                        AppMetadataRow(listOf(stringResource(R.string.knowledge_variant_count, com.mukhtabir.android.util.formatNumber(item.attemptedQuestions), com.mukhtabir.android.util.formatNumber(item.questionsCount))))
                        val confidence = item.averageConfidence
                        Text(stringResource(R.string.knowledge_average_confidence, stringResource(when {
                            confidence == null -> R.string.none
                            confidence >= 2.67 -> R.string.confidence_certain
                            confidence >= 1.67 -> R.string.confidence_uncertain
                            else -> R.string.confidence_guessing
                        })))
                        AppOutlinedButton({onOpenDetail(item.id)},enabled=!state.loading && state.error==null) {
                            Text(stringResource(R.string.explore_knowledge_detail))
                        }
                        if (canStartSessions && item.questionIds.isNotEmpty()) {
                            AppButton({ onReview(item.questionIds, item.title) },
                                enabled = !state.loading && state.error == null) { Text(stringResource(R.string.review)) }
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun knowledgeStatusLabel(status: String): String = stringResource(
    when (status) {
        "mastered" -> R.string.knowledge_mastered
        "developing" -> R.string.knowledge_developing
        "needs_work" -> R.string.knowledge_needs_work
        "unstarted" -> R.string.knowledge_unstarted
        else -> R.string.all_questions
    },
)
