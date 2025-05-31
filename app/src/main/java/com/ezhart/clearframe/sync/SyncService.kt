package com.ezhart.clearframe.sync

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class SyncService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return super.onStartCommand(intent, flags, startId)
    }

    private fun start(context: Context) {

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val syncRequest =
            PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork("SyncPhotos", ExistingPeriodicWorkPolicy.UPDATE, syncRequest)
    }

    private val binder = SyncServiceBinder()

    inner class SyncServiceBinder : Binder() {
        fun getService(): SyncService = this@SyncService
    }

    override fun onBind(intent: Intent): IBinder {
        start(this.applicationContext)
        return binder
    }
}

