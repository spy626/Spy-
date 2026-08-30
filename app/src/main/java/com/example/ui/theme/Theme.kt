package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LyraDarkColorScheme = darkColorScheme(
    primary = LyraCyan,
    onPrimary = LyraObsidian,
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = LyraCyan,
    secondary = LyraViolet,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF4C1D95),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = LyraPink,
    onTertiary = Color.White,
    background = LyraObsidian,
    onBackground = LyraTextPrimary,
    surface = LyraSurfaceDark,
    onSurface = LyraTextPrimary,
    surfaceVariant = LyraSurfaceCard,
    onSurfaceVariant = LyraTextSecondary,
    error = LyraError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LyraDarkColorScheme,
        typography = Typography,
        content = content
    )
}
