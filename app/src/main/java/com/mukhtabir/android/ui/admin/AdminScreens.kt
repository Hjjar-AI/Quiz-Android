@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.admin

import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppSuccessText

import com.mukhtabir.android.ui.components.AppPagination

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppActionRow

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.Checkbox
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.material3.MaterialTheme
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.util.formatLocalDateTime
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.mukhtabir.android.data.remote.AdminUserCreateRequest
import com.mukhtabir.android.data.remote.AdminUserUpdateRequest
import com.mukhtabir.android.data.remote.RoleCapabilitiesDto
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.data.remote.UserCapabilitiesDto
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.ErrorState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

data class AdminUsersState(
    val users: List<UserDto> = emptyList(), val page: Int = 1, val totalPages: Int = 1,
    val search: String = "", val selected: UserDto? = null, val username: String = "",
    val fullName: String = "", val password: String = "", val adminPassword: String = "",
    val expiryDays: String = "60", val renewalDays: String = "0",
    val role: String = "member", val active: Boolean = true, val loading: Boolean = false,
    val error: Throwable? = null, val validation: Int? = null, val saved: Boolean = false,
    val needsReview: Boolean = false,val createUncertain: Boolean = false,val listFresh: Boolean = false,
    val batchIds: Set<Long> = emptySet(), val batchUnknown: Set<Long> = emptySet(),
    val batchReviewed: Boolean = false, val batchRows: List<UserDto> = emptyList(), val batchMissing: Set<Long> = emptySet(), val batchDesired: Boolean? = null, val batchSuccess: Int = 0, val batchFailed: Int = 0,
)

