package com.mukhtabir.android.ui.masterexams

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.AppStatus

internal data class DraftFieldError(val key: String, @StringRes val message: Int, val limit: Int? = null)
internal data class DraftValidationUi(val errors: List<DraftFieldError> = emptyList(), val attempt: Int = 0) {
    fun hasAny(vararg keys: String): Boolean = errors.any { it.key in keys }
}

@Composable
internal fun DraftValidationSummary(validation: DraftValidationUi) {
    key(validation.attempt) {
        AppStatus(stringResource(R.string.draft_validation_summary, com.mukhtabir.android.util.formatNumber(validation.errors.map { it.key }.distinct().size)), modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
    }
}

/** Placement waits for expanded sections/lazy items; repeated invalid submits request focus again. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DraftValidatedField(key: String, validation: DraftValidationUi, showMessage: Boolean = true,
    content: @Composable (String?, FocusRequester) -> Unit) {
    val issue = validation.errors.firstOrNull { it.key == key }
    val message = issue?.let { if (it.limit == null) stringResource(it.message) else stringResource(it.message, com.mukhtabir.android.util.formatNumber(it.limit)) }
    val focus = remember(key) { FocusRequester() }
    val bring = remember(key) { BringIntoViewRequester() }
    var placed by remember(key, validation.attempt) { mutableStateOf(false) }
    val first = validation.errors.firstOrNull()?.key == key
    LaunchedEffect(first, placed, validation.attempt) {
        if (first && placed && validation.attempt > 0) {
            bring.bringIntoView()
            focus.requestFocus()
        }
    }
    Column(Modifier.fillMaxWidth().bringIntoViewRequester(bring).onGloballyPositioned { placed = it.isAttached }) {
        content(message, focus)
        if (message != null && showMessage) AppStatus(message, style = MaterialTheme.typography.bodySmall, modifier = Modifier.semantics { error(message) })
    }
}
