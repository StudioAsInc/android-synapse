package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.Uri
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * Manager for uploading media files to Supabase Storage
 */
object MediaUploadManager {
    
    /**
     * Uploads multiple media items to Supabase Storage
     */
    suspend fun uploadMultipleMedia(
        context: Context,
        mediaItems: List<MediaItem>,
        onProgress: (Float) -> Unit,
        onComplete: (List<MediaItem>) -> Unit,
        onError: (String) -> Unit
    ) {
        withContext(Dispatchers.IO) {
            try {
                val uploadedItems = mutableListOf<MediaItem>()
                val storage = SupabaseClient.client.storage
                
                mediaItems.forEachIndexed { index, mediaItem ->
                    try {
                        // Generate unique filename
                        val timestamp = System.currentTimeMillis()
                        val extension = when (mediaItem.type) {
                            MediaType.IMAGE -> "jpg"
                            MediaType.VIDEO -> "mp4"
                        }
                        val fileName = "${mediaItem.type.name.lowercase()}_${timestamp}_${UUID.randomUUID()}.${extension}"
                        
                        // Determine bucket based on media type
                        val bucketName = when (mediaItem.type) {
                            MediaType.IMAGE -> "post-images"
                            MediaType.VIDEO -> "post-videos"
                        }
                        
                        // Read file bytes
                        val bytes: ByteArray? = try {
                            val uri = Uri.parse(mediaItem.url)
                            if (uri.scheme == "content") {
                                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                            } else {
                                val file = File(mediaItem.url)
                                if (file.exists()) file.readBytes() else null
                            }
                        } catch (e: Exception) {
                            val file = File(mediaItem.url)
                            if (file.exists()) file.readBytes() else null
                        }

                        if (bytes == null) {
                            android.util.Log.w("MediaUpload", "File not found or unreadable: ${mediaItem.url}, using original URL")
                            uploadedItems.add(mediaItem)
                        } else {
                            // Upload to Supabase Storage
                            val bucket = storage.from(bucketName)
                            bucket.upload(fileName, bytes)
                            
                            // Get public URL
                            val publicUrl = bucket.publicUrl(fileName)
                            
                            // Create uploaded media item
                            val uploadedItem = mediaItem.copy(
                                id = UUID.randomUUID().toString(),
                                url = publicUrl,
                                size = bytes.size.toLong(),
                                mimeType = when (mediaItem.type) {
                                    MediaType.IMAGE -> "image/jpeg"
                                    MediaType.VIDEO -> "video/mp4"
                                }
                            )
                            
                            uploadedItems.add(uploadedItem)
                            android.util.Log.d("MediaUpload", "Uploaded ${mediaItem.type}: $publicUrl")
                        }
                    } catch (uploadError: Exception) {
                        android.util.Log.w("MediaUpload", "Failed to upload ${mediaItem.url}: ${uploadError.message}")
                        // Add original item if upload fails
                        uploadedItems.add(mediaItem)
                    }
                    
                    // Update progress
                    val progress = (index + 1).toFloat() / mediaItems.size
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
                
                withContext(Dispatchers.Main) {
                    onComplete(uploadedItems)
                }
            } catch (e: Exception) {
                android.util.Log.e("MediaUpload", "Media upload failed", e)
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Upload failed")
                }
            }
        }
    }
    
    /**
     * Upload single media item
     */
    suspend fun uploadSingleMedia(
        context: Context,
        mediaItem: MediaItem,
        onProgress: (Float) -> Unit = {},
        onComplete: (MediaItem) -> Unit,
        onError: (String) -> Unit
    ) {
        uploadMultipleMedia(
            context,
            listOf(mediaItem),
            onProgress,
            { items -> onComplete(items.first()) },
            onError
        )
    }
}