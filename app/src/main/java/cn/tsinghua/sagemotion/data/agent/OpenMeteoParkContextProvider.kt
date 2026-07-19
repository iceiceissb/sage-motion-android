package cn.tsinghua.sagemotion.data.agent

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ParkContext(
    val temperatureCelsius: Double,
    val apparentTemperatureCelsius: Double,
    val windSpeedKmh: Double,
    val precipitationMm: Double,
    val weatherCode: Int,
    val uvIndexMax: Double?,
    val usAqi: Int?,
    val pm25: Double?,
    val observedAt: String,
)

enum class ContextOrigin {
    LIVE,
    FRESH_CACHE,
    STALE_CACHE,
}

data class ParkContextReading(
    val context: ParkContext,
    val origin: ContextOrigin,
)

interface ParkContextProvider {
    suspend fun load(): ParkContextReading?
}

/**
 * Keyless environmental context for the online demo. The coordinates are a fixed
 * Tsinghua campus demo point: the app never requests or uploads participant location.
 */
class OpenMeteoParkContextProvider(context: Context) : ParkContextProvider {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun load(): ParkContextReading? {
        val now = System.currentTimeMillis()
        val cached = readCache()
        if (cached != null && now - cached.savedAtMillis <= FRESH_CACHE_MS) {
            return ParkContextReading(cached.context, ContextOrigin.FRESH_CACHE)
        }

        val live = runCatching { fetchLive() }.getOrNull()
        if (live != null) {
            saveCache(live, now)
            return ParkContextReading(live, ContextOrigin.LIVE)
        }

        return cached
            ?.takeIf { now - it.savedAtMillis <= STALE_CACHE_MS }
            ?.let { ParkContextReading(it.context, ContextOrigin.STALE_CACHE) }
    }

    private suspend fun fetchLive(): ParkContext = coroutineScope {
        val weather = async(Dispatchers.IO) { requestJson(WEATHER_URL) }
        val airQuality = async(Dispatchers.IO) { requestJson(AIR_QUALITY_URL) }
        parse(weather.await(), airQuality.await())
    }

    private fun requestJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = NETWORK_TIMEOUT_MS
            readTimeout = NETWORK_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SageMotion/0.4.0 academic-research-demo")
        }
        return try {
            val status = connection.responseCode
            if (status !in 200..299) error("Open-Meteo HTTP $status")
            val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun parse(weather: JSONObject, airQuality: JSONObject): ParkContext {
        val current = weather.getJSONObject("current")
        val daily = weather.optJSONObject("daily")
        val air = airQuality.optJSONObject("current")
        return ParkContext(
            temperatureCelsius = current.getDouble("temperature_2m"),
            apparentTemperatureCelsius = current.getDouble("apparent_temperature"),
            windSpeedKmh = current.getDouble("wind_speed_10m"),
            precipitationMm = current.getDouble("precipitation"),
            weatherCode = current.getInt("weather_code"),
            uvIndexMax = daily?.optJSONArray("uv_index_max")?.optNullableDouble(0),
            usAqi = air?.optNullableDouble("us_aqi")?.toInt(),
            pm25 = air?.optNullableDouble("pm2_5"),
            observedAt = current.optString("time", "未知时间"),
        )
    }

    private fun saveCache(context: ParkContext, savedAtMillis: Long) {
        preferences.edit()
            .putLong(KEY_SAVED_AT, savedAtMillis)
            .putString(KEY_CONTEXT, context.toJson().toString())
            .apply()
    }

    private fun readCache(): CachedContext? {
        val raw = preferences.getString(KEY_CONTEXT, null) ?: return null
        val savedAt = preferences.getLong(KEY_SAVED_AT, 0L)
        if (savedAt <= 0L) return null
        return runCatching { CachedContext(JSONObject(raw).toParkContext(), savedAt) }.getOrNull()
    }

    private data class CachedContext(val context: ParkContext, val savedAtMillis: Long)

    private companion object {
        const val PREFERENCES_NAME = "sage_open_meteo_cache"
        const val KEY_CONTEXT = "park_context"
        const val KEY_SAVED_AT = "saved_at"
        const val NETWORK_TIMEOUT_MS = 2_200
        const val FRESH_CACHE_MS = 30 * 60 * 1_000L
        const val STALE_CACHE_MS = 6 * 60 * 60 * 1_000L

        const val WEATHER_URL =
            "https://api.open-meteo.com/v1/forecast?latitude=40.0030&longitude=116.3260&current=temperature_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m&daily=uv_index_max&timezone=Asia%2FShanghai&forecast_days=1"
        const val AIR_QUALITY_URL =
            "https://air-quality-api.open-meteo.com/v1/air-quality?latitude=40.0030&longitude=116.3260&current=pm2_5,us_aqi&timezone=Asia%2FShanghai"
    }
}

private fun JSONObject.toParkContext() = ParkContext(
    temperatureCelsius = getDouble("temperatureCelsius"),
    apparentTemperatureCelsius = getDouble("apparentTemperatureCelsius"),
    windSpeedKmh = getDouble("windSpeedKmh"),
    precipitationMm = getDouble("precipitationMm"),
    weatherCode = getInt("weatherCode"),
    uvIndexMax = optNullableDouble("uvIndexMax"),
    usAqi = optNullableDouble("usAqi")?.toInt(),
    pm25 = optNullableDouble("pm25"),
    observedAt = getString("observedAt"),
)

private fun ParkContext.toJson() = JSONObject()
    .put("temperatureCelsius", temperatureCelsius)
    .put("apparentTemperatureCelsius", apparentTemperatureCelsius)
    .put("windSpeedKmh", windSpeedKmh)
    .put("precipitationMm", precipitationMm)
    .put("weatherCode", weatherCode)
    .put("uvIndexMax", uvIndexMax ?: JSONObject.NULL)
    .put("usAqi", usAqi ?: JSONObject.NULL)
    .put("pm25", pm25 ?: JSONObject.NULL)
    .put("observedAt", observedAt)

private fun JSONObject.optNullableDouble(key: String): Double? =
    if (isNull(key) || !has(key)) null else optDouble(key).takeUnless { it.isNaN() }

private fun org.json.JSONArray.optNullableDouble(index: Int): Double? =
    if (index !in 0 until length() || isNull(index)) null else optDouble(index).takeUnless { it.isNaN() }
