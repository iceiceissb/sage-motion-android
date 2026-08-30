package cn.tsinghua.sagemotion.data.agent

import cn.tsinghua.sagemotion.data.MockAiDemoApi
import cn.tsinghua.sagemotion.model.DemoMode
import org.junit.Assert.assertSame
import org.junit.Test

class ParkAgentRuntimeTest {
    @Test
    fun selectsDeterministicRuntimeForFormalExperiment() {
        val deterministic = MockAiDemoApi()
        val connected = MockAiDemoApi()
        val runtime = ParkAgentRuntime(deterministic, connected)

        assertSame(deterministic, runtime.apiFor(DemoMode.EXPERIMENT_OFFLINE))
    }

    @Test
    fun selectsConnectedRuntimeForEcologicalDemo() {
        val deterministic = MockAiDemoApi()
        val connected = MockAiDemoApi()
        val runtime = ParkAgentRuntime(deterministic, connected)

        assertSame(connected, runtime.apiFor(DemoMode.ONLINE_AGENT))
    }
}
