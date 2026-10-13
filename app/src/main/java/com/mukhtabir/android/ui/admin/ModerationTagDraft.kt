package com.mukhtabir.android.ui.admin

internal enum class ModerationTagError { EMPTY, TOO_LONG, OVERLAP }

// One tag per line permits commas within a tag, matching Django's list-of-strings contract.
internal fun moderationTagNames(text: String): List<String> =
    text.lineSequence().map(String::trim).filter(String::isNotEmpty).distinct().toList()

internal fun moderationTagError(add: List<String>, remove: List<String>): ModerationTagError? = when {
    add.isEmpty() && remove.isEmpty() -> ModerationTagError.EMPTY
    (add+remove).any { it.codePointCount(0,it.length)>50 } -> ModerationTagError.TOO_LONG
    add.toSet().intersect(remove.toSet()).isNotEmpty() -> ModerationTagError.OVERLAP
    else -> null
}
