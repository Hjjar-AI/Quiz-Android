package com.mukhtabir.android.ui.admin

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.UserDto
import com.mukhtabir.android.util.formatLocalDateTime

@Composable
internal fun AdminUserOverview(user: UserDto) {
    Text(stringResource(R.string.admin_user_contributions, com.mukhtabir.android.util.formatNumber(user.questionsCount)))
    Text(stringResource(R.string.admin_user_latest_exam,user.latestExamAccuracy?.let {
        java.text.NumberFormat.getPercentInstance().apply {maximumFractionDigits=1}.format(it/100.0)
    } ?: stringResource(R.string.none)))
    user.latestExamTag?.takeIf(String::isNotBlank)?.let {Text(it)}
    Text(stringResource(R.string.admin_user_last_login,user.lastLogin?.let(::formatLocalDateTime) ?: stringResource(R.string.none)))
}
