package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SupabaseDatabaseService : IDatabaseService {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = "YOUR_SUPABASE_URL",
        supabaseKey = "YOUR_SUPABASE_KEY"
    ) {
        install(Postgrest)
        install(Realtime)
    }

    override fun getReference(path: String): IDatabaseReference {
        return SupabaseDatabaseReference(supabase, path)
    }

    override fun getData(query: IQuery, listener: IDataListener) {
        val supabaseQuery = query as SupabaseQuery
        serviceScope.launch {
            try {
                val response = supabaseQuery.query.select {
                    supabaseQuery.orderBy?.let { (column, order) ->
                        order(column, order)
                    }
                    supabaseQuery.limit?.let {
                        limit(it.toLong())
                    }
                    supabaseQuery.endBeforeValue?.let {
                        filter {
                            lt("id", it)
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

    override fun setValue(ref: IDatabaseReference, value: Any?, listener: ICompletionListener<Unit>) {
        val supabaseRef = ref as SupabaseDatabaseReference
        serviceScope.launch {
            try {
                if (value == null) {
                    supabaseRef.reference.delete()
                } else {
                    supabaseRef.reference.upsert(value, onConflict = "id")
                }
                withContext(Dispatchers.Main) {
                    listener.onComplete(Unit, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener.onComplete(null, e)
                }
            }
        }
    }

    override fun updateChildren(
        ref: IDatabaseReference,
        updates: Map<String, Any?>,
        listener: ICompletionListener<Unit>
    ) {
        val supabaseRef = ref as SupabaseDatabaseReference
        serviceScope.launch {
            try {
                supabaseRef.reference.update(updates)
                withContext(Dispatchers.Main) {
                    listener.onComplete(Unit, null)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener.onComplete(null, e)
                }
            }
        }
    }

    override fun addRealtimeListener(ref: IDatabaseReference, listener: IRealtimeListener): IRealtimeChannel {
        val supabaseRef = ref as SupabaseDatabaseReference
        val channel = supabase.channel(supabaseRef.path)
        serviceScope.launch {
            channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = supabaseRef.path
            }.onEach {
                withContext(Dispatchers.Main) {
                    listener.onInsert(SupabaseDataSnapshot(it.record))
                }
            }.launchIn(this)

            channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = supabaseRef.path
            }.onEach {
                withContext(Dispatchers.Main) {
                    listener.onUpdate(SupabaseDataSnapshot(it.record))
                }
            }.launchIn(this)

            channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                table = supabaseRef.path
            }.onEach {
                withContext(Dispatchers.Main) {
                    listener.onDelete(SupabaseDataSnapshot(it.oldRecord))
                }
            }.launchIn(this)

            channel.subscribe()
        }
        return SupabaseRealtimeChannel(channel)
    }

    override fun removeRealtimeListener(channel: IRealtimeChannel) {
        val supabaseChannel = channel as SupabaseRealtimeChannel
        serviceScope.launch {
            supabaseChannel.channel.unsubscribe()
        }
    }
}

abstract class SupabaseQuery(internal var query: PostgrestQueryBuilder) : IQuery {

    internal var orderBy: Pair<String, io.github.jan.supabase.postgrest.query.Order>? = null
    internal var limit: Int? = null
    internal var endBeforeValue: String? = null

    override fun orderByChild(path: String): IQuery {
        this.orderBy = Pair(path, io.github.jan.supabase.postgrest.query.Order.ASCENDING)
        return this
    }

    override fun equalTo(value: String?): IQuery {
        // This would require knowing the column to filter on.
        // e.g., query.eq("column_name", value)
        return this
    }

    override fun limitToLast(limit: Int): IQuery {
        // No direct equivalent. PostgREST has range(), but not from the end.
        return this
    }

    override fun limitToFirst(limit: Int): IQuery {
        this.limit = limit
        return this
    }

    override fun startAt(value: String): IQuery {
        // Requires column and is typically used with order().
        return this
    }

    override fun endAt(value: String): IQuery {
        // Requires column and is typically used with order().
        return this
    }

    override fun orderByKey(): IQuery {
        this.orderBy = Pair("id", io.github.jan.supabase.postgrest.query.Order.ASCENDING) // Assuming 'id' is the primary key
        return this
    }

    override fun endBefore(value: String?): IQuery {
        if (value != null) {
            this.endBeforeValue = value
        }
        return this
    }
}

class SupabaseDatabaseReference(
    private val supabase: SupabaseClient,
    internal val path: String
) : SupabaseQuery(supabase.from(path)), IDatabaseReference {

    val reference = supabase.from(path)

    override fun child(path: String): IDatabaseReference {
        val newPath = "${this.path.removeSuffix("/")}/${path.removePrefix("/")}"
        return SupabaseDatabaseReference(supabase, newPath)
    }

    override fun push(): IDatabaseReference {
        // Returns a reference to a new, unique key, but the key generation is
        // typically handled by the database (e.g., UUID).
        // This implementation is a placeholder.
        return this
    }

    override val key: String?
        get() = path.substringAfterLast('/')
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