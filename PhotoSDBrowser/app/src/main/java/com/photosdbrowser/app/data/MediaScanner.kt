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

/** How many levels of subfolders to search below the chosen root for photo folders. */
private const val MAX_SCAN_DEPTH = 4

private fun String?.extension(): String =
    this.orEmpty().substringAfterLast('.', "").lowercase()

private fun DocumentFile.isImageFile(): Boolean =
    isFile && name.extension() in IMAGE_EXTENSIONS

/**
 * Scans a SAF directory tree on a background thread using [DocumentFile] so it works for both
 * internal storage and removable SD cards picked through the storage access framework.
 */
class MediaScanner(private val context: Context) {

    /**
     * Walks the tree below [rootUri] up to [MAX_SCAN_DEPTH] levels deep and returns every folder
     * (at any depth, including the root itself) that directly contains photos. This covers SD
     * card layouts where photos sit straight inside the chosen folder as well as layouts with
     * one or more levels of subfolders (e.g. DCIM/100CANON).
     */
    suspend fun scanFolders(rootUri: Uri): List<FolderInfo> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, rootUri) ?: return@withContext emptyList()
        val folders = mutableListOf<FolderInfo>()
        collectPhotoFolders(root, depth = 0, into = folders)
        folders.sortedBy { it.name.lowercase() }
    }

    private fun collectPhotoFolders(folder: DocumentFile, depth: Int, into: MutableList<FolderInfo>) {
        val children = folder.listFiles()
        val photos = children.filter { it.isImageFile() }
        if (photos.isNotEmpty()) {
            into += FolderInfo(
                uri = folder.uri,
                name = folder.name.orEmpty(),
                coverUri = photos.first().uri,
                photoCount = photos.size
            )
        }
        if (depth < MAX_SCAN_DEPTH) {
            children.filter { it.isDirectory }.forEach { collectPhotoFolders(it, depth + 1, into) }
        }
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
