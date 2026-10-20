package com.example.engine.motion

import android.graphics.Bitmap
import android.graphics.RectF
import com.example.data.model.DetectionZone
import kotlin.math.abs

data class MotionResult(
    val isMotionDetected: Boolean,
    val motionScore: Float, // 0.0 to 1.0
    val activeZoneIndices: List<Int> = emptyList(),
    val boundingBox: RectF? = null,
    val timestamp: Long = System.currentTimeMillis()
)

class MotionDetector {
    private var previousGrid: IntArray? = null
    private val gridCols = 32
    private val gridRows = 24

    /**
     * Analyzes a downscaled or standard bitmap frame for motion differencing.
     * @param bitmap Frame bitmap
     * @param sensitivity Float from 0.05 (very sensitive) to 0.70 (requires major motion)
     * @param zone Active/inactive zone matrix
     */
    fun analyzeFrame(
        bitmap: Bitmap,
        sensitivity: Float = 0.25f,
        zone: DetectionZone = DetectionZone()
    ): MotionResult {
        val scaled = runCatching {
            Bitmap.createScaledBitmap(bitmap, gridCols, gridRows, false)
        }.getOrNull() ?: return MotionResult(isMotionDetected = false, motionScore = 0f)
        val pixels = IntArray(gridCols * gridRows)
        scaled.getPixels(pixels, 0, gridCols, 0, 0, gridCols, gridRows)

        val currentLuma = IntArray(pixels.size)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            // Standard perceptual luminance formula
            currentLuma[i] = (r * 299 + g * 587 + b * 114) / 1000
        }

        val prev = previousGrid
        previousGrid = currentLuma

        if (prev == null) {
            return MotionResult(isMotionDetected = false, motionScore = 0f)
        }

        var totalMonitoredPixels = 0
        var changedPixels = 0
        val lumaDeltaThreshold = 28 // Luminance diff needed to count pixel as changed
        val triggeredZones = mutableSetOf<Int>()

        var minX = gridCols
        var maxX = 0
        var minY = gridRows
        var maxY = 0

        for (y in 0 until gridRows) {
            val zoneRow = (y * zone.rows) / gridRows
            for (x in 0 until gridCols) {
                val zoneCol = (x * zone.cols) / gridCols
                val zoneIndex = zoneRow * zone.cols + zoneCol

                // Check if user has armed this zone
                if (!zone.isCellActive(zoneRow, zoneCol)) {
                    continue
                }

                totalMonitoredPixels++
                val idx = y * gridCols + x
                val diff = abs(currentLuma[idx] - prev[idx])

                if (diff > lumaDeltaThreshold) {
                    changedPixels++
                    triggeredZones.add(zoneIndex)
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        if (totalMonitoredPixels == 0) {
            return MotionResult(isMotionDetected = false, motionScore = 0f)
        }

        val rawScore = changedPixels.toFloat() / totalMonitoredPixels.toFloat()
        // Map sensitivity (slider 0.05 to 0.50) to required change threshold
        // At sensitivity 0.25, motion triggers when ~2% of monitored pixels change
        val triggerThreshold = (1.0f - sensitivity).coerceIn(0.01f, 0.99f) * 0.08f
        val isTriggered = rawScore >= triggerThreshold

        val boundingBox = if (isTriggered && maxX >= minX && maxY >= minY) {
            RectF(
                minX.toFloat() / gridCols,
                minY.toFloat() / gridRows,
                (maxX + 1).toFloat() / gridCols,
                (maxY + 1).toFloat() / gridRows
            )
        } else null

        return MotionResult(
            isMotionDetected = isTriggered,
            motionScore = (rawScore * 5f).coerceIn(0f, 1f),
            activeZoneIndices = triggeredZones.toList(),
            boundingBox = boundingBox
        )
    }

    fun reset() {
        previousGrid = null
    }
}
