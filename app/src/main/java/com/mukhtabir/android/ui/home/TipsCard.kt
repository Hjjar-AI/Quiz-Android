package com.mukhtabir.android.ui.home

import com.mukhtabir.android.ui.theme.AppTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import com.mukhtabir.android.ui.components.AppCard as Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R

/**
 * Small rotating "Did you know?" card (Phase 1, item 26).
 *
 * Renders nothing when [tips] is empty. Rotation is owned by the
 * caller's ViewModel, not this composable — the list of tips is
 * fetched once and the index advances on a timer the ViewModel
 * controls. That keeps the composable stateless and makes the
 * rotation cadence testable without a Compose harness.
 *
 * The web's card advances every 10 minutes (`TIP_ROTATE_MS` in
 * `useDashboardController.js`). The Android home VM uses the same
 * cadence; the reasoning is that a tip is background reading, not a
 * notification, and a shorter interval would flicker under the
 * user's gaze.
 */
@Composable
fun TipsCard(
    tip: String,
    modifier: Modifier = Modifier,
) {
    if (tip.isBlank()) return

    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(com.mukhtabir.android.ui.components.AppLayoutTokens.cardInsets),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.tips_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = tip,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}
