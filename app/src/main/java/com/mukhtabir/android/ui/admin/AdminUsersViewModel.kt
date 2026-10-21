@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.data.remote.ApiException
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import com.mukhtabir.android.data.remote.AdminUserCreateRequest
import com.mukhtabir.android.data.remote.AdminUserUpdateRequest
import com.mukhtabir.android.data.remote.UserDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

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

private fun accountDays(value: String): Int? {
    val digits=value.trim().map { ch -> Character.digit(ch,10).takeIf { it>=0 }?.toString() ?: return null }.joinToString("")
    val number=digits.toIntOrNull() ?: return null
    val maximum=ChronoUnit.DAYS.between(LocalDate.now(),LocalDate.of(9999,12,30))
    return number.takeIf { it>=0 && it.toLong()<=maximum }
}
