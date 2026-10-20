package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.CameraDeviceEntity
import com.example.data.local.MotionEventEntity
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.LiveGreen
import com.example.ui.theme.SurveillanceDarkBg
import com.example.ui.theme.SurveillanceSurface
import com.example.viewmodel.CctvAppMode
import com.example.viewmodel.CctvViewModel

@Composable
fun DashboardScreen(
    viewModel: CctvViewModel,
    cameras: List<CameraDeviceEntity>,
    motionEvents: List<MotionEventEntity>,
    modifier: Modifier = Modifier
) {
    val unreadAlertsCount = motionEvents.size

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SurveillanceDarkBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "OMNICAM COMMAND",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "Encrypted Multi-Node Surveillance",
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(SurveillanceSurface, CircleShape)
                        .border(1.dp, Color(0xFF28344A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "System Secured",
                        tint = LiveGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Quick Mode Action Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeActionCard(
                        title = "Camera Node",
                        subtitle = "Stream & Detect Motion",
                        icon = Icons.Default.CameraAlt,
                        accentColor = CyanAccent,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_camera_node",
                        onClick = { viewModel.setAppMode(CctvAppMode.CAMERA_NODE) }
                    )
                    ModeActionCard(
                        title = "Monitor Console",
                        subtitle = "Remote Live Stream",
                        icon = Icons.Default.Videocam,
                        accentColor = LiveGreen,
                        modifier = Modifier.weight(1f),
                        testTag = "nav_viewer_console",
                        onClick = { viewModel.setAppMode(CctvAppMode.VIEWER_CONSOLE) }
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ModeActionCard(
                        title = "Alerts Archive",
                        subtitle = "$unreadAlertsCount Motion Logs",
                        icon = Icons.Default.NotificationsActive,
                        accentColor = Color(0xFFFF9100),
                        modifier = Modifier.weight(1f),
                        testTag = "nav_alerts_archive",
                        onClick = { viewModel.setAppMode(CctvAppMode.ALERTS_ARCHIVE) }
                    )
                    ModeActionCard(
                        title = "Settings",
                        subtitle = "Bandwidth & Security",
                        icon = Icons.Default.Settings,
                        accentColor = Color(0xFF94A3B8),
                        modifier = Modifier.weight(1f),
                        testTag = "nav_settings",
                        onClick = { viewModel.setAppMode(CctvAppMode.SETTINGS) }
                    )
                }
            }
        }

        // Active Cameras Header
        item {
            Text(
                text = "REGISTERED NODES (${cameras.size})",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Cameras List
        items(cameras) { cam ->
            Card(
                colors = CardDefaults.cardColors(containerColor = SurveillanceSurface),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, Color(0xFF28344A), RoundedCornerShape(12.dp))
                    .clickable {
                        viewModel.selectCameraChannel(cam.id)
                        viewModel.setAppMode(CctvAppMode.VIEWER_CONSOLE)
                    }
                    .testTag("camera_item_${cam.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(if (cam.isOnline) LiveGreen else Color(0xFFFF3D57), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cam.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${cam.location} • ${cam.resolution} • Battery ${cam.batteryPercent}%",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp
                        )
                    }
                    Surface(
                        color = Color(0xFF1C2433),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = if (cam.isOnline) "ONLINE" else "OFFLINE",
                            color = if (cam.isOnline) LiveGreen else Color(0xFFFF3D57),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ModeActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurveillanceSurface),
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .border(1.dp, Color(0xFF28344A), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}
