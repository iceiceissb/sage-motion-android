package cn.tsinghua.sagemotion.ui.screens

import android.net.Uri
import android.speech.tts.TextToSpeech
import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.automirrored.filled.VolumeUp
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
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cn.tsinghua.sagemotion.R
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.AiTaskResult
import cn.tsinghua.sagemotion.model.ExperimentCondition
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.ExperimentUiState
import cn.tsinghua.sagemotion.model.RouteChoice
import cn.tsinghua.sagemotion.model.VisionFinding
import cn.tsinghua.sagemotion.model.JourneyPhotoMoment
import cn.tsinghua.sagemotion.ui.components.BreathingVoiceOrb
import cn.tsinghua.sagemotion.ui.components.AdaptiveRouteMotion
import cn.tsinghua.sagemotion.ui.components.MemoryWeaveMotion
import cn.tsinghua.sagemotion.ui.components.ExplorationAmbientMotion
import cn.tsinghua.sagemotion.ui.components.RouteSemanticMotion
import cn.tsinghua.sagemotion.ui.components.VisualSemanticMotion
import cn.tsinghua.sagemotion.ui.components.VoiceSemanticField
import cn.tsinghua.sagemotion.ui.theme.SageDivider
import cn.tsinghua.sagemotion.ui.theme.SageGreen
import cn.tsinghua.sagemotion.ui.theme.SageGreenDark
import cn.tsinghua.sagemotion.ui.theme.SageInk
import cn.tsinghua.sagemotion.ui.theme.SageMist
import cn.tsinghua.sagemotion.ui.theme.SageMuted
import cn.tsinghua.sagemotion.ui.theme.SageOchre
import cn.tsinghua.sagemotion.ui.theme.SageSurface
import cn.tsinghua.sagemotion.ui.theme.SageWarningSurface
import java.util.Locale
import kotlin.math.hypot

@Composable
fun ExperimentScreen(
    state: ExperimentUiState,
    onRunScenario: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onRestartDemo: () -> Unit,
    onFinishSession: () -> Unit,
    onAdopt: () -> Unit,
    onEvidence: () -> Unit,
    onCloseEvidence: () -> Unit,
    onRouteSelected: (RouteChoice) -> Unit,
    onScenarioSelected: (ExperimentScenario) -> Unit,
    onConditionSelected: (Int) -> Unit,
    onNextCondition: () -> Unit,
    onPreviewPrevious: () -> Unit,
    onPreviewNext: () -> Unit,
    onResearcherPanel: (Boolean) -> Unit,
    onHistory: () -> Unit,
    onExport: () -> Unit,
    onExportAll: () -> Unit,
    onVoiceTranscript: (String) -> Unit,
    onRouteConstraintChanged: (String) -> Unit = {},
    onRoutePreferenceToggled: (String) -> Unit = {},
    onVisualQuestionAsked: (String) -> Unit = {},
    onClearVisualQuestion: () -> Unit = {},
    onCreatePhotoUri: () -> Uri,
    onPhotoCaptured: (Boolean) -> Unit,
    onShareJourney: () -> Unit,
    onBeginJourneySummary: () -> Unit,
) {
    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    val toolScenario = state.scenario in setOf(
        ExperimentScenario.VISUAL,
        ExperimentScenario.VOICE,
        ExperimentScenario.ADJUST,
        ExperimentScenario.CREATE,
    )
    val backToHub = { onCancel(); onScenarioSelected(ExperimentScenario.EXPLORE) }
    BackHandler {
        if (toolScenario && !state.isRunning) backToHub() else showExitDialog = true
    }
    if (state.demoCompleted) {
        DemoCompleteScreen(
            onRestart = onRestartDemo,
            onFinish = onFinishSession,
            onResearcherPanel = { onResearcherPanel(true) },
        )
    } else AnimatedContent(
        targetState = state.scenario,
        transitionSpec = {
            (slideInHorizontally(tween(420)) { it / 5 } + fadeIn(tween(360))) togetherWith
                (slideOutHorizontally(tween(320)) { -it / 7 } + fadeOut(tween(260)))
        },
        label = "scenarioTransition",
    ) { scenario ->
        when (scenario) {
        ExperimentScenario.ENVIRONMENT -> RouteExperiment(
            state = state,
            onRun = onRunScenario,
            onCancel = onCancel,
            onReset = onReset,
            onAdopt = onAdopt,
            onEvidence = onEvidence,
            onRouteSelected = onRouteSelected,
            onConstraintChanged = onRouteConstraintChanged,
            onPreferenceToggled = onRoutePreferenceToggled,
            onResearcherPanel = { onResearcherPanel(true) },
        )

        ExperimentScenario.EXPLORE -> ExplorationHub(
            state = state,
            onPhoto = { onScenarioSelected(ExperimentScenario.VISUAL) },
            onVoice = { onScenarioSelected(ExperimentScenario.VOICE) },
            onReplan = { onScenarioSelected(ExperimentScenario.ADJUST) },
            onFinish = onBeginJourneySummary,
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
            onCreatePhotoUri = onCreatePhotoUri,
            onPhotoCaptured = onPhotoCaptured,
            onBack = backToHub,
            onQuestionAsked = onVisualQuestionAsked,
            onClearQuestion = onClearVisualQuestion,
        )

        ExperimentScenario.VOICE -> VoiceExperiment(
            state = state,
            onRun = onRunScenario,
            onCancel = onCancel,
            onReset = onReset,
            onAdopt = onAdopt,
            onEvidence = onEvidence,
            onResearcherPanel = { onResearcherPanel(true) },
            onVoiceTranscript = onVoiceTranscript,
            onBack = backToHub,
        )

        ExperimentScenario.CREATE -> CreateExperiment(
            state = state,
            onRun = onRunScenario,
            onCancel = onCancel,
            onReset = onReset,
            onAdopt = onAdopt,
            onEvidence = onEvidence,
            onShare = onShareJourney,
            onResearcherPanel = { onResearcherPanel(true) },
            onBack = backToHub,
        )

        ExperimentScenario.ADJUST -> AdjustExperiment(
            state = state,
            onRun = onRunScenario,
            onCancel = onCancel,
            onReset = onReset,
            onAdopt = onAdopt,
            onEvidence = onEvidence,
            onRouteSelected = onRouteSelected,
            onResearcherPanel = { onResearcherPanel(true) },
            onBack = backToHub,
        )
        }
    }

    if (state.evidenceVisible) {
        EvidenceDialog(
            evidence = state.taskResult?.evidence.orEmpty(),
            onDismiss = onCloseEvidence,
        )
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
            onPreviewPrevious = onPreviewPrevious,
            onPreviewNext = onPreviewNext,
            onHistory = onHistory,
            onExport = onExport,
            onExportAll = onExportAll,
            onFinishSession = onFinishSession,
        )
    }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("结束当前会话？") },
            text = { Text("实验数据已经实时保存。结束后可以在历史数据中查看和导出；也可以继续当前流程。") },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onFinishSession() }) { Text("保存并结束") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("继续实验") }
            },
        )
    }
}

@Composable
private fun ExplorationHub(
    state: ExperimentUiState,
    onPhoto: () -> Unit,
    onVoice: () -> Unit,
    onReplan: () -> Unit,
    onFinish: () -> Unit,
    onResearcherPanel: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xFFE5E8E2))) {
        Image(painterResource(R.drawable.park_map_background), "当前路线地图", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .18f)))
        RouteSemanticMotion(AiStage.COMPLETE, state.condition)
        ExplorationAmbientMotion(state.capturedPhotoUris.size, state.voiceInteractionCount, state.replanCount)
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
            AiStatusPanel(state, onResearcherPanel) {}
            Surface(
                color = SageGreenDark.copy(alpha = .94f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.padding(top = 10.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).background(Color(0xFF9DE0B6), CircleShape))
                    Text("湖边林荫线进行中", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 9.dp).weight(1f))
                    Text("可随时调用工具", color = Color.White.copy(alpha = .72f), fontSize = 11.sp)
                }
            }
        }
        Surface(
            color = Color.White.copy(alpha = .97f),
            shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            shadowElevation = 14.dp,
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
        ) {
            Column(Modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 17.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("探索工作台", color = SageInk, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Text("功能可以重复使用，不会强制按顺序推进", color = SageMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                    Surface(color = SageMist, shape = RoundedCornerShape(12.dp)) {
                        Text("${state.completedTaskCount} 次 AI 任务", color = SageGreenDark, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp))
                    }
                }
                if (state.capturedPhotoUris.isNotEmpty()) {
                    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        state.capturedPhotoUris.takeLast(3).forEach { uri -> PhotoThumbnail(uri, Modifier.padding(end = 7.dp)) }
                        Column(Modifier.padding(start = 2.dp)) {
                            Text("旅程照片 ${state.capturedPhotoUris.size} 张", color = SageInk, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            Text("结束时会进入知识游记", color = SageMuted, fontSize = 10.sp)
                        }
                    }
                }
                Row(Modifier.fillMaxWidth().padding(top = 13.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HubActionCard(Icons.Default.CameraAlt, "拍照圈搜", "圈出画面再问", "${state.visualInteractionCount} 次", SageGreen, onPhoto, Modifier.weight(1f))
                    HubActionCard(Icons.Default.ChatBubbleOutline, "语音对话", "边走边说", "${state.voiceInteractionCount} 次", Color(0xFF4E718B), onVoice, Modifier.weight(1f))
                    HubActionCard(Icons.AutoMirrored.Filled.AltRoute, "重新规划", "变化随时响应", "${state.replanCount} 次", SageOchre, onReplan, Modifier.weight(1f))
                }
                Button(onClick = onFinish, modifier = Modifier.fillMaxWidth().padding(top = 13.dp).height(53.dp), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text("结束探索并生成知识游记", fontSize = 15.sp)
                }
            }
        }
    }
}

