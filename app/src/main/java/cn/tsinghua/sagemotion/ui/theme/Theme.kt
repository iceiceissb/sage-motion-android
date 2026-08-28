package cn.tsinghua.sagemotion.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ---- 品牌基色：林冠绿 ----
val SageGreen = Color(0xFF3F6F5C)
val SageGreenDark = Color(0xFF294E40)

/** 更深的林冠色，用于覆盖在真实场景之上的深色浮层，保证白字对比度。 */
val SageCanopy = Color(0xFF16302A)
val SageMist = Color(0xFFDCE8E1)
val SageSurface = Color(0xFFF8F7F3)
val SageInk = Color(0xFF26332F)
val SageMuted = Color(0xFF68736E)
val SageDivider = Color(0xFFD9DEDA)

// ---- 语义保留色 ----
/** 不确定性专用赭色。只表达信息缺口与能力边界，不得用作装饰。 */
val SageOchre = Color(0xFFB86B2C)
val SageWarningSurface = Color(0xFFFFF3E8)

// ---- 手账拼贴层 ----
/** 手账纸底。对应设计建议「手账拼贴画的形式，路线+照片+小标题文字」。 */
val SagePaper = Color(0xFFF5F0E4)
val SagePaperEdge = Color(0xFFE3D9C4)
val SagePaperShade = Color(0xFFEDE5D4)

/**
 * 戏曲朱红。八家郊野公园南园以戏曲文化为主题，这里作为手账贴纸与地标的装饰色。
 * 仅用于装饰，绝不承担状态语义，避免与赭色的「不确定」混淆。
 */
val SageOpera = Color(0xFFA8453C)

/** 手账胶带与节点编号的暖金色，同样只作装饰。 */
val SageGold = Color(0xFFC08A2E)

/** 田野志的结构色。只用于语音、标注和编辑结构，不扩散成第二品牌主色。 */
val SageCobalt = Color(0xFF315FBE)

/** 比品牌绿更接近墨色，用于纸面上的主动作、路线和大标题。 */
val SageForestInk = Color(0xFF173D32)

/** 纸面辅助文字，避免灰色与真实地图混在一起。 */
val SagePaperMuted = Color(0xFF756F61)

// ---- 数字公园信号层 ----
/** 地图 HUD 的深墨底；只用于空间界面叠层，不替换真实高德底图。 */
val SageHud = Color(0xFF08110F)
val SageHudEdge = Color(0xFF52615A).copy(alpha = .70f)
val SageHudMuted = Color(0xFFA9B4AE)

/** 主行动、推荐路线与“正在感知”的唯一高能强调色。 */
val SageSignalLime = Color(0xFFD8FF2F)

/** 语音、声场和实时环境数据。 */
val SageSignalCyan = Color(0xFF39DDD6)

/** 备选路线、警示与重规划；不承担普通装饰。 */
val SageSignalCoral = Color(0xFFFF7466)

// ---- 开屏与天光 ----
val SageSky = Color(0xFFCADCE2)
val SageDawn = Color(0xFFE8D9C0)

private val SageColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = Color.White,
    primaryContainer = SageMist,
    onPrimaryContainer = SageGreenDark,
    secondary = Color(0xFF718C80),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE4EDE7),
    onSecondaryContainer = SageGreenDark,
    tertiary = SageOchre,
    onTertiary = Color.White,
    tertiaryContainer = SageWarningSurface,
    onTertiaryContainer = Color(0xFF7A4A20),
    background = SageSurface,
    onBackground = SageInk,
    surface = SageSurface,
    onSurface = SageInk,
    surfaceVariant = Color(0xFFEFF2EF),
    onSurfaceVariant = SageMuted,
    outline = SageDivider,
    outlineVariant = Color(0xFFE6EAE7),
    error = Color(0xFF9E3F34),
    onError = Color.White,
)

/**
 * 排版层级。遵循无障碍约束：正文不小于 14sp，辅助说明最低 11sp。
 * Study 1 显示 83.8% 的 AI 动效同时依赖文字辅助，因此文字层级与动效同等重要。
 */
private val SageTypography = Typography(
    displaySmall = TextStyle(fontSize = 30.sp, lineHeight = 36.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.SansSerif, color = SageInk),
    headlineMedium = TextStyle(fontSize = 25.sp, lineHeight = 31.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, color = SageInk),
    headlineSmall = TextStyle(fontSize = 22.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, color = SageInk),
    titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 26.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif, color = SageInk),
    titleMedium = TextStyle(fontSize = 16.sp, lineHeight = 22.sp, fontWeight = FontWeight.Medium, color = SageInk),
    bodyLarge = TextStyle(fontSize = 15.sp, lineHeight = 22.sp, color = SageInk),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp, color = SageInk),
    labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.Medium),
    labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp),
)

@Composable
fun SageMotionTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = true
        }
    }
    MaterialTheme(
        colorScheme = SageColorScheme,
        typography = SageTypography,
        content = content,
    )
}
