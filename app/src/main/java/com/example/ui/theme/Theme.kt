package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = BroadcastCyan,
    onPrimary = OnBroadcastCyan,
    primaryContainer = BroadcastCyanContainer,
    onPrimaryContainer = TextPrimary,
    secondary = GoldStar,
    onSecondary = ObsidianVoid,
    secondaryContainer = SlateHighlight,
    onSecondaryContainer = TextPrimary,
    tertiary = SignalCoral,
    onTertiary = TextPrimary,
    tertiaryContainer = SignalCoralContainer,
    onTertiaryContainer = TextPrimary,
    background = ObsidianVoid,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = SlateElevated,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
    error = SignalCoral,
    onError = TextPrimary
)

private val LightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightSurface,
    primaryContainer = BroadcastCyan.copy(alpha = 0.16f),
    onPrimaryContainer = LightTextPrimary,
    secondary = GoldStar,
    onSecondary = LightTextPrimary,
    tertiary = SignalCoral,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightTextSecondary.copy(alpha = 0.25f),
    error = SignalCoral
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun SatViewTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AppShapes,
        content = content
    )
}
