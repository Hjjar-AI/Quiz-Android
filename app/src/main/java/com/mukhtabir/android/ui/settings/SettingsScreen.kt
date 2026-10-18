@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.settings

import com.mukhtabir.android.ui.theme.AppTheme

import com.mukhtabir.android.ui.components.AppSuccessText

import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPageHeading

import com.mukhtabir.android.ui.components.AppOutlinedButton

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import com.mukhtabir.android.ui.components.AppLayoutTokens

import com.mukhtabir.android.ui.components.AppActionRow

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mukhtabir.android.ui.components.AppButton as Button
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import com.mukhtabir.android.ui.components.AppFilterChip as FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.data.remote.AdminService
import com.mukhtabir.android.ui.components.ErrorState
import androidx.compose.runtime.DisposableEffect

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRoute(preferences: AppPreferences, serverUrl: String, serverAddressHidden: Boolean, onChangeServer: (() -> Unit)?,
    savedServerFiles: List<com.mukhtabir.android.core.ImportedServerFile> = emptyList(), activeFileSlot: Int? = null, onSelectServerFile: ((Int) -> Unit)? = null) {
    val theme by preferences.theme.collectAsState()
    val language by preferences.language.collectAsState()
    val display by preferences.display.collectAsState()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(com.mukhtabir.android.ui.components.AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppPageHeading(stringResource(R.string.nav_settings), stringResource(R.string.preferences_local))
        ReadingComfortSettings(preferences, display)
        AppSection(stringResource(R.string.settings_study), subtitle = stringResource(R.string.preferences_page) + ": " +
            (if (display.perPage == 0) stringResource(R.string.system_default) else com.mukhtabir.android.util.formatNumber(display.perPage))) {
            Text(stringResource(R.string.preferences_page), style = MaterialTheme.typography.titleSmall)
            AppActionRow() {
                listOf(0, 10, 20, 50, 100).forEach { size ->
                    FilterChip(display.perPage == size, { preferences.setDisplay(display.copy(perPage = size)) },
                        label = { Text(if (size == 0) stringResource(R.string.system_default) else com.mukhtabir.android.util.formatNumber(size)) })
                }
            }
            Text(stringResource(R.string.difficulty), style = MaterialTheme.typography.titleSmall)
            AppActionRow() {
                listOf("" to R.string.difficulty_any, "easy" to R.string.difficulty_easy, "medium" to R.string.difficulty_medium, "hard" to R.string.difficulty_hard).forEach { (value, label) ->
                    FilterChip(display.difficulty == value, { preferences.setDisplay(display.copy(difficulty = value)) }, label = { Text(stringResource(label)) })
                }
            }
            com.mukhtabir.android.ui.components.AppSwitchRow(stringResource(R.string.preferences_auto), display.autoAdvance, onCheckedChange = { preferences.setDisplay(display.copy(autoAdvance = it)) } )
            com.mukhtabir.android.ui.components.AppSwitchRow(stringResource(R.string.preferences_sound), display.sound, onCheckedChange = { preferences.setDisplay(display.copy(sound = it)) } )
            com.mukhtabir.android.ui.components.AppSwitchRow(stringResource(R.string.preferences_haptics), display.haptics, onCheckedChange = { preferences.setDisplay(display.copy(haptics = it)) } )
            Text(stringResource(R.string.preferences_haptics_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppSection(stringResource(R.string.settings_appearance), subtitle = listOf(
            com.mukhtabir.android.ui.theme.themeLabel(theme),
            stringResource(if (display.compact) R.string.preferences_compact else R.string.ui_spacing_standard)).joinToString(" · ")) {
            com.mukhtabir.android.ui.components.AppSwitchRow(stringResource(R.string.preferences_compact), display.compact, onCheckedChange = { preferences.setDisplay(display.copy(compact = it)) } )
            Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleSmall)
            com.mukhtabir.android.ui.components.AppThemePicker(preferences)

        }
        AppSection(stringResource(R.string.language), subtitle = when (language) { "ar" -> "العربية"; "en" -> "English"; else -> stringResource(R.string.system_default) }) {
            AppActionRow() {
                listOf(AppPreferences.LANGUAGE_SYSTEM, "en", "ar").forEach { code ->
                    FilterChip(language == code, {
                        preferences.setLanguage(code)
                        (context as? Activity)?.recreate()
                    }, label = { Text(when (code) { "ar" -> "العربية"; "en" -> "English"; else -> stringResource(R.string.system_default) }) })
                }
            }
        }
        AppSection(stringResource(R.string.server_address), subtitle = stringResource(if (serverAddressHidden) R.string.server_file_selected else R.string.ui_manual_connection)) {
            if (serverAddressHidden) Text(stringResource(R.string.server_file_selected), style = MaterialTheme.typography.bodyMedium)
            else androidx.compose.foundation.text.selection.SelectionContainer { Text(serverUrl, style = MaterialTheme.typography.bodyMedium) }
            Text(stringResource(R.string.server_saved_files_hint))
            savedServerFiles.forEach { file ->
                Text(stringResource(R.string.server_saved_file_number, file.slot), style = MaterialTheme.typography.titleSmall)
                if (serverAddressHidden && activeFileSlot == file.slot) Text(stringResource(R.string.server_saved_file_current))
                onSelectServerFile?.let { select ->
                    AppOutlinedButton({ select(file.slot) }) {
                        Text(stringResource(R.string.server_use_saved_file, file.slot))
                    }
                }
            }
            onChangeServer?.let { change ->
                AppOutlinedButton(change) { Text(stringResource(R.string.server_change)) }
            }
        }
        AppOutlinedButton(preferences::resetDisplay) { Text(stringResource(R.string.preferences_reset)) }
    }
}


@Composable
fun AdminSettingsRoute(adminService: AdminService, serverUrl: String, canManageSystem: Boolean,
    store: com.mukhtabir.android.core.SessionDraftStore, userId: Long, onBusyChanged: (Boolean) -> Unit) {
    val vm: AdminSettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(key="settings-$userId-$canManageSystem",
        factory=AdminSettingsViewModel.Factory(adminService,canManageSystem,store,userId))
    val s by vm.state.collectAsState()
    val blocked=com.mukhtabir.android.ui.components.editorRecoveryBlocked(vm.recovery)
    var fileBusy by remember {mutableStateOf(false)}
    val busy=s.busy || fileBusy
    com.mukhtabir.android.ui.components.BlockedBackHandler(enabled = busy)
    DisposableEffect(busy) {onBusyChanged(busy);onDispose {onBusyChanged(false)}}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),verticalArrangement=Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
        AppPageHeading(stringResource(R.string.nav_admin_settings))
        if(canManageSystem) {
            com.mukhtabir.android.ui.components.EditorRecoveryControls(vm.recovery,busy,vm::restore)
            ServerConnectionExport(serverUrl,canManageSystem,busy) {fileBusy=it}
            if(busy) AppLoadingIndicator()
            s.draft?.let {d->
                val editable=!busy && !blocked && !s.conflict && !d.pending
                OutlinedTextField(d.expiry,{vm.field(expiry=it)},textStyle=com.mukhtabir.android.util.ltrInputStyle(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number),enabled=editable,label={Text(stringResource(R.string.default_expiry_days))},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(d.renewal,{vm.field(renewal=it)},textStyle=com.mukhtabir.android.util.ltrInputStyle(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number),enabled=editable,label={Text(stringResource(R.string.default_renewal_days))},modifier=Modifier.fillMaxWidth())
                OutlinedTextField(d.duration,{vm.field(duration=it)},textStyle=com.mukhtabir.android.util.ltrInputStyle(),keyboardOptions=androidx.compose.foundation.text.KeyboardOptions(keyboardType=androidx.compose.ui.text.input.KeyboardType.Number),enabled=editable,label={Text(stringResource(R.string.exam_duration_minutes))},modifier=Modifier.fillMaxWidth())
                Button(vm::save,enabled=editable) {Text(stringResource(R.string.save))}
                if(d.pending || s.conflict) {
                    Text(stringResource(R.string.author_write_unknown))
                    s.latest?.let { latest ->
                        val values = listOf(latest.defaultExpiryDays, latest.defaultRenewalDays, latest.examDurationMinutes)
                        Text(values.joinToString(" · ") { value ->
                            value.toLongOrNull()?.let { com.mukhtabir.android.util.formatNumber(it) } ?: value
                        })
                    }
                    if(s.reviewed && s.latest!=null) Row {
                        OutlinedButton({vm.resolve(true)},enabled=!busy && !blocked) {Text(stringResource(R.string.keep_question_draft))}
                        OutlinedButton({vm.resolve(false)},enabled=!busy && !blocked) {Text(stringResource(R.string.use_server_question))}
                    }
                }
            }
            OutlinedButton(vm::refresh,enabled=!busy) {Text(stringResource(R.string.refresh))}
            if(s.invalid) Text(stringResource(R.string.settings_invalid_numbers),color=MaterialTheme.colorScheme.error)
            if(s.saved) AppSuccessText(stringResource(R.string.saved))
            s.error?.let {ErrorState(throwable=it,onRetry=if(busy) null else vm::refresh)}
        }
    }
}
