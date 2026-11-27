package com.nexus.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Mode tema yang dipilih pengguna (disimpan di Settings). */
enum class ThemeMode { SYSTEM, DARK, LIGHT }

private val DarkScheme = darkColorScheme(
    primary = NexusPrimary,
    onPrimary = NexusTextPrimary,
    primaryContainer = NexusPrimaryDeep,
    onPrimaryContainer = NexusTextPrimary,
    secondary = NexusAccent,
    onSecondary = NexusBackground,
    tertiary = NexusAccent,
    background = NexusBackground,
    onBackground = NexusTextPrimary,
    surface = NexusSurface,
    onSurface = NexusTextPrimary,
    surfaceVariant = NexusSurfaceHigh,
    onSurfaceVariant = NexusTextSecondary,
    outline = NexusOutline,
    error = Color(0xFFFF5C5C),
)

private val LightScheme = lightColorScheme(
    primary = NexusPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD8),
    secondary = NexusAccent,
    background = Color(0xFFF7F6F9),
    onBackground = Color(0xFF15141A),
    surface = Color.White,
    onSurface = Color(0xFF15141A),
    surfaceVariant = Color(0xFFEDECF1),
    onSurfaceVariant = Color(0xFF5E5C66),
    outline = Color(0xFFD0CFD6),
)

/** Hanya untuk akses ekstra (mis. memilih gaya bubble) tanpa membaca MaterialTheme. */
data class NexusExtras(val isDark: Boolean)

val LocalNexusExtras = staticCompositionLocalOf { NexusExtras(isDark = true) }

@Composable
fun NexusAITheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    // Dynamic color sengaja dimatikan agar identitas merah NEXUS tetap konsisten.
    val scheme = if (dark) DarkScheme else LightScheme

    CompositionLocalProvider(LocalNexusExtras provides NexusExtras(isDark = dark)) {
        MaterialTheme(
            colorScheme = scheme,
            typography = NexusTypography,
            shapes = NexusShapes,
            content = content,
        )
    }
}
