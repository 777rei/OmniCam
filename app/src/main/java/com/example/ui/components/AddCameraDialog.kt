package com.example.ui.components

/**
 * OmniCam Add Camera & Hardware Node Modal Dialog
 * Created by Rei (777rei / reialwaysfreemium)
 * Privacy-First Security Architecture
 */

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.ConsoleBase
import com.example.ui.theme.ConsoleBorder
import com.example.ui.theme.ConsoleSurface
import com.example.ui.theme.PrecisionBlue
import com.example.ui.theme.StatusLiveEmerald
import com.example.ui.theme.StatusWarningAmber
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.CctvViewModel

@Composable
fun AddCameraDialog(
    viewModel: CctvViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Preset Zones, 1: Scan IP, 2: USB, 3: Direct RTSP, 4: Phone
    val isScanning by viewModel.isScanningNetwork.collectAsState()
    val scanProgress by viewModel.scanProgress.collectAsState()
    val discoveredCameras by viewModel.discoveredCameras.collectAsState()

    var customName by remember { mutableStateOf("") }
    var customUrl by remember { mutableStateOf("") }
    var customLocation by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.scanUsbCameras()
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = ConsoleSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ConsoleBorder),
            shadowElevation = 16.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
                .testTag("add_camera_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(6.dp).background(PrecisionBlue, CircleShape))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ADD CCTV CAMERA NODE",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 0.5.sp
                            )
                        }
                        Text(
                            text = "Preset Zones, Wi-Fi Scanner, USB UVC, or RTSP Stream",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tabs Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ConsoleBase)
                        .border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    TabPill("Zones", icon = Icons.Default.GridOn, isSelected = selectedTab == 0, modifier = Modifier.weight(1f)) { selectedTab = 0 }
                    TabPill("Scan IP", icon = Icons.Default.Lan, isSelected = selectedTab == 1, modifier = Modifier.weight(1f)) { selectedTab = 1 }
                    TabPill("USB", icon = Icons.Default.Usb, isSelected = selectedTab == 2, modifier = Modifier.weight(1f)) { selectedTab = 2 }
                    TabPill("RTSP", icon = Icons.Default.Videocam, isSelected = selectedTab == 3, modifier = Modifier.weight(1f)) { selectedTab = 3 }
                    TabPill("Phone", icon = Icons.Default.CameraAlt, isSelected = selectedTab == 4, modifier = Modifier.weight(1f)) { selectedTab = 4 }
                }

                Spacer(modifier = Modifier.height(12.dp))

                when (selectedTab) {
                    0 -> { // Preset Security Zones
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "INSTANT PRESET SURVEILLANCE NODES",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            PresetZoneCard(
                                title = "Main Entrance & Lobby",
                                location = "Corridor CH-01",
                                desc = "Security gateway doorway with 1080p motion radar",
                                onClick = {
                                    viewModel.addCustomCamera(
                                        name = "Main Entrance & Lobby",
                                        streamUrl = "rtsp://cctv.local:554/ch1",
                                        location = "Front Corridor",
                                        resolution = "1080p FHD"
                                    )
                                    onDismiss()
                                }
                            )

                            PresetZoneCard(
                                title = "Perimeter & Driveway",
                                location = "Exterior CH-02",
                                desc = "Fence line & asphalt driveway perimeter lighting",
                                onClick = {
                                    viewModel.addCustomCamera(
                                        name = "Perimeter & Driveway",
                                        streamUrl = "rtsp://cctv.local:554/ch2",
                                        location = "Driveway Gate",
                                        resolution = "1080p FHD"
                                    )
                                    onDismiss()
                                }
                            )

                            PresetZoneCard(
                                title = "Server Room & Vault",
                                location = "Facility CH-03",
                                desc = "Critical server rack array with environmental telemetry",
                                onClick = {
                                    viewModel.addCustomCamera(
                                        name = "Server Room & Vault",
                                        streamUrl = "rtsp://cctv.local:554/ch3",
                                        location = "Data Center",
                                        resolution = "1080p FHD"
                                    )
                                    onDismiss()
                                }
                            )

                            PresetZoneCard(
                                title = "Warehouse & Loading Dock",
                                location = "Industrial CH-04",
                                desc = "Roll-up industrial door & logistics loading zone",
                                onClick = {
                                    viewModel.addCustomCamera(
                                        name = "Warehouse Loading Dock",
                                        streamUrl = "rtsp://cctv.local:554/ch4",
                                        location = "Shipping Bay",
                                        resolution = "1080p FHD"
                                    )
                                    onDismiss()
                                }
                            )
                        }
                    }

                    1 -> { // Subnet CCTV Scanner
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isScanning) "SCANNING WI-FI (${(scanProgress * 100).toInt()}%)..." else "DETECTED LOCAL CCTV NODES",
                                    color = if (isScanning) PrecisionBlue else TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { viewModel.startNetworkScan() },
                                    enabled = !isScanning,
                                    colors = ButtonDefaults.buttonColors(containerColor = PrecisionBlue),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    if (isScanning) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(12.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Scan", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                    }
                                }
                            }

                            val networkCams = discoveredCameras.filter { it.protocol in listOf("RTSP", "MJPEG", "ONVIF", "HTTP") }
                            if (networkCams.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .background(ConsoleBase, RoundedCornerShape(8.dp))
                                        .border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = if (isScanning) "Probing local network ports 554, 80, 8080..." else "No Wi-Fi CCTV cameras detected yet.",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap 'Scan' or use 'RTSP' tab to add your camera's IP.",
                                            color = TextSecondary,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(networkCams, key = { it.id }) { item ->
                                        DiscoveredCameraRow(item = item) {
                                            viewModel.addDiscoveredCamera(item)
                                            onDismiss()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> { // USB Camera
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "CONNECTED USB / UVC CAMERAS",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Button(
                                    onClick = { viewModel.scanUsbCameras() },
                                    colors = ButtonDefaults.buttonColors(containerColor = PrecisionBlue),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Refresh", fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                                }
                            }

                            val usbCams = discoveredCameras.filter { it.protocol == "USB/UVC" }
                            if (usbCams.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(130.dp)
                                        .background(ConsoleBase, RoundedCornerShape(8.dp))
                                        .border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = "No USB camera currently detected.",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Button(
                                            onClick = {
                                                viewModel.addCustomCamera(
                                                    name = "USB External Webcam",
                                                    streamUrl = "usb://uvc/0",
                                                    location = "USB Port",
                                                    resolution = "1080p FHD"
                                                )
                                                onDismiss()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2638)),
                                            shape = RoundedCornerShape(6.dp),
                                            modifier = Modifier.height(30.dp)
                                        ) {
                                            Text("+ Add Generic USB Node", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = PrecisionBlue)
                                        }
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(usbCams, key = { it.id }) { item ->
                                        DiscoveredCameraRow(item = item) {
                                            viewModel.addDiscoveredCamera(item)
                                            onDismiss()
                                        }
                                    }
                                }
                            }
                        }
                    }

                    3 -> { // Direct RTSP Input
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = customName,
                                onValueChange = { customName = it },
                                label = { Text("Camera Name (e.g. Back Garden)", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrecisionBlue,
                                    unfocusedBorderColor = ConsoleBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = customUrl,
                                onValueChange = { customUrl = it },
                                label = { Text("RTSP / MJPEG Stream URL", fontSize = 11.sp) },
                                placeholder = { Text("rtsp://192.168.1.100:554/live", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrecisionBlue,
                                    unfocusedBorderColor = ConsoleBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = customLocation,
                                onValueChange = { customLocation = it },
                                label = { Text("Location Tag (e.g. Backyard)", fontSize = 11.sp) },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrecisionBlue,
                                    unfocusedBorderColor = ConsoleBorder,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    viewModel.addCustomCamera(
                                        name = customName.ifBlank { "RTSP CCTV Node" },
                                        streamUrl = customUrl.ifBlank { "rtsp://192.168.1.100:554/live" },
                                        location = customLocation.ifBlank { "Security Zone" }
                                    )
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrecisionBlue),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(38.dp)
                            ) {
                                Text("Connect RTSP Camera", fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                            }
                        }
                    }

                    4 -> { // Phone Sensors
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "SELECT PHONE CAMERA SENSOR",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )

                            PhonePresetCard(
                                title = "Phone Main Rear Camera",
                                desc = "1080p FHD • Hardware Sensor • Armed Motion Detection",
                                onClick = {
                                    viewModel.addCustomCamera(
                                        name = "Phone Rear Main Lens",
                                        streamUrl = "http://localhost:8080/live",
                                        location = "Primary Station",
                                        resolution = "1080p FHD"
                                    )
                                    onDismiss()
                                }
                            )

                            PhonePresetCard(
                                title = "Phone Front Wide Lens",
                                desc = "720p HD • Front Facing Sensor • Intercom Mode",
                                onClick = {
                                    viewModel.addCustomCamera(
                                        name = "Phone Front Lens",
                                        streamUrl = "http://localhost:8080/live",
                                        location = "Interior Console",
                                        resolution = "720p HD"
                                    )
                                    onDismiss()
                                }
                            )

                            PhonePresetCard(
                                title = "Remote OmniCam Phone Link",
                                desc = "Connect to another phone running OmniCam on Wi-Fi",
                                onClick = {
                                    viewModel.addCustomCamera(
                                        name = "Paired OmniCam Phone",
                                        streamUrl = "http://192.168.1.150:8080/live",
                                        location = "Remote Station",
                                        resolution = "1080p FHD"
                                    )
                                    onDismiss()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetZoneCard(
    title: String,
    location: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF141824))
            .border(1.dp, ConsoleBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .background(Color(0xFF1E2638), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Videocam, contentDescription = null, tint = PrecisionBlue, modifier = Modifier.size(18.dp))
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = location, color = StatusLiveEmerald, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
            }
            Text(text = desc, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }

        Icon(Icons.Default.Add, contentDescription = "Add", tint = PrecisionBlue, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun TabPill(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) PrecisionBlue else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = if (isSelected) Color.White else TextMuted, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                color = if (isSelected) Color.White else TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
private fun DiscoveredCameraRow(
    item: com.example.engine.discovery.DiscoveredCamera,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF141824))
            .border(1.dp, ConsoleBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onAdd)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = item.name, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(text = "${item.ip}:${item.port} • ${item.protocol}", color = TextSecondary, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
        Button(
            onClick = onAdd,
            colors = ButtonDefaults.buttonColors(containerColor = PrecisionBlue),
            shape = RoundedCornerShape(4.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp),
            modifier = Modifier.height(24.dp)
        ) {
            Text("+ Add", fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun PhonePresetCard(
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF141824))
            .border(1.dp, ConsoleBorder, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CameraAlt, contentDescription = null, tint = PrecisionBlue, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
            Text(text = desc, color = TextMuted, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
        }
        Icon(Icons.Default.Add, contentDescription = "Select", tint = PrecisionBlue, modifier = Modifier.size(16.dp))
    }
}
