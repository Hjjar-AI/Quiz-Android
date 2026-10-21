package com.mukhtabir.android.ui.auth

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppLaunchLayout
import com.mukhtabir.android.ui.components.AppLaunchHeader
import com.mukhtabir.android.ui.components.DemoLaunchCard
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppCardBody
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppOutlinedButton

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.mukhtabir.android.core.ServerConnectionFile
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import com.mukhtabir.android.ui.components.AppConfirmationDialog as AlertDialog
import com.mukhtabir.android.ui.components.AppButton as Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Icon
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import com.mukhtabir.android.ui.components.AppTextButton as TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import com.mukhtabir.android.R
import com.mukhtabir.android.core.ServerSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)
@Composable
fun ServerSetupScreen(settings: ServerSettings, initialUrl: String, onSaved: (Boolean) -> Unit, onCancel: (() -> Unit)?, initialFileSlot: Int? = null, language: String = com.mukhtabir.android.core.AppPreferences.LANGUAGE_ARABIC, onLanguageChange: (String) -> Unit = {}, onDemo: (() -> Unit)? = null) {
    val firstLaunch = onDemo != null && settings.read() == null && initialFileSlot == null
    var showSetup by rememberSaveable { mutableStateOf(!firstLaunch) }
    if (!showSetup) {
        AppLaunchLayout {
            AppLaunchHeader()
            onDemo?.let { DemoLaunchCard(onOpen = it) }
            AppCard(Modifier.fillMaxWidth()) {
                AppCardBody(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.launch_online_title), style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth().semantics { heading() }, textAlign = TextAlign.Center)
                    Text(stringResource(R.string.launch_online_hint), Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                    Button({ showSetup = true }, Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.launch_connect_server))
                    }
                }
            }
            SetupLanguageButton(language, onLanguageChange)
        }
        return
    }
    // Invalid pasted URLs may contain credentials; keep the editable input memory-only.
    var savedFiles by remember { mutableStateOf(settings.importedFiles()) }
    val initialFile = savedFiles.firstOrNull { it.slot == initialFileSlot }
    var selectedSlot by rememberSaveable { mutableStateOf(initialFile?.slot ?: settings.activeImportedSlot()) }
    var uploadSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    var usingFile by rememberSaveable { mutableStateOf(initialFile != null || settings.isFileConfigured()) }
    var fileUrl by rememberSaveable { mutableStateOf(initialFile?.address ?: if (settings.isFileConfigured()) settings.read() else null) }
    // Switching to manual entry never copies an imported address into this field.
    var input by remember { mutableStateOf(if (initialFile != null || settings.isFileConfigured()) "" else initialUrl) }
    var pendingUrl by rememberSaveable { mutableStateOf<String?>(null) }
    var showFiles by rememberSaveable { mutableStateOf(initialFile != null || settings.isFileConfigured()) }
    var showHelp by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<Int?>(null) }
    var sameServer by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope=rememberCoroutineScope()
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val slot = uploadSlot
        uploadSlot = null
        if (uri != null && slot != null && !busy) {
            busy = true
            error = null
            scope.launch {
                try {
                    val address = withContext(Dispatchers.IO) {
                        requireNotNull(context.contentResolver.openInputStream(uri)).use(ServerConnectionFile::read)
                    }
                    val result = withContext(Dispatchers.IO) { settings.saveImportedFile(slot, address) }
                    if (result.isFailure) {
                        error = R.string.server_import_save_failed
                        return@launch
                    }
                    savedFiles = settings.importedFiles()
                    selectedSlot = slot
                    fileUrl = address
                    usingFile = true
                    input = ""
                    sameServer = false
                    pendingUrl = null
                } catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { error = R.string.server_file_invalid }
                finally { busy = false }
            }
        }
    }
    val cancelSetup: (() -> Unit)? = if (firstLaunch) ({ showSetup = false }) else onCancel
    BackHandler { if (!busy) cancelSetup?.invoke() }
    pendingUrl?.let { url ->
        AlertDialog(onDismissRequest={if(!busy) pendingUrl=null},
            title={Text(stringResource(R.string.server_confirm_title))},
            text={Column(verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                if (usingFile) {
                    Text(stringResource(R.string.server_file_selected))
                    selectedSlot?.let { slot -> Text(stringResource(R.string.server_saved_file_number, slot)) }
                } else Text(url)
                if(url.startsWith("http://")) Text(stringResource(R.string.server_http_warning))
                if(settings.read()!=url) Text(stringResource(R.string.server_change_warning))
            }},
            confirmButton={TextButton({
                if(!busy) {
                    busy=true
                    error=null
                    val changed=settings.read()!=url
                    scope.launch {
                        try {
                            val result=withContext(Dispatchers.IO) { settings.save(url, sameServer, fromFile = usingFile, importedSlot = if (usingFile) selectedSlot else null) }
                            busy=false
                            pendingUrl=null
                            if(result.isSuccess) onSaved(changed) else error=R.string.server_save_failed
                        } catch(cancel: CancellationException) { throw cancel }
                    }
                }
            },enabled=!busy) { Text(stringResource(R.string.server_setup_title)) }},
            dismissButton={TextButton({pendingUrl=null},enabled=!busy) { Text(stringResource(R.string.cancel)) }})
    }
    AppLaunchLayout {
        cancelSetup?.let { cancel ->
            TextButton(cancel, enabled = !busy) { Text(stringResource(R.string.back)) }
        }
        SetupLanguageButton(language, onLanguageChange, enabled = !busy)
        Text(stringResource(R.string.server_setup_title),style=MaterialTheme.typography.headlineMedium,
            modifier=Modifier.semantics { heading() })
        Text(stringResource(R.string.server_setup_hint))
        AppSection(stringResource(R.string.server_show_files), enabled = !busy,
            expandedOverride = showFiles, onExpandedChange = { showFiles = it }) {
            Text(stringResource(R.string.server_saved_files_hint))
            (1..3).forEach { slot ->
                val saved = savedFiles.firstOrNull { it.slot == slot }
                Column(verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
                    Text(stringResource(R.string.server_saved_file_number, slot))
                    if (saved != null) {
                        if (settings.activeImportedSlot() == slot) {
                            Text(stringResource(R.string.server_saved_file_current))
                        }
                        if (usingFile && selectedSlot == slot) {
                            Text(stringResource(R.string.server_saved_file_selected))
                        }
                        AppOutlinedButton({
                            if (!busy) {
                                fileUrl = saved.address
                                selectedSlot = slot
                                usingFile = true
                                input = ""
                                sameServer = false
                                pendingUrl = null
                                error = null
                            }
                        }, enabled = !busy) { Text(stringResource(R.string.server_use_saved_file, slot)) }
                    }
                    AppOutlinedButton({
                        if (!busy) {
                            uploadSlot = slot
                            try { picker.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }
                            catch (_: Exception) { uploadSlot = null; error = R.string.server_file_invalid }
                        }
                    }, enabled = !busy) {
                        Text(stringResource(if (saved == null) R.string.server_add_file_slot else R.string.server_replace_file_slot, slot))
                    }
                }
            }
            Text(stringResource(R.string.server_file_hint))
        }
        if (usingFile) {
            Text(stringResource(R.string.server_file_selected))
            TextButton({
                if (!busy) {
                    usingFile = false
                    fileUrl = null
                    selectedSlot = null
                    input = ""
                    pendingUrl = null
                    sameServer = false
                    error = null
                }
            }, enabled = !busy) { Text(stringResource(R.string.server_enter_manually)) }
        } else {
            Text(stringResource(R.string.server_enter_manually),style=MaterialTheme.typography.titleMedium)
            OutlinedTextField(input,{input=it;error=null;sameServer=false},enabled=!busy,
                label={Text(stringResource(R.string.server_address))},singleLine=true,
                keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Uri,autoCorrect=false),modifier=Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
        }
        val selectedUrl = if (usingFile) fileUrl else ServerSettings.normalize(input)
        if (settings.read() != null && selectedUrl != settings.read()) {
            com.mukhtabir.android.ui.components.AppToggleRow(
                modifier = Modifier.fillMaxWidth().toggleable(
                    value = sameServer, enabled = !busy, role = androidx.compose.ui.semantics.Role.Checkbox,
                    onValueChange = { sameServer = it }),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                androidx.compose.material3.Checkbox(sameServer, onCheckedChange = null, enabled = !busy)
                Text(stringResource(R.string.server_same_server), modifier = Modifier.weight(1f))
            }
            Text(stringResource(R.string.server_same_server_hint))
        }
        AppSection(stringResource(R.string.server_show_help), enabled = !busy,
            expandedOverride = showHelp, onExpandedChange = { showHelp = it }) {
            Text(stringResource(R.string.server_phone_hint))
            Text(stringResource(R.string.server_certificate_hint))
        }
        error?.let { Text(stringResource(it),color=MaterialTheme.colorScheme.error,
            modifier=Modifier.semantics { liveRegion=LiveRegionMode.Polite }) }
        if(busy) AppLoadingIndicator()
        Button({
            val normalized = if (usingFile) fileUrl else ServerSettings.normalize(input)
            if(normalized==null) error=R.string.server_invalid_address else pendingUrl=normalized
        }, modifier = Modifier.fillMaxWidth(), enabled=!busy) { Text(stringResource(R.string.server_continue)) }
    }
}

