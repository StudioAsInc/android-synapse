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

class ActivityResultHandler(private val activity: ChatActivity) {

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == activity.REQ_CD_IMAGE_PICKER && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val resolvedFilePaths = ArrayList<String>()

                try {
                    data.clipData?.let { clipData ->
                        for (i in 0 until clipData.itemCount) {
                            val fileUri = clipData.getItemAt(i).uri
                            val path = StorageUtil.getPathFromUri(activity.applicationContext, fileUri)
                            if (path != null && path.isNotEmpty()) {
                                resolvedFilePaths.add(path)
                            } else {
                                Log.w("ChatActivity", "Failed to resolve file path for clip data item $i")
                            }
                        }
                    } ?: data.data?.let { uri ->
                        val path = StorageUtil.getPathFromUri(activity.applicationContext, uri)
                        if (path != null && path.isNotEmpty()) {
                            resolvedFilePaths.add(path)
                        } else {
                            Log.w("ChatActivity", "Failed to resolve file path for single data")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("ChatActivity", "Error processing file picker result: " + e.message)
                    Toast.makeText(activity, "Error processing selected files", Toast.LENGTH_SHORT).show()
                    return
                }

                if (resolvedFilePaths.isNotEmpty()) {
                    activity.attachmentLayoutListHolder.visibility = View.VISIBLE

                    val startingPosition = activity.attactmentmap.size

                    for (filePath in resolvedFilePaths) {
                        try {
                            val itemMap = HashMap<String, Any>()
                            itemMap["localPath"] = filePath
                            itemMap["uploadState"] = "pending"

                            // Get image dimensions safely
                            val options = BitmapFactory.Options()
                            options.inJustDecodeBounds = true
                            try {
                                BitmapFactory.decodeFile(filePath, options)
                                itemMap["width"] = if (options.outWidth > 0) options.outWidth else 100
                                itemMap["height"] = if (options.outHeight > 0) options.outHeight else 100
                            } catch (e: Exception) {
                                Log.w("ChatActivity", "Could not decode image dimensions for: $filePath")
                                itemMap["width"] = 100
                                itemMap["height"] = 100
                            }

                            activity.attactmentmap.add(itemMap)
                        } catch (e: Exception) {
                            Log.e("ChatActivity", "Error processing file: $filePath, Error: " + e.message)
                        }
                    }

                    // Notify adapter of changes
                    activity.rv_attacmentList.adapter?.notifyItemRangeInserted(startingPosition, resolvedFilePaths.size)

                    // Start upload for each item
                    for (i in resolvedFilePaths.indices) {
                        try {
                            activity._startUploadForItem(startingPosition + i)
                        } catch (e: Exception) {
                            Log.e("ChatActivity", "Error starting upload for item " + i + ": " + e.message)
                        }
                    }
                } else {
                    Log.w("ChatActivity", "No valid file paths resolved from file picker")
                    Toast.makeText(activity, "No valid files selected", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}