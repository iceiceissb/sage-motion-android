package cn.tsinghua.sagemotion.data.agent

import cn.tsinghua.sagemotion.data.AiDemoApi
import cn.tsinghua.sagemotion.data.AiTaskEvent
import cn.tsinghua.sagemotion.data.AiTaskRequest
import cn.tsinghua.sagemotion.model.AiTaskResult
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.ResultMetric
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * A small orchestration agent: it preserves the app's semantic stage stream while
 * enriching scripted outcomes with live environmental context. It never invents a
 * live visual-recognition result and always has an explicit offline fallback.
 */
class ParkAgentApi(
    private val scriptedApi: AiDemoApi,
    private val contextProvider: ParkContextProvider,
) : AiDemoApi {
    override fun runTask(request: AiTaskRequest): Flow<AiTaskEvent> = flow {
        coroutineScope {
            val needsContext = request.scenario == ExperimentScenario.ENVIRONMENT ||
                request.scenario == ExperimentScenario.VOICE ||
                request.scenario == ExperimentScenario.ADJUST
            val context = if (needsContext) async { contextProvider.load() } else null

            scriptedApi.runTask(request).collect { event ->
                when (event) {
                    is AiTaskEvent.StageChanged -> emit(event)
                    is AiTaskEvent.Completed -> {
                        val reading = context?.let { withTimeoutOrNull(3_200L) { it.await() } }
                        emit(AiTaskEvent.Completed(enrich(event.result, request.scenario, reading)))
                    }
                }
            }
        }
    }
}

internal fun enrich(
    scripted: AiTaskResult,
    scenario: ExperimentScenario,
    reading: ParkContextReading?,
): AiTaskResult {
    if (scenario == ExperimentScenario.VISUAL) {
        return if (scripted.isLiveData) scripted else scripted.copy(
            sourceLabel = "离线固定视觉刺激 · 非实时识别",
            isLiveData = false,
        )
    }
    if (scenario == ExperimentScenario.CREATE) {
        return scripted.copy(sourceLabel = "本地旅程素材 · 生成式演示")
    }
    if (scenario == ExperimentScenario.EXPLORE) {
        return scripted.copy(sourceLabel = "探索工作台 · 本地状态")
    }
    if (reading == null) {
        return scripted.copy(
            sourceLabel = "Agent 接口不可用 · 已回退离线脚本",
            isLiveData = false,
        )
    }

    val context = reading.context
    val sourceState = when (reading.origin) {
        ContextOrigin.LIVE -> "实时"
        ContextOrigin.FRESH_CACHE -> "30 分钟内缓存"
        ContextOrigin.STALE_CACHE -> "备用缓存"
    }
    val temperature = context.temperatureCelsius.oneDecimal()
    val feelsLike = context.apparentTemperatureCelsius.oneDecimal()
    val wind = context.windSpeedKmh.oneDecimal()
    val aqi = context.usAqi?.toString() ?: "暂无"
    val uv = context.uvIndexMax?.oneDecimal() ?: "暂无"
    val environment = "清华校园固定演示点：${temperature}°C，体感 ${feelsLike}°C，风速 $wind km/h，AQI $aqi。"
    val source = "联网 Agent · Open-Meteo（$sourceState）"
    val commonEvidence = listOf(
        "实时环境：$environment",
        "今日最高 UV 指数：$uv",
        "观测时间：${context.observedAt}（固定演示点，不是参与者位置）",
    )

    val enriched = when (scenario) {
        ExperimentScenario.ENVIRONMENT -> scripted.copy(
            summary = "${scripted.summary}$environment 路线几何与遮阴/座椅仍为本地实验脚本。",
            uncertainty = "实时环境数据可能延迟；路线几何、拥挤度与安全信息并非实时，请以现场为准",
            metrics = listOf(
                ResultMetric("${temperature}°C", "体感 ${feelsLike}°C"),
                ResultMetric("AQI $aqi", "PM2.5 ${context.pm25?.oneDecimal() ?: "--"}"),
                ResultMetric("UV $uv", weatherLabel(context.weatherCode)),
            ),
            evidence = commonEvidence + scripted.evidence,
        )

        ExperimentScenario.VOICE -> scripted.copy(
            summary = "${scripted.summary}$environment ${photoAdvice(context)}",
            uncertainty = "天气与空气质量来自固定演示点；现场光线、人流和道路状态仍需自行确认",
            evidence = commonEvidence + scripted.evidence,
        )

        ExperimentScenario.ADJUST -> scripted.copy(
            summary = "${scripted.summary}$environment 动态路线仍为本地演示几何。",
            uncertainty = "天气来自固定演示点；封路事件与路线几何为研究脚本，请以现场标识为准",
            metrics = listOf(
                ResultMetric("+4 分钟", "绕行约 260 米"),
                ResultMetric("AQI $aqi", "环境参考"),
                ResultMetric("UV $uv", weatherLabel(context.weatherCode)),
            ),
            evidence = commonEvidence + scripted.evidence,
        )

        ExperimentScenario.VISUAL, ExperimentScenario.CREATE, ExperimentScenario.EXPLORE -> scripted
    }

    return enriched.copy(
        sourceLabel = source,
        sourceUrl = "https://open-meteo.com/",
        isLiveData = true,
        attribution = "天气数据：Open-Meteo；空气质量数据：CAMS（经 Open-Meteo）",
    )
}

private fun photoAdvice(context: ParkContext): String = when {
    context.precipitationMm > 0.1 -> "当前有降水，建议优先选择有遮挡的取景点。"
    (context.uvIndexMax ?: 0.0) >= 6.0 -> "今日 UV 较高，建议避开正午并注意防晒。"
    context.windSpeedKmh >= 25.0 -> "当前风较大，拍摄花草时可使用更快快门。"
    else -> "当前环境条件较平稳，仍建议到场后确认光线。"
}

private fun weatherLabel(code: Int): String = when (code) {
    0 -> "晴朗"
    1, 2, 3 -> "多云"
    45, 48 -> "有雾"
    in 51..67, in 80..82 -> "有雨"
    in 71..77, in 85..86 -> "有雪"
    in 95..99 -> "雷雨"
    else -> "环境数据"
}

private fun Double.oneDecimal(): String = String.format(Locale.CHINA, "%.1f", this)
