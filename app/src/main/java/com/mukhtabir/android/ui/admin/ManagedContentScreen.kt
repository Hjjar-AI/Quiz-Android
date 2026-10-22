@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.data.remote.CategoryService
import com.mukhtabir.android.data.remote.ManagedContentService
import com.mukhtabir.android.data.remote.ManagedKind
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppConfirmationButton
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerOutlinedButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppMetadataRow
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPagination
import com.mukhtabir.android.ui.components.AppSwitchRow
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme

@Composable
fun ManagedContentRoute(kind: ManagedKind,service: ManagedContentService,categories: CategoryService,
    admin: AdminService,userId: Long,permitted: Boolean,canLookupUsers: Boolean,pageSize: Int,
    onBack: () -> Unit,onBusy: (Boolean) -> Unit) {
    val vm: ManagedContentViewModel=viewModel(key="manage-$kind-$userId-$permitted-$canLookupUsers",
        factory=ManagedContentViewModel.Factory(kind,service,categories,admin,permitted,canLookupUsers,pageSize))
    val s by vm.state.collectAsLifecycleState()
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
    if(confirmation!=null || removing!=null) AppConfirmationDialog(onDismissRequest={if(!busy) {confirmation=null;removing=null}},
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
        confirmButton={AppConfirmationButton(destructive = removing != null || confirmation == "delete", onClick = {
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
        dismissButton={AppTextButton({confirmation=null;removing=null},enabled=!busy) { Text(stringResource(R.string.cancel)) }})
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item { Text(title) }
        item { AppActionRow() {
            AppOutlinedButton(back,enabled=!busy) { Text(stringResource(R.string.back)) }
            AppOutlinedButton(vm::refresh,enabled=!busy) { Text(stringResource(R.string.refresh)) }
        } }
        if(busy) item { AppLoadingIndicator() }
        s.error?.let { item { ErrorState(throwable=it,onRetry=if(!busy) vm::refresh else null) } }
        s.validation?.let { item { Text(stringResource(it),Modifier.semantics {liveRegion=LiveRegionMode.Polite}) } }
        s.message?.let { item { Text(stringResource(it)) } }
        s.memberResult?.let { (added,requested) -> item { Text(stringResource(R.string.manage_members_added, com.mukhtabir.android.util.formatNumber(added), com.mukhtabir.android.util.formatNumber(requested))) } }
        if(s.needsReview || s.uncertainCreate) item {
            Text(stringResource(R.string.manage_uncertain))
            if(s.uncertainCreate) AppOutlinedButton(vm::acknowledgeCreate,enabled=s.reviewed && !busy && !s.needsReview) {
                Text(stringResource(R.string.quiz_finish_review_done))
            }
        }
        s.conflict?.let { server -> item {
            Text(stringResource(R.string.account_changed_review))
            Text(server.name);Text(server.description.orEmpty());Text(server.weights.toString())
            Text(stringResource(if(server.active) R.string.active else R.string.inactive))
            AppMetadataRow(listOf(stringResource(R.string.member_count, com.mukhtabir.android.util.formatNumber(server.members.size))))
            AppActionRow() {
                AppOutlinedButton({vm.resolve(false)},enabled=!busy) { Text(stringResource(R.string.use_server_question)) }
                AppOutlinedButton({vm.resolve(true)},enabled=!busy) { Text(stringResource(R.string.keep_question_draft)) }
            }
        } }
        if(!s.editing) {
            item { AppTextField(s.query,{vm.field("search",it)},label={Text(stringResource(R.string.search))},modifier=Modifier.fillMaxWidth()) }
            item { AppButton(vm::create,enabled=enabled) { Text(stringResource(R.string.create)) } }
            val filtered=s.items.filter { s.query.isBlank() || it.name.contains(s.query,true) || it.id.toString()==s.query.trim() }
            val size=pageSize.coerceAtLeast(1)
            val last=((filtered.size+size-1)/size).coerceAtLeast(1)
            val page=s.page.coerceIn(1,last)
            items(filtered.drop((page-1)*size).take(size),key={"row-${it.id}"}) { row ->
                AppCard(onClick={vm.open(row.id)},enabled=enabled,modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(AppLayoutTokens.denseCardInsets)) {
                        Text(row.name);Text(row.description.orEmpty())
                        Text(stringResource(if(row.active) R.string.active else R.string.inactive))
                    }
                }
            }
            if(filtered.isEmpty() && !busy && s.error==null) item { EmptyState(stringResource(R.string.manage_empty)) }
            item { AppPagination(page, last, onPrevious = {vm.page(page-1)}, onNext = {vm.page(page+1)}, previousEnabled = page>1 && !busy, nextEnabled = page<last && !busy) }
        } else {
            item {
                AppTextField(s.name,{vm.field("name",it)},label={Text(stringResource(R.string.manage_name))},enabled=!busy,modifier=Modifier.fillMaxWidth())
                AppTextField(s.description,{vm.field("description",it)},label={Text(stringResource(R.string.description))},enabled=!busy,modifier=Modifier.fillMaxWidth())
                AppSwitchRow(activeLabel, s.active, vm::active,
                    enabled = !busy && (kind == ManagedKind.BLUEPRINT || s.baseline != null))
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
                            if(id in s.weights) AppTextField(s.weights.getValue(id),{vm.weight(id,it)},enabled=!busy,
                                keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Decimal),
                                label={Text(stringResource(R.string.manage_weight))},modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
                        }
                    }
                }
                if(!s.categoriesLoaded) item { AppOutlinedButton(vm::loadCategories,enabled=!busy) { Text(stringResource(R.string.retry)) } }
            }
            item { AppActionRow() {
                AppButton(vm::save,enabled=enabled) { Text(stringResource(R.string.save)) }
                if(s.baseline!=null) AppDangerOutlinedButton({confirmation="delete"},enabled=enabled && !s.dirty) { Text(stringResource(if (kind == ManagedKind.GROUP) R.string.ux_delete_group else R.string.ux_delete_blueprint)) }
            } }
            if(kind==ManagedKind.GROUP && s.baseline!=null) {
                item {
                    Text(stringResource(R.string.manage_members))
                    AppTextField(s.memberIds,{vm.field("members",it)},enabled=!busy,label={Text(stringResource(R.string.manage_member_ids))},modifier=Modifier.fillMaxWidth())
                    if(canLookupUsers) {
                        AppTextField(s.userQuery,{vm.field("users",it)},enabled=!busy,label={Text(stringResource(R.string.search))},modifier=Modifier.fillMaxWidth())
                        AppOutlinedButton({vm.searchUsers()},enabled=!busy) { Text(stringResource(R.string.search)) }
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
                        AppOutlinedButton({vm.searchUsers(users.page-1)},enabled=!busy && users.page>1) { Text(stringResource(R.string.previous)) }
                        AppOutlinedButton({vm.searchUsers(users.page+1)},enabled=!busy && users.page<users.totalPages) { Text(stringResource(R.string.next)) }
                    } }
                }
                item { AppButton(vm::addMembers,enabled=enabled && s.memberIds.isNotBlank()) { Text(stringResource(R.string.manage_add_members)) } }
                items(s.baseline!!.members,key={"member-${it.userId}"}) { member ->
                    Column {
                        Text("${com.mukhtabir.android.util.accountIdentity(member.fullName ?: member.username, null, member.userId)} · ${com.mukhtabir.android.util.roleLabel(member.role)}")
                        Text(stringResource(if(member.visible) R.string.manage_member_visible else R.string.manage_member_hidden))
                        AppOutlinedButton({removing=member.userId},enabled=enabled && !s.dirty) { Text(stringResource(R.string.manage_remove)) }
                    }
                }
            }
        }
    }
}
