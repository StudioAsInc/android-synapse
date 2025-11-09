package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.Post
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

class PostRepository {
    
    private val client = SupabaseClient.client
    
    // In-memory cache for recently fetched pages
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(expirationMs: Long = CACHE_EXPIRATION_MS): Boolean {
            return System.currentTimeMillis() - timestamp > expirationMs
        }
    }
    
    private val postsCache = mutableMapOf<String, CacheEntry<List<Post>>>()
    
    companion object {
        private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L // 5 minutes
    }
    
    /**
     * Invalidate cache on refresh operations
     */
    fun invalidateCache() {
        postsCache.clear()
        android.util.Log.d("PostRepository", "Cache invalidated")
    }
    
    /**
     * Get cache key for a specific page
     */
    private fun getCacheKey(page: Int, pageSize: Int): String {
        return "posts_page_${page}_size_${pageSize}"
    }
    
    suspend fun createPost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("PostRepository", "Creating post: $post")
            
            // Check if Supabase is configured
            if (!SupabaseClient.isConfigured()) {
                android.util.Log.e("PostRepository", "Supabase is not configured properly")
                return@withContext Result.failure(Exception("Supabase not configured. Please update gradle.properties with your Supabase credentials."))
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
    
    suspend fun getPost(postId: String): Result<Post?> = withContext(Dispatchers.IO) {
        return@withContext try {
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
    
    suspend fun getPosts(limit: Int = 20, offset: Int = 0): Result<List<Post>> = withContext(Dispatchers.IO) {
        return@withContext try {
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
            
            // Provide more specific error messages
            val errorMessage = when {
                e.message?.contains("relation \"posts\" does not exist", ignoreCase = true) == true -> 
                    "Database table 'posts' does not exist. Please create the posts table in your Supabase database."
                e.message?.contains("connection", ignoreCase = true) == true -> 
                    "Cannot connect to Supabase. Check your internet connection and Supabase configuration."
                e.message?.contains("unauthorized", ignoreCase = true) == true -> 
                    "Unauthorized access to Supabase. Check your API key and RLS policies."
                e.message?.contains("serialization", ignoreCase = true) == true -> 
                    "Data format error. The database schema might not match the expected format."
                else -> "Database error: ${e.message}"
            }
            
            Result.failure(Exception(errorMessage))
        }
    }
    
    /**
     * Fetch posts with pagination support
     * @param page Page number (0-indexed)
     * @param pageSize Number of items per page
     * @return Result containing list of posts
     */
    suspend fun getPostsPage(page: Int, pageSize: Int): Result<List<Post>> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Check cache before making network request
            val cacheKey = getCacheKey(page, pageSize)
            val cachedEntry = postsCache[cacheKey]
            
            if (cachedEntry != null && !cachedEntry.isExpired()) {
                android.util.Log.d("PostRepository", "Returning cached posts for page $page")
                return@withContext Result.success(cachedEntry.data)
            }
            
            android.util.Log.d("PostRepository", "Fetching posts page $page with size $pageSize from Supabase...")
            
            // Calculate offset as page * pageSize
            val offset = page * pageSize
            
            // Use Supabase limit() and range() for pagination
            val posts = client.from("posts")
                .select() {
                    // Set the range for pagination
                    range(offset.toLong(), (offset + pageSize - 1).toLong())
                }
                .decodeList<Post>()
                .sortedByDescending { it.timestamp }
            
            // Store in cache
            postsCache[cacheKey] = CacheEntry(posts)
            
            android.util.Log.d("PostRepository", "Successfully fetched ${posts.size} posts for page $page")
            Result.success(posts)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Failed to fetch posts page: ${e.message}", e)
            
            // Provide detailed error messages for common failures
            val errorMessage = when {
                e.message?.contains("relation \"posts\" does not exist", ignoreCase = true) == true -> 
                    "Database table 'posts' does not exist. Please create the posts table in your Supabase database."
                e.message?.contains("connection", ignoreCase = true) == true -> 
                    "Cannot connect to Supabase. Check your internet connection and Supabase configuration."
                e.message?.contains("timeout", ignoreCase = true) == true -> 
                    "Request timed out. Please check your internet connection and try again."
                e.message?.contains("unauthorized", ignoreCase = true) == true -> 
                    "Unauthorized access to Supabase. Check your API key and RLS policies."
                e.message?.contains("serialization", ignoreCase = true) == true -> 
                    "Data format error. The database schema might not match the expected format."
                e.message?.contains("network", ignoreCase = true) == true -> 
                    "Network error occurred. Please check your internet connection."
                e.message?.contains("ssl", ignoreCase = true) == true -> 
                    "Secure connection failed. Please check your network settings."
                else -> "Failed to load posts: ${e.message ?: "Unknown error"}"
            }
            
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun getUserPosts(userId: String, limit: Int = 20): Result<List<Post>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val posts = client.from("posts")
                .select() {
                    filter {
                        eq("author_uid", userId)
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