@Composable
private fun HubActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    detail: String,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) .95f else 1f,
        animationSpec = tween(150),
        label = "hubActionPress",
    )
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (pressed) 0.dp else 2.dp),
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.linearGradient(listOf(Color.White.copy(alpha = .98f), accent.copy(alpha = .13f))))
                .padding(horizontal = 8.dp, vertical = 11.dp),
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(accent.copy(alpha = .10f), radius = size.minDimension * .46f, center = Offset(size.width * .92f, size.height * .05f))
                drawCircle(accent.copy(alpha = .08f), radius = size.minDimension * .20f, center = Offset(size.width * .08f, size.height * .92f), style = Stroke(width = 2.5f))
            }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(38.dp).background(accent.copy(alpha = .16f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(color = accent.copy(alpha = .12f), shape = RoundedCornerShape(8.dp)) {
                        Text(detail, color = accent, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp))
                    }
                }
                Text(title, color = SageInk, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth().padding(top = 8.dp), maxLines = 1)
                Text(subtitle, color = SageMuted, fontSize = 9.sp, modifier = Modifier.fillMaxWidth().padding(top = 2.dp), maxLines = 1)
            }
        }
    }
}

@Composable
private fun PhotoThumbnail(rawUri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(rawUri) {
        runCatching {
            context.contentResolver.openInputStream(Uri.parse(rawUri))?.use(BitmapFactory::decodeStream)?.asImageBitmap()
        }.getOrNull()
    }
    if (bitmap != null) {
        Image(bitmap, "过程照片", modifier.size(48.dp).clip(RoundedCornerShape(12.dp)), contentScale = ContentScale.Crop)
    } else {
        Box(modifier.size(48.dp).background(SageMist, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.CameraAlt, null, tint = SageGreen, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun FunctionHeader(
    state: ExperimentUiState,
    onBack: () -> Unit,
    onResearcherPanel: () -> Unit,
    onCancel: () -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            color = Color.White.copy(alpha = .94f),
            shape = CircleShape,
            shadowElevation = 7.dp,
            modifier = Modifier.padding(end = 9.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回探索工作台", tint = SageInk)
            }
        }
        Box(Modifier.weight(1f)) {
            AiStatusPanel(state, onResearcherPanel, onCancel)
        }
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
    onConstraintChanged: (String) -> Unit,
    onPreferenceToggled: (String) -> Unit,
    onResearcherPanel: () -> Unit,
) {
    val inspection = LocalInspectionMode.current
    val constraintSpeech = if (inspection) {
        remember { SpeechInputState("预览模式 · 可用语言补充路线约束", false, .25f) {} }
    } else {
        rememberRealSpeechInputState(onConstraintChanged)
    }
    Box(Modifier.fillMaxSize().background(Color(0xFFE6E7E0))) {
        Image(
            painter = painterResource(R.drawable.park_map_background),
            contentDescription = "公园地图",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.20f)))

        if (state.isRunning || state.resultVisible) {
            RouteSemanticMotion(stage = state.aiStage, condition = state.condition)
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
            PromptBubble(
                text = if (state.isRunning || state.resultVisible) {
                    buildString {
                        append("路线约束：")
                        append(routePreferenceSummary(state.routePreferenceIds))
                        state.routeConstraintText.takeIf { it.isNotBlank() }?.let { append(" · $it") }
                    }
                } else {
                    "先告诉我你今天想怎么走，再为你计算两条路线"
                },
            )
        }

        AnimatedVisibility(
            visible = state.isRunning && state.aiStage != AiStage.ACTIVATING,
            enter = fadeIn(tween(420)) + slideInVertically(tween(520)) { it / 4 },
            exit = fadeOut(tween(220)) + slideOutVertically(tween(260)) { -it / 5 },
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 34.dp, vertical = 68.dp),
        ) {
            RouteCompetitionCard(state.aiStage, state.condition)
        }

        if (!state.isRunning && !state.resultVisible) {
            RouteConstraintCard(
                text = state.routeConstraintText,
                selectedIds = state.routePreferenceIds,
                speech = constraintSpeech,
                onTextChanged = onConstraintChanged,
                onPreferenceToggled = onPreferenceToggled,
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
            state.taskResult?.let { result ->
                RouteResultPanel(
                    result = result,
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
}

private fun routePreferenceSummary(ids: Set<String>): String = listOf(
    "shade" to "阴凉",
    "rest" to "有座椅",
    "short" to "路程短",
    "quiet" to "少人安静",
).filter { it.first in ids }.joinToString("、") { it.second }.ifBlank { "舒适易行" }

@Composable
private fun RouteConstraintCard(
    text: String,
    selectedIds: Set<String>,
    speech: SpeechInputState,
    onTextChanged: (String) -> Unit,
    onPreferenceToggled: (String) -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = Color.White.copy(alpha = .98f),
        shadowElevation = 16.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = SageMist, shape = CircleShape) {
                    Icon(Icons.AutoMirrored.Filled.AltRoute, null, tint = SageGreenDark, modifier = Modifier.padding(9.dp).size(20.dp))
                }
                Column(Modifier.padding(start = 11.dp).weight(1f)) {
                    Text("先约束，再推荐", color = SageInk, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("选择偏好，也可以直接说出今天的需求", color = SageMuted, fontSize = 11.sp)
                }
                Surface(color = SageGreen.copy(alpha = .10f), shape = RoundedCornerShape(9.dp)) {
                    Text("输入阶段", color = SageGreenDark, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
            }
            listOf(
                listOf("shade" to "阴凉优先", "rest" to "沿途有座椅"),
                listOf("short" to "路程更短", "quiet" to "避开人群"),
            ).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (id, label) ->
                        FilterChip(
                            selected = id in selectedIds,
                            onClick = { onPreferenceToggled(id) },
                            label = { Text(label, maxLines = 1, fontSize = 11.sp) },
                            leadingIcon = if (id in selectedIds) {{ Icon(Icons.Default.Check, null, Modifier.size(15.dp)) }} else null,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text("补充一句，例如：不要台阶") },
                )
                IconButton(
                    onClick = speech.onToggle,
                    modifier = Modifier.padding(start = 7.dp).size(48.dp).background(
                        if (speech.isListening) SageGreen else SageMist,
                        CircleShape,
                    ),
                ) {
                    Icon(Icons.Default.Mic, "语音输入路线约束", tint = if (speech.isListening) Color.White else SageGreenDark)
                }
            }
            Text(speech.status, color = if (speech.isListening) SageGreenDark else SageMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            Button(
                onClick = onRun,
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp).height(52.dp),
                shape = RoundedCornerShape(17.dp),
            ) {
                Text("按这些约束计算两条路线", fontSize = 15.sp)
            }
        }
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
                    AnimatedContent(
                        targetState = statusTitle(state, semantic),
                        transitionSpec = { (slideInVertically(tween(240)) { it / 2 } + fadeIn(tween(220))) togetherWith (slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(160))) },
                        label = "stageTitle",
                    ) { title ->
                        Text(title, fontWeight = FontWeight.SemiBold, color = SageInk, fontSize = 15.sp)
                    }
                    if (semantic && state.isRunning) {
                        AnimatedContent(targetState = stageDetail(state.aiStage, state.scenario), label = "stageDetail") { detail ->
                            Text(detail, color = SageMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                if (!state.isRunning) {
                    Surface(color = SageMist, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            text = "体验 ${state.scenarioProgress}",
                            color = SageGreenDark,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
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
        ExperimentScenario.EXPLORE -> listOf("路线", "探索", "记录")
        ExperimentScenario.VISUAL -> listOf("取景", "识别", "解释")
        ExperimentScenario.VOICE -> listOf("聆听", "理解", "回答")
        ExperimentScenario.CREATE -> listOf("汇总", "生成", "编排")
        ExperimentScenario.ADJUST -> listOf("检测", "绕行", "权衡")
    }
    val progress = when (stage) {
        AiStage.ACTIVATING -> 0
        AiStage.LOCATING, AiStage.LISTENING, AiStage.RECOGNIZING -> 1
        AiStage.REASONING, AiStage.SUMMARIZING, AiStage.REPLANNING -> 2
        AiStage.RESPONDING, AiStage.GENERATING, AiStage.EDITING, AiStage.DECIDING,
        AiStage.UNCERTAIN, AiStage.COMPLETE -> 3
        else -> 0
    }
    val railProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = (progress / labels.size.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(620),
        label = "semanticStepRail",
    )
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(17.dp)) {
            val inset = 18.dp.toPx()
            val start = Offset(inset, center.y)
            val end = Offset(size.width - inset, center.y)
            drawLine(SageDivider.copy(alpha = .72f), start, end, 2.dp.toPx(), StrokeCap.Round)
            val activeEnd = Offset(start.x + (end.x - start.x) * railProgress, center.y)
            drawLine(SageGreen, start, activeEnd, 3.dp.toPx(), StrokeCap.Round)
            labels.forEachIndexed { index, _ ->
                val x = start.x + (end.x - start.x) * (index / (labels.lastIndex).coerceAtLeast(1).toFloat())
                val active = index < progress
                drawCircle(Color.White, if (active) 6.dp.toPx() else 5.dp.toPx(), Offset(x, center.y))
                drawCircle(if (active) SageGreen else SageDivider, if (active) 3.5.dp.toPx() else 2.5.dp.toPx(), Offset(x, center.y))
            }
            if (railProgress in .02f..0.98f) {
                drawCircle(SageGreen.copy(alpha = .15f), 8.dp.toPx(), activeEnd)
                drawCircle(Color.White, 3.dp.toPx(), activeEnd)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                Text(
                    label,
                    fontSize = 10.sp,
                    color = if (index < progress) SageGreenDark else SageMuted,
                    fontWeight = if (index == progress - 1) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        if (showUncertainty && stage == AiStage.UNCERTAIN) {
            Text(
                "发现信息缺口 · 保留候选并等待核查",
                fontSize = 10.sp,
                color = SageOchre,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 5.dp),
            )
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
private fun VisualStartCard(
    hasPhoto: Boolean,
    analysisStatus: String?,
    findings: String,
    onCapture: () -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = Color.White,
        shadowElevation = 10.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 18.dp)) {
            Text(if (hasPhoto) "照片已进入识别流程" else "拍下眼前的发现", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = SageInk)
            Text(
                analysisStatus ?: "可调用相机拍摄真实照片，端侧模型会提取图像语义线索。",
                color = SageMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 5.dp),
            )
            if (findings.isNotBlank()) {
                Text(findings, color = SageGreenDark, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp).background(SageMist, RoundedCornerShape(10.dp)).padding(8.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = onCapture, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(19.dp)); Spacer(Modifier.width(6.dp)); Text(if (hasPhoto) "重拍" else "实际拍照")
                }
                Button(onClick = onRun, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(17.dp)) {
                    Text("开始识别")
                }
            }
        }
    }
}

@Composable
private fun VoiceStartCard(
    transcript: String,
    speechStatus: String,
    isListening: Boolean,
    onListen: () -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = Color.White,
        shadowElevation = 10.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 18.dp)) {
            Text("边走边问，不必盯着屏幕", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = SageInk)
            Text(
                if (transcript.isBlank()) speechStatus else "已转写：“$transcript” · $speechStatus",
                color = SageMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 5.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = onListen, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Default.Mic, null, Modifier.size(19.dp)); Spacer(Modifier.width(6.dp)); Text(if (isListening) "结束聆听" else if (transcript.isBlank()) "实际语音" else "重新说")
                }
                Button(onClick = onRun, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(17.dp)) {
                    Text("开始回答")
                }
            }
        }
    }
}

