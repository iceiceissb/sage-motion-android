package cn.tsinghua.sagemotion.data.agent

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
}
