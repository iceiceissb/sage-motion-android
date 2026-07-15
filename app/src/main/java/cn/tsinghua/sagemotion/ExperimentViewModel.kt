package cn.tsinghua.sagemotion

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import cn.tsinghua.sagemotion.data.ExperimentLogger
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ConditionOrder
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.ExperimentTiming
import cn.tsinghua.sagemotion.model.ExperimentUiState
import cn.tsinghua.sagemotion.model.RouteChoice
import cn.tsinghua.sagemotion.model.stageSequenceFor
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ExperimentViewModel(application: Application) : AndroidViewModel(application) {
    private val logger = ExperimentLogger(application)
    private var runJob: Job? = null

    var uiState = androidx.compose.runtime.mutableStateOf(ExperimentUiState())
        private set

    fun startSession(participantId: String, order: ConditionOrder) {
        val normalizedId = participantId.trim().ifBlank { "P000" }
        logger.startSession(normalizedId)
        uiState.value = ExperimentUiState(
            sessionStarted = true,
            participantId = normalizedId,
            order = order,
            statusMessage = "实验会话已开始",
        )
        logEvent("session_started", details = "order=${order.name}")
    }

    fun runCurrentScenario() {
        if (uiState.value.isRunning) return
        runJob?.cancel()
        uiState.value = uiState.value.copy(
            isRunning = true,
            resultVisible = false,
            evidenceVisible = false,
            selectedRoute = RouteChoice.RECOMMENDED,
            statusMessage = null,
        )
        logEvent("task_started")

        runJob = viewModelScope.launch {
            for (stage in stageSequenceFor(uiState.value.scenario)) {
                enterStage(stage)
                delay(ExperimentTiming.delayFor(stage))
            }
            uiState.value = uiState.value.copy(
                isRunning = false,
                resultVisible = true,
                completedTaskCount = uiState.value.completedTaskCount + 1,
            )
            logEvent("task_result_visible")
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
        )
        logEvent("scenario_selected", details = scenario.id)
    }

    fun setConditionIndex(index: Int) {
        if (index !in uiState.value.order.conditions.indices) return
        stopRun()
        uiState.value = uiState.value.copy(
            conditionIndex = index,
            aiStage = AiStage.IDLE,
            isRunning = false,
            resultVisible = false,
            evidenceVisible = false,
            researcherPanelVisible = false,
            selectedRoute = RouteChoice.RECOMMENDED,
        )
        logEvent("condition_selected")
    }

    fun nextCondition() {
        val next = (uiState.value.conditionIndex + 1).coerceAtMost(uiState.value.order.conditions.lastIndex)
        setConditionIndex(next)
    }

    fun selectRoute(choice: RouteChoice) {
        uiState.value = uiState.value.copy(selectedRoute = choice)
        logEvent("route_selected", action = choice.logValue)
    }

    fun adoptResult() {
        uiState.value = uiState.value.copy(
            aiStage = AiStage.COMPLETE,
            statusMessage = when (uiState.value.scenario) {
                ExperimentScenario.ENVIRONMENT -> "路线已开始"
                ExperimentScenario.VISUAL -> "识别结果已保存"
                ExperimentScenario.VOICE -> "回答已确认"
            },
        )
        logEvent(
            event = "result_adopted",
            action = uiState.value.selectedRoute.logValue,
            resultAdopted = "true",
        )
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
            statusMessage = "本次任务已取消",
        )
        logEvent("task_cancelled", action = "cancel")
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
        )
        logEvent("task_reset")
    }

    fun setResearcherPanelVisible(visible: Boolean) {
        uiState.value = uiState.value.copy(researcherPanelVisible = visible)
        if (visible) logEvent("researcher_panel_opened")
    }

    fun createShareIntent(): Intent? {
        val file = logger.latestLogFile() ?: return null
        val context = getApplication<Application>()
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", file)
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "SAGE 实验日志 ${uiState.value.participantId}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    override fun onCleared() {
        runJob?.cancel()
        super.onCleared()
    }

    private fun enterStage(stage: AiStage) {
        uiState.value = uiState.value.copy(aiStage = stage)
        logEvent("state_enter")
    }

    private fun stopRun() {
        runJob?.cancel()
        runJob = null
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

