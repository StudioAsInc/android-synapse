package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import java.io.File

/**
 * Utility class for file operations
 */
object FileUtil {
    
    /**
     * Converts URI to file path
     */
    fun convertUriToFilePath(context: Context, uri: Uri): String? {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val columnIndex = it.getColumnIndex(MediaStore.Images.Media.DATA)
                    if (columnIndex != -1) {
                        it.getString(columnIndex)
                    } else {
                        // Fallback: return URI as string
                        uri.toString()
                    }
                } else {
                    uri.toString()
                }
            } ?: uri.toString()
        } catch (e: Exception) {
            uri.toString()
        }
    }
    
    /**
     * Gets file size from URI
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        return try {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val sizeIndex = it.getColumnIndex(MediaStore.Images.Media.SIZE)
                    if (sizeIndex != -1) {
                        it.getLong(sizeIndex)
                    } else {
                        0L
                    }
                } else {
                    0L
                }
            } ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
}