package com.reposo.infinix

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import kotlin.math.min

/** Anillo de bateria con el porcentaje en el centro. */
class BatteryRingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var level = 0
    private var charging = false

    private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x33FFFFFF
    }
    private val arc = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    }
    private val rect = RectF()

    fun set(level: Int, charging: Boolean) {
        this.level = level.coerceIn(0, 100)
        this.charging = charging
        contentDescription = "Bateria $level por ciento"
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val size = min(width, height).toFloat()
        if (size <= 0f) return
        val cx = width / 2f
        val cy = height / 2f
        val stroke = size * 0.09f
        track.strokeWidth = stroke
        arc.strokeWidth = stroke
        val r = size / 2f - stroke / 2f
        rect.set(cx - r, cy - r, cx + r, cy + r)

        arc.color = if (level <= 20 && !charging) RED else GREEN
        canvas.drawOval(rect, track)
        canvas.drawArc(rect, -90f, 360f * level / 100f, false, arc)

        text.color = arc.color
        text.textSize = size * 0.32f
        val y = cy - (text.descent() + text.ascent()) / 2f
        canvas.drawText(level.toString(), cx, y, text)
    }

    companion object {
        private val GREEN = 0xFF30D158.toInt()
        private val RED = 0xFFFF453A.toInt()
    }
}
