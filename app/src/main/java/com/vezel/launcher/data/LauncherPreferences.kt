package com.vezel.launcher.data

import android.content.Context
import android.content.SharedPreferences

/**
 * SharedPreferences stubs for vehicle color and theme.
 * Real personalization lands in a later sprint.
 */
class LauncherPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var vehicleColorKey: String
        get() = prefs.getString(KEY_VEHICLE_COLOR, DEFAULT_COLOR) ?: DEFAULT_COLOR
        set(value) = prefs.edit().putString(KEY_VEHICLE_COLOR, value).apply()

    var themeKey: String
        get() = prefs.getString(KEY_THEME, DEFAULT_THEME) ?: DEFAULT_THEME
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    companion object {
        private const val PREFS_NAME = "vezel_launcher_prefs"
        private const val KEY_VEHICLE_COLOR = "vehicle_color"
        private const val KEY_THEME = "theme"
        const val DEFAULT_COLOR = "slot_blue_gray"
        const val DEFAULT_THEME = "dark"
    }
}
