package cn.tsinghua.sagemotion.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.tsinghua.sagemotion.model.LandmarkGlyph
import cn.tsinghua.sagemotion.model.LandmarkStyle
import cn.tsinghua.sagemotion.model.ParkLandmark
import cn.tsinghua.sagemotion.ui.theme.SageGold
import cn.tsinghua.sagemotion.ui.theme.SageGreen
import cn.tsinghua.sagemotion.ui.theme.SageGreenDark
import cn.tsinghua.sagemotion.ui.theme.SageInk
import cn.tsinghua.sagemotion.ui.theme.SageMotion
import cn.tsinghua.sagemotion.ui.theme.SageMuted
import cn.tsinghua.sagemotion.ui.theme.SageOpera
import cn.tsinghua.sagemotion.ui.theme.SagePanelRaised
import cn.tsinghua.sagemotion.ui.theme.SageSignalLime

/**
 * 路线几何。主路线、备选路线与重规划路线共用同一套控制点，
 * 保证语义动效、地标层与手账拼贴落在完全相同的空间位置上。
 */
internal object RouteGeometry {

    fun main(size: Size): Path = Path().apply {
        moveTo(size.width * .13f, size.height * .57f)
        cubicTo(size.width * .28f, size.height * .49f, size.width * .35f, size.height * .40f, size.width * .49f, size.height * .38f)
        cubicTo(size.width * .63f, size.height * .36f, size.width * .70f, size.height * .27f, size.width * .84f, size.height * .25f)
    }

    fun alternative(size: Size): Path = Path().apply {
        moveTo(size.width * .13f, size.height * .57f)
        cubicTo(size.width * .29f, size.height * .62f, size.width * .54f, size.height * .59f, size.width * .84f, size.height * .25f)
    }

    /** 沿主路线取点。fraction 为 0–1 的路程比例。 */
    fun pointOnMain(size: Size, fraction: Float): Offset {
        val measure = PathMeasure().apply { setPath(main(size), false) }
        return measure.getPosition(measure.length * fraction.coerceIn(0f, 1f))
    }
}

/**
 * 沿途地标层。
 *
 * 对应设计建议便签：「路线规划要不要参考【圆周旅迹】，它会把路途的地点标注出来」，
 * 以及「看看地标是 1. 数字形式 2. icon形式 3. 3D效果（访谈中有人建议这样和圆周旅迹区分开来）」。
 *
 * 三种形态都实现了，由 [style] 切换。地标随路线显影逐个落位——
 * 顺序与路程一致，形成 common fate 的感知分组，而不是一次性全部弹出。
 *
 * @param reveal 主路线当前的显影进度，地标只在路线已经画到它那里时才出现。
 */
@Composable
fun RouteLandmarkLayer(
    landmarks: List<ParkLandmark>,
    style: LandmarkStyle,
    reveal: Float,
    modifier: Modifier = Modifier,
    selectedId: String? = null,
    onSelect: (ParkLandmark) -> Unit = {},
    showLabels: Boolean = true,
) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier.fillMaxSize()) {
        val canvasSize = with(density) { Size(maxWidth.toPx(), maxHeight.toPx()) }
        val pinSize: Dp = if (style == LandmarkStyle.DEPTH) 46.dp else 38.dp

        landmarks.forEach { landmark ->
            // 只有路线已经算到这个点位，标注才落下——因果顺序对用户可见。
            val arrived = reveal >= landmark.routeFraction - .02f
            val drop by animateFloatAsState(
                targetValue = if (arrived) 1f else 0f,
                animationSpec = SageMotion.reboundLow(),
                label = "landmarkDrop_${landmark.id}",
            )
            if (drop <= 0.01f) return@forEach

            val point = RouteGeometry.pointOnMain(canvasSize, landmark.routeFraction)
            val xDp = with(density) { point.x.toDp() } - pinSize / 2
            val yDp = with(density) { point.y.toDp() } - pinSize

            Box(Modifier.offset(x = xDp, y = yDp)) {
                LandmarkPin(
                    landmark = landmark,
                    index = landmarks.indexOf(landmark),
                    style = style,
                    selected = landmark.id == selectedId,
                    drop = drop,
                    size = pinSize,
                    onClick = { onSelect(landmark) },
                )
            }

            if (showLabels && drop > .55f) {
                // 名称贴在锚点右侧，随锚点一起落位，不单独做动画抢注意力。
                Box(
                    Modifier
                        .offset(x = xDp + pinSize - 2.dp, y = yDp + pinSize / 3)
                        .graphicsLayer { alpha = ((drop - .55f) / .45f).coerceIn(0f, 1f) },
                ) {
                    LandmarkLabel(landmark)
                }
            }
        }
    }
}

