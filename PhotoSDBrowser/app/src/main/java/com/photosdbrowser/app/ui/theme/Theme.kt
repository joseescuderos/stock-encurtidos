package com.photosdbrowser.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JoseEscuderosLightColors = lightColorScheme(
    primary = BrandWine,
    onPrimary = LightBackground,
    background = LightBackground,
    onBackground = OnLightPrimary,
    surface = LightSurface,
    onSurface = OnLightPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnLightSecondary,
    secondary = BrandGold,
    onSecondary = OnLightPrimary
)

/**
 * Light theme matching the "joseescuderos" brand identity (white background, warm
 * wine-to-gold accents) used across all three screens.
 */
@Composable
fun PhotoSDBrowserTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = LightBackground.toArgb()
            window.navigationBarColor = LightBackground.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = true
            controller.isAppearanceLightNavigationBars = true
        }
    }

    MaterialTheme(
        colorScheme = JoseEscuderosLightColors,
        typography = Typography,
        content = content
    )
}
