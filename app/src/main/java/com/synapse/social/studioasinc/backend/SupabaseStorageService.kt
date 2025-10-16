package com.synapse.social.studioasinc.backend

import android.net.Uri
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IStorageService
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class SupabaseStorageService : IStorageService {

    private val supabase = SupabaseClient.client

    override fun uploadFile(uri: Uri, path: String, listener: ICompletionListener<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val file = File(uri.path!!)
                val result = supabase.storage.from("public").upload(path, file.readBytes())
                listener.onComplete(result, null)
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
