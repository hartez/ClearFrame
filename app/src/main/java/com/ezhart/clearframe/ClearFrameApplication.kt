package com.ezhart.clearframe

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache
import coil.util.DebugLogger

class ClearFrameApplication : Application(), ImageLoaderFactory {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.50)
                    .build()
            }
            .logger(DebugLogger())
            .build()
    }
}

