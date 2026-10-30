package com.vezel.launcher.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import com.vezel.launcher.R
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Locale-aware clock/date that refreshes on TIME_TICK, timezone, and date changes.
 * Exposes 12-hour time + AM/PM, greeting, and a long date for the mockup header.
 */
class ClockProvider(
    private val context: Context,
    private val onTick: (ClockUi) -> Unit
) {

    data class ClockUi(
        val time12: String,
        val amPm: String,
        val dateLong: String,
        val greeting: String,
        val hour24: Int
    )

    private val timeFmt = SimpleDateFormat("h:mm", Locale.getDefault())
    private val dateFmt = SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())

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
        val cal = Calendar.getInstance().apply { time = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val amPm = if (cal.get(Calendar.AM_PM) == Calendar.AM) {
            context.getString(R.string.clock_am)
        } else {
            context.getString(R.string.clock_pm)
        }
        onTick(
            ClockUi(
                time12 = timeFmt.format(now),
                amPm = amPm,
                dateLong = dateFmt.format(now),
                greeting = greetingFor(hour),
                hour24 = hour
            )
        )
    }

    private fun greetingFor(hour: Int): String = when (hour) {
        in 5..11 -> context.getString(R.string.greeting_morning)
        in 12..16 -> context.getString(R.string.greeting_afternoon)
        in 17..20 -> context.getString(R.string.greeting_evening)
        else -> context.getString(R.string.greeting_night)
    }

    /** True when auto theme should prefer night (dark) surfaces. */
    fun isNightHour(hour24: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): Boolean =
        hour24 < 6 || hour24 >= 18
}
