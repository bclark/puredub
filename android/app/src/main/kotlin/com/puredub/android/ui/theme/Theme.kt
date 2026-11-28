package com.puredub.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Dark = darkColorScheme(
    primary = Color(0xFFF3B66C), onPrimary = Color(0xFF35200A),
    primaryContainer = Color(0xFF52391E), onPrimaryContainer = Color(0xFFFFDDB8),
    secondary = Color(0xFF61D9DD), onSecondary = Color(0xFF00373A),
    secondaryContainer = Color(0xFF164C51), onSecondaryContainer = Color(0xFFAEF3F4),
    background = Color(0xFF101820), onBackground = Color(0xFFE6EEF2),
    surface = Color(0xFF101820), onSurface = Color(0xFFE6EEF2),
    surfaceVariant = Color(0xFF23303D), onSurfaceVariant = Color(0xFFBDCBD4),
    surfaceContainer = Color(0xFF19232D), surfaceContainerHigh = Color(0xFF23303D),
    outline = Color(0xFF7E929F), outlineVariant = Color(0xFF3A4B58)
)
private val Light = lightColorScheme(
    primary = Color(0xFF795019), onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB8), onPrimaryContainer = Color(0xFF2B1700),
    secondary = Color(0xFF00666D), onSecondary = Color.White,
    secondaryContainer = Color(0xFFA5EFF2), onSecondaryContainer = Color(0xFF002023),
    background = Color(0xFFF5F8FA), onBackground = Color(0xFF14212A),
    surface = Color(0xFFF5F8FA), onSurface = Color(0xFF14212A),
    surfaceVariant = Color(0xFFDDE6EC), onSurfaceVariant = Color(0xFF3A4B58),
    surfaceContainer = Color(0xFFE9EFF3), surfaceContainerHigh = Color(0xFFE0E8EE),
    outline = Color(0xFF627580), outlineVariant = Color(0xFFB9C8D1)
)

@Composable
fun PureDubTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, typography = Typography, content = content)
}
