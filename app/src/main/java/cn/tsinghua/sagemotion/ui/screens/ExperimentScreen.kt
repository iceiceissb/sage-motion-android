package cn.tsinghua.sagemotion.ui.screens

import android.net.Uri
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
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
import cn.tsinghua.sagemotion.model.ParkRoute
import cn.tsinghua.sagemotion.ui.components.BreathingVoiceOrb
import cn.tsinghua.sagemotion.ui.components.AmapParkMap
import cn.tsinghua.sagemotion.ui.components.AnimatedMascot
import cn.tsinghua.sagemotion.ui.components.BubbleChoice
import cn.tsinghua.sagemotion.ui.components.FrostedGlassSurface
import cn.tsinghua.sagemotion.ui.components.SignalContours
import cn.tsinghua.sagemotion.ui.components.SignalDivider
import cn.tsinghua.sagemotion.ui.components.SignalHudSurface
import cn.tsinghua.sagemotion.ui.components.SignalWaveform
import cn.tsinghua.sagemotion.ui.components.MascotMood
import cn.tsinghua.sagemotion.ui.components.MemoryWeaveMotion
import cn.tsinghua.sagemotion.ui.components.JourneyStats
import cn.tsinghua.sagemotion.ui.components.ScrapbookJournal
import cn.tsinghua.sagemotion.ui.components.VisualSemanticMotion
import cn.tsinghua.sagemotion.ui.components.VoiceSemanticField
import cn.tsinghua.sagemotion.ui.components.sageBubbleShape
import cn.tsinghua.sagemotion.ui.theme.SageDivider
import cn.tsinghua.sagemotion.ui.theme.SageGreen
import cn.tsinghua.sagemotion.ui.theme.SageGreenDark
import cn.tsinghua.sagemotion.ui.theme.SageHudEdge
import cn.tsinghua.sagemotion.ui.theme.SageHudMuted
import cn.tsinghua.sagemotion.ui.theme.SageInk
import cn.tsinghua.sagemotion.ui.theme.SageMist
import cn.tsinghua.sagemotion.ui.theme.SageMuted
import cn.tsinghua.sagemotion.ui.theme.SageOchre
import cn.tsinghua.sagemotion.ui.theme.SageOnSignal
import cn.tsinghua.sagemotion.ui.theme.SagePanel
import cn.tsinghua.sagemotion.ui.theme.SagePanelRaised
import cn.tsinghua.sagemotion.ui.theme.SagePanelSoft
import cn.tsinghua.sagemotion.ui.theme.SageSignalCoral
import cn.tsinghua.sagemotion.ui.theme.SageSignalCyan
import cn.tsinghua.sagemotion.ui.theme.SageSignalLime
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
    onExportCurrentZip: () -> Unit = {},
    onExportAll: () -> Unit,
    onVoiceTranscript: (String) -> Unit,
    onRouteConstraintChanged: (String) -> Unit = {},
    onReplanRequestChanged: (String) -> Unit = {},
    onRoutePreferenceToggled: (String) -> Unit = {},
    onVisualQuestionAsked: (String) -> Unit = {},
    onClearVisualQuestion: () -> Unit = {},
    onCreatePhotoUri: () -> Uri,
    onPhotoCaptured: (Boolean) -> Unit,
    onShareJourney: () -> Unit,
    onBeginJourneySummary: () -> Unit,
    onRecordMisoperation: () -> Unit = {},
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
            onExitRequest = { showExitDialog = true },
        )

        ExperimentScenario.EXPLORE -> ExplorationHub(
            state = state,
            onPhoto = { onScenarioSelected(ExperimentScenario.VISUAL) },
            onVoice = { onScenarioSelected(ExperimentScenario.VOICE) },
            onReplan = { onScenarioSelected(ExperimentScenario.ADJUST) },
            onFinish = onBeginJourneySummary,
            onResearcherPanel = { onResearcherPanel(true) },
            onExitRequest = { showExitDialog = true },
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
            onRequestChanged = onReplanRequestChanged,
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
            onExportCurrentZip = onExportCurrentZip,
            onExportAll = onExportAll,
            onRecordMisoperation = onRecordMisoperation,
            onFinishSession = onFinishSession,
        )
    }
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("结束今天的探索？") },
            text = { Text("旅程记录已经实时保存在本机。结束后可以在历史记录中查看和导出，也可以继续逛一会儿。") },
            confirmButton = {
                TextButton(onClick = { showExitDialog = false; onFinishSession() }) { Text("保存并结束") }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) { Text("继续探索") }
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
    onExitRequest: () -> Unit,
) {
    val density = LocalDensity.current
    var consoleHeightPx by remember(density) {
        mutableIntStateOf(with(density) { 190.dp.roundToPx() })
    }
    val consoleHeight = with(density) { consoleHeightPx.toDp() }

    Box(Modifier.fillMaxSize().background(Color(0xFF07110E))) {
        AmapParkMap(
            contentDescription = "当前路线地图",
            modifier = Modifier.fillMaxSize(),
            // 采用重规划后，工作台必须立即切到高德返回的另一条候选路线，
            // 不能继续显示进入任务前的旧路线。
            selectedAlternative = state.routeReplanned || state.adoptedRoute == RouteChoice.ALTERNATIVE,
            guidanceControls = true,
            guidanceBottomInset = consoleHeight,
        )
        Box(Modifier.fillMaxSize().background(Color(0xFF020907).copy(alpha = .34f)))
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JourneyExitButton(onExitRequest)
                Box(Modifier.weight(1f)) { AiStatusPanel(state, onResearcherPanel, onCancel = {}) }
            }
            SignalHudSurface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 4.dp, bottomStart = 14.dp),
                borderColor = SageSignalLime.copy(alpha = .42f),
                shadowElevation = 3.dp,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(7.dp).background(SageSignalLime, CircleShape))
                    Text(
                        "${state.activeRouteName}进行中",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 9.dp).weight(1f),
                    )
                    SignalWaveform(Modifier.width(46.dp).height(15.dp))
                    Text("探索中", color = SageSignalLime, fontSize = 9.sp, modifier = Modifier.padding(start = 8.dp))
                }
            }
        }
        SignalHudSurface(
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            borderColor = SageSignalLime.copy(alpha = .56f),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { consoleHeightPx = it.height },
        ) {
            SignalContours(
                modifier = Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 8.dp).size(width = 112.dp, height = 72.dp),
            )
            Column(Modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 15.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val toolUseCount = state.visualInteractionCount + state.voiceInteractionCount + state.replanCount
                    val photoCount = state.capturedPhotoUris.size
                    Column(Modifier.weight(1f)) {
                        Text(
                            "林间探索",
                            color = SageSignalLime,
                            fontSize = 30.sp,
                            lineHeight = 33.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Black,
                            letterSpacing = .5.sp,
                        )
                        Text("让环境信号成为下一步线索", color = SageHudMuted, fontSize = 10.sp)
                    }
                    Text(
                        if (photoCount > 0) "$toolUseCount 次记录 · $photoCount 张照片" else "$toolUseCount 次记录",
                        color = Color.White.copy(alpha = .66f),
                        fontSize = 10.sp,
                    )
                }
                SignalDivider(Modifier.fillMaxWidth().padding(top = 10.dp).height(1.dp), color = SageSignalLime)
                Row(Modifier.fillMaxWidth().padding(top = 8.dp).height(75.dp), verticalAlignment = Alignment.CenterVertically) {
                    HubActionCard(HubTaskKind.PHOTO, "视觉发现", "${state.visualInteractionCount} 次", SageSignalLime, onPhoto, Modifier.weight(1f).fillMaxHeight())
                    SignalDivider(Modifier.width(1.dp).fillMaxHeight(.68f), vertical = true)
                    HubActionCard(HubTaskKind.VOICE, "语音提问", "${state.voiceInteractionCount} 次", SageSignalCyan, onVoice, Modifier.weight(1f).fillMaxHeight())
                    SignalDivider(Modifier.width(1.dp).fillMaxHeight(.68f), vertical = true)
                    HubActionCard(HubTaskKind.REPLAN, "调整路线", "${state.replanCount} 次", SageSignalCoral, onReplan, Modifier.weight(1f).fillMaxHeight())
                }
                Button(
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(50.dp),
                    shape = RoundedCornerShape(15.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SageSignalLime),
                ) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(17.dp), tint = Color(0xFF08110F)); Spacer(Modifier.width(7.dp)); Text("完成探索", fontSize = 15.sp, color = Color(0xFF08110F), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun HubActionCard(
    kind: HubTaskKind,
    title: String,
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
    Surface(
        color = if (pressed) accent.copy(alpha = .12f) else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .graphicsLayer { scaleX = pressScale; scaleY = pressScale }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 7.dp, vertical = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HubTaskGlyph(kind, accent)
            Text(title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Medium, maxLines = 1, modifier = Modifier.padding(top = 3.dp))
            Text(detail, color = accent, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 1.dp))
        }
    }
}

