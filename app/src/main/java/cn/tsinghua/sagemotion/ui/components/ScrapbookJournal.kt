package cn.tsinghua.sagemotion.ui.components

import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.content.Context
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.tsinghua.sagemotion.R
import cn.tsinghua.sagemotion.model.JourneyPhotoMoment
import cn.tsinghua.sagemotion.model.ParkLandmark
import cn.tsinghua.sagemotion.ui.theme.SageGold
import cn.tsinghua.sagemotion.ui.theme.SageGreen
import cn.tsinghua.sagemotion.ui.theme.SageGreenDark
import cn.tsinghua.sagemotion.ui.theme.SageInk
import cn.tsinghua.sagemotion.ui.theme.SageMotion
import cn.tsinghua.sagemotion.ui.theme.SageMuted
import cn.tsinghua.sagemotion.ui.theme.SageOpera
import cn.tsinghua.sagemotion.ui.theme.SagePaper
import cn.tsinghua.sagemotion.ui.theme.SagePaperEdge
import cn.tsinghua.sagemotion.ui.theme.SagePaperShade
import cn.tsinghua.sagemotion.ui.theme.SagePanelRaised
import cn.tsinghua.sagemotion.ui.theme.SageSignalLime
import kotlin.math.sin

private enum class SceneMotif { BOTANICAL, SCREEN, WATER, ARCHITECTURE, HUMAN, GENERAL }

private fun sceneMotifFor(label: String): SceneMotif {
    val normalized = label.lowercase()
    return when {
        listOf("flower", "plant", "leaf", "rose", "花", "草", "叶", "月季", "蔷薇").any(normalized::contains) -> SceneMotif.BOTANICAL
        listOf("phone", "mobile", "television", "screen", "tv", "手机", "电视", "屏幕", "显示器").any(normalized::contains) -> SceneMotif.SCREEN
        listOf("water", "lake", "river", "pond", "湖", "河", "水", "池").any(normalized::contains) -> SceneMotif.WATER
        listOf("building", "room", "gate", "bridge", "建筑", "房间", "门", "桥", "亭").any(normalized::contains) -> SceneMotif.ARCHITECTURE
        listOf("person", "people", "human", "人", "游客", "儿童").any(normalized::contains) -> SceneMotif.HUMAN
        else -> SceneMotif.GENERAL
    }
}

private fun sceneMotifForPhoto(context: Context, rawUri: String, label: String): SceneMotif {
    val semantic = sceneMotifFor(label)
    val bitmap = runCatching {
        val uri = Uri.parse(rawUri)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > 140 || bounds.outHeight / sample > 140) sample *= 2
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
    }.getOrNull() ?: return semantic

    var pixels = 0
    var pink = 0
    var green = 0
    var blue = 0
    var dark = 0
    val stride = 2
    for (y in 0 until bitmap.height step stride) {
        for (x in 0 until bitmap.width step stride) {
            val pixel = bitmap.getPixel(x, y)
            val red = AndroidColor.red(pixel)
            val g = AndroidColor.green(pixel)
            val b = AndroidColor.blue(pixel)
            pixels++
            if (red > 135 && red > g * 1.22f && b > g * .72f) pink++
            if (g > 70 && g > red * .78f && g > b * 1.12f) green++
            if (b > 110 && b > red * 1.16f && b > g * 1.08f) blue++
            if (red + g + b < 150) dark++
        }
    }
    if (pixels == 0) return semantic
    val pinkRatio = pink.toFloat() / pixels
    val greenRatio = green.toFloat() / pixels
    val blueRatio = blue.toFloat() / pixels
    val darkRatio = dark.toFloat() / pixels
    return when {
        pinkRatio > .012f && greenRatio > .12f -> SceneMotif.BOTANICAL
        semantic == SceneMotif.SCREEN -> SceneMotif.SCREEN
        semantic == SceneMotif.BOTANICAL -> SceneMotif.BOTANICAL
        blueRatio > .20f && darkRatio > .08f -> SceneMotif.SCREEN
        else -> semantic
    }
}

private fun SceneMotif.microText(): String = when (this) {
    SceneMotif.BOTANICAL -> "Petals after rain"
    SceneMotif.SCREEN -> "Blue light, quiet room"
    SceneMotif.WATER -> "Water holds the light"
    SceneMotif.ARCHITECTURE -> "Edges remember the room"
    SceneMotif.HUMAN -> "A quiet passing figure"
    SceneMotif.GENERAL -> "Something caught the eye"
}

