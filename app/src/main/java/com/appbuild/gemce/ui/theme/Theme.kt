package com.appbuild.gemce.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.appbuild.gemce.domain.settings.FontPreference
import com.appbuild.gemce.domain.settings.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF2DD4BF),           // Crisp modern teal
    onPrimary = Color(0xFF003830),
    primaryContainer = Color(0xFF0F4D44),
    onPrimaryContainer = Color(0xFF99F6E4),
    secondary = Color(0xFF818CF8),         // Soft Indigo
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF312E81),
    onSecondaryContainer = Color(0xFFC7D2FE),
    tertiary = Color(0xFF38BDF8),          // Sky Blue
    background = Color(0xFF090D16),        // Deep Slate
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF0F172A),           // Slate 900
    onSurface = Color(0xFFF1F5F9),
    surfaceVariant = Color(0xFF1E293B),    // Slate 800
    onSurfaceVariant = Color(0xFF94A3B8),
    surfaceContainerHigh = Color(0xFF24334A),
    outline = Color(0xFF334155),
    error = Color(0xFFF87171)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),           // Academic Teal 700
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFCCFBF1),  // Teal 100
    onPrimaryContainer = Color(0xFF115E59),
    secondary = Color(0xFF4338CA),         // Indigo 700
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE0E7FF),
    onSecondaryContainer = Color(0xFF3730A3),
    tertiary = Color(0xFF0284C7),          // Sky 600
    background = Color(0xFFF8FAFC),        // Pure Slate 50
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFFFFFFF),           // Pure White
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),    // Slate 100
    onSurfaceVariant = Color(0xFF475569),
    surfaceContainerHigh = Color(0xFFE2E8F0),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFDC2626)
)

@Composable
fun GemceTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    fontPreference: FontPreference = FontPreference.SYSTEM,
    content: @Composable () -> Unit
) {
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }

    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    val family = getFontFamily(fontPreference)
    val typography = createAppTypography(fontPreference)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography
    ) {
        // Guarantee that EVERY Text composable throughout the entire app inherits the chosen font
        CompositionLocalProvider(
            LocalTextStyle provides TextStyle(fontFamily = family)
        ) {
            ProvideTextStyle(value = TextStyle(fontFamily = family)) {
                content()
            }
        }
    }
}