class AdminUsersViewModel(private val service: AdminService,private val pageSize:Int, private val canManage:Boolean, private val actingUserId: Long) : ViewModel() {
    private val _state = MutableStateFlow(AdminUsersState())
    val state = _state.asStateFlow()
    init { load() }
    fun field(name: String, value: String) {
        if (_state.value.loading || !canManage || _state.value.batchUnknown.isNotEmpty()) return
        _state.update { state ->
            val edited = when (name) {
                "search" -> state.copy(search=value)
                "username" -> state.copy(username=value)
                "name" -> state.copy(fullName=value)
                "password" -> state.copy(password=value)
                "admin" -> state.copy(adminPassword=value)
                "expiry" -> state.copy(expiryDays=value)
                "renewal" -> state.copy(renewalDays=value)
                else -> state.copy(role=value)
            }
            edited.copy(saved=false, validation=null)
        }
    }
    fun active(value: Boolean) { if (canManage && !_state.value.loading && _state.value.batchUnknown.isEmpty()) _state.update { it.copy(active=value, saved=false) } }
    fun load(page: Int = 1) {
        val s = _state.value
        if (!canManage || s.loading || page < 1) return
        _state.update { it.copy(loading=true, error=null) }
        viewModelScope.launch {
            try {
                var p = service.users(page, pageSize, s.search.takeIf(String::isNotBlank))
                if(p.page>p.totalPages) p=service.users(p.totalPages.coerceAtLeast(1),pageSize,s.search.takeIf(String::isNotBlank))
                coroutineContext.ensureActive()
                _state.update { it.copy(users=p.items, page=p.page, totalPages=p.totalPages, loading=false,needsReview=false,listFresh=true) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(loading=false, error=e) } }
        }
    }
    fun select(user: UserDto?) {
        if (_state.value.loading || !canManage || _state.value.needsReview || _state.value.createUncertain || _state.value.batchUnknown.isNotEmpty()) return
        _state.update { s -> s.copy(selected=user, username=user?.username.orEmpty(), fullName=user?.fullName.orEmpty(),
            role=user?.role ?: "member", active=user?.isActive ?: true, password="", adminPassword="",
            expiryDays=if(user == null) "60" else "", renewalDays=(user?.autoRenewDays ?: 0).toString(), saved=false,
            error=null, validation=null) }
    }
    fun save() {
        val s = _state.value
        if (!canManage || s.loading || s.needsReview || s.createUncertain || s.batchUnknown.isNotEmpty()) return
        val expiry = accountDays(s.expiryDays)
        val renewal = accountDays(s.renewalDays)
        val invalidDays = (s.selected == null || s.expiryDays.isNotBlank()) && (expiry == null || expiry < 0)
        if (invalidDays || renewal == null || renewal < 0) {
            _state.update { it.copy(validation=R.string.admin_days_invalid) }; return
        }
        if (s.adminPassword.isBlank() || s.adminPassword.codePointCount(0,s.adminPassword.length)>128 || s.password.codePointCount(0,s.password.length)>128 ||
            s.fullName.codePointCount(0,s.fullName.length)>100 || s.username.trim().codePointCount(0,s.username.trim().length)>150 ||
            (s.selected == null && (s.username.isBlank() || s.password.isBlank()))) {
            _state.update { it.copy(validation=R.string.admin_credentials_required) }; return
        }
        _state.update { it.copy(loading=true, error=null, validation=null, saved=false) }
        viewModelScope.launch {
            var writeStarted=false
            try {
                s.selected?.let { selected ->
                    val fresh=service.user(selected.id)
                    coroutineContext.ensureActive()
                    if(fresh.fullName!=selected.fullName || fresh.role!=selected.role || fresh.isActive!=selected.isActive ||
                        fresh.expiresAt!=selected.expiresAt || fresh.autoRenewDays!=selected.autoRenewDays) {
                        _state.update { it.copy(loading=false,selected=fresh,validation=R.string.account_changed_review) }
                        return@launch
                    }
                }
                writeStarted=true
                val saved = if (s.selected == null) service.createUser(AdminUserCreateRequest(
                    s.username.trim(), s.password, s.fullName, s.role, s.active, requireNotNull(expiry), renewal, s.adminPassword))
                else service.updateUser(s.selected.id, AdminUserUpdateRequest(
                    s.fullName, s.role, s.active, expiry, renewal, s.password.takeIf(String::isNotBlank), s.adminPassword))
                coroutineContext.ensureActive()
                // Keep the confirmed response; a later list failure must not turn this save into a failure.
                _state.update { it.copy(loading=false, users=it.users.map { u -> if(u.id==saved.id) saved else u }) }
                select(saved)
                _state.update { it.copy(saved=true) }
                load(1)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                coroutineContext.ensureActive()
                val certain=e is ApiException && e.statusCode in setOf(400,401,403,404,409,422,429)
                _state.update { it.copy(loading=false,error=e,adminPassword="",needsReview=writeStarted && !certain,
                    createUncertain=writeStarted && !certain && s.selected==null,listFresh=false) }
            }
        }
    }
    fun acknowledgeCreateReview() {
        val s=_state.value
        if(!canManage || s.loading || !s.listFresh || s.needsReview) return
        _state.update { it.copy(createUncertain=false) }
    }
    fun selectBatch(id: Long) {
        val s=_state.value
        if(!canManage || s.loading || s.needsReview || s.createUncertain || s.batchUnknown.isNotEmpty() ||
            id==actingUserId || s.users.none { it.id==id && !it.isStub }) return
        _state.update { it.copy(batchIds=if(id in it.batchIds) it.batchIds-id else it.batchIds+id) }
    }
    fun clearBatch() {if(!_state.value.loading && _state.value.batchUnknown.isEmpty()) _state.update {it.copy(batchIds=emptySet())}}
    fun batch(active: Boolean, password: String) {
        val s=_state.value
        if(!canManage || s.loading || s.needsReview || s.createUncertain || s.batchUnknown.isNotEmpty() ||
            s.batchIds.isEmpty() || s.selected!=null || s.username.isNotBlank() || s.fullName.isNotBlank() ||
            s.password.isNotEmpty() || password.isBlank() || password.length>128) return
        val ids=s.batchIds.filter {it!=actingUserId}
        _state.update {it.copy(loading=true,error=null,batchSuccess=0,batchFailed=0,batchReviewed=false,batchDesired=active,batchRows=emptyList(),batchMissing=emptySet())}
        viewModelScope.launch {
            try {
                for(id in ids) {
                    try {
                        service.setUserActive(id,password,active)
                        coroutineContext.ensureActive()
                        _state.update {it.copy(users=it.users.map {row->if(row.id==id) row.copy(isActive=active) else row},
                            batchIds=it.batchIds-id,batchSuccess=it.batchSuccess+1)}
                    } catch(cancel: CancellationException) {throw cancel}
                    catch(failure: Exception) {
                        coroutineContext.ensureActive()
                        val rejected=failure is ApiException && failure.statusCode in setOf(400,401,403,404,405,409,422,429)
                        _state.update {it.copy(error=failure,batchFailed=it.batchFailed+if(rejected) 1 else 0,
                            batchUnknown=if(rejected) it.batchUnknown else it.batchUnknown+id)}
                        if(!rejected || failure is ApiException && failure.statusCode in setOf(401,403,429)) break
                    }
                }
            } finally {_state.update {it.copy(loading=false)}}
        }
    }
    fun reviewBatch() {
        val s=_state.value
        if(!canManage || s.loading || s.batchUnknown.isEmpty()) return
        _state.update {it.copy(loading=true,error=null,batchReviewed=false,batchRows=emptyList(),batchMissing=emptySet())}
        viewModelScope.launch {
            try {
                for(id in s.batchUnknown) {
                    try {
                        val row=service.user(id);coroutineContext.ensureActive()
                        _state.update {it.copy(users=it.users.map {old->if(old.id==id) row else old},batchRows=it.batchRows+row)}
                    } catch(failure: ApiException) {if(failure.statusCode!=404) throw failure
                        _state.update {it.copy(users=it.users.filterNot {row->row.id==id},batchIds=it.batchIds-id,batchMissing=it.batchMissing+id)}}
                }
                _state.update {it.copy(batchReviewed=true)}
            } catch(cancel: CancellationException) {throw cancel}
            catch(failure: Exception) {_state.update {it.copy(error=failure)}}
            finally {_state.update {it.copy(loading=false)}}
        }
    }
    fun acknowledgeBatch() {
        if(!canManage || _state.value.loading || !_state.value.batchReviewed) return
        _state.update {it.copy(batchUnknown=emptySet(),batchReviewed=false)}
    }
    class Factory(private val service: AdminService,private val pageSize:Int,private val canManage:Boolean,private val actingUserId: Long): ViewModelProvider.Factory { @Suppress("UNCHECKED_CAST") override fun <T:ViewModel> create(modelClass:Class<T>):T=AdminUsersViewModel(service,pageSize.coerceAtLeast(1),canManage,actingUserId) as T }
}

