package com.photosdbrowser.app.data.model

import android.net.Uri

/**
 * A single photo file inside a folder. [isRaw] flags camera RAW formats (CR2/ARW/NEF) whose
 * embedded JPEG thumbnail is used for preview instead of decoding the full RAW.
 */
data class PhotoInfo(
    val uri: Uri,
    val name: String,
    val isRaw: Boolean,
    val lastModified: Long = 0L
)
