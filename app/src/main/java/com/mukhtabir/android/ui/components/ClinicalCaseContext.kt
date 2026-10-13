package com.mukhtabir.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ClinicalCaseContextDto

/** Render the case carried by this payload, preserving frozen session context. */
@Composable
fun ClinicalCaseContext(case: ClinicalCaseContextDto?, modifier: Modifier = Modifier) {
    val title = case?.title?.trim()?.takeIf(String::isNotEmpty)
    val stem = case?.stem?.takeIf(String::isNotBlank)
    if (title == null && stem == null) return
    AppSection(if (title == null) stringResource(R.string.clinical_case_context)
        else stringResource(R.string.clinical_case_title, title), modifier = modifier, initiallyExpanded = true) {
        stem?.let { MarkdownText(it, Modifier.fillMaxWidth()) }
    }
}
