package cn.tsinghua.sagemotion.data.agent

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/** Produces a bounded, metadata-free JPEG data URL for one explicitly approved upload. */
internal class VisionImagePayloadEncoder(context: Context) {
    private val photoAssets = cn.tsinghua.sagemotion.data.vision.PhotoAssets(context)

    fun encodeDataUrl(uriText: String): String? = runCatching {
        val uri = Uri.parse(uriText)
        val decoded = photoAssets.decode(uri)

        var bitmap = decoded
        try {
            bitmap = bitmap.fitWithin(MAX_UPLOAD_EDGE)
            for (quality in JPEG_QUALITIES) {
                val bytes = bitmap.toJpeg(quality)
                if (bytes.size <= MAX_UPLOAD_BYTES) {
                    val encoded = Base64.encodeToString(bytes, Base64.NO_WRAP)
                    return@runCatching "data:image/jpeg;base64,$encoded"
                }
            }

            bitmap = bitmap.fitWithin((max(bitmap.width, bitmap.height) * 0.78f).roundToInt())
            val bytes = bitmap.toJpeg(JPEG_QUALITIES.last())
            require(bytes.size <= MAX_UPLOAD_BYTES) { "Compressed image remains too large" }
            "data:image/jpeg;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        } finally {
            bitmap.recycle()
        }
    }.getOrNull()

    private fun Bitmap.fitWithin(maxEdge: Int): Bitmap {
        val edge = max(width, height)
        if (edge <= maxEdge) return this
        val scale = maxEdge.toFloat() / edge
        val resized = Bitmap.createScaledBitmap(
            this,
            (width * scale).roundToInt().coerceAtLeast(1),
            (height * scale).roundToInt().coerceAtLeast(1),
            true,
        )
        if (resized !== this) recycle()
        return resized
    }

    private fun Bitmap.toJpeg(quality: Int): ByteArray = ByteArrayOutputStream().use { output ->
        check(compress(Bitmap.CompressFormat.JPEG, quality, output)) { "JPEG compression failed" }
        output.toByteArray()
    }

    private companion object {
        const val MAX_UPLOAD_EDGE = 1_280
        const val MAX_UPLOAD_BYTES = 1_200_000
        val JPEG_QUALITIES = intArrayOf(82, 72, 62, 52)
    }
}
