package com.mukhtabir.android.util

import androidx.compose.runtime.Composable
import androidx.compose.material3.LocalTextStyle
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextDirection
import androidx.core.text.BidiFormatter
import com.mukhtabir.android.R

/** Isolate user/server fragments before composing mixed-direction labels. */
fun bidi(value: String): String = BidiFormatter.getInstance().unicodeWrap(value)
fun contentIdentity(title: String, id: Long): String = "${bidi(title)} · ${bidi("#${formatNumber(id)}")}"

fun accountIdentity(name: String, username: String?, id: Long): String =
    listOfNotNull(name.takeIf(String::isNotBlank)?.let(::bidi), username?.let { bidi("@$it") },
        bidi("#${formatNumber(id)}")).joinToString(" · ")

@Composable
fun roleLabel(role: String, serverLabel: String? = null): String = when (role) {
    "admin" -> stringResource(R.string.role_admin)
    "moderator" -> stringResource(R.string.role_moderator)
    "member" -> stringResource(R.string.role_member)
    else -> bidi(serverLabel ?: role)
}

@Composable
fun ltrInputStyle(): TextStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
