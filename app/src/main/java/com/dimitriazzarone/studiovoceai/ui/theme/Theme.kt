package com.dimitriazzarone.studiovoceai.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val StudioVoceDarkColors = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = DeepNavy,
    primaryContainer = Color(0xFF123A49),
    onPrimaryContainer = CyanLight,
    background = DeepNavy,
    onBackground = SoftWhite,
    surface = NightPanel,
    onSurface = SoftWhite,
    surfaceVariant = Color(0xFF14243A),
    onSurfaceVariant = MutedBlue,
    secondary = Color(0xFF45D78A),
    tertiary = Color(0xFFFFD166),
    error = Color(0xFFFF6B5E)
)

@Composable
fun StudioVoceAITheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as Activity).window
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
    }

    MaterialTheme(
        colorScheme = StudioVoceDarkColors,
        typography = StudioVoceTypography,
        content = content
    )
}
