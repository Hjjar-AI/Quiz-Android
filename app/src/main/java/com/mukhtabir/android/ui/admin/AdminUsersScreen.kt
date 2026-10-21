@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.admin

import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppSuccessText

import com.mukhtabir.android.ui.components.AppPagination

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppActionRow

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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.util.formatLocalDateTime
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.ErrorState

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
        item { com.mukhtabir.android.ui.components.AppSearchRow(
            field = { fieldModifier -> OutlinedTextField(s.search, { vm.field("search", it) },
                label = { Text(stringResource(R.string.search)) }, enabled = !s.loading, modifier = fieldModifier) },
            action = { Button({ vm.load(1) }, enabled = !s.loading) { Text(stringResource(R.string.search)) } },
        ) }
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
            com.mukhtabir.android.ui.components.AppSwitchRow(stringResource(R.string.active), s.active, vm::active, enabled = !s.loading)
            OutlinedTextField(s.adminPassword,{vm.field("admin",it)},label={Text(stringResource(R.string.current_admin_password))},visualTransformation=PasswordVisualTransformation(),enabled=!s.loading,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
            AppActionRow {Button(vm::save,enabled=!s.loading && !s.needsReview && !s.createUncertain){Text(stringResource(R.string.save))}; if(s.selected!=null) OutlinedButton({vm.select(null)},enabled=!s.loading){Text(stringResource(R.string.cancel))}}
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
