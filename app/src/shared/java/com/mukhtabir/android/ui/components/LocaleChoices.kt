@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.components

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.core.AppPreferences

@Composable
fun appLanguageLabel(code: String): String = when (code) {
    "ar" -> "العربية"
    "en" -> "English"
    else -> stringResource(R.string.system_default)
}

/** Callers apply preferences/recreation; choosing a chip has no hidden lifecycle side effect. */
@Composable
fun AppLanguageChoices(selected: String, onSelect: (String) -> Unit, enabled: Boolean = true) {
    AppActionRow(Modifier.selectableGroup()) {
        listOf(AppPreferences.LANGUAGE_SYSTEM, "en", "ar").forEach { code ->
            AppFilterChip(selected == code, { onSelect(code) }, enabled = enabled,
                modifier = Modifier.semantics { role = Role.RadioButton },
                label = { Text(appLanguageLabel(code)) })
        }
    }
}

/** Authoring locale keys are retained exactly, including custom keys from the current document. */
@Composable
fun TranslationLocaleChoices(selected: String, locales: Collection<String>,
    onSelect: (String) -> Unit, enabled: Boolean) {
    AppActionRow(Modifier.selectableGroup()) {
        (listOf("ar", "en") + locales).distinct().forEach { code -> key(code) {
            AppFilterChip(selected == code, { onSelect(code) }, enabled = enabled,
                modifier = Modifier.semantics { role = Role.RadioButton },
                label = { Text(code) })
        } }
    }
}