@Composable
private fun RouteResultPanel(
    result: AiTaskResult,
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
            ResultSourceNote(result)
            Text(
                text = if (choice == RouteChoice.RECOMMENDED) result.title else result.alternativeTitle ?: result.title,
                color = SageInk,
                fontSize = 21.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(result.summary, color = SageMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 7.dp))
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RouteChoiceCard(
                    title = "推荐路线",
                    detail = "湖边林荫线",
                    selected = choice == RouteChoice.RECOMMENDED,
                    accent = SageGreen,
                    onClick = { onChoice(RouteChoice.RECOMMENDED) },
                    modifier = Modifier.weight(1f),
                )
                RouteChoiceCard(
                    title = "备选路线",
                    detail = "草坪外环线",
                    selected = choice == RouteChoice.ALTERNATIVE,
                    accent = SageOchre,
                    onClick = { onChoice(RouteChoice.ALTERNATIVE) },
                    modifier = Modifier.weight(1f),
                )
            }
            if (full) {
                Surface(
                    color = SageWarningSurface,
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.padding(top = 9.dp),
                ) {
                    Text(
                        result.uncertainty.orEmpty(),
                        color = Color(0xFF805024),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 8.dp),
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                result.metrics.forEach { metric ->
                    MetricCard(metric.title, metric.detail, Modifier.weight(1f))
                }
            }
            Button(onClick = onAdopt, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(54.dp), shape = RoundedCornerShape(17.dp)) {
                Text(result.primaryAction, fontSize = 16.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (full) TextButton(onClick = onEvidence) { Text("查看依据") }
                TextButton(onClick = onReset) { Text("重新开始") }
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
private fun RouteChoiceCard(
    title: String,
    detail: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fill by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = .12f) else SageSurface,
        animationSpec = tween(260),
        label = "routeChoiceFill",
    )
    Surface(
        color = fill,
        shape = RoundedCornerShape(15.dp),
        modifier = modifier
            .border(if (selected) 1.5.dp else 1.dp, if (selected) accent.copy(alpha = .72f) else SageDivider, RoundedCornerShape(15.dp))
            .clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 10.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(20.dp).background(if (selected) accent else SageDivider, CircleShape), contentAlignment = Alignment.Center) {
                if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(13.dp))
            }
            Column(Modifier.padding(start = 8.dp)) {
                Text(title, color = if (selected) accent else SageMuted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Text(detail, color = SageInk, fontSize = 11.sp, maxLines = 1)
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
private fun RouteCompetitionCard(stage: AiStage, condition: ExperimentCondition) {
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = when (stage) {
            AiStage.LOCATING -> .18f
            AiStage.REASONING -> .72f
            AiStage.UNCERTAIN -> .91f
            AiStage.COMPLETE -> 1f
            else -> .06f
        },
        animationSpec = tween(900),
        label = "routeScoreProgress",
    )
    Surface(
        color = Color.White.copy(alpha = .91f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(SageGreen, CircleShape))
                Text("两条候选路线正在竞争", color = SageInk, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp).weight(1f))
                Text(
                    when (stage) {
                        AiStage.LOCATING -> "构建路径"
                        AiStage.REASONING -> "比较条件"
                        AiStage.UNCERTAIN -> "核查缺口"
                        else -> "即将完成"
                    },
                    color = SageMuted,
                    fontSize = 10.sp,
                )
            }
            CandidateRouteRow("湖边林荫线", "遮阴 · 座椅 · 人流", progress, SageGreen, Modifier.padding(top = 13.dp))
            CandidateRouteRow(
                "草坪外环线",
                if (condition == ExperimentCondition.SAGE_FULL) "开阔 · 较远 · 部分数据缺口" else "开阔 · 较远 · 风景",
                (progress * .86f).coerceAtMost(.84f),
                SageOchre,
                Modifier.padding(top = 11.dp),
            )
        }
    }
}

