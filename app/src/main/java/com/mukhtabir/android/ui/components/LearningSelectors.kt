@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.mukhtabir.android.ui.components

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.mukhtabir.android.R
import com.mukhtabir.android.util.formatNumber

/** Selection timing, defaults and write locks remain with each learning flow. */
@Composable
internal fun ConfidenceSelector(value: Int?, onSelect: (Int) -> Unit, enabled: Boolean,
    required: Boolean = false, hint: String? = null) {
    AppSection(stringResource(R.string.confidence_prompt),
        initiallyExpanded = required && value == null, revealError = required && value == null,
        subtitle = value?.let { confidenceLabel(it) }
            ?: stringResource(R.string.confidence_required_navigation)) {
        hint?.let { Text(it) }
        AppActionRow(Modifier.selectableGroup()) {
            (1..3).forEach { level ->
                AppFilterChip(value == level, { onSelect(level) }, enabled = enabled,
                    modifier = Modifier.semantics { role = Role.RadioButton },
                    label = { Text(confidenceLabel(level)) })
            }
        }
    }
}

/** Shared rating presentation only; aggregate reads and unknown writes remain caller-owned. */
@Composable
internal fun QuestionRatingSelector(selected: Int, onSelect: (Int) -> Unit, enabled: Boolean) {
    Text(stringResource(R.string.your_rating))
    AppActionRow(Modifier.selectableGroup()) {
        (1..5).forEach { value ->
            AppFilterChip(selected == value, { onSelect(value) }, enabled = enabled,
                modifier = Modifier.semantics { role = Role.RadioButton },
                label = { Text(stringResource(R.string.library_rating_value, formatNumber(value))) })
        }
    }
}
