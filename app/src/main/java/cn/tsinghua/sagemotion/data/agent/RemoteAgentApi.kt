package cn.tsinghua.sagemotion.data.agent

import android.content.Context
import cn.tsinghua.sagemotion.BuildConfig
import cn.tsinghua.sagemotion.data.AiDemoApi
import cn.tsinghua.sagemotion.data.AiTaskEvent
import cn.tsinghua.sagemotion.data.AiTaskRequest
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.AiTaskResult
import cn.tsinghua.sagemotion.model.ResultMetric
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * SSE adapter for the server-side primary agent. The OpenAI key never crosses
 * this boundary: Android only knows the project backend URL and, optionally,
 * a scoped client token issued by that backend or its API gateway.
 */
class RemoteAgentApi(
    context: Context,
    baseUrl: String,
    private val bearerToken: String = "",
) : AiDemoApi {
    private val endpoint = "${baseUrl.trimEnd('/')}/v1/agent/tasks:stream"
    private val imageEncoder = VisionImagePayloadEncoder(context)

    override fun runTask(request: AiTaskRequest): Flow<AiTaskEvent> = flow {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            setRequestProperty("Accept", "text/event-stream")
            setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            setRequestProperty("Cache-Control", "no-cache")
            setRequestProperty("X-Sage-App-Version", BuildConfig.VERSION_NAME)
            if (bearerToken.isNotBlank()) {
                setRequestProperty("Authorization", "Bearer $bearerToken")
            }
        }
        try {
            val visionImageDataUrl = request.visionImageUri?.let { imageEncoder.encodeDataUrl(it) ?: throw IOException("Unable to prepare authorized image") }
            val body = request.toRemoteJson(
                requestId = UUID.randomUUID().toString().replace("-", ""),
                visionImageDataUrl = visionImageDataUrl,
            )
            connection.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(body.toString())
            }
            val status = connection.responseCode
            if (status !in 200..299) {
                throw IOException("SAGE backend returned HTTP $status")
            }

            var eventName = "message"
            val data = StringBuilder()

            suspend fun dispatchFrame() {
                if (data.isEmpty()) return
                decodeRemoteEvent(eventName, data.toString())?.let { emit(it) }
                eventName = "message"
                data.clear()
            }

            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (true) {
                    val line = reader.readLine() ?: break
                    when {
                        line.isEmpty() -> dispatchFrame()
                        line.startsWith(":") -> Unit
                        line.startsWith("event:") -> eventName = line.substringAfter(':').trim()
                        line.startsWith("data:") -> {
                            if (data.isNotEmpty()) data.append('\n')
                            data.append(line.substringAfter(':').trimStart())
                        }
                    }
                }
            }
            dispatchFrame()
        } finally {
            connection.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val CONNECT_TIMEOUT_MS = 8_000
        const val READ_TIMEOUT_MS = 75_000
    }
}

/** Remote-first ecological mode with an explicit, non-crashing local fallback. */
class RemoteFirstAiDemoApi(
    private val remoteApi: AiDemoApi,
    private val localFallbackApi: AiDemoApi,
) : AiDemoApi {
    override fun runTask(request: AiTaskRequest): Flow<AiTaskEvent> = flow {
        var completed = false
        try {
            remoteApi.runTask(request).collect { event ->
                if (event is AiTaskEvent.Completed) completed = true
                emit(event)
            }
            // A truncated SSE response can close cleanly without an error frame.
            // It must still produce a result so the UI does not wait indefinitely.
            if (!completed) throw IOException("Remote agent stream ended before completion")
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (completed) return@flow
            localFallbackApi.runTask(request).collect { event ->
                emit(
                    if (event is AiTaskEvent.Completed) {
                        event.copy(
                            result = event.result.copy(
                                evidence = listOf("服务端主 Agent 暂不可用，本轮已自动切换到本地工具编排") +
                                    event.result.evidence,
                                sourceLabel = "本地安全回退 · ${event.result.sourceLabel}",
                            ),
                        )
                    } else {
                        event
                    },
                )
            }
        }
    }
}

