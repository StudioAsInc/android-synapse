package com.synapse.social.studioasinc.backend

import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import com.synapse.social.studioasinc.SupabaseClient
import kotlinx.serialization.json.JsonElement

/**
 * Service for handling Supabase database operations.
 * Provides methods for CRUD operations on Supabase tables.
 */
class SupabaseDatabaseService {

    private val client = SupabaseClient.client

    /**
     * Inserts data into a table.
     * @param table Table name
     * @param data Data to insert as a map
     * @return List of inserted records
     * @throws Exception if insert fails
     */
    suspend fun insert(table: String, data: Map<String, Any?>): List<Map<String, Any?>> {
        return client.postgrest[table].insert(data) {
            select()
        }.decodeList<Map<String, Any?>>()
    }

    /**
     * Updates data in a table with filter.
     * @param table Table name
     * @param data Data to update as a map
     * @param filter Filter function to apply WHERE conditions
     * @return List of updated records
     * @throws Exception if update fails
     */
    suspend fun update(
        table: String, 
        data: Map<String, Any?>,
        filter: (PostgrestFilterBuilder.() -> Unit)
    ): List<Map<String, Any?>> {
        return client.postgrest[table].update(data) {
            select()
            filter()
        }.decodeList<Map<String, Any?>>()
    }

    /**
     * Selects data from a table with optional filtering.
     * @param table Table name
     * @param columns Columns to select (default: all)
     * @param filter Optional filter function to apply WHERE conditions
     * @return List of selected records
     * @throws Exception if select fails
     */
    suspend fun <T> select(
        table: String,
        columns: String = "*",
        filter: (PostgrestFilterBuilder.() -> Unit)? = null
    ): List<Map<String, Any?>> {
        val query = client.postgrest[table].select(columns = Columns.raw(columns))
        
        filter?.let { 
            query.filter { filter() }
        }
        
        return query.decodeList<Map<String, Any?>>()
    }

    /**
     * Selects all data from a table.
     * @param table Table name
     * @param columns Columns to select (default: all)
     * @return List of all records
     * @throws Exception if select fails
     */
    suspend fun selectAll(table: String, columns: String = "*"): List<Map<String, Any?>> {
        return client.postgrest[table]
            .select(columns = Columns.raw(columns))
            .decodeList<Map<String, Any?>>()
    }

    /**
     * Selects a single record by ID.
     * @param table Table name
     * @param id Record ID
     * @param idColumn ID column name (default: "id")
     * @param columns Columns to select (default: all)
     * @return Single record or null if not found
     * @throws Exception if select fails
     */
    suspend fun selectById(
        table: String, 
        id: String, 
        idColumn: String = "id",
        columns: String = "*"
    ): Map<String, Any?>? {
        return client.postgrest[table]
            .select(columns = Columns.raw(columns)) {
                filter {
                    eq(idColumn, id)
                }
            }
            .decodeSingleOrNull<Map<String, Any?>>()
    }

    /**
     * Deletes records from a table.
     * @param table Table name
     * @param filter Filter function to apply WHERE conditions
     * @return List of deleted records
     * @throws Exception if delete fails
     */
    suspend fun delete(
        table: String,
        filter: PostgrestFilterBuilder.() -> Unit
    ): List<Map<String, Any?>> {
        return client.postgrest[table].delete {
            select()
            filter()
        }.decodeList<Map<String, Any?>>()
    }

    /**
     * Deletes a record by ID.
     * @param table Table name
     * @param id Record ID
     * @param idColumn ID column name (default: "id")
     * @return Deleted record or null if not found
     * @throws Exception if delete fails
     */
    suspend fun deleteById(
        table: String, 
        id: String, 
        idColumn: String = "id"
    ): Map<String, Any?>? {
        return client.postgrest[table].delete {
            select()
            filter {
                eq(idColumn, id)
            }
        }.decodeSingleOrNull<Map<String, Any?>>()
    }

    /**
     * Counts records in a table with optional filtering.
     * @param table Table name
     * @param filter Optional filter function to apply WHERE conditions
     * @return Number of records
     * @throws Exception if count fails
     */
    suspend fun count(
        table: String,
        filter: (PostgrestFilterBuilder.() -> Unit)? = null
    ): Long {
        val query = client.postgrest[table].select(columns = Columns.raw("*"), count = Count.EXACT)
        
        filter?.let { 
            query.filter { filter() }
        }
        
        return query.execute().countOrNull() ?: 0L
    }

    /**
     * Executes a raw SQL query using RPC (Remote Procedure Call).
     * @param functionName Name of the database function to call
     * @param parameters Parameters to pass to the function
     * @return Result of the function call
     * @throws Exception if RPC call fails
     */
    suspend fun rpc(
        functionName: String,
        parameters: Map<String, Any?> = emptyMap()
    ): JsonElement {
        return client.postgrest.rpc(functionName, parameters)
    }

    /**
     * Upserts (insert or update) data into a table.
     * @param table Table name
     * @param data Data to upsert as a map
     * @param onConflict Columns to check for conflicts (default: "id")
     * @return List of upserted records
     * @throws Exception if upsert fails
     */
    suspend fun upsert(
        table: String, 
        data: Map<String, Any?>,
        onConflict: String = "id"
    ): List<Map<String, Any?>> {
        return client.postgrest[table].upsert(data, onConflict = onConflict) {
            select()
        }.decodeList<Map<String, Any?>>()
    }

    /**
     * Selects data from a table with filtering.
     * @param table Table name
     * @param columns Columns to select (default: all)
     * @param filter Filter function to apply WHERE conditions
     * @return List of selected records
     * @throws Exception if select fails
     */
    suspend inline fun <reified T> selectWithFilter(
        table: String,
        columns: String = "*",
        noinline filter: PostgrestFilterBuilder.() -> Unit
    ): List<T> {
        return client.postgrest[table]
            .select(columns = Columns.raw(columns)) {
                filter()
            }
            .decodeList<T>()
    }

    /**
     * Updates data in a table without filter (updates all records).
     * @param table Table name
     * @param data Data to update as a map
     * @return List of updated records
     * @throws Exception if update fails
     */
    suspend fun update(
        table: String, 
        data: Map<String, Any?>
    ): List<Map<String, Any?>> {
        return client.postgrest[table].update(data) {
            select()
        }.decodeList<Map<String, Any?>>()
    }
}