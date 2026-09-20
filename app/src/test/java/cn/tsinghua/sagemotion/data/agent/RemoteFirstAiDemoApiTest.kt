package cn.tsinghua.sagemotion.data.agent

import cn.tsinghua.sagemotion.data.AiDemoApi
import cn.tsinghua.sagemotion.data.AiTaskEvent
import cn.tsinghua.sagemotion.data.AiTaskRequest
import cn.tsinghua.sagemotion.data.MockAiDemoApi
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ExperimentScenario
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteFirstAiDemoApiTest {
    @Test
    fun streamEndingAfterProgressStillProducesAFallbackResult() = runBlocking {
        val truncatedRemote = object : AiDemoApi {
            override fun runTask(request: AiTaskRequest): Flow<AiTaskEvent> = flow {
                emit(AiTaskEvent.StageChanged(AiStage.COMPLETE))
            }
        }
        val events = RemoteFirstAiDemoApi(truncatedRemote, MockAiDemoApi(waitFor = {}))
            .runTask(AiTaskRequest(ExperimentScenario.VOICE, "附近有什么？")).toList()

        val completed = events.filterIsInstance<AiTaskEvent.Completed>().single()
        assertTrue(completed.result.sourceLabel.startsWith("本地安全回退"))
    }

    @Test
    fun remoteFailureFallsBackWithoutCrashingTheTask() = runBlocking {
        val failingRemote = object : AiDemoApi {
            override fun runTask(request: AiTaskRequest): Flow<AiTaskEvent> = flow {
                error("backend unavailable")
            }
        }
        val api = RemoteFirstAiDemoApi(
            remoteApi = failingRemote,
            localFallbackApi = MockAiDemoApi(waitFor = {}),
        )

        val events = api.runTask(
            AiTaskRequest(ExperimentScenario.VOICE, "附近有什么适合拍照的地方？"),
        ).toList()
        val completed = events.filterIsInstance<AiTaskEvent.Completed>().single()

        assertTrue(completed.result.sourceLabel.startsWith("本地安全回退"))
        assertTrue(completed.result.evidence.first().contains("服务端主 Agent 暂不可用"))
    }
}
