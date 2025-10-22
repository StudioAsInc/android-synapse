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
                
                // Process and clean the image to fix corruption issues
                val cleanedImageData = processAndCleanImage(context, uri)
                if (cleanedImageData == null) {
                    return@withContext Result.failure(Exception("Failed to process image. Please try a different image."))
                }
                
                val fileName = "${UUID.randomUUID()}.jpg" // Always use JPG for consistency
                android.util.Log.d("SupabaseStorage", "Generated filename: $fileName")
                
                // Try to get the bucket
                val bucket = try {
                    storage["avatars"]
                } catch (e: Exception) {
                    android.util.Log.e("SupabaseStorage", "Failed to access 'avatars' bucket", e)
                    return@withContext Result.failure(Exception("Storage bucket 'avatars' not found. Please check Supabase storage configuration."))
                }
                
                android.util.Log.d("SupabaseStorage", "Cleaned image data size: ${cleanedImageData.size} bytes")
                
                // Upload the cleaned file
                try {
                    bucket.upload(fileName, cleanedImageData)
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
    
    private suspend fun processAndCleanImage(context: Context, uri: Uri): ByteArray? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                android.util.Log.e("SupabaseStorage", "Failed to open input stream")
                return null
            }
            
            // Decode the bitmap to clean any corruption
            val bitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            
            if (bitmap == null) {
                android.util.Log.e("SupabaseStorage", "Failed to decode bitmap - image may be corrupted")
                return null
            }
            
            android.util.Log.d("SupabaseStorage", "Original bitmap: ${bitmap.width}x${bitmap.height}")
            
            // Resize if too large (max 1024x1024 for profile images)
            val maxSize = 1024
            val scaledBitmap = if (bitmap.width > maxSize || bitmap.height > maxSize) {
                val scale = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
                val newWidth = (bitmap.width * scale).toInt()
                val newHeight = (bitmap.height * scale).toInt()
                android.graphics.Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
            } else {
                bitmap
            }
            
            android.util.Log.d("SupabaseStorage", "Scaled bitmap: ${scaledBitmap.width}x${scaledBitmap.height}")
            
            // Compress to clean JPEG format
            val outputStream = java.io.ByteArrayOutputStream()
            val compressed = scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, outputStream)
            
            if (!compressed) {
                android.util.Log.e("SupabaseStorage", "Failed to compress bitmap")
                return null
            }
            
            val cleanedData = outputStream.toByteArray()
            outputStream.close()
            
            // Clean up bitmaps
            if (scaledBitmap != bitmap) {
                scaledBitmap.recycle()
            }
            bitmap.recycle()
            
            android.util.Log.d("SupabaseStorage", "Image processed successfully: ${cleanedData.size} bytes")
            cleanedData
            
        } catch (e: Exception) {
            android.util.Log.e("SupabaseStorage", "Failed to process image", e)
            null
        }
    }
}
