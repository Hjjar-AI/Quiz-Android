package com.mukhtabir.android.ui.admin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.ui.components.AppConfirmationDialog
import com.mukhtabir.android.ui.components.AppDangerOutlinedButton
import com.mukhtabir.android.ui.components.AppDangerTextButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppPasswordField
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppStatusKind
import com.mukhtabir.android.ui.components.AppTextButton
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme

@Composable
fun AdminAccountRoute(id: Long,actingUserId: Long,service: AdminService,canManage: Boolean,
    onBack: () -> Unit,onOwnPassword: () -> Unit,onBusyChanged: (Boolean) -> Unit,onPermissions: (() -> Unit)? = null) {
    val vm: AdminAccountViewModel=viewModel(key="admin-account-$actingUserId-$id-$canManage",
        factory=AdminAccountViewModel.Factory(id,actingUserId,service,canManage))
    val s by vm.state.collectAsLifecycleState()
    var action by rememberSaveable { mutableStateOf<String?>(null) }
    var leaving by rememberSaveable { mutableStateOf(false) }
    val back={ if(s.adminPassword.isNotEmpty() || s.newPassword.isNotEmpty() || s.repeatPassword.isNotEmpty() || s.generatedPassword!=null || s.generatedUncertain) leaving=true else onBack() }
    DisposableEffect(s.busy) { onBusyChanged(s.busy);onDispose { onBusyChanged(false) } }
    RefreshOnResume(vm::refresh)
    BackHandler { if(!s.busy) back() }
    if(!canManage) { Text(stringResource(R.string.error_forbidden));return }
    val enabled=!s.busy && !s.loading && !s.needsReview && s.user!=null && !s.deleted &&
        s.generatedPassword==null && !s.generatedUncertain
    if(leaving) AppConfirmationDialog(onDismissRequest={leaving=false},title={Text(stringResource(R.string.account_leave_title))},
        text={Text(stringResource(if(s.generatedPassword!=null || s.generatedUncertain) R.string.account_generated_leave else R.string.account_leave_message))},
        confirmButton={AppDangerTextButton({vm.clearSecrets();leaving=false;onBack()},enabled=!s.busy){Text(stringResource(R.string.ux_discard_account_changes))}},
        dismissButton={AppTextButton({leaving=false}){Text(stringResource(R.string.keep_editing))}})
    if(action!=null) AppConfirmationDialog(onDismissRequest={action=null},
        title={Text(stringResource(when(action) {
            "reset" -> R.string.account_reset_password
            "generate" -> R.string.account_generate_password
            "delete" -> R.string.account_delete
            else -> R.string.account_change_status
        }))},
        text={Text(stringResource(when(action) {
            "reset" -> R.string.account_reset_confirm
            "generate" -> R.string.account_generate_confirm
            "delete" -> R.string.account_delete_confirm
            else -> R.string.account_toggle_confirm
        },s.user?.username.orEmpty()))},
        confirmButton={AppTextButton({when(action) {
            "reset" -> vm.resetPassword()
            "generate" -> vm.generatePassword()
            "delete" -> vm.deleteUser()
            else -> vm.toggle()
        };action=null},enabled=enabled){Text(stringResource(if (action == "delete") R.string.account_delete else R.string.confirm))}},
        dismissButton={AppTextButton({action=null}){Text(stringResource(R.string.cancel))}})
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppPageHeading(stringResource(R.string.account_tools_title))
        AppTextButton(back,enabled=!s.busy){Text(stringResource(R.string.cancel))}
        AppOutlinedButton(vm::refresh,enabled=!s.busy && !s.loading && !s.deleted){Text(stringResource(R.string.refresh))}
        if(s.busy || s.loading) AppLoadingIndicator()
        s.error?.let { ErrorState(throwable=it,onRetry=if(!s.busy && !s.loading) vm::refresh else null) }
        s.actionError?.let { ErrorState(throwable=it) }
        s.validation?.let { AppStatus(stringResource(it)) }
        s.success?.let { Text(stringResource(it),modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) }
        if(s.needsReview) AppStatus(stringResource(R.string.account_review_required), kind = AppStatusKind.WARNING)
        s.generatedPassword?.let { password ->
            Text(stringResource(R.string.account_generated_hint))
            SelectionContainer { Text(password,style=MaterialTheme.typography.bodyLarge.copy(textDirection=TextDirection.Ltr)) }
            AppOutlinedButton(vm::acknowledgeGeneratedOutcome,enabled=!s.busy && !s.loading) {
                Text(stringResource(R.string.account_generated_done))
            }
        }
        if(s.generatedUncertain) {
            AppStatus(stringResource(R.string.account_generated_uncertain), kind = AppStatusKind.WARNING)
            AppOutlinedButton(vm::acknowledgeGeneratedOutcome,enabled=!s.busy && !s.loading && !s.needsReview) {
                Text(stringResource(R.string.account_generated_reviewed))
            }
        }
        s.user?.let { user ->
            Text(user.displayName,style=MaterialTheme.typography.titleLarge)
            Text("@${user.username}")
            AdminUserOverview(user)
            onPermissions?.let {open->AppOutlinedButton(open,enabled=!s.busy && !s.loading && !s.needsReview){Text(stringResource(R.string.nav_admin_permissions))}}
            Text(stringResource(R.string.account_identity,user.id,com.mukhtabir.android.util.roleLabel(user.role)))
            Text(stringResource(if(user.isActive) R.string.active else R.string.inactive))
            Text(if(user.bypassesExpiry) stringResource(R.string.account_expiry_bypassed) else stringResource(R.string.account_expiry,user.expiresAt ?: stringResource(R.string.none)))
            Text(stringResource(if(user.mustChangePassword) R.string.account_change_required else R.string.account_change_not_required))
            if(user.isStub) Text(stringResource(R.string.account_stub_hint))
            if(id==actingUserId) {
                Text(stringResource(R.string.account_self_hint))
                AppOutlinedButton(onOwnPassword,enabled=!s.busy){Text(stringResource(R.string.nav_change_password))}
            } else {
                AppSection(stringResource(R.string.account_tools_title)) {
                    AppPasswordField(s.adminPassword,{vm.field("admin",it)},enabled=enabled,
                        label={Text(stringResource(R.string.current_admin_password))},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrect=false),
                        singleLine=true,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
                    AppOutlinedButton({action="toggle"},enabled=enabled && !user.isStub) {
                        Text(stringResource(if(user.isActive) R.string.account_deactivate else R.string.account_activate))
                    }
                    AppPasswordField(s.newPassword,{vm.field("new",it)},enabled=enabled,
                        label={Text(stringResource(R.string.new_password))},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrect=false),singleLine=true,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
                    AppPasswordField(s.repeatPassword,{vm.field("repeat",it)},enabled=enabled,
                        label={Text(stringResource(R.string.account_repeat_password))},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password,autoCorrect=false,imeAction=androidx.compose.ui.text.input.ImeAction.Done),singleLine=true,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
                    Text(stringResource(R.string.account_reset_hint))
                    AppOutlinedButton({action="reset"},enabled=enabled){Text(stringResource(R.string.account_reset_password))}
                    AppOutlinedButton({action="generate"},enabled=enabled){Text(stringResource(R.string.account_generate_password))}
                    Text(stringResource(R.string.account_delete_hint))
                    AppDangerOutlinedButton({action="delete"},enabled=enabled){Text(stringResource(R.string.account_delete))}
                    AppTextButton(vm::clearSecrets,enabled=!s.busy && s.generatedPassword==null){Text(stringResource(R.string.account_clear_passwords))}
                }
            }
        }
    }
}
