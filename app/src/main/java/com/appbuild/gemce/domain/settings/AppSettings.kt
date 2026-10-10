package com.appbuild.gemce.domain.settings

import androidx.compose.ui.text.font.FontFamily

enum class ThemeMode(val label: String, val icon: String) {
    SYSTEM("System Default", "📱"),
    LIGHT("Light Mode", "☀️"),
    DARK("Dark Mode", "🌙")
}

enum class FontPreference(val label: String, val description: String) {
    SYSTEM("System Default", "Standard Android system typography"),
    CALIBRI("Calibri", "Clean, modern, highly legible study text (packaged TrueType)"),
    SERIF("Serif", "Traditional editorial typeface for dense reading (packaged TrueType)"),
    MONOSPACE("Monospace", "Fixed-width font for technical & STEM subjects (packaged TrueType)")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val fontPreference: FontPreference = FontPreference.SYSTEM,
    val processOnlyWhileCharging: Boolean = false
)
