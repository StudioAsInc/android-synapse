package com.synapse.social.studioasinc.backend

import com.google.gson.Gson
import com.google.gson.JsonElement
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseReference
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IQuery
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// --- Wrapper Implementations for Supabase ---

private class SupabaseQueryWrapper(var table: String, var queryBuilder: PostgrestQueryBuilder.() -> Unit) : IQuery {
    override fun orderByChild(path: String): IQuery {
        // Not directly supported in Supabase Postgrest, requires raw query
        return this
    }

    override fun equalTo(value: String?): IQuery {
        queryBuilder = { filter { eq("id", value ?: "") } }
        return this
    }

    override fun limitToLast(limit: Int): IQuery {
        queryBuilder = { limit(limit) }
        return this
    }

    override fun limitToFirst(limit: Int): IQuery {
        queryBuilder = { limit(limit) }
        return this
    }

    override fun startAt(value: String): IQuery {
        // Not directly supported
        return this
    }

    override fun endAt(value: String): IQuery {
        // Not directly supported
        return this
    }
}

private class SupabaseDbReferenceWrapper(val table: String, val id: String? = null) : IDatabaseReference {
    override fun child(path: String): IDatabaseReference {
        return SupabaseDbReferenceWrapper(table, path)
    }

    override fun push(): IDatabaseReference {
        // Not directly supported
        return this
    }

    override val key: String?
        get() = id

    override fun orderByChild(path: String): IQuery {
        return SupabaseQueryWrapper(table) {}.orderByChild(path)
    }

    override fun equalTo(value: String?): IQuery {
        return SupabaseQueryWrapper(table) {}.equalTo(value)
    }

    override fun limitToLast(limit: Int): IQuery {
        return SupabaseQueryWrapper(table) {}.limitToLast(limit)
    }

    override fun limitToFirst(limit: Int): IQuery {
        return SupabaseQueryWrapper(table) {}.limitToFirst(limit)
    }

    override fun startAt(value: String): IQuery {
        return SupabaseQueryWrapper(table) {}.startAt(value)
    }

    override fun endAt(value: String): IQuery {
        return SupabaseQueryWrapper(table) {}.endAt(value)
    }
}

private class SupabaseDataSnapshot(private val data: JsonElement) : IDataSnapshot {
    override fun <T> getValue(valueType: Class<T>): T? {
        return Gson().fromJson(data, valueType)
    }

    override fun exists(): Boolean {
        return !data.isJsonNull
    }

    override val children: Iterable<IDataSnapshot>
        get() = data.asJsonArray.map { SupabaseDataSnapshot(it) }

    override val key: String?
        get() = if (data.isJsonObject) data.asJsonObject.get("id")?.asString else null
}

private class SupabaseDbError(override val message: String, override val code: Int) : IDatabaseError

// --- Service Implementation ---

class SupabaseDatabaseService : IDatabaseService {

    private val supabase = SupabaseClient.client

    override fun getReference(path: String): IDatabaseReference {
        return SupabaseDbReferenceWrapper(path)
    }

    override fun getData(query: IQuery, listener: IDataListener) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val supabaseQuery = query as SupabaseQueryWrapper
                val result = supabase.postgrest.from(supabaseQuery.table).select(supabaseQuery.queryBuilder)
                val jsonElement = Gson().toJsonTree(result)
                listener.onDataChange(SupabaseDataSnapshot(jsonElement))
            } catch (e: Exception) {
                listener.onCancelled(SupabaseDbError(e.message ?: "Unknown error", 0))
            }
        }
    }

    override fun setValue(ref: IDatabaseReference, value: Any?, listener: ICompletionListener<Unit>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val supabaseRef = ref as SupabaseDbReferenceWrapper
                supabase.postgrest.from(supabaseRef.table).upsert(value!!)
                listener.onComplete(Unit, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun updateChildren(ref: IDatabaseReference, updates: Map<String, Any?>, listener: ICompletionListener<Unit>) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val supabaseRef = ref as SupabaseDbReferenceWrapper
                supabase.postgrest.from(supabaseRef.table).update(updates) {
                    filter {
                        eq("id", supabaseRef.id ?: "")
                    }
                }
                listener.onComplete(Unit, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }
}
