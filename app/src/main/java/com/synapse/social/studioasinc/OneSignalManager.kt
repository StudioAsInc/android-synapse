package com.synapse.social.studioasinc

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.onesignal.OneSignal

object OneSignalManager {

    private const val TAG = "OneSignalManager"

    /**
     * Logs the user into OneSignal using their Firebase UID as the External ID.
     * This is the recommended approach for OneSignal v5+ to identify users.
     * Also stores the OneSignal Player ID in Firebase for notification targeting.
     *
     * @param userUid The Firebase UID of the user.
     */
    @JvmStatic
    fun loginUser(userUid: String) {
        if (userUid.isBlank()) {
            Log.w(TAG, "User UID is blank. Aborting OneSignal login.")
            return
        }

        try {
            // Login to OneSignal with external user ID
            OneSignal.login(userUid)
            Log.i(TAG, "OneSignal login called for user: $userUid")
            
            // Get and store the OneSignal Player ID
            updatePlayerIdInFirebase(userUid)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during OneSignal login for user: $userUid", e)
        }
    }
    
    /**
     * Updates the OneSignal Player ID in Firebase Database.
     * This ensures the notification system can target the user properly.
     *
     * @param userUid The Firebase UID of the user.
     */
    @JvmStatic
    fun updatePlayerIdInFirebase(userUid: String) {
        if (userUid.isBlank()) {
            Log.w(TAG, "User UID is blank. Cannot update Player ID.")
            return
        }
        
        try {
            val playerId = OneSignal.User.pushSubscription.id
            if (playerId?.isNotEmpty() == true) {
                val userRef = FirebaseDatabase.getInstance()
                    .getReference("skyline/users")
                    .child(userUid)
                    .child("oneSignalPlayerId")
                
                userRef.setValue(playerId)
                    .addOnSuccessListener {
                        Log.i(TAG, "Successfully updated OneSignal Player ID for user: $userUid")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to update OneSignal Player ID for user: $userUid", e)
                    }
            } else {
                Log.w(TAG, "OneSignal Player ID is null or blank for user: $userUid")
                // Retry after a delay if Player ID is not available yet
                retryPlayerIdUpdate(userUid)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting OneSignal Player ID for user: $userUid", e)
            retryPlayerIdUpdate(userUid)
        }
    }
    
    /**
     * Retries updating the Player ID after a delay.
     * This handles cases where OneSignal hasn't initialized the Player ID yet.
     */
    private fun retryPlayerIdUpdate(userUid: String, retryCount: Int = 0) {
        if (retryCount >= 3) {
            Log.e(TAG, "Max retry attempts reached for updating Player ID for user: $userUid")
            return
        }
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            Log.i(TAG, "Retrying Player ID update for user: $userUid (attempt ${retryCount + 1})")
            updatePlayerIdInFirebase(userUid)
        }, (retryCount + 1) * 2000L) // 2s, 4s, 6s delays
    }
    
    /**
     * Logs out the current user from OneSignal.
     */
    @JvmStatic
    fun logoutUser() {
        try {
            OneSignal.logout()
            Log.i(TAG, "OneSignal logout called")
        } catch (e: Exception) {
            Log.e(TAG, "Error during OneSignal logout", e)
        }
    }
    
    /**
     * Gets the current OneSignal Player ID.
     * @return The OneSignal Player ID or null if not available.
     */
    @JvmStatic
    fun getCurrentPlayerId(): String? {
        return try {
            OneSignal.User.pushSubscription.id
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current OneSignal Player ID", e)
            null
        }
    }
    
    /**
     * Checks if OneSignal is properly initialized and has a valid Player ID.
     * @return true if OneSignal is ready, false otherwise.
     */
    @JvmStatic
    fun isOneSignalReady(): Boolean {
        return try {
            val playerId = OneSignal.User.pushSubscription.id
            playerId?.isNotEmpty() == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking OneSignal ready state", e)
            false
        }
    }
}
