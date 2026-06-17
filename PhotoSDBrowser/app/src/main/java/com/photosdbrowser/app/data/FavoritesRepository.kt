package com.photosdbrowser.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesDataStore by preferencesDataStore(name = "favorites")

/**
 * Guarda las fotos marcadas como favoritas (por su URI) para que el cliente pueda
 * señalar las que le gustan y luego exportar/compartir esa selección.
 */
class FavoritesRepository(private val context: Context) {

    private val key = stringSetPreferencesKey("favorite_uris")

    val favorites: Flow<Set<String>> =
        context.favoritesDataStore.data.map { it[key] ?: emptySet() }

    suspend fun toggle(uri: String) {
        context.favoritesDataStore.edit { prefs ->
            val current = prefs[key] ?: emptySet()
            prefs[key] = if (uri in current) current - uri else current + uri
        }
    }
}
