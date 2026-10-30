package com.vezel.launcher.system

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.vezel.launcher.data.LauncherPreferences
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executors

/**
 * HTTPS weather (Open-Meteo, no API key) with SharedPreferences cache and offline degrade.
 * Never blocks launcher startup — fetches on a background executor after resume.
 */
class WeatherRepository(
    context: Context,
    private val onChanged: (WeatherUi) -> Unit = {}
) {

    data class WeatherUi(
        val temperatureC: Float? = null,
        val summary: String? = null,
        val cachedAtLabel: String? = null,
        val available: Boolean = false,
        val fromCache: Boolean = false
    )

    private val appContext = context.applicationContext
    private val prefs = LauncherPreferences(appContext)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val executor = Executors.newSingleThreadExecutor { r ->
        Thread(r, "vezel-weather").apply { isDaemon = true }
    }

    fun current(): WeatherUi = readCacheUi()

    fun refreshAsync() {
        // Publish cache immediately so UI is never empty while network runs.
        val cached = readCacheUi()
        if (cached.available) {
            mainHandler.post { onChanged(cached) }
        } else {
            mainHandler.post { onChanged(WeatherUi(available = false)) }
        }
        executor.execute { fetchAndCache() }
    }

    fun shutdown() {
        executor.shutdownNow()
    }

    @SuppressLint("MissingPermission")
    private fun fetchAndCache() {
        val location = lastKnownLocation()
        if (location == null) {
            mainHandler.post { onChanged(readCacheUi().let { if (it.available) it else WeatherUi(available = false) }) }
            return
        }
        try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=${location.latitude}&longitude=${location.longitude}" +
                    "&current=temperature_2m,weather_code&timezone=auto"
            )
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("Accept", "application/json")
            }
            try {
                val code = conn.responseCode
                if (code !in 200..299) {
                    Log.w(TAG, "Weather HTTP $code")
                    mainHandler.post { onChanged(readCacheUi()) }
                    return
                }
                val body = conn.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(body)
                val current = json.getJSONObject("current")
                val temp = current.getDouble("temperature_2m").toFloat()
                val wmo = current.optInt("weather_code", -1)
                val summary = wmoSummary(wmo)
                val now = System.currentTimeMillis()
                prefs.cacheWeather(temp, summary, now, location.latitude, location.longitude)
                val ui = WeatherUi(
                    temperatureC = temp,
                    summary = summary,
                    cachedAtLabel = formatCacheTime(now),
                    available = true,
                    fromCache = false
                )
                mainHandler.post { onChanged(ui) }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Weather fetch failed: ${e.message}")
            mainHandler.post { onChanged(readCacheUi()) }
        }
    }

    private fun readCacheUi(): WeatherUi {
        val temp = prefs.weatherTempC ?: return WeatherUi(available = false)
        val at = prefs.weatherCachedAtMs
        if (at <= 0L) return WeatherUi(available = false)
        val age = System.currentTimeMillis() - at
        // Keep showing timestamped cache even when stale; hide only if absurdly old.
        if (age > MAX_CACHE_AGE_MS) {
            return WeatherUi(
                temperatureC = temp,
                summary = prefs.weatherSummary,
                cachedAtLabel = formatCacheTime(at),
                available = true,
                fromCache = true
            )
        }
        return WeatherUi(
            temperatureC = temp,
            summary = prefs.weatherSummary,
            cachedAtLabel = formatCacheTime(at),
            available = true,
            fromCache = age > FRESH_MS
        )
    }

    @SuppressLint("MissingPermission")
    private fun lastKnownLocation(): Location? {
        val fine = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (!fine) return null
        val lm = appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatCacheTime(epochMs: Long): String {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        return fmt.format(Date(epochMs))
    }

    private fun wmoSummary(code: Int): String = when (code) {
        0 -> "Clear"
        1, 2, 3 -> "Cloudy"
        45, 48 -> "Fog"
        51, 53, 55, 56, 57 -> "Drizzle"
        61, 63, 65, 66, 67, 80, 81, 82 -> "Rain"
        71, 73, 75, 77, 85, 86 -> "Snow"
        95, 96, 99 -> "Storm"
        else -> "Weather"
    }

    companion object {
        private const val TAG = "VezelLauncher"
        private const val FRESH_MS = 30 * 60 * 1000L
        private const val MAX_CACHE_AGE_MS = 24 * 60 * 60 * 1000L
    }
}
