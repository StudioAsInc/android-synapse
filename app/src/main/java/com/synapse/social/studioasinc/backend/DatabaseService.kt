package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.backend.interfaces.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.PostgrestBuilder
import io.github.jan.supabase.postgrest.query.PostgrestResult
import io.github.jan.supabase.postgrest.query.PostgrestFilterBuilder
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.subscribe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import android.util.Log
import io.github.jan.supabase.exceptions.RestException

// TODO(supabase): Implement Supabase Database Service
// This service should implement the IDatabaseService interface and provide a concrete
// implementation using the Supabase Kotlin client library.
// See: https://supabase.com/docs/reference/kotlin/select
//
// Key tasks:
// 1.  Initialize the Supabase client.
// 2.  Implement getReference to return a Supabase-specific IDatabaseReference. This will likely involve creating a wrapper around the Supabase query builder.
// 3.  Implement getData to fetch data from Supabase tables using PostgREST.
// 4.  Implement setValue and updateChildren to write data to Supabase tables using PostgREST.
// 5.  Create Supabase-specific wrappers for IQuery, IDatabaseReference, IDataSnapshot, and IDatabaseError to map the Supabase API to the existing interfaces.

class DatabaseService(private val supabase: SupabaseClient) : IDatabaseService {

    // --- Supabase-specific Implementations of Interfaces ---

    class SupabaseQuery(var postgrestFilterBuilder: PostgrestFilterBuilder, private var currentColumn: String? = null) : IQuery {
        override fun orderByChild(path: String): IQuery {
            currentColumn = path
            postgrestFilterBuilder = postgrestFilterBuilder.order(path)
            return this
        }

        override fun equalTo(value: String?): IQuery {
            value?.let {
                currentColumn?.let { col ->
                    postgrestFilterBuilder = postgrestFilterBuilder.eq(col, it)
                } ?: Log.e("SupabaseQuery", "equalTo called without a specified column. Use orderByChild or ensure column is set.")
            }
            return this
        }

        override fun limitToLast(limit: Int): IQuery {
            postgrestFilterBuilder = postgrestFilterBuilder.limit(limit)
            return this
        }

        override fun limitToFirst(limit: Int): IQuery {
            postgrestFilterBuilder = postgrestFilterBuilder.limit(limit)
            return this
        }

        override fun startAt(value: String): IQuery {
            currentColumn?.let { col ->
                postgrestFilterBuilder = postgrestFilterBuilder.gte(col, value)
            } ?: Log.e("SupabaseQuery", "startAt called without a specified column. Use orderByChild or ensure column is set.")
            return this
        }

        override fun endAt(value: String): IQuery {
            currentColumn?.let { col ->
                postgrestFilterBuilder = postgrestFilterBuilder.lte(col, value)
            } ?: Log.e("SupabaseQuery", "endAt called without a specified column. Use orderByChild or ensure column is set.")
            return this
        }
    }

    class SupabaseDatabaseReference(private val postgrest: Postgrest, private val path: String) : IDatabaseReference {
        private var currentTable: String = path.split("/").first()
        private var currentFilterBuilder: PostgrestFilterBuilder = postgrest[currentTable].select()
        private var lastChildKey: String? = null

        init {
            val pathSegments = path.split("/")
            if (pathSegments.size > 1) {
                lastChildKey = pathSegments.last()
            }
        }

        override fun child(path: String): IDatabaseReference {
            return SupabaseDatabaseReference(postgrest, "${this.path}/$path")
        }

        override fun push(): IDatabaseReference {
            Log.w("SupabaseDatabaseReference", "push() is a simplified implementation. Supabase generates IDs on insert.")
            return SupabaseDatabaseReference(postgrest, "${path}/_new_key_") // Placeholder
        }

        override val key: String?
            get() = path.split("/").lastOrNull()

        // IQuery implementations (delegated or re-implemented if needed)
        override fun orderByChild(path: String): IQuery {
            currentFilterBuilder = currentFilterBuilder.order(path)
            return SupabaseQuery(currentFilterBuilder, path)
        }

        override fun equalTo(value: String?): IQuery {
            value?.let {
                // This assumes the 'key' in the path is the column to filter by.
                // This needs to be dynamic based on how Firebase queries are structured.
                val columnToFilter = lastChildKey ?: "id" // Default to "id" if no child key
                currentFilterBuilder = currentFilterBuilder.eq(columnToFilter, it)
            }
            return SupabaseQuery(currentFilterBuilder, lastChildKey)
        }

        override fun limitToLast(limit: Int): IQuery {
            currentFilterBuilder = currentFilterBuilder.limit(limit)
            return SupabaseQuery(currentFilterBuilder, lastChildKey)
        }

        override fun limitToFirst(limit: Int): IQuery {
            currentFilterBuilder = currentFilterBuilder.limit(limit)
            return SupabaseQuery(currentFilterBuilder, lastChildKey)
        }

        override fun startAt(value: String): IQuery {
            val columnToFilter = lastChildKey ?: "id"
            currentFilterBuilder = currentFilterBuilder.gte(columnToFilter, value)
            return SupabaseQuery(currentFilterBuilder, lastChildKey)
        }

        override fun endAt(value: String): IQuery {
            val columnToFilter = lastChildKey ?: "id"
            currentFilterBuilder = currentFilterBuilder.lte(columnToFilter, value)
            return SupabaseQuery(currentFilterBuilder, lastChildKey)
        }
    }

