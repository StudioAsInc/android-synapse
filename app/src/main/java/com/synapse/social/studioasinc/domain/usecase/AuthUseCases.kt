package com.synapse.social.studioasinc.domain.usecase

import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<String> {
        return authRepository.signIn(email, password)
    }
}

class SignUpUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(
        email: String, 
        password: String, 
        username: String
    ): Result<String> {
        return try {
            val authResult = authRepository.signUp(email, password)
            if (authResult.isSuccess) {
                val userId = authResult.getOrThrow()
                val user = User(
                    uid = userId,
                    email = email,
                    username = username
                )
                userRepository.createUser(user)
                authResult
            } else {
                authResult
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

class SignOutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return authRepository.signOut()
    }
}

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository
) {
    suspend operator fun invoke(): Result<User?> {
        val userId = authRepository.getCurrentUserId()
        return if (userId != null) {
            userRepository.getUserById(userId)
        } else {
            Result.success(null)
        }
    }
}

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<Boolean> {
        return authRepository.observeAuthState()
    }
}