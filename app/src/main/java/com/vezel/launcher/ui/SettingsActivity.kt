package com.vezel.launcher.ui

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.vezel.launcher.BuildConfig
import com.vezel.launcher.R
import com.vezel.launcher.data.LauncherPreferences
import com.vezel.launcher.system.AppDiscovery
import com.vezel.launcher.system.MediaSessionController
import com.vezel.launcher.vehicle.PaintColor
import com.vezel.launcher.vehicle.VehicleAssetRepository

class SettingsActivity : AppCompatActivity() {

    private lateinit var prefs: LauncherPreferences
    private lateinit var assets: VehicleAssetRepository
    private lateinit var discovery: AppDiscovery
    private lateinit var paintPreview: ImageView
    private lateinit var colorValue: TextView
    private lateinit var swatchRow: LinearLayout
    private lateinit var themeValue: TextView
    private lateinit var homeNote: TextView
    private val swatchViews = mutableMapOf<String, ImageView>()

    private data class PkgOption(val label: String, val packageName: String) {
        override fun toString(): String = if (packageName.isEmpty()) label else "$label ($packageName)"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        prefs = LauncherPreferences(this)
        assets = VehicleAssetRepository(this)
        discovery = AppDiscovery(this)
        paintPreview = findViewById(R.id.paintPreview)
        colorValue = findViewById(R.id.colorValue)
        swatchRow = findViewById(R.id.swatchRow)
        themeValue = findViewById(R.id.themeValue)
        homeNote = findViewById(R.id.homeNote)

        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.aboutBody).text = getString(
            R.string.about_body_fmt,
            BuildConfig.VERSION_NAME,
            BuildConfig.FLAVOR,
            if (BuildConfig.HOME_CAPABLE) "yes" else "no"
        )

        setupThemeControls()
        setupOemSpinners()
        setupHomeRecovery()
        setupMediaAccess()
        setupReduceAnim()

        buildSwatches()
        assets.preloadAll()
        applySelection(prefs.vehicleColorKey, persist = false)
        refreshThemeLabel()
        refreshHomeNote()
    }

    private fun setupThemeControls() {
        val dark = findViewById<Button>(R.id.btnThemeDark)
        val day = findViewById<Button>(R.id.btnThemeDay)
        val auto = findViewById<Button>(R.id.btnThemeAuto)
        dark.setOnClickListener { applyTheme(LauncherPreferences.THEME_DARK) }
        day.setOnClickListener { applyTheme(LauncherPreferences.THEME_DAY) }
        auto.setOnClickListener { applyTheme(LauncherPreferences.THEME_AUTO) }
    }

    private fun applyTheme(key: String) {
        prefs.themeKey = key
        when (key) {
            LauncherPreferences.THEME_DAY ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            LauncherPreferences.THEME_AUTO ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else ->
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        refreshThemeLabel()
        // Recreate so dashboard colors pick up day/dark surfaces on next resume.
        recreate()
    }

    private fun refreshThemeLabel() {
        themeValue.text = prefs.themeKey
    }

    private fun setupOemSpinners() {
        val apps = discovery.discoverLauncherApps()
        bindSpinner(R.id.spinnerRadio, apps, prefs.radioPackage, AppDiscovery.RADIO_PACKAGES) {
            prefs.radioPackage = it
        }
        bindSpinner(R.id.spinnerMaps, apps, prefs.mapsPackage, AppDiscovery.MAPS_PACKAGES) {
            prefs.mapsPackage = it
        }
        bindSpinner(R.id.spinnerPhone, apps, prefs.phonePackage, AppDiscovery.PHONE_PACKAGES) {
            prefs.phonePackage = it
        }
        bindSpinner(
            R.id.spinnerMedia,
            apps,
            prefs.mediaPackage,
            MediaSessionController.MEDIA_FALLBACKS
        ) {
            prefs.mediaPackage = it
        }
    }

    private fun bindSpinner(
        spinnerId: Int,
        apps: List<AppDiscovery.LauncherApp>,
        selectedPkg: String,
        hints: List<String>,
        onPick: (String) -> Unit
    ) {
        val spinner = findViewById<Spinner>(spinnerId)
        val options = mutableListOf(PkgOption(getString(R.string.pkg_auto), ""))
        val preferredHints = hints.filter { discovery.isPackageInstalled(it) }
        for (hint in preferredHints) {
            val label = apps.firstOrNull { it.packageName == hint }?.label ?: hint
            options.add(PkgOption(label, hint))
        }
        for (app in apps) {
            if (options.none { it.packageName == app.packageName }) {
                options.add(PkgOption(app.label, app.packageName))
            }
        }
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        val index = options.indexOfFirst { it.packageName == selectedPkg }.coerceAtLeast(0)
        spinner.setSelection(index, false)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                onPick(options[position].packageName)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) = Unit
        }
    }

    private fun setupHomeRecovery() {
        findViewById<Button>(R.id.btnHomeSettings).setOnClickListener {
            if (!discovery.openHomeAppSettings()) {
                Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
            }
        }
        findViewById<Button>(R.id.btnSystemSettings).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_SETTINGS))
            } catch (_: Exception) {
                Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun refreshHomeNote() {
        homeNote.text = if (BuildConfig.HOME_CAPABLE) {
            getString(R.string.home_capable_note)
        } else {
            getString(R.string.not_default_home_note)
        }
    }

    private fun setupMediaAccess() {
        findViewById<Button>(R.id.btnNotificationAccess).setOnClickListener {
            MediaSessionController(this).openNotificationListenerSettings()
        }
    }

    private fun setupReduceAnim() {
        val box = findViewById<CheckBox>(R.id.checkReduceAnim)
        box.isChecked = prefs.reduceAnimations
        box.setOnCheckedChangeListener { _, checked ->
            prefs.reduceAnimations = checked
        }
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
