package com.example.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CameraDeviceEntity
import com.example.ui.components.LiveVideoFeed
import com.example.ui.theme.AlertRed
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.SurveillanceDarkBg
import com.example.ui.theme.SurveillanceSurface
import com.example.viewmodel.CctvViewModel
import com.example.viewmodel.ViewerConsoleState

@Composable
fun ViewerConsoleScreen(
    viewModel: CctvViewModel,
    state: ViewerConsoleState,
    cameras: List<CameraDeviceEntity>,
    frame: Bitmap?,
    modifier: Modifier = Modifier
) {
    val activeCamera = cameras.find { it.id == state.selectedCameraId }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurveillanceDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Channel Selector Ribbon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            cameras.forEach { cam ->
                val isSelected = cam.id == state.selectedCameraId
                Surface(
                    color = if (isSelected) CyanAccent else SurveillanceSurface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .border(1.dp, if (isSelected) CyanAccent else Color(0xFF28344A), RoundedCornerShape(8.dp))
                        .clickable { viewModel.selectCameraChannel(cam.id) }
                        .testTag("channel_pill_${cam.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(if (cam.isOnline) LiveGreen else Color.Gray, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = cam.name,
                            color = if (isSelected) Color.Black else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // Live Video Feed Viewer with Zoom/Pan & Night Vision
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            LiveVideoFeed(
                frame = frame,
                motionResult = null,
                channelLabel = activeCamera?.name?.uppercase() ?: "SURVEILLANCE FEED",
                nightVisionMode = state.nightVisionMode,
                zoomLevel = state.zoomLevel,
                panOffsetX = state.panOffsetX,
                panOffsetY = state.panOffsetY,
                isStreaming = activeCamera?.isOnline ?: true,
                latencyMs = state.latencyMs,
                onTransform = { zoom, panX, panY ->
                    viewModel.setZoomLevel(state.zoomLevel * zoom)
                    viewModel.setPanOffset(state.panOffsetX + panX, state.panOffsetY + panY)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // PTZ & Vision Controls (Night Vision, Zoom Cycle, Snapshot)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.cycleNightVisionMode() },
                colors = ButtonDefaults.buttonColors(containerColor = SurveillanceSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Visibility, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "NV Mode", fontSize = 11.sp, color = Color.White)
            }

            Button(
                onClick = {
                    val nextZoom = if (state.zoomLevel >= 3.0f) 1.0f else state.zoomLevel + 0.5f
                    viewModel.setZoomLevel(nextZoom)
                },
                colors = ButtonDefaults.buttonColors(containerColor = SurveillanceSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.ZoomIn, contentDescription = null, tint = LiveGreen, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "${String.format("%.1f", state.zoomLevel)}x", fontSize = 11.sp, color = Color.White)
            }

            Button(
                onClick = { viewModel.takeViewerSnapshot() },
                colors = ButtonDefaults.buttonColors(containerColor = SurveillanceSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Snapshot", fontSize = 11.sp, color = Color.White)
            }
        }

        // Remote Controls Section Header
        Text(
            text = "REMOTE TELEMETRY & COMMANDS",
            color = Color(0xFF94A3B8),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.padding(top = 4.dp)
        )

        // Remote Action Buttons (Torch, Siren, Flip Lens)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.toggleTorch() },
                colors = ButtonDefaults.buttonColors(containerColor = SurveillanceSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.FlashlightOn, contentDescription = null, tint = Color(0xFFFF9100), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Remote Torch", fontSize = 10.sp, color = Color.White)
            }

            Button(
                onClick = { viewModel.triggerManualSiren() },
                colors = ButtonDefaults.buttonColors(containerColor = SurveillanceSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.LocalFireDepartment, contentDescription = null, tint = AlertRed, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Remote Alarm", fontSize = 10.sp, color = AlertRed)
            }

            Button(
                onClick = { viewModel.toggleCameraLens() },
                colors = ButtonDefaults.buttonColors(containerColor = SurveillanceSurface),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Cameraswitch, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Flip Node Lens", fontSize = 10.sp, color = Color.White)
            }
        }

        // Intercom & Recording Actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.toggleIntercom() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isIntercomActive) AlertRed else SurveillanceSurface,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = if (state.isIntercomActive) "Muting Mic" else "Push-to-Talk", fontSize = 11.sp)
            }

            Button(
                onClick = { viewModel.toggleRecordingClip() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (state.isRecordingClip) AlertRed else SurveillanceSurface,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                Box(modifier = Modifier.size(8.dp).background(if (state.isRecordingClip) Color.White else AlertRed, CircleShape))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (state.isRecordingClip) "Recording..." else "Record Clip", fontSize = 11.sp)
            }
        }
    }
}
