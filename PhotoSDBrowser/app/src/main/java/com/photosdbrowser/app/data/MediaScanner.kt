package com.photosdbrowser.app.data

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.photosdbrowser.app.data.model.FolderInfo
import com.photosdbrowser.app.data.model.PhotoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val IMAGE_EXTENSIONS = setOf(
    "jpg", "jpeg", "png", "heic", "heif", "webp", "gif", "bmp",
    "cr2", "arw", "nef"
)
private val RAW_EXTENSIONS = setOf("cr2", "arw", "nef")

private fun String?.extension(): String =
    this.orEmpty().substringAfterLast('.', "").lowercase()

private fun DocumentFile.isImageFile(): Boolean =
    isFile && name.extension() in IMAGE_EXTENSIONS

/**
 * Scans a SAF directory tree on a background thread using [DocumentFile] so it works for both
 * internal storage and removable SD cards picked through the storage access framework.
 */
class MediaScanner(private val context: Context) {

    suspend fun scanFolders(rootUri: Uri): List<FolderInfo> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext emptyList()
        root.listFiles()
            .asSequence()
            .filter { it.isDirectory }
            .map { folder ->
                val photos = folder.listFiles().filter { it.isImageFile() }
                FolderInfo(
                    uri = folder.uri,
                    name = folder.name.orEmpty(),
                    coverUri = photos.firstOrNull()?.uri,
                    photoCount = photos.size
                )
            }
            .filter { it.photoCount > 0 }
            .sortedBy { it.name.lowercase() }
            .toList()
    }

    suspend fun scanPhotos(folderUri: Uri): List<PhotoInfo> = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        folder.listFiles()
            .asSequence()
            .filter { it.isImageFile() }
            .map { file ->
                val name = file.name.orEmpty()
                PhotoInfo(uri = file.uri, name = name, isRaw = name.extension() in RAW_EXTENSIONS)
            }
            .sortedBy { it.name.lowercase() }
            .toList()
    }
}
