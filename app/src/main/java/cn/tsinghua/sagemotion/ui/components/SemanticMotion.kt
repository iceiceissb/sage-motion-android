package cn.tsinghua.sagemotion.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ExperimentCondition
import cn.tsinghua.sagemotion.ui.theme.SageGreen
import cn.tsinghua.sagemotion.ui.theme.SageGreenDark
import cn.tsinghua.sagemotion.ui.theme.SageMist
import cn.tsinghua.sagemotion.ui.theme.SageOchre
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun RouteSemanticMotion(
    stage: AiStage,
    condition: ExperimentCondition,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "routeSemantic")
    val travel by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1900), RepeatMode.Restart), label = "travel")
    val breathe by transition.animateFloat(.25f, 1f, infiniteRepeatable(tween(1050), RepeatMode.Reverse), label = "breathe")
    val mainReveal by animateFloatAsState(
        targetValue = when (stage) {
            AiStage.ACTIVATING -> .06f
            AiStage.LOCATING -> .43f
            AiStage.REASONING -> .88f
            AiStage.UNCERTAIN, AiStage.COMPLETE -> 1f
            else -> 0f
        },
        animationSpec = tween(1080),
        label = "mainRouteReveal",
    )
    val alternativeReveal by animateFloatAsState(
        targetValue = when (stage) {
            AiStage.ACTIVATING -> .03f
            AiStage.LOCATING -> .29f
            AiStage.REASONING -> .94f
            AiStage.UNCERTAIN, AiStage.COMPLETE -> 1f
            else -> 0f
        },
        animationSpec = tween(1260),
        label = "alternativeRouteReveal",
    )

    Canvas(modifier.fillMaxSize()) {
        val main = routePath(size)
        val alternative = alternativeRoutePath(size)
        val semantic = condition != ExperimentCondition.BASELINE
        val full = condition == ExperimentCondition.SAGE_FULL

        val mainMeasure = PathMeasure().apply { setPath(main, false) }
        val alternativeMeasure = PathMeasure().apply { setPath(alternative, false) }
        val visibleMain = Path()
        val visibleAlternative = Path()
        mainMeasure.getSegment(0f, mainMeasure.length * mainReveal.coerceIn(0f, 1f), visibleMain, true)
        alternativeMeasure.getSegment(0f, alternativeMeasure.length * alternativeReveal.coerceIn(0f, 1f), visibleAlternative, true)

        // 两条低对比“可搜索空间”先存在，随后才由计算过程逐段点亮。
        drawPath(main, Color.White.copy(alpha = .54f), style = Stroke(12.dp.toPx(), cap = StrokeCap.Round))
        drawPath(alternative, Color.White.copy(alpha = .42f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
        drawPath(main, SageGreenDark.copy(alpha = .10f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        drawPath(alternative, SageOchre.copy(alpha = .09f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

        drawPath(visibleAlternative, Color.White.copy(alpha = .86f), style = Stroke(9.dp.toPx(), cap = StrokeCap.Round))
        drawPath(
            visibleAlternative,
            SageOchre.copy(alpha = if (stage == AiStage.COMPLETE || stage == AiStage.UNCERTAIN) .68f else .50f),
            style = Stroke(
                4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (semantic && stage == AiStage.REASONING) PathEffect.dashPathEffect(floatArrayOf(17f, 11f), -travel * 64f) else null,
            ),
        )
        drawPath(visibleMain, Color.White.copy(alpha = .94f), style = Stroke(11.dp.toPx(), cap = StrokeCap.Round))
        drawPath(
            visibleMain,
            SageGreen,
            style = Stroke(
                6.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (semantic && stage == AiStage.REASONING) PathEffect.dashPathEffect(floatArrayOf(26f, 12f), -travel * 82f) else null,
            ),
        )

        val start = Offset(size.width * .13f, size.height * .57f)
        val destination = Offset(size.width * .84f, size.height * .25f)
        val branch = Offset(size.width * .22f, size.height * .53f)
        if (stage == AiStage.LOCATING || stage == AiStage.ACTIVATING) {
            repeat(3) { index ->
                val local = (travel + index / 3f) % 1f
                drawCircle(SageGreen.copy(alpha = (1f - local) * .35f), (12f + local * 42f).dp.toPx(), start, style = Stroke(2.dp.toPx()))
            }
        }

        // 显影头让用户看见“正在算到哪里”，而不是路线突然完成。
        if (mainReveal in .02f..0.995f) {
            val head = mainMeasure.getPosition(mainMeasure.length * mainReveal)
            drawCircle(SageGreen.copy(alpha = .15f + breathe * .16f), (15f + breathe * 5f).dp.toPx(), head)
            drawCircle(Color.White, 9.dp.toPx(), head)
            drawCircle(SageGreen, 5.dp.toPx(), head)
        }
        if (alternativeReveal in .02f..0.995f) {
            val head = alternativeMeasure.getPosition(alternativeMeasure.length * alternativeReveal)
            drawCircle(SageOchre.copy(alpha = .12f + breathe * .14f), (13f + breathe * 4f).dp.toPx(), head)
            drawCircle(Color.White, 8.dp.toPx(), head)
            drawCircle(SageOchre, 4.dp.toPx(), head)
        }

        if (stage == AiStage.REASONING && semantic) {
            val mainProbe = mainMeasure.getPosition(mainMeasure.length * travel)
            val alternativeProbe = alternativeMeasure.getPosition(alternativeMeasure.length * ((travel + .38f) % 1f))
            drawCircle(Color.White.copy(alpha = .82f), 11.dp.toPx(), mainProbe)
            drawCircle(SageGreen, 5.dp.toPx(), mainProbe)
            drawCircle(Color.White.copy(alpha = .75f), 9.dp.toPx(), alternativeProbe)
            drawCircle(SageOchre, 4.dp.toPx(), alternativeProbe)
            val sources = listOf(
                Offset(size.width * .30f, size.height * .30f),
                Offset(size.width * .58f, size.height * .49f),
                Offset(size.width * .72f, size.height * .18f),
                Offset(size.width * .40f, size.height * .66f),
                Offset(size.width * .78f, size.height * .54f),
            )
            sources.forEachIndexed { index, source ->
                val phase = ((travel * 1.4f + index * .17f) % 1f)
                val targetMeasure = if (index % 2 == 0) mainMeasure else alternativeMeasure
                val target = targetMeasure.getPosition(targetMeasure.length * (.18f + index * .16f).coerceAtMost(.92f))
                val point = source + (target - source) * phase
                drawCircle(SageMist.copy(alpha = 1f - phase * .45f), 4.dp.toPx(), point)
                drawLine(SageGreenDark.copy(alpha = .06f), source, target, 1.dp.toPx())
            }
        }

        if (semantic && stage == AiStage.REASONING) {
            drawCircle(Color.White.copy(alpha = .85f), 8.dp.toPx(), branch)
            drawCircle(SageGreenDark.copy(alpha = .70f), 4.dp.toPx(), branch)
            drawCircle(SageGreenDark.copy(alpha = .12f + breathe * .10f), (13f + breathe * 7f).dp.toPx(), branch, style = Stroke(1.5.dp.toPx()))
        }

        if (full && (stage == AiStage.UNCERTAIN || stage == AiStage.COMPLETE)) {
            val gap = alternativeMeasure.getPosition(alternativeMeasure.length * .62f)
            drawCircle(SageOchre.copy(alpha = .20f + breathe * .18f), (17f + breathe * 8f).dp.toPx(), gap, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 7f))))
        }

        drawCircle(Color.White, 11.dp.toPx(), start)
        drawCircle(SageGreen, 6.dp.toPx(), start)
        if (mainReveal > .94f || alternativeReveal > .94f) {
            drawCircle(Color.White.copy(alpha = .90f), (12f + breathe * 3f).dp.toPx(), destination)
            drawCircle(if (stage == AiStage.UNCERTAIN && full) SageOchre else SageGreen, 8.dp.toPx(), destination)
        }
    }
}

@Composable
fun MemoryWeaveMotion(
    stage: AiStage,
    condition: ExperimentCondition,
    modifier: Modifier = Modifier,
    photoCount: Int = 0,
    voiceCount: Int = 0,
    replanCount: Int = 0,
) {
    val transition = rememberInfiniteTransition(label = "memoryWeave")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1700), RepeatMode.Restart), label = "pulse")
    val breathe by transition.animateFloat(.35f, 1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "breathe")
    val weave by animateFloatAsState(
        targetValue = when (stage) {
            AiStage.ACTIVATING -> .10f
            AiStage.SUMMARIZING -> .38f
            AiStage.GENERATING -> .68f
            AiStage.EDITING -> .88f
            AiStage.COMPLETE -> 1f
            else -> 0f
        },
        animationSpec = tween(1050),
        label = "weaveProgress",
    )
    Canvas(modifier.fillMaxSize()) {
        val semantic = condition != ExperimentCondition.BASELINE
        val full = condition == ExperimentCondition.SAGE_FULL
        val center = Offset(size.width * .50f, size.height * .47f)
        val sources = listOf(
            Offset(size.width * .15f, size.height * .30f),
            Offset(size.width * .84f, size.height * .27f),
            Offset(size.width * .12f, size.height * .64f),
            Offset(size.width * .86f, size.height * .66f),
            Offset(size.width * .50f, size.height * .18f),
        )
        val destinations = listOf(
            Offset(size.width * .31f, size.height * .39f),
            Offset(size.width * .69f, size.height * .39f),
            Offset(size.width * .31f, size.height * .59f),
            Offset(size.width * .69f, size.height * .59f),
            Offset(size.width * .50f, size.height * .49f),
        )
        if (!semantic) {
            drawCircle(SageGreen.copy(alpha = .10f + breathe * .10f), (52f + breathe * 10f).dp.toPx(), center)
            return@Canvas
        }

        sources.forEachIndexed { index, source ->
            val gather = (weave * 2.15f - index * .12f).coerceIn(0f, 1f)
            val gathered = source + (center - source) * gather
            val arrange = ((weave - .42f) * 2.2f).coerceIn(0f, 1f)
            val point = gathered + (destinations[index] - center) * arrange
            drawLine(
                SageGreen.copy(alpha = .12f + gather * .28f),
                source,
                point,
                1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 9f), -pulse * 22f),
            )
            drawCircle(Color.White.copy(alpha = .92f), 12.dp.toPx(), point)
            drawCircle(if (index == 4) SageOchre else SageGreen, (4f + breathe * 1.5f).dp.toPx(), point)
        }

        // 素材卫星数量来自真实旅程数据，让生成动效不再只是固定装饰。
        listOf(photoCount.coerceAtMost(4), voiceCount.coerceAtMost(3), replanCount.coerceAtMost(3)).forEachIndexed { group, count ->
            val anchor = sources[listOf(0, 1, 3)[group]]
            repeat(count) { index ->
                val phase = ((pulse + index * .23f + group * .11f) % 1f)
                val orbitAngle = (index / count.coerceAtLeast(1).toFloat() + pulse * .18f) * 2f * PI.toFloat()
                val orbitRadius = (16f + group * 4f).dp.toPx()
                val origin = Offset(anchor.x + kotlin.math.cos(orbitAngle) * orbitRadius, anchor.y + sin(orbitAngle) * orbitRadius)
                val transfer = if (weave > .20f) ((weave - .20f) * 1.35f).coerceIn(0f, 1f) else 0f
                val point = origin + (center - origin) * (transfer * (.58f + phase * .42f))
                val color = listOf(SageGreen, SageMist, SageOchre)[group]
                drawCircle(color.copy(alpha = .28f + (1f - phase) * .52f), (2.5f + (1f - phase) * 2f).dp.toPx(), point)
            }
        }

        if (weave > .42f) {
            val top = size.height * .34f
            val bottom = size.height * .64f
            drawLine(SageGreen.copy(alpha = .45f), Offset(center.x, top), Offset(center.x, top + (bottom - top) * ((weave - .42f) / .58f).coerceIn(0f, 1f)), 3.dp.toPx(), StrokeCap.Round)
            repeat(3) { index ->
                val y = top + index * (bottom - top) / 2f
                val appear = ((weave - .48f - index * .10f) * 4f).coerceIn(0f, 1f)
                drawCircle(Color.White.copy(alpha = appear), 9.dp.toPx(), Offset(center.x, y))
                drawCircle(SageGreen.copy(alpha = appear), 4.dp.toPx(), Offset(center.x, y))
                drawLine(SageGreen.copy(alpha = appear * .40f), Offset(center.x + 12.dp.toPx(), y), Offset(center.x + (36f + index * 18f).dp.toPx(), y), 2.dp.toPx(), StrokeCap.Round)
            }
        }

        if (stage == AiStage.EDITING || stage == AiStage.COMPLETE) {
            val cursorX = size.width * (.30f + pulse * .40f)
            drawLine(SageOchre.copy(alpha = .35f + breathe * .45f), Offset(cursorX, size.height * .70f), Offset(cursorX, size.height * .74f), 2.dp.toPx())
            drawCircle(SageGreen.copy(alpha = .10f + breathe * .12f), (34f + breathe * 10f).dp.toPx(), center, style = Stroke(1.5.dp.toPx()))
        }
        if (full && stage == AiStage.EDITING) {
            drawCircle(SageOchre.copy(alpha = .26f + breathe * .25f), 42.dp.toPx(), center, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f, 9f))))
        }
    }
}

