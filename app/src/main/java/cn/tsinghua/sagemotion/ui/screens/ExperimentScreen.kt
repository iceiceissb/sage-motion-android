package cn.tsinghua.sagemotion.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.tsinghua.sagemotion.R
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ExperimentCondition
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.ExperimentUiState
import cn.tsinghua.sagemotion.model.RouteChoice
import cn.tsinghua.sagemotion.ui.theme.SageDivider
import cn.tsinghua.sagemotion.ui.theme.SageGreen
import cn.tsinghua.sagemotion.ui.theme.SageGreenDark
import cn.tsinghua.sagemotion.ui.theme.SageInk
import cn.tsinghua.sagemotion.ui.theme.SageMist
import cn.tsinghua.sagemotion.ui.theme.SageMuted
import cn.tsinghua.sagemotion.ui.theme.SageOchre
import cn.tsinghua.sagemotion.ui.theme.SageWarningSurface

@Composable
fun ExperimentScreen(
    state: ExperimentUiState,
    onRunScenario: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onAdopt: () -> Unit,
    onEvidence: () -> Unit,
    onCloseEvidence: () -> Unit,
    onRouteSelected: (RouteChoice) -> Unit,
    onScenarioSelected: (ExperimentScenario) -> Unit,
    onConditionSelected: (Int) -> Unit,
    onNextCondition: () -> Unit,
    onResearcherPanel: (Boolean) -> Unit,
    onExport: () -> Unit,
) {
    when (state.scenario) {
        ExperimentScenario.ENVIRONMENT -> RouteExperiment(
            state = state,
            onRun = onRunScenario,
            onCancel = onCancel,
            onReset = onReset,
            onAdopt = onAdopt,
            onEvidence = onEvidence,
            onRouteSelected = onRouteSelected,
            onResearcherPanel = { onResearcherPanel(true) },
        )

        ExperimentScenario.VISUAL -> VisualExperiment(
            state = state,
            onRun = onRunScenario,
            onCancel = onCancel,
            onReset = onReset,
            onAdopt = onAdopt,
            onEvidence = onEvidence,
            onResearcherPanel = { onResearcherPanel(true) },
        )

        ExperimentScenario.VOICE -> VoiceExperiment(
            state = state,
            onRun = onRunScenario,
            onCancel = onCancel,
            onReset = onReset,
            onAdopt = onAdopt,
            onEvidence = onEvidence,
            onResearcherPanel = { onResearcherPanel(true) },
        )
    }

    if (state.evidenceVisible) {
        EvidenceDialog(scenario = state.scenario, onDismiss = onCloseEvidence)
    }
    if (state.researcherPanelVisible) {
        ResearcherPanel(
            state = state,
            onDismiss = { onResearcherPanel(false) },
            onScenarioSelected = onScenarioSelected,
            onConditionSelected = onConditionSelected,
            onRun = onRunScenario,
            onReset = onReset,
            onNextCondition = onNextCondition,
            onExport = onExport,
        )
    }
}

@Composable
private fun RouteExperiment(
    state: ExperimentUiState,
    onRun: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onAdopt: () -> Unit,
    onEvidence: () -> Unit,
    onRouteSelected: (RouteChoice) -> Unit,
    onResearcherPanel: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xFFE6E7E0))) {
        Image(
            painter = painterResource(R.drawable.park_map_background),
            contentDescription = "公园地图",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.20f)))

        if (state.isRunning || state.resultVisible) {
            RouteOverlay(condition = state.condition, isRunning = state.isRunning)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            AiStatusPanel(
                state = state,
                onLongPress = onResearcherPanel,
                onCancel = onCancel,
            )
            PromptBubble(text = state.scenario.participantPrompt)
        }

        if (!state.isRunning && !state.resultVisible) {
            StartCard(
                title = "想找一条舒服的路线吗？",
                actionLabel = state.scenario.actionLabel,
                onRun = onRun,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        AnimatedVisibility(
            visible = state.resultVisible,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            RouteResultPanel(
                condition = state.condition,
                choice = state.selectedRoute,
                onChoice = onRouteSelected,
                onAdopt = onAdopt,
                onEvidence = onEvidence,
                onReset = onReset,
            )
        }
    }
}

