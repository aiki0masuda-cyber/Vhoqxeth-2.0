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

private val LightColorScheme = lightColorScheme(
    primary = VhoqxethIndigo600,
    onPrimary = Color.White,
    primaryContainer = VhoqxethCardLightAlt,
    onPrimaryContainer = VhoqxethIndigo700,
    secondary = VhoqxethCobalt600,
    onSecondary = Color.White,
    secondaryContainer = VhoqxethCardLightAlt,
    onSecondaryContainer = VhoqxethCobalt600,
    tertiary = VhoqxethRose600,
    onTertiary = Color.White,
    background = VhoqxethCanvasLight,
    onBackground = VhoqxethSlate900,
    surface = VhoqxethCardLight,
    onSurface = VhoqxethSlate900,
    surfaceVariant = VhoqxethCardLightAlt,
    onSurfaceVariant = VhoqxethSlate600,
    outline = VhoqxethBorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = VhoqxethIndigo500,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E293B),
    onPrimaryContainer = Color(0xFFA5B4FC),
    secondary = VhoqxethCobalt500,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFF93C5FD),
    tertiary = Color(0xFFFB7185),
    onTertiary = Color.White,
    background = Color(0xFF0F172A),
    onBackground = Color(0xFFF8FAFC),
    surface = Color(0xFF1E293B),
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF475569)
)

@Composable
fun VhoqxethTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                // Light theme requires dark status and nav bar icons
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
