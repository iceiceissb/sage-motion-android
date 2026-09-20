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

    // 两条件对照。评审建议：正式实验可能只保留「对照基线 VS 我们的设计」两个条件，
    // 招募时就排好顺序，主试在设置页核对即可。六种拉丁方顺序仍然保留，不做删减。
    AC("同应用基线 → SAGE", listOf(ExperimentCondition.BASELINE, ExperimentCondition.SAGE_FULL)),
    CA("SAGE → 同应用基线", listOf(ExperimentCondition.SAGE_FULL, ExperimentCondition.BASELINE)),
    ;

    /** 两条件对照组，用于设置页分组显示。 */
    val isPairedComparison: Boolean get() = conditions.size == 2
}

/**
 * 沿途地标的呈现形态。
 *
 * 对应设计建议便签：「看看地标是 1. 数字形式 2. icon形式 3. 3D效果
 * （访谈中有人建议这样和圆周旅迹区分开来）」。
 * 三种都实现并可在研究员设置页切换，默认用带高度的 [DEPTH]。
 */
enum class LandmarkStyle(val label: String, val description: String) {
    NUMBER("数字标注", "圆周旅迹式的顺序编号，读顺序最快"),
    ICON("图标标注", "用场地类型图形区分，读语义最快"),
    DEPTH("立体标注", "带底座与投影的立体锚点，和平面标注拉开层次"),
}

/**
 * 八家郊野公园南园的沿途点位。
 *
 * 南园分为管理服务中心、康体健身步道、综合运动区、儿童活动区和生态湖区五个区；
 * 公园在提升建设中突出戏曲文化主题，并大量应用北京地区乡土宿根花卉。
 * 这些点位既用于 A 的路线标注，也用于 C 的手账拼贴节点。
 *
 * @param routeFraction 该点位在主推荐路线上的位置比例（0 起点，1 终点）。
 */
data class ParkLandmark(
    val id: String,
    val name: String,
    val zone: String,
    val glyph: LandmarkGlyph,
    val routeFraction: Float,
    val minutesFromStart: Int,
    val note: String,
)

/** 地标图形。用形状而不是颜色区分类型，满足「状态不只依赖颜色」的无障碍约束。 */
enum class LandmarkGlyph { GATE, WATER, GROVE, STAGE, REST, PLAY }

object ParkRoute {
    const val NAME = "湖边林荫线"
    const val ALTERNATIVE_NAME = "草坪外环线"
    const val START_POINT = "八家郊野公园南园 · 南门"
    const val TOTAL_DISTANCE_METERS = 850
    const val TOTAL_MINUTES = 12