@Composable
private fun RouteOverlay(condition: ExperimentCondition, isRunning: Boolean) {
    val transition = rememberInfiniteTransition(label = "route")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 34f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Restart),
        label = "routePhase",
    )
    Canvas(Modifier.fillMaxSize()) {
        val main = Path().apply {
            moveTo(size.width * .13f, size.height * .57f)
            cubicTo(size.width * .28f, size.height * .49f, size.width * .35f, size.height * .40f, size.width * .49f, size.height * .38f)
            cubicTo(size.width * .63f, size.height * .36f, size.width * .70f, size.height * .27f, size.width * .84f, size.height * .25f)
        }
        val alternative = Path().apply {
            moveTo(size.width * .13f, size.height * .57f)
            cubicTo(size.width * .29f, size.height * .62f, size.width * .54f, size.height * .59f, size.width * .84f, size.height * .25f)
        }
        drawPath(main, Color.White.copy(alpha = .94f), style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
        drawPath(
            main,
            SageGreen,
            style = Stroke(
                width = 6.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = if (condition == ExperimentCondition.BASELINE || !isRunning) null else PathEffect.dashPathEffect(floatArrayOf(24f, 12f), -phase),
            ),
        )
        if (condition == ExperimentCondition.SAGE_FULL) {
            drawPath(
                alternative,
                Color.White.copy(alpha = .88f),
                style = Stroke(8.dp.toPx(), cap = StrokeCap.Round),
            )
            drawPath(
                alternative,
                Color(0xFF78817D),
                style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 12f))),
            )
        }
        drawCircle(Color.White, 10.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .13f, size.height * .57f))
        drawCircle(SageGreen, 6.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .13f, size.height * .57f))
        drawCircle(SageGreen, 9.dp.toPx(), center = androidx.compose.ui.geometry.Offset(size.width * .84f, size.height * .25f))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AiStatusPanel(
    state: ExperimentUiState,
    onLongPress: () -> Unit,
    onCancel: () -> Unit,
) {
    val semantic = state.condition != ExperimentCondition.BASELINE
    val full = state.condition == ExperimentCondition.SAGE_FULL
    Surface(
        color = Color.White.copy(alpha = .94f),
        shape = RoundedCornerShape(22.dp),
        tonalElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.5.dp,
                        color = if (full && state.aiStage == AiStage.UNCERTAIN) SageOchre else SageGreen,
                    )
                } else {
                    Icon(
                        imageVector = if (state.aiStage == AiStage.COMPLETE) Icons.Default.Check else Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = SageGreen,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = when {
                            state.isRunning && !semantic -> "正在处理…"
                            state.isRunning -> state.aiStage.label
                            state.resultVisible && semantic -> when (state.scenario) {
                                ExperimentScenario.ENVIRONMENT -> "已比较 2 条路线"
                                ExperimentScenario.VISUAL -> "识别与比较完成"
                                ExperimentScenario.VOICE -> "回答已生成"
                            }
                            state.aiStage == AiStage.COMPLETE -> state.statusMessage ?: "已完成"
                            else -> state.scenario.phaseLabel
                        },
                        fontWeight = FontWeight.SemiBold,
                        color = SageInk,
                        fontSize = 15.sp,
                    )
                    if (semantic && state.isRunning) {
                        Text(
                            text = stageDetail(state.aiStage, state.scenario),
                            color = SageMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (state.isRunning) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "取消", modifier = Modifier.size(19.dp))
                    }
                }
            }
            if (semantic && (state.isRunning || state.resultVisible)) {
                Spacer(Modifier.height(11.dp))
                SemanticSteps(stage = state.aiStage, scenario = state.scenario, showUncertainty = full)
            }
        }
    }
}

