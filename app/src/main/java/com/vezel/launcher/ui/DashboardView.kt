package com.vezel.launcher.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.vezel.launcher.R
import com.vezel.launcher.data.LauncherPreferences
import com.vezel.launcher.system.AppDiscovery
import com.vezel.launcher.system.LocationSpeedProvider
import com.vezel.launcher.vehicle.VehicleAssetRepository
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 1280×720-oriented dashboard: header, vehicle stage, speed gauge, dock.
 */
class DashboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Callbacks {
        fun onOpenSettings()
        fun onOpenApps()
    }

    var callbacks: Callbacks? = null

    private val preferences = LauncherPreferences(context)
    private val assets = VehicleAssetRepository(context)
    private val discovery = AppDiscovery(context)
    private val speedProvider = LocationSpeedProvider()

    private val brandView: TextView
    private val clockView: TextView
    private val vehicleStage: FrameLayout
    private val vehicleImage: ImageView
    private val speedGauge: SpeedGaugeView
    private val shortcutRow: LinearLayout

    private var displayedColorKey: String? = null

    init {
        orientation = VERTICAL
        setBackgroundColor(ContextCompat.getColor(context, R.color.vezel_bg))
        val pad = dp(16)
        setPadding(pad, pad, pad, pad)

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, R.color.vezel_header))
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        brandView = TextView(context).apply {
            text = context.getString(R.string.brand_name)
            setTextColor(ContextCompat.getColor(context, R.color.vezel_text))
            textSize = 22f
            paint.isFakeBoldText = true
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        clockView = TextView(context).apply {
            text = context.getString(R.string.clock_placeholder)
            setTextColor(ContextCompat.getColor(context, R.color.vezel_text_muted))
            textSize = 20f
            setPadding(dp(12), 0, dp(12), 0)
        }
        val settingsBtn = Button(context).apply {
            text = context.getString(R.string.settings)
            setOnClickListener { callbacks?.onOpenSettings() }
        }
        header.addView(brandView)
        header.addView(clockView)
        header.addView(settingsBtn)
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val stageRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).also {
                it.topMargin = dp(12)
            }
        }

        vehicleImage = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            contentDescription = context.getString(R.string.vehicle_stage)
            // Keep stage dark so decode latency never flashes white.
            setBackgroundColor(ContextCompat.getColor(context, R.color.vezel_vehicle_slot))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        vehicleStage = FrameLayout(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.vezel_vehicle_slot))
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.4f)
            addView(vehicleImage)
        }

        speedGauge = SpeedGaugeView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 0.6f).also {
                it.marginStart = dp(12)
            }
            setSpeedKmh(speedProvider.currentSpeedKmh())
        }

        stageRow.addView(vehicleStage)
        stageRow.addView(speedGauge)
        addView(stageRow)

        shortcutRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.topMargin = dp(12)
            }
        }
        listOf("Radio", "Maps", "Phone", "Apps").forEachIndexed { index, label ->
            val tile = TextView(context).apply {
                text = label
                gravity = Gravity.CENTER
                textSize = 16f
                setTextColor(ContextCompat.getColor(context, R.color.vezel_text))
                setBackgroundResource(R.drawable.bg_tile)
                setPadding(dp(8), dp(18), dp(8), dp(18))
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).also {
                    if (index > 0) it.marginStart = dp(10)
                }
                setOnClickListener {
                    when (label) {
                        "Radio" -> discovery.launchKnownOrFallback(AppDiscovery.RADIO_PACKAGES)
                        "Maps" -> discovery.launchKnownOrFallback(
                            AppDiscovery.MAPS_PACKAGES,
                            Intent.ACTION_VIEW
                        )
                        "Phone" -> discovery.launchKnownOrFallback(
                            AppDiscovery.PHONE_PACKAGES,
                            Intent.ACTION_DIAL
                        )
                        "Apps" -> callbacks?.onOpenApps()
                    }
                }
            }
            shortcutRow.addView(tile)
        }
        addView(shortcutRow)

        val dock = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(ContextCompat.getColor(context, R.color.vezel_dock))
            setPadding(dp(12), dp(10), dp(12), dp(10))
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.topMargin = dp(12)
            }
        }
        fun dockButton(labelRes: Int, click: () -> Unit): Button {
            return Button(context).apply {
                text = context.getString(labelRes)
                setBackgroundResource(R.drawable.bg_dock_button)
                setTextColor(ContextCompat.getColor(context, R.color.vezel_text))
                layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).also {
                    it.marginStart = dp(6)
                    it.marginEnd = dp(6)
                }
                setOnClickListener { click() }
            }
        }
        dock.addView(dockButton(R.string.dock_radio) {
            discovery.launchKnownOrFallback(AppDiscovery.RADIO_PACKAGES)
        })
        dock.addView(dockButton(R.string.dock_maps) {
            discovery.launchKnownOrFallback(AppDiscovery.MAPS_PACKAGES, Intent.ACTION_VIEW)
        })
        dock.addView(dockButton(R.string.dock_phone) {
            discovery.launchKnownOrFallback(AppDiscovery.PHONE_PACKAGES, Intent.ACTION_DIAL)
        })
        dock.addView(dockButton(R.string.dock_apps) {
            callbacks?.onOpenApps()
        })
        addView(dock)

        assets.preloadAll()
        refreshClock()
        refreshVehicleSlot()
    }

    fun refreshClock() {
        val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
        clockView.text = fmt.format(Date())
    }

    /**
     * Applies the persisted paint. Keeps the previous bitmap until the new one is ready.
     */
    fun refreshVehicleSlot() {
        val key = preferences.vehicleColorKey
        val cached = assets.getCached(key)
        if (cached != null && !cached.isRecycled) {
            applyBitmap(key, cached)
        }
        assets.loadAsync(key) { paint, bitmap ->
            applyBitmap(paint.key, bitmap)
        }
    }

    private fun applyBitmap(key: String, bitmap: Bitmap) {
        if (bitmap.isRecycled) return
        vehicleImage.setImageBitmap(bitmap)
        displayedColorKey = key
    }

    fun refreshSpeed() {
        speedGauge.setSpeedKmh(speedProvider.currentSpeedKmh())
    }

    override fun onDetachedFromWindow() {
        assets.shutdown()
        super.onDetachedFromWindow()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
