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
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileExtension = context.contentResolver.getType(uri)?.split("/")?.last() ?: "jpg"
                val fileName = "${UUID.randomUUID()}.$fileExtension"
                val bucket = storage["avatars"]

                val data = inputStream?.readBytes()
                if (data == null) {
                    Result.failure(Exception("Failed to read image data"))
                } else {
                    bucket.upload(fileName, data)
                    val publicUrl = bucket.publicUrl(fileName)
                    Result.success(publicUrl)
                }
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}
