package com.mukhtabir.android.data.remote

import kotlinx.serialization.json.*

/** Only bounded serializer fields and recognized auth reasons are retained for UI use. */
internal fun safeValidationDetails(value: JsonElement?): JsonObject? {
    val fields = setOf("username", "password", "current_password", "new_password", "admin_password", "full_name", "email",
        "name", "title", "description", "question", "explanation", "choices", "correct_answer", "translations",
        "category", "category_ids", "tags", "tags_filter", "difficulty", "color", "icon", "limit", "question_ids",
        "answers", "answer", "confidence", "pre_answer", "error_reason", "non_field_errors", "detail", "body", "text",
        "expires_at", "auto_renew_days", "default_expiry_days", "default_renewal_days", "exam_duration_minutes",
        "starts_at", "ends_at", "duration_minutes", "expected_version", "weights", "members", "role", "capabilities",
        "source", "source_url", "review_date", "knowledge_object", "case", "file", "format", "reason",
        "tag", "parent", "parent_id", "category_id", "verification_notes", "is_verified", "is_active", "is_draft",
        "case_id", "knowledge_object_id", "group_id", "co_authors", "audience", "weight", "question_weights",
        "category_mapping", "mapping", "start_at", "end_at", "full_bank", "occurred_at", "answered_at", "operation_id", "selection_strategy")
    fun clean(node: JsonElement, depth: Int): JsonElement? = when {
        depth > 3 -> null
        node is JsonPrimitive && node.isString -> node.content.takeIf {
            it.length in 1..2000 && it.none {c -> c.isISOControl() && c!='\n' && c!='\t'}
        }?.let(::JsonPrimitive)
        node is JsonArray -> JsonArray(node.take(20).mapNotNull {clean(it,depth+1)}).takeIf {it.isNotEmpty()}
        node is JsonObject -> JsonObject(node.entries.take(30).mapNotNull { (key, child) ->
            if(key !in fields && key.toIntOrNull()==null && !key.matches(Regex("[a-z]{2}(?:-[A-Z]{2})?"))) null
            else clean(child,depth+1)?.let {key to it}
        }.toMap()).takeIf {it.isNotEmpty()}
        else -> null
    }
    val root=value as? JsonObject ?: return null
    return JsonObject(root.entries.take(50).mapNotNull { (key,child) ->
        if(key !in fields) null
        else if(key=="reason") (child as? JsonPrimitive)?.contentOrNull?.takeIf {
            it in setOf("ACCOUNT_EXPIRED","MUST_CHANGE_PASSWORD")
        }?.let {key to JsonPrimitive(it)}
        else clean(child,0)?.let {key to it}
    }.toMap()).takeIf {it.isNotEmpty()}
}

internal fun validationMessages(details: JsonObject?): List<Pair<String,String>> {
    fun messages(value: JsonElement): List<String> = when(value) {
        is JsonPrimitive -> listOfNotNull(value.contentOrNull)
        is JsonArray -> value.flatMap {messages(it)}
        is JsonObject -> value.values.flatMap {messages(it)}
        else -> emptyList()
    }
    return details?.filterKeys {it!="reason"}?.flatMap { (field,value) -> messages(value).map {field to it} }?.distinct().orEmpty().take(30)
}
