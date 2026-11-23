package com.mukhtabir.android.util

/** Question-content defaults shared by editors and the public-config DTO. */
object QuestionContentLimits {
    const val QUESTION = 4000
    const val EXPLANATION = 4000
    const val CASE_STEM = 4000
    const val CHOICE = 500
    const val MAX_CHOICES = 15
}

/** Match the backend's Unicode-character count rather than UTF-16 code units. */
fun String.contentLength(): Int = codePointCount(0, length)
