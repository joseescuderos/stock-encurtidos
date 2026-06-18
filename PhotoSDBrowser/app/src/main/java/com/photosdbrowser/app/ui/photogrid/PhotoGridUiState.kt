package com.photosdbrowser.app.ui.photogrid

import com.photosdbrowser.app.data.model.PhotoInfo

sealed interface PhotoGridUiState {
    data object Loading : PhotoGridUiState
    data object Empty : PhotoGridUiState
    data class Success(val photos: List<PhotoInfo>) : PhotoGridUiState
    data class Error(val message: String) : PhotoGridUiState
}
