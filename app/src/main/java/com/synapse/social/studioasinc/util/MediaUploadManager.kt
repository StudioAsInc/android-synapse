package com.synapse.social.studioasinc.util

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.synapse.social.studioasinc.ImageUploader
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
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
                            // Video upload is not implemented as per user request.
                            // For now, we will just mark it as failed.
                            onError("Video upload is not supported.")
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
}