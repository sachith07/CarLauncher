package com.vezel.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Locale-aware clock/date that refreshes on TIME_TICK, timezone, and date changes.
 */
class ClockProvider(
    private val context: Context,
    private val onTick: (time: String, date: String) -> Unit
) {

    private val timeFmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("EEE d MMM", Locale.getDefault())

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            publish()
        }
    }

    private var registered = false

    fun start() {
        if (registered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
            addAction(Intent.ACTION_DATE_CHANGED)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        registered = true
        publish()
    }

    fun stop() {
        if (!registered) return
        try {
            context.unregisterReceiver(receiver)
        } catch (_: Exception) {
            // already unregistered
        }
        registered = false
    }

    fun publish() {
        val now = Date()
        onTick(timeFmt.format(now), dateFmt.format(now))
    }
}
