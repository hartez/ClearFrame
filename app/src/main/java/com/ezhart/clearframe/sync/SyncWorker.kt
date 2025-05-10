package com.ezhart.clearframe.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ezhart.clearframe.ClearFrameApplication
import com.ezhart.clearframe.model.Photo
import kotlinx.serialization.Serializable
import okhttp3.ResponseBody
import org.greenrobot.eventbus.EventBus
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

private const val TAG = "SyncService"

private const val BASE_URL = "http://192.168.1.16:5556"
//private const val BASE_URL = "http://frame"

interface RemotePhotoService {
    @GET("/")
    suspend fun getPhotos(): List<RemotePhoto>

    @GET
    @Streaming
    suspend fun download(@Url url: String): Response<ResponseBody>
}

data class ReloadRequest(val reason: String)

@Serializable
data class RemotePhoto(
    val name: String,
    val url: String,
    val digest: String
)

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    private val retrofit =
        Retrofit.Builder().addConverterFactory(GsonConverterFactory.create()).baseUrl(BASE_URL)
            .build()

    private val photoService: RemotePhotoService by lazy {
        retrofit.create(RemotePhotoService::class.java)
    }

    override suspend fun doWork(): Result {
        try {
            Log.d(TAG,"Synchronizing...")

            val remotePhotos = getRemotePhotoList()
            val localPhotos = getLocalPhotoList()

            for (remotePhoto in remotePhotos) {
                Log.d(
                    TAG,
                    "Found remote photo ${remotePhoto.url} with digest ${remotePhoto.digest}"
                )
            }

            for (localPhoto in localPhotos) {
                Log.d(
                    TAG,
                    "Found local photo ${localPhoto.filename} with digest ${localPhoto.digest}"
                )
            }

            val photosDownloaded: Boolean = cleanupPhotos(getDeleteList(remotePhotos, localPhotos))
            val photosDeleted: Boolean = downloadPhotos(getDownloadList(remotePhotos, localPhotos))

            if (photosDeleted || photosDownloaded) {

                var reason = "Photos changed "

                if(photosDeleted){
                    reason += "(deletions) "
                }

                if(photosDownloaded){
                    reason += "(downloads) "
                }

                EventBus.getDefault().post(ReloadRequest(reason))
            }

            return Result.success()
        } catch (e: Exception) {
            Log.e(TAG, e.stackTraceToString())
        }

        return Result.failure()
    }

    private fun getDownloadList(
        remotePhotos: List<RemotePhoto>,
        localPhotos: List<Photo>
    ): List<RemotePhoto> {

        val toDownload = mutableListOf<RemotePhoto>()

        for (remotePhoto in remotePhotos) {
            val remoteDigest = remotePhoto.digest

            if (localPhotos.any { p -> p.digest == remoteDigest }) {
                Log.d(
                    TAG,
                    "Remote photo ${remotePhoto.name} with digest ${remotePhoto.digest} already exists locally; skipping download."
                )

                // TODO If the local photo exists with the same digest but the filename is different, we need to rename the local photo (to preserve intended slideshow ordering)

                continue
            }

            toDownload.add(remotePhoto)
        }

        return toDownload
    }

    private fun getDeleteList(
        remotePhotos: List<RemotePhoto>,
        localPhotos: List<Photo>
    ): List<Photo> {
        val toDelete = mutableListOf<Photo>()

        for (localPhoto in localPhotos) {
            val digest = localPhoto.digest

            if (remotePhotos.any { p -> p.digest == digest }) {
                continue
            }

            Log.d(
                TAG,
                "Local photo ${localPhoto.filename} with digest ${localPhoto.digest} does not exist remotely; marking for cleanup."
            )

            toDelete.add(localPhoto)
        }

        return toDelete
    }

    private suspend fun getRemotePhotoList(): List<RemotePhoto> {
        Log.d(TAG, "Getting remote photo list...")

        val photos = photoService.getPhotos()

        // Handle remapping the URLs from the service for the emulator
        return photos.map {
            RemotePhoto(
                it.name, it.url.replace("http://frame", BASE_URL), it.digest
            )
        }
    }

    private suspend fun getLocalPhotoList(): List<Photo> {
        Log.d(TAG, "Getting local photo list...")

        val photoRepository =
            (applicationContext as ClearFrameApplication).container.photoRepository

        return photoRepository.getPhotos()
    }

    private suspend fun downloadPhotos(photos: List<RemotePhoto>): Boolean {
        var photosChanged = false

        for (photo in photos) {
            Log.d(TAG, "Downloading ${photo.name}")
            val responseBody = photoService.download(photo.url).body()
            if (saveFile(responseBody, photo.name)) {
                photosChanged = true
            }
        }

        return photosChanged
    }

    private fun cleanupPhotos(photos: List<Photo>): Boolean {

        var photosChanged = false

        for (photo in photos) {
            Log.d(TAG, "Deleting ${photo.filename}")
            val deleted = File(photo.filename).delete()

            if (!deleted) {
                Log.e(TAG, "Error deleting ${photo.filename}")
            } else {
                photosChanged = true
            }
        }

        return photosChanged
    }

    private fun saveFile(body: ResponseBody?, filename: String): Boolean {
        if (body == null) return false
        var input: InputStream? = null
        try {
            input = body.byteStream()

            // Initially tried to use Path() for building this path, but got a missing library error
            // so I just punted and used a template
            //val path = Path(applicationContext.filesDir.path, filename).toString()
            val path = "${applicationContext.filesDir.path}/$filename"

            Log.d(TAG, "Destination is $path")

            val fos = FileOutputStream(path)
            fos.use { output ->
                val buffer = ByteArray(4 * 1024) // or other buffer size
                var read: Int
                while (input.read(buffer).also { read = it } != -1) {
                    output.write(buffer, 0, read)
                }
                output.flush()
            }
            return true
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        } finally {
            input?.close()
        }

        return false
    }
}