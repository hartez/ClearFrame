package com.ezhart.clearframe.data

import android.content.Context
import android.util.Log
import com.ezhart.clearframe.model.Photo
import java.io.File
import java.security.MessageDigest

private const val TAG = "PhotoRepository"

interface PhotoRepository {
    suspend fun getPhotos(): List<Photo>
}

class InternalStoragePhotoRepository(private val context: Context) : PhotoRepository{

    override suspend fun getPhotos(): List<Photo> {

        val storagePath = context.filesDir.path
        val imagePaths: List<String> = context.fileList()
            .filter { f -> f.endsWith("jpg", true) }
            .sortedBy { f -> f }
            // Initially tried to use Path() for building this path, but got a missing library error
            // so I just punted and used a template
            //.map { f -> Path(storagePath, f).toString() }
            .map { f -> "$storagePath/$f" }

        val photos = mutableListOf<Photo>()

        for(imagePath in imagePaths){
            Log.d(TAG, "Getting hash of $imagePath")
            val digest = File(imagePath).toHash("SHA-1")
            photos.add(Photo(imagePath, digest))
        }

        return photos
    }

    private fun File.toHash(algorithm: String): String {
        val md = MessageDigest.getInstance(algorithm)
        inputStream().use { stream ->
            val buffer = ByteArray(1024)
            var bytesRead: Int
            while (stream.read(buffer).also { bytesRead = it } != -1) {
                md.update(buffer, 0, bytesRead)
            }
        }
        val digest = md.digest()
        return digest.joinToString("") { "%02x".format(it) }
    }
}