/**
 * 本次旅程的可选元数据，对应设计建议里列出的
 * 「可选：icon、AIGC、打卡点建筑、时间点、总耗时」。
 */
data class JourneyStats(
    val routeName: String,
    val totalMinutes: Int,
    val distanceMeters: Int,
    val photoCount: Int,
    val questionCount: Int,
    val voiceCount: Int,
    val replanCount: Int,
    val dateLabel: String,
)

/**
 * Gathered Scenes 风格的本机纸刊预览。
 *
 * 对应设计建议便签：「手账拼贴画的形式，路线+照片+小标题文字。
 * 可选：icon、AIGC、打卡点建筑、时间点、总耗时？」
 *
 * 这里仅负责在端内保持真实照片、路线关系与数据可回看，不冒充生成模型输出。
 * 真正的 Gathered Scenes 成品由服务端生成：真实照片为锚、单一高饱和色成为结构、
 * 大面积留白与撕纸边界共同构成画面。与「路线图」视图并存，不替换它。
 *
 * 生成时按 [reveal] 逐块落位：先纸和路线，再照片，最后贴纸与统计，
 * 保持 staged transition，一次只解释一个主要变化。
 */
@Composable
fun ScrapbookJournal(
    moments: List<JourneyPhotoMoment>,
    landmarks: List<ParkLandmark>,
    stats: JourneyStats,
    modifier: Modifier = Modifier,
    selectedIndex: Int = -1,
    onMomentSelected: (Int) -> Unit = {},
) {
    val inspection = LocalInspectionMode.current
    var entered by remember { mutableStateOf(inspection) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = SageMotion.reveal(1_600),
        label = "scrapbookReveal",
    )

    Surface(
        color = SagePaper,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier,
    ) {
        Box {
            PaperTexture(Modifier.matchParentSize())
            Column(Modifier.padding(8.dp)) {
                GatheredScenesField(
                    moments = moments,
                    landmarks = landmarks,
                    reveal = reveal,
                    selectedIndex = selectedIndex,
                    onMomentSelected = onMomentSelected,
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 5f),
                )
            }
        }
    }
}

/** 纸底：微噪点、折痕与压暗边缘。用绘制而不是位图，避免额外资源和缩放模糊。 */
@Composable
private fun PaperTexture(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        // 纸纹噪点：确定性分布，避免每帧重排造成闪烁。
        val step = 13.dp.toPx()
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = if (row % 2 == 0) 0f else step * .5f
            while (x < size.width) {
                val jitter = sin((x * 0.07f + y * 0.11f).toDouble()).toFloat()
                drawCircle(
                    color = SagePaperEdge.copy(alpha = .16f + jitter * .10f),
                    radius = .9f + jitter * .5f,
                    center = Offset(x, y),
                )
                x += step
            }
            y += step
            row++
        }
        // 中缝折痕
        drawLine(
            SagePaperShade.copy(alpha = .55f),
            Offset(size.width * .5f, 0f),
            Offset(size.width * .5f, size.height),
            1.dp.toPx(),
        )
        // 四边压暗，做出纸张边缘的厚度。
        drawRect(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                0f to SagePaperShade.copy(alpha = .35f),
                .08f to Color.Transparent,
                .92f to Color.Transparent,
                1f to SagePaperShade.copy(alpha = .35f),
            ),
            size = size,
        )
    }
}

