package com.mukhtabir.android.ui.categories

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.*
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.*
import com.mukhtabir.android.data.remote.*
import com.mukhtabir.android.ui.components.*
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerOutlinedButton
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppSuccessText
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import java.util.UUID
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable

@Serializable
internal data class CategoryDraft(val name: String = "", val description: String = "", val color: String = "#667eea",
    val icon: String = "bi-folder", val editing: CategoryDto? = null, val action: String? = null,
    val operation: String? = null, val deleting: CategoryDto? = null, val uncertain: Boolean = false,
    val mergeSource: CategoryDto? = null, val mergeTarget: CategoryDto? = null)
internal data class CategoriesUiState(val items: List<CategoryDto> = emptyList(), val draft: CategoryDraft = CategoryDraft(),
    val busy: Boolean = false, val error: Throwable? = null, val actionError: Throwable? = null,
    val latest: CategoryDto? = null, val conflict: Boolean = false, val reviewed: Boolean = false,
    val terminalReceipt: Boolean = false, val saved: Boolean = false, val confirmDelete: CategoryDto? = null)

internal class CategoriesViewModel(private val service: CategoryService, private val canManage: Boolean,
    store: SessionDraftStore, userId: Long) : ViewModel() {
    private val _state = MutableStateFlow(CategoriesUiState()); val state = _state.asStateFlow()
    val recovery = EditorRecovery(viewModelScope, store, userId, "categories-new", CategoryDraft.serializer())
    init { recovery.observe(state, canPersist = { !it.busy }) { it.draft.takeIf { d -> d != CategoryDraft() } }; load() }
    /** Navigation waits for the exact latest snapshot; no server write is dispatched here. */
    fun leave(discard: Boolean, onDone: () -> Unit) {
        val snapshot = _state.value
        if (snapshot.busy) return
        if (!discard && (!recovery.ready.value || recovery.pending.value != null)) { onDone(); return }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            val persisted = recovery.persist(if (discard) null else snapshot.draft.takeIf { it != CategoryDraft() })
            _state.update { it.copy(busy = false, draft = if (persisted && discard) CategoryDraft() else it.draft) }
            if (persisted) onDone()
        }
    }
    fun restore(draft: CategoryDraft) { _state.update { it.copy(draft = draft.copy(uncertain = draft.uncertain || draft.action != null), conflict = draft.editing != null, saved = false) } }
    private fun editable() = canManage && !_state.value.busy && !recovery.blocked && !_state.value.draft.uncertain && !_state.value.conflict
    fun field(name: String? = null, description: String? = null, color: String? = null, icon: String? = null) {
        if (!editable()) return
        _state.update { it.copy(draft = it.draft.copy(name = name ?: it.draft.name, description = description ?: it.draft.description,
            color = color ?: it.draft.color, icon = icon ?: it.draft.icon), saved = false) }
    }
    fun edit(item: CategoryDto) { if (editable()) _state.update { it.copy(draft = CategoryDraft(item.name, item.description.orEmpty(), item.color ?: "#667eea", item.icon ?: "bi-folder", item), saved = false) } }
    fun cancelEdit() { if (canManage && !_state.value.busy && !_state.value.draft.uncertain) _state.update { it.copy(draft = CategoryDraft(), conflict = false, latest = null) } }
    fun load() {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try { val rows = service.list().items; ensureActive(); _state.update { it.copy(items = rows) } }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { _state.update { it.copy(error = failure) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    fun save() = write(false)
    fun retryPending() {
        val s = _state.value
        if (!canManage || s.busy || s.terminalReceipt || recovery.blocked || !s.draft.uncertain) return
        _state.update { it.copy(draft = it.draft.copy(uncertain = false), conflict = false) }
        if (s.draft.action == "merge") merge(s.draft.mergeSource, s.draft.mergeTarget, s.draft.operation)
        else write(s.draft.action == "delete")
    }
    fun requestDelete(item: CategoryDto) { if (editable()) _state.update { it.copy(confirmDelete = item) } }
    fun dismissDelete() { if (!_state.value.busy) _state.update { it.copy(confirmDelete = null) } }
    fun confirmDelete() {
        val item = _state.value.confirmDelete ?: return
        if (!editable()) return
        _state.update { it.copy(draft = it.draft.copy(deleting = item), confirmDelete = null) }; write(true)
    }
    fun merge(source: CategoryDto?, target: CategoryDto?, operation: String? = null) {
        if (!editable() || source == null || target == null || source.id == target.id) return
        if (operation == null && _state.value.draft != CategoryDraft()) return
        val pending = _state.value.draft.copy(action = "merge", mergeSource = source, mergeTarget = target,
            operation = operation ?: UUID.randomUUID().toString())
        _state.update { it.copy(busy = true, actionError = null, saved = false, reviewed = false, draft = pending) }
        viewModelScope.launch {
            var sent = false
            try {
                if (!recovery.persist(pending)) {
                    _state.update { it.copy(draft = pending.copy(uncertain = true)) }
                    return@launch
                }
                sent = true
                val result = service.merge(source, target, requireNotNull(pending.operation))
                ensureActive()
                _state.update { it.copy(items = (it.items.filterNot { row -> row.id == source.id || row.id == result.id } + result).sortedBy { row -> row.name },
                    draft = CategoryDraft(), saved = true, conflict = false) }
                recovery.persist(null)
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                ensureActive()
                val rejected = failure.isRejectedWrite()
                val conflict = failure is ApiException && failure.statusCode in setOf(404, 409)
                _state.update { it.copy(actionError = failure,
                    draft = if (rejected && !conflict) CategoryDraft() else pending.copy(uncertain = sent && !rejected,
                        operation = if (rejected) null else pending.operation),
                    conflict = conflict) }
            } finally { _state.update { it.copy(busy = false) } }
        }
    }
    private fun write(delete: Boolean) {
        val s = _state.value; val d = s.draft
        if (!editable() || (!delete && d.name.isBlank())) return
        val pending = d.copy(action = if (delete) "delete" else "save", operation = if (!delete && d.editing == null) d.operation ?: UUID.randomUUID().toString() else null)
        _state.update { it.copy(busy = true, actionError = null, saved = false, draft = pending) }
        viewModelScope.launch {
            var sent = false
            try {
                if (!recovery.persist(pending)) return@launch
                sent = true
                if (delete) {
                    val item = requireNotNull(pending.deleting); service.delete(item.id, item.version)
                    ensureActive(); _state.update { it.copy(items = it.items.filterNot { row -> row.id == item.id }) }
                } else {
                    val request = CategoryWriteRequest(pending.name.trim(), pending.description.trim(), pending.color.trim(), pending.icon,
                        expectedVersion = pending.editing?.version, operationId = pending.operation)
                    val result = if (pending.editing == null) service.create(request) else service.update(pending.editing.id, request)
                    ensureActive(); _state.update { it.copy(items = (it.items.filterNot { row -> row.id == result.id } + result).sortedBy { row -> row.name }) }
                }
                _state.update { it.copy(draft = CategoryDraft(), saved = true) }; recovery.persist(null)
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                ensureActive(); val rejected = failure.isRejectedWrite()
                _state.update { it.copy(actionError = failure, draft = pending.copy(uncertain = sent && !rejected,
                    action = if (sent && !rejected) pending.action else null), conflict = failure is ApiException && failure.statusCode == 409) }
            } finally { _state.update { it.copy(busy = false) } }
        }
    }
    fun reconcile() {
        val s = _state.value; val d = s.draft
        if (!canManage || s.busy || recovery.blocked) return
        _state.update { it.copy(busy = true, actionError = null, reviewed = false) }
        viewModelScope.launch {
            try {
                val rows = service.list().items
                if (d.action == "merge" && d.operation == null) {
                    ensureActive()
                    _state.update { it.copy(items = rows, reviewed = true, latest = null) }
                    return@launch
                }
                val id = if (d.operation != null && d.action in setOf("save", "merge") && (d.editing == null || d.action == "merge"))
                    service.receipt(d.operation, if (d.action == "merge") "category.merge" else "category.create")
                    else d.deleting?.id ?: d.editing?.id
                val latest = rows.firstOrNull { it.id == id }
                ensureActive()
                if ((d.action == "delete" && latest == null) || (d.operation != null && latest != null)) {
                    _state.update { it.copy(items = rows, draft = CategoryDraft(), saved = true, conflict = false) }; recovery.persist(null)
                } else _state.update { it.copy(items = rows, latest = latest, reviewed = true, terminalReceipt=d.operation!=null && latest==null, conflict = d.editing != null) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { _state.update { it.copy(actionError = failure, terminalReceipt=failure is ContentTargetGoneException,reviewed=failure is ContentTargetGoneException,latest=if(failure is ContentTargetGoneException) null else it.latest,conflict=if(failure is ContentTargetGoneException) false else it.conflict) } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }
    fun resolve(keep: Boolean) {
        val s = _state.value; val latest = s.latest ?: return
        if (!canManage || recovery.blocked || s.busy || !s.reviewed) return
        _state.update { it.copy(draft = if (keep) s.draft.copy(editing = latest, action = null, uncertain = false, deleting = null)
            else CategoryDraft(latest.name, latest.description.orEmpty(), latest.color ?: "#667eea", latest.icon ?: "bi-folder", latest), conflict = false, reviewed = false) }
    }
    fun acknowledge() { if (canManage && !recovery.blocked && !_state.value.busy && _state.value.reviewed) _state.update { it.copy(draft = it.draft.copy(uncertain = false, action = null, deleting = null,operation=null,mergeSource=null,mergeTarget=null), terminalReceipt=false,reviewed = false, conflict=false) } }
    class Factory(private val service: CategoryService, private val allowed: Boolean, private val store: SessionDraftStore, private val user: Long): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>): T = CategoriesViewModel(service, allowed, store, user) as T
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoriesRoute(service: CategoryService, canManage: Boolean, store: SessionDraftStore, userId: Long,
    onBack: () -> Unit, onBusyChanged: (Boolean) -> Unit) {
    val vm: CategoriesViewModel = viewModel(key = "categories-$userId-$canManage", factory = CategoriesViewModel.Factory(service, canManage, store, userId))
    val s by vm.state.collectAsLifecycleState(); val blocked = editorRecoveryBlocked(vm.recovery)
    var iconPicker by remember { mutableStateOf(false) }
    var discardEditor by remember { mutableStateOf(false) }
    var nextEditor by remember { mutableStateOf<CategoryDto?>(null) }
    var mergeSource by remember { mutableStateOf<CategoryDto?>(null) }
    var mergeTarget by remember { mutableStateOf<CategoryDto?>(null) }
    val currentEditor=s.draft.editing
    val dirty=if(currentEditor==null) s.draft.name.isNotBlank() || s.draft.description.isNotBlank() || s.draft.color!="#667eea" || s.draft.icon!="bi-folder"
        else s.draft.name!=currentEditor.name || s.draft.description!=currentEditor.description.orEmpty() || s.draft.color!=currentEditor.color.orEmpty() || s.draft.icon!=currentEditor.icon.orEmpty()
    val editable = canManage && !s.busy && !blocked && !s.draft.uncertain && !s.conflict
    var leaving by remember { mutableStateOf(false) }
    val recoveryError by vm.recovery.error.collectAsLifecycleState()
    val back: () -> Unit = {
        if (!s.busy) {
            if (dirty || s.draft.uncertain || s.draft.action != null) leaving = true
            else vm.leave(false, onBack)
        }
    }
    BackHandler(onBack = back)
    BlockedBackHandler(s.busy)
    DisposableEffect(s.busy) { onBusyChanged(s.busy); onDispose { onBusyChanged(false) } }
    if (leaving) AppConfirmationDialog(onDismissRequest = { if (!s.busy) leaving = false },
        title = { Text(stringResource(R.string.category_leave_title)) },
        text = { Column {
            Text(stringResource(if (s.draft.uncertain || s.draft.action != null) R.string.author_leave_unknown else R.string.category_leave_draft))
            recoveryError?.let { ErrorState(throwable = it) }
        } },
        confirmButton = { Column {
            AppTextButton({ vm.leave(false, onBack) }, enabled = !s.busy) { Text(stringResource(R.string.author_save_later)) }
            AppDangerTextButton({ vm.leave(true, onBack) }, enabled = !s.busy) { Text(stringResource(R.string.discard_changes_continue)) }
        } }, dismissButton = { AppTextButton({ leaving = false }, enabled = !s.busy) { Text(stringResource(R.string.keep_editing)) } })
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        item {
            AppPageHeading(stringResource(R.string.nav_categories))
            EditorRecoveryControls(vm.recovery, s.busy, vm::restore)
            if (canManage) {
                AppTextField(s.draft.name, { vm.field(name=it) }, label={Text(stringResource(R.string.category_name))}, enabled=editable, modifier=Modifier.fillMaxWidth())
                AppTextField(s.draft.description, { vm.field(description=it) }, label={Text(stringResource(R.string.description))}, enabled=editable, modifier=Modifier.fillMaxWidth())
                AppTextField(s.draft.color, { vm.field(color=it) }, label={Text(stringResource(R.string.category_color))}, enabled=editable, modifier=Modifier.fillMaxWidth())
                AppOutlinedButton({iconPicker=true},enabled=editable) { Text(stringResource(R.string.category_icon_selected,s.draft.icon)) }
                AppActionRow { AppButton(vm::save,enabled=editable && s.draft.name.isNotBlank()) {Text(stringResource(R.string.save))}
                    AppOutlinedButton({nextEditor=null;if(dirty) discardEditor=true else vm.cancelEdit()},enabled=!s.busy && !blocked && !s.draft.uncertain) {Text(stringResource(R.string.cancel))} }
            }
            if (s.saved) AppSuccessText(stringResource(R.string.saved))
            if (s.busy) AppLoadingIndicator()
            s.error?.let {ErrorState(throwable=it,onRetry=if(s.busy) null else vm::load)}
            s.actionError?.let {ErrorState(throwable=it)}
            if (s.draft.uncertain || s.conflict) {
                Text(stringResource(if(s.terminalReceipt) R.string.recovery_target_gone else R.string.author_write_unknown))
                if (s.draft.uncertain && !s.terminalReceipt) AppOutlinedButton(vm::retryPending,enabled=!s.busy && !blocked) {Text(stringResource(R.string.retry_same_request))}
                AppOutlinedButton(vm::reconcile,enabled=!s.busy && !blocked) {Text(stringResource(R.string.refresh))}
                s.latest?.let { Text("${it.name} · ${it.color} · ${it.icon}") }
                if (s.reviewed && s.latest != null && s.conflict) AppActionRow {
                    AppOutlinedButton({vm.resolve(true)}) {Text(stringResource(R.string.keep_question_draft))}
                    AppOutlinedButton({vm.resolve(false)}) {Text(stringResource(R.string.use_server_question))}
                } else if(s.reviewed) AppOutlinedButton(vm::acknowledge) {Text(stringResource(R.string.tools_reviewed))}
            }
        }
        items(s.items,key={it.id}) { item ->
            AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(AppLayoutTokens.denseCardInsets)) {Row {
                val known=categoryIconOptions.firstOrNull {it.value==item.icon}
                Icon(iconFor(known?.nativeIcon ?: MaterialIcons.CATEGORIES),contentDescription=null)
                Text(item.name,Modifier.weight(1f))
            };item.description?.let {Text(it)}
                if(canManage) Column {AppOutlinedButton({if(dirty) {nextEditor=item;discardEditor=true} else vm.edit(item)},enabled=editable) {Text(stringResource(R.string.edit))}
                    AppOutlinedButton({mergeSource=item;mergeTarget=null},enabled=editable && !dirty && currentEditor==null && s.items.size > 1) {Text(stringResource(R.string.category_merge))}
                    AppDangerOutlinedButton({vm.requestDelete(item)},enabled=editable) {Text(stringResource(R.string.ux_delete_category))} }
            } }
        }
    }
    mergeSource?.let { source -> AppConfirmationDialog(onDismissRequest={if(!s.busy) mergeSource=null},
        title={Text(stringResource(R.string.category_merge))},
        text={Column {
            Text(stringResource(R.string.category_merge_impact,source.name))
            LazyColumn(Modifier.selectableGroup()) { items(s.items.filterNot {it.id==source.id},key={it.id}) { target ->
                AppRadioRow(target.name, selected=mergeTarget?.id==target.id, enabled=editable,
                    onClick={mergeTarget=target})
            } }
        }},
        confirmButton={AppButton({val target=mergeTarget;mergeSource=null;mergeTarget=null;vm.merge(source,target)},enabled=editable && mergeTarget!=null) {Text(stringResource(R.string.category_merge))}},
        dismissButton={AppTextButton({mergeSource=null;mergeTarget=null},enabled=!s.busy) {Text(stringResource(R.string.cancel))}})
    }
    if(discardEditor) AppConfirmationDialog(onDismissRequest={discardEditor=false;nextEditor=null},
        title={Text(stringResource(R.string.ux_discard_category_changes))},text={Text(stringResource(R.string.manage_leave_confirm))},
        confirmButton={AppDangerTextButton({val target=nextEditor;discardEditor=false;nextEditor=null;vm.cancelEdit();target?.let(vm::edit)},enabled=!s.busy && !blocked) {Text(stringResource(R.string.ux_discard_category_changes))}},
        dismissButton={AppTextButton({discardEditor=false;nextEditor=null}) {Text(stringResource(R.string.cancel))}})
    if(iconPicker) AppConfirmationDialog(onDismissRequest={iconPicker=false},title={Text(stringResource(R.string.category_icon))}, text={
        LazyColumn(Modifier.selectableGroup()) { items(categoryIconOptions,key={it.value}) { option ->
            AppRadioRow(stringResource(option.label), selected=s.draft.icon==option.value,
                enabled=editable, onClick={vm.field(icon=option.value);iconPicker=false},
                leadingIcon={Icon(iconFor(option.nativeIcon),contentDescription=null)})
        }
        if(categoryIconOptions.none {it.value==s.draft.icon}) item {Text(stringResource(R.string.category_icon_selected,s.draft.icon))} }
    },confirmButton={AppTextButton({iconPicker=false}) {Text(stringResource(R.string.close))}})
    s.confirmDelete?.let { item -> AppConfirmationDialog(onDismissRequest=vm::dismissDelete,title={Text(stringResource(R.string.ux_delete_category))},
        text={Text(stringResource(R.string.category_delete_confirmation,item.name))},confirmButton={AppDangerTextButton(vm::confirmDelete,enabled=editable){Text(stringResource(R.string.ux_delete_category))}},
        dismissButton={AppOutlinedButton(vm::dismissDelete){Text(stringResource(R.string.cancel))}}) }
}
