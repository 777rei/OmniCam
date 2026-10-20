package com.example.ui.screens

/**
 * OmniCam Live Stream & Fullscreen Surveillance Console
 * Created by Rei (777rei / reialwaysfreemium)
 * High-Performance Real-Time Stream & Real Post-Recording DVR Engine
 */

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.ControlCamera
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CameraDeviceEntity
import com.example.data.local.MotionEventEntity
import com.example.engine.dvr.DvrClip
import com.example.ui.components.CctvBadge
import com.example.ui.components.EventTimelineScrubber
import com.example.ui.components.MarkerType
import com.example.ui.components.PtzDirection
import com.example.ui.components.PtzOverlayControls
import com.example.ui.components.TimelineMarker
import com.example.ui.theme.ConsoleBase
import com.example.ui.theme.ConsoleBorder
import com.example.ui.theme.ConsoleSurface
import com.example.ui.theme.PrecisionBlue
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusLiveEmerald
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.TargetBoundingCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SnapshotManager
import com.example.viewmodel.CctvViewModel
import com.example.viewmodel.NightVisionFilter
import com.example.viewmodel.ViewerConsoleState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveFeedDetailScreen(
    viewModel: CctvViewModel,
    state: ViewerConsoleState,
    activeCamera: CameraDeviceEntity?,
    frame: Bitmap?,
    motionEvents: List<MotionEventEntity>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isPtzVisible by remember { mutableStateOf(false) }
    var isLiveStreamMode by remember { mutableStateOf(true) }

    // DVR Playback State observed from ViewModel
    val dvrPlaybackTimeMs by viewModel.dvrPlaybackTimeMs.collectAsState()
    val dvrFrame by viewModel.dvrFrame.collectAsState()
    val isDvrPlaying by viewModel.isDvrPlaying.collectAsState()
    val dvrSpeed by viewModel.dvrSpeed.collectAsState()
    val recordedClips by viewModel.recordedClips.collectAsState()

    // Live millisecond ticker
    var currentLiveClockMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(isLiveStreamMode) {
        while (isLiveStreamMode) {
            delay(100)
            currentLiveClockMs = System.currentTimeMillis()
        }
    }

    val displayTimestamp = if (isLiveStreamMode) currentLiveClockMs else dvrPlaybackTimeMs
    val activeDisplayFrame = if (isLiveStreamMode) frame else (dvrFrame ?: frame)
    val timeFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US) }
    val clipTimeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }

    val timelineMarkers = remember(motionEvents) {
        motionEvents.take(15).map { event ->
            TimelineMarker(
                timestampMs = event.timestamp,
                type = if (event.confidence > 0.7f) MarkerType.ALERT else MarkerType.MOTION,
                label = event.eventType
            )
        }
    }

    val colorFilter = when (state.nightVisionMode) {
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
            .fillMaxSize()
            .background(ConsoleBase)
    ) {
        // Main Video Canvas: Live Video or Post-Recording DVR Frame
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        viewModel.setZoomLevel(state.zoomLevel * zoom)
                        viewModel.setPanOffset(state.panOffsetX + pan.x, state.panOffsetY + pan.y)
                    }
                }
        ) {
            if (activeDisplayFrame != null) {
                Image(
                    bitmap = activeDisplayFrame.asImageBitmap(),
                    contentDescription = activeCamera?.name ?: "Surveillance Stream",
                    contentScale = ContentScale.Crop,
                    colorFilter = colorFilter,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = state.zoomLevel
                            scaleY = state.zoomLevel
                            translationX = state.panOffsetX
                            translationY = state.panOffsetY
                        }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ACQUIRING DIRECT SECURE CCTV FEED...",
                        color = PrecisionBlue,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Post-Recording DVR Banner Watermark
            if (!isLiveStreamMode) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xDD000000))
                            .border(1.dp, StatusWarningAmber, RoundedCornerShape(6.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(7.dp).background(StatusWarningAmber, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isDvrPlaying) "▶ DVR PLAYING (${dvrSpeed}X)" else "⏸ DVR PAUSED",
                            color = StatusWarningAmber,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Subtle Bounding Target Box
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .border(1.2.dp, TargetBoundingCyan.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
            )
        }

        // Top Navigation Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopStart)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xE60A0C10))
                    .border(1.dp, ConsoleBorder)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF141822), CircleShape)
                        .border(1.dp, ConsoleBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CctvBadge(
                            text = activeCamera?.name?.uppercase() ?: "OMNICAM NODE",
                            color = PrecisionBlue
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        CctvBadge(
                            text = if (isLiveStreamMode) "LIVE" else "DVR PLAYBACK",
                            color = if (isLiveStreamMode) StatusLiveEmerald else StatusWarningAmber,
                            isPulsing = isLiveStreamMode
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = timeFormat.format(Date(displayTimestamp)),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }

                // Quick Snapshot Button
                IconButton(
                    onClick = {
                        val snapFrame = activeDisplayFrame
                        if (snapFrame != null) {
                            val path = SnapshotManager.saveTimestampedSnapshot(
                                context = context,
                                sourceBitmap = snapFrame,
                                cameraName = activeCamera?.name ?: "SURVEILLANCE"
                            )
                            Toast.makeText(
                                context,
                                if (path != null) "Picture & timestamp saved to gallery!" else "Failed to save photo",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "No frame available to capture", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .background(PrecisionBlue, CircleShape)
                        .testTag("save_picture_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture Snapshot",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Post-Recording Jump to Live Notification Banner
            AnimatedVisibility(visible = !isLiveStreamMode, enter = fadeIn(), exit = fadeOut()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xF02A1F0D))
                        .border(1.dp, StatusWarningAmber.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(StatusWarningAmber, CircleShape))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "POST-RECORDING DVR PLAYBACK",
                            color = StatusWarningAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Button(
                        onClick = {
                            isLiveStreamMode = true
                            viewModel.seekDvr(System.currentTimeMillis())
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusAlertRed),
                        shape = RoundedCornerShape(4.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                        modifier = Modifier.height(26.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("Jump to Live", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }

        // Floating PTZ Toggle Button
        FloatingActionButton(
            onClick = { isPtzVisible = !isPtzVisible },
            containerColor = if (isPtzVisible) PrecisionBlue else Color(0xD0141822),
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .size(42.dp)
        ) {
            Icon(Icons.Default.ControlCamera, contentDescription = "Toggle PTZ Overlay", modifier = Modifier.size(20.dp))
        }

        // PTZ 8-Way Directional Touchpad Overlay
        if (isPtzVisible) {
            PtzOverlayControls(
                onDirection = { dir ->
                    when (dir) {
                        PtzDirection.UP -> viewModel.setPanOffset(state.panOffsetX, state.panOffsetY - 40f)
                        PtzDirection.DOWN -> viewModel.setPanOffset(state.panOffsetX, state.panOffsetY + 40f)
                        PtzDirection.LEFT -> viewModel.setPanOffset(state.panOffsetX - 40f, state.panOffsetY)
                        PtzDirection.RIGHT -> viewModel.setPanOffset(state.panOffsetX + 40f, state.panOffsetY)
                        PtzDirection.UP_LEFT -> viewModel.setPanOffset(state.panOffsetX - 30f, state.panOffsetY - 30f)
                        PtzDirection.UP_RIGHT -> viewModel.setPanOffset(state.panOffsetX + 30f, state.panOffsetY - 30f)
                        PtzDirection.DOWN_LEFT -> viewModel.setPanOffset(state.panOffsetX - 30f, state.panOffsetY + 30f)
                        PtzDirection.DOWN_RIGHT -> viewModel.setPanOffset(state.panOffsetX + 30f, state.panOffsetY + 30f)
                        PtzDirection.CENTER -> {
                            viewModel.setPanOffset(0f, 0f)
                            viewModel.setZoomLevel(1.0f)
                        }
                    }
                },
                onZoomIn = { viewModel.setZoomLevel(state.zoomLevel + 0.4f) },
                onZoomOut = { viewModel.setZoomLevel((state.zoomLevel - 0.4f).coerceAtLeast(1f)) },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 64.dp)
            )
        }

        // Bottom HUD Panel: DVR Scrubber, DVR Controls, Recorded Clips, & Action Buttons
        Surface(
            color = Color(0xEE0A0C10),
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, ConsoleBorder),
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Interactive Video Timeline Scrubber with millisecond precision
                EventTimelineScrubber(
                    markers = timelineMarkers,
                    currentPositionMs = displayTimestamp,
                    isLive = isLiveStreamMode,
                    onSeek = { seekMs ->
                        isLiveStreamMode = false
                        viewModel.seekDvr(seekMs)
                    },
                    onJumpToLive = {
                        isLiveStreamMode = true
                        viewModel.seekDvr(System.currentTimeMillis())
                    }
                )

                // Post-Recording DVR Playback Controls Row
                if (!isLiveStreamMode) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rewind 10s
                        Button(
                            onClick = { viewModel.stepDvr(-10) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.FastRewind, contentDescription = "-10s", modifier = Modifier.size(13.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("-10s", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                        }

                        // Play / Pause Toggle
                        Button(
                            onClick = { viewModel.toggleDvrPlayPause() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isDvrPlaying) StatusWarningAmber else PrecisionBlue),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = if (isDvrPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isDvrPlaying) "Pause" else "Play",
                                modifier = Modifier.size(15.dp),
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isDvrPlaying) "PAUSE" else "PLAY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black
                            )
                        }

                        // Speed Toggle (1x, 2x, 4x)
                        Button(
                            onClick = {
                                val nextSpeed = when (dvrSpeed) {
                                    1.0f -> 2.0f
                                    2.0f -> 4.0f
                                    else -> 1.0f
                                }
                                viewModel.setDvrPlaybackSpeed(nextSpeed)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = "Speed", modifier = Modifier.size(13.dp), tint = PrecisionBlue)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("${dvrSpeed.toInt()}X", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }

                        // Fast Forward 10s
                        Button(
                            onClick = { viewModel.stepDvr(10) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("+10s", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(Icons.Default.FastForward, contentDescription = "+10s", modifier = Modifier.size(13.dp))
                        }

                        // Return to Live Button
                        Button(
                            onClick = {
                                isLiveStreamMode = true
                                viewModel.seekDvr(System.currentTimeMillis())
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusAlertRed),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("LIVE", fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    // Recorded Event Clips Tray (Click to review past recorded footage)
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "RECORDED CLIPS & EVENTS",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "${recordedClips.size} CLIPS ARCHIVED",
                                color = TextMuted,
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            items(recordedClips, key = { it.id }) { clip ->
                                DvrClipCard(
                                    clip = clip,
                                    timeStr = clipTimeFormat.format(Date(clip.startTimeMs)),
                                    onClick = {
                                        isLiveStreamMode = false
                                        viewModel.selectDvrClip(clip)
                                    }
                                )
                            }
                        }
                    }
                }

                // Quick Action Buttons Row (Night Vision, Torch, Siren, Flip, Intercom)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HudActionButton(
                        icon = Icons.Default.Visibility,
                        label = "NV Mode",
                        accentColor = PrecisionBlue,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.cycleNightVisionMode()
                    }

                    HudActionButton(
                        icon = Icons.Default.FlashlightOn,
                        label = "Torch",
                        accentColor = StatusWarningAmber,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.toggleTorch()
                    }

                    HudActionButton(
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Siren",
                        accentColor = StatusAlertRed,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.triggerManualSiren()
                    }

                    HudActionButton(
                        icon = Icons.Default.Cameraswitch,
                        label = "Flip Lens",
                        accentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.toggleCameraLens()
                    }

                    HudActionButton(
                        icon = Icons.Default.Mic,
                        label = if (state.isIntercomActive) "Muting" else "Talk",
                        accentColor = if (state.isIntercomActive) StatusAlertRed else Color.White,
                        modifier = Modifier.weight(1f)
                    ) {
                        viewModel.toggleIntercom()
                    }
                }
            }
        }
    }
}

@Composable
private fun DvrClipCard(
    clip: DvrClip,
    timeStr: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF141824))
            .border(1.dp, if (clip.confidence > 0.8f) StatusAlertRed.copy(alpha = 0.5f) else ConsoleBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (clip.confidence > 0.8f) Icons.Default.NotificationsActive else Icons.Default.Videocam,
            contentDescription = null,
            tint = if (clip.confidence > 0.8f) StatusAlertRed else StatusWarningAmber,
            modifier = Modifier.size(13.dp)
        )
        Spacer(modifier = Modifier.width(5.dp))
        Column {
            Text(
                text = "$timeStr (${clip.durationSeconds}s)",
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = clip.eventType,
                color = if (clip.confidence > 0.8f) StatusAlertRed else TextSecondary,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun HudActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF141822)),
        shape = RoundedCornerShape(8.dp),
        contentPadding = PaddingValues(vertical = 6.dp, horizontal = 2.dp),
        modifier = modifier.border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = label, fontSize = 9.sp, color = Color.White, fontFamily = FontFamily.Monospace)
        }
    }
}
