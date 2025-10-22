package com.synapse.social.studioasinc.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.synapse.social.studioasinc.domain.usecase.*

class AuthViewModelFactory(
    private val signInUseCase: SignInUseCase,
    private val signUpUseCase: SignUpUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(
                signInUseCase,
                signUpUseCase,
                signOutUseCase,
                getCurrentUserUseCase,
                observeAuthStateUseCase
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}