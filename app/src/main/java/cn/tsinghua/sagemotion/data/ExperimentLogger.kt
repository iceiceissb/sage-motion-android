package cn.tsinghua.sagemotion.data

import android.content.Context
import android.os.SystemClock
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ExperimentCondition
import cn.tsinghua.sagemotion.model.ExperimentScenario
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExperimentLogger(private val context: Context) {
    private val logDirectory = File(context.filesDir, "experiment_logs")
    private var logFile: File? = null
    private var sessionStartElapsed: Long = 0L

    fun startSession(participantId: String): File {
        logDirectory.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val safeParticipant = participantId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        logFile = File(logDirectory, "sage_${safeParticipant}_$timestamp.csv")
        sessionStartElapsed = SystemClock.elapsedRealtime()
        logFile!!.writeText(CSV_HEADER + "\n", Charsets.UTF_8)
        return logFile!!
    }

    @Synchronized
    fun log(
        participantId: String,
        condition: ExperimentCondition,
        conditionOrder: String,
        scenario: ExperimentScenario,
        stage: AiStage,
        event: String,
        action: String = "",
        confidence: String = confidenceFor(stage),
        resultAdopted: String = "",
        details: String = "",
    ) {
        val file = logFile ?: startSession(participantId)
        val wallClock = System.currentTimeMillis()
        val elapsed = SystemClock.elapsedRealtime() - sessionStartElapsed
        val values = listOf(
            wallClock.toString(),
            elapsed.toString(),
            participantId,
            condition.id,
            conditionOrder,
            scenario.id,
            scenario.phaseLabel,
            stage.id,
            event,
            action,
            confidence,
            resultAdopted,
            details,
        )
        file.appendText(values.joinToString(",") { csv(it) } + "\n", Charsets.UTF_8)
    }

    fun latestLogFile(): File? = logFile

    fun listLogFiles(): List<File> =
        logDirectory.listFiles()?.sortedByDescending { it.lastModified() }.orEmpty()

    private fun csv(value: String): String = "\"${value.replace("\"", "\"\"")}\""

    private companion object {
        const val CSV_HEADER =
            "wall_clock_ms,elapsed_ms,participant_id,condition_id,condition_order,task_id,task_phase,ai_state,event,user_action,confidence,result_adopted,details"

        fun confidenceFor(stage: AiStage): String = when (stage) {
            AiStage.UNCERTAIN -> "low"
            AiStage.ERROR -> "failed"
            AiStage.COMPLETE -> "high"
            else -> "not_applicable"
        }
    }
}

