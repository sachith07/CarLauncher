package com.vezel.launcher.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

/**
 * Enables MediaSessionManager.getActiveSessions for this package when the user
 * grants notification listener access. No notification content is persisted.
 */
class MediaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        Log.i(TAG, "Media notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = Unit

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = Unit

    companion object {
        private const val TAG = "VezelLauncher"
    }
}
