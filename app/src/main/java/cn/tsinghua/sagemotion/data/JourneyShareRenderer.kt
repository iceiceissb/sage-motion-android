package cn.tsinghua.sagemotion.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import cn.tsinghua.sagemotion.R
import cn.tsinghua.sagemotion.model.ExperimentUiState
import cn.tsinghua.sagemotion.model.RouteChoice
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 根据本次会话素材生成可直接分享到微信/相册的手账式 PNG，而不是分享一段纯文字。 */
class JourneyShareRenderer(private val context: Context) {
    fun render(state: ExperimentUiState): File? = runCatching {
        val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawColor(PAPER)
        drawPaperTexture(canvas, paint)

        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.color = INK
        paint.textSize = 64f
        canvas.drawText("我的公园知识游记", 72f, 118f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.color = MUTED
        paint.textSize = 29f
        val date = SimpleDateFormat("yyyy.MM.dd", Locale.CHINA).format(Date())
        canvas.drawText("$date  ·  八家郊野公园南园", 74f, 168f, paint)
        drawTape(canvas, paint, 786f, 74f, 222f, 58f, GOLD, -4f)
        paint.color = Color.WHITE
        paint.textSize = 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SAGE MEMORY", 808f, 112f, paint)

        val mapRect = RectF(60f, 220f, 1020f, 810f)
        drawPaperCard(canvas, paint, mapRect, 38f, Color.WHITE)
        BitmapFactory.decodeResource(context.resources, R.drawable.park_map_background)?.let { map ->
            drawBitmapCrop(canvas, map, RectF(78f, 238f, 1002f, 792f), 30f, .64f)
            map.recycle()
        }
        drawRoute(canvas, paint, state.routeReplanned, state.replanCount, state.adoptedRoute)
        drawTag(canvas, paint, 96f, 260f, "${state.activeRouteName} · 约 850m", SAGE_DARK)
        drawTag(canvas, paint, 732f, 716f, "${state.journeyPhotoMoments.size} 照片", SAGE)

        paint.color = INK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 42f
        canvas.drawText("沿途发现", 68f, 882f, paint)
        paint.color = MUTED
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 26f
        canvas.drawText("照片、问题和当时的环境被留在同一条路线上", 68f, 926f, paint)

        val photoMoments = state.journeyPhotoMoments.takeLast(3)
        val renderedPhotos = if (photoMoments.isEmpty()) {
            listOfNotNull(BitmapFactory.decodeResource(context.resources, R.drawable.flower_stimulus)?.let { it to null })
        } else {
            photoMoments.mapNotNull { moment -> decodeJourneyUri(moment.photoUri)?.let { it to moment } }
        }
        val photoWidth = if (renderedPhotos.size <= 1) 420f else 286f
        renderedPhotos.take(3).forEachIndexed { index, rendered ->
            val (photo, moment) = rendered
            val left = 68f + index * (photoWidth + 28f)
            val rect = RectF(left, 968f + if (index % 2 == 1) 24f else 0f, left + photoWidth, 1268f + if (index % 2 == 1) 24f else 0f)
            drawPaperCard(canvas, paint, RectF(rect.left - 10f, rect.top - 10f, rect.right + 10f, rect.bottom + 54f), 18f, Color.WHITE)
            drawBitmapCrop(canvas, photo, rect, 15f, 1f)
            paint.color = INK
            paint.textSize = 24f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val label = moment?.label?.take(10) ?: if (index == 0) "沿途花境" else "旅程发现 ${index + 1}"
            canvas.drawText(label, rect.left + 8f, rect.bottom + 37f, paint)
            photo.recycle()
        }

        val voiceRect = RectF(68f, 1360f, 520f, 1532f)
        val replanRect = RectF(548f, 1360f, 1012f, 1532f)
        drawNote(canvas, paint, voiceRect, BLUE_PAPER, "语音发现 · ${state.voiceInteractionCount} 次", state.voiceTranscripts.lastOrNull().orEmpty().ifBlank { state.voiceTranscript.ifBlank { "边走边问，留下当时的想法" } }, "VOICE")
        drawNote(canvas, paint, replanRect, ORANGE_PAPER, "路线调整 · ${state.replanCount} 次", if (state.routeReplanned) "未走的旧路段变灰，新路线绕开变化点" else if (state.replanCount > 0) "比较过改道方案，但未采用新路线" else "本次没有触发路线调整", "REPLAN")

        val question = state.journeyPhotoMoments.flatMap { it.questions }.lastOrNull()
        val quoteRect = RectF(68f, 1594f, 1012f, 1816f)
        drawPaperCard(canvas, paint, quoteRect, 28f, Color.WHITE)
        paint.color = SAGE_DARK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 28f
        canvas.drawText(question?.question?.let { "Q  $it" } ?: "今天的一个发现", 96f, 1650f, paint)
        paint.color = INK
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 27f
        drawWrappedText(
            canvas,
            paint,
            question?.answer ?: "湖边路线、照片、语音和变化点共同构成了今天的知识路径。",
            96f,
            1702f,
            872f,
            40f,
            3,
        )
        paint.color = MUTED
        paint.textSize = 22f
        canvas.drawText("由 SAGE 根据本次旅程素材生成 · 分享前请核查内容", 70f, 1880f, paint)

        val directory = File(context.cacheDir, "journey_shares").apply { mkdirs() }
        directory.listFiles()?.filter { it.isFile && System.currentTimeMillis() - it.lastModified() > 86_400_000L }?.forEach { it.delete() }
        val file = File(directory, "sage_journey_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        file
    }.getOrNull()

    private fun decodeUri(raw: String): Bitmap? = runCatching {
        context.contentResolver.openInputStream(Uri.parse(raw))?.use(BitmapFactory::decodeStream)
    }.getOrNull()

    private fun decodeJourneyUri(raw: String): Bitmap? = if (raw.startsWith("fixed://flower/")) {
        BitmapFactory.decodeResource(context.resources, R.drawable.flower_stimulus)
    } else {
        decodeUri(raw)
    }

    private fun drawPaperTexture(canvas: Canvas, paint: Paint) {
        paint.strokeWidth = 1f
        for (y in 18 until HEIGHT step 24) {
            paint.color = if ((y / 24) % 2 == 0) Color.argb(18, 126, 103, 70) else Color.argb(11, 63, 111, 92)
            canvas.drawLine(0f, y.toFloat(), WIDTH.toFloat(), y.toFloat(), paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun drawPaperCard(canvas: Canvas, paint: Paint, rect: RectF, radius: Float, color: Int) {
        paint.color = Color.argb(28, 35, 45, 40)
        canvas.drawRoundRect(RectF(rect.left + 5f, rect.top + 9f, rect.right + 5f, rect.bottom + 9f), radius, radius, paint)
        paint.color = color
        canvas.drawRoundRect(rect, radius, radius, paint)
    }

    private fun drawBitmapCrop(canvas: Canvas, bitmap: Bitmap, dst: RectF, radius: Float, alpha: Float) {
        val srcRatio = bitmap.width.toFloat() / bitmap.height
        val dstRatio = dst.width() / dst.height()
        val src = if (srcRatio > dstRatio) {
            val target = (bitmap.height * dstRatio).toInt()
            Rect((bitmap.width - target) / 2, 0, (bitmap.width + target) / 2, bitmap.height)
        } else {
            val target = (bitmap.width / dstRatio).toInt()
            Rect(0, (bitmap.height - target) / 2, bitmap.width, (bitmap.height + target) / 2)
        }
        val save = canvas.save()
        val clip = Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) }
        canvas.clipPath(clip)
        val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply { this.alpha = (255 * alpha).toInt() }
        canvas.drawBitmap(bitmap, src, dst, bitmapPaint)
        canvas.restoreToCount(save)
    }

    private fun drawRoute(
        canvas: Canvas,
        paint: Paint,
        routeReplanned: Boolean,
        replanCount: Int,
        adoptedRoute: RouteChoice,
    ) {
        val route = Path().apply {
            moveTo(122f, 704f)
            if (adoptedRoute == RouteChoice.ALTERNATIVE && !routeReplanned) {
                cubicTo(300f, 756f, 642f, 716f, 944f, 362f)
            } else {
                cubicTo(266f, 462f, 438f, 640f, 570f, 494f)
                cubicTo(690f, 344f, 790f, 312f, 944f, 362f)
            }
        }
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeWidth = 18f
        paint.color = Color.WHITE
        canvas.drawPath(route, paint)
        paint.strokeWidth = 10f
        paint.color = if (routeReplanned) Color.rgb(158, 167, 162) else SAGE_DARK
        canvas.drawPath(route, paint)
        if (routeReplanned) {
            val walked = Path().apply {
                moveTo(122f, 704f)
                cubicTo(266f, 462f, 438f, 640f, 570f, 494f)
            }
            paint.strokeWidth = 10f
            paint.color = SAGE_DARK
            canvas.drawPath(walked, paint)
            val fork = Path().apply {
                moveTo(570f, 494f)
                cubicTo(682f, 668f, 850f, 584f, 944f, 362f)
            }
            paint.strokeWidth = 18f
            paint.color = Color.WHITE
            canvas.drawPath(fork, paint)
            paint.strokeWidth = 10f
            paint.color = SAGE_DARK
            canvas.drawPath(fork, paint)
            paint.style = Paint.Style.FILL
            paint.color = OCHRE
            canvas.drawCircle(708f, 608f, 23f, paint)
            paint.color = Color.WHITE
            paint.textSize = 24f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("↗", 696f, 617f, paint)
        } else if (replanCount > 0) {
            val proposedFork = Path().apply {
                moveTo(570f, 494f)
                cubicTo(682f, 668f, 850f, 584f, 944f, 362f)
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 16f
            paint.color = Color.WHITE
            canvas.drawPath(proposedFork, paint)
            paint.strokeWidth = 8f
            paint.color = OCHRE
            paint.pathEffect = DashPathEffect(floatArrayOf(18f, 12f), 0f)
            canvas.drawPath(proposedFork, paint)
            paint.pathEffect = null
            paint.style = Paint.Style.FILL
            paint.color = OCHRE
            canvas.drawCircle(708f, 608f, 20f, paint)
            paint.color = Color.WHITE
            paint.textSize = 20f
            paint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("?", 702f, 615f, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = BLUE
        canvas.drawCircle(462f, 552f, 22f, paint)
        paint.color = Color.WHITE
        paint.textSize = 23f
        canvas.drawText("●", 454f, 560f, paint)
    }

    private fun drawTag(canvas: Canvas, paint: Paint, x: Float, y: Float, text: String, color: Int) {
        paint.textSize = 25f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val width = paint.measureText(text) + 36f
        paint.color = color
        canvas.drawRoundRect(RectF(x, y, x + width, y + 52f), 24f, 24f, paint)
        paint.color = Color.WHITE
        canvas.drawText(text, x + 18f, y + 35f, paint)
    }

    private fun drawTape(canvas: Canvas, paint: Paint, x: Float, y: Float, width: Float, height: Float, color: Int, rotation: Float) {
        canvas.save()
        canvas.rotate(rotation, x + width / 2, y + height / 2)
        paint.color = color
        canvas.drawRoundRect(RectF(x, y, x + width, y + height), 7f, 7f, paint)
        canvas.restore()
    }

    private fun drawNote(canvas: Canvas, paint: Paint, rect: RectF, color: Int, title: String, detail: String, stamp: String) {
        drawPaperCard(canvas, paint, rect, 24f, color)
        paint.color = INK
        paint.textSize = 29f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(title, rect.left + 24f, rect.top + 48f, paint)
        paint.color = MUTED
        paint.textSize = 23f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        drawWrappedText(canvas, paint, detail, rect.left + 24f, rect.top + 88f, rect.width() - 48f, 31f, 2)
        paint.color = Color.argb(55, 38, 66, 56)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawCircle(rect.right - 55f, rect.bottom - 42f, 28f, paint)
        paint.style = Paint.Style.FILL
        paint.textSize = 13f
        paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(stamp, rect.right - 80f, rect.bottom - 37f, paint)
    }

    private fun drawWrappedText(canvas: Canvas, paint: Paint, text: String, x: Float, y: Float, maxWidth: Float, lineHeight: Float, maxLines: Int) {
        var line = ""
        var lineIndex = 0
        for (char in text) {
            val candidate = line + char
            if (paint.measureText(candidate) > maxWidth && line.isNotEmpty()) {
                canvas.drawText(if (lineIndex == maxLines - 1) "$line…" else line, x, y + lineIndex * lineHeight, paint)
                lineIndex++
                line = char.toString()
                if (lineIndex >= maxLines) return
            } else line = candidate
        }
        if (line.isNotEmpty() && lineIndex < maxLines) canvas.drawText(line, x, y + lineIndex * lineHeight, paint)
    }

    private companion object {
        const val WIDTH = 1080
        const val HEIGHT = 1920
        val PAPER = Color.rgb(247, 241, 226)
        val INK = Color.rgb(38, 51, 47)
        val MUTED = Color.rgb(98, 111, 105)
        val SAGE = Color.rgb(63, 111, 92)
        val SAGE_DARK = Color.rgb(41, 78, 64)
        val OCHRE = Color.rgb(184, 107, 44)
        val BLUE = Color.rgb(78, 113, 139)
        val GOLD = Color.rgb(192, 138, 46)
        val BLUE_PAPER = Color.rgb(225, 235, 240)
        val ORANGE_PAPER = Color.rgb(252, 234, 216)
    }
}
