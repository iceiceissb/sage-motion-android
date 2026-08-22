package cn.tsinghua.sagemotion.ui.components

import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
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
import kotlinx.coroutines.delay

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
 * 同时避免 GIF 的 256 色限制和额外解码开销。角色只在进入场景或语义状态变化时
 * 播放一轮短动作，随后停在自然帧；只有明确的处理中状态才通过 [loop] 持续动作，
 * 不会常驻循环抢占地图注意力。
 * 截图测试固定在第二帧，保证回归图稳定。
 */
@Composable
fun AnimatedMascot(
    mood: MascotMood,
    modifier: Modifier = Modifier,
    contentDescription: String = mood.description,
    animate: Boolean = true,
    loop: Boolean = false,
    @DrawableRes spriteRes: Int = R.drawable.ip_ginkgo_sprite_v2,
) {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    val image = remember(context.resources, spriteRes) {
        BitmapFactory.decodeResource(context.resources, spriteRes).asImageBitmap()
    }
    var frame by remember(mood) { mutableIntStateOf(1) }
    LaunchedEffect(mood, animate, loop, inspection) {
        if (!animate || inspection) {
            frame = 1
            return@LaunchedEffect
        }
        do {
            // 默认两轮 4 帧约 1.4 秒；处理中可循环，状态结束后协程会自动取消。
            repeat(8) { index ->
                frame = index % 4
                delay(175)
            }
        } while (loop)
        frame = 1
    }
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
