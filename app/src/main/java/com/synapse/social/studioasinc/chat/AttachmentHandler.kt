package com.synapse.social.studioasinc.chat

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.AsyncUploadService
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.SketchwareUtil
import com.synapse.social.studioasinc.StorageUtil
import com.synapse.social.studioasinc.attachments.Rv_attacmentListAdapter
import com.synapse.social.studioasinc.PresenceManager
import com.google.firebase.auth.FirebaseAuth

class AttachmentHandler(
    private val activity: Activity,
    private val attachmentLayout: View,
    private val attachmentsRecyclerView: RecyclerView,
    private val listener: AttachmentListener
) {
    val attachments = ArrayList<HashMap<String, Any>>()
    private val attachmentAdapter = Rv_attacmentListAdapter(activity, attachments, attachmentLayout)

    init {
        attachmentsRecyclerView.adapter = attachmentAdapter
    }
    fun handleAttachmentResult(data: Intent?) {
        if (data == null) return

        val resolvedFilePaths = ArrayList<String>()
        try {
            if (data.clipData != null) {
                for (i in 0 until data.clipData!!.itemCount) {
                    val fileUri = data.clipData!!.getItemAt(i).uri
                    StorageUtil.getPathFromUri(activity, fileUri)?.let { resolvedFilePaths.add(it) }
                }
            } else if (data.data != null) {
                StorageUtil.getPathFromUri(activity, data.data!!)?.let { resolvedFilePaths.add(it) }
            }
        } catch (e: Exception) {
            Log.e("AttachmentHandler", "Error processing file picker result: ${e.message}")
            Toast.makeText(activity, "Error processing selected files", Toast.LENGTH_SHORT).show()
            return
        }

        if (resolvedFilePaths.isNotEmpty()) {
            attachmentLayout.visibility = View.VISIBLE
            val startingPosition = attachments.size

            for (filePath in resolvedFilePaths) {
                try {
                    val itemMap = HashMap<String, Any>()
                    itemMap["localPath"] = filePath
                    itemMap["uploadState"] = "pending" // pending, uploading, success, failed
                    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(filePath, options)
                    itemMap["width"] = if (options.outWidth > 0) options.outWidth else 100
                    itemMap["height"] = if (options.outHeight > 0) options.outHeight else 100
                    attachments.add(itemMap)
                } catch (e: Exception) {
                    Log.e("AttachmentHandler", "Error processing file: $filePath, Error: ${e.message}")
                }
            }

            attachmentAdapter.notifyItemRangeInserted(startingPosition, resolvedFilePaths.size)

            for (i in resolvedFilePaths.indices) {
                startUploadForItem(startingPosition + i)
            }
        } else {
            Toast.makeText(activity, "No valid files selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startUploadForItem(position: Int) {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            PresenceManager.setActivity(auth.currentUser!!.uid, "Sending an attachment")
        }

        if (position < 0 || position >= attachments.size) {
            Log.e("AttachmentHandler", "Invalid position for upload: $position")
            return
        }

        if (!SketchwareUtil.isConnected(activity)) {
            attachments[position]["uploadState"] = "failed"
            attachmentAdapter.notifyItemChanged(position)
            return
        }

        val itemMap = attachments[position]
        if (itemMap["uploadState"] != "pending") return

        itemMap["uploadState"] = "uploading"
        itemMap["uploadProgress"] = 0.0
        attachmentAdapter.notifyItemChanged(position)

        val filePath = itemMap["localPath"].toString()
        val file = java.io.File(filePath)
        if (!file.exists()) {
            itemMap["uploadState"] = "failed"
            attachmentAdapter.notifyItemChanged(position)
            return
        }

        AsyncUploadService.uploadWithNotification(activity, filePath, file.name, object : AsyncUploadService.UploadProgressListener {
            override fun onProgress(path: String, percent: Int) {
                if (position < attachments.size && path == attachments[position]["localPath"]) {
                    attachments[position]["uploadProgress"] = percent.toDouble()
                    attachmentAdapter.notifyItemChanged(position)
                }
            }

            override fun onSuccess(path: String, url: String, publicId: String) {
                if (position < attachments.size && path == attachments[position]["localPath"]) {
                    attachments[position]["uploadState"] = "success"
                    attachments[position]["cloudinaryUrl"] = url
                    attachments[position]["publicId"] = publicId
                    attachmentAdapter.notifyItemChanged(position)
                    listener.onAttachmentUploaded(url)
                }
            }

            override fun onFailure(path: String, error: String) {
                 if (position < attachments.size && path == attachments[position]["localPath"]) {
                    attachments[position]["uploadState"] = "failed"
                    attachmentAdapter.notifyItemChanged(position)
                }
                Log.e("AttachmentHandler", "Upload failed: $error")
            }
        })
    }

    fun resetAttachmentState() {
        attachmentLayout.visibility = View.GONE
        val oldSize = attachments.size
        if (oldSize > 0) {
            attachments.clear()
            attachmentAdapter.notifyItemRangeRemoved(0, oldSize)
        }
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            PresenceManager.setActivity(auth.currentUser!!.uid, "Idle")
        }
    }

    interface AttachmentListener {
        fun onAttachmentUploaded(url: String)
    }
}