@Composable
private fun ScrapbookHeader(stats: JourneyStats, reveal: Float) {
    val headerIn = (reveal * 3f).coerceIn(0f, 1f)
    Column(
        Modifier.graphicsLayer {
            alpha = headerIn
            translationY = (1f - headerIn) * -14f
        },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = SageSignalLime.copy(alpha = .14f), shape = RoundedCornerShape(100.dp)) {
                Text(
                    "本机纸刊预览",
                    color = SageSignalLime,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
            Text(
                "保留真实照片 · 生成式版本需联网",
                color = SageMuted,
                fontSize = 9.sp,
                modifier = Modifier.padding(start = 7.dp).weight(1f),
            )
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                stats.routeName,
                color = SageInk,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )
            // 手写感下划线：一道略带弧度的笔触，而不是直线。
            Spacer(Modifier.width(8.dp))
            Canvas(Modifier.width(46.dp).height(18.dp)) {
                val stroke = Path().apply {
                    moveTo(0f, size.height * .72f)
                    cubicTo(size.width * .3f, size.height * .42f, size.width * .6f, size.height * .92f, size.width, size.height * .55f)
                }
                drawPath(stroke, SageOpera.copy(alpha = .75f), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            }
        }
        Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stats.dateLabel, color = SageMuted, fontSize = 12.sp)
            Box(Modifier.padding(horizontal = 7.dp).size(3.dp).background(SageGold, CircleShape))
            // 总耗时：设计建议里点名的可选项之一。
            Text("共 ${stats.totalMinutes} 分钟", color = SageMuted, fontSize = 12.sp)
            Box(Modifier.padding(horizontal = 7.dp).size(3.dp).background(SageGold, CircleShape))
            Text("${stats.distanceMeters} 米", color = SageMuted, fontSize = 12.sp)
        }
    }
}

/**
 * 端内预览遵循 Gathered Scenes 的结构，而不是堆叠多张“拍立得”：一次只让一张真实照片
 * 成为锚点，路线曲线、一个钴蓝色结构与大块安静纸面共同组织视线。轻触画面轮换照片。
 */
@Composable
private fun GatheredScenesField(
    moments: List<JourneyPhotoMoment>,
    landmarks: List<ParkLandmark>,
    reveal: Float,
    selectedIndex: Int,
    onMomentSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeIndex = selectedIndex.coerceIn(0, moments.lastIndex.coerceAtLeast(0))
    val moment = moments.getOrNull(activeIndex)
    val context = LocalContext.current
    val motif = remember(moment?.photoUri, moment?.label) {
        sceneMotifForPhoto(context, moment?.photoUri.orEmpty(), moment?.label.orEmpty())
    }
    val cobalt = Color(0xFF2857C5)
    BoxWithConstraints(
        modifier
            .clickable(
                enabled = moments.size > 1,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onMomentSelected((activeIndex + 1) % moments.size) },
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = reveal }) {
            drawGatheredSceneMotif(motif, cobalt)
        }

        if (moment != null) {
            TornPhotoAnchor(
                rawUri = moment.photoUri,
                contentDescription = moment.label,
                Modifier
                    .align(Alignment.TopStart)
                    .offset(x = (-10).dp, y = maxHeight * .34f)
                    .fillMaxWidth(.70f)
                    .height(maxHeight * .34f)
                    .graphicsLayer {
                        alpha = reveal
                        translationX = (1f - reveal) * -28f
                    },
            )
        } else {
            EmptyScrapbookNote(Modifier.align(Alignment.Center).fillMaxWidth(.82f))
        }

        Text(
            motif.microText(),
            color = SageInk.copy(alpha = .62f),
            fontSize = 9.sp,
            letterSpacing = 1.0.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 22.dp),
        )
        if (moments.size > 1) {
            Row(
                Modifier.align(Alignment.TopStart).padding(start = 12.dp, top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                moments.take(5).forEachIndexed { index, _ ->
                    Box(
                        Modifier
                            .size(if (index == activeIndex) 8.dp else 5.dp)
                            .background(if (index == activeIndex) cobalt else SagePaperEdge, CircleShape),
                    )
                }
            }
        }
    }
}

/**
 * 根据当前照片的端侧语义标签选择唯一的插画语法。
 * 这不是把同一朵花套在所有照片上：屏幕、植物、水面、建筑与人物各自使用不同的大形体，
 * 同时保留 skill 要求的真实照片锚点、安静留白、单一钴蓝结构色和撕纸边界。
 */
