package cn.tsinghua.sagemotion.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val SageGreen = Color(0xFF3F6F5C)
val SageGreenDark = Color(0xFF294E40)
val SageMist = Color(0xFFDCE8E1)
val SageSurface = Color(0xFFF8F7F3)
val SageInk = Color(0xFF26332F)
val SageMuted = Color(0xFF68736E)
val SageOchre = Color(0xFFB86B2C)
val SageWarningSurface = Color(0xFFFFF3E8)
val SageDivider = Color(0xFFD9DEDA)

private val SageColorScheme = lightColorScheme(
    primary = SageGreen,
    onPrimary = Color.White,
    primaryContainer = SageMist,
    onPrimaryContainer = SageGreenDark,
    secondary = Color(0xFF718C80),
    onSecondary = Color.White,
    tertiary = SageOchre,
    onTertiary = Color.White,
    background = SageSurface,
    onBackground = SageInk,
    surface = SageSurface,
    onSurface = SageInk,
    surfaceVariant = Color(0xFFEFF2EF),
    onSurfaceVariant = SageMuted,
    outline = SageDivider,
    error = Color(0xFF9E3F34),
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
        content = content,
    )
}

