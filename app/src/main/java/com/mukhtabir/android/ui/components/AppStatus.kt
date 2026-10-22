package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.mukhtabir.android.ui.theme.AppTheme

/** Wording conveys the outcome; icon and color provide additional, consistent cues. */
enum class AppStatusKind { INFO, SUCCESS, WARNING, ERROR }

@Composable
fun AppStatus(text: String, modifier: Modifier = Modifier, kind: AppStatusKind = AppStatusKind.ERROR,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    action: (@Composable () -> Unit)? = null) {
    val color = when (kind) {
        AppStatusKind.INFO -> AppTheme.feedback.info
        AppStatusKind.SUCCESS -> AppTheme.feedback.success
        AppStatusKind.WARNING -> AppTheme.feedback.warning
        AppStatusKind.ERROR -> MaterialTheme.colorScheme.error
    }
    val icon = when (kind) {
        AppStatusKind.INFO -> Icons.Outlined.Info
        AppStatusKind.SUCCESS -> Icons.Outlined.CheckCircle
        AppStatusKind.WARNING -> Icons.Outlined.WarningAmber
        AppStatusKind.ERROR -> Icons.Outlined.ErrorOutline
    }
    Row(modifier.semantics(mergeDescendants = true) {
        liveRegion = LiveRegionMode.Polite
        if (kind == AppStatusKind.ERROR) error(text)
    },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
        Icon(icon, contentDescription = null, tint = color)
        Text(text, Modifier.weight(1f, fill = false), style = style, color = color)
        action?.invoke()
    }
}
