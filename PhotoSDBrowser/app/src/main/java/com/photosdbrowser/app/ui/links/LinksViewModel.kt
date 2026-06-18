package com.photosdbrowser.app.ui.links

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photosdbrowser.app.data.LinkConfigRepository
import com.photosdbrowser.app.data.model.LinkConfig
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface LinksUiState {
    data object Loading : LinksUiState
    data class Ready(val links: List<LinkConfig>) : LinksUiState
}

class LinksViewModel(application: Application) : AndroidViewModel(application) {

    val uiState = LinkConfigRepository(application).links
        .map { links -> LinksUiState.Ready(links.filter { it.visible }) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LinksUiState.Loading
        )
}
