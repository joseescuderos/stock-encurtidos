package com.photosdbrowser.app.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.photosdbrowser.app.data.model.LinkConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.linksDataStore by preferencesDataStore(name = "links_config")

class LinkConfigRepository(private val context: Context) {

    private val linksKey = stringPreferencesKey("links_json")

    val links: Flow<List<LinkConfig>> = context.linksDataStore.data.map { prefs ->
        val json = prefs[linksKey] ?: return@map emptyList()
        runCatching { Json.decodeFromString<List<LinkConfig>>(json) }.getOrElse { emptyList() }
    }

    suspend fun save(links: List<LinkConfig>) {
        context.linksDataStore.edit { prefs ->
            prefs[linksKey] = Json.encodeToString(links)
        }
    }
}
