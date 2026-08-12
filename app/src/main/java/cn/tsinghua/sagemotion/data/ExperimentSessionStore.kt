package cn.tsinghua.sagemotion.data

import android.content.Context
import cn.tsinghua.sagemotion.model.ConditionOrder
import cn.tsinghua.sagemotion.model.DemoMode
import cn.tsinghua.sagemotion.model.ExperimentScenario
import cn.tsinghua.sagemotion.model.ExperimentUiState
import cn.tsinghua.sagemotion.model.JourneyPhotoMoment
import cn.tsinghua.sagemotion.model.JourneyQuestion
import cn.tsinghua.sagemotion.model.LandmarkStyle
import cn.tsinghua.sagemotion.model.RouteChoice
import cn.tsinghua.sagemotion.model.TaskPerformance
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
    val replanRequestText: String,
    val routePreferenceIds: Set<String>,
    val adoptedRoute: RouteChoice,
    val voiceTranscript: String,
    val voiceTranscripts: List<String>,
    val capturedPhotoUri: String?,
    val capturedPhotoUris: List<String>,
    val visionFindings: List<VisionFinding>,
    val visualQuestion: String,
    val visualAnswer: String?,
    val journeyPhotoMoments: List<JourneyPhotoMoment>,
    val voiceInteractionCount: Int,
    val visualInteractionCount: Int,
    val replanCount: Int,
    val routeReplanned: Boolean,
    val landmarkStyle: LandmarkStyle,
    val taskStartedAtMillis: Long,
    val resultPresentedAtMillis: Long,
    val taskMisoperationCount: Int,
    val taskAttemptCount: Int,
    val pendingPostTaskSurvey: TaskPerformance?,
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
            .putString(KEY_REPLAN_REQUEST, state.replanRequestText)
            .putStringSet(KEY_ROUTE_PREFERENCES, state.routePreferenceIds)
            .putString(KEY_ADOPTED_ROUTE, state.adoptedRoute.name)
            .putString(KEY_VOICE_TRANSCRIPT, state.voiceTranscript)
            .putString(KEY_VOICE_TRANSCRIPTS, state.voiceTranscripts.joinToString(";;", transform = ::encodeText))
            .putString(KEY_CAPTURED_PHOTO_URI, state.capturedPhotoUri)
            .putString(KEY_CAPTURED_PHOTO_URIS, state.capturedPhotoUris.joinToString(";;"))
            .putString(KEY_VISION_FINDINGS, encodeFindings(state.visionFindings))
            .putString(KEY_VISUAL_QUESTION, state.visualQuestion)
            .putString(KEY_VISUAL_ANSWER, state.visualAnswer)
            .putString(KEY_JOURNEY_MOMENTS, encodeMoments(state.journeyPhotoMoments))
            .putInt(KEY_VOICE_COUNT, state.voiceInteractionCount)
            .putInt(KEY_VISUAL_COUNT, state.visualInteractionCount)
            .putInt(KEY_REPLAN_COUNT, state.replanCount)
            .putBoolean(KEY_ROUTE_REPLANNED, state.routeReplanned)
            .putString(KEY_LANDMARK_STYLE, state.landmarkStyle.name)
            .putLong(KEY_TASK_STARTED_AT, state.taskStartedAtMillis)
            .putLong(KEY_RESULT_PRESENTED_AT, state.resultPresentedAtMillis)
            .putInt(KEY_TASK_MISOPERATIONS, state.taskMisoperationCount)
            .putInt(KEY_TASK_ATTEMPTS, state.taskAttemptCount)
            .putString(KEY_PENDING_SURVEY_SCENARIO, state.pendingPostTaskSurvey?.scenario?.name)
            .putInt(KEY_PENDING_SURVEY_INSTANCE, state.pendingPostTaskSurvey?.taskInstance ?: 0)
            .putLong(KEY_PENDING_SURVEY_COMPLETION, state.pendingPostTaskSurvey?.completionTimeMs ?: 0L)
            .putLong(KEY_PENDING_SURVEY_DECISION, state.pendingPostTaskSurvey?.decisionTimeMs ?: 0L)
            .putInt(KEY_PENDING_SURVEY_MISOPERATIONS, state.pendingPostTaskSurvey?.misoperationCount ?: 0)
            .putInt(KEY_PENDING_SURVEY_ATTEMPTS, state.pendingPostTaskSurvey?.attemptCount ?: 0)
            .apply()
    }

    fun restore(): RestoredSession? {
        if (!preferences.getBoolean(KEY_ACTIVE, false)) return null
        val participantId = preferences.getString(KEY_PARTICIPANT, null) ?: return null
        val logFileName = preferences.getString(KEY_LOG_FILE, null) ?: return null
        val restoredOrder = enumValueOrDefault(preferences.getString(KEY_ORDER, null), ConditionOrder.ABC)
        val restoredScenario = enumValueOrDefault(preferences.getString(KEY_SCENARIO, null), ExperimentScenario.ENVIRONMENT)
        val pendingSurveyScenario = preferences.getString(KEY_PENDING_SURVEY_SCENARIO, null)
            ?.let { raw -> ExperimentScenario.entries.firstOrNull { it.name == raw } }
        val pendingSurvey = pendingSurveyScenario?.let { scenario ->
            TaskPerformance(
                taskInstance = preferences.getInt(KEY_PENDING_SURVEY_INSTANCE, 0).coerceAtLeast(1),
                scenario = scenario,
                completionTimeMs = preferences.getLong(KEY_PENDING_SURVEY_COMPLETION, 0L).coerceAtLeast(0L),
                decisionTimeMs = preferences.getLong(KEY_PENDING_SURVEY_DECISION, 0L).coerceAtLeast(0L),
                misoperationCount = preferences.getInt(KEY_PENDING_SURVEY_MISOPERATIONS, 0).coerceAtLeast(0),
                attemptCount = preferences.getInt(KEY_PENDING_SURVEY_ATTEMPTS, 1).coerceAtLeast(1),
            )
        }
        return RestoredSession(
            participantId = participantId,
            order = restoredOrder,
            demoMode = enumValueOrDefault(
                preferences.getString(KEY_DEMO_MODE, null),
                DemoMode.EXPERIMENT_OFFLINE,
            ),
            // 条件数量随顺序而变（六种拉丁方是 3 个，两条件对照是 2 个），
            // 必须按实际顺序长度收敛，否则恢复会话时会越界。
            conditionIndex = preferences.getInt(KEY_CONDITION_INDEX, 0)
                .coerceIn(0, restoredOrder.conditions.lastIndex),
            scenario = restoredScenario,
            completedScenarios = preferences.getStringSet(KEY_COMPLETED_SCENARIOS, emptySet()).orEmpty()
                .mapNotNull { name -> ExperimentScenario.entries.firstOrNull { it.name == name } }
                .toSet(),
            completedTaskCount = preferences.getInt(KEY_COMPLETED_TASKS, 0).coerceAtLeast(0),
            demoCompleted = preferences.getBoolean(KEY_DEMO_COMPLETED, false),
            logFileName = logFileName,
            routeConstraintText = preferences.getString(KEY_ROUTE_CONSTRAINT, "").orEmpty(),
            replanRequestText = preferences.getString(KEY_REPLAN_REQUEST, "").orEmpty(),
            routePreferenceIds = preferences.getStringSet(KEY_ROUTE_PREFERENCES, setOf("shade", "rest")).orEmpty(),
            adoptedRoute = enumValueOrDefault(preferences.getString(KEY_ADOPTED_ROUTE, null), RouteChoice.RECOMMENDED),
            voiceTranscript = preferences.getString(KEY_VOICE_TRANSCRIPT, "").orEmpty(),
            voiceTranscripts = preferences.getString(KEY_VOICE_TRANSCRIPTS, "").orEmpty()
                .split(";;").filter { it.isNotBlank() }.map(::decodeText),
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
            routeReplanned = preferences.getBoolean(KEY_ROUTE_REPLANNED, false),
            landmarkStyle = enumValueOrDefault(preferences.getString(KEY_LANDMARK_STYLE, null), LandmarkStyle.DEPTH),
            taskStartedAtMillis = preferences.getLong(KEY_TASK_STARTED_AT, 0L).coerceAtLeast(0L),
            resultPresentedAtMillis = preferences.getLong(KEY_RESULT_PRESENTED_AT, 0L).coerceAtLeast(0L),
            taskMisoperationCount = preferences.getInt(KEY_TASK_MISOPERATIONS, 0).coerceAtLeast(0),
            taskAttemptCount = preferences.getInt(KEY_TASK_ATTEMPTS, 0).coerceAtLeast(0),
            pendingPostTaskSurvey = pendingSurvey,
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
        const val KEY_REPLAN_REQUEST = "replan_request"
        const val KEY_ROUTE_PREFERENCES = "route_preferences"
        const val KEY_ADOPTED_ROUTE = "adopted_route"
        const val KEY_VOICE_TRANSCRIPT = "voice_transcript"
        const val KEY_VOICE_TRANSCRIPTS = "voice_transcripts"
        const val KEY_CAPTURED_PHOTO_URI = "captured_photo_uri"
        const val KEY_CAPTURED_PHOTO_URIS = "captured_photo_uris"
        const val KEY_VISION_FINDINGS = "vision_findings"
        const val KEY_VISUAL_QUESTION = "visual_question"
        const val KEY_VISUAL_ANSWER = "visual_answer"
        const val KEY_JOURNEY_MOMENTS = "journey_moments"
        const val KEY_VOICE_COUNT = "voice_count"
        const val KEY_VISUAL_COUNT = "visual_count"
        const val KEY_REPLAN_COUNT = "replan_count"
        const val KEY_ROUTE_REPLANNED = "route_replanned"
        const val KEY_LANDMARK_STYLE = "landmark_style"
        const val KEY_TASK_STARTED_AT = "task_started_at"
        const val KEY_RESULT_PRESENTED_AT = "result_presented_at"
        const val KEY_TASK_MISOPERATIONS = "task_misoperations"
        const val KEY_TASK_ATTEMPTS = "task_attempts"
        const val KEY_PENDING_SURVEY_SCENARIO = "pending_survey_scenario"
        const val KEY_PENDING_SURVEY_INSTANCE = "pending_survey_instance"
        const val KEY_PENDING_SURVEY_COMPLETION = "pending_survey_completion"
        const val KEY_PENDING_SURVEY_DECISION = "pending_survey_decision"
        const val KEY_PENDING_SURVEY_MISOPERATIONS = "pending_survey_misoperations"
        const val KEY_PENDING_SURVEY_ATTEMPTS = "pending_survey_attempts"
    }
}
