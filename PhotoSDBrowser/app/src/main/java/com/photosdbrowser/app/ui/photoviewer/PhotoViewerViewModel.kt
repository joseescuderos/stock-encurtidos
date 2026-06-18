package com.photosdbrowser.app.ui.photoviewer

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photosdbrowser.app.data.MediaScanner
import com.photosdbrowser.app.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PhotoViewerViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = MediaScanner(application)
    private val settingsRepository = SettingsRepository(application)

    private val _uiState = MutableStateFlow(PhotoViewerUiState())
    val uiState: StateFlow<PhotoViewerUiState> = _uiState.asStateFlow()

    val slideshowIntervalMs: StateFlow<Long> = settingsRepository.settings
        .map { it.slideshowIntervalMs }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = com.photosdbrowser.app.data.DEFAULT_SLIDESHOW_INTERVAL_MS
        )

    private var loadedFolderUri: Uri? = null

    fun load(folderUri: Uri, startIndex: Int) {
        if (loadedFolderUri == folderUri) return
        loadedFolderUri = folderUri

        viewModelScope.launch {
            val photos = runCatching { scanner.scanPhotos(folderUri) }.getOrDefault(emptyList())
            _uiState.value = PhotoViewerUiState(
                photos = photos,
                startIndex = startIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)),
                isLoading = false
            )
        }
    }

    fun setSlideshowInterval(ms: Long) {
        viewModelScope.launch { settingsRepository.setSlideshowInterval(ms) }
    }
}
