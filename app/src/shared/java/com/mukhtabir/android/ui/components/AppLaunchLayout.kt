package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.ui.theme.AppTheme

/** One scroll owner, keyboard clearance and a readable form width for public entry screens. */
@Composable
fun AppLaunchLayout(centerVertically: Boolean = true, content: @Composable ColumnScope.() -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize().imePadding()) {
        val viewportHeight = maxHeight
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                .heightIn(min = viewportHeight).padding(AppLayoutTokens.authInsets),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = if (centerVertically) Arrangement.Center else Arrangement.Top,
        ) {
            Column(Modifier.widthIn(max = AppLayoutTokens.authMaxWidth).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section), content = content)
        }
    }
}

@Composable
fun AppLaunchHeader() {
    Text(stringResource(R.string.app_name), Modifier.fillMaxWidth().semantics { heading() },
        style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
}

/** Eligibility stays with each caller; downloaded practice can remain available during recovery. */
@Composable
fun OfflineLaunchActions(onDownloaded: (() -> Unit)?) {
    onDownloaded?.let { open ->
        AppCard(Modifier.fillMaxWidth()) {
            AppCardBody(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.launch_offline_title), style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().semantics { heading() }, textAlign = TextAlign.Center)
                Text(stringResource(R.string.launch_downloaded_hint), Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
                AppSecondaryButton(open, Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.offline_open_cached), textAlign = TextAlign.Center)
                }
            }
        }
    }
}
