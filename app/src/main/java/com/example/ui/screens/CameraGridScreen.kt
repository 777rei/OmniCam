package com.example.ui.screens

/**
 * OmniCam Security Console Camera Grid View
 * Created by Rei (777rei / reialwaysfreemium)
 * High-Performance Multi-Channel CCTV Surveillance Grid
 */

import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CameraDeviceEntity
import com.example.ui.components.AddCameraDialog
import com.example.ui.components.CameraViewport
import com.example.ui.components.StreamMetrics
import com.example.ui.theme.ConsoleBase
import com.example.ui.theme.ConsoleBorder
import com.example.ui.theme.ConsoleSurface
import com.example.ui.theme.PrecisionBlue
import com.example.ui.theme.StatusAlertRed
import com.example.ui.theme.StatusLiveEmerald
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.SnapshotManager
import com.example.viewmodel.CctvViewModel

@Composable
fun CameraGridScreen(
    viewModel: CctvViewModel,
    cameras: List<CameraDeviceEntity>,
    frame: Bitmap?,
    onOpenStreamDetail: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var gridColumns by remember(cameras.size) { mutableIntStateOf(if (cameras.size <= 1) 1 else 2) }
    var showAddDialog by remember { mutableStateOf(false) }
    var cameraToDelete by remember { mutableStateOf<CameraDeviceEntity?>(null) }

    // Multi-camera live frames map from ViewModel
    val cameraFrames by viewModel.cameraFrames.collectAsState()

    if (showAddDialog) {
        AddCameraDialog(
            viewModel = viewModel,
            onDismiss = { showAddDialog = false }
        )
    }

    cameraToDelete?.let { cam ->
        AlertDialog(
            onDismissRequest = { cameraToDelete = null },
            title = {
                Text(
                    text = "REMOVE CAMERA NODE?",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${cam.name}' from the security console?",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeCamera(cam.id)
                        cameraToDelete = null
                        Toast.makeText(context, "Camera node removed", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusAlertRed)
                ) {
                    Text("Remove", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { cameraToDelete = null }) {
                    Text("Cancel", color = TextSecondary, fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                }
            },
            containerColor = ConsoleSurface,
            shape = RoundedCornerShape(12.dp)
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ConsoleBase)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top Technical Telemetry Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ConsoleSurface)
                    .border(1.dp, ConsoleBorder)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(6.dp).background(StatusLiveEmerald, CircleShape))
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "SECURITY CONSOLE",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${cameras.size} ${if (cameras.size == 1) "NODE" else "NODES"} ONLINE",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    Text(
                        text = "CREATED BY REI (777rei) • PRIVACY FIRST",
                        color = PrecisionBlue,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Add Camera Node Button
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrecisionBlue),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier
                        .height(30.dp)
                        .testTag("add_camera_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(2.dp))
                    Text("Add", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Quick Snapshot Capture
                IconButton(
                    onClick = {
                        val activeFrame = frame ?: cameraFrames.values.firstOrNull()
                        if (activeFrame != null) {
                            val path = SnapshotManager.saveTimestampedSnapshot(
                                context = context,
                                sourceBitmap = activeFrame,
                                cameraName = "GRID-ALL-CAPTURE"
                            )
                            Toast.makeText(
                                context,
                                if (path != null) "Picture & timestamp saved to gallery!" else "Failed to save photo",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(context, "No stream frame to capture", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .size(30.dp)
                        .background(Color(0xFF19202D), RoundedCornerShape(6.dp))
                        .border(1.dp, ConsoleBorder, RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Save Picture with Timestamp",
                        tint = PrecisionBlue,
                        modifier = Modifier.size(14.dp)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Grid Layout Pills (1x1, 2x2, 3x3)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0F131D))
                        .border(1.dp, ConsoleBorder, RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    GridPill(label = "1x1", isSelected = gridColumns == 1) { gridColumns = 1 }
                    GridPill(label = "2x2", isSelected = gridColumns == 2) { gridColumns = 2 }
                    GridPill(label = "3x3", isSelected = gridColumns == 3) { gridColumns = 3 }
                }
            }

            // Camera Multi-Stream Grid with individual feeds per camera
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .testTag("camera_stream_grid"),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(cameras, key = { _, cam -> cam.id }) { index, cam ->
                    val camFrame = cameraFrames[cam.id] ?: if (cam.isOnline) frame else null
                    val aspectRatio = if (gridColumns == 1) 16f / 9f else if (gridColumns == 2) 4f / 3f else 1f

                    CameraViewport(
                        cameraName = cam.name,
                        channelNumber = index + 1,
                        frame = camFrame,
                        isLive = cam.isOnline,
                        telemetry = StreamMetrics(
                            bitrateKbps = if (cam.isOnline) 2400 else 0,
                            fps = if (cam.isOnline) 30 else 0,
                            resolution = cam.resolution
                        ),
                        onDoubleTap = {
                            viewModel.selectCameraChannel(cam.id)
                            onOpenStreamDetail(cam.id)
                        },
                        onLongPress = {
                            if (cameras.size > 1) {
                                cameraToDelete = cam
                            }
                        },
                        onClick = {
                            viewModel.selectCameraChannel(cam.id)
                            onOpenStreamDetail(cam.id)
                        },
                        onDvrPlayback = {
                            viewModel.selectCameraChannel(cam.id)
                            viewModel.seekDvr(System.currentTimeMillis() - 30_000L) // 30s playback
                            onOpenStreamDetail(cam.id)
                        },
                        onSnapshot = {
                            if (camFrame != null) {
                                val path = SnapshotManager.saveTimestampedSnapshot(
                                    context = context,
                                    sourceBitmap = camFrame,
                                    cameraName = cam.name
                                )
                                Toast.makeText(
                                    context,
                                    if (path != null) "Photo saved for ${cam.name}!" else "Failed to save photo",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(aspectRatio)
                    )
                }
            }
        }

        // Floating Action Button to quickly add camera
        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = PrecisionBlue,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(48.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Camera Node")
        }
    }
}

@Composable
private fun GridPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (isSelected) PrecisionBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )
    }
}
