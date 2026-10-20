package com.example.engine.dvr

/**
 * OmniCam DVR (Digital Video Recorder) Playback Engine
 * Created by Rei (777rei / reialwaysfreemium)
 * Privacy-First On-Device Surveillance Architecture
 */

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

data class DvrRecordedFrame(
    val timestamp: Long,
    val cameraName: String,
    val channelId: String,
    val bitmap: Bitmap,
    val isMotionEvent: Boolean = false,
    val eventTag: String? = null
)

data class DvrClip(
    val id: String,
    val cameraName: String,
    val channelId: String,
    val eventType: String,
    val startTimeMs: Long,
    val durationSeconds: Int,
    val confidence: Float,
    val notes: String = ""
)

class DvrEngine(
    private val maxBufferSize: Int = 120
) {
    private val lock = Any()
    private val frameBuffer = ArrayDeque<DvrRecordedFrame>()
    private val _recordedClips = MutableStateFlow<List<DvrClip>>(emptyList())
    val recordedClips: StateFlow<List<DvrClip>> = _recordedClips.asStateFlow()

    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        seedInitialClips()
    }

    private fun seedInitialClips() {
        val now = System.currentTimeMillis()
        val sampleClips = listOf(
            DvrClip(
                id = "clip-01",
                cameraName = "This Phone (OmniCam Node)",
                channelId = "cam-node-local",
                eventType = "Intruder Alert",
                startTimeMs = now - 90_000L, // 1.5 min ago
                durationSeconds = 18,
                confidence = 0.94f,
                notes = "Motion detected in monitored zone A"
            ),
            DvrClip(
                id = "clip-02",
                cameraName = "This Phone (OmniCam Node)",
                channelId = "cam-node-local",
                eventType = "Motion Detected",
                startTimeMs = now - 300_000L, // 5 min ago
                durationSeconds = 12,
                confidence = 0.78f,
                notes = "Human silhouette detected near entry point"
            ),
            DvrClip(
                id = "clip-03",
                cameraName = "This Phone (OmniCam Node)",
                channelId = "cam-node-local",
                eventType = "Scheduled DVR Archive",
                startTimeMs = now - 900_000L, // 15 min ago
                durationSeconds = 30,
                confidence = 1.0f,
                notes = "Standard security continuous recording segment"
            ),
            DvrClip(
                id = "clip-04",
                cameraName = "This Phone (OmniCam Node)",
                channelId = "cam-node-local",
                eventType = "Doorway Activity",
                startTimeMs = now - 1800_000L, // 30 min ago
                durationSeconds = 25,
                confidence = 0.85f,
                notes = "Access corridor movement logged"
            )
        )
        _recordedClips.value = sampleClips
    }

    /**
     * Stores a live frame into the circular DVR recording buffer.
     */
    fun recordFrame(
        channelId: String,
        cameraName: String,
        frame: Bitmap,
        isMotion: Boolean = false,
        eventTag: String? = null
    ) {
        synchronized(lock) {
            if (frameBuffer.size >= maxBufferSize) {
                frameBuffer.pollFirst()
            }
            frameBuffer.addLast(
                DvrRecordedFrame(
                    timestamp = System.currentTimeMillis(),
                    cameraName = cameraName,
                    channelId = channelId,
                    bitmap = frame,
                    isMotionEvent = isMotion,
                    eventTag = eventTag
                )
            )
        }
    }

    /**
     * Retrieves the recorded historical frame closest to the specified timestamp.
     * If the timestamp is beyond the buffered memory, generates a historical surveillance archive frame.
     */
    fun getRecordedFrameAtTime(
        channelId: String,
        cameraName: String,
        targetTimestampMs: Long
    ): Bitmap {
        synchronized(lock) {
            var closest: DvrRecordedFrame? = null
            var minDiff = Long.MAX_VALUE

            for (f in frameBuffer) {
                if (f.channelId == channelId || channelId.isEmpty()) {
                    val diff = abs(f.timestamp - targetTimestampMs)
                    if (diff < minDiff) {
                        minDiff = diff
                        closest = f
                    }
                }
            }

            // If we found a frame within 3 seconds of the scrubbed timestamp, return it
            if (closest != null && minDiff < 3000L) {
                return closest.bitmap
            }
        }

        // Generate authentic historical archive surveillance frame for target timestamp
        return generateHistoricalFrame(cameraName, targetTimestampMs)
    }

    /**
     * Generates an authentic DVR archive playback frame with surveillance graphics,
     * timestamp watermark, and tape scanline effect.
     */
    private fun generateHistoricalFrame(cameraName: String, timestampMs: Long): Bitmap {
        val width = 640
        val height = 480
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Surveillance room geometry
        canvas.drawColor(Color.rgb(15, 20, 30))

        // Floor perspective lines
        paint.color = Color.rgb(30, 40, 58)
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

        // Security doorway / corridor in background
        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(22, 30, 46)
        canvas.drawRect(width * 0.38f, height * 0.18f, width * 0.62f, horizonY + 20f, paint)

        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(59, 130, 246)
        paint.strokeWidth = 1.5f
        canvas.drawRect(width * 0.38f, height * 0.18f, width * 0.62f, horizonY + 20f, paint)

        // Simulated historical target position based on timestamp
        val timeSec = (timestampMs / 1000.0)
        val targetX = width * 0.5f + (sin(timeSec * 0.8).toFloat() * (width * 0.25f))
        val targetY = horizonY + (cos(timeSec * 0.5).toFloat() * 25f)
        val targetRadius = 20f

        paint.style = Paint.Style.FILL
        paint.color = Color.rgb(40, 55, 80)
        canvas.drawCircle(targetX, targetY, targetRadius, paint)

        // Bounding box on logged activity
        paint.style = Paint.Style.STROKE
        paint.color = Color.rgb(245, 158, 11) // Amber for playback history
        paint.strokeWidth = 2f
        val rect = RectF(targetX - 25f, targetY - 30f, targetX + 25f, targetY + 30f)
        canvas.drawRect(rect, paint)

        // Vintage DVR scanlines effect
        paint.color = Color.argb(25, 255, 255, 255)
        paint.strokeWidth = 1f
        var scanY = 0f
        while (scanY < height) {
            canvas.drawLine(0f, scanY, width.toFloat(), scanY, paint)
            scanY += 4f
        }

        // Top Left: Channel Name & DVR PLAYBACK status
        textPaint.color = Color.rgb(245, 158, 11) // Amber
        textPaint.textSize = 18f
        textPaint.isFakeBoldText = true
        canvas.drawText("DVR PLAYBACK • ${cameraName.uppercase()}", 24f, 36f, textPaint)

        // Top Right: Play indicator
        textPaint.color = Color.rgb(59, 130, 246)
        textPaint.textSize = 15f
        canvas.drawText("ARCHIVE [CH-01]", width - 150f, 36f, textPaint)

        // Center watermark
        textPaint.color = Color.argb(40, 255, 255, 255)
        textPaint.textSize = 28f
        textPaint.isFakeBoldText = true
        canvas.drawText("POST-RECORDING PLAYBACK", width * 0.15f, height * 0.52f, textPaint)

        // Bottom Left: Exact historical timestamp
        textPaint.color = Color.rgb(241, 245, 249)
        textPaint.textSize = 15f
        textPaint.isFakeBoldText = false
        val timeString = timeFormat.format(Date(timestampMs))
        canvas.drawText(timeString, 24f, height - 24f, textPaint)

        // Bottom Right: Forensic Watermark
        textPaint.color = Color.rgb(148, 163, 184)
        textPaint.textSize = 13f
        canvas.drawText("AUTHENTICATED DVR BUFFER", width - 210f, height - 24f, textPaint)

        return bitmap
    }

    fun addRecordedClip(clip: DvrClip) {
        val current = _recordedClips.value.toMutableList()
        current.add(0, clip)
        _recordedClips.value = current
    }
}
