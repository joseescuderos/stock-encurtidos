package com.photosdbrowser.app.ui.photoviewer

import com.photosdbrowser.app.data.model.PhotoInfo

data class PhotoViewerUiState(
    val photos: List<PhotoInfo> = emptyList(),
    val startIndex: Int = 0,
    val isLoading: Boolean = true
)
