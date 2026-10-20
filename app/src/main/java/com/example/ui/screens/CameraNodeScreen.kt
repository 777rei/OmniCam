package com.example.ui.screens

import android.Manifest
import android.graphics.Bitmap
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.OpenInBrowser
import com.example.util.SnapshotManager
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.camera.CameraHelper
import com.example.ui.components.LiveVideoFeed
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.SurveillanceDarkBg
import com.example.ui.theme.SurveillanceSurface
import com.example.viewmodel.CameraNodeState
import com.example.viewmodel.CctvViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CameraNodeScreen(
    viewModel: CctvViewModel,
    state: CameraNodeState,
    frame: Bitmap?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember { mutableStateOf(false) }
    var activeCameraHelper by remember { mutableStateOf<CameraHelper?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            viewModel.toggleCameraSourceMode(true)
        }
    }

    DisposableEffect(state.useHardwareCamera) {
        var cameraHelper: CameraHelper? = null
        if (state.useHardwareCamera) {
            cameraHelper = CameraHelper(
                context = context,
                lifecycleOwner = lifecycleOwner,
                onFrameAnalyzed = { bmp ->
                    viewModel.onCameraFrameCaptured(bmp)
                }
            ).apply {
                startCamera()
            }
            activeCameraHelper = cameraHelper
        }
        onDispose {
            cameraHelper?.shutdown()
            activeCameraHelper = null
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(SurveillanceDarkBg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top status row with Network transport (Wi-Fi / Cellular) & Viewers
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "CAMERA NODE CONSOLE",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${state.networkType} • ${state.localIp}:${state.port}",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Surface(
                    color = if (state.isStreaming) LiveGreen.copy(alpha = 0.2f) else Color(0xFF28344A),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (state.isStreaming) "STREAMING (${state.viewerCount})" else "PAUSED",
                        color = if (state.isStreaming) LiveGreen else Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // Live Viewfinder / Preview
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF070B11))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(12.dp))
            ) {
                if (state.useHardwareCamera) {
                    // CameraX Hardware Preview View
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                                activeCameraHelper?.bindPreviewSurface(this.surfaceProvider)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { previewView ->
                            activeCameraHelper?.bindPreviewSurface(previewView.surfaceProvider)
                        }
                    )

                    // Overlay HUD on top of CameraX PreviewView
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .align(Alignment.TopStart),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Color(0xCC090D14), RoundedCornerShape(6.dp))
                                .border(1.dp, Color(0xFF334155), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(if (state.isStreaming) LiveGreen else Color.Gray, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "HARDWARE CAM",
                                color = LiveGreen,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Row(
                            modifier = Modifier
                                .background(Color(0xCC090D14), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "AES-256",
                                tint = CyanAccent,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "AES-256",
                                color = CyanAccent,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    // Motion Detected Alert overlay
                    if (state.motionResult?.isMotionDetected == true) {
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 42.dp)
                                .background(Color(0xEEFF1744), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alert",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "MOTION DETECTED",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 1.sp
                            )
                        }
                    }
                } else {
                    // Fallback to Synthetic / Simulated stream with moving targets
                    LiveVideoFeed(
                        frame = frame,
                        motionResult = state.motionResult,
                        channelLabel = "SIMULATION TEST FEED",
                        isStreaming = state.isStreaming,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Mode Selector: Camera Lens vs Simulation Mode
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.useHardwareCamera) CyanAccent else SurveillanceSurface,
                        contentColor = if (state.useHardwareCamera) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Camera Lens", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.toggleCameraSourceMode(false) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!state.useHardwareCamera) CyanAccent else SurveillanceSurface,
                        contentColor = if (!state.useHardwareCamera) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = "Simulation Mode", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Remote Browser Stream Link & Web Controller Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF141822))
                    .border(
                        1.dp,
                        if (state.isStreaming) LiveGreen.copy(alpha = 0.5f) else Color(0xFF28344A),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(if (state.isStreaming) LiveGreen else Color.Gray, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BROWSER STREAM & CONTROLLER",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = if (state.isStreaming) "ONLINE" else "PAUSED",
                        color = if (state.isStreaming) LiveGreen else Color.Gray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Stream URL Box with Tap-to-Copy
                val streamAddress = "http://${state.localIp}:${state.port}"
                Surface(
                    color = Color(0xFF0A0C10),
                    shape = RoundedCornerShape(8.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF28344A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("OmniCam URL", streamAddress))
                            Toast.makeText(context, "Copied: $streamAddress", Toast.LENGTH_SHORT).show()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = streamAddress,
                                color = CyanAccent,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Open in browser for live feed & remote PTZ/torch/siren controls",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Link",
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(streamAddress))
                                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Unable to launch browser: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = state.isStreaming,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.OpenInBrowser, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Open in Browser", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (frame != null) {
                                val saved = SnapshotManager.saveTimestampedSnapshot(
                                    context = context,
                                    sourceBitmap = frame,
                                    cameraName = "CAM-01 | NODE BROADCAST"
                                )
                                Toast.makeText(
                                    context,
                                    if (saved != null) "Picture & timestamp saved to Gallery!\n$saved" else "Failed to save photo",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "No active frame to save", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp), tint = CyanAccent)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Save Picture", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Created by Rei (777rei • reialwaysfreemium)",
                        color = Color(0xFF3B82F6),
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PRIVACY FIRST",
                        color = LiveGreen,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Tactical Action Buttons (Stream, Arm, Siren)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.toggleStreamingServer() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isStreaming) AlertRed else CyanAccent,
                        contentColor = if (state.isStreaming) Color.White else Color.Black
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_streaming_button")
                ) {
                    Text(text = if (state.isStreaming) "Stop Server" else "Start Server", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.toggleArmedState() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isArmed) LiveGreen else SurveillanceSurface,
                        contentColor = if (state.isArmed) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("toggle_armed_button")
                ) {
                    Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (state.isArmed) "Armed" else "Disarmed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.triggerManualSiren() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isSirenPlaying) AlertRed else SurveillanceSurface,
                        contentColor = if (state.isSirenPlaying) Color.White else AlertRed
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("trigger_siren_button")
                ) {
                    Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (state.isSirenPlaying) "Stop Siren" else "Siren", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Quick Device Controls (Torch, Flip Lens, Stealth Mode)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        activeCameraHelper?.toggleTorch()
                        viewModel.toggleTorch()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isTorchOn) Color(0xFFFF9100) else SurveillanceSurface,
                        contentColor = if (state.isTorchOn) Color.Black else Color.White
                    ),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.FlashlightOn, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (state.isTorchOn) "Torch ON" else "Torch", fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        activeCameraHelper?.toggleCameraLens()
                        viewModel.toggleCameraLens()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SurveillanceSurface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Cameraswitch, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Flip Lens", fontSize = 11.sp, color = Color.White)
                }

                Button(
                    onClick = { viewModel.toggleStealthMode() },
                    colors = ButtonDefaults.buttonColors(containerColor = SurveillanceSurface),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(imageVector = Icons.Default.Nightlight, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Stealth", fontSize = 11.sp, color = Color.White)
                }
            }

            // Motion Detection Zone Grid Masking Editor
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurveillanceSurface)
                    .border(1.dp, Color(0xFF28344A), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "MOTION DETECTION ZONE MASK (4x4)",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(4),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.height(90.dp)
                ) {
                    items(16) { index ->
                        val isActive = state.detectionZone.activeCells.getOrElse(index) { true }
                        Box(
                            modifier = Modifier
                                .height(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isActive) CyanAccent.copy(alpha = 0.4f) else Color.DarkGray)
                                .border(1.dp, if (isActive) CyanAccent else Color.Gray, RoundedCornerShape(4.dp))
                            .clickable { viewModel.toggleZoneCell(index) }
                            .testTag("zone_cell_$index"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isActive) "ACTIVE" else "MUTED",
                                color = if (isActive) Color.White else Color.Gray,
                                fontSize = 7.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // Stealth Mode / Battery Saver Screen Overlay
        if (state.isStealthMode) {
            val clockFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable { viewModel.toggleStealthMode() }
                    .testTag("stealth_mode_overlay"),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = clockFormat.format(Date()),
                        color = Color(0xFF1E293B),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SURVEILLANCE & STREAMING ACTIVE",
                        color = Color(0xFF0F172A),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Tap anywhere to unlock display",
                        color = Color(0xFF334155),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}
