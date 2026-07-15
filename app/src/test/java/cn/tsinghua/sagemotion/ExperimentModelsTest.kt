package cn.tsinghua.sagemotion

import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ConditionOrder
import cn.tsinghua.sagemotion.model.ExperimentCondition
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.stageSequenceFor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperimentModelsTest {
    @Test
    fun latinSquareContainsAllConditionsExactlyOncePerOrder() {
        ConditionOrder.entries.forEach { order ->
            assertEquals(3, order.conditions.size)
            assertEquals(3, order.conditions.distinct().size)
            assertTrue(order.conditions.containsAll(ExperimentCondition.entries))
        }
    }

    @Test
    fun eachCoreScenarioHasACompleteSemanticSequence() {
        ExperimentScenario.entries.forEach { scenario ->
            val sequence = stageSequenceFor(scenario)
            assertEquals(AiStage.ACTIVATING, sequence.first())
            assertEquals(AiStage.COMPLETE, sequence.last())
            assertTrue(sequence.contains(AiStage.REASONING))
        }
    }

    @Test
    fun voiceSequenceIncludesListeningAndResponding() {
        val sequence = stageSequenceFor(ExperimentScenario.VOICE)
        assertTrue(sequence.contains(AiStage.LISTENING))
        assertTrue(sequence.contains(AiStage.RESPONDING))
    }
}
