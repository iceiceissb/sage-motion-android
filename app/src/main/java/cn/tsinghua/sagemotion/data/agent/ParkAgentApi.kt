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
    private val placeProvider: ParkPlaceProvider? = null,
) : AiDemoApi {
    override fun runTask(request: AiTaskRequest): Flow<AiTaskEvent> = flow {
        coroutineScope {
            val needsContext = request.scenario == ExperimentScenario.ENVIRONMENT ||
                request.scenario == ExperimentScenario.VOICE ||
                request.scenario == ExperimentScenario.ADJUST
            val context = if (needsContext) async { contextProvider.load() } else null
            val places = if (
                request.scenario == ExperimentScenario.VOICE &&
                isFoodQuestion(request.prompt) &&
                placeProvider != null
            ) async { placeProvider.nearbyFood() } else null

            scriptedApi.runTask(request).collect { event ->
                when (event) {
                    is AiTaskEvent.StageChanged -> emit(event)
                    is AiTaskEvent.Completed -> {
                        val reading = context?.let { withTimeoutOrNull(3_200L) { it.await() } }
                        val placeReading = places?.let { withTimeoutOrNull(15_000L) { it.await() } }
                        emit(AiTaskEvent.Completed(enrich(event.result, request, reading, placeReading)))
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
): AiTaskResult = enrich(scripted, AiTaskRequest(scenario, scenario.participantPrompt), reading)

internal fun enrich(
    scripted: AiTaskResult,
    request: AiTaskRequest,
    reading: ParkContextReading?,
    placeReading: PlaceReading? = null,
): AiTaskResult {
    val scenario = request.scenario
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
    if (scenario == ExperimentScenario.VOICE && isFoodQuestion(request.prompt)) {
        return if (placeReading != null) {
            foodPlaceResult(scripted, request.prompt, placeReading, reading)
        } else {
            foodToolUnavailableResult(scripted, request.prompt, reading)
        }
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
    val environment = "东升八家郊野公园固定演示点：${temperature}°C，体感 ${feelsLike}°C，风速 $wind km/h，AQI $aqi。"
    val source = "联网 Agent · Open-Meteo（$sourceState）"
    val commonEvidence = listOf(
        "实时环境：$environment",
        "今日最高 UV 指数：$uv",
        "观测时间：${context.observedAt}（固定演示点，不是参与者位置）",
    )
    val decision = chooseRoute(request.prompt, context, scenario == ExperimentScenario.ADJUST)

    val enriched = when (scenario) {
        ExperimentScenario.ENVIRONMENT -> scripted.copy(
            title = "推荐：${decision.primary}",
            alternativeTitle = "备选：${decision.alternative}",
            summary = "${decision.reason} $environment 路线候选来自内置园路图，环境约束来自实时工具。",
            uncertainty = "实时环境数据可能延迟；路线几何、拥挤度与安全信息并非实时，请以现场为准",
            metrics = listOf(
                ResultMetric("${temperature}°C", "体感 ${feelsLike}°C"),
                ResultMetric("AQI $aqi", "PM2.5 ${context.pm25?.oneDecimal() ?: "--"}"),
                ResultMetric("UV $uv", weatherLabel(context.weatherCode)),
            ),
            evidence = commonEvidence + "Agent 路线评分：${decision.scoreNote}" + scripted.evidence,
        )

        ExperimentScenario.VOICE -> scripted.copy(
            summary = "${scripted.summary}$environment ${photoAdvice(context)}",
            uncertainty = "天气与空气质量来自固定演示点；现场光线、人流和道路状态仍需自行确认",
            evidence = commonEvidence + scripted.evidence,
        )

        ExperimentScenario.ADJUST -> scripted.copy(
            title = "路线已调整：${decision.primary}",
            alternativeTitle = "保留当前路线 · 自行判断",
            summary = "${decision.reason} $environment",
            uncertainty = "天气来自固定演示点；封路事件与路线几何为研究脚本，请以现场标识为准",
            metrics = listOf(
                ResultMetric("+4 分钟", "绕行约 260 米"),
                ResultMetric("AQI $aqi", "环境参考"),
                ResultMetric("UV $uv", weatherLabel(context.weatherCode)),
            ),
            evidence = commonEvidence + "用户变化描述：${request.prompt}" + "Agent 路线评分：${decision.scoreNote}" + scripted.evidence,
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

internal fun isFoodQuestion(prompt: String): Boolean {
    val compact = prompt.replace(Regex("\\s+"), "")
    return listOf("吃", "餐厅", "餐馆", "饭店", "美食", "咖啡", "茶饮", "小吃").any(compact::contains)
}

private fun foodPlaceResult(
    scripted: AiTaskResult,
    prompt: String,
    reading: PlaceReading,
    weather: ParkContextReading?,
): AiTaskResult {
    val places = reading.places.take(3)
    if (places.isEmpty()) return scripted
    val nearest = places.first()
    val listText = places.joinToString("；") { place ->
        buildString {
            append("${place.name}（${place.category}，约 ${place.distanceMeters} 米")
            place.address?.let { append("，$it") }
            append("）")
        }
    }
    val weatherAdvice = weather?.context?.let { context ->
        when {
            context.precipitationMm > .1 -> " 当前有降水，出发前建议确认是否需要室内路线。"
            (context.uvIndexMax ?: 0.0) >= 6.0 -> " 今日 UV 较高，步行时注意防晒。"
            else -> ""
        }
    }.orEmpty()
    return scripted.copy(
        title = "附近餐饮：${nearest.name}",
        summary = "我调用了附近地点工具，以公园固定实验点为中心找到：$listText。$weatherAdvice",
        uncertainty = "OpenStreetMap 不提供口味、评分与实时营业状态；“附近”不等于“最好吃”，出发前请再确认营业信息",
        metrics = places.map { ResultMetric("约 ${it.distanceMeters} 米", it.name) },
        evidence = listOf(
            "用户问题：$prompt",
            "地点工具：${reading.source}，半径 2.5 公里",
            "查询中心：东升八家郊野公园南区固定实验点（不是参与者实时位置）",
        ) + scripted.evidence,
        sourceLabel = "联网 Agent · ${reading.source} 附近地点",
        sourceUrl = "https://www.openstreetmap.org/",
        isLiveData = true,
        attribution = "地点数据 © OpenStreetMap contributors",
    )
}

private fun foodToolUnavailableResult(
    scripted: AiTaskResult,
    prompt: String,
    weather: ParkContextReading?,
): AiTaskResult {
    val weatherNote = weather?.context?.let { context ->
        " 环境工具已返回：${context.temperatureCelsius.oneDecimal()}°C，体感 ${context.apparentTemperatureCelsius.oneDecimal()}°C，AQI ${context.usAqi ?: "暂无"}。"
    }.orEmpty()
    return scripted.copy(
        title = "附近地点工具暂未返回结果",
        summary = "Agent 已识别餐饮意图，并依次调用 OpenStreetMap Overpass 与 Nominatim；本次查询超时或没有返回命名地点。为了避免虚构商户，本次不提供餐馆名称，可点“重新开始”重试。$weatherNote",
        uncertainty = "联网 Agent 已执行工具调用，但公共地点服务可能限流、排队或受当前网络影响；该结果不代表附近没有餐饮",
        evidence = listOf(
            "用户问题：$prompt",
            "已尝试工具：OpenStreetMap Overpass（两个公共实例）",
            "备用工具：OpenStreetMap Nominatim 有界地点搜索",
            "处理原则：没有可靠地点数据时不生成商户名称",
        ) + scripted.evidence,
        sourceLabel = "联网 Agent · 地点工具未返回",
        sourceUrl = "https://www.openstreetmap.org/",
        isLiveData = false,
        attribution = "已尝试地点数据：OpenStreetMap contributors",
    )
}

private data class RouteDecision(
    val primary: String,
    val alternative: String,
    val reason: String,
    val scoreNote: String,
)

/**
 * 可复现的轻量路线决策器：把语句中的约束和天气工具结果转成候选路线分数。
 * 正式实验可锁定输入；生态演示则使用实时天气，但不需要付费 LLM 或 API Key。
 */
private fun chooseRoute(prompt: String, context: ParkContext, adjusting: Boolean): RouteDecision {
    val compactPrompt = prompt.replace(Regex("\\s+"), "")
    val rainy = context.precipitationMm > .1 || context.weatherCode in 51..67 || context.weatherCode in 80..82 ||
        listOf("雨", "避雨", "雷").any(compactPrompt::contains)
    val blocked = listOf("封路", "封闭", "施工", "走不通").any(compactPrompt::contains)
    val crowded = listOf("拥挤", "人多", "安静", "避开人群").any(compactPrompt::contains)
    val wantsShort = listOf("短", "快", "少走").any(compactPrompt::contains)
    val wantsRest = listOf("座椅", "休息", "累", "阴凉").any(compactPrompt::contains)

    val shelteredScore = (if (rainy) 5 else 0) + (if (blocked && adjusting) 3 else 0) + (if (wantsRest) 2 else 0)
    val quietScore = (if (crowded) 5 else 0) + (if (wantsRest) 1 else 0)
    val lakesideScore = (if (wantsShort) 4 else 0) + (if (!rainy) 2 else -2) + (if (wantsRest) 2 else 0)

    return when {
        shelteredScore > quietScore && shelteredScore > lakesideScore -> RouteDecision(
            primary = if (adjusting) "林下连廊绕行线" else "连廊避雨线",
            alternative = "湖边林荫线",
            reason = if (blocked) "已绕开用户报告的中断路段，并优先经过有遮挡的连廊。" else "当前降水或避雨约束权重最高，优先经过连续遮挡区域。",
            scoreNote = "连廊 $shelteredScore，静谧 $quietScore，湖边 $lakesideScore",
        )
        quietScore > shelteredScore && quietScore > lakesideScore -> RouteDecision(
            primary = "林下静谧线",
            alternative = "湖边林荫线",
            reason = "已降低人流密集区域的权重，优先选择林下支路。",
            scoreNote = "静谧 $quietScore，连廊 $shelteredScore，湖边 $lakesideScore",
        )
        else -> RouteDecision(
            primary = "湖边林荫线",
            alternative = "草坪外环线",
            reason = "路程、树荫和休息点的综合分更高，保留外环线作为备选。",
            scoreNote = "湖边 $lakesideScore，连廊 $shelteredScore，静谧 $quietScore",
        )
    }
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
