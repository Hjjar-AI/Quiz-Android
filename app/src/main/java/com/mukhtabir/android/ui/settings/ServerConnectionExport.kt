package com.mukhtabir.android.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.R
import com.mukhtabir.android.core.ServerConnectionFile
import com.mukhtabir.android.core.ServerSettings
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppTextField
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal fun ServerConnectionExport(serverUrl: String, canExport: Boolean, busy: Boolean, onBusyChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permitted by rememberUpdatedState(canExport)
    var address by remember(serverUrl) { mutableStateOf(serverUrl) }
    var pending by rememberSaveable { mutableStateOf<String?>(null) }
    var message by rememberSaveable { mutableStateOf<Int?>(null) }
    DisposableEffect(Unit) {
        onBusyChanged(pending != null)
        onDispose { onBusyChanged(false) }
    }
    val saver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val bytes = pending
        if (uri == null || bytes == null || !permitted) {
            if (uri != null && bytes == null) message = R.string.server_file_export_failed
            pending = null
            onBusyChanged(false)
        } else scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(bytes.toByteArray(Charsets.UTF_8)) }
                }
                message = R.string.server_file_exported
            } catch (cancel: CancellationException) { throw cancel }
            catch (_: Exception) { message = R.string.server_file_export_failed }
            finally { pending = null; onBusyChanged(false) }
        }
    }
    Text(stringResource(R.string.server_export_hint))
    AppTextField(address, { address = it; message = null }, enabled = canExport && !busy && pending == null,
        label = { Text(stringResource(R.string.server_address)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                textStyle = com.mukhtabir.android.util.ltrInputStyle(),
            )
    AppOutlinedButton({
        if (permitted && !busy && pending == null) {
            val normalized = ServerSettings.normalize(address)
            if (normalized == null) message = R.string.server_invalid_address
            else {
                pending = ServerConnectionFile.export(normalized)
                message = null
                onBusyChanged(true)
                try { saver.launch("mukhtabir-connection.json") }
                catch (_: Exception) { pending = null; onBusyChanged(false); message = R.string.server_file_export_failed }
            }
        }
    }, enabled = canExport && !busy && pending == null) { Text(stringResource(R.string.server_export_file)) }
    message?.let { Text(stringResource(it), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
}
