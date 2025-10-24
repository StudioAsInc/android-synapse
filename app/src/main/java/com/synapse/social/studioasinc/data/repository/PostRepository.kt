package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.Post
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PostRepository {
    
    private val client = SupabaseClient.client
    
    suspend fun createPost(post: Post): Result<Post> {
        return try {
            android.util.Log.d("PostRepository", "Creating post: $post")
            
            // Check if Supabase is configured
            if (!SupabaseClient.isConfigured()) {
                android.util.Log.e("PostRepository", "Supabase is not configured properly")
                return Result.failure(Exception("Supabase not configured. Please update gradle.properties with your Supabase credentials."))
            }
            
            client.from("posts").insert(post)
            android.util.Log.d("PostRepository", "Post created successfully")
            Result.success(post)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Failed to create post", e)
            
            // Provide more specific error messages
            val errorMessage = when {
                e.message?.contains("relation \"posts\" does not exist", ignoreCase = true) == true -> 
                    "Database table 'posts' does not exist. Please create the posts table in your Supabase database."
                e.message?.contains("violates foreign key constraint", ignoreCase = true) == true -> 
                    "User profile not found. Please complete your profile first."
                e.message?.contains("connection", ignoreCase = true) == true -> 
                    "Cannot connect to Supabase. Check your internet connection and Supabase configuration."
                e.message?.contains("unauthorized", ignoreCase = true) == true -> 
                    "Unauthorized access to Supabase. Check your API key and RLS policies."
                else -> "Database error: ${e.message}"
            }
            
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun getPost(postId: String): Result<Post?> {
        return try {
            val post = client.from("posts")
                .select() {
                    filter {
                        eq("id", postId)
                    }
                }
                .decodeSingleOrNull<Post>()
            
            Result.success(post)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Failed to fetch post", e)
            Result.failure(e)
        }
    }
    
    suspend fun getPosts(limit: Int = 20, offset: Int = 0): Result<List<Post>> {
        return try {
            android.util.Log.d("PostRepository", "Fetching posts from Supabase...")
            
            val posts = client.from("posts")
                .select() {
                    limit(limit.toLong())
                }
                .decodeList<Post>()
                .sortedByDescending { it.timestamp }
            
            android.util.Log.d("PostRepository", "Successfully fetched ${posts.size} posts")
            Result.success(posts)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Failed to fetch posts: ${e.message}", e)
            
            // If serialization fails, try to fetch as raw data
            try {
                android.util.Log.d("PostRepository", "Trying to fetch posts as raw data...")
                val rawPosts = client.from("posts")
                    .select() {
                        limit(limit.toLong())
                    }
                    .decodeList<kotlinx.serialization.json.JsonObject>()
                
                android.util.Log.d("PostRepository", "Raw posts data: $rawPosts")
                Result.success(emptyList()) // Return empty list for now
            } catch (rawException: Exception) {
                android.util.Log.e("PostRepository", "Raw fetch also failed: ${rawException.message}", rawException)
                Result.failure(e)
            }
        }
    }
    
    suspend fun getUserPosts(userId: String, limit: Int = 20): Result<List<Post>> {
        return try {
            val posts = client.from("posts")
                .select() {
                    filter {
                        eq("authorUid", userId)
                    }
                    limit(limit.toLong())
                }
                .decodeList<Post>()
                .sortedByDescending { it.timestamp }
            
            Result.success(posts)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Failed to fetch user posts", e)
            Result.failure(e)
        }
    }
    
    suspend fun updatePost(postId: String, updates: Map<String, Any?>): Result<Post> {
        return try {
            val post = Post(id = postId, authorUid = "")
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchPosts(query: String): Result<List<Post>> {
        return try {
            Result.success(emptyList())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    fun observePosts(): Flow<List<Post>> = flow {
        emit(emptyList())
    }
}