package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import com.mukhtabir.android.ui.components.AppIconButton as IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import com.mukhtabir.android.R

@Composable
fun AppBackBar(title: String, enabled: Boolean, onMenu: (() -> Unit)? = null, onBack: () -> Unit) {
    Surface {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = AppLayoutTokens.shellVerticalPadding), verticalAlignment = Alignment.CenterVertically) {
            onMenu?.let { action -> IconButton(action, enabled = enabled) { Icon(Icons.Filled.Menu, stringResource(R.string.drawer_open)) } }
            IconButton(onClick = onBack, enabled = enabled, modifier = Modifier) {
                Icon(Icons.Filled.ArrowBack, stringResource(R.string.back),
                    Modifier.directionalIcon())
            }
            Text(title, style = MaterialTheme.typography.titleLarge, softWrap = true, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() })
        }
    }
}

@Composable
fun AppTopBar(title: String, enabled: Boolean, onMenu: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = AppLayoutTokens.shellVerticalPadding),
            verticalAlignment = Alignment.CenterVertically) {
            IconButton(onMenu, enabled = enabled, modifier = Modifier) {
                Icon(Icons.Filled.Menu, stringResource(R.string.drawer_open))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, softWrap = true, maxLines = 2,
                overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() })
        }
    }
}
