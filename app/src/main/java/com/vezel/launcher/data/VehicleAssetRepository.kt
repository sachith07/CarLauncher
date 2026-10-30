package com.vezel.launcher.data

import android.graphics.Color

/**
 * Stub repository for vehicle paint assets.
 * WebP assets are out of scope for Sprint 0–1; returns a solid color slot.
 */
class VehicleAssetRepository(
    private val preferences: LauncherPreferences
) {

    fun currentColorSlot(): Int {
        return when (preferences.vehicleColorKey) {
            "slot_white" -> Color.parseColor("#D8DEE9")
            "slot_red" -> Color.parseColor("#B33A3A")
            "slot_black" -> Color.parseColor("#1A1F2A")
            else -> Color.parseColor("#2A3A52")
        }
    }

    fun availableColorKeys(): List<String> = listOf(
        "slot_blue_gray",
        "slot_white",
        "slot_red",
        "slot_black"
    )
}
