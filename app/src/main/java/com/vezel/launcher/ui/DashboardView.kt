package com.vezel.launcher.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
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
 * Mockup-aligned 1280×720 dashboard: header, three cards, vehicle hero, 8-item dock.
 */
class DashboardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    interface Callbacks {
        fun onOpenSettings()
        fun onOpenApps()
        fun onRequestLocationPermission()
    }

    var callbacks: Callbacks? = null

    private val preferences = LauncherPreferences(context)
    private val assets = VehicleAssetRepository(context)
    private val discovery = AppDiscovery(context)

    private val greetingText: TextView
    private val dateText: TextView
    private val clockText: TextView
    private val clockAmPm: TextView
    private val themePill: View
    private val themePillText: TextView
    private val speedGauge: SpeedGaugeView
    private val tripValue: TextView
    private val driveValue: TextView
    private val headingValue: TextView
    private val mediaArt: ImageView
    private val mediaTitle: TextView
    private val mediaArtist: TextView
    private val mediaProgress: ProgressBar
    private val mediaPosition: TextView
    private val mediaDuration: TextView
    private val btnMediaPlay: ImageView
    private val weatherLocation: TextView
    private val weatherHiLo: TextView
    private val weatherDetails: TextView
    private val vehicleImage: ImageView
    private val vehicleReflection: ImageView
    private val dashboardRoot: View

    private val dockItems: List<DockItem>

    private var displayedColorKey: String? = null
    private var bound = false
    private var selectedDockId: Int = R.id.dockNav
    private var lastHour24: Int = 12

    private val speedProvider = LocationSpeedProvider(context) { telemetry ->
        post { applyTelemetry(telemetry) }
    }

    private val mediaController = MediaSessionController(context) { now ->
        post { applyMedia(now) }
    }

    private val weatherRepository = WeatherRepository(context) { wx ->
        post { applyWeather(wx) }
    }

    private lateinit var clockProvider: ClockProvider

    private data class DockItem(
        val root: View,
        val icon: ImageView,
        val label: TextView,
        val underline: View,
        val id: Int
    )

    init {
        LayoutInflater.from(context).inflate(R.layout.dashboard_content, this, true)
        dashboardRoot = findViewById(R.id.dashboardRoot)
        greetingText = findViewById(R.id.greetingText)
        dateText = findViewById(R.id.dateText)
        clockText = findViewById(R.id.clockText)
        clockAmPm = findViewById(R.id.clockAmPm)
        themePill = findViewById(R.id.themePill)
        themePillText = findViewById(R.id.themePillText)
        speedGauge = findViewById(R.id.speedGauge)
        tripValue = findViewById(R.id.tripValue)
        driveValue = findViewById(R.id.driveValue)
        headingValue = findViewById(R.id.headingValue)
        mediaArt = findViewById(R.id.mediaArt)
        mediaTitle = findViewById(R.id.mediaTitle)
        mediaArtist = findViewById(R.id.mediaArtist)
        mediaProgress = findViewById(R.id.mediaProgress)
        mediaPosition = findViewById(R.id.mediaPosition)
        mediaDuration = findViewById(R.id.mediaDuration)
        btnMediaPlay = findViewById(R.id.btnMediaPlay)
        weatherLocation = findViewById(R.id.weatherLocation)
        weatherHiLo = findViewById(R.id.weatherHiLo)
        weatherDetails = findViewById(R.id.weatherDetails)
        vehicleImage = findViewById(R.id.vehicleImage)
        vehicleReflection = findViewById(R.id.vehicleReflection)

        findViewById<View>(R.id.btnHome).setOnClickListener {
            selectDock(R.id.dockNav)
        }

        themePill.setOnClickListener { cycleTheme() }

        speedGauge.setSpeedKmh(null)
        findViewById<View>(R.id.speedCard).setOnClickListener {
            if (!speedProvider.hasLocationPermission()) {
                callbacks?.onRequestLocationPermission()
            }
        }

        findViewById<View>(R.id.mediaCard).setOnLongClickListener {
            mediaController.openNotificationListenerSettings()
            true
        }
        findViewById<ImageView>(R.id.btnMediaPrev).setOnClickListener {
            if (mediaController.current().available) mediaController.skipToPrevious()
            else openMusic()
        }
        btnMediaPlay.setOnClickListener {
            if (mediaController.current().available) mediaController.togglePlayPause()
            else openMusic()
        }
        findViewById<ImageView>(R.id.btnMediaNext).setOnClickListener {
            if (mediaController.current().available) mediaController.skipToNext()
            else openMusic()
        }

        dockItems = listOf(
            bindDock(R.id.dockNav, R.drawable.ic_dock_nav, R.string.dock_navigation) {
                selectDock(R.id.dockNav)
                discovery.launchMaps()
            },
            bindDock(R.id.dockMusic, R.drawable.ic_dock_music, R.string.dock_music) {
                selectDock(R.id.dockMusic)
                openMusic()
            },
            bindDock(R.id.dockVideo, R.drawable.ic_dock_video, R.string.dock_video) {
                selectDock(R.id.dockVideo)
                discovery.launchVideo()
            },
            bindDock(R.id.dockPhone, R.drawable.ic_dock_phone, R.string.dock_phone) {
                selectDock(R.id.dockPhone)
                discovery.launchPhone()
            },
            bindDock(R.id.dockRadio, R.drawable.ic_dock_radio, R.string.dock_radio) {
                selectDock(R.id.dockRadio)
                discovery.launchRadio()
            },
            bindDock(R.id.dockConnectivity, R.drawable.ic_dock_connectivity, R.string.dock_connectivity) {
                selectDock(R.id.dockConnectivity)
                discovery.launchConnectivity()
            },
            bindDock(R.id.dockSettings, R.drawable.ic_dock_settings, R.string.dock_settings) {
                selectDock(R.id.dockSettings)
                callbacks?.onOpenSettings()
            },
            bindDock(R.id.dockApps, R.drawable.ic_dock_apps, R.string.dock_apps) {
                selectDock(R.id.dockApps)
                callbacks?.onOpenApps()
            }
        )
        selectDock(R.id.dockNav)

        clockProvider = ClockProvider(context) { ui ->
            post { applyClock(ui) }
        }

        assets.preloadAll()
        refreshClock()
        refreshVehicleSlot()
        applyMedia(mediaController.current())
        applyWeather(weatherRepository.current())
        applyTelemetry(speedProvider.current())
        applyThemeSurfaces()
        updateThemePill()
    }

    private fun bindDock(id: Int, iconRes: Int, labelRes: Int, click: () -> Unit): DockItem {
        val root = findViewById<View>(id)
        val icon = root.findViewById<ImageView>(R.id.dockIcon)
        val label = root.findViewById<TextView>(R.id.dockLabel)
        val underline = root.findViewById<View>(R.id.dockUnderline)
        icon.setImageResource(iconRes)
        label.setText(labelRes)
        root.setOnClickListener { click() }
        return DockItem(root, icon, label, underline, id)
    }

    private fun selectDock(id: Int) {
        selectedDockId = id
        val accent = ContextCompat.getColor(context, R.color.vezel_accent)
        val normal = ContextCompat.getColor(context, themeTextColor())
        for (item in dockItems) {
            val selected = item.id == id
            val color = if (selected) accent else normal
            item.icon.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN)
            item.label.setTextColor(color)
            item.underline.visibility = if (selected) View.VISIBLE else View.INVISIBLE
        }
    }

    private fun openMusic() {
        if (!mediaController.openPlayerOrFallback(discovery)) {
            Toast.makeText(context, R.string.no_app_found, Toast.LENGTH_SHORT).show()
        }
    }

    private fun cycleTheme() {
        preferences.themeKey = when (preferences.themeKey) {
            LauncherPreferences.THEME_DARK -> LauncherPreferences.THEME_DAY
            LauncherPreferences.THEME_DAY -> LauncherPreferences.THEME_AUTO
            else -> LauncherPreferences.THEME_DARK
        }
        applyThemeSurfaces()
        updateThemePill()
        selectDock(selectedDockId)
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
        applyThemeSurfaces()
        updateThemePill()
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
        applyThemeSurfaces()
        updateThemePill()
        selectDock(selectedDockId)
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
        vehicleReflection.setImageBitmap(bitmap)
        displayedColorKey = key
        if (changed && !preferences.reduceAnimations) {
            vehicleImage.startAnimation(AlphaAnimation(0.85f, 1f).apply {
                duration = 180
                fillAfter = true
            })
        }
    }

    fun refreshSpeed() {
        applyTelemetry(speedProvider.current())
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

    private fun applyClock(ui: ClockProvider.ClockUi) {
        lastHour24 = ui.hour24
        greetingText.text = ui.greeting
        dateText.text = ui.dateLong
        clockText.text = ui.time12
        clockAmPm.text = ui.amPm
        if (preferences.themeKey == LauncherPreferences.THEME_AUTO) {
            applyThemeSurfaces()
            updateThemePill()
            selectDock(selectedDockId)
        }
    }

    private fun applyTelemetry(t: LocationSpeedProvider.Telemetry) {
        speedGauge.setSpeedKmh(t.speedKmh)
        tripValue.text = context.getString(R.string.trip_fmt, t.tripKm)
        driveValue.text = context.getString(R.string.drive_fmt, t.driveMinutes)
        headingValue.text = t.heading ?: context.getString(R.string.heading_placeholder)
    }

    private fun applyMedia(now: MediaSessionController.NowPlaying) {
        if (now.available && !now.title.isNullOrBlank()) {
            mediaTitle.text = now.title
            mediaArtist.text = now.artist?.takeIf { it.isNotBlank() }
                ?: if (now.isPlaying) {
                    context.getString(R.string.media_playing)
                } else {
                    context.getString(R.string.media_paused)
                }
        } else {
            mediaTitle.text = context.getString(R.string.media_placeholder_title)
            mediaArtist.text = if (mediaController.isNotificationAccessEnabled()) {
                context.getString(R.string.media_idle)
            } else {
                context.getString(R.string.media_placeholder_artist)
            }
        }

        if (now.art != null && !now.art.isRecycled) {
            mediaArt.setImageBitmap(now.art)
            mediaArt.scaleType = ImageView.ScaleType.CENTER_CROP
            mediaArt.setPadding(0, 0, 0, 0)
            mediaArt.clearColorFilter()
        } else {
            mediaArt.setImageResource(R.drawable.ic_music_note)
            mediaArt.scaleType = ImageView.ScaleType.CENTER
            val pad = (18 * resources.displayMetrics.density).toInt()
            mediaArt.setPadding(pad, pad, pad, pad)
            mediaArt.colorFilter = PorterDuffColorFilter(
                ContextCompat.getColor(context, R.color.vezel_text_muted),
                PorterDuff.Mode.SRC_IN
            )
        }

        btnMediaPlay.setImageResource(
            if (now.isPlaying) R.drawable.ic_media_pause else R.drawable.ic_media_play
        )

        val duration = now.durationMs.coerceAtLeast(0L)
        val position = now.positionMs.coerceAtLeast(0L)
        if (duration > 0L) {
            mediaProgress.max = 1000
            mediaProgress.progress = ((position.toDouble() / duration.toDouble()) * 1000.0).toInt()
                .coerceIn(0, 1000)
            mediaPosition.text = formatMs(position)
            mediaDuration.text = formatMs(duration)
        } else {
            mediaProgress.progress = 0
            mediaPosition.text = context.getString(R.string.media_time_zero)
            mediaDuration.text = context.getString(R.string.media_time_zero)
        }
    }

    private fun applyWeather(wx: WeatherRepository.WeatherUi) {
        if (!wx.available || wx.temperatureC == null) {
            weatherLocation.text = context.getString(R.string.weather_location_placeholder)
            weatherHiLo.text = context.getString(R.string.weather_placeholder)
            weatherDetails.text = context.getString(R.string.weather_offline)
            return
        }
        weatherLocation.text = wx.locationLabel
            ?: context.getString(R.string.weather_location_default)
        val high = wx.highC ?: wx.temperatureC
        val low = wx.lowC ?: wx.temperatureC
        weatherHiLo.text = context.getString(R.string.weather_hi_lo_fmt, high, low)
        val summary = wx.summary ?: context.getString(R.string.weather_label)
        val sunset = wx.sunsetLabel ?: "--:--"
        var details = context.getString(
            R.string.weather_details_fmt,
            wx.temperatureC,
            summary,
            sunset
        )
        if (wx.fromCache && wx.cachedAtLabel != null) {
            details += " · " + context.getString(R.string.weather_cached_fmt, wx.cachedAtLabel)
        }
        weatherDetails.text = details
    }

    private fun formatMs(ms: Long): String {
        val totalSec = (ms / 1000L).toInt().coerceAtLeast(0)
        val m = totalSec / 60
        val s = totalSec % 60
        return "%d:%02d".format(m, s)
    }

    private fun applyThemeSurfaces() {
        val bg = ContextCompat.getColor(context, themeBgColor())
        setBackgroundColor(bg)
        dashboardRoot.setBackgroundColor(bg)
        val tile = ContextCompat.getColor(context, themeTileColor())
        findViewById<View>(R.id.speedCard).background?.mutate()?.setTint(tile)
        findViewById<View>(R.id.mediaCard).background?.mutate()?.setTint(tile)
        findViewById<View>(R.id.weatherCard).background?.mutate()?.setTint(tile)
        themePill.background?.mutate()?.setTint(ContextCompat.getColor(context, themePillColor()))

        val text = ContextCompat.getColor(context, themeTextColor())
        val muted = ContextCompat.getColor(context, themeMutedColor())
        greetingText.setTextColor(text)
        dateText.setTextColor(muted)
        clockText.setTextColor(text)
        clockAmPm.setTextColor(muted)
        themePillText.setTextColor(text)
        mediaTitle.setTextColor(text)
        mediaArtist.setTextColor(muted)
        weatherHiLo.setTextColor(text)
        weatherDetails.setTextColor(muted)
        weatherLocation.setTextColor(muted)
        tripValue.setTextColor(text)
        driveValue.setTextColor(text)
        headingValue.setTextColor(text)
    }

    private fun updateThemePill() {
        val key = preferences.themeKey
        val night = when (key) {
            LauncherPreferences.THEME_DAY -> false
            LauncherPreferences.THEME_DARK -> true
            else -> clockProvider.isNightHour(lastHour24)
        }
        themePillText.text = when (key) {
            LauncherPreferences.THEME_AUTO ->
                if (night) {
                    context.getString(R.string.theme_pill_auto_night)
                } else {
                    context.getString(R.string.theme_pill_auto_day)
                }
            LauncherPreferences.THEME_DAY -> context.getString(R.string.theme_pill_day)
            else -> context.getString(R.string.theme_pill_night)
        }
    }

    private fun effectiveDark(): Boolean = when (preferences.themeKey) {
        LauncherPreferences.THEME_DAY -> false
        LauncherPreferences.THEME_DARK -> true
        else -> clockProvider.isNightHour(lastHour24)
    }

    private fun themeBgColor(): Int =
        if (effectiveDark()) R.color.vezel_bg else R.color.vezel_day_bg

    private fun themeTileColor(): Int =
        if (effectiveDark()) R.color.vezel_tile else R.color.vezel_day_tile

    private fun themePillColor(): Int =
        if (effectiveDark()) R.color.vezel_pill else R.color.vezel_day_pill

    private fun themeTextColor(): Int =
        if (effectiveDark()) R.color.vezel_text else R.color.vezel_day_text

    private fun themeMutedColor(): Int =
        if (effectiveDark()) R.color.vezel_text_muted else R.color.vezel_day_muted

    override fun onDetachedFromWindow() {
        unbindLifecycle()
        weatherRepository.shutdown()
        assets.shutdown()
        super.onDetachedFromWindow()
    }
}