@Composable
private fun CandidateRouteRow(title: String, detail: String, progress: Float, color: Color, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = SageInk, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text("${(progress * 100).toInt()}%", color = color, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
        Box(Modifier.fillMaxWidth().padding(top = 5.dp).height(5.dp).background(SageDivider.copy(alpha = .68f), CircleShape)) {
            Box(Modifier.fillMaxWidth(progress.coerceIn(.02f, 1f)).height(5.dp).background(color, CircleShape))
        }
        Text(detail, color = SageMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
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
    onCreatePhotoUri: () -> Uri,
    onPhotoCaptured: (Boolean) -> Unit,
    onBack: () -> Unit,
    onQuestionAsked: (String) -> Unit,
    onClearQuestion: () -> Unit,
) {
    val context = LocalContext.current
    var circleCenter by remember { mutableStateOf<Offset?>(null) }
    var circleRadius by remember { mutableFloatStateOf(0f) }
    var circleReady by rememberSaveable(state.capturedPhotoUri) { mutableStateOf(false) }
    var customQuestion by rememberSaveable(state.capturedPhotoUri) { mutableStateOf("") }
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        onPhotoCaptured(success)
    }
    val capturedBitmap = remember(state.capturedPhotoUri) {
        state.capturedPhotoUri?.let { raw ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(raw))?.use(BitmapFactory::decodeStream)?.asImageBitmap()
            }.getOrNull()
        }
    }
    LaunchedEffect(state.capturedPhotoUri) {
        circleCenter = null
        circleRadius = 0f
        circleReady = false
        customQuestion = ""
    }
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (capturedBitmap != null) {
            Image(capturedBitmap, "刚拍摄的照片", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Image(
                painter = painterResource(R.drawable.flower_stimulus),
                contentDescription = "粉色月季",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .12f)))
        if (state.isRunning || state.resultVisible) {
            VisualSemanticMotion(stage = state.aiStage, condition = state.condition)
        }
        AnimatedVisibility(
            visible = state.resultVisible,
            enter = fadeIn(tween(500)),
            exit = fadeOut(tween(180)),
        ) {
            VisionFindingBubbleLayer(state.visionFindings)
        }
        if (state.resultVisible) {
            CircleSearchOverlay(
                center = circleCenter,
                radius = circleRadius,
                enabled = state.visualAnswer == null,
                onStart = { point ->
                    circleCenter = point
                    circleRadius = 0f
                    circleReady = false
                    if (state.visualQuestion.isNotBlank()) onClearQuestion()
                },
                onDrag = { point ->
                    val origin = circleCenter ?: point
                    circleRadius = hypot(point.x - origin.x, point.y - origin.y)
                },
                onEnd = { circleReady = circleRadius > 28f },
            )
        }
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
            FunctionHeader(state, onBack, onResearcherPanel, onCancel)
            PromptBubble(state.scenario.participantPrompt)
        }
        AnimatedVisibility(
            visible = state.isRunning,
            enter = fadeIn(tween(380)) + slideInVertically(tween(480)) { it / 3 },
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(20.dp),
        ) {
            VisionSignalCard(state)
        }
        if (!state.isRunning && !state.resultVisible) {
            VisualStartCard(
                hasPhoto = state.capturedPhotoUri != null,
                analysisStatus = state.photoAnalysisStatus,
                findings = state.visionFindings.joinToString(" · ") { "${it.label} ${(it.confidence * 100).toInt()}%" },
                onCapture = { runCatching { cameraLauncher.launch(onCreatePhotoUri()) } },
                onRun = onRun,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        AnimatedVisibility(
            state.resultVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        ) {
            CircleSearchPanel(
                circleReady = circleReady,
                findings = state.visionFindings,
                question = state.visualQuestion,
                answer = state.visualAnswer,
                customQuestion = customQuestion,
                onCustomQuestionChanged = { customQuestion = it.take(80) },
                onQuestionAsked = onQuestionAsked,
                onClear = {
                    circleCenter = null
                    circleRadius = 0f
                    circleReady = false
                    customQuestion = ""
                    onClearQuestion()
                },
                onCapture = { runCatching { cameraLauncher.launch(onCreatePhotoUri()) } },
                onEvidence = if (state.condition == ExperimentCondition.SAGE_FULL) onEvidence else null,
                onAdopt = onAdopt,
            )
        }
    }
}

