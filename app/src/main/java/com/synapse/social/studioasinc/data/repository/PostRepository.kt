package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.ReactionType
import com.synapse.social.studioasinc.model.UserReaction
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
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
    private val profileCache = mutableMapOf<String, CacheEntry<ProfileData>>()
    
    companion object {
        private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L // 5 minutes
        private const val TAG = "PostRepository"
    }
    
    /**
     * Profile data for caching
     */
    private data class ProfileData(
        val username: String?,
        val avatarUrl: String?,
        val isVerified: Boolean
    )
    
    /**
     * Invalidate cache on refresh operations
     */
    fun invalidateCache() {
        postsCache.clear()
        profileCache.clear()
        android.util.Log.d(TAG, "Cache invalidated")
    }
    
    /**
     * Construct full media URL from storage path
     */
    fun constructMediaUrl(storagePath: String): String {
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath
        }
        
        val supabaseUrl = SupabaseClient.getUrl()
        val bucketName = "post-media"
        return "$supabaseUrl/storage/v1/object/public/$bucketName/$storagePath"
    }
    
    /**
     * Map Supabase errors to user-friendly messages
     */
    private fun mapSupabaseError(exception: Exception): String {
        android.util.Log.e(TAG, "Supabase error: ${exception.message}", exception)
        
        return when {
            exception.message?.contains("relation", ignoreCase = true) == true -> 
                "Database table does not exist"
            exception.message?.contains("connection", ignoreCase = true) == true -> 
                "Connection failed. Please check your internet connection."
            exception.message?.contains("timeout", ignoreCase = true) == true -> 
                "Request timed out. Please try again."
            exception.message?.contains("unauthorized", ignoreCase = true) == true -> 
                "Permission denied. Please check your account status."
            exception.message?.contains("policy", ignoreCase = true) == true ||
            exception.message?.contains("rls", ignoreCase = true) == true -> 
                "Permission denied. Row-level security policy blocked this operation."
            exception.message?.contains("network", ignoreCase = true) == true -> 
                "Network error. Please check your connection."
            exception.message?.contains("serialization", ignoreCase = true) == true -> 
                "Data format error. Please contact support."
            else -> "Database error: ${exception.message ?: "Unknown error"}"
        }
    }
    
    /**
     * Get cache key for a specific page
     */
    private fun getCacheKey(page: Int, pageSize: Int): String {
        return "posts_page_${page}_size_${pageSize}"
    }
    
    suspend fun createPost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d(TAG, "Creating post: $post")
            
            // Check if Supabase is configured
            if (!SupabaseClient.isConfigured()) {
                android.util.Log.e(TAG, "Supabase is not configured properly")
                return@withContext Result.failure(Exception("Supabase not configured. Please update gradle.properties with your Supabase credentials."))
            }
            
            client.from("posts").insert(post)
            android.util.Log.d(TAG, "Post created successfully")
            Result.success(post)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to create post", e)
            val errorMessage = mapSupabaseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun getPost(postId: String): Result<Post?> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d(TAG, "Fetching post: $postId")
            val post = client.from("posts")
                .select() {
                    filter {
                        eq("id", postId)
                    }
                }
                .decodeSingleOrNull<Post>()
            
            android.util.Log.d(TAG, "Post fetched successfully")
            Result.success(post)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch post", e)
            val errorMessage = mapSupabaseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
    
    suspend fun getPosts(limit: Int = 20, offset: Int = 0): Result<List<Post>> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d(TAG, "Fetching posts from Supabase...")
            
            val posts = client.from("posts")
                .select() {
                    limit(limit.toLong())
                }
                .decodeList<Post>()
                .sortedByDescending { it.timestamp }
            
            android.util.Log.d(TAG, "Successfully fetched ${posts.size} posts")
            Result.success(posts)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch posts: ${e.message}", e)
            val errorMessage = mapSupabaseError(e)
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
                android.util.Log.d(TAG, "Returning cached posts for page $page")
                return@withContext Result.success(cachedEntry.data)
            }
            
            android.util.Log.d(TAG, "Fetching posts page $page with size $pageSize from Supabase...")
            
            // Calculate offset as page * pageSize
            val offset = page * pageSize
            
            // Use single query with joins for profiles and post_media tables
            val response = client.from("posts")
                .select(
                    columns = Columns.raw("""
                        *,
                        profiles!inner(username, avatar_url, verify),
                        post_media!left(id, url, type, position, created_at)
                    """.trimIndent())
                ) {
                    range(offset.toLong(), (offset + pageSize - 1).toLong())
                }
                .decodeList<HashMap<String, Any>>()
            
            android.util.Log.d(TAG, "Raw response: $response")
            
            // Parse response and construct Post objects with joined data
            val posts = response.mapNotNull { postData ->
                try {
                    val post = parsePostFromHashMap(postData)
                    
                    // Extract profile data from join
                    val profileData = postData["profiles"] as? Map<*, *>
                    if (profileData != null) {
                        post.username = profileData["username"] as? String
                        post.avatarUrl = profileData["avatar_url"] as? String
                        post.isVerified = profileData["verify"] as? Boolean ?: false
                        
                        // Cache profile data
                        val authorUid = post.authorUid
                        if (authorUid.isNotEmpty()) {
                            profileCache[authorUid] = CacheEntry(
                                ProfileData(
                                    username = post.username,
                                    avatarUrl = post.avatarUrl,
                                    isVerified = post.isVerified
                                )
                            )
                        }
                    }
                    
                    // Extract media items from join
                    val mediaData = postData["post_media"] as? List<*>
                    if (mediaData != null && mediaData.isNotEmpty()) {
                        val mediaItems = mediaData.mapNotNull { mediaItem ->
                            val mediaMap = mediaItem as? Map<*, *> ?: return@mapNotNull null
                            try {
                                val url = mediaMap["url"] as? String ?: return@mapNotNull null
                                val typeStr = mediaMap["type"] as? String ?: "IMAGE"
                                val position = (mediaMap["position"] as? Number)?.toInt() ?: 0
                                val createdAt = mediaMap["created_at"] as? String ?: ""
                                
                                Pair(
                                    position to createdAt,
                                    MediaItem(
                                        id = mediaMap["id"] as? String ?: "",
                                        url = constructMediaUrl(url),
                                        type = if (typeStr.equals("VIDEO", ignoreCase = true)) MediaType.VIDEO else MediaType.IMAGE,
                                        thumbnailUrl = null,
                                        duration = null,
                                        size = null,
                                        mimeType = null
                                    )
                                )
                            } catch (e: Exception) {
                                android.util.Log.e(TAG, "Failed to parse media item: ${e.message}")
                                null
                            }
                        }.sortedWith(compareBy({ it.first.first }, { it.first.second }))
                         .map { it.second }
                        
                        post.mediaItems = mediaItems.toMutableList()
                    }
                    
                    post
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to parse post: ${e.message}", e)
                    null
                }
            }.sortedByDescending { it.timestamp }
            
            // Populate reaction data
            val postsWithReactions = populatePostReactions(posts)
            
            // Store in cache
            postsCache[cacheKey] = CacheEntry(postsWithReactions)
            
            android.util.Log.d(TAG, "Successfully fetched ${posts.size} posts for page $page")
            Result.success(postsWithReactions)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch posts page: ${e.message}", e)
            val errorMessage = mapSupabaseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
    
    /**
     * Parse Post object from HashMap response
     */
    private fun parsePostFromHashMap(data: HashMap<String, Any>): Post {
        return Post(
            id = data["id"] as? String ?: "",
            key = data["key"] as? String,
            authorUid = data["author_uid"] as? String ?: "",
            postText = data["post_text"] as? String,
            postImage = data["post_image"] as? String,
            postType = data["post_type"] as? String,
            postHideViewsCount = data["post_hide_views_count"] as? String,
            postHideLikeCount = data["post_hide_like_count"] as? String,
            postHideCommentsCount = data["post_hide_comments_count"] as? String,
            postDisableComments = data["post_disable_comments"] as? String,
            postVisibility = data["post_visibility"] as? String,
            publishDate = data["publish_date"] as? String,
            timestamp = (data["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis(),
            likesCount = (data["likes_count"] as? Number)?.toInt() ?: 0,
            commentsCount = (data["comments_count"] as? Number)?.toInt() ?: 0,
            viewsCount = (data["views_count"] as? Number)?.toInt() ?: 0
        )
    }
    
    suspend fun getUserPosts(userId: String, limit: Int = 20): Result<List<Post>> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d(TAG, "Fetching posts for user: $userId")
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
            android.util.Log.d(TAG, "Successfully fetched ${posts.size} user posts")
            Result.success(postsWithReactions)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch user posts", e)
            val errorMessage = mapSupabaseError(e)
            Result.failure(Exception(errorMessage))
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
     * 
     * Enhanced with:
     * - Authentication verification before proceeding
     * - Retry logic for network failures (up to 2 retries)
     * - Specific error messages for RLS policy failures
     * - Comprehensive logging for all operations
     */
    suspend fun toggleReaction(
        postId: String,
        userId: String,
        reactionType: ReactionType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d(TAG, "Toggling reaction: ${reactionType.name} for post $postId by user $userId")
            
            // Verify authentication before proceeding (Requirement 5.1)
            if (userId.isEmpty() || userId.length < 8) {
                android.util.Log.e(TAG, "Authentication verification failed: Invalid user ID")
                return@withContext Result.failure(Exception("User must be authenticated to react to posts"))
            }
            
            // Verify user is actually authenticated with Supabase
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null) {
                android.util.Log.e(TAG, "Authentication verification failed: No authenticated user")
                return@withContext Result.failure(Exception("User must be authenticated to react to posts"))
            }
            
            android.util.Log.d(TAG, "Authentication verified for user $userId")
            
            // Attempt the reaction operation with retry logic (Requirement 5.4)
            var lastException: Exception? = null
            val maxRetries = 2
            var shouldRetry = true
            var attempt = 0
            
            while (attempt <= maxRetries && shouldRetry) {
                try {
                    android.util.Log.d(TAG, "Reaction attempt ${attempt + 1} of ${maxRetries + 1}")
                    
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
                            android.util.Log.d(TAG, "Reaction removed successfully")
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
                            android.util.Log.d(TAG, "Reaction updated to ${reactionType.name} successfully")
                        }
                    } else {
                        // Create new reaction (Requirement 5.2)
                        val newReaction = mapOf(
                            "user_id" to userId,
                            "post_id" to postId,
                            "reaction_type" to reactionType.name
                        )
                        client.from("reactions").insert(newReaction)
                        android.util.Log.d(TAG, "New reaction created successfully")
                    }

                    // Success - return immediately
                    return@withContext Result.success(Unit)
                    
                } catch (e: Exception) {
                    lastException = e
                    android.util.Log.e(TAG, "Reaction attempt ${attempt + 1} failed: ${e.message}", e)
                    
                    // Check if this is an RLS error (don't retry RLS errors)
                    val isRLSError = e.message?.contains("policy", ignoreCase = true) == true ||
                                    e.message?.contains("rls", ignoreCase = true) == true ||
                                    e.message?.contains("row-level security", ignoreCase = true) == true
                    
                    if (isRLSError) {
                        android.util.Log.e(TAG, "RLS policy error detected - not retrying")
                        shouldRetry = false
                    } else {
                        // Check if this is a network error (retry these)
                        val isNetworkError = e.message?.contains("network", ignoreCase = true) == true ||
                                            e.message?.contains("connection", ignoreCase = true) == true ||
                                            e.message?.contains("timeout", ignoreCase = true) == true
                        
                        if (!isNetworkError) {
                            android.util.Log.e(TAG, "Non-network error detected - not retrying")
                            shouldRetry = false
                        } else if (attempt < maxRetries) {
                            // If we have more retries, wait with exponential backoff
                            val backoffMs = 100L * (attempt + 1) // 100ms, 200ms
                            android.util.Log.d(TAG, "Waiting ${backoffMs}ms before retry")
                            kotlinx.coroutines.delay(backoffMs)
                        }
                    }
                }
                
                attempt++
            }
            
            // All retries exhausted - return failure with mapped error
            android.util.Log.e(TAG, "All reaction attempts failed", lastException)
            val errorMessage = mapSupabaseError(lastException ?: Exception("Unknown error"))
            Result.failure(Exception(errorMessage))
            
        } catch (e: Exception) {
            // Catch any unexpected errors
            android.util.Log.e(TAG, "Unexpected error in toggleReaction", e)
            val errorMessage = mapSupabaseError(e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Get reaction summary for a post (count of each reaction type)
     */
    suspend fun getReactionSummary(postId: String): Result<Map<ReactionType, Int>> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d(TAG, "Fetching reaction summary for post $postId")

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

            android.util.Log.d(TAG, "Reaction summary: $summary")
            Result.success(summary)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch reaction summary", e)
            val errorMessage = mapSupabaseError(e)
            Result.failure(Exception(errorMessage))
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
            android.util.Log.d(TAG, "Fetching users who reacted to post $postId")

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
                    username = user?.get("username") as? String ?: "Unknown User",
                    profileImage = user?.get("profile_image_url") as? String,
                    isVerified = user?.get("verify") as? Boolean ?: false,
                    reactionType = reaction["reaction_type"] as? String ?: "LIKE",
                    reactedAt = reaction["created_at"] as? String
                )
            }

            android.util.Log.d(TAG, "Found ${userReactions.size} users who reacted")
            Result.success(userReactions)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch users who reacted", e)
            val errorMessage = mapSupabaseError(e)
            Result.failure(Exception(errorMessage))
        }
    }

    /**
     * Get current user's reaction on a post
     */
    suspend fun getUserReaction(postId: String, userId: String): Result<ReactionType?> = withContext(Dispatchers.IO) {
        return@withContext try {
            android.util.Log.d(TAG, "Fetching user reaction for post $postId")
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
            
            android.util.Log.d(TAG, "User reaction: $reactionType")
            Result.success(reactionType)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch user reaction", e)
            val errorMessage = mapSupabaseError(e)
            Result.failure(Exception(errorMessage))
        }
    }
    /**
     * Helper to populate reaction data for a list of posts
     */
    private suspend fun populatePostReactions(posts: List<Post>): List<Post> {
        if (posts.isEmpty()) return posts
        
        return try {
            android.util.Log.d(TAG, "Populating reactions for ${posts.size} posts")
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
            
            android.util.Log.d(TAG, "Fetched ${allReactions.size} reactions")
            
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
            android.util.Log.e(TAG, "Failed to populate reactions", e)
            posts
        }
    }
}