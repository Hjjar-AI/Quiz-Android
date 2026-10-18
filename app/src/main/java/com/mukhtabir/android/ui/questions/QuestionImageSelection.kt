package com.mukhtabir.android.ui.questions

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.mukhtabir.android.R
import com.mukhtabir.android.data.remote.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal data class SelectedQuestionImage(val bytes: ByteArray, val mime: String, val filename: String, val preview: Bitmap)

// Match Django's 5 MiB limit and accepted formats. Never trust a provider's name or MIME alone.
internal suspend fun readQuestionImage(context: Context, uri: Uri): SelectedQuestionImage = withContext(Dispatchers.IO) {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val owner = coroutineContext
        try { com.mukhtabir.android.data.remote.readBoundedQuestionImage(input, checkActive = { owner.ensureActive() }) }
        catch (_: IllegalArgumentException) { throw ApiException(null, context.getString(R.string.question_image_too_large)) }
    } ?: throw ApiException(null, context.getString(R.string.question_image_unreadable))
    decodeQuestionImage(context, bytes)
}

internal suspend fun decodeQuestionImage(context: Context, bytes: ByteArray): SelectedQuestionImage = withContext(Dispatchers.IO) {
    if (bytes.size > 5 * 1024 * 1024) throw ApiException(null, context.getString(R.string.question_image_too_large))
    fun starts(vararg values: Int) = bytes.size>=values.size && values.indices.all { (bytes[it].toInt() and 255)==values[it] }
    fun textAt(offset: Int, text: String) = bytes.size>=offset+text.length && text.indices.all { bytes[offset+it].toInt()==text[it].code }
    val (mime,extension)=when {
        starts(255,216,255) -> "image/jpeg" to "jpg"
        starts(137,80,78,71,13,10,26,10) -> "image/png" to "png"
        textAt(0,"GIF87a") || textAt(0,"GIF89a") -> "image/gif" to "gif"
        textAt(0,"RIFF") && textAt(8,"WEBP") -> "image/webp" to "webp"
        else -> throw ApiException(null,context.getString(R.string.question_image_type_invalid))
    }
    coroutineContext.ensureActive()
    val preview = try { com.mukhtabir.android.data.remote.decodeBoundedQuestionBitmap(bytes,
        com.mukhtabir.android.data.remote.QuestionImageLimits.previewEdge) }
    catch (_: IllegalArgumentException) { throw ApiException(null, context.getString(R.string.question_image_unreadable)) }
    SelectedQuestionImage(bytes,mime,"question.$extension",preview)
}