private fun DrawScope.drawGatheredSceneMotif(motif: SceneMotif, cobalt: Color) {
    val quiet = Color(0xFFB8B1A4)
    val olive = Color(0xFF7D8058)
    val charcoal = Color(0xFF34342F)
    when (motif) {
        SceneMotif.SCREEN -> {
            // 对应电视/手机照片：倾斜屏幕、室内墙面和光束，而不是植物模板。
            drawPath(
                Path().apply {
                    moveTo(size.width * .28f, size.height * .38f)
                    lineTo(size.width * .88f, size.height * .25f)
                    lineTo(size.width * .66f, size.height * .68f)
                    lineTo(size.width * .20f, size.height * .57f)
                    close()
                },
                quiet.copy(alpha = .62f),
            )
            drawLine(
                charcoal,
                Offset(size.width * .80f, size.height * .17f),
                Offset(size.width * .61f, size.height * .80f),
                7.dp.toPx(),
                StrokeCap.Square,
            )
            drawPath(
                Path().apply {
                    moveTo(-size.width * .10f, size.height * .98f)
                    lineTo(size.width * .48f, size.height * .61f)
                    lineTo(size.width * .60f, size.height * .70f)
                    lineTo(size.width * .13f, size.height * 1.08f)
                    close()
                },
                cobalt.copy(alpha = .94f),
            )
            drawPath(
                Path().apply {
                    moveTo(size.width * .48f, size.height * .65f)
                    lineTo(size.width * .91f, size.height * .58f)
                    lineTo(size.width * .76f, size.height * .73f)
                    close()
                },
                olive.copy(alpha = .90f),
            )
        }

        SceneMotif.BOTANICAL -> {
            val stem = Path().apply {
                moveTo(size.width * .60f, size.height * .94f)
                cubicTo(size.width * .69f, size.height * .70f, size.width * .62f, size.height * .49f, size.width * .77f, size.height * .22f)
            }
            drawPath(stem, olive, style = Stroke(14.dp.toPx(), cap = StrokeCap.Round))
            listOf(
                Triple(Offset(size.width * .58f, size.height * .70f), Size(size.width * .34f, size.height * .15f), -24f),
                Triple(Offset(size.width * .70f, size.height * .48f), Size(size.width * .30f, size.height * .14f), 22f),
                Triple(Offset(size.width * .62f, size.height * .34f), Size(size.width * .28f, size.height * .12f), -26f),
            ).forEach { (center, leafSize, angle) ->
                rotate(angle, center) {
                    drawOval(
                        olive.copy(alpha = .88f),
                        Offset(center.x - leafSize.width / 2f, center.y - leafSize.height / 2f),
                        leafSize,
                    )
                }
            }
            val flowerCenter = Offset(size.width * .77f, size.height * .20f)
            repeat(5) { index ->
                rotate(index * 72f, flowerCenter) {
                    drawOval(
                        Color(0xFFC87986),
                        Offset(flowerCenter.x - size.width * .026f, flowerCenter.y - size.width * .095f),
                        Size(size.width * .052f, size.width * .10f),
                    )
                }
            }
            drawCircle(Color(0xFFE0B29B), size.width * .025f, flowerCenter)
            drawPath(
                Path().apply {
                    moveTo(-size.width * .08f, size.height * .96f)
                    cubicTo(size.width * .14f, size.height * .86f, size.width * .33f, size.height * .73f, size.width * .52f, size.height * .63f)
                    lineTo(size.width * .61f, size.height * .72f)
                    cubicTo(size.width * .38f, size.height * .87f, size.width * .17f, size.height * 1.01f, -size.width * .06f, size.height * 1.08f)
                    close()
                },
                cobalt.copy(alpha = .92f),
            )
        }

        SceneMotif.WATER -> {
            repeat(3) { index ->
                val y = size.height * (.42f + index * .13f)
                drawPath(
                    Path().apply {
                        moveTo(size.width * .30f, y)
                        cubicTo(size.width * .48f, y - size.height * .06f, size.width * .69f, y + size.height * .05f, size.width * .96f, y - size.height * .02f)
                    },
                    if (index == 1) cobalt else quiet.copy(alpha = .74f),
                    style = Stroke((if (index == 1) 12 else 8).dp.toPx(), cap = StrokeCap.Round),
                )
            }
            drawCircle(olive.copy(alpha = .72f), size.width * .10f, Offset(size.width * .75f, size.height * .23f))
        }

        SceneMotif.ARCHITECTURE -> {
            drawPath(
                Path().apply {
                    moveTo(size.width * .47f, size.height * .78f)
                    lineTo(size.width * .48f, size.height * .31f)
                    lineTo(size.width * .72f, size.height * .18f)
                    lineTo(size.width * .94f, size.height * .36f)
                    lineTo(size.width * .91f, size.height * .82f)
                    close()
                },
                quiet.copy(alpha = .70f),
            )
            repeat(3) { index ->
                drawRect(
                    if (index == 1) cobalt else olive.copy(alpha = .78f),
                    Offset(size.width * (.57f + index * .10f), size.height * .42f),
                    Size(size.width * .065f, size.height * .18f),
                )
            }
            drawLine(charcoal, Offset(size.width * .43f, size.height * .80f), Offset(size.width * .95f, size.height * .80f), 3.dp.toPx())
        }

        SceneMotif.HUMAN -> {
            drawCircle(quiet.copy(alpha = .78f), size.width * .075f, Offset(size.width * .75f, size.height * .30f))
            drawPath(
                Path().apply {
                    moveTo(size.width * .75f, size.height * .37f)
                    cubicTo(size.width * .62f, size.height * .52f, size.width * .68f, size.height * .70f, size.width * .58f, size.height * .89f)
                    moveTo(size.width * .72f, size.height * .53f)
                    lineTo(size.width * .91f, size.height * .63f)
                    moveTo(size.width * .67f, size.height * .66f)
                    lineTo(size.width * .82f, size.height * .91f)
                },
                olive,
                style = Stroke(13.dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                Path().apply {
                    moveTo(-size.width * .08f, size.height * .92f)
                    lineTo(size.width * .58f, size.height * .67f)
                    lineTo(size.width * .65f, size.height * .78f)
                    lineTo(size.width * .04f, size.height * 1.04f)
                    close()
                },
                cobalt.copy(alpha = .92f),
            )
        }

        SceneMotif.GENERAL -> {
            drawPath(
                Path().apply {
                    moveTo(size.width * .46f, size.height * .27f)
                    cubicTo(size.width * .76f, size.height * .16f, size.width * .95f, size.height * .42f, size.width * .84f, size.height * .69f)
                    cubicTo(size.width * .73f, size.height * .88f, size.width * .48f, size.height * .78f, size.width * .39f, size.height * .57f)
                    close()
                },
                olive.copy(alpha = .72f),
            )
            drawPath(
                Path().apply {
                    moveTo(-size.width * .06f, size.height * .94f)
                    cubicTo(size.width * .20f, size.height * .85f, size.width * .40f, size.height * .74f, size.width * .64f, size.height * .57f)
                    lineTo(size.width * .71f, size.height * .67f)
                    cubicTo(size.width * .44f, size.height * .86f, size.width * .20f, size.height * 1.01f, -size.width * .08f, size.height * 1.06f)
                    close()
                },
                cobalt.copy(alpha = .92f),
            )
        }
    }

    // 一条安静的手绘轨迹把照片和插画组织成同一页，不冒充地图路线。
    drawPath(
        Path().apply {
            moveTo(size.width * .20f, size.height * 1.02f)
            cubicTo(size.width * .47f, size.height * .90f, size.width * .66f, size.height * .55f, size.width * .83f, -size.height * .02f)
        },
        charcoal.copy(alpha = .80f),
        style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 7f))),
    )
}

