package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.SynapseApplication
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.PollOption
import com.synapse.social.studioasinc.model.ReactionType
import com.synapse.social.studioasinc.model.UserReaction
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import com.synapse.social.studioasinc.data.Result
import com.synapse.social.studioasinc.util.ErrorHandler
import com.synapse.social.studioasinc.util.RetryPolicy
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
    private val retryPolicy = RetryPolicy()

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

    private fun getCacheKey(page: Int, pageSize: Int) = "posts_page_${page}_size_${pageSize}"

    suspend fun createPost(post: Post): Result<Post> = withContext(Dispatchers.IO) {
        try {
            retryPolicy.executeWithRetry {
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
                    post.hasLocation?.let { put("has_location", it) }
                    post.locationName?.let { put("location_name", it) }
                    post.locationAddress?.let { put("location_address", it) }
                    post.locationLatitude?.let { put("location_latitude", it) }
                    post.locationLongitude?.let { put("location_longitude", it) }
                    post.youtubeUrl?.let { put("youtube_url", it) }
                }
                client.from("posts").insert(insertData)
            }
            invalidateCache()
            Result.Success(post)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun getPost(postId: String): Result<Post?> = withContext(Dispatchers.IO) {
        try {
            val post = retryPolicy.executeWithRetry {
                client.from("posts")
                    .select { filter { eq("id", postId) } }
                    .decodeSingleOrNull<Post>()
            }
            Result.Success(post)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun getPosts(limit: Int = 20, offset: Int = 0): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val posts = retryPolicy.executeWithRetry {
                client.from("posts")
                    .select { limit(limit.toLong()) }
                    .decodeList<Post>()
                    .sortedByDescending { it.timestamp }
            }
            Result.Success(posts)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun getPostsPage(page: Int, pageSize: Int): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val cacheKey = getCacheKey(page, pageSize)
            val cachedEntry = postsCache[cacheKey]
            if (cachedEntry != null && !cachedEntry.isExpired()) {
                return@withContext Result.Success(cachedEntry.data)
            }

            val response = retryPolicy.executeWithRetry {
                client.from("posts")
                    .select(
                        columns = Columns.raw("""
                            *,
                            users!posts_author_uid_fkey(uid, username, avatar, verify)
                        """.trimIndent())
                    ) {
                        range((page * pageSize).toLong(), ((page + 1) * pageSize - 1).toLong())
                    }
                    .decodeList<JsonObject>()
            }

            val posts = response.mapNotNull { parsePostWithUserData(it) }.sortedByDescending { it.timestamp }
            val postsWithReactions = populatePostReactions(posts)
            postsCache[cacheKey] = CacheEntry(postsWithReactions)
            Result.Success(postsWithReactions)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

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
            hasPoll = data["has_poll"]?.jsonPrimitive?.booleanOrNull,
            pollQuestion = data["poll_question"]?.jsonPrimitive?.contentOrNull,
            pollOptions = data["poll_options"]?.jsonArray?.mapNotNull {
                val obj = it.jsonObject
                val text = obj["text"]?.jsonPrimitive?.contentOrNull
                val votes = obj["votes"]?.jsonPrimitive?.intOrNull ?: 0
                if (text != null) PollOption(text, votes) else null
            },
            pollEndTime = data["poll_end_time"]?.jsonPrimitive?.contentOrNull,
            hasLocation = data["has_location"]?.jsonPrimitive?.booleanOrNull,
            locationName = data["location_name"]?.jsonPrimitive?.contentOrNull,
            locationAddress = data["location_address"]?.jsonPrimitive?.contentOrNull,
            locationLatitude = data["location_latitude"]?.jsonPrimitive?.doubleOrNull,
            locationLongitude = data["location_longitude"]?.jsonPrimitive?.doubleOrNull,
            youtubeUrl = data["youtube_url"]?.jsonPrimitive?.contentOrNull
        )
        
        val userData = data["users"]?.jsonObject
        if (userData != null) {
            post.username = userData["username"]?.jsonPrimitive?.contentOrNull
            post.avatarUrl = userData["avatar"]?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) }
            post.isVerified = userData["verify"]?.jsonPrimitive?.booleanOrNull ?: false
            
            val authorUid = post.authorUid
            if (authorUid.isNotEmpty()) {
                profileCache[authorUid] = CacheEntry(
                    ProfileData(post.username, post.avatarUrl, post.isVerified)
                )
            }
        }
        
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
            val posts = retryPolicy.executeWithRetry {
                client.from("posts")
                    .select { filter { eq("author_uid", userId) }; limit(limit.toLong()) }
                    .decodeList<Post>()
                    .sortedByDescending { it.timestamp }
            }
            Result.Success(populatePostReactions(posts))
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun updatePost(postId: String, updates: Map<String, Any?>): Result<Post> = withContext(Dispatchers.IO) {
        try {
            retryPolicy.executeWithRetry {
                client.from("posts").update(updates) {
                    filter { eq("id", postId) }
                }
            }
            Result.Success(Post(id = postId, authorUid = "")) // Placeholder
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            retryPolicy.executeWithRetry {
                client.from("posts").delete { filter { eq("id", postId) } }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun searchPosts(query: String): Result<List<Post>> = withContext(Dispatchers.IO) {
        try {
            val posts = retryPolicy.executeWithRetry {
                client.from("posts")
                    .select {
                        filter {
                            or {
                                ilike("post_text", "%$query%")
                                ilike("poll_question", "%$query%")
                            }
                        }
                    }
                    .decodeList<Post>()
            }
            Result.Success(posts)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    fun observePosts(): Flow<Result<List<Post>>> = flow {
        emit(Result.Loading)
        try {
            client.channel("posts").postgresChangeFlow<PostgresAction>(schema = "public") {
                table = "posts"
            }.map {
                when (val result = getPosts()) {
                    is Result.Success -> Result.Success(result.data)
                    is Result.Error -> Result.Error(result.exception, result.message)
                    else -> Result.Loading
                }
            }.catch { e ->
                emit(Result.Error(e as Exception, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext())))
            }.collect { emit(it) }
        } catch (e: Exception) {
            emit(Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext())))
        }
    }

    suspend fun toggleReaction(
        postId: String,
        userId: String,
        reactionType: ReactionType
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            retryPolicy.executeWithRetry {
                val existingReaction = client.from("reactions")
                    .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                    .decodeSingleOrNull<JsonObject>()

                if (existingReaction != null) {
                    val existingType = existingReaction["reaction_type"]?.jsonPrimitive?.contentOrNull
                    if (existingType == reactionType.name) {
                        client.from("reactions")
                            .delete { filter { eq("post_id", postId); eq("user_id", userId) } }
                    } else {
                        client.from("reactions")
                            .update({
                                set("reaction_type", reactionType.name)
                                set("updated_at", java.time.Instant.now().toString())
                            }) { filter { eq("post_id", postId); eq("user_id", userId) } }
                    }
                } else {
                    client.from("reactions").insert(buildJsonObject {
                        put("user_id", userId)
                        put("post_id", postId)
                        put("reaction_type", reactionType.name)
                    })
                }
            }
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun getReactionSummary(postId: String): Result<Map<ReactionType, Int>> = withContext(Dispatchers.IO) {
        try {
            val summary = retryPolicy.executeWithRetry {
                val reactions = client.from("reactions")
                    .select { filter { eq("post_id", postId) } }
                    .decodeList<JsonObject>()

                reactions
                    .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                    .mapValues { it.value.size }
            }
            Result.Success(summary)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun getUsersWhoReacted(
        postId: String,
        reactionType: ReactionType? = null
    ): Result<List<UserReaction>> = withContext(Dispatchers.IO) {
        try {
            val userReactions = retryPolicy.executeWithRetry {
                val reactions = client.from("reactions")
                    .select {
                        filter {
                            eq("post_id", postId)
                            if (reactionType != null) eq("reaction_type", reactionType.name)
                        }
                    }
                    .decodeList<JsonObject>()

                if (reactions.isEmpty()) return@executeWithRetry emptyList()

                val userIds = reactions.mapNotNull { it["user_id"]?.jsonPrimitive?.contentOrNull }
                if (userIds.isEmpty()) return@executeWithRetry emptyList()

                val users = client.from("users")
                    .select { filter { isIn("uid", userIds) } }
                    .decodeList<JsonObject>()
                    .associateBy { it["uid"]?.jsonPrimitive?.contentOrNull }

                reactions.mapNotNull { reaction ->
                    val userId = reaction["user_id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                    val user = users[userId]
                    UserReaction(
                        userId = userId,
                        username = user?.get("username")?.jsonPrimitive?.contentOrNull ?: "Unknown",
                        profileImage = user?.get("avatar")?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) },
                        isVerified = user?.get("verify")?.jsonPrimitive?.booleanOrNull ?: false,
                        reactionType = reaction["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE",
                        reactedAt = reaction["created_at"]?.jsonPrimitive?.contentOrNull
                    )
                }
            }
            Result.Success(userReactions)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
        }
    }

    suspend fun getUserReaction(postId: String, userId: String): Result<ReactionType?> = withContext(Dispatchers.IO) {
        try {
            val reactionType = retryPolicy.executeWithRetry {
                val reaction = client.from("reactions")
                    .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                    .decodeSingleOrNull<JsonObject>()

                reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull?.let { ReactionType.fromString(it) }
            }
            Result.Success(reactionType)
        } catch (e: Exception) {
            Result.Error(e, ErrorHandler.getErrorMessage(e, SynapseApplication.applicationContext()))
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
