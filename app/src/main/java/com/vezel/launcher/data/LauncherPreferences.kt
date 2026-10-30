package com.vezel.launcher.data

import android.content.Context
import android.content.SharedPreferences
import com.vezel.launcher.vehicle.PaintColor

/**
 * SharedPreferences for vehicle color, theme, OEM packages, and weather cache.
 */
class LauncherPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var vehicleColorKey: String
        get() = PaintColor.normalizeKey(
            prefs.getString(KEY_VEHICLE_COLOR, DEFAULT_COLOR) ?: DEFAULT_COLOR
        )
        set(value) = prefs.edit()
            .putString(KEY_VEHICLE_COLOR, PaintColor.normalizeKey(value))
            .apply()

    /** dark | day | auto */
    var themeKey: String
        get() = normalizeTheme(prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME)
        set(value) = prefs.edit().putString(KEY_THEME, normalizeTheme(value)).apply()

    var radioPackage: String
        get() = prefs.getString(KEY_RADIO_PKG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_RADIO_PKG, value.trim()).apply()

    var mapsPackage: String
        get() = prefs.getString(KEY_MAPS_PKG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MAPS_PKG, value.trim()).apply()

    var phonePackage: String
        get() = prefs.getString(KEY_PHONE_PKG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_PHONE_PKG, value.trim()).apply()

    var mediaPackage: String
        get() = prefs.getString(KEY_MEDIA_PKG, "") ?: ""
        set(value) = prefs.edit().putString(KEY_MEDIA_PKG, value.trim()).apply()

    /** Reduce non-essential motion (FR-12 optional animations off by default preference). */
    var reduceAnimations: Boolean
        get() = prefs.getBoolean(KEY_REDUCE_ANIM, true)
        set(value) = prefs.edit().putBoolean(KEY_REDUCE_ANIM, value).apply()

    val weatherTempC: Float?
        get() = if (prefs.contains(KEY_WX_TEMP)) prefs.getFloat(KEY_WX_TEMP, 0f) else null

    val weatherSummary: String?
        get() = prefs.getString(KEY_WX_SUMMARY, null)

    val weatherCachedAtMs: Long
        get() = prefs.getLong(KEY_WX_AT, 0L)

    fun cacheWeather(
        tempC: Float,
        summary: String,
        atMs: Long,
        lat: Double,
        lon: Double
    ) {
        prefs.edit()
            .putFloat(KEY_WX_TEMP, tempC)
            .putString(KEY_WX_SUMMARY, summary)
            .putLong(KEY_WX_AT, atMs)
            .putString(KEY_WX_LAT, lat.toString())
            .putString(KEY_WX_LON, lon.toString())
            .apply()
    }

    fun clearWeatherCache() {
        prefs.edit()
            .remove(KEY_WX_TEMP)
            .remove(KEY_WX_SUMMARY)
            .remove(KEY_WX_AT)
            .remove(KEY_WX_LAT)
            .remove(KEY_WX_LON)
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "vezel_launcher_prefs"
        private const val KEY_VEHICLE_COLOR = "vehicle_color"
        private const val KEY_THEME = "theme"
        private const val KEY_RADIO_PKG = "pkg_radio"
        private const val KEY_MAPS_PKG = "pkg_maps"
        private const val KEY_PHONE_PKG = "pkg_phone"
        private const val KEY_MEDIA_PKG = "pkg_media"
        private const val KEY_REDUCE_ANIM = "reduce_animations"
        private const val KEY_WX_TEMP = "wx_temp_c"
        private const val KEY_WX_SUMMARY = "wx_summary"
        private const val KEY_WX_AT = "wx_cached_at"
        private const val KEY_WX_LAT = "wx_lat"
        private const val KEY_WX_LON = "wx_lon"

        const val DEFAULT_COLOR = "blue_gray"
        const val DEFAULT_THEME = "dark"
        const val THEME_DARK = "dark"
        const val THEME_DAY = "day"
        const val THEME_AUTO = "auto"

        fun normalizeTheme(key: String): String = when (key.lowercase()) {
            THEME_DAY, "light" -> THEME_DAY
            THEME_AUTO -> THEME_AUTO
            else -> THEME_DARK
        }
    }
}
