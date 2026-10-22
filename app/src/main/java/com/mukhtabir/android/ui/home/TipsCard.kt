package com.mukhtabir.android.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.AppCard
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.theme.AppTheme

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

    AppCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppLayoutTokens.cardInsets),
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.section),
            verticalAlignment = Alignment.Top,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
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
