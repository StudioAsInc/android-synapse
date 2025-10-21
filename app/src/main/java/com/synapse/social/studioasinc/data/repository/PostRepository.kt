package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.Post
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor() {
    
    private val client = SupabaseClient.client
    
    suspend fun createPost(post: Post): Result<Post> {
        return try {
            val result = client.from("posts").insert(post) {
                select()
            }.decodeSingle<Post>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPost(postId: String): Result<Post?> {
        return try {
            val post = client.from("posts")
                .select()
                .eq("post_id", postId)
                .decodeSingleOrNull<Post>()
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getPosts(limit: Int = 20, offset: Int = 0): Result<List<Post>> {
        return try {
            val posts = client.from("posts")
                .select()
                .order("created_at", ascending = false)
                .limit(limit.toLong())
                .range(offset.toLong(), (offset + limit - 1).toLong())
                .decodeList<Post>()
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun getUserPosts(userId: String, limit: Int = 20): Result<List<Post>> {
        return try {
            val posts = client.from("posts")
                .select()
                .eq("author_id", userId)
                .order("created_at", ascending = false)
                .limit(limit.toLong())
                .decodeList<Post>()
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun updatePost(postId: String, updates: Map<String, Any?>): Result<Post> {
        return try {
            val result = client.from("posts")
                .update(updates) {
                    select()
                }
                .eq("post_id", postId)
                .decodeSingle<Post>()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            client.from("posts")
                .update(mapOf("deleted_at" to "now()"))
                .eq("post_id", postId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchPosts(query: String): Result<List<Post>> {
        return try {
            val posts = client.from("posts")
                .select()
                .ilike("content", "%$query%")
                .order("created_at", ascending = false)
                .decodeList<Post>()
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observePosts(): Flow<List<Post>> = flow {
        try {
            val posts = getPosts().getOrElse { emptyList() }
            emit(posts)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
}