    /** 主推荐路线沿途点位，按路程顺序排列。 */
    val LANDMARKS = listOf(
        ParkLandmark("gate", "南园南门", "管理服务中心", LandmarkGlyph.GATE, .00f, 0, "出发点，公园导览图与饮水点都在这里"),
        ParkLandmark("grove", "乡土宿根花境", "康体健身步道", LandmarkGlyph.GROVE, .22f, 3, "北京乡土宿根花卉群落，四季可观"),
        ParkLandmark("stage", "戏曲文化林地", "康体健身步道", LandmarkGlyph.STAGE, .43f, 5, "南园戏曲主题节点，古曲戏文场景与植物意向结合"),
        ParkLandmark("rest", "林荫休息廊", "综合运动区", LandmarkGlyph.REST, .62f, 8, "连续树荫与座椅，适合中途休息"),
        ParkLandmark("water", "生态湖区观景点", "生态湖区", LandmarkGlyph.WATER, .84f, 10, "南园水面最开阔的一段，逆光时段适合拍照"),
        ParkLandmark("play", "儿童活动区外环", "儿童活动区", LandmarkGlyph.PLAY, 1.00f, 12, "人流较多，作为路线终点便于离园"),
    )
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

/** 三种实验条件共用的任务后 1–7 点量表，题目、顺序和量尺保持完全一致。 */
enum class SurveyDimension(
    val exportId: String,
    val shortLabel: String,
    val statement: String,
    val lowAnchor: String,
    val highAnchor: String,
) {
    STATE_RECOGNITION("state_recognition", "状态识别", "我能判断 AI 当前处于哪个处理阶段。", "完全不能", "完全能"),
    PROCESS_UNDERSTANDING("process_understanding", "过程理解", "我理解 AI 是如何得到刚才这个结果的。", "完全不理解", "完全理解"),
    CALIBRATED_TRUST("calibrated_trust", "校准信任", "我知道刚才的结果在什么情况下可以相信、什么情况下需要核查。", "完全不知道", "非常清楚"),
    PERCEIVED_CONTROL("perceived_control", "可控感", "在刚才的任务中，我可以修改、拒绝或接管 AI 的建议。", "完全不同意", "完全同意"),
    WORKLOAD("workload", "工作负荷", "完成刚才的任务需要我付出很多注意力和思考。", "负荷很低", "负荷很高"),
}

/** 单次任务从启动到作答前的客观行为指标。 */
data class TaskPerformance(
    val taskInstance: Int,
    val scenario: ExperimentScenario,
    val completionTimeMs: Long,
    val decisionTimeMs: Long,
    val misoperationCount: Int,
    val attemptCount: Int,
)

/** 写入会话 CSV 的一次完整任务后测量。 */
data class PostTaskMeasurement(
    val performance: TaskPerformance,
    val ratings: Map<SurveyDimension, Int>,
)

data class ExperimentUiState(
    val sessionStarted: Boolean = false,
    val participantId: String = "",
    val order: ConditionOrder = ConditionOrder.ABC,
    val demoMode: DemoMode = DemoMode.ONLINE_AGENT,
    val landmarkStyle: LandmarkStyle = LandmarkStyle.DEPTH,
    val welcomeShown: Boolean = false,
    val conditionIndex: Int = 0,
    val scenario: ExperimentScenario = ExperimentScenario.ENVIRONMENT,
    val aiStage: AiStage = AiStage.IDLE,
    val isRunning: Boolean = false,
    val resultVisible: Boolean = false,
    val selectedRoute: RouteChoice = RouteChoice.RECOMMENDED,
    /** A 任务中真正采纳的路线。selectedRoute 仅表示当前结果面板里的临时选择。 */
    val adoptedRoute: RouteChoice = RouteChoice.RECOMMENDED,
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
    val replanRequestText: String = "",
    val routePreferenceIds: Set<String> = setOf("shade", "rest"),
    val voiceTranscript: String = "",
    val voiceTranscripts: List<String> = emptyList(),
    val capturedPhotoUri: String? = null,
    val capturedPhotoUris: List<String> = emptyList(),
    val visionFindings: List<VisionFinding> = emptyList(),
    val selectedImageRegion: ImageRegion? = null,
    val selectedImageUri: String? = null,
    val selectedImageFindings: List<VisionFinding> = emptyList(),
    val visualSelectionBusy: Boolean = false,
    val visualAnswerBusy: Boolean = false,
    val visualAnswerSource: String? = null,
    val spatial: JourneySpatialState = JourneySpatialState(),
    val generatedZinePath: String? = null,
    val zineBusy: Boolean = false,
    val zineMessage: String? = null,
    val photoAnalysisStatus: String? = null,
    /** Session-memory only: never persisted, so restored sessions require fresh consent. */
    val cloudVisionUploadApproved: Boolean = false,
    val visualQuestion: String = "",
    val visualAnswer: String? = null,
    val journeyPhotoMoments: List<JourneyPhotoMoment> = emptyList(),
    val voiceInteractionCount: Int = 0,
    val visualInteractionCount: Int = 0,
    val replanCount: Int = 0,
    val routeReplanned: Boolean = false,
    val taskStartedAtMillis: Long = 0L,
    val resultPresentedAtMillis: Long = 0L,
    val taskMisoperationCount: Int = 0,
    val taskAttemptCount: Int = 0,
    val pendingPostTaskSurvey: TaskPerformance? = null,
) {
    val condition: ExperimentCondition
        get() = order.conditions[conditionIndex]

    val activeRouteName: String
        get() = spatial.activeRoute?.name ?: when {
            demoMode == DemoMode.ONLINE_AGENT -> "尚未采纳路线"
            routeReplanned -> "林下连廊绕行线"
            adoptedRoute == RouteChoice.ALTERNATIVE -> ParkRoute.ALTERNATIVE_NAME
            else -> ParkRoute.NAME
        }

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
    val recommendedRouteId: String? = null,
    val alternativeRouteId: String? = null,
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
    val journeyImagePath: String? = null,
    val zineImagePath: String? = null,
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
