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

private data class Entry(
    val documentId: String,
    val name: String,
    val isDirectory: Boolean,
    val lastModified: Long
) {
    val isImage: Boolean get() = !isDirectory && name.extension() in IMAGE_EXTENSIONS
}

private data class FolderScan(val cover: android.net.Uri?, val count: Int)

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
            // Root shown as a folder for any photos sitting directly inside the chosen folder.
            folders += FolderInfo(
                uri = docUri(rootUri, rootDocId),
                name = displayName(rootUri, rootDocId),
                coverUri = docUri(rootUri, rootPhotos.first().documentId),
                photoCount = rootPhotos.size
            )
        }

        rootChildren.filter { it.isDirectory }.forEach { dir ->
            val scan = scanFolderContents(rootUri, dir.documentId)
            val cover = scan.cover ?: return@forEach
            folders += FolderInfo(
                uri = docUri(rootUri, dir.documentId),
                name = dir.name,
                coverUri = cover,
                photoCount = scan.count,
                lastModified = dir.lastModified
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
                    isRaw = entry.name.extension() in RAW_EXTENSIONS,
                    lastModified = entry.lastModified
                )
            } else if (entry.isDirectory) {
                collectPhotos(treeUri, entry.documentId, into)
            }
        }
    }

    /**
     * Walks the subtree under [parentDocId] a single time, gathering both the cover photo (the
     * first one found) and the total photo count. Doing it in one pass — instead of one walk for
     * the cover and another for the count — roughly halves the SAF queries, which are the slow part
     * on removable SD cards.
     */
    private fun scanFolderContents(treeUri: Uri, parentDocId: String): FolderScan {
        var cover: Uri? = null
        var count = 0
        val pending = ArrayDeque<String>()
        pending.addLast(parentDocId)
        while (pending.isNotEmpty()) {
            val current = pending.removeLast()
            listChildren(treeUri, current).forEach { entry ->
                when {
                    entry.isImage -> {
                        count++
                        if (cover == null) cover = docUri(treeUri, entry.documentId)
                    }
                    entry.isDirectory -> pending.addLast(entry.documentId)
                }
            }
        }
        return FolderScan(cover, count)
    }

    private fun listChildren(treeUri: Uri, parentDocId: String): List<Entry> {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
        val result = mutableListOf<Entry>()
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
            DocumentsContract.Document.COLUMN_LAST_MODIFIED
        )
        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)
        }.getOrNull()?.use { cursor ->
            while (cursor.moveToNext()) {
                val id = cursor.getString(0) ?: continue
                val name = cursor.getString(1) ?: ""
                val mime = cursor.getString(2)
                val lastModified = if (cursor.isNull(3)) 0L else cursor.getLong(3)
                result += Entry(
                    documentId = id,
                    name = name,
                    isDirectory = mime == DocumentsContract.Document.MIME_TYPE_DIR,
                    lastModified = lastModified
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
