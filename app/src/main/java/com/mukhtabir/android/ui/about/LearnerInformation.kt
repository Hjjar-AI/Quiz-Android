package com.mukhtabir.android.ui.about

import com.mukhtabir.android.ui.theme.AppTheme
import com.mukhtabir.android.ui.components.AppSection

import com.mukhtabir.android.ui.components.AppPageHeading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.R

/** Native app guidance; deployment-specific policy and account requests go to the administrator. */
@Composable
fun LearnerInformation(privacy: Boolean, onBack: () -> Unit) {
    val sections = if (privacy) listOf(
        R.string.privacy_account_title to R.string.privacy_account_body,
        R.string.privacy_device_title to R.string.privacy_device_body,
        R.string.privacy_control_title to R.string.privacy_control_body,
    ) else listOf(
        R.string.help_study_title to R.string.help_study_body,
        R.string.help_review_title to R.string.help_review_body,
        R.string.help_recovery_title to R.string.help_recovery_body,
        R.string.help_master_title to R.string.help_master_body,
        R.string.help_library_title to R.string.help_library_body,
        R.string.help_progress_title to R.string.help_progress_body,
        R.string.help_preferences_title to R.string.help_preferences_body,
        R.string.help_files_title to R.string.help_files_body,
    )
    LazyColumn(Modifier.fillMaxSize().padding(com.mukhtabir.android.ui.components.AppLayoutTokens.pageInsets), verticalArrangement=Arrangement.spacedBy(AppTheme.spacing.section)) {
        item { OutlinedButton(onBack) { Text(stringResource(R.string.back)) } }
        item { AppPageHeading(stringResource(if(privacy) R.string.privacy_title else R.string.help_title)) }
        sections.forEach { (title,body) ->
            item { AppSection(stringResource(title)) { Text(stringResource(body)) } }
        }
        item { FullContent(privacy) }
        item { AppSection(stringResource(R.string.help_support_title)) {
            androidx.compose.foundation.text.selection.SelectionContainer { Text(stringResource(R.string.help_support_body)) }
        } }
    }
}
