package cn.tsinghua.sagemotion.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.tsinghua.sagemotion.ExperimentViewModel
import cn.tsinghua.sagemotion.ui.screens.ExperimentScreen
import cn.tsinghua.sagemotion.ui.screens.HistoryScreen
import cn.tsinghua.sagemotion.ui.screens.ResearcherSetupScreen

@Composable
fun SageMotionApp(viewModel: ExperimentViewModel = viewModel()) {
    val state by viewModel.uiState
    val context = LocalContext.current

    fun launchShare(intent: Intent?, title: String) {
        intent?.let { context.startActivity(Intent.createChooser(it, title)) }
    }

    if (state.historyVisible) {
        HistoryScreen(
            sessions = state.historySessions,
            selectedDetail = state.selectedHistory,
            onBack = viewModel::closeHistory,
            onSelect = viewModel::selectHistory,
            onCloseDetail = viewModel::closeHistoryDetail,
            onExportSession = { fileName ->
                launchShare(viewModel.createSessionShareIntent(fileName), "导出本次实验日志")
            },
            onExportAll = {
                launchShare(viewModel.createAllSessionsShareIntent(), "导出全部实验数据")
            },
            onDeleteSession = viewModel::deleteHistorySession,
            onDeleteAll = viewModel::deleteAllHistory,
        )
        return
    }

    if (!state.sessionStarted) {
        ResearcherSetupScreen(
            onStart = viewModel::startSession,
            onHistory = viewModel::openHistory,
            savedSessionCount = state.historySessions.size,
            statusMessage = state.statusMessage,
        )
        return
    }

    ExperimentScreen(
        state = state,
        onRunScenario = viewModel::runCurrentScenario,
        onCancel = viewModel::cancelTask,
        onReset = viewModel::resetTask,
        onRestartDemo = viewModel::restartDemo,
        onFinishSession = viewModel::finishSession,
        onAdopt = viewModel::adoptResult,
        onEvidence = viewModel::showEvidence,
        onCloseEvidence = viewModel::hideEvidence,
        onRouteSelected = viewModel::selectRoute,
        onScenarioSelected = viewModel::setScenario,
        onConditionSelected = viewModel::setConditionIndex,
        onNextCondition = viewModel::nextCondition,
        onPreviewPrevious = viewModel::previewPreviousStage,
        onPreviewNext = viewModel::previewNextStage,
        onResearcherPanel = viewModel::setResearcherPanelVisible,
        onHistory = viewModel::openHistory,
        onExport = {
            launchShare(viewModel.createCurrentShareIntent(), "导出实验日志")
        },
        onExportAll = {
            launchShare(viewModel.createAllSessionsShareIntent(), "导出全部实验数据")
        },
        onVoiceTranscript = viewModel::setVoiceTranscript,
        onRouteConstraintChanged = viewModel::setRouteConstraint,
        onRoutePreferenceToggled = viewModel::toggleRoutePreference,
        onVisualQuestionAsked = viewModel::askVisualQuestion,
        onClearVisualQuestion = viewModel::clearVisualQuestion,
        onCreatePhotoUri = viewModel::createPhotoCaptureUri,
        onPhotoCaptured = viewModel::onPhotoCaptureCompleted,
        onShareJourney = {
            launchShare(viewModel.createJourneyShareIntent(), "分享知识游记")
        },
        onBeginJourneySummary = viewModel::beginJourneySummary,
    )
}
