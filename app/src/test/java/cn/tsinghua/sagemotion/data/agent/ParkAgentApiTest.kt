package cn.tsinghua.sagemotion.data.agent

import cn.tsinghua.sagemotion.data.AiTaskRequest
import cn.tsinghua.sagemotion.model.AiTaskResult
import cn.tsinghua.sagemotion.model.ExperimentScenario
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParkAgentApiTest {
    private val scripted = AiTaskResult(
        title = "scripted",
        summary = "base。",
        uncertainty = null,
        primaryAction = "next",
        evidence = listOf("script evidence"),
    )

    private val reading = ParkContextReading(
        context = ParkContext(
            temperatureCelsius = 28.2,
            apparentTemperatureCelsius = 30.1,
            windSpeedKmh = 8.4,
            precipitationMm = 0.0,
            weatherCode = 1,
            uvIndexMax = 6.2,
            usAqi = 42,
            pm25 = 11.3,
            observedAt = "2026-07-18T10:00",
        ),
        origin = ContextOrigin.LIVE,
    )

    @Test
    fun environmentResultIsEnrichedAndAttributed() {
        val result = enrich(scripted, ExperimentScenario.ENVIRONMENT, reading)

        assertTrue(result.isLiveData)
        assertTrue(result.summary.contains("28.2°C"))
        assertTrue(result.evidence.any { it.contains("固定演示点") })
        assertTrue(result.attribution.orEmpty().contains("CAMS"))
    }

    @Test
    fun unavailableProviderFallsBackExplicitly() {
        val result = enrich(scripted, ExperimentScenario.VOICE, null)

        assertFalse(result.isLiveData)
        assertTrue(result.sourceLabel.contains("回退离线脚本"))
        assertTrue(result.summary.contains("base"))
    }

    @Test
    fun visualStimulusNeverClaimsLiveRecognition() {
        val result = enrich(scripted, ExperimentScenario.VISUAL, reading)

        assertFalse(result.isLiveData)
        assertTrue(result.sourceLabel.contains("非实时识别"))
        assertTrue(result.sourceUrl == null)
    }

    @Test
    fun replanUsesUserConstraintAndWeatherToolInRouteDecision() {
        val result = enrich(
            scripted,
            AiTaskRequest(ExperimentScenario.ADJUST, "前方封路，而且快下雨了"),
            reading,
        )

        assertTrue(result.title.contains("林下连廊绕行线"))
        assertTrue(result.summary.contains("绕开"))
        assertTrue(result.evidence.any { it.contains("用户变化描述") })
    }

    @Test
    fun spacedSpeechFoodQuestionRoutesToNearbyPlaceTool() {
        val result = enrich(
            scripted,
            AiTaskRequest(ExperimentScenario.VOICE, "附近 有 哪里 有 好 吃 的"),
            reading,
            PlaceReading(
                listOf(
                    NearbyPlace("林畔咖啡", "咖啡/茶饮", 420),
                    NearbyPlace("南门家常菜", "餐馆", 680),
                ),
            ),
        )

        assertTrue(isFoodQuestion("附近 有 哪里 有 好 吃 的"))
        assertTrue(result.title.contains("林畔咖啡"))
        assertTrue(result.summary.contains("420 米"))
        assertTrue(result.sourceLabel.contains("OpenStreetMap"))
    }

    @Test
    fun foodToolFailureIsReportedAsAttemptedToolCallInsteadOfOfflineAnswer() {
        val result = enrich(
            scripted,
            AiTaskRequest(ExperimentScenario.VOICE, "附近有什么好吃的"),
            reading,
            placeReading = null,
        )

        assertFalse(result.isLiveData)
        assertTrue(result.title.contains("地点工具暂未返回"))
        assertTrue(result.summary.contains("Overpass"))
        assertTrue(result.summary.contains("Nominatim"))
        assertTrue(result.sourceLabel.contains("地点工具未返回"))
        assertFalse(result.summary.contains("离线模式"))
    }
}
