package com.mukhtabir.android.ui.sessions

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppCard as Card
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.DeviceSessionsService
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.util.formatLocalDateTime

@Composable
fun DeviceSessionsRoute(service: DeviceSessionsService,userId: Long,onBack: () -> Unit,
    onBusy: (Boolean) -> Unit,onCurrentRevoked: () -> Unit) {
    val vm: DeviceSessionsViewModel=viewModel(key="device-sessions-$userId",factory=DeviceSessionsViewModel.Factory(service))
    val s by vm.state.collectAsState()
    var confirming by remember { mutableStateOf<String?>(null) }
    com.mukhtabir.android.ui.components.BlockedBackHandler(enabled = s.busy)
    DisposableEffect(s.busy) { onBusy(s.busy);onDispose {onBusy(false)} }
    LaunchedEffect(s.signedOut) { if(s.signedOut) onCurrentRevoked() }
    RefreshOnResume(vm::refresh)
    confirming?.let { id -> AlertDialog(onDismissRequest={if(!s.busy) {confirming=null;vm.clearPassword()}},
        title={Text(stringResource(R.string.device_session_revoke))},
        text={Column {
            Text(stringResource(if(s.items.any { it.id==id && it.current }) R.string.device_session_confirm_current else R.string.device_session_confirm))
            OutlinedTextField(s.password,vm::password,label={Text(stringResource(R.string.current_password))},
                visualTransformation=PasswordVisualTransformation(),enabled=!s.busy,modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
        }},
        confirmButton={TextButton({vm.revoke(id);confirming=null},enabled=!s.busy && s.password.isNotBlank() && s.password.codePointCount(0,s.password.length)<=128) {Text(stringResource(R.string.confirm))}},
        dismissButton={TextButton({confirming=null;vm.clearPassword()},enabled=!s.busy) {Text(stringResource(R.string.cancel))}})
    }
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets),verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item {AppPageHeading(stringResource(R.string.device_sessions_title))}
        item {Text(stringResource(R.string.device_sessions_hint))}
        item {OutlinedButton(onBack,enabled=!s.busy) {Text(stringResource(R.string.back))}}
        item {OutlinedButton(vm::refresh,enabled=!s.busy) {Text(stringResource(R.string.refresh))}}
        if(s.busy) item {AppLoadingIndicator()}
        s.error?.let {item {ErrorState(throwable=it,onRetry=if(s.busy) null else vm::refresh)}}
        s.message?.let {item {Text(stringResource(it))}}
        if(s.pending!=null) item {
            Text(stringResource(R.string.device_session_uncertain))
            if(s.reviewReady) {
                Text(stringResource(if(s.items.any {it.id==s.pending}) R.string.device_session_still_active else R.string.device_session_absent))
                OutlinedButton(vm::acknowledgeReview,enabled=!s.busy) {Text(stringResource(R.string.quiz_finish_review_done))}
            }
        }
        if(s.loaded && !s.busy && s.error==null && s.items.isEmpty()) item {com.mukhtabir.android.ui.components.EmptyState(stringResource(R.string.device_sessions_empty))}
        items(s.items,key={it.id}) {session -> Card(Modifier.fillMaxWidth()) {Column(Modifier.padding(com.mukhtabir.android.ui.components.AppLayoutTokens.denseCardInsets),verticalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
            if(session.current) Text(stringResource(R.string.device_session_current))
            Text(session.userAgent.ifBlank {stringResource(R.string.none)})
            com.mukhtabir.android.ui.components.AppMetadataRow(listOf(
                stringResource(R.string.device_session_ip,session.ip ?: stringResource(R.string.none)),
                stringResource(R.string.device_session_seen,formatLocalDateTime(session.lastSeen))))
            OutlinedButton({confirming=session.id;vm.clearPassword()},enabled=!s.busy && s.pending==null) {Text(stringResource(R.string.device_session_revoke))}
        } } }
    }
}
