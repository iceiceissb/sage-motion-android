package cn.tsinghua.sagemotion.model

enum class ExperimentCondition(
    val id: String,
    val researcherLabel: String,
) {
    BASELINE("C0", "Baseline · 通用反馈"),
    SEMANTIC("C1", "Semantic Motion · 状态语义"),
    SAGE_FULL("C2", "SAGE Full · 语义 + 证据 + 接管"),
}

enum class ConditionOrder(
    val label: String,
    val conditions: List<ExperimentCondition>,
) {
    ABC("A → B → C", listOf(ExperimentCondition.BASELINE, ExperimentCondition.SEMANTIC, ExperimentCondition.SAGE_FULL)),
    ACB("A → C → B", listOf(ExperimentCondition.BASELINE, ExperimentCondition.SAGE_FULL, ExperimentCondition.SEMANTIC)),
    BAC("B → A → C", listOf(ExperimentCondition.SEMANTIC, ExperimentCondition.BASELINE, ExperimentCondition.SAGE_FULL)),
    BCA("B → C → A", listOf(ExperimentCondition.SEMANTIC, ExperimentCondition.SAGE_FULL, ExperimentCondition.BASELINE)),
    CAB("C → A → B", listOf(ExperimentCondition.SAGE_FULL, ExperimentCondition.BASELINE, ExperimentCondition.SEMANTIC)),
    CBA("C → B → A", listOf(ExperimentCondition.SAGE_FULL, ExperimentCondition.SEMANTIC, ExperimentCondition.BASELINE)),
}

enum class ExperimentScenario(
    val id: String,
    val phaseLabel: String,
    val title: String,
    val participantPrompt: String,
    val actionLabel: String,
) {
    ENVIRONMENT(
        id = "A",
        phaseLabel = "到达公园 · 环境感知",
        title = "建立方向感",
        participantPrompt = "我有点累，帮我找一条阴凉、能坐下的路线",
        actionLabel = "帮我规划路线",
    ),
    EXPLORE(
        id = "B",
        phaseLabel = "路线进行中 · 自由探索",
        title = "探索工作台",
        participantPrompt = "路线已启动，可以随时拍照询问、语音对话或重新规划",
        actionLabel = "",
    ),
    VISUAL(
        id = "B1",
        phaseLabel = "游玩中 · 拍照询问",
        title = "识别眼前的事物",
        participantPrompt = "帮我看看这张照片里有什么？",
        actionLabel = "拍照并识别",
    ),
    VOICE(
        id = "B2",
        phaseLabel = "游玩中 · 语音陪伴",
        title = "边走边问",
        participantPrompt = "这附近有什么适合拍照的地方？",
        actionLabel = "开始语音提问",
    ),
    CREATE(
        id = "C",
        phaseLabel = "离园前 · 创作与分享",
        title = "把今天编成一条回忆",
        participantPrompt = "帮我把路线、照片和发现整理成一张知识游记",
        actionLabel = "生成旅程回顾",
    ),
    ADJUST(
        id = "D",
        phaseLabel = "途中变化 · 动态调整",
        title = "根据变化重新规划",
        participantPrompt = "前方临时封路，而且快下雨了，帮我调整路线",
        actionLabel = "动态调整路线",
    ),
}

enum class AiStage(val id: String, val label: String) {
    IDLE("idle", "等待开始"),
    ACTIVATING("activating", "正在唤起"),
    LOCATING("locating", "正在定位"),
    LISTENING("listening", "正在听"),
    RECOGNIZING("recognizing", "正在识别"),
    REASONING("reasoning", "正在比较与推理"),
    RESPONDING("responding", "正在组织回答"),
    SUMMARIZING("summarizing", "正在汇总旅程"),
    GENERATING("generating", "正在生成回顾"),
    EDITING("editing", "正在编排内容"),
    REPLANNING("replanning", "正在重新规划"),
    DECIDING("deciding", "正在权衡方案"),
    UNCERTAIN("uncertain", "结果可能不完整"),
    COMPLETE("complete", "已完成"),
    ERROR("error", "暂时无法完成"),
}

enum class RouteChoice(val logValue: String) {
    RECOMMENDED("recommended"),
    ALTERNATIVE("alternative"),
}

enum class DemoMode(
    val label: String,
    val description: String,
) {
    EXPERIMENT_OFFLINE(
        label = "实验模式 · 离线固定",
        description = "固定刺激与固定时序，适合正式实验和条件比较",
    ),
    ONLINE_AGENT(
        label = "联网 Agent · 生态演示",
        description = "接入实时环境数据；超时会自动使用缓存或离线脚本",
    ),
}

