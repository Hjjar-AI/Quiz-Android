@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.CapabilityGroupDto
import com.mukhtabir.android.data.remote.PermissionUserDto
import com.mukhtabir.android.data.remote.PermissionUserPageDto
import com.mukhtabir.android.data.remote.RoleCapabilitiesDto
import com.mukhtabir.android.data.remote.UserCapabilitiesDto
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppPagination
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppSuccessText
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.BlockedBackHandler
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal data class PermissionsState(
    val data: RoleCapabilitiesDto? = null, val role: String = "member", val selected: Set<String> = emptySet(),
    val user: UserCapabilitiesDto? = null, val selectedUser: PermissionUserDto? = null,
    val overrides: Map<String, Boolean> = emptyMap(), val query: String = "", val appliedQuery: String = "",
    val users: PermissionUserPageDto? = null, val requestedQuery: String = "", val requestedPage: Int = 1,
    val loading: Boolean = false,
    val error: Throwable? = null, val lookupError: Throwable? = null, val notice: Int? = null,
    val saved: Boolean = false, val uncertain: String? = null,
) {
    val roleDirty: Boolean get() = data != null && selected != data.roles[role].orEmpty().toSet()
    val userDirty: Boolean get() = user != null && overrides != user.overrides
}

internal class PermissionsViewModel(private val service: AdminService, private val canManage: Boolean,
    private val pageSize: Int) : ViewModel() {
    private val _state = MutableStateFlow(PermissionsState())
    val state = _state.asStateFlow()
    init { loadCatalog() }
    private fun editable() = canManage && !_state.value.loading && _state.value.uncertain == null
    private fun read(action: suspend () -> Unit) {
        if (!canManage || _state.value.loading) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            try { action(); coroutineContext.ensureActive() }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { _state.update { it.copy(error = failure) } }
            finally { _state.update { it.copy(loading = false) } }
        }
    }
    fun loadCatalog() {
        if (!editable()) return
        read {
            val data = service.permissions(); coroutineContext.ensureActive()
            _state.update { s -> s.copy(data = data,
                selected = if (s.data == null) data.roles[s.role].orEmpty().toSet() else s.selected) }
        }
    }
    fun role(value: String, discardDraft: Boolean = false) {
        val s = _state.value
        if (!editable() || value == s.role || value == "admin" || value !in s.data?.roles.orEmpty() || (s.roleDirty && !discardDraft)) return
        _state.update { it.copy(role = value, selected = it.data?.roles?.get(value).orEmpty().toSet(), saved = false, notice = null) }
    }
    fun toggle(cap: String) {
        if (!editable() || cap !in _state.value.data?.capabilities.orEmpty()) return
        _state.update { it.copy(selected = if (cap in it.selected) it.selected - cap else it.selected + cap, saved = false) }
    }
    fun query(value: String) { if (editable()) _state.update { it.copy(query = value) } }
    fun search(page: Int = 1, retry: Boolean = false) {
        if (!editable() || page < 1) return
        val requestedPage = if (retry) _state.value.requestedPage else page
        val query = if (retry) _state.value.requestedQuery else if (page != 1) _state.value.appliedQuery else _state.value.query.trim()
        _state.update { it.copy(lookupError = null, requestedQuery = query, requestedPage = requestedPage) }
        read {
            try {
                var result = service.permissionUsers(requestedPage, pageSize, query)
                if (result.page > result.totalPages && result.totalPages > 0)
                    result = service.permissionUsers(result.totalPages, pageSize, query)
                coroutineContext.ensureActive()
                _state.update { it.copy(users = result, appliedQuery = query) }
            } catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) { _state.update { it.copy(lookupError = failure) } }
        }
    }
    fun openTarget(id: Long) {
        if(!editable() || id<=0 || _state.value.userDirty) return
        read {
            val result=service.userPermissions(id);coroutineContext.ensureActive()
            _state.update {it.copy(user=result,selectedUser=PermissionUserDto(result.userId,result.username,role=result.role),overrides=result.overrides,saved=false)}
        }
    }
    fun select(user: PermissionUserDto, discardDraft: Boolean = false) {
        if (!editable() || _state.value.selectedUser?.id == user.id || (_state.value.userDirty && !discardDraft) || _state.value.users?.items?.none { it.id == user.id } != false) return
        _state.update { it.copy(selectedUser = user, user = null, overrides = emptyMap(), saved = false, notice = null) }
        loadUser()
    }
    fun loadUser() {
        if (!editable() || _state.value.userDirty) return
        val id = _state.value.selectedUser?.id ?: return
        read {
            val result = service.userPermissions(id); coroutineContext.ensureActive()
            _state.update { it.copy(user = result, overrides = result.overrides) }
        }
    }
    fun override(cap: String, value: Boolean?) {
        if (!editable() || cap !in _state.value.data?.capabilities.orEmpty() || _state.value.user == null) return
        _state.update { it.copy(overrides = it.overrides.toMutableMap().apply {
            if (value == null) remove(cap) else put(cap, value)
        }, saved = false) }
    }
    private fun write(kind: String, action: suspend (() -> Unit) -> Unit) {
        if (!editable()) return
        _state.update { it.copy(loading = true, error = null, notice = null, saved = false) }
        viewModelScope.launch {
            var dispatched = false
            try { action { dispatched = true }; coroutineContext.ensureActive() }
            catch (cancel: CancellationException) { throw cancel }
            catch (failure: Exception) {
                val certain = failure is ApiException && failure.statusCode in setOf(400, 401, 403, 404, 409, 422, 429)
                _state.update { it.copy(error = failure, uncertain = if (!dispatched || certain) null else kind) }
            } finally { _state.update { it.copy(loading = false) } }
        }
    }
    fun saveRole() {
        val s = _state.value; val baseline = s.data ?: return
        if (s.role == "admin" || s.role !in baseline.roles || (s.userDirty && s.user?.role == s.role)) return
        write("role") { dispatch ->
            val fresh = service.permissions(); coroutineContext.ensureActive()
            if (fresh.roles[s.role].orEmpty().toSet() != baseline.roles[s.role].orEmpty().toSet()) {
                _state.update { it.copy(data = fresh, notice = R.string.permissions_changed) }
                return@write
            }
            dispatch()
            val result = service.updateRole(s.role, s.selected.sorted()); coroutineContext.ensureActive()
            _state.update { it.copy(data = fresh.copy(roles = fresh.roles + (result.role to result.capabilities)),
                selected = result.capabilities.toSet(), saved = true,
                user = it.user?.takeUnless { u -> u.role == result.role }) }
        }
    }
    fun saveUser() {
        val s = _state.value; val user = s.user ?: return
        if (s.selectedUser?.id != user.userId) return
        write("user") { dispatch ->
            val fresh = service.userPermissions(user.userId); coroutineContext.ensureActive()
            if (fresh.role != user.role || fresh.overrides != user.overrides || fresh.roleCapabilities != user.roleCapabilities) {
                _state.update { it.copy(user = fresh, notice = R.string.permissions_changed) }
                return@write
            }
            dispatch()
            val result = service.updateUserPermissions(user.userId, s.overrides); coroutineContext.ensureActive()
            _state.update { it.copy(user = fresh.copy(overrides = result.overrides, resolved = result.resolved),
                overrides = result.overrides, saved = true) }
        }
    }
    /** Only GETs resolve an uncertain write; a differing draft requires another explicit Save. */
    fun reconcile() {
        val s = _state.value; val kind = s.uncertain ?: return
        read {
            if (kind == "role") {
                val fresh = service.permissions(); coroutineContext.ensureActive()
                _state.update { it.copy(data = fresh, uncertain = null,
                    saved = fresh.roles[s.role].orEmpty().toSet() == s.selected, notice = R.string.permissions_reconciled) }
            } else {
                val fresh = service.userPermissions(requireNotNull(s.selectedUser).id); coroutineContext.ensureActive()
                _state.update { it.copy(user = fresh, uncertain = null, saved = fresh.overrides == s.overrides,
                    notice = R.string.permissions_reconciled) }
            }
        }
    }
    class Factory(private val service: AdminService, private val canManage: Boolean, private val pageSize: Int) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T : ViewModel> create(modelClass: Class<T>): T =
            PermissionsViewModel(service, canManage, pageSize.coerceIn(1, 500)) as T
    }
}

