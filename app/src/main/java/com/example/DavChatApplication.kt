package com.example

import android.app.Application
import android.graphics.Bitmap
import coil.Coil
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Custom Application class for Dav Chat.
 * Implements [ImageLoaderFactory] to configure Coil globally with software bitmaps
 * (`allowHardware(false)`), preventing GraphicBufferAllocator and AHardwareBuffer allocation
 * errors (such as 1x1 buffer allocation failures) on emulators and virtualized GPUs.
 */
class DavChatApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        // Preemptively initialize Coil singleton with hardware bitmaps disabled
        val loader = newImageLoader()
        Coil.setImageLoader(loader)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // Disabling hardware bitmaps prevents GraphicBufferAllocator (1x1) AHardwareBuffer errors
            .allowHardware(false)
            .bitmapConfig(Bitmap.Config.ARGB_8888)
            .allowRgb565(false)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .build()
    }
}
