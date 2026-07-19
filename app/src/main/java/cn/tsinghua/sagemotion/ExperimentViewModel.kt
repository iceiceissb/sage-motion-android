package cn.tsinghua.sagemotion

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.tsinghua.sagemotion.data.AiDemoApi
import cn.tsinghua.sagemotion.data.AiTaskEvent
import cn.tsinghua.sagemotion.data.AiTaskRequest
import cn.tsinghua.sagemotion.data.ExperimentLogger
import cn.tsinghua.sagemotion.data.ExperimentSessionStore
import cn.tsinghua.sagemotion.data.MockAiDemoApi
import cn.tsinghua.sagemotion.data.agent.OpenMeteoParkContextProvider
import cn.tsinghua.sagemotion.data.agent.ParkAgentApi
import cn.tsinghua.sagemotion.data.vision.OnDeviceVisionAnalyzer
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ConditionOrder
import cn.tsinghua.sagemotion.model.DemoMode
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.ExperimentUiState
import cn.tsinghua.sagemotion.model.JourneyPhotoMoment
import cn.tsinghua.sagemotion.model.JourneyQuestion
import cn.tsinghua.sagemotion.model.RouteChoice
import cn.tsinghua.sagemotion.model.stageSequenceFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class ExperimentViewModel(application: Application) : AndroidViewModel(application) {
    private val logger = ExperimentLogger(application)
    private val sessionStore = ExperimentSessionStore(application)
    private val offlineApi: AiDemoApi = MockAiDemoApi()
    private val onlineAgentApi: AiDemoApi = ParkAgentApi(
        scriptedApi = offlineApi,
        contextProvider = OpenMeteoParkContextProvider(application),
    )
    private val visionAnalyzer = OnDeviceVisionAnalyzer(application)
    private var pendingCaptureUri: Uri? = null
    private var runJob: Job? = null

    var uiState = androidx.compose.runtime.mutableStateOf(ExperimentUiState())
        private set

    init {
        restoreActiveSession()
    }

    fun startSession(participantId: String, order: ConditionOrder, demoMode: DemoMode) {
        val normalizedId = participantId.trim().ifBlank { "P000" }
        logger.startSession(normalizedId)
        uiState.value = ExperimentUiState(
            sessionStarted = true,
            participantId = normalizedId,
            order = order,
            demoMode = demoMode,
            statusMessage = "实验会话已开始 · 数据实时保存在本机",
        )
        logEvent(
            "session_started",
            details = "order=${order.name};mode=${demoMode.name};device=${Build.MANUFACTURER} ${Build.MODEL};android=${Build.VERSION.SDK_INT};app=${BuildConfig.VERSION_NAME}",
        )
        persistState()
    }

    fun finishSession() {
        if (!uiState.value.sessionStarted) return
        stopRun()
        logEvent("session_completed", details = "completed_tasks=${uiState.value.completedTaskCount}")
        sessionStore.clear()
        logger.releaseActiveSession()
        uiState.value = ExperimentUiState(
            historySessions = logger.listSessionSummaries(),
            statusMessage = "会话已保存",
        )
    }

    fun runCurrentScenario() {
        if (uiState.value.isRunning || uiState.value.demoCompleted || uiState.value.scenario == ExperimentScenario.EXPLORE) return
        runJob?.cancel()
        uiState.value = uiState.value.copy(
            isRunning = true,
            resultVisible = false,
            evidenceVisible = false,
            selectedRoute = RouteChoice.RECOMMENDED,
            statusMessage = null,
            taskResult = null,
        )
        logEvent("task_started")
        persistState()

        val routePreferences = routePreferenceLabels(uiState.value.routePreferenceIds)
        val request = AiTaskRequest(
            scenario = uiState.value.scenario,
            prompt = when (uiState.value.scenario) {
                ExperimentScenario.VOICE -> uiState.value.voiceTranscript.ifBlank { uiState.value.scenario.participantPrompt }
                ExperimentScenario.ENVIRONMENT -> buildString {
                    append("请规划公园路线。偏好：")
                    append(routePreferences.ifBlank { "舒适易行" })
                    uiState.value.routeConstraintText.takeIf { it.isNotBlank() }?.let { append("；补充约束：$it") }
                }
                else -> uiState.value.scenario.participantPrompt
            },
            visionFindings = if (uiState.value.scenario == ExperimentScenario.VISUAL) uiState.value.visionFindings else emptyList(),
        )
        runJob = viewModelScope.launch {
            try {
                val api = when (uiState.value.demoMode) {
                    DemoMode.EXPERIMENT_OFFLINE -> offlineApi
                    DemoMode.ONLINE_AGENT -> onlineAgentApi
                }
                api.runTask(request).collect { event ->
                    when (event) {
                        is AiTaskEvent.StageChanged -> enterStage(event.stage)
                        is AiTaskEvent.Completed -> {
                            uiState.value = uiState.value.copy(
                                isRunning = false,
                                resultVisible = true,
                                taskResult = event.result,
                                completedTaskCount = uiState.value.completedTaskCount + 1,
                            )
                            logEvent(
                                "task_result_visible",
                                details = "source=${event.result.sourceLabel};live=${event.result.isLiveData}",
                            )
                            persistState()
                        }
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                uiState.value = uiState.value.copy(
                    aiStage = AiStage.ERROR,
                    isRunning = false,
                    resultVisible = false,
                    statusMessage = "演示暂时无法完成，已保留现场，可重新尝试",
                )
                logEvent("task_failed", details = error.javaClass.simpleName)
                persistState()
            }
        }
    }

    fun setScenario(scenario: ExperimentScenario) {
        stopRun()
        uiState.value = uiState.value.copy(
            scenario = scenario,
            aiStage = AiStage.IDLE,
            isRunning = false,
            resultVisible = false,
            evidenceVisible = false,
            researcherPanelVisible = false,
            selectedRoute = RouteChoice.RECOMMENDED,
            taskResult = null,
            demoCompleted = false,
        )
        logEvent("scenario_selected", details = scenario.id)
        persistState()
    }

    fun setVoiceTranscript(transcript: String) {
        val normalized = transcript.trim().take(160)
        uiState.value = uiState.value.copy(voiceTranscript = normalized)
        logEvent("voice_transcribed", details = normalized)
        persistState()
    }

    fun setRouteConstraint(text: String) {
        uiState.value = uiState.value.copy(routeConstraintText = text.take(160))
        persistState()
    }

    fun toggleRoutePreference(id: String) {
        val current = uiState.value.routePreferenceIds
        val updated = if (id in current) current - id else current + id
        uiState.value = uiState.value.copy(routePreferenceIds = updated)
        logEvent("route_preference_changed", action = id, details = "selected=${id in updated}")
        persistState()
    }

    fun askVisualQuestion(question: String) {
        val normalized = question.trim().take(120)
        if (normalized.isBlank()) return
        val finding = uiState.value.visionFindings.maxByOrNull { it.confidence }
        val subject = finding?.label ?: "圈选区域"
        val confidence = finding?.let { "（端侧识别置信度 ${(it.confidence * 100).toInt()}%）" }.orEmpty()
        val answer = when {
            "特点" in normalized -> "$subject 的形态、材质与周围环境形成了当前可见特征。$confidence 建议结合圈选范围和现场距离继续观察。"
            "为什么" in normalized || "原因" in normalized -> "从画面线索看，它与当前位置的光照、植被和使用场景有关。$confidence 这是基于图像的解释，现场标牌会是更可靠的补充依据。"
            "拍" in normalized || "记录" in normalized -> "适合记录。可以保留圈选主体，并让周围环境占画面约三分之一，这样知识游记既有细节也有地点语境。"
            else -> "圈选区域最可能与“$subject”有关。$confidence 我已把问题、识别线索和这张过程照片一起保留到本次旅程。"
        }
        val currentPhoto = uiState.value.capturedPhotoUri.orEmpty()
        val currentMoments = uiState.value.journeyPhotoMoments
        val existingIndex = currentMoments.indexOfLast { it.photoUri == currentPhoto }
        val questionRecord = JourneyQuestion(normalized, answer)
        val updatedMoments = if (existingIndex >= 0) {
            currentMoments.toMutableList().apply {
                val existing = this[existingIndex]
                this[existingIndex] = existing.copy(
                    label = subject,
                    questions = (existing.questions + questionRecord).takeLast(8),
                )
            }
        } else {
            (currentMoments + JourneyPhotoMoment(currentPhoto, subject, listOf(questionRecord))).takeLast(12)
        }
        uiState.value = uiState.value.copy(
            visualQuestion = normalized,
            visualAnswer = answer,
            journeyPhotoMoments = updatedMoments,
        )
        logEvent("circle_search_question", action = normalized, details = "subject=$subject;has_photo=${uiState.value.capturedPhotoUri != null}")
        persistState()
    }

    fun clearVisualQuestion() {
        uiState.value = uiState.value.copy(visualQuestion = "", visualAnswer = null)
        logEvent("circle_search_reset")
        persistState()
    }

    fun createPhotoCaptureUri(): Uri {
        val context = getApplication<Application>()
        val directory = File(context.cacheDir, "camera_captures").apply { mkdirs() }
        val file = File(directory, "sage_capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        pendingCaptureUri = uri
        return uri
    }

    fun onPhotoCaptureCompleted(success: Boolean) {
        val uri = pendingCaptureUri
        if (!success || uri == null) {
            uiState.value = uiState.value.copy(photoAnalysisStatus = "未完成拍照，可继续使用固定实验图片")
            logEvent("photo_capture_cancelled")
            return
        }
        val moments = uiState.value.journeyPhotoMoments
        uiState.value = uiState.value.copy(
            capturedPhotoUri = uri.toString(),
            capturedPhotoUris = (uiState.value.capturedPhotoUris + uri.toString()).distinct().takeLast(12),
            photoAnalysisStatus = "正在进行端侧多模态识别…",
            visionFindings = emptyList(),
            visualQuestion = "",
            visualAnswer = null,
            journeyPhotoMoments = if (moments.any { it.photoUri == uri.toString() }) moments else {
                (moments + JourneyPhotoMoment(uri.toString(), "正在识别")).takeLast(12)
            },
        )
        logEvent("photo_captured", details = "source=device_camera;uri=$uri")
        visionAnalyzer.analyze(
            uri = uri,
            onSuccess = { findings ->
                val label = findings.maxByOrNull { it.confidence }?.label ?: "旅程发现"
                uiState.value = uiState.value.copy(
                    visionFindings = findings,
                    photoAnalysisStatus = if (findings.isEmpty()) "没有识别到稳定线索，可重新拍摄" else "已提取 ${findings.size} 条图像线索",
                    journeyPhotoMoments = uiState.value.journeyPhotoMoments.map { moment ->
                        if (moment.photoUri == uri.toString()) moment.copy(label = label) else moment
                    },
                )
                logEvent(
                    "photo_analyzed",
                    details = findings.joinToString("|") { "${it.label}:${it.confidence}" },
                )
                persistState()
            },
            onFailure = { error ->
                uiState.value = uiState.value.copy(photoAnalysisStatus = "端侧识别未完成，仍可使用固定刺激继续")
                logEvent("photo_analysis_failed", details = error.javaClass.simpleName)
                persistState()
            },
        )
        persistState()
    }

    fun setConditionIndex(index: Int) {
        if (index !in uiState.value.order.conditions.indices) return
        stopRun()
        uiState.value = uiState.value.copy(
            conditionIndex = index,
            scenario = ExperimentScenario.ENVIRONMENT,
            aiStage = AiStage.IDLE,
            isRunning = false,
            resultVisible = false,
            evidenceVisible = false,
            researcherPanelVisible = false,
            selectedRoute = RouteChoice.RECOMMENDED,
            taskResult = null,
            completedScenarios = emptySet(),
            demoCompleted = false,
            capturedPhotoUri = null,
            capturedPhotoUris = emptyList(),
            visionFindings = emptyList(),
            routeConstraintText = "",
            routePreferenceIds = setOf("shade", "rest"),
            visualQuestion = "",
            visualAnswer = null,
            journeyPhotoMoments = emptyList(),
            voiceTranscript = "",
            voiceInteractionCount = 0,
            visualInteractionCount = 0,
            replanCount = 0,
        )
        logEvent("condition_selected")
        persistState()
    }

    fun nextCondition() {
        val next = (uiState.value.conditionIndex + 1)
            .coerceAtMost(uiState.value.order.conditions.lastIndex)
        setConditionIndex(next)
    }

    fun selectRoute(choice: RouteChoice) {
        uiState.value = uiState.value.copy(selectedRoute = choice)
        logEvent("route_selected", action = choice.logValue)
        persistState()
    }

    fun adoptResult() {
        val currentScenario = uiState.value.scenario
        val completed = uiState.value.completedScenarios + currentScenario
        logEvent(
            event = "result_adopted",
            action = uiState.value.selectedRoute.logValue,
            resultAdopted = "true",
        )
        val nextScenario = when (currentScenario) {
            ExperimentScenario.ENVIRONMENT, ExperimentScenario.VISUAL,
            ExperimentScenario.VOICE, ExperimentScenario.ADJUST -> ExperimentScenario.EXPLORE
            ExperimentScenario.CREATE -> null
            ExperimentScenario.EXPLORE -> ExperimentScenario.EXPLORE
        }
        uiState.value = uiState.value.copy(
            scenario = nextScenario ?: currentScenario,
            aiStage = if (nextScenario == null) AiStage.COMPLETE else AiStage.IDLE,
            isRunning = false,
            resultVisible = false,
            evidenceVisible = false,
            selectedRoute = RouteChoice.RECOMMENDED,
            taskResult = null,
            completedScenarios = completed,
            demoCompleted = currentScenario == ExperimentScenario.CREATE,
            voiceInteractionCount = uiState.value.voiceInteractionCount + if (currentScenario == ExperimentScenario.VOICE) 1 else 0,
            visualInteractionCount = uiState.value.visualInteractionCount + if (currentScenario == ExperimentScenario.VISUAL) 1 else 0,
            replanCount = uiState.value.replanCount + if (currentScenario == ExperimentScenario.ADJUST) 1 else 0,
            statusMessage = if (currentScenario == ExperimentScenario.CREATE) "完整体验已完成" else "已返回探索工作台，可继续调用功能",
        )
        if (currentScenario == ExperimentScenario.CREATE) logEvent("demo_completed")
        persistState()
    }

    fun beginJourneySummary() {
        logEvent("journey_summary_requested", details = "photos=${uiState.value.capturedPhotoUris.size};voice=${uiState.value.voiceInteractionCount};replans=${uiState.value.replanCount}")
        setScenario(ExperimentScenario.CREATE)
    }

    fun restartDemo() {
        stopRun()
        uiState.value = uiState.value.copy(
            scenario = ExperimentScenario.ENVIRONMENT,
            aiStage = AiStage.IDLE,
            isRunning = false,
            resultVisible = false,
            evidenceVisible = false,
            selectedRoute = RouteChoice.RECOMMENDED,
            taskResult = null,
            completedScenarios = emptySet(),
            demoCompleted = false,
            capturedPhotoUri = null,
            capturedPhotoUris = emptyList(),
            visionFindings = emptyList(),
            routeConstraintText = "",
            routePreferenceIds = setOf("shade", "rest"),
            visualQuestion = "",
            visualAnswer = null,
            journeyPhotoMoments = emptyList(),
            voiceTranscript = "",
            voiceInteractionCount = 0,
            visualInteractionCount = 0,
            replanCount = 0,
            statusMessage = null,
        )
        logEvent("demo_restarted")
        persistState()
    }

    fun showEvidence() {
        uiState.value = uiState.value.copy(evidenceVisible = true)
        logEvent("evidence_opened", action = "view_evidence")
    }

    fun hideEvidence() {
        uiState.value = uiState.value.copy(evidenceVisible = false)
        logEvent("evidence_closed")
    }

    fun cancelTask() {
        stopRun()
        uiState.value = uiState.value.copy(
            aiStage = AiStage.IDLE,
            isRunning = false,
            resultVisible = false,
            evidenceVisible = false,
            statusMessage = "本次任务已取消，可安全重新开始",
            taskResult = null,
        )
        logEvent("task_cancelled", action = "cancel")
        persistState()
    }

    fun resetTask() {
        stopRun()
        uiState.value = uiState.value.copy(
            aiStage = AiStage.IDLE,
            isRunning = false,
            resultVisible = false,
            evidenceVisible = false,
            selectedRoute = RouteChoice.RECOMMENDED,
            statusMessage = null,
            taskResult = null,
        )
        logEvent("task_reset")
        persistState()
    }

    fun previewPreviousStage() = previewStage(step = -1)

    fun previewNextStage() = previewStage(step = 1)

    fun setResearcherPanelVisible(visible: Boolean) {
        uiState.value = uiState.value.copy(researcherPanelVisible = visible)
        if (visible) logEvent("researcher_panel_opened")
    }

    fun openHistory() {
        uiState.value = uiState.value.copy(
            historyVisible = true,
            historySessions = logger.listSessionSummaries(),
            selectedHistory = null,
            researcherPanelVisible = false,
        )
    }

    fun closeHistory() {
        uiState.value = uiState.value.copy(historyVisible = false, selectedHistory = null)
    }

    fun selectHistory(fileName: String) {
        uiState.value = uiState.value.copy(selectedHistory = logger.sessionDetail(fileName))
    }

    fun closeHistoryDetail() {
        uiState.value = uiState.value.copy(selectedHistory = null)
    }

    fun deleteHistorySession(fileName: String) {
        val deleted = logger.deleteSession(fileName)
        uiState.value = uiState.value.copy(
            historySessions = logger.listSessionSummaries(),
            selectedHistory = null,
            statusMessage = if (deleted) "该会话及关联照片已删除" else "当前进行中的会话不能删除",
        )
    }

    fun deleteAllHistory() {
        val deleted = logger.deleteAllStoredData()
        uiState.value = uiState.value.copy(
            historySessions = logger.listSessionSummaries(),
            selectedHistory = null,
            statusMessage = "已删除 $deleted 个历史会话",
        )
    }

    fun createCurrentShareIntent(): Intent? =
        logger.latestLogFile()?.let { file -> shareIntent(file, "text/csv", "SAGE 实验日志 ${uiState.value.participantId}") }

    fun createSessionShareIntent(fileName: String): Intent? =
        logger.fileByName(fileName)?.let { file -> shareIntent(file, "text/csv", "SAGE 历史实验日志") }

    fun createAllSessionsShareIntent(): Intent? =
        logger.createAllSessionsArchive()?.let { file -> shareIntent(file, "application/zip", "SAGE 全部实验数据") }

    fun createJourneyShareIntent(): Intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "我的 SAGE 公园知识游记")
        putExtra(
            Intent.EXTRA_TEXT,
            "我的公园知识游记\n\n路线：湖边林荫线 · 约 850 米\n过程照片：${uiState.value.capturedPhotoUris.size} 张\n照片问题：${uiState.value.journeyPhotoMoments.sumOf { it.questions.size }} 个\n拍照询问：${uiState.value.visualInteractionCount} 次\n语音对话：${uiState.value.voiceInteractionCount} 次\n动态重规划：${uiState.value.replanCount} 次\n\n由 SAGE Demo 根据本次旅程素材生成，分享前请核查内容。",
        )
    }

    override fun onCleared() {
        runJob?.cancel()
        visionAnalyzer.close()
        super.onCleared()
    }

    private fun restoreActiveSession() {
        val restored = sessionStore.restore()
        if (restored == null) {
            uiState.value = ExperimentUiState(historySessions = logger.listSessionSummaries())
            return
        }
        if (!logger.resumeSession(restored.logFileName)) {
            sessionStore.clear()
            uiState.value = ExperimentUiState(
                historySessions = logger.listSessionSummaries(),
                statusMessage = "上次会话文件不存在，已安全回到开始页",
            )
            return
        }
        val restoredMoments = restored.journeyPhotoMoments.ifEmpty {
            restored.capturedPhotoUris.mapIndexed { index, uri ->
                val isCurrent = uri == restored.capturedPhotoUri
                JourneyPhotoMoment(
                    photoUri = uri,
                    label = if (isCurrent) restored.visionFindings.maxByOrNull { it.confidence }?.label ?: "旅程发现" else "沿途发现 ${index + 1}",
                    questions = if (isCurrent && restored.visualQuestion.isNotBlank() && restored.visualAnswer != null) {
                        listOf(JourneyQuestion(restored.visualQuestion, restored.visualAnswer))
                    } else {
                        emptyList()
                    },
                )
            }
        }
        uiState.value = ExperimentUiState(
            sessionStarted = true,
            participantId = restored.participantId,
            order = restored.order,
            demoMode = restored.demoMode,
            conditionIndex = restored.conditionIndex,
            scenario = restored.scenario,
            aiStage = if (restored.demoCompleted) AiStage.COMPLETE else AiStage.IDLE,
            completedScenarios = restored.completedScenarios,
            completedTaskCount = restored.completedTaskCount,
            demoCompleted = restored.demoCompleted,
            voiceTranscript = restored.voiceTranscript,
            routeConstraintText = restored.routeConstraintText,
            routePreferenceIds = restored.routePreferenceIds,
            capturedPhotoUri = restored.capturedPhotoUri,
            capturedPhotoUris = restored.capturedPhotoUris,
            visionFindings = restored.visionFindings,
            visualQuestion = restored.visualQuestion,
            visualAnswer = restored.visualAnswer,
            journeyPhotoMoments = restoredMoments,
            voiceInteractionCount = restored.voiceInteractionCount,
            visualInteractionCount = restored.visualInteractionCount,
            replanCount = restored.replanCount,
            statusMessage = if (restored.demoCompleted) "已恢复完成的实验会话" else "已恢复上次会话 · 中断任务可重新开始",
        )
        logEvent("session_restored")
        persistState()
    }

    private fun enterStage(stage: AiStage) {
        val previous = uiState.value.aiStage
        if (previous != AiStage.IDLE && previous != stage) {
            logEvent("state_exit", details = "next=${stage.id}")
        }
        uiState.value = uiState.value.copy(aiStage = stage)
        logEvent("state_enter")
        persistState()
    }

    private fun previewStage(step: Int) {
        stopRun()
        val stages = listOf(AiStage.IDLE) + stageSequenceFor(uiState.value.scenario)
        val currentIndex = stages.indexOf(uiState.value.aiStage).coerceAtLeast(0)
        val target = stages[(currentIndex + step).coerceIn(stages.indices)]
        uiState.value = uiState.value.copy(
            aiStage = target,
            isRunning = target != AiStage.IDLE,
            resultVisible = false,
            taskResult = null,
            researcherPanelVisible = false,
            statusMessage = if (target == AiStage.IDLE) null else "手动预览 · ${target.label}",
        )
        logEvent("stage_previewed", details = target.id)
        persistState()
    }

    private fun stopRun() {
        runJob?.cancel()
        runJob = null
    }

    private fun routePreferenceLabels(ids: Set<String>): String = listOf(
        "shade" to "阴凉优先",
        "rest" to "沿途有座椅",
        "short" to "路程短",
        "quiet" to "避开人群",
    ).filter { it.first in ids }.joinToString("、") { it.second }

    private fun persistState() {
        sessionStore.save(uiState.value, logger.activeLogFileName())
    }

    private fun shareIntent(file: File, mimeType: String, subject: String): Intent {
        val context = getApplication<Application>()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun logEvent(
        event: String,
        action: String = "",
        resultAdopted: String = "",
        details: String = "",
    ) {
        val state = uiState.value
        if (!state.sessionStarted) return
        logger.log(
            participantId = state.participantId,
            condition = state.condition,
            conditionOrder = state.order.name,
            scenario = state.scenario,
            stage = state.aiStage,
            event = event,
            action = action,
            resultAdopted = resultAdopted,
            details = details,
        )
    }
}
