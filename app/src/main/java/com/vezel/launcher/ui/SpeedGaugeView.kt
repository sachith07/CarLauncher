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
 * Placeholder speed gauge. Displays "--" until LocationSpeedProvider is wired.
 */
class SpeedGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var speedText: String = context.getString(R.string.speed_placeholder)

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

    private val bounds = RectF()

    fun setSpeedKmh(speed: Float?) {
        speedText = if (speed == null) {
            context.getString(R.string.speed_placeholder)
        } else {
            speed.toInt().toString()
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
        canvas.drawText(speedText, cx, cy + 8f, textPaint)
        canvas.drawText(context.getString(R.string.speed_unit), cx, cy + 36f, unitPaint)
    }
}
