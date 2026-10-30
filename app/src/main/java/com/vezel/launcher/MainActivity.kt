package com.vezel.launcher

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.GridView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vezel.launcher.system.AppDiscovery
import com.vezel.launcher.ui.DashboardView
import com.vezel.launcher.ui.SettingsActivity

class MainActivity : AppCompatActivity(), DashboardView.Callbacks {

    private lateinit var dashboardView: DashboardView
    private lateinit var appsPanel: View
    private lateinit var appsGrid: GridView
    private lateinit var appsEmpty: TextView
    private lateinit var discovery: AppDiscovery

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        dashboardView.onLocationPermissionResult(granted)
        if (!granted) {
            Toast.makeText(this, R.string.location_denied, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        discovery = AppDiscovery(this)
        dashboardView = findViewById(R.id.dashboardView)
        appsPanel = findViewById(R.id.appsPanel)
        appsGrid = findViewById(R.id.appsGrid)
        appsEmpty = findViewById(R.id.appsEmpty)
        dashboardView.callbacks = this

        findViewById<Button>(R.id.btnBackDashboard).setOnClickListener {
            showDashboard()
        }
        findViewById<Button>(R.id.btnRefreshApps).setOnClickListener {
            populateApps(forceRefresh = true)
        }

        logRuntimeInfo()
        maybeRequestLocation()
    }

    override fun onResume() {
        super.onResume()
        dashboardView.bindLifecycle()
    }

    override fun onPause() {
        dashboardView.unbindLifecycle()
        super.onPause()
    }

    override fun onOpenSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onOpenApps() {
        populateApps(forceRefresh = false)
        appsPanel.visibility = View.VISIBLE
        dashboardView.visibility = View.GONE
    }

    override fun onRequestLocationPermission() {
        maybeRequestLocation(forcePrompt = true)
    }

    private fun populateApps(forceRefresh: Boolean) {
        val apps = discovery.discoverLauncherApps(forceRefresh)
        if (apps.isEmpty()) {
            appsEmpty.visibility = View.VISIBLE
            appsGrid.visibility = View.GONE
        } else {
            appsEmpty.visibility = View.GONE
            appsGrid.visibility = View.VISIBLE
            appsGrid.adapter = AppGridAdapter(apps)
            appsGrid.setOnItemClickListener { _, _, position, _ ->
                val app = apps[position]
                if (!discovery.openLauncherApp(app)) {
                    Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showDashboard() {
        appsPanel.visibility = View.GONE
        dashboardView.visibility = View.VISIBLE
        dashboardView.refreshAll()
    }

    private fun maybeRequestLocation(forcePrompt: Boolean = false) {
        val granted = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            dashboardView.onLocationPermissionResult(true)
            return
        }
        if (forcePrompt || shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else if (forcePrompt) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            // First launch: request once so GPS speed / weather can work.
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (appsPanel.visibility == View.VISIBLE) {
            showDashboard()
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    private fun logRuntimeInfo() {
        val runtime = Runtime.getRuntime()
        val maxMb = runtime.maxMemory() / (1024 * 1024)
        val totalMb = runtime.totalMemory() / (1024 * 1024)
        val freeMb = runtime.freeMemory() / (1024 * 1024)
        val abi = Build.SUPPORTED_ABIS.joinToString()
        Log.i(
            TAG,
            "Vezel Drive start | api=${Build.VERSION.SDK_INT} abi=$abi " +
                "package=$packageName memory(max/total/free MB)=$maxMb/$totalMb/$freeMb " +
                "flavor=${BuildConfig.FLAVOR} homeCapable=${BuildConfig.HOME_CAPABLE}"
        )
    }

    private inner class AppGridAdapter(
        private val apps: List<AppDiscovery.LauncherApp>
    ) : BaseAdapter() {
        override fun getCount(): Int = apps.size
        override fun getItem(position: Int): AppDiscovery.LauncherApp = apps[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = (convertView as? LinearLayout) ?: LinearLayout(this@MainActivity).apply {
                orientation = LinearLayout.VERTICAL
                gravity = android.view.Gravity.CENTER_HORIZONTAL
                setPadding(8, 12, 8, 12)
                setBackgroundColor(ContextCompat.getColor(context, R.color.vezel_tile))
                minimumHeight = (48 * resources.displayMetrics.density).toInt()
                val icon = ImageView(context).apply {
                    id = ICON_ID
                    layoutParams = LinearLayout.LayoutParams(96, 96)
                }
                val label = TextView(context).apply {
                    id = LABEL_ID
                    setTextColor(ContextCompat.getColor(context, R.color.vezel_text))
                    textSize = 12f
                    gravity = android.view.Gravity.CENTER
                    maxLines = 2
                }
                addView(icon)
                addView(label)
            }
            val app = apps[position]
            row.findViewById<ImageView>(ICON_ID).setImageDrawable(app.icon)
            row.findViewById<TextView>(LABEL_ID).text = app.label
            return row
        }
    }

    companion object {
        private const val TAG = "VezelLauncher"
        private const val ICON_ID = 0x5F1001
        private const val LABEL_ID = 0x5F1002
    }
}
