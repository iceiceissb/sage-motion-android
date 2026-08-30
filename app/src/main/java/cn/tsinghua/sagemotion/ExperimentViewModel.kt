package cn.tsinghua.sagemotion

import android.app.Application
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.tsinghua.sagemotion.data.AiTaskEvent
import cn.tsinghua.sagemotion.data.AiTaskRequest
import cn.tsinghua.sagemotion.data.AgentJourneyContext
import cn.tsinghua.sagemotion.data.ExperimentLogger
import cn.tsinghua.sagemotion.data.ExperimentSessionStore
import cn.tsinghua.sagemotion.data.JourneyShareRenderer
import cn.tsinghua.sagemotion.data.agent.ParkAgentRuntime
import cn.tsinghua.sagemotion.data.vision.OnDeviceVisionAnalyzer
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ConditionOrder
import cn.tsinghua.sagemotion.model.DemoMode
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.ExperimentUiState
import cn.tsinghua.sagemotion.model.JourneyPhotoMoment
import cn.tsinghua.sagemotion.model.JourneyQuestion
import cn.tsinghua.sagemotion.model.LandmarkStyle
import cn.tsinghua.sagemotion.model.PostTaskMeasurement
import cn.tsinghua.sagemotion.model.RouteChoice
import cn.tsinghua.sagemotion.model.SurveyDimension
import cn.tsinghua.sagemotion.model.TaskPerformance
import cn.tsinghua.sagemotion.model.stageSequenceFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.File

class ExperimentViewModel(application: Application) : AndroidViewModel(application) {
    private val logger = ExperimentLogger(application)
    private val sessionStore = ExperimentSessionStore(application)
    private val agentRuntime = ParkAgentRuntime.create(application)
    private val visionAnalyzer = OnDeviceVisionAnalyzer(application)
    private val journeyShareRenderer = JourneyShareRenderer(application)
    private var pendingCaptureUri: Uri? = null
    private var runJob: Job? = null

    var uiState = androidx.compose.runtime.mutableStateOf(ExperimentUiState())
        private set

    init {
        restoreActiveSession()
    }

    fun startSession(
        participantId: String,
    ) {
        val order = ConditionOrder.ABC
        val demoMode = DemoMode.ONLINE_AGENT
        val landmarkStyle = LandmarkStyle.DEPTH
        val normalizedId = participantId.trim().ifBlank { "P000" }
        logger.startSession(normalizedId)
        uiState.value = ExperimentUiState(
            sessionStarted = true,
            participantId = normalizedId,
            order = order,
            demoMode = demoMode,
            landmarkStyle = landmarkStyle,
            welcomeShown = false,
            statusMessage = "实验会话已开始 · 数据实时保存在本机",
        )
        logEvent(
            "session_started",
            details = "order=${order.name};conditions=${order.conditions.size};mode=${demoMode.name};landmark=${landmarkStyle.name};device=${Build.MANUFACTURER} ${Build.MODEL};android=${Build.VERSION.SDK_INT};app=${BuildConfig.VERSION_NAME}",
        )
        persistState()
    }

    /**
     * 结束开屏欢迎动画，进入 A 任务。
     *
     * 单独记一条事件，这样分析时可以把欢迎页停留时长从第一个任务的反应时里剔除。
     */
    fun completeWelcome() {
        if (uiState.value.welcomeShown) return
        uiState.value = uiState.value.copy(welcomeShown = true)
        logEvent("welcome_completed")
        persistState()
    }

    /** 切换沿途地标的呈现形态（数字 / 图标 / 立体）。 */
    fun setLandmarkStyle(style: LandmarkStyle) {
        if (uiState.value.landmarkStyle == style) return
        uiState.value = uiState.value.copy(landmarkStyle = style)
        logEvent("landmark_style_changed", action = style.name)
        persistState()
    }

