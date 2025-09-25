package com.synapse.social.studioasinc.util

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import com.synapse.social.studioasinc.services.FileUploaderService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File

object MediaUploadManager {

    private val fileUploaderService = FileUploaderService()

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
        val file = File(localPath)
        fileUploaderService.uploadFile(localPath, file.name, object : FileUploaderService.UploadListener {
            override fun onProgress(percent: Int) {}
            override fun onSuccess(url: String, publicId: String) {
                onComplete(url)
            }
            override fun onFailure(error: String) {
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
                val thumbnail = generateVideoThumbnail(localPath)
                val file = File(localPath)
                fileUploaderService.uploadFile(localPath, file.name, object : FileUploaderService.UploadListener {
                    override fun onProgress(percent: Int) {}
                    override fun onSuccess(url: String, publicId: String) {
                        if (thumbnail != null) {
                            uploadThumbnail(thumbnail) { thumbnailUrl ->
                                onComplete(url, thumbnailUrl)
                            }
                        } else {
                            onComplete(url, null)
                        }
                    }
                    override fun onFailure(error: String) {
                        onComplete(null, null)
                    }
                })
            } catch (e: Exception) {
                onComplete(null, null)
            }
        }
    }

    private fun generateVideoThumbnail(videoPath: String): ByteArray? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoPath)
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
        val tempFile = File.createTempFile("thumbnail", ".jpg")
        tempFile.writeBytes(thumbnailData)
        fileUploaderService.uploadFile(tempFile.absolutePath, tempFile.name, object : FileUploaderService.UploadListener {
            override fun onProgress(percent: Int) {}
            override fun onSuccess(url: String, publicId: String) {
                onComplete(url)
                tempFile.delete()
            }
            override fun onFailure(error: String) {
                onComplete(null)
                tempFile.delete()
            }
        })
    }
}