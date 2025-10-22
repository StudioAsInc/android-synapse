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
                
                // Convert data to Map if it's a serializable object
                val insertData = when (data) {
                    is Map<*, *> -> {
                        // Clean and validate the map data
                        val cleanedMap = mutableMapOf<String, Any?>()
                        data.forEach { (key, value) ->
                            val keyStr = key.toString()
                            when (value) {
                                null -> cleanedMap[keyStr] = null
                                is String -> cleanedMap[keyStr] = value
                                is Number -> cleanedMap[keyStr] = value
                                is Boolean -> cleanedMap[keyStr] = value
                                else -> {
                                    android.util.Log.w("SupabaseDB", "Converting non-primitive value to string: $keyStr = $value")
                                    cleanedMap[keyStr] = value.toString()
                                }
                            }
                        }
                        cleanedMap
                    }
                    else -> {
                        // Use reflection to convert data class to map
                        val dataMap = mutableMapOf<String, Any?>()
                        try {
                            data::class.java.declaredFields.forEach { field ->
                                field.isAccessible = true
                                val value = field.get(data)
                                val fieldName = field.name
                                when (value) {
                                    null -> dataMap[fieldName] = null
                                    is String -> dataMap[fieldName] = value
                                    is Number -> dataMap[fieldName] = value
                                    is Boolean -> dataMap[fieldName] = value
                                    else -> {
                                        android.util.Log.w("SupabaseDB", "Converting field $fieldName to string: $value")
                                        dataMap[fieldName] = value.toString()
                                    }
                                }
                            }
                        } catch (reflectionError: Exception) {
                            android.util.Log.e("SupabaseDB", "Reflection failed", reflectionError)
                            throw Exception("Failed to serialize data object: ${reflectionError.message}")
                        }
                        dataMap
                    }
                }
                
                android.util.Log.d("SupabaseDB", "Cleaned insert data: $insertData")
                
                // Validate that we have a valid client
                if (!SupabaseClient.isConfigured()) {
                    throw Exception("Supabase client not configured")
                }
                
                // Perform the insertion
                try {
                    client.from(table).insert(insertData)
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