package com.example.ecoswap.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = EcoGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = EcoGreenContainer,
    onPrimaryContainer = EcoGreenDark,
    secondary = Color(0xFF43A047),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE8F5E9),
    onSecondaryContainer = EcoGreenDark,
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    background = EcoGreenBackground,
    onBackground = Color(0xFF1C2B1C),
    surface = EcoGreenSurface,
    onSurface = Color(0xFF1C2B1C),
    surfaceVariant = Color(0xFFE8F5E9),
    onSurfaceVariant = Color(0xFF3E5A3E),
    error = Color(0xFFC62828),
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = EcoGreenDarkPrimary,
    onPrimary = EcoGreenDark,
    primaryContainer = Color(0xFF2E5A32),
    onPrimaryContainer = Color(0xFFC8E6C9),
    secondary = Color(0xFF66BB6A),
    onSecondary = EcoGreenDark,
    background = EcoGreenDarkBackground,
    onBackground = Color(0xFFE8F5E9),
    surface = EcoGreenDarkSurface,
    onSurface = Color(0xFFE8F5E9),
    surfaceVariant = Color(0xFF2A3D2A),
    onSurfaceVariant = Color(0xFFB9D4BA)
)

@Composable
fun EcoSwapTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = EcoShapes,
        content = content
    )
}
