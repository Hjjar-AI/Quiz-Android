package com.mukhtabir.android.ui.about

import com.mukhtabir.android.ui.components.AppSectionHeading
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.BuildConfig
import com.mukhtabir.android.R
import com.mukhtabir.android.ui.components.AppLayoutTokens
import com.mukhtabir.android.ui.components.AppOutlinedButton
import com.mukhtabir.android.ui.theme.AppTheme

@Composable
fun AboutScreen(onHelp: () -> Unit = {}, onPrivacy: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().then(Modifier.verticalScroll(rememberScrollState())).padding(AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        AppSectionHeading(stringResource(R.string.app_name))
        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
        Text(stringResource(R.string.about_build, BuildConfig.VERSION_CODE, BuildConfig.BUILD_TYPE))
        Text(stringResource(R.string.about_description))
        Text(stringResource(R.string.about_minimum_android))
        Text(stringResource(R.string.about_security))
        AppOutlinedButton(onHelp) { Text(stringResource(R.string.help_title)) }
        AppOutlinedButton(onPrivacy) { Text(stringResource(R.string.privacy_title)) }
    }
}
