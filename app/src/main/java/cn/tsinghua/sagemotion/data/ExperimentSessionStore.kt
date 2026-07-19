package cn.tsinghua.sagemotion.data

import android.content.Context
import cn.tsinghua.sagemotion.model.ConditionOrder
import cn.tsinghua.sagemotion.model.DemoMode
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.ExperimentUiState
import cn.tsinghua.sagemotion.model.JourneyPhotoMoment
import cn.tsinghua.sagemotion.model.JourneyQuestion
import cn.tsinghua.sagemotion.model.VisionFinding
import android.util.Base64

data class RestoredSession(
    val participantId: String,
    val order: ConditionOrder,
    val demoMode: DemoMode,
    val conditionIndex: Int,
    val scenario: ExperimentScenario,
    val completedScenarios: Set<ExperimentScenario>,
    val completedTaskCount: Int,
    val demoCompleted: Boolean,
    val logFileName: String,
    val routeConstraintText: String,
    val routePreferenceIds: Set<String>,
    val voiceTranscript: String,
    val capturedPhotoUri: String?,
    val capturedPhotoUris: List<String>,
    val visionFindings: List<VisionFinding>,
    val visualQuestion: String,
    val visualAnswer: String?,
    val journeyPhotoMoments: List<JourneyPhotoMoment>,
    val voiceInteractionCount: Int,
    val visualInteractionCount: Int,
    val replanCount: Int,
)

class ExperimentSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("sage_active_session", Context.MODE_PRIVATE)

    fun save(state: ExperimentUiState, logFileName: String?) {
        if (!state.sessionStarted || logFileName == null) return
        preferences.edit()
            .putBoolean(KEY_ACTIVE, true)
            .putString(KEY_PARTICIPANT, state.participantId)
            .putString(KEY_ORDER, state.order.name)
            .putString(KEY_DEMO_MODE, state.demoMode.name)
            .putInt(KEY_CONDITION_INDEX, state.conditionIndex)
            .putString(KEY_SCENARIO, state.scenario.name)
            .putStringSet(KEY_COMPLETED_SCENARIOS, state.completedScenarios.map { it.name }.toSet())
            .putInt(KEY_COMPLETED_TASKS, state.completedTaskCount)
            .putBoolean(KEY_DEMO_COMPLETED, state.demoCompleted)
            .putString(KEY_LOG_FILE, logFileName)
            .putString(KEY_ROUTE_CONSTRAINT, state.routeConstraintText)
            .putStringSet(KEY_ROUTE_PREFERENCES, state.routePreferenceIds)
            .putString(KEY_VOICE_TRANSCRIPT, state.voiceTranscript)
            .putString(KEY_CAPTURED_PHOTO_URI, state.capturedPhotoUri)
            .putString(KEY_CAPTURED_PHOTO_URIS, state.capturedPhotoUris.joinToString(";;"))
            .putString(KEY_VISION_FINDINGS, encodeFindings(state.visionFindings))
            .putString(KEY_VISUAL_QUESTION, state.visualQuestion)
            .putString(KEY_VISUAL_ANSWER, state.visualAnswer)
            .putString(KEY_JOURNEY_MOMENTS, encodeMoments(state.journeyPhotoMoments))
            .putInt(KEY_VOICE_COUNT, state.voiceInteractionCount)
            .putInt(KEY_VISUAL_COUNT, state.visualInteractionCount)
            .putInt(KEY_REPLAN_COUNT, state.replanCount)
            .apply()
    }

    fun restore(): RestoredSession? {
        if (!preferences.getBoolean(KEY_ACTIVE, false)) return null
        val participantId = preferences.getString(KEY_PARTICIPANT, null) ?: return null
        val logFileName = preferences.getString(KEY_LOG_FILE, null) ?: return null
        return RestoredSession(
            participantId = participantId,
            order = enumValueOrDefault(preferences.getString(KEY_ORDER, null), ConditionOrder.ABC),
            demoMode = enumValueOrDefault(
                preferences.getString(KEY_DEMO_MODE, null),
                DemoMode.EXPERIMENT_OFFLINE,
            ),
            conditionIndex = preferences.getInt(KEY_CONDITION_INDEX, 0).coerceIn(0, 2),
            scenario = enumValueOrDefault(preferences.getString(KEY_SCENARIO, null), ExperimentScenario.ENVIRONMENT),
            completedScenarios = preferences.getStringSet(KEY_COMPLETED_SCENARIOS, emptySet()).orEmpty()
                .mapNotNull { name -> ExperimentScenario.entries.firstOrNull { it.name == name } }
                .toSet(),
            completedTaskCount = preferences.getInt(KEY_COMPLETED_TASKS, 0).coerceAtLeast(0),
            demoCompleted = preferences.getBoolean(KEY_DEMO_COMPLETED, false),
            logFileName = logFileName,
            routeConstraintText = preferences.getString(KEY_ROUTE_CONSTRAINT, "").orEmpty(),
            routePreferenceIds = preferences.getStringSet(KEY_ROUTE_PREFERENCES, setOf("shade", "rest")).orEmpty(),
            voiceTranscript = preferences.getString(KEY_VOICE_TRANSCRIPT, "").orEmpty(),
            capturedPhotoUri = preferences.getString(KEY_CAPTURED_PHOTO_URI, null),
            capturedPhotoUris = preferences.getString(KEY_CAPTURED_PHOTO_URIS, "").orEmpty()
                .split(";;").filter { it.isNotBlank() },
            visionFindings = decodeFindings(preferences.getString(KEY_VISION_FINDINGS, "").orEmpty()),
            visualQuestion = preferences.getString(KEY_VISUAL_QUESTION, "").orEmpty(),
            visualAnswer = preferences.getString(KEY_VISUAL_ANSWER, null),
            journeyPhotoMoments = decodeMoments(preferences.getString(KEY_JOURNEY_MOMENTS, "").orEmpty()),
            voiceInteractionCount = preferences.getInt(KEY_VOICE_COUNT, 0),
            visualInteractionCount = preferences.getInt(KEY_VISUAL_COUNT, 0),
            replanCount = preferences.getInt(KEY_REPLAN_COUNT, 0),
        )
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == value } ?: default

    private fun encodeFindings(findings: List<VisionFinding>): String = findings.joinToString(";;") {
        "${it.label.replace(";", "").replace("|", "")}|${it.confidence}"
    }

    private fun decodeFindings(raw: String): List<VisionFinding> = raw.split(";;").mapNotNull { item ->
        val parts = item.split("|", limit = 2)
        val confidence = parts.getOrNull(1)?.toFloatOrNull() ?: return@mapNotNull null
        VisionFinding(parts[0], confidence)
    }

    private fun encodeMoments(moments: List<JourneyPhotoMoment>): String = moments.joinToString(";;") { moment ->
        val questions = moment.questions.joinToString("~") { item ->
            "${encodeText(item.question)}^${encodeText(item.answer)}"
        }
        "${encodeText(moment.photoUri)}|${encodeText(moment.label)}|$questions"
    }

    private fun decodeMoments(raw: String): List<JourneyPhotoMoment> = raw.split(";;").mapNotNull { item ->
        if (item.isBlank()) return@mapNotNull null
        val parts = item.split("|", limit = 3)
        val uri = decodeText(parts.getOrNull(0).orEmpty())
        val label = decodeText(parts.getOrNull(1).orEmpty()).ifBlank { "旅程发现" }
        val questions = parts.getOrNull(2).orEmpty().split("~").mapNotNull { encodedQuestion ->
            if (encodedQuestion.isBlank()) return@mapNotNull null
            val pair = encodedQuestion.split("^", limit = 2)
            val question = decodeText(pair.getOrNull(0).orEmpty())
            val answer = decodeText(pair.getOrNull(1).orEmpty())
            if (question.isBlank()) null else JourneyQuestion(question, answer)
        }
        JourneyPhotoMoment(uri, label, questions)
    }

    private fun encodeText(value: String): String = Base64.encodeToString(value.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)

    private fun decodeText(value: String): String = runCatching {
        String(Base64.decode(value, Base64.NO_WRAP), Charsets.UTF_8)
    }.getOrDefault("")

    private companion object {
        const val KEY_ACTIVE = "active"
        const val KEY_PARTICIPANT = "participant_id"
        const val KEY_ORDER = "condition_order"
        const val KEY_DEMO_MODE = "demo_mode"
        const val KEY_CONDITION_INDEX = "condition_index"
        const val KEY_SCENARIO = "scenario"
        const val KEY_COMPLETED_SCENARIOS = "completed_scenarios"
        const val KEY_COMPLETED_TASKS = "completed_tasks"
        const val KEY_DEMO_COMPLETED = "demo_completed"
        const val KEY_LOG_FILE = "log_file"
        const val KEY_ROUTE_CONSTRAINT = "route_constraint"
        const val KEY_ROUTE_PREFERENCES = "route_preferences"
        const val KEY_VOICE_TRANSCRIPT = "voice_transcript"
        const val KEY_CAPTURED_PHOTO_URI = "captured_photo_uri"
        const val KEY_CAPTURED_PHOTO_URIS = "captured_photo_uris"
        const val KEY_VISION_FINDINGS = "vision_findings"
        const val KEY_VISUAL_QUESTION = "visual_question"
        const val KEY_VISUAL_ANSWER = "visual_answer"
        const val KEY_JOURNEY_MOMENTS = "journey_moments"
        const val KEY_VOICE_COUNT = "voice_count"
        const val KEY_VISUAL_COUNT = "visual_count"
        const val KEY_REPLAN_COUNT = "replan_count"
    }
}
