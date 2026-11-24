package com.mukhtabir.android.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.data.remote.CategoryDto
import com.mukhtabir.android.shared.R

/** Keep the editor's display fallback when a selected category is missing from its choices. */
@Composable
fun AppCategoryField(
    categories: List<CategoryDto>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isError: Boolean = false,
    missingSelectionLabel: String = "",
    emptySelectionLabel: String = "",
) {
    val value = if (selectedId == null) emptySelectionLabel
    else categories.firstOrNull { it.id == selectedId }?.name ?: missingSelectionLabel
    AppSelectField(stringResource(R.string.nav_categories), value, modifier, enabled, isError) { closeMenu ->
        AppMenuItem(stringResource(R.string.none), { onSelect(null); closeMenu() },
            isSelected = selectedId == null)
        categories.forEach { category ->
            AppMenuItem(category.name, { onSelect(category.id); closeMenu() },
                isSelected = selectedId == category.id)
        }
    }
}
