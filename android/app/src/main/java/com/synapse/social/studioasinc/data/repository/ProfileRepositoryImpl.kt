package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ProfileRepositoryImpl : ProfileRepository {
    private val client = SupabaseClient.client

    override fun getProfile(userId: String): Flow<Result<UserProfile>> = flow {
        try {
            val profile = client.from("profiles")
                .select() {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<UserProfile>()
            emit(Result.success(profile))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override suspend fun updateProfile(userId: String, profile: UserProfile): Result<UserProfile> = try {
        val updated = client.from("profiles")
            .update(profile) {
                filter {
                    eq("id", userId)
                }
            }
            .decodeSingle<UserProfile>()
        Result.success(updated)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun followUser(userId: String, targetUserId: String): Result<Unit> = try {
        client.from("followers").insert(
            mapOf(
                "follower_id" to userId,
                "following_id" to targetUserId
            )
        )
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun unfollowUser(userId: String, targetUserId: String): Result<Unit> = try {
        client.from("followers")
            .delete {
                filter {
                    eq("follower_id", userId)
                    eq("following_id", targetUserId)
                }
            }
        Result.success(Unit)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getFollowers(userId: String, limit: Int, offset: Int): Result<List<UserProfile>> = try {
        val followers = client.from("followers")
            .select("following_id(*)") {
                filter {
                    eq("follower_id", userId)
                }
                limit(limit)
                offset(offset)
            }
            .decodeList<Map<String, UserProfile>>()
            .mapNotNull { it["following_id"] }
        Result.success(followers)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getFollowing(userId: String, limit: Int, offset: Int): Result<List<UserProfile>> = try {
        val following = client.from("followers")
            .select("follower_id(*)") {
                filter {
                    eq("following_id", userId)
                }
                limit(limit)
                offset(offset)
            }
            .decodeList<Map<String, UserProfile>>()
            .mapNotNull { it["follower_id"] }
        Result.success(following)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getProfilePosts(userId: String, limit: Int, offset: Int): Result<List<Any>> = try {
        val posts = client.from("posts")
            .select() {
                filter {
                    eq("user_id", userId)
                }
                limit(limit)
                offset(offset)
                order("created_at", ascending = false)
            }
            .decodeList<Any>()
        Result.success(posts)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getProfilePhotos(userId: String, limit: Int, offset: Int): Result<List<Any>> = try {
        val photos = client.from("photos")
            .select() {
                filter {
                    eq("user_id", userId)
                }
                limit(limit)
                offset(offset)
                order("created_at", ascending = false)
            }
            .decodeList<Any>()
        Result.success(photos)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun getProfileReels(userId: String, limit: Int, offset: Int): Result<List<Any>> = try {
        val reels = client.from("reels")
            .select() {
                filter {
                    eq("user_id", userId)
                }
                limit(limit)
                offset(offset)
                order("created_at", ascending = false)
            }
            .decodeList<Any>()
        Result.success(reels)
    } catch (e: Exception) {
        Result.failure(e)
    }

    override suspend fun isFollowing(userId: String, targetUserId: String): Result<Boolean> = try {
        val result = client.from("followers")
            .select() {
                filter {
                    eq("follower_id", userId)
                    eq("following_id", targetUserId)
                }
            }
            .decodeList<Map<String, Any>>()
        Result.success(result.isNotEmpty())
    } catch (e: Exception) {
        Result.failure(e)
    }
}
