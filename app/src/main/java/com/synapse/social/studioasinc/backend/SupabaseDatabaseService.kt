package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.GlobalScope
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.onEach

class SupabaseDatabaseService : IDatabaseService {

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
        GlobalScope.launch {
            try {
                val response = supabaseQuery.query.select().decodeList<Map<String, Any?>>()
                listener.onDataChange(SupabaseDataSnapshot(response))
            } catch (e: Exception) {
                listener.onCancelled(SupabaseDatabaseError(e.message ?: "Unknown error", 0))
            }
        }
    }

    override fun setValue(ref: IDatabaseReference, value: Any?, listener: ICompletionListener<Unit>) {
        val supabaseRef = ref as SupabaseDatabaseReference
        GlobalScope.launch {
            try {
                supabaseRef.reference.upsert(value!!)
                listener.onComplete(Unit, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun updateChildren(ref: IDatabaseReference, updates: Map<String, Any?>, listener: ICompletionListener<Unit>) {
        val supabaseRef = ref as SupabaseDatabaseReference
        GlobalScope.launch {
            try {
                supabaseRef.reference.update(updates)
                listener.onComplete(Unit, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun addRealtimeListener(ref: IDatabaseReference, listener: IRealtimeListener): IRealtimeChannel {
        val supabaseRef = ref as SupabaseDatabaseReference
        val channel = supabase.channel(supabaseRef.path)
        GlobalScope.launch {
            channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = supabaseRef.path
            }.onEach { 
                listener.onInsert(SupabaseDataSnapshot(it.newRecord))
            }.launchIn(this)

            channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = supabaseRef.path
            }.onEach { 
                listener.onUpdate(SupabaseDataSnapshot(it.newRecord))
            }.launchIn(this)

            channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                table = supabaseRef.path
            }.onEach { 
                listener.onDelete(SupabaseDataSnapshot(it.oldRecord))
            }.launchIn(this)

            channel.subscribe()
        }
        return SupabaseRealtimeChannel(channel)
    }

    override fun removeRealtimeListener(channel: IRealtimeChannel) {
        val supabaseChannel = channel as SupabaseRealtimeChannel
        GlobalScope.launch {
            supabaseChannel.channel.unsubscribe()
        }
    }
}

abstract class SupabaseQuery(val query: io.github.jan.supabase.postgrest.query.PostgrestQuery) : IQuery {

    override fun orderByChild(path: String): IQuery {
        return this
    }

    override fun equalTo(value: String?): IQuery {
        return this
    }

    override fun limitToLast(limit: Int): IQuery {
        return this
    }

    override fun limitToFirst(limit: Int): IQuery {
        return this
    }

    override fun startAt(value: String): IQuery {
        return this
    }

    override fun endAt(value: String): IQuery {
        return this
    }
}

class SupabaseDatabaseReference(private val supabase: SupabaseClient, private val path: String) : SupabaseQuery(supabase.from(path)), IDatabaseReference {

    val reference = supabase.from(path)

    override fun child(path: String): IDatabaseReference {
        return SupabaseDatabaseReference(supabase, "${this.path}/$path")
    }

    override fun push(): IDatabaseReference {
        return this
    }

    override val key: String?
        get() = path.substringAfterLast('/')
}

class SupabaseDataSnapshot(private val data: Any?) : IDataSnapshot {

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
        get() = null
}

class SupabaseDatabaseError(override val message: String, override val code: Int) : IDatabaseError
