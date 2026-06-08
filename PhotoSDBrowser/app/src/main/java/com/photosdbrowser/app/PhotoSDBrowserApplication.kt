package com.photosdbrowser.app

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.photosdbrowser.app.image.RawThumbnailFetcherFactory

class PhotoSDBrowserApplication : Application(), ImageLoaderFactory {

    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .components { add(RawThumbnailFetcherFactory(this@PhotoSDBrowserApplication)) }
            .build()
}