    fun finishSession() {
        if (!uiState.value.sessionStarted) return
        stopRun()
        persistJourneyImageIfAvailable()
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
        val now = System.currentTimeMillis()
        uiState.value = uiState.value.copy(
            isRunning = true,
            resultVisible = false,
            evidenceVisible = false,
            selectedRoute = RouteChoice.RECOMMENDED,
            statusMessage = null,
            taskResult = null,
            taskStartedAtMillis = uiState.value.taskStartedAtMillis.takeIf { it > 0L } ?: now,
            resultPresentedAtMillis = 0L,
            taskAttemptCount = uiState.value.taskAttemptCount + 1,
        )
        logEvent("task_started", details = "attempt=${uiState.value.taskAttemptCount};task_instance=${uiState.value.completedTaskCount + 1}")
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
                ExperimentScenario.ADJUST -> uiState.value.replanRequestText.ifBlank { uiState.value.scenario.participantPrompt }
                else -> uiState.value.scenario.participantPrompt
            },
            visionFindings = if (uiState.value.scenario == ExperimentScenario.VISUAL) uiState.value.visionFindings else emptyList(),
            journeyContext = AgentJourneyContext(
                activeRouteName = uiState.value.activeRouteName,
                routeReplanned = uiState.value.routeReplanned,
                previousVoiceTurns = uiState.value.voiceTranscripts.takeLast(12),
                visionLabels = (
                    uiState.value.visionFindings.map { it.label } +
                        uiState.value.journeyPhotoMoments.map { it.label }
                    ).distinct().takeLast(24),
                photoQuestionCount = uiState.value.journeyPhotoMoments.sumOf { it.questions.size },
                visualInteractionCount = uiState.value.visualInteractionCount,
                voiceInteractionCount = uiState.value.voiceInteractionCount,
                replanCount = uiState.value.replanCount,
            ),
        )
        logEvent("task_input_submitted", action = "submit", details = "prompt=${request.prompt.replace(';', '；').replace('\n', ' ').take(180)}")
        runJob = viewModelScope.launch {
            try {
                val api = agentRuntime.apiFor(uiState.value.demoMode)
                api.runTask(request).collect { event ->
                    when (event) {
                        is AiTaskEvent.StageChanged -> enterStage(event.stage)
                        is AiTaskEvent.Completed -> {
                            val resultPresentedAt = System.currentTimeMillis()
                            val completedScenario = uiState.value.scenario
                            val completedVoice = uiState.value.voiceTranscript.trim()
                            uiState.value = uiState.value.copy(
                                isRunning = false,
                                resultVisible = true,
                                taskResult = event.result,
                                resultPresentedAtMillis = resultPresentedAt,
                                voiceInteractionCount = uiState.value.voiceInteractionCount + if (completedScenario == ExperimentScenario.VOICE) 1 else 0,
                                voiceTranscripts = if (completedScenario == ExperimentScenario.VOICE && completedVoice.isNotBlank()) {
                                    (uiState.value.voiceTranscripts + completedVoice).takeLast(12)
                                } else {
                                    uiState.value.voiceTranscripts
                                },
                                visualInteractionCount = uiState.value.visualInteractionCount + if (completedScenario == ExperimentScenario.VISUAL) 1 else 0,
                                replanCount = uiState.value.replanCount + if (completedScenario == ExperimentScenario.ADJUST) 1 else 0,
                            )
                            logEvent(
                                "task_result_visible",
                                details = "source=${event.result.sourceLabel};live=${event.result.isLiveData};completion_time_ms=${(resultPresentedAt - uiState.value.taskStartedAtMillis).coerceAtLeast(0L)}",
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
        var next = uiState.value.copy(
            scenario = scenario,
            aiStage = AiStage.IDLE,
            isRunning = false,
            resultVisible = false,
            evidenceVisible = false,
            researcherPanelVisible = false,
            selectedRoute = RouteChoice.RECOMMENDED,
            taskResult = null,
            demoCompleted = false,
            taskStartedAtMillis = 0L,
            resultPresentedAtMillis = 0L,
            taskMisoperationCount = 0,
            taskAttemptCount = 0,
            pendingPostTaskSurvey = null,
        )
        next = when (scenario) {
            ExperimentScenario.VISUAL -> next.copy(
                capturedPhotoUri = null,
                visionFindings = emptyList(),
                photoAnalysisStatus = null,
                visualQuestion = "",
                visualAnswer = null,
            )
            ExperimentScenario.VOICE -> next.copy(voiceTranscript = "")
            ExperimentScenario.ADJUST -> next.copy(replanRequestText = "")
            else -> next
        }
        uiState.value = next
        logEvent("scenario_selected", details = scenario.id)
        persistState()
    }

    fun setVoiceTranscript(transcript: String) {
        val normalized = transcript.trim().take(160)
        uiState.value = uiState.value.copy(voiceTranscript = normalized)
        persistState()
    }

    fun setRouteConstraint(text: String) {
        uiState.value = uiState.value.copy(routeConstraintText = text.take(160))
        persistState()
    }

    fun setReplanRequest(text: String) {
        uiState.value = uiState.value.copy(replanRequestText = text.take(160))
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
        val usesFixedFlowerStimulus = uiState.value.capturedPhotoUri == null
        val subject = visualSubject(finding?.label, usesFixedFlowerStimulus)
        val confidence = finding?.let { "（端侧识别置信度 ${(it.confidence * 100).toInt()}%）" }.orEmpty()
        val answer = when {
            "是什么" in normalized || "什么花" in normalized -> if (usesFixedFlowerStimulus || subject.contains("花")) {
                "画面中是一簇粉红色蔷薇科花卉，外观更接近月季或蔷薇。可以看到成簇花朵、五枚展开花瓣和有锯齿的复叶；仅凭这张照片还不能可靠确定具体品种。"
            } else {
                "圈选主体最可能是“$subject”。$confidence 这是端侧通用图像标签给出的类别线索，仍建议结合实物和现场标牌核查。"
            }
            "特点" in normalized -> if (usesFixedFlowerStimulus || subject.contains("花")) {
                "这簇花呈粉红色，花朵成簇开放，花瓣较薄，叶片为绿色复叶且边缘有细锯齿。这些特征符合常见月季或蔷薇类植物，但不足以精确到品种。"
            } else {
                "$subject 的轮廓、表面和周围环境构成了当前可见特征。$confidence 建议结合圈选范围和现场距离继续观察。"
            }
            "为什么" in normalized || "原因" in normalized -> if (usesFixedFlowerStimulus || subject.contains("花")) {
                "这类月季或蔷薇常被种在公园花境和步道旁：花期观赏性强，成簇生长容易形成连续景观，也能为昆虫提供花粉与花蜜。具体栽植原因仍以园区说明为准。"
            } else {
                "从画面线索看，它与当前位置的光照、植被和使用场景有关。$confidence 这是基于图像的解释，现场标牌会是更可靠的补充依据。"
            }
            "拍" in normalized || "记录" in normalized -> "适合记录。可以保留圈选主体，并让周围环境占画面约三分之一，这样知识游记既有细节也有地点语境。"
            else -> "你的问题指向画面中的“$subject”。$confidence 目前可以确认它的基础视觉类别，但更细的身份或成因仍需要现场信息补充。"
        }
        // 固定刺激也必须按任务实例建独立节点，不能把第二次使用继续追加到第一次答案上。
        val currentPhoto = uiState.value.capturedPhotoUri
            ?: "fixed://flower/${uiState.value.visualInteractionCount.coerceAtLeast(1)}"
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
            recordMisoperation("photo_capture_cancelled")
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
            adoptedRoute = RouteChoice.RECOMMENDED,
            taskResult = null,
            completedScenarios = emptySet(),
            demoCompleted = false,
            capturedPhotoUri = null,
            capturedPhotoUris = emptyList(),
            visionFindings = emptyList(),
            routeConstraintText = "",
            replanRequestText = "",
            routePreferenceIds = setOf("shade", "rest"),
            visualQuestion = "",
            visualAnswer = null,
            journeyPhotoMoments = emptyList(),
            voiceTranscript = "",
            voiceTranscripts = emptyList(),
            voiceInteractionCount = 0,
            visualInteractionCount = 0,
            replanCount = 0,
            routeReplanned = false,
            taskStartedAtMillis = 0L,
            resultPresentedAtMillis = 0L,
            taskMisoperationCount = 0,
            taskAttemptCount = 0,
            pendingPostTaskSurvey = null,
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
        if (!uiState.value.resultVisible) return
        val currentScenario = uiState.value.scenario
        val now = System.currentTimeMillis()
        val performance = TaskPerformance(
            taskInstance = uiState.value.completedTaskCount + 1,
            scenario = currentScenario,
            completionTimeMs = (uiState.value.resultPresentedAtMillis - uiState.value.taskStartedAtMillis).coerceAtLeast(0L),
            decisionTimeMs = (now - uiState.value.resultPresentedAtMillis).coerceAtLeast(0L),
            misoperationCount = uiState.value.taskMisoperationCount,
            attemptCount = uiState.value.taskAttemptCount.coerceAtLeast(1),
        )
        val operationTimeMs = (now - uiState.value.taskStartedAtMillis).coerceAtLeast(0L)
        logEvent(
            event = "result_adopted",
            action = uiState.value.selectedRoute.logValue,
            resultAdopted = "true",
            details = "task_instance=${performance.taskInstance};completion_time_ms=${performance.completionTimeMs};decision_time_ms=${performance.decisionTimeMs};operation_time_ms=$operationTimeMs;misoperations=${performance.misoperationCount};attempts=${performance.attemptCount}",
        )
        logEvent(
            event = "task_measurement",
            action = "task_completed",
            details = "measurement_version=2;operation_time_ms=$operationTimeMs",
            measurement = PostTaskMeasurement(performance, emptyMap()),
        )
        completeTask(performance.scenario)
    }

    private fun completeTask(currentScenario: ExperimentScenario) {
        val nextScenario = when (currentScenario) {
            ExperimentScenario.ENVIRONMENT, ExperimentScenario.VISUAL,
            ExperimentScenario.VOICE, ExperimentScenario.ADJUST -> ExperimentScenario.EXPLORE
            ExperimentScenario.CREATE -> null
            ExperimentScenario.EXPLORE -> ExperimentScenario.EXPLORE
        }
        uiState.value = uiState.value.copy(
            scenario = nextScenario ?: currentScenario,
            aiStage = if (nextScenario == null) AiStage.COMPLETE else AiStage.IDLE,
            resultVisible = false,
            isRunning = false,
            evidenceVisible = false,
            selectedRoute = RouteChoice.RECOMMENDED,
            taskResult = null,
            completedScenarios = uiState.value.completedScenarios + currentScenario,
            completedTaskCount = uiState.value.completedTaskCount + 1,
            demoCompleted = currentScenario == ExperimentScenario.CREATE,
            taskStartedAtMillis = 0L,
            resultPresentedAtMillis = 0L,
            taskMisoperationCount = 0,
            taskAttemptCount = 0,
            pendingPostTaskSurvey = null,
            routeReplanned = if (currentScenario == ExperimentScenario.ADJUST) {
                uiState.value.selectedRoute == RouteChoice.RECOMMENDED
            } else {
                uiState.value.routeReplanned
            },
            adoptedRoute = if (currentScenario == ExperimentScenario.ENVIRONMENT) {
                uiState.value.selectedRoute
            } else {
                uiState.value.adoptedRoute
            },
            statusMessage = if (currentScenario == ExperimentScenario.CREATE) "完整体验已完成" else "任务操作时间已保存 · 已返回探索工作台",
        )
        if (currentScenario == ExperimentScenario.CREATE) {
            logEvent("demo_completed")
            persistJourneyImageIfAvailable()
        }
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
            adoptedRoute = RouteChoice.RECOMMENDED,
            taskResult = null,
            completedScenarios = emptySet(),
            demoCompleted = false,
            capturedPhotoUri = null,
            capturedPhotoUris = emptyList(),
            visionFindings = emptyList(),
            routeConstraintText = "",
            replanRequestText = "",
            routePreferenceIds = setOf("shade", "rest"),
            visualQuestion = "",
            visualAnswer = null,
            journeyPhotoMoments = emptyList(),
            voiceTranscript = "",
            voiceTranscripts = emptyList(),
            voiceInteractionCount = 0,
            visualInteractionCount = 0,
            replanCount = 0,
            routeReplanned = false,
            taskStartedAtMillis = 0L,
            resultPresentedAtMillis = 0L,
            taskMisoperationCount = 0,
            taskAttemptCount = 0,
            pendingPostTaskSurvey = null,
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
        val nextMisoperations = uiState.value.taskMisoperationCount + 1
        uiState.value = uiState.value.copy(
            aiStage = AiStage.IDLE,
            isRunning = false,
            resultVisible = false,
            evidenceVisible = false,
            statusMessage = "本次任务已取消，可安全重新开始",
            taskResult = null,
            resultPresentedAtMillis = 0L,
            taskMisoperationCount = nextMisoperations,
        )
        logEvent("task_cancelled", action = "cancel", details = "misoperation_count=$nextMisoperations")
        persistState()
    }

    fun resetTask() {
        stopRun()
        val nextMisoperations = uiState.value.taskMisoperationCount + 1
        uiState.value = uiState.value.copy(
            aiStage = AiStage.IDLE,
            isRunning = false,
            resultVisible = false,
            evidenceVisible = false,
            selectedRoute = RouteChoice.RECOMMENDED,
            statusMessage = null,
            taskResult = null,
            resultPresentedAtMillis = 0L,
            taskMisoperationCount = nextMisoperations,
        )
        logEvent("task_reset", details = "misoperation_count=$nextMisoperations")
        persistState()
    }

    fun recordMisoperation(reason: String = "researcher_observed") {
        val next = uiState.value.taskMisoperationCount + 1
        uiState.value = uiState.value.copy(taskMisoperationCount = next)
        logEvent("misoperation_recorded", action = reason, details = "misoperation_count=$next")
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
        logger.createCurrentCsvExport()?.let { file -> shareIntent(file, "text/csv", "SAGE 实验日志 ${uiState.value.participantId}") }

    fun createCurrentSessionArchiveShareIntent(): Intent? =
        logger.createCurrentSessionArchive()?.let { file -> shareIntent(file, "application/zip", "SAGE 会话数据 ${uiState.value.participantId}") }

    fun createSessionShareIntent(fileName: String): Intent? =
        logger.createSessionCsvExport(fileName)?.let { file -> shareIntent(file, "text/csv", "SAGE 历史实验日志") }

    fun createHistoryJourneyShareIntent(fileName: String): Intent? =
        logger.sessionJourneyImage(fileName)?.let { file ->
            shareIntent(file, "image/png", "SAGE 历史知识游记").apply {
                putExtra(Intent.EXTRA_TEXT, "我的公园知识游记")
            }
        }

    fun createAllSessionsShareIntent(): Intent? =
        logger.createAllSessionsArchive()?.let { file -> shareIntent(file, "application/zip", "SAGE 全部实验数据") }

    fun createJourneyShareIntent(): Intent {
        val caption = "我的公园知识游记 · ${uiState.value.activeRouteName}\n${uiState.value.journeyPhotoMoments.size} 张照片 · ${uiState.value.voiceInteractionCount} 次语音 · ${uiState.value.replanCount} 次改道"
        val image = journeyShareRenderer.render(uiState.value)
        return if (image != null) {
            shareIntent(image, "image/png", "我的 SAGE 公园知识游记").apply {
                putExtra(Intent.EXTRA_TEXT, caption)
            }
        } else {
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "我的 SAGE 公园知识游记")
                putExtra(Intent.EXTRA_TEXT, caption)
            }
        }
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
            landmarkStyle = restored.landmarkStyle,
            // 恢复会话时不重播开屏动画：参与者已经进入过场景，重播会干扰实验节奏。
            welcomeShown = true,
            conditionIndex = restored.conditionIndex,
            scenario = restored.scenario,
            aiStage = if (restored.demoCompleted) AiStage.COMPLETE else AiStage.IDLE,
            completedScenarios = restored.completedScenarios,
            completedTaskCount = restored.completedTaskCount,
            demoCompleted = restored.demoCompleted,
            voiceTranscript = restored.voiceTranscript,
            routeConstraintText = restored.routeConstraintText,
            replanRequestText = restored.replanRequestText,
            routePreferenceIds = restored.routePreferenceIds,
            adoptedRoute = restored.adoptedRoute,
            capturedPhotoUri = restored.capturedPhotoUri,
            capturedPhotoUris = restored.capturedPhotoUris,
            visionFindings = restored.visionFindings,
            visualQuestion = restored.visualQuestion,
            visualAnswer = restored.visualAnswer,
            journeyPhotoMoments = restoredMoments,
            voiceInteractionCount = restored.voiceInteractionCount,
            voiceTranscripts = restored.voiceTranscripts,
            visualInteractionCount = restored.visualInteractionCount,
            replanCount = restored.replanCount,
            routeReplanned = restored.routeReplanned,
            taskStartedAtMillis = restored.taskStartedAtMillis,
            resultPresentedAtMillis = restored.resultPresentedAtMillis,
            taskMisoperationCount = restored.taskMisoperationCount,
            taskAttemptCount = restored.taskAttemptCount,
            pendingPostTaskSurvey = restored.pendingPostTaskSurvey,
            statusMessage = when {
                restored.pendingPostTaskSurvey != null -> "已恢复待提交的任务后问卷"
                restored.demoCompleted -> "已恢复完成的实验会话"
                else -> "已恢复上次会话 · 中断任务可重新开始"
            },
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

    private fun visualSubject(label: String?, fixedFlowerStimulus: Boolean): String {
        if (fixedFlowerStimulus) return "粉红色月季或蔷薇类花卉"
        val normalized = label.orEmpty().trim()
        return when {
            normalized.isBlank() -> "圈选主体"
            normalized in setOf("主体区域", "环境线索", "表面特征", "圈选区域", "圈选内容") -> "圈选主体"
            normalized in setOf("花朵", "植物", "花瓣", "花园", "叶片", "自然") -> "画面中的花卉植物"
            else -> normalized
        }
    }

    private fun persistState() {
        sessionStore.save(uiState.value, logger.activeLogFileName())
    }

    private fun persistJourneyImageIfAvailable() {
        if (uiState.value.journeyPhotoMoments.isEmpty() && uiState.value.completedTaskCount == 0) return
        journeyShareRenderer.render(uiState.value)?.let(logger::storeJourneyImage)
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
        measurement: PostTaskMeasurement? = null,
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
            measurement = measurement,
        )
    }
}
