package cn.tsinghua.sagemotion

import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ConditionOrder
import cn.tsinghua.sagemotion.model.ExperimentCondition
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.JourneyPhotoMoment
import cn.tsinghua.sagemotion.model.JourneyQuestion
import cn.tsinghua.sagemotion.model.ExperimentUiState
import cn.tsinghua.sagemotion.model.RouteChoice
import cn.tsinghua.sagemotion.model.stageSequenceFor
import cn.tsinghua.sagemotion.model.nextScenarioAfter
import cn.tsinghua.sagemotion.model.SurveyDimension
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperimentModelsTest {
    @Test
    fun latinSquareContainsAllConditionsExactlyOncePerOrder() {
        ConditionOrder.entries.filterNot { it.isPairedComparison }.forEach { order ->
            assertEquals(3, order.conditions.size)
            assertEquals(3, order.conditions.distinct().size)
            assertTrue(order.conditions.containsAll(ExperimentCondition.entries))
        }
    }

    @Test
    fun pairedComparisonContainsBaselineAndSageExactlyOnce() {
        ConditionOrder.entries.filter { it.isPairedComparison }.forEach { order ->
            assertEquals(2, order.conditions.size)
            assertEquals(2, order.conditions.distinct().size)
            assertTrue(order.conditions.contains(ExperimentCondition.BASELINE))
            assertTrue(order.conditions.contains(ExperimentCondition.SAGE_FULL))
        }
    }

    @Test
    fun postTaskSurveyHasFiveStableSevenPointDimensions() {
        assertEquals(5, SurveyDimension.entries.size)
        assertEquals(
            listOf(
                "state_recognition",
                "process_understanding",
                "calibrated_trust",
                "perceived_control",
                "workload",
            ),
            SurveyDimension.entries.map { it.exportId },
        )
    }

    @Test
    fun eachCoreScenarioHasACompleteSemanticSequence() {
        ExperimentScenario.entries.filterNot { it == ExperimentScenario.EXPLORE }.forEach { scenario ->
            val sequence = stageSequenceFor(scenario)
            assertEquals(AiStage.ACTIVATING, sequence.first())
            assertEquals(AiStage.COMPLETE, sequence.last())
            assertTrue(sequence.size >= 5)
        }
    }

    @Test
    fun voiceSequenceIncludesListeningAndResponding() {
        val sequence = stageSequenceFor(ExperimentScenario.VOICE)
        assertTrue(sequence.contains(AiStage.LISTENING))
        assertTrue(sequence.contains(AiStage.RESPONDING))
    }

    @Test
    fun participantDemoStartsWithPlanningThenReturnsToolsToTheParallelHub() {
        assertEquals(ExperimentScenario.EXPLORE, nextScenarioAfter(ExperimentScenario.ENVIRONMENT))
        assertEquals(ExperimentScenario.EXPLORE, nextScenarioAfter(ExperimentScenario.VISUAL))
        assertEquals(ExperimentScenario.EXPLORE, nextScenarioAfter(ExperimentScenario.VOICE))
        assertEquals(ExperimentScenario.EXPLORE, nextScenarioAfter(ExperimentScenario.ADJUST))
        assertEquals(null, nextScenarioAfter(ExperimentScenario.CREATE))
    }

    @Test
    fun creationAndAdjustmentExposeDistinctMotionSemantics() {
        assertTrue(stageSequenceFor(ExperimentScenario.CREATE).contains(AiStage.SUMMARIZING))
        assertTrue(stageSequenceFor(ExperimentScenario.CREATE).contains(AiStage.EDITING))
        assertTrue(stageSequenceFor(ExperimentScenario.ADJUST).contains(AiStage.REPLANNING))
        assertTrue(stageSequenceFor(ExperimentScenario.ADJUST).contains(AiStage.DECIDING))
    }

    @Test
    fun journeyPhotoMomentKeepsEveryQuestionAttachedToItsPhoto() {
        val moment = JourneyPhotoMoment(
            photoUri = "content://photo/1",
            label = "湖边拱桥",
            questions = listOf(
                JourneyQuestion("这是什么？", "一座拱桥。"),
                JourneyQuestion("为什么在这里？", "用于连接湖岸步道。"),
            ),
        )
        assertEquals("content://photo/1", moment.photoUri)
        assertEquals(2, moment.questions.size)
        assertEquals("为什么在这里？", moment.questions.last().question)
    }

    @Test
    fun adoptedAlternativeRouteSurvivesBeyondTheResultPanel() {
        val state = ExperimentUiState(demoMode = cn.tsinghua.sagemotion.model.DemoMode.EXPERIMENT_OFFLINE, adoptedRoute = RouteChoice.ALTERNATIVE)
        assertEquals("草坪外环线", state.activeRouteName)
        assertEquals("林下连廊绕行线", state.copy(routeReplanned = true).activeRouteName)
    }
}
