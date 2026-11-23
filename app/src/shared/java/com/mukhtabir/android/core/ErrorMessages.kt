package com.mukhtabir.android.core

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.mukhtabir.android.shared.R
import com.mukhtabir.android.data.remote.ApiException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Translate a [Throwable] to a user-facing message.
 *
 * Stable backend reasons use native bilingual resources. The transport
 * localizes legacy summaries and field fallbacks for callers outside Compose.
 *
 * Call sites should not branch on `statusCode` themselves — they
 * should call [userMessage] and render the result.
 */
@Composable
fun Throwable.userMessage(): String {
    val fallback = stringResource(R.string.error_unknown)
    val api = this as? ApiException
    val apiMessage = api?.let { error ->
        val reason = apiReasonResource(error.reason)?.let { stringResource(it) } ?: error.summary
        val fields = error.fieldErrors.map { (field,value) ->
            val label = when(field) {
                "username" -> stringResource(R.string.username)
                "password" -> stringResource(R.string.password)
                "current_password" -> stringResource(R.string.current_password)
                "new_password" -> stringResource(R.string.new_password)
                "full_name" -> stringResource(R.string.full_name)
                "description" -> stringResource(R.string.description)
                "question" -> stringResource(R.string.question_text)
                else -> null
            }
            if(label==null) value else stringResource(R.string.api_field_error,label,value)
        }
        val heading=reason.takeUnless {error.reason==null && error.fieldErrors.any {it.second==reason}}
        (listOfNotNull(heading)+fields).filter {it.isNotBlank()}.distinct().joinToString("\n")
    }?.takeIf {it.isNotBlank()}

    if (apiMessage != null && this !is IOException) {
        return apiMessage
    }

    @StringRes val resId: Int = when (this) {
        is ApiException -> when (statusCode) {
            400 -> R.string.error_bad_request
            401 -> R.string.error_unauthorized
            403 -> R.string.error_forbidden
            404 -> R.string.error_not_found
            409 -> R.string.error_conflict
            in 500..599 -> R.string.error_server
            else -> R.string.error_unknown
        }
        is SocketTimeoutException -> R.string.error_timeout
        is UnknownHostException -> R.string.error_no_connection
        is IOException -> R.string.error_network
        else -> R.string.error_unknown
    }

    // If the API also carried a message, prefer it even for network
    // failures where the backend envelope exists — that message is
    // the more specific description.
    return apiMessage ?: runCatching { stringResource(resId) }.getOrDefault(fallback)
}

/**
 * Non-composable variant for use outside a composition scope.
 *
 * Resolves the same mapping but returns a resource ID rather than a
 * string. Callers inside composables should use [userMessage];
 * callers that only need the resource (e.g. to route through their
 * own string resource provider) can use this.
 */
@StringRes
fun Throwable.userMessageRes(): Int = when (this) {
    is ApiException -> apiReasonResource(reason) ?: when (statusCode) {
        400 -> R.string.error_bad_request
        401 -> R.string.error_unauthorized
        403 -> R.string.error_forbidden
        404 -> R.string.error_not_found
        409 -> R.string.error_conflict
        in 500..599 -> R.string.error_server
        else -> R.string.error_unknown
    }
    is SocketTimeoutException -> R.string.error_timeout
    is UnknownHostException -> R.string.error_no_connection
    is IOException -> R.string.error_network
    else -> R.string.error_unknown
}
