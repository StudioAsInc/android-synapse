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
 */
class ProfileRepositoryImpl : ProfileRepository {
    private val client = SupabaseClient.client

    private fun constructMediaUrl(storagePath: String): String {
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) return storagePath
        return "${SupabaseClient.getUrl()}/storage/v1/object/public/post-media/$storagePath"
    }

    private fun constructAvatarUrl(storagePath: String): String {
        if (storagePath.startsWith("http://") || storagePath.startsWith("https://")) return storagePath
        return "${SupabaseClient.getUrl()}/storage/v1/object/public/user-avatars/$storagePath"
    }

    override fun getProfile(userId: String): Flow<Result<UserProfile>> = flow {
        val cacheKey = "profile_$userId"
        NetworkOptimizer.getCached<UserProfile>(cacheKey)?.let {
            emit(Result.success(it))
            return@flow
        }
        
        try {
            val response = client.from("users").select() { 
                filter { eq("uid", userId) } 
            }.decodeSingleOrNull<JsonObject>()
            
            if (response == null) {
                emit(Result.failure(Exception("Profile not found")))
                return@flow
            }
            
            val profile = UserProfile(
                id = response["uid"]?.jsonPrimitive?.contentOrNull ?: userId,
                username = response["username"]?.jsonPrimitive?.contentOrNull ?: "",
                name = response["display_name"]?.jsonPrimitive?.contentOrNull,
                bio = response["bio"]?.jsonPrimitive?.contentOrNull,
                profileImageUrl = response["avatar"]?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) },
                coverImageUrl = response["cover_image"]?.jsonPrimitive?.contentOrNull?.let { constructMediaUrl(it) },
                isVerified = response["verify"]?.jsonPrimitive?.booleanOrNull ?: false,
                isPrivate = response["is_private"]?.jsonPrimitive?.booleanOrNull ?: false,
                postCount = response["posts_count"]?.jsonPrimitive?.intOrNull ?: 0,
                followerCount = response["followers_count"]?.jsonPrimitive?.intOrNull ?: 0,
                followingCount = response["following_count"]?.jsonPrimitive?.intOrNull ?: 0,
                location = response["location"]?.jsonPrimitive?.contentOrNull,
                website = response["website"]?.jsonPrimitive?.contentOrNull,
                gender = response["gender"]?.jsonPrimitive?.contentOrNull,
                pronouns = response["pronouns"]?.jsonPrimitive?.contentOrNull
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
            columns = Columns.raw("*, users!posts_author_uid_fkey(uid, username, avatar, verify)")
        ) { 
            filter { eq("author_uid", userId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<JsonObject>()
        
        val posts = response.mapNotNull { data -> parsePost(data) }
        Result.success(posts)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getProfilePhotos(userId: String, limit: Int, offset: Int): Result<List<Any>> = try {
        val response = client.from("posts").select(
            columns = Columns.raw("id, media_items")
        ) { 
            filter { eq("author_uid", userId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<JsonObject>()
        
        val photos = response.flatMap { data ->
            val postId = data["id"]?.jsonPrimitive?.contentOrNull ?: return@flatMap emptyList()
            data["media_items"]?.takeIf { it !is JsonNull }?.jsonArray?.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val typeStr = mediaMap["type"]?.jsonPrimitive?.contentOrNull ?: "IMAGE"
                if (typeStr.equals("VIDEO", ignoreCase = true)) return@mapNotNull null
                com.synapse.social.studioasinc.ui.profile.components.MediaItem(
                    id = mediaMap["id"]?.jsonPrimitive?.contentOrNull ?: postId,
                    url = constructMediaUrl(url),
                    isVideo = false
                )
            } ?: emptyList()
        }
        Result.success(photos)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getProfileReels(userId: String, limit: Int, offset: Int): Result<List<Any>> = try {
        val response = client.from("posts").select(
            columns = Columns.raw("id, media_items")
        ) { 
            filter { eq("author_uid", userId) }
            limit(limit.toLong())
            range(offset.toLong(), (offset + limit - 1).toLong())
        }.decodeList<JsonObject>()
        
        val reels = response.flatMap { data ->
            val postId = data["id"]?.jsonPrimitive?.contentOrNull ?: return@flatMap emptyList()
            data["media_items"]?.takeIf { it !is JsonNull }?.jsonArray?.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                val typeStr = mediaMap["type"]?.jsonPrimitive?.contentOrNull ?: "IMAGE"
                if (!typeStr.equals("VIDEO", ignoreCase = true)) return@mapNotNull null
                com.synapse.social.studioasinc.ui.profile.components.MediaItem(
                    id = mediaMap["id"]?.jsonPrimitive?.contentOrNull ?: postId,
                    url = constructMediaUrl(url),
                    isVideo = true
                )
            } ?: emptyList()
        }
        Result.success(reels)
    } catch (e: Exception) {
        Result.failure(e)
    }

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
            id = data["id"]?.jsonPrimitive?.contentOrNull ?: return null,
            authorUid = data["author_uid"]?.jsonPrimitive?.contentOrNull ?: "",
            postText = data["post_text"]?.jsonPrimitive?.contentOrNull,
            postImage = data["post_image"]?.jsonPrimitive?.contentOrNull?.let { constructMediaUrl(it) },
            postType = data["post_type"]?.jsonPrimitive?.contentOrNull,
            timestamp = data["timestamp"]?.jsonPrimitive?.longOrNull ?: System.currentTimeMillis(),
            likesCount = data["likes_count"]?.jsonPrimitive?.intOrNull ?: 0,
            commentsCount = data["comments_count"]?.jsonPrimitive?.intOrNull ?: 0,
            viewsCount = data["views_count"]?.jsonPrimitive?.intOrNull ?: 0,
            hasPoll = data["has_poll"]?.jsonPrimitive?.booleanOrNull,
            pollQuestion = data["poll_question"]?.jsonPrimitive?.contentOrNull,
            pollOptions = data["poll_options"]?.jsonArray?.mapNotNull {
                val obj = it.jsonObject
                val text = obj["text"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                PollOption(text, obj["votes"]?.jsonPrimitive?.intOrNull ?: 0)
            }
        )
        
        data["users"]?.jsonObject?.let { userData ->
            post.username = userData["username"]?.jsonPrimitive?.contentOrNull
            post.avatarUrl = userData["avatar"]?.jsonPrimitive?.contentOrNull?.let { constructAvatarUrl(it) }
            post.isVerified = userData["verify"]?.jsonPrimitive?.booleanOrNull ?: false
        }
        
        data["media_items"]?.takeIf { it !is JsonNull }?.jsonArray?.let { mediaData ->
            post.mediaItems = mediaData.mapNotNull { item ->
                val mediaMap = item.jsonObject
                val url = mediaMap["url"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                MediaItem(
                    id = mediaMap["id"]?.jsonPrimitive?.contentOrNull ?: "",
                    url = constructMediaUrl(url),
                    type = if (mediaMap["type"]?.jsonPrimitive?.contentOrNull.equals("VIDEO", true)) MediaType.VIDEO else MediaType.IMAGE,
                    thumbnailUrl = mediaMap["thumbnailUrl"]?.jsonPrimitive?.contentOrNull?.let { constructMediaUrl(it) }
                )
            }.toMutableList()
        }
        
        return post
    }
}
