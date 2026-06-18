package com.photosdbrowser.app.ui.folderlist

import com.photosdbrowser.app.data.model.FolderInfo

sealed interface FolderListUiState {
    data object NoFolderSelected : FolderListUiState
    data object Loading : FolderListUiState
    data object Empty : FolderListUiState
    data class Success(val folders: List<FolderInfo>) : FolderListUiState
    data class Error(val message: String) : FolderListUiState
}
