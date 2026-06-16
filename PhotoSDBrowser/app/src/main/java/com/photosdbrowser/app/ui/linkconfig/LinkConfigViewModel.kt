package com.photosdbrowser.app.ui.linkconfig

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photosdbrowser.app.data.LinkConfigRepository
import com.photosdbrowser.app.data.model.LinkConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

class LinkConfigViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = LinkConfigRepository(application)

    val links = repository.links.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun add(label: String, url: String, visible: Boolean) {
        if (label.isBlank() || url.isBlank()) return
        viewModelScope.launch {
            val updated = links.value + LinkConfig(
                id = UUID.randomUUID().toString(),
                label = label.trim(),
                url = url.trim(),
                visible = visible
            )
            repository.save(updated)
        }
    }

    fun update(id: String, label: String, url: String, visible: Boolean) {
        if (label.isBlank() || url.isBlank()) return
        viewModelScope.launch {
            val updated = links.value.map { link ->
                if (link.id == id) {
                    link.copy(label = label.trim(), url = url.trim(), visible = visible)
                } else {
                    link
                }
            }
            repository.save(updated)
        }
    }

    fun setVisible(id: String, visible: Boolean) {
        viewModelScope.launch {
            val updated = links.value.map { link ->
                if (link.id == id) link.copy(visible = visible) else link
            }
            repository.save(updated)
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            repository.save(links.value.filter { it.id != id })
        }
    }
}
