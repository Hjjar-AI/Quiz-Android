package com.mukhtabir.android.data.remote

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = Application::class)
class QuestionImageDataTest {
    @Test fun `stream bytes are preserved for known and unknown lengths`() {
        val bytes = ByteArray(20000) { (it % 251).toByte() }
        assertArrayEquals(bytes, readBoundedQuestionImage(ByteArrayInputStream(bytes)))
        assertArrayEquals(bytes, readBoundedQuestionImage(ByteArrayInputStream(bytes), bytes.size.toLong()))
    }
    @Test fun `declared oversize is rejected before reading provider stream`() {
        val input = object : ByteArrayInputStream(byteArrayOf(1)) { override fun read(b: ByteArray, off: Int, len: Int): Int = error("Must not read") }
        assertThrows(IllegalArgumentException::class.java) { readBoundedQuestionImage(input, QuestionImageLimits.maxBytes.toLong() + 1) }
    }
    @Test fun `exact byte limit is accepted and unknown oversize rejected`() {
        assertEquals(QuestionImageLimits.maxBytes, readBoundedQuestionImage(ByteArrayInputStream(ByteArray(QuestionImageLimits.maxBytes))).size)
        assertThrows(IllegalArgumentException::class.java) { readBoundedQuestionImage(ByteArrayInputStream(ByteArray(QuestionImageLimits.maxBytes + 1))) }
    }
    @Test fun `declared short length cannot bypass actual byte limit`() {
        assertThrows(IllegalArgumentException::class.java) { readBoundedQuestionImage(ByteArrayInputStream(ByteArray(QuestionImageLimits.maxBytes + 1)), 1) }
    }
    @Test fun `cancellation interrupts stream copying`() {
        var checks = 0
        assertThrows(CancellationException::class.java) {
            readBoundedQuestionImage(ByteArrayInputStream(ByteArray(20000)), checkActive = { if (++checks == 2) throw CancellationException("cancelled") })
        }
        assertEquals(2, checks)
    }
    @Test fun `base64 supports legacy line breaks without changing bytes`() {
        val bytes = ByteArray(100) { it.toByte() }
        assertArrayEquals(bytes, decodeQuestionImageData(Base64.encodeToString(bytes, Base64.DEFAULT)))
    }
    @Test fun `encoded oversize is rejected before decoding`() {
        assertThrows(IllegalArgumentException::class.java) { decodeQuestionImageData("A".repeat(7200001)) }
    }
    @Test fun `decoded oversize is rejected even below encoded character limit`() {
        val encoded = Base64.encodeToString(ByteArray(QuestionImageLimits.maxBytes + 1), Base64.NO_WRAP)
        assertTrue(encoded.length < 7200000)
        assertThrows(IllegalArgumentException::class.java) { decodeQuestionImageData(encoded) }
    }
    @Test fun `empty and invalid bitmap data fail explicitly`() {
        assertThrows(IllegalArgumentException::class.java) { decodeBoundedQuestionBitmap(byteArrayOf(), 1280) }
        assertThrows(IllegalArgumentException::class.java) { decodeBoundedQuestionBitmap("not an image".toByteArray(), 1280) }
    }
    @Test fun `bitmap decode scales large input to requested bounds`() {
        val bitmap = Bitmap.createBitmap(2400, 1200, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().also { assertTrue(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) }.toByteArray()
        bitmap.recycle()
        val decoded = decodeBoundedQuestionBitmap(bytes, 600)
        try { assertTrue(decoded.width <= 600); assertTrue(decoded.height <= 600); assertTrue(decoded.width > 0) }
        finally { decoded.recycle() }
    }
}