@OptIn(
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class,
)
@Composable
private fun SetupLanguageButton(language: String, onLanguageChange: (String) -> Unit, enabled: Boolean = true) {
    var open by rememberSaveable { mutableStateOf(false) }
    val selectedLanguage = when (language) {
        "ar" -> "العربية"
        "en" -> "English"
        else -> stringResource(R.string.system_default)
    }
    AppOutlinedButton({ open = true }, Modifier.fillMaxWidth(), enabled = enabled) {
        Icon(Icons.Filled.Language, contentDescription = null)
        Text(stringResource(R.string.language_picker_selection,
            androidx.core.text.BidiFormatter.getInstance().unicodeWrap(selectedLanguage)),
            modifier = Modifier.padding(start = com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap),
            textAlign = TextAlign.Center)
    }
    if (open) AlertDialog(
        onDismissRequest = { open = false },
        title = { Text(stringResource(R.string.language)) },
        text = {
            com.mukhtabir.android.ui.components.AppActionRow {
                listOf("system", "en", "ar").forEach { code ->
                    com.mukhtabir.android.ui.components.AppFilterChip(
                        selected = language == code,
                        onClick = { open = false; onLanguageChange(code) },
                        enabled = enabled,
                        label = { Text(when (code) {
                            "ar" -> "العربية"
                            "en" -> "English"
                            else -> stringResource(R.string.system_default)
                        }) },
                    )
                }
            }
        },
        confirmButton = { TextButton({ open = false }) { Text(stringResource(R.string.close)) } },
    )
}
