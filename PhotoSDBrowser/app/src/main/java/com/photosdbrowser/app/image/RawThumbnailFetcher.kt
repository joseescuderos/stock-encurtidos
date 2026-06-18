package com.photosdbrowser.app.image

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.graphics.drawable.toDrawable
import androidx.exifinterface.media.ExifInterface
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val RAW_EXTENSIONS = setOf("cr2", "arw", "nef")

private fun Uri.isRawPhoto(): Boolean =
    lastPathSegment.orEmpty().substringAfterLast('.', "").lowercase() in RAW_EXTENSIONS

/**
 * Routes RAW camera files (CR2/ARW/NEF) to [RawThumbnailFetcher] and lets Coil's default
 * fetchers handle every other format (JPG/PNG/HEIC/...).
 */
class RawThumbnailFetcherFactory(private val context: Context) : Fetcher.Factory<Uri> {
    override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
        if (!data.isRawPhoto()) return null
        return RawThumbnailFetcher(context, data)
    }
}

/**
 * Decodes only the small embedded JPEG preview stored in a RAW file's EXIF data — the full RAW
 * is never decoded, which keeps thumbnailing fast on tablet hardware.
 */
class RawThumbnailFetcher(
    private val context: Context,
    private val uri: Uri
) : Fetcher {

    override suspend fun fetch(): FetchResult? = withContext(Dispatchers.IO) {
        val thumbnailBytes = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).thumbnailBytes
        } ?: return@withContext null

        val bitmap = BitmapFactory.decodeByteArray(thumbnailBytes, 0, thumbnailBytes.size)
            ?: return@withContext null

        DrawableResult(
            drawable = bitmap.toDrawable(context.resources),
            isSampled = false,
            dataSource = DataSource.DISK
        )
    }
}
