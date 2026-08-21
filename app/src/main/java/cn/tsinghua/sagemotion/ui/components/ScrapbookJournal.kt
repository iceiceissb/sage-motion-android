package cn.tsinghua.sagemotion.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import kotlin.math.sin

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
        shape = RoundedCornerShape(22.dp),
        modifier = modifier,
    ) {
        Box {
            PaperTexture(Modifier.matchParentSize())
            Column(Modifier.padding(horizontal = 16.dp, vertical = 16.dp)) {
                ScrapbookHeader(stats, reveal)
                GatheredScenesField(
                    moments = moments,
                    landmarks = landmarks,
                    reveal = reveal,
                    selectedIndex = selectedIndex,
                    onMomentSelected = onMomentSelected,
                    modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                )
                ScrapbookFooter(stats, reveal, Modifier.padding(top = 14.dp))
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
            Surface(color = SageGreenDark, shape = RoundedCornerShape(100.dp)) {
                Text(
                    "本机纸刊预览",
                    color = Color.White,
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
    val landmark = landmarks.getOrNull(
        if (landmarks.isEmpty()) 0
        else ((activeIndex + 1) * landmarks.lastIndex / moments.size.coerceAtLeast(1)).coerceIn(0, landmarks.lastIndex),
    )
    val cobalt = Color(0xFF2857C5)
    val tornShape = remember {
        GenericShape { size, _ ->
            moveTo(size.width * .03f, size.height * .05f)
            lineTo(size.width * .18f, size.height * .015f)
            lineTo(size.width * .37f, size.height * .04f)
            lineTo(size.width * .58f, size.height * .012f)
            lineTo(size.width * .78f, size.height * .045f)
            lineTo(size.width * .97f, size.height * .02f)
            lineTo(size.width * .985f, size.height * .22f)
            lineTo(size.width * .96f, size.height * .43f)
            lineTo(size.width * .99f, size.height * .66f)
            lineTo(size.width * .955f, size.height * .94f)
            lineTo(size.width * .78f, size.height * .98f)
            lineTo(size.width * .61f, size.height * .955f)
            lineTo(size.width * .42f, size.height * .99f)
            lineTo(size.width * .23f, size.height * .96f)
            lineTo(size.width * .02f, size.height * .985f)
            lineTo(size.width * .04f, size.height * .75f)
            lineTo(size.width * .012f, size.height * .53f)
            lineTo(size.width * .045f, size.height * .31f)
            close()
        }
    }

    Box(
        modifier
            .height(430.dp)
            .clickable(
                enabled = moments.size > 1,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { onMomentSelected((activeIndex + 1) % moments.size) },
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = reveal }) {
            // 宽阔的安静纸面里只保留一组简化叶形，避免逐叶描摹。
            val leafMass = Path().apply {
                moveTo(size.width * .57f, size.height * .10f)
                cubicTo(size.width * .92f, size.height * .02f, size.width * 1.02f, size.height * .26f, size.width * .73f, size.height * .42f)
                cubicTo(size.width * .92f, size.height * .44f, size.width * .99f, size.height * .67f, size.width * .64f, size.height * .70f)
                cubicTo(size.width * .74f, size.height * .88f, size.width * .56f, size.height * .96f, size.width * .49f, size.height * .72f)
                close()
            }
            drawPath(leafMass, SageGreen.copy(alpha = .18f))

            val route = Path().apply {
                moveTo(size.width * .03f, size.height * .87f)
                cubicTo(size.width * .27f, size.height * .78f, size.width * .33f, size.height * .43f, size.width * .56f, size.height * .50f)
                cubicTo(size.width * .72f, size.height * .55f, size.width * .82f, size.height * .30f, size.width * .97f, size.height * .18f)
            }
            drawPath(route, SageGreenDark.copy(alpha = .34f), style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 7f))))

            // 单一钴蓝色与路线共用同一条斜向骨架，承担引导而不是角落装饰。
            val bluePassage = Path().apply {
                moveTo(size.width * .03f, size.height * .79f)
                cubicTo(size.width * .24f, size.height * .72f, size.width * .40f, size.height * .56f, size.width * .58f, size.height * .49f)
                cubicTo(size.width * .68f, size.height * .45f, size.width * .72f, size.height * .39f, size.width * .78f, size.height * .34f)
            }
            drawPath(bluePassage, cobalt.copy(alpha = .78f), style = Stroke(11.dp.toPx(), cap = StrokeCap.Round))
        }

        if (moment != null) {
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(top = 28.dp)
                    .fillMaxWidth(.76f)
                    .height(245.dp)
                    .clip(tornShape)
                    .background(SagePaperShade)
                    .graphicsLayer {
                        alpha = reveal
                        translationX = (1f - reveal) * -28f
                    },
            ) {
                ScrapbookPhoto(moment.photoUri, moment.label, Modifier.fillMaxSize())
                Box(Modifier.matchParentSize().background(Color.White.copy(alpha = .04f)))
            }
            Surface(
                color = SagePaper.copy(alpha = .94f),
                shape = RoundedCornerShape(10.dp),
                shadowElevation = 1.dp,
                modifier = Modifier.align(Alignment.CenterEnd).padding(top = 86.dp),
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                    Text(moment.label.ifBlank { "沿途发现" }, color = SageInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(
                        landmark?.name ?: "园内片段",
                        color = SageMuted,
                        fontSize = 9.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        } else {
            EmptyScrapbookNote(Modifier.align(Alignment.Center).fillMaxWidth(.82f))
        }

        Text(
            "Along the way",
            color = SageInk.copy(alpha = .62f),
            fontSize = 10.sp,
            letterSpacing = 1.2.sp,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 8.dp, bottom = 28.dp),
        )
        if (moments.size > 1) {
            Surface(
                color = SageGreenDark.copy(alpha = .90f),
                shape = RoundedCornerShape(100.dp),
                modifier = Modifier.align(Alignment.BottomStart).padding(start = 8.dp, bottom = 18.dp),
            ) {
                Text(
                    "轻触换一张 · ${activeIndex + 1}/${moments.size}",
                    color = Color.White,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                )
            }
        }
    }
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
                color = SageGreenDark,
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
            color = Color.White,
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
    Surface(color = Color.White.copy(alpha = .60f), shape = RoundedCornerShape(14.dp), modifier = modifier) {
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
    Surface(color = Color.White.copy(alpha = .66f), shape = RoundedCornerShape(10.dp), modifier = modifier) {
        Column(
            Modifier.padding(vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, color = SageGreenDark, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
