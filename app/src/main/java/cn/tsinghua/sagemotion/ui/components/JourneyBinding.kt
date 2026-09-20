package cn.tsinghua.sagemotion.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import cn.tsinghua.sagemotion.model.*

data class JourneyBinding(
    val state: ExperimentUiState,
    val onRoutes: (List<ParkRoutePlan>, Boolean) -> Unit,
    val onLocation: (GeoFix) -> Unit,
    val onRegion: (ImageRegion?) -> Unit,
    val onGenerateZine: (String) -> Unit = {},
    val onShareZine: () -> Unit = {},
)

val LocalJourneyBinding = staticCompositionLocalOf<JourneyBinding?> { null }
