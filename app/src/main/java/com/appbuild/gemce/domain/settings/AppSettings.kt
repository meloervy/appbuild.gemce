package com.appbuild.gemce.domain.settings

import androidx.compose.ui.text.font.FontFamily

enum class ThemeMode(val label: String) {
    SYSTEM("System Default"),
    LIGHT("Light Mode"),
    DARK("Dark Mode")
}

enum class FontPreference(val label: String, val description: String) {
    SYSTEM("System Default", "Standard Android system typography"),
    SANS_SERIF("Sans-Serif (Calibri style)", "Clean, modern, highly legible study text"),
    SERIF("Serif (Academic)", "Traditional editorial typeface for dense reading"),
    MONOSPACE("Monospace (Code / Technical)", "Fixed-width font for technical & STEM subjects")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontPreference: FontPreference = FontPreference.SYSTEM
)
