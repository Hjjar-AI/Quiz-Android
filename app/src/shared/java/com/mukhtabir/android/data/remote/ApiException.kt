package com.mukhtabir.android.data.remote

import kotlinx.serialization.json.JsonElement

class ApiException(
    val statusCode: Int?,
    message: String,
    cause: Throwable? = null,
    details: JsonElement? = null,
    fieldErrorFallback: String? = null,
) : Exception(message, cause) {
    val summary: String = message
    val details = safeValidationDetails(details)
    val fieldErrors = validationMessages(this.details).map { (field, value) ->
        field to if (fieldErrorFallback != null && value.any { it in '\u0600'..'\u06ff' }) fieldErrorFallback else value
    }.distinct()
    val reason: String? get() = (this.details?.get("reason") as? kotlinx.serialization.json.JsonPrimitive)?.content
    val isCsrfFailure: Boolean get() = statusCode == 403 && reason == "CSRF_FAILED"
    override val message: String get() = (listOf(summary) + fieldErrors.map {it.second}).filter {it.isNotBlank()}.distinct().joinToString("\n")
}
