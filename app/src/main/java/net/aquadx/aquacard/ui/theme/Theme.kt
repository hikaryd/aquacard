package net.aquadx.aquacard.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Fixed dark scheme — dynamic color (Material You) intentionally disabled.
private val AquaDarkColors = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentContainer,
    onPrimaryContainer = OnAccentContainer,
    secondary = Accent,
    onSecondary = OnAccent,
    secondaryContainer = AccentContainer,
    onSecondaryContainer = OnAccentContainer,
    tertiary = Accent,
    onTertiary = OnAccent,
    background = Bg,
    onBackground = TextPrimary,
    surface = SurfaceColor,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = OutlineColor,
    outlineVariant = OutlineVariantColor,
    error = ErrorColor,
    onError = OnAccent,
    errorContainer = ErrorContainerColor,
    onErrorContainer = OnErrorContainerColor,
)

@Composable
fun AquaCardTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AquaDarkColors,
        typography = AppTypography,
        content = content,
    )
}