@Composable
private fun VisionFindingBubbleLayer(findings: List<VisionFinding>) {
    val visibleFindings = findings.take(5).ifEmpty {
        listOf(
            VisionFinding("主体区域", .82f),
            VisionFinding("环境线索", .74f),
            VisionFinding("表面特征", .68f),
        )
    }
    val floatMotion = androidx.compose.animation.core.rememberInfiniteTransition(label = "findingFloat")
    val floatY by floatMotion.animateFloat(
        initialValue = -4f,
        targetValue = 5f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1700),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "findingFloatY",
    )
    val positions = listOf(.09f to .23f, .57f to .31f, .22f to .49f, .61f to .56f, .38f to .68f)
    BoxWithConstraints(Modifier.fillMaxSize().padding(top = 148.dp, bottom = 270.dp, start = 10.dp, end = 10.dp)) {
        visibleFindings.forEachIndexed { index, finding ->
            val (xFactor, yFactor) = positions[index]
            Surface(
                color = Color(0xFF13251F).copy(alpha = .86f),
                shape = RoundedCornerShape(16.dp),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .offset(x = (maxWidth - 132.dp) * xFactor, y = (maxHeight - 42.dp) * yFactor)
                    .widthIn(max = 150.dp)
                    .graphicsLayer {
                        translationY = floatY * if (index % 2 == 0) 1f else -.72f
                    },
            ) {
                Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(if (index == 0) Color(0xFF9DE0B6) else Color.White.copy(alpha = .72f), CircleShape))
                    Column(Modifier.padding(start = 7.dp)) {
                        Text(finding.label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                        Text("${(finding.confidence * 100).toInt()}% · 语义候选", color = Color.White.copy(alpha = .62f), fontSize = 8.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CircleSearchOverlay(
    center: Offset?,
    radius: Float,
    enabled: Boolean,
    onStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onEnd: () -> Unit,
) {
    val pulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "circleSearchPulse")
    val halo by pulse.animateFloat(
        initialValue = .88f,
        targetValue = 1.10f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(920),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "circleHalo",
    )
    Canvas(
        Modifier
            .fillMaxSize()
            .padding(top = 154.dp, bottom = 285.dp, start = 8.dp, end = 8.dp)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = onStart,
                    onDragEnd = onEnd,
                    onDragCancel = onEnd,
                    onDrag = { change, _ -> onDrag(change.position) },
                )
            },
    ) {
        center?.let { origin ->
            if (radius > 2f) {
                drawCircle(Color.White.copy(alpha = .16f), radius * halo, origin, style = Stroke(width = 10f))
                drawCircle(Color(0xFFB9F5CE), radius, origin, style = Stroke(width = 4.5f, cap = StrokeCap.Round))
                drawCircle(Color.White.copy(alpha = .95f), 5.5f, origin)
                drawLine(Color.White.copy(alpha = .72f), origin - Offset(12f, 0f), origin + Offset(12f, 0f), 2f)
                drawLine(Color.White.copy(alpha = .72f), origin - Offset(0f, 12f), origin + Offset(0f, 12f), 2f)
            }
        }
    }
}

@Composable
private fun CircleSearchPanel(
    circleReady: Boolean,
    findings: List<VisionFinding>,
    question: String,
    answer: String?,
    customQuestion: String,
    onCustomQuestionChanged: (String) -> Unit,
    onQuestionAsked: (String) -> Unit,
    onClear: () -> Unit,
    onCapture: () -> Unit,
    onEvidence: (() -> Unit)?,
    onAdopt: () -> Unit,
) {
    val subject = findings.maxByOrNull { it.confidence }?.label ?: "圈选内容"
    val panelState = when {
        answer != null -> 2
        circleReady -> 1
        else -> 0
    }
    Surface(
        color = Color.White.copy(alpha = .98f),
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        shadowElevation = 18.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        AnimatedContent(
            targetState = panelState,
            transitionSpec = { (fadeIn(tween(260)) + slideInHorizontally { it / 5 }) togetherWith fadeOut(tween(150)) },
            label = "circleSearchPanel",
        ) { phase ->
            Column(Modifier.navigationBarsPadding().padding(horizontal = 19.dp, vertical = 16.dp)) {
                when (phase) {
                    0 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = SageMist, shape = CircleShape) {
                                Icon(Icons.Default.Gesture, null, tint = SageGreenDark, modifier = Modifier.padding(9.dp).size(21.dp))
                            }
                            Column(Modifier.padding(start = 11.dp).weight(1f)) {
                                Text("圈出你想问的部分", color = SageInk, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                Text("AI 已完成多点识别 · 在照片上拖动手指画圈", color = SageMuted, fontSize = 11.sp)
                            }
                            TextButton(onClick = onCapture) { Text("重拍") }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f).height(5.dp).background(SageMist, CircleShape)) {
                                Box(Modifier.fillMaxWidth(.66f).height(5.dp).background(Brush.horizontalGradient(listOf(SageGreen, Color(0xFF8BCFA4))), CircleShape))
                            }
                            Text("识别 → 圈选 → 提问", color = SageGreenDark, fontSize = 10.sp, modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                    1 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = SageGreen, modifier = Modifier.size(22.dp))
                            Column(Modifier.padding(start = 9.dp).weight(1f)) {
                                Text("想了解“$subject”的什么？", color = SageInk, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                                Text("点击推荐问题会直接向 AI 提问", color = SageMuted, fontSize = 10.sp)
                            }
                            TextButton(onClick = onClear) { Text("重圈") }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            listOf("这是什么？", "它有什么特点？").forEach { suggestion ->
                                OutlinedButton(onClick = { onQuestionAsked(suggestion) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(13.dp)) {
                                    Text(suggestion, fontSize = 11.sp, maxLines = 1)
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { onQuestionAsked("为什么会出现在这里？") },
                            modifier = Modifier.fillMaxWidth().padding(top = 7.dp),
                            shape = RoundedCornerShape(13.dp),
                        ) { Text("为什么会出现在这里？", fontSize = 11.sp) }
                        Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = customQuestion,
                                onValueChange = onCustomQuestionChanged,
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                label = { Text("或者输入自己的问题") },
                            )
                            IconButton(
                                onClick = { onQuestionAsked(customQuestion) },
                                enabled = customQuestion.isNotBlank(),
                                modifier = Modifier.padding(start = 7.dp).size(48.dp).background(SageGreen, CircleShape),
                            ) { Icon(Icons.Default.Search, "发送问题", tint = Color.White) }
                        }
                    }
                    else -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = SageMist, shape = CircleShape) {
                                Icon(Icons.Default.AutoAwesome, null, tint = SageGreenDark, modifier = Modifier.padding(9.dp).size(20.dp))
                            }
                            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                                Text(question, color = SageGreenDark, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("AI 圈搜解答", color = SageMuted, fontSize = 10.sp)
                            }
                            TextButton(onClick = onClear) { Text("换个区域") }
                        }
                        Text(answer.orEmpty(), color = SageInk, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 10.dp).background(SageMist.copy(alpha = .72f), RoundedCornerShape(15.dp)).padding(12.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onEvidence != null) {
                                OutlinedButton(onClick = onEvidence, modifier = Modifier.weight(.72f).height(49.dp), shape = RoundedCornerShape(15.dp)) { Text("查看依据") }
                            }
                            Button(onClick = onAdopt, modifier = Modifier.weight(1.28f).height(49.dp), shape = RoundedCornerShape(15.dp)) { Text("保存到游记并返回") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VisionSignalCard(state: ExperimentUiState) {
    val tokens = state.visionFindings.take(3).map { it.label }.ifEmpty { listOf("场景结构", "主体轮廓", "表面特征") }
    Surface(
        color = Color(0xFF172620).copy(alpha = .87f),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(Color(0xFF9DE0B6), CircleShape))
                AnimatedContent(targetState = state.aiStage, label = "visionSignalTitle") { stage ->
                    Text(
                        when (stage) {
                            AiStage.ACTIVATING -> "镜头已接入 · 建立视觉坐标"
                            AiStage.RECOGNIZING -> "扫描画面 · 提取候选区域"
                            AiStage.REASONING -> "视觉线索正在聚合比较"
                            AiStage.UNCERTAIN -> "保留多个候选 · 标记不确定"
                            else -> "视觉记录已经形成"
                        },
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                tokens.forEachIndexed { index, token ->
                    Surface(
                        color = if (index == 0) SageGreen.copy(alpha = .42f) else Color.White.copy(alpha = .10f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(token, color = Color.White.copy(alpha = .88f), fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 6.dp, vertical = 7.dp))
                    }
                }
            }
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
    onVoiceTranscript: (String) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val inspection = LocalInspectionMode.current
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var ttsReady by remember { mutableStateOf(false) }
    DisposableEffect(context, inspection) {
        if (inspection) return@DisposableEffect onDispose { }
        val engine = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = Locale.SIMPLIFIED_CHINESE
        }
        tts = engine
        onDispose { engine.stop(); engine.shutdown(); tts = null }
    }
    val speechInput = if (inspection) {
        remember { SpeechInputState("预览模式 · 轻触后直接在应用内说话", false, .38f) {} }
    } else {
        rememberRealSpeechInputState(onVoiceTranscript)
    }
    LaunchedEffect(state.resultVisible, state.taskResult?.summary, ttsReady) {
        if (state.resultVisible && ttsReady) {
            state.taskResult?.summary?.let { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "sage_answer") }
        }
    }
    Box(Modifier.fillMaxSize().background(Color(0xFFE8ECE8))) {
        Image(painterResource(R.drawable.park_map_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .48f)
        Box(Modifier.fillMaxSize().background(Color(0xFFDCE6E0).copy(alpha = .54f)))
        if (state.isRunning || state.resultVisible || speechInput.isListening) {
            VoiceSemanticField(stage = if (speechInput.isListening) AiStage.LISTENING else state.aiStage, condition = state.condition, inputLevel = speechInput.level)
        }
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
            FunctionHeader(state, onBack, onResearcherPanel, onCancel)
        }
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BreathingVoiceOrb(active = state.isRunning || speechInput.isListening, stage = if (speechInput.isListening) AiStage.LISTENING else state.aiStage, inputLevel = speechInput.level)
            AnimatedContent(
                targetState = if (state.isRunning && state.condition != ExperimentCondition.BASELINE) state.aiStage.label else if (state.isRunning) "正在处理…" else "轻触开始对话",
                label = "voiceStage",
                modifier = Modifier.padding(top = 22.dp),
            ) { label ->
                Text(label, fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = SageInk)
            }
            Text(
                text = "“${state.voiceTranscript.ifBlank { state.scenario.participantPrompt }}”",
                textAlign = TextAlign.Center,
                color = SageMuted,
                lineHeight = 21.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (!state.isRunning && !state.resultVisible) {
            VoiceStartCard(
                transcript = state.voiceTranscript,
                speechStatus = speechInput.status,
                isListening = speechInput.isListening,
                onListen = speechInput.onToggle,
                onRun = onRun,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        AnimatedVisibility(
            state.resultVisible,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
        ) {
            state.taskResult?.let { result ->
                ResultPanel(
                    result = result,
                    showUncertainty = state.condition == ExperimentCondition.SAGE_FULL,
                    onPrimary = onAdopt,
                    onEvidence = if (state.condition == ExperimentCondition.SAGE_FULL) onEvidence else null,
                    onReset = onReset,
                    onSpeak = { result.summary.let { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "sage_replay") } },
                )
            }
        }
    }
}

@Composable
private fun CreateExperiment(
    state: ExperimentUiState,
    onRun: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onAdopt: () -> Unit,
    onEvidence: () -> Unit,
    onShare: () -> Unit,
    onResearcherPanel: () -> Unit,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xFFF2F0E8))) {
        MemoryWeaveMotion(
            stage = state.aiStage,
            condition = state.condition,
            photoCount = state.capturedPhotoUris.size,
            voiceCount = state.voiceInteractionCount,
            replanCount = state.replanCount,
        )
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
            FunctionHeader(state, onBack, onResearcherPanel, onCancel)
            PromptBubble(state.scenario.participantPrompt)
        }
        AnimatedVisibility(
            visible = state.isRunning && state.aiStage != AiStage.ACTIVATING,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 8 },
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            JourneyPreviewCard(state)
        }
        if (!state.isRunning && !state.resultVisible) {
            StartCard("把路线、照片与发现编成知识游记", state.scenario.actionLabel, onRun, Modifier.align(Alignment.BottomCenter))
        }
        AnimatedVisibility(
            visible = state.resultVisible,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(.82f),
        ) {
            state.taskResult?.let { result ->
                JourneyRoutePanel(
                    result = result,
                    moments = journeyMomentsForDisplay(state),
                    showUncertainty = state.condition == ExperimentCondition.SAGE_FULL,
                    onPrimary = onAdopt,
                    onEvidence = if (state.condition == ExperimentCondition.SAGE_FULL) onEvidence else null,
                    onReset = onReset,
                    onShare = onShare,
                )
            }
        }
    }
}

