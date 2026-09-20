package cn.tsinghua.sagemotion.data

import cn.tsinghua.sagemotion.model.*
import kotlin.math.ceil

/** Bind every selectable result to SDK geometry. No shade/crowd facts are inferred from distance. */
fun bindRouteResult(result: AiTaskResult, routes: List<ParkRoutePlan>, activeId: String?, adjusting: Boolean): AiTaskResult {
    val candidates = routes.distinctBy { it.id }.filter { it.points.size >= 2 }
    val ranked = candidates.sortedBy { it.distanceMeters }
    val preferred = if (adjusting) ranked.firstOrNull { it.id != activeId } else ranked.firstOrNull()
    if (preferred == null) return result.copy(
        title = if (adjusting) "暂无可用的不同路线" else "等待真实步行路线",
        summary = if (adjusting) "当前没有不同于已选路线的可用方案，可以保留当前路线。" else "地图尚未返回可用路径，请检查网络后重新开始。",
        primaryAction = if (adjusting) "保留当前路线" else "等待路线返回",
        metrics = emptyList(), alternativeTitle = null, recommendedRouteId = null, alternativeRouteId = null,
        sourceLabel = "高德路线尚未就绪", isLiveData = false,
    )
    val alternative = if (adjusting) candidates.firstOrNull { it.id == activeId }
        else ranked.firstOrNull { it.id != preferred.id }
    return result.copy(
        title = "${if (adjusting) "可选改道" else "推荐"}：${preferred.name}",
        alternativeTitle = alternative?.let { "${if (adjusting) "保留" else "备选"}：${it.name}" },
        summary = "${preferred.summary}。${if (adjusting) "与当前路线不同；未核实封路或障碍是否已避开。" else "按高德返回的实际距离比较；遮阴与座椅尚无可靠数据。"}",
        uncertainty = "树荫、座椅、人流和封路未实时核实，请以现场为准",
        metrics = listOf(ResultMetric("${ceil(preferred.durationSeconds / 60.0).toInt()} 分钟", "高德预计"), ResultMetric("${preferred.distanceMeters.toInt()} 米", "高德路径长度")),
        recommendedRouteId = preferred.id, alternativeRouteId = alternative?.id,
        sourceLabel = "高德步行路线 · ${candidates.size} 条可用方案",
        evidence = listOf("选中路线：${preferred.name}", "路线距离和时长来自高德 SDK；不代表实际已走里程", "暂按距离排序；树荫、座椅、封路等偏好需现场确认"),
    )
}
