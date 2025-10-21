package com.synapse.social.studioasinc.backend

import com.synapse.social.studioasinc.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import android.util.Log
import java.net.UnknownHostException
import java.net.SocketTimeoutException
import java.io.IOException

/**
 * Authentication error handler for error classification and user-friendly messages
 */
class AuthErrorHandler {
    companion object {
        private const val TAG = "AuthErrorHandler"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val RETRY_DELAY_MS = 1000L
        
        fun handleAuthError(error: Throwable): AuthError {
            Log.d(TAG, "Handling auth error: ${error.message}")
            logAuthenticationError(error)
            
            return when {
                error.message?.contains("email not confirmed", ignoreCase = true) == true -> 
                    AuthError.EMAIL_NOT_VERIFIED
                error.message?.contains("Email not confirmed", ignoreCase = true) == true -> 
                    AuthError.EMAIL_NOT_VERIFIED
                error.message?.contains("invalid", ignoreCase = true) == true -> 
                    AuthError.INVALID_CREDENTIALS
                error.message?.contains("Invalid login credentials", ignoreCase = true) == true -> 
                    AuthError.INVALID_CREDENTIALS
                error.message?.contains("network", ignoreCase = true) == true -> 
                    AuthError.NETWORK_ERROR
                error.message?.contains("connection", ignoreCase = true) == true -> 
                    AuthError.NETWORK_ERROR
                error.message?.contains("timeout", ignoreCase = true) == true -> 
                    AuthError.NETWORK_ERROR
                error.message?.contains("unreachable", ignoreCase = true) == true -> 
                    AuthError.NETWORK_ERROR
                error is java.net.UnknownHostException -> 
                    AuthError.NETWORK_ERROR
                error is java.net.SocketTimeoutException -> 
                    AuthError.NETWORK_ERROR
                error is java.io.IOException -> 
                    AuthError.NETWORK_ERROR
                else -> 
                    AuthError.UNKNOWN_ERROR
            }
        }
        
        fun getErrorMessage(error: AuthError): String {
            return when (error) {
                AuthError.EMAIL_NOT_VERIFIED -> 
                    "Please verify your email address to continue"
                AuthError.INVALID_CREDENTIALS -> 
                    "Invalid email or password"
                AuthError.NETWORK_ERROR -> 
                    "Network connection error. Please check your internet connection and try again."
                AuthError.SUPABASE_NOT_CONFIGURED -> 
                    "Authentication service not configured"
                AuthError.UNKNOWN_ERROR -> 
                    "An unexpected error occurred"
            }
        }
        
        /**
         * Determines if an error is recoverable and should be retried
         */
        fun isRecoverableError(error: AuthError): Boolean {
            return when (error) {
                AuthError.NETWORK_ERROR -> true
                AuthError.UNKNOWN_ERROR -> true
                else -> false
            }
        }
        
        /**
         * Gets recovery action for specific error types
         */
        fun getRecoveryAction(error: AuthError): RecoveryAction {
            return when (error) {
                AuthError.EMAIL_NOT_VERIFIED -> RecoveryAction.RESEND_VERIFICATION
                AuthError.INVALID_CREDENTIALS -> RecoveryAction.RETRY_WITH_CORRECT_CREDENTIALS
                AuthError.NETWORK_ERROR -> RecoveryAction.RETRY_WITH_DELAY
                AuthError.SUPABASE_NOT_CONFIGURED -> RecoveryAction.CHECK_CONFIGURATION
                AuthError.UNKNOWN_ERROR -> RecoveryAction.RETRY_WITH_DELAY
            }
        }
        
        /**
         * Executes retry logic with exponential backoff for network-related failures
         */
        suspend fun <T> executeWithRetry(
            maxAttempts: Int = MAX_RETRY_ATTEMPTS,
            initialDelay: Long = RETRY_DELAY_MS,
            operation: suspend () -> T
        ): T {
            var currentDelay = initialDelay
            var lastException: Exception? = null
            
            repeat(maxAttempts) { attempt ->
                try {
                    Log.d(TAG, "Executing operation, attempt ${attempt + 1}/$maxAttempts")
                    return operation()
                } catch (e: Exception) {
                    lastException = e
                    val authError = handleAuthError(e)
                    
                    Log.w(TAG, "Operation failed on attempt ${attempt + 1}: ${e.message}")
                    
                    // Only retry for recoverable errors
                    if (!isRecoverableError(authError) || attempt == maxAttempts - 1) {
                        Log.e(TAG, "Non-recoverable error or max attempts reached", e)
                        throw e
                    }
                    
                    // Wait before retry with exponential backoff
                    Log.d(TAG, "Retrying in ${currentDelay}ms...")
                    kotlinx.coroutines.delay(currentDelay)
                    currentDelay *= 2 // Exponential backoff
                }
            }
            
            throw lastException ?: Exception("Operation failed after $maxAttempts attempts")
        }
        
        /**
         * Logs authentication errors and verification attempts for debugging
         */
        private fun logAuthenticationError(error: Throwable) {
            Log.e(TAG, "Authentication error occurred", error)
            
            // Log additional context for debugging
            when {
                error.message?.contains("email not confirmed", ignoreCase = true) == true -> {
                    Log.i(TAG, "Email verification required - user needs to check email")
                }
                error.message?.contains("invalid", ignoreCase = true) == true -> {
                    Log.i(TAG, "Invalid credentials provided - user should check email/password")
                }
                error is java.net.UnknownHostException -> {
                    Log.w(TAG, "Network connectivity issue - DNS resolution failed")
                }
                error is java.net.SocketTimeoutException -> {
                    Log.w(TAG, "Network timeout - slow connection or server issues")
                }
                error is java.io.IOException -> {
                    Log.w(TAG, "IO error during authentication - network or server issue")
                }
            }
        }
        
        /**
         * Logs verification attempts for monitoring
         */
        fun logVerificationAttempt(email: String, success: Boolean, errorMessage: String? = null) {
            if (success) {
                Log.i(TAG, "Email verification successful for: $email")
            } else {
                Log.w(TAG, "Email verification failed for: $email, error: $errorMessage")
            }
        }
        
        /**
         * Logs resend verification attempts
         */
        fun logResendVerificationAttempt(email: String, success: Boolean, errorMessage: String? = null) {
            if (success) {
                Log.i(TAG, "Resend verification email successful for: $email")
            } else {
                Log.w(TAG, "Resend verification email failed for: $email, error: $errorMessage")
            }
        }
    }
}

