@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.selected
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
import com.mukhtabir.android.data.remote.GroupDto
import com.mukhtabir.android.data.remote.GroupLeaderboardDto
import com.mukhtabir.android.data.remote.GroupService
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppMetadataRow
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GroupsUiState(
    val groups: List<GroupDto> = emptyList(),
    val selected: Long? = null,
    val days: Int = 7,
    val leaderboard: GroupLeaderboardDto? = null,
    val visible: Boolean = true,
    val loading: Boolean = false,
    val boardLoading: Boolean = false,
    val visibilityBusy: Boolean = false,
    val error: Throwable? = null,
    val boardError: Throwable? = null,
    val actionError: Throwable? = null,
    val visibilityNeedsReview: Boolean = false,
    val message: Int? = null,
)

class GroupsViewModel(private val service: GroupService, userId: Long, handle: SavedStateHandle) : ViewModel() {
    private val saved = LearnerSavedState(handle,userId,"member_groups")
    private val _state = MutableStateFlow(GroupsUiState(selected=saved.expandedId(),
        days=saved.filter()?.toIntOrNull()?.takeIf { it in setOf(7,30,90) } ?: 7))
    val state = _state.asStateFlow()
    private var boardJob: Job? = null
    private var failedVisibility: Pair<Long, Boolean>? = null

    init { load() }

    fun load() {
        if (_state.value.loading || _state.value.visibilityBusy) return
        boardJob?.cancel()
        _state.update { it.copy(loading = true, boardLoading = false, error = null) }
        viewModelScope.launch {
            runCatching { service.mine() }.onSuccess { response ->
                ensureActive()
                val selected = _state.value.selected?.takeIf { id -> response.items.any { it.id == id } }
                    ?: response.items.firstOrNull()?.id
                val pending=failedVisibility
                val reviewed=pending?.let { (id,value) ->
                    response.items.firstOrNull { it.id==id }?.showInLeaderboard==value
                }
                failedVisibility=null
                _state.update { it.copy(groups = response.items, loading = false, visibilityNeedsReview=false,
                    actionError=null,message=when(reviewed) {
                        true -> R.string.group_visibility_matches
                        false -> R.string.group_visibility_reviewed
                        null -> it.message
                    }) }
                if (selected != null) open(selected) else {
                    saved.saveExpandedId(null)
                    _state.update { it.copy(selected = null, leaderboard = null,
                        boardError = null, actionError = null) }
                }
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                ensureActive()
                _state.update { it.copy(loading = false, error = failure) }
            }
        }
    }

    fun open(id: Long) {
        val current = _state.value
        if (current.loading || current.visibilityBusy || (current.selected == id && current.boardLoading)) return
        val group = current.groups.firstOrNull { it.id == id } ?: return
        boardJob?.cancel()
        val days=current.days
        saved.saveExpandedId(id)
        _state.update { it.copy(selected = id, visible = group.showInLeaderboard,
            leaderboard = it.leaderboard.takeIf { board -> board?.group?.id == id },
            boardLoading = true, boardError = null, actionError = null) }
        boardJob = viewModelScope.launch {
            runCatching { service.leaderboard(id,days).also { check(it.group.id == id && it.days == days) } }
                .onSuccess { board ->
                    ensureActive()
                    if (_state.value.selected != id || _state.value.days != days) return@onSuccess
                    // Visibility comes from mine()/the confirmed write. The board's
                    // group block does not contain show_in_leaderboard.
                    _state.update { it.copy(leaderboard = board, boardLoading = false) }
                }.onFailure { failure ->
                    if (failure is CancellationException) throw failure
                    ensureActive()
                    if (_state.value.selected != id || _state.value.days != days) return@onFailure
                    _state.update { it.copy(boardLoading = false, boardError = failure) }
                }
        }
    }

    fun retryBoard() { _state.value.selected?.let(::open) }

    fun changeDays(days: Int) {
        val s=_state.value
        if(days !in setOf(7,30,90) || s.loading || s.visibilityBusy || days==s.days) return
        boardJob?.cancel()
        saved.saveFilter(days.toString())
        _state.update { it.copy(days=days,boardLoading=false) }
        s.selected?.let(::open)
    }

    fun setVisible(value: Boolean) {
        val current = _state.value
        val id = current.selected ?: return
        if (current.loading || current.boardLoading || current.visibilityBusy || current.visibilityNeedsReview || current.error != null ||
            current.leaderboard?.group?.id != id || current.groups.none { it.id == id } || current.visible == value
        ) return
        _state.update { it.copy(visibilityBusy = true, actionError = null,message=null) }
        viewModelScope.launch {
            runCatching { service.setVisibility(id, value) }.onSuccess {
                ensureActive()
                failedVisibility = null
                _state.update { it.copy(visible = value, visibilityBusy = false,message=R.string.saved,
                    groups = it.groups.map { group -> if (group.id == id) group.copy(showInLeaderboard = value) else group }) }
                open(id)
            }.onFailure { failure ->
                if (failure is CancellationException) throw failure
                ensureActive()
                failedVisibility = id to value
                _state.update { it.copy(visibilityBusy = false, actionError = failure,visibilityNeedsReview=true) }
            }
        }
    }

