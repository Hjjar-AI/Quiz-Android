package com.mukhtabir.android.data.remote

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.InputStream

/** Shared network, local-pack and upload-preview limits; no image disk cache. */
object QuestionImageLimits {
    const val maxBytes = 5 * 1024 * 1024
    const val maxPixels = 64_000_000L
    const val maxEdge = 16_384
    const val previewEdge = 1280
    const val detailEdge = 2048
}

fun readBoundedQuestionImage(input: InputStream, contentLength: Long = -1,
    checkActive: () -> Unit = {}): ByteArray {
    require(contentLength <= QuestionImageLimits.maxBytes) { "Image byte limit exceeded" }
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        checkActive()
        val count = input.read(buffer)
        if (count < 0) break
        require(output.size().toLong() + count <= QuestionImageLimits.maxBytes) { "Image byte limit exceeded" }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

fun decodeQuestionImageData(encoded: String): ByteArray {
    // Reject oversized encoded input before allocating its decoded counterpart (allow legacy line breaks).
    require(encoded.length <= 7_200_000) { "Image byte limit exceeded" }
    return Base64.decode(encoded, Base64.DEFAULT).also {
        require(it.size <= QuestionImageLimits.maxBytes) { "Image byte limit exceeded" }
    }
}

fun decodeBoundedQuestionBitmap(bytes: ByteArray, requestedEdge: Int): Bitmap {
    require(bytes.isNotEmpty() && bytes.size <= QuestionImageLimits.maxBytes) { "Invalid image bytes" }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth in 1..QuestionImageLimits.maxEdge && bounds.outHeight in 1..QuestionImageLimits.maxEdge &&
        bounds.outWidth.toLong() * bounds.outHeight <= QuestionImageLimits.maxPixels) { "Image pixel limit exceeded" }
    val target = requestedEdge.coerceIn(1, QuestionImageLimits.detailEdge)
    var sample = 1
    while ((bounds.outWidth.toLong() + sample - 1) / sample > target || (bounds.outHeight.toLong() + sample - 1) / sample > target) sample *= 2
    return requireNotNull(BitmapFactory.decodeByteArray(bytes, 0, bytes.size,
        BitmapFactory.Options().apply { inSampleSize = sample })) { "Unsupported question image" }
}
