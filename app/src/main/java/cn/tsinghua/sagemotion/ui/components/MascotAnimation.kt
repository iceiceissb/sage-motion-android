package cn.tsinghua.sagemotion.ui.components

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import cn.tsinghua.sagemotion.R
import kotlin.math.floor

/** 银小叶在不同任务中的语义动作；行号与生成的 4 x 5 序列帧资源保持一致。 */
enum class MascotMood(internal val spriteRow: Int, val description: String) {
    IDLE(0, "银小叶正在陪伴"),
    NAVIGATING(1, "银小叶正在指路"),
    DISCOVERING(2, "银小叶正在观察"),
    LISTENING(3, "银小叶正在倾听"),
    CELEBRATING(4, "银小叶正在庆祝"),
}

/**
 * 播放生成式序列帧资源。
 *
 * 使用 sprite sheet 而不是 GIF：Android 端可以保留完整 Alpha、按任务直接切换动作，
 * 同时避免 GIF 的 256 色限制和额外解码开销。截图测试固定在第二帧，保证回归图稳定。
 */
@Composable
fun AnimatedMascot(
    mood: MascotMood,
    modifier: Modifier = Modifier,
    contentDescription: String = mood.description,
    animate: Boolean = true,
    @DrawableRes spriteRes: Int = R.drawable.ip_ginkgo_sprite_v2,
) {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val image = remember(context.resources, spriteRes) {
        BitmapFactory.decodeResource(context.resources, spriteRes).asImageBitmap()
    }
    val transition = rememberInfiniteTransition(label = "mascotFrames")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_480, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "mascotFrameProgress",
    )
    val frame = if (!animate || inspection) 1 else floor(progress).toInt().coerceIn(0, 3)
    val cellWidth = image.width / 4
    val cellHeight = image.height / 5

    Canvas(
        modifier
            .aspectRatio(cellWidth.toFloat() / cellHeight.toFloat())
            .semantics { this.contentDescription = contentDescription },
    ) {
        drawSpriteCell(
            image = image,
            sourceOffset = IntOffset(frame * cellWidth, mood.spriteRow * cellHeight),
            sourceSize = IntSize(cellWidth, cellHeight),
        )
    }
}

private fun DrawScope.drawSpriteCell(
    image: androidx.compose.ui.graphics.ImageBitmap,
    sourceOffset: IntOffset,
    sourceSize: IntSize,
) {
    drawImage(
        image = image,
        srcOffset = sourceOffset,
        srcSize = sourceSize,
        dstOffset = IntOffset.Zero,
        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
    )
}
