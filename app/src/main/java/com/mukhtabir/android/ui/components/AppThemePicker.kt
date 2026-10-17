@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.AppIconButton
import com.mukhtabir.android.ui.components.AppLayoutTokens

@Composable
fun AppThemePicker(preferences: AppPreferences, enabled: Boolean = true, iconOnly: Boolean = false) {
    val theme by preferences.theme.collectAsState()
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val systemDark = isSystemInDarkTheme()
    val menuState = stringResource(if (menuOpen) R.string.ui_expanded else R.string.ui_collapsed)
    Box {
        if (iconOnly) AppIconButton({ menuOpen = true }, enabled = enabled, modifier = Modifier.semantics { stateDescription = menuState }) {
            Icon(Icons.Filled.Palette, stringResource(R.string.drawer_theme_value, themeLabel(theme)), Modifier.size(AppLayoutTokens.secondaryIconSize))
        } else AppOutlinedButton({ menuOpen = true }, enabled = enabled, modifier = Modifier.semantics { stateDescription = menuState }) {
            Icon(Icons.Filled.Palette, contentDescription = null)
            Text(themeLabel(theme))
        }
        DropdownMenu(menuOpen, { menuOpen = false }, Modifier.heightIn(max = AppLayoutTokens.menuMaxHeight)) {
            Box(Modifier.widthIn(min=208.dp,max=288.dp).heightIn(max=AppLayoutTokens.menuMaxHeight)
                .appVerticalScrollIndicator(scroll)) {
                Column(Modifier.verticalScroll(scroll).padding(end=10.dp)) {
                    val groups = listOf(
                        R.string.system_default to listOf(ThemePreference.SYSTEM),
                        R.string.ui_themes_light to ThemePreference.values().filter { it != ThemePreference.SYSTEM && it !in AppPreferences.DARK_THEMES },
                        R.string.ui_themes_dark to ThemePreference.values().filter { it in AppPreferences.DARK_THEMES })
                    groups.forEach { (title, options) ->
                        Text(stringResource(title), style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(AppLayoutTokens.cardInsets).semantics { heading() })
                        options.forEach { option ->
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
}
