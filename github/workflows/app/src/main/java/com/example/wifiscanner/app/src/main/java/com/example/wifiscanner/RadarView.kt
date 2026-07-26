package com.example.wifiscanner

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

data class RadarPoint(val label: String, val distanceMeters: Float)

/**
 * Simple radar-style view.
 * NOTE: We only know estimated DISTANCE from signal strength, not direction.
 * So each point is placed at a random angle around the center at the
 * correct radial distance. This is clearly labeled as an approximation.
 */
class RadarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private var points: List<RadarPoint> = emptyList()
    private val angleSeed = HashMap<String, Double>()

    private val circlePaint = Paint().apply {
        color = Color.parseColor("#DDDDDD")
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val centerPaint = Paint().apply {
        color = Color.parseColor("#2196F3")
        style = Paint.Style.FILL
    }

    private val dotPaint = Paint().apply {
        color = Color.parseColor("#E91E63")
        style = Paint.Style.FILL
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 26f
        isAntiAlias = true
    }

    fun setPoints(newPoints: List<RadarPoint>) {
        points = newPoints
        // keep stable random angle per label so it doesn't jump on redraw
        for (p in newPoints) {
            if (!angleSeed.containsKey(p.label)) {
                angleSeed[p.label] = Random.nextDouble(0.0, 2 * Math.PI)
            }
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width / 2f
        val cy = height / 2f
        val maxRadius = min(width, height) / 2f - 40f

        // max distance to scale rings (at least 10m so close points aren't cramped)
        val maxDist = (points.maxOfOrNull { it.distanceMeters } ?: 10f).coerceAtLeast(10f)

        // draw range rings (4 rings)
        for (i in 1..4) {
            val r = maxRadius * i / 4f
            canvas.drawCircle(cx, cy, r, circlePaint)
            canvas.drawText(
                "${"%.0f".format(maxDist * i / 4f)}m",
                cx + r - 20f, cy - 6f, textPaint
            )
        }

        // draw center (you / your phone)
        canvas.drawCircle(cx, cy, 16f, centerPaint)
        canvas.drawText("You", cx + 20f, cy + 8f, textPaint)

        // draw each network at its estimated distance
        for (p in points) {
            val angle = angleSeed[p.label] ?: 0.0
            val r = (p.distanceMeters / maxDist) * maxRadius
            val x = cx + (r * cos(angle)).toFloat()
            val y = cy + (r * sin(angle)).toFloat()
            canvas.drawCircle(x, y, 12f, dotPaint)
            canvas.drawText(p.label, x + 16f, y, textPaint)
        }
    }
}