    class SupabaseDataSnapshot(private val data: JsonElement?, private val snapshotKey: String? = null) : IDataSnapshot {
        private val gson = Gson()

        override fun <T> getValue(valueType: Class<T>): T? {
            if (data == null) return null
            return try {
                gson.fromJson(data, valueType)
            } catch (e: Exception) {
                Log.e("SupabaseDataSnapshot", "Error converting Supabase data to ${valueType.simpleName}", e)
                null
            }
        }

        override fun exists(): Boolean = data != null

        override val children: Iterable<IDataSnapshot>
            get() {
                if (data == null || !data.isJsonObject()) return emptyList()
                return data.jsonObject.entries.map { entry ->
                    SupabaseDataSnapshot(entry.value, entry.key)
                }
            }

        override val key: String?
            get() = snapshotKey ?: data?.jsonObject?.keys?.firstOrNull()
    }

    class SupabaseDatabaseError(override val message: String, override val code: Int) : IDatabaseError

    // --- IDatabaseService Implementations ---

    override fun getReference(path: String): IDatabaseReference {
        return SupabaseDatabaseReference(supabase.postgrest, path)
    }

    override fun getData(query: IQuery, listener: IDataListener) {
        if (query is SupabaseQuery) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val result: PostgrestResult = query.postgrestFilterBuilder.select().execute()
                    if (result.body != null) {
                        val dataSnapshot = SupabaseDataSnapshot(result.data.let { kotlinx.serialization.json.Json.parseToJsonElement(it) })
                        listener.onDataChange(dataSnapshot)
                    } else {
                        listener.onCancelled(SupabaseDatabaseError("No data found", 404))
                    }
                } catch (e: RestException) {
                    listener.onCancelled(SupabaseDatabaseError(e.message ?: "Supabase REST error", e.statusCode))
                } catch (e: Exception) {
                    listener.onCancelled(SupabaseDatabaseError(e.message ?: "Unknown error during data retrieval", -1))
                }
            }
        } else {
            listener.onCancelled(SupabaseDatabaseError("Invalid query type", -1))
        }
    }

    override fun setValue(ref: IDatabaseReference, value: Any?, listener: ICompletionListener<Unit>) {
        if (ref is SupabaseDatabaseReference) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val pathSegments = ref.path.split("/")
                    val tableName = pathSegments.first()
                    val primaryKey = if (pathSegments.size > 1 && pathSegments.last() != "_new_key_") pathSegments.last() else null

                    if (value != null) {
                        val dataToInsert = if (value is Map<*, *>) value as Map<String, Any> else mapOf("value" to value)
                        val finalData = if (primaryKey != null) dataToInsert + ("id" to primaryKey) else dataToInsert

                        supabase.postgrest[tableName].upsert(finalData, onConflict = "id").execute()
                        listener.onSuccess(Unit)
                    } else {
                        // Handle deletion if value is null
                        if (primaryKey != null) {
                            supabase.postgrest[tableName].delete { eq("id", primaryKey) }.execute()
                            listener.onSuccess(Unit)
                        } else {
                            listener.onFailure(Exception("Cannot delete a whole table via setValue with null value."))
                        }
                    }
                } catch (e: RestException) {
                    listener.onFailure(SupabaseDatabaseError(e.message ?: "Supabase REST error", e.statusCode))
                } catch (e: Exception) {
                    listener.onFailure(e)
                }
            }
        } else {
            listener.onFailure(Exception("Invalid database reference type"))
        }
    }

    override fun updateChildren(ref: IDatabaseReference, updates: Map<String, Any?>, listener: ICompletionListener<Unit>) {
        if (ref is SupabaseDatabaseReference) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val pathSegments = ref.path.split("/")
                    val tableName = pathSegments.first()
                    val primaryKey = if (pathSegments.size > 1) pathSegments.last() else null

                    if (primaryKey != null) {
                        supabase.postgrest[tableName].update(updates) { eq("id", primaryKey) }.execute()
                        listener.onSuccess(Unit)
                    } else {
                        listener.onFailure(Exception("Cannot update children without a primary key in the reference path."))
                    }
                } catch (e: RestException) {
                    listener.onFailure(SupabaseDatabaseError(e.message ?: "Supabase REST error", e.statusCode))
                } catch (e: Exception) {
                    listener.onFailure(e)
                }
            }
        } else {
            listener.onFailure(Exception("Invalid database reference type"))
        }
    }

    override fun listenToRealtimeUpdates(tableName: String, listener: IRealtimeListener) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val channel = supabase.channel("db_changes")
                channel.postgresChangeFlow<PostgresAction>(schema = "public", table = tableName).collect {
                    when (it) {
                        is PostgresAction.Insert -> listener.onInsert(it.newRecord.toString())
                        is PostgresAction.Update -> listener.onUpdate(it.newRecord.toString())
                        is PostgresAction.Delete -> listener.onDelete(it.oldRecord.toString())
                        else -> {}
                    }
                }
                channel.subscribe()
            } catch (e: RestException) {
                Log.e("DatabaseService", "Error setting up realtime listener: ${e.message}", e)
                // Optionally, you could add an onError callback to IRealtimeListener
            } catch (e: Exception) {
                Log.e("DatabaseService", "Error setting up realtime listener", e)
                // Optionally, you could add an onError callback to IRealtimeListener
            }
        }
    }
}
