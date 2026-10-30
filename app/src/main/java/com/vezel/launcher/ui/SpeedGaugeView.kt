package com.vezel.launcher.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat
import com.vezel.launcher.R

/**
 * Speed gauge. Displays "--" when GPS unavailable; "0" when speed is valid zero.
 */
class SpeedGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var speedText: String = context.getString(R.string.speed_placeholder)
    private var statusText: String = context.getString(R.string.speed_unavailable)

    private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        color = ContextCompat.getColor(context, R.color.vezel_gauge_ring)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.vezel_text)
        textAlign = Paint.Align.CENTER
        textSize = 42f
        isFakeBoldText = true
    }

    private val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.vezel_text_muted)
        textAlign = Paint.Align.CENTER
        textSize = 16f
    }

    private val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ContextCompat.getColor(context, R.color.vezel_text_muted)
        textAlign = Paint.Align.CENTER
        textSize = 12f
    }

    private val bounds = RectF()

    /**
     * @param speed km/h when a valid fix exists; null when GPS unavailable / stale.
     */
    fun setSpeedKmh(speed: Float?) {
        if (speed == null) {
            speedText = context.getString(R.string.speed_placeholder)
            statusText = context.getString(R.string.speed_unavailable)
        } else {
            speedText = speed.toInt().toString()
            statusText = context.getString(R.string.speed_unit)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val pad = ringPaint.strokeWidth
        bounds.set(pad, pad, width - pad, height - pad)
        canvas.drawArc(bounds, 135f, 270f, false, ringPaint)

        val cx = width / 2f
        val cy = height / 2f
        canvas.drawText(speedText, cx, cy + 4f, textPaint)
        canvas.drawText(statusText, cx, cy + 32f, if (speedText == "--") statusPaint else unitPaint)
    }
}
