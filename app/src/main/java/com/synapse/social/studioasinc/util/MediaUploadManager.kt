package com.synapse.social.studioasinc.util

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.synapse.social.studioasinc.ImageUploader
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import io.github.jan_tennert.supabase.SupabaseClient
import io.github.jan_tennert.supabase.storage.Storage
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.util.UUID

class MediaUploadManager(private val supabase: SupabaseClient) { // Changed from object to class

    private val storage = supabase.storage // Supabase Storage instance

    fun uploadMultipleMedia(
        mediaItems: List<MediaItem>,
        onProgress: (Float) -> Unit,
        onComplete: (List<MediaItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val uploadedItems = mutableListOf<MediaItem>()
                val totalItems = mediaItems.size
                var completedUploads = 0

                mediaItems.forEach { mediaItem ->
                    when (mediaItem.type) {
                        MediaType.IMAGE -> {
                            // Using existing ImageUploader for images, assuming it will be migrated separately or uses local paths
                            // If ImageUploader uses FirebaseStorage, it needs to be updated.
                            uploadImage(mediaItem.url) { uploadedUrl ->
                                if (uploadedUrl != null) {
                                    uploadedItems.add(mediaItem.copy(url = uploadedUrl))
                                    completedUploads++
                                    onProgress(completedUploads.toFloat() / totalItems)

                                    if (completedUploads == totalItems) {
                                        onComplete(uploadedItems)
                                    }
                                } else {
                                    onError("Failed to upload image: ${mediaItem.url}")
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
                                    onProgress(completedUploads.toFloat() / totalItems)

                                    if (completedUploads == totalItems) {
                                        onComplete(uploadedItems)
                                    }
                                } else {
                                    onError("Failed to upload video: ${mediaItem.url}")
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Unknown error occurred during multiple media upload")
                }
            }
        }
    }

    private fun uploadImage(
        localPath: String,
        onComplete: (String?) -> Unit
    ) {
        // Continue using existing ImageUploader, as it's an abstraction.
        // It's assumed that ImageUploader itself will be migrated to Supabase Storage.
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
                if (!videoFile.exists()) {
                    withContext(Dispatchers.Main) { onComplete(null, null) }
                    return@launch
                }

                // Generate thumbnail
                val thumbnailData = generateVideoThumbnail(localPath)

                // Upload video
                val videoFileName = "videos/${UUID.randomUUID()}.mp4"
                val videoBytes = videoFile.readBytes() // Read video file into byte array

                storage.from("videos").upload(videoFileName, videoBytes, false)
                val videoUrl = storage.from("videos").getPublicUrl(videoFileName)

                var thumbnailUrl: String? = null
                if (thumbnailData != null) {
                    thumbnailUrl = uploadThumbnail(thumbnailData)
                }
                withContext(Dispatchers.Main) { onComplete(videoUrl, thumbnailUrl) }
            } catch (e: Exception) {
                Log.e("MediaUploadManager", "Error uploading video: ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete(null, null) }
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
            Log.e("MediaUploadManager", "Error generating video thumbnail: ${e.message}", e)
            null
        }
    }

    private suspend fun uploadThumbnail(
        thumbnailData: ByteArray
    ): String? {
        return withContext(Dispatchers.IO) {
            try {
                val thumbnailFileName = "thumbnails/${UUID.randomUUID()}.jpg"
                storage.from("thumbnails").upload(thumbnailFileName, thumbnailData, false)
                storage.from("thumbnails").getPublicUrl(thumbnailFileName)
            } catch (e: Exception) {
                Log.e("MediaUploadManager", "Error uploading thumbnail: ${e.message}", e)
                null
            }
        }
    }
}
