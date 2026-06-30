package com.band7.tool.ui

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Blue700 = Color(0xFF1976D2)
val Blue500 = Color(0xFF2196F3)
val Blue200 = Color(0xFF90CAF9)
val Orange500 = Color(0xFFFF9800)
val Green500 = Color(0xFF4CAF50)
val Red500 = Color(0xFFF44336)

val AppTheme = lightColorScheme(
    primary = Blue700,
    onPrimary = Color.White,
    primaryContainer = Blue200,
    secondary = Orange500,
    tertiary = Green500,
    error = Red500,
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
)
