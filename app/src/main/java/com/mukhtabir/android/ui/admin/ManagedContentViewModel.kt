package com.mukhtabir.android.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.data.remote.ApiException
import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.ManagedContentDto
import com.mukhtabir.android.data.remote.ManagedContentService
import com.mukhtabir.android.data.remote.ManagedKind
import com.mukhtabir.android.data.remote.UserPageDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.coroutineContext

internal data class ManagedState(
    val items: List<ManagedContentDto> = emptyList(),val query: String = "",val page: Int = 1,
    val editing: Boolean = false,val baseline: ManagedContentDto? = null,
    val name: String = "",val description: String = "",val active: Boolean = true,
    val weights: Map<Long,String> = emptyMap(),val memberIds: String = "",
    val categories: List<CategoryDto> = emptyList(),val categoriesLoaded: Boolean = false,val categoriesBusy: Boolean = false,
    val busy: Boolean = false,val error: Throwable? = null,val lookupError: Throwable? = null,
    val validation: Int? = null,val message: Int? = null,val memberResult: Pair<Int,Int>? = null,
    val needsReview: Boolean = false,val uncertainCreate: Boolean = false,val reviewed: Boolean = false,
    val deletionPending: Boolean = false,val conflict: ManagedContentDto? = null,val userQuery: String = "",val users: UserPageDto? = null,
    val usersBusy: Boolean = false,val usersError: Throwable? = null,
) {
    val dirty: Boolean get() = editing && (name!=(baseline?.name ?: "") || description!=baseline?.description.orEmpty() ||
        active!=(baseline?.active ?: true) || weights!=(baseline?.weights.orEmpty().mapNotNull { (id,value) ->
            id.toLongOrNull()?.let { it to value.toString() }
        }.toMap()) || memberIds.isNotBlank())
}

