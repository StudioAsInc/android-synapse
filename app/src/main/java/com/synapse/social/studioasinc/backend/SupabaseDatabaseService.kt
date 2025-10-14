package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.BuildConfig
import com.synapse.social.studioasinc.backend.interfaces.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.GoTrue
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*

private data class Filter(val column: String, val value: String)

private class SupabaseQueryWrapper(
    val tableName: String,
    private val filters: List<Filter> = emptyList(),
    private val orderBy: String? = null,
    private val limit: Int? = null
) : IQuery {

    override fun orderByChild(path: String): IQuery {
        return SupabaseQueryWrapper(tableName, filters, path, limit)
    }

    override fun equalTo(value: String?): IQuery {
        if (value == null || orderBy == null) return this
        val newFilters = filters + Filter(orderBy, value)
        return SupabaseQueryWrapper(tableName, newFilters, orderBy, limit)
    }

    override fun limitToLast(limit: Int): IQuery {
        return SupabaseQueryWrapper(tableName, filters, orderBy, limit)
    }

    override fun limitToFirst(limit: Int): IQuery = this
    override fun startAt(value: String): IQuery = this
    override fun endAt(value: String): IQuery = this

    fun build(query: Postgrest) {
        filters.forEach { filter ->
            query.filter(filter.column, io.github.jan.supabase.postgrest.query.FilterOperator.EQ, filter.value)
        }
        orderBy?.let {
            query.order(it)
        }
        limit?.let {
            query.limit(it)
        }
    }
}

private class SupabaseDbReferenceWrapper(
    val tableName: String,
    private val filters: List<Filter> = emptyList()
) : IDatabaseReference {
    private val queryWrapper = SupabaseQueryWrapper(tableName, filters)
    override fun orderByChild(path: String): IQuery = queryWrapper.orderByChild(path)
    override fun equalTo(value: String?): IQuery = queryWrapper.equalTo(value)
    override fun limitToLast(limit: Int): IQuery = queryWrapper.limitToLast(limit)
    override fun limitToFirst(limit: Int): IQuery = queryWrapper.limitToFirst(limit)
    override fun startAt(value: String): IQuery = queryWrapper.startAt(value)
    override fun endAt(value: String): IQuery = queryWrapper.endAt(value)

    override fun child(path: String): IDatabaseReference = SupabaseDbReferenceWrapper("$tableName/$path", filters)
    override fun push(): IDatabaseReference = this
    override val key: String? = null
}

private class SupabaseDataSnapshot(private val jsonElement: JsonElement) : IDataSnapshot {
    override fun <T> getValue(valueType: Class<T>): T? {
        return try {
            Json.decodeFromJsonElement(serializer(valueType), jsonElement)
        } catch (e: Exception) {
            null
        }
    }
    override fun exists(): Boolean = jsonElement !is JsonNull
    override val children: Iterable<IDataSnapshot>
        get() = if (jsonElement is JsonArray) {
            jsonElement.map { SupabaseDataSnapshot(it) }
        } else {
            emptyList()
        }
    override val key: String? = (jsonElement.jsonObject["key"] as? JsonPrimitive)?.content
}

private class SupabaseDbError(override val message: String, override val code: Int) : IDatabaseError

class SupabaseDatabaseService : IDatabaseService {

    private val supabase: SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(GoTrue)
        install(Postgrest)
    }

    override fun getReference(path: String): IDatabaseReference {
        return SupabaseDbReferenceWrapper(path)
    }

    override fun getData(query: IQuery, listener: IDataListener) {
        val supQuery = query as SupabaseQueryWrapper
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = supabase.postgrest[supQuery.tableName].select {
                    supQuery.build(this)
                }
                listener.onDataChange(SupabaseDataSnapshot(result.body!!))
            } catch (e: Exception) {
                listener.onCancelled(SupabaseDbError(e.message ?: "Unknown error", 0))
            }
        }
    }

    override fun setValue(ref: IDatabaseReference, value: Any?, listener: ICompletionListener<Unit>) {
        val supRef = ref as SupabaseDbReferenceWrapper
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.postgrest[supRef.tableName].insert(value as Map<String, Any?>)
                listener.onComplete(Unit, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun updateChildren(ref: IDatabaseReference, updates: Map<String, Any?>, listener: ICompletionListener<Unit>) {
        val supQuery = (ref as SupabaseDbReferenceWrapper).let { SupabaseQueryWrapper(it.tableName) }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.postgrest[supQuery.tableName].update(updates) {
                    supQuery.build(this)
                }
                listener.onComplete(Unit, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun delete(ref: IDatabaseReference, listener: ICompletionListener<Unit>) {
        val supQuery = (ref as SupabaseDbReferenceWrapper).let { SupabaseQueryWrapper(it.tableName) }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                supabase.postgrest[supQuery.tableName].delete {
                    supQuery.build(this)
                }
                listener.onComplete(Unit, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }
}
