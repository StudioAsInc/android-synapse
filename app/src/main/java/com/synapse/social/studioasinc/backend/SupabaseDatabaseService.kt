package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.*
import java.io.File
import java.util.UUID

class SupabaseDatabaseService(private val supabase: SupabaseClient) : IDatabaseService {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun getReference(path: String): IDatabaseReference {
        return SupabaseDatabaseReference(supabase, path, this)
    }

    override fun getData(query: IQuery, listener: IDataListener) {
        serviceScope.launch {
            try {
                // This is a simplification. A real implementation would need to parse the query object.
                val path = (query as SupabaseDatabaseReference).path
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

    override fun setValue(ref: IDatabaseReference, value: Any?, listener: ICompletionListener<Unit>) {
        serviceScope.launch {
            try {
                val path = (ref as SupabaseDatabaseReference).path
                supabase.from(path).upsert(value!!, onConflict = "id")
                withContext(Dispatchers.Main) {
                    listener.onComplete(Unit, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener.onComplete(null, e.message)
                }
            }
        }
    }

    override fun updateChildren(ref: IDatabaseReference, updates: Map<String, Any?>, listener: ICompletionListener<Unit>) {
        serviceScope.launch {
            try {
                val path = (ref as SupabaseDatabaseReference).path
                val table = path.substringBeforeLast("/")
                val id = path.substringAfterLast("/")
                supabase.from(table).update(updates) {
                    filter {
                        eq("id", id)
                    }
                }
                withContext(Dispatchers.Main) {
                    listener.onComplete(Unit, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener.onComplete(null, e.message)
                }
            }
        }
    }

    override fun uploadFile(bucket: String, path: String, filePath: String, listener: ICompletionListener<String>) {
        serviceScope.launch {
            try {
                val file = File(filePath)
                val data = file.readBytes()
                val publicUrl = supabase.storage.from(bucket).upload(path, data, upsert = true)
                withContext(Dispatchers.Main) {
                    listener.onComplete(publicUrl, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener.onComplete(null, e.message)
                }
            }
        }
    }

    override fun addRealtimeListener(ref: IDatabaseReference, listener: IRealtimeListener): IRealtimeChannel {
        // Placeholder implementation
        return object : IRealtimeChannel {
            override fun unsubscribe() {}
        }
    }

    override fun removeRealtimeListener(channel: IRealtimeChannel) {
        // Placeholder implementation
        channel.unsubscribe()
    }

    override fun searchUsers(query: String, listener: IDataListener) {
        serviceScope.launch {
            try {
                val response = supabase.from("users")
                    .select {
                        filter {
                            or {
                                ilike("username", "%$query%")
                                ilike("nickname", "%$query%")
                            }
                        }
                    }.decodeList<Map<String, Any?>>()
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
    val supabase: SupabaseClient,
    val path: String,
    private val dbService: SupabaseDatabaseService
) : IDatabaseReference {

    override fun child(s: String): IDatabaseReference {
        val newPath = "${path.removeSuffix("/")}/${s.removePrefix("/")}"
        return SupabaseDatabaseReference(supabase, newPath, dbService)
    }

    override fun push(): IDatabaseReference {
        val newPath = "${path.removeSuffix("/")}/${UUID.randomUUID()}"
        return SupabaseDatabaseReference(supabase, newPath, dbService)
    }

    override val key: String?
        get() = path.substringAfterLast('/')

    // IQuery placeholder implementations
    override fun orderByChild(path: String): IQuery = this
    override fun equalTo(value: String?): IQuery = this
    override fun limitToLast(limit: Int): IQuery = this
    override fun limitToFirst(limit: Int): IQuery = this
    override fun startAt(value: String): IQuery = this
    override fun endAt(value: String): IQuery = this
    override fun orderByKey(): IQuery = this
    override fun endBefore(value: String?): IQuery = this
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