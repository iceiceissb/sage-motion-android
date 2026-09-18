package cn.tsinghua.sagemotion.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
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
import java.util.Locale

data class SpeechInputState(
    val status: String,
    val isListening: Boolean,
    val level: Float,
    val onToggle: () -> Unit,
)

/**
 * Compact APK speech input backed by the device RecognitionService.
 *
 * This flavor intentionally does not bundle Vosk or its Chinese acoustic model. Manual text
 * input remains available when a phone does not provide a compatible speech service.
 */
@Composable
fun rememberRealSpeechInputState(
    currentText: String,
    onTranscript: (String) -> Unit,
): SpeechInputState {
    val context = LocalContext.current
    val latestTranscriptHandler by rememberUpdatedState(onTranscript)
    val latestText by rememberUpdatedState(currentText)
    var isListening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("使用手机语音服务；也可直接键盘输入") }
    var level by remember { mutableFloatStateOf(0f) }
    var sessionBase by remember { mutableStateOf("") }

    val speechRecognizer = remember(context) {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                SpeechRecognizer.isOnDeviceRecognitionAvailable(context) ->
                SpeechRecognizer.createOnDeviceSpeechRecognizer(context)

            SpeechRecognizer.isRecognitionAvailable(context) ->
                SpeechRecognizer.createSpeechRecognizer(context)

            else -> null
        }
    }

    fun publish(spokenText: String, completed: Boolean) {
        val spoken = normalizeMandarinTranscript(spokenText)
        if (spoken.isBlank()) return
        val merged = listOf(sessionBase, spoken).filter { it.isNotBlank() }.joinToString("；")
        latestTranscriptHandler(merged)
        status = if (completed) {
            "转写完成，已写入输入框；请确认或修改后提交"
        } else {
            "正在听：$spoken"
        }
    }

    val listener = remember {
        object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                level = .35f
                status = "正在听，请开始说话；说完后点“结束”"
            }

            override fun onBeginningOfSpeech() {
                status = "正在听…"
            }

            override fun onRmsChanged(rmsdB: Float) {
                level = ((rmsdB + 2f) / 12f).coerceIn(.18f, 1f)
            }

            override fun onBufferReceived(buffer: ByteArray?) = Unit

            override fun onEndOfSpeech() {
                status = "正在完成转写，请稍候…"
            }

            override fun onError(error: Int) {
                isListening = false
                level = 0f
                status = when (error) {
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "需要麦克风权限才能听懂语音"
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "系统语音网络不可用，请重试或直接键盘输入"
                    SpeechRecognizer.ERROR_NO_MATCH -> "没有听清，请再说一次或直接键盘输入"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到语音，请靠近麦克风重试"
                    else -> "系统语音识别未完成，请重试或直接键盘输入"
                }
            }

            override fun onResults(results: Bundle?) {
                isListening = false
                level = 0f
                val transcript = results.bestTranscript()
                if (transcript.isBlank()) {
                    status = "没有听清，请再说一次或直接键盘输入"
                } else {
                    publish(transcript, completed = true)
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                partialResults.bestTranscript().takeIf { it.isNotBlank() }?.let {
                    publish(it, completed = false)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        }
    }

    DisposableEffect(speechRecognizer, listener) {
        speechRecognizer?.setRecognitionListener(listener)
        if (speechRecognizer == null) {
            status = "此手机未提供系统语音识别，请直接键盘输入"
        }
        onDispose {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
    }

    fun beginRecognition() {
        val recognizer = speechRecognizer
        if (recognizer == null) {
            status = "此手机未提供系统语音识别，请直接键盘输入"
            return
        }
        if (isListening) {
            recognizer.stopListening()
            status = "正在完成转写，请稍候…"
            return
        }
        sessionBase = latestText.trim()
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.SIMPLIFIED_CHINESE.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        runCatching { recognizer.startListening(intent) }
            .onSuccess {
                isListening = true
                level = .25f
                status = "正在启动手机语音服务…"
            }
            .onFailure {
                isListening = false
                level = 0f
                status = "手机语音服务启动失败，请重试或直接键盘输入"
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

    return SpeechInputState(status, isListening, level, toggle)
}

private fun Bundle?.bestTranscript(): String = this
    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
    ?.firstOrNull()
    .orEmpty()

private fun normalizeMandarinTranscript(text: String): String = text
    .trim()
    .replace(Regex("(?<=[\\u4E00-\\u9FFF])\\s+(?=[\\u4E00-\\u9FFF])"), "")
    .replace(Regex("\\s+"), " ")
