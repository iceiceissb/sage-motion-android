package cn.tsinghua.sagemotion.data

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ExperimentCondition
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.HistoryEvent
import cn.tsinghua.sagemotion.model.SessionDetail
import cn.tsinghua.sagemotion.model.SessionSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExperimentLogger(private val context: Context) {
    private val logDirectory = File(context.filesDir, "experiment_logs")
    private val exportDirectory = File(context.filesDir, "experiment_exports")
    private var logFile: File? = null
    private var sessionStartElapsed: Long = 0L

    fun startSession(participantId: String): File {
        logDirectory.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss-SSS", Locale.US).format(Date())
        val safeParticipant = participantId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        logFile = File(logDirectory, "sage_${safeParticipant}_$timestamp.csv")
        sessionStartElapsed = SystemClock.elapsedRealtime()
        logFile!!.writeText(CSV_HEADER + "\n", Charsets.UTF_8)
        return logFile!!
    }

    fun resumeSession(fileName: String): Boolean {
        val safeName = File(fileName).name
        val candidate = File(logDirectory, safeName)
        if (!candidate.isFile) return false
        logFile = candidate
        sessionStartElapsed = SystemClock.elapsedRealtime()
        return true
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
        file.appendText(CsvCodec.encodeRow(values) + "\n", Charsets.UTF_8)
    }

    fun latestLogFile(): File? = logFile?.takeIf { it.isFile }

    fun activeLogFileName(): String? = latestLogFile()?.name

    fun releaseActiveSession() {
        logFile = null
        sessionStartElapsed = 0L
    }

    fun fileByName(fileName: String): File? {
        val candidate = File(logDirectory, File(fileName).name)
        return candidate.takeIf { it.isFile }
    }

    fun deleteSession(fileName: String): Boolean {
        val file = fileByName(fileName) ?: return false
        if (file.name == logFile?.name) return false
        deletePhotoArtifacts(file)
        return file.delete()
    }

    fun deleteAllStoredData(): Int {
        val activeName = logFile?.name
        var deleted = 0
        listLogFiles().filterNot { it.name == activeName }.forEach { file ->
            deletePhotoArtifacts(file)
            if (file.delete()) deleted++
        }
        exportDirectory.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
        if (activeName == null) {
            File(context.cacheDir, "camera_captures").listFiles()?.filter { it.isFile }?.forEach { it.delete() }
        }
        return deleted
    }

    fun listSessionSummaries(): List<SessionSummary> =
        listLogFiles().mapNotNull(::parseSummary)

    fun sessionDetail(fileName: String): SessionDetail? {
        val file = fileByName(fileName) ?: return null
        val rows = readRows(file)
        val summary = summaryFor(file, rows) ?: return null
        val events = rows.mapNotNull { values ->
            if (values.size < COLUMN_COUNT) return@mapNotNull null
            HistoryEvent(
                timestampMillis = values[0].toLongOrNull() ?: return@mapNotNull null,
                elapsedMillis = values[1].toLongOrNull() ?: 0L,
                taskId = values[5],
                taskPhase = values[6],
                aiState = values[7],
                event = values[8],
                action = values[9],
                details = values[12],
            )
        }
        return SessionDetail(summary = summary, events = events)
    }

    fun createAllSessionsArchive(): File? {
        val files = listLogFiles()
        if (files.isEmpty()) return null
        exportDirectory.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val archive = File(exportDirectory, "sage_all_sessions_$timestamp.zip")
        ZipOutputStream(FileOutputStream(archive)).use { zip ->
            files.forEach { file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { input -> input.copyTo(zip) }
                zip.closeEntry()
            }
        }
        return archive
    }

    private fun listLogFiles(): List<File> {
        logDirectory.mkdirs()
        return logDirectory.listFiles { file -> file.isFile && file.extension.equals("csv", true) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()
    }

    private fun parseSummary(file: File): SessionSummary? = summaryFor(file, readRows(file))

    private fun summaryFor(file: File, rows: List<List<String>>): SessionSummary? {
        if (rows.isEmpty()) return null
        val validRows = rows.filter { it.size >= COLUMN_COUNT }
        if (validRows.isEmpty()) return null
        val first = validRows.first()
        val last = validRows.last()
        val startedAt = first[0].toLongOrNull() ?: file.lastModified()
        val updatedAt = last[0].toLongOrNull() ?: file.lastModified()
        val sessionDetails = validRows.firstOrNull { it[8] == "session_started" }?.get(12).orEmpty()
        val demoMode = when {
            sessionDetails.contains("mode=ONLINE_AGENT") -> "ONLINE_AGENT"
            sessionDetails.contains("mode=EXPERIMENT_OFFLINE") -> "EXPERIMENT_OFFLINE"
            else -> "LEGACY"
        }
        return SessionSummary(
            fileName = file.name,
            participantId = first[2].ifBlank { "未知参与者" },
            startedAtMillis = startedAt,
            updatedAtMillis = updatedAt,
            durationMillis = validRows.maxOfOrNull { it[1].toLongOrNull() ?: 0L } ?: 0L,
            conditionOrder = first[4].ifBlank { "—" },
            eventCount = validRows.size,
            completedTaskCount = validRows.count { it[8] == "task_result_visible" },
            adoptedCount = validRows.count { it[8] == "result_adopted" },
            completed = validRows.any { it[8] == "session_completed" || it[8] == "demo_completed" },
            sizeBytes = file.length(),
            demoMode = demoMode,
        )
    }

    private fun readRows(file: File): List<List<String>> = runCatching {
        file.useLines(Charsets.UTF_8) { lines ->
            lines.drop(1).filter { it.isNotBlank() }.map(CsvCodec::decodeRow).toList()
        }
    }.getOrDefault(emptyList())

    private fun deletePhotoArtifacts(file: File) {
        readRows(file)
            .filter { it.size >= COLUMN_COUNT && it[8] == "photo_captured" }
            .mapNotNull { row -> row[12].substringAfter("uri=", "").takeIf { it.isNotBlank() } }
            .mapNotNull { raw -> runCatching { Uri.parse(raw).lastPathSegment }.getOrNull() }
            .forEach { name -> File(context.cacheDir, "camera_captures/${File(name).name}").delete() }
    }

    private companion object {
        const val COLUMN_COUNT = 13
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
