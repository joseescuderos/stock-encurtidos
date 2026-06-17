package com.photosdbrowser.app.data.model

import android.net.Uri

/**
 * A photo folder discovered under the selected SD card root.
 */
data class FolderInfo(
    val uri: Uri,
    val name: String,
    val coverUri: Uri?,
    val photoCount: Int,
    val lastModified: Long = 0L
)
