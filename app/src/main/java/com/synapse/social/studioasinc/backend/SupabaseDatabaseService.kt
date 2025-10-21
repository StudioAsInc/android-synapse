package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns

/**
 * Supabase Database Service
 */
class SupabaseDatabaseService {
    
    private val client = SupabaseClient.client
    
    suspend inline fun <reified T> select(table: String, columns: String = "*"): List<T> {
        return client.from(table).select(columns = Columns.raw(columns)).decodeList<T>()
    }
    
    suspend inline fun <reified T> selectSingle(table: String, columns: String = "*"): T? {
        return try {
            client.from(table).select(columns = Columns.raw(columns)).decodeSingle<T>()
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun insert(table: String, data: Map<String, Any?>): Map<String, Any?> {
        client.from(table).insert(data)
        return data
    }
    
    suspend fun update(table: String, data: Map<String, Any?>): Map<String, Any?> {
        client.from(table).update(data)
        return data
    }
    
    suspend fun delete(table: String): Boolean {
        return try {
            client.from(table).delete()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    suspend fun upsert(table: String, data: Map<String, Any?>): Map<String, Any?> {
        client.from(table).upsert(data)
        return data
    }
    
    // Helper method for filtering queries
    suspend inline fun <reified T> selectWithFilter(
        table: String, 
        columns: String = "*",
        noinline filterBuilder: suspend (io.github.jan.supabase.postgrest.query.PostgrestQueryBuilder) -> Unit = {}
    ): List<T> {
        return client.from(table).select(columns = Columns.raw(columns)) {
            filterBuilder(this)
        }.decodeList<T>()
    }
}