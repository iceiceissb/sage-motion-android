package cn.tsinghua.sagemotion.data.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.core.content.FileProvider
import cn.tsinghua.sagemotion.model.ImageRegion
import java.io.File
import kotlin.math.*

/** One bounded, upright image is used for screen display, labeling and region crops. */
class PhotoAssets(private val context: Context) {
    fun ensurePersistent(rawUri: String): String = runCatching {
        val uri = Uri.parse(rawUri)
        if (uri.authority != "${context.packageName}.files" || uri.pathSegments.firstOrNull() != "camera_captures") return@runCatching rawUri
        val name = File(uri.lastPathSegment ?: return@runCatching rawUri).name
        val directory = File(context.filesDir, "camera_captures").apply { mkdirs() }
        val target = File(directory, name)
        if (!target.isFile) {
            val temp = File.createTempFile("migration_", ".jpg", directory)
            try {
                context.contentResolver.openInputStream(uri)?.use { source -> temp.outputStream().use(source::copyTo) }
                    ?: error("Photo no longer available")
                check(temp.renameTo(target))
            } finally { temp.delete() }
        }
        FileProvider.getUriForFile(context, "${context.packageName}.files", target).toString()
    }.getOrDefault(rawUri)

    fun decode(uri: Uri, maxEdge: Int = 2560): Bitmap {
        require(maxEdge > 0)
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        require(bounds.outWidth > 0 && bounds.outHeight > 0)
        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / sample > maxEdge) sample *= 2
        val source = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("Photo is unreadable")
        val orientation = runCatching { resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        } }.getOrNull()
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(-90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(-90f)
            }
        }
        if (matrix.isIdentity) return source
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true).also { if (it !== source) source.recycle() }
    }

    fun crop(rawUri: String, region: ImageRegion): Uri {
        val source = decode(Uri.parse(rawUri))
        val left = floor(region.left * source.width).toInt().coerceIn(0, source.width - 1)
        val top = floor(region.top * source.height).toInt().coerceIn(0, source.height - 1)
        val width = (ceil(region.right * source.width).toInt() - left).coerceIn(1, source.width - left)
        val height = (ceil(region.bottom * source.height).toInt() - top).coerceIn(1, source.height - top)
        val cropped = Bitmap.createBitmap(source, left, top, width, height)
        try {
            val directory = File(context.cacheDir, "vision_crops").apply { mkdirs() }
            directory.listFiles()?.filter { it.lastModified() < System.currentTimeMillis() - 86_400_000L }?.forEach { it.delete() }
            val file = File.createTempFile("region_", ".jpg", directory)
            file.outputStream().use { check(cropped.compress(Bitmap.CompressFormat.JPEG, 90, it)) }
            return FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        } finally { if (cropped !== source) cropped.recycle(); source.recycle() }
    }
}
