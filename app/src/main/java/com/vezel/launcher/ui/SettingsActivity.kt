package com.vezel.launcher.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.vezel.launcher.R
import com.vezel.launcher.data.LauncherPreferences

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val prefs = LauncherPreferences(this)
        findViewById<TextView>(R.id.colorValue).text = prefs.vehicleColorKey
        findViewById<TextView>(R.id.themeValue).text = prefs.themeKey
        findViewById<Button>(R.id.btnBack).setOnClickListener { finish() }
    }
}
