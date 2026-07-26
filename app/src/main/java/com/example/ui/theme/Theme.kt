package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val ElegantDarkColorScheme =
    darkColorScheme(
        primary = Color(0xFFD0BCFF),
        onPrimary = Color(0xFF381E72),
        primaryContainer = Color(0xFF4F378B),
        onPrimaryContainer = Color(0xFFEADDFF),
        secondary = Color(0xFFCCC2DC),
        onSecondary = Color(0xFF332D41),
        secondaryContainer = Color(0xFF4A4458),
        onSecondaryContainer = Color(0xFFE8DEF8),
        background = Color(0xFF121212),
        onBackground = Color(0xFFE6E1E5),
        surface = Color(0xFF121212),
        onSurface = Color(0xFFE6E1E5),
        surfaceVariant = Color(0xFF49454F),
        onSurfaceVariant = Color(0xFFCAC4D0),
        surfaceContainer = Color(0xFF1C1B1F),
        error = Color(0xFFF2B8B5),
        onError = Color(0xFF601410),
        outline = Color(0xFF938F99)
    )

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark theme as requested
    dynamicColor: Boolean = false, // Disable dynamic colors to enforce our elegant dark palette
    content: @Composable () -> Unit,
) {
    val colorScheme = ElegantDarkColorScheme

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
