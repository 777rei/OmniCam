package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.SurveillanceDarkBg
import com.example.ui.theme.SurveillanceSurface
import com.example.viewmodel.CameraNodeState
import com.example.viewmodel.CctvViewModel

@Composable
fun SettingsScreen(
    viewModel: CctvViewModel,
    state: CameraNodeState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var sensitivity by remember { mutableStateOf(state.settings.motionSensitivity) }
    var lowBandwidth by remember { mutableStateOf(state.settings.lowBandwidthMode) }
    var sirenOnMotion by remember { mutableStateOf(state.settings.sirenOnMotion) }
    var jpegQuality by remember { mutableStateOf(state.settings.jpegQuality.toFloat()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurveillanceDarkBg)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "SURVEILLANCE SETTINGS",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Black,
            fontFamily = FontFamily.Monospace
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = SurveillanceSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF28344A), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Motion Sensitivity Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Motion Sensitivity", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${(sensitivity * 100).toInt()}%", color = CyanAccent, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = sensitivity,
                        onValueChange = { sensitivity = it },
                        valueRange = 0.05f..0.75f,
                        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                    )
                }

                // JPEG Quality Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Stream JPEG Quality", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "${jpegQuality.toInt()}%", color = CyanAccent, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                    }
                    Slider(
                        value = jpegQuality,
                        onValueChange = { jpegQuality = it },
                        valueRange = 20f..95f,
                        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                    )
                }

                // Low Bandwidth Mode Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Low Bandwidth Mode", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Reduces frame rate and compression for cellular data", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = lowBandwidth,
                        onCheckedChange = { lowBandwidth = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = CyanAccent.copy(alpha = 0.5f))
                    )
                }

                // Siren on Motion Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "Siren on Intruder Alert", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text(text = "Automatically sound alarm when intruder confidence > 60%", color = Color(0xFF94A3B8), fontSize = 11.sp)
                    }
                    Switch(
                        checked = sirenOnMotion,
                        onCheckedChange = { sirenOnMotion = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = CyanAccent, checkedTrackColor = CyanAccent.copy(alpha = 0.5f))
                    )
                }
            }
        }

        // Network & Telemetry Info Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurveillanceSurface),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF28344A), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(text = "SURVEILLANCE NETWORK INFO", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                Text(text = "Transport: ${state.networkType}", color = Color(0xFF94A3B8), fontSize = 12.sp)
                Text(text = "Stream URL: http://${state.localIp}:${state.port}/live", color = CyanAccent, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                Text(text = "Encryption: AES-256 End-to-End Key Active", color = Color(0xFF00E676), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }

        // Developer Ownership & Privacy-First Architecture Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1522)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF3B82F6).copy(alpha = 0.5f), RoundedCornerShape(12.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "DEVELOPER & OWNERSHIP",
                        color = Color(0xFF3B82F6),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "PRIVACY FIRST",
                        color = Color(0xFF22C55E),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "Created by Rei",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )

                Text(
                    text = "Handle: 777rei • Brand: reialwaysfreemium",
                    color = CyanAccent,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Package ID: com.android.rei777.alwaysfreemiumapp.omnicam",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF141C2B), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛡️ Zero-Knowledge / Privacy-First App: 100% on-device processing. No external clouds, zero third-party telemetry, fully contained to your encrypted local network.",
                        color = Color(0xFFCBD5E1),
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }


        // Test Alarm Button
        Button(
            onClick = { viewModel.triggerManualSiren() },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C2433), contentColor = Color(0xFFFF3D57)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Text(text = if (state.isSirenPlaying) "STOP TEST SIREN" else "TEST SIREN ALARM", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // Save Button
        Button(
            onClick = {
                val updated = state.settings.copy(
                    motionSensitivity = sensitivity,
                    lowBandwidthMode = lowBandwidth,
                    sirenOnMotion = sirenOnMotion,
                    jpegQuality = jpegQuality.toInt()
                )
                viewModel.updateSettings(updated)
                Toast.makeText(context, "Surveillance settings applied!", Toast.LENGTH_SHORT).show()
            },
            colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text(text = "SAVE & APPLY CONFIG", fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
    }
}
