package com.synapse.social.studioasinc.util

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.AsyncUploadService
import com.synapse.social.studioasinc.PresenceManager
import com.synapse.social.studioasinc.backend.IAuthenticationService
import java.io.File

class ItemUploadHandler(
    private val context: Context,
    private val authService: IAuthenticationService,
    private val attactmentmap: ArrayList<HashMap<String, Any>>,
    private val rv_attacmentList: RecyclerView,
    private val onUploadSuccess: (String) -> Unit
) {

    fun startUpload(position: Int) {
        authService.getCurrentUser()?.let {
            PresenceManager.setActivity(it.getUid(), "Sending an attachment")
        }

        if (position < 0 || position >= attactmentmap.size) {
            Log.e("ItemUploadHandler", "Invalid position for upload: $position, size: ${attactmentmap.size}")
            return
        }

        val itemMap = attactmentmap[position]
        if (itemMap["uploadState"] != "pending") {
            return
        }

        itemMap["uploadState"] = "uploading"
        itemMap["uploadProgress"] = 0.0
        rv_attacmentList.adapter?.notifyItemChanged(position)

        val filePath = itemMap["localPath"].toString()
        if (filePath.isEmpty()) {
            Log.e("ItemUploadHandler", "Invalid file path for upload")
            itemMap["uploadState"] = "failed"
            rv_attacmentList.adapter?.notifyItemChanged(position)
            return
        }

        val file = File(filePath)
        if (!file.exists()) {
            Log.e("ItemUploadHandler", "File does not exist: $filePath")
            itemMap["uploadState"] = "failed"
            rv_attacmentList.adapter?.notifyItemChanged(position)
            return
        }

        AsyncUploadService.uploadWithNotification(context, filePath, file.name, object : AsyncUploadService.UploadProgressListener {
            override fun onProgress(filePath: String, percent: Int) {
                try {
                    if (position >= 0 && position < attactmentmap.size) {
                        val currentItem = attactmentmap[position]
                        if (filePath == currentItem["localPath"]) {
                            currentItem["uploadProgress"] = percent.toDouble()
                            rv_attacmentList.adapter?.notifyItemChanged(position)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ItemUploadHandler", "Error updating upload progress: " + e.message)
                }
            }

            override fun onSuccess(filePath: String, url: String, publicId: String) {
                try {
                    if (position >= 0 && position < attactmentmap.size) {
                        val mapToUpdate = attactmentmap[position]
                        if (filePath == mapToUpdate["localPath"]) {
                            mapToUpdate["uploadState"] = "success"
                            mapToUpdate["cloudinaryUrl"] = url
                            mapToUpdate["publicId"] = publicId
                            rv_attacmentList.adapter?.notifyItemChanged(position)
                            onUploadSuccess(url)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ItemUploadHandler", "Error updating upload success: " + e.message)
                }
            }

            override fun onFailure(filePath: String, error: String) {
                try {
                    if (position >= 0 && position < attactmentmap.size) {
                        val currentItem = attactmentmap[position]
                        if (filePath == currentItem["localPath"]) {
                            currentItem["uploadState"] = "failed"
                            rv_attacmentList.adapter?.notifyItemChanged(position)
                        }
                    }
                    Log.e("ItemUploadHandler", "Upload failed: $error")
                } catch (e: Exception) {
                    Log.e("ItemUploadHandler", "Error updating upload failure: " + e.message)
                }
            }
        })
    }
}