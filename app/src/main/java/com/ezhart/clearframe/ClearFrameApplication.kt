package com.ezhart.clearframe

import android.app.Application
import org.greenrobot.eventbus.EventBus

class ClearFrameApplication : Application() {
    lateinit var container: AppContainer
    override fun onCreate() {
        super.onCreate()
        container = DefaultAppContainer(this)
    }
}