@Composable
fun ExplorationAmbientMotion(
    photoCount: Int,
    voiceCount: Int,
    replanCount: Int,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "explorationAmbient")
    val travel by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(5200), RepeatMode.Restart), label = "slowTraveler")
    val pulse by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1700), RepeatMode.Restart), label = "memoryPulse")
    Canvas(modifier.fillMaxSize()) {
        val path = routePath(size)
        val measure = PathMeasure().apply { setPath(path, false) }
        val point = measure.getPosition(measure.length * travel)
        val tail = Path()
        measure.getSegment(measure.length * (travel - .10f).coerceAtLeast(0f), measure.length * travel, tail, true)
        drawPath(tail, SageMist.copy(alpha = .42f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(Color.White.copy(alpha = .78f), (10f + (1f - pulse) * 5f).dp.toPx(), point)
        drawCircle(SageGreenDark, 5.dp.toPx(), point)

        listOf(photoCount, voiceCount, replanCount).forEachIndexed { index, count ->
            if (count <= 0) return@forEachIndexed
            val anchor = measure.getPosition(measure.length * listOf(.30f, .56f, .78f)[index])
            val color = listOf(SageGreen, SageMist, SageOchre)[index]
            repeat(count.coerceAtMost(3)) { ring ->
                val local = ((pulse + ring * .24f) % 1f)
                drawCircle(color.copy(alpha = (1f - local) * .26f), (10f + local * (24f + ring * 4f)).dp.toPx(), anchor, style = Stroke(1.5.dp.toPx()))
            }
            drawCircle(Color.White.copy(alpha = .86f), 8.dp.toPx(), anchor)
            drawCircle(color, 4.dp.toPx(), anchor)
        }
    }
}

@Composable
fun AdaptiveRouteMotion(
    stage: AiStage,
    condition: ExperimentCondition,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "adaptiveRoute")
    val travel by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2100), RepeatMode.Restart), label = "travel")
    val pulse by transition.animateFloat(.25f, 1f, infiniteRepeatable(tween(950), RepeatMode.Reverse), label = "pulse")
    val morph by animateFloatAsState(
        targetValue = when (stage) {
            AiStage.ACTIVATING, AiStage.LOCATING -> 0f
            AiStage.REPLANNING -> .62f
            AiStage.DECIDING, AiStage.UNCERTAIN, AiStage.COMPLETE -> 1f
            else -> 0f
        },
        animationSpec = tween(1350),
        label = "routeMorph",
    )
    val newReveal by animateFloatAsState(
        targetValue = when (stage) {
            AiStage.REPLANNING -> .70f
            AiStage.DECIDING, AiStage.UNCERTAIN, AiStage.COMPLETE -> 1f
            else -> 0f
        },
        animationSpec = tween(1450),
        label = "newRouteReveal",
    )
    Canvas(modifier.fillMaxSize()) {
        val semantic = condition != ExperimentCondition.BASELINE
        val full = condition == ExperimentCondition.SAGE_FULL
        val oldPath = routePath(size)
        val newPath = Path().apply {
            moveTo(size.width * .13f, size.height * .57f)
            cubicTo(size.width * .26f, size.height * .50f, size.width * .35f, size.height * (.40f + .20f * morph), size.width * .50f, size.height * (.38f + .23f * morph))
            cubicTo(size.width * .67f, size.height * (.36f + .20f * morph), size.width * .75f, size.height * .34f, size.width * .84f, size.height * .25f)
        }
        drawPath(oldPath, Color.White.copy(alpha = .86f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
        drawPath(
            oldPath,
            SageOchre.copy(alpha = if (morph > .1f) .46f * (1f - morph * .65f) else .58f),
            style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, pathEffect = if (semantic) PathEffect.dashPathEffect(floatArrayOf(12f, 11f), -travel * 28f) else null),
        )
        if (morph > .05f) {
            val measure = PathMeasure().apply { setPath(newPath, false) }
            val visibleNew = Path()
            measure.getSegment(0f, measure.length * newReveal.coerceIn(0f, 1f), visibleNew, true)
            drawPath(visibleNew, Color.White.copy(alpha = .92f * morph), style = Stroke(11.dp.toPx(), cap = StrokeCap.Round))
            drawPath(visibleNew, SageGreen.copy(alpha = morph), style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
            if (newReveal in .03f..0.995f) {
                val head = measure.getPosition(measure.length * newReveal)
                drawCircle(SageGreen.copy(alpha = .16f + pulse * .14f), (15f + pulse * 7f).dp.toPx(), head)
                drawCircle(Color.White.copy(alpha = morph), 10.dp.toPx(), head)
                drawCircle(SageGreen.copy(alpha = morph), 5.dp.toPx(), head)
            }
            if (semantic && newReveal > .45f) {
                val point = measure.getPosition(measure.length * travel * newReveal)
                drawCircle(Color.White.copy(alpha = morph), 10.dp.toPx(), point)
                drawCircle(SageGreen.copy(alpha = morph), 4.5.dp.toPx(), point)
            }
        }
        val obstacle = Offset(size.width * .50f, size.height * .39f)
        if (stage == AiStage.LOCATING || stage == AiStage.REPLANNING || stage == AiStage.DECIDING) {
            repeat(2) { index ->
                val radius = (18f + (pulse + index * .35f) * 22f).dp.toPx()
                drawCircle(SageOchre.copy(alpha = (1f - pulse) * .30f), radius, obstacle, style = Stroke(2.dp.toPx()))
            }
            drawLine(SageOchre, obstacle - Offset(7.dp.toPx(), 7.dp.toPx()), obstacle + Offset(7.dp.toPx(), 7.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
            drawLine(SageOchre, obstacle + Offset(-7.dp.toPx(), 7.dp.toPx()), obstacle + Offset(7.dp.toPx(), -7.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
            if (stage == AiStage.REPLANNING && semantic) {
                repeat(5) { index ->
                    val local = ((travel * 1.2f + index * .17f) % 1f)
                    val angle = (-.9f + index * .42f) * PI.toFloat()
                    val radius = (16f + local * 56f).dp.toPx()
                    val point = Offset(obstacle.x + kotlin.math.cos(angle) * radius, obstacle.y + sin(angle) * radius)
                    drawCircle(SageOchre.copy(alpha = (1f - local) * .38f), (4f - local * 2f).dp.toPx(), point)
                }
            }
        }
        if (full && stage == AiStage.UNCERTAIN) {
            drawPath(newPath, SageOchre.copy(alpha = .22f + pulse * .20f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f))))
        }
    }
}

@Composable
fun VisualSemanticMotion(
    stage: AiStage,
    condition: ExperimentCondition,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "visualSemantic")
    val scan by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1750), RepeatMode.Reverse), label = "scan")
    val orbit by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(2400), RepeatMode.Restart), label = "orbit")
    val breathe by transition.animateFloat(.35f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "breathe")
    val focusReveal by animateFloatAsState(
        targetValue = when (stage) {
            AiStage.ACTIVATING -> .12f
            AiStage.RECOGNIZING -> .52f
            AiStage.REASONING -> .82f
            AiStage.UNCERTAIN, AiStage.COMPLETE -> 1f
            else -> 0f
        },
        animationSpec = tween(920),
        label = "visionFocusReveal",
    )

    Canvas(modifier.fillMaxSize()) {
        val semantic = condition != ExperimentCondition.BASELINE
        val full = condition == ExperimentCondition.SAGE_FULL
        val left = size.width * .11f
        val top = size.height * .21f
        val width = size.width * .78f
        val height = size.height * .48f

        val corner = 30.dp.toPx()
        val frame = Path().apply {
            moveTo(left, top + corner); lineTo(left, top); lineTo(left + corner, top)
            moveTo(left + width - corner, top); lineTo(left + width, top); lineTo(left + width, top + corner)
            moveTo(left + width, top + height - corner); lineTo(left + width, top + height); lineTo(left + width - corner, top + height)
            moveTo(left + corner, top + height); lineTo(left, top + height); lineTo(left, top + height - corner)
        }
        drawPath(frame, Color.White.copy(alpha = .62f + breathe * .32f), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))

        val lensCenter = Offset(size.width / 2f, top + height * .50f)
        if (stage == AiStage.ACTIVATING) {
            repeat(4) { index ->
                val diameter = (72f + index * 17f + breathe * 9f).dp.toPx()
                drawArc(
                    color = Color.White.copy(alpha = .15f + (4 - index) * .07f),
                    startAngle = orbit * 360f + index * 54f,
                    sweepAngle = 42f + index * 5f,
                    useCenter = false,
                    topLeft = lensCenter - Offset(diameter / 2f, diameter / 2f),
                    size = Size(diameter, diameter),
                    style = Stroke((3f - index * .35f).dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }

        if (!semantic) {
            val y = top + height * scan
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = .23f), Color.Transparent),
                    startY = y - 34.dp.toPx(),
                    endY = y + 34.dp.toPx(),
                ),
                topLeft = Offset(left, y - 34.dp.toPx()),
                size = Size(width, 68.dp.toPx()),
            )
            drawCircle(Color.White.copy(alpha = .18f + breathe * .10f), (34f + breathe * 7f).dp.toPx(), lensCenter, style = Stroke(2.dp.toPx()))
            return@Canvas
        }

        if (stage == AiStage.RECOGNIZING || stage == AiStage.REASONING) {
            val y = top + height * scan
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(Color.Transparent, SageMist.copy(alpha = .12f), SageMist.copy(alpha = .68f), SageMist.copy(alpha = .12f), Color.Transparent),
                    startY = y - 46.dp.toPx(),
                    endY = y + 46.dp.toPx(),
                ),
                topLeft = Offset(left + 3.dp.toPx(), y - 46.dp.toPx()),
                size = Size(width - 6.dp.toPx(), 92.dp.toPx()),
            )
            drawLine(Color.White.copy(alpha = .9f), Offset(left + 10.dp.toPx(), y), Offset(left + width - 10.dp.toPx(), y), 2.dp.toPx())
        }

        val features = listOf(
            Offset(left + width * .34f, top + height * .33f),
            Offset(left + width * .62f, top + height * .30f),
            Offset(left + width * .48f, top + height * .57f),
            Offset(left + width * .28f, top + height * .66f),
            Offset(left + width * .70f, top + height * .64f),
        )
        val focusBoxes = listOf(
            Pair(Offset(left + width * .19f, top + height * .20f), Size(width * .27f, height * .24f)),
            Pair(Offset(left + width * .52f, top + height * .22f), Size(width * .24f, height * .20f)),
            Pair(Offset(left + width * .35f, top + height * .49f), Size(width * .34f, height * .23f)),
        )
        if (stage == AiStage.RECOGNIZING || stage == AiStage.REASONING || stage == AiStage.UNCERTAIN || stage == AiStage.COMPLETE) {
            focusBoxes.forEachIndexed { index, (origin, boxSize) ->
                val appear = (focusReveal * 2.1f - index * .30f).coerceIn(0f, 1f)
                val expanded = Size(boxSize.width * appear, boxSize.height * appear)
                drawRoundRect(
                    color = Color.White.copy(alpha = appear * .58f),
                    topLeft = origin + Offset((boxSize.width - expanded.width) / 2f, (boxSize.height - expanded.height) / 2f),
                    size = expanded,
                    cornerRadius = CornerRadius(10.dp.toPx()),
                    style = Stroke(1.4.dp.toPx(), pathEffect = if (stage == AiStage.REASONING) PathEffect.dashPathEffect(floatArrayOf(8f, 6f), -orbit * 24f) else null),
                )
                if (appear > .72f) {
                    val indicatorWidth = boxSize.width * (.34f + index * .13f)
                    drawLine(
                        SageMist.copy(alpha = appear * .88f),
                        origin + Offset(0f, boxSize.height + 7.dp.toPx()),
                        origin + Offset(indicatorWidth, boxSize.height + 7.dp.toPx()),
                        3.dp.toPx(),
                        StrokeCap.Round,
                    )
                }
            }
        }
        if (stage == AiStage.RECOGNIZING || stage == AiStage.REASONING || stage == AiStage.UNCERTAIN || stage == AiStage.COMPLETE) {
            features.forEachIndexed { index, point ->
                val pulse = .55f + .45f * sin((orbit + index * .18f) * 2f * PI.toFloat()).let(::abs)
                drawCircle(Color.White.copy(alpha = .30f * pulse), (10f + pulse * 5f).dp.toPx(), point)
                drawCircle(SageMist.copy(alpha = .75f + pulse * .25f), (3f + pulse * 2f).dp.toPx(), point)
            }
        }
        if (stage == AiStage.REASONING) {
            val hub = Offset(left + width * .51f, top + height * .47f)
            features.forEachIndexed { index, point ->
                val flow = ((orbit * 1.4f + index * .16f) % 1f)
                drawLine(SageMist.copy(alpha = .26f + breathe * .26f), point, hub, 1.5.dp.toPx())
                drawCircle(Color.White.copy(alpha = .35f + (1f - flow) * .45f), 3.dp.toPx(), point + (hub - point) * flow)
            }
            drawCircle(SageMist.copy(alpha = .22f), (20f + breathe * 8f).dp.toPx(), hub)
            drawCircle(Color.White, 5.dp.toPx(), hub)
        }
        if (full && stage == AiStage.UNCERTAIN) {
            val candidateA = Offset(left + width * .42f, top + height * .46f)
            val candidateB = Offset(left + width * .61f, top + height * .53f)
            drawCircle(SageOchre.copy(alpha = .35f + breathe * .35f), (34f + breathe * 5f).dp.toPx(), candidateA, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
            drawCircle(Color.White.copy(alpha = .35f + (1f - breathe) * .30f), (30f + (1f - breathe) * 5f).dp.toPx(), candidateB, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))))
        }
    }
}