internal class ManagedContentViewModel(private val kind: ManagedKind,private val service: ManagedContentService,
    private val categories: CategoryService,private val admin: AdminService,private val permitted: Boolean,
    private val canLookupUsers: Boolean,private val pageSize: Int): ViewModel() {
    private val _state=MutableStateFlow(ManagedState())
    val state=_state.asStateFlow()
    init { if(kind==ManagedKind.BLUEPRINT) loadCategories();refresh() }

    private fun apply(item: ManagedContentDto?) {
        _state.update { it.copy(editing=true,baseline=item,name=item?.name.orEmpty(),description=item?.description.orEmpty(),
            active=item?.active ?: true,weights=item?.weights.orEmpty().mapNotNull { (id,value) -> id.toLongOrNull()?.let { it to value.toString() } }.toMap(),
            memberIds="",conflict=null,needsReview=false,error=null,validation=null) }
    }
    fun create() { if(permitted && !_state.value.busy && !_state.value.editing && !_state.value.needsReview && !_state.value.uncertainCreate) apply(null) }
    fun open(id: Long) {
        if(!permitted || _state.value.busy || _state.value.dirty || _state.value.needsReview || _state.value.uncertainCreate) return
        read { apply(service.detail(kind,id)) }
    }
    fun close() {
        if(_state.value.busy || _state.value.usersBusy) return
        _state.update { it.copy(editing=false,baseline=null,name="",description="",weights=emptyMap(),memberIds="",conflict=null,users=null) }
    }
    fun field(field: String,value: String) {
        if(!permitted || _state.value.busy) return
        _state.update { when(field) {
            "name" -> it.copy(name=value,validation=null)
            "description" -> it.copy(description=value,validation=null)
            "members" -> it.copy(memberIds=value,validation=null)
            "search" -> it.copy(query=value,page=1)
            "users" -> it.copy(userQuery=value)
            else -> it
        } }
    }
    fun active(value: Boolean) { if(permitted && !_state.value.busy) _state.update { it.copy(active=value) } }
    fun weight(id: Long,value: String?) {
        if(!permitted || _state.value.busy || kind!=ManagedKind.BLUEPRINT || id<=0) return
        _state.update { it.copy(weights=if(value==null) it.weights-id else it.weights+(id to value),validation=null) }
    }
    fun page(value: Int) { if(value>0 && !_state.value.busy) _state.update { it.copy(page=value) } }
    private fun read(action: suspend () -> Unit) {
        if(!permitted || _state.value.busy) return
        _state.update { it.copy(busy=true,error=null) }
        viewModelScope.launch {
            try { action();coroutineContext.ensureActive() }
            catch(cancel: CancellationException) { throw cancel }
            catch(error: Exception) { coroutineContext.ensureActive();_state.update { it.copy(error=error) } }
            finally { _state.update { it.copy(busy=false) } }
        }
    }
    fun refresh() = read {
        val s=_state.value
        val rows=service.list(kind);coroutineContext.ensureActive()
        _state.update { it.copy(items=rows,reviewed=true) }
        s.baseline?.let { old ->
            val fresh=try { service.detail(kind,old.id) } catch(error: ApiException) {
                if(s.deletionPending && error.statusCode==404) {
                    _state.update { it.copy(editing=false,baseline=null,needsReview=false,deletionPending=false,message=R.string.manage_deleted) }
                    return@read
                }
                throw error
            }
            coroutineContext.ensureActive()
            _state.update { it.copy(deletionPending=false) }
            if(s.dirty) _state.update { it.copy(conflict=fresh.takeIf { it!=old },needsReview=false) }
            else { apply(fresh);_state.update { it.copy(reviewed=true) } }
        } ?: _state.update { it.copy(needsReview=false) }
    }
    fun resolve(keep: Boolean) {
        val fresh=_state.value.conflict ?: return
        if(_state.value.busy) return
        if(keep) _state.update { it.copy(baseline=fresh,conflict=null) } else apply(fresh)
    }
    fun acknowledgeCreate() {
        if(!_state.value.busy && _state.value.reviewed && !_state.value.needsReview)
            _state.update { it.copy(uncertainCreate=false) }
    }
    fun loadCategories() {
        if(!permitted || kind!=ManagedKind.BLUEPRINT || _state.value.categoriesBusy) return
        _state.update { it.copy(categoriesBusy=true) }
        viewModelScope.launch {
            try { val data=categories.list().items;coroutineContext.ensureActive();_state.update { it.copy(categories=data,categoriesLoaded=true,lookupError=null) } }
            catch(cancel: CancellationException) { throw cancel }
            catch(error: Exception) { _state.update { it.copy(lookupError=error) } }
            finally { _state.update { it.copy(categoriesBusy=false) } }
        }
    }
    fun searchUsers(page: Int=1) {
        if(!permitted || !canLookupUsers || _state.value.usersBusy || _state.value.busy || page<1) return
        val query=_state.value.userQuery
        _state.update { it.copy(usersBusy=true,usersError=null) }
        viewModelScope.launch {
            try { val rows=admin.users(page,pageSize.coerceAtLeast(1),query);coroutineContext.ensureActive();_state.update { it.copy(users=rows) } }
            catch(cancel: CancellationException) { throw cancel }
            catch(error: Exception) { _state.update { it.copy(usersError=error) } }
            finally { _state.update { it.copy(usersBusy=false) } }
        }
    }
    fun selectUser(id: Long) {
        if(!permitted || !canLookupUsers || _state.value.busy || id<=0) return
        val existing=parseMemberIds(_state.value.memberIds) ?: return
        field("members",(if(id in existing) existing-id else existing+id).joinToString(", "))
    }
    fun save() {
        val s=_state.value
        if(!allowed() || !s.editing) return
        val weights=linkedMapOf<Long,Double>()
        if(kind==ManagedKind.BLUEPRINT) {
            if(!s.categoriesLoaded) { _state.update { it.copy(validation=R.string.manage_categories_required) };return }
            for((id,input) in s.weights) {
                val number=normalizeDigits(input.trim()).replace('٫','.').toDoubleOrNull()
                if(number==null || !number.isFinite() || number<0 || s.categories.none { it.id==id }) {
                    _state.update { it.copy(validation=R.string.manage_weights_invalid) };return
                }
                if(number>0) weights[id]=number
            }
        }
        if(s.name.isBlank() || s.name.trim().codePointCount(0,s.name.trim().length)>100 || s.memberIds.isNotBlank()) {
            _state.update { it.copy(validation=R.string.manage_name_invalid) };return
        }
        mutate(prepare={
            if(kind!=ManagedKind.BLUEPRINT) true else {
                val current=categories.list().items;coroutineContext.ensureActive()
                _state.update {it.copy(categories=current,categoriesLoaded=true)}
                if(weights.keys.any {id -> current.none {it.id==id}}) {
                    _state.update {it.copy(validation=R.string.manage_weights_invalid)};false
                } else true
            }
        }) { service.save(kind,s.baseline?.id,s.name.trim(),s.description,s.active,weights,s.baseline?.version).also {
            coroutineContext.ensureActive();apply(it);_state.update { state -> state.copy(message=R.string.saved) }
        } }
    }
    private fun allowed(): Boolean {
        val s=_state.value
        return permitted && !s.busy && !s.usersBusy && !s.needsReview && !s.uncertainCreate && s.conflict==null
    }
    private fun mutate(deleting: Boolean=false,prepare: suspend () -> Boolean = {true},action: suspend () -> Unit) {
        if(!allowed()) return
        val s=_state.value
        _state.update { it.copy(busy=true,error=null,validation=null,reviewed=false) }
        viewModelScope.launch {
            var started=false
            try {
                s.baseline?.let { old ->
                    val fresh=service.detail(kind,old.id);coroutineContext.ensureActive()
                    if(fresh!=old) { _state.update { it.copy(conflict=fresh) };return@launch }
                }
                if(!prepare()) return@launch
                coroutineContext.ensureActive()
                started=true
                if(deleting) _state.update { it.copy(deletionPending=true) }
                action();coroutineContext.ensureActive()
                try { val rows=service.list(kind);coroutineContext.ensureActive();_state.update { it.copy(items=rows) } }
                catch(cancel: CancellationException) { throw cancel }
                catch(error: Exception) { _state.update { it.copy(error=error) } }
            } catch(cancel: CancellationException) { throw cancel }
            catch(error: Exception) {
                val rejected=error is ApiException && error.statusCode in setOf(400,401,403,404,409,422,429)
                _state.update { it.copy(error=error,needsReview=it.needsReview || (started && !rejected),
                    deletionPending=it.deletionPending && !rejected,
                    uncertainCreate=started && !rejected && s.baseline==null) }
            } finally { _state.update { it.copy(busy=false) } }
        }
    }
    fun delete() {
        val s=_state.value;val id=s.baseline?.id ?: return
        if(s.dirty || !allowed()) return
        mutate(deleting=true) { service.delete(kind,id);coroutineContext.ensureActive();_state.update { it.copy(editing=false,baseline=null,deletionPending=false,message=R.string.manage_deleted) } }
    }
    fun addMembers() {
        val s=_state.value;val id=s.baseline?.id ?: return
        if(kind!=ManagedKind.GROUP || !allowed() || metadataDirty(s)) return
        val ids=parseMemberIds(s.memberIds)
        if(ids.isNullOrEmpty()) { _state.update { it.copy(validation=R.string.manage_members_invalid) };return }
        mutate {
            val result=service.addMembers(id,ids);coroutineContext.ensureActive()
            _state.update { it.copy(memberIds="",memberResult=result.added to result.requested,message=R.string.saved,needsReview=true) }
            val fresh=service.detail(kind,id);coroutineContext.ensureActive();apply(fresh)
        }
    }
    fun removeMember(user: Long) {
        val s=_state.value;val id=s.baseline?.id ?: return
        if(kind!=ManagedKind.GROUP || s.dirty || !allowed() || s.baseline.members.none { it.userId==user }) return
        mutate {
            service.removeMember(id,user);coroutineContext.ensureActive()
            _state.update { it.copy(message=R.string.saved,needsReview=true) }
            val fresh=service.detail(kind,id);coroutineContext.ensureActive();apply(fresh)
        }
    }
    private fun metadataDirty(s: ManagedState): Boolean {
        val baseline=s.baseline ?: return true
        return s.name!=baseline.name || s.description!=baseline.description.orEmpty() || s.active!=baseline.active
    }
    class Factory(private val kind: ManagedKind,private val service: ManagedContentService,private val categories: CategoryService,
        private val admin: AdminService,private val permitted: Boolean,private val users: Boolean,private val pageSize: Int): ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST") override fun <T: ViewModel> create(modelClass: Class<T>): T =
            ManagedContentViewModel(kind,service,categories,admin,permitted,users,pageSize) as T
    }
}

internal fun normalizeDigits(input: String)=input.map { char -> Character.digit(char,10).takeIf { it>=0 }?.digitToChar() ?: char }.joinToString("")
internal fun parseMemberIds(input: String): List<Long>? {
    if(input.isBlank()) return emptyList()
    val ids=input.trim().split(Regex("[,،\\s]+"))
    return ids.map { normalizeDigits(it).toLongOrNull()?.takeIf { id -> id>0 } ?: return null }.distinct()
}
