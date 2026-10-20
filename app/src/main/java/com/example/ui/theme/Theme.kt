package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SecurityConsoleColorScheme = darkColorScheme(
    primary = PrecisionBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E3A8A),
    onPrimaryContainer = Color(0xFFDBEAFE),
    secondary = StatusLiveEmerald,
    onSecondary = Color(0xFF022C11),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = StatusWarningAmber,
    error = StatusAlertRed,
    background = ConsoleBase,
    onBackground = TextPrimary,
    surface = ConsoleSurface,
    onSurface = TextPrimary,
    surfaceVariant = ConsoleSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = ConsoleBorder
)

@Composable
fun MyApplicationTheme(
    // Enforce strict Dark Mode Only for Technical Security Console
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SecurityConsoleColorScheme,
        typography = Typography,
        content = content
    )
}
