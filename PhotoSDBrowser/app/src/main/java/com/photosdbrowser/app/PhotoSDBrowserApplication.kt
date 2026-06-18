package com.photosdbrowser.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.photosdbrowser.app.image.RawThumbnailFetcherFactory

class PhotoSDBrowserApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(RawThumbnailFetcherFactory(this@PhotoSDBrowserApplication)) }
            // Generous in-memory and on-disk caches so already-seen thumbnails and photos load
            // instantly when scrolling back, which matters most when browsing a slow SD card.
            .memoryCache {
                MemoryCache.Builder(this@PhotoSDBrowserApplication)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            // Decode thumbnails at a lower bit depth to cut memory and speed up grid rendering.
            .allowRgb565(true)
            .crossfade(false)
            .build()
}
