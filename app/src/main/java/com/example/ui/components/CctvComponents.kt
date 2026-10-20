package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CropFree
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ConsoleBase
import com.example.ui.theme.ConsoleBorder
import com.example.ui.theme.ConsoleSurface
import com.example.ui.theme.PrecisionBlue
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusLiveEmerald
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PtzDirection {
    UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT, CENTER
}

data class TimelineMarker(
    val timestampMs: Long,
    val type: MarkerType, // MOTION, ALERT, CONTINUOUS
    val label: String
)

enum class MarkerType {
    MOTION, ALERT, CONTINUOUS
}

/**
 * 1. CctvBadge with optional pulsing glow for live indicators and alert tags.
 */
@Composable
fun CctvBadge(
    text: String,
    color: Color,
    isPulsing: Boolean = false,
    modifier: Modifier = Modifier
) {
    val alpha = if (isPulsing) {
        val transition = rememberInfiniteTransition(label = "badgePulse")
        val animatedAlpha by transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(600),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
        animatedAlpha
    } else 1.0f

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xD0000000))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = alpha))
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            letterSpacing = 0.5.sp
        )
    }
}

/**
 * 2. Specialized PTZ 8-way directional touchpad overlay.
 */
@Composable
fun PtzOverlayControls(
    onDirection: (PtzDirection) -> Unit,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color(0xDD141822),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x333B82F6)),
        shadowElevation = 8.dp,
        modifier = modifier.width(220.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "PTZ TELEMETRY",
                color = PrecisionBlue,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 3x3 Directional Pad
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row 1: UP_LEFT, UP, UP_RIGHT
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PtzButton(Icons.Default.NorthWest) { onDirection(PtzDirection.UP_LEFT) }
                    PtzButton(Icons.Default.ArrowUpward) { onDirection(PtzDirection.UP) }
                    PtzButton(Icons.Default.NorthEast) { onDirection(PtzDirection.UP_RIGHT) }
                }
                // Row 2: LEFT, CENTER, RIGHT
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PtzButton(Icons.Default.ArrowBack) { onDirection(PtzDirection.LEFT) }
                    PtzButton(Icons.Default.CropFree, isCenter = true) { onDirection(PtzDirection.CENTER) }
                    PtzButton(Icons.Default.ArrowForward) { onDirection(PtzDirection.RIGHT) }
                }
                // Row 3: DOWN_LEFT, DOWN, DOWN_RIGHT
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    PtzButton(Icons.Default.SouthWest) { onDirection(PtzDirection.DOWN_LEFT) }
                    PtzButton(Icons.Default.ArrowDownward) { onDirection(PtzDirection.DOWN) }
                    PtzButton(Icons.Default.SouthEast) { onDirection(PtzDirection.DOWN_RIGHT) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Zoom In / Out Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = onZoomOut,
                    modifier = Modifier
                        .size(36.dp)
                        .background(ConsoleSurface, CircleShape)
                        .border(1.dp, ConsoleBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Text(
                    text = "OPTICAL / DIGITAL ZOOM",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                IconButton(
                    onClick = onZoomIn,
                    modifier = Modifier
                        .size(36.dp)
                        .background(ConsoleSurface, CircleShape)
                        .border(1.dp, ConsoleBorder, CircleShape)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = PrecisionBlue, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PtzButton(
    icon: ImageVector,
    isCenter: Boolean = false,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isCenter) PrecisionBlue.copy(alpha = 0.25f) else Color(0xFF0F131D))
            .border(1.dp, if (isCenter) PrecisionBlue else ConsoleBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isCenter) PrecisionBlue else Color.White,
            modifier = Modifier.size(18.dp)
        )
    }
}

/**
 * 3. Video Timeline & Event Scrubber with color-coded ticks and millisecond precision readout.
 * Supports Real-time Live synchronization and Post-Recording DVR playback mode.
 */
@Composable
fun EventTimelineScrubber(
    markers: List<TimelineMarker>,
    currentPositionMs: Long,
    isLive: Boolean = true,
    totalDurationMs: Long = 3600_000L, // 1 hour window
    onSeek: (Long) -> Unit,
    onJumpToLive: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    var scrubberProgress by remember(isLive) { mutableFloatStateOf(if (isLive) 1.0f else 0.85f) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0E1118))
            .border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isLive) StatusLiveEmerald else StatusWarningAmber, CircleShape)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isLive) "DVR TIMELINE [LIVE]" else "DVR [POST-RECORDING]",
                    color = if (isLive) StatusLiveEmerald else StatusWarningAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = timeFormat.format(Date(currentPositionMs)),
                    color = if (isLive) PrecisionBlue else StatusWarningAmber,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )

                if (!isLive) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(StatusAlertRed)
                            .clickable {
                                scrubberProgress = 1.0f
                                onJumpToLive()
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "LIVE",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Canvas Track with hourly ticks, continuous blue bands, motion orange ticks, alert red ticks
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                        scrubberProgress = newProgress
                        if (newProgress >= 0.98f) {
                            onJumpToLive()
                        } else {
                            val targetMs = System.currentTimeMillis() - ((1f - newProgress) * totalDurationMs).toLong()
                            onSeek(targetMs)
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val trackY = size.height * 0.5f
                val trackHeight = 10f

                // Continuous recording blue baseline bar
                drawRect(
                    color = Color(0xFF1E3A8A).copy(alpha = 0.6f),
                    topLeft = Offset(0f, trackY - (trackHeight / 2)),
                    size = Size(size.width, trackHeight)
                )

                // Hourly ticks
                val tickInterval = size.width / 12f
                for (i in 0..12) {
                    val x = i * tickInterval
                    drawLine(
                        color = Color(0x40FFFFFF),
                        start = Offset(x, trackY - 10f),
                        end = Offset(x, trackY + 10f),
                        strokeWidth = 1.5f
                    )
                }

                // Color-coded event markers
                markers.forEachIndexed { index, marker ->
                    val x = (index.toFloat() / (markers.size.coerceAtLeast(1) + 1)) * size.width
                    val color = when (marker.type) {
                        MarkerType.ALERT -> StatusAlertRed
                        MarkerType.MOTION -> StatusWarningAmber
                        MarkerType.CONTINUOUS -> PrecisionBlue
                    }
                    drawRect(
                        color = color,
                        topLeft = Offset(x - 2.5f, trackY - 14f),
                        size = Size(5f, 28f)
                    )
                }

                // Scrubber Playhead handle
                val currentHandleProgress = if (isLive) 1.0f else scrubberProgress
                val handleX = size.width * currentHandleProgress
                drawLine(
                    color = if (isLive) StatusLiveEmerald else Color.White,
                    start = Offset(handleX, 0f),
                    end = Offset(handleX, size.height),
                    strokeWidth = 2.5f
                )
                drawCircle(
                    color = if (isLive) StatusLiveEmerald else PrecisionBlue,
                    radius = 5f,
                    center = Offset(handleX, 6f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Legend Row - Compact
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            LegendItem(color = PrecisionBlue, label = "Continuous [REC]")
            LegendItem(color = StatusWarningAmber, label = "Motion Event")
            LegendItem(color = StatusAlertRed, label = "Critical Alert")
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(6.dp).background(color, RoundedCornerShape(1.dp)))
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace
        )
    }
}