@Composable
private fun JourneyPreviewCard(state: ExperimentUiState) {
    val activeCount = when (state.aiStage) {
        AiStage.SUMMARIZING -> 1
        AiStage.GENERATING -> 2
        AiStage.EDITING, AiStage.COMPLETE -> 3
        else -> 0
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = .94f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = SageGreen, modifier = Modifier.size(20.dp))
                Text("正在编织今日旅程", color = SageInk, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
            JourneyRoutePreview(
                moments = journeyMomentsForDisplay(state),
                stage = state.aiStage,
                modifier = Modifier.fillMaxWidth().padding(top = 11.dp).height(142.dp),
            )
            listOf(
                Triple(Icons.Default.LocationOn, "湖边林荫线", "路线 · 12 分钟"),
                Triple(
                    Icons.Default.CameraAlt,
                    state.visionFindings.take(2).joinToString("、") { it.label }.ifBlank { "照片与视觉线索" },
                    if (state.capturedPhotoUris.isEmpty()) "尚未拍摄" else "${state.capturedPhotoUris.size} 张过程照片",
                ),
                Triple(Icons.AutoMirrored.Filled.VolumeUp, state.voiceTranscript.ifBlank { "语音问答记录" }.take(14), "共 ${state.voiceInteractionCount} 次对话"),
            ).forEachIndexed { index, item ->
                val active = index < activeCount
                val rowProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (active) 1f else 0f,
                    animationSpec = tween(durationMillis = 620, delayMillis = index * 90),
                    label = "journeyMaterial$index",
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 13.dp)
                        .graphicsLayer {
                            alpha = .30f + rowProgress * .70f
                            translationX = (1f - rowProgress) * 34f
                            scaleX = .97f + rowProgress * .03f
                            scaleY = .97f + rowProgress * .03f
                        },
                ) {
                    Surface(color = if (active) SageMist else Color(0xFFF1F2EF), shape = CircleShape) {
                        Icon(item.first, null, tint = if (active) SageGreen else SageMuted, modifier = Modifier.padding(8.dp).size(17.dp))
                    }
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(item.second, color = if (active) SageInk else SageMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(item.third, color = SageMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    AnimatedVisibility(active, enter = fadeIn() + slideInHorizontally { it / 2 }) {
                        Icon(Icons.Default.Check, null, tint = SageGreen, modifier = Modifier.size(17.dp))
                    }
                }
            }
        }
    }
}

private fun journeyMomentsForDisplay(state: ExperimentUiState): List<JourneyPhotoMoment> {
    val knownUris = state.journeyPhotoMoments.map { it.photoUri }.toSet()
    val legacyPhotos = state.capturedPhotoUris.filterNot { it in knownUris }.mapIndexed { index, uri ->
        JourneyPhotoMoment(uri, "沿途发现 ${index + 1}")
    }
    return (state.journeyPhotoMoments + legacyPhotos).takeLast(8)
}

private val journeyNodePositions = listOf(
    .04f to .74f,
    .17f to .48f,
    .31f to .60f,
    .45f to .50f,
    .58f to .34f,
    .70f to .18f,
    .82f to .16f,
    .91f to .22f,
)

@Composable
private fun JourneyRoutePreview(
    moments: List<JourneyPhotoMoment>,
    stage: AiStage,
    modifier: Modifier = Modifier,
) {
    val routeProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = when (stage) {
            AiStage.SUMMARIZING -> .42f
            AiStage.GENERATING -> .78f
            AiStage.EDITING, AiStage.COMPLETE -> 1f
            else -> .08f
        },
        animationSpec = tween(1100),
        label = "journeyRouteReveal",
    )
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Color(0xFFE8EEE9))) {
        Image(painterResource(R.drawable.park_map_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .28f)
        Canvas(Modifier.fillMaxSize()) {
            val fullPath = Path().apply {
                moveTo(size.width * .05f, size.height * .78f)
                cubicTo(size.width * .24f, size.height * .38f, size.width * .38f, size.height * .78f, size.width * .57f, size.height * .43f)
                cubicTo(size.width * .70f, size.height * .18f, size.width * .82f, size.height * .18f, size.width * .94f, size.height * .28f)
            }
            drawPath(fullPath, Color.White.copy(alpha = .92f), style = Stroke(width = 11f, cap = StrokeCap.Round))
            val measure = PathMeasure().apply { setPath(fullPath, false) }
            val visiblePath = Path()
            measure.getSegment(0f, measure.length * routeProgress, visiblePath, true)
            drawPath(visiblePath, SageGreen, style = Stroke(width = 6f, cap = StrokeCap.Round))
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            moments.take(5).forEachIndexed { index, moment ->
                val nodeProgress by androidx.compose.animation.core.animateFloatAsState(
                    targetValue = if (routeProgress >= (index + 1f) / 6f) 1f else 0f,
                    animationSpec = tween(460, delayMillis = index * 90),
                    label = "previewNode$index",
                )
                val (x, y) = journeyNodePositions[index]
                JourneyPhotoImage(
                    rawUri = moment.photoUri,
                    contentDescription = moment.label,
                    modifier = Modifier
                        .offset(x = (maxWidth - 48.dp) * x, y = (maxHeight - 48.dp) * y)
                        .size(48.dp)
                        .graphicsLayer {
                            alpha = nodeProgress
                            scaleX = .68f + nodeProgress * .32f
                            scaleY = .68f + nodeProgress * .32f
                        }
                        .border(3.dp, Color.White, CircleShape),
                )
            }
        }
        Surface(color = SageGreenDark.copy(alpha = .90f), shape = RoundedCornerShape(10.dp), modifier = Modifier.align(Alignment.TopStart).padding(9.dp)) {
            Text("路线正在串联 ${moments.size} 个发现", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
        }
    }
}

@Composable
private fun JourneyRoutePanel(
    result: AiTaskResult,
    moments: List<JourneyPhotoMoment>,
    showUncertainty: Boolean,
    onPrimary: () -> Unit,
    onEvidence: (() -> Unit)?,
    onReset: () -> Unit,
    onShare: () -> Unit,
) {
    var selectedIndex by rememberSaveable(moments.size) { mutableIntStateOf(0) }
    LaunchedEffect(moments.size) {
        selectedIndex = selectedIndex.coerceIn(0, (moments.lastIndex).coerceAtLeast(0))
    }
    Surface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = Color.White.copy(alpha = .98f),
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = SageMist, shape = CircleShape) {
                    Icon(Icons.Default.LocationOn, null, tint = SageGreenDark, modifier = Modifier.padding(9.dp).size(21.dp))
                }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text("今天走过的知识路线", color = SageInk, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("点击沿线照片，回看当时的问题与回答", color = SageMuted, fontSize = 11.sp)
                }
                IconButton(onClick = onShare, modifier = Modifier.background(SageMist, CircleShape)) {
                    Icon(Icons.Default.Share, "分享知识游记", tint = SageGreenDark)
                }
            }
            JourneyRouteMap(
                moments = moments,
                selectedIndex = selectedIndex,
                onSelected = { selectedIndex = it },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(265.dp),
            )
            if (moments.isNotEmpty()) {
                AnimatedContent(
                    targetState = selectedIndex,
                    transitionSpec = {
                        (fadeIn(tween(260)) + slideInHorizontally(tween(320)) { it / 6 }) togetherWith
                            (fadeOut(tween(160)) + slideOutHorizontally(tween(220)) { -it / 8 })
                    },
                    label = "journeyMomentDetail",
                    modifier = Modifier.padding(top = 11.dp),
                ) { index ->
                    JourneyMomentDetail(moments[index.coerceIn(moments.indices)], index + 1, moments.size)
                }
            } else {
                Surface(color = Color(0xFFF4F6F3), shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().padding(top = 11.dp)) {
                    Text("这次旅程还没有照片节点。下次可在探索途中拍照圈搜，照片和问题会自动落到路线上。", color = SageMuted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(14.dp))
                }
            }
            if (showUncertainty && result.uncertainty != null) {
                Text(
                    result.uncertainty,
                    color = Color(0xFF805024),
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 10.dp).background(SageWarningSurface, RoundedCornerShape(12.dp)).padding(9.dp),
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onEvidence != null) {
                    OutlinedButton(onClick = onEvidence, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(15.dp)) { Text("生成依据") }
                }
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(15.dp)) { Text("重新生成") }
            }
            Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(53.dp), shape = RoundedCornerShape(17.dp)) {
                Text(result.primaryAction, fontSize = 15.sp)
            }
        }
    }
}

