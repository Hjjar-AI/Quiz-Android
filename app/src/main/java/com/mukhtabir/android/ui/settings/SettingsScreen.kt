@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.settings

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.core.AppPreferences
import com.mukhtabir.android.ui.components.AppActionRow
import com.mukhtabir.android.ui.components.AppFilterChip
import com.mukhtabir.android.ui.components.AppLanguageChoices
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.components.AppPageHeading
import com.mukhtabir.android.ui.components.AppSection
import com.mukhtabir.android.ui.components.AppSectionHeading
import com.mukhtabir.android.ui.components.AppSwitchRow
import com.mukhtabir.android.ui.components.AppThemePicker
import com.mukhtabir.android.ui.components.appLanguageLabel
import com.mukhtabir.android.ui.components.collectAsLifecycleState
import com.mukhtabir.android.ui.theme.AppTheme

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsRoute(preferences: AppPreferences, serverUrl: String, serverAddressHidden: Boolean, onChangeServer: (() -> Unit)?,
    savedServerFiles: List<com.mukhtabir.android.core.ImportedServerFile> = emptyList(), activeFileSlot: Int? = null, onSelectServerFile: ((Int) -> Unit)? = null) {
    val theme by preferences.theme.collectAsLifecycleState()
    val language by preferences.language.collectAsLifecycleState()
    val display by preferences.display.collectAsLifecycleState()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(AppLayoutTokens.pageInsets),
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppPageHeading(stringResource(R.string.nav_settings), stringResource(R.string.preferences_local))
        ReadingComfortSettings(preferences, display)
        AppSection(stringResource(R.string.settings_study), subtitle = stringResource(R.string.preferences_page) + ": " +
            (if (display.perPage == 0) stringResource(R.string.system_default) else com.mukhtabir.android.util.formatNumber(display.perPage))) {
            AppSectionHeading(stringResource(R.string.preferences_page))
            AppActionRow() {
                listOf(0, 10, 20, 50, 100).forEach { size ->
                    AppFilterChip(display.perPage == size, { preferences.setDisplay(display.copy(perPage = size)) },
                        label = { Text(if (size == 0) stringResource(R.string.system_default) else com.mukhtabir.android.util.formatNumber(size)) })
                }
            }
            AppSectionHeading(stringResource(R.string.difficulty))
            AppActionRow() {
                listOf("" to R.string.difficulty_any, "easy" to R.string.difficulty_easy, "medium" to R.string.difficulty_medium, "hard" to R.string.difficulty_hard).forEach { (value, label) ->
                    AppFilterChip(display.difficulty == value, { preferences.setDisplay(display.copy(difficulty = value)) }, label = { Text(stringResource(label)) })
                }
            }
            AppSwitchRow(stringResource(R.string.preferences_auto), display.autoAdvance, onCheckedChange = { preferences.setDisplay(display.copy(autoAdvance = it)) } )
            AppSwitchRow(stringResource(R.string.preferences_sound), display.sound, onCheckedChange = { preferences.setDisplay(display.copy(sound = it)) } )
            AppSwitchRow(stringResource(R.string.preferences_haptics), display.haptics, onCheckedChange = { preferences.setDisplay(display.copy(haptics = it)) } )
            Text(stringResource(R.string.preferences_haptics_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        AppSection(stringResource(R.string.settings_appearance), subtitle = listOf(
            com.mukhtabir.android.ui.theme.themeLabel(theme),
            stringResource(if (display.compact) R.string.preferences_compact else R.string.ui_spacing_standard)).joinToString(" · ")) {
            AppSwitchRow(stringResource(R.string.preferences_compact), display.compact, onCheckedChange = { preferences.setDisplay(display.copy(compact = it)) } )
            AppSectionHeading(stringResource(R.string.theme))
            AppThemePicker(preferences)

        }
        AppSection(stringResource(R.string.language), subtitle = appLanguageLabel(language)) {
            AppLanguageChoices(language, onSelect = { code ->
                preferences.setLanguage(code)
                (context as? Activity)?.recreate()
            })
        }
        AppSection(stringResource(R.string.server_address), subtitle = stringResource(if (serverAddressHidden) R.string.server_file_selected else R.string.ui_manual_connection)) {
            if (serverAddressHidden) Text(stringResource(R.string.server_file_selected), style = MaterialTheme.typography.bodyMedium)
            else androidx.compose.foundation.text.selection.SelectionContainer { Text(serverUrl, style = MaterialTheme.typography.bodyMedium) }
            Text(stringResource(R.string.server_saved_files_hint))
            savedServerFiles.forEach { file ->
                Text(stringResource(R.string.server_saved_file_number, com.mukhtabir.android.util.formatNumber(file.slot)), style = MaterialTheme.typography.titleSmall)
                if (serverAddressHidden && activeFileSlot == file.slot) Text(stringResource(R.string.server_saved_file_current))
                onSelectServerFile?.let { select ->
                    AppOutlinedButton({ select(file.slot) }) {
                        Text(stringResource(R.string.server_use_saved_file, com.mukhtabir.android.util.formatNumber(file.slot)))
                    }
                }
            }
            onChangeServer?.let { change ->
                AppOutlinedButton(change) { Text(stringResource(R.string.server_change)) }
            }
        }
        AppOutlinedButton(preferences::resetDisplay) { Text(stringResource(R.string.preferences_reset)) }
    }
}