@Composable fun AdminUsersRoute(service: AdminService,pageSize:Int,roles:Map<String,String>,canManage:Boolean,actingUserId:Long,onOpenUser:(Long)->Unit,onPermissions:((Long)->Unit)? = null,onBusyChanged:(Boolean)->Unit = {}) {
    val vm:AdminUsersViewModel=viewModel(key="admin-users-$actingUserId-$pageSize-$canManage",factory=AdminUsersViewModel.Factory(service,pageSize,canManage,actingUserId)); val s by vm.state.collectAsState()
    RefreshOnResume { vm.load(s.page) }
    com.mukhtabir.android.ui.components.BlockedBackHandler(enabled = s.loading)
    androidx.compose.runtime.DisposableEffect(s.loading) {onBusyChanged(s.loading);onDispose {onBusyChanged(false)}}
    var batchAction by androidx.compose.runtime.remember { mutableStateOf<Boolean?>(null) }
    var batchPassword by androidx.compose.runtime.remember { mutableStateOf("") }
    var openTools by rememberSaveable { mutableStateOf<Long?>(null) }
    if(openTools!=null) AlertDialog(onDismissRequest={openTools=null},
        title={Text(stringResource(R.string.account_tools_title))},text={Text(stringResource(R.string.account_open_tools_confirm))},
        confirmButton={TextButton({val id=requireNotNull(openTools);vm.select(null);openTools=null;onOpenUser(id)},enabled=!s.loading){Text(stringResource(R.string.confirm))}},
        dismissButton={TextButton({openTools=null}){Text(stringResource(R.string.cancel))}})
    batchAction?.let { active -> AlertDialog(onDismissRequest={batchAction=null;batchPassword=""},
        title={Text(stringResource(if(active) R.string.admin_batch_activate else R.string.admin_batch_deactivate))},
        text={Column {Text(stringResource(R.string.moderation_confirm_count,s.batchIds.size))
            OutlinedTextField(batchPassword,{batchPassword=it},visualTransformation=PasswordVisualTransformation(),
                label={Text(stringResource(R.string.current_admin_password))},enabled=!s.loading,
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )}},
        confirmButton={TextButton({val secret=batchPassword;batchPassword="";batchAction=null;vm.batch(active,secret)},
            enabled=!s.loading && batchPassword.isNotBlank()){Text(stringResource(R.string.confirm))}},
        dismissButton={TextButton({batchAction=null;batchPassword=""}){Text(stringResource(R.string.cancel))}}) }
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item { AppPageHeading(stringResource(R.string.nav_admin_users)) }
        item { Row(horizontalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
            OutlinedTextField(s.search,{vm.field("search",it)},label={Text(stringResource(R.string.search))},enabled=!s.loading,modifier=Modifier.weight(1f))
            Button({vm.load(1)},enabled=!s.loading) { Text(stringResource(R.string.search)) }
        } }
        s.error?.let { item { ErrorState(throwable = it, onRetry = if(s.loading) null else { { vm.load(s.page) } }) } }
        s.validation?.let { item { Text(stringResource(it), color=MaterialTheme.colorScheme.error) } }
        if(s.saved) item { AppSuccessText(stringResource(R.string.saved)) }
        if(s.needsReview || s.createUncertain) item {
            Text(stringResource(R.string.admin_user_uncertain))
            if(s.createUncertain) OutlinedButton(vm::acknowledgeCreateReview,enabled=!s.loading && s.listFresh && !s.needsReview) {
                Text(stringResource(R.string.quiz_finish_review_done))
            }
        }
        item { Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
            Text(stringResource(if(s.selected==null) R.string.create_user else R.string.edit_user),style=MaterialTheme.typography.titleMedium)
            s.selected?.let { user -> OutlinedButton({openTools=user.id},enabled=!s.loading){Text(stringResource(R.string.account_tools_title))} }
            OutlinedTextField(s.username,{vm.field("username",it)},label={Text(stringResource(R.string.username))},enabled=!s.loading && s.selected==null,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
            OutlinedTextField(s.fullName,{vm.field("name",it)},label={Text(stringResource(R.string.full_name))},enabled=!s.loading,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(s.password,{vm.field("password",it)},label={Text(stringResource(if(s.selected==null) R.string.password else R.string.new_password_optional))},visualTransformation=PasswordVisualTransformation(),enabled=!s.loading,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
            s.selected?.let { selected -> Text(stringResource(R.string.admin_current_expiry,
                selected.expiresAt?.let(::formatLocalDateTime) ?: stringResource(R.string.none))) }
            Text(stringResource(R.string.admin_expiry_hint))
            AppActionRow() {
                if(s.selected!=null) FilterChip(s.expiryDays.isBlank(),{vm.field("expiry","")},{Text(stringResource(R.string.admin_keep_expiry))},enabled=!s.loading)
                listOf(0,30,60,90,365).forEach { days ->
                    FilterChip(s.expiryDays==days.toString(),{vm.field("expiry",days.toString())},
                        {Text(if(days==0) stringResource(R.string.admin_no_expiry) else stringResource(R.string.group_days,com.mukhtabir.android.util.formatNumber(days)))},enabled=!s.loading)
                }
            }
            OutlinedTextField(s.expiryDays,{vm.field("expiry",it)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),label={Text(stringResource(R.string.expiry_days))},enabled=!s.loading,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
            OutlinedTextField(s.renewalDays,{vm.field("renewal",it)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),label={Text(stringResource(R.string.renewal_days))},enabled=!s.loading,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
            AppActionRow(){ (roles.keys.ifEmpty{setOf("member","moderator","admin")}+s.role).forEach { role -> FilterChip(s.role==role,{vm.field("role",role)},{Text(com.mukhtabir.android.util.roleLabel(role, roles[role]))},enabled=!s.loading) } }
            Row(verticalAlignment=Alignment.CenterVertically){Text(stringResource(R.string.active),Modifier.weight(1f));Switch(s.active,vm::active,enabled=!s.loading)}
            OutlinedTextField(s.adminPassword,{vm.field("admin",it)},label={Text(stringResource(R.string.current_admin_password))},visualTransformation=PasswordVisualTransformation(),enabled=!s.loading,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
            Row(horizontalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)){Button(vm::save,enabled=!s.loading && !s.needsReview && !s.createUncertain){Text(stringResource(R.string.save))}; if(s.selected!=null) OutlinedButton({vm.select(null)},enabled=!s.loading){Text(stringResource(R.string.cancel))}}
        } } }
        item {
            if(s.batchIds.isNotEmpty()) {
                Text(stringResource(R.string.moderation_selected,s.batchIds.size))
                val ready=!s.loading && !s.needsReview && !s.createUncertain && s.batchUnknown.isEmpty() && s.selected==null &&
                    s.username.isBlank() && s.fullName.isBlank() && s.password.isEmpty()
                Button({batchAction=true},enabled=ready){Text(stringResource(R.string.admin_batch_activate))}
                OutlinedButton({batchAction=false},enabled=ready){Text(stringResource(R.string.admin_batch_deactivate))}
                TextButton(vm::clearBatch,enabled=!s.loading && s.batchUnknown.isEmpty()){Text(stringResource(R.string.clear_selection))}
            }
            if(s.batchSuccess+s.batchFailed+s.batchUnknown.size>0) Text(stringResource(R.string.admin_batch_result,s.batchSuccess,s.batchFailed,s.batchUnknown.size))
            if(s.batchUnknown.isNotEmpty()) {
                Text(stringResource(R.string.admin_batch_unknown))
                Text(stringResource(if(s.batchDesired==true) R.string.admin_batch_activate else R.string.admin_batch_deactivate))
                s.batchRows.forEach {row->Text(row.displayName+": "+stringResource(if(row.isActive) R.string.active else R.string.inactive))}
                s.batchMissing.forEach {id->Text(com.mukhtabir.android.util.bidi("#${com.mukhtabir.android.util.formatNumber(id)}")+" · "+stringResource(R.string.no_items))}
                OutlinedButton(vm::reviewBatch,enabled=!s.loading){Text(stringResource(R.string.refresh))}
                TextButton(vm::acknowledgeBatch,enabled=!s.loading && s.batchReviewed){Text(stringResource(R.string.content_reviewed))}
            }
        }
        if(s.loading) item { AppLoadingIndicator() }
        items(s.users,key={it.id}) { u -> Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(u.id in s.batchIds,{vm.selectBatch(u.id)},enabled=!s.loading && !s.needsReview && !s.createUncertain && s.batchUnknown.isEmpty() && !u.isStub && u.id!=actingUserId,modifier=Modifier.semantics {contentDescription=u.displayName})
                Text(u.displayName,style=MaterialTheme.typography.titleMedium)
            }
            Text(com.mukhtabir.android.util.accountIdentity("", u.username, u.id))
            Text("${com.mukhtabir.android.util.roleLabel(u.role)} · ${if(u.isActive) stringResource(R.string.active) else stringResource(R.string.inactive)}")
            AdminUserOverview(u)
            OutlinedButton({vm.select(u)},enabled=!s.loading && s.batchUnknown.isEmpty()){Text(stringResource(R.string.edit_user))}
            onPermissions?.let {open->OutlinedButton({open(u.id)},enabled=!s.loading && s.batchUnknown.isEmpty()){Text(stringResource(R.string.nav_admin_permissions))}}
        } } }
        if(s.totalPages>1) item { AppPagination(s.page, s.totalPages, onPrevious = {vm.load(s.page-1)}, onNext = {vm.load(s.page+1)}, previousEnabled = !s.loading && s.page>1, nextEnabled = !s.loading && s.page<s.totalPages, modifier = Modifier.fillMaxWidth()) }
    }
}

private fun accountDays(value: String): Int? {
    val digits=value.trim().map { ch -> Character.digit(ch,10).takeIf { it>=0 }?.toString() ?: return null }.joinToString("")
    val number=digits.toIntOrNull() ?: return null
    val maximum=ChronoUnit.DAYS.between(LocalDate.now(),LocalDate.of(9999,12,30))
    return number.takeIf { it>=0 && it.toLong()<=maximum }
}
