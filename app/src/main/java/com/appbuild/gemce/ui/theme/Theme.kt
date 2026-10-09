package com.appbuild.gemce.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.appbuild.gemce.domain.settings.FontPreference
import com.appbuild.gemce.domain.settings.ThemeMode

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    secondary = GreenGrey80,
    tertiary = Lime80,
    background = DarkBackground,
    surface = DarkSurface
)

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    secondary = GreenGrey40,
    tertiary = Lime40
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
    val typography = createAppTypography(fontPreference)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content
    )
}
