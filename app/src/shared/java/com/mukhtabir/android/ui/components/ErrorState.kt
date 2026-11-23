package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.core.userMessage
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.theme.AppTheme

/**
 * Error state with a message and an optional retry button.
 *
 * [throwable] is mapped through [userMessage] so the copy is consistent
 * across the app. Callers that already have a pre-translated message
 * (e.g. a ViewModel state field) should pass [message] directly.
 */
@Composable
fun ErrorState(
    modifier: Modifier = Modifier,
    throwable: Throwable? = null,
    message: String? = null,
    title: String? = null,
    icon: ImageVector = Icons.Outlined.ErrorOutline,
    onRetry: (() -> Unit)? = null,
) {
    val resolvedMessage: String = when {
        message != null -> message
        throwable != null -> throwable.userMessage()
        else -> stringResource(R.string.error_unknown)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
            .padding(vertical = AppLayoutTokens.feedbackVerticalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
        )
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )
        }
        Text(
            text = resolvedMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            AppOutlinedButton(onClick = onRetry) {
                Text(stringResource(R.string.retry))
            }
        }
    }
}