@Composable
private fun LandmarkLabel(landmark: ParkLandmark) {
    Surface(
        color = SagePanelRaised.copy(alpha = .96f),
        shape = RoundedCornerShape(9.dp),
        shadowElevation = 3.dp,
    ) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                landmark.name,
                color = SageInk,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 86.dp),
            )
            Text(
                "${landmark.minutesFromStart}′",
                color = SageMuted,
                fontSize = 9.sp,
                modifier = Modifier.padding(start = 5.dp),
            )
        }
    }
}

@Composable
private fun LandmarkPin(
    landmark: ParkLandmark,
    index: Int,
    style: LandmarkStyle,
    selected: Boolean,
    drop: Float,
    size: Dp,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) .92f else 1f,
        animationSpec = SageMotion.fast(),
        label = "landmarkPress",
    )
    Box(
        Modifier
            .size(size)
            .graphicsLayer {
                // 落位：从上方掉下并回弹一次，越晚的点位越晚落地。
                translationY = (1f - drop) * -18f
                alpha = drop
                scaleX = press
                scaleY = press
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        when (style) {
            LandmarkStyle.NUMBER -> NumberPin(index + 1, selected)
            LandmarkStyle.ICON -> IconPin(landmark.glyph, selected)
            LandmarkStyle.DEPTH -> DepthPin(landmark.glyph, index + 1, selected)
        }
    }
}

/** 形态一：数字标注。最接近圆周旅迹的读法，顺序一目了然。 */
@Composable
private fun NumberPin(number: Int, selected: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height * .42f)
            val radius = size.minDimension * .34f
            drawCircle(Color.Black.copy(alpha = .12f), radius * .55f, Offset(center.x, size.height * .92f))
            drawCircle(Color.White, radius + 2.dp.toPx(), center)
            drawCircle(if (selected) SageGreenDark else SageGreen, radius, center)
            // 引脚：把标注和地面位置连起来，避免悬空歧义。
            drawLine(
                Color.White,
                Offset(center.x, center.y + radius),
                Offset(center.x, size.height * .88f),
                2.5.dp.toPx(),
                StrokeCap.Round,
            )
        }
        Text(
            number.toString(),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(y = (-4).dp),
        )
    }
}

/** 形态二：图标标注。用形状表达场地类型，读语义比读顺序快。 */
@Composable
private fun IconPin(glyph: LandmarkGlyph, selected: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * .42f)
        val radius = size.minDimension * .34f
        drawCircle(Color.Black.copy(alpha = .12f), radius * .55f, Offset(center.x, size.height * .92f))
        drawCircle(Color.White, radius + 2.dp.toPx(), center)
        drawCircle(if (selected) SageGreenDark else SageGreen, radius, center)
        drawLine(
            Color.White,
            Offset(center.x, center.y + radius),
            Offset(center.x, size.height * .88f),
            2.5.dp.toPx(),
            StrokeCap.Round,
        )
        drawLandmarkGlyph(glyph, center, radius * .92f, Color.White)
    }
}

/**
 * 形态三：立体标注。
 *
 * 访谈里有人建议用 3D 效果和圆周旅迹区分开来。这里不做真透视，
 * 而是用「地面投影椭圆 + 立柱 + 抬高的斜面牌」构成可读的伪三维：
 * 在移动端小尺寸下比真 3D 更清楚，也不会因为透视变形而遮挡路线。
 */
