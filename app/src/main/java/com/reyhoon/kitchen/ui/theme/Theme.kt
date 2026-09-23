package com.reyhoon.kitchen.ui.theme

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

private val LightColorScheme = lightColorScheme(
    primary = GreenMid,
    onPrimary = SurfaceWhite,
    primaryContainer = GreenPale,
    onPrimaryContainer = GreenDark,
    secondary = OrangeSecondary,
    onSecondary = SurfaceWhite,
    secondaryContainer = Cream,
    onSecondaryContainer = Color(0xFF5D4037),
    tertiary = AmberAccent,
    background = BackgroundLight,
    surface = SurfaceCard,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    error = ErrorRed,
    outline = GreenLight.copy(alpha = 0.5f)
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = GreenDark,
    primaryContainer = GreenPrimary,
    secondary = OrangeLight,
    onSecondary = Color(0xFF3E2723),
    background = Color(0xFF121A12),
    surface = Color(0xFF1B241B),
    onBackground = Color(0xFFE8F5E9),
    onSurface = Color(0xFFE8F5E9),
    error = Color(0xFFEF9A9A)
)

@Composable
fun ReyhoonKitchenTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
