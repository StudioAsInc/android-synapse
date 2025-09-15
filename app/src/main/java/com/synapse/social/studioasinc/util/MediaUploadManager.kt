package com.synapse.social.studioasinc.util

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.synapse.social.studioasinc.ImageUploader
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID

object MediaUploadManager {
    
    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference
    
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
        // Use existing ImageUploader
        ImageUploader.uploadImage(localPath, object : ImageUploader.UploadCallback {
            override fun onUploadComplete(imageUrl: String) {
                onComplete(imageUrl)
            }
            
            override fun onUploadError(errorMessage: String) {
                onComplete(null)
            }
        })
    }
    
    private fun uploadVideo(
        localPath: String,
        onComplete: (String?, String?) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val videoFile = File(localPath)
                val videoUri = Uri.fromFile(videoFile)
                
                // Generate thumbnail
                val thumbnail = generateVideoThumbnail(localPath)
                
                // Upload video
                val videoRef = storageRef.child("videos/${UUID.randomUUID()}.mp4")
                val uploadTask = videoRef.putFile(videoUri)
                
                uploadTask.addOnSuccessListener { taskSnapshot ->
                    videoRef.downloadUrl.addOnSuccessListener { videoUrl ->
                        // Upload thumbnail if generated
                        if (thumbnail != null) {
                            uploadThumbnail(thumbnail) { thumbnailUrl ->
                                onComplete(videoUrl.toString(), thumbnailUrl)
                            }
                        } else {
                            onComplete(videoUrl.toString(), null)
                        }
                    }
                }.addOnFailureListener {
                    onComplete(null, null)
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
        val thumbnailRef = storageRef.child("thumbnails/${UUID.randomUUID()}.jpg")
        
        thumbnailRef.putBytes(thumbnailData)
            .addOnSuccessListener {
                thumbnailRef.downloadUrl.addOnSuccessListener { url ->
                    onComplete(url.toString())
                }
            }
            .addOnFailureListener {
                onComplete(null)
            }
    }
}