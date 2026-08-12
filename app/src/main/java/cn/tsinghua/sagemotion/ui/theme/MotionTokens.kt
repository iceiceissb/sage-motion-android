package cn.tsinghua.sagemotion.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing as ComposeEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.unit.dp

/**
 * Motion token 层，对应 AI动效--DESIGN 第 8 节「开发规范：可以写成 motion token」。
 *
 * 所有页面只允许引用这里的时长、缓动和弹性，不再各自写魔法数字，
 * 这样 Study 1 里编码过的「时间语法 / 视觉语法」参数在实现层可被审计和复现。
 */
object SageMotion {

    /** motion.duration.* —— 单位毫秒。 */
    object Duration {
        /** 120–180ms：即时反馈，按压、勾选、开关。 */
        const val FAST = 150
        const val FAST_MIN = 120
        const val FAST_MAX = 180

        /** 240–360ms：标准阶段转换，卡片进出、状态文字切换。 */
        const val STANDARD = 300
        const val STANDARD_MIN = 240
        const val STANDARD_MAX = 360

        /** 800–1600ms：推理循环，仅用于「持续进行中」的过程表达。 */
        const val REASONING = 1_200
        const val REASONING_MIN = 800
        const val REASONING_MAX = 1_600

        /** 一次性显影：路径、拼贴、地标落位等永久性状态改变。 */
        const val REVEAL = 1_050

        /** 完成收束。Study 1 中只有 28.8% 的片段给出明确完成提示，这里单独留出时长。 */
        const val COMPLETION = 550
    }

    /** motion.easing.* */
    object Easing {
        /** enter = ease-out：元素进入，快起慢收，注意力落在终点。 */
        val Enter: ComposeEasing = CubicBezierEasing(0f, 0f, 0.2f, 1f)

        /** exit = ease-in：元素退出，慢起快收，不与新内容抢注意力。 */
        val Exit: ComposeEasing = CubicBezierEasing(0.4f, 0f, 1f, 1f)

        /** standard：阶段之间的位移与形变。 */
        val Standard: ComposeEasing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)

        /** reasoning = smooth continuous：循环动画必须匀速，避免读出「快结束了」的错误暗示。 */
        val Reasoning: ComposeEasing = LinearEasing

        /** 显影：末端更缓，让「算到哪里」的显影头看得清。 */
        val Reveal: ComposeEasing = CubicBezierEasing(0.25f, 0.1f, 0.15f, 1f)
    }

    /** motion.opacity.overlay = 40–70% */
    object Overlay {
        const val MIN = 0.40f
        const val DEFAULT = 0.55f
        const val MAX = 0.70f

        /** 语义动效叠加在真实场景之上时的压暗层，保持场景可辨识。 */
        const val SCENE_SCRIM = 0.18f
    }

    /** motion.rebound.* —— 0–1 低回弹，2–3 中回弹；不使用高回弹。 */
    object Rebound {
        const val LOW_DAMPING = 0.92f
        const val MEDIUM_DAMPING = 0.68f
        const val STIFFNESS_LOW = Spring.StiffnessLow
        const val STIFFNESS_MEDIUM = Spring.StiffnessMediumLow
    }

    /** 语义动效在场景层上的统一描边与节点尺寸。 */
    object Stroke {
        val RouteMain = 6.dp
        val RouteAlternative = 4.dp
        val RouteCasing = 11.dp
        val Hairline = 1.5.dp
    }

    // ---- 常用 spec，直接引用即可 ----

    fun <T> fast(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(Duration.FAST, delayMillis, Easing.Standard)

    fun <T> standard(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(Duration.STANDARD, delayMillis, Easing.Standard)

    fun <T> enter(durationMillis: Int = Duration.STANDARD, delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(durationMillis, delayMillis, Easing.Enter)

    fun <T> exit(durationMillis: Int = Duration.FAST_MAX, delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(durationMillis, delayMillis, Easing.Exit)

    fun <T> reveal(durationMillis: Int = Duration.REVEAL, delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(durationMillis, delayMillis, Easing.Reveal)

    fun <T> completion(delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(Duration.COMPLETION, delayMillis, Easing.Enter)

    /** 低回弹：位置与尺寸的确定性变化。 */
    fun <T> reboundLow(): AnimationSpec<T> =
        spring(dampingRatio = Rebound.LOW_DAMPING, stiffness = Rebound.STIFFNESS_LOW)

    /** 中回弹：需要一点「落定感」的采纳、锁定、盖章。 */
    fun <T> reboundMedium(): AnimationSpec<T> =
        spring(dampingRatio = Rebound.MEDIUM_DAMPING, stiffness = Rebound.STIFFNESS_MEDIUM)

    /**
     * 推理循环。匀速且首尾相接，表达「仍在进行」而不是「进度百分比」。
     */
    fun loop(durationMillis: Int = Duration.REASONING) =
        infiniteRepeatable<Float>(tween(durationMillis, easing = Easing.Reasoning), RepeatMode.Restart)

    /**
     * 呼吸循环。用于聆听、感知范围等「在场」表达，往返且缓动，避免机械感。
     */
    fun breathe(durationMillis: Int = 1_050) =
        infiniteRepeatable<Float>(tween(durationMillis, easing = Easing.Standard), RepeatMode.Reverse)
}
