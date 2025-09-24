package com.synapse.social.studioasinc.util

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.synapse.social.studioasinc.Supabase.client
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

object MediaUploadManager {
    
    fun uploadMultipleMedia(
        mediaItems: List<MediaItem>,
        onProgress: (Float) -> Unit,
        onComplete: (List<MediaItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uploadedItems = mutableListOf<MediaItem>()
                var completedUploads = 0
                
                mediaItems.forEach { mediaItem ->
                    when (mediaItem.type) {
                        MediaType.IMAGE -> {
                            uploadImage(mediaItem.url) { uploadedUrl ->
                                if (uploadedUrl != null) {
                                    uploadedItems.add(mediaItem.copy(url = uploadedUrl))
                                    completedUploads++
                                    onProgress(completedUploads.toFloat() / mediaItems.size)
                                    
                                    if (completedUploads == mediaItems.size) {
                                        onComplete(uploadedItems)
                                    }
                                } else {
                                    onError("Failed to upload image")
                                }
                            }
                        }
                        MediaType.VIDEO -> {
                            uploadVideo(mediaItem.url) { uploadedUrl, thumbnailUrl ->
                                if (uploadedUrl != null) {
                                    uploadedItems.add(mediaItem.copy(
                                        url = uploadedUrl,
                                        thumbnailUrl = thumbnailUrl
                                    ))
                                    completedUploads++
                                    onProgress(completedUploads.toFloat() / mediaItems.size)
                                    
                                    if (completedUploads == mediaItems.size) {
                                        onComplete(uploadedItems)
                                    }
                                } else {
                                    onError("Failed to upload video")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                onError(e.message ?: "Unknown error occurred")
            }
        }
    }
    
    private fun uploadImage(
        localPath: String,
        onComplete: (String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(localPath)
                val url = client.storage["media"].upload("${UUID.randomUUID()}.${file.extension}", file.readBytes(), upsert = true)
                onComplete(url)
            } catch (e: Exception) {
                onComplete(null)
            }
        }
    }
    
    private fun uploadVideo(
        localPath: String,
        onComplete: (String?, String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val videoFile = File(localPath)
                
                // Generate thumbnail
                val thumbnail = generateVideoThumbnail(localPath)
                
                // Upload video
                val videoUrl = client.storage["media"].upload("${UUID.randomUUID()}.mp4", videoFile.readBytes(), upsert = true)
                
                // Upload thumbnail if generated
                if (thumbnail != null) {
                    uploadThumbnail(thumbnail) { thumbnailUrl ->
                        onComplete(videoUrl, thumbnailUrl)
                    }
                } else {
                    onComplete(videoUrl, null)
                }
            } catch (e: Exception) {
                onComplete(null, null)
            }
        }
    }
    
    private fun generateVideoThumbnail(videoPath: String): ByteArray? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
            
            // Get frame at 1 second
            val bitmap = retriever.getFrameAtTime(1000000) // 1 second in microseconds
            retriever.release()
            
            if (bitmap != null) {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                outputStream.toByteArray()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun uploadThumbnail(
        thumbnailData: ByteArray,
        onComplete: (String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = client.storage["media"].upload("${UUID.randomUUID()}.jpg", thumbnailData, upsert = true)
                onComplete(url)
            } catch (e: Exception) {
                onComplete(null)
            }
        }
    }
}