@Composable
private fun TornPhotoAnchor(
    rawUri: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val tornShape = remember {
        GenericShape { size, _ -> addPath(tornPhotoPath(size)) }
    }
    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            drawPath(
                tornPhotoPath(size),
                SagePaperEdge.copy(alpha = .78f),
                style = Stroke(7.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        ScrapbookPhoto(
            rawUri,
            contentDescription,
            Modifier.matchParentSize().clip(tornShape).background(SagePaperShade),
        )
        Canvas(Modifier.matchParentSize()) {
            // 可见的纤维毛边：确定性短线跨过照片与纸张边界，不使用整齐白框。
            val fiber = Color(0xFFD8C8A8).copy(alpha = .92f)
            repeat(34) { index ->
                val x = size.width * (index / 33f)
                val topY = size.height * (.025f + .017f * sin(index * 1.7).toFloat())
                val bottomY = size.height * (.97f + .012f * sin(index * 1.2).toFloat())
                val length = (2.5f + (index % 4)) * density
                drawLine(fiber, Offset(x, topY - length), Offset(x + (index % 3 - 1) * density, topY + length), .7.dp.toPx())
                drawLine(fiber, Offset(x, bottomY - length), Offset(x + ((index + 1) % 3 - 1) * density, bottomY + length), .7.dp.toPx())
            }
            repeat(20) { index ->
                val y = size.height * (index / 19f)
                val leftX = size.width * (.026f + .012f * sin(index * 1.45).toFloat())
                val rightX = size.width * (.972f + .010f * sin(index * 1.15).toFloat())
                val length = (2.5f + (index % 3)) * density
                drawLine(fiber, Offset(leftX - length, y), Offset(leftX + length, y + (index % 2) * density), .7.dp.toPx())
                drawLine(fiber, Offset(rightX - length, y), Offset(rightX + length, y - (index % 2) * density), .7.dp.toPx())
            }
        }
    }
}

private fun tornPhotoPath(size: Size): Path = Path().apply {
    moveTo(size.width * .025f, size.height * .06f)
    lineTo(size.width * .19f, size.height * .018f)
    lineTo(size.width * .38f, size.height * .05f)
    lineTo(size.width * .57f, size.height * .012f)
    lineTo(size.width * .79f, size.height * .055f)
    lineTo(size.width * .975f, size.height * .025f)
    lineTo(size.width * .99f, size.height * .26f)
    lineTo(size.width * .955f, size.height * .49f)
    lineTo(size.width * .988f, size.height * .74f)
    lineTo(size.width * .95f, size.height * .965f)
    lineTo(size.width * .78f, size.height * .99f)
    lineTo(size.width * .60f, size.height * .955f)
    lineTo(size.width * .42f, size.height * .995f)
    lineTo(size.width * .22f, size.height * .96f)
    lineTo(size.width * .018f, size.height * .985f)
    lineTo(size.width * .045f, size.height * .73f)
    lineTo(size.width * .012f, size.height * .52f)
    lineTo(size.width * .05f, size.height * .29f)
    close()
}

/**
 * 拼贴主体。一条手写感的竖向路线贯穿整页，照片左右交错贴在路线两侧，
 * 打卡点标签落在路线上——这就是「路线 + 照片 + 小标题文字」的版式。
 */
@Composable
private fun ScrapbookSpine(
    moments: List<JourneyPhotoMoment>,
    landmarks: List<ParkLandmark>,
    reveal: Float,
    selectedIndex: Int,
    onMomentSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val entries = buildEntries(moments, landmarks)
    if (entries.isEmpty()) {
        EmptyScrapbookNote(modifier)
        return
    }
    val rowHeight = 116.dp

    BoxWithConstraints(modifier.height(rowHeight * entries.size)) {
        val widthPx = constraints.maxWidth.toFloat()
        val heightPx = constraints.maxHeight.toFloat()

        // 手写路线：一条带轻微摆动的竖向折线，路程越往下越晚显影。
        Canvas(Modifier.fillMaxSize()) {
            val spine = Path().apply {
                moveTo(widthPx * .5f, 0f)
                entries.indices.forEach { index ->
                    val segmentTop = heightPx * (index / entries.size.toFloat())
                    val segmentBottom = heightPx * ((index + 1) / entries.size.toFloat())
                    val bulge = if (index % 2 == 0) widthPx * .10f else -widthPx * .10f
                    cubicTo(
                        widthPx * .5f + bulge, segmentTop + (segmentBottom - segmentTop) * .35f,
                        widthPx * .5f + bulge, segmentTop + (segmentBottom - segmentTop) * .65f,
                        widthPx * .5f, segmentBottom,
                    )
                }
            }
            val measure = PathMeasure().apply { setPath(spine, false) }
            val drawn = Path()
            measure.getSegment(0f, measure.length * reveal, drawn, true)
            // 双描边：外层浅色垫底模拟纸上的笔压，内层是主线。
            drawPath(drawn, SagePaperShade, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
            drawPath(
                drawn,
                SageGreenDark.copy(alpha = .78f),
                style = Stroke(
                    2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(11f, 7f)),
                ),
            )
        }

        entries.forEachIndexed { index, entry ->
            val appear = ((reveal - index * .12f) * 3.4f).coerceIn(0f, 1f)
            if (appear <= 0f) return@forEachIndexed
            val onLeft = index % 2 == 0
            val yOffset = rowHeight * index

            // 打卡点标签落在路线上，同时给出时间点。
            Box(
                Modifier
                    .offset(y = yOffset + 30.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                CheckpointStamp(entry, appear)
            }

            Box(
                Modifier
                    .offset(
                        x = if (onLeft) 4.dp else maxWidth - 150.dp,
                        y = yOffset + 6.dp,
                    )
                    .widthIn(max = 146.dp)
                    .graphicsLayer {
                        alpha = appear
                        translationX = (1f - appear) * (if (onLeft) -30f else 30f)
                    },
            ) {
                TapedPhotoCard(
                    entry = entry,
                    index = index,
                    selected = index == selectedIndex,
                    onClick = { if (entry.momentIndex >= 0) onMomentSelected(entry.momentIndex) },
                )
            }
        }
    }
}

/** 拼贴条目：可能来自一张过程照片，也可能只是一个打卡点。 */
private data class ScrapEntry(
    val title: String,
    val caption: String,
    val minuteLabel: String,
    val photoUri: String?,
    val glyphLandmark: ParkLandmark?,
    val questionCount: Int,
    /** 在 moments 列表中的下标，-1 表示这是纯打卡点、没有对应照片。 */
    val momentIndex: Int,
)

/**
 * 把照片与打卡点编织成一条时间线。
 *
 * 照片优先占位（那是用户真实走过的证据），打卡点补足没有照片的路段，
 * 这样即使一张照片都没拍，手账也仍然是一条完整的路线故事。
 */
private fun buildEntries(
    moments: List<JourneyPhotoMoment>,
    landmarks: List<ParkLandmark>,
): List<ScrapEntry> {
    if (moments.isEmpty() && landmarks.isEmpty()) return emptyList()
    val entries = mutableListOf<ScrapEntry>()
    val usableLandmarks = landmarks.ifEmpty { emptyList() }

    moments.forEachIndexed { index, moment ->
        // 把照片按顺序对应到沿途点位，让「在哪里拍的」有空间落点。
        val landmark = usableLandmarks.getOrNull(
            if (usableLandmarks.isEmpty()) 0
            else ((index + 1) * usableLandmarks.lastIndex / (moments.size).coerceAtLeast(1)).coerceIn(0, usableLandmarks.lastIndex),
        )
        entries += ScrapEntry(
            title = moment.label.ifBlank { "沿途发现" },
            caption = moment.questions.firstOrNull()?.question ?: landmark?.note ?: "路上随手记下的一幕",
            minuteLabel = landmark?.let { "${it.minutesFromStart}′" } ?: "",
            photoUri = moment.photoUri.takeIf { it.isNotBlank() },
            glyphLandmark = landmark,
            questionCount = moment.questions.size,
            momentIndex = index,
        )
    }
    if (entries.size < 3) {
        // 照片太少时用真实点位补足版面，而不是塞占位图。
        usableLandmarks.filter { landmark -> entries.none { it.glyphLandmark?.id == landmark.id } }
            .take(3 - entries.size)
            .forEach { landmark ->
                entries += ScrapEntry(
                    title = landmark.name,
                    caption = landmark.note,
                    minuteLabel = "${landmark.minutesFromStart}′",
                    photoUri = null,
                    glyphLandmark = landmark,
                    questionCount = 0,
                    momentIndex = -1,
                )
            }
    }
    return entries.sortedBy { it.glyphLandmark?.routeFraction ?: 0f }.take(6)
}

/** 打卡点印章：路线上的时间点标记，对应「打卡点建筑、时间点」。 */
@Composable
private fun CheckpointStamp(entry: ScrapEntry, appear: Float) {
    val landmark = entry.glyphLandmark ?: return
    Surface(
        color = SagePaper,
        shape = RoundedCornerShape(11.dp),
        shadowElevation = 1.dp,
        modifier = Modifier.graphicsLayer {
            alpha = appear
            scaleX = .86f + appear * .14f
            scaleY = .86f + appear * .14f
        },
    ) {
        Row(
            Modifier
                .background(SagePaper)
                .padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(15.dp)) {
                drawCircle(SageOpera.copy(alpha = .14f), size.minDimension * .5f)
                drawLandmarkGlyph(landmark.glyph, center, size.minDimension * .32f, SageOpera)
            }
            Text(
                landmark.name,
                color = SageSignalLime,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.padding(start = 5.dp),
            )
            if (entry.minuteLabel.isNotBlank()) {
                Text(
                    entry.minuteLabel,
                    color = SageOpera,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 5.dp),
                )
            }
        }
    }
}