@Composable
private fun DepthPin(glyph: LandmarkGlyph, number: Int, selected: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val groundY = size.height * .90f
            val faceCenter = Offset(cx, size.height * .36f)
            val faceRadius = size.minDimension * .32f

            // 地面投影：确立「牌子立在地上」的空间关系。
            drawOval(
                color = Color.Black.copy(alpha = .16f),
                topLeft = Offset(cx - faceRadius * .75f, groundY - faceRadius * .22f),
                size = Size(faceRadius * 1.5f, faceRadius * .44f),
            )
            // 立柱
            drawLine(
                SageGreenDark.copy(alpha = .55f),
                Offset(cx, groundY),
                Offset(cx, faceCenter.y + faceRadius * .70f),
                3.dp.toPx(),
                StrokeCap.Round,
            )
            // 侧面：制造厚度。
            drawRoundRect(
                color = SageGreenDark,
                topLeft = Offset(cx - faceRadius, faceCenter.y - faceRadius * .78f + 4.dp.toPx()),
                size = Size(faceRadius * 2f, faceRadius * 1.56f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),
            )
            // 正面牌
            drawRoundRect(
                color = if (selected) SageGold else Color.White,
                topLeft = Offset(cx - faceRadius, faceCenter.y - faceRadius * .78f),
                size = Size(faceRadius * 2f, faceRadius * 1.56f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(9.dp.toPx()),
            )
            // 顶部高光棱：让厚度有方向感（光来自左上）。
            drawLine(
                Color.White.copy(alpha = .85f),
                Offset(cx - faceRadius * .78f, faceCenter.y - faceRadius * .70f),
                Offset(cx + faceRadius * .55f, faceCenter.y - faceRadius * .70f),
                1.5.dp.toPx(),
                StrokeCap.Round,
            )
            drawLandmarkGlyph(glyph, Offset(cx, faceCenter.y - faceRadius * .06f), faceRadius * .78f, if (selected) SageInk else SageGreen)
        }
        // 序号角标：立体形态同时保留顺序信息，兼顾两种读法。
        Surface(
            color = SageOpera,
            shape = CircleShape,
            modifier = Modifier.align(Alignment.TopEnd).offset(x = (-2).dp, y = 1.dp),
        ) {
            Text(
                number.toString(),
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
            )
        }
    }
}