private fun AiTaskRequest.toRemoteJson(requestId: String, visionImageDataUrl: String?): JSONObject = JSONObject()
    .put("request_id", requestId)
    .put("scenario", scenario.id)
    .put("prompt", prompt.take(2_000))
    .put("vision_is_region", visionIsRegion)
    .put("has_captured_photo", hasCapturedPhoto)
    .put(
        "vision_findings",
        JSONArray().apply {
            visionFindings.take(20).forEach { finding ->
                put(JSONObject().put("label", finding.label.take(80)).put("confidence", finding.confidence))
            }
        },
    )
    .put(
        "journey_context",
        JSONObject()
            .put("active_route_name", journeyContext.activeRouteName.take(80))
            .put("route_replanned", journeyContext.routeReplanned)
            .put("previous_voice_turns", JSONArray(journeyContext.previousVoiceTurns.takeLast(12)))
            .put("vision_labels", JSONArray(journeyContext.visionLabels.takeLast(24)))
            .put("photo_question_count", journeyContext.photoQuestionCount.coerceIn(0, 100))
            .put("visual_interaction_count", journeyContext.visualInteractionCount.coerceIn(0, 100))
            .put("voice_interaction_count", journeyContext.voiceInteractionCount.coerceIn(0, 100))
            .put("replan_count", journeyContext.replanCount.coerceIn(0, 100)),
    )
    .put(
        "client_capabilities",
        JSONArray(
            buildList {
                addAll(
                    listOf(
                        "amap_route",
                        "on_device_vision",
                        if (BuildConfig.BUNDLED_OFFLINE_SPEECH) "offline_speech" else "system_speech",
                        "local_journey_memory",
                    ),
                )
                if (visionImageDataUrl != null) add("multimodal_image_upload")
            },
        ),
    )
    .apply {
        if (visionImageDataUrl != null) put("vision_image_data_url", visionImageDataUrl)
    }

internal fun decodeRemoteEvent(eventName: String, rawData: String): AiTaskEvent? {
    val payload = JSONObject(rawData)
    return when (eventName) {
        "stage_changed" -> {
            val stageId = payload.optString("stage")
            val stage = AiStage.entries.firstOrNull { it.id == stageId }
                ?: throw IOException("Unknown remote agent stage")
            AiTaskEvent.StageChanged(stage)
        }
        "completed" -> AiTaskEvent.Completed(payload.getJSONObject("result").toAiTaskResult())
        "error" -> throw IOException(payload.optString("code", "agent_unavailable"))
        "tool_started", "tool_completed", "message" -> null
        else -> null
    }
}

private fun JSONObject.toAiTaskResult(): AiTaskResult = AiTaskResult(
    title = getString("title"),
    summary = getString("summary"),
    uncertainty = optNullableString("uncertainty"),
    primaryAction = getString("primary_action"),
    evidence = getJSONArray("evidence").stringValues(),
    alternativeTitle = optNullableString("alternative_title"),
    metrics = getJSONArray("metrics").objectValues().map { metric ->
        ResultMetric(metric.getString("title"), metric.getString("detail"))
    },
    sourceLabel = getString("source_label"),
    sourceUrl = optNullableString("source_url"),
    isLiveData = optBoolean("is_live_data", false),
    attribution = optNullableString("attribution"),
)

private fun JSONObject.optNullableString(key: String): String? =
    if (!has(key) || isNull(key)) null else optString(key).takeIf(String::isNotBlank)

private fun JSONArray.stringValues(): List<String> = buildList {
    for (index in 0 until length()) optString(index).takeIf(String::isNotBlank)?.let(::add)
}

private fun JSONArray.objectValues(): List<JSONObject> = buildList {
    for (index in 0 until length()) optJSONObject(index)?.let(::add)
}
