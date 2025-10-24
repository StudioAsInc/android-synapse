package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

/**
 * Supabase Database Service
 * Handles database operations using Supabase Postgrest
 */
class SupabaseDatabaseService : IDatabaseService {
    
    private val client = SupabaseClient.client
    
    /**
     * Insert data into a table
     */
    suspend fun insert(table: String, data: Any): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("SupabaseDB", "Inserting data into table '$table': $data")
                
                // Perform the insertion directly with the serializable object
                try {
                    client.from(table).insert(data)
                    android.util.Log.d("SupabaseDB", "Data inserted successfully into table '$table'")
                } catch (insertError: Exception) {
                    android.util.Log.e("SupabaseDB", "Insert operation failed", insertError)
                    throw insertError
                }
                
                Result.success(Unit)
                
            } catch (e: Exception) {
                android.util.Log.e("SupabaseDB", "Database insertion failed", e)
                
                // Provide more specific error messages
                val errorMessage = when {
                    e.message?.contains("serialization", ignoreCase = true) == true -> 
                        "Data serialization error: ${e.message}"
                    e.message?.contains("duplicate", ignoreCase = true) == true -> 
                        "Duplicate entry error: ${e.message}"
                    e.message?.contains("constraint", ignoreCase = true) == true -> 
                        "Database constraint violation: ${e.message}"
                    e.message?.contains("column", ignoreCase = true) == true -> 
                        "Database column error: ${e.message}"
                    e.message?.contains("table", ignoreCase = true) == true -> 
                        "Database table error: ${e.message}"
                    else -> e.message ?: "Database insertion failed"
                }
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    /**
     * Update data in a table
     */
    override suspend fun update(table: String, data: Map<String, Any?>, filter: String, value: Any): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from(table).update(data) {
                    filter { 
                        eq(filter, value)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update data in a table with serializable object
     */
    suspend fun updateWithObject(table: String, data: Any, filter: String, value: Any): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d("SupabaseDB", "Updating data in table '$table' with filter '$filter'='$value': $data")
                
                client.from(table).update(data) {
                    filter { 
                        eq(filter, value)
                    }
                }
                android.util.Log.d("SupabaseDB", "Data updated successfully in table '$table'")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e("SupabaseDB", "Database update failed", e)
                
                // Provide more specific error messages
                val errorMessage = when {
                    e.message?.contains("serialization", ignoreCase = true) == true -> 
                        "Data serialization error: ${e.message}"
                    e.message?.contains("constraint", ignoreCase = true) == true -> 
                        "Database constraint violation: ${e.message}"
                    e.message?.contains("column", ignoreCase = true) == true -> 
                        "Database column error: ${e.message}"
                    e.message?.contains("table", ignoreCase = true) == true -> 
                        "Database table error: ${e.message}"
                    else -> e.message ?: "Database update failed"
                }
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    /**
     * Select data from a table
     */
    override suspend fun select(table: String, columns: String): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select(columns = Columns.raw(columns))
                    .decodeList<JsonObject>()
                
                val mappedResult = result.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }
                Result.success(mappedResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Select data from a table with filter
     */
    override suspend fun selectWhere(table: String, columns: String, filter: String, value: Any): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select(columns = Columns.raw(columns)) {
                    filter { 
                        eq(filter, value)
                    }
                }.decodeList<JsonObject>()
                
                val mappedResult = result.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, jsonValue) ->
                        jsonValue.toString().removeSurrounding("\"")
                    }
                }
                Result.success(mappedResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Delete data from a table
     */
    override suspend fun delete(table: String, filter: String, value: Any): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from(table).delete {
                    filter { 
                        eq(filter, value)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Delete all data from a table with filter
     */
    override suspend fun deleteWhere(table: String, filter: String, value: Any): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from(table).delete {
                    filter { 
                        eq(filter, value)
                    }
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Count records in a table
     */
    override suspend fun count(table: String): Result<Long> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select(columns = Columns.raw("*"))
                    .decodeList<JsonObject>()
                Result.success(result.size.toLong())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Check if record exists
     */
    override suspend fun exists(table: String, filter: String, value: Any): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select(columns = Columns.raw("*")) {
                    filter { 
                        eq(filter, value)
                    }
                }.decodeList<JsonObject>()
                Result.success(result.isNotEmpty())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Upsert data (insert or update)
     */
    override suspend fun upsert(table: String, data: Map<String, Any?>): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from(table).upsert(data)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Get single record
     */
    suspend fun getSingle(table: String, filter: String, value: Any): Result<Map<String, Any?>?> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select {
                    filter { 
                        eq(filter, value)
                    }
                }.decodeSingleOrNull<JsonObject>()
                
                val mappedResult = result?.toMap()?.mapValues { (_, jsonValue) ->
                    jsonValue.toString().removeSurrounding("\"")
                }
                Result.success(mappedResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update user presence
     */
    suspend fun updatePresence(userId: String, isOnline: Boolean): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.from("user_presence").upsert(mapOf(
                    "user_id" to userId,
                    "is_online" to isOnline,
                    "last_seen" to System.currentTimeMillis()
                ))
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Select with filter (alias for selectWhere)
     */
    suspend fun selectWithFilter(table: String, columns: String = "*", filter: String, value: Any): Result<List<Map<String, Any?>>> {
        return selectWhere(table, columns, filter, value)
    }
    
    /**
     * Select by ID (convenience method)
     */
    suspend fun selectById(table: String, id: String, columns: String = "*"): Result<Map<String, Any?>?> {
        return withContext(Dispatchers.IO) {
            try {
                val result = client.from(table).select(columns = Columns.raw(columns)) {
                    filter { eq("id", id) }
                }.decodeSingleOrNull<JsonObject>()
                
                val mappedResult = result?.toMap()?.mapValues { (_, jsonValue) ->
                    jsonValue.toString().removeSurrounding("\"")
                }
                Result.success(mappedResult)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}