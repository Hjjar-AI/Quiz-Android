package com.mukhtabir.android.ui.admin

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppSuccessText

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerOutlinedButton

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mukhtabir.android.ui.components.AppIconButton as IconButton
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.*
import kotlinx.serialization.Serializable
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.ui.components.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal fun tagNodes(tree: List<TagTreeNode>): List<TagTreeNode> {
    val result = linkedMapOf<Long, TagTreeNode>()
    fun visit(nodes: List<TagTreeNode>, depth: Int) {
        if (depth > 64) return
        nodes.forEach { if (it.id !in result) { result[it.id] = it; visit(it.children, depth + 1) } }
    }
    visit(tree, 0)
    return result.values.toList()
}
@Serializable
internal data class TagDraft(val tree: List<TagTreeNode>, val selected: Set<Long>, val expanded: Set<Long>,
    val action: String?, val source: Long?, val name: String, val version: Int, val posting: Boolean)
internal data class TagManagementState(
    val version: Int = 1, val latestVersion: Int = 1, val posting: Boolean = false,
    val tree: List<TagTreeNode> = emptyList(), val counts: Map<String, Int> = emptyMap(), val selected: Set<Long> = emptySet(), val expanded: Set<Long> = emptySet(),
    val loading: Boolean = false, val loaded: Boolean = false, val busy: Boolean = false, val error: Throwable? = null,
    val countsError: Throwable? = null, val actionError: Throwable? = null, val uncertain: Boolean = false, val reviewed: Boolean = false,
    val action: String? = null, val source: Long? = null, val name: String = "", val saved: Boolean = false, val validation: Int? = null,
)
internal class TagManagementViewModel(private val service: ContentManagementService, private val questions: QuestionService,
    private val permitted: Boolean, store: SessionDraftStore, userId: Long) : ViewModel() {
    private val _state = MutableStateFlow(TagManagementState()); val state = _state.asStateFlow()
    val recovery = EditorRecovery(viewModelScope,store,userId,"tag_management-new",TagDraft.serializer())
    private fun snapshot() = _state.value.let {s -> TagDraft(s.tree,s.selected,s.expanded,s.action,s.source,s.name,s.version,s.posting || s.uncertain).takeIf {s.action!=null || s.selected.isNotEmpty() || s.uncertain} }
    init { recovery.observe(state) { snapshot() }; refresh() }
    fun restore(d: TagDraft) { _state.update { it.copy(tree=d.tree,version=d.version,latestVersion=d.version,selected=d.selected,expanded=d.expanded,action=d.action,source=d.source,name=d.name,uncertain=d.posting,loaded=true,reviewed=false) } }
    private fun editable() = permitted && !recovery.blocked && !_state.value.loading && !_state.value.busy && !_state.value.uncertain
    fun refresh() {
        if (!permitted || _state.value.loading || _state.value.busy) return
        _state.update { it.copy(loading = true, error = null, reviewed = false) }
        viewModelScope.launch {
            try {
                val response = service.tags(); val tree = response.tree
                val countResult = try { Result.success(questions.tags().items.associate { it.name to it.count }) }
                    catch (cancel: CancellationException) { throw cancel }
                    catch (failure: Exception) { Result.failure(failure) }
                coroutineContext.ensureActive()
                _state.update { it.copy(tree = tree, latestVersion=response.version, version=if(it.action!=null || it.selected.isNotEmpty()) it.version else response.version, uncertain=it.uncertain || ((it.action!=null || it.selected.isNotEmpty()) && it.version!=response.version), counts = countResult.getOrDefault(it.counts), countsError = countResult.exceptionOrNull(), loaded = true, loading = false, reviewed = true,
                    selected = it.selected.intersect(tagNodes(tree).map { n -> n.id }.toSet())) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { _state.update { it.copy(loading = false, error = failure) } }
        }
    }
    fun acknowledge() { if (permitted && !_state.value.loading && !_state.value.busy && _state.value.reviewed)
        _state.update { it.copy(uncertain = false, version=it.latestVersion, actionError = null, reviewed=false) } }
    fun expand(id: Long) {
        if(permitted && !_state.value.loading && !_state.value.busy) _state.update {
            it.copy(expanded = if(id in it.expanded) it.expanded-id else it.expanded+id)
        }
    }
    fun toggle(id: Long) { if (editable() && tagNodes(_state.value.tree).any { it.id == id })
        _state.update { it.copy(selected = if (id in it.selected) it.selected - id else it.selected + id) } }
    fun open(action: String, id: Long? = null) {
        if (!editable() || !_state.value.loaded) return
        if (action == "merge" && id != null) {
            if (tagNodes(_state.value.tree).none { it.id == id }) return
            _state.update { it.copy(selected = setOf(id)) }
        }
        if (action == "merge" && _state.value.selected.isEmpty()) return
        if (action != "merge" && tagNodes(_state.value.tree).none { it.id == id }) return
        _state.update { it.copy(action = action, source = id, name = "", validation = null, saved = false, actionError = null) }
    }
    fun field(value: String) { if (editable()) _state.update { it.copy(name = value, validation = null) } }
    fun close() { if (editable()) _state.update { it.copy(action = null) } }
    fun submit() {
        val s = _state.value; val action = s.action ?: return
        if (!editable()) return
        val nodes = tagNodes(s.tree).associateBy { it.id }
        val sources = (if (action == "merge") s.selected else setOfNotNull(s.source)).mapNotNull(nodes::get)
        val name = s.name.trim()
        if (sources.isEmpty() || (action != "delete" && (name.isBlank() || name.codePointCount(0, name.length) > 50)) ||
            (action == "rename" && nodes.values.any { it.name == name }) ||
            (action == "merge" && sources.any { it.name == name })) {
            _state.update { it.copy(validation = R.string.taxonomy_invalid) }; return
        }
        val target = nodes.values.find { it.name == name }
        if (action == "merge" && target != null && sources.any { source -> tagNodes(source.children).any { it.id == target.id } }) {
            _state.update { it.copy(validation = R.string.taxonomy_descendant) }; return
        }
        _state.update { it.copy(busy = true, actionError = null, saved = false) }
        viewModelScope.launch {
            var dispatched = false
            try {
                val freshResponse = service.tags(); val fresh = tagNodes(freshResponse.tree).associateBy { it.id }; coroutineContext.ensureActive()
                if (freshResponse.version != s.version || sources.any { fresh[it.id]?.name != it.name } || (target != null && fresh[target.id]?.name != name)) {
                    _state.update { it.copy(busy = false, actionError = IllegalStateException(), uncertain = true) }; return@launch
                }
                _state.update {it.copy(posting=true)}
                if (!recovery.persist(requireNotNull(snapshot()))) return@launch
                dispatched = true
                when (action) { "rename" -> service.renameTag(sources.single().name, name, s.version)
                    "delete" -> service.deleteTag(sources.single().name, s.version)
                    else -> service.mergeTags(sources.map { it.name }, name, s.version) }
                coroutineContext.ensureActive()
                _state.update { it.copy(busy = false, saved = true, action = null, selected = emptySet(), posting=false) }
                recovery.persist(null)
                refresh()
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                val certain = failure is ApiException && failure.statusCode in setOf(400, 401, 403, 404, 409, 422, 429)
                _state.update { it.copy(busy = false, posting=false, actionError = failure, uncertain = (dispatched && !certain) || (failure is ApiException && failure.statusCode==409), reviewed = false) }
            } finally { _state.update { it.copy(busy=false, posting=false) } }
        }
    }
    class Factory(private val service: ContentManagementService, private val questions: QuestionService, private val permitted: Boolean, private val store: SessionDraftStore, private val user: Long) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T = TagManagementViewModel(service, questions, permitted, store, user) as T
    }
}

@Composable
fun TagManagementRoute(service: ContentManagementService, questions: QuestionService, userId: Long, permitted: Boolean, store: SessionDraftStore,
    onBack: () -> Unit, onBusy: (Boolean) -> Unit, onChanged: () -> Unit) {
    val vm: TagManagementViewModel = viewModel(key = "tag-management-$userId-$permitted", factory = TagManagementViewModel.Factory(service, questions, permitted, store, userId))
    val s by vm.state.collectAsState(); val recoveryBlocked=editorRecoveryBlocked(vm.recovery); var leave by remember { mutableStateOf(false) }
    val busy = s.loading || s.busy
    BackHandler { if (!busy) { if (s.uncertain || s.action != null) leave = true else onBack() } }
    DisposableEffect(busy) { onBusy(busy); onDispose { onBusy(false) } }
    LaunchedEffect(s.saved) { if (s.saved) onChanged() }
    if (!permitted) { Text(stringResource(R.string.error_forbidden)); return }
    val enabled = !recoveryBlocked && !busy && !s.uncertain
    if (leave) AlertDialog(onDismissRequest = { leave = false }, title = { Text(stringResource(R.string.confirm)) },
        text = { Text(stringResource(R.string.manage_leave_confirm)) }, confirmButton = { TextButton({ leave = false; onBack() }) { Text(stringResource(R.string.confirm)) } },
        dismissButton = { TextButton({ leave = false }) { Text(stringResource(R.string.cancel)) } })
    if (s.action != null && !s.uncertain && !recoveryBlocked) AlertDialog(onDismissRequest = { if (!busy) vm.close() },
        title = { Text(stringResource(when (s.action) { "rename" -> R.string.taxonomy_rename; "delete" -> R.string.ux_delete_tag; else -> R.string.taxonomy_merge })) },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
            val nodes = tagNodes(s.tree)
            val sources = if (s.action == "merge") nodes.filter { it.id in s.selected } else nodes.filter { it.id == s.source }
            sources.forEach { Text(it.name + " · " + (s.counts[it.name]?.let { count -> stringResource(R.string.taxonomy_visible_links, count) } ?: stringResource(R.string.taxonomy_counts_unavailable))) }
            Text(stringResource(when (s.action) { "delete" -> R.string.taxonomy_delete_impact; "rename" -> R.string.taxonomy_rename_impact; else -> R.string.taxonomy_merge_impact }))
            if(s.countsError != null) Text(stringResource(R.string.taxonomy_counts_stale))
            if (s.action != "delete") {
                OutlinedTextField(s.name, vm::field, enabled = enabled, label = { Text(stringResource(R.string.taxonomy_target)) }, modifier = Modifier.fillMaxWidth())
                if (s.action == "merge") AppSection(stringResource(R.string.taxonomy_existing_target)) {
                    val excluded = nodes.filter { it.id in s.selected }.flatMap { listOf(it.id) + tagNodes(it.children).map { child -> child.id } }.toSet()
                    nodes.filter { it.id !in excluded }.forEach { node -> AppOutlinedButton({ vm.field(node.name) }, enabled = enabled) { Text(node.name) } }
                }
            }
            s.validation?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }
            s.actionError?.let { ErrorState(throwable = it) }
        } }, confirmButton = { TextButton(vm::submit, enabled = enabled) { Text(stringResource(when (s.action) {
            "delete" -> R.string.ux_delete_tag
            "merge" -> R.string.taxonomy_merge
            "rename" -> R.string.taxonomy_rename
            else -> R.string.confirm
        })) } },
        dismissButton = { TextButton(vm::close, enabled = enabled) { Text(stringResource(R.string.cancel)) } })
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item { EditorRecoveryControls(vm.recovery,busy,vm::restore) }
        item {
            AppPageHeading(stringResource(R.string.taxonomy_title))
            AppOutlinedButton(vm::refresh, enabled = !busy) { Text(stringResource(R.string.refresh)) }
            if (busy) AppLoadingIndicator()
            s.error?.let { ErrorState(throwable = it, onRetry = if (!busy) vm::refresh else null) }
            s.countsError?.let { ErrorState(throwable = it, onRetry = if(!busy) vm::refresh else null); Text(stringResource(R.string.taxonomy_counts_stale)) }
            if (s.action == null || s.uncertain) s.actionError?.let { ErrorState(throwable = it) }
            if (s.saved) AppSuccessText(stringResource(R.string.saved))
            if (s.uncertain) {
                Text(stringResource(R.string.content_unknown))
                AppOutlinedButton(vm::acknowledge, enabled = !busy && s.reviewed) { Text(stringResource(R.string.content_reviewed)) }
            }
            Text(stringResource(R.string.moderation_selected, s.selected.size))
            AppButton({ vm.open("merge") }, enabled = enabled && s.selected.isNotEmpty()) { Text(stringResource(R.string.taxonomy_merge)) }
            if (s.loaded && !busy && s.error==null && s.tree.isEmpty()) com.mukhtabir.android.ui.components.EmptyState(stringResource(R.string.no_items))
        }
        fun rows(nodes: List<TagTreeNode>, parents: List<TagTreeNode>) {
            if(parents.size > 64) return
            nodes.forEach { node -> if(parents.none { it.id == node.id }) {
                item(key = node.id) { TagRow(node, parents, s, vm, enabled) }
                if(node.id in s.expanded) rows(node.children, parents + node)
            } }
        }
        rows(s.tree, emptyList())
    }
}

