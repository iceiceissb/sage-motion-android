package cn.tsinghua.sagemotion.data

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

        ExperimentScenario.VISUAL -> AiTaskResult(
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

        ExperimentScenario.VOICE -> AiTaskResult(
            title = "推荐：湖心桥东侧",
            summary = "向前约 180 米，桥边能拍到湖面、柳树和远处亭子。下午光线更柔和。",
            uncertainty = "现场拥挤度为估计值，可能有延迟",
            primaryAction = "完成体验",
            evidence = listOf(
                "语音转写：${request.prompt}",
                "距离：当前位置约 180 米",
                "景观：湖面、柳树、亭子同框",
                "光线：下午侧逆光",
                "缺口：人流数据约有 10 分钟延迟",
            ),
        )

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
}
