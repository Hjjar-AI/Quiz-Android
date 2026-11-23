@file:OptIn(androidx.compose.material.ExperimentalMaterialApi::class)

package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.shared.R

/** Gesture shortcut for existing guarded reads; explicit accessible Refresh/Retry stays available. */
@Composable
fun PullToRefresh(refreshing: Boolean,onRefresh: () -> Unit,enabled: Boolean = true,content: @Composable () -> Unit) {
    val state=rememberPullRefreshState(refreshing,onRefresh)
    val label=stringResource(R.string.refresh)
    Box(Modifier.fillMaxSize().pullRefresh(state,enabled=enabled)) {
        content()
        PullRefreshIndicator(refreshing,state,Modifier.align(Alignment.TopCenter).semantics {contentDescription=label},
            backgroundColor=MaterialTheme.colorScheme.surface,contentColor=MaterialTheme.colorScheme.primary)
    }
}