@Composable
private fun JourneyRouteMap(
    moments: List<JourneyPhotoMoment>,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var entered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val reveal by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (entered) 1f else .02f,
        animationSpec = tween(1250),
        label = "finalJourneyRouteReveal",
    )
    val selectedPulse = androidx.compose.animation.core.rememberInfiniteTransition(label = "selectedJourneyPulse")
    val pulse by selectedPulse.animateFloat(
        initialValue = .88f,
        targetValue = 1.10f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1050),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "selectedJourneyNodePulse",
    )
    Box(modifier.clip(RoundedCornerShape(21.dp)).background(Color(0xFFE4ECE6))) {
        Image(painterResource(R.drawable.park_map_background), "本次步行路线图", Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = .52f)
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .18f)))
        Canvas(Modifier.fillMaxSize()) {
            val fullPath = Path().apply {
                moveTo(size.width * .06f, size.height * .82f)
                cubicTo(size.width * .18f, size.height * .48f, size.width * .35f, size.height * .70f, size.width * .49f, size.height * .52f)
                cubicTo(size.width * .62f, size.height * .35f, size.width * .71f, size.height * .13f, size.width * .93f, size.height * .24f)
            }
            drawPath(fullPath, Color.White.copy(alpha = .96f), style = Stroke(width = 15f, cap = StrokeCap.Round))
            val measure = PathMeasure().apply { setPath(fullPath, false) }
            val segment = Path()
            measure.getSegment(0f, measure.length * reveal, segment, true)
            drawPath(segment, SageGreenDark, style = Stroke(width = 8f, cap = StrokeCap.Round))
            drawCircle(SageGreenDark, 9f, Offset(size.width * .06f, size.height * .82f))
            drawCircle(Color.White, 4f, Offset(size.width * .06f, size.height * .82f))
            drawCircle(SageGreenDark, 10f, Offset(size.width * .93f, size.height * .24f))
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val compactNodes = moments.size > 6
            val nodeSize = if (compactNodes) 48.dp else 62.dp
            moments.take(8).forEachIndexed { index, moment ->
                val (x, y) = journeyNodePositions[index]
                JourneyPhotoNode(
                    moment = moment,
                    index = index,
                    selected = index == selectedIndex,
                    selectedPulse = pulse,
                    compact = compactNodes,
                    onClick = { onSelected(index) },
                    modifier = Modifier.offset(x = (maxWidth - nodeSize) * x, y = (maxHeight - nodeSize) * y),
                )
            }
        }
        Surface(color = SageGreenDark.copy(alpha = .92f), shape = RoundedCornerShape(10.dp), modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(Color(0xFF9DE0B6), CircleShape))
                Text("湖边林荫线 · ${moments.size} 个照片节点", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

@Composable
private fun JourneyPhotoNode(
    moment: JourneyPhotoMoment,
    index: Int,
    selected: Boolean,
    selectedPulse: Float,
    compact: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier
            .size(if (compact) 48.dp else 62.dp)
            .graphicsLayer {
                val scale = if (selected) selectedPulse else if (pressed) .92f else 1f
                scaleX = scale
                scaleY = scale
            }
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Box(Modifier.matchParentSize().background(SageGreen.copy(alpha = .22f), CircleShape))
        JourneyPhotoImage(
            rawUri = moment.photoUri,
            contentDescription = "第 ${index + 1} 个照片节点：${moment.label}",
            modifier = Modifier.size(if (compact) 40.dp else 52.dp).border(if (selected) 4.dp else 3.dp, if (selected) SageGreenDark else Color.White, CircleShape),
        )
        if (moment.questions.isNotEmpty()) {
            Surface(color = SageOchre, shape = CircleShape, modifier = Modifier.align(Alignment.TopEnd)) {
                Text(moment.questions.size.toString(), color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
            }
        }
    }
}

@Composable
private fun JourneyPhotoImage(rawUri: String, contentDescription: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(rawUri) {
        rawUri.takeIf { it.isNotBlank() }?.let { value ->
            runCatching {
                context.contentResolver.openInputStream(Uri.parse(value))?.use(BitmapFactory::decodeStream)?.asImageBitmap()
            }.getOrNull()
        }
    }
    if (bitmap != null) {
        Image(bitmap, contentDescription, modifier.clip(CircleShape), contentScale = ContentScale.Crop)
    } else {
        Image(painterResource(R.drawable.flower_stimulus), contentDescription, modifier.clip(CircleShape), contentScale = ContentScale.Crop)
    }
}

@Composable
private fun JourneyMomentDetail(moment: JourneyPhotoMoment, position: Int, total: Int) {
    Surface(color = Color(0xFFF4F7F4), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JourneyPhotoImage(moment.photoUri, moment.label, Modifier.size(46.dp).border(2.dp, Color.White, CircleShape))
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(moment.label, color = SageInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("路线节点 $position / $total · ${moment.questions.size} 个问题", color = SageMuted, fontSize = 10.sp)
                }
                Icon(Icons.Default.CameraAlt, null, tint = SageGreen, modifier = Modifier.size(18.dp))
            }
            if (moment.questions.isEmpty()) {
                Text("这张照片被保存在路线中，但当时没有继续提问。", color = SageMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 10.dp))
            } else {
                moment.questions.forEachIndexed { index, item ->
                    Column(Modifier.fillMaxWidth().padding(top = 10.dp).background(Color.White, RoundedCornerShape(13.dp)).padding(10.dp)) {
                        Text("Q${index + 1}  ${item.question}", color = SageGreenDark, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Text(item.answer, color = SageInk, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustExperiment(
    state: ExperimentUiState,
    onRun: () -> Unit,
    onCancel: () -> Unit,
    onReset: () -> Unit,
    onAdopt: () -> Unit,
    onEvidence: () -> Unit,
    onRouteSelected: (RouteChoice) -> Unit,
    onResearcherPanel: () -> Unit,
    onBack: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xFFE5E8E2))) {
        Image(painterResource(R.drawable.park_map_background), "动态路线地图", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = .25f)))
        if (state.isRunning || state.resultVisible) AdaptiveRouteMotion(state.aiStage, state.condition)
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
            FunctionHeader(state, onBack, onResearcherPanel, onCancel)
            PromptBubble(state.scenario.participantPrompt)
            AnimatedVisibility(state.isRunning && state.aiStage != AiStage.ACTIVATING) {
                Surface(
                    color = SageWarningSurface.copy(alpha = .94f),
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.padding(top = 10.dp),
                ) {
                    Text(
                        when (state.aiStage) {
                            AiStage.LOCATING -> "检测到变化：湖心桥北段临时封闭"
                            AiStage.REPLANNING -> "旧路线淡出 · 新路线正在绕过封闭点"
                            AiStage.DECIDING -> "权衡时间、遮雨与休息点"
                            AiStage.UNCERTAIN -> "保留原路线作为可接管备选"
                            else -> "正在读取环境变化"
                        },
                        color = Color(0xFF805024),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = state.isRunning && state.aiStage in setOf(AiStage.REPLANNING, AiStage.DECIDING, AiStage.UNCERTAIN),
            enter = fadeIn(tween(360)) + slideInVertically(tween(520)) { it / 4 },
            exit = fadeOut(tween(220)),
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 34.dp, vertical = 80.dp),
        ) {
            ReplanDecisionCard(state.aiStage)
        }
        if (!state.isRunning && !state.resultVisible) {
            StartCard("前方变化，需要重新规划", state.scenario.actionLabel, onRun, Modifier.align(Alignment.BottomCenter))
        }
        AnimatedVisibility(
            visible = state.resultVisible,
            enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            state.taskResult?.let { result ->
                RouteResultPanel(
                    result = result,
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
}

@Composable
private fun ReplanDecisionCard(stage: AiStage) {
    val progress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = when (stage) {
            AiStage.REPLANNING -> .66f
            AiStage.DECIDING -> .90f
            AiStage.UNCERTAIN, AiStage.COMPLETE -> 1f
            else -> .08f
        },
        animationSpec = tween(1080),
        label = "replanDecisionProgress",
    )
    Surface(color = Color.White.copy(alpha = .92f), shape = RoundedCornerShape(24.dp), shadowElevation = 10.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = SageWarningSurface, shape = CircleShape) {
                    Icon(Icons.AutoMirrored.Filled.AltRoute, null, tint = SageOchre, modifier = Modifier.padding(8.dp).size(18.dp))
                }
                Column(Modifier.padding(start = 10.dp)) {
                    Text("路线正在保持因果连续", color = SageInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("旧路线保留为残影，新分支逐段绕开变化点", color = SageMuted, fontSize = 10.sp)
                }
            }
            CandidateRouteRow("原路线", "封闭点前停止 · 可回退", (1f - progress * .58f).coerceAtLeast(.28f), SageOchre, Modifier.padding(top = 13.dp))
            CandidateRouteRow("新路线", "+4 分钟 · 经过 2 处连廊", progress, SageGreen, Modifier.padding(top = 11.dp))
        }
    }
}

@Composable
private fun ResultPanel(
    result: AiTaskResult,
    showUncertainty: Boolean,
    onPrimary: () -> Unit,
    onEvidence: (() -> Unit)?,
    onReset: () -> Unit,
    onSpeak: (() -> Unit)? = null,
    onShare: (() -> Unit)? = null,
    photoUris: List<String> = emptyList(),
) {
    Surface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = Color.White,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 20.dp)) {
            ResultSourceNote(result)
            Text(result.title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = SageInk)
            Text(result.summary, color = SageMuted, fontSize = 14.sp, lineHeight = 21.sp, modifier = Modifier.padding(top = 8.dp))
            if (photoUris.isNotEmpty()) {
                Surface(color = Color(0xFFF5F7F4), shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                    Column(Modifier.padding(10.dp)) {
                        Text("旅程中的照片 · ${photoUris.size} 张", color = SageInk, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Row(Modifier.padding(top = 8.dp)) {
                            photoUris.takeLast(4).forEach { uri -> PhotoThumbnail(uri, Modifier.padding(end = 7.dp)) }
                        }
                    }
                }
            }
            if (showUncertainty && result.uncertainty != null) {
                Text(
                    result.uncertainty,
                    color = Color(0xFF805024),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp).background(SageWarningSurface, RoundedCornerShape(12.dp)).padding(10.dp),
                )
            }
            Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(54.dp), shape = RoundedCornerShape(17.dp)) {
                Text(result.primaryAction, fontSize = 16.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (onSpeak != null) TextButton(onClick = onSpeak) { Icon(Icons.AutoMirrored.Filled.VolumeUp, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("播放语音") }
                if (onShare != null) TextButton(onClick = onShare) { Icon(Icons.Default.Share, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("分享") }
                if (onEvidence != null) TextButton(onClick = onEvidence) { Text("查看依据") }
                TextButton(onClick = onReset) { Text("重新开始") }
            }
        }
    }
}

@Composable
private fun ResultSourceNote(result: AiTaskResult) {
    if (result.sourceLabel == "离线固定刺激") return
    val uriHandler = LocalUriHandler.current
    Surface(
        color = if (result.isLiveData) SageMist else SageWarningSurface,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.padding(bottom = 10.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        ) {
            Text(
                result.sourceLabel,
                color = if (result.isLiveData) SageGreenDark else Color(0xFF805024),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f),
            )
            result.sourceUrl?.let { url ->
                TextButton(onClick = { uriHandler.openUri(url) }) { Text("数据来源", fontSize = 11.sp) }
            }
        }
    }
    result.attribution?.let {
        Text(
            it,
            color = SageMuted,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DemoCompleteScreen(
    onRestart: () -> Unit,
    onFinish: () -> Unit,
    onResearcherPanel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SageSurface)
            .safeDrawingPadding()
            .padding(horizontal = 24.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            color = Color.White,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = {}, onLongClick = onResearcherPanel),
        ) {
            Text(
                "SAGE 公园体验",
                color = SageInk,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier.size(88.dp).background(SageGreen, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(44.dp))
        }
        Text(
            "完整体验已完成",
            color = SageInk,
            fontSize = 26.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 22.dp),
        )
        Text(
            "环境感知、拍照识别、语音陪伴、创作分享和动态调整五个阶段均已演示。",
            color = SageMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 21.sp,
            modifier = Modifier.padding(top = 8.dp),
        )
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
        ) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                val stages = listOf("A  路线规划", "B1  拍照识别", "B2  语音陪伴", "C  创作分享", "D  动态调整")
                stages.forEachIndexed { index, label ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
                    ) {
                        Icon(Icons.Default.Check, null, tint = SageGreen, modifier = Modifier.size(19.dp))
                        Text(label, color = SageInk, modifier = Modifier.padding(start = 10.dp).weight(1f))
                        Text("已完成", color = SageMuted, fontSize = 12.sp)
                    }
                    if (index < stages.lastIndex) HorizontalDivider(color = SageDivider)
                }
            }
        }
        Button(
            onClick = onRestart,
            shape = RoundedCornerShape(17.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 18.dp).height(54.dp),
        ) {
            Text("重新体验完整流程", fontSize = 16.sp)
        }
        OutlinedButton(
            onClick = onFinish,
            shape = RoundedCornerShape(17.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(50.dp),
        ) {
            Icon(Icons.Default.Archive, null, modifier = Modifier.size(18.dp))
            Text("保存并结束本次会话", modifier = Modifier.padding(start = 8.dp))
        }
        Text("全部操作已实时保存在本机", color = SageMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 9.dp))
        Spacer(Modifier.weight(1f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EvidenceDialog(evidence: List<String>, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = SageMist) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = SageGreen,
                        modifier = Modifier.padding(10.dp).size(22.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("为什么这样判断", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = SageInk)
                    Text("把 AI 用到的线索逐条展示给你", fontSize = 12.sp, color = SageMuted)
                }
            }
            evidence.forEachIndexed { index, item ->
                Surface(
                    color = if (index % 2 == 0) SageSurface else SageMist.copy(alpha = 0.72f),
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                        Box(
                            Modifier.size(28.dp).background(SageGreen, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${index + 1}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (index) {
                                    0 -> "主要线索"
                                    1 -> "交叉验证"
                                    else -> "补充依据"
                                },
                                color = SageGreenDark,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(item, color = SageInk, fontSize = 14.sp, lineHeight = 20.sp)
                        }
                    }
                }
            }
            Surface(color = SageOchre.copy(alpha = 0.10f), shape = RoundedCornerShape(15.dp)) {
                Text(
                    "AI 结果可能有误。拍照识别会显示候选标签与置信度，重要信息请结合现场标识确认。",
                    color = SageOchre,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth().padding(13.dp),
                )
            }
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("完成查看")
            }
            Spacer(Modifier.height(4.dp))
        }
    }
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
    onPreviewPrevious: () -> Unit,
    onPreviewNext: () -> Unit,
    onHistory: () -> Unit,
    onExport: () -> Unit,
    onExportAll: () -> Unit,
    onFinishSession: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
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
                ExperimentScenario.entries.filterNot { it == ExperimentScenario.EXPLORE }.forEach { scenario ->
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
            Text("动效状态预览", fontWeight = FontWeight.Medium, modifier = Modifier.padding(top = 14.dp))
            Text("当前：${state.aiStage.label}。可逐步检查每种状态，不触发 API。", color = SageMuted, fontSize = 11.sp, modifier = Modifier.padding(top = 3.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 7.dp)) {
                OutlinedButton(onClick = onPreviewPrevious, modifier = Modifier.weight(1f)) { Text("上一状态") }
                OutlinedButton(onClick = onPreviewNext, modifier = Modifier.weight(1f)) { Text("下一状态") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = onNextCondition, modifier = Modifier.weight(1f)) { Text("下一条件") }
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("导出 CSV") }
            }
            HorizontalDivider(Modifier.padding(vertical = 14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onHistory, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.History, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("历史数据")
                }
                OutlinedButton(onClick = onExportAll, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Archive, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("导出全部")
                }
            }
            TextButton(
                onClick = { onDismiss(); onFinishSession() },
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 16.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(7.dp))
                Text("保存并结束会话")
            }
        }
    }
}

