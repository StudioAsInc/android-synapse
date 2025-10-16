package com.synapse.social.studioasinc.backend

import android.content.Context
import android.net.Uri
import com.synapse.social.studioasinc.SynapseApp
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IStorageService
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

class SupabaseStorageService : IStorageService {

    private val supabase = SupabaseClient.client
    private val context = SynapseApp.getContext()

    override fun uploadFile(uri: Uri, path: String, listener: ICompletionListener<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val result = supabase.storage.from("public").upload(path, bytes)
                    listener.onComplete(result, null)
                } else {
                    listener.onComplete(null, Exception("Failed to read file"))
                }
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun downloadUrl(path: String, listener: ICompletionListener<Uri>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = supabase.storage.from("public").publicUrl(path)
                listener.onComplete(Uri.parse(url), null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun deleteFile(path: String, listener: ICompletionListener<Unit>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.storage.from("public").delete(path)
                listener.onComplete(Unit, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }
}
