package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics

enum class AppMenuItemStyle { STANDARD, COMPACT }

@Composable
fun AppMenu(expanded: Boolean, onDismissRequest: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    DropdownMenu(expanded, onDismissRequest, modifier = Modifier.heightIn(max = AppLayoutTokens.menuMaxHeight),
        content = content)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppMenuItem(
    label: String,
    onClick: () -> Unit,
    style: AppMenuItemStyle = AppMenuItemStyle.STANDARD,
    enabled: Boolean = true,
    isSelected: Boolean? = null,
    showSelectionIndicator: Boolean = false,
) {
    val compact = style == AppMenuItemStyle.COMPACT
    DropdownMenuItem(
        text = {
            if (compact) Text(label, style = MaterialTheme.typography.labelLarge) else Text(label)
        },
        onClick = onClick,
        modifier = if (isSelected == null) Modifier else Modifier.semantics { selected = isSelected },
        trailingIcon = if (showSelectionIndicator) {
            {
                if (isSelected == true) Icon(Icons.Filled.Check, contentDescription = null,
                    modifier = Modifier.size(AppLayoutTokens.secondaryIconSize))
            }
        } else null,
        enabled = enabled,
        colors = MenuDefaults.itemColors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = AppControlTokens.disabledContentAlpha),
        ),
        contentPadding = if (compact) AppLayoutTokens.secondaryContentPadding else MenuDefaults.DropdownMenuItemContentPadding,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSelectField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    options: @Composable ColumnScope.(closeMenu: () -> Unit) -> Unit,
) {
    var menuOpen by remember(enabled) { mutableStateOf(false) }
    ExposedDropdownMenuBox(menuOpen, { if (enabled) menuOpen = it }) {
        AppTextField(
            value, {}, modifier = modifier.menuAnchor().fillMaxWidth(),
            enabled = enabled, readOnly = true, isError = isError,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(menuOpen) },
        )
        ExposedDropdownMenu(menuOpen, { menuOpen = false },
            modifier = Modifier.heightIn(max = AppLayoutTokens.menuMaxHeight)) {
            options(this) { menuOpen = false }
        }
    }
}
