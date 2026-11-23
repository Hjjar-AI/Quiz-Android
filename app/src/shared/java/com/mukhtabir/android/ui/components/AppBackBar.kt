package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.ui.theme.AppSurfaces

@Composable
fun AppBackBar(title: String, enabled: Boolean, onMenu: (() -> Unit)? = null, onBack: () -> Unit) {
    AppScreenBar(title, navigation = {
        onMenu?.let { action -> AppIconButton(action, enabled = enabled) { Icon(Icons.Filled.Menu, stringResource(R.string.drawer_open)) } }
        AppIconButton(onClick = onBack, enabled = enabled, modifier = Modifier) {
            Icon(Icons.Filled.ArrowBack, stringResource(R.string.back),
                Modifier.directionalIcon())
        }
    })
}

@Composable
fun AppTopBar(title: String, enabled: Boolean, onMenu: () -> Unit, onSettings: (() -> Unit)? = null) {
    AppScreenBar(title, navigation = {
        AppIconButton(onMenu, enabled = enabled, modifier = Modifier) {
            Icon(Icons.Filled.Menu, stringResource(R.string.drawer_open))
        }
    }, actions = {
        onSettings?.let { action ->
            AppIconButton(action, enabled = enabled) {
                Icon(Icons.Filled.Settings, stringResource(R.string.nav_settings))
            }
        }
    })
}

/** Shared shell geometry; wrappers retain navigation labels, enabled guards and callbacks. */
@Composable
fun AppScreenBar(title: String, modifier: Modifier = Modifier,
    navigation: @Composable RowScope.() -> Unit = {}, actions: @Composable RowScope.() -> Unit = {}) {
    Surface(modifier = modifier, color = AppSurfaces.container) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(
            horizontal = AppLayoutTokens.shellHorizontalPadding, vertical = AppLayoutTokens.shellVerticalPadding),
            verticalAlignment = Alignment.CenterVertically) {
            navigation()
            Text(title, style = MaterialTheme.typography.titleLarge, softWrap = true, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f)
                    .padding(horizontal = AppLayoutTokens.shellHorizontalPadding).semantics { heading() })
            actions()
        }
    }
}
