package com.vezel.launcher.system

/**
 * Stub GPS / speed provider. Live binding is out of scope for Sprint 0–1.
 */
class LocationSpeedProvider {

    /** Always null until GPS sprint; UI shows "--". */
    fun currentSpeedKmh(): Float? = null

    fun start() {
        // no-op stub
    }

    fun stop() {
        // no-op stub
    }
}
