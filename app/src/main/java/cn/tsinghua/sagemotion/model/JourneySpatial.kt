package cn.tsinghua.sagemotion.model

import java.security.MessageDigest
import java.util.Locale
import kotlin.math.*

data class GeoPoint(val latitude: Double, val longitude: Double) {
    fun distanceTo(other: GeoPoint): Double {
        val a = Math.toRadians(latitude)
        val b = Math.toRadians(other.latitude)
        val d = sin((b - a) / 2).pow(2) + cos(a) * cos(b) *
            sin(Math.toRadians(other.longitude - longitude) / 2).pow(2)
        return 6_371_000 * 2 * atan2(sqrt(d.coerceIn(0.0, 1.0)), sqrt((1 - d).coerceIn(0.0, 1.0)))
    }
}

/** Coordinates remain on-device, in the GCJ-02 coordinate system returned by AMap. */
data class GeoFix(val point: GeoPoint, val timestamp: Long, val accuracyMeters: Float) {
    fun usableAt(now: Long): Boolean = accuracyMeters in 0f..50f && now - timestamp in 0..60_000L &&
        point.latitude.isFinite() && point.longitude.isFinite() &&
        point.latitude in -90.0..90.0 && point.longitude in -180.0..180.0
}

data class RouteInstruction(val text: String, val points: List<GeoPoint>)
data class ParkRoutePlan(
    val id: String,
    val name: String,
    val points: List<GeoPoint>,
    val distanceMeters: Float,
    val durationSeconds: Long,
    val instructions: List<RouteInstruction> = emptyList(),
) {
    val summary: String get() = "${distanceMeters.roundToInt()} 米 · 约 ${ceil(durationSeconds / 60.0).toInt()} 分钟"
    companion object {
        fun stableId(points: List<GeoPoint>): String {
            val geometry = points.joinToString(";") { String.format(Locale.ROOT, "%.5f,%.5f", it.latitude, it.longitude) }
            return "amap_" + MessageDigest.getInstance("SHA-256").digest(geometry.toByteArray())
                .take(10).joinToString("") { "%02x".format(it) }
        }
    }
}

data class JourneyEvent(val kind: String, val timestamp: Long, val reference: String = "", val location: GeoFix? = null)
data class JourneySpatialState(
    val routes: List<ParkRoutePlan> = emptyList(),
    val activeRouteId: String? = null,
    val track: List<GeoFix> = emptyList(),
    val events: List<JourneyEvent> = emptyList(),
) {
    val activeRoute: ParkRoutePlan? get() = routes.firstOrNull { it.id == activeRouteId }
    fun record(fix: GeoFix, now: Long): JourneySpatialState {
        if (!fix.usableAt(now) || (track.lastOrNull()?.timestamp ?: 0L) >= fix.timestamp) return this
        val last = track.lastOrNull()
        if (last != null && last.point.distanceTo(fix.point) < 5 && fix.timestamp - last.timestamp < 30_000) return this
        return copy(track = (track + fix).takeLast(3_000))
    }
}

/** Do not draw a traveled line across missing GPS samples or implausible jumps. */
fun List<GeoFix>.continuousSegments(): List<List<GeoPoint>> {
    val segments = mutableListOf<MutableList<GeoPoint>>()
    var previous: GeoFix? = null
    for (fix in this) {
        val last = previous
        val gap = if (last == null) Long.MAX_VALUE else fix.timestamp - last.timestamp
        val split = last == null || gap <= 0 || gap > 90_000 ||
            last.point.distanceTo(fix.point) > max(100.0, gap / 1000.0 * 8)
        if (split) segments += mutableListOf(fix.point) else segments.last() += fix.point
        previous = fix
    }
    return segments
}

/** Normalized bounds in the upright source image, not in screen coordinates. */
data class ImageRegion(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    init { require(left >= 0 && top >= 0 && right <= 1 && bottom <= 1 && right > left && bottom > top) }
}

object ImageRegionMapping {
    fun fromCircle(x: Float, y: Float, radius: Float, viewWidth: Float, viewHeight: Float, imageWidth: Int, imageHeight: Int): ImageRegion? {
        if (viewWidth <= 0 || viewHeight <= 0 || imageWidth <= 0 || imageHeight <= 0 || radius <= 0 ||
            !listOf(x, y, radius, viewWidth, viewHeight).all { it.isFinite() }) return null
        val scale = max(viewWidth / imageWidth, viewHeight / imageHeight)
        val dx = (imageWidth * scale - viewWidth) / 2
        val dy = (imageHeight * scale - viewHeight) / 2
        val left = ((x - radius + dx) / scale / imageWidth).coerceIn(0f, 1f)
        val right = ((x + radius + dx) / scale / imageWidth).coerceIn(0f, 1f)
        val top = ((y - radius + dy) / scale / imageHeight).coerceIn(0f, 1f)
        val bottom = ((y + radius + dy) / scale / imageHeight).coerceIn(0f, 1f)
        return if (right - left < .005f || bottom - top < .005f) null else ImageRegion(left, top, right, bottom)
    }
}
