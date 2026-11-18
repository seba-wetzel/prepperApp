package com.example.prepperapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFA074),
    onPrimary = Color(0xFF351300),
    primaryContainer = Color(0xFF502713),
    onPrimaryContainer = Color(0xFFFFDCCB),
    background = Color(0xFF0E1522),
    onBackground = Color(0xFFE8EDF5),
    surface = Color(0xFF172133),
    onSurface = Color(0xFFE8EDF5),
    surfaceContainer = Color(0xFF172133),
    onSurfaceVariant = Color(0xFFA6B3C8),
    outlineVariant = Color(0xFF3C4B65),
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFAA3A06),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1D1),
    onPrimaryContainer = Color(0xFF462009),
    background = Color(0xFFEEF1F4),
    onBackground = Color(0xFF14213D),
    surface = Color.White,
    onSurface = Color(0xFF14213D),
    surfaceContainer = Color.White,
    onSurfaceVariant = Color(0xFF536078),
    outlineVariant = Color(0xFFD9DFE7),
)

@Composable
fun PrepperAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
