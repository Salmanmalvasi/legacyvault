package com.example.legacyvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = SlateNavy,
    onPrimary = Color.White,
    primaryContainer = SlateNavyLight,
    onPrimaryContainer = Color.White,
    secondary = HeritageTeal,
    onSecondary = Color.White,
    tertiary = ForestGreen,
    background = WarmCream,
    surface = CardBackground,
    onBackground = DeepCharcoal,
    onSurface = DeepCharcoal,
    outline = SoftBorder
)

@Composable
fun LegacyvaultTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}