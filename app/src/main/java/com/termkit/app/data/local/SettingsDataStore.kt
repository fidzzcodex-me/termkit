package com.termkit.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.termkit.app.data.model.AppSettings
import com.termkit.app.data.model.AppTheme
import com.termkit.app.data.model.TerminalFontSize
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsStore by preferencesDataStore(name = "settings_store")

class SettingsDataStore(private val context: Context) {

    private val themeKey = stringPreferencesKey("theme")
    private val fontSizeKey = stringPreferencesKey("terminal_font_size")
    private val keepScreenOnKey = booleanPreferencesKey("keep_screen_on")

    val settings: Flow<AppSettings> = context.settingsStore.data.map { prefs ->
        AppSettings(
            theme = prefs[themeKey]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.SYSTEM,
            terminalFontSize = prefs[fontSizeKey]?.let { runCatching { TerminalFontSize.valueOf(it) }.getOrNull() } ?: TerminalFontSize.NORMAL,
            keepScreenOnWhileConnected = prefs[keepScreenOnKey] ?: true
        )
    }

    suspend fun setTheme(theme: AppTheme) {
        context.settingsStore.edit { it[themeKey] = theme.name }
    }

    suspend fun setFontSize(size: TerminalFontSize) {
        context.settingsStore.edit { it[fontSizeKey] = size.name }
    }

    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.settingsStore.edit { it[keepScreenOnKey] = enabled }
    }

    suspend fun clearAll() {
        context.settingsStore.edit { it.clear() }
    }
}
