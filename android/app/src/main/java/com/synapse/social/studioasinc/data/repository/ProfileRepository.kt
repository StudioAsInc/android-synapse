package com.synapse.social.studioasinc.data.repository

import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.model.Post
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    suspend fun getProfile(uid: String): Result<UserProfile>
    suspend fun updateProfile(uid: String, profile: UserProfile): Result<Unit>
    suspend fun followUser(currentUid: String, targetUid: String): Result<Unit>
    suspend fun unfollowUser(currentUid: String, targetUid: String): Result<Unit>
    suspend fun isFollowing(currentUid: String, targetUid: String): Result<Boolean>
    suspend fun getProfileContent(uid: String): Result<List<Post>>
}
