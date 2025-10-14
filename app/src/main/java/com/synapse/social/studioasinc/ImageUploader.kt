
package com.synapse.social.studioasinc

import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.File

object ImageUploader {

    interface UploadCallback {
        fun onProgress(percent: Int)
        fun onUploadComplete(imageUrl: String)
        fun onUploadError(errorMessage: String)
    }

    fun uploadImage(filePath: String, callback: UploadCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(filePath)
                val fileName = file.name
                val fileBytes = file.readBytes()

                Supabase.client.storage["avatars"].uploadAsFlow(fileName, fileBytes, false).collect {
                    when (it) {
                        is io.github.jan.supabase.storage.UploadStatus.Progress -> {
                            callback.onProgress(it.totalBytesSend.toFloat() / it.contentLength * 100).toInt()
                        }
                        is io.github.jan.supabase.storage.UploadStatus.Success -> {
                            callback.onUploadComplete(it.publicUrl)
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                callback.onUploadError("Error: " + e.message)
            }
        }
    }

    fun deleteImage(imageUrl: String, callback: (Boolean, String?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val fileName = imageUrl.substring(imageUrl.lastIndexOf('/') + 1)
                Supabase.client.storage["avatars"].delete(fileName)
                callback(true, null)
            } catch (e: Exception) {
                e.printStackTrace()
                callback(false, "Error: " + e.message)
            }
        }
    }
}
