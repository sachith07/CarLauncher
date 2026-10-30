package com.vezel.launcher.vehicle

/**
 * Catalog of available Vezel body paints keyed by [PaintColor.key] / prefs vehicleColor.
 */
class PaintColorRepository {

    fun all(): List<PaintColor> = PaintColor.ALL

    fun find(key: String): PaintColor = PaintColor.fromKey(key)

    fun keys(): List<String> = PaintColor.ALL.map { it.key }
}
