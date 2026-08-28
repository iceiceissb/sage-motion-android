package cn.tsinghua.sagemotion.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import cn.tsinghua.sagemotion.ui.theme.SageHud
import cn.tsinghua.sagemotion.ui.theme.SageHudEdge
import cn.tsinghua.sagemotion.ui.theme.SageSignalCyan
import cn.tsinghua.sagemotion.ui.theme.SageSignalLime

/**
 * 真实地图上方的“环境信号层”。深色只用来托住信息，地图仍是空间主角；
 * 一像素亮边和微弱扫描线建立数字公园的品牌识别，不模拟厚重毛玻璃。
 */
@Composable
fun SignalHudSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(22.dp),
    borderColor: Color = SageHudEdge,
    shadowElevation: androidx.compose.ui.unit.Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        shadowElevation = shadowElevation,
    ) {
        Box(
            modifier = Modifier
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            SageHud.copy(alpha = .98f),
                            Color(0xFF07100E).copy(alpha = .96f),
                            Color(0xFF030807).copy(alpha = .98f),
                        ),
                    ),
                )
                .border(1.dp, borderColor, shape),
        ) {
            Canvas(Modifier.matchParentSize()) {
                var y = 14.dp.toPx()
                while (y < size.height) {
                    drawLine(
                        Color.White.copy(alpha = .018f),
                        Offset(0f, y),
                        Offset(size.width, y),
                        strokeWidth = .6.dp.toPx(),
                    )
                    y += 17.dp.toPx()
                }
                drawLine(
                    SageSignalCyan.copy(alpha = .10f),
                    Offset(size.width * .12f, 0f),
                    Offset(size.width * .78f, 0f),
                    strokeWidth = 1.dp.toPx(),
                )
            }
            content()
        }
    }
}

@Composable
fun SignalDivider(
    modifier: Modifier = Modifier,
    vertical: Boolean = false,
    color: Color = Color.White,
) {
    Canvas(modifier) {
        if (vertical) {
            drawLine(
                color.copy(alpha = .20f),
                Offset(center.x, 0f),
                Offset(center.x, size.height),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx())),
            )
        } else {
            drawLine(
                color.copy(alpha = .20f),
                Offset(0f, center.y),
                Offset(size.width, center.y),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
}

@Composable
fun SignalWaveform(
    modifier: Modifier = Modifier,
    color: Color = SageSignalCyan,
) {
    Canvas(modifier) {
        val samples = listOf(.08f, .18f, .10f, .42f, .16f, .76f, .28f, .58f, .12f, .34f, .09f, .20f)
        val step = size.width / samples.lastIndex
        val path = Path().apply {
            moveTo(0f, center.y)
            samples.forEachIndexed { index, sample ->
                lineTo(index * step, center.y - size.height * sample * .48f)
                lineTo((index + .45f) * step, center.y + size.height * sample * .32f)
            }
            lineTo(size.width, center.y)
        }
        drawPath(path, color.copy(alpha = .24f), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))
        drawPath(path, color, style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(SageSignalLime.copy(alpha = .22f), 6.dp.toPx(), Offset(size.width, center.y))
        drawCircle(SageSignalLime, 2.4.dp.toPx(), Offset(size.width, center.y))
    }
}

/** 少量等高线装饰；固定路径保证 Compose 截图回归稳定。 */
@Composable
fun SignalContours(
    modifier: Modifier = Modifier,
    color: Color = SageSignalLime,
) {
    Canvas(modifier) {
        repeat(5) { index ->
            val inset = index * 8.dp.toPx()
            val path = Path().apply {
                moveTo(size.width * .14f + inset, size.height * .18f + inset * .12f)
                cubicTo(size.width * .34f, -inset * .04f, size.width * .80f, size.height * .04f + inset, size.width * .88f - inset * .18f, size.height * .28f)
                cubicTo(size.width * .98f - inset, size.height * .56f, size.width * .67f, size.height * .86f - inset * .18f, size.width * .42f, size.height * .78f - inset * .08f)
                cubicTo(size.width * .16f, size.height * .70f, size.width * .02f + inset * .20f, size.height * .42f, size.width * .14f + inset, size.height * .18f + inset * .12f)
            }
            drawPath(
                path,
                color.copy(alpha = .30f - index * .038f),
                style = Stroke(width = 1.dp.toPx()),
            )
        }
    }
}