/**
 * 胶带贴住的照片卡。
 *
 * 微倾角是手账的关键质感，但角度必须小且稳定（±3°），
 * 否则在小屏上会读成「排版错了」而不是「贴上去的」。
 */
@Composable
private fun TapedPhotoCard(
    entry: ScrapEntry,
    index: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val tilt = listOf(-2.6f, 2.1f, -1.7f, 2.8f, -2.2f, 1.6f)[index % 6]
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .graphicsLayer { rotationZ = tilt }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = entry.momentIndex >= 0,
                onClick = onClick,
            ),
    ) {
        Surface(
            color = SagePanelRaised,
            shape = RoundedCornerShape(3.dp),
            shadowElevation = if (selected) 8.dp else 3.dp,
        ) {
            Column(Modifier.padding(5.dp)) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(SagePaperShade),
                ) {
                    if (entry.photoUri != null) {
                        ScrapbookPhoto(entry.photoUri, entry.title, Modifier.fillMaxSize())
                    } else {
                        // 没有照片的打卡点用绘制的场地图形占位，不假装成照片。
                        Canvas(Modifier.fillMaxSize()) {
                            drawRect(SagePaperShade)
                            entry.glyphLandmark?.let {
                                drawLandmarkGlyph(it.glyph, Offset(size.width / 2f, size.height / 2f), size.minDimension * .18f, SageGreen.copy(alpha = .55f))
                            }
                        }
                    }
                    if (entry.questionCount > 0) {
                        Surface(
                            color = SageOpera,
                            shape = CircleShape,
                            modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
                        ) {
                            Text(
                                "${entry.questionCount} 问",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                // 小标题文字：手账的核心信息层。
                Text(
                    entry.title,
                    color = SageInk,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
                Text(
                    entry.caption,
                    color = SageMuted,
                    fontSize = 9.sp,
                    lineHeight = 13.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp, bottom = 2.dp),
                )
            }
        }
        // 胶带：斜贴在左上角，半透明暖色，压住照片边缘。
        Canvas(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = (-8).dp, y = (-7).dp)
                .size(width = 44.dp, height = 18.dp),
        ) {
            rotate(-24f) {
                drawRect(SageGold.copy(alpha = .34f))
                drawRect(Color.White.copy(alpha = .18f), size = Size(size.width, size.height * .35f))
            }
        }
    }
}