@Composable
private fun TagRow(node: TagTreeNode, parents: List<TagTreeNode>, s: TagManagementState, vm: TagManagementViewModel, enabled: Boolean) {
    val expanded = node.id in s.expanded
    val state = stringResource(if(expanded) R.string.ui_expanded else R.string.ui_collapsed)
    AppCard(Modifier.fillMaxWidth().padding(start = (parents.size.coerceAtMost(3) * 8).dp)) {
        Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets), verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Row(Modifier.weight(1f).toggleable(node.id in s.selected, enabled=enabled, role=Role.Checkbox, onValueChange={vm.toggle(node.id)})) {
                    Checkbox(node.id in s.selected, null, enabled=enabled)
                    Text(node.name, Modifier.weight(1f), style=MaterialTheme.typography.titleMedium)
                }
                IconButton({vm.expand(node.id)}, enabled=!s.loading && !s.busy, modifier=Modifier.semantics { stateDescription=state }) {
                    Icon(if(expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        stringResource(if(expanded) R.string.taxonomy_collapse else R.string.taxonomy_expand))
                }
            }
            if(parents.isNotEmpty()) com.mukhtabir.android.ui.components.AppMetadataRow(listOf(parents.joinToString(" / ") { it.name }))
            if(expanded) {
                val count=s.counts[node.name]
                Text(if(count==null) stringResource(R.string.taxonomy_counts_unavailable) else stringResource(R.string.taxonomy_visible_links,count))
                AppOutlinedButton({vm.open("rename",node.id)},enabled=enabled) {Text(stringResource(R.string.taxonomy_rename))}
                AppOutlinedButton({vm.open("merge",node.id)},enabled=enabled) {Text(stringResource(R.string.taxonomy_merge_one))}
                AppDangerOutlinedButton({vm.open("delete",node.id)},enabled=enabled) {Text(stringResource(R.string.ux_delete_tag))}
            }
        }
    }
}
