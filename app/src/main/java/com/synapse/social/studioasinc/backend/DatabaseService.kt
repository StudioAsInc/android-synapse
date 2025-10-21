package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Supabase-based database service that implements the existing interface
 * for backward compatibility during migration.
 */
class DatabaseService : IDatabaseService {

    private val supabaseDbService = SupabaseDatabaseService()

    override fun getReference(path: String): IDatabaseReference {
        return SupabaseDbReferenceWrapper(path)
    }

    override fun getData(query: IQuery, listener: IDataListener) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // This is a simplified implementation
                // In practice, you'd need to parse the query and convert it to Supabase format
                val data = supabaseDbService.select<Map<String, Any?>>("users") // placeholder
                val snapshot = SupabaseDataSnapshot(data.firstOrNull() ?: emptyMap())
                listener.onDataChange(snapshot)
            } catch (e: Exception) {
                val error = SupabaseDbError(e.message ?: "Unknown error", -1)
                listener.onCancelled(error)
            }
        }
    }

    override fun setValue(ref: IDatabaseReference, value: Any?, listener: ICompletionListener<Unit>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // Convert Firebase-style setValue to Supabase upsert
                val refWrapper = ref as SupabaseDbReferenceWrapper
                val data = mapOf("data" to value) // This would need proper mapping
                supabaseDbService.upsert(refWrapper.tableName, data)
                listener.onComplete(Unit, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }

    override fun updateChildren(ref: IDatabaseReference, updates: Map<String, Any?>, listener: ICompletionListener<Unit>) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val refWrapper = ref as SupabaseDbReferenceWrapper
                supabaseDbService.update(refWrapper.tableName, updates)
                listener.onComplete(Unit, null)
            } catch (e: Exception) {
                listener.onComplete(null, e)
            }
        }
    }
}

// Wrapper classes for Supabase to maintain interface compatibility

private class SupabaseDbReferenceWrapper(val tableName: String) : IDatabaseReference {
    override fun orderByChild(path: String): IQuery = SupabaseQueryWrapper(tableName, path)
    override fun equalTo(value: String?): IQuery = SupabaseQueryWrapper(tableName)
    override fun limitToLast(limit: Int): IQuery = SupabaseQueryWrapper(tableName)
    override fun limitToFirst(limit: Int): IQuery = SupabaseQueryWrapper(tableName)
    override fun startAt(value: String): IQuery = SupabaseQueryWrapper(tableName)
    override fun endAt(value: String): IQuery = SupabaseQueryWrapper(tableName)
    override fun child(path: String): IDatabaseReference = SupabaseDbReferenceWrapper("$tableName/$path")
    override fun push(): IDatabaseReference = SupabaseDbReferenceWrapper(tableName)
    override val key: String? get() = tableName
}

private class SupabaseQueryWrapper(val tableName: String, val orderBy: String? = null) : IQuery {
    override fun orderByChild(path: String): IQuery = SupabaseQueryWrapper(tableName, path)
    override fun equalTo(value: String?): IQuery = SupabaseQueryWrapper(tableName, orderBy)
    override fun limitToLast(limit: Int): IQuery = SupabaseQueryWrapper(tableName, orderBy)
    override fun limitToFirst(limit: Int): IQuery = SupabaseQueryWrapper(tableName, orderBy)
    override fun startAt(value: String): IQuery = SupabaseQueryWrapper(tableName, orderBy)
    override fun endAt(value: String): IQuery = SupabaseQueryWrapper(tableName, orderBy)
}

private class SupabaseDataSnapshot(private val data: Map<String, Any?>) : IDataSnapshot {
    override fun <T> getValue(valueType: Class<T>): T? {
        return when (valueType) {
            String::class.java -> data.values.firstOrNull() as? T
            else -> null
        }
    }
    override fun exists(): Boolean = data.isNotEmpty()
    override val children: Iterable<IDataSnapshot> get() = emptyList()
    override val key: String? get() = data.keys.firstOrNull()
}

private class SupabaseDbError(override val message: String, override val code: Int) : IDatabaseError