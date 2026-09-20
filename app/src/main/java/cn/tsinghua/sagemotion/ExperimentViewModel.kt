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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import cn.tsinghua.sagemotion.model.*
import cn.tsinghua.sagemotion.data.bindRouteResult
import cn.tsinghua.sagemotion.data.vision.PhotoAssets
import java.io.File

class ExperimentViewModel(application: Application) : AndroidViewModel(application) {
    private val logger = ExperimentLogger(application)
    private val sessionStore = ExperimentSessionStore(application)
    private val agentRuntime = ParkAgentRuntime.create(application)
    private val visionAnalyzer = OnDeviceVisionAnalyzer(application)
    private val journeyShareRenderer = JourneyShareRenderer(application)
    private var pendingCaptureUri: Uri? = null
    private var runJob: Job? = null
    private var visualJob: Job? = null
    private var zineJob: Job? = null
    private val photoAssets = PhotoAssets(application)
    private var latestFix: GeoFix? = null

    var uiState = androidx.compose.runtime.mutableStateOf(ExperimentUiState())
        private set

    init {
        restoreActiveSession()
    }

    fun startSession(
        participantId: String,
    ) {
        latestFix = null
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
        latestFix = null
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
            hasCapturedPhoto = uiState.value.capturedPhotoUri != null,
            visionFindings = if (uiState.value.scenario == ExperimentScenario.VISUAL) uiState.value.visionFindings else emptyList(),
            visionImageUri = uiState.value.capturedPhotoUri.takeIf {
                uiState.value.scenario == ExperimentScenario.VISUAL &&
                    uiState.value.demoMode == DemoMode.ONLINE_AGENT &&
                    uiState.value.cloudVisionUploadApproved
            },
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
                            val boundResult = bindCurrentRoutes(event.result)
                            val resultPresentedAt = System.currentTimeMillis()
                            val completedScenario = uiState.value.scenario
                            val completedVoice = uiState.value.voiceTranscript.trim()
                            uiState.value = uiState.value.copy(
                                isRunning = false,
                                resultVisible = true,
                                taskResult = boundResult,
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
                            if (completedScenario == ExperimentScenario.VOICE) recordJourneyEvent("voice", completedVoice)
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
                selectedImageRegion = null, selectedImageUri = null, selectedImageFindings = emptyList(),
                visionFindings = emptyList(),
                photoAnalysisStatus = null,
                cloudVisionUploadApproved = false,
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

    fun setCloudVisionUploadApproved(approved: Boolean) {
        val state = uiState.value
        if (
            state.scenario != ExperimentScenario.VISUAL ||
            state.demoMode != DemoMode.ONLINE_AGENT ||
            state.capturedPhotoUri == null
        ) return
        uiState.value = state.copy(cloudVisionUploadApproved = approved)
        logEvent(if (approved) "cloud_vision_upload_approved" else "cloud_vision_upload_revoked")
        // Deliberately not persisted: reopening the app always requires fresh consent.
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

    fun selectVisualRegion(region: ImageRegion?) {
        visualJob?.cancel()
        val photo = uiState.value.capturedPhotoUri
        uiState.value = uiState.value.copy(selectedImageRegion = region, selectedImageUri = null,
            selectedImageFindings = emptyList(), visualAnswer = null, visualQuestion = "",
            visualAnswerBusy = false, visualSelectionBusy = region != null && photo != null)
        if (region == null || photo == null) return
        visualJob = viewModelScope.launch {
            try {
                val cropped = withContext(Dispatchers.IO) { photoAssets.crop(photo, region) }
                val findings = visionAnalyzer.analyzeSuspending(cropped)
                uiState.value = uiState.value.copy(selectedImageUri = cropped.toString(), selectedImageFindings = findings,
                    visualSelectionBusy = false, photoAnalysisStatus = "已识别选区 · ${findings.size} 条类别线索")
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                uiState.value = uiState.value.copy(visualSelectionBusy = false, photoAnalysisStatus = "选区分析失败，请重圈或重新拍摄")
            }
        }
    }

    fun askVisualQuestion(question: String) {
        val normalized = question.trim().take(120)
        val state = uiState.value
        if (normalized.isBlank() || state.visualSelectionBusy || state.visualAnswerBusy) return
        if (state.selectedImageRegion != null && state.capturedPhotoUri != null && state.selectedImageUri == null) {
            uiState.value = state.copy(visualAnswer = "选区尚未识别成功，请重新圈选后提问。")
            return
        }
        visualJob?.cancel()
        val selected = state.selectedImageUri != null
        val findings = if (selected) state.selectedImageFindings else state.visionFindings
        val subject = visualSubject(findings.maxByOrNull { it.confidence }?.label, state.capturedPhotoUri == null)
        val prior = state.journeyPhotoMoments.lastOrNull { it.photoUri == state.capturedPhotoUri }?.questions.orEmpty().takeLast(3)
        val request = AiTaskRequest(
            scenario = ExperimentScenario.VISUAL,
            prompt = buildString {
                if (prior.isNotEmpty()) append("同一张照片此前的问答：" + prior.joinToString("；") { "${it.question}：${it.answer.take(180)}" } + "。")
                append("本次问题：$normalized。" + if (selected) "图片是用户圈选区域的裁剪图。" else "请分析整张照片。")
            },
            hasCapturedPhoto = state.capturedPhotoUri != null,
            visualQuestion = normalized,
            visionIsRegion = selected,
            visionFindings = findings,
            visionImageUri = (state.selectedImageUri ?: state.capturedPhotoUri).takeIf {
                state.cloudVisionUploadApproved && state.demoMode == DemoMode.ONLINE_AGENT
            },
        )
        uiState.value = state.copy(visualAnswerBusy = true, visualQuestion = normalized, visualAnswer = null)
        visualJob = viewModelScope.launch {
            try {
                agentRuntime.apiFor(state.demoMode).runTask(request).collect { event ->
                    if (event is AiTaskEvent.Completed) {
                        saveVisualAnswer(normalized, subject, event.result.summary)
                        uiState.value = uiState.value.copy(visualAnswerBusy = false, visualAnswerSource = event.result.sourceLabel, taskResult = event.result)
                    }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                uiState.value = uiState.value.copy(visualAnswerBusy = false, visualAnswer = "本次回答未完成，照片已保留，请重试。", visualAnswerSource = "请求失败")
            }
        }
    }

    private fun saveVisualAnswer(normalized: String, subject: String, answer: String) {
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
        visualJob?.cancel()
        uiState.value = uiState.value.copy(visualQuestion = "", visualAnswer = null, visualAnswerBusy = false, visualAnswerSource = null)
        logEvent("circle_search_reset")
        persistState()
    }

    fun createPhotoCaptureUri(): Uri {
        val context = getApplication<Application>()
        val directory = File(context.filesDir, "camera_captures").apply { mkdirs() }
        val file = File(directory, "sage_capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        pendingCaptureUri = uri
        return uri
    }

    fun onPhotoCaptureCompleted(success: Boolean) {
        visualJob?.cancel()
        val uri = pendingCaptureUri
        if (!success || uri == null) {
            uiState.value = uiState.value.copy(photoAnalysisStatus = "未完成拍照，可继续使用固定实验图片")
            recordMisoperation("photo_capture_cancelled")
            return
        }
        val moments = uiState.value.journeyPhotoMoments
        uiState.value = uiState.value.copy(
            capturedPhotoUri = uri.toString(),
            selectedImageRegion = null, selectedImageUri = null, selectedImageFindings = emptyList(),
            capturedPhotoUris = (uiState.value.capturedPhotoUris + uri.toString()).distinct().takeLast(12),
            photoAnalysisStatus = "正在提取端侧图像标签…",
            cloudVisionUploadApproved = false,
            visionFindings = emptyList(),
            visualQuestion = "",
            visualAnswer = null,
            journeyPhotoMoments = if (moments.any { it.photoUri == uri.toString() }) moments else {
                (moments + JourneyPhotoMoment(uri.toString(), "正在识别")).takeLast(12)
            },
        )
        recordJourneyEvent("photo", uri.toString())
        logEvent("photo_captured", details = "source=device_camera;uri=$uri")
        visionAnalyzer.analyze(
            uri = uri,
            onSuccess = { findings ->
                if (uiState.value.capturedPhotoUri != uri.toString()) return@analyze
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
                if (uiState.value.capturedPhotoUri != uri.toString()) return@analyze
                uiState.value = uiState.value.copy(photoAnalysisStatus = "端侧识别未完成，请重拍或授权云端分析")
                logEvent("photo_analysis_failed", details = error.javaClass.simpleName)
                persistState()
            },
        )
        persistState()
    }

    fun setConditionIndex(index: Int) {
        latestFix = null
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
            cloudVisionUploadApproved = false,
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
            spatial = JourneySpatialState(),
            generatedZinePath = null, zineMessage = null,
            selectedImageRegion = null, selectedImageUri = null, selectedImageFindings = emptyList(),
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
        if (uiState.value.demoMode == DemoMode.ONLINE_AGENT && choice == RouteChoice.ALTERNATIVE &&
            uiState.value.taskResult?.alternativeRouteId == null) return
        uiState.value = uiState.value.copy(selectedRoute = choice)
        logEvent("route_selected", action = choice.logValue)
        persistState()
    }

    fun adoptResult() {
        if (!uiState.value.resultVisible) return
        val current = uiState.value
        if (current.demoMode == DemoMode.ONLINE_AGENT && current.scenario in listOf(ExperimentScenario.ENVIRONMENT, ExperimentScenario.ADJUST)) {
            val id = if (current.selectedRoute == RouteChoice.ALTERNATIVE) current.taskResult?.alternativeRouteId else current.taskResult?.recommendedRouteId
            if (id == null && !(current.scenario == ExperimentScenario.ADJUST && current.spatial.activeRoute != null)) {
                uiState.value = current.copy(statusMessage = "请等待地图返回真实路线后再采纳")
                return
            }
            if (id != null) {
                uiState.value = current.copy(spatial = current.spatial.copy(activeRouteId = id),
                    routeReplanned = current.routeReplanned || (current.scenario == ExperimentScenario.ADJUST && id != current.spatial.activeRouteId))
                if (current.scenario == ExperimentScenario.ADJUST && id != current.spatial.activeRouteId) recordJourneyEvent("replan", id)
            }
        }
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
            routeReplanned = if (currentScenario == ExperimentScenario.ADJUST && uiState.value.demoMode == DemoMode.EXPERIMENT_OFFLINE) {
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
            cloudVisionUploadApproved = false,
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
            spatial = JourneySpatialState(),
            generatedZinePath = null, zineMessage = null,
            selectedImageRegion = null, selectedImageUri = null, selectedImageFindings = emptyList(),
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
            cloudVisionUploadApproved = false,
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
            cloudVisionUploadApproved = false,
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

    fun createHistoryZineShareIntent(fileName: String): Intent? = logger.sessionZineImage(fileName)
        ?.let { shareIntent(it, "image/png", "SAGE 历史 AI 拾景纸刊") }

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

    fun generateZine(photoUri: String) {
        if (uiState.value.zineBusy || photoUri !in uiState.value.capturedPhotoUris) return
        val caption = uiState.value.journeyPhotoMoments.firstOrNull { it.photoUri == photoUri }?.label ?: "公园拾景"
        uiState.value = uiState.value.copy(zineBusy = true, zineMessage = "正在生成，通常需要几分钟…")
        logEvent("zine_generation_approved", details = "selected_photo_only=true")
        zineJob = viewModelScope.launch {
            try {
                val image = cn.tsinghua.sagemotion.data.agent.RemoteZineApi(getApplication()).generate(photoUri, caption)
                val saved = logger.storeZineImage(image) ?: error("图片保存失败")
                image.delete()
                uiState.value = uiState.value.copy(generatedZinePath = saved.absolutePath, zineMessage = "AI 生成纸刊已保存在本机", zineBusy = false)
                logEvent("zine_generated")
                persistState()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                uiState.value = uiState.value.copy(zineBusy = false, zineMessage = error.message ?: "生成失败，请稍后再试")
                logEvent("zine_generation_failed")
            }
        }
    }

    fun createZineShareIntent(): Intent? = uiState.value.generatedZinePath?.let(::File)?.takeIf { it.isFile }
        ?.let { shareIntent(it, "image/png", "我的 AI 拾景纸刊") }

    override fun onCleared() {
        runJob?.cancel()
        visualJob?.cancel()
        zineJob?.cancel()
        visionAnalyzer.close()
        super.onCleared()
    }

    private fun restoreActiveSession() {
        val loaded = sessionStore.restore()
        val restored = loaded?.let { old ->
            val refs = (old.capturedPhotoUris + old.journeyPhotoMoments.map { it.photoUri } + listOfNotNull(old.capturedPhotoUri)).distinct().associateWith(photoAssets::ensurePersistent)
            old.copy(capturedPhotoUri = old.capturedPhotoUri?.let { refs[it] ?: it },
                capturedPhotoUris = old.capturedPhotoUris.map { refs[it] ?: it },
                journeyPhotoMoments = old.journeyPhotoMoments.map { it.copy(photoUri = refs[it.photoUri] ?: it.photoUri) },
                spatial = old.spatial.copy(events = old.spatial.events.map { it.copy(reference = refs[it.reference] ?: it.reference) }))
        }
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
            spatial = restored.spatial,
            generatedZinePath = logger.sessionZineImage(restored.logFileName)?.takeIf { it.name == restored.generatedZineName }?.absolutePath,
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
        zineJob?.cancel()
        visualJob?.cancel()
        uiState.value = uiState.value.copy(visualSelectionBusy = false, visualAnswerBusy = false, zineBusy = false,
            zineMessage = if (uiState.value.zineBusy) "生成已中断，已有纸刊仍保留" else uiState.value.zineMessage)
        runJob?.cancel()
        runJob = null
    }

    fun onMapRoutes(routes: List<ParkRoutePlan>, guiding: Boolean) {
        val current = uiState.value
        if (!current.sessionStarted || routes.isEmpty()) return
        val active = if (guiding && routes.none { it.id == current.spatial.activeRouteId }) routes.first().id else current.spatial.activeRouteId
        if (routes == current.spatial.routes && active == current.spatial.activeRouteId) return
        uiState.value = current.copy(spatial = current.spatial.copy(routes = routes, activeRouteId = active))
        if (current.resultVisible && current.taskResult != null && current.scenario in listOf(ExperimentScenario.ENVIRONMENT, ExperimentScenario.ADJUST)) {
            uiState.value = uiState.value.copy(taskResult = bindCurrentRoutes(current.taskResult))
        }
        logEvent("map_routes_received", details = "ids=${routes.joinToString { it.id }}")
        persistState()
    }

    fun onMapLocation(fix: GeoFix) {
        val now = System.currentTimeMillis()
        if (!uiState.value.sessionStarted || !fix.usableAt(now)) return
        latestFix = fix
        val updated = uiState.value.spatial.record(fix, now)
        if (updated != uiState.value.spatial) {
            uiState.value = uiState.value.copy(spatial = updated)
            persistState()
        }
    }

    private fun recordJourneyEvent(kind: String, reference: String) {
        val now = System.currentTimeMillis()
        val spatial = uiState.value.spatial
        uiState.value = uiState.value.copy(spatial = spatial.copy(events = (spatial.events +
            JourneyEvent(kind, now, reference, latestFix?.takeIf { it.usableAt(now) })).takeLast(300)))
    }

    private fun bindCurrentRoutes(result: cn.tsinghua.sagemotion.model.AiTaskResult): cn.tsinghua.sagemotion.model.AiTaskResult {
        val state = uiState.value
        if (state.demoMode != DemoMode.ONLINE_AGENT || state.scenario !in listOf(ExperimentScenario.ENVIRONMENT, ExperimentScenario.ADJUST)) return result
        return bindRouteResult(result, state.spatial.routes, state.spatial.activeRouteId, state.scenario == ExperimentScenario.ADJUST)
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
        logger.storeSpatial(uiState.value.spatial)
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
