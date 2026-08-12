package cn.tsinghua.sagemotion.data

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import cn.tsinghua.sagemotion.model.AiStage
import cn.tsinghua.sagemotion.model.ExperimentCondition
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.HistoryEvent
import cn.tsinghua.sagemotion.model.PostTaskMeasurement
import cn.tsinghua.sagemotion.model.SessionDetail
import cn.tsinghua.sagemotion.model.SessionSummary
import cn.tsinghua.sagemotion.model.SurveyDimension
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
        logFile!!.outputStream().use { output ->
            output.write(UTF8_BOM)
            output.write((CSV_HEADER + "\n").toByteArray(Charsets.UTF_8))
        }
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
        measurement: PostTaskMeasurement? = null,
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
            measurement?.performance?.taskInstance?.toString().orEmpty(),
            measurement?.performance?.completionTimeMs?.toString().orEmpty(),
            measurement?.performance?.decisionTimeMs?.toString().orEmpty(),
            measurement?.performance?.misoperationCount?.toString().orEmpty(),
            measurement?.performance?.attemptCount?.toString().orEmpty(),
            measurement?.ratings?.get(SurveyDimension.STATE_RECOGNITION)?.toString().orEmpty(),
            measurement?.ratings?.get(SurveyDimension.PROCESS_UNDERSTANDING)?.toString().orEmpty(),
            measurement?.ratings?.get(SurveyDimension.CALIBRATED_TRUST)?.toString().orEmpty(),
            measurement?.ratings?.get(SurveyDimension.PERCEIVED_CONTROL)?.toString().orEmpty(),
            measurement?.ratings?.get(SurveyDimension.WORKLOAD)?.toString().orEmpty(),
        )
        file.appendText(CsvCodec.encodeRow(values) + "\n", Charsets.UTF_8)
    }

    fun latestLogFile(): File? = logFile?.takeIf { it.isFile }

    fun createCurrentCsvExport(): File? = latestLogFile()?.let(::createTaskMeasurementCsv)

    fun createSessionCsvExport(fileName: String): File? = fileByName(fileName)?.let(::createTaskMeasurementCsv)

    fun activeLogFileName(): String? = latestLogFile()?.name

    fun releaseActiveSession() {
        logFile = null
        sessionStartElapsed = 0L
    }

    /** 把临时生成的游记固化到会话目录，确保结束会话后仍可预览和分享。 */
    fun storeJourneyImage(source: File): File? = runCatching {
        val session = latestLogFile() ?: return@runCatching null
        logDirectory.mkdirs()
        val target = journeyImageFor(session)
        source.copyTo(target, overwrite = true)
    }.getOrNull()

    fun sessionJourneyImage(fileName: String): File? =
        fileByName(fileName)?.let(::journeyImageFor)?.takeIf { it.isFile }

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
        return SessionDetail(
            summary = summary,
            events = events,
            journeyImagePath = sessionJourneyImage(file.name)?.absolutePath,
        )
    }

    fun createAllSessionsArchive(): File? {
        val files = listLogFiles()
        if (files.isEmpty()) return null
        exportDirectory.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val archive = File(exportDirectory, "sage_all_sessions_$timestamp.zip")
        ZipOutputStream(FileOutputStream(archive)).use { zip ->
            writeDataDictionary(zip)
            files.forEach { file ->
                writeCsvEntry(zip, file.name, file.readBytes())
                writeTaskMeasurementSummary(zip, file)
                writeJourneyImage(zip, file)
            }
        }
        return archive
    }

    fun createCurrentSessionArchive(): File? {
        val file = latestLogFile() ?: return null
        exportDirectory.mkdirs()
        val timestamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val archive = File(exportDirectory, "${file.nameWithoutExtension}_$timestamp.zip")
        ZipOutputStream(FileOutputStream(archive)).use { zip ->
            writeDataDictionary(zip)
            writeCsvEntry(zip, file.name, file.readBytes())
            writeTaskMeasurementSummary(zip, file)
            writeJourneyImage(zip, file)
        }
        return archive
    }

    private fun writeDataDictionary(zip: ZipOutputStream) {
        zip.putNextEntry(ZipEntry("DATA_DICTIONARY.txt"))
        zip.write(UTF8_BOM)
        zip.write(DATA_DICTIONARY.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun createTaskMeasurementCsv(source: File): File {
        exportDirectory.mkdirs()
        val target = File(exportDirectory, source.nameWithoutExtension + "_task_measurements.csv")
        target.outputStream().use { output ->
            output.write(UTF8_BOM)
            output.write(taskMeasurementSummary(source))
        }
        return target
    }

    private fun writeCsvEntry(zip: ZipOutputStream, name: String, bytes: ByteArray) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(UTF8_BOM)
        zip.write(if (bytes.startsWithBom()) bytes.copyOfRange(UTF8_BOM.size, bytes.size) else bytes)
        zip.closeEntry()
    }

    /** 一行一个任务，便于直接做统计；原始事件流仍保留用于时序和误操作审计。 */
    private fun writeTaskMeasurementSummary(zip: ZipOutputStream, file: File) {
        writeCsvEntry(zip, "${file.nameWithoutExtension}_task_measurements.csv", taskMeasurementSummary(file))
    }

    private fun writeJourneyImage(zip: ZipOutputStream, file: File) {
        val image = journeyImageFor(file).takeIf { it.isFile } ?: return
        zip.putNextEntry(ZipEntry(image.name))
        image.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    private fun taskMeasurementSummary(file: File): ByteArray {
        val rows = readRows(file).filter { it.size >= 23 && it[8] == "post_task_measurement" }
        val summaryHeader = listOf(
            "participant_id", "condition_id", "condition_order", "task_id", "task_phase", "task_instance",
            "completion_time_ms", "decision_time_ms", "misoperation_count", "attempt_count",
            "state_recognition", "process_understanding", "calibrated_trust", "perceived_control", "workload",
        )
        return buildString {
            append(CsvCodec.encodeRow(summaryHeader)).append('\n')
            rows.forEach { row ->
                append(CsvCodec.encodeRow(listOf(row[2], row[3], row[4], row[5], row[6]) + row.subList(13, 23))).append('\n')
            }
        }.toByteArray(Charsets.UTF_8)
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
        journeyImageFor(file).delete()
        readRows(file)
            .filter { it.size >= COLUMN_COUNT && it[8] == "photo_captured" }
            .mapNotNull { row -> row[12].substringAfter("uri=", "").takeIf { it.isNotBlank() } }
            .mapNotNull { raw -> runCatching { Uri.parse(raw).lastPathSegment }.getOrNull() }
            .forEach { name -> File(context.cacheDir, "camera_captures/${File(name).name}").delete() }
    }

    private fun journeyImageFor(file: File): File =
        File(logDirectory, "${file.nameWithoutExtension}_journey.png")

    private companion object {
        const val COLUMN_COUNT = 13 // Old files remain readable; new measurement columns are appended.
        val UTF8_BOM = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        const val CSV_HEADER =
            "wall_clock_ms,elapsed_ms,participant_id,condition_id,condition_order,task_id,task_phase,ai_state,event,user_action,confidence,result_adopted,details,task_instance,completion_time_ms,decision_time_ms,misoperation_count,attempt_count,state_recognition,process_understanding,calibrated_trust,perceived_control,workload"

        val DATA_DICTIONARY = """
            SAGE Motion 预实验数据字典（UTF-8）

            每次任务完成后会写入一行 event=post_task_measurement：
            - task_instance：当前会话内已完成任务的顺序号
            - completion_time_ms：点击启动任务至结果首次呈现的时间
            - decision_time_ms：结果首次呈现至用户采纳或确认的时间
            - misoperation_count：取消、重置、拍照失败及研究员人工补记的总数
            - attempt_count：该任务实例的启动尝试次数
            - state_recognition / process_understanding / calibrated_trust /
              perceived_control / workload：1–7 点量表；工作负荷越高表示负荷越高

            三个实验条件使用相同题目顺序、量尺和提交流程。
            ZIP 中原始会话 CSV 是完整事件流；*_task_measurements.csv 是一行一个任务的分析表。
            推荐主分析使用任务汇总表，状态进入/退出事件用于操作核查与过程分析。
        """.trimIndent()

        fun ByteArray.startsWithBom(): Boolean = size >= 3 && this[0] == UTF8_BOM[0] && this[1] == UTF8_BOM[1] && this[2] == UTF8_BOM[2]

        fun confidenceFor(stage: AiStage): String = when (stage) {
            AiStage.UNCERTAIN -> "low"
            AiStage.ERROR -> "failed"
            AiStage.COMPLETE -> "high"
            else -> "not_applicable"
        }
    }
}
