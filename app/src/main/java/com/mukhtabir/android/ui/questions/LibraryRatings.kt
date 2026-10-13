package com.mukhtabir.android.ui.questions

import com.mukhtabir.android.ui.components.AppSuccessText

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppActionRow

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.QuestionRatingRowDto
import com.mukhtabir.android.data.remote.QuestionService
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.util.formatNumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LibraryRatingsState(
    val rows: Map<Long, QuestionRatingRowDto> = emptyMap(),
    val drafts: Map<Long, Int> = emptyMap(),
    val saved: Map<Long, Int> = emptyMap(),
    val loading: Boolean = false,
    val savingId: Long? = null,
    val error: Throwable? = null,
    val writeErrors: Map<Long, Throwable> = emptyMap(),
    val uncertain: Set<Long> = emptySet(),
    val reviewed: Set<Long> = emptySet(),
)

/** Memory-only page ratings/drafts. Unknown writes require a fresh read and explicit review. */
class LibraryRatingsViewModel(private val service: QuestionService) : ViewModel() {
    private val mutable = MutableStateFlow(LibraryRatingsState())
    val state = mutable.asStateFlow()
    private var visible = emptyList<Long>()
    private var readJob: Job? = null

    fun visible(ids: List<Long>) {
        visible = ids.distinct()
        refresh()
    }

    fun refresh() {
        if (mutable.value.savingId != null) return
        readJob?.cancel()
        val ids = (visible + mutable.value.uncertain).distinct()
        if (ids.isEmpty()) {
            mutable.update { it.copy(rows = emptyMap(), loading = false, error = null) }
            return
        }
        mutable.update { it.copy(loading = true, error = null) }
        readJob = viewModelScope.launch {
            try {
                val rows = service.ratings(ids)
                ensureActive()
                mutable.update { it.copy(rows = rows, loading = false,
                    reviewed = it.reviewed + it.uncertain.intersect(rows.keys)) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                ensureActive(); mutable.update { it.copy(loading = false, error = failure) }
            }
        }
    }

    fun select(id: Long, value: Int) {
        val s = mutable.value
        if (value !in 1..5 || s.loading || s.savingId != null || id in s.uncertain || id !in s.rows) return
        mutable.update { it.copy(drafts = it.drafts + (id to value), writeErrors = it.writeErrors - id, saved = it.saved - id) }
    }

    fun acknowledge(id: Long) {
        val s = mutable.value
        if (s.loading || s.savingId != null || id !in s.reviewed) return
        mutable.update { it.copy(uncertain = it.uncertain - id, reviewed = it.reviewed - id, writeErrors = it.writeErrors - id) }
    }

    fun save(id: Long) {
        val s = mutable.value
        val value = s.drafts[id] ?: return
        if (s.loading || s.savingId != null || id in s.uncertain || id !in visible || id !in s.rows ||
            value !in 1..5 || value == (s.saved[id] ?: s.rows[id]?.userRating)) return
        mutable.update { it.copy(savingId = id, writeErrors = it.writeErrors - id, reviewed = it.reviewed - id) }
        val pageAtDispatch = visible.toList()
        viewModelScope.launch {
            var confirmed = false
            try {
                service.rate(id, value)
                ensureActive(); confirmed = true
                // Confirmed success survives an aggregate refresh failure.
                mutable.update { it.copy(saved = it.saved + (id to value), rows = it.rows - id) }
                val rating = service.rating(id)
                ensureActive()
                mutable.update { it.copy(rows = it.rows + (id to QuestionRatingRowDto(id, rating.userRating, rating.average, rating.count))) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                ensureActive()
                val rejected = failure is ApiException && failure.statusCode in setOf(400, 401, 403, 404, 409, 422, 429)
                mutable.update { it.copy(writeErrors = it.writeErrors + (id to failure),
                    uncertain = if (!confirmed && !rejected) it.uncertain + id else it.uncertain) }
            } finally {
                // No detached refresh or mutation replay after account disposal.
                if (kotlin.coroutines.coroutineContext[kotlinx.coroutines.Job]?.isActive == true) {
                    mutable.update { it.copy(savingId = null) }
                    if (visible != pageAtDispatch || (confirmed && visible.any { it !in mutable.value.rows })) refresh()
                }
            }
        }
    }

    class Factory(private val service: QuestionService) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LibraryRatingsViewModel(service) as T
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun LibraryRatingDialog(id: Long, state: LibraryRatingsState, vm: LibraryRatingsViewModel, dismiss: () -> Unit) {
    val row = state.rows[id]
    val selected = state.drafts[id] ?: row?.userRating ?: 0
    val busy = state.loading || state.savingId != null
    val uncertain = id in state.uncertain
    AlertDialog(
        onDismissRequest = { if (state.savingId != id) dismiss() },
        title = { Text(stringResource(R.string.select_question_number, id)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                row?.let { Text(if (it.count == 0) stringResource(R.string.no_ratings)
                    else stringResource(R.string.rating_average, it.average, it.count)) }
                if (busy) AppLoadingIndicator()
                if (!busy && row == null) Text(stringResource(R.string.library_rating_unavailable))
                Text(stringResource(R.string.your_rating))
                AppActionRow() {
                    (1..5).forEach { value -> FilterChip(selected == value, { vm.select(id, value) },
                        enabled = !busy && !uncertain && row != null,
                        label = { Text(stringResource(R.string.library_rating_value, formatNumber(value))) }) }
                }
                if (id in state.saved) AppSuccessText(stringResource(R.string.saved))
                if (uncertain) Text(stringResource(R.string.library_rating_unknown))
                state.writeErrors[id]?.let { ErrorState(throwable = it) }
                state.error?.let { ErrorState(throwable = it) }
                TextButton(vm::refresh, enabled = !busy) { Text(stringResource(R.string.refresh_rating)) }
                if (uncertain && id in state.reviewed) TextButton({ vm.acknowledge(id) }, enabled = !busy) {
                    Text(stringResource(R.string.content_reviewed))
                }
            }
        },
        confirmButton = {
            TextButton({ vm.save(id) }, enabled = !busy && !uncertain && row != null && selected in 1..5 &&
                selected != (state.saved[id] ?: row.userRating)) { Text(stringResource(R.string.save_rating)) }
        },
        dismissButton = { TextButton(dismiss, enabled = state.savingId != id) { Text(stringResource(R.string.close)) } },
    )
}
