package cn.tsinghua.sagemotion.ui.screens

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.tsinghua.sagemotion.R
import cn.tsinghua.sagemotion.ui.components.AnimatedMascot
import cn.tsinghua.sagemotion.ui.components.FrostedGlassSurface
import cn.tsinghua.sagemotion.ui.components.MascotMood
import cn.tsinghua.sagemotion.ui.components.sageBubbleShape
import cn.tsinghua.sagemotion.ui.theme.SageCanopy
import cn.tsinghua.sagemotion.ui.theme.SageGreen
import cn.tsinghua.sagemotion.ui.theme.SageGreenDark
import cn.tsinghua.sagemotion.ui.theme.SageInk
import cn.tsinghua.sagemotion.ui.theme.SageMist
import cn.tsinghua.sagemotion.ui.theme.SageMotion
import cn.tsinghua.sagemotion.ui.theme.SageMuted
import cn.tsinghua.sagemotion.ui.theme.SageSignalCyan
import cn.tsinghua.sagemotion.ui.theme.SageSignalLime
import kotlin.math.PI
import kotlin.math.sin

/**
 * 开屏欢迎动画。
 *
 * 对应设计建议便签：「设置好后，我在想这里能否有一些开屏的欢迎动画？让用户有点代入场景感？
 * 然后再开始后面的任务」。
 *
 * 场景取自真实实验地点——北京海淀东升镇八家郊野公园南园：晨光、生态湖区、林地群落，
 * 以及南园的戏曲文化主题。整个画面用 Canvas 分层绘制，不引入额外位图资源。
 *
 * 编排遵循 staged transition：一次只推进一个主要变化（天光 → 地形 → 湖面 → 林冠 →
 * 步道显影 → 文案落位 → AI 在场），降低对象追踪负担。全程可跳过，不阻断实验流程。
 */
