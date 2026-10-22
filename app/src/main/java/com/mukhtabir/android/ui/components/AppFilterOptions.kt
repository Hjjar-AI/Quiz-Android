package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.R

/** Local option search never changes the selected filter or dispatches a network read. */
@Composable
fun AppFilterOptions(label: String, options: List<Pair<String, String>>, selected: String,
    onSelect: (String) -> Unit, enabled: Boolean = true) {
    var query by remember(label) { mutableStateOf("") }
    Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.semantics { heading() })
    if (options.size > 8) AppTextField(query, { query = it }, enabled = enabled,
        modifier = Modifier.fillMaxWidth(), singleLine = true,
        label = { Text(stringResource(R.string.ui_search_options, label)) })
    Column(Modifier.fillMaxWidth().heightIn(max = AppLayoutTokens.filterMenuMaxHeight)
        .verticalScroll(rememberScrollState()).selectableGroup()) {
        val retained = if (selected.isNotEmpty() && options.none { it.first == selected })
            options + (selected to selected) else options
        retained.filter { (key, title) -> key == selected || key.isEmpty() || title.contains(query, ignoreCase = true) }
            .forEach { (key, title) ->
                Row(Modifier.fillMaxWidth().heightIn(min = AppLayoutTokens.touchTarget)
                    .selectable(key == selected, enabled = enabled, role = Role.RadioButton, onClick = { onSelect(key) }),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppLayoutTokens.controlGap)) {
                    RadioButton(key == selected, onClick = null, enabled = enabled)
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                }
            }
    }
}
