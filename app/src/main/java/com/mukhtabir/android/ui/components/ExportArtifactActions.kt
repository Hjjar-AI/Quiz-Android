@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.components

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R

/** Only the already-saved document URI is shared; artifact writes and picker launchers stay upstream. */
@Composable
internal fun ExportArtifactActions(filename: String, mimeType: String, savedUri: Uri?, enabled: Boolean,
    onSave: () -> Unit, onShareFailed: () -> Unit) {
    val context = LocalContext.current
    Text(filename)
    AppActionRow {
        AppOutlinedButton(onSave, enabled = enabled) { Text(stringResource(R.string.save)) }
        savedUri?.let { uri ->
            AppOutlinedButton({
                if (!shareExportDocument(context, mimeType, uri)) onShareFailed()
            }, enabled = enabled) { Text(stringResource(R.string.transfer_share)) }
        }
    }
}

private fun shareExportDocument(context: Context, mimeType: String, uri: Uri): Boolean = try {
    context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType(mimeType)
        .putExtra(Intent.EXTRA_STREAM, uri).apply { clipData = ClipData.newRawUri("export", uri) }
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION), null))
    true
} catch (_: ActivityNotFoundException) { false }
catch (_: SecurityException) { false }
