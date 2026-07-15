package cn.tsinghua.sagemotion.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.tsinghua.sagemotion.ExperimentViewModel
import cn.tsinghua.sagemotion.ui.screens.ExperimentScreen
import cn.tsinghua.sagemotion.ui.screens.ResearcherSetupScreen

@Composable
fun SageMotionApp(viewModel: ExperimentViewModel = viewModel()) {
    val state by viewModel.uiState
    val context = LocalContext.current

    if (!state.sessionStarted) {
        ResearcherSetupScreen(onStart = viewModel::startSession)
        return
    }

    ExperimentScreen(
        state = state,
        onRunScenario = viewModel::runCurrentScenario,
        onCancel = viewModel::cancelTask,
        onReset = viewModel::resetTask,
        onAdopt = viewModel::adoptResult,
        onEvidence = viewModel::showEvidence,
        onCloseEvidence = viewModel::hideEvidence,
        onRouteSelected = viewModel::selectRoute,
        onScenarioSelected = viewModel::setScenario,
        onConditionSelected = viewModel::setConditionIndex,
        onNextCondition = viewModel::nextCondition,
        onResearcherPanel = viewModel::setResearcherPanelVisible,
        onExport = {
            viewModel.createShareIntent()?.let { shareIntent ->
                context.startActivity(Intent.createChooser(shareIntent, "导出实验日志"))
            }
        },
    )
}

