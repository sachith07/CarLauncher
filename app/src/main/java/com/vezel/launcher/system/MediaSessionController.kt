package com.vezel.launcher.system

/**
 * Stub MediaSession controller. Binding is out of scope for Sprint 0–1.
 */
class MediaSessionController {

    data class NowPlaying(
        val title: String? = null,
        val artist: String? = null,
        val isPlaying: Boolean = false
    )

    fun current(): NowPlaying = NowPlaying()

    fun connect() {
        // no-op stub
    }

    fun disconnect() {
        // no-op stub
    }
}
