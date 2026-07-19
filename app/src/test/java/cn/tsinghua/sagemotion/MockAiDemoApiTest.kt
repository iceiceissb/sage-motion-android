package cn.tsinghua.sagemotion

import cn.tsinghua.sagemotion.data.AiTaskEvent
import cn.tsinghua.sagemotion.data.AiTaskRequest
import cn.tsinghua.sagemotion.data.MockAiDemoApi
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ExperimentScenario
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MockAiDemoApiTest {
    @Test
    fun mockApiStreamsStagesAndCompletesWithoutNetwork() = runBlocking {
        val api = MockAiDemoApi(waitFor = {})
        val events = api.runTask(
            AiTaskRequest(
                scenario = ExperimentScenario.ENVIRONMENT,
                prompt = ExperimentScenario.ENVIRONMENT.participantPrompt,
            ),
        ).toList()

        assertEquals(AiStage.ACTIVATING, (events.first() as AiTaskEvent.StageChanged).stage)
        assertTrue(events.last() is AiTaskEvent.Completed)
        val result = (events.last() as AiTaskEvent.Completed).result
        assertEquals("推荐：湖边林荫线", result.title)
        assertTrue(result.evidence.isNotEmpty())
    }

    @Test
    fun mockApiCompletesCreationAndDynamicAdjustment() = runBlocking {
        val api = MockAiDemoApi(waitFor = {})
        val creation = api.runTask(AiTaskRequest(ExperimentScenario.CREATE, "生成旅程回顾")).toList()
        val adjustment = api.runTask(AiTaskRequest(ExperimentScenario.ADJUST, "前方封路")).toList()

        assertTrue((creation.last() as AiTaskEvent.Completed).result.title.contains("生成"))
        assertTrue((adjustment.last() as AiTaskEvent.Completed).result.title.contains("调整"))
        assertTrue(adjustment.filterIsInstance<AiTaskEvent.StageChanged>().any { it.stage == AiStage.REPLANNING })
    }
}