@Composable
fun VoiceSemanticField(
    stage: AiStage,
    condition: ExperimentCondition,
    modifier: Modifier = Modifier,
    inputLevel: Float = 0f,
) {
    val transition = rememberInfiniteTransition(label = "voiceSemantic")
    val phase by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1500), RepeatMode.Restart), label = "phase")
    val breathe by transition.animateFloat(.30f, 1f, infiniteRepeatable(tween(920), RepeatMode.Reverse), label = "breathe")
    Canvas(modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height * .46f)
        val semantic = condition != ExperimentCondition.BASELINE
        val full = condition == ExperimentCondition.SAGE_FULL
        repeat(3) { index ->
            val local = (phase + index / 3f) % 1f
            drawCircle(
                SageGreen.copy(alpha = (1f - local) * if (semantic) .18f else .10f),
                (58f + local * 100f).dp.toPx(),
                center,
                style = Stroke((1.2f + (1f - local)).dp.toPx()),
            )
        }
        if (!semantic) return@Canvas

        if (stage == AiStage.LISTENING || stage == AiStage.ACTIVATING) {
            val barWidth = 4.dp.toPx()
            val gap = 8.dp.toPx()
            repeat(9) { index ->
                val distance = abs(index - 4)
                val wave = abs(sin((phase * 2f * PI + index * .62f).toFloat()))
                val height = (12f + wave * (30f - distance * 2f) + inputLevel * (26f - distance * 2f)).dp.toPx()
                val x = center.x + (index - 4) * gap
                drawLine(SageGreen.copy(alpha = .48f + breathe * .42f), Offset(x, center.y - height / 2f), Offset(x, center.y + height / 2f), barWidth, StrokeCap.Round)
            }
        }
        if (stage == AiStage.REASONING) {
            repeat(7) { index ->
                val angle = (index / 7f + phase) * 2f * PI.toFloat()
                val radius = (78f - phase * 38f + index % 2 * 12f).dp.toPx()
                val point = Offset(center.x + kotlin.math.cos(angle) * radius, center.y + sin(angle) * radius)
                drawLine(SageMist.copy(alpha = .38f), point, center, 1.dp.toPx())
                drawCircle(SageGreen.copy(alpha = .60f + breathe * .25f), (3f + (index % 3)).dp.toPx(), point)
            }
        }
        if (stage == AiStage.RESPONDING || stage == AiStage.COMPLETE) {
            repeat(4) { index ->
                val local = (phase + index * .22f) % 1f
                val y = center.y + 72.dp.toPx() - local * 150.dp.toPx()
                val half = (18f + local * 54f).dp.toPx()
                drawLine(SageGreen.copy(alpha = (1f - local) * .32f), Offset(center.x - half, y), Offset(center.x + half, y), 2.dp.toPx(), StrokeCap.Round)
            }
        }
        if (full && stage == AiStage.UNCERTAIN) {
            drawCircle(SageOchre.copy(alpha = .28f + breathe * .32f), (82f + breathe * 8f).dp.toPx(), center, style = Stroke(2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f))))
        }
    }
}

