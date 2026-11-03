@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.mukhtabir.android.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppPagination
import com.mukhtabir.android.ui.components.AppPasswordField
import com.mukhtabir.android.ui.components.AppSearchRow
import com.mukhtabir.android.ui.components.AppSectionHeading
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppSuccessText
import com.mukhtabir.android.ui.components.AppSwitchRow
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.BlockedBackHandler
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.ScreenBusyEffect
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatLocalDateTime

@Composable fun AdminUsersRoute(service: AdminService,pageSize:Int,roles:Map<String,String>,canManage:Boolean,actingUserId:Long,onOpenUser:(Long)->Unit,onPermissions:((Long)->Unit)? = null,onBusyChanged:(Boolean)->Unit = {}) {
    val vm:AdminUsersViewModel=viewModel(key="admin-users-$actingUserId-$pageSize-$canManage",factory=AdminUsersViewModel.Factory(service,pageSize,canManage,actingUserId)); val s by vm.state.collectAsLifecycleState()
    RefreshOnResume { vm.load(s.page) }
    BlockedBackHandler(enabled = s.loading)
    ScreenBusyEffect(s.loading, onBusyChanged)
    var batchAction by androidx.compose.runtime.remember { mutableStateOf<Boolean?>(null) }
    var batchPassword by androidx.compose.runtime.remember { mutableStateOf("") }
    var openTools by rememberSaveable { mutableStateOf<Long?>(null) }
    if(openTools!=null) AppConfirmationDialog(onDismissRequest={openTools=null},
        title={Text(stringResource(R.string.account_tools_title))},text={Text(stringResource(R.string.account_open_tools_confirm))},
        confirmButton={AppTextButton({val id=requireNotNull(openTools);vm.select(null);openTools=null;onOpenUser(id)},enabled=!s.loading){Text(stringResource(R.string.confirm))}},
        dismissButton={AppTextButton({openTools=null}){Text(stringResource(R.string.cancel))}})
    batchAction?.let { active -> AppConfirmationDialog(onDismissRequest={batchAction=null;batchPassword=""},
        title={Text(stringResource(if(active) R.string.admin_batch_activate else R.string.admin_batch_deactivate))},
        text={Column {Text(stringResource(R.string.moderation_confirm_count, com.mukhtabir.android.util.formatNumber(s.batchIds.size)))
            AppPasswordField(batchPassword,{batchPassword=it},label={Text(stringResource(R.string.current_admin_password))},enabled=!s.loading,
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )}},
        confirmButton={AppTextButton({val secret=batchPassword;batchPassword="";batchAction=null;vm.batch(active,secret)},
            enabled=!s.loading && batchPassword.isNotBlank()){Text(stringResource(R.string.confirm))}},
        dismissButton={AppTextButton({batchAction=null;batchPassword=""}){Text(stringResource(R.string.cancel))}}) }
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item { AppPageHeading(stringResource(R.string.nav_admin_users)) }
        item { AppSearchRow(
            field = { fieldModifier -> AppTextField(s.search, { vm.field("search", it) },
                label = { Text(stringResource(R.string.search)) }, enabled = !s.loading, modifier = fieldModifier) },
            action = { AppButton({ vm.load(1) }, enabled = !s.loading) { Text(stringResource(R.string.search)) } },
        ) }
        s.error?.let { item { ErrorState(throwable = it, onRetry = if(s.loading) null else { { vm.load(s.page) } }) } }
        s.validation?.let { item { AppStatus(stringResource(it)) } }
        if(s.saved) item { AppSuccessText(stringResource(R.string.saved)) }
        if(s.needsReview || s.createUncertain) item {
            Text(stringResource(R.string.admin_user_uncertain))
            if(s.createUncertain) AppOutlinedButton(vm::acknowledgeCreateReview,enabled=!s.loading && s.listFresh && !s.needsReview) {
                Text(stringResource(R.string.quiz_finish_review_done))
            }
        }
        item { AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            AppSectionHeading(stringResource(if(s.selected==null) R.string.create_user else R.string.edit_user))
            s.selected?.let { user -> AppOutlinedButton({openTools=user.id},enabled=!s.loading){Text(stringResource(R.string.account_tools_title))} }
            AppTextField(s.username,{vm.field("username",it)},label={Text(stringResource(R.string.username))},enabled=!s.loading && s.selected==null,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(), singleLine = true)
            AppTextField(s.fullName,{vm.field("name",it)},label={Text(stringResource(R.string.full_name))},enabled=!s.loading,modifier=Modifier.fillMaxWidth(), singleLine = true)
            AppPasswordField(s.password,{vm.field("password",it)},label={Text(stringResource(if(s.selected==null) R.string.password else R.string.new_password_optional))},enabled=!s.loading,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(), keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Next))
            s.selected?.let { selected -> Text(stringResource(R.string.admin_current_expiry,
                selected.expiresAt?.let(::formatLocalDateTime) ?: stringResource(R.string.none))) }
            Text(stringResource(R.string.admin_expiry_hint))
            AppActionRow() {
                if(s.selected!=null) AppFilterChip(s.expiryDays.isBlank(),{vm.field("expiry","")},{Text(stringResource(R.string.admin_keep_expiry))},enabled=!s.loading)
                listOf(0,30,60,90,365).forEach { days ->
                    AppFilterChip(s.expiryDays==days.toString(),{vm.field("expiry",days.toString())},
                        {Text(if(days==0) stringResource(R.string.admin_no_expiry) else stringResource(R.string.group_days,com.mukhtabir.android.util.formatNumber(days)))},enabled=!s.loading)
                }
            }
            AppTextField(s.expiryDays,{vm.field("expiry",it)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),label={Text(stringResource(R.string.expiry_days))},enabled=!s.loading,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(), singleLine = true)
            AppTextField(s.renewalDays,{vm.field("renewal",it)},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),label={Text(stringResource(R.string.renewal_days))},enabled=!s.loading,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(), singleLine = true)
            AppActionRow(){ (roles.keys.ifEmpty{setOf("member","moderator","admin")}+s.role).forEach { role -> AppFilterChip(s.role==role,{vm.field("role",role)},{Text(com.mukhtabir.android.util.roleLabel(role, roles[role]))},enabled=!s.loading) } }
            AppSwitchRow(stringResource(R.string.active), s.active, vm::active, enabled = !s.loading)
            AppPasswordField(s.adminPassword,{vm.field("admin",it)},label={Text(stringResource(R.string.current_admin_password))},enabled=!s.loading,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
            AppActionRow {AppButton(vm::save,enabled=!s.loading && !s.needsReview && !s.createUncertain){Text(stringResource(R.string.save))}; if(s.selected!=null) AppOutlinedButton({vm.select(null)},enabled=!s.loading){Text(stringResource(R.string.cancel))}}
        } } }
        item {
            if(s.batchIds.isNotEmpty()) {
                Text(stringResource(R.string.moderation_selected, com.mukhtabir.android.util.formatNumber(s.batchIds.size)))
                val ready=!s.loading && !s.needsReview && !s.createUncertain && s.batchUnknown.isEmpty() && s.selected==null &&
                    s.username.isBlank() && s.fullName.isBlank() && s.password.isEmpty()
                AppButton({batchAction=true},enabled=ready){Text(stringResource(R.string.admin_batch_activate))}
                AppOutlinedButton({batchAction=false},enabled=ready){Text(stringResource(R.string.admin_batch_deactivate))}
                AppTextButton(vm::clearBatch,enabled=!s.loading && s.batchUnknown.isEmpty()){Text(stringResource(R.string.clear_selection))}
            }
            if(s.batchSuccess+s.batchFailed+s.batchUnknown.size>0) Text(stringResource(R.string.admin_batch_result, com.mukhtabir.android.util.formatNumber(s.batchSuccess), com.mukhtabir.android.util.formatNumber(s.batchFailed), com.mukhtabir.android.util.formatNumber(s.batchUnknown.size)))
            if(s.batchUnknown.isNotEmpty()) {
                Text(stringResource(R.string.admin_batch_unknown))
                Text(stringResource(if(s.batchDesired==true) R.string.admin_batch_activate else R.string.admin_batch_deactivate))
                s.batchRows.forEach {row->Text(row.displayName+": "+stringResource(if(row.isActive) R.string.active else R.string.inactive))}
                s.batchMissing.forEach {id->Text(com.mukhtabir.android.util.bidi("#${com.mukhtabir.android.util.formatNumber(id)}")+" · "+stringResource(R.string.no_items))}
                AppOutlinedButton(vm::reviewBatch,enabled=!s.loading){Text(stringResource(R.string.refresh))}
                AppTextButton(vm::acknowledgeBatch,enabled=!s.loading && s.batchReviewed){Text(stringResource(R.string.content_reviewed))}
            }
        }
        if(s.loading) item { AppLoadingIndicator() }
        items(s.users,key={it.id}) { u -> AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Checkbox(u.id in s.batchIds,{vm.selectBatch(u.id)},enabled=!s.loading && !s.needsReview && !s.createUncertain && s.batchUnknown.isEmpty() && !u.isStub && u.id!=actingUserId,modifier=Modifier.semantics {contentDescription=u.displayName})
                AppSectionHeading(u.displayName)
            }
            Text(com.mukhtabir.android.util.accountIdentity("", u.username, u.id))
            Text("${com.mukhtabir.android.util.roleLabel(u.role)} · ${if(u.isActive) stringResource(R.string.active) else stringResource(R.string.inactive)}")
            AdminUserOverview(u)
            AppOutlinedButton({vm.select(u)},enabled=!s.loading && s.batchUnknown.isEmpty()){Text(stringResource(R.string.edit_user))}
            onPermissions?.let {open->AppOutlinedButton({open(u.id)},enabled=!s.loading && s.batchUnknown.isEmpty()){Text(stringResource(R.string.nav_admin_permissions))}}
        } } }
        if(s.totalPages>1) item { AppPagination(s.page, s.totalPages, onPrevious = {vm.load(s.page-1)}, onNext = {vm.load(s.page+1)}, previousEnabled = !s.loading && s.page>1, nextEnabled = !s.loading && s.page<s.totalPages, modifier = Modifier.fillMaxWidth()) }
    }
}
