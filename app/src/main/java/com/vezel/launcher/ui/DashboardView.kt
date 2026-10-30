package com.vezel.launcher.ui

import android.content.Context
import android.graphics.Bitmap
import android.util.AttributeSet
import android.view.Gravity
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.vezel.launcher.R
import com.vezel.launcher.data.LauncherPreferences
import com.vezel.launcher.system.AppDiscovery
import com.vezel.launcher.system.ClockProvider
import com.vezel.launcher.system.LocationSpeedProvider
import com.vezel.launcher.system.MediaSessionController
import com.vezel.launcher.system.WeatherRepository
import com.vezel.launcher.vehicle.VehicleAssetRepository

/**
 * 1280×720-oriented dashboard: header, media/weather, speed + vehicle, dock.
 */
class DashboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    interface Callbacks {
        fun onOpenSettings()
        fun onOpenApps()
        fun onRequestLocationPermission()
    }

    var callbacks: Callbacks? = null

    private val preferences = LauncherPreferences(context)
    private val assets = VehicleAssetRepository(context)
    private val discovery = AppDiscovery(context)

    private val brandView: TextView
    private val clockView: TextView
    private val dateView: TextView
    private val vehicleStage: FrameLayout
    private val vehicleImage: ImageView
    private val speedGauge: SpeedGaugeView
    private val mediaTitle: TextView
    private val mediaArtist: TextView
    private val weatherTemp: TextView
    private val weatherSummary: TextView

    private var displayedColorKey: String? = null
    private var bound = false

    private val speedProvider = LocationSpeedProvider(context) { kmh ->
        post { speedGauge.setSpeedKmh(kmh) }
    }

    private val mediaController = MediaSessionController(context) { now ->
        post { applyMedia(now) }
    }

    private val weatherRepository = WeatherRepository(context) { wx ->
        post { applyWeather(wx) }
    }

    private lateinit var clockProvider: ClockProvider

    init {
        orientation = VERTICAL
        applyThemeBackground()
        val pad = dp(16)
        setPadding(pad, pad, pad, pad)

        val header = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(ContextCompat.getColor(context, themeHeaderColor()))
            setPadding(dp(16), dp(10), dp(16), dp(10))
        }
        brandView = TextView(context).apply {
            text = context.getString(R.string.brand_name)
            setTextColor(ContextCompat.getColor(context, themeTextColor()))
            textSize = 22f
            paint.isFakeBoldText = true
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        val clockCol = LinearLayout(context).apply {
            orientation = VERTICAL
            gravity = Gravity.END
            setPadding(dp(12), 0, dp(12), 0)
        }
        clockView = TextView(context).apply {
            text = context.getString(R.string.clock_placeholder)
            setTextColor(ContextCompat.getColor(context, themeTextColor()))
            textSize = 20f
            gravity = Gravity.END
        }
        dateView = TextView(context).apply {
            text = ""
            setTextColor(ContextCompat.getColor(context, themeMutedColor()))
            textSize = 12f
            gravity = Gravity.END
        }
        clockCol.addView(clockView)
        clockCol.addView(dateView)
        val settingsBtn = Button(context).apply {
            text = context.getString(R.string.settings)
            setOnClickListener { callbacks?.onOpenSettings() }
        }
        header.addView(brandView)
        header.addView(clockCol)
        header.addView(settingsBtn)
        addView(header, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))

        val cards = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.topMargin = dp(10)
            }
        }
        val mediaCard = cardColumn().also { col ->
            col.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1.2f)
            TextView(context).apply {
                text = context.getString(R.string.media_label)
                setTextColor(ContextCompat.getColor(context, R.color.vezel_text_muted))
                textSize = 12f
                col.addView(this)
            }
            mediaTitle = TextView(context).apply {
                text = context.getString(R.string.media_placeholder)
                setTextColor(ContextCompat.getColor(context, R.color.vezel_text))
                textSize = 16f
                maxLines = 1
                col.addView(this)
            }
            mediaArtist = TextView(context).apply {
                text = context.getString(R.string.media_hint)
                setTextColor(ContextCompat.getColor(context, R.color.vezel_text_muted))
                textSize = 13f
                maxLines = 1
                col.addView(this)
            }
            col.setOnClickListener {
                if (!mediaController.openPlayerOrFallback(discovery)) {
                    Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show()
                }
            }
            col.setOnLongClickListener {
                mediaController.openNotificationListenerSettings()
                true
            }
        }
        val weatherCard = cardColumn().also { col ->
            col.layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 0.8f).also {
                it.marginStart = dp(10)
            }
            TextView(context).apply {
                text = context.getString(R.string.weather_label)
                setTextColor(ContextCompat.getColor(context, R.color.vezel_text_muted))
                textSize = 12f
                col.addView(this)
            }
            weatherTemp = TextView(context).apply {
                text = context.getString(R.string.weather_placeholder)
                setTextColor(ContextCompat.getColor(context, R.color.vezel_text))
                textSize = 18f
                col.addView(this)
            }
            weatherSummary = TextView(context).apply {
                text = context.getString(R.string.weather_offline)
                setTextColor(ContextCompat.getColor(context, R.color.vezel_text_muted))
                textSize = 13f
                maxLines = 2
                col.addView(this)
            }
        }
        cards.addView(mediaCard)
        cards.addView(weatherCard)
        addView(cards)

        val stageRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 0, 1f).also {
                it.topMargin = dp(12)
            }
        }

        speedGauge = SpeedGaugeView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 0.55f)
            setSpeedKmh(null)
            setOnClickListener {
                if (!speedProvider.hasLocationPermission()) {
                    callbacks?.onRequestLocationPermission()
                }
            }
        }

        vehicleImage = ImageView(context).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            contentDescription = context.getString(R.string.vehicle_stage)
            setBackgroundColor(ContextCompat.getColor(context, R.color.vezel_vehicle_slot))
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        vehicleStage = FrameLayout(context).apply {
            setBackgroundColor(ContextCompat.getColor(context, R.color.vezel_vehicle_slot))
            layoutParams = LayoutParams(0, LayoutParams.MATCH_PARENT, 1.45f).also {
                it.marginStart = dp(12)
            }
            addView(vehicleImage)
        }

        stageRow.addView(speedGauge)
        stageRow.addView(vehicleStage)
        addView(stageRow)

        val dock = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER
            setBackgroundColor(ContextCompat.getColor(context, themeDockColor()))
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
                minimumHeight = dp(48)
                setOnClickListener { click() }
            }
        }
        dock.addView(dockButton(R.string.dock_radio) { discovery.launchRadio() })
        dock.addView(dockButton(R.string.dock_maps) { discovery.launchMaps() })
        dock.addView(dockButton(R.string.dock_phone) { discovery.launchPhone() })
        dock.addView(dockButton(R.string.dock_apps) { callbacks?.onOpenApps() })
        addView(dock)

        clockProvider = ClockProvider(context) { time, date ->
            post {
                clockView.text = time
                dateView.text = date
            }
        }

        assets.preloadAll()
        refreshClock()
        refreshVehicleSlot()
        applyMedia(mediaController.current())
        applyWeather(weatherRepository.current())
    }

    fun bindLifecycle() {
        if (bound) {
            refreshAll()
            return
        }
        bound = true
        clockProvider.start()
        speedProvider.start()
        mediaController.connect()
        weatherRepository.refreshAsync()
        refreshVehicleSlot()
    }

    fun unbindLifecycle() {
        clockProvider.stop()
        speedProvider.stop()
        mediaController.disconnect()
        bound = false
    }

    fun refreshAll() {
        refreshClock()
        refreshVehicleSlot()
        refreshSpeed()
        mediaController.refresh()
        weatherRepository.refreshAsync()
        applyThemeBackground()
    }

    fun refreshClock() {
        clockProvider.publish()
    }

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
        val changed = displayedColorKey != null && displayedColorKey != key
        vehicleImage.setImageBitmap(bitmap)
        displayedColorKey = key
        if (changed && !preferences.reduceAnimations) {
            vehicleImage.startAnimation(AlphaAnimation(0.85f, 1f).apply {
                duration = 180
                fillAfter = true
            })
        }
    }

    fun refreshSpeed() {
        speedGauge.setSpeedKmh(speedProvider.currentSpeedKmh())
        if (!speedProvider.hasLocationPermission()) {
            speedGauge.setSpeedKmh(null)
        }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            speedProvider.stop()
            speedProvider.start()
            weatherRepository.refreshAsync()
        } else {
            speedGauge.setSpeedKmh(null)
        }
    }

    private fun applyMedia(now: MediaSessionController.NowPlaying) {
        if (now.available && !now.title.isNullOrBlank()) {
            mediaTitle.text = now.title
            mediaArtist.text = when {
                !now.artist.isNullOrBlank() && now.isPlaying ->
                    context.getString(R.string.media_playing_fmt, now.artist)
                !now.artist.isNullOrBlank() -> now.artist
                now.isPlaying -> context.getString(R.string.media_playing)
                else -> context.getString(R.string.media_paused)
            }
        } else {
            mediaTitle.text = context.getString(R.string.media_placeholder)
            mediaArtist.text = if (mediaController.isNotificationAccessEnabled()) {
                context.getString(R.string.media_idle)
            } else {
                context.getString(R.string.media_hint)
            }
        }
    }

    private fun applyWeather(wx: WeatherRepository.WeatherUi) {
        if (!wx.available || wx.temperatureC == null) {
            weatherTemp.text = context.getString(R.string.weather_placeholder)
            weatherSummary.text = context.getString(R.string.weather_offline)
            return
        }
        weatherTemp.text = context.getString(R.string.weather_temp_fmt, wx.temperatureC)
        val parts = mutableListOf<String>()
        wx.summary?.let { parts.add(it) }
        if (wx.fromCache && wx.cachedAtLabel != null) {
            parts.add(context.getString(R.string.weather_cached_fmt, wx.cachedAtLabel))
        }
        weatherSummary.text = parts.joinToString(" · ").ifBlank {
            context.getString(R.string.weather_label)
        }
    }

    private fun cardColumn(): LinearLayout {
        return LinearLayout(context).apply {
            orientation = VERTICAL
            setBackgroundResource(R.drawable.bg_tile)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            isClickable = true
            isFocusable = true
            minimumHeight = dp(72)
        }
    }

    private fun applyThemeBackground() {
        setBackgroundColor(ContextCompat.getColor(context, themeBgColor()))
    }

    private fun themeBgColor(): Int = when (preferences.themeKey) {
        LauncherPreferences.THEME_DAY -> R.color.vezel_day_bg
        else -> R.color.vezel_bg
    }

    private fun themeHeaderColor(): Int = when (preferences.themeKey) {
        LauncherPreferences.THEME_DAY -> R.color.vezel_day_header
        else -> R.color.vezel_header
    }

    private fun themeDockColor(): Int = when (preferences.themeKey) {
        LauncherPreferences.THEME_DAY -> R.color.vezel_day_dock
        else -> R.color.vezel_dock
    }

    private fun themeTextColor(): Int = when (preferences.themeKey) {
        LauncherPreferences.THEME_DAY -> R.color.vezel_day_text
        else -> R.color.vezel_text
    }

    private fun themeMutedColor(): Int = when (preferences.themeKey) {
        LauncherPreferences.THEME_DAY -> R.color.vezel_day_muted
        else -> R.color.vezel_text_muted
    }

    override fun onDetachedFromWindow() {
        unbindLifecycle()
        weatherRepository.shutdown()
        assets.shutdown()
        super.onDetachedFromWindow()
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
