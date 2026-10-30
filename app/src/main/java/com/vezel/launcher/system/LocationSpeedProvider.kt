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
 * GPS speed in km/h from Location.speed (m/s × 3.6), plus session trip/drive/heading.
 * No valid / fresh fix → null speed (UI shows "--" / unavailable). Distinguishes 0 km/h from missing GPS.
 */
class LocationSpeedProvider(
    private val context: Context,
    private val onChanged: (Telemetry) -> Unit = {}
) : LocationListener {

    data class Telemetry(
        val speedKmh: Float?,
        val tripKm: Float,
        val driveMinutes: Int,
        val heading: String?
    )

    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private val mainHandler = Handler(Looper.getMainLooper())

    @Volatile
    private var lastSpeedKmh: Float? = null

    @Volatile
    private var lastFixElapsedMs: Long = 0L

    @Volatile
    private var lastHeading: String? = null

    private var lastLocation: Location? = null
    private var tripMeters: Float = 0f
    private var sessionStartElapsedMs: Long = 0L
    private var started = false

    private val staleChecker = object : Runnable {
        override fun run() {
            if (!started) return
            if (lastFixElapsedMs > 0L &&
                SystemClock.elapsedRealtime() - lastFixElapsedMs > STALE_TIMEOUT_MS
            ) {
                lastSpeedKmh = null
                publish()
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

    fun current(): Telemetry = buildTelemetry()

    @SuppressLint("MissingPermission")
    fun start() {
        if (started) return
        started = true
        if (sessionStartElapsedMs == 0L) {
            sessionStartElapsedMs = SystemClock.elapsedRealtime()
        }
        if (!hasLocationPermission()) {
            lastSpeedKmh = null
            publish()
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
            publish()
        } catch (_: Exception) {
            lastSpeedKmh = null
            publish()
        }
        mainHandler.removeCallbacks(staleChecker)
        mainHandler.post(staleChecker)
        publish()
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
            publish()
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
        val previous = lastLocation
        if (!fromCache && previous != null) {
            val delta = previous.distanceTo(location)
            if (delta in 0.5f..500f) {
                tripMeters += delta
            }
        }
        if (!fromCache) {
            lastLocation = location
        } else if (lastLocation == null) {
            lastLocation = location
        }

        if (location.hasBearing()) {
            lastHeading = bearingToCompass(location.bearing)
        }

        if (!location.hasSpeed()) {
            if (!fromCache && lastSpeedKmh == null) publish()
            return
        }
        val ageMs = (SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos) / 1_000_000L
        if (fromCache && (ageMs < 0L || ageMs > STALE_TIMEOUT_MS)) {
            return
        }
        lastFixElapsedMs = SystemClock.elapsedRealtime()
        lastSpeedKmh = (location.speed * 3.6f).coerceAtLeast(0f)
        publish()
    }

    private fun publish() {
        onChanged(buildTelemetry())
    }

    private fun buildTelemetry(): Telemetry {
        val elapsed = if (sessionStartElapsedMs > 0L) {
            SystemClock.elapsedRealtime() - sessionStartElapsedMs
        } else {
            0L
        }
        return Telemetry(
            speedKmh = lastSpeedKmh,
            tripKm = tripMeters / 1000f,
            driveMinutes = (elapsed / 60_000L).toInt().coerceAtLeast(0),
            heading = lastHeading
        )
    }

    private fun bearingToCompass(bearing: Float): String {
        val dirs = arrayOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
        val normalized = ((bearing % 360f) + 360f) % 360f
        val index = ((normalized + 22.5f) / 45f).toInt() % 8
        return dirs[index]
    }

    companion object {
        private const val MIN_INTERVAL_MS = 1_000L
        private const val MIN_DISTANCE_M = 0f
        private const val STALE_TIMEOUT_MS = 8_000L
        private const val STALE_CHECK_MS = 2_000L
    }
}