@Composable
fun BreathingVoiceOrb(active: Boolean, stage: AiStage, modifier: Modifier = Modifier, inputLevel: Float = 0f) {
    val transition = rememberInfiniteTransition(label = "voiceOrb")
    val pulse by transition.animateFloat(.94f, 1.08f, infiniteRepeatable(tween(if (stage == AiStage.LISTENING) 720 else 1100), RepeatMode.Reverse), label = "pulse")
    val halo by transition.animateFloat(.18f, .48f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "halo")
    Box(modifier.size(112.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(104.dp).scale(if (active) pulse + inputLevel * .10f else 1f).drawBehind {
                drawCircle(Brush.radialGradient(listOf(SageMist.copy(alpha = halo), Color.Transparent)))
            },
        )
        Canvas(Modifier.size(94.dp)) {
            drawCircle(SageMist.copy(alpha = .82f))
            drawCircle(SageGreenDark.copy(alpha = .18f), radius = size.minDimension * .38f, style = Stroke(2.dp.toPx()))
            drawCircle(SageGreen, radius = size.minDimension * .31f)
            if (active) {
                repeat(3) { index ->
                    val x = center.x + (index - 1) * 10.dp.toPx()
                    val bar = (8f + abs(sin((pulse + index) * PI.toFloat())) * 11f + inputLevel * (20f - index * 2f)).dp.toPx()
                    drawLine(Color.White, Offset(x, center.y - bar / 2f), Offset(x, center.y + bar / 2f), 3.dp.toPx(), StrokeCap.Round)
                }
            } else {
                drawCircle(Color.White, 5.dp.toPx(), center)
            }
        }
    }
}

private fun routePath(size: Size): Path = Path().apply {
    moveTo(size.width * .13f, size.height * .57f)
    cubicTo(size.width * .28f, size.height * .49f, size.width * .35f, size.height * .40f, size.width * .49f, size.height * .38f)
    cubicTo(size.width * .63f, size.height * .36f, size.width * .70f, size.height * .27f, size.width * .84f, size.height * .25f)
}

private fun alternativeRoutePath(size: Size): Path = Path().apply {
    moveTo(size.width * .13f, size.height * .57f)
    cubicTo(size.width * .29f, size.height * .62f, size.width * .54f, size.height * .59f, size.width * .84f, size.height * .25f)
}
