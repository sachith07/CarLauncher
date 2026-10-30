package com.vezel.launcher.ui

import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vezel.launcher.R
import com.vezel.launcher.data.LauncherPreferences
import com.vezel.launcher.vehicle.PaintColor
import com.vezel.launcher.vehicle.VehicleAssetRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: LauncherPreferences
    private lateinit var assets: VehicleAssetRepository
    private lateinit var paintPreview: ImageView
    private lateinit var colorValue: TextView
    private lateinit var swatchRow: LinearLayout
    private val swatchViews = mutableMapOf<String, ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = LauncherPreferences(this)
        assets = VehicleAssetRepository(this)
        paintPreview = findViewById(R.id.paintPreview)
        colorValue = findViewById(R.id.colorValue)
        swatchRow = findViewById(R.id.swatchRow)
        findViewById<TextView>(R.id.themeValue).text = prefs.themeKey
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }

        buildSwatches()
        assets.preloadAll()
        applySelection(prefs.vehicleColorKey, persist = false)
    }

    private fun buildSwatches() {
        swatchRow.removeAllViews()
        swatchViews.clear()
        val size = (48 * resources.displayMetrics.density).toInt()
        val gap = (10 * resources.displayMetrics.density).toInt()
        for (paint in PaintColor.ALL) {
            val swatch = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).also {
                    it.marginEnd = gap
                    it.gravity = Gravity.CENTER_VERTICAL
                }
                contentDescription = paint.displayName
                background = swatchDrawable(paint.swatchArgb, selected = false)
                isClickable = true
                isFocusable = true
                minimumWidth = size
                minimumHeight = size
                setOnClickListener { applySelection(paint.key, persist = true) }
            }
            swatchViews[paint.key] = swatch
            swatchRow.addView(swatch)
        }
    }

    private fun applySelection(key: String, persist: Boolean) {
        val paint = PaintColor.fromKey(key)
        if (persist) {
            prefs.vehicleColorKey = paint.key
        }
        colorValue.text = getString(R.string.color_selected_fmt, paint.displayName, paint.key)

        for ((k, view) in swatchViews) {
            val p = PaintColor.fromKey(k)
            view.background = swatchDrawable(p.swatchArgb, selected = k == paint.key)
        }

        val cached = assets.getCached(paint.key)
        if (cached != null && !cached.isRecycled) {
            paintPreview.setImageBitmap(cached)
        }
        assets.loadAsync(paint.key) { _, bitmap ->
            if (!isFinishing && !bitmap.isRecycled) {
                paintPreview.setImageBitmap(bitmap)
            }
        }
    }

    private fun swatchDrawable(fillArgb: Int, selected: Boolean): GradientDrawable {
        val stroke = if (selected) {
            ContextCompat.getColor(this, R.color.vezel_text)
        } else {
            ContextCompat.getColor(this, R.color.vezel_accent)
        }
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(fillArgb)
            setStroke(if (selected) 4 else 2, stroke)
        }
    }

    override fun onDestroy() {
        assets.shutdown()
        super.onDestroy()
    }
}
