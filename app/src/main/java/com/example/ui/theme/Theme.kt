package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val StudioDarkColorScheme = darkColorScheme(
    primary = CyanAccent,
    onPrimary = DarkBackground,
    primaryContainer = CyanAccentMuted,
    onPrimaryContainer = TextPrimary,
    secondary = EmeraldAccent,
    onSecondary = DarkBackground,
    secondaryContainer = DarkSurfaceVariant,
    onSecondaryContainer = EmeraldAccent,
    tertiary = AmberAccent,
    onTertiary = DarkBackground,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    surfaceContainer = DarkSurfaceContainer,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle,
    error = RoseAccent,
    onError = TextPrimary
)

val StudioLightColorScheme = lightColorScheme(
    primary = XcodeBlue,
    onPrimary = LightSurface,
    primaryContainer = Color(0xFFE5F1FF),
    onPrimaryContainer = Color(0xFF00438F),
    secondary = XcodeTeal,
    onSecondary = LightSurface,
    secondaryContainer = Color(0xFFE6F4F5),
    onSecondaryContainer = Color(0xFF134247),
    tertiary = XcodeMagenta,
    onTertiary = LightSurface,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightSurfaceContainer,
    outline = LightBorder,
    outlineVariant = LightBorderSubtle,
    error = XcodeCrimson,
    onError = LightSurface
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) StudioDarkColorScheme else StudioLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                try {
                    val insetsController = WindowCompat.getInsetsController(window, view)
                    insetsController.isAppearanceLightStatusBars = !darkTheme
                    insetsController.isAppearanceLightNavigationBars = !darkTheme
                } catch (_: Exception) {
                    // Ignore window controller adjustment failures
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
