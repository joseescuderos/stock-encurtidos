package com.photosdbrowser.app.ui.photogrid

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photosdbrowser.app.data.MediaScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PhotoGridViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = MediaScanner(application)

    private val _uiState = MutableStateFlow<PhotoGridUiState>(PhotoGridUiState.Loading)
    val uiState: StateFlow<PhotoGridUiState> = _uiState.asStateFlow()

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
                        error.message ?: "Couldn't read photos in this folder"
                    )
                }
        }
    }
}
