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
    onPrimary = Color.White,
    primaryContainer = GreenPale,
    onPrimaryContainer = Color(0xFF0A0A0A),
    secondary = OrangeSecondary,
    onSecondary = Color.White,
    secondaryContainer = Cream,
    onSecondaryContainer = Color(0xFF1A1A1A),
    tertiary = AmberAccent,
    onTertiary = Color(0xFF1A1A1A),
    background = BackgroundLight,
    surface = SurfaceCard,
    onBackground = Color(0xFF0A0A0A),
    onSurface = Color(0xFF0A0A0A),
    onSurfaceVariant = Color(0xFF1B2E1C),
    error = ErrorRed,
    onError = Color.White,
    outline = Color(0xFF3D5C40)
)

private val DarkColorScheme = darkColorScheme(
    primary = GreenLight,
    onPrimary = Color(0xFF0A0A0A),
    primaryContainer = GreenPrimary,
    onPrimaryContainer = Color.White,
    secondary = OrangeLight,
    onSecondary = Color(0xFF0A0A0A),
    background = Color(0xFF0E140E),
    surface = Color(0xFF1A221A),
    onBackground = Color(0xFFF5F5F5),
    onSurface = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFFE0E0E0),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF0A0A0A)
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