@Composable
private fun EmptyScrapbookNote(modifier: Modifier = Modifier) {
    Surface(color = SagePanelRaised.copy(alpha = .92f), shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Text(
            "这一页还空着。下次在探索途中拍照圈搜，照片、问题和打卡点会自动贴进这本手账。",
            color = SageMuted,
            fontSize = 12.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(14.dp),
        )
    }
}

/** 页脚统计。把本次旅程的可量化部分收在一起，作为手账的落款。 */
@Composable
private fun ScrapbookFooter(stats: JourneyStats, reveal: Float, modifier: Modifier = Modifier) {
    val footerIn = ((reveal - .7f) / .3f).coerceIn(0f, 1f)
    Column(
        modifier.graphicsLayer {
            alpha = footerIn
            translationY = (1f - footerIn) * 16f
        },
    ) {
        Canvas(Modifier.fillMaxWidth().height(1.dp)) {
            drawLine(
                SagePaperEdge,
                Offset(0f, 0f),
                Offset(size.width, 0f),
                1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            ScrapStat("照片", stats.photoCount.toString(), Modifier.weight(1f))
            ScrapStat("提问", stats.questionCount.toString(), Modifier.weight(1f))
            ScrapStat("语音", stats.voiceCount.toString(), Modifier.weight(1f))
            ScrapStat("改线", stats.replanCount.toString(), Modifier.weight(1f))
        }
        Text(
            "本机排版预览 · 照片未上传 · 模型纸刊需接入生成服务",
            color = SageMuted,
            fontSize = 9.sp,
            modifier = Modifier.padding(top = 9.dp),
        )
    }
}

@Composable
private fun ScrapStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(color = SagePanelRaised, shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(
            Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, color = SageSignalLime, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(label, color = SageMuted, fontSize = 9.sp)
        }
    }
}

/** 手账内的照片。与主界面共用同一套解码与降级策略。 */
@Composable
fun ScrapbookPhoto(rawUri: String, contentDescription: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(rawUri) {
        rawUri.takeIf { it.isNotBlank() }?.let { value ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(value))?.use(BitmapFactory::decodeStream)?.asImageBitmap()
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(bitmap, contentDescription, modifier, contentScale = ContentScale.Crop)
    } else {
        Image(painterResource(R.drawable.flower_stimulus), contentDescription, modifier, contentScale = ContentScale.Crop)
    }
}
