package com.cartracker.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = TrueBlack,
    primaryContainer = NeonCyanGlow,
    onPrimaryContainer = NeonCyan,
    secondary = OnSurfaceSecondary,
    onSecondary = TrueBlack,
    secondaryContainer = SurfaceContainerHigh,
    onSecondaryContainer = OnSurfacePrimary,
    tertiary = SuccessGreen,
    onTertiary = TrueBlack,
    background = TrueBlack,
    onBackground = OnSurfacePrimary,
    surface = SurfaceContainer,
    onSurface = OnSurfacePrimary,
    surfaceVariant = SurfaceContainerHigh,
    onSurfaceVariant = OnSurfaceSecondary,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    surfaceContainerHighest = SurfaceContainerHighest,
    outline = GlassBorder,
    outlineVariant = SurfaceContainerHigh,
    error = ErrorRed,
    onError = TrueBlack,
    errorContainer = ErrorRedGlow,
    onErrorContainer = ErrorRed,
    scrim = Color(0xCC000000),
    inverseSurface = OnSurfacePrimary,
    inverseOnSurface = TrueBlack,
    inversePrimary = NeonCyanDim,
)

@Composable
fun CarTrackerTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
