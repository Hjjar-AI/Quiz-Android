@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import com.mukhtabir.android.R
import com.mukhtabir.android.core.MoreSection
import com.mukhtabir.android.core.NavigationCatalog
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.iconFor

@Composable
fun AppDrawerContent(user: UserDto, currentRoute: String?, enabled: Boolean, preferences: AppPreferences? = null, onNavigate: (String) -> Unit) {
    val destinations = NavigationCatalog.visibleFor(user)
    val selectedRoute = com.mukhtabir.android.core.ShellNavigation.parent(currentRoute)?.route
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(Modifier.fillMaxHeight()) {
        val pinSettings = maxHeight >= AppLayoutTokens.drawerPinnedMinHeight * fontScale
        Column(Modifier.fillMaxHeight()
            .then(if (pinSettings) Modifier else Modifier.verticalScroll(rememberScrollState()))
            .padding(horizontal = AppLayoutTokens.libraryPadding),
            verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
            DrawerHeader(user, preferences, enabled)
            // On short/large-text screens the whole drawer scrolls so no links are clipped.
            destinations.firstOrNull { it.route == "more/settings" }?.let { destination ->
                DrawerDestination(destination, selectedRoute, enabled, onNavigate)
            }
            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            Column(if (pinSettings) Modifier.weight(1f).verticalScroll(rememberScrollState()) else Modifier,
                verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
                NavigationDrawerItem(label = { Text(stringResource(R.string.home)) }, selected = currentRoute == "home",
                    modifier = Modifier.semantics { if (!enabled) disabled() },
                    onClick = { if (enabled) onNavigate("home") }, icon = { Icon(Icons.Filled.Home, null) })
                MoreSection.values().forEach { section ->
                    val rows = destinations.filter { it.section == section && it.route != "more/settings" }
                    if (rows.isNotEmpty()) {
                        val containsSelection = rows.any { it.route == selectedRoute }
                        val everyday = section in setOf(MoreSection.LEARN, MoreSection.REVIEW, MoreSection.EXAMS,
                            MoreSection.ACCOUNT, MoreSection.SUPPORT)
                        var expanded by rememberSaveable(user.id, section.name) { mutableStateOf(everyday || containsSelection) }
                        LaunchedEffect(selectedRoute) { if (containsSelection) expanded = true }
                        val description = stringResource(if (expanded) R.string.ui_expanded else R.string.ui_collapsed)
                        com.mukhtabir.android.ui.components.AppListRow(Modifier
                            .clickable(enabled = enabled, role = Role.Button) { expanded = !expanded }
                            .semantics { stateDescription = description }) {
                            Text(stringResource(section.titleRes), style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f).semantics { heading() })
                            Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (expanded) rows.forEach { destination ->
                            DrawerDestination(destination, selectedRoute, enabled, onNavigate)
                        }
                        Divider(Modifier.padding(vertical = AppLayoutTokens.tightGap), color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerHeader(user: UserDto, preferences: AppPreferences?, enabled: Boolean) {
    Column(Modifier.fillMaxWidth().padding(horizontal = AppLayoutTokens.cardPadding, vertical = AppLayoutTokens.tightGap),
        verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.tightGap)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
            Text(stringResource(R.string.app_name), modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
            preferences?.let { com.mukhtabir.android.ui.components.AppThemePicker(it, enabled, iconOnly = true) }
        }
        Text(user.displayName, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun DrawerDestination(destination: com.mukhtabir.android.core.MoreDestination,
    selectedRoute: String?, enabled: Boolean, onNavigate: (String) -> Unit) {
    NavigationDrawerItem(
        label = { Text(stringResource(destination.labelRes), style = MaterialTheme.typography.bodyMedium) },
        icon = { Icon(iconFor(destination.iconName), null) },
        selected = selectedRoute == destination.route,
        modifier = Modifier.semantics { if (!enabled) disabled() },
        onClick = { if (enabled) onNavigate(destination.route) },
    )
}

/** Kept for restored navigation from older app tasks; new navigation uses the drawer. */
@Composable
fun MoreScreen(user: UserDto, onNavigate: (String) -> Unit) {
    AppDrawerContent(user, currentRoute = "more", enabled = true, onNavigate = onNavigate)
}