private enum class HubTaskKind { PHOTO, VOICE, REPLAN }

@Composable
private fun JourneyExitButton(onClick: () -> Unit) {
    SignalHudSurface(
        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 13.dp, bottomEnd = 4.dp, bottomStart = 13.dp),
        borderColor = SageSignalLime.copy(alpha = .50f),
        shadowElevation = 6.dp,
        modifier = Modifier.padding(end = 9.dp).size(width = 54.dp, height = 58.dp),
    ) {
        Column(
            Modifier.fillMaxSize().clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, "保存并回到首页", tint = SageSignalLime, modifier = Modifier.size(23.dp))
            Text("退出", color = Color.White.copy(alpha = .82f), fontSize = 9.sp, modifier = Modifier.padding(top = 2.dp))
        }
    }
}

/** 简洁线性图标：不使用大色块、外圈或装饰弧，避免在浅色地图上形成灰边。 */
@Composable
private fun HubTaskGlyph(kind: HubTaskKind, accent: Color) {
    Canvas(Modifier.size(24.dp).padding(2.dp)) {
        val stroke = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round)
        when (kind) {
            HubTaskKind.PHOTO -> {
                drawRoundRect(accent, cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx()), style = stroke)
                drawCircle(accent, radius = size.minDimension * .18f, center = center, style = stroke)
                drawLine(accent, Offset(size.width * .24f, 0f), Offset(size.width * .43f, 0f), strokeWidth = stroke.width, cap = StrokeCap.Round)
            }
            HubTaskKind.VOICE -> {
                val bars = listOf(.34f, .68f, 1f, .68f, .34f)
                bars.forEachIndexed { index, fraction ->
                    val x = size.width * (.12f + index * .19f)
                    val half = size.height * .36f * fraction
                    drawLine(accent, Offset(x, center.y - half), Offset(x, center.y + half), strokeWidth = 2.6.dp.toPx(), cap = StrokeCap.Round)
                }
            }
            HubTaskKind.REPLAN -> {
                val start = Offset(size.width * .18f, size.height * .80f)
                val fork = Offset(size.width * .48f, size.height * .50f)
                val upper = Offset(size.width * .82f, size.height * .18f)
                val lower = Offset(size.width * .82f, size.height * .74f)
                drawLine(accent, start, fork, strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(accent, fork, upper, strokeWidth = stroke.width, cap = StrokeCap.Round)
                drawLine(accent.copy(alpha = .72f), fork, lower, strokeWidth = stroke.width, cap = StrokeCap.Round)
                listOf(start, fork, upper, lower).forEach { drawCircle(accent, 2.3.dp.toPx(), it) }
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
        SignalHudSurface(
            shape = CircleShape,
            borderColor = SageSignalLime.copy(alpha = .48f),
            shadowElevation = 7.dp,
            modifier = Modifier.padding(end = 9.dp).size(48.dp),
        ) {
            IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回探索工作台", tint = SageSignalLime)
            }
        }
        Box(Modifier.weight(1f)) {
            AiStatusPanel(
                state = state,
                onLongPress = onResearcherPanel,
                onCancel = onCancel,
                showMascot = state.scenario != ExperimentScenario.VOICE,
            )
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
    onExitRequest: () -> Unit,
) {
    val inspection = LocalInspectionMode.current
    val constraintSpeech = if (inspection) {
        remember { SpeechInputState("预览模式 · 可用语言补充路线约束", false, .25f) {} }
    } else {
        rememberRealSpeechInputState(state.routeConstraintText, onConstraintChanged)
    }
    Box(Modifier.fillMaxSize().background(Color(0xFF07110E))) {
        AmapParkMap(
            contentDescription = "公园地图",
            modifier = Modifier.fillMaxSize(),
            selectedAlternative = state.selectedRoute == RouteChoice.ALTERNATIVE,
            routeEnabled = state.isRunning || state.resultVisible,
            showRouteSummary = false,
        )
        Box(Modifier.fillMaxSize().background(Color(0xFF020907).copy(alpha = 0.34f)))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                JourneyExitButton(onExitRequest)
                Box(Modifier.weight(1f)) {
                    AiStatusPanel(state = state, onLongPress = onResearcherPanel, onCancel = onCancel)
                }
            }
            if (!state.resultVisible) {
                PromptBubble(
                    text = if (state.isRunning) {
                        "正在把偏好与园路条件放到一起比较"
                    } else {
                        "先告诉我你今天想怎么走，再为你推荐合适的路线"
                    },
                )
            }
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

private fun mascotMoodFor(state: ExperimentUiState): MascotMood = when {
    state.aiStage == AiStage.COMPLETE || state.scenario == ExperimentScenario.CREATE -> MascotMood.CELEBRATING
    state.aiStage == AiStage.LISTENING || state.scenario == ExperimentScenario.VOICE -> MascotMood.LISTENING
    state.aiStage == AiStage.RECOGNIZING || state.scenario == ExperimentScenario.VISUAL -> MascotMood.DISCOVERING
    state.aiStage in setOf(AiStage.LOCATING, AiStage.REPLANNING, AiStage.DECIDING) ||
        state.scenario in setOf(ExperimentScenario.ENVIRONMENT, ExperimentScenario.EXPLORE, ExperimentScenario.ADJUST) -> MascotMood.NAVIGATING
    else -> MascotMood.IDLE
}

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
    FrostedGlassSurface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        modifier = modifier.fillMaxWidth().imePadding(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 20.dp, vertical = 17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("先约束，再推荐", color = SageInk, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text("选择偏好，也可以直接说出今天的需求", color = SageMuted, fontSize = 11.sp)
                }
                Surface(color = SageSignalLime.copy(alpha = .10f), shape = RoundedCornerShape(9.dp)) {
                    Text("告诉银小叶", color = SageSignalLime, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
            }
            listOf(
                listOf(Triple("shade", "阴凉优先", SagePanelRaised), Triple("rest", "沿途有座椅", SagePanelRaised)),
                listOf(Triple("short", "路程更短", SagePanelRaised), Triple("quiet", "避开人群", SagePanelRaised)),
            ).forEachIndexed { rowIndex, row ->
                Row(Modifier.fillMaxWidth().padding(top = 7.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEachIndexed { columnIndex, (id, label, tint) ->
                        BubbleChoice(
                            selected = id in selectedIds,
                            onClick = { onPreferenceToggled(id) },
                            variant = rowIndex * 2 + columnIndex,
                            tint = tint,
                            modifier = Modifier.weight(1f),
                        ) {
                            if (id in selectedIds) {
                                Icon(Icons.Default.Check, null, Modifier.size(15.dp))
                            } else {
                                Box(Modifier.size(8.dp).background(SageHudMuted.copy(alpha = .52f), CircleShape))
                            }
                            Text(label, maxLines = 1, fontSize = 11.sp, modifier = Modifier.padding(start = 7.dp))
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChanged,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = sageBubbleShape(2),
                    label = { Text("补充一句，例如：不要台阶") },
                )
                IconButton(
                    onClick = speech.onToggle,
                    modifier = Modifier.padding(start = 7.dp).size(48.dp).background(
                        if (speech.isListening) SageSignalCyan else SageMist,
                        CircleShape,
                    ),
                ) {
                    Icon(Icons.Default.Mic, "语音输入路线约束", tint = if (speech.isListening) SageOnSignal else SageSignalCyan)
                }
            }
            Text(speech.status, color = if (speech.isListening) SageSignalCyan else SageMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            Button(
                onClick = onRun,
                enabled = !speech.isListening,
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp).height(52.dp),
                shape = sageBubbleShape(1),
            ) {
                Text(if (text.isBlank()) "按这些偏好推荐合适路线" else "确认约束并推荐路线", fontSize = 15.sp, color = SageOnSignal, fontWeight = FontWeight.Bold)
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
    showMascot: Boolean = true,
) {
    val semantic = state.condition != ExperimentCondition.BASELINE
    val full = state.condition == ExperimentCondition.SAGE_FULL
    SignalHudSurface(
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 9.dp, bottomEnd = 16.dp, bottomStart = 9.dp),
        borderColor = SageHudEdge,
        shadowElevation = 7.dp,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = {}, onLongClick = onLongPress),
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showMascot) {
                    AnimatedMascot(
                        mood = mascotMoodFor(state),
                        modifier = Modifier.size(31.dp),
                    )
                    Spacer(Modifier.width(7.dp))
                }
                Column(Modifier.weight(1f)) {
                    AnimatedContent(
                        targetState = statusTitle(state, semantic),
                        transitionSpec = { (slideInVertically(tween(240)) { it / 2 } + fadeIn(tween(220))) togetherWith (slideOutVertically(tween(180)) { -it / 2 } + fadeOut(tween(160))) },
                        label = "stageTitle",
                    ) { title ->
                        Text(
                            title,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.SansSerif,
                            color = Color.White,
                            fontSize = 15.sp,
                            letterSpacing = .2.sp,
                        )
                    }
                    if (semantic && state.isRunning) {
                        AnimatedContent(targetState = stageDetail(state.aiStage, state.scenario), label = "stageDetail") { detail ->
                            Text(detail, color = SageHudMuted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
                SignalWaveform(
                    modifier = Modifier.width(if (state.isRunning) 52.dp else 38.dp).height(18.dp),
                    color = if (state.scenario == ExperimentScenario.ADJUST) SageSignalCoral else SageSignalCyan,
                )
                if (!state.isRunning) {
                    Surface(color = SageSignalLime.copy(alpha = .10f), shape = RoundedCornerShape(5.dp), modifier = Modifier.padding(start = 7.dp)) {
                        Text(
                            text = "旅程 ${state.scenarioProgress}",
                            color = SageSignalLime,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                }
                if (state.isRunning) {
                    IconButton(onClick = onCancel, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "取消", tint = Color.White, modifier = Modifier.size(19.dp))
                    }
                }
            }
            if (semantic && (state.isRunning || state.resultVisible)) {
                Spacer(Modifier.height(8.dp))
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
            drawLine(Color.White.copy(alpha = .18f), start, end, 1.5.dp.toPx(), StrokeCap.Round)
            val activeEnd = Offset(start.x + (end.x - start.x) * railProgress, center.y)
            drawLine(SageSignalLime, start, activeEnd, 2.5.dp.toPx(), StrokeCap.Round)
            labels.forEachIndexed { index, _ ->
                val x = start.x + (end.x - start.x) * (index / (labels.lastIndex).coerceAtLeast(1).toFloat())
                val active = index < progress
                drawCircle(Color(0xFF08110F), if (active) 6.dp.toPx() else 5.dp.toPx(), Offset(x, center.y))
                drawCircle(if (active) SageSignalLime else Color.White.copy(alpha = .24f), if (active) 3.5.dp.toPx() else 2.5.dp.toPx(), Offset(x, center.y))
            }
            if (railProgress in .02f..0.98f) {
                drawCircle(SageSignalLime.copy(alpha = .16f), 8.dp.toPx(), activeEnd)
                drawCircle(Color.White, 3.dp.toPx(), activeEnd)
            }
        }
        Row(Modifier.fillMaxWidth()) {
            labels.forEachIndexed { index, label ->
                Text(
                    label,
                    fontSize = 10.sp,
                    color = if (index < progress) SageSignalLime else SageHudMuted,
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
                color = SageSignalCoral,
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
            .background(SagePanelRaised.copy(alpha = .96f))
            .border(1.dp, SageHudEdge, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("“$text”", color = SageInk, fontSize = 13.sp, modifier = Modifier.weight(1f, fill = false))
    }
}

@Composable
private fun StartCard(title: String, actionLabel: String, onRun: () -> Unit, modifier: Modifier = Modifier) {
    FrostedGlassSurface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 20.dp),
        ) {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = SageInk)
            Text("点击后将播放本条件的完整 AI 反馈过程。", color = SageMuted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp, bottom = 16.dp))
            Button(onClick = onRun, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(17.dp)) {
                Text(actionLabel, fontSize = 16.sp, color = SageOnSignal, fontWeight = FontWeight.Bold)
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
    FrostedGlassSurface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        tint = SagePanelRaised,
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
                Text(findings, color = SageSignalLime, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp).background(SageMist, RoundedCornerShape(10.dp)).padding(8.dp))
            }
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedButton(onClick = onCapture, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(17.dp)) {
                    Icon(Icons.Default.CameraAlt, null, Modifier.size(19.dp)); Spacer(Modifier.width(6.dp)); Text(if (hasPhoto) "重拍" else "实际拍照")
                }
                Button(onClick = onRun, modifier = Modifier.weight(1f).height(52.dp), shape = RoundedCornerShape(17.dp)) {
                    Text("开始识别", color = SageOnSignal, fontWeight = FontWeight.Bold)
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
    onTranscriptChanged: (String) -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FrostedGlassSurface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        tint = SagePanelRaised,
        modifier = modifier.fillMaxWidth().imePadding(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 18.dp)) {
            Text("边走边问，不必盯着屏幕", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = SageInk)
            Text(
                "可以说话或手动输入，识别文字会写入输入框，确认后再回答。",
                color = SageMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 5.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = transcript,
                    onValueChange = onTranscriptChanged,
                    label = { Text("说话或键盘输入问题") },
                    placeholder = { Text("例如：附近哪里有好吃的？") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(15.dp),
                )
                IconButton(
                    onClick = onListen,
                    modifier = Modifier.padding(start = 7.dp).size(48.dp).background(
                        if (isListening) SageSignalCyan else SageMist,
                        CircleShape,
                    ),
                ) {
                    Icon(
                        Icons.Default.Mic,
                        if (isListening) "结束语音输入" else "开始语音输入",
                        tint = if (isListening) SageOnSignal else SageSignalCyan,
                    )
                }
            }
            Text(
                speechStatus,
                color = if (isListening) SageSignalCyan else SageMuted,
                fontSize = 10.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Button(
                enabled = transcript.isNotBlank() && !isListening,
                onClick = onRun,
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp).height(52.dp),
                shape = RoundedCornerShape(17.dp),
            ) {
                Text("确认问题并回答", color = SageOnSignal, fontWeight = FontWeight.Bold)
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
    val routeTitle = if (choice == RouteChoice.RECOMMENDED) result.title else result.alternativeTitle ?: result.title
    SignalHudSurface(
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
        borderColor = SageSignalLime.copy(alpha = .54f),
        shadowElevation = 11.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        SignalContours(
            modifier = Modifier.align(Alignment.TopEnd).padding(top = 5.dp, end = 8.dp).size(width = 124.dp, height = 82.dp),
        )
        Column(Modifier.navigationBarsPadding().padding(horizontal = 18.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.width(13.dp).height(4.dp).background(SageSignalLime, RoundedCornerShape(3.dp)))
                Text(
                    "路线建议",
                    color = SageSignalLime,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 7.dp),
                )
                SignalWaveform(Modifier.padding(start = 9.dp).width(34.dp).height(12.dp))
                Spacer(Modifier.weight(1f))
                Text(
                    result.sourceLabel.substringBefore("（").substringBefore("(").take(22),
                    color = SageHudMuted,
                    fontSize = 9.sp,
                )
            }
            RouteVisualBoard(
                title = routeTitle,
                metrics = result.metrics.take(3).map { it.title to it.detail }.ifEmpty {
                    listOf("阴凉" to "树荫优先", "座椅" to "3 处", "路程" to "约 12 分")
                },
                alternative = choice == RouteChoice.ALTERNATIVE,
                modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
            )
            SignalDivider(Modifier.fillMaxWidth().padding(top = 6.dp).height(1.dp), color = SageSignalLime)
            RouteChoiceCard(
                title = "推荐路线",
                detail = "湖边林荫线 · 途经湖边与林荫",
                selected = choice == RouteChoice.RECOMMENDED,
                accent = SageSignalLime,
                onClick = { onChoice(RouteChoice.RECOMMENDED) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            )
            SignalDivider(Modifier.fillMaxWidth().height(1.dp))
            RouteChoiceCard(
                title = "备选路线",
                detail = "草坪外环线 · 更短但更晒",
                selected = choice == RouteChoice.ALTERNATIVE,
                accent = SageSignalCoral,
                onClick = { onChoice(RouteChoice.ALTERNATIVE) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
            )
            if (full) {
                Surface(
                    color = SageSignalCoral.copy(alpha = .08f),
                    shape = RoundedCornerShape(9.dp),
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                ) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, null, tint = SageSignalCoral, modifier = Modifier.size(14.dp))
                        Text(
                            result.uncertainty.orEmpty(),
                            color = Color.White.copy(alpha = .66f),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 6.dp),
                        )
                    }
                }
            }
            Button(
                onClick = onAdopt,
                modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SageSignalLime),
            ) {
                Text(result.primaryAction, fontSize = 15.sp, color = Color(0xFF08110F), fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Color(0xFF08110F), modifier = Modifier.size(17.dp))
            }
            Row(Modifier.fillMaxWidth().height(40.dp), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                if (full) TextButton(onClick = onEvidence) { Text("查看依据", color = SageHudMuted) }
                TextButton(onClick = onReset) { Text("重新开始", color = SageHudMuted) }
            }
        }
    }
}

@Composable
private fun RouteVisualBoard(
    title: String,
    metrics: List<Pair<String, String>>,
    alternative: Boolean,
    modifier: Modifier = Modifier,
) {
    val accent = if (alternative) SageSignalCoral else SageSignalLime
    Column(modifier) {
        Text(
            title.substringAfter("：").ifBlank { title },
            color = Color.White,
            fontSize = 25.sp,
            lineHeight = 29.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            if (alternative) "开阔外环，视野更宽" else "沿湖而行，树荫连续，步行更舒适",
            color = SageHudMuted,
            fontSize = 10.sp,
            modifier = Modifier.padding(top = 1.dp),
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp).height(38.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            metrics.take(3).forEachIndexed { index, metric ->
                if (index > 0) SignalDivider(Modifier.width(1.dp).fillMaxHeight(.72f), vertical = true)
                Column(
                    Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(metric.first, color = if (index == 0) accent else Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    Text(metric.second, color = SageHudMuted, fontSize = 9.sp, maxLines = 1, modifier = Modifier.padding(top = 2.dp))
                }
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
        targetValue = if (selected) accent.copy(alpha = .08f) else Color.Transparent,
        animationSpec = tween(260),
        label = "routeChoiceFill",
    )
    Surface(
        color = fill,
        shape = RoundedCornerShape(9.dp),
        modifier = modifier
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(20.dp)
                    .border(1.5.dp, accent, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) Box(Modifier.size(10.dp).background(accent, CircleShape))
            }
            Column(Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    title,
                    color = if (selected) accent else Color.White,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                )
                Text(
                    detail,
                    color = SageHudMuted,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = if (selected) accent else Color.White.copy(alpha = .62f),
                modifier = Modifier.size(17.dp),
            )
        }
    }
}

@Composable
private fun MetricCard(title: String, detail: String, modifier: Modifier = Modifier) {
    Surface(color = SagePanelSoft, shape = RoundedCornerShape(15.dp), modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 9.dp),
        ) {
            Text(title, color = SageSignalLime, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
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
        color = SagePanelRaised.copy(alpha = .96f),
        shape = RoundedCornerShape(24.dp),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(SageSignalLime, CircleShape))
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
            CandidateRouteRow("湖边林荫线", "遮阴 · 座椅 · 人流", progress, SageSignalLime, Modifier.padding(top = 13.dp))
            CandidateRouteRow(
                "草坪外环线",
                if (condition == ExperimentCondition.SAGE_FULL) "开阔 · 较远 · 部分数据缺口" else "开阔 · 较远 · 风景",
                (progress * .86f).coerceAtMost(.84f),
                SageSignalCoral,
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
    val inspection = LocalInspectionMode.current
    var circleCenter by remember { mutableStateOf<Offset?>(null) }
    var circleRadius by remember { mutableFloatStateOf(0f) }
    var circleReady by rememberSaveable(state.capturedPhotoUri) { mutableStateOf(inspection && state.resultVisible) }
    var customQuestion by rememberSaveable(state.capturedPhotoUri) { mutableStateOf("") }
    val questionSpeech = if (inspection) {
        remember { SpeechInputState("预览模式 · 可说话或键盘输入问题", false, .25f) {} }
    } else {
        rememberRealSpeechInputState(customQuestion) { customQuestion = it.take(80) }
    }
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
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .30f)))
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
            if (!state.resultVisible) PromptBubble(state.scenario.participantPrompt)
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
                usesFixedFlowerStimulus = state.capturedPhotoUri == null,
                question = state.visualQuestion,
                answer = state.visualAnswer,
                customQuestion = customQuestion,
                questionSpeech = questionSpeech,
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
        initialValue = -3f,
        targetValue = 4f,
        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
            animation = tween(1700),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse,
        ),
        label = "findingFloatY",
    )
    val inspection = LocalInspectionMode.current
    val positions = listOf(.08f to .20f, .64f to .31f, .20f to .47f, .66f to .56f, .40f to .67f)
    val bubbleColors = listOf(
        SagePanelRaised,
        Color(0xFF111B18),
        Color(0xFF0D1918),
        Color(0xFF181412),
        Color(0xFF11170D),
    )
    val accentColors = listOf(
        SageSignalLime,
        SageSignalCoral,
        SageSignalCyan,
        Color(0xFFA7BEFF),
        Color(0xFFFFC45B),
    )
    BoxWithConstraints(Modifier.fillMaxSize().padding(top = 148.dp, bottom = 270.dp, start = 10.dp, end = 10.dp)) {
        visibleFindings.forEachIndexed { index, finding ->
            val (xFactor, yFactor) = positions[index % positions.size]
            val bubbleSize = (78f + finding.confidence.coerceIn(.5f, 1f) * 16f).dp
            val accent = accentColors[index % accentColors.size]
            Surface(
                color = bubbleColors[index % bubbleColors.size].copy(alpha = .94f),
                contentColor = SageInk,
                shape = CircleShape,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .offset(x = (maxWidth - bubbleSize) * xFactor, y = (maxHeight - bubbleSize) * yFactor)
                    .size(bubbleSize)
                    .graphicsLayer {
                        translationY = if (inspection) 0f else floatY * if (index % 2 == 0) 1f else -.72f
                    }
                    .border(1.dp, accent.copy(alpha = .58f), CircleShape),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(Modifier.size(18.dp)) {
                        Box(Modifier.align(Alignment.Center).size(8.dp).background(accent, CircleShape))
                        Box(Modifier.align(Alignment.TopStart).size(4.dp).background(accent.copy(alpha = .55f), CircleShape))
                        Box(Modifier.align(Alignment.BottomEnd).size(4.dp).background(accent.copy(alpha = .42f), CircleShape))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = finding.label,
                        color = SageInk,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${(finding.confidence * 100).toInt()}%",
                        color = accent,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                    )
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
    usesFixedFlowerStimulus: Boolean,
    question: String,
    answer: String?,
    customQuestion: String,
    questionSpeech: SpeechInputState,
    onCustomQuestionChanged: (String) -> Unit,
    onQuestionAsked: (String) -> Unit,
    onClear: () -> Unit,
    onCapture: () -> Unit,
    onEvidence: (() -> Unit)?,
    onAdopt: () -> Unit,
) {
    val rawSubject = findings.maxByOrNull { it.confidence }?.label
    val subject = when {
        usesFixedFlowerStimulus -> "粉红色花卉"
        rawSubject.isNullOrBlank() -> "圈选主体"
        rawSubject in setOf("主体区域", "环境线索", "表面特征", "圈选区域", "圈选内容") -> "圈选主体"
        else -> rawSubject
    }
    val panelState = when {
        answer != null -> 2
        circleReady -> 1
        else -> 0
    }
    FrostedGlassSurface(
        tint = SagePanelRaised,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        modifier = Modifier.fillMaxWidth().imePadding(),
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
                                Icon(Icons.Default.Gesture, null, tint = SageSignalLime, modifier = Modifier.padding(9.dp).size(21.dp))
                            }
                            Column(Modifier.padding(start = 11.dp).weight(1f)) {
                                Text("圈出你想问的部分", color = SageInk, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                                Text("AI 已完成多点识别 · 在照片上拖动手指画圈", color = SageMuted, fontSize = 11.sp)
                            }
                            TextButton(onClick = onCapture) { Text("重拍") }
                        }
                        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f).height(5.dp).background(SageMist, CircleShape)) {
                                Box(
                                    Modifier
                                        .fillMaxWidth(.66f)
                                        .height(5.dp)
                                        .background(
                                            brush = Brush.horizontalGradient(listOf(SageSignalLime, SageSignalCyan)),
                                            shape = CircleShape,
                                        ),
                                )
                            }
                            Text("识别 → 圈选 → 提问", color = SageSignalLime, fontSize = 10.sp, modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                    1 -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Search, null, tint = SageSignalLime, modifier = Modifier.size(22.dp))
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
                                label = { Text("说话或键盘输入自己的问题") },
                            )
                            IconButton(
                                onClick = questionSpeech.onToggle,
                                modifier = Modifier.padding(start = 7.dp).size(48.dp).background(
                                    if (questionSpeech.isListening) SageSignalCyan else SageMist,
                                    CircleShape,
                                ),
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    if (questionSpeech.isListening) "完成语音问题" else "语音输入问题",
                                    tint = if (questionSpeech.isListening) SageOnSignal else SageSignalCyan,
                                )
                            }
                        }
                        Text(
                            questionSpeech.status,
                            color = if (questionSpeech.isListening) SageSignalCyan else SageMuted,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                        Button(
                            onClick = { onQuestionAsked(customQuestion) },
                            enabled = customQuestion.isNotBlank() && !questionSpeech.isListening,
                            modifier = Modifier.fillMaxWidth().padding(top = 7.dp).height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Icon(Icons.Default.Search, null, Modifier.size(17.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("确认问题并提问", color = SageOnSignal, fontWeight = FontWeight.Bold)
                        }
                    }
                    else -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(color = SageMist, shape = CircleShape) {
                                Icon(Icons.Default.AutoAwesome, null, tint = SageSignalLime, modifier = Modifier.padding(9.dp).size(20.dp))
                            }
                            Column(Modifier.padding(start = 10.dp).weight(1f)) {
                                Text(question, color = SageSignalLime, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text("AI 圈搜解答", color = SageMuted, fontSize = 10.sp)
                            }
                            TextButton(onClick = onClear) { Text("换个区域") }
                        }
                        Text(answer.orEmpty(), color = SageInk, fontSize = 13.sp, lineHeight = 20.sp, modifier = Modifier.padding(top = 10.dp).background(SageMist.copy(alpha = .72f), RoundedCornerShape(15.dp)).padding(12.dp))
                        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (onEvidence != null) {
                                OutlinedButton(onClick = onEvidence, modifier = Modifier.weight(.72f).height(49.dp), shape = RoundedCornerShape(15.dp)) { Text("查看依据") }
                            }
                            Button(onClick = onAdopt, modifier = Modifier.weight(1.28f).height(49.dp), shape = RoundedCornerShape(15.dp)) { Text("保存到游记并返回", color = SageOnSignal, fontWeight = FontWeight.Bold) }
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
        color = SagePanelRaised.copy(alpha = .94f),
        shape = RoundedCornerShape(22.dp),
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 15.dp, vertical = 13.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(8.dp).background(SageSignalLime, CircleShape))
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
                        color = if (index == 0) SageSignalLime.copy(alpha = .16f) else SagePanelSoft,
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
    var isSpeaking by remember { mutableStateOf(false) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    DisposableEffect(context, inspection) {
        if (inspection) return@DisposableEffect onDispose { }
        val engine = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) tts?.language = Locale.SIMPLIFIED_CHINESE
        }
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                mainHandler.post { isSpeaking = true }
            }

            override fun onDone(utteranceId: String?) {
                mainHandler.post { isSpeaking = false }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                mainHandler.post { isSpeaking = false }
            }
        })
        tts = engine
        onDispose {
            engine.stop()
            engine.shutdown()
            tts = null
            isSpeaking = false
        }
    }
    val speechInput = if (inspection) {
        remember { SpeechInputState("预览模式 · 轻触后直接在应用内说话", false, .38f) {} }
    } else {
        rememberRealSpeechInputState(state.voiceTranscript, onVoiceTranscript)
    }
    Box(Modifier.fillMaxSize().background(SageSurface)) {
        AmapParkMap(
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = .38f },
            gesturesEnabled = false,
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .68f)))
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
            BreathingVoiceOrb(
                active = state.isRunning || speechInput.isListening,
                stage = if (speechInput.isListening) AiStage.LISTENING else state.aiStage,
                inputLevel = speechInput.level,
                modifier = Modifier.clickable(
                    enabled = !state.isRunning && !state.resultVisible,
                    onClick = speechInput.onToggle,
                ),
            )
            AnimatedContent(
                targetState = when {
                    speechInput.isListening -> "正在聆听 · 轻触完成"
                    state.isRunning && state.condition != ExperimentCondition.BASELINE -> state.aiStage.label
                    state.isRunning -> "正在处理…"
                    else -> "轻触开始对话"
                },
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
                onTranscriptChanged = onVoiceTranscript,
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
                    isSpeaking = isSpeaking,
                    onSpeak = if (ttsReady) {
                        {
                            if (isSpeaking) {
                                tts?.stop()
                                isSpeaking = false
                            } else {
                                val status = tts?.speak(result.summary, TextToSpeech.QUEUE_FLUSH, null, "sage_replay")
                                if (status == TextToSpeech.ERROR) isSpeaking = false
                            }
                        }
                    } else null,
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
    Box(Modifier.fillMaxSize().background(SageSurface)) {
        MemoryWeaveMotion(
            stage = state.aiStage,
            condition = state.condition,
            photoCount = state.capturedPhotoUris.size,
            voiceCount = state.voiceInteractionCount,
            replanCount = state.replanCount,
        )
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
            FunctionHeader(state, onBack, onResearcherPanel, onCancel)
            if (!state.resultVisible) PromptBubble(state.scenario.participantPrompt)
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
                    voiceCount = state.voiceInteractionCount,
                    replanCount = state.replanCount,
                    routeReplanned = state.routeReplanned,
                    adoptedRoute = state.adoptedRoute,
                    voiceTranscripts = state.voiceTranscripts,
                    voiceTranscript = state.voiceTranscript,
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
        colors = CardDefaults.cardColors(containerColor = SagePanelRaised.copy(alpha = .96f)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp),
    ) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, null, tint = SageSignalLime, modifier = Modifier.size(20.dp))
                Text("正在编织今日旅程", color = SageInk, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp))
            }
            JourneyRoutePreview(
                moments = journeyMomentsForDisplay(state),
                stage = state.aiStage,
                modifier = Modifier.fillMaxWidth().padding(top = 11.dp).height(142.dp),
            )
            listOf(
                Triple(Icons.Default.LocationOn, state.activeRouteName, "路线 · 12 分钟"),
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
                    Surface(color = if (active) SageSignalLime.copy(alpha = .12f) else SagePanelSoft, shape = CircleShape) {
                        Icon(item.first, null, tint = if (active) SageSignalLime else SageMuted, modifier = Modifier.padding(8.dp).size(17.dp))
                    }
                    Column(Modifier.padding(start = 10.dp).weight(1f)) {
                        Text(item.second, color = if (active) SageInk else SageMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(item.third, color = SageMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                    AnimatedVisibility(active, enter = fadeIn() + slideInHorizontally { it / 2 }) {
                        Icon(Icons.Default.Check, null, tint = SageSignalLime, modifier = Modifier.size(17.dp))
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
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(SagePanelSoft)) {
        AmapParkMap(
            contentDescription = null,
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = .42f },
            gesturesEnabled = false,
        )
        Canvas(Modifier.fillMaxSize()) {
            val fullPath = Path().apply {
                moveTo(size.width * .05f, size.height * .78f)
                cubicTo(size.width * .24f, size.height * .38f, size.width * .38f, size.height * .78f, size.width * .57f, size.height * .43f)
                cubicTo(size.width * .70f, size.height * .18f, size.width * .82f, size.height * .18f, size.width * .94f, size.height * .28f)
            }
            drawPath(fullPath, Color.Black.copy(alpha = .78f), style = Stroke(width = 11f, cap = StrokeCap.Round))
            val measure = PathMeasure().apply { setPath(fullPath, false) }
            val visiblePath = Path()
            measure.getSegment(0f, measure.length * routeProgress, visiblePath, true)
            drawPath(visiblePath, SageSignalLime, style = Stroke(width = 6f, cap = StrokeCap.Round))
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
                        .border(3.dp, SageSignalLime, CircleShape),
                )
            }
        }
        Surface(color = SagePanel.copy(alpha = .94f), shape = RoundedCornerShape(10.dp), modifier = Modifier.align(Alignment.TopStart).padding(9.dp)) {
            Text("路线正在串联 ${moments.size} 个发现", color = Color.White, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
        }
    }
}

@Composable
private fun JourneyRoutePanel(
    result: AiTaskResult,
    moments: List<JourneyPhotoMoment>,
    voiceCount: Int,
    replanCount: Int,
    routeReplanned: Boolean,
    adoptedRoute: RouteChoice,
    voiceTranscripts: List<String>,
    voiceTranscript: String,
    showUncertainty: Boolean,
    onPrimary: () -> Unit,
    onEvidence: (() -> Unit)?,
    onReset: () -> Unit,
    onShare: () -> Unit,
) {
    val inspection = LocalInspectionMode.current
    var selectedIndex by rememberSaveable(moments.size) { mutableIntStateOf(0) }
    // 真机始终从拾景纸刊进入；截图测试用未采用改线场景覆盖知识路线视图。
    var zineMode by rememberSaveable(inspection, routeReplanned) {
        mutableStateOf(!inspection || routeReplanned)
    }
    LaunchedEffect(moments.size) {
        selectedIndex = selectedIndex.coerceIn(0, (moments.lastIndex).coerceAtLeast(0))
    }
    Surface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        color = SagePanel.copy(alpha = .99f),
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
                Column(Modifier.weight(1f)) {
                    Text(if (zineMode) "今天的拾景纸刊" else "今天走过的知识路线", color = SageInk, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
                    Text(if (zineMode) "真实照片作锚，让沿途发现长成一页纸上风景" else "点击沿线照片，回看当时的问题与回答", color = SageMuted, fontSize = 11.sp)
                }
                IconButton(onClick = onShare, modifier = Modifier.background(SageMist, CircleShape)) {
                    Icon(Icons.Default.Share, "分享知识游记", tint = SageSignalCyan)
                }
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BubbleChoice(
                    selected = zineMode,
                    onClick = { zineMode = true },
                    variant = 0,
                    tint = SagePanelRaised,
                    modifier = Modifier.weight(1f),
                ) { Text("拾景纸刊", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                BubbleChoice(
                    selected = !zineMode,
                    onClick = { zineMode = false },
                    variant = 1,
                    tint = SagePanelRaised,
                    modifier = Modifier.weight(1f),
                ) { Text("知识路线", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
            }
            if (zineMode) {
                ScrapbookJournal(
                    moments = moments,
                    landmarks = ParkRoute.LANDMARKS,
                    stats = JourneyStats(
                        routeName = ParkRoute.NAME,
                        totalMinutes = ParkRoute.TOTAL_MINUTES,
                        distanceMeters = ParkRoute.TOTAL_DISTANCE_METERS,
                        photoCount = moments.size,
                        questionCount = moments.sumOf { it.questions.size },
                        voiceCount = voiceCount,
                        replanCount = replanCount,
                        dateLabel = "八家郊野公园 · 南园",
                    ),
                    selectedIndex = selectedIndex,
                    onMomentSelected = { selectedIndex = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
            } else {
                JourneyRouteMap(
                    moments = moments,
                    voiceCount = voiceCount,
                    replanCount = replanCount,
                    routeReplanned = routeReplanned,
                    adoptedRoute = adoptedRoute,
                    selectedIndex = selectedIndex,
                    onSelected = { selectedIndex = it },
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(238.dp),
                )
                if (voiceCount > 0 || replanCount > 0) {
                Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (voiceCount > 0) {
                        JourneyMaterialSummary(
                            icon = Icons.Default.Mic,
                            title = "$voiceCount 次语音发现",
                            detail = voiceTranscripts.lastOrNull().orEmpty().ifBlank { voiceTranscript.ifBlank { "沿途语音问答" } }.take(22),
                            accent = SageSignalCyan,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (replanCount > 0) {
                        JourneyMaterialSummary(
                            icon = Icons.AutoMirrored.Filled.AltRoute,
                            title = if (routeReplanned) "$replanCount 次路线调整" else "$replanCount 次重规划建议",
                            detail = if (routeReplanned) "节点已落在当前高德路线" else "建议已记录 · 不叠加虚拟线路",
                            accent = SageSignalCoral,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                }
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
                Surface(color = SagePanelSoft, shape = RoundedCornerShape(17.dp), modifier = Modifier.fillMaxWidth().padding(top = 11.dp)) {
                    Text("这次旅程还没有照片节点。下次可在探索途中拍照圈搜，照片和问题会自动落到路线上。", color = SageMuted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(14.dp))
                }
                }
            }
            if (showUncertainty && result.uncertainty != null) {
                Text(
                    result.uncertainty,
                    color = SageSignalCoral,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 10.dp).background(SageWarningSurface, RoundedCornerShape(12.dp)).padding(9.dp),
                )
            }
            Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onEvidence != null) {
                    OutlinedButton(onClick = onEvidence, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(15.dp)) { Text("生成依据") }
                }
                OutlinedButton(onClick = onReset, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(15.dp)) { Text("重新编排") }
            }
            Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(53.dp), shape = RoundedCornerShape(17.dp)) {
                Text(result.primaryAction, fontSize = 15.sp, color = SageOnSignal, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun JourneyRouteMap(
    moments: List<JourneyPhotoMoment>,
    voiceCount: Int,
    replanCount: Int,
    routeReplanned: Boolean,
    adoptedRoute: RouteChoice,
    selectedIndex: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier.clip(sageBubbleShape(6)).background(SagePanelSoft)) {
        AmapParkMap(
            contentDescription = "本次高德真实步行路线与沿途节点",
            modifier = Modifier.fillMaxSize(),
            selectedAlternative = routeReplanned || adoptedRoute == RouteChoice.ALTERNATIVE,
            showAlternativeRoutes = false,
            journeyPhotoUris = moments.map(JourneyPhotoMoment::photoUri),
            journeyVoiceCount = voiceCount,
            journeyReplanCount = replanCount,
            selectedJourneyPhotoIndex = selectedIndex,
            onJourneyPhotoSelected = onSelected,
            gesturesEnabled = false,
            useNightStyle = true,
        )
        Surface(color = SagePanel.copy(alpha = .94f), shape = RoundedCornerShape(10.dp), modifier = Modifier.align(Alignment.TopStart).padding(10.dp)) {
            Row(Modifier.padding(horizontal = 8.dp, vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(6.dp).background(SageSignalLime, CircleShape))
                Text(
                    "高德真实路线 · ${moments.size} 照片 · $voiceCount 语音 · $replanCount 次调整",
                    color = Color.White,
                    fontSize = 9.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun JourneyMaterialSummary(
    icon: ImageVector,
    title: String,
    detail: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = accent.copy(alpha = .11f),
        shape = sageBubbleShape(if (accent == SageOchre) 5 else 2),
        border = androidx.compose.foundation.BorderStroke(1.dp, SageDivider),
        shadowElevation = 2.dp,
        modifier = modifier,
    ) {
        Row(Modifier.padding(horizontal = 11.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = accent, contentColor = SageOnSignal, shape = CircleShape) {
                Icon(icon, null, modifier = Modifier.padding(7.dp).size(16.dp))
            }
            Column(Modifier.padding(start = 9.dp)) {
                Text(title, color = SageInk, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(detail, color = SageMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
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
    Surface(
        color = SagePanelRaised,
        shape = sageBubbleShape(4),
        border = androidx.compose.foundation.BorderStroke(1.dp, SageDivider),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center) {
                    Box(Modifier.size(54.dp).background(SageGreen.copy(alpha = .14f), sageBubbleShape(1)))
                    JourneyPhotoImage(moment.photoUri, moment.label, Modifier.size(44.dp).border(2.dp, SageSignalLime, CircleShape))
                }
                Column(Modifier.padding(start = 10.dp).weight(1f)) {
                    Text(moment.label, color = SageInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text("第 $position 个沿途发现 · 共 $total 个节点", color = SageMuted, fontSize = 10.sp)
                }
                Surface(color = SageSignalLime.copy(alpha = .14f), shape = RoundedCornerShape(100.dp)) {
                    Text("${moment.questions.size} 问", color = SageSignalLime, fontSize = 9.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp))
                }
            }
            if (moment.questions.isEmpty()) {
                Text(
                    "这张照片已经落在路线节点上；当时没有继续提问。",
                    color = SageMuted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 11.dp).background(SagePanelSoft, sageBubbleShape(3)).padding(11.dp),
                )
            } else {
                moment.questions.forEachIndexed { index, item ->
                    Surface(
                        color = if (index % 2 == 0) SagePanelSoft else SageWarningSurface,
                        shape = sageBubbleShape(index + 7),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    ) {
                        Column(Modifier.padding(11.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(color = if (index % 2 == 0) SageSignalLime else SageSignalCoral, shape = CircleShape) {
                                    Text("Q${index + 1}", color = SageOnSignal, fontSize = 9.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp))
                                }
                                Text(item.question, color = if (index % 2 == 0) SageSignalLime else SageSignalCoral, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 8.dp).weight(1f))
                            }
                            Text(item.answer, color = SageInk, fontSize = 11.sp, lineHeight = 17.sp, modifier = Modifier.padding(top = 7.dp))
                        }
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
    onRequestChanged: (String) -> Unit,
    onResearcherPanel: () -> Unit,
    onBack: () -> Unit,
) {
    val inspection = LocalInspectionMode.current
    val requestSpeech = if (inspection) {
        remember { SpeechInputState("预览模式 · 可语音或手动描述变化", false, .2f) {} }
    } else {
        rememberRealSpeechInputState(state.replanRequestText, onRequestChanged)
    }
    Box(Modifier.fillMaxSize().background(SageSurface)) {
        AmapParkMap(
            contentDescription = "动态路线地图",
            modifier = Modifier.fillMaxSize(),
            // 在重规划任务中“推荐”代表新的绕行方案，“备选”才是保留旧路线。
            selectedAlternative = state.selectedRoute == RouteChoice.RECOMMENDED,
            showRouteSummary = false,
        )
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .42f)))
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp)) {
            FunctionHeader(state, onBack, onResearcherPanel, onCancel)
            if (state.isRunning || state.resultVisible) {
                PromptBubble(state.replanRequestText.ifBlank { state.scenario.participantPrompt })
            }
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
                        color = SageSignalCoral,
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
            ReplanInputCard(
                request = state.replanRequestText,
                speech = requestSpeech,
                onRequestChanged = onRequestChanged,
                onRun = onRun,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
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
private fun ReplanInputCard(
    request: String,
    speech: SpeechInputState,
    onRequestChanged: (String) -> Unit,
    onRun: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val suggested = "前方临时封路，而且快下雨了，帮我调整路线"
    FrostedGlassSurface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        tint = SagePanelRaised,
        modifier = modifier.fillMaxWidth().imePadding(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 18.dp)) {
            Text("发生了什么变化？", fontSize = 21.sp, fontWeight = FontWeight.SemiBold, color = SageInk)
                Text("可以说话、手动输入，或轻触下面的示例。", color = SageMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
            FilterChip(
                selected = request == suggested,
                onClick = { onRequestChanged(suggested) },
                label = { Text("示例：封路且即将下雨", fontSize = 11.sp) },
                modifier = Modifier.padding(top = 9.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = request,
                    onValueChange = onRequestChanged,
                    label = { Text("描述路线变化") },
                    placeholder = { Text("例如：前面太拥挤了") },
                    singleLine = true,
                    shape = RoundedCornerShape(15.dp),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = speech.onToggle,
                    modifier = Modifier.padding(start = 7.dp).size(48.dp).background(
                        if (speech.isListening) SageSignalCyan else SageMist,
                        CircleShape,
                    ),
                ) {
                    Icon(Icons.Default.Mic, "语音描述变化", tint = if (speech.isListening) SageOnSignal else SageSignalCyan)
                }
            }
            Text(speech.status, color = if (speech.isListening) SageSignalCyan else SageMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
            Button(
                enabled = request.isNotBlank() && !speech.isListening,
                onClick = onRun,
                modifier = Modifier.fillMaxWidth().padding(top = 9.dp).height(52.dp),
                shape = RoundedCornerShape(17.dp),
            ) {
                Text("确认变化并调整路线", fontSize = 15.sp, color = SageOnSignal, fontWeight = FontWeight.Bold)
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
    Surface(color = SagePanelRaised.copy(alpha = .96f), shape = RoundedCornerShape(24.dp), shadowElevation = 10.dp, modifier = Modifier.fillMaxWidth()) {
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
            CandidateRouteRow("原路线", "封闭点前停止 · 可回退", (1f - progress * .58f).coerceAtLeast(.28f), SageSignalCoral, Modifier.padding(top = 13.dp))
            CandidateRouteRow("新路线", "+4 分钟 · 经过 2 处连廊", progress, SageSignalLime, Modifier.padding(top = 11.dp))
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
    isSpeaking: Boolean = false,
    onShare: (() -> Unit)? = null,
    photoUris: List<String> = emptyList(),
) {
    FrostedGlassSurface(
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        tint = SagePanelRaised,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 22.dp, vertical = 20.dp)) {
            ResultSourceNote(result)
            VoiceAnswerScene(result)
            Surface(
                color = SagePanelSoft,
                shape = sageBubbleShape(3),
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            ) {
                Text(
                    result.summary,
                    color = SageInk.copy(alpha = .82f),
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
                )
            }
            if (photoUris.isNotEmpty()) {
                Surface(color = SagePanelSoft, shape = RoundedCornerShape(15.dp), modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
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
                    color = SageSignalCoral,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 10.dp).background(SageWarningSurface, RoundedCornerShape(12.dp)).padding(10.dp),
                )
            }
            Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth().padding(top = 12.dp).height(54.dp), shape = RoundedCornerShape(17.dp)) {
                Text(result.primaryAction, fontSize = 16.sp, color = SageOnSignal, fontWeight = FontWeight.Bold)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (onSpeak != null) TextButton(onClick = onSpeak) {
                    Icon(
                        if (isSpeaking) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        null,
                        Modifier.size(17.dp),
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(if (isSpeaking) "关闭语音" else "播放语音")
                }
                if (onShare != null) TextButton(onClick = onShare) { Icon(Icons.Default.Share, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("分享") }
                if (onEvidence != null) TextButton(onClick = onEvidence) { Text("查看依据") }
                TextButton(onClick = onReset) { Text("重新开始") }
            }
        }
    }
}

/**
 * 把语音 Agent 的回答呈现成一张“小路线发现板”，避免结果只剩一段说明文字。
 * 结构化指标优先来自工具结果；离线问答则把证据拆成地点、距离、建议等短标签。
 */
@Composable
private fun VoiceAnswerScene(result: AiTaskResult) {
    val tokens = remember(result) { resultVisualTokens(result) }
    Surface(
        color = SagePanelSoft,
        shape = sageBubbleShape(1),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth().height(154.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                val route = Path().apply {
                    moveTo(size.width * .08f, size.height * .72f)
                    cubicTo(
                        size.width * .28f,
                        size.height * .90f,
                        size.width * .48f,
                        size.height * .38f,
                        size.width * .86f,
                        size.height * .30f,
                    )
                }
                drawPath(
                    route,
                    SageSignalCyan.copy(alpha = .46f),
                    style = Stroke(
                        width = 2.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
                    ),
                )
                drawCircle(SageSignalCyan.copy(alpha = .26f), 10.dp.toPx(), Offset(size.width * .08f, size.height * .72f))
                drawCircle(SageSignalCyan, 5.dp.toPx(), Offset(size.width * .08f, size.height * .72f))
                drawCircle(SageSignalLime.copy(alpha = .28f), 13.dp.toPx(), Offset(size.width * .86f, size.height * .30f))
                drawCircle(SageSignalLime, 4.dp.toPx(), Offset(size.width * .86f, size.height * .30f))
            }
            Row(
                Modifier.fillMaxWidth().padding(start = 14.dp, end = 12.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(color = SageSignalCyan, contentColor = SageOnSignal, shape = CircleShape) {
                    Icon(Icons.Default.LocationOn, null, Modifier.padding(7.dp).size(17.dp))
                }
                Text(
                    result.title.removePrefix("推荐："),
                    color = SageInk,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 9.dp).weight(1f),
                )
                Text("沿途发现", color = SageSignalCyan, fontSize = 9.sp, letterSpacing = .6.sp)
            }
            Row(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                tokens.take(3).forEachIndexed { index, token ->
                    Surface(
                        color = listOf(SagePanelRaised, Color(0xFF10201F), Color(0xFF241614))[index % 3],
                        shape = sageBubbleShape(index + 4),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(Modifier.padding(horizontal = 8.dp, vertical = 7.dp)) {
                            Text(token.first, color = SageInk, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(token.second, color = SageMuted, fontSize = 8.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 2.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun resultVisualTokens(result: AiTaskResult): List<Pair<String, String>> {
    if (result.metrics.isNotEmpty()) return result.metrics.take(3).map { it.title to it.detail }
    val evidenceTokens = result.evidence
        .asSequence()
        .filterNot { it.startsWith("语音转写") || it.startsWith("Agent 策略") }
        .map { item ->
            val parts = item.split('：', ':', limit = 2)
            parts.first().take(8) to parts.getOrElse(1) { "已核对" }.take(14)
        }
        .take(3)
        .toList()
    return (evidenceTokens + listOf(
        "方向" to "沿路线继续",
        "距离" to "现场核对",
        "提示" to "留意标牌",
    )).take(3)
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
                color = if (result.isLiveData) SageSignalCyan else SageSignalCoral,
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
            color = SagePanelRaised,
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
        Image(
            painter = painterResource(R.drawable.ip_ginkgo_guide),
            contentDescription = "银小叶庆祝体验完成",
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(148.dp),
        )
        Text(
            "银小叶陪你完成了探索",
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
            colors = CardDefaults.cardColors(containerColor = SagePanelRaised),
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
                        Icon(Icons.Default.Check, null, tint = SageSignalLime, modifier = Modifier.size(19.dp))
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
            Text("重新体验完整流程", fontSize = 16.sp, color = SageOnSignal, fontWeight = FontWeight.Bold)
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
        containerColor = SagePanel,
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
                        tint = SageSignalLime,
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
                            Modifier.size(28.dp).background(SageSignalLime, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("${index + 1}", color = SageOnSignal, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.width(11.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                when (index) {
                                    0 -> "主要线索"
                                    1 -> "交叉验证"
                                    else -> "补充依据"
                                },
                                color = SageSignalLime,
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
                Text("完成查看", color = SageOnSignal, fontWeight = FontWeight.Bold)
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
    onExportCurrentZip: () -> Unit,
    onExportAll: () -> Unit,
    onRecordMisoperation: () -> Unit,
    onFinishSession: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = SagePanel) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Settings, null, tint = SageSignalLime)
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
                Button(onClick = { onDismiss(); onRun() }, modifier = Modifier.weight(1f)) { Text("运行任务", color = SageOnSignal, fontWeight = FontWeight.Bold) }
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
                OutlinedButton(onClick = onExport, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Share, null); Spacer(Modifier.width(6.dp)); Text("测量 CSV") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 8.dp)) {
                OutlinedButton(onClick = onRecordMisoperation, modifier = Modifier.weight(1f)) {
                    Text("补记误操作 +1")
                }
                OutlinedButton(onClick = onExportCurrentZip, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Archive, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("本次 ZIP")
                }
            }
            Text(
                "当前任务：误操作 ${state.taskMisoperationCount} 次 · 尝试 ${state.taskAttemptCount} 次",
                color = SageMuted,
                fontSize = 11.sp,
                modifier = Modifier.padding(top = 7.dp),
            )
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
