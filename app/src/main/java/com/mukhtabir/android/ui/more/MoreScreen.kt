@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import com.mukhtabir.android.core.AppPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.core.MoreSection
import com.mukhtabir.android.core.NavigationCatalog
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.iconFor

@Composable
fun AppDrawerContent(user: UserDto, currentRoute: String?, enabled: Boolean, preferences: AppPreferences? = null, onNavigate: (String) -> Unit) {
    val destinations = NavigationCatalog.visibleFor(user)
    val standalone = destinations.filter { it.route in setOf("more/about", "more/privacy") }
    Column(Modifier.fillMaxHeight().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = AppTheme.spacing.content),
        verticalArrangement = Arrangement.spacedBy(com.mukhtabir.android.ui.components.AppLayoutTokens.controlGap)) {
        Column(Modifier.padding(horizontal = AppLayoutTokens.cardPadding, vertical = AppLayoutTokens.tightGap),
            verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                Text(stringResource(R.string.app_name), modifier = Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall)
                preferences?.let { com.mukhtabir.android.ui.components.AppThemePicker(it, enabled, iconOnly = true) }
            }
            Text(user.displayName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        NavigationDrawerItem(label = { Text(stringResource(R.string.home)) }, selected = currentRoute == "home",
            onClick = { if (enabled) onNavigate("home") }, icon = { Icon(Icons.Filled.Home, null) })
        Divider(Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
        MoreSection.values().forEach { section ->
            val rows = destinations.filter { it.section == section && it !in standalone }
            if (rows.isNotEmpty()) {
                var expanded by rememberSaveable(user.id, section.name) { mutableStateOf(false) }
                val description = stringResource(if (expanded) R.string.ui_expanded else R.string.ui_collapsed)
                com.mukhtabir.android.ui.components.AppListRow(Modifier.clickable(enabled = enabled, role = Role.Button) { expanded = !expanded }
                    .semantics { stateDescription = description }) {
                    Text(stringResource(section.titleRes), style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f).semantics { heading() })
                    Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (expanded) rows.forEach { destination ->
                    NavigationDrawerItem(
                        label = { Text(stringResource(destination.labelRes), style = MaterialTheme.typography.bodyMedium) },
                        icon = { Icon(iconFor(destination.iconName), null) },
                        selected = com.mukhtabir.android.core.ShellNavigation.parent(currentRoute)?.route == destination.route,
                        onClick = { if (enabled) onNavigate(destination.route) },
                    )
                }
            }
        }
        if (standalone.isNotEmpty()) Divider(Modifier.padding(vertical = AppLayoutTokens.tightGap), color = MaterialTheme.colorScheme.outlineVariant)
        standalone.forEach { destination ->
            val selected = com.mukhtabir.android.core.ShellNavigation.parent(currentRoute)?.route == destination.route
            com.mukhtabir.android.ui.components.AppListRow(Modifier
                .clickable(enabled = enabled, role = Role.Button) { if (enabled) onNavigate(destination.route) }
                .semantics { this.selected = selected }) {
                Text(stringResource(destination.labelRes), style = MaterialTheme.typography.titleSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics { heading() })
            }
        }
    }
}

/** Kept for restored navigation from older app tasks; new navigation uses the drawer. */
@Composable
fun MoreScreen(user: UserDto, onNavigate: (String) -> Unit) {
    AppDrawerContent(user, currentRoute = "more", enabled = true, onNavigate = onNavigate)
}
