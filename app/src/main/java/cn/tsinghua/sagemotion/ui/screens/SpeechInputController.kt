package cn.tsinghua.sagemotion.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

data class SpeechInputState(
    val status: String,
    val isListening: Boolean,
    val level: Float,
    val onToggle: () -> Unit,
)

@Composable
fun rememberRealSpeechInputState(onTranscript: (String) -> Unit): SpeechInputState {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("轻触后直接在应用内说话") }
    var level by remember { mutableFloatStateOf(0f) }
    val recognizer = remember(context) {
        if (SpeechRecognizer.isRecognitionAvailable(context)) SpeechRecognizer.createSpeechRecognizer(context) else null
    }
    val recognitionIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
    }
    DisposableEffect(recognizer) {
        recognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { isListening = true; level = .08f; status = "正在听，请开始说话…" }
            override fun onBeginningOfSpeech() { status = "正在记录你的问题…" }
            override fun onRmsChanged(rmsdB: Float) { level = ((rmsdB + 2f) / 12f).coerceIn(.04f, 1f) }
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() { isListening = false; level = 0f; status = "正在转写…" }
            override fun onError(error: Int) {
                isListening = false
                level = 0f
                status = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "没有听清，请再试一次"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有检测到语音，请靠近麦克风"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "需要麦克风权限才能使用实际语音"
                    else -> "语音服务暂时不可用（错误 $error）"
                }
            }
            override fun onResults(results: Bundle?) {
                isListening = false
                level = 0f
                val transcript = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
                if (transcript.isNullOrBlank()) {
                    status = "没有获得转写结果，请重试"
                } else {
                    onTranscript(transcript)
                    status = "转写完成，可以开始回答"
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let { partial ->
                    if (partial.isNotBlank()) status = "正在听：$partial"
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        onDispose { recognizer?.cancel(); recognizer?.destroy() }
    }

    fun beginRecognition() {
        if (recognizer == null) {
            status = "当前设备没有可用的系统语音识别服务"
            return
        }
        if (isListening) {
            recognizer.stopListening()
        } else {
            runCatching { recognizer.startListening(recognitionIntent) }
                .onFailure { status = "无法启动语音服务：${it.javaClass.simpleName}" }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) beginRecognition() else status = "麦克风权限被拒绝，可在系统设置中重新允许"
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
