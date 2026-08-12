package cn.tsinghua.sagemotion.data.agent

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class NearbyPlace(
    val name: String,
    val category: String,
    val distanceMeters: Int,
    val address: String? = null,
)

data class PlaceReading(
    val places: List<NearbyPlace>,
    val source: String = "OpenStreetMap",
)

interface ParkPlaceProvider {
    suspend fun nearbyFood(): PlaceReading?
}

/**
 * 免费且免密钥的附近餐饮工具。只在用户提出餐饮问题时查询 OpenStreetMap，
 * 不上传参与者位置；查询中心固定为东升八家郊野公园南区实验点。
 *
 * 公共 Overpass 实例偶尔会排队或限流，因此依次尝试两个实例，再使用
 * Nominatim 的有界 amenity 查询兜底。所有来源都只返回实际命名的地点。
 */
class OpenStreetMapPlaceProvider : ParkPlaceProvider {
    @Volatile private var cachedReading: PlaceReading? = null
    @Volatile private var cachedAtMillis: Long = 0L

    override suspend fun nearbyFood(): PlaceReading? = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        cachedReading?.takeIf { now - cachedAtMillis <= CACHE_MS }?.let { return@withContext it }

        val live = OVERPASS_URLS.firstNotNullOfOrNull { endpoint ->
            runCatching { fetchOverpass(endpoint) }.getOrNull()
        } ?: runCatching { fetchNominatim() }.getOrNull()

        if (live != null) {
            cachedReading = live
            cachedAtMillis = now
        }
        live ?: cachedReading
    }

    private fun fetchOverpass(endpoint: String): PlaceReading? {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            val body = "data=" + URLEncoder.encode(FOOD_QUERY, Charsets.UTF_8.name())
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) error("Overpass HTTP ${connection.responseCode}")
            val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseOverpassPlaces(JSONObject(payload)).takeIf { it.isNotEmpty() }?.let {
                PlaceReading(it, source = "OpenStreetMap Overpass")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchNominatim(): PlaceReading? {
        val query = URLEncoder.encode("[restaurant]", Charsets.UTF_8.name())
        val url = "$NOMINATIM_URL?format=jsonv2&addressdetails=1&namedetails=1&limit=12&bounded=1&viewbox=$VIEWBOX&q=$query"
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.5")
            setRequestProperty("User-Agent", USER_AGENT)
        }
        return try {
            if (connection.responseCode !in 200..299) error("Nominatim HTTP ${connection.responseCode}")
            val payload = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            parseNominatimPlaces(JSONArray(payload)).takeIf { it.isNotEmpty() }?.let {
                PlaceReading(it, source = "OpenStreetMap Nominatim")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun parseOverpassPlaces(payload: JSONObject): List<NearbyPlace> {
        val elements = payload.optJSONArray("elements") ?: return emptyList()
        return buildList {
            for (index in 0 until elements.length()) {
                val element = elements.optJSONObject(index) ?: continue
                val tags = element.optJSONObject("tags") ?: continue
                val name = tags.optString("name:zh").ifBlank { tags.optString("name") }.trim()
                if (name.isBlank()) continue
                val center = element.optJSONObject("center")
                val latitude = if (element.has("lat")) element.optDouble("lat") else center?.optDouble("lat")
                val longitude = if (element.has("lon")) element.optDouble("lon") else center?.optDouble("lon")
                if (latitude == null || longitude == null || latitude.isNaN() || longitude.isNaN()) continue
                val amenity = tags.optString("amenity")
                add(
                    NearbyPlace(
                        name = name,
                        category = amenityCategory(amenity),
                        distanceMeters = distanceMeters(PARK_LATITUDE, PARK_LONGITUDE, latitude, longitude),
                        address = listOf(tags.optString("addr:street"), tags.optString("addr:housenumber"))
                            .map(String::trim).filter(String::isNotBlank).joinToString("").ifBlank { null },
                    ),
                )
            }
        }.distinctBy { it.name }.sortedBy { it.distanceMeters }.take(5)
    }

    private fun parseNominatimPlaces(payload: JSONArray): List<NearbyPlace> = buildList {
        for (index in 0 until payload.length()) {
            val item = payload.optJSONObject(index) ?: continue
            val namedetails = item.optJSONObject("namedetails")
            val name = namedetails?.optString("name:zh").orEmpty()
                .ifBlank { namedetails?.optString("name").orEmpty() }
                .ifBlank { item.optString("display_name").substringBefore(',') }
                .trim()
            val latitude = item.optString("lat").toDoubleOrNull() ?: continue
            val longitude = item.optString("lon").toDoubleOrNull() ?: continue
            if (name.isBlank()) continue
            add(
                NearbyPlace(
                    name = name,
                    category = amenityCategory(item.optString("type")),
                    distanceMeters = distanceMeters(PARK_LATITUDE, PARK_LONGITUDE, latitude, longitude),
                    address = item.optString("display_name").substringAfter(',', "").trim().ifBlank { null },
                ),
            )
        }
    }.distinctBy { it.name }.sortedBy { it.distanceMeters }.take(5)

    private fun amenityCategory(amenity: String): String = when (amenity) {
        "cafe" -> "咖啡/茶饮"
        "fast_food" -> "简餐"
        else -> "餐馆"
    }

    private fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
        val latitudeDistance = Math.toRadians(lat2 - lat1)
        val longitudeDistance = Math.toRadians(lon2 - lon1)
        val a = sin(latitudeDistance / 2) * sin(latitudeDistance / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(longitudeDistance / 2) * sin(longitudeDistance / 2)
        return (EARTH_RADIUS_METERS * 2 * atan2(sqrt(a), sqrt(1 - a))).toInt()
    }

    private companion object {
        const val PARK_LATITUDE = 40.01546
        const val PARK_LONGITUDE = 116.32724
        const val EARTH_RADIUS_METERS = 6_371_000.0
        const val NETWORK_TIMEOUT_MS = 4_500
        const val CACHE_MS = 30 * 60 * 1_000L
        const val USER_AGENT = "SageMotion/1.5.0 academic-research-demo"
        const val NOMINATIM_URL = "https://nominatim.openstreetmap.org/search"
        const val VIEWBOX = "116.3000,40.0350,116.3550,39.9950"
        val OVERPASS_URLS = listOf(
            "https://overpass.kumi.systems/api/interpreter",
            "https://overpass-api.de/api/interpreter",
        )
        const val FOOD_QUERY = "[out:json][timeout:6];(node(around:2500,40.01546,116.32724)[amenity~\"restaurant|cafe|fast_food\"];way(around:2500,40.01546,116.32724)[amenity~\"restaurant|cafe|fast_food\"];);out center tags 30;"
    }
}
