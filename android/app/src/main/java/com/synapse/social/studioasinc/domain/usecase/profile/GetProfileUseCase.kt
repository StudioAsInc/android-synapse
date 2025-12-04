package com.synapse.social.studioasinc.domain.usecase.profile

import com.synapse.social.studioasinc.data.model.UserProfile
import com.synapse.social.studioasinc.data.repository.ProfileRepository

class GetProfileUseCase(private val repository: ProfileRepository) {
    suspend operator fun invoke(uid: String): Result<UserProfile> {
        if (uid.isBlank()) {
            return Result.failure(IllegalArgumentException("User ID cannot be empty"))
        }
        return repository.getProfile(uid)
    }
}
