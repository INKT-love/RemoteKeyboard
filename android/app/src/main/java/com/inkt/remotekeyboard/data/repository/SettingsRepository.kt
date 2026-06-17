package com.inkt.remotekeyboard.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.inkt.remotekeyboard.data.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val autoConnectKey = booleanPreferencesKey("auto_connect")

    val themeMode: Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        val name = prefs[themeModeKey] ?: ThemeMode.MONET.name
        try { ThemeMode.valueOf(name) } catch (_: Exception) { ThemeMode.MONET }
    }

    val autoConnect: Flow<Boolean> = context.settingsDataStore.data.map { prefs ->
        prefs[autoConnectKey] ?: false
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { prefs ->
            prefs[themeModeKey] = mode.name
        }
    }

    suspend fun setAutoConnect(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[autoConnectKey] = enabled
        }
    }
}