@Composable
private fun SemanticSteps(stage: AiStage, scenario: ExperimentScenario, showUncertainty: Boolean) {
    val labels = when (scenario) {
        ExperimentScenario.ENVIRONMENT -> listOf("位置", "遮阴", "座椅")
        ExperimentScenario.VISUAL -> listOf("取景", "识别", "解释")
        ExperimentScenario.VOICE -> listOf("聆听", "理解", "回答")
    }
    val progress = when (stage) {
        AiStage.ACTIVATING -> 0
        AiStage.LOCATING, AiStage.LISTENING, AiStage.RECOGNIZING -> 1
        AiStage.REASONING -> 2
        AiStage.RESPONDING, AiStage.UNCERTAIN, AiStage.COMPLETE -> 3
        else -> 0
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        labels.forEachIndexed { index, label ->
            val active = index < progress
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(if (active) SageGreen else SageDivider, CircleShape),
                )
                Spacer(Modifier.width(5.dp))
                Text(label, fontSize = 11.sp, color = if (active) SageGreenDark else SageMuted)
            }
        }
        if (showUncertainty && stage == AiStage.UNCERTAIN) {
            Text("信息不足", fontSize = 11.sp, color = SageOchre, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PromptBubble(text: String) {
    Row(
        modifier = Modifier
            .padding(top = 12.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = .90f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("“$text”", color = SageInk, fontSize = 13.sp, modifier = Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun StartCard(title: String, actionLabel: String, onRun: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = Color.White,
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = SageInk)
            Text("点击后将播放本条件的完整 AI 反馈过程。", color = SageMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 16.dp))
            Button(onClick = onRun, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp)) {
                Text(actionLabel, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun RouteResultPanel(
    condition: ExperimentCondition,
    choice: RouteChoice,
    onChoice: (RouteChoice) -> Unit,
    onAdopt: () -> Unit,
    onEvidence: () -> Unit,
    onReset: () -> Unit,
) {
    val full = condition == ExperimentCondition.SAGE_FULL
    Surface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 18.dp)) {
            Text(
                text = if (choice == RouteChoice.RECOMMENDED) "推荐：湖边林荫线" else "备选：草坪外环线",
                color = SageInk,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (full) {
                Surface(
                    color = SageWarningSurface,
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.padding(top = 9.dp),
                ) {
                    Text(
                        "后半段遮阴信息不足，建议途中留意休息点",
                        color = Color(0xFF805024),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    )
                }
            } else {
                Text("沿湖步行，途经多个休息点。", color = SageMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 7.dp))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                MetricCard("12 分钟", "≈ 850 米", Modifier.weight(1f))
                MetricCard("3 处座椅", "沿途可见", Modifier.weight(1f))
                MetricCard("避开拥挤", "当前人流较少", Modifier.weight(1f))
            }
            Button(onClick = onAdopt, modifier = Modifier.fillMaxWidth().height(54.dp).padding(top = 12.dp), shape = RoundedCornerShape(17.dp)) {
                Text("开始路线", fontSize = 16.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                TextButton(onClick = {
                    onChoice(if (choice == RouteChoice.RECOMMENDED) RouteChoice.ALTERNATIVE else RouteChoice.RECOMMENDED)
                }) { Text("换路线") }
                if (full) TextButton(onClick = onEvidence) { Text("查看依据") }
                if (!full) TextButton(onClick = onReset) { Text("重新开始") }
            }
            if (full) {
                Text(
                    "AI 结果可能不完整，你可以查看依据或更换路线",
                    textAlign = TextAlign.Center,
                    color = SageMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, detail: String, modifier: Modifier = Modifier) {
    Surface(color = Color(0xFFF0F4F1), shape = RoundedCornerShape(15.dp), modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 9.dp),
        ) {
            Text(title, color = SageGreenDark, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
            Text(detail, color = SageMuted, fontSize = 10.sp, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
        }
    }
}

@Composable
private fun VisualExperiment(
    state: ExperimentUiState,
    onRun: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onAdopt: () -> Unit,
    onEvidence: () -> Unit,
    onResearcherPanel: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Image(
            painter = painterResource(R.drawable.flower_stimulus),
            contentDescription = "粉色月季",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .12f)))
        if (state.isRunning && state.condition != ExperimentCondition.BASELINE) ScanFrame(state.aiStage)
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
            AiStatusPanel(state, onResearcherPanel, onCancel)
            PromptBubble(state.scenario.participantPrompt)
        }
        if (!state.isRunning && !state.resultVisible) {
            StartCard("镜头已对准花朵", state.scenario.actionLabel, onRun, Modifier.align(Alignment.BottomCenter))
        }
        AnimatedVisibility(
            state.resultVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        ) {
            ResultPanel(
                title = "识别结果：月季",
                body = if (state.condition == ExperimentCondition.SAGE_FULL) "较可能是丰花月季。叶缘与花瓣形态匹配，但单张照片无法确认具体品种。" else "这是月季，花期通常从春末持续到秋季。",
                warning = if (state.condition == ExperimentCondition.SAGE_FULL) "置信度 78% · 品种信息可能不完整" else null,
                primary = "保存识别结果",
                onPrimary = onAdopt,
                onEvidence = if (state.condition == ExperimentCondition.SAGE_FULL) onEvidence else null,
                onReset = onReset,
            )
        }
    }
}

@Composable
private fun ScanFrame(stage: AiStage) {
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(0f, 1f, infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "scanProgress")
    Canvas(Modifier.fillMaxSize().padding(horizontal = 46.dp, vertical = 190.dp)) {
        val border = Path().apply {
            moveTo(0f, 28f); lineTo(0f, 0f); lineTo(28f, 0f)
            moveTo(size.width - 28f, 0f); lineTo(size.width, 0f); lineTo(size.width, 28f)
            moveTo(size.width, size.height - 28f); lineTo(size.width, size.height); lineTo(size.width - 28f, size.height)
            moveTo(28f, size.height); lineTo(0f, size.height); lineTo(0f, size.height - 28f)
        }
        drawPath(border, Color.White, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        if (stage == AiStage.RECOGNIZING || stage == AiStage.REASONING) {
            val y = size.height * progress
            drawLine(SageMist.copy(alpha = .9f), start = androidx.compose.ui.geometry.Offset(10f, y), end = androidx.compose.ui.geometry.Offset(size.width - 10f, y), strokeWidth = 2.dp.toPx())
        }
    }
}

@Composable
private fun VoiceExperiment(
    state: ExperimentUiState,
    onRun: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onAdopt: () -> Unit,
    onEvidence: () -> Unit,
    onResearcherPanel: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xFFE8ECE8))) {
        Image(painterResource(R.drawable.park_map_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .48f)
        Box(Modifier.fillMaxSize().background(Color(0xFFDCE6E0).copy(alpha = .54f)))
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
            AiStatusPanel(state, onResearcherPanel, onCancel)
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            VoiceOrb(active = state.isRunning)
            Text(
                text = if (state.isRunning && state.condition != ExperimentCondition.BASELINE) state.aiStage.label else if (state.isRunning) "正在处理…" else "轻触开始对话",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = SageInk,
                modifier = Modifier.padding(top = 22.dp),
            )
            Text(
                text = "“${state.scenario.participantPrompt}”",
                textAlign = TextAlign.Center,
                color = SageMuted,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (!state.isRunning && !state.resultVisible) {
            StartCard("边走边问，不必盯着屏幕", state.scenario.actionLabel, onRun, Modifier.align(Alignment.BottomCenter))
        }
        AnimatedVisibility(
            state.resultVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        ) {
            ResultPanel(
                title = "推荐：湖心桥东侧",
                body = "向前约 180 米，桥边能拍到湖面、柳树和远处亭子。下午光线更柔和。",
                warning = if (state.condition == ExperimentCondition.SAGE_FULL) "现场拥挤度为估计值，可能有延迟" else null,
                primary = "开始步行导航",
                onPrimary = onAdopt,
                onEvidence = if (state.condition == ExperimentCondition.SAGE_FULL) onEvidence else null,
                onReset = onReset,
            )
        }
    }
}

@Composable
private fun VoiceOrb(active: Boolean) {
    val transition = rememberInfiniteTransition(label = "voice")
    val pulse by transition.animateFloat(.92f, 1.12f, infiniteRepeatable(tween(850), RepeatMode.Reverse), label = "pulse")
    Box(
        Modifier
            .size(if (active) (104 * pulse).dp else 104.dp)
            .background(SageMist.copy(alpha = .75f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.size(76.dp).background(SageGreen, CircleShape), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
    }
}

@Composable
private fun ResultPanel(
    title: String,
    body: String,
    warning: String?,
    primary: String,
    onPrimary: () -> Unit,
    onEvidence: (() -> Unit)?,
    onReset: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 20.dp)) {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = SageInk)
            Text(body, color = SageMuted, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))
            if (warning != null) {
                Text(
                    warning,
                    color = Color(0xFF805024),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp).background(SageWarningSurface, RoundedCornerShape(12.dp)).padding(10.dp),
                )
            }
            Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth().height(54.dp).padding(top = 12.dp), shape = RoundedCornerShape(17.dp)) {
                Text(primary, fontSize = 16.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (onEvidence != null) TextButton(onClick = onEvidence) { Text("查看依据") }
                TextButton(onClick = onReset) { Text("重新开始") }
            }
        }
    }
}