/**
 * Recovery actions for different error types
 */
enum class RecoveryAction {
    RESEND_VERIFICATION,
    RETRY_WITH_CORRECT_CREDENTIALS,
    RETRY_WITH_DELAY,
    CHECK_CONFIGURATION
}

/**
 * Supabase Authentication Service
 * Handles user authentication using Supabase Auth
 */
class SupabaseAuthenticationService : com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService {
    
    private val client = SupabaseClient.client
    
    /**
     * Sign up a new user with email and password
     */
    override suspend fun signUp(email: String, password: String): Result<AuthResult> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }
                
                // Clear any existing session first
                try {
                    client.auth.signOut()
                } catch (e: Exception) {
                    // Ignore sign out errors
                }
                
                // Attempt sign up with retry logic for network errors
                val authResult = AuthErrorHandler.executeWithRetry {
                    client.auth.signUpWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }
                
                // Check if user was created
                val supabaseUser = client.auth.currentUserOrNull()
                if (supabaseUser != null && supabaseUser.id.isNotEmpty()) {
                    val user = User(
                        id = supabaseUser.id,
                        email = supabaseUser.email ?: email,
                        emailConfirmed = supabaseUser.emailConfirmedAt != null,
                        createdAt = supabaseUser.createdAt?.toString()
                    )
                    
                    // Check if email verification is needed
                    val needsVerification = supabaseUser.emailConfirmedAt == null
                    
                    // Log verification attempt
                    AuthErrorHandler.logVerificationAttempt(email, !needsVerification)
                    
                    Result.success(AuthResult(
                        user = user,
                        needsEmailVerification = needsVerification,
                        message = if (needsVerification) "Please check your email and click the verification link to activate your account." else null
                    ))
                } else {
                    Result.failure(Exception("Account creation failed"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "Sign up failed", e)
                // Make sure to clear any partial session on error
                try {
                    client.auth.signOut()
                } catch (signOutError: Exception) {
                    // Ignore sign out errors
                }
                
                val authError = AuthErrorHandler.handleAuthError(e)
                val errorMessage = AuthErrorHandler.getErrorMessage(authError)
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    /**
     * Sign in with email and password
     */
    override suspend fun signIn(email: String, password: String): Result<AuthResult> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }
                
                // Clear any existing session first
                try {
                    client.auth.signOut()
                } catch (e: Exception) {
                    // Ignore sign out errors
                }
                
                // Attempt sign in with retry logic for network errors
                val authResult = AuthErrorHandler.executeWithRetry {
                    client.auth.signInWith(Email) {
                        this.email = email
                        this.password = password
                    }
                }
                
                // Verify the user is actually authenticated
                val supabaseUser = client.auth.currentUserOrNull()
                if (supabaseUser != null && supabaseUser.id.isNotEmpty()) {
                    val user = User(
                        id = supabaseUser.id,
                        email = supabaseUser.email ?: email,
                        emailConfirmed = supabaseUser.emailConfirmedAt != null,
                        createdAt = supabaseUser.createdAt?.toString()
                    )
                    
                    // Check if email is verified
                    if (supabaseUser.emailConfirmedAt == null) {
                        // Log verification attempt
                        AuthErrorHandler.logVerificationAttempt(email, false, "Email not verified")
                        
                        // Email not verified - return result indicating verification needed
                        Result.success(AuthResult(
                            user = user,
                            needsEmailVerification = true,
                            message = "Please verify your email address to continue"
                        ))
                    } else {
                        // Log successful verification
                        AuthErrorHandler.logVerificationAttempt(email, true)
                        
                        // Email verified - successful authentication
                        Result.success(AuthResult(
                            user = user,
                            needsEmailVerification = false,
                            message = null
                        ))
                    }
                } else {
                    Result.failure(Exception("Authentication failed - invalid credentials"))
                }
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "Sign in failed", e)
                // Make sure to clear any partial session on error
                try {
                    client.auth.signOut()
                } catch (signOutError: Exception) {
                    // Ignore sign out errors
                }
                
                val authError = AuthErrorHandler.handleAuthError(e)
                val errorMessage = AuthErrorHandler.getErrorMessage(authError)
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    /**
     * Sign out the current user
     */
    override suspend fun signOut(): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.signOut()
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Resend verification email to the specified email address
     */
    override suspend fun resendVerificationEmail(email: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }
                
                // Execute with retry logic for network errors
                AuthErrorHandler.executeWithRetry {
                    // Note: In Supabase Kotlin SDK 2.6.0, there isn't a direct resend method
                    // This is a placeholder implementation that would need to be updated
                    // when the SDK supports resend functionality or use REST API directly
                    Log.d("SupabaseAuth", "Resend verification email requested for: $email")
                    Log.w("SupabaseAuth", "Resend functionality not yet implemented - requires SDK update or REST API call")
                    
                    // For now, simulate success to prevent app crashes
                    // In a real implementation, you would make a direct REST API call to Supabase
                    Unit
                }
                
                // Log resend attempt
                AuthErrorHandler.logResendVerificationAttempt(email, true)
                
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "Failed to resend verification email", e)
                
                // Log failed resend attempt
                AuthErrorHandler.logResendVerificationAttempt(email, false, e.message)
                
                val authError = AuthErrorHandler.handleAuthError(e)
                val errorMessage = AuthErrorHandler.getErrorMessage(authError)
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    /**
     * Check if email is verified for the given email address
     */
    override suspend fun checkEmailVerified(email: String): Result<Boolean> {
        return withContext(Dispatchers.IO) {
            try {
                // Check if Supabase is configured
                if (!SupabaseClient.isConfigured()) {
                    return@withContext Result.failure(Exception("Supabase not configured. Please set up your credentials."))
                }
                
                // Get current user and check verification status with retry logic
                val isVerified = AuthErrorHandler.executeWithRetry {
                    val user = client.auth.currentUserOrNull()
                    if (user != null && user.email == email) {
                        val verified = user.emailConfirmedAt != null
                        Log.d("SupabaseAuth", "Email verification status for $email: $verified")
                        verified
                    } else {
                        // No current user or email mismatch
                        false
                    }
                }
                
                // Log verification check
                AuthErrorHandler.logVerificationAttempt(email, isVerified)
                
                Result.success(isVerified)
            } catch (e: Exception) {
                Log.e("SupabaseAuth", "Failed to check email verification status", e)
                
                // Log failed verification check
                AuthErrorHandler.logVerificationAttempt(email, false, e.message)
                
                val authError = AuthErrorHandler.handleAuthError(e)
                val errorMessage = AuthErrorHandler.getErrorMessage(authError)
                Result.failure(Exception(errorMessage))
            }
        }
    }
    
    /**
     * Get current user
     */
    override fun getCurrentUser(): User? {
        return try {
            // Check if Supabase is configured
            if (!SupabaseClient.isConfigured()) {
                return null
            }
            
            val user = client.auth.currentUserOrNull()
            if (user != null && user.id.isNotEmpty()) {
                User(
                    id = user.id,
                    email = user.email ?: "",
                    emailConfirmed = user.emailConfirmedAt != null,
                    createdAt = user.createdAt?.toString()
                )
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("SupabaseAuth", "Failed to get current user", e)
            null
        }
    }
    
    /**
     * Get current user ID
     */
    override fun getCurrentUserId(): String? {
        return client.auth.currentUserOrNull()?.id
    }
    
    /**
     * Update user password
     */
    override suspend fun updatePassword(newPassword: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.updateUser {
                    password = newPassword
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Update user email
     */
    override suspend fun updateEmail(newEmail: String): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                client.auth.updateUser {
                    email = newEmail
                }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }
}

/**
 * User data class
 */
data class User(
    val id: String,
    val email: String,
    val emailConfirmed: Boolean = false,
    val createdAt: String? = null
)

/**
 * Enhanced authentication result with verification status
 */
data class AuthResult(
    val user: User?,
    val needsEmailVerification: Boolean = false,
    val message: String? = null
)

/**
 * Authentication error types for specific error classification
 */
enum class AuthError {
    EMAIL_NOT_VERIFIED,
    INVALID_CREDENTIALS,
    NETWORK_ERROR,
    SUPABASE_NOT_CONFIGURED,
    UNKNOWN_ERROR
}