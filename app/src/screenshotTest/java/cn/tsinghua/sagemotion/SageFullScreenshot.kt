package cn.tsinghua.sagemotion

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ConditionOrder
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.ExperimentUiState
import cn.tsinghua.sagemotion.ui.screens.ExperimentScreen
import cn.tsinghua.sagemotion.ui.theme.SageMotionTheme

@Preview(name = "SAGE Full Route Result", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SageFullRouteResultScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.ENVIRONMENT,
                aiStage = AiStage.COMPLETE,
                resultVisible = true,
            ),
            onRunScenario = {},
            onCancel = {},
            onReset = {},
            onAdopt = {},
            onEvidence = {},
            onCloseEvidence = {},
            onRouteSelected = {},
            onScenarioSelected = {},
            onConditionSelected = {},
            onNextCondition = {},
            onResearcherPanel = {},
            onExport = {},
        )
    }
}

@Preview(name = "SAGE Full Visual Result", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SageFullVisualResultScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 2,
                scenario = ExperimentScenario.VISUAL,
                aiStage = AiStage.COMPLETE,
                resultVisible = true,
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onEvidence = {}, onCloseEvidence = {}, onRouteSelected = {},
            onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onResearcherPanel = {}, onExport = {},
        )
    }
}

@Preview(name = "Semantic Voice Listening", widthDp = 390, heightDp = 844, showBackground = true)
@Composable
fun SemanticVoiceListeningScreenshot() {
    SageMotionTheme {
        ExperimentScreen(
            state = ExperimentUiState(
                sessionStarted = true,
                participantId = "P001",
                order = ConditionOrder.ABC,
                conditionIndex = 1,
                scenario = ExperimentScenario.VOICE,
                aiStage = AiStage.LISTENING,
                isRunning = true,
            ),
            onRunScenario = {}, onCancel = {}, onReset = {}, onAdopt = {},
            onEvidence = {}, onCloseEvidence = {}, onRouteSelected = {},
            onScenarioSelected = {}, onConditionSelected = {}, onNextCondition = {},
            onResearcherPanel = {}, onExport = {},
        )
    }
}
