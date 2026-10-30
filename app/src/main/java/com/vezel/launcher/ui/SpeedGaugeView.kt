package com.vezel.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.vezel.launcher.R
import kotlin.math.min

/**
 * Semi-circular 0–180 km/h arc with orange block segments.
 * Displays "--" when GPS unavailable; "0" when speed is valid zero.
 */
class SpeedGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var speedKmh: Float? = null
    private var speedText: String = context.getString(R.string.speed_placeholder)
    private var statusText: String = context.getString(R.string.speed_unavailable)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        color = ContextCompat.getColor(context, R.color.vezel_gauge_track)
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        color = ContextCompat.getColor(context, R.color.vezel_gauge_ring)
    }

    private val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.BUTT
        color = ContextCompat.getColor(context, R.color.vezel_gauge_track)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.vezel_text)
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.vezel_text_muted)
        textAlign = Paint.Align.CENTER
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.vezel_text_muted)
        textAlign = Paint.Align.CENTER
    }

    private val bounds = RectF()

    /**
     * @param speed km/h when a valid fix exists; null when GPS unavailable / stale.
     */
    fun setSpeedKmh(speed: Float?) {
        speedKmh = speed
        if (speed == null) {
            speedText = context.getString(R.string.speed_placeholder)
            statusText = context.getString(R.string.speed_unavailable)
        } else {
            speedText = speed.toInt().coerceIn(0, MAX_SPEED.toInt()).toString()
            statusText = context.getString(R.string.speed_unit)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        val stroke = size * 0.085f
        trackPaint.strokeWidth = stroke
        fillPaint.strokeWidth = stroke
        tickPaint.strokeWidth = stroke * 0.35f

        val pad = stroke
        val left = (width - size) / 2f + pad
        val top = height * 0.08f + pad * 0.2f
        bounds.set(left, top, left + size - pad * 2f, top + size - pad * 2f)

        // Background arc 0→180 (180° sweep starting at 180° = left)
        canvas.drawArc(bounds, START_ANGLE, SWEEP_ANGLE, false, trackPaint)

        // Segment gaps along the track for a "block" look
        val segments = 36
        val gapSweep = 1.2f
        val segSweep = (SWEEP_ANGLE / segments) - gapSweep
        for (i in 0 until segments) {
            val start = START_ANGLE + i * (SWEEP_ANGLE / segments) + gapSweep / 2f
            canvas.drawArc(bounds, start, segSweep, false, tickPaint)
        }

        val value = speedKmh
        if (value != null) {
            val fraction = (value / MAX_SPEED).coerceIn(0f, 1f)
            val filledSegments = (fraction * segments).toInt().coerceAtLeast(if (value > 0f) 1 else 0)
            for (i in 0 until filledSegments) {
                val start = START_ANGLE + i * (SWEEP_ANGLE / segments) + gapSweep / 2f
                canvas.drawArc(bounds, start, segSweep, false, fillPaint)
            }
        }

        // End labels 0 / 180
        labelPaint.textSize = size * 0.055f
        val labelY = bounds.centerY() + bounds.height() * 0.42f
        canvas.drawText("0", bounds.left + stroke, labelY, labelPaint)
        canvas.drawText("180", bounds.right - stroke, labelY, labelPaint)

        val cx = bounds.centerX()
        val cy = bounds.centerY() + bounds.height() * 0.08f
        textPaint.textSize = size * 0.22f
        unitPaint.textSize = size * 0.06f
        canvas.drawText(speedText, cx, cy, textPaint)
        canvas.drawText(statusText, cx, cy + size * 0.09f, unitPaint)
    }

    companion object {
        private const val MAX_SPEED = 180f
        private const val START_ANGLE = 180f
        private const val SWEEP_ANGLE = 180f
    }
}
