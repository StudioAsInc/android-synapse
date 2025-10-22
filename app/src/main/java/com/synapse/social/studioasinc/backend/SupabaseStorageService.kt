package com.synapse.social.studioasinc.backend

import android.content.Context
import android.net.Uri
import io.github.jan.supabase.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class SupabaseStorageService(private val storage: Storage) {

    suspend fun uploadImage(context: Context, uri: Uri): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("SupabaseStorage", "Starting image upload for URI: $uri")
                
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream == null) {
                    android.util.Log.e("SupabaseStorage", "Failed to open input stream for URI: $uri")
                    return@withContext Result.failure(Exception("Cannot access image file. Please try selecting the image again."))
                }
                
                val mimeType = context.contentResolver.getType(uri)
                android.util.Log.d("SupabaseStorage", "Image MIME type: $mimeType")
                
                val fileExtension = when {
                    mimeType?.contains("jpeg") == true || mimeType?.contains("jpg") == true -> "jpg"
                    mimeType?.contains("png") == true -> "png"
                    mimeType?.contains("webp") == true -> "webp"
                    else -> "jpg" // Default fallback
                }
                
                val fileName = "${UUID.randomUUID()}.$fileExtension"
                android.util.Log.d("SupabaseStorage", "Generated filename: $fileName")
                
                // Try to get the bucket - this might fail if bucket doesn't exist
                val bucket = try {
                    storage["avatars"]
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseStorage", "Failed to access 'avatars' bucket", e)
                    return@withContext Result.failure(Exception("Storage bucket 'avatars' not found. Please check Supabase storage configuration."))
                }

                val data = try {
                    inputStream.readBytes()
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseStorage", "Failed to read image data", e)
                    return@withContext Result.failure(Exception("Failed to read image data: ${e.message}"))
                } finally {
                    inputStream.close()
                }
                
                if (data.isEmpty()) {
                    android.util.Log.e("SupabaseStorage", "Image data is empty")
                    return@withContext Result.failure(Exception("Image file is empty or corrupted"))
                }
                
                android.util.Log.d("SupabaseStorage", "Image data size: ${data.size} bytes")
                
                // Upload the file
                try {
                    bucket.upload(fileName, data)
                    android.util.Log.d("SupabaseStorage", "File uploaded successfully: $fileName")
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseStorage", "Failed to upload file", e)
                    return@withContext Result.failure(Exception("Upload failed: ${e.message}"))
                }
                
                // Get the public URL
                val publicUrl = try {
                    bucket.publicUrl(fileName)
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseStorage", "Failed to get public URL", e)
                    return@withContext Result.failure(Exception("Failed to get image URL: ${e.message}"))
                }
                
                android.util.Log.d("SupabaseStorage", "Upload successful, public URL: $publicUrl")
                Result.success(publicUrl)
                
            } catch (e: Exception) {
                android.util.Log.e("SupabaseStorage", "Unexpected error during image upload", e)
                Result.failure(Exception("Image upload failed: ${e.message}"))
            }
        }
    }
}
