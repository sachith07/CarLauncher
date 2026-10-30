package com.vezel.launcher

import android.content.Intent
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
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.vezel.launcher.system.AppDiscovery
import com.vezel.launcher.ui.DashboardView
import com.vezel.launcher.ui.SettingsActivity

class MainActivity : AppCompatActivity(), DashboardView.Callbacks {

    private lateinit var dashboardView: DashboardView
    private lateinit var appsPanel: View
    private lateinit var appsGrid: GridView
    private lateinit var discovery: AppDiscovery

    private val clockTicker = object : Runnable {
        override fun run() {
            if (::dashboardView.isInitialized) {
                dashboardView.refreshClock()
                dashboardView.postDelayed(this, 30_000L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        discovery = AppDiscovery(this)
        dashboardView = findViewById(R.id.dashboardView)
        appsPanel = findViewById(R.id.appsPanel)
        appsGrid = findViewById(R.id.appsGrid)
        dashboardView.callbacks = this

        findViewById<Button>(R.id.btnBackDashboard).setOnClickListener {
            showDashboard()
        }

        logRuntimeInfo()
    }

    override fun onResume() {
        super.onResume()
        dashboardView.refreshClock()
        dashboardView.refreshVehicleSlot()
        dashboardView.refreshSpeed()
        dashboardView.removeCallbacks(clockTicker)
        dashboardView.post(clockTicker)
    }

    override fun onPause() {
        dashboardView.removeCallbacks(clockTicker)
        super.onPause()
    }

    override fun onOpenSettings() {
        startActivity(Intent(this, SettingsActivity::class.java))
    }

    override fun onOpenApps() {
        val apps = discovery.discoverLauncherApps()
        appsGrid.adapter = AppGridAdapter(apps)
        appsGrid.setOnItemClickListener { _, _, position, _ ->
            val app = apps[position]
            if (!discovery.openLauncherApp(app)) {
                Toast.makeText(this, R.string.open_failed, Toast.LENGTH_SHORT).show()
            }
        }
        appsPanel.visibility = View.VISIBLE
        dashboardView.visibility = View.GONE
    }

    private fun showDashboard() {
        appsPanel.visibility = View.GONE
        dashboardView.visibility = View.VISIBLE
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
                "package=$packageName memory(max/total/free MB)=$maxMb/$totalMb/$freeMb"
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
