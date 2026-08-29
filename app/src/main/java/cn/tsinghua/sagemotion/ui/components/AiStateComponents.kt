package cn.tsinghua.sagemotion.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.tsinghua.sagemotion.ui.theme.SageGold
import cn.tsinghua.sagemotion.ui.theme.SageGreen
import cn.tsinghua.sagemotion.ui.theme.SageGreenDark
import cn.tsinghua.sagemotion.ui.theme.SageInk
import cn.tsinghua.sagemotion.ui.theme.SageMist
import cn.tsinghua.sagemotion.ui.theme.SageMotion
import cn.tsinghua.sagemotion.ui.theme.SageMuted
import cn.tsinghua.sagemotion.ui.theme.SageOchre
import cn.tsinghua.sagemotion.ui.theme.SageOnSignal
import cn.tsinghua.sagemotion.ui.theme.SagePanelRaised
import cn.tsinghua.sagemotion.ui.theme.SagePanelSoft
import cn.tsinghua.sagemotion.ui.theme.SageSignalCoral
import cn.tsinghua.sagemotion.ui.theme.SageSignalLime
import cn.tsinghua.sagemotion.ui.theme.SageWarningSurface
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * AI 状态组件库，对应 AI动效--DESIGN 第 8 节列出的四个组件
 * （AI floating button、listening bubble、reasoning ring、generating card），
 * 并补上两个针对 Study 1 参数缺口的新组件：完成印记与能力边界条。
 *
 * 所有时长与缓动都来自 [SageMotion]，不在组件内部另写魔法数字。
 */

/**
 * Reasoning ring —— 推理环。
 *
 * Study 1 中推理过程可见性平均仅 0.50/2：多数产品只表达「正在忙」，
 * 不表达「正在把哪些依据折进来」。这里用两条反向匀速弧表达持续推理，
 * 用外圈的证据刻度表达已纳入的依据条数，让过程可数而不是纯装饰。
 *
 * 匀速循环是刻意的：不能让用户从速度变化里读出错误的「快好了」暗示。
 */
@Composable
fun ReasoningRing(
    modifier: Modifier = Modifier,
    evidenceCount: Int = 0,
    evidenceTotal: Int = 5,
    accent: Color = SageSignalLime,
    uncertain: Boolean = false,
) {
    val transition = rememberInfiniteTransition(label = "reasoningRing")
    val spin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = SageMotion.loop(SageMotion.Duration.REASONING_MAX),
        label = "reasoningSpin",
    )
    val counterSpin by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = SageMotion.loop(SageMotion.Duration.REASONING_MIN + 400),
        label = "reasoningCounterSpin",
    )
    val settled by animateFloatAsState(
        targetValue = evidenceCount.coerceIn(0, evidenceTotal) / evidenceTotal.toFloat(),
        animationSpec = SageMotion.reveal(),
        label = "reasoningEvidence",
    )
    val ringColor = if (uncertain) SageOchre else accent

    Canvas(modifier.size(64.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outer = size.minDimension * .44f
        val inner = size.minDimension * .30f

        // 底环：可搜索空间先存在。
        drawCircle(ringColor.copy(alpha = .14f), outer, center, style = Stroke(2.dp.toPx()))

        // 外弧顺时针、内弧逆时针，形成持续但不进度化的推理感。
        drawArc(
            color = ringColor.copy(alpha = .85f),
            startAngle = spin * 360f,
            sweepAngle = 96f,
            useCenter = false,
            topLeft = center - Offset(outer, outer),
            size = Size(outer * 2f, outer * 2f),
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round),
        )
        drawArc(
            color = ringColor.copy(alpha = .45f),
            startAngle = -counterSpin * 360f + 140f,
            sweepAngle = 62f,
            useCenter = false,
            topLeft = center - Offset(inner, inner),
            size = Size(inner * 2f, inner * 2f),
            style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
        )

        // 证据刻度：每纳入一条依据点亮一格，让「推理到哪一步」可数。
        repeat(evidenceTotal) { index ->
            val angle = (index / evidenceTotal.toFloat()) * 2f * PI.toFloat() - PI.toFloat() / 2f
            val lit = index < (settled * evidenceTotal)
            val radius = outer + 6.dp.toPx()
            val point = Offset(center.x + cos(angle) * radius, center.y + sin(angle) * radius)
            drawCircle(
                color = if (lit) ringColor else ringColor.copy(alpha = .20f),
                radius = if (lit) 3.dp.toPx() else 2.dp.toPx(),
                center = point,
            )
        }

        // 不确定时环不闭合，用虚线表达「没有落定」。
        if (uncertain) {
            drawCircle(
                SageOchre.copy(alpha = .55f),
                inner * .55f,
                center,
                style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))),
            )
        } else {
            drawCircle(ringColor.copy(alpha = .22f), inner * .38f, center)
        }
    }
}

