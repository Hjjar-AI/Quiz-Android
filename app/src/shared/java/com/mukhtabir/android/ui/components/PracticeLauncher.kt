@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.TestMode
import com.mukhtabir.android.ui.components.AppTextField
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.util.formatNumber

/** The caller freezes its selection on open and owns the guarded handoff to session setup. */
@Composable
fun PracticeLauncher(title: String, ids: List<Long>, maximum: Int, enabled: Boolean,
    onDismiss: () -> Unit, onStart: (TestMode, List<Long>, String) -> Unit,
    onDownload: ((List<Long>) -> Unit)? = null, initialMode: TestMode = TestMode.STUDY) {
    val selected = remember(ids) { ids.filter { it > 0 }.distinct() }
    var mode by remember { mutableStateOf(initialMode) }
    var count by remember { mutableStateOf(minOf(selected.size, maximum.coerceAtLeast(1)).toString()) }
    var dispatched by remember { mutableStateOf(false) }
    val max = minOf(selected.size, maximum.coerceAtLeast(0))
    val number = count.toIntOrNull()?.takeIf { it in 1..max }
    AppConfirmationDialog(onDismissRequest = { if (!dispatched) onDismiss() },
        title = { Text(stringResource(R.string.flow_practice)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
                Text(title)
                Text(stringResource(R.string.flow_material_count, formatNumber(selected.size)))
                AppActionRow {
                    TestMode.values().forEach { option -> AppFilterChip(mode == option, { mode = option }, enabled = enabled && !dispatched,
                        label = { Text(stringResource(when (option) { TestMode.STUDY -> R.string.study; TestMode.EXAM -> R.string.exam; TestMode.RECALL -> R.string.recall })) }) }
                }
                AppTextField(count, { count = it.map { c -> Character.digit(c, 10).takeIf { d -> d >= 0 }?.digitToChar() ?: c }.joinToString("") },
                    Modifier.fillMaxWidth(), enabled = enabled && !dispatched, label = { Text(stringResource(R.string.flow_count)) },
                    supportingText = { Text(stringResource(R.string.flow_count_hint, formatNumber(max))) }, isError = number == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                onDownload?.let { download -> AppOutlinedButton({
                    number?.let { n -> if (enabled && !dispatched) { dispatched = true; download(selected.take(n)) } }
                }, enabled = enabled && !dispatched && number != null) { Text(stringResource(R.string.offline_download)) } }
            }
        },
        confirmButton = { AppButton({ number?.let { n -> if (enabled && !dispatched) {
            dispatched = true; onStart(mode, selected.take(n), title)
        } } }, enabled = enabled && !dispatched && number != null) { Text(stringResource(R.string.flow_start)) } },
        dismissButton = { AppTextButton(onDismiss, enabled = !dispatched) { Text(stringResource(R.string.cancel)) } })
}