@Composable
private fun EvidenceDialog(scenario: ExperimentScenario, onDismiss: () -> Unit) {
    val evidence = when (scenario) {
        ExperimentScenario.ENVIRONMENT -> listOf("地图：预计步行时间 12 分钟", "公园设施：沿途标注 3 处座椅", "环境数据：前半段树冠覆盖较高", "缺口：后半段遮阴数据更新于 3 天前")
        ExperimentScenario.VISUAL -> listOf("花瓣：重瓣、粉红色", "叶片：羽状复叶、叶缘有锯齿", "候选：月季 78%，蔷薇 15%", "缺口：单张照片无法判断具体品种")
        ExperimentScenario.VOICE -> listOf("距离：当前位置约 180 米", "景观：湖面、柳树、亭子同框", "光线：下午侧逆光", "缺口：人流数据约有 10 分钟延迟")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Info, null) },
        title = { Text("AI 判断依据") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                evidence.forEach { Text("• $it", color = SageMuted, fontSize = 14.sp) }
                Text("这些信息仅用于解释推荐，不代表绝对准确。", fontSize = 12.sp, color = SageOchre, modifier = Modifier.padding(top = 4.dp))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("知道了") } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResearcherPanel(
    state: ExperimentUiState,
    onDismiss: () -> Unit,
    onScenarioSelected: (ExperimentScenario) -> Unit,
    onConditionSelected: (Int) -> Unit,
    onRun: () -> Unit,
    onReset: () -> Unit,
    onNextCondition: () -> Unit,
    onExport: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, tint = SageGreen)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text("研究员控制台", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("${state.participantId} · 条件 ${state.conditionProgress} · 已完成 ${state.completedTaskCount} 次", color = SageMuted, fontSize = 12.sp)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "关闭") }
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp))
            Text("实验条件", fontWeight = FontWeight.Medium)
            Column(Modifier.padding(top = 6.dp)) {
                state.order.conditions.forEachIndexed { index, condition ->
                    FilterChip(
                        selected = state.conditionIndex == index,
                        onClick = { onConditionSelected(index) },
                        label = { Text("${index + 1}. ${condition.researcherLabel}") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Text("实验任务", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.padding(top = 7.dp)) {
                ExperimentScenario.entries.forEach { scenario ->
                    FilterChip(
                        selected = state.scenario == scenario,
                        onClick = { onScenarioSelected(scenario) },
                        label = { Text("${scenario.id} ${scenario.title}", maxLines = 1) },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 16.dp)) {
                Button(onClick = { onDismiss(); onRun() }, modifier = Modifier.weight(1f)) { Text("运行任务") }
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Refresh, null); Spacer(Modifier.width(6.dp)); Text("重置") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp, bottom = 20.dp)) {
                OutlinedButton(onClick = onNextCondition, modifier = Modifier.weight(1f)) { Text("下一条件") }
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("导出 CSV") }
            }
        }
    }
}

private fun stageDetail(stage: AiStage, scenario: ExperimentScenario): String = when (stage) {
    AiStage.ACTIVATING -> "正在准备所需能力"
    AiStage.LOCATING -> "确认当前位置与公园入口"
    AiStage.LISTENING -> "识别你的语音问题"
    AiStage.RECOGNIZING -> "提取画面中的花瓣与叶片特征"
    AiStage.REASONING -> when (scenario) {
        ExperimentScenario.ENVIRONMENT -> "比较 2 条路线的遮阴与休息点"
        ExperimentScenario.VISUAL -> "对照候选植物特征"
        ExperimentScenario.VOICE -> "结合距离、景观与光线"
    }
    AiStage.RESPONDING -> "把答案组织成简短建议"
    AiStage.UNCERTAIN -> "标出信息不足的部分"
    AiStage.COMPLETE -> "结果已准备好"
    else -> ""
}
