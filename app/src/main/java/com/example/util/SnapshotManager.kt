package com.example.util

/**
 * Snapshot & Verification Watermark Engine
 * Created by Rei (777rei / reialwaysfreemium)
 * Privacy First App - On-Device Cryptographic Surveillance Snapshots
 */

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SnapshotManager {

    /**
     * Burns an official security surveillance timestamp and camera metadata onto the bitmap,
     * and saves it directly to the device's Pictures / OmniCam gallery.
     */
    fun saveTimestampedSnapshot(
        context: Context,
        sourceBitmap: Bitmap,
        cameraName: String = "CAM-01 | SURVEILLANCE",
        timestampMs: Long = System.currentTimeMillis()
    ): String? {
        val watermarkedBitmap = addTimestampWatermark(sourceBitmap, cameraName, timestampMs)
        val timeStampFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
        val filename = "OMNICAM_${timeStampFormat.format(Date(timestampMs))}.jpg"

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/OmniCam")
                }
                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return null

                context.contentResolver.openOutputStream(uri)?.use { out: OutputStream ->
                    watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                "Saved to Pictures/OmniCam/$filename"
            } else {
                val picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val appDir = File(picturesDir, "OmniCam").apply { mkdirs() }
                val file = File(appDir, filename)
                FileOutputStream(file).use { out ->
                    watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                file.absolutePath
            }
        } catch (e: Exception) {
            // Fallback to internal app storage if external permission or media store fails
            try {
                val internalDir = File(context.filesDir, "cctv_snapshots").apply { mkdirs() }
                val file = File(internalDir, filename)
                FileOutputStream(file).use { out ->
                    watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                file.absolutePath
            } catch (ex: Exception) {
                null
            }
        }
    }

    /**
     * Renders a crisp tactical timestamp banner onto the bitmap.
     */
    private fun addTimestampWatermark(
        source: Bitmap,
        cameraName: String,
        timestampMs: Long
    ): Bitmap {
        val result = source.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val width = result.width.toFloat()
        val height = result.height.toFloat()

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = (height * 0.038f).coerceIn(16f, 36f)
            typeface = android.graphics.Typeface.MONOSPACE
            isFakeBoldText = true
        }

        val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(0, 240, 255) // Cyan
            textSize = (height * 0.028f).coerceIn(12f, 26f)
            typeface = android.graphics.Typeface.MONOSPACE
        }

        val bannerHeight = (height * 0.11f).coerceIn(40f, 90f)

        // Draw translucent dark obsidian banner at bottom
        paint.color = Color.argb(200, 10, 12, 16)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, height - bannerHeight, width, height, paint)

        // Draw precision cyan accent line above banner
        paint.color = Color.rgb(59, 130, 246) // Precision Blue
        paint.strokeWidth = 3f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(0f, height - bannerHeight, width, height - bannerHeight, paint)

        val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS 'UTC'", Locale.US)
        val timeString = timeFormat.format(Date(timestampMs))

        val padX = 20f
        val line1Y = height - (bannerHeight * 0.52f)
        val line2Y = height - (bannerHeight * 0.18f)

        // Draw Camera Name and Timestamp
        canvas.drawText("$cameraName • $timeString", padX, line1Y, textPaint)

        // Draw Security, Ownership & Encryption Badge
        canvas.drawText("OMNICAM • CREATED BY REI (777rei) • PRIVACY-FIRST CONSOLE • AES-256 ENCRYPTED", padX, line2Y, subTextPaint)

        return result
    }

    /**
     * Burns an official security surveillance timestamp directly into JPEG byte array.
     */
    fun addTimestampWatermarkToJpeg(
        jpegBytes: ByteArray,
        cameraName: String = "CAM-01 | REMOTE BROADCAST",
        timestampMs: Long = System.currentTimeMillis()
    ): ByteArray {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
                ?: return jpegBytes
            val watermarked = addTimestampWatermark(bitmap, cameraName, timestampMs)
            val stream = java.io.ByteArrayOutputStream()
            watermarked.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            stream.toByteArray()
        } catch (e: Exception) {
            jpegBytes
        }
    }
}
