package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.ReactionType
import com.synapse.social.studioasinc.model.UserReaction
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.gotrue.auth
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
            // Populate reaction data
            val postsWithReactions = populatePostReactions(posts)
            
            // Store in cache
            postsCache[cacheKey] = CacheEntry(postsWithReactions)
            
            android.util.Log.d("PostRepository", "Successfully fetched ${posts.size} posts for page $page")
            Result.success(postsWithReactions)
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
            
            val postsWithReactions = populatePostReactions(posts)
            Result.success(postsWithReactions)
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

    // ==================== REACTION METHODS ====================

    /**
     * Toggle a reaction on a post. If the user already reacted with that type, it removes it.
     * If the user reacted with a different type, it updates to the new type.
     */
    suspend fun toggleReaction(
        postId: String,
        userId: String,
        reactionType: ReactionType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("PostRepository", "Toggling reaction: ${reactionType.name} for post $postId by user $userId")

            // First, check if user already reacted
            val existingReaction = client.from("reactions")
                .select() {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<HashMap<String, Any>>()

            if (existingReaction != null) {
                val existingType = existingReaction["reaction_type"] as? String

                if (existingType == reactionType.name) {
                    // Remove the reaction (toggle off)
                    client.from("reactions")
                        .delete {
                            filter {
                                eq("post_id", postId)
                                eq("user_id", userId)
                            }
                        }
                    android.util.Log.d("PostRepository", "Reaction removed")
                } else {
                    // Update to new reaction type
                    client.from("reactions")
                        .update({
                            set("reaction_type", reactionType.name)
                            set("updated_at", java.time.Instant.now().toString())
                        }) {
                            filter {
                                eq("post_id", postId)
                                eq("user_id", userId)
                            }
                        }
                    android.util.Log.d("PostRepository", "Reaction updated to ${reactionType.name}")
                }
            } else {
                // Create new reaction
                val newReaction = mapOf(
                    "user_id" to userId,
                    "post_id" to postId,
                    "reaction_type" to reactionType.name
                )
                client.from("reactions").insert(newReaction)
                android.util.Log.d("PostRepository", "New reaction created")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Failed to toggle reaction", e)
            Result.failure(e)
        }
    }

    /**
     * Get reaction summary for a post (count of each reaction type)
     */
    suspend fun getReactionSummary(postId: String): Result<Map<ReactionType, Int>> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("PostRepository", "Fetching reaction summary for post $postId")

            val reactions = client.from("reactions")
                .select() {
                    filter {
                        eq("post_id", postId)
                    }
                }
                .decodeList<HashMap<String, Any>>()

            // Group by reaction type and count
            val summary = reactions
                .groupBy { 
                    val typeStr = it["reaction_type"] as? String ?: "LIKE"
                    ReactionType.fromString(typeStr)
                }
                .mapValues { it.value.size }

            android.util.Log.d("PostRepository", "Reaction summary: $summary")
            Result.success(summary)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Failed to fetch reaction summary", e)
            Result.failure(e)
        }
    }

    /**
     * Get users who reacted to a post, optionally filtered by reaction type
     */
    suspend fun getUsersWhoReacted(
        postId: String,
        reactionType: ReactionType? = null
    ): Result<List<UserReaction>> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d("PostRepository", "Fetching users who reacted to post $postId")

            // Fetch reactions
            val reactions = client.from("reactions")
                .select() {
                    filter {
                        eq("post_id", postId)
                        if (reactionType != null) {
                            eq("reaction_type", reactionType.name)
                        }
                    }
                }
                .decodeList<HashMap<String, Any>>()

            if (reactions.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            // Get user IDs
            val userIds = reactions.mapNotNull { it["user_id"] as? String }
            
            if (userIds.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            // Fetch user details
            val users = client.from("users")
                .select() {
                    filter {
                        isIn("uid", userIds)
                    }
                }
                .decodeList<HashMap<String, Any>>()
                .associateBy { it["uid"] as? String }

            // Map to UserReaction
            val userReactions = reactions.mapNotNull { reaction ->
                val userId = reaction["user_id"] as? String ?: return@mapNotNull null
                val user = users[userId]
                
                UserReaction(
                    userId = userId,
                    username = user?.get("username") as? String ?: "Unknown",
                    profileImage = user?.get("profile_image_url") as? String,
                    isVerified = user?.get("verify") as? Boolean ?: false,
                    reactionType = reaction["reaction_type"] as? String ?: "LIKE",
                    reactedAt = reaction["created_at"] as? String
                )
            }

            Result.success(userReactions)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Failed to fetch users who reacted", e)
            Result.failure(e)
        }
    }

    /**
     * Get current user's reaction on a post
     */
    suspend fun getUserReaction(postId: String, userId: String): Result<ReactionType?> = withContext(Dispatchers.IO) {
        return@withContext try {
            val reaction = client.from("reactions")
                .select() {
                    filter {
                        eq("post_id", postId)
                        eq("user_id", userId)
                    }
                }
                .decodeSingleOrNull<HashMap<String, Any>>()

            val reactionTypeStr = reaction?.get("reaction_type") as? String
            val reactionType = if (reactionTypeStr != null) ReactionType.fromString(reactionTypeStr) else null
            
            Result.success(reactionType)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Failed to fetch user reaction", e)
            Result.failure(e)
        }
    }
    /**
     * Helper to populate reaction data for a list of posts
     */
    private suspend fun populatePostReactions(posts: List<Post>): List<Post> {
        if (posts.isEmpty()) return posts
        
        return try {
            val postIds = posts.map { it.id }
            val currentUserId = client.auth.currentUserOrNull()?.id
            
            // 1. Fetch all reactions for these posts
            val allReactions = client.from("reactions")
                .select() {
                    filter {
                        isIn("post_id", postIds)
                    }
                }
                .decodeList<HashMap<String, Any>>()
            
            // 2. Group by post_id
            val reactionsByPost = allReactions.groupBy { it["post_id"] as? String }
            
            posts.map { post ->
                val postReactions = reactionsByPost[post.id] ?: emptyList()
                
                // Calculate summary
                val summary = postReactions
                    .groupBy { 
                        val typeStr = it["reaction_type"] as? String ?: "LIKE"
                        ReactionType.fromString(typeStr)
                    }
                    .mapValues { it.value.size }
                
                // Find user reaction
                var userReactionType: ReactionType? = null
                if (currentUserId != null) {
                    val userReaction = postReactions.find { 
                        (it["user_id"] as? String) == currentUserId 
                    }
                    if (userReaction != null) {
                        val typeStr = userReaction["reaction_type"] as? String
                        if (typeStr != null) {
                            userReactionType = ReactionType.fromString(typeStr)
                        }
                    }
                }
                
                post.copy(
                    reactions = summary,
                    userReaction = userReactionType
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Failed to populate reactions", e)
            posts
        }
    }
}