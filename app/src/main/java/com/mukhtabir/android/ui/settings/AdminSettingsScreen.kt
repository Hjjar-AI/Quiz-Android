@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.settings

import com.mukhtabir.android.ui.components.editorRecoveryBlocked
import com.mukhtabir.android.ui.components.EditorRecoveryControls
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.AppSuccessText
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.components.BlockedBackHandler
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.collectAsLifecycleState

@Composable
fun AdminSettingsRoute(adminService: AdminService, serverUrl: String, canManageSystem: Boolean,
    store: com.mukhtabir.android.core.SessionDraftStore, userId: Long, onBusyChanged: (Boolean) -> Unit) {
    val vm: AdminSettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key="settings-$userId-$canManageSystem",
        factory=AdminSettingsViewModel.Factory(adminService,canManageSystem,store,userId))
    val s by vm.state.collectAsLifecycleState()
    val blocked=editorRecoveryBlocked(vm.recovery)
    var fileBusy by remember {mutableStateOf(false)}
    val busy=s.busy || fileBusy
    BlockedBackHandler(enabled = busy)
    DisposableEffect(busy) {onBusyChanged(busy);onDispose {onBusyChanged(false)}}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),verticalArrangement=Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        AppPageHeading(stringResource(R.string.nav_admin_settings))
        if(canManageSystem) {
            EditorRecoveryControls(vm.recovery,busy,vm::restore)
            ServerConnectionExport(serverUrl,canManageSystem,busy) {fileBusy=it}
            if(busy) AppLoadingIndicator()
            s.draft?.let {d->
                val editable=!busy && !blocked && !s.conflict && !d.pending
                AppTextField(d.expiry,{vm.field(expiry=it)},textStyle=com.mukhtabir.android.util.ltrInputStyle(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number),enabled=editable,label={Text(stringResource(R.string.default_expiry_days))},modifier=Modifier.fillMaxWidth())
                AppTextField(d.renewal,{vm.field(renewal=it)},textStyle=com.mukhtabir.android.util.ltrInputStyle(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number),enabled=editable,label={Text(stringResource(R.string.default_renewal_days))},modifier=Modifier.fillMaxWidth())
                AppTextField(d.duration,{vm.field(duration=it)},textStyle=com.mukhtabir.android.util.ltrInputStyle(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number),enabled=editable,label={Text(stringResource(R.string.exam_duration_minutes))},modifier=Modifier.fillMaxWidth())
                AppButton(vm::save,enabled=editable) {Text(stringResource(R.string.save))}
                if(d.pending || s.conflict) {
                    Text(stringResource(R.string.author_write_unknown))
                    s.latest?.let { latest ->
                        val values = listOf(latest.defaultExpiryDays, latest.defaultRenewalDays, latest.examDurationMinutes)
                        Text(values.joinToString(" · ") { value ->
                            value.toLongOrNull()?.let { com.mukhtabir.android.util.formatNumber(it) } ?: value
                        })
                    }
                    if(s.reviewed && s.latest!=null) Row {
                        AppOutlinedButton({vm.resolve(true)},enabled=!busy && !blocked) {Text(stringResource(R.string.keep_question_draft))}
                        AppOutlinedButton({vm.resolve(false)},enabled=!busy && !blocked) {Text(stringResource(R.string.use_server_question))}
                    }
                }
            }
            AppOutlinedButton(vm::refresh,enabled=!busy) {Text(stringResource(R.string.refresh))}
            if(s.invalid) AppStatus(stringResource(R.string.settings_invalid_numbers))
            if(s.saved) AppSuccessText(stringResource(R.string.saved))
            s.error?.let {ErrorState(throwable=it,onRetry=if(busy) null else vm::refresh)}
        }
    }
}