data class ExperimentUiState(
    val sessionStarted: Boolean = false,
    val participantId: String = "",
    val order: ConditionOrder = ConditionOrder.ABC,
    val demoMode: DemoMode = DemoMode.EXPERIMENT_OFFLINE,
    val conditionIndex: Int = 0,
    val scenario: ExperimentScenario = ExperimentScenario.ENVIRONMENT,
    val aiStage: AiStage = AiStage.IDLE,
    val isRunning: Boolean = false,
    val resultVisible: Boolean = false,
    val selectedRoute: RouteChoice = RouteChoice.RECOMMENDED,
    val evidenceVisible: Boolean = false,
    val researcherPanelVisible: Boolean = false,
    val completedTaskCount: Int = 0,
    val statusMessage: String? = null,
    val taskResult: AiTaskResult? = null,
    val completedScenarios: Set<ExperimentScenario> = emptySet(),
    val demoCompleted: Boolean = false,
    val historyVisible: Boolean = false,
    val historySessions: List<SessionSummary> = emptyList(),
    val selectedHistory: SessionDetail? = null,
    val routeConstraintText: String = "",
    val routePreferenceIds: Set<String> = setOf("shade", "rest"),
    val voiceTranscript: String = "",
    val capturedPhotoUri: String? = null,
    val capturedPhotoUris: List<String> = emptyList(),
    val visionFindings: List<VisionFinding> = emptyList(),
    val photoAnalysisStatus: String? = null,
    val visualQuestion: String = "",
    val visualAnswer: String? = null,
    val journeyPhotoMoments: List<JourneyPhotoMoment> = emptyList(),
    val voiceInteractionCount: Int = 0,
    val visualInteractionCount: Int = 0,
    val replanCount: Int = 0,
) {
    val condition: ExperimentCondition
        get() = order.conditions[conditionIndex]

    val conditionProgress: String
        get() = "${conditionIndex + 1} / ${order.conditions.size}"

    val scenarioProgress: String
        get() = when (scenario) {
            ExperimentScenario.ENVIRONMENT -> "开始"
            ExperimentScenario.EXPLORE -> "探索中"
            ExperimentScenario.CREATE -> "完成"
            else -> "工具"
        }
}

data class ResultMetric(
    val title: String,
    val detail: String,
)

data class VisionFinding(
    val label: String,
    val confidence: Float,
)

data class JourneyQuestion(
    val question: String,
    val answer: String,
)

data class JourneyPhotoMoment(
    val photoUri: String,
    val label: String,
    val questions: List<JourneyQuestion> = emptyList(),
)

/** API-neutral result model shared by the offline mock and a future network adapter. */
data class AiTaskResult(
    val title: String,
    val summary: String,
    val uncertainty: String?,
    val primaryAction: String,
    val evidence: List<String>,
    val alternativeTitle: String? = null,
    val metrics: List<ResultMetric> = emptyList(),
    val sourceLabel: String = "离线固定刺激",
    val sourceUrl: String? = null,
    val isLiveData: Boolean = false,
    val attribution: String? = null,
)

data class SessionSummary(
    val fileName: String,
    val participantId: String,
    val startedAtMillis: Long,
    val updatedAtMillis: Long,
    val durationMillis: Long,
    val conditionOrder: String,
    val eventCount: Int,
    val completedTaskCount: Int,
    val adoptedCount: Int,
    val completed: Boolean,
    val sizeBytes: Long,
    val demoMode: String = "LEGACY",
)

data class HistoryEvent(
    val timestampMillis: Long,
    val elapsedMillis: Long,
    val taskId: String,
    val taskPhase: String,
    val aiState: String,
    val event: String,
    val action: String,
    val details: String,
)

data class SessionDetail(
    val summary: SessionSummary,
    val events: List<HistoryEvent>,
)

fun stageSequenceFor(scenario: ExperimentScenario): List<AiStage> = when (scenario) {
    ExperimentScenario.ENVIRONMENT -> listOf(
        AiStage.ACTIVATING,
        AiStage.LOCATING,
        AiStage.REASONING,
        AiStage.UNCERTAIN,
        AiStage.COMPLETE,
    )

    ExperimentScenario.EXPLORE -> listOf(AiStage.ACTIVATING, AiStage.COMPLETE)

    ExperimentScenario.VISUAL -> listOf(
        AiStage.ACTIVATING,
        AiStage.RECOGNIZING,
        AiStage.REASONING,
        AiStage.UNCERTAIN,
        AiStage.COMPLETE,
    )

    ExperimentScenario.VOICE -> listOf(
        AiStage.ACTIVATING,
        AiStage.LISTENING,
        AiStage.REASONING,
        AiStage.RESPONDING,
        AiStage.COMPLETE,
    )

    ExperimentScenario.CREATE -> listOf(
        AiStage.ACTIVATING,
        AiStage.SUMMARIZING,
        AiStage.GENERATING,
        AiStage.EDITING,
        AiStage.COMPLETE,
    )

    ExperimentScenario.ADJUST -> listOf(
        AiStage.ACTIVATING,
        AiStage.LOCATING,
        AiStage.REPLANNING,
        AiStage.DECIDING,
        AiStage.UNCERTAIN,
        AiStage.COMPLETE,
    )
}

fun nextScenarioAfter(scenario: ExperimentScenario): ExperimentScenario? = when (scenario) {
    ExperimentScenario.ENVIRONMENT -> ExperimentScenario.EXPLORE
    ExperimentScenario.VISUAL, ExperimentScenario.VOICE, ExperimentScenario.ADJUST -> ExperimentScenario.EXPLORE
    ExperimentScenario.EXPLORE -> null
    ExperimentScenario.CREATE -> null
}

object ExperimentTiming {
    const val ACTIVATION_MS = 800L
    const val INPUT_MS = 1_200L
    const val REASONING_MS = 1_600L
    const val UNCERTAINTY_MS = 1_000L
    const val COMPLETION_MS = 550L

    fun delayFor(stage: AiStage): Long = when (stage) {
        AiStage.ACTIVATING -> ACTIVATION_MS
        AiStage.LOCATING, AiStage.LISTENING, AiStage.RECOGNIZING -> INPUT_MS
        AiStage.REASONING, AiStage.RESPONDING, AiStage.SUMMARIZING,
        AiStage.GENERATING, AiStage.EDITING, AiStage.REPLANNING, AiStage.DECIDING -> REASONING_MS
        AiStage.UNCERTAIN -> UNCERTAINTY_MS
        AiStage.COMPLETE -> COMPLETION_MS
        else -> 400L
    }
}
