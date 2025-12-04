package com.synapse.social.studioasinc.data.repository

import android.util.Log
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.*
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.gotrue.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*

/**
 * Repository for fetching detailed post information with author data.
 * Handles post loading, view count incrementing, and media parsing.
 * 
 * Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3
 */
class PostDetailRepository {
    
    private val client = SupabaseClient.client
    
    companion object {
        private const val TAG = "PostDetailRepository"
    }
    
    /**
     * Fetch a post with all details including author information.
     * Performs a join query to get user data along with the post.
     * 
     * @param postId The ID of the post to fetch
     * @return Result containing PostDetail or error
     * 
     * Requirements: 1.1, 1.2, 1.3, 1.4, 2.1, 2.2, 2.3
     */
    suspend fun getPostWithDetails(postId: String): Result<PostDetail> = withContext(Dispatchers.IO) {
        try {
            if (!SupabaseClient.isConfigured()) {
                return@withContext Result.failure(Exception("Supabase not configured"))
            }
            
            Log.d(TAG, "Fetching post details for: $postId")
            
            // Query post with user join - media_items is JSONB in posts table
            val response = client.from("posts")
                .select(
                    columns = Columns.raw("""
                        *,
                        users!posts_author_uid_fkey(uid, username, display_name, email, bio, avatar, followers_count, following_count, posts_count, status, account_type, verify, banned)
                    """.trimIndent())
                ) {
                    filter { eq("id", postId) }
                }
                .decodeSingleOrNull<JsonObject>()
            
            if (response == null) {
                Log.w(TAG, "Post not found: $postId")
                return@withContext Result.failure(Exception("Post not found"))
            }
            
            val post = parsePostFromJson(response)
            val author = parseUserProfileFromJson(response["users"]?.jsonObject)
                ?: return@withContext Result.failure(Exception("Author not found"))
            
            // Get current user's reaction and bookmark status
            val currentUserId = client.auth.currentUserOrNull()?.id
            var userReaction: ReactionType? = null
            var isBookmarked = false
            var hasReshared = false
            
            if (currentUserId != null) {
                // Get user's reaction
                userReaction = getUserReactionForPost(postId, currentUserId)
                
                // Check bookmark status
                isBookmarked = checkBookmarkStatus(postId, currentUserId)
                
                // Check reshare status
                hasReshared = checkReshareStatus(postId, currentUserId)
            }
            
            // Get reaction summary
            val reactionSummary = getReactionSummary(postId)
            
            // Get poll results if post has poll
            var pollResults: List<PollOptionResult>? = null
            var userPollVote: Int? = null
            if (post.hasPoll == true) {
                val pollData = getPollData(postId, currentUserId)
                pollResults = pollData.first
                userPollVote = pollData.second
            }
            
            val postDetail = PostDetail(
                post = post,
                author = author,
                reactionSummary = reactionSummary,
                userReaction = userReaction,
                isBookmarked = isBookmarked,
                hasReshared = hasReshared,
                pollResults = pollResults,
                userPollVote = userPollVote
            )
            
            Log.d(TAG, "Successfully fetched post details for: $postId")
            Result.success(postDetail)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to fetch post details: ${e.message}", e)
            Result.failure(Exception(mapSupabaseError(e)))
        }
    }

    
    /**
     * Increment the view count for a post.
     * 
     * @param postId The ID of the post
     * @return Result indicating success or failure
     */
    suspend fun incrementViewCount(postId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Fetch current view count first since RPC has type mismatch issues (text vs uuid)
            val response = client.from("posts")
                .select(columns = Columns.list("views_count")) {
                    filter { eq("id", postId) }
                }
                .decodeSingleOrNull<JsonObject>()

            val currentViews = response?.get("views_count")?.jsonPrimitive?.intOrNull ?: 0

            // Update with incremented value
            client.from("posts").update(mapOf("views_count" to currentViews + 1)) {
                filter { eq("id", postId) }
            }

            Log.d(TAG, "Incremented view count for post: $postId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to increment view count: ${e.message}", e)
            Result.success(Unit) // Don't fail the whole operation if view count fails
        }
    }
    
    /**
     * Observe post changes in real-time.
     * 
     * @param postId The ID of the post to observe
     * @return Flow emitting PostDetail updates
     */
    fun observePostChanges(postId: String): Flow<PostDetail> = flow {
        // Initial fetch
        val result = getPostWithDetails(postId)
        if (result.isSuccess) {
            emit(result.getOrThrow())
        }
        // Real-time updates would be handled via Supabase Realtime subscriptions
        // in the ViewModel layer
    }
    
    // ==================== PRIVATE HELPER METHODS ====================
    
    /**
     * Parse Post from JsonObject with embedded media_items JSONB.
     */
    private fun parsePostFromJson(data: JsonObject): Post {
        val post = Post(
            id = data["id"]?.jsonPrimitive?.contentOrNull ?: "",
            key = data["key"]?.jsonPrimitive?.contentOrNull,
            authorUid = data["author_uid"]?.jsonPrimitive?.contentOrNull ?: "",
            postText = data["post_text"]?.jsonPrimitive?.contentOrNull,
            postImage = data["post_image"]?.jsonPrimitive?.contentOrNull,
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
            // Encryption fields
            isEncrypted = data["is_encrypted"]?.jsonPrimitive?.booleanOrNull,
            nonce = data["nonce"]?.jsonPrimitive?.contentOrNull,
            encryptionKeyId = data["encryption_key_id"]?.jsonPrimitive?.contentOrNull,
            // Edit/Delete tracking
            isDeleted = data["is_deleted"]?.jsonPrimitive?.booleanOrNull,
            isEdited = data["is_edited"]?.jsonPrimitive?.booleanOrNull,
            editedAt = data["edited_at"]?.jsonPrimitive?.contentOrNull,
            deletedAt = data["deleted_at"]?.jsonPrimitive?.contentOrNull,
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
            pollAllowMultiple = data["poll_allow_multiple"]?.jsonPrimitive?.booleanOrNull,
            // Location fields
            hasLocation = data["has_location"]?.jsonPrimitive?.booleanOrNull,
            locationName = data["location_name"]?.jsonPrimitive?.contentOrNull,
            locationAddress = data["location_address"]?.jsonPrimitive?.contentOrNull,
            locationLatitude = data["location_latitude"]?.jsonPrimitive?.doubleOrNull,
            locationLongitude = data["location_longitude"]?.jsonPrimitive?.doubleOrNull,
            locationPlaceId = data["location_place_id"]?.jsonPrimitive?.contentOrNull,
            // YouTube embed
            youtubeUrl = data["youtube_url"]?.jsonPrimitive?.contentOrNull
        )
        
        // Parse media_items from JSONB column
        val mediaData = data["media_items"]?.takeIf { it !is JsonNull }?.jsonArray
        if (mediaData != null && mediaData.isNotEmpty()) {
            post.mediaItems = parseMediaItems(mediaData)
        }
        
        return post
    }
    
    /**
     * Parse media items from JSONB array.
     * Handles the media_items JSONB column parsing.
     * 
     * Requirements: 1.2
     */
    private fun parseMediaItems(mediaData: JsonArray): MutableList<MediaItem> {
        return mediaData.mapNotNull { item ->
            try {
                val mediaMap = item.jsonObject
                val url = mediaMap["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val typeStr = mediaMap["type"]?.jsonPrimitive?.contentOrNull ?: "IMAGE"
                MediaItem(
                    id = mediaMap["id"]?.jsonPrimitive?.contentOrNull ?: "",
                    url = url,
                    type = if (typeStr.equals("VIDEO", ignoreCase = true)) MediaType.VIDEO else MediaType.IMAGE,
                    thumbnailUrl = mediaMap["thumbnailUrl"]?.jsonPrimitive?.contentOrNull,
                    duration = mediaMap["duration"]?.jsonPrimitive?.longOrNull,
                    size = mediaMap["size"]?.jsonPrimitive?.longOrNull,
                    mimeType = mediaMap["mimeType"]?.jsonPrimitive?.contentOrNull
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse media item: ${e.message}")
                null
            }
        }.toMutableList()
    }
    
    /**
     * Parse UserProfile from joined user data.
     * 
     * Requirements: 2.1, 2.2, 2.3
     */
    private fun parseUserProfileFromJson(userData: JsonObject?): UserProfile? {
        if (userData == null) return null
        
        return try {
            UserProfile(
                uid = userData["uid"]?.jsonPrimitive?.contentOrNull ?: return null,
                username = userData["username"]?.jsonPrimitive?.contentOrNull ?: "",
                displayName = userData["display_name"]?.jsonPrimitive?.contentOrNull ?: "",
                email = userData["email"]?.jsonPrimitive?.contentOrNull ?: "",
                bio = userData["bio"]?.jsonPrimitive?.contentOrNull,
                profileImageUrl = userData["avatar"]?.jsonPrimitive?.contentOrNull,
                followersCount = userData["followers_count"]?.jsonPrimitive?.intOrNull ?: 0,
                followingCount = userData["following_count"]?.jsonPrimitive?.intOrNull ?: 0,
                postsCount = userData["posts_count"]?.jsonPrimitive?.intOrNull ?: 0,
                status = userData["status"]?.jsonPrimitive?.contentOrNull ?: "offline",
                account_type = userData["account_type"]?.jsonPrimitive?.contentOrNull ?: "user",
                verify = userData["verify"]?.jsonPrimitive?.booleanOrNull ?: false,
                banned = userData["banned"]?.jsonPrimitive?.booleanOrNull ?: false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse user profile: ${e.message}")
            null
        }
    }

    
    /**
     * Get user's reaction for a specific post.
     */
    private suspend fun getUserReactionForPost(postId: String, userId: String): ReactionType? {
        return try {
            val reaction = client.from("reactions")
                .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                .decodeSingleOrNull<JsonObject>()
            
            reaction?.get("reaction_type")?.jsonPrimitive?.contentOrNull?.let {
                ReactionType.fromString(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get user reaction: ${e.message}")
            null
        }
    }
    
    /**
     * Get reaction summary for a post.
     */
    private suspend fun getReactionSummary(postId: String): Map<ReactionType, Int> {
        return try {
            val reactions = client.from("reactions")
                .select { filter { eq("post_id", postId) } }
                .decodeList<JsonObject>()
            
            reactions
                .groupBy { ReactionType.fromString(it["reaction_type"]?.jsonPrimitive?.contentOrNull ?: "LIKE") }
                .mapValues { it.value.size }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get reaction summary: ${e.message}")
            emptyMap()
        }
    }
    
    /**
     * Check if user has bookmarked the post.
     */
    private suspend fun checkBookmarkStatus(postId: String, userId: String): Boolean {
        return try {
            val bookmark = client.from("favorites")
                .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                .decodeSingleOrNull<JsonObject>()
            bookmark != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check bookmark status: ${e.message}")
            false
        }
    }
    
    /**
     * Check if user has reshared the post.
     */
    private suspend fun checkReshareStatus(postId: String, userId: String): Boolean {
        return try {
            val reshare = client.from("reshares")
                .select { filter { eq("post_id", postId); eq("user_id", userId) } }
                .decodeSingleOrNull<JsonObject>()
            reshare != null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check reshare status: ${e.message}")
            false
        }
    }
    
    /**
     * Get poll data including results and user's vote.
     */
    private suspend fun getPollData(postId: String, userId: String?): Pair<List<PollOptionResult>?, Int?> {
        return try {
            // Get the post to get poll options
            val post = client.from("posts")
                .select { filter { eq("id", postId) } }
                .decodeSingleOrNull<JsonObject>()
            
            val options = post?.get("poll_options")?.jsonArray?.mapNotNull {
                it.jsonObject["text"]?.jsonPrimitive?.contentOrNull
            } ?: emptyList()
            
            if (options.isEmpty()) {
                return Pair(null, null)
            }
            
            // Get all votes for this poll
            val votes = client.from("poll_votes")
                .select { filter { eq("post_id", postId) } }
                .decodeList<JsonObject>()
            
            // Count votes per option
            val voteCounts = votes.groupBy { 
                it["option_index"]?.jsonPrimitive?.intOrNull ?: 0 
            }.mapValues { it.value.size }
            
            // Calculate results
            val pollResults = PollOptionResult.calculateResults(options, voteCounts)
            
            // Get user's vote if logged in
            val userVote = if (userId != null) {
                votes.find { it["user_id"]?.jsonPrimitive?.contentOrNull == userId }
                    ?.get("option_index")?.jsonPrimitive?.intOrNull
            } else null
            
            Pair(pollResults, userVote)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get poll data: ${e.message}")
            Pair(null, null)
        }
    }
    
    /**
     * Map Supabase errors to user-friendly messages.
     */
    private fun mapSupabaseError(exception: Exception): String {
        val message = exception.message ?: "Unknown error"
        
        Log.e(TAG, "Supabase error: $message", exception)
        
        return when {
            message.contains("PGRST200") -> "Database table not found"
            message.contains("PGRST100") -> "Database column does not exist"
            message.contains("PGRST116") -> "Post not found"
            message.contains("relation", ignoreCase = true) -> "Database table does not exist"
            message.contains("column", ignoreCase = true) -> "Database column mismatch"
            message.contains("policy", ignoreCase = true) || message.contains("rls", ignoreCase = true) -> 
                "Permission denied"
            message.contains("connection", ignoreCase = true) || message.contains("network", ignoreCase = true) -> 
                "Connection failed. Please check your internet connection."
            message.contains("timeout", ignoreCase = true) -> "Request timed out. Please try again."
            message.contains("unauthorized", ignoreCase = true) -> "Permission denied."
            else -> "Failed to load post: $message"
        }
    }
    
    // ==================== UTILITY METHODS FOR TESTING ====================
    
    /**
     * Check if a post contains a YouTube URL.
     * Used for Property 2: YouTube URL detection.
     * 
     * Requirements: 1.3
     */
    fun hasYouTubeUrl(post: Post): Boolean {
        return !post.youtubeUrl.isNullOrEmpty()
    }
    
    /**
     * Check if a post has been edited.
     * Used for Property 4: Edited post detection.
     * 
     * Requirements: 1.6
     */
    fun isPostEdited(post: Post): Boolean {
        return post.isEdited == true
    }
    
    /**
     * Get the edited timestamp for an edited post.
     * Returns null if post is not edited.
     * 
     * Requirements: 1.6
     */
    fun getEditedTimestamp(post: Post): String? {
        return if (post.isEdited == true) post.editedAt else null
    }
    
    /**
     * Determine which badge to display for an author.
     * Returns "verified" if verified, "premium" if premium account, null otherwise.
     * 
     * Requirements: 2.2, 2.3
     */
    fun getAuthorBadge(author: UserProfile): String? {
        return when {
            author.verify -> "verified"
            author.account_type == "premium" -> "premium"
            else -> null
        }
    }
    
    /**
     * Check if post data is complete with all required fields.
     * Used for Property 1: Post loading returns complete data.
     * 
     * Requirements: 1.1, 1.2, 1.4, 2.1
     */
    fun isPostDataComplete(postDetail: PostDetail): Boolean {
        val post = postDetail.post
        val author = postDetail.author
        
        // Required fields must be present
        if (post.id.isEmpty()) return false
        if (post.authorUid.isEmpty()) return false
        if (author.uid.isEmpty()) return false
        
        // If post has media, media items must be present
        if (post.postType == "IMAGE" || post.postType == "VIDEO") {
            if (post.mediaItems.isNullOrEmpty() && post.postImage.isNullOrEmpty()) {
                // Allow posts without media if they have text
                if (post.postText.isNullOrEmpty()) return false
            }
        }
        
        // If post has location flag, location data should be present
        if (post.hasLocation == true) {
            if (post.locationName.isNullOrEmpty() && post.locationAddress.isNullOrEmpty()) {
                return false
            }
        }
        
        // If post has poll flag, poll data should be present
        if (post.hasPoll == true) {
            if (post.pollQuestion.isNullOrEmpty() || post.pollOptions.isNullOrEmpty()) {
                return false
            }
        }
        
        return true
    }
}
