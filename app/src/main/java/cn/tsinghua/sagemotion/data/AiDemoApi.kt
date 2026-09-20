package cn.tsinghua.sagemotion.data

import cn.tsinghua.sagemotion.BuildConfig
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.AiTaskResult
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.ExperimentTiming
import cn.tsinghua.sagemotion.model.ResultMetric
import cn.tsinghua.sagemotion.model.VisionFinding
import cn.tsinghua.sagemotion.model.stageSequenceFor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

data class AiTaskRequest(
    val scenario: ExperimentScenario,
    val prompt: String,
    val visionFindings: List<VisionFinding> = emptyList(),
    /** Included only after the user approves a one-time remote multimodal upload. */
    val visionImageUri: String? = null,
    val journeyContext: AgentJourneyContext = AgentJourneyContext(),
    val hasCapturedPhoto: Boolean = false,
    val visualQuestion: String? = null,
    val visionIsRegion: Boolean = false,
)

/** Minimal, non-identifying journey memory sent to the remote primary agent. */
data class AgentJourneyContext(
    val activeRouteName: String = "湖边林荫线",
    val routeReplanned: Boolean = false,
    val previousVoiceTurns: List<String> = emptyList(),
    val visionLabels: List<String> = emptyList(),
    val photoQuestionCount: Int = 0,
    val visualInteractionCount: Int = 0,
    val voiceInteractionCount: Int = 0,
    val replanCount: Int = 0,
)

sealed interface AiTaskEvent {
    data class StageChanged(val stage: AiStage) : AiTaskEvent
    data class Completed(val result: AiTaskResult) : AiTaskEvent
}

/**
 * Stable boundary for the future AI backend. A Retrofit/Ktor/SSE implementation only
 * needs to emit the same events; the offline APK keeps using [MockAiDemoApi].
 */
interface AiDemoApi {
    fun runTask(request: AiTaskRequest): Flow<AiTaskEvent>
}