private data class PermissionDestination(val role: String? = null, val user: PermissionUserDto? = null, val exit: Boolean = false)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun PermissionsRoute(service: AdminService, canManage: Boolean, actingUserId: Long, pageSize: Int, roles: Map<String, String>, onBusyChanged: (Boolean) -> Unit, onBack: () -> Unit, initialUserId: Long? = null) {
    val vm: PermissionsViewModel = viewModel(key = "admin-permissions-$actingUserId-$canManage-$pageSize-$initialUserId",
        factory = PermissionsViewModel.Factory(service, canManage, pageSize))
    val s by vm.state.collectAsLifecycleState()
    var requestedTarget by androidx.compose.runtime.remember(initialUserId) {mutableStateOf(false)}
    LaunchedEffect(initialUserId,s.data,s.loading) {
        if(!requestedTarget && initialUserId!=null && s.data!=null && !s.loading) {requestedTarget=true;vm.openTarget(initialUserId)}
    }
    var destination by androidx.compose.runtime.remember { mutableStateOf<PermissionDestination?>(null) }
    val leave: () -> Unit = {
        if (!s.loading) {
            if (s.roleDirty || s.userDirty || s.uncertain != null) destination = PermissionDestination(exit = true)
            else onBack()
        }
    }
    BackHandler(onBack = leave)
    BlockedBackHandler(enabled = s.loading)
    destination?.let { target ->
        AppConfirmationDialog(onDismissRequest = { destination = null },
            title = { Text(stringResource(R.string.permissions_unsaved_title)) },
            text = { Text(stringResource(if (s.uncertain != null) R.string.author_leave_unknown else R.string.permissions_unsaved_body)) },
            confirmButton = { AppDangerTextButton({
                destination = null
                when {
                    target.exit -> onBack()
                    target.role != null -> vm.role(target.role, discardDraft = true)
                    target.user != null -> vm.select(target.user, discardDraft = true)
                }
            }, enabled = !s.loading && (target.exit || s.uncertain == null)) { Text(stringResource(R.string.discard_changes_continue)) } },
            dismissButton = { AppTextButton({ destination = null }) { Text(stringResource(R.string.keep_editing)) } })
    }
    DisposableEffect(s.loading) {
        onBusyChanged(s.loading)
        onDispose { onBusyChanged(false) }
    }
    val enabled = canManage && !s.loading && s.uncertain == null
    val catalog = s.data
    // Preserve the server's grouping; include any future ungrouped catalog entries.
    val groups = catalog?.groups.orEmpty() + listOfNotNull(catalog?.let { data ->
        val missing = data.capabilities - data.groups.flatMap { it.capabilities }.toSet()
        if (missing.isEmpty()) null else CapabilityGroupDto(stringResource(R.string.permissions_other), missing)
    })
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item { AppPageHeading(stringResource(R.string.nav_admin_permissions)) }
        if (s.loading) item { AppLoadingIndicator() }
        s.error?.let { failure -> item {
            ErrorState(throwable = failure, onRetry = if (!enabled) null else if (catalog == null) vm::loadCatalog else initialUserId?.takeIf {s.selectedUser==null && s.user==null}?.let {target->{vm.openTarget(target)}})
        } }
        s.notice?.let { item { Text(stringResource(it), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) } }
        if (s.saved) item { AppSuccessText(stringResource(R.string.saved)) }
        if (s.uncertain != null) item {
            Text(stringResource(R.string.permissions_uncertain))
            AppOutlinedButton(vm::reconcile, enabled = !s.loading) { Text(stringResource(R.string.refresh)) }
        }
        item { AppSection(stringResource(R.string.role_capabilities), initiallyExpanded = true) {
            AppActionRow() {
                catalog?.roles?.keys?.filter { it != "admin" }?.forEach { role ->
                    AppFilterChip(s.role == role, { if (role != s.role && s.roleDirty) destination = PermissionDestination(role = role) else vm.role(role) }, { Text(com.mukhtabir.android.util.roleLabel(role, roles[role])) }, enabled = enabled)
                }
            }
            groups.forEach { group -> AppSection(group.label) {
                group.capabilities.distinct().forEach { cap ->
                    Row(Modifier.fillMaxWidth().toggleable(value = cap in s.selected, enabled = enabled, role = Role.Checkbox, onValueChange = { vm.toggle(cap) }), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(cap in s.selected, null, enabled = enabled)
                        Column(Modifier.weight(1f)) {
                            Text(if(cap=="tests.download_full_bank") stringResource(R.string.offline_bank_download) else cap)
                            if(cap=="tests.download_full_bank") Text(stringResource(R.string.offline_bank_capability_description),style=MaterialTheme.typography.bodySmall)
                            Text(stringResource(if (cap in catalog?.roles?.get(s.role).orEmpty()) R.string.permissions_role_allow else R.string.permissions_role_deny), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            } }
            if (s.userDirty && s.user?.role == s.role) Text(stringResource(R.string.permissions_save_user_first))
            AppButton(vm::saveRole, enabled = enabled && catalog != null && !(s.userDirty && s.user?.role == s.role)) { Text(stringResource(R.string.save_role_capabilities)) }
        } }
        item { AppSection(stringResource(R.string.user_overrides), initiallyExpanded = true) {
            AppTextField(s.query, vm::query, label = { Text(stringResource(R.string.permissions_search_user)) },
                enabled = enabled, modifier = Modifier.fillMaxWidth())
            AppOutlinedButton({ vm.search() }, enabled = enabled) { Text(stringResource(R.string.search)) }
            s.lookupError?.let { ErrorState(throwable = it, onRetry = if (enabled) ({ vm.search(retry = true) }) else null) }
            s.users?.let { page ->
                Text(stringResource(R.string.permissions_results_query, s.appliedQuery.ifBlank { stringResource(R.string.none) }))
                if (page.items.isEmpty() && !s.loading && s.lookupError==null) EmptyState(stringResource(R.string.permissions_no_users))
                page.items.forEach { user ->
                    AppOutlinedButton({ if (s.selectedUser?.id != user.id && s.userDirty) destination = PermissionDestination(user = user) else vm.select(user) }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.weight(1f)) {
                            Text(user.displayName)
                            Text("${com.mukhtabir.android.util.bidi("@${user.username}")} · ${com.mukhtabir.android.util.roleLabel(user.role, roles[user.role])} · " +
                                stringResource(if (user.isActive) R.string.active else R.string.inactive), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (page.totalPages > 1) {
                    AppPagination(page.page, page.totalPages, onPrevious = { vm.search(page.page - 1) }, onNext = { vm.search(page.page + 1) }, previousEnabled = enabled, nextEnabled = enabled)
                }
            }
            s.selectedUser?.let { selected ->
                Text(selected.displayName, style = MaterialTheme.typography.titleMedium)
                if (s.user == null) AppOutlinedButton(vm::loadUser, enabled = enabled) { Text(stringResource(R.string.retry)) }
            }
            s.user?.let { user ->
                Text("${com.mukhtabir.android.util.bidi("@${user.username}")} · ${com.mukhtabir.android.util.roleLabel(user.role, roles[user.role])}", style = MaterialTheme.typography.titleMedium)
                groups.forEach { group -> AppSection(group.label) {
                    group.capabilities.distinct().forEach { cap ->
                        Text(if(cap=="tests.download_full_bank") stringResource(R.string.offline_bank_download) else cap, style = MaterialTheme.typography.titleSmall)
                        if(cap=="tests.download_full_bank") Text(stringResource(R.string.offline_bank_capability_description),style=MaterialTheme.typography.bodySmall)
                        Text(stringResource(if (cap in user.resolved) R.string.permissions_effective_allow else R.string.permissions_effective_deny))
                        Text(stringResource(if (cap in user.roleCapabilities) R.string.permissions_role_allow else R.string.permissions_role_deny), style = MaterialTheme.typography.bodySmall)
                        AppActionRow() {
                            listOf<Boolean?>(null, true, false).forEach { value ->
                                AppFilterChip(s.overrides[cap] == value, { vm.override(cap, value) }, label = {
                                    Text(stringResource(when (value) { true -> R.string.allow; false -> R.string.deny; null -> R.string.inherit }))
                                }, enabled = enabled)
                            }
                        }
                    }
                } }
                AppButton(vm::saveUser, enabled = enabled) { Text(stringResource(R.string.save_user_overrides)) }
            }
        } }
    }
}
