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
    VISUAL(
        id = "B1",
        phaseLabel = "游玩中 · 视觉探索",
        title = "识别眼前的花",
        participantPrompt = "这是什么花？",
        actionLabel = "开始识别",
    ),
    VOICE(
        id = "B2",
        phaseLabel = "游玩中 · 语音陪伴",
        title = "边走边问",
        participantPrompt = "这附近有什么适合拍照的地方？",
        actionLabel = "开始语音提问",
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
    UNCERTAIN("uncertain", "结果可能不完整"),
    COMPLETE("complete", "已完成"),
    ERROR("error", "暂时无法完成"),
}

enum class RouteChoice(val logValue: String) {
    RECOMMENDED("recommended"),
    ALTERNATIVE("alternative"),
}

data class ExperimentUiState(
    val sessionStarted: Boolean = false,
    val participantId: String = "",
    val order: ConditionOrder = ConditionOrder.ABC,
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
) {
    val condition: ExperimentCondition
        get() = order.conditions[conditionIndex]

    val conditionProgress: String
        get() = "${conditionIndex + 1} / ${order.conditions.size}"
}

fun stageSequenceFor(scenario: ExperimentScenario): List<AiStage> = when (scenario) {
    ExperimentScenario.ENVIRONMENT -> listOf(
        AiStage.ACTIVATING,
        AiStage.LOCATING,
        AiStage.REASONING,
        AiStage.UNCERTAIN,
        AiStage.COMPLETE,
    )

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
}

object ExperimentTiming {
    const val ACTIVATION_MS = 650L
    const val INPUT_MS = 900L
    const val REASONING_MS = 1_350L
    const val UNCERTAINTY_MS = 900L
    const val COMPLETION_MS = 500L

    fun delayFor(stage: AiStage): Long = when (stage) {
        AiStage.ACTIVATING -> ACTIVATION_MS
        AiStage.LOCATING, AiStage.LISTENING, AiStage.RECOGNIZING -> INPUT_MS
        AiStage.REASONING, AiStage.RESPONDING -> REASONING_MS
        AiStage.UNCERTAIN -> UNCERTAINTY_MS
        AiStage.COMPLETE -> COMPLETION_MS
        else -> 400L
    }
}

