package com.reyhoon.kitchen.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** همیشه تم روشن — خوانایی بهتر در نور روز */
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

@Composable
fun ReyhoonKitchenTheme(
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
