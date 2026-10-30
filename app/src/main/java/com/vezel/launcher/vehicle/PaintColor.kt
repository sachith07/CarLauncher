package com.vezel.launcher.vehicle

import androidx.annotation.DrawableRes
import com.vezel.launcher.R

/**
 * One of the six body paints (same silhouette, keyed for prefs + drawable).
 */
data class PaintColor(
    val key: String,
    val displayName: String,
    val swatchArgb: Int,
    @DrawableRes val drawableRes: Int
) {
    companion object {
        val ALL: List<PaintColor> = listOf(
            PaintColor("blue_gray", "Blue Gray", 0xFF2A3A52.toInt(), R.drawable.vezel_blue_gray),
            PaintColor("white", "White", 0xFFD8DEE9.toInt(), R.drawable.vezel_white),
            PaintColor("red", "Red", 0xFFB33A3A.toInt(), R.drawable.vezel_red),
            PaintColor("black", "Black", 0xFF1A1F2A.toInt(), R.drawable.vezel_black),
            PaintColor("silver", "Silver", 0xFFA8B0BC.toInt(), R.drawable.vezel_silver),
            PaintColor("blue", "Blue", 0xFF2E5AA0.toInt(), R.drawable.vezel_blue)
        )

        fun fromKey(key: String): PaintColor {
            val normalized = normalizeKey(key)
            return ALL.firstOrNull { it.key == normalized } ?: ALL.first()
        }

        /** Maps Sprint 1 stub keys (slot_*) onto Sprint 2 keys. */
        fun normalizeKey(key: String): String {
            return when (key) {
                "slot_blue_gray", "blue_gray" -> "blue_gray"
                "slot_white", "white" -> "white"
                "slot_red", "red" -> "red"
                "slot_black", "black" -> "black"
                "slot_silver", "silver" -> "silver"
                "slot_blue", "blue" -> "blue"
                else -> "blue_gray"
            }
        }
    }
}
