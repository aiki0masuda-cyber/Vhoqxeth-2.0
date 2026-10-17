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

private val MinimalistPrecisionScheme = darkColorScheme(
    primary = DavAccentSky,
    onPrimary = Color.Black,
    primaryContainer = DavSurfaceElevated,
    onPrimaryContainer = DavAccentSky,
    secondary = DavAccentIndigo,
    onSecondary = Color.White,
    secondaryContainer = DavSurfaceCard,
    onSecondaryContainer = Color(0xFFA5B4FC),
    tertiary = DavAccentGold,
    onTertiary = Color.Black,
    background = DavCanvasDark,
    onBackground = DavTextPrimary,
    surface = DavSurfaceCard,
    onSurface = DavTextPrimary,
    surfaceVariant = DavSurfaceElevated,
    onSurfaceVariant = DavTextSecondary,
    outline = DavBorder
)

@Composable
fun DavInfoExchangeTheme(
    darkTheme: Boolean = true, // Minimalist Precision defaults to focused dark slate canvas
    content: @Composable () -> Unit
) {
    val colorScheme = MinimalistPrecisionScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.background.toArgb()
                window.navigationBarColor = colorScheme.background.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun DavCheekaTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) = DavInfoExchangeTheme(darkTheme = darkTheme, content = content)

@Composable
fun VhoqxethTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) = DavInfoExchangeTheme(darkTheme = darkTheme, content = content)
