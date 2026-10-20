package com.example.ui.screens

import android.graphics.BitmapFactory
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.MotionEventEntity
import com.example.ui.components.CctvBadge
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
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventLogScreen(
    viewModel: CctvViewModel,
    motionEvents: List<MotionEventEntity>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)
    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ConsoleBase)
    ) {
        // Top Technical Telemetry Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConsoleSurface)
                .border(1.dp, ConsoleBorder)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "INCIDENT AUDIT LOGS",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = "${motionEvents.size} LOGGED SECURITY EVENTS • HIGH FIDELITY",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Export CSV Action
            IconButton(
                onClick = {
                    val path = viewModel.exportHistoryCsv(context)
                    Toast.makeText(
                        context,
                        if (path != null) "Exported audit CSV!\n$path" else "Export failed",
                        Toast.LENGTH_LONG
                    ).show()
                },
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0xFF19202D), RoundedCornerShape(6.dp))
                    .border(1.dp, ConsoleBorder, RoundedCornerShape(6.dp))
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Export CSV",
                    tint = PrecisionBlue,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Clear All Action
            if (motionEvents.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearAllAlerts() },
                    modifier = Modifier
                        .size(34.dp)
                        .background(Color(0xFF19202D), RoundedCornerShape(6.dp))
                        .border(1.dp, ConsoleBorder, RoundedCornerShape(6.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear All",
                        tint = StatusAlertRed,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        if (motionEvents.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "NO DETECTION EVENTS LOGGED",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(motionEvents, key = { it.id }) { event ->
                    EventLogItem(
                        event = event,
                        timeStr = timeFormat.format(Date(event.timestamp)),
                        dateStr = dateFormat.format(Date(event.timestamp)),
                        onSaveSnapshot = {
                            if (!event.snapshotUri.isNullOrEmpty()) {
                                val file = File(event.snapshotUri)
                                if (file.exists()) {
                                    val bmp = BitmapFactory.decodeFile(file.absolutePath)
                                    if (bmp != null) {
                                        val path = SnapshotManager.saveTimestampedSnapshot(
                                            context = context,
                                            sourceBitmap = bmp,
                                            cameraName = event.cameraName,
                                            timestampMs = event.timestamp
                                        )
                                        Toast.makeText(context, "Saved event picture with timestamp:\n$path", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } else {
                                Toast.makeText(context, "No saved screenshot file for this event", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onToggleStar = { viewModel.toggleStarEvent(event.id) },
                        onDelete = { viewModel.deleteEvent(event) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EventLogItem(
    event: MotionEventEntity,
    timeStr: String,
    dateStr: String,
    onSaveSnapshot: () -> Unit,
    onToggleStar: () -> Unit,
    onDelete: () -> Unit
) {
    val badgeColor = when {
        event.confidence > 0.7f -> StatusAlertRed
        event.eventType.contains("Vehicle", ignoreCase = true) -> StatusWarningAmber
        else -> TargetBoundingCyan
    }

    val categoryBadgeText = when {
        event.confidence > 0.7f -> "[INTRUDER]"
        event.eventType.contains("Vehicle", ignoreCase = true) -> "[LINE CROSSING]"
        event.eventType.contains("Audio", ignoreCase = true) -> "[AUDIO SPIKE]"
        else -> "[MOTION]"
    }

    // Load local thumbnail if available
    val thumbnailBitmap = remember(event.snapshotUri) {
        if (!event.snapshotUri.isNullOrEmpty()) {
            val file = File(event.snapshotUri)
            if (file.exists()) BitmapFactory.decodeFile(file.absolutePath) else null
        } else null
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = ConsoleSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // High-Contrast Thumbnail Preview (16:9 ratio, fixed width 90.dp)
            Box(
                modifier = Modifier
                    .width(90.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF07090E))
                    .border(1.dp, ConsoleBorder, RoundedCornerShape(4.dp))
                    .clickable(onClick = onSaveSnapshot),
                contentAlignment = Alignment.Center
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap.asImageBitmap(),
                        contentDescription = "Event Snapshot",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Event Details Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CctvBadge(text = categoryBadgeText, color = badgeColor)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = timeStr,
                        color = PrecisionBlue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = event.cameraName.uppercase(),
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${dateStr} • ${(event.confidence * 100).toInt()}% Confidence",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Save Picture Action Button
            IconButton(
                onClick = onSaveSnapshot,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Save Picture with Timestamp",
                    tint = PrecisionBlue,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Star Action
            IconButton(
                onClick = onToggleStar,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = if (event.isStarred) Icons.Default.Star else Icons.Outlined.StarBorder,
                    contentDescription = "Star",
                    tint = if (event.isStarred) StatusWarningAmber else TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Delete Action
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = TextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