@Composable
fun WelcomeScreen(
    participantId: String,
    onEnter: () -> Unit,
) {
    // 单一时间轴驱动全部分层，保证各层节奏严格对齐。
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val animatedTimeline by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(durationMillis = 4_200, easing = SageMotion.Easing.Standard),
        label = "welcomeTimeline",
    )
    val timeline = if (LocalInspectionMode.current) 1f else animatedTimeline

    val ambient = rememberInfiniteTransition(label = "welcomeAmbient")
    val drift by ambient.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = SageMotion.loop(9_000),
        label = "welcomeDrift",
    )
    val shimmer by ambient.animateFloat(
        initialValue = .3f,
        targetValue = 1f,
        animationSpec = SageMotion.breathe(2_400),
        label = "welcomeShimmer",
    )

    // 各分层的进入区间。stageIn 把全局时间轴切成互不重叠的主变化。
    fun stageIn(from: Float, to: Float): Float =
        ((timeline - from) / (to - from)).coerceIn(0f, 1f)

    val skyIn = stageIn(0f, .22f)
    val hillsIn = stageIn(.12f, .40f)
    val lakeIn = stageIn(.26f, .52f)
    val canopyIn = stageIn(.38f, .64f)
    val trailIn = stageIn(.52f, .80f)
    val titleIn = stageIn(.62f, .84f)
    val orbIn = stageIn(.78f, 1f)

    Box(
        Modifier
            .fillMaxSize()
            .background(SageCanopy)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onEnter,
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val horizon = h * .54f

            // 1) 夜色：与全局黑色信息层一致，保留公园场景但不再切回浅色纸景。
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFF030605).copy(alpha = skyIn),
                    .38f to Color(0xFF071715).copy(alpha = skyIn),
                    1f to SageCanopy,
                ),
                size = Size(w, h),
            )

            // 冷色导航光源，与实时语音的青色信号同源。
            val sunY = horizon - h * (.08f + .06f * skyIn)
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(SageSignalCyan.copy(alpha = .20f * skyIn), Color.Transparent),
                    center = Offset(w * .72f, sunY),
                    radius = w * .34f,
                ),
                radius = w * .34f,
                center = Offset(w * .72f, sunY),
            )
            drawCircle(SageSignalLime.copy(alpha = .52f * skyIn), w * .035f, Offset(w * .72f, sunY))

            // 2) 远山轮廓：两层视差，后层更淡更慢。
            if (hillsIn > 0f) {
                val backHills = Path().apply {
                    moveTo(0f, horizon)
                    cubicTo(w * .18f, horizon - h * .10f * hillsIn, w * .34f, horizon - h * .03f * hillsIn, w * .52f, horizon - h * .07f * hillsIn)
                    cubicTo(w * .72f, horizon - h * .12f * hillsIn, w * .86f, horizon - h * .02f * hillsIn, w, horizon - h * .05f * hillsIn)
                    lineTo(w, horizon); lineTo(0f, horizon); close()
                }
                drawPath(backHills, Color(0xFF17332C).copy(alpha = .76f * hillsIn))

                val frontHills = Path().apply {
                    moveTo(0f, horizon + h * .01f)
                    cubicTo(w * .22f, horizon - h * .05f * hillsIn, w * .44f, horizon + h * .02f * hillsIn, w * .66f, horizon - h * .03f * hillsIn)
                    cubicTo(w * .82f, horizon - h * .06f * hillsIn, w * .93f, horizon + h * .01f, w, horizon - h * .01f)
                    lineTo(w, horizon + h * .02f); lineTo(0f, horizon + h * .02f); close()
                }
                drawPath(frontHills, Color(0xFF0D211C).copy(alpha = .92f * hillsIn))
            }

            // 3) 生态湖区：南园五区之一。水面用横向反光带表达，不做波浪循环，避免抢注意力。
            if (lakeIn > 0f) {
                val lakeTop = horizon + h * .02f
                val lakeBottom = horizon + h * .17f
                drawRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            Color(0xFF123537).copy(alpha = .90f * lakeIn),
                            Color(0xFF071A1B).copy(alpha = .96f * lakeIn),
                        ),
                        startY = lakeTop,
                        endY = lakeBottom,
                    ),
                    topLeft = Offset(0f, lakeTop),
                    size = Size(w, lakeBottom - lakeTop),
                )
                repeat(5) { index ->
                    val y = lakeTop + (lakeBottom - lakeTop) * (.18f + index * .17f)
                    val half = w * (.30f - index * .04f) * lakeIn
                    drawLine(
                        color = SageSignalCyan.copy(alpha = (.22f - index * .03f) * lakeIn * shimmer),
                        start = Offset(w * .72f - half, y),
                        end = Offset(w * .72f + half, y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }
            }

            // 4) 近景林冠：乡土树种群落的剪影，从下方升入。
            if (canopyIn > 0f) {
                val canopyBase = h * (1.06f - .06f * canopyIn)
                repeat(7) { index ->
                    val cx = w * (.06f + index * .148f)
                    val treeHeight = h * (.20f + (index % 3) * .045f) * canopyIn
                    val spread = w * (.10f + (index % 2) * .025f)
                    val crown = Path().apply {
                        moveTo(cx - spread, canopyBase)
                        cubicTo(
                            cx - spread * .85f, canopyBase - treeHeight * .70f,
                            cx - spread * .35f, canopyBase - treeHeight,
                            cx, canopyBase - treeHeight * .96f,
                        )
                        cubicTo(
                            cx + spread * .35f, canopyBase - treeHeight,
                            cx + spread * .85f, canopyBase - treeHeight * .70f,
                            cx + spread, canopyBase,
                        )
                        close()
                    }
                    drawPath(crown, SageCanopy.copy(alpha = (.62f + (index % 3) * .12f) * canopyIn))
                }
            }

            // 5) 步道显影：Trail 语义动词的开场亮相，与 A 任务的路线显影同源。
            if (trailIn > 0f) {
                val trail = Path().apply {
                    moveTo(w * .50f, h * .98f)
                    cubicTo(w * .40f, h * .86f, w * .62f, h * .76f, w * .54f, h * .68f)
                    cubicTo(w * .48f, h * .62f, w * .60f, h * .60f, w * .66f, horizon + h * .04f)
                }
                val measure = PathMeasure().apply { setPath(trail, false) }
                val visible = Path()
                measure.getSegment(0f, measure.length * trailIn, visible, true)
                drawPath(visible, SageSignalLime.copy(alpha = .28f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                drawPath(visible, SageSignalLime.copy(alpha = .80f), style = Stroke(4.dp.toPx(), cap = StrokeCap.Round))

                // 显影头：让「正在走进这片公园」这件事有一个可追踪的位置。
                if (trailIn < .99f) {
                    val head = measure.getPosition(measure.length * trailIn)
                    drawCircle(Color.White.copy(alpha = .30f), 12.dp.toPx(), head)
                    drawCircle(Color.White, 4.dp.toPx(), head)
                }
            }

            // 6) 环境浮尘 / 落叶：唯一的持续循环，幅度极低，作为「场景仍在呼吸」的底噪。
            repeat(9) { index ->
                val phase = (drift + index * .11f) % 1f
                val x = w * ((.08f + index * .107f + phase * .05f) % 1f)
                val y = h * (.94f - phase * .62f)
                val alpha = (1f - phase) * .34f * canopyIn
                val radius = (1.6f + (index % 3) * .9f).dp.toPx()
                drawCircle(SageSignalCyan.copy(alpha = alpha), radius, Offset(x, y))
            }
        }

        // 原创银杏叶向导 IP：最后进入场景，作为 AI “在场”的具体形象。
        AnimatedMascot(
            mood = MascotMood.IDLE,
            contentDescription = "银小叶公园向导",
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp, bottom = 74.dp)
                .size(188.dp)
                .graphicsLayer {
                    alpha = orbIn
                    translationY = sin(drift * 2f * PI.toFloat()) * 7f
                    rotationZ = sin(drift * 2f * PI.toFloat()) * 1.2f
                },
        )

        // 顶部：地点与参与者标识，最先落位，先给出「你在哪里」。
        Column(
            Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(horizontal = 26.dp, vertical = 18.dp)
                .graphicsLayer {
                    alpha = skyIn
                    translationY = (1f - skyIn) * -24f
                },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(SageSignalLime, CircleShape))
                Text(
                    "北京 · 海淀东升 · 八家郊野公园南园",
                    color = SageInk,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            if (participantId.isNotBlank()) {
                Text(
                    "参与者 $participantId",
                    color = SageMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 4.dp, start = 14.dp),
                )
            }
        }

        // 右上角跳过：任何时候都能离开，不阻断实验节奏。
        TextButton(
            onClick = onEnter,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            Text("跳过", color = SageSignalLime, fontSize = 13.sp)
        }

        // 底部：标题与进入按钮，最后落位。
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(horizontal = 26.dp, vertical = 30.dp),
        ) {
            Text(
                "今天，让 AI\n陪你慢慢逛",
                color = Color.White,
                fontSize = 34.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.graphicsLayer {
                    alpha = titleIn
                    translationY = (1f - titleIn) * 40f
                },
            )
            Text(
                "戏曲主题的林地、生态湖区与康体步道。\n不赶路，也不必盯着屏幕。",
                color = Color.White.copy(alpha = .82f),
                fontSize = 14.sp,
                lineHeight = 22.sp,
                modifier = Modifier
                    .padding(top = 12.dp)
                    .graphicsLayer {
                        alpha = titleIn
                        translationY = (1f - titleIn) * 28f
                    },
            )

            // AI 在场：呼吸球最后出现，把「场景」交接给「助手」。
            Row(
                Modifier
                    .padding(top = 22.dp)
                    .graphicsLayer { alpha = orbIn },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(8.dp).background(Color(0xFFB8D96D), CircleShape))
                Column(Modifier.padding(start = 12.dp)) {
                    Text("银小叶已经在你身边", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    Text("它会陪你看路线、找方向、记录发现", color = Color.White.copy(alpha = .68f), fontSize = 11.sp)
                }
            }

            Button(
                onClick = onEnter,
                shape = sageBubbleShape(1),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 22.dp)
                    .height(54.dp)
                    .graphicsLayer {
                        alpha = orbIn
                        translationY = (1f - orbIn) * 22f
                    },
            ) {
                Text("开始今天的探索", fontSize = 16.sp)
            }
        }
    }
}

/**
 * 欢迎页之后、进入 A 任务之前的场景说明卡。
 * 把公园的真实分区讲清楚，让参与者带着空间预期进入路线规划任务。
 */
@Composable
fun ParkBriefCard(modifier: Modifier = Modifier) {
    FrostedGlassSurface(
        tint = Color(0xFFEAF5D9),
        shape = sageBubbleShape(0),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("你现在在南园入口", color = SageInk, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "南园分为管理服务中心、康体健身步道、综合运动区、儿童活动区和生态湖区五个区。",
                color = SageMuted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 6.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                listOf("生态湖区", "康体步道", "戏曲林地").forEach { zone ->
                    Surface(color = SageMist, shape = RoundedCornerShape(9.dp), modifier = Modifier.weight(1f)) {
                        Text(
                            zone,
                            color = SageGreenDark,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        )
                    }
                }
            }
        }
    }
}

/** 欢迎页与主流程之间共享的强调色，避免在调用处重复引用主题常量。 */
internal val WelcomeAccent = SageGreen
