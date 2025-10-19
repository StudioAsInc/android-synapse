package com.synapse.social.studioasinc.util

import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.synapse.social.studioasinc.AsyncUploadService
import com.synapse.social.studioasinc.PresenceManager
import com.synapse.social.studioasinc.SketchwareUtil
import com.synapse.social.studioasinc.model.Attachment
import java.io.File

class ItemUploadHandler(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val attachments: ArrayList<Attachment>,
    private val rv_attacmentList: RecyclerView,
    private val onUploadSuccess: (String) -> Unit
) {

    fun startUpload(position: Int) {
        if (auth.currentUser != null) {
            PresenceManager.setActivity(auth.currentUser!!.uid, "Sending an attachment")
        }

        if (position < 0 || position >= attachments.size) {
            Log.e("ItemUploadHandler", "Invalid position for upload: $position, size: ${attachments.size}")
            return
        }

        if (!SketchwareUtil.isConnected(context)) {
            try {
                val attachment = attachments[position]
                attachment.uploadState = "failed"
                rv_attacmentList.adapter?.notifyItemChanged(position)
            } catch (e: Exception) {
                Log.e("ItemUploadHandler", "Error updating upload state: " + e.message)
            }
            return
        }

        val attachment = attachments[position]
        if (attachment.uploadState != "pending") {
            return
        }

        attachment.uploadState = "uploading"
        attachment.uploadProgress = 0.0
        rv_attacmentList.adapter?.notifyItemChanged(position)

        val filePath = attachment.localPath
        if (filePath.isEmpty()) {
            Log.e("ItemUploadHandler", "Invalid file path for upload")
            attachment.uploadState = "failed"
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
                    if (position >= 0 && position < attachments.size) {
                        val currentItem = attachments[position]
                        if (filePath == currentItem.localPath) {
                            currentItem.uploadProgress = percent.toDouble()
                            rv_attacmentList.adapter?.notifyItemChanged(position)
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ItemUploadHandler", "Error updating upload progress: " + e.message)
                }
            }

            override fun onSuccess(filePath: String, url: String, publicId: String) {
                try {
                    if (position >= 0 && position < attachments.size) {
                        val attachment = attachments[position]
                        if (filePath == attachment.localPath) {
                            attachment.uploadState = "success"
                            // The URL and publicId are not part of the Attachment data class
                            // but you might want to update the attachment object with this info
                            // if you add them to the data class.
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
                    if (position >= 0 && position < attachments.size) {
                        val currentItem = attachments[position]
                        if (filePath == currentItem.localPath) {
                            currentItem.uploadState = "failed"
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