/**
 * Listening bubble —— 聆听球。
 *
 * 幅度由真实麦克风 RMS 驱动，而不是播放固定循环：运动必须响应真实输入，
 * 否则用户无法据此判断「AI 到底有没有听到我」。
 * 刻意不缩放外层布局尺寸，避免界面抖动（见 DESIGN_SPEC 动效研究约束）。
 */
@Composable
fun ListeningBubble(
    inputLevel: Float,
    modifier: Modifier = Modifier,
    active: Boolean = true,
    accent: Color = SageSignalLime,
) {
    val transition = rememberInfiniteTransition(label = "listeningBubble")
    val ripple by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = SageMotion.loop(SageMotion.Duration.REASONING_MIN + 600),
        label = "listeningRipple",
    )
    val breathe by transition.animateFloat(
        initialValue = .35f,
        targetValue = 1f,
        animationSpec = SageMotion.breathe(),
        label = "listeningBreathe",
    )
    val level by animateFloatAsState(
        targetValue = inputLevel.coerceIn(0f, 1f),
        animationSpec = SageMotion.fast(),
        label = "listeningLevel",
    )

    Canvas(modifier.size(96.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val base = size.minDimension * .26f

        // 同心低对比波纹：表达「感知范围」而不是「加载进度」。
        repeat(3) { index ->
            val local = (ripple + index / 3f) % 1f
            drawCircle(
                color = accent.copy(alpha = (1f - local) * if (active) .26f else .10f),
                radius = base + local * size.minDimension * .30f + level * 8.dp.toPx(),
                center = center,
                style = Stroke((1.5f + (1f - local)).dp.toPx()),
            )
        }
        drawCircle(accent.copy(alpha = .12f + breathe * .10f), base, center)

        if (!active) {
            drawCircle(accent, 5.dp.toPx(), center)
            return@Canvas
        }

        // 非匀速声波条：中间高两侧低，幅度直接由音量驱动。
        val gap = 7.dp.toPx()
        repeat(7) { index ->
            val distance = abs(index - 3)
            val wave = abs(sin((ripple * 2f * PI + index * .7f).toFloat()))
            val barHeight = (7.dp.toPx() + wave * (13f - distance * 1.6f).dp.toPx() + level * (20f - distance * 2.4f).dp.toPx())
            val x = center.x + (index - 3) * gap
            drawLine(
                color = accent.copy(alpha = .55f + breathe * .35f),
                start = Offset(x, center.y - barHeight / 2f),
                end = Offset(x, center.y + barHeight / 2f),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * Generating card —— 生成卡片。
 *
 * Study 1 中「流式输出」只出现 6 次（7.5%），多数流程仍是「等待—整体出现」。
 * 这里让骨架行逐条被写入并带一道行内高光，表达内容正在成形，
 * 而不是用一个转圈遮住全部过程。
 */
@Composable
fun GeneratingCard(
    title: String,
    lineCount: Int = 3,
    modifier: Modifier = Modifier,
    accent: Color = SageSignalLime,
) {
    val transition = rememberInfiniteTransition(label = "generatingCard")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = SageMotion.loop(SageMotion.Duration.REASONING),
        label = "generatingSweep",
    )
    Surface(
        color = SagePanelRaised.copy(alpha = .96f),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 6.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(7.dp).background(accent, CircleShape))
                Text(
                    title,
                    color = SageInk,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            repeat(lineCount) { index ->
                // 每行有各自的起写时间，形成「逐条写入」而不是整块闪现。
                val lineStart = index / (lineCount + 1f)
                val written = ((sweep - lineStart) * (lineCount + 1f)).coerceIn(0f, 1f)
                val widthFraction = listOf(1f, .86f, .64f, .78f)[index % 4]
                Canvas(
                    Modifier
                        .fillMaxWidth(widthFraction)
                        .padding(top = 9.dp)
                        .height(8.dp),
                ) {
                    drawRoundRect(
                        color = accent.copy(alpha = .10f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                    )
                    if (written > 0f) {
                        drawRoundRect(
                            color = accent.copy(alpha = .42f),
                            size = Size(size.width * written, size.height),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()),
                        )
                        // 行内高光跟在写入头后面，指明当前正在生成的位置。
                        if (written < 1f) {
                            drawCircle(
                                color = accent,
                                radius = 3.dp.toPx(),
                                center = Offset(size.width * written, size.height / 2f),
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * AI floating badge —— 常驻 AI 存在感标记。
 *
 * Study 1 显示运动方向以「自下而上」为主（32.5%），底部浮层是 AI 进入界面的主要语法。
 * 这个标记保持低打扰的边缘存在感，只在状态切换时做一次短位移。
 */
@Composable
fun AiFloatingBadge(
    label: String,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    accent: Color = SageSignalLime,
) {
    val transition = rememberInfiniteTransition(label = "aiBadge")
    val glow by transition.animateFloat(
        initialValue = .28f,
        targetValue = .72f,
        animationSpec = SageMotion.breathe(SageMotion.Duration.REASONING),
        label = "aiBadgeGlow",
    )
    Surface(
        color = SagePanelRaised.copy(alpha = .96f),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Canvas(Modifier.size(10.dp)) {
                if (active) {
                    drawCircle(accent.copy(alpha = glow * .5f), size.minDimension * .5f)
                }
                drawCircle(SageSignalLime, size.minDimension * .28f)
            }
            Text(
                label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}

/**
 * Completion seal —— 明确的完成印记。
 *
 * Study 1 里只有 28.8% 的片段给出明确完成提示，小米样本更低到 20.7%：
 * 多数流程靠「加载动画停了」暗示结束，用户还得自己判断 AI 是否真的做完了。
 * 这里让对勾自己描出来并收一次低回弹，把完成变成一个可被察觉的独立事件，
 * 而不是「什么都没有了」。一次性播放，绝不循环。
 */
@Composable
fun CompletionSeal(
    visible: Boolean,
    label: String,
    modifier: Modifier = Modifier,
    accent: Color = SageSignalLime,
) {
    var played by remember { mutableStateOf(false) }
    LaunchedEffect(visible) { if (visible) played = true }
    val draw by animateFloatAsState(
        targetValue = if (visible && played) 1f else 0f,
        animationSpec = SageMotion.completion(),
        label = "completionDraw",
    )
    val settle by animateFloatAsState(
        targetValue = if (visible && played) 1f else .82f,
        animationSpec = SageMotion.reboundMedium(),
        label = "completionSettle",
    )
    if (draw <= 0f) return

    Row(
        modifier = modifier.graphicsLayer { alpha = draw },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Canvas(
            Modifier
                .size(24.dp)
                .graphicsLayer { scaleX = settle; scaleY = settle },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(accent.copy(alpha = .16f), size.minDimension * .5f, center)

            // 对勾按路径长度逐段描出：完成本身是一个被「画出来」的动作。
            val check = Path().apply {
                moveTo(size.width * .26f, size.height * .52f)
                lineTo(size.width * .44f, size.height * .70f)
                lineTo(size.width * .76f, size.height * .32f)
            }
            val measure = PathMeasure().apply { setPath(check, false) }
            val drawn = Path()
            measure.getSegment(0f, measure.length * draw, drawn, true)
            drawPath(drawn, accent, style = Stroke(2.6.dp.toPx(), cap = StrokeCap.Round))
        }
        Text(
            label,
            color = accent,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

/**
 * Model boundary chip —— 能力边界条。
 *
 * 这是 Study 1 里最大的结构性缺口：输入边界可见性 1.49/2，模型边界可见性只有 0.50/2。
 * 用户能看出 AI 在处理哪张图、哪段话，却看不出 AI 能做什么、不能做什么、可能怎么错。
 * 这个组件把「不能做的那一半」明写出来，只在 SAGE Full 条件下出现。
 */
@Composable
fun ModelBoundaryChip(
    canDo: String,
    cannotDo: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = SageWarningSurface,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(13.dp)) {
                    // 半实半虚的环：一半能力已知，一半留白。形状本身就说明了边界，
                    // 不依赖颜色单独承载语义。
                    drawArc(
                        color = SageOchre,
                        startAngle = -90f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(2.dp.toPx(), cap = StrokeCap.Round),
                    )
                    drawArc(
                        color = SageOchre.copy(alpha = .45f),
                        startAngle = 90f,
                        sweepAngle = 180f,
                        useCenter = false,
                        style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))),
                    )
                }
                Text(
                    "能力边界",
                    color = SageSignalCoral,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 7.dp),
                )
            }
            BoundaryLine("可以判断", canDo, SageSignalLime, filled = true)
            BoundaryLine("无法判断", cannotDo, SageSignalCoral, filled = false)
        }
    }
}

@Composable
private fun BoundaryLine(prefix: String, text: String, accent: Color, filled: Boolean) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Canvas(Modifier.padding(top = 4.dp).size(7.dp)) {
            if (filled) {
                drawCircle(accent)
            } else {
                drawCircle(accent, style = Stroke(1.4.dp.toPx()))
            }
        }
        Text(
            prefix,
            color = accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 6.dp).width(52.dp),
        )
        Text(
            text,
            color = SageInk,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * 阶段进度指示条。把 AI 阶段名称、序号与完成状态同时给出，
 * 因为 Study 1 显示 83.8% 的样本依赖文字辅助，纯抽象动画不足以传达状态。
 */
@Composable
fun StageChips(
    labels: List<String>,
    activeIndex: Int,
    modifier: Modifier = Modifier,
    uncertain: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        labels.forEachIndexed { index, label ->
            val done = index < activeIndex
            val current = index == activeIndex
            val weight by animateFloatAsState(
                targetValue = if (current) 1.35f else 1f,
                animationSpec = SageMotion.standard(),
                label = "stageChipWeight$index",
            )
            Surface(
                color = when {
                    current && uncertain -> SageWarningSurface
                    current -> SageMist
                    done -> SageMist.copy(alpha = .55f)
                    else -> SagePanelSoft
                },
                shape = RoundedCornerShape(9.dp),
                modifier = Modifier.weight(weight),
            ) {
                Row(
                    Modifier.padding(horizontal = 7.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Canvas(Modifier.size(6.dp)) {
                        when {
                            done -> drawCircle(SageSignalLime)
                            current && uncertain -> drawCircle(SageSignalCoral)
                            current -> drawCircle(SageSignalLime, style = Stroke(1.4.dp.toPx()))
                            else -> drawCircle(SageMuted.copy(alpha = .35f), style = Stroke(1.dp.toPx()))
                        }
                    }
                    Spacer(Modifier.width(5.dp))
                    Text(
                        label,
                        fontSize = 10.sp,
                        maxLines = 1,
                        color = when {
                            current && uncertain -> SageSignalCoral
                            current || done -> SageSignalLime
                            else -> SageMuted
                        },
                        fontWeight = if (current) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/** 手账拼贴里用到的暖金强调点，导出给拼贴组件复用。 */
internal val ScrapbookAccent = SageGold
