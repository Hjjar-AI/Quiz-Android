package com.mukhtabir.android.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.core.ThemePreference
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.previewThemeColors
import com.mukhtabir.android.ui.theme.themeLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppThemePicker(preferences: AppPreferences, enabled: Boolean = true, iconOnly: Boolean = false) {
    val theme by preferences.theme.collectAsLifecycleState()
    var open by rememberSaveable { mutableStateOf(false) }
    val systemDark = isSystemInDarkTheme()
    val menuState = stringResource(if (open) R.string.ui_expanded else R.string.ui_collapsed)
    if (iconOnly) AppIconButton({ open = true }, enabled = enabled,
        modifier = Modifier.semantics { stateDescription = menuState }) {
        Icon(Icons.Filled.Palette, stringResource(R.string.drawer_theme_value, themeLabel(theme)))
    } else AppOutlinedButton({ open = true }, enabled = enabled,
        modifier = Modifier.semantics { stateDescription = menuState }) {
        Icon(Icons.Filled.Palette, contentDescription = null)
        Text(themeLabel(theme))
    }
    if (open) AppBottomSheet(stringResource(R.string.theme), onDismiss = { open = false }, actions = {
        AppButton({ open = false }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.done)) }
    }) {
        Text(stringResource(R.string.ui_theme_live_preview), style = MaterialTheme.typography.bodySmall)
        val groups = listOf(
            R.string.system_default to listOf(ThemePreference.SYSTEM),
            R.string.ui_themes_light to ThemePreference.values().filter { it != ThemePreference.SYSTEM && it !in AppPreferences.DARK_THEMES },
            R.string.ui_themes_dark to ThemePreference.values().filter { it in AppPreferences.DARK_THEMES })
        groups.forEach { (title, options) ->
            Text(stringResource(title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            AppTileGrid(options.size) { index, tileModifier ->
                val option = options[index]
                val colors = previewThemeColors(option, systemDark)
                val label = themeLabel(option)
                Surface(onClick = { if (enabled) preferences.setTheme(option) }, enabled = enabled,
                    modifier = tileModifier.heightIn(min = AppLayoutTokens.touchTarget).semantics(mergeDescendants = true) {
                        selected = theme == option
                        contentDescription = label
                    }, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(if (theme == option) AppLayoutTokens.selectedBorderWidth else AppLayoutTokens.cardBorderWidth,
                        if (theme == option) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(AppLayoutTokens.denseCardInsets),
                        verticalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                        Surface(Modifier.fillMaxWidth().heightIn(min = AppThemePickerGeometry.themePreviewHeight).clearAndSetSemantics { },
                            color = colors.background, contentColor = colors.onBackground, shape = MaterialTheme.shapes.small) {
                            Column(Modifier.padding(AppLayoutTokens.denseCardInsets)) {
                                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.labelLarge)
                                Surface(color = colors.surface, contentColor = colors.onSurface, shape = MaterialTheme.shapes.small) {
                                    Text(stringResource(R.string.reading_preview), Modifier.padding(AppLayoutTokens.denseCardInsets),
                                        style = MaterialTheme.typography.bodySmall)
                                }
                                Surface(color = colors.primary, contentColor = colors.onPrimary, shape = MaterialTheme.shapes.small) {
                                    Text(stringResource(R.string.study), Modifier.padding(AppLayoutTokens.denseCardInsets),
                                        style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                            if (theme == option) Icon(Icons.Filled.Check, contentDescription = null)
                        }
                    }
                }
            }
        }
    }
}

private object AppThemePickerGeometry {
    val themePreviewHeight = 72.dp
}
