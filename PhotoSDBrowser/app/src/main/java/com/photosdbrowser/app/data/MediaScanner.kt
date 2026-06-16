package com.photosdbrowser.app.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
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

private data class Entry(val documentId: String, val name: String, val isDirectory: Boolean) {
    val isImage: Boolean get() = !isDirectory && name.extension() in IMAGE_EXTENSIONS
}

/**
 * Scans a SAF directory tree using [DocumentsContract] + [android.content.ContentResolver]
 * directly (instead of DocumentFile) because that is far more reliable when enumerating the
 * children of nested subfolders on removable SD cards picked through the storage access framework.
 */
class MediaScanner(private val context: Context) {

    /**
     * Shows the top-level folders inside [rootUri] (plus the root itself if it holds photos
     * directly). Each folder uses, as its cover, the first photo found anywhere inside it.
     */
    suspend fun scanFolders(rootUri: Uri): List<FolderInfo> = withContext(Dispatchers.IO) {
        val rootDocId = DocumentsContract.getTreeDocumentId(rootUri)
        val folders = mutableListOf<FolderInfo>()

        val rootChildren = listChildren(rootUri, rootDocId)

        val rootPhotos = rootChildren.filter { it.isImage }
        if (rootPhotos.isNotEmpty()) {
            folders += FolderInfo(
                uri = docUri(rootUri, rootDocId),
                name = displayName(rootUri, rootDocId),
                coverUri = docUri(rootUri, rootPhotos.first().documentId),
                photoCount = countPhotos(rootUri, rootDocId)
            )
        }

        rootChildren.filter { it.isDirectory }.forEach { dir ->
            val cover = findFirstPhoto(rootUri, dir.documentId) ?: return@forEach
            folders += FolderInfo(
                uri = docUri(rootUri, dir.documentId),
                name = dir.name,
                coverUri = cover,
                photoCount = countPhotos(rootUri, dir.documentId)
            )
        }

        folders.sortedBy { it.name.lowercase() }
    }

    /** Collects every photo inside [folderUri] and all of its subfolders, at any depth. */
    suspend fun scanPhotos(folderUri: Uri): List<PhotoInfo> = withContext(Dispatchers.IO) {
        val docId = DocumentsContract.getDocumentId(folderUri)
        val photos = mutableListOf<PhotoInfo>()
        collectPhotos(folderUri, docId, photos)
        photos.sortedBy { it.name.lowercase() }
    }

    private fun collectPhotos(treeUri: Uri, parentDocId: String, into: MutableList<PhotoInfo>) {
        listChildren(treeUri, parentDocId).forEach { entry ->
            if (entry.isImage) {
                into += PhotoInfo(
                    uri = docUri(treeUri, entry.documentId),
                    name = entry.name,
                    isRaw = entry.name.extension() in RAW_EXTENSIONS
                )
            } else if (entry.isDirectory) {
                collectPhotos(treeUri, entry.documentId, into)
            }
        }
    }

    private fun findFirstPhoto(treeUri: Uri, parentDocId: String): Uri? {
        val children = listChildren(treeUri, parentDocId)
        children.firstOrNull { it.isImage }?.let { return docUri(treeUri, it.documentId) }
        children.filter { it.isDirectory }.forEach { dir ->
            findFirstPhoto(treeUri, dir.documentId)?.let { return it }
        }
        return null
    }

    private fun countPhotos(treeUri: Uri, parentDocId: String): Int {
        val children = listChildren(treeUri, parentDocId)
        return children.count { it.isImage } +
            children.filter { it.isDirectory }.sumOf { countPhotos(treeUri, it.documentId) }
    }

    private fun listChildren(treeUri: Uri, parentDocId: String): List<Entry> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val result = mutableListOf<Entry>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)
        }.getOrNull()?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: ""
                val mime = cursor.getString(2)
                result += Entry(
                    documentId = id,
                    name = name,
                    isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR
                )
            }
        }
        return result
    }

    private fun docUri(treeUri: Uri, documentId: String): Uri =
        DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

    private fun displayName(treeUri: Uri, documentId: String): String =
        runCatching {
            context.contentResolver.query(
                docUri(treeUri, documentId),
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null, null, null
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull() ?: ""
}