/** 用几何形状画场地类型，不依赖字体图标，保证任意缩放下都清晰。 */
internal fun DrawScope.drawLandmarkGlyph(glyph: LandmarkGlyph, center: Offset, radius: Float, color: Color) {
    val r = radius * .58f
    when (glyph) {
        LandmarkGlyph.GATE -> {
            // 门：两柱一梁
            drawLine(color, Offset(center.x - r, center.y + r * .8f), Offset(center.x - r, center.y - r * .5f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(center.x + r, center.y + r * .8f), Offset(center.x + r, center.y - r * .5f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(center.x - r * 1.25f, center.y - r * .5f), Offset(center.x + r * 1.25f, center.y - r * .5f), 2.dp.toPx(), StrokeCap.Round)
        }
        LandmarkGlyph.WATER -> {
            // 水：两道波
            repeat(2) { row ->
                val y = center.y - r * .25f + row * r * .75f
                val wave = Path().apply {
                    moveTo(center.x - r, y)
                    cubicTo(center.x - r * .4f, y - r * .5f, center.x + r * .4f, y + r * .5f, center.x + r, y)
                }
                drawPath(wave, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        LandmarkGlyph.GROVE -> {
            // 花境：一枝三点
            drawLine(color, Offset(center.x, center.y + r), Offset(center.x, center.y - r * .2f), 2.dp.toPx(), StrokeCap.Round)
            drawCircle(color, r * .34f, Offset(center.x, center.y - r * .55f))
            drawCircle(color, r * .26f, Offset(center.x - r * .6f, center.y - r * .1f))
            drawCircle(color, r * .26f, Offset(center.x + r * .6f, center.y - r * .1f))
        }
        LandmarkGlyph.STAGE -> {
            // 戏台：台面加两侧幕
            drawLine(color, Offset(center.x - r * 1.1f, center.y + r * .7f), Offset(center.x + r * 1.1f, center.y + r * .7f), 2.dp.toPx(), StrokeCap.Round)
            val curtain = Path().apply {
                moveTo(center.x - r, center.y + r * .5f)
                cubicTo(center.x - r * .8f, center.y - r * .3f, center.x - r * .3f, center.y - r * .6f, center.x, center.y - r * .65f)
                cubicTo(center.x + r * .3f, center.y - r * .6f, center.x + r * .8f, center.y - r * .3f, center.x + r, center.y + r * .5f)
            }
            drawPath(curtain, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
        LandmarkGlyph.REST -> {
            // 座椅：靠背与坐面
            drawLine(color, Offset(center.x - r * .9f, center.y + r * .1f), Offset(center.x + r * .9f, center.y + r * .1f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(center.x - r * .9f, center.y - r * .6f), Offset(center.x - r * .9f, center.y + r * .8f), 2.dp.toPx(), StrokeCap.Round)
            drawLine(color, Offset(center.x + r * .9f, center.y + r * .1f), Offset(center.x + r * .9f, center.y + r * .8f), 2.dp.toPx(), StrokeCap.Round)
        }
        LandmarkGlyph.PLAY -> {
            // 活动区：滑梯折线
            val slide = Path().apply {
                moveTo(center.x - r, center.y + r * .8f)
                lineTo(center.x + r * .2f, center.y - r * .6f)
                lineTo(center.x + r, center.y - r * .6f)
            }
            drawPath(slide, color, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
            drawCircle(color, r * .22f, Offset(center.x - r * .35f, center.y + r * .1f))
        }
    }
}

/**
 * 路段信息条。参考圆周旅迹在路线上直接标出里程与用时的做法，
 * 让「这条路线有多长」不需要点开结果面板才能知道。
 */
@Composable
fun RouteSegmentChip(
    distanceLabel: String,
    minutesLabel: String,
    accent: Color = SageGreen,
    modifier: Modifier = Modifier,
) {
    Surface(color = accent, shape = RoundedCornerShape(8.dp), shadowElevation = 2.dp, modifier = modifier) {
        Row(Modifier.padding(horizontal = 7.dp, vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(distanceLabel, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
            Box(
                Modifier
                    .padding(horizontal = 5.dp)
                    .size(2.dp)
                    .background(Color.White.copy(alpha = .6f), CircleShape),
            )
            Text(minutesLabel, color = Color.White.copy(alpha = .88f), fontSize = 9.sp)
        }
    }
}

/**
 * 地标清单。结果面板里用列表再讲一遍沿途点位，
 * 因为 Study 1 显示 83.8% 的样本需要文字辅助，空间标注不能独自承担全部信息。
 */
@Composable
fun LandmarkList(
    landmarks: List<ParkLandmark>,
    style: LandmarkStyle,
    modifier: Modifier = Modifier,
    selectedId: String? = null,
    onSelect: (ParkLandmark) -> Unit = {},
) {
    Column(modifier) {
        landmarks.forEachIndexed { index, landmark ->
            val selected = landmark.id == selectedId
            Surface(
                color = if (selected) SageSignalLime.copy(alpha = .11f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .padding(top = if (index == 0) 0.dp else 4.dp)
                    .clickable { onSelect(landmark) },
            ) {
                Row(
                    Modifier.padding(horizontal = 9.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Canvas(Modifier.size(26.dp)) {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        drawCircle(if (selected) SageSignalLime.copy(alpha = .18f) else SageGreen.copy(alpha = .16f), size.minDimension * .5f, center)
                        drawLandmarkGlyph(landmark.glyph, center, size.minDimension * .38f, if (selected) SageSignalLime else SageMuted)
                    }
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(
                            "${index + 1}. ${landmark.name}",
                            color = SageInk,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${landmark.zone} · ${landmark.note}",
                            color = SageMuted,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        "${landmark.minutesFromStart} 分",
                        color = SageMuted,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(start = 6.dp).width(34.dp),
                    )
                }
            }
        }
        // 标注形态说明：让研究员在录像里能看出当前用的是哪一种。
        Text(
            "标注形态：${style.label} · ${style.description}",
            color = SageMuted,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 6.dp, start = 9.dp),
        )
    }
}
