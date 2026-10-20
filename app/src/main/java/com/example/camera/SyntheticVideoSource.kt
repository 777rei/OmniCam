package com.example.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * OmniCam High-Performance Surveillance Scene Generator
 * Generates distinct real-time multi-channel CCTV camera feeds.
 */
class SyntheticVideoSource(
    private val width: Int = 640,
    private val height: Int = 480
) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private var globalFrameCount = 0L

    var isSimulatedMotionActive: Boolean = false

    /**
     * Generates a frame bitmap for surveillance streaming and motion testing.
     * Uses sceneIndex or channelId to render distinct physical surveillance zones.
     */
    fun generateFrame(
        channelName: String = "CAM-01 [FRONT]",
        sceneIndex: Int = 0
    ): Bitmap {
        globalFrameCount++
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        when (abs(sceneIndex) % 4) {
            0 -> drawCorridorScene(canvas, channelName)
            1 -> drawPerimeterDrivewayScene(canvas, channelName)
            2 -> drawServerRoomScene(canvas, channelName)
            else -> drawLoadingDockScene(canvas, channelName)
        }

        // Standard HUD overlay: corner brackets, center crosshair, timestamp
        drawCornerBrackets(canvas, width.toFloat(), height.toFloat())
        drawSurveillanceHud(canvas, channelName)

        return bitmap
    }

    private fun drawCorridorScene(canvas: Canvas, channelName: String) {
        // Deep obsidian navy slate
        canvas.drawColor(Color.rgb(14, 18, 28))

        // Floor perspective lines
        paint.color = Color.rgb(28, 38, 56)
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        val horizonY = height * 0.45f
        canvas.drawLine(0f, horizonY, width.toFloat(), horizonY, paint)

        for (i in -4..4) {
            val startX = (width / 2) + (i * 35f)
            val endX = (width / 2) + (i * 140f)
            canvas.drawLine(startX, horizonY, endX, height.toFloat(), paint)
        }
        for (row in 1..4) {
            val y = horizonY + (row * row * 18f)
            if (y < height) {
                canvas.drawLine(0f, y, width.toFloat(), y, paint)
            }
        }

        // Security Doorway
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(22, 30, 46)
        canvas.drawRect(width * 0.40f, height * 0.20f, width * 0.60f, horizonY + 20f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(0, 229, 255)
        paint.strokeWidth = 1.5f
        canvas.drawRect(width * 0.40f, height * 0.20f, width * 0.60f, horizonY + 20f, paint)

        // Moving target
        drawMovingTarget(canvas, horizonY, 0)
    }

    private fun drawPerimeterDrivewayScene(canvas: Canvas, channelName: String) {
        // Night sky and asphalt driveway
        canvas.drawColor(Color.rgb(10, 14, 22))

        val horizonY = height * 0.40f
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(20, 26, 38)
        canvas.drawRect(0f, horizonY, width.toFloat(), height.toFloat(), paint)

        // Driveway lane markers
        paint.color = Color.rgb(45, 58, 80)
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(width * 0.5f, horizonY, width * 0.1f, height.toFloat(), paint)
        canvas.drawLine(width * 0.5f, horizonY, width * 0.9f, height.toFloat(), paint)

        // Perimeter fence posts
        paint.color = Color.rgb(35, 48, 68)
        paint.strokeWidth = 2f
        for (x in 20..width step 60) {
            canvas.drawLine(x.toFloat(), horizonY - 40f, x.toFloat(), horizonY + 20f, paint)
        }
        canvas.drawLine(0f, horizonY - 30f, width.toFloat(), horizonY - 30f, paint)
        canvas.drawLine(0f, horizonY - 10f, width.toFloat(), horizonY - 10f, paint)

        // Lamp glow
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(40, 255, 215, 0)
        canvas.drawCircle(width * 0.2f, horizonY - 50f, 35f, paint)

        drawMovingTarget(canvas, horizonY, 1)
    }

    private fun drawServerRoomScene(canvas: Canvas, channelName: String) {
        // Dark server room background
        canvas.drawColor(Color.rgb(12, 15, 24))

        val horizonY = height * 0.50f
        // Server racks on left and right
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(20, 24, 36)
        canvas.drawRect(0f, 0f, width * 0.32f, height.toFloat(), paint)
        canvas.drawRect(width * 0.68f, 0f, width.toFloat(), height.toFloat(), paint)

        // Server rack units & blinking LED lights
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(35, 44, 64)
        paint.strokeWidth = 1.5f
        for (y in 40..height step 35) {
            canvas.drawLine(10f, y.toFloat(), width * 0.30f, y.toFloat(), paint)
            canvas.drawLine(width * 0.70f, y.toFloat(), width - 10f, y.toFloat(), paint)

            // Blinking green/cyan LEDs
            paint.style = Paint.Style.FILL
            val blink = ((globalFrameCount + y) % 30) < 15
            paint.color = if (blink) Color.rgb(0, 255, 180) else Color.rgb(0, 120, 90)
            canvas.drawCircle(width * 0.28f, y - 10f, 3f, paint)
            paint.color = if (!blink) Color.rgb(0, 229, 255) else Color.rgb(0, 90, 140)
            canvas.drawCircle(width * 0.72f, y - 10f, 3f, paint)
            paint.style = Paint.Style.STROKE
            paint.color = Color.rgb(35, 44, 64)
        }

        drawMovingTarget(canvas, horizonY, 2)
    }

    private fun drawLoadingDockScene(canvas: Canvas, channelName: String) {
        // Industrial warehouse loading bay
        canvas.drawColor(Color.rgb(16, 20, 28))

        val horizonY = height * 0.42f
        // Industrial roll-up door
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(26, 32, 45)
        canvas.drawRect(width * 0.25f, height * 0.15f, width * 0.75f, horizonY + 30f, paint)

        // Slat lines
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(42, 54, 76)
        paint.strokeWidth = 1.5f
        for (y in (height * 0.15f).toInt()..(horizonY + 30f).toInt() step 18) {
            canvas.drawLine(width * 0.25f, y.toFloat(), width * 0.75f, y.toFloat(), paint)
        }

        // Yellow caution stripes on loading dock edge
        paint.strokeWidth = 6f
        for (x in 0..width step 30) {
            paint.color = if ((x / 30) % 2 == 0) Color.rgb(234, 179, 8) else Color.rgb(20, 20, 20)
            canvas.drawLine(x.toFloat(), horizonY + 35f, (x + 20).toFloat(), horizonY + 55f, paint)
        }

        drawMovingTarget(canvas, horizonY, 3)
    }

    private fun drawMovingTarget(canvas: Canvas, horizonY: Float, offsetSeed: Int) {
        val timeSec = (globalFrameCount + (offsetSeed * 45)) * 0.08
        val targetX: Float
        val targetY: Float
        val targetRadius: Float

        if (isSimulatedMotionActive) {
            // Rapid moving object crossing zone
            targetX = width * 0.5f + (sin(timeSec * 3.2).toFloat() * (width * 0.32f))
            targetY = horizonY + (cos(timeSec * 1.8).toFloat() * 35f)
            targetRadius = 26f

            // Red detection highlight on moving intruder
            paint.style = Paint.Style.STROKE
            paint.color = Color.rgb(255, 61, 87)
            paint.strokeWidth = 2.5f
            val rect = RectF(targetX - targetRadius - 8f, targetY - targetRadius - 12f, targetX + targetRadius + 8f, targetY + targetRadius + 12f)
            canvas.drawRect(rect, paint)

            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(255, 61, 87)
            canvas.drawCircle(targetX, targetY, targetRadius, paint)

            textPaint.color = Color.rgb(255, 61, 87)
            textPaint.textSize = 13f
            textPaint.isFakeBoldText = true
            canvas.drawText("TARGET DETECTED [94%]", rect.left, rect.top - 6f, textPaint)
        } else {
            // Subtle ambient motion
            targetX = width * 0.5f + (sin(timeSec * 0.4).toFloat() * 14f)
            targetY = horizonY + 8f
            targetRadius = 12f

            paint.style = Paint.Style.FILL
            paint.color = Color.rgb(45, 60, 85)
            canvas.drawCircle(targetX, targetY, targetRadius, paint)
        }
    }

    private fun drawSurveillanceHud(canvas: Canvas, channelName: String) {
        // Crosshair at center
        paint.style = Paint.Style.STROKE
        paint.color = Color.argb(90, 0, 229, 255)
        paint.strokeWidth = 1.2f
        val cx = width / 2f
        val cy = height / 2f
        canvas.drawLine(cx - 24f, cy, cx + 24f, cy, paint)
        canvas.drawLine(cx, cy - 24f, cx, cy + 24f, paint)
        canvas.drawCircle(cx, cy, 16f, paint)

        // Subtle timestamp in bottom-left
        textPaint.color = Color.rgb(226, 232, 240)
        textPaint.textSize = 14f
        textPaint.isFakeBoldText = false
        val timeString = timeFormat.format(Date())
        canvas.drawText(timeString, 20f, height - 18f, textPaint)

        // Subtle REC indicator
        paint.style = Paint.Style.FILL
        paint.color = if ((globalFrameCount % 24) < 14) Color.rgb(239, 68, 68) else Color.TRANSPARENT
        canvas.drawCircle(width - 50f, 26f, 5f, paint)
        textPaint.color = Color.rgb(239, 68, 68)
        textPaint.textSize = 14f
        textPaint.isFakeBoldText = true
        canvas.drawText("REC", width - 40f, 31f, textPaint)
    }

    private fun drawCornerBrackets(canvas: Canvas, w: Float, h: Float) {
        paint.color = Color.argb(140, 0, 229, 255)
        paint.strokeWidth = 2f
        paint.style = Paint.Style.STROKE
        val m = 14f
        val len = 20f

        // Top-left
        canvas.drawLine(m, m, m + len, m, paint)
        canvas.drawLine(m, m, m, m + len, paint)

        // Top-right
        canvas.drawLine(w - m, m, w - m - len, m, paint)
        canvas.drawLine(w - m, m, w - m, m + len, paint)

        // Bottom-left
        canvas.drawLine(m, h - m, m + len, h - m, paint)
        canvas.drawLine(m, h - m, m, h - m - len, paint)

        // Bottom-right
        canvas.drawLine(w - m, h - m, w - m - len, h - m, paint)
        canvas.drawLine(w - m, h - m, w - m, h - m - len, paint)
    }
}
