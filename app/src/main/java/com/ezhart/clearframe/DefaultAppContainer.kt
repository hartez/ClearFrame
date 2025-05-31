package com.ezhart.clearframe

import android.content.Context
import com.ezhart.clearframe.data.InternalStoragePhotoRepository
import com.ezhart.clearframe.data.PhotoRepository

interface AppContainer {
    val photoRepository: PhotoRepository
}

class DefaultAppContainer(context: Context) : AppContainer {
    override val photoRepository: PhotoRepository by lazy {
        InternalStoragePhotoRepository(context)
    }
}
