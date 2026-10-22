package com.mukhtabir.android.ui.notices

import com.mukhtabir.android.ui.components.AppSectionHeading
import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mukhtabir.android.R
import com.mukhtabir.android.core.StudyReminders
import com.mukhtabir.android.data.remote.MasterExamService
import com.mukhtabir.android.ui.components.AppButton
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppLoadingIndicator
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppStatus
import com.mukhtabir.android.ui.components.EmptyState
import com.mukhtabir.android.ui.components.ErrorState
import com.mukhtabir.android.ui.components.PullToRefresh
import com.mukhtabir.android.ui.components.RefreshOnResume
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatLocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
fun NoticesRoute(service: MasterExamService, reminders: StudyReminders, userId: Long,
    onOpenExam: (Long) -> Unit, onBack: () -> Unit) {
    val vm: NoticesViewModel = viewModel(key = "notices-$userId", factory = NoticesViewModel.Factory(service))
    val state by vm.state.collectAsLifecycleState()
    val context = LocalContext.current
    var enabled by remember(userId) { mutableStateOf(reminders.enabled) }
    var hour by remember(userId) { mutableStateOf(reminders.hour) }
    var minute by remember(userId) { mutableStateOf(reminders.minute) }
    var permitted by remember { mutableStateOf(reminders.notificationsAllowed()) }
    var reminderError by remember { mutableStateOf(false) }
    fun saveReminder(activate: Boolean, h: Int = hour, m: Int = minute) {
        val saved = reminders.update(userId, activate, h, m)
        reminderError = saved.isFailure
        enabled = reminders.enabled; hour = reminders.hour; minute = reminders.minute
        permitted = reminders.notificationsAllowed()
    }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permitted = reminders.notificationsAllowed()
        if (granted && permitted) saveReminder(true)
    }
    RefreshOnResume {
        permitted = reminders.notificationsAllowed()
        enabled = reminders.enabled
        vm.refresh()
    }
    PullToRefresh(state.busy,vm::refresh) {
    LazyColumn(Modifier.fillMaxSize().padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        item { AppPageHeading(stringResource(R.string.notices_title)) }
        item { AppOutlinedButton(onBack) { Text(stringResource(R.string.back)) } }
        item { AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            AppSectionHeading(stringResource(R.string.reminder_title))
            Text(stringResource(R.string.reminder_hint))
            Text(stringResource(R.string.reminder_time, LocalTime.of(hour, minute).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))))
            AppOutlinedButton({ TimePickerDialog(context, { _, h, m -> saveReminder(enabled, h, m) },
                hour, minute, DateFormat.is24HourFormat(context)).show() }) { Text(stringResource(R.string.reminder_change_time)) }
            AppButton({
                if (enabled) saveReminder(false)
                else if (permitted) saveReminder(true)
                else if (Build.VERSION.SDK_INT >= 33) permission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }) { Text(stringResource(if (enabled) R.string.reminder_disable else R.string.reminder_enable)) }
            if (!permitted) {
                Text(stringResource(R.string.reminder_disabled_permission))
                AppOutlinedButton({
                    val settings = if (Build.VERSION.SDK_INT >= 26) Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                    context.startActivity(settings)
                }) { Text(stringResource(R.string.reminder_open_settings)) }
            }
            if (reminderError) AppStatus(stringResource(R.string.reminder_error))
        } } }
        item { Text(stringResource(R.string.notices_hint)) }
        item { AppOutlinedButton(vm::refresh, enabled = !state.busy) { Text(stringResource(R.string.refresh)) } }
        if (state.busy) item { AppLoadingIndicator() }
        state.error?.let { item { ErrorState(throwable=it, onRetry = if (state.busy) null else vm::refresh) } }
        if (state.loaded && state.items.isEmpty()) item { EmptyState(stringResource(R.string.notices_empty)) }
        items(state.items, key = { it.id }) { exam ->
            AppCard(Modifier.fillMaxWidth()) { Column(Modifier.padding(AppLayoutTokens.cardInsets), verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                Text(exam.name, style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.master_window, formatLocalDateTime(exam.opensAt), formatLocalDateTime(exam.closesAt)))
                AppOutlinedButton({ onOpenExam(exam.id) }, enabled = !state.busy) { Text(stringResource(R.string.notice_open_exam)) }
            } }
        }
    }
    }
}
