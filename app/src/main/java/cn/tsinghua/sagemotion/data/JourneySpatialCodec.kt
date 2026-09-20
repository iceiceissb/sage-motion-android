package cn.tsinghua.sagemotion.data

import cn.tsinghua.sagemotion.model.*
import org.json.JSONArray
import org.json.JSONObject

object JourneySpatialCodec {
    private fun point(p: GeoPoint) = JSONArray().put(p.latitude).put(p.longitude)
    private fun fix(f: GeoFix) = JSONObject().put("p", point(f.point)).put("t", f.timestamp).put("a", f.accuracyMeters)
    private fun parsePoint(a: JSONArray) = GeoPoint(a.getDouble(0), a.getDouble(1))
    private fun parseFix(o: JSONObject) = GeoFix(parsePoint(o.getJSONArray("p")), o.getLong("t"), o.getDouble("a").toFloat())
    private fun <T> array(items: List<T>, encode: (T) -> Any): JSONArray = JSONArray().apply { items.forEach { put(encode(it)) } }
    private fun <T> JSONArray.values(decode: (JSONObject) -> T) = (0 until length()).map { decode(getJSONObject(it)) }
    private fun points(a: JSONArray) = (0 until a.length()).map { parsePoint(a.getJSONArray(it)) }

    fun encode(state: JourneySpatialState): String = JSONObject()
        .put("version", 1).put("coordinate_system", "GCJ-02")
        .put("active", state.activeRouteId)
        .put("routes", array(state.routes) { r ->
            JSONObject().put("id", r.id).put("name", r.name).put("points", array(r.points, ::point))
                .put("distance", r.distanceMeters).put("duration", r.durationSeconds)
                .put("steps", array(r.instructions) { JSONObject().put("text", it.text).put("points", array(it.points, ::point)) })
        })
        .put("track", array(state.track, ::fix))
        .put("events", array(state.events) { e ->
            JSONObject().put("kind", e.kind).put("time", e.timestamp).put("ref", e.reference).apply {
                e.location?.let { put("location", fix(it)) }
            }
        }).toString()

    fun decode(raw: String?): JourneySpatialState = runCatching {
        val o = JSONObject(raw ?: "{}")
        JourneySpatialState(
            routes = o.optJSONArray("routes")?.values { r ->
                ParkRoutePlan(r.getString("id"), r.getString("name"), points(r.getJSONArray("points")),
                    r.getDouble("distance").toFloat(), r.getLong("duration"),
                    r.optJSONArray("steps")?.values { RouteInstruction(it.getString("text"), points(it.getJSONArray("points"))) }.orEmpty())
            }.orEmpty(),
            activeRouteId = o.optString("active").takeIf { it.isNotBlank() },
            track = o.optJSONArray("track")?.values(::parseFix).orEmpty(),
            events = o.optJSONArray("events")?.values { e ->
                JourneyEvent(e.getString("kind"), e.getLong("time"), e.optString("ref"), e.optJSONObject("location")?.let(::parseFix))
            }.orEmpty(),
        )
    }.getOrDefault(JourneySpatialState())
}
