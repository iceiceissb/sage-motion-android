package cn.tsinghua.sagemotion.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import cn.tsinghua.sagemotion.ui.theme.SageHudEdge
import cn.tsinghua.sagemotion.ui.theme.SagePanel
import cn.tsinghua.sagemotion.ui.theme.SagePanelRaised
import cn.tsinghua.sagemotion.ui.theme.SageSignalLime

/**
 * 地图界面的轻量“毛玻璃”容器。
 *
 * 高德 MapView 是原生 View，Compose 无法稳定地对其做实时背景取样模糊；这里使用半透明渐变、
 * 高光描边与柔和投影形成玻璃质感，同时保证旧设备、录屏和实验帧率稳定。
 */
@Composable
fun FrostedGlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(24.dp),
    tint: Color = SagePanelRaised,
    content: @Composable BoxScope.() -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = Color.Transparent,
        shadowElevation = 10.dp,
        tonalElevation = 1.dp,
    ) {
        Box(
            Modifier
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            SagePanelRaised.copy(alpha = .98f),
                            tint.copy(alpha = .94f),
                            SagePanel.copy(alpha = .98f),
                        ),
                    ),
                )
                .border(1.dp, SageHudEdge, shape),
            content = content,
        )
    }
}

/** 带一点不对称的“软泡泡”轮廓，避免所有卡片都像同一套实验表单。 */
fun sageBubbleShape(variant: Int = 0): Shape = when (variant.mod(4)) {
    0 -> RoundedCornerShape(topStart = 24.dp, topEnd = 15.dp, bottomEnd = 25.dp, bottomStart = 18.dp)
    1 -> RoundedCornerShape(topStart = 17.dp, topEnd = 26.dp, bottomEnd = 18.dp, bottomStart = 24.dp)
    2 -> RoundedCornerShape(topStart = 27.dp, topEnd = 19.dp, bottomEnd = 23.dp, bottomStart = 15.dp)
    else -> RoundedCornerShape(topStart = 19.dp, topEnd = 23.dp, bottomEnd = 15.dp, bottomStart = 27.dp)
}

/**
 * 可选择的泡泡按钮。形状和淡彩负责亲和感，勾选/文字仍明确表达状态，
 * 不让装饰取代实验所需的信息可读性。
 */
@Composable
fun BubbleChoice(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: Int = 0,
    accent: Color = SageSignalLime,
    tint: Color = SagePanelRaised,
    content: @Composable RowScope.() -> Unit,
) {
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.025f else 1f,
        label = "bubbleChoiceScale",
    )
    val shape = sageBubbleShape(variant)
    Surface(
        onClick = onClick,
        modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
        shape = shape,
        color = if (selected) tint else SagePanel.copy(alpha = .92f),
        contentColor = accent,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent.copy(alpha = .72f) else SageHudEdge,
        ),
        shadowElevation = if (selected) 5.dp else 1.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}
