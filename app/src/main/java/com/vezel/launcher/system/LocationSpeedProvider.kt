package com.vezel.launcher.system

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat

/**
 * GPS speed in km/h from Location.speed (m/s × 3.6).
 * No valid / fresh fix → null (UI shows "--" / unavailable). Distinguishes 0 km/h from missing GPS.
 */
class LocationSpeedProvider(
    private val context: Context,
    private val onSpeedChanged: (Float?) -> Unit = {}
) : LocationListener {

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var lastSpeedKmh: Float? = null

    @Volatile
    private var lastFixElapsedMs: Long = 0L

    private var started = false

    private val staleChecker = object : Runnable {
        override fun run() {
            if (!started) return
            if (lastFixElapsedMs > 0L &&
                SystemClock.elapsedRealtime() - lastFixElapsedMs > STALE_TIMEOUT_MS
            ) {
                lastSpeedKmh = null
                onSpeedChanged(null)
            }
            mainHandler.postDelayed(this, STALE_CHECK_MS)
        }
    }

    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun currentSpeedKmh(): Float? = lastSpeedKmh

    @SuppressLint("MissingPermission")
    fun start() {
        if (started) return
        started = true
        if (!hasLocationPermission()) {
            lastSpeedKmh = null
            onSpeedChanged(null)
            return
        }
        try {
            val providers = listOf(
                LocationManager.GPS_PROVIDER,
                LocationManager.NETWORK_PROVIDER
            )
            for (provider in providers) {
                if (!locationManager.isProviderEnabled(provider)) continue
                locationManager.requestLocationUpdates(
                    provider,
                    MIN_INTERVAL_MS,
                    MIN_DISTANCE_M,
                    this,
                    Looper.getMainLooper()
                )
                locationManager.getLastKnownLocation(provider)?.let { consume(it, fromCache = true) }
            }
        } catch (_: SecurityException) {
            lastSpeedKmh = null
            onSpeedChanged(null)
        } catch (_: Exception) {
            lastSpeedKmh = null
            onSpeedChanged(null)
        }
        mainHandler.removeCallbacks(staleChecker)
        mainHandler.post(staleChecker)
    }

    fun stop() {
        started = false
        mainHandler.removeCallbacks(staleChecker)
        try {
            locationManager.removeUpdates(this)
        } catch (_: Exception) {
            // ignore
        }
    }

    override fun onLocationChanged(location: Location) {
        consume(location, fromCache = false)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    override fun onProviderEnabled(provider: String) = Unit

    override fun onProviderDisabled(provider: String) {
        if (!hasAnyEnabledProvider()) {
            lastSpeedKmh = null
            onSpeedChanged(null)
        }
    }

    private fun hasAnyEnabledProvider(): Boolean {
        return try {
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        } catch (_: Exception) {
            false
        }
    }

    private fun consume(location: Location, fromCache: Boolean) {
        if (!location.hasSpeed()) {
            if (!fromCache) {
                // Fresh fix without speed still counts as presence; keep prior or treat as 0 only
                // when we have a moving-capable fix — prefer unavailable over inventing speed.
                if (lastSpeedKmh == null) onSpeedChanged(null)
            }
            return
        }
        val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
        if (fromCache && (ageMs < 0L || ageMs > STALE_TIMEOUT_MS)) {
            return
        }
        lastFixElapsedMs = SystemClock.elapsedRealtime()
        val kmh = (location.speed * 3.6f).coerceAtLeast(0f)
        lastSpeedKmh = kmh
        onSpeedChanged(kmh)
    }

    companion object {
        private const val MIN_INTERVAL_MS = 1_000L
        private const val MIN_DISTANCE_M = 0f
        private const val STALE_TIMEOUT_MS = 8_000L
        private const val STALE_CHECK_MS = 2_000L
    }
}
