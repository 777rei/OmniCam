package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Technical Utilitarian / Modern Security Console Tokens
val ConsoleBase = Color(0xFF0A0C10)           // Deep obsidian background
val ConsoleSurface = Color(0xFF141822)        // Subtle dark slate container
val ConsoleSurfaceVariant = Color(0xFF19202D) // Elevated HUD / controls
val ConsoleBorder = Color(0x14FFFFFF)         // 1px subtle rgba(255, 255, 255, 0.08)
val ConsoleBorderStrong = Color(0xFF263142)

// Accent & Status Indicators
val PrecisionBlue = Color(0xFF3B82F6)         // Accent Interactive
val StatusLiveEmerald = Color(0xFF22C55E)     // Live / Online stream
val StatusWarningAmber = Color(0xFFF59E0B)    // Motion Event / Warning / Vehicle
val StatusAlertRed = Color(0xFFEF4444)        // Critical Alert / Loss / Alarm
val StatusOfflineGray = Color(0xFF6B7280)     // Offline Stream / Muted
val TargetBoundingCyan = Color(0xFF00F0FF)    // Target Detection / Human

// Typography Colors
val TextPrimary = Color(0xFFF9FAFB)
val TextSecondary = Color(0xFF9CA3AF)
val TextMuted = Color(0xFF4B5563)

// Backward Compatibility Aliases
val SurveillanceDarkBg = ConsoleBase
val SurveillanceSurface = ConsoleSurface
val SurveillanceSurfaceVariant = ConsoleSurfaceVariant
val SurveillanceBorder = ConsoleBorder
val CyanAccent = PrecisionBlue
val LiveGreen = StatusLiveEmerald
val AlertRed = StatusAlertRed
val WarningOrange = StatusWarningAmber
val StealthOledBlack = Color(0xFF000000)