    // A lost visibility response requires membership reads, never replay of the failed write.
    fun retryVisibility() = load()

    class Factory(private val service: GroupService,private val userId: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>,extras: CreationExtras): T =
            GroupsViewModel(service,userId,extras.createSavedStateHandle()) as T
    }
}

@Composable
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
fun GroupsRoute(service: GroupService, userId: Long) {
    val vm: GroupsViewModel = viewModel(key = "groups-$userId", factory = GroupsViewModel.Factory(service,userId))
    val state by vm.state.collectAsLifecycleState()
    val busy = state.loading || state.visibilityBusy
    RefreshOnResume(vm::load)
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {
            AppPageHeading(stringResource(R.string.nav_groups), action = {
                AppActionRow(fillWidth = false) {
                    AppOutlinedButton(vm::load, enabled = !busy) { Text(stringResource(R.string.refresh)) }
                }
            })
        }
        item {
            AppSection(stringResource(R.string.group_period)) {
            AppActionRow() {
                listOf(7,30,90).forEach { days ->
                    AppFilterChip(selected=state.days==days,onClick={vm.changeDays(days)},enabled=!busy,
                        label={Text(stringResource(R.string.group_days,formatNumber(days)))})
                }
            }
        }
        }
        if (busy || state.boardLoading) item { AppLoadingIndicator() }
        state.message?.let { item { Text(stringResource(it),modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) } }
        if(state.visibilityNeedsReview) item { AppStatus(stringResource(R.string.group_visibility_recovery)) }
        state.error?.let { item { ErrorState(throwable = it, onRetry = if (busy) null else vm::load,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) } }
        state.boardError?.let { item { ErrorState(throwable = it, onRetry = if (busy || state.boardLoading) null else vm::retryBoard,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) } }
        state.actionError?.let { item { ErrorState(throwable = it, onRetry = if (busy || state.boardLoading) null else vm::retryVisibility,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) } }
        if (!state.loading && state.error == null && state.groups.isEmpty()) item { EmptyState(stringResource(R.string.no_groups)) }
        items(state.groups, key = { "group-${it.id}" }) { group ->
            AppCard(Modifier.fillMaxWidth().semantics { selected = state.selected == group.id }
                .clickable(role = androidx.compose.ui.semantics.Role.Button, enabled = !busy) { vm.open(group.id) }) {
                Column(Modifier.padding(AppLayoutTokens.denseCardInsets)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                        if (state.selected == group.id) Icon(Icons.Filled.Check, contentDescription = null)
                        Text(group.name, style = MaterialTheme.typography.titleMedium)
                    }
                    if (group.description.isNotBlank()) Text(group.description)
                    AppMetadataRow(listOf(stringResource(R.string.member_count, com.mukhtabir.android.util.formatNumber(group.memberCount))))
                }
            }
        }
        state.leaderboard?.takeIf { it.group.id == state.selected }?.let { board ->
            item {
                Text(board.group.name, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.semantics { heading() })
                Text(stringResource(R.string.group_board_period,formatNumber(board.days)),style=MaterialTheme.typography.titleLarge)
                val visibilityLabel=stringResource(R.string.show_on_leaderboard)
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(visibilityLabel,Modifier.weight(1f))
                    Switch(state.visible, vm::setVisible, enabled = !busy && !state.boardLoading &&
                        !state.visibilityNeedsReview && state.error == null,
                        modifier=Modifier.semantics { contentDescription=visibilityLabel })
                }
                Text(stringResource(R.string.group_visibility_hint))
            }
            if(board.rows.isEmpty() && !state.boardLoading && state.boardError==null && state.error==null) item { EmptyState(stringResource(R.string.group_board_empty)) }
            items(board.rows, key = { "member-${it.userId}" }) { row ->
                AppCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                        Text(row.fullName.ifBlank { row.username },style=MaterialTheme.typography.titleMedium)
                        if(row.userId==userId) Text(stringResource(R.string.group_self))
                        if(row.rank>0) Text(stringResource(R.string.group_rank,formatNumber(row.rank)))
                        Text(stringResource(R.string.leaderboard_score, com.mukhtabir.android.util.formatNumber(row.questionsAnswered), row.accuracy))
                        AppMetadataRow(listOf(stringResource(R.string.group_streak,formatNumber(row.currentStreak))))
                    }
                }
            }
        }
    }
}
