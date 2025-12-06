package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.SupabaseClient
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType
import com.synapse.social.studioasinc.model.PollOption
import com.synapse.social.studioasinc.ui.profile.utils.NetworkOptimizer
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*

/**
 * Implementation of ProfileRepository with Supabase backend integration.
 * 
 * Features:
 * - Request caching with 1-minute TTL
 * - Automatic retry with exponential backoff
 * - RLS policy compliance
 * 
 * Uses SupabaseClient singleton for all database operations.
 */
class ProfileRepositoryImpl : ProfileRepository {
    private val client = SupabaseClient.client

    private companion object {
        // JSON field keys
        const val KEY_UID = "uid"
        const val KEY_USERNAME = "username"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_BIO = "bio"
        const val KEY_AVATAR = "avatar"
        const val KEY_COVER_IMAGE = "cover_image"
        const val KEY_VERIFY = "verify"
        const val KEY_IS_PRIVATE = "is_private"
        const val KEY_POSTS_COUNT = "posts_count"
        const val KEY_FOLLOWERS_COUNT = "followers_count"
        const val KEY_FOLLOWING_COUNT = "following_count"
        const val KEY_LOCATION = "location"
        const val KEY_WEBSITE = "website"
        const val KEY_GENDER = "gender"
        const val KEY_PRONOUNS = "pronouns"
        const val KEY_ID = "id"
        const val KEY_AUTHOR_UID = "author_uid"
        const val KEY_POST_TEXT = "post_text"
        const val KEY_POST_IMAGE = "post_image"
        const val KEY_POST_TYPE = "post_type"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_LIKES_COUNT = "likes_count"
        const val KEY_COMMENTS_COUNT = "comments_count"
        const val KEY_VIEWS_COUNT = "views_count"
        const val KEY_HAS_POLL = "has_poll"
        const val KEY_POLL_QUESTION = "poll_question"
        const val KEY_POLL_OPTIONS = "poll_options"
        const val KEY_MEDIA_ITEMS = "media_items"
        const val KEY_USERS = "users"
        const val KEY_URL = "url"
        const val KEY_TYPE = "type"
        const val KEY_THUMBNAIL_URL = "thumbnailUrl"
        const val KEY_TEXT = "text"
        const val KEY_VOTES = "votes"
        
        // Storage buckets
        const val BUCKET_POST_MEDIA = "post-media"
        const val BUCKET_USER_AVATARS = "user-avatars"
        
        // Media types
        const val MEDIA_TYPE_VIDEO = "VIDEO"
        const val MEDIA_TYPE_IMAGE = "IMAGE"
    }

