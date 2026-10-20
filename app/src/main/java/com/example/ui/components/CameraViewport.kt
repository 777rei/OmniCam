package com.example.ui.components

/**
 * OmniCam High-Performance Surveillance Camera Viewport
 * Created by Rei (777rei / reialwaysfreemium)
 * Real-time CCTV display card with HUD overlays and direct action controls
 */

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.motion.MotionResult
import com.example.ui.theme.ConsoleBorder
import com.example.ui.theme.PrecisionBlue
import com.example.ui.theme.StatusLiveEmerald
import com.example.ui.theme.StatusOfflineGray
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.TargetBoundingCyan
import com.example.viewmodel.NightVisionFilter

data class StreamMetrics(
    val bitrateKbps: Int = 2400,
    val fps: Int = 30,
    val resolution: String = "1080p",
    val latencyMs: Int = 18
)

@Composable
fun CameraViewport(
    cameraName: String,
    channelNumber: Int,
    frame: Bitmap?,
    isLive: Boolean = true,
    telemetry: StreamMetrics = StreamMetrics(),
    motionResult: MotionResult? = null,
    nightVisionMode: NightVisionFilter = NightVisionFilter.NORMAL,
    onDoubleTap: () -> Unit = {},
    onLongPress: () -> Unit = {},
    onClick: () -> Unit = {},
    onSnapshot: () -> Unit = {},
    onDvrPlayback: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colorFilter = when (nightVisionMode) {
        NightVisionFilter.NORMAL -> null
        NightVisionFilter.IR_MONOCHROME -> {
            val matrix = ColorMatrix().apply { setToSaturation(0f) }
            ColorFilter.colorMatrix(matrix)
        }
        NightVisionFilter.NIGHT_VISION_GREEN -> {
            ColorFilter.colorMatrix(
                ColorMatrix(
                    floatArrayOf(
                        0.1f, 0.1f, 0.1f, 0f, 0f,
                        0.3f, 1.2f, 0.3f, 0f, 40f,
                        0.1f, 0.1f, 0.1f, 0f, 0f,
                        0f,   0f,   0f,   1f, 0f
                    )
                )
            )
        }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF07090E))
            .border(1.dp, if (isLive) Color(0x353B82F6) else ConsoleBorder, RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { onDoubleTap() },
                    onLongPress = { onLongPress() },
                    onTap = { onClick() }
                )
            }
            .testTag("camera_viewport_$cameraName")
    ) {
        if (frame != null && isLive) {
            Image(
                bitmap = frame.asImageBitmap(),
                contentDescription = cameraName,
                contentScale = ContentScale.Crop,
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Standby obsidian grid background with crosshair
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isLive) "INITIALIZING SECURE CCTV..." else "FEED OFFLINE",
                    color = if (isLive) PrecisionBlue else StatusOfflineGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Target Bounding Box Layer
        if (motionResult?.isMotionDetected == true && motionResult.boundingBox != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
                    .border(1.5.dp, TargetBoundingCyan, RoundedCornerShape(4.dp))
                    .background(Color(0x1800F0FF))
            )
        }

        // Floating Top-Left Channel & Camera Identifier
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xCC000000))
                .border(0.5.dp, ConsoleBorder, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "CH-0$channelNumber • ${cameraName.uppercase()}",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
        }

        // Floating Top-Right Live Bitrate Status Pill
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xCC000000))
                .border(1.dp, if (isLive) StatusLiveEmerald.copy(alpha = 0.5f) else Color.Transparent, RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(if (isLive) StatusLiveEmerald else StatusOfflineGray)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isLive) "${telemetry.fps} FPS • ${telemetry.resolution}" else "OFFLINE",
                color = if (isLive) Color.White else StatusOfflineGray,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
        }

        // Floating Bottom Action Bar with Quick Controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick DVR Playback button
            IconButton(
                onClick = onDvrPlayback,
                modifier = Modifier
                    .size(26.dp)
                    .background(Color(0xCC0A0E18), CircleShape)
                    .border(0.5.dp, ConsoleBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "DVR Playback",
                    tint = StatusWarningAmber,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Quick Snapshot button
            IconButton(
                onClick = onSnapshot,
                modifier = Modifier
                    .size(26.dp)
                    .background(Color(0xCC0A0E18), CircleShape)
                    .border(0.5.dp, ConsoleBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Take Snapshot",
                    tint = PrecisionBlue,
                    modifier = Modifier.size(13.dp)
                )
            }

            // Fullscreen button
            IconButton(
                onClick = onClick,
                modifier = Modifier
                    .size(26.dp)
                    .background(Color(0xCC0A0E18), CircleShape)
                    .border(0.5.dp, ConsoleBorder, CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Fullscreen,
                    contentDescription = "Open Stream",
                    tint = Color.White,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}