class MockAiDemoApi(
    private val waitFor: suspend (Long) -> Unit = { delay(it) },
) : AiDemoApi {
    override fun runTask(request: AiTaskRequest): Flow<AiTaskEvent> = flow {
        stageSequenceFor(request.scenario).forEach { stage ->
            emit(AiTaskEvent.StageChanged(stage))
            waitFor(ExperimentTiming.delayFor(stage))
        }
        emit(AiTaskEvent.Completed(resultFor(request)))
    }

    private fun resultFor(request: AiTaskRequest): AiTaskResult = when (request.scenario) {
        ExperimentScenario.ENVIRONMENT -> AiTaskResult(
            title = "推荐：湖边林荫线",
            alternativeTitle = "备选：草坪外环线",
            summary = "沿湖步行，途经多个休息点。",
            uncertainty = "后半段遮阴信息不足，建议途中留意休息点",
            primaryAction = "开始路线 · 下一步",
            metrics = listOf(
                ResultMetric("12 分钟", "≈ 850 米"),
                ResultMetric("3 处座椅", "沿途可见"),
                ResultMetric("避开拥挤", "当前人流较少"),
            ),
            evidence = listOf(
                "地图：预计步行时间 12 分钟",
                "公园设施：沿途标注 3 处座椅",
                "环境数据：前半段树冠覆盖较高",
                "缺口：后半段遮阴数据更新于 3 天前",
            ),
        )

        ExperimentScenario.EXPLORE -> AiTaskResult(
            title = "探索工作台已就绪",
            summary = "可以随时拍照询问、语音对话、重新规划，最后再生成知识游记。",
            uncertainty = null,
            primaryAction = "返回探索",
            evidence = emptyList(),
        )

        ExperimentScenario.VISUAL -> if (request.hasCapturedPhoto) capturedVisionResult(request) else AiTaskResult(
            title = if (request.visionFindings.isEmpty()) "识别结果：月季" else "端侧图像线索：${request.visionFindings.take(2).joinToString("、") { it.label }}",
            summary = if (request.visionFindings.isEmpty()) {
                "较可能是丰花月季。叶缘与花瓣形态匹配，但单张照片无法确认具体品种。"
            } else {
                "已分析刚拍摄的照片并提取通用视觉标签，识别对象不限定为花卉；结果会作为本次旅程的视觉线索保存。"
            },
            uncertainty = if (request.visionFindings.isEmpty()) "置信度 78% · 品种信息可能不完整" else "通用图像分类可能漏检或误标；精细类别请结合实物与现场说明核查",
            primaryAction = "保存结果 · 下一步",
            evidence = if (request.visionFindings.isEmpty()) listOf(
                "花瓣：重瓣、粉红色",
                "叶片：羽状复叶、叶缘有锯齿",
                "候选：月季 78%，蔷薇 15%",
                "缺口：单张照片无法判断具体品种",
            ) else request.visionFindings.map { "端侧标签：${it.label} · ${(it.confidence * 100).toInt()}%" } +
                "边界：端侧通用模型提供场景与物体类别，不保证细粒度身份识别",
            sourceLabel = if (request.visionFindings.isEmpty()) "离线固定视觉刺激" else "实际拍照 · 端侧 ML Kit",
            isLiveData = request.visionFindings.isNotEmpty(),
        )

        ExperimentScenario.VOICE -> voiceResultFor(request.prompt)

        ExperimentScenario.CREATE -> AiTaskResult(
            title = "已生成：一条会呼吸的公园记忆",
            summary = "AI 已把湖边林荫路线、过程中的视觉发现、语音问答和现场照片编成一张可继续编辑的知识游记。",
            uncertainty = "自动摘要可能遗漏个人感受；分享前可检查照片、地点与文字",
            primaryAction = "保存回顾 · 下一步",
            metrics = listOf(
                ResultMetric("1 条路线", "湖边林荫线"),
                ResultMetric("3 个发现", "地点与知识"),
                ResultMetric("1 张照片", "可替换编辑"),
            ),
            evidence = listOf(
                "路线：850 米 · 3 处休息点",
                "视觉发现：过程照片与端侧识别线索",
                "语音发现：湖心桥东侧拍照点",
                "编辑提醒：分享前检查自动生成内容",
            ),
        )

        ExperimentScenario.ADJUST -> AiTaskResult(
            title = "路线已动态调整",
            alternativeTitle = "保留原路线 · 自行绕行",
            summary = "已避开临时封闭的湖心桥北段，改走连廊与林下步道；预计增加 4 分钟，但遮雨与座椅更多。",
            uncertainty = "封路和降雨来自演示事件；真实通行状态仍需以现场标识为准",
            primaryAction = "采用新路线 · 完成体验",
            metrics = listOf(
                ResultMetric("+4 分钟", "总计约 16 分钟"),
                ResultMetric("避开封路", "绕行 260 米"),
                ResultMetric("2 处连廊", "可临时避雨"),
            ),
            evidence = listOf(
                "变化：湖心桥北段临时封闭",
                "天气：短时降雨概率上升",
                "新路线：增加 260 米，经过 2 处连廊",
                "接管：可保留原路线或采用新路线",
            ),
        )
    }

    private fun capturedVisionResult(request: AiTaskRequest): AiTaskResult {
        val labels = request.visionFindings.take(5)
        val scope = if (request.visionIsRegion) "圈选区域" else "整张照片"
        val label = labels.firstOrNull()?.label
        val summary = when {
            label == null -> "$scope 暂未提取到可靠标签；可以扩大选区、重新拍摄，或授权云端看图。"
            request.visualQuestion == null -> "$scope 提取到：${labels.joinToString("、") { it.label }}。这些是通用类别线索。"
            else -> "关于“${request.visualQuestion}”：$scope 的主要类别线索是“$label”。本地分类模型无法解释细节、成因或精确物种；可授权云端看图继续询问。"
        }
        return AiTaskResult(
            title = if (label == null) "尚无可靠识别结果" else "$scope：$label",
            summary = summary,
            uncertainty = "通用类别标签不是物种或物体身份鉴定",
            primaryAction = "保存结果并返回",
            evidence = labels.map { "端侧类别：${it.label} · ${(it.confidence * 100).toInt()}%" },
            sourceLabel = "$scope · 端侧 ML Kit · 本地解读",
            isLiveData = labels.isNotEmpty(),
        )
    }

    /**
     * 免费、离线且确定性的基础问答。正式条件比较不接入随机大模型，避免回答质量成为混淆变量；
     * 生态演示中的语音转写由当前 APK flavor 提供：完整版使用 Vosk，精简版使用手机语音服务。
     */
    private fun voiceResultFor(rawPrompt: String): AiTaskResult {
        val prompt = rawPrompt.trim()
        val compactPrompt = prompt.replace(Regex("\\s+"), "")
        val response = when {
            listOf("吃", "餐厅", "餐馆", "饭店", "美食", "咖啡", "茶饮", "小吃").any(compactPrompt::contains) -> Triple(
                "附近餐饮：建议查询南门外",
                "我识别到这是附近餐饮需求。离线模式没有实时商户与营业信息，可先从公园南门出园后查看周边餐饮；切换到联网 Agent 后会调用 OpenStreetMap 附近地点工具返回名称和距离。",
                listOf("意图：附近餐饮", "离线边界：不虚构商户名称", "联网工具：OpenStreetMap Nearby"),
            )
            listOf("拍照", "拍照的地方", "好看", "景色", "打卡").any(compactPrompt::contains) -> Triple(
                "推荐：湖心桥东侧",
                "向前约 180 米到湖心桥东侧，那里能把湖面、柳树和远处亭子一起拍进画面；下午侧光会更柔和。",
                listOf("距离：当前位置约 180 米", "景观：湖面、柳树、亭子同框", "建议：保留三分之一环境作为地点线索"),
            )
            listOf("花", "植物", "月季", "蔷薇").any(compactPrompt::contains) -> Triple(
                "附近花境：林荫步道南段",
                "沿林荫步道向南约 120 米有一片宿根花境，常见月季、蔷薇和季节性草花。花木身份请以现场标牌为准。",
                listOf("位置：林荫步道南段", "距离：约 120 米", "边界：语音回答不能替代植物专业鉴定"),
            )
            listOf("厕所", "洗手间", "卫生间").any(compactPrompt::contains) -> Triple(
                "洗手间：南门服务中心",
                "最近的公共洗手间在南门管理服务中心附近，沿当前路线返回约 6 分钟。现场指示牌的信息最可靠。",
                listOf("设施：南门管理服务中心", "步行：约 6 分钟", "提醒：开放状态以现场指示为准"),
            )
            listOf("休息", "座椅", "累", "坐").any(compactPrompt::contains) -> Triple(
                "休息点：林荫休息廊",
                "继续向前约 90 米就是林荫休息廊，沿途有连续树荫和多处座椅，适合短暂停留。",
                listOf("距离：约 90 米", "设施：座椅与连续树荫", "路线：仍在湖边林荫线上"),
            )
            listOf("下雨", "雨", "避雨", "天气").any(compactPrompt::contains) -> Triple(
                "避雨建议：转向连廊",
                "如果开始下雨，可以在下一个岔路转向综合运动区连廊；预计多走 4 分钟，但遮蔽更连续。",
                listOf("备选：综合运动区连廊", "代价：约增加 4 分钟", "提醒：雷雨天气应尽快离开开阔水边"),
            )
            listOf("出口", "怎么走", "回去", "南门").any(compactPrompt::contains) -> Triple(
                "返回南门",
                "沿湖边林荫线继续前行，在儿童活动区外环右转可返回南门，全程约 12 分钟。",
                listOf("终点：南园南门", "预计：约 12 分钟", "途经：儿童活动区外环"),
            )
            else -> Triple(
                "我理解了你的问题",
                "你问的是“$prompt”。这个问题没有命中可核实的本地设施或环境工具，我不会编造现场事实；可以继续补充地点、想找的设施或路线需求，我会重新判断应该调用天气、路线还是附近地点工具。",
                listOf("语音转写：$prompt", "Agent 策略：先识别意图，再选择天气、路线或地点工具", "边界：没有可靠工具结果时不编造现场事实"),
            )
        }
        return AiTaskResult(
            title = response.first,
            summary = response.second,
            uncertainty = "位置与现场状态来自固定演示数据，出行时请以标牌和实际环境为准",
            primaryAction = "完成体验",
            evidence = listOf("语音转写：$prompt") + response.third,
            sourceLabel = if (BuildConfig.BUNDLED_OFFLINE_SPEECH) {
                "Vosk 离线语音 · 本地 Agent 路由"
            } else {
                "手机系统语音 · 本地 Agent 路由"
            },
        )
    }
}
