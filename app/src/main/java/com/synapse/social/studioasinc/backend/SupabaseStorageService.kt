package com.synapse.social.studioasinc.backend

import android.content.Context
import android.net.Uri
import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

/**
 * Supabase Storage Service
 * Handles file uploads to Supabase Storage
 */
class SupabaseStorageService {
    
    private val client = SupabaseClient.client
    private val storage = client.storage
    
    /**
     * Upload avatar image to Supabase Storage
     * @param userId User ID for folder organization
     * @param filePath Local file path
     * @return Public URL of uploaded image
     */
    suspend fun uploadAvatar(userId: String, filePath: String): Result<String> {
        return uploadImage("avatars", userId, filePath)
    }
    
    /**
     * Upload cover image to Supabase Storage
     * @param userId User ID for folder organization
     * @param filePath Local file path
     * @return Public URL of uploaded image
     */
    suspend fun uploadCover(userId: String, filePath: String): Result<String> {
        return uploadImage("covers", userId, filePath)
    }
    
    /**
     * Upload post image to Supabase Storage
     * @param userId User ID for folder organization
     * @param filePath Local file path
     * @return Public URL of uploaded image
     */
    suspend fun uploadPostImage(userId: String, filePath: String): Result<String> {
        return uploadImage("posts", userId, filePath)
    }
    
    /**
     * Generic image upload function
     */
    private suspend fun uploadImage(bucket: String, userId: String, filePath: String): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("SupabaseStorage", "Uploading image from: $filePath to bucket: $bucket")
                
                val file = File(filePath)
                if (!file.exists()) {
                    return@withContext Result.failure(Exception("File not found: $filePath"))
                }
                
                val fileBytes = file.readBytes()
                val fileName = "${UUID.randomUUID()}.${file.extension}"
                val path = "$userId/$fileName"
                
                // Upload to Supabase Storage
                storage.from(bucket).upload(path, fileBytes, upsert = false)
                
                // Get public URL
                val publicUrl = storage.from(bucket).publicUrl(path)
                
                android.util.Log.d("SupabaseStorage", "Upload successful: $publicUrl")
                Result.success(publicUrl)
                
            } catch (e: Exception) {
                android.util.Log.e("SupabaseStorage", "Upload failed", e)
                Result.failure(Exception("Upload failed: ${e.message}"))
            }
        }
    }
    
    /**
     * Delete image from Supabase Storage
     */
    suspend fun deleteImage(bucket: String, path: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                storage.from(bucket).delete(path)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Extract path from public URL
     */
    fun extractPathFromUrl(url: String, bucket: String): String? {
        return try {
            val bucketPath = "/storage/v1/object/public/$bucket/"
            if (url.contains(bucketPath)) {
                url.substringAfter(bucketPath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
