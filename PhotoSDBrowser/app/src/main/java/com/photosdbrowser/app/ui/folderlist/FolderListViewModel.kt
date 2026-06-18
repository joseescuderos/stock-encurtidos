package com.photosdbrowser.app.ui.folderlist

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.photosdbrowser.app.data.MediaScanner
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the SD card root chosen through the SAF folder picker, persists it across launches and
 * scans it for photo folders.
 */
class FolderListViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = MediaScanner(application)
    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _uiState = MutableStateFlow<FolderListUiState>(FolderListUiState.NoFolderSelected)
    val uiState: StateFlow<FolderListUiState> = _uiState.asStateFlow()

    init {
        restorePreviousRoot()
    }

    /** Called once the user picks a folder via [androidx.activity.result.contract.ActivityResultContracts.OpenDocumentTree]. */
    fun onRootFolderSelected(uri: Uri) {
        getApplication<Application>().contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
        prefs.edit().putString(KEY_ROOT_URI, uri.toString()).apply()
        loadFolders(uri)
    }

    fun retry() {
        prefs.getString(KEY_ROOT_URI, null)?.let { loadFolders(Uri.parse(it)) }
    }

    private fun restorePreviousRoot() {
        val savedUri = prefs.getString(KEY_ROOT_URI, null)?.let(Uri::parse) ?: return
        if (hasPersistedReadPermission(savedUri)) {
            loadFolders(savedUri)
        }
    }

    private fun hasPersistedReadPermission(uri: Uri): Boolean =
        getApplication<Application>().contentResolver.persistedUriPermissions
            .any { it.uri == uri && it.isReadPermission }

    private fun loadFolders(rootUri: Uri) {
        _uiState.value = FolderListUiState.Loading
        viewModelScope.launch {
            runCatching { scanner.scanFolders(rootUri) }
                .onSuccess { folders ->
                    _uiState.value = if (folders.isEmpty()) {
                        FolderListUiState.Empty
                    } else {
                        FolderListUiState.Success(folders)
                    }
                }
                .onFailure { error ->
                    _uiState.value = FolderListUiState.Error(
                        error.message ?: "No se ha podido leer la carpeta seleccionada"
                    )
                }
        }
    }

    private companion object {
        const val PREFS_NAME = "photo_sd_browser_prefs"
        const val KEY_ROOT_URI = "root_uri"
    }
}
