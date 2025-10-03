package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.*
import java.io.File
import java.util.Map
import java.util.UUID

class SupabaseDatabaseService : IDatabaseService {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }

    override fun getReference(path: String): IDatabaseReference {
        return SupabaseDatabaseReference(supabase, path)
    }

    override fun getUserById(uid: String, listener: IDataListener) {
        // Placeholder implementation
        listener.onCancelled(SupabaseDatabaseError("Not implemented", -1))
    }

    override fun setValue(path: String, value: Any, listener: ICompletionListener<Any?>) {
        serviceScope.launch {
            try {
                // Assuming path is table name and value contains primary key.
                supabase.from(path).upsert(value, onConflict = "id")
                withContext(Dispatchers.Main) {
                    listener.onComplete(value, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener.onComplete(null, e)
                }
            }
        }
    }

    override fun updateChildren(path: String, children: MutableMap<String, Any>, listener: ICompletionListener<*>) {
        serviceScope.launch {
            try {
                // Assuming path is table/id
                val table = path.substringBeforeLast("/")
                val id = path.substringAfterLast("/")
                supabase.from(table).update(children as Map<String, Any?>) {
                    filter {
                        eq("id", id)
                    }
                }
                withContext(Dispatchers.Main) {
                    (listener as ICompletionListener<Any?>).onComplete(null, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    (listener as ICompletionListener<Any?>).onComplete(null, e)
                }
            }
        }
    }

    override fun uploadFile(file: File, path: String, listener: ICompletionListener<Any?>) {
        serviceScope.launch {
            try {
                val data = file.readBytes()
                // Assuming a default bucket "synapse" and path is the remote path
                val publicUrl = supabase.storage.from("synapse").upload(path, data, upsert = true)
                withContext(Dispatchers.Main) {
                    listener.onComplete(publicUrl, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener.onComplete(null, e)
                }
            }
        }
    }

    override fun searchUsers(query: String, listener: IDataListener) {
        // Placeholder implementation
        listener.onCancelled(SupabaseDatabaseError("Not implemented", -1))
    }

    override fun getFollowers(uid: String, listener: IDataListener) {
        // Placeholder implementation
        listener.onCancelled(SupabaseDatabaseError("Not implemented", -1))
    }

    override fun getFollowing(uid: String, listener: IDataListener) {
        // Placeholder implementation
        listener.onCancelled(SupabaseDatabaseError("Not implemented", -1))
    }

    override fun getData(path: String, listener: IDataListener) {
        serviceScope.launch {
            try {
                val response = supabase.from(path).select().decodeList<Map<String, Any?>>()
                withContext(Dispatchers.Main) {
                    listener.onDataChange(SupabaseDataSnapshot(response))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener.onCancelled(SupabaseDatabaseError(e.message ?: "Unknown error", 0))
                }
            }
        }
    }
}

class SupabaseDatabaseReference(
    private val supabase: SupabaseClient,
    private var _path: String
) : IDatabaseReference {

    override fun child(s: String): IDatabaseReference {
        val newPath = "${_path.removeSuffix("/")}/${s.removePrefix("/")}"
        return SupabaseDatabaseReference(supabase, newPath)
    }

    override fun push(): IDatabaseReference {
        val newPath = "${_path.removeSuffix("/")}/${UUID.randomUUID()}"
        return SupabaseDatabaseReference(supabase, newPath)
    }

    override fun getKey(): String? {
        return _path.substringAfterLast('/')
    }
}

class SupabaseDataSnapshot(private val data: Any?) : IDataSnapshot {

    @Suppress("UNCHECKED_CAST")
    override fun <T> getValue(valueType: Class<T>): T? {
        return data as? T
    }

    override fun exists(): Boolean {
        return data != null
    }

    override val children: Iterable<IDataSnapshot>
        get() = if (data is List<*>) {
            data.map { SupabaseDataSnapshot(it) }
        } else {
            emptyList()
        }

    override val key: String?
        get() = (data as? Map<*, *>)?.get("id")?.toString()
}

class SupabaseDatabaseError(override val message: String, override val code: Int) : IDatabaseError