    private fun constructStorageUrl(storagePath: String, bucket: String): String {
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) return storagePath
        return "${SupabaseClient.getUrl()}/storage/v1/object/public/$bucket/$storagePath"
    }

    private fun constructMediaUrl(storagePath: String): String = constructStorageUrl(storagePath, BUCKET_POST_MEDIA)
    
    private fun constructAvatarUrl(storagePath: String): String = constructStorageUrl(storagePath, BUCKET_USER_AVATARS)

    override fun getProfile(userId: String): Flow<Result<UserProfile>> = flow {
        val cacheKey = "profile_$userId"
        NetworkOptimizer.getCached<UserProfile>(cacheKey)?.let {
            emit(Result.success(it))
            return@flow
        }
        
        try {
            val response = NetworkOptimizer.withRetry {
                client.from("users").select() { 
                    filter { eq(KEY_UID, userId) } 
                }.decodeSingleOrNull<JsonObject>()
            }
            
            if (response == null) {
                emit(Result.failure(Exception("Profile not found")))
                return@flow
            }
            
            val profile = UserProfile(
                id = response[KEY_UID]?.jsonPrimitive?.contentOrNull ?: userId,
                username = response[KEY_USERNAME]?.jsonPrimitive?.contentOrNull ?: "",
                name = response[KEY_DISPLAY_NAME]?.jsonPrimitive?.contentOrNull,
                bio = response[KEY_BIO]?.jsonPrimitive?.contentOrNull,
                profileImageUrl = response[KEY_AVATAR]?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) },
                coverImageUrl = response[KEY_COVER_IMAGE]?.jsonPrimitive?.contentOrNull?.let { constructMediaUrl(it) },
                isVerified = response[KEY_VERIFY]?.jsonPrimitive?.booleanOrNull ?: false,
                isPrivate = response[KEY_IS_PRIVATE]?.jsonPrimitive?.booleanOrNull ?: false,
                postCount = response[KEY_POSTS_COUNT]?.jsonPrimitive?.intOrNull ?: 0,
                followerCount = response[KEY_FOLLOWERS_COUNT]?.jsonPrimitive?.intOrNull ?: 0,
                followingCount = response[KEY_FOLLOWING_COUNT]?.jsonPrimitive?.intOrNull ?: 0,
                location = response[KEY_LOCATION]?.jsonPrimitive?.contentOrNull,
                website = response[KEY_WEBSITE]?.jsonPrimitive?.contentOrNull,
                gender = response[KEY_GENDER]?.jsonPrimitive?.contentOrNull,
                pronouns = response[KEY_PRONOUNS]?.jsonPrimitive?.contentOrNull
            )
            NetworkOptimizer.cache(cacheKey, profile)
            emit(Result.success(profile))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun updateProfile(userId: String, profile: UserProfile): Result<UserProfile> = try {
        val updated = client.from("users").update(profile) { filter { eq("uid", userId) } }.decodeSingle<UserProfile>()
        Result.success(updated)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun followUser(userId: String, targetUserId: String): Result<Unit> = try {
        client.from("followers").insert(mapOf("follower_id" to userId, "following_id" to targetUserId))
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun unfollowUser(userId: String, targetUserId: String): Result<Unit> = try {
        client.from("followers").delete { filter { eq("follower_id", userId); eq("following_id", targetUserId) } }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getFollowers(userId: String, limit: Int, offset: Int): Result<List<UserProfile>> = try {
        val followers = client.from("followers").select() { 
            filter { eq("follower_id", userId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<Map<String, UserProfile>>().mapNotNull { it["following_id"] }
        Result.success(followers)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getFollowing(userId: String, limit: Int, offset: Int): Result<List<UserProfile>> = try {
        val following = client.from("followers").select() { 
            filter { eq("following_id", userId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<Map<String, UserProfile>>().mapNotNull { it["follower_id"] }
        Result.success(following)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getProfilePosts(userId: String, limit: Int, offset: Int): Result<List<Any>> = try {
        val response = client.from("posts").select(
            columns = Columns.raw("*, users!posts_author_uid_fkey($KEY_UID, $KEY_USERNAME, $KEY_AVATAR, $KEY_VERIFY)")
        ) { 
            filter { eq(KEY_AUTHOR_UID, userId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<JsonObject>()
        
        val posts = response.mapNotNull { data -> parsePost(data) }
        Result.success(posts)
    } catch (e: Exception) {
        Result.failure(e)
    }

    private suspend fun getMediaItemsByType(userId: String, limit: Int, offset: Int, isVideo: Boolean): Result<List<com.synapse.social.studioasinc.ui.profile.components.MediaItem>> = try {
        val response = client.from("posts").select(
            columns = Columns.raw("$KEY_ID, $KEY_MEDIA_ITEMS")
        ) { 
            filter { eq(KEY_AUTHOR_UID, userId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<JsonObject>()
        
        val mediaItems = response.flatMap { data ->
            val postId = data[KEY_ID]?.jsonPrimitive?.contentOrNull ?: return@flatMap emptyList()
            data[KEY_MEDIA_ITEMS]?.takeIf { it !is JsonNull }?.jsonArray?.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap[KEY_URL]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val typeStr = mediaMap[KEY_TYPE]?.jsonPrimitive?.contentOrNull ?: MEDIA_TYPE_IMAGE
                val isVideoType = typeStr.equals(MEDIA_TYPE_VIDEO, ignoreCase = true)
                if (isVideoType != isVideo) return@mapNotNull null
                com.synapse.social.studioasinc.ui.profile.components.MediaItem(
                    id = mediaMap[KEY_ID]?.jsonPrimitive?.contentOrNull ?: postId,
                    url = constructMediaUrl(url),
                    isVideo = isVideo
                )
            } ?: emptyList()
        }
        Result.success(mediaItems)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getProfilePhotos(userId: String, limit: Int, offset: Int): Result<List<Any>> = 
        getMediaItemsByType(userId, limit, offset, isVideo = false)

    override suspend fun getProfileReels(userId: String, limit: Int, offset: Int): Result<List<Any>> = 
        getMediaItemsByType(userId, limit, offset, isVideo = true)

    override suspend fun isFollowing(userId: String, targetUserId: String): Result<Boolean> = try {
        val result = client.from("followers").select() { 
            filter { eq("follower_id", userId); eq("following_id", targetUserId) } 
        }.decodeList<JsonObject>()
        Result.success(result.isNotEmpty())
    } catch (e: Exception) {
        Result.failure(e)
    }

    private fun parsePost(data: JsonObject): Post? {
        val post = Post(
            id = data[KEY_ID]?.jsonPrimitive?.contentOrNull ?: return null,
            authorUid = data[KEY_AUTHOR_UID]?.jsonPrimitive?.contentOrNull ?: "",
            postText = data[KEY_POST_TEXT]?.jsonPrimitive?.contentOrNull,
            postImage = data[KEY_POST_IMAGE]?.jsonPrimitive?.contentOrNull?.let { constructMediaUrl(it) },
            postType = data[KEY_POST_TYPE]?.jsonPrimitive?.contentOrNull,
            timestamp = data[KEY_TIMESTAMP]?.jsonPrimitive?.longOrNull ?: 0L,
            likesCount = data[KEY_LIKES_COUNT]?.jsonPrimitive?.intOrNull ?: 0,
            commentsCount = data[KEY_COMMENTS_COUNT]?.jsonPrimitive?.intOrNull ?: 0,
            viewsCount = data[KEY_VIEWS_COUNT]?.jsonPrimitive?.intOrNull ?: 0,
            hasPoll = data[KEY_HAS_POLL]?.jsonPrimitive?.booleanOrNull,
            pollQuestion = data[KEY_POLL_QUESTION]?.jsonPrimitive?.contentOrNull,
            pollOptions = data[KEY_POLL_OPTIONS]?.jsonArray?.mapNotNull {
                val obj = it.jsonObject
                val text = obj[KEY_TEXT]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                PollOption(text, obj[KEY_VOTES]?.jsonPrimitive?.intOrNull ?: 0)
            }
        )
        
        data[KEY_USERS]?.jsonObject?.let { userData ->
            post.username = userData[KEY_USERNAME]?.jsonPrimitive?.contentOrNull
            post.avatarUrl = userData[KEY_AVATAR]?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) }
            post.isVerified = userData[KEY_VERIFY]?.jsonPrimitive?.booleanOrNull ?: false
        }
        
        data[KEY_MEDIA_ITEMS]?.takeIf { it !is JsonNull }?.jsonArray?.let { mediaData ->
            post.mediaItems = mediaData.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap[KEY_URL]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                MediaItem(
                    id = mediaMap[KEY_ID]?.jsonPrimitive?.contentOrNull ?: "",
                    url = constructMediaUrl(url),
                    type = if (mediaMap[KEY_TYPE]?.jsonPrimitive?.contentOrNull.equals(MEDIA_TYPE_VIDEO, true)) MediaType.VIDEO else MediaType.IMAGE,
                    thumbnailUrl = mediaMap[KEY_THUMBNAIL_URL]?.jsonPrimitive?.contentOrNull?.let { constructMediaUrl(it) }
                )
            }.toMutableList()
        }
        
        return post
    }
}
