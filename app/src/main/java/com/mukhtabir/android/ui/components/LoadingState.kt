package com.mukhtabir.android.ui.components

import com.mukhtabir.android.ui.components.AppLoadingIndicator

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R

/**
 * Full-screen or inline loading indicator.
 *
 * [fullScreen] fills the available box and centers; otherwise the
 * indicator sits inline with a small vertical padding.
 */
@Composable
fun LoadingState(
    modifier: Modifier = Modifier,
    fullScreen: Boolean = true,
    label: String? = null,
) {
    val boxModifier = if (fullScreen) modifier.fillMaxSize() else modifier
    Column(
        modifier = boxModifier.padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppLoadingIndicator()
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = stringResource(R.string.loading),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
