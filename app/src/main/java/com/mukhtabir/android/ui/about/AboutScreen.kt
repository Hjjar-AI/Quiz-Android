package com.mukhtabir.android.ui.about

import com.mukhtabir.android.ui.theme.AppTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import com.mukhtabir.android.ui.components.AppOutlinedButton as OutlinedButton
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mukhtabir.android.BuildConfig
import com.mukhtabir.android.R

@Composable
fun AboutScreen(onHelp: () -> Unit = {}, onPrivacy: () -> Unit = {}) {
    Column(Modifier.fillMaxSize().then(Modifier.verticalScroll(rememberScrollState())).padding(com.mukhtabir.android.ui.components.AppLayoutTokens.pageInsets), verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.section)) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME))
        Text(stringResource(R.string.about_build, BuildConfig.VERSION_CODE, BuildConfig.BUILD_TYPE))
        Text(stringResource(R.string.about_description))
        Text(stringResource(R.string.about_minimum_android))
        Text(stringResource(R.string.about_security))
        OutlinedButton(onHelp) { Text(stringResource(R.string.help_title)) }
        OutlinedButton(onPrivacy) { Text(stringResource(R.string.privacy_title)) }
    }
}
