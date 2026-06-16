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
        val children = root.listFiles() ?: return@withContext emptyList()
        val folders = mutableListOf<FolderInfo>()

        // If root itself contains photos directly, add it as a folder entry
        val rootPhotos = children.filter { it.isImageFile() }
        if (rootPhotos.isNotEmpty()) {
            folders += FolderInfo(
                uri = root.uri,
                name = root.name.orEmpty(),
                coverUri = rootPhotos.first().uri,
                photoCount = countPhotosRecursive(root)
            )
        }

        // Each immediate subfolder that has photos anywhere inside it
        children.filter { it.isDirectory }.forEach { subdir ->
            val cover = findFirstPhoto(subdir) ?: return@forEach
            folders += FolderInfo(
                uri = subdir.uri,
                name = subdir.name.orEmpty(),
                coverUri = cover,
                photoCount = countPhotosRecursive(subdir)
            )
        }

        folders.sortedBy { it.name.lowercase() }
    }

    private fun findFirstPhoto(folder: DocumentFile): Uri? {
        val children = folder.listFiles() ?: return null
        children.find { it.isImageFile() }?.let { return it.uri }
        children.filter { it.isDirectory }.forEach { sub ->
            findFirstPhoto(sub)?.let { return it }
        }
        return null
    }

    private fun countPhotosRecursive(folder: DocumentFile): Int {
        val children = folder.listFiles() ?: return 0
        return children.count { it.isImageFile() } +
            children.filter { it.isDirectory }.sumOf { countPhotosRecursive(it) }
    }

    suspend fun scanPhotos(folderUri: Uri): List<PhotoInfo> = withContext(Dispatchers.IO) {
        val folder = DocumentFile.fromTreeUri(context, folderUri) ?: return@withContext emptyList()
        val photos = mutableListOf<PhotoInfo>()
        collectPhotosRecursive(folder, photos)
        photos.sortedBy { it.name.lowercase() }
    }

    private fun collectPhotosRecursive(folder: DocumentFile, into: MutableList<PhotoInfo>) {
        val children = folder.listFiles() ?: return
        children.forEach { file ->
            if (file.isImageFile()) {
                val name = file.name.orEmpty()
                into += PhotoInfo(uri = file.uri, name = name, isRaw = name.extension() in RAW_EXTENSIONS)
            } else if (file.isDirectory) {
                collectPhotosRecursive(file, into)
            }
        }
    }
}
