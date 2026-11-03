package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.R

/** A bounded lookup surface. Search drafts, result freshness and selection authority stay upstream. */
@Composable
internal fun AppSearchPicker(title: String, onDismiss: () -> Unit, query: String,
    onQueryChange: (String) -> Unit, onSearch: () -> Unit,
    queryEnabled: Boolean, searchEnabled: Boolean, loading: Boolean,
    error: Throwable?, onRetry: (() -> Unit)?,
    confirmButton: @Composable () -> Unit, dismissButton: (@Composable () -> Unit)? = null,
    beforeSearch: @Composable () -> Unit = {}, status: @Composable () -> Unit = {},
    content: LazyListScope.() -> Unit) {
    AppConfirmationDialog(onDismissRequest = onDismiss, title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = AppLayoutTokens.pickerMaxHeight),
                verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                item {
                    beforeSearch()
                    AppSearchRow(field = { modifier ->
                        AppTextField(query, onQueryChange, enabled = queryEnabled, singleLine = true,
                            label = { Text(stringResource(R.string.search)) }, modifier = modifier)
                    }, action = {
                        AppOutlinedButton(onSearch, enabled = searchEnabled) { Text(stringResource(R.string.search)) }
                    })
                    if (loading) AppLoadingIndicator()
                    error?.let { ErrorState(throwable = it, onRetry = onRetry,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
                    status()
                }
                content()
            }
        }, confirmButton = confirmButton, dismissButton = dismissButton)
}
