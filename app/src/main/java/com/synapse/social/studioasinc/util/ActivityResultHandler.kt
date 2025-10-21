package com.synapse.social.studioasinc.util

import android.app.Activity
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.Toast
import com.synapse.social.studioasinc.ChatActivity
import com.synapse.social.studioasinc.StorageUtil
import java.util.ArrayList
import java.util.HashMap

/**
 * Handles activity results for file picking and attachment processing in chat.
 * Migrated to work with Supabase storage and modern Android file handling.
 */
class ActivityResultHandler(private val activity: ChatActivity) {

    companion object {
        private const val TAG = "ActivityResultHandler"
    }

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == activity.REQ_CD_IMAGE_PICKER && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                processSelectedFiles(data)
            }
        }
    }

    private fun processSelectedFiles(data: Intent) {
        val resolvedFilePaths = ArrayList<String>()

        try {
            // Handle multiple files
            if (data.clipData != null) {
                for (i in 0 until data.clipData!!.itemCount) {
                    val fileUri = data.clipData!!.getItemAt(i).uri
                    val path = StorageUtil.getPathFromUri(activity.applicationContext, fileUri)
                    if (path != null && path.isNotEmpty()) {
                        resolvedFilePaths.add(path)
                    } else {
                        Log.w(TAG, "Failed to resolve file path for clip data item $i")
                    }
                }
            } 
            // Handle single file
            else if (data.data != null) {
                val fileUri = data.data
                val path = StorageUtil.getPathFromUri(activity.applicationContext, fileUri)
                if (path != null && path.isNotEmpty()) {
                    resolvedFilePaths.add(path)
                } else {
                    Log.w(TAG, "Failed to resolve file path for single data")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing file picker result: ${e.message}")
            Toast.makeText(activity, "Error processing selected files", Toast.LENGTH_SHORT).show()
            return
        }

        if (resolvedFilePaths.isNotEmpty()) {
            addAttachmentsToChat(resolvedFilePaths)
        } else {
            Log.w(TAG, "No valid file paths resolved from file picker")
            Toast.makeText(activity, "No valid files selected", Toast.LENGTH_SHORT).show()
        }
    }

    private fun addAttachmentsToChat(filePaths: List<String>) {
        try {
            // Show attachment layout
            activity.attachmentLayoutListHolder.visibility = View.VISIBLE

            val startingPosition = activity.attactmentmap.size

            // Process each file
            for (filePath in filePaths) {
                try {
                    val itemMap = createAttachmentItem(filePath)
                    activity.attactmentmap.add(itemMap)
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing file: $filePath, Error: ${e.message}")
                }
            }

            // Notify adapter of changes
            activity.rv_attacmentList.adapter?.let { adapter ->
                adapter.notifyItemRangeInserted(startingPosition, filePaths.size)
            }

            // Start upload for each item
            for (i in filePaths.indices) {
                try {
                    activity._startUploadForItem((startingPosition + i).toDouble())
                } catch (e: Exception) {
                    Log.e(TAG, "Error starting upload for item $i: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding attachments to chat: ${e.message}")
            Toast.makeText(activity, "Error adding attachments", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createAttachmentItem(filePath: String): HashMap<String, Any> {
        val itemMap = HashMap<String, Any>()
        itemMap["localPath"] = filePath
        itemMap["uploadState"] = "pending"

        // Get image dimensions safely
        try {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true
            BitmapFactory.decodeFile(filePath, options)
            
            itemMap["width"] = if (options.outWidth > 0) options.outWidth else 100
            itemMap["height"] = if (options.outHeight > 0) options.outHeight else 100
        } catch (e: Exception) {
            Log.w(TAG, "Could not decode image dimensions for: $filePath")
            itemMap["width"] = 100
            itemMap["height"] = 100
        }

        return itemMap
    }

    /**
     * Handle camera capture results
     */
    fun handleCameraResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Implementation for camera capture if needed
        // This would handle results from camera intents
    }

    /**
     * Handle document picker results
     */
    fun handleDocumentResult(requestCode: Int, resultCode: Int, data: Intent?) {
        // Implementation for document picker if needed
        // This would handle results from document picker intents
    }
}