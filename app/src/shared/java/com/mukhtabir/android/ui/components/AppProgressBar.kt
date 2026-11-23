package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Material supplies range semantics. Callers can associate a domain label with the range. */
@Composable
fun AppProgressBar(progress: Float, modifier: Modifier = Modifier, label: String? = null) {
    LinearProgressIndicator(progress = if (progress.isFinite()) progress.coerceIn(0f, 1f) else 0f,
        modifier = modifier.heightIn(min = 4.dp).then(
            if (label == null) Modifier else Modifier.semantics { contentDescription = label }))
}
