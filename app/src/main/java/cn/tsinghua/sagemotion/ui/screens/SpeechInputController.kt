package cn.tsinghua.sagemotion.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService

data class SpeechInputState(
    val status: String,
    val isListening: Boolean,
    val level: Float,
    val onToggle: () -> Unit,
)

/**
 * 应用自带的免费离线中文语音识别。
 *
 * 不再依赖厂商是否安装 Android RecognitionService：首次进入语音界面时，Vosk 会把
 * APK assets 中的 42 MB 中文移动端模型解包到应用私有目录，随后直接读取麦克风并转写。
 */
@Composable
fun rememberRealSpeechInputState(
    currentText: String,
    onTranscript: (String) -> Unit,
): SpeechInputState {
    val context = LocalContext.current
    val latestTranscriptHandler by rememberUpdatedState(onTranscript)
    val latestText by rememberUpdatedState(currentText)
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    var model by remember { mutableStateOf<Model?>(null) }
    var speechService by remember { mutableStateOf<SpeechService?>(null) }
    var isListening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("正在准备离线中文语音识别…") }
    var level by remember { mutableFloatStateOf(0f) }
    var sessionBase by remember { mutableStateOf("") }
    var committedSpeech by remember { mutableStateOf("") }
    var partialSpeech by remember { mutableStateOf("") }
    var recognitionSession by remember { mutableStateOf(0) }

    fun shutdownService(requestStop: Boolean = false) {
        if (requestStop) speechService?.stop()
        speechService?.shutdown()
        speechService = null
        isListening = false
        level = 0f
    }

    fun transcriptFrom(payload: String, key: String): String = runCatching {
        normalizeMandarinTranscript(JSONObject(payload).optString(key))
    }.getOrDefault("")

    fun appendCommitted(text: String) {
        if (text.isBlank() || committedSpeech == text || committedSpeech.endsWith(text)) return
        committedSpeech = listOf(committedSpeech, text).filter { it.isNotBlank() }.joinToString("，")
    }

    fun mergedTranscript(includePartial: Boolean): String {
        val spoken = listOf(committedSpeech, partialSpeech.takeIf { includePartial }.orEmpty())
            .filter { it.isNotBlank() }
            .joinToString("，")
        return listOf(sessionBase, spoken).filter { it.isNotBlank() }.joinToString("；")
    }

    fun publishTranscript(includePartial: Boolean) {
        mergedTranscript(includePartial).takeIf { it.isNotBlank() }?.let(latestTranscriptHandler)
    }

    fun acceptTranscript(text: String) {
        appendCommitted(text.ifBlank { partialSpeech })
        partialSpeech = ""
        shutdownService()
        val merged = mergedTranscript(includePartial = false)
        if (committedSpeech.isBlank()) {
            status = "没有听清，请再说一次"
        } else {
            latestTranscriptHandler(merged)
            status = "转写完成，已写入输入框；请确认或修改后提交"
        }
    }

    val listener = remember {
        object : RecognitionListener {
            override fun onPartialResult(hypothesis: String) {
                val partial = transcriptFrom(hypothesis, "partial")
                if (partial.isNotBlank()) mainHandler.post {
                    partialSpeech = partial
                    publishTranscript(includePartial = true)
                    status = "正在听：$partial"
                }
            }

            override fun onResult(hypothesis: String) {
                val text = transcriptFrom(hypothesis, "text")
                if (text.isNotBlank()) mainHandler.post {
                    appendCommitted(text)
                    partialSpeech = ""
                    publishTranscript(includePartial = false)
                    status = "已识别，可继续说；说完后点“完成”"
                }
            }

            override fun onFinalResult(hypothesis: String) {
                val text = transcriptFrom(hypothesis, "text")
                mainHandler.post { acceptTranscript(text) }
            }

            override fun onError(exception: Exception) {
                mainHandler.post {
                    shutdownService()
                    status = "离线识别未完成，请检查麦克风后重试"
                }
            }

            override fun onTimeout() {
                mainHandler.post {
                    shutdownService()
                    status = "没有检测到语音，请靠近麦克风重试"
                }
            }
        }
    }

    fun beginRecognition() {
        if (isListening) {
            speechService?.stop()
            val stoppingSession = recognitionSession
            status = "正在完成转写，请稍候…"
            mainHandler.postDelayed({
                if (isListening && recognitionSession == stoppingSession) {
                    acceptTranscript(partialSpeech)
                }
            }, FINAL_RESULT_GRACE_MS)
            return
        }
        val readyModel = model
        if (readyModel == null) {
            status = "离线中文模型仍在准备，请稍候再试"
            return
        }
        runCatching {
            recognitionSession += 1
            sessionBase = latestText.trim()
            committedSpeech = ""
            partialSpeech = ""
            val recognizer = Recognizer(readyModel, SAMPLE_RATE)
            SpeechService(recognizer, SAMPLE_RATE).also { service ->
                speechService = service
                service.startListening(listener)
            }
        }.onSuccess {
            isListening = true
            level = .58f
            status = "正在听，请开始说话；说完后点“结束”"
        }.onFailure {
            shutdownService()
            status = "麦克风启动失败，请重新授权后再试"
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginRecognition() else status = "需要麦克风权限才能听懂语音"
    }
    val toggle = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            beginRecognition()
        } else {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    DisposableEffect(context) {
        StorageService.unpack(
            context,
            MODEL_ASSET,
            MODEL_TARGET,
            { loaded ->
                mainHandler.post {
                    model = loaded
                    status = "离线中文语音已就绪，轻触“语音”开始"
                }
            },
            { error ->
                mainHandler.post {
                    status = "离线语音模型准备失败：${error.message.orEmpty().take(36)}"
                }
            },
        )
        onDispose {
            shutdownService(requestStop = true)
            model?.close()
            model = null
        }
    }
    return SpeechInputState(status, isListening, level, toggle)
}

private const val SAMPLE_RATE = 16_000f
private const val FINAL_RESULT_GRACE_MS = 900L
private const val MODEL_ASSET = "vosk-model-small-cn-0.22"
private const val MODEL_TARGET = "vosk-model-cn"

private fun normalizeMandarinTranscript(text: String): String = text
    .trim()
    .replace(Regex("(?<=[\\u4E00-\\u9FFF])\\s+(?=[\\u4E00-\\u9FFF])"), "")
    .replace(Regex("\\s+"), " ")
