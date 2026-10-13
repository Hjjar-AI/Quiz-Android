@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.more

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.mukhtabir.android.ui.components.appVerticalScrollIndicator
import com.mukhtabir.android.ui.theme.previewThemeColors
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.ui.theme.themeLabel
import com.mukhtabir.android.core.ThemePreference
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
import com.mukhtabir.android.ui.components.AppIconButton
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
                preferences?.let { DrawerThemeButton(it, enabled) }
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


@Composable
private fun DrawerThemeButton(preferences: AppPreferences, enabled: Boolean) {
    val theme by preferences.theme.collectAsState()
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val systemDark = isSystemInDarkTheme()
    val menuState = stringResource(if (menuOpen) R.string.ui_expanded else R.string.ui_collapsed)
    Box {
        AppIconButton({ menuOpen = true }, enabled = enabled, modifier = Modifier.semantics { stateDescription = menuState }) {
            Icon(Icons.Filled.Palette, stringResource(R.string.drawer_theme_value, themeLabel(theme)), Modifier.size(AppLayoutTokens.secondaryIconSize))
        }
        DropdownMenu(menuOpen, { menuOpen = false }, Modifier.heightIn(max = AppLayoutTokens.menuMaxHeight)) {
            Box(Modifier.widthIn(min=208.dp,max=288.dp).heightIn(max=AppLayoutTokens.menuMaxHeight)
                .appVerticalScrollIndicator(scroll)) {
                Column(Modifier.verticalScroll(scroll).padding(end=10.dp)) {
                    ThemePreference.values().forEach { option ->
                        val colors=previewThemeColors(option,systemDark)
                        DropdownMenuItem(text = { Text(themeLabel(option)) },
                            modifier = Modifier.semantics { selected = theme == option }, enabled = enabled,
                            leadingIcon={
                                Row(Modifier.clearAndSetSemantics { },horizontalArrangement=Arrangement.spacedBy(3.dp)) {
                                    listOf(colors.background,colors.surface,colors.primary).forEach { color ->
                                        Box(Modifier.size(12.dp).background(color,CircleShape).border(.5.dp,colors.outline,CircleShape))
                                    }
                                }
                            },
                            trailingIcon=if(theme==option) ({ Icon(Icons.Filled.Check,contentDescription=null,
                                modifier=Modifier.size(AppLayoutTokens.secondaryIconSize)) }) else null,
                            onClick = { if (enabled) { preferences.setTheme(option); menuOpen = false } })
                    }
                }
            }
        }
    }
}
