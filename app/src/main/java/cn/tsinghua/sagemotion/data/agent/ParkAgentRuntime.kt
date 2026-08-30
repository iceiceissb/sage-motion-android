package cn.tsinghua.sagemotion.data.agent

import android.content.Context
import cn.tsinghua.sagemotion.BuildConfig
import cn.tsinghua.sagemotion.data.AiDemoApi
import cn.tsinghua.sagemotion.data.MockAiDemoApi
import cn.tsinghua.sagemotion.model.DemoMode

/**
 * Single entry point for the app's task intelligence.
 *
 * SAGE currently uses one primary orchestrator with typed tools instead of a
 * collection of autonomous child agents. Formal studies select the deterministic
 * runtime; ecological demos select the connected tool-orchestration runtime.
 * Both preserve the same [AiDemoApi] event contract for the UI state machine.
 */
class ParkAgentRuntime(
    private val deterministicApi: AiDemoApi,
    private val connectedApi: AiDemoApi,
) {
    fun apiFor(mode: DemoMode): AiDemoApi = when (mode) {
        DemoMode.EXPERIMENT_OFFLINE -> deterministicApi
        DemoMode.ONLINE_AGENT -> connectedApi
    }

    companion object {
        fun create(context: Context): ParkAgentRuntime {
            val deterministicApi = MockAiDemoApi()
            val localConnectedApi = ParkAgentApi(
                scriptedApi = deterministicApi,
                contextProvider = OpenMeteoParkContextProvider(context.applicationContext),
                placeProvider = OpenStreetMapPlaceProvider(),
            )
            val remoteUrl = BuildConfig.SAGE_AGENT_BACKEND_URL.trim().takeIf { url ->
                url.startsWith("https://") || (BuildConfig.DEBUG && url.startsWith("http://"))
            }
            val connectedApi = if (remoteUrl != null) {
                RemoteFirstAiDemoApi(
                    remoteApi = RemoteAgentApi(
                        baseUrl = remoteUrl,
                        bearerToken = BuildConfig.SAGE_AGENT_CLIENT_TOKEN,
                    ),
                    localFallbackApi = localConnectedApi,
                )
            } else {
                localConnectedApi
            }
            return ParkAgentRuntime(
                deterministicApi = deterministicApi,
                connectedApi = connectedApi,
            )
        }
    }
}
