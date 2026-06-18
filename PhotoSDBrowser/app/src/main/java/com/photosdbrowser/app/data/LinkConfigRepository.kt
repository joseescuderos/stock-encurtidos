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
    private val seededKey = stringPreferencesKey("seeded")

    val links: Flow<List<LinkConfig>> = context.linksDataStore.data.map { prefs ->
        if (prefs[seededKey] == null) {
            save(DEFAULT_LINKS)
            return@map DEFAULT_LINKS
        }
        val json = prefs[linksKey] ?: return@map emptyList()
        runCatching { Json.decodeFromString<List<LinkConfig>>(json) }.getOrElse { emptyList() }
    }

    suspend fun save(links: List<LinkConfig>) {
        context.linksDataStore.edit { prefs ->
            prefs[linksKey] = Json.encodeToString(links)
            prefs[seededKey] = "1"
        }
    }
}

private var idCounter = 0
private fun nextId() = "default_${++idCounter}"

private val DEFAULT_LINKS = listOf(
    LinkConfig(nextId(), "PRECIOS SESIÓN DE PAREJA", "https://bit.ly/3mM4jk5"),
    LinkConfig(nextId(), "PRECIOS REPORTAJE DE FAMILIA 2025", "https://escuderosfotografia.com/precios-familias"),
    LinkConfig(nextId(), "RECOMENDACIONES SESIÓN DE FAMILIA", "https://escuderosfotografia.com/recomendaciones-sesion-familia"),
    LinkConfig(nextId(), "PRECIOS ACTOS FALLEROS 2026/27", "https://escuderosfotografia.com/fallas2027"),
    LinkConfig(nextId(), "RECOMENDACIONES SESIÓN DE EMBARAZADA", "https://escuderosfotografia.com/recomendaciones-sesion-embarazada"),
    LinkConfig(nextId(), "COMUNIONES 2026", "https://escuderosfotografia.com/comuniones-2026"),
    LinkConfig(nextId(), "RECOMENDACIONES SESIÓN DE COMUNIÓN", "https://acortar.link/3tRYs1"),
    LinkConfig(nextId(), "ÁLBUM KOTI COLLECTION", "https://bit.ly/3nGiCr5"),
    LinkConfig(nextId(), "ÁLBUMES", "https://escuderosfotografia.com/albumes"),
    LinkConfig(nextId(), "ÁLBUM FUSION BOOK", "https://joseescuderos.com/album-fusion-book"),
    LinkConfig(nextId(), "ÁLBUMES DE BODA 2025/26", "https://joseescuderos.com/albumes-25-26"),
    LinkConfig(nextId(), "PRECIOS COPIAS EN PAPEL", "https://escuderosfotografia.com/copias-papel"),
    LinkConfig(nextId(), "DECORACIÓN Y AMPLIACIONES", "https://escuderosfotografia.com/decoracion-ampliaciones"),
    LinkConfig(nextId(), "PRECIOS SESIONES DE PAREJA", "https://onx.la/08718"),
    LinkConfig(nextId(), "PRECIOS REPORTAJE DE BAUTIZO", "https://escuderosfotografia.com/bautizos"),
    LinkConfig(nextId(), "GALERÍA BAUTIZO ANDREAS", "https://bit.ly/3rBch0L"),
    LinkConfig(nextId(), "PRECIO REPORTAJE BODAS DE PLATA/ORO", "https://bit.ly/3IvrCax"),
    LinkConfig(nextId(), "RECOMENDACIONES SESIÓN DE PAREJA", "https://bit.ly/39dTdQX"),
    LinkConfig(nextId(), "PRECIOS DE BODAS 2026/27", "https://joseescuderos.com/tarifas-26-27")
)
