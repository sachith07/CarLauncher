package com.vezel.launcher.system

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import com.vezel.launcher.data.LauncherPreferences
import com.vezel.launcher.service.MediaNotificationListener

/**
 * Attempts MediaSessionManager binding when a notification listener is enabled.
 * Otherwise exposes a placeholder and deep-links a preferred media package.
 */
class MediaSessionController(
    private val context: Context,
    private val onChanged: (NowPlaying) -> Unit = {}
) {

    data class NowPlaying(
        val title: String? = null,
        val artist: String? = null,
        val isPlaying: Boolean = false,
        val packageName: String? = null,
        val available: Boolean = false,
        val positionMs: Long = 0L,
        val durationMs: Long = 0L,
        val art: Bitmap? = null
    )

    private val prefs = LauncherPreferences(context)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var sessionManager: MediaSessionManager? = null
    private var activeController: MediaController? = null
    private var connected = false

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener { sessions ->
        bindBestSession(sessions)
    }

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            publishFrom(activeController)
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            publishFrom(activeController)
        }
    }

    private val progressTicker = object : Runnable {
        override fun run() {
            if (!connected) return
            if (lastPublished.isPlaying) {
                publishFrom(activeController)
            }
            mainHandler.postDelayed(this, 1000L)
        }
    }

    fun current(): NowPlaying = lastPublished

    @Volatile
    private var lastPublished: NowPlaying = NowPlaying()

    fun isNotificationAccessEnabled(): Boolean {
        val cn = ComponentName(context, MediaNotificationListener::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.split(':').any {
            ComponentName.unflattenFromString(it)?.flattenToString() == cn.flattenToString() ||
                it.contains(context.packageName)
        }
    }

    fun connect() {
        if (connected) {
            refresh()
            return
        }
        connected = true
        mainHandler.removeCallbacks(progressTicker)
        mainHandler.post(progressTicker)
        if (!isNotificationAccessEnabled()) {
            publish(NowPlaying(available = false))
            return
        }
        try {
            sessionManager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            val cn = ComponentName(context, MediaNotificationListener::class.java)
            sessionManager?.addOnActiveSessionsChangedListener(
                sessionListener,
                cn,
                mainHandler
            )
            bindBestSession(sessionManager?.getActiveSessions(cn))
        } catch (e: SecurityException) {
            Log.w(TAG, "Media session access denied: ${e.message}")
            publish(NowPlaying(available = false))
        } catch (e: Exception) {
            Log.w(TAG, "Media session connect failed: ${e.message}")
            publish(NowPlaying(available = false))
        }
    }

    fun disconnect() {
        connected = false
        mainHandler.removeCallbacks(progressTicker)
        try {
            activeController?.unregisterCallback(controllerCallback)
        } catch (_: Exception) {
        }
        activeController = null
        try {
            sessionManager?.removeOnActiveSessionsChangedListener(sessionListener)
        } catch (_: Exception) {
        }
        sessionManager = null
    }

    fun refresh() {
        if (!isNotificationAccessEnabled()) {
            publish(NowPlaying(available = false))
            return
        }
        try {
            val cn = ComponentName(context, MediaNotificationListener::class.java)
            bindBestSession(sessionManager?.getActiveSessions(cn))
        } catch (_: Exception) {
            publish(NowPlaying(available = false))
        }
    }

    fun togglePlayPause() {
        val controls = activeController?.transportControls ?: return
        if (lastPublished.isPlaying) controls.pause() else controls.play()
    }

    fun skipToNext() {
        activeController?.transportControls?.skipToNext()
    }

    fun skipToPrevious() {
        activeController?.transportControls?.skipToPrevious()
    }

    /** Opens the preferred media package, or a common music player fallback. */
    fun openPlayerOrFallback(discovery: AppDiscovery): Boolean {
        val preferred = prefs.mediaPackage
        if (preferred.isNotBlank() && discovery.openPackage(preferred)) return true
        val activePkg = lastPublished.packageName
        if (!activePkg.isNullOrBlank() && discovery.openPackage(activePkg)) return true
        for (pkg in MEDIA_FALLBACKS) {
            if (discovery.openPackage(pkg)) return true
        }
        return false
    }

    fun openNotificationListenerSettings() {
        try {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            try {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            } catch (_: Exception) {
            }
        }
    }

    private fun bindBestSession(sessions: List<MediaController>?) {
        val best = sessions?.firstOrNull { controller ->
            val state = controller.playbackState?.state
            state == PlaybackState.STATE_PLAYING || state == PlaybackState.STATE_BUFFERING
        } ?: sessions?.firstOrNull()

        if (activeController !== best) {
            try {
                activeController?.unregisterCallback(controllerCallback)
            } catch (_: Exception) {
            }
            activeController = best
            try {
                best?.registerCallback(controllerCallback, mainHandler)
            } catch (_: Exception) {
            }
        }
        publishFrom(best)
    }

    private fun publishFrom(controller: MediaController?) {
        if (controller == null) {
            publish(NowPlaying(available = false))
            return
        }
        val meta = controller.metadata
        val state = controller.playbackState
        val title = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: meta?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        val playing = state?.state == PlaybackState.STATE_PLAYING ||
            state?.state == PlaybackState.STATE_BUFFERING
        val duration = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 0L
        val position = state?.position?.coerceAtLeast(0L) ?: 0L
        val art = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
        publish(
            NowPlaying(
                title = title,
                artist = artist,
                isPlaying = playing,
                packageName = controller.packageName,
                available = !title.isNullOrBlank() || playing,
                positionMs = position,
                durationMs = duration,
                art = art
            )
        )
    }

    private fun publish(now: NowPlaying) {
        lastPublished = now
        mainHandler.post { onChanged(now) }
    }

    companion object {
        private const val TAG = "VezelLauncher"
        val MEDIA_FALLBACKS = listOf(
            "com.google.android.apps.youtube.music",
            "com.spotify.music",
            "com.android.music",
            "com.google.android.music"
        )
    }
}