private fun stageDetail(stage: AiStage, scenario: ExperimentScenario): String = when (stage) {
    AiStage.ACTIVATING -> "正在准备所需能力"
    AiStage.LOCATING -> "确认当前位置与公园入口"
    AiStage.LISTENING -> "识别你的语音问题"
    AiStage.RECOGNIZING -> "提取画面中的物体与场景特征"
    AiStage.REASONING -> when (scenario) {
        ExperimentScenario.ENVIRONMENT -> "比较 2 条路线的遮阴与休息点"
        ExperimentScenario.EXPLORE -> "整理当前路线与探索记录"
        ExperimentScenario.VISUAL -> "比较画面中的候选语义标签"
        ExperimentScenario.VOICE -> "结合距离、景观与光线"
        ExperimentScenario.CREATE -> "关联路线、照片与知识发现"
        ExperimentScenario.ADJUST -> "比较时间、遮雨与通行风险"
    }
    AiStage.RESPONDING -> "把答案组织成简短建议"
    AiStage.SUMMARIZING -> "让已完成的路线、照片和问答共同汇聚"
    AiStage.GENERATING -> "沿时间顺序生成知识路线与回忆卡片"
    AiStage.EDITING -> "检查标题、照片、地点与可分享内容"
    AiStage.REPLANNING -> "保留旧路线轨迹并生成可追踪的新分支"
    AiStage.DECIDING -> "权衡绕行时间、天气与休息条件"
    AiStage.UNCERTAIN -> "标出信息不足的部分"
    AiStage.COMPLETE -> "结果已准备好"
    else -> ""
}

private fun statusTitle(state: ExperimentUiState, semantic: Boolean): String = when {
    state.isRunning && !semantic -> "正在处理…"
    state.isRunning -> state.aiStage.label
    state.resultVisible && semantic -> when (state.scenario) {
        ExperimentScenario.ENVIRONMENT -> "已比较 2 条路线"
        ExperimentScenario.EXPLORE -> "探索工具已就绪"
        ExperimentScenario.VISUAL -> "识别与比较完成"
        ExperimentScenario.VOICE -> "回答已生成"
        ExperimentScenario.CREATE -> "知识游记已生成"
        ExperimentScenario.ADJUST -> "动态路线已更新"
    }
    state.aiStage == AiStage.COMPLETE -> state.statusMessage ?: "已完成"
    else -> state.statusMessage ?: state.scenario.phaseLabel
}
