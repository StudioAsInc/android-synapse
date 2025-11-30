package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.PollOption
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
import kotlinx.serialization.json.*

class PostRepository {
    
    private val client = SupabaseClient.client
    
    private data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(expirationMs: Long = CACHE_EXPIRATION_MS): Boolean =
            System.currentTimeMillis() - timestamp > expirationMs
    }
    
    private val postsCache = mutableMapOf<String, CacheEntry<List<Post>>>()
    private val profileCache = mutableMapOf<String, CacheEntry<ProfileData>>()
    
    companion object {
        private const val CACHE_EXPIRATION_MS = 5 * 60 * 1000L
        private const val TAG = "PostRepository"
    }
    
    private data class ProfileData(
        val username: String?,
        val avatarUrl: String?,
        val isVerified: Boolean
    )
    
    fun invalidateCache() {
        postsCache.clear()
        profileCache.clear()
        android.util.Log.d(TAG, "Cache invalidated")
    }
    
    fun constructMediaUrl(storagePath: String): String {
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath
        }
        val supabaseUrl = SupabaseClient.getUrl()
        return "$supabaseUrl/storage/v1/object/public/post-media/$storagePath"
    }
    
    private fun constructAvatarUrl(storagePath: String): String {
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) {
            return storagePath
        }
        val supabaseUrl = SupabaseClient.getUrl()
        return "$supabaseUrl/storage/v1/object/public/user-avatars/$storagePath"
    }
    
    /**
     * Enhanced error mapping with Supabase error codes logged
     */
    private fun mapSupabaseError(exception: Exception): String {
        val message = exception.message ?: "Unknown error"
        
        // Extract and log PGRST error codes
        val pgrstMatch = Regex("PGRST\\d+").find(message)
        if (pgrstMatch != null) {
            android.util.Log.e(TAG, "Supabase PostgREST error code: ${pgrstMatch.value}")
        }
        
        android.util.Log.e(TAG, "Supabase error: $message", exception)
        
        return when {
            message.contains("PGRST200") -> "Relation/table not found in schema"
            message.contains("PGRST100") -> "Column does not exist"
            message.contains("PGRST116") -> "No rows returned (expected single)"
            message.contains("relation", ignoreCase = true) -> "Database table does not exist"
            message.contains("column", ignoreCase = true) -> "Database column mismatch"
            message.contains("policy", ignoreCase = true) || message.contains("rls", ignoreCase = true) -> 
                "Permission denied. Row-level security policy blocked this operation."
            message.contains("connection", ignoreCase = true) || message.contains("network", ignoreCase = true) -> 
                "Connection failed. Please check your internet connection."
            message.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            message.contains("unauthorized", ignoreCase = true) -> "Permission denied."
            message.contains("serialization", ignoreCase = true) -> "Data format error."
            else -> "Database error: $message"
        }
    }
    
    private fun getCacheKey(page: Int, pageSize: Int) = "posts_page_${page}_size_${pageSize}"
    
    suspend fun createPost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured."))
            }
            
            // Build insert data as JsonObject
            val insertData = buildJsonObject {
                put("id", post.id)
                post.key?.let { put("key", it) }
                put("author_uid", post.authorUid)
                post.postText?.let { put("post_text", it) }
                post.postImage?.let { put("post_image", it) }
                post.postType?.let { put("post_type", it) }
                post.postVisibility?.let { put("post_visibility", it) }
                post.postHideViewsCount?.let { put("post_hide_views_count", it) }
                post.postHideLikeCount?.let { put("post_hide_like_count", it) }
                post.postHideCommentsCount?.let { put("post_hide_comments_count", it) }
                post.postDisableComments?.let { put("post_disable_comments", it) }
                post.publishDate?.let { put("publish_date", it) }
                put("timestamp", post.timestamp)
                put("likes_count", 0)
                put("comments_count", 0)
                put("views_count", 0)
                // Media items as JSONB
                post.mediaItems?.let { items ->
                    put("media_items", buildJsonArray {
                        items.forEach { media ->
                            add(buildJsonObject {
                                put("id", media.id)
                                put("url", media.url)
                                put("type", media.type.name)
                                media.thumbnailUrl?.let { put("thumbnailUrl", it) }
                                media.duration?.let { put("duration", it) }
                                media.size?.let { put("size", it) }
                                media.mimeType?.let { put("mimeType", it) }
                            })
                        }
                    })
                }
                // Poll fields
                post.hasPoll?.let { put("has_poll", it) }
                post.pollQuestion?.let { put("poll_question", it) }
                post.pollOptions?.let { options ->
                    put("poll_options", buildJsonArray {
                        options.forEach { opt ->
                            add(buildJsonObject {
                                put("text", opt.text)
                                put("votes", opt.votes)
                            })
                        }
                    })
                }
                post.pollEndTime?.let { put("poll_end_time", it) }
                // Location fields
                post.hasLocation?.let { put("has_location", it) }
                post.locationName?.let { put("location_name", it) }
                post.locationAddress?.let { put("location_address", it) }
                post.locationLatitude?.let { put("location_latitude", it) }
                post.locationLongitude?.let { put("location_longitude", it) }
                // YouTube
                post.youtubeUrl?.let { put("youtube_url", it) }
            }
            
            android.util.Log.d(TAG, "Creating post with data: $insertData")
            client.from("posts").insert(insertData)
            invalidateCache()
            Result.success(post)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to create post", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }
    
    suspend fun getPost(postId: String): Result<Post?> = withContext(Dispatchers.IO) {
        try {
            val post = client.from("posts")
                .select { filter { eq("id", postId) } }
                .decodeSingleOrNull<Post>()
            Result.success(post)
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }
    
    suspend fun getPosts(limit: Int = 20, offset: Int = 0): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val posts = client.from("posts")
                .select { limit(limit.toLong()) }
                .decodeList<Post>()
                .sortedByDescending { it.timestamp }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }
    
    /**
     * Fetch posts with pagination - FIXED for current schema:
     * - Uses `users` table instead of `profiles`
     * - Media is embedded in `media_items` JSONB column (no post_media table)
     */
    suspend fun getPostsPage(page: Int, pageSize: Int): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val cacheKey = getCacheKey(page, pageSize)
            val cachedEntry = postsCache[cacheKey]
            if (cachedEntry != null && !cachedEntry.isExpired()) {
                return@withContext Result.success(cachedEntry.data)
            }
            
            android.util.Log.d(TAG, "Fetching posts page $page with size $pageSize")
            val offset = page * pageSize
            
            // Query posts with user join - media_items is already in posts table as JSONB
            val response = client.from("posts")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!posts_author_uid_fkey(uid, username, profile_image_url, verify)
                    """.trimIndent())
                ) {
                    range(offset.toLong(), (offset + pageSize - 1).toLong())
                }
                .decodeList<JsonObject>()
            
            android.util.Log.d(TAG, "Raw response count: ${response.size}")
            
            val posts = response.mapNotNull { postData ->
                try {
                    parsePostWithUserData(postData)
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Failed to parse post: ${e.message}", e)
                    null
                }
            }.sortedByDescending { it.timestamp }
            
            val postsWithReactions = populatePostReactions(posts)
            postsCache[cacheKey] = CacheEntry(postsWithReactions)
            
            android.util.Log.d(TAG, "Successfully fetched ${posts.size} posts for page $page")
            Result.success(postsWithReactions)
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to fetch posts page: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }
    
    /**
     * Parse Post from JsonObject with embedded user data and media_items JSONB
     */
    private fun parsePostWithUserData(data: JsonObject): Post {
        val post = Post(
            id = data["id"]?.jsonPrimitive?.contentOrNull ?: "",
            key = data["key"]?.jsonPrimitive?.contentOrNull,
            authorUid = data["author_uid"]?.jsonPrimitive?.contentOrNull ?: "",
            postText = data["post_text"]?.jsonPrimitive?.contentOrNull,
            postImage = data["post_image"]?.jsonPrimitive?.contentOrNull?.let { constructMediaUrl(it) },
            postType = data["post_type"]?.jsonPrimitive?.contentOrNull,
            postHideViewsCount = data["post_hide_views_count"]?.jsonPrimitive?.contentOrNull,
            postHideLikeCount = data["post_hide_like_count"]?.jsonPrimitive?.contentOrNull,
            postHideCommentsCount = data["post_hide_comments_count"]?.jsonPrimitive?.contentOrNull,
            postDisableComments = data["post_disable_comments"]?.jsonPrimitive?.contentOrNull,
            postVisibility = data["post_visibility"]?.jsonPrimitive?.contentOrNull,
            publishDate = data["publish_date"]?.jsonPrimitive?.contentOrNull,
            timestamp = data["timestamp"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
            likesCount = data["likes_count"]?.jsonPrimitive?.intOrNull ?: 0,
            commentsCount = data["comments_count"]?.jsonPrimitive?.intOrNull ?: 0,
            viewsCount = data["views_count"]?.jsonPrimitive?.intOrNull ?: 0,
            resharesCount = data["reshares_count"]?.jsonPrimitive?.intOrNull ?: 0,
            // Poll fields
            hasPoll = data["has_poll"]?.jsonPrimitive?.booleanOrNull,
            pollQuestion = data["poll_question"]?.jsonPrimitive?.contentOrNull,
            pollOptions = data["poll_options"]?.jsonArray?.mapNotNull {
                val obj = it.jsonObject
                val text = obj["text"]?.jsonPrimitive?.contentOrNull
                val votes = obj["votes"]?.jsonPrimitive?.intOrNull ?: 0
                if (text != null) PollOption(text, votes) else null
            },
            pollEndTime = data["poll_end_time"]?.jsonPrimitive?.contentOrNull,
            // Location fields
            hasLocation = data["has_location"]?.jsonPrimitive?.booleanOrNull,
            locationName = data["location_name"]?.jsonPrimitive?.contentOrNull,
            locationAddress = data["location_address"]?.jsonPrimitive?.contentOrNull,
            locationLatitude = data["location_latitude"]?.jsonPrimitive?.doubleOrNull,
            locationLongitude = data["location_longitude"]?.jsonPrimitive?.doubleOrNull,
            // YouTube
            youtubeUrl = data["youtube_url"]?.jsonPrimitive?.contentOrNull
        )
        
        // Extract user data from join (users table, not profiles)
        val userData = data["users"]?.jsonObject
        if (userData != null) {
            post.username = userData["username"]?.jsonPrimitive?.contentOrNull
            post.avatarUrl = userData["profile_image_url"]?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) }
            post.isVerified = userData["verify"]?.jsonPrimitive?.booleanOrNull ?: false
            
            val authorUid = post.authorUid
            if (authorUid.isNotEmpty()) {
                profileCache[authorUid] = CacheEntry(
                    ProfileData(post.username, post.avatarUrl, post.isVerified)
                )
            }
        }
        
        // Parse media_items from JSONB column (already in posts table)
        val mediaData = data["media_items"]?.takeIf { it !is JsonNull }?.jsonArray
        if (mediaData != null && mediaData.isNotEmpty()) {
            post.mediaItems = mediaData.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val typeStr = mediaMap["type"]?.jsonPrimitive?.contentOrNull ?: "IMAGE"
                MediaItem(
                    id = mediaMap["id"]?.jsonPrimitive?.contentOrNull ?: "",
                    url = constructMediaUrl(url),
                    type = if (typeStr.equals("VIDEO", ignoreCase = true)) MediaType.VIDEO else MediaType.IMAGE,
                    thumbnailUrl = mediaMap["thumbnailUrl"]?.jsonPrimitive?.contentOrNull?.let { constructMediaUrl(it) },
                    duration = mediaMap["duration"]?.jsonPrimitive?.longOrNull,
                    size = mediaMap["size"]?.jsonPrimitive?.longOrNull,
                    mimeType = mediaMap["mimeType"]?.jsonPrimitive?.contentOrNull
                )
            }.toMutableList()
        }
        
        return post
    }
    
    suspend fun getUserPosts(userId: String, limit: Int = 20): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val posts = client.from("posts")
                .select { filter { eq("author_uid", userId) }; limit(limit.toLong()) }
                .decodeList<Post>()
                .sortedByDescending { it.timestamp }
            Result.success(populatePostReactions(posts))
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }
    
    suspend fun updatePost(postId: String, updates: Map<String, Any?>): Result<Post> {
        return Result.success(Post(id = postId, authorUid = ""))
    }
    
    suspend fun deletePost(postId: String): Result<Unit> = Result.success(Unit)
    
    suspend fun searchPosts(query: String): Result<List<Post>> = Result.success(emptyList())
    
    fun observePosts(): Flow<List<Post>> = flow { emit(emptyList()) }

    // ==================== REACTION METHODS (using reactions table) ====================

    /**
     * Toggle reaction - uses `reactions` table (not `likes`)
     */
    suspend fun toggleReaction(
        postId: String,
        userId: String,
        reactionType: ReactionType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            android.util.Log.d(TAG, "Toggling reaction: ${reactionType.name} for post $postId")
            
            // Verify authentication
            val currentUser = client.auth.currentUserOrNull()
            if (currentUser == null || userId.isEmpty()) {
                return@withContext Result.failure(Exception("User must be authenticated to react"))
            }
            
            var lastException: Exception? = null
            repeat(3) { attempt ->
                try {
                    val existingReaction = client.from("reactions")
                        .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                        .decodeSingleOrNull<JsonObject>()

                    if (existingReaction != null) {
                        val existingType = existingReaction["reaction_type"]?.jsonPrimitive?.contentOrNull
                        if (existingType == reactionType.name) {
                            // Remove reaction
                            client.from("reactions")
                                .delete { filter { eq("post_id", postId); eq("user_id", userId) } }
                            android.util.Log.d(TAG, "Reaction removed")
                        } else {
                            // Update reaction type
                            client.from("reactions")
                                .update({
                                    set("reaction_type", reactionType.name)
                                    set("updated_at", java.time.Instant.now().toString())
                                }) { filter { eq("post_id", postId); eq("user_id", userId) } }
                            android.util.Log.d(TAG, "Reaction updated to ${reactionType.name}")
                        }
                    } else {
                        // Insert new reaction
                        client.from("reactions").insert(buildJsonObject {
                            put("user_id", userId)
                            put("post_id", postId)
                            put("reaction_type", reactionType.name)
                        })
                        android.util.Log.d(TAG, "New reaction created")
                    }
                    return@withContext Result.success(Unit)
                } catch (e: Exception) {
                    lastException = e
                    val isRLSError = e.message?.contains("policy", true) == true
                    if (isRLSError || attempt == 2) throw e
                    kotlinx.coroutines.delay(100L * (attempt + 1))
                }
            }
            Result.failure(Exception(mapSupabaseError(lastException ?: Exception("Unknown"))))
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun getReactionSummary(postId: String): Result<Map<ReactionType, Int>> = withContext(Dispatchers.IO) {
        try {
            val reactions = client.from("reactions")
                .select { filter { eq("post_id", postId) } }
                .decodeList<JsonObject>()
            
            val summary = reactions
                .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                .mapValues { it.value.size }
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun getUsersWhoReacted(
        postId: String,
        reactionType: ReactionType? = null
    ): Result<List<UserReaction>> = withContext(Dispatchers.IO) {
        try {
            val reactions = client.from("reactions")
                .select {
                    filter {
                        eq("post_id", postId)
                        if (reactionType != null) eq("reaction_type", reactionType.name)
                    }
                }
                .decodeList<JsonObject>()

            if (reactions.isEmpty()) return@withContext Result.success(emptyList())

            val userIds = reactions.mapNotNull { it["user_id"]?.jsonPrimitive?.contentOrNull }
            if (userIds.isEmpty()) return@withContext Result.success(emptyList())

            val users = client.from("users")
                .select { filter { isIn("uid", userIds) } }
                .decodeList<JsonObject>()
                .associateBy { it["uid"]?.jsonPrimitive?.contentOrNull }

            val userReactions = reactions.mapNotNull { reaction ->
                val userId = reaction["user_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val user = users[userId]
                UserReaction(
                    userId = userId,
                    username = user?.get("username")?.jsonPrimitive?.contentOrNull ?: "Unknown",
                    profileImage = user?.get("profile_image_url")?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) },
                    isVerified = user?.get("verify")?.jsonPrimitive?.booleanOrNull ?: false,
                    reactionType = reaction["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE",
                    reactedAt = reaction["created_at"]?.jsonPrimitive?.contentOrNull
                )
            }
            Result.success(userReactions)
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    suspend fun getUserReaction(postId: String, userId: String): Result<ReactionType?> = withContext(Dispatchers.IO) {
        try {
            val reaction = client.from("reactions")
                .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                .decodeSingleOrNull<JsonObject>()
            
            val typeStr = reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull
            Result.success(typeStr?.let { ReactionType.fromString(it) })
        } catch (e: Exception) {
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    private suspend fun populatePostReactions(posts: List<Post>): List<Post> {
        if (posts.isEmpty()) return posts
        
        return try {
            val postIds = posts.map { it.id }
            val currentUserId = client.auth.currentUserOrNull()?.id
            
            val allReactions = client.from("reactions")
                .select { filter { isIn("post_id", postIds) } }
                .decodeList<JsonObject>()
            
            val reactionsByPost = allReactions.groupBy { it["post_id"]?.jsonPrimitive?.contentOrNull }
            
            posts.map { post ->
                val postReactions = reactionsByPost[post.id] ?: emptyList()
                val summary = postReactions
                    .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                    .mapValues { it.value.size }
                
                val userReactionType = if (currentUserId != null) {
                    postReactions.find { it["user_id"]?.jsonPrimitive?.contentOrNull == currentUserId }
                        ?.let { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                } else null
                
                post.copy(reactions = summary, userReaction = userReactionType)
            }
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Failed to populate reactions", e)
            posts
        }
    }
}
