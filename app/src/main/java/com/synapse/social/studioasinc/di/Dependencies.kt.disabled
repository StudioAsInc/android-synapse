package com.synapse.social.studioasinc.di

import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.ChatRepository
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.domain.usecase.*

object Dependencies {
    
    // Repositories
    val authRepository by lazy { AuthRepository() }
    val userRepository by lazy { UserRepository() }
    val chatRepository by lazy { ChatRepository() }
    val postRepository by lazy { PostRepository() }
    
    // Use Cases
    val signInUseCase by lazy { SignInUseCase(authRepository) }
    val signUpUseCase by lazy { SignUpUseCase(authRepository, userRepository) }
    val signOutUseCase by lazy { SignOutUseCase(authRepository) }
    val getCurrentUserUseCase by lazy { GetCurrentUserUseCase(authRepository, userRepository) }
    val observeAuthStateUseCase by lazy { ObserveAuthStateUseCase(authRepository) }
    
    val sendMessageUseCase by lazy { SendMessageUseCase(chatRepository, authRepository) }
    val getMessagesUseCase by lazy { GetMessagesUseCase(chatRepository) }
    val observeMessagesUseCase by lazy { ObserveMessagesUseCase(chatRepository) }
    val getUserChatsUseCase by lazy { GetUserChatsUseCase(chatRepository, authRepository) }
    val deleteMessageUseCase by lazy { DeleteMessageUseCase(chatRepository, authRepository) }
    val editMessageUseCase by lazy { EditMessageUseCase(chatRepository, authRepository) }
}