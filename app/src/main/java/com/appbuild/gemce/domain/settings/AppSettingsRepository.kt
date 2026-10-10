package com.appbuild.gemce.domain.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AppSettingsRepository {

    private const val PREFS_NAME = "gemce_app_settings_prefs"
    private const val KEY_THEME_MODE = "theme_mode"
    private const val KEY_FONT_PREFERENCE = "font_preference"
    private const val KEY_PROCESS_ONLY_WHILE_CHARGING = "process_only_while_charging"

    private var prefs: SharedPreferences? = null

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun initialize(context: Context) {
        val sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        val themeName = sp.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val themeMode = try {
            ThemeMode.valueOf(themeName)
        } catch (_: Exception) {
            ThemeMode.SYSTEM
        }

        val fontName = sp.getString(KEY_FONT_PREFERENCE, FontPreference.SYSTEM.name) ?: FontPreference.SYSTEM.name
        val fontPref = try {
            if (fontName == "SANS_SERIF") FontPreference.CALIBRI else FontPreference.valueOf(fontName)
        } catch (_: Exception) {
            FontPreference.SYSTEM
        }

        val chargingOnly = sp.getBoolean(KEY_PROCESS_ONLY_WHILE_CHARGING, false)

        _settings.value = AppSettings(
            themeMode = themeMode,
            fontPreference = fontPref,
            processOnlyWhileCharging = chargingOnly
        )
    }

    fun updateThemeMode(mode: ThemeMode) {
        _settings.value = _settings.value.copy(themeMode = mode)
        prefs?.edit()?.putString(KEY_THEME_MODE, mode.name)?.apply()
    }

    fun updateFontPreference(fontPref: FontPreference) {
        _settings.value = _settings.value.copy(fontPreference = fontPref)
        prefs?.edit()?.putString(KEY_FONT_PREFERENCE, fontPref.name)?.apply()
    }

    fun updateProcessOnlyWhileCharging(enabled: Boolean) {
        _settings.value = _settings.value.copy(processOnlyWhileCharging = enabled)
        prefs?.edit()?.putBoolean(KEY_PROCESS_ONLY_WHILE_CHARGING, enabled)?.apply()
    }
}
