package com.photosdbrowser.app.ui.photogrid

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photosdbrowser.app.data.FavoritesRepository
import com.photosdbrowser.app.data.MediaScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotoGridViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = MediaScanner(application)
    private val favoritesRepository = FavoritesRepository(application)

    private val _uiState = MutableStateFlow<PhotoGridUiState>(PhotoGridUiState.Loading)
    val uiState: StateFlow<PhotoGridUiState> = _uiState.asStateFlow()

    val favorites: StateFlow<Set<String>> = favoritesRepository.favorites.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptySet()
    )

    private var loadedFolderUri: Uri? = null

    fun loadPhotos(folderUri: Uri) {
        if (loadedFolderUri == folderUri) return
        loadedFolderUri = folderUri

        _uiState.value = PhotoGridUiState.Loading
        viewModelScope.launch {
            runCatching { scanner.scanPhotos(folderUri) }
                .onSuccess { photos ->
                    _uiState.value = if (photos.isEmpty()) {
                        PhotoGridUiState.Empty
                    } else {
                        PhotoGridUiState.Success(photos)
                    }
                }
                .onFailure { error ->
                    _uiState.value = PhotoGridUiState.Error(
                        error.message ?: "No se han podido leer las fotos de esta carpeta"
                    )
                }
        }
    }

    fun toggleFavorite(uri: String) {
        viewModelScope.launch { favoritesRepository.toggle(uri) }
    }
}
