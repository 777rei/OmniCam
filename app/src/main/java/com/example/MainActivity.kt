package com.example

/**
 * OmniCam Security & Surveillance Console
 * Created by Rei (777rei / reialwaysfreemium)
 * App Package: com.android.rei777.alwaysfreemiumapp.omnicam
 * Privacy First App - 100% On-Device & Direct Local Network Transport
 */

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.CameraGridScreen
import com.example.ui.screens.CameraNodeScreen
import com.example.ui.screens.EventLogScreen
import com.example.ui.screens.LiveFeedDetailScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.ConsoleBase
import com.example.ui.theme.ConsoleBorder
import com.example.ui.theme.ConsoleSurface
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.PrecisionBlue
import com.example.ui.theme.TextMuted
import com.example.viewmodel.CctvAppMode
import com.example.viewmodel.CctvViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val cctvViewModel: CctvViewModel = viewModel()
                val currentMode by cctvViewModel.currentMode.collectAsState()
                val cameraNodeState by cctvViewModel.cameraNodeState.collectAsState()
                val viewerState by cctvViewModel.viewerState.collectAsState()
                val cameras by cctvViewModel.cameras.collectAsState()
                val motionEvents by cctvViewModel.motionEvents.collectAsState()
                val currentFrame by cctvViewModel.currentFrame.collectAsState()

                BackHandler(enabled = currentMode != CctvAppMode.DASHBOARD) {
                    cctvViewModel.setAppMode(CctvAppMode.DASHBOARD)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = ConsoleBase,
                    bottomBar = {
                        NavigationBar(
                            containerColor = ConsoleSurface,
                            contentColor = Color.White
                        ) {
                            NavigationBarItem(
                                selected = currentMode == CctvAppMode.DASHBOARD,
                                onClick = { cctvViewModel.setAppMode(CctvAppMode.DASHBOARD) },
                                icon = { Icon(Icons.Default.GridView, contentDescription = "Camera Grid") },
                                label = { Text("Grid", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrecisionBlue,
                                    unselectedIconColor = TextMuted,
                                    selectedTextColor = PrecisionBlue,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = Color(0xFF1E2638)
                                )
                            )
                            NavigationBarItem(
                                selected = currentMode == CctvAppMode.LIVE_DETAIL,
                                onClick = { cctvViewModel.setAppMode(CctvAppMode.LIVE_DETAIL) },
                                icon = { Icon(Icons.Default.Videocam, contentDescription = "Live Console") },
                                label = { Text("Stream", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrecisionBlue,
                                    unselectedIconColor = TextMuted,
                                    selectedTextColor = PrecisionBlue,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = Color(0xFF1E2638)
                                )
                            )
                            NavigationBarItem(
                                selected = currentMode == CctvAppMode.CAMERA_NODE,
                                onClick = { cctvViewModel.setAppMode(CctvAppMode.CAMERA_NODE) },
                                icon = { Icon(Icons.Default.CameraAlt, contentDescription = "Camera Node") },
                                label = { Text("Node", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrecisionBlue,
                                    unselectedIconColor = TextMuted,
                                    selectedTextColor = PrecisionBlue,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = Color(0xFF1E2638)
                                )
                            )
                            NavigationBarItem(
                                selected = currentMode == CctvAppMode.ALERTS_ARCHIVE,
                                onClick = { cctvViewModel.setAppMode(CctvAppMode.ALERTS_ARCHIVE) },
                                icon = { Icon(Icons.Default.NotificationsActive, contentDescription = "Logs") },
                                label = { Text("Logs", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrecisionBlue,
                                    unselectedIconColor = TextMuted,
                                    selectedTextColor = PrecisionBlue,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = Color(0xFF1E2638)
                                )
                            )
                            NavigationBarItem(
                                selected = currentMode == CctvAppMode.SETTINGS,
                                onClick = { cctvViewModel.setAppMode(CctvAppMode.SETTINGS) },
                                icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                                label = { Text("Config", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = PrecisionBlue,
                                    unselectedIconColor = TextMuted,
                                    selectedTextColor = PrecisionBlue,
                                    unselectedTextColor = TextMuted,
                                    indicatorColor = Color(0xFF1E2638)
                                )
                            )
                        }
                    }
                ) { innerPadding ->
                    val modifier = Modifier.padding(innerPadding)
                    when (currentMode) {
                        CctvAppMode.DASHBOARD -> CameraGridScreen(
                            viewModel = cctvViewModel,
                            cameras = cameras,
                            frame = currentFrame,
                            onOpenStreamDetail = { camId ->
                                cctvViewModel.selectCameraChannel(camId)
                                cctvViewModel.setAppMode(CctvAppMode.LIVE_DETAIL)
                            },
                            modifier = modifier
                        )
                        CctvAppMode.LIVE_DETAIL, CctvAppMode.VIEWER_CONSOLE -> {
                            val activeCam = cameras.find { it.id == viewerState.selectedCameraId } ?: cameras.firstOrNull()
                            LiveFeedDetailScreen(
                                viewModel = cctvViewModel,
                                state = viewerState,
                                activeCamera = activeCam,
                                frame = currentFrame,
                                motionEvents = motionEvents,
                                onBack = { cctvViewModel.setAppMode(CctvAppMode.DASHBOARD) },
                                modifier = modifier
                            )
                        }
                        CctvAppMode.CAMERA_NODE -> CameraNodeScreen(
                            viewModel = cctvViewModel,
                            state = cameraNodeState,
                            frame = currentFrame,
                            modifier = modifier
                        )
                        CctvAppMode.ALERTS_ARCHIVE -> EventLogScreen(
                            viewModel = cctvViewModel,
                            motionEvents = motionEvents,
                            modifier = modifier
                        )
                        CctvAppMode.SETTINGS -> SettingsScreen(
                            viewModel = cctvViewModel,
                            state = cameraNodeState,
                            modifier = modifier
                        )
                    }
                }
            }
        }
    }
}
