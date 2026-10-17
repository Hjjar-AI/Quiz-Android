@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.admin

import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppPagination

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppDangerOutlinedButton

import com.mukhtabir.android.ui.components.AppActionRow

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.Checkbox
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.ManagedContentService
import com.mukhtabir.android.data.remote.ManagedKind
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume

@Composable
fun ManagedContentRoute(kind: ManagedKind,service: ManagedContentService,categories: CategoryService,
    admin: AdminService,userId: Long,permitted: Boolean,canLookupUsers: Boolean,pageSize: Int,
    onBack: () -> Unit,onBusy: (Boolean) -> Unit) {
    val vm: ManagedContentViewModel=viewModel(key="manage-$kind-$userId-$permitted-$canLookupUsers",
        factory=ManagedContentViewModel.Factory(kind,service,categories,admin,permitted,canLookupUsers,pageSize))
    val s by vm.state.collectAsState()
    var confirmation by remember { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf<Long?>(null) }
    val busy=s.busy || s.usersBusy
    val back: () -> Unit={ if(!busy) {
        if(s.dirty || s.needsReview || s.uncertainCreate) confirmation="leave"
        else if(s.editing) vm.close() else onBack()
    } }
    BackHandler(onBack=back)
    DisposableEffect(busy) { onBusy(busy);onDispose { onBusy(false) } }
    RefreshOnResume(vm::refresh)
    if(!permitted) { Text(stringResource(R.string.error_forbidden));return }
    val title=stringResource(if(kind==ManagedKind.GROUP) R.string.manage_groups else R.string.manage_blueprints)
    val activeLabel=stringResource(R.string.active)
    val enabled=!busy && !s.needsReview && !s.uncertainCreate && s.conflict==null
    if(confirmation!=null || removing!=null) AlertDialog(onDismissRequest={if(!busy) {confirmation=null;removing=null}},
        title={Text(stringResource(when {
            removing != null -> R.string.ux_remove_group_member
            confirmation == "delete" -> if (kind == ManagedKind.GROUP) R.string.ux_delete_group else R.string.ux_delete_blueprint
            else -> R.string.confirm
        }))},
        text={Text(stringResource(when {
            removing!=null -> R.string.manage_remove_member_confirm
            confirmation=="delete" -> if(kind==ManagedKind.GROUP) R.string.manage_delete_group_confirm else R.string.manage_delete_blueprint_confirm
            else -> R.string.manage_leave_confirm
        }))},
        confirmButton={TextButton({
            val member=removing
            when {
                member!=null -> vm.removeMember(member)
                confirmation=="delete" -> vm.delete()
                else -> { if(s.needsReview || s.uncertainCreate) onBack() else vm.close() }
            }
            confirmation=null;removing=null
        },enabled=!busy) { Text(stringResource(when {
            removing != null -> R.string.ux_remove_group_member
            confirmation == "delete" -> if (kind == ManagedKind.GROUP) R.string.ux_delete_group else R.string.ux_delete_blueprint
            else -> R.string.confirm
        })) }},
        dismissButton={TextButton({confirmation=null;removing=null},enabled=!busy) { Text(stringResource(R.string.cancel)) }})
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item { Text(title) }
        item { AppActionRow() {
            OutlinedButton(back,enabled=!busy) { Text(stringResource(R.string.back)) }
            OutlinedButton(vm::refresh,enabled=!busy) { Text(stringResource(R.string.refresh)) }
        } }
        if(busy) item { AppLoadingIndicator() }
        s.error?.let { item { ErrorState(throwable=it,onRetry=if(!busy) vm::refresh else null) } }
        s.validation?.let { item { Text(stringResource(it),Modifier.semantics {liveRegion=LiveRegionMode.Polite}) } }
        s.message?.let { item { Text(stringResource(it)) } }
        s.memberResult?.let { (added,requested) -> item { Text(stringResource(R.string.manage_members_added,added,requested)) } }
        if(s.needsReview || s.uncertainCreate) item {
            Text(stringResource(R.string.manage_uncertain))
            if(s.uncertainCreate) OutlinedButton(vm::acknowledgeCreate,enabled=s.reviewed && !busy && !s.needsReview) {
                Text(stringResource(R.string.quiz_finish_review_done))
            }
        }
        s.conflict?.let { server -> item {
            Text(stringResource(R.string.account_changed_review))
            Text(server.name);Text(server.description.orEmpty());Text(server.weights.toString())
            Text(stringResource(if(server.active) R.string.active else R.string.inactive))
            com.mukhtabir.android.ui.components.AppMetadataRow(listOf(stringResource(R.string.member_count,server.members.size)))
            AppActionRow() {
                OutlinedButton({vm.resolve(false)},enabled=!busy) { Text(stringResource(R.string.use_server_question)) }
                OutlinedButton({vm.resolve(true)},enabled=!busy) { Text(stringResource(R.string.keep_question_draft)) }
            }
        } }
        if(!s.editing) {
            item { OutlinedTextField(s.query,{vm.field("search",it)},label={Text(stringResource(R.string.search))},modifier=Modifier.fillMaxWidth()) }
            item { Button(vm::create,enabled=enabled) { Text(stringResource(R.string.create)) } }
            val filtered=s.items.filter { s.query.isBlank() || it.name.contains(s.query,true) || it.id.toString()==s.query.trim() }
            val size=pageSize.coerceAtLeast(1)
            val last=((filtered.size+size-1)/size).coerceAtLeast(1)
            val page=s.page.coerceIn(1,last)
            items(filtered.drop((page-1)*size).take(size),key={"row-${it.id}"}) { row ->
                Card(onClick={vm.open(row.id)},enabled=enabled,modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets)) {
                        Text(row.name);Text(row.description.orEmpty())
                        Text(stringResource(if(row.active) R.string.active else R.string.inactive))
                    }
                }
            }
            if(filtered.isEmpty() && !busy && s.error==null) item { com.mukhtabir.android.ui.components.EmptyState(stringResource(R.string.manage_empty)) }
            item { AppPagination(page, last, onPrevious = {vm.page(page-1)}, onNext = {vm.page(page+1)}, previousEnabled = page>1 && !busy, nextEnabled = page<last && !busy) }
        } else {
            item {
                OutlinedTextField(s.name,{vm.field("name",it)},label={Text(stringResource(R.string.manage_name))},enabled=!busy,modifier=Modifier.fillMaxWidth())
                OutlinedTextField(s.description,{vm.field("description",it)},label={Text(stringResource(R.string.description))},enabled=!busy,modifier=Modifier.fillMaxWidth())
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Text(stringResource(R.string.active),Modifier.weight(1f))
                    Switch(s.active,vm::active,modifier=Modifier.semantics {contentDescription=activeLabel},enabled=!busy && (kind==ManagedKind.BLUEPRINT || s.baseline!=null))
                }
            }
            if(kind==ManagedKind.BLUEPRINT) {
                item { Text(stringResource(R.string.manage_weights_hint)) }
                s.lookupError?.let { item { ErrorState(throwable=it,onRetry=vm::loadCategories) } }
                items((s.categories.map { it.id }+s.weights.keys).distinct(),key={"category-$it"}) { id ->
                    val categoryLabel=s.categories.firstOrNull {it.id==id}?.name ?: "#$id"
                    Row(verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(id in s.weights,{checked -> vm.weight(id,if(checked) "1" else null)},enabled=!busy,modifier=Modifier.semantics {contentDescription=categoryLabel})
                        Column(Modifier.weight(1f)) {
                            Text(categoryLabel)
                            if(id in s.weights) OutlinedTextField(s.weights.getValue(id),{vm.weight(id,it)},enabled=!busy,
                                keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal),
                                label={Text(stringResource(R.string.manage_weight))},modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
                        }
                    }
                }
                if(!s.categoriesLoaded) item { OutlinedButton(vm::loadCategories,enabled=!busy) { Text(stringResource(R.string.retry)) } }
            }
            item { AppActionRow() {
                Button(vm::save,enabled=enabled) { Text(stringResource(R.string.save)) }
                if(s.baseline!=null) AppDangerOutlinedButton({confirmation="delete"},enabled=enabled && !s.dirty) { Text(stringResource(if (kind == ManagedKind.GROUP) R.string.ux_delete_group else R.string.ux_delete_blueprint)) }
            } }
            if(kind==ManagedKind.GROUP && s.baseline!=null) {
                item {
                    Text(stringResource(R.string.manage_members))
                    OutlinedTextField(s.memberIds,{vm.field("members",it)},enabled=!busy,label={Text(stringResource(R.string.manage_member_ids))},modifier=Modifier.fillMaxWidth())
                    if(canLookupUsers) {
                        OutlinedTextField(s.userQuery,{vm.field("users",it)},enabled=!busy,label={Text(stringResource(R.string.search))},modifier=Modifier.fillMaxWidth())
                        OutlinedButton({vm.searchUsers()},enabled=!busy) { Text(stringResource(R.string.search)) }
                    }
                }
                s.usersError?.let { item { ErrorState(throwable=it,onRetry=if(!busy) ({vm.searchUsers(s.users?.page ?: 1)}) else null) } }
                s.users?.let { users ->
                    items(users.items,key={"lookup-${it.id}"}) { user ->
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Checkbox(user.id in parseMemberIds(s.memberIds).orEmpty(),{vm.selectUser(user.id)},enabled=!busy,modifier=Modifier.semantics {contentDescription=com.mukhtabir.android.util.accountIdentity(user.displayName, user.username, user.id)})
                            Text(com.mukhtabir.android.util.accountIdentity(user.displayName, user.username, user.id))
                        }
                    }
                    item { AppActionRow() {
                        OutlinedButton({vm.searchUsers(users.page-1)},enabled=!busy && users.page>1) { Text(stringResource(R.string.previous)) }
                        OutlinedButton({vm.searchUsers(users.page+1)},enabled=!busy && users.page<users.totalPages) { Text(stringResource(R.string.next)) }
                    } }
                }
                item { Button(vm::addMembers,enabled=enabled && s.memberIds.isNotBlank()) { Text(stringResource(R.string.manage_add_members)) } }
                items(s.baseline!!.members,key={"member-${it.userId}"}) { member ->
                    Column {
                        Text("${com.mukhtabir.android.util.accountIdentity(member.fullName ?: member.username, null, member.userId)} · ${com.mukhtabir.android.util.roleLabel(member.role)}")
                        Text(stringResource(if(member.visible) R.string.manage_member_visible else R.string.manage_member_hidden))
                        OutlinedButton({removing=member.userId},enabled=enabled && !s.dirty) { Text(stringResource(R.string.manage_remove)) }
                    }
                }
            }
        }
    }
}
