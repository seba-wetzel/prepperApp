package ar.nuestramerica.prepper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF5B50),
    onPrimary = Ink,
    primaryContainer = Color(0xFF68140F),
    onPrimaryContainer = Color(0xFFFFDAD5),
    secondary = Color(0xFF9BD4AE),
    secondaryContainer = Color(0xFF17482E),
    onSecondaryContainer = Color(0xFFB5F1C9),
    tertiaryContainer = Color(0xFF513B12),
    onTertiaryContainer = Color(0xFFFFE2A8),
    onSecondary = Color(0xFF07391F),
    background = Color(0xFF151412),
    onBackground = Color(0xFFF4EBDD),
    surface = Color(0xFF211F1B),
    onSurface = Color(0xFFF4EBDD),
    surfaceContainer = Color(0xFF2A2722),
    onSurfaceVariant = Color(0xFFD0C5B6),
    outline = Color(0xFF938A7F),
    outlineVariant = Color(0xFF49443E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

private val LightColorScheme = lightColorScheme(
    primary = EmergencyRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD5),
    onPrimaryContainer = Color(0xFF410001),
    secondary = FieldGreen,
    tertiaryContainer = Color(0xFFFFE2A8),
    onTertiaryContainer = Color(0xFF402C04),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB5F1C9),
    onSecondaryContainer = Color(0xFF00210D),
    background = Paper,
    onBackground = Ink,
    surface = PaperLight,
    onSurface = Ink,
    surfaceContainer = Color(0xFFE9DFCF),
    onSurfaceVariant = Graphite,
    outline = Color(0xFF756F66),
    outlineVariant = Color(0xFFCFC5B5),
    error = EmergencyRedDark,
    onError = Color.White,
)

private val PrepperShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(2.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
)

@Composable
fun PrepperAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = PrepperShapes,
        content = content,
    )
}
