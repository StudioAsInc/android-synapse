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
 * Handles database operations using Supabase Postgrest.
 * Provides CRUD operations with comprehensive error handling and logging.
 */
class SupabaseDatabaseService : IDatabaseService {
    
    companion object {
        private const val TAG = "SupabaseDB"
    }
    
    private val client = SupabaseClient.client
    
    /**
     * Insert data into a table.
     * @param table The name of the table to insert into
     * @param data The data object to insert (must be serializable or a Map)
     * @return Result indicating success or failure with detailed error message
     */
    suspend fun insert(table: String, data: Any): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "Inserting data into table '$table'")
                
                // Convert Map to JsonObject to avoid serialization issues
                val insertData = when (data) {
                    is Map<*, *> -> {
                        kotlinx.serialization.json.buildJsonObject {
                            data.forEach { (key, value) ->
                                when (value) {
                                    is String -> put(key.toString(), kotlinx.serialization.json.JsonPrimitive(value))
                                    is Number -> put(key.toString(), kotlinx.serialization.json.JsonPrimitive(value))
                                    is Boolean -> put(key.toString(), kotlinx.serialization.json.JsonPrimitive(value))
                                    null -> put(key.toString(), kotlinx.serialization.json.JsonNull)
                                    else -> put(key.toString(), kotlinx.serialization.json.JsonPrimitive(value.toString()))
                                }
                            }
                        }
                    }
                    else -> data
                }
                
                client.from(table).insert(insertData)
                android.util.Log.d(TAG, "Data inserted successfully into table '$table'")
                
                Result.success(Unit)
                
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Database insertion failed for table '$table'", e)
                
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
     * Update data in a table with map-based data.
     * @param table The name of the table to update
     * @param data Map of column names to values
     * @param filter The column name to filter by
     * @param value The value to match in the filter column
     * @return Result indicating success or failure
     */
    override suspend fun update(table: String, data: Map<String, Any?>, filter: String, value: Any): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "Updating data in table '$table' where $filter=$value")
                
                // Convert Map to JsonObject to avoid serialization issues
                val updateData = kotlinx.serialization.json.buildJsonObject {
                    data.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, kotlinx.serialization.json.JsonPrimitive(value))
                            is Number -> put(key, kotlinx.serialization.json.JsonPrimitive(value))
                            is Boolean -> put(key, kotlinx.serialization.json.JsonPrimitive(value))
                            null -> put(key, kotlinx.serialization.json.JsonNull)
                            else -> put(key, kotlinx.serialization.json.JsonPrimitive(value.toString()))
                        }
                    }
                }
                
                client.from(table).update(updateData) {
                    filter { 
                        eq(filter, value)
                    }
                }
                android.util.Log.d(TAG, "Data updated successfully in table '$table'")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Database update failed for table '$table'", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update data in a table with serializable object.
     * @param table The name of the table to update
     * @param data The data object to update (must be serializable)
     * @param filter The column name to filter by
     * @param value The value to match in the filter column
     * @return Result indicating success or failure with detailed error message
     */
    suspend fun updateWithObject(table: String, data: Any, filter: String, value: Any): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "Updating data in table '$table' where $filter=$value")
                
                client.from(table).update(data) {
                    filter { 
                        eq(filter, value)
                    }
                }
                android.util.Log.d(TAG, "Data updated successfully in table '$table'")
                Result.success(Unit)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Database update failed for table '$table'", e)
                
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
    
    /**
     * Search posts by content using text search
     * @param query The search query string
     * @param limit Maximum number of results to return
     * @return Result with list of matching posts
     */
    suspend fun searchPosts(query: String, limit: Int = 20): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "Searching posts with query: $query")
                
                val result = client.from("posts").select(columns = Columns.raw("*")) {
                    filter {
                        ilike("content", "%$query%")
                    }
                    limit(limit.toLong())
                    order(column = "timestamp", order = io.github.jan.supabase.postgrest.query.Order.DESCENDING)
                }.decodeList<JsonObject>()
                
                val mappedResult = result.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }
                
                android.util.Log.d(TAG, "Found ${mappedResult.size} posts matching query")
                Result.success(mappedResult)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Post search failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Search users by username or nickname
     * @param query The search query string
     * @param limit Maximum number of results to return
     * @return Result with list of matching users
     */
    suspend fun searchUsers(query: String, limit: Int = 20): Result<List<Map<String, Any?>>> {
        return withContext(Dispatchers.IO) {
            try {
                android.util.Log.d(TAG, "Searching users with query: $query")
                
                val result = client.from("users").select(columns = Columns.raw("*")) {
                    filter {
                        or {
                            ilike("username", "%$query%")
                            ilike("nickname", "%$query%")
                        }
                    }
                    limit(limit.toLong())
                }.decodeList<JsonObject>()
                
                val mappedResult = result.map { jsonObject ->
                    jsonObject.toMap().mapValues { (_, value) ->
                        value.toString().removeSurrounding("\"")
                    }
                }
                
                android.util.Log.d(TAG, "Found ${mappedResult.size} users matching query")
                Result.success(mappedResult)
            } catch (e: Exception) {
                android.util.Log.e(TAG, "User search failed", e)
                Result.failure(e)
            }
        }
    }
}