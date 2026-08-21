package cn.tsinghua.sagemotion

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.AiTaskResult
import cn.tsinghua.sagemotion.model.ConditionOrder
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.ExperimentUiState
import cn.tsinghua.sagemotion.model.SessionSummary
import cn.tsinghua.sagemotion.model.VisionFinding
import cn.tsinghua.sagemotion.model.JourneyPhotoMoment
import cn.tsinghua.sagemotion.model.JourneyQuestion
import cn.tsinghua.sagemotion.model.TaskPerformance
import cn.tsinghua.sagemotion.model.RouteChoice
import cn.tsinghua.sagemotion.ui.screens.ExperimentScreen
import cn.tsinghua.sagemotion.ui.screens.HistoryScreen
import cn.tsinghua.sagemotion.ui.screens.PostTaskSurveyScreen
import cn.tsinghua.sagemotion.ui.screens.WelcomeScreen
import cn.tsinghua.sagemotion.ui.theme.SageMotionTheme

@Preview(name = "Welcome with IP", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun WelcomeWithIpScreenshot() {
    SageMotionTheme {
        WelcomeScreen(participantId = "P001", onEnter = {})
    }
}

@Preview(name = "Demo Complete with IP", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun DemoCompleteWithIpScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                demoCompleted = true,
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "Post-task Survey", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun PostTaskSurveyScreenshot() {
    SageMotionTheme {
        PostTaskSurveyScreen(
            performance = TaskPerformance(
                taskInstance = 3,
                scenario = ExperimentScenario.VISUAL,
                completionTimeMs = 5_240,
                decisionTimeMs = 2_180,
                misoperationCount = 0,
                attemptCount = 1,
            ),
            onSubmit = {},
        )
    }
}

@Preview(name = "SAGE Full Route Result", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SageFullRouteResultScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.ENVIRONMENT,
                aiStage = AiStage.COMPLETE,
                resultVisible = true,
                taskResult = AiTaskResult("推荐：湖边林荫线", "沿湖步行，途经多个休息点。", "后半段遮阴信息不足", "开始路线 · 下一步", listOf("地图数据", "设施数据")),
            ),
            onRunScenario = {},
            onCancel = {},
            onReset = {},
            onRestartDemo = {},
            onFinishSession = {},
            onAdopt = {},
            onEvidence = {},
            onCloseEvidence = {},
            onRouteSelected = {},
            onScenarioSelected = {},
            onConditionSelected = {},
            onNextCondition = {},
            onPreviewPrevious = {},
            onPreviewNext = {},
            onResearcherPanel = {},
            onHistory = {},
            onExport = {},
            onExportAll = {},
            onVoiceTranscript = {}, onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "SAGE Dual Route Calculating", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SageDualRouteCalculatingScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.ENVIRONMENT,
                aiStage = AiStage.REASONING,
                isRunning = true,
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "SAGE Full Visual Result", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SageFullVisualResultScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.VISUAL,
                aiStage = AiStage.COMPLETE,
                resultVisible = true,
                taskResult = AiTaskResult("识别结果：月季", "较可能是丰花月季，叶缘与花瓣形态匹配。", "置信度 78%", "保存结果 · 下一步", listOf("花瓣特征", "叶片特征")),
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {},
            onEvidence = {}, onCloseEvidence = {}, onRouteSelected = {},
            onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {},
            onResearcherPanel = {}, onHistory = {}, onExport = {}, onExportAll = {},
            onVoiceTranscript = {}, onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "SAGE Visual Signal Extraction", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SageVisualSignalExtractionScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.VISUAL,
                aiStage = AiStage.RECOGNIZING,
                isRunning = true,
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "Semantic Voice Listening", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SemanticVoiceListeningScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 1,
                scenario = ExperimentScenario.VOICE,
                aiStage = AiStage.LISTENING,
                isRunning = true,
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {},
            onEvidence = {}, onCloseEvidence = {}, onRouteSelected = {},
            onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {},
            onResearcherPanel = {}, onHistory = {}, onExport = {}, onExportAll = {},
            onVoiceTranscript = {}, onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "SAGE Creation Weaving", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SageCreationWeavingScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.CREATE,
                aiStage = AiStage.GENERATING,
                isRunning = true,
                journeyPhotoMoments = sampleJourneyMoments(),
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

private fun sampleJourneyMoments() = listOf(
    JourneyPhotoMoment("", "湖边拱桥", listOf(JourneyQuestion("它有什么特点？", "曲线桥身和水面倒影形成了清晰的空间层次。"))),
    JourneyPhotoMoment("", "林下植物", listOf(JourneyQuestion("这是什么？", "较可能是适应林下光照环境的常见观赏植物。"), JourneyQuestion("为什么种在这里？", "这里湿度和遮阴条件更适合它生长。"))),
    JourneyPhotoMoment("", "休息平台", emptyList()),
)

@Preview(name = "Journey Route Story", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun JourneyRouteStoryScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.CREATE,
                aiStage = AiStage.COMPLETE,
                resultVisible = true,
                journeyPhotoMoments = sampleJourneyMoments(),
                voiceTranscript = "这附近有什么适合拍照的地方？",
                voiceInteractionCount = 3,
                voiceTranscripts = listOf("附近哪里适合拍照？", "湖边有洗手间吗？", "下一处座椅在哪里？"),
                replanCount = 1,
                routeReplanned = true,
                taskResult = AiTaskResult("知识游记已生成", "路线、照片与沿途问题已经串联完成。", "部分识别结论需要结合现场标牌核查", "完成本次体验", listOf("路线记录", "照片问答")),
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "Journey Proposed Replan", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun JourneyProposedReplanScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.CREATE,
                aiStage = AiStage.COMPLETE,
                resultVisible = true,
                journeyPhotoMoments = sampleJourneyMoments(),
                voiceTranscript = "附近哪里有好吃的？",
                voiceInteractionCount = 1,
                voiceTranscripts = listOf("附近哪里有好吃的？"),
                replanCount = 1,
                routeReplanned = false,
                taskResult = AiTaskResult("知识游记已生成", "路线、照片与沿途问题已经串联完成。", "橙色虚线表示比较过但未采用的改道建议", "完成本次体验", listOf("路线记录", "照片问答")),
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "SAGE Dynamic Replanning", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SageDynamicReplanningScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.ADJUST,
                aiStage = AiStage.REPLANNING,
                isRunning = true,
                replanRequestText = "前方临时封路，而且快下雨了，帮我调整路线",
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "Parallel Exploration Hub", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun ParallelExplorationHubScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.EXPLORE,
                aiStage = AiStage.COMPLETE,
                adoptedRoute = RouteChoice.ALTERNATIVE,
                completedTaskCount = 5,
                visualInteractionCount = 2,
                voiceInteractionCount = 2,
                replanCount = 1,
                routeReplanned = true,
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "Alternative Route Exploration Hub", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun AlternativeRouteExplorationHubScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.EXPLORE,
                aiStage = AiStage.COMPLETE,
                adoptedRoute = RouteChoice.ALTERNATIVE,
                completedTaskCount = 2,
                visualInteractionCount = 1,
                voiceInteractionCount = 1,
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "Route Constraint Input", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun RouteConstraintInputScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.ENVIRONMENT,
                routePreferenceIds = setOf("shade", "rest", "quiet"),
                routeConstraintText = "不要台阶，想经过湖边",
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "Circle Search Answer", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun CircleSearchAnswerScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.VISUAL,
                aiStage = AiStage.COMPLETE,
                resultVisible = true,
                visionFindings = listOf(VisionFinding("拱桥", .88f), VisionFinding("水面", .81f), VisionFinding("树木", .76f)),
                visualQuestion = "它有什么特点？",
                visualAnswer = "拱桥的曲线结构与水面倒影形成了清晰的视觉特征，建议把周围环境一起记录进知识游记。",
                taskResult = AiTaskResult("多点识别完成", "已形成画面语义候选。", "图像识别仍可能有偏差", "保存", listOf("端侧图像标签", "用户圈选范围")),
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "Circle Search Voice Question", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun CircleSearchVoiceQuestionScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.VISUAL,
                aiStage = AiStage.COMPLETE,
                resultVisible = true,
                visionFindings = listOf(VisionFinding("粉红色花卉", .88f), VisionFinding("叶片", .81f)),
                taskResult = AiTaskResult("多点识别完成", "已形成画面语义候选。", "图像识别仍可能有偏差", "保存", listOf("端侧图像标签")),
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "Voice Manual Input", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun VoiceManualInputScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.VOICE,
                aiStage = AiStage.IDLE,
                voiceTranscript = "附近哪里有好吃的？",
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "Replan Request Input", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun ReplanRequestInputScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.ADJUST,
                aiStage = AiStage.IDLE,
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onRestartDemo = {}, onFinishSession = {}, onEvidence = {}, onCloseEvidence = {},
            onRouteSelected = {}, onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onPreviewPrevious = {}, onPreviewNext = {}, onResearcherPanel = {}, onHistory = {},
            onExport = {}, onExportAll = {}, onVoiceTranscript = {}, onReplanRequestChanged = {},
            onCreatePhotoUri = { android.net.Uri.EMPTY }, onPhotoCaptured = {}, onShareJourney = {}, onBeginJourneySummary = {},
        )
    }
}

@Preview(name = "History List", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun HistoryListScreenshot() {
    SageMotionTheme {
        HistoryScreen(
            sessions = listOf(
                SessionSummary("sage_P001.csv", "P001", 1_784_342_000_000, 1_784_342_300_000, 300_000, "ABC", 26, 3, 3, true, 8_240),
                SessionSummary("sage_P002.csv", "P002", 1_784_250_000_000, 1_784_250_120_000, 120_000, "BCA", 11, 1, 0, false, 3_180),
            ),
            selectedDetail = null,
            onBack = {},
            onSelect = {},
            onCloseDetail = {},
            onExportSession = {},
            onShareJourney = {},
            onExportAll = {},
            onDeleteSession = {},
            onDeleteAll = {},
        )
    }
}
