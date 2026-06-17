package com.photosdbrowser.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "app_settings")

const val DEFAULT_SLIDESHOW_INTERVAL_MS = 4000L

/** Preferencias globales de la app: tema y velocidad de la presentación automática. */
data class AppSettings(
    val darkTheme: Boolean = false,
    val slideshowIntervalMs: Long = DEFAULT_SLIDESHOW_INTERVAL_MS
)

class SettingsRepository(private val context: Context) {

    private val darkThemeKey = booleanPreferencesKey("dark_theme")
    private val slideshowKey = longPreferencesKey("slideshow_interval_ms")

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            darkTheme = prefs[darkThemeKey] ?: false,
            slideshowIntervalMs = prefs[slideshowKey] ?: DEFAULT_SLIDESHOW_INTERVAL_MS
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.settingsDataStore.edit { it[darkThemeKey] = enabled }
    }

    suspend fun setSlideshowInterval(ms: Long) {
        context.settingsDataStore.edit { it[slideshowKey] = ms }
    }
}
