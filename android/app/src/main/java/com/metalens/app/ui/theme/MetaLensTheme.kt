package com.metalens.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette aligned with Meta Wearables DAT sample apps, but kept as a light theme
// (white background/surfaces) for MetaLens.
private val MetaLensLightColorScheme =
    lightColorScheme(
        primary = Color(0xFF0064E0), // AppColor.DeepBlue
        onPrimary = Color.White,
        primaryContainer = Color(0xFFD6E4FF),
        onPrimaryContainer = Color(0xFF001B3D),

        secondary = Color(0xFF61BC63), // AppColor.Green
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFD7F4D8),
        onSecondaryContainer = Color(0xFF0B3A0D),

        tertiary = Color(0xFFFFCC00), // AppColor.Yellow
        onTertiary = Color(0xFF1A1A1A),
        tertiaryContainer = Color(0xFFFFF3C4),
        onTertiaryContainer = Color(0xFF332400),

        error = Color(0xFFFF3B30), // AppColor.Red
        onError = Color.White,
        errorContainer = Color(0xFFFFD8DB), // AppColor.DestructiveBackground
        onErrorContainer = Color(0xFFAA071E), // AppColor.DestructiveForeground

        background = Color.White,
        onBackground = Color(0xFF1A1A1A),
        surface = Color.White,
        onSurface = Color(0xFF1A1A1A),
        surfaceVariant = Color(0xFFF2F3F5),
        onSurfaceVariant = Color(0xFF5C5F66),
        outline = Color(0xFFCDD3DA),
    )

@Composable
fun MetaLensTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MetaLensLightColorScheme,
        content = content,
    )
}

