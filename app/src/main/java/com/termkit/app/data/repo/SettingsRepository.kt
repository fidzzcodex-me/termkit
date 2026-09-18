package com.termkit.app.data.repo

import com.termkit.app.data.local.SettingsDataStore
import com.termkit.app.data.model.AppSettings
import com.termkit.app.data.model.AppTheme
import com.termkit.app.data.model.TerminalFontSize
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val dataStore: SettingsDataStore) {

    val settings: Flow<AppSettings> = dataStore.settings

    suspend fun setTheme(theme: AppTheme) = dataStore.setTheme(theme)

    suspend fun setFontSize(size: TerminalFontSize) = dataStore.setFontSize(size)

    suspend fun setKeepScreenOn(enabled: Boolean) = dataStore.setKeepScreenOn(enabled)

    suspend fun clearAll() = dataStore.clearAll()
}
