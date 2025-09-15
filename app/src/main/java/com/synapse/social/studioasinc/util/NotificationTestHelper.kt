package com.synapse.social.studioasinc.util

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.synapse.social.studioasinc.NotificationConfig
import com.synapse.social.studioasinc.NotificationHelper
import com.synapse.social.studioasinc.OneSignalManager
import java.util.*

/**
 * Utility class for testing and debugging the notification system.
 * This helps identify issues and verify that notifications work end-to-end.
 */
object NotificationTestHelper {
    
    private const val TAG = "NotificationTestHelper"
    
    /**
     * Performs a comprehensive test of the notification system.
     * 
     * @param context The application context
     * @param callback Callback with test results
     */
    fun performComprehensiveTest(context: Context, callback: (TestResult) -> Unit) {
        val result = TestResult()
        
        Log.i(TAG, "Starting comprehensive notification system test...")
        
        // Test 1: Check notification permissions
        result.hasNotificationPermission = NotificationPermissionHelper.hasNotificationPermission(context)
        Log.i(TAG, "Notification permission: ${result.hasNotificationPermission}")
        
        // Test 2: Check OneSignal configuration
        result.isOneSignalConfigured = NotificationConfig.isConfigurationValid()
        Log.i(TAG, "OneSignal configured: ${result.isOneSignalConfigured}")
        
        // Test 3: Check OneSignal initialization
        result.isOneSignalReady = OneSignalManager.isOneSignalReady()
        Log.i(TAG, "OneSignal ready: ${result.isOneSignalReady}")
        
        // Test 4: Check current user authentication
        val currentUser = FirebaseAuth.getInstance().currentUser
        result.isUserAuthenticated = currentUser != null
        Log.i(TAG, "User authenticated: ${result.isUserAuthenticated}")
        
        if (currentUser != null) {
            result.currentUserId = currentUser.uid
            
            // Test 5: Check if user has OneSignal Player ID in Firebase
            FirebaseDatabase.getInstance()
                .getReference("skyline/users")
                .child(currentUser.uid)
                .child("oneSignalPlayerId")
                .get()
                .addOnSuccessListener { snapshot ->
                    val playerId = snapshot.getValue(String::class.java)
                    result.hasPlayerIdInFirebase = !playerId.isNullOrBlank()
                    result.playerIdInFirebase = playerId
                    Log.i(TAG, "Player ID in Firebase: ${result.hasPlayerIdInFirebase} ($playerId)")
                    
                    // Test 6: Get current OneSignal Player ID
                    val currentPlayerId = OneSignalManager.getCurrentPlayerId()
                    result.currentPlayerId = currentPlayerId
                    result.hasCurrentPlayerId = !currentPlayerId.isNullOrBlank()
                    Log.i(TAG, "Current Player ID: ${result.hasCurrentPlayerId} ($currentPlayerId)")
                    
                    // Test 7: Check if Player IDs match
                    result.playerIdsMatch = playerId == currentPlayerId
                    Log.i(TAG, "Player IDs match: ${result.playerIdsMatch}")
                    
                    // Complete the test
                    result.overallSuccess = calculateOverallSuccess(result)
                    Log.i(TAG, "Overall test success: ${result.overallSuccess}")
                    
                    callback(result)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to get Player ID from Firebase", e)
                    result.hasPlayerIdInFirebase = false
                    result.error = e.message
                    result.overallSuccess = false
                    callback(result)
                }
        } else {
            result.overallSuccess = false
            result.error = "User not authenticated"
            callback(result)
        }
    }
    
    /**
     * Sends a test notification to verify the system works.
     * 
     * @param recipientUid The UID of the recipient
     * @param callback Callback with the result
     */
    fun sendTestNotification(recipientUid: String, callback: (Boolean, String?) -> Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            callback(false, "User not authenticated")
            return
        }
        
        if (recipientUid.isBlank()) {
            callback(false, "Recipient UID is blank")
            return
        }
        
        Log.i(TAG, "Sending test notification to: $recipientUid")
        
        val testMessage = "Test notification sent at ${Date()}"
        val data = mapOf("test" to "true", "timestamp" to System.currentTimeMillis().toString())
        
        try {
            NotificationHelper.sendNotification(
                recipientUid,
                currentUser.uid,
                testMessage,
                "test_notification",
                data
            )
            callback(true, "Test notification sent successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send test notification", e)
            callback(false, "Failed to send test notification: ${e.message}")
        }
    }
    
    /**
     * Fixes common notification issues automatically.
     * 
     * @param context The application context
     * @param callback Callback with fix results
     */
    fun autoFixNotificationIssues(context: Context, callback: (List<FixResult>) -> Unit) {
        val fixes = mutableListOf<FixResult>()
        val currentUser = FirebaseAuth.getInstance().currentUser
        
        if (currentUser == null) {
            fixes.add(FixResult("User Authentication", false, "User not logged in"))
            callback(fixes)
            return
        }
        
        // Fix 1: Update OneSignal Player ID in Firebase
        try {
            OneSignalManager.updatePlayerIdInFirebase(currentUser.uid)
            fixes.add(FixResult("Player ID Update", true, "Player ID update initiated"))
        } catch (e: Exception) {
            fixes.add(FixResult("Player ID Update", false, "Failed: ${e.message}"))
        }
        
        // Fix 2: Request notification permissions if needed
        if (!NotificationPermissionHelper.hasNotificationPermission(context)) {
            fixes.add(FixResult("Notification Permission", false, "Permission not granted - user action required"))
        } else {
            fixes.add(FixResult("Notification Permission", true, "Permission already granted"))
        }
        
        // Fix 3: Validate OneSignal configuration
        if (NotificationConfig.isConfigurationValid()) {
            fixes.add(FixResult("OneSignal Configuration", true, "Configuration is valid"))
        } else {
            fixes.add(FixResult("OneSignal Configuration", false, "Invalid configuration - check credentials"))
        }
        
        callback(fixes)
    }
    
    /**
     * Gets detailed system information for debugging.
     */
    fun getSystemInfo(context: Context): SystemInfo {
        return SystemInfo(
            androidVersion = android.os.Build.VERSION.RELEASE,
            apiLevel = android.os.Build.VERSION.SDK_INT,
            hasNotificationPermission = NotificationPermissionHelper.hasNotificationPermission(context),
            oneSignalAppId = NotificationConfig.ONESIGNAL_APP_ID,
            isOneSignalConfigured = NotificationConfig.isConfigurationValid(),
            isOneSignalReady = OneSignalManager.isOneSignalReady(),
            currentUserId = FirebaseAuth.getInstance().currentUser?.uid,
            currentPlayerId = OneSignalManager.getCurrentPlayerId(),
            notificationSystemDescription = NotificationConfig.getNotificationSystemDescription(),
            useClientSideNotifications = NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS,
            enableFallbackMechanisms = NotificationConfig.ENABLE_FALLBACK_MECHANISMS,
            enableDeepLinking = NotificationConfig.ENABLE_DEEP_LINKING
        )
    }
    
    private fun calculateOverallSuccess(result: TestResult): Boolean {
        return result.hasNotificationPermission &&
               result.isOneSignalConfigured &&
               result.isOneSignalReady &&
               result.isUserAuthenticated &&
               result.hasPlayerIdInFirebase &&
               result.hasCurrentPlayerId &&
               result.playerIdsMatch
    }
    
    /**
     * Data class representing test results
     */
    data class TestResult(
        var hasNotificationPermission: Boolean = false,
        var isOneSignalConfigured: Boolean = false,
        var isOneSignalReady: Boolean = false,
        var isUserAuthenticated: Boolean = false,
        var currentUserId: String? = null,
        var hasPlayerIdInFirebase: Boolean = false,
        var playerIdInFirebase: String? = null,
        var hasCurrentPlayerId: Boolean = false,
        var currentPlayerId: String? = null,
        var playerIdsMatch: Boolean = false,
        var overallSuccess: Boolean = false,
        var error: String? = null
    ) {
        fun getDetailedReport(): String {
            return buildString {
                appendLine("=== NOTIFICATION SYSTEM TEST REPORT ===")
                appendLine("Overall Success: $overallSuccess")
                appendLine()
                appendLine("Test Results:")
                appendLine("• Notification Permission: $hasNotificationPermission")
                appendLine("• OneSignal Configured: $isOneSignalConfigured")
                appendLine("• OneSignal Ready: $isOneSignalReady")
                appendLine("• User Authenticated: $isUserAuthenticated")
                appendLine("• Current User ID: $currentUserId")
                appendLine("• Has Player ID in Firebase: $hasPlayerIdInFirebase")
                appendLine("• Player ID in Firebase: $playerIdInFirebase")
                appendLine("• Has Current Player ID: $hasCurrentPlayerId")
                appendLine("• Current Player ID: $currentPlayerId")
                appendLine("• Player IDs Match: $playerIdsMatch")
                if (error != null) {
                    appendLine("• Error: $error")
                }
                appendLine("==========================================")
            }
        }
    }
    
    /**
     * Data class representing fix results
     */
    data class FixResult(
        val fixName: String,
        val success: Boolean,
        val message: String
    )
    
    /**
     * Data class representing system information
     */
    data class SystemInfo(
        val androidVersion: String,
        val apiLevel: Int,
        val hasNotificationPermission: Boolean,
        val oneSignalAppId: String,
        val isOneSignalConfigured: Boolean,
        val isOneSignalReady: Boolean,
        val currentUserId: String?,
        val currentPlayerId: String?,
        val notificationSystemDescription: String,
        val useClientSideNotifications: Boolean,
        val enableFallbackMechanisms: Boolean,
        val enableDeepLinking: Boolean
    ) {
        fun getDetailedReport(): String {
            return buildString {
                appendLine("=== NOTIFICATION SYSTEM INFO ===")
                appendLine("Android Version: $androidVersion (API $apiLevel)")
                appendLine("Notification Permission: $hasNotificationPermission")
                appendLine("OneSignal App ID: $oneSignalAppId")
                appendLine("OneSignal Configured: $isOneSignalConfigured")
                appendLine("OneSignal Ready: $isOneSignalReady")
                appendLine("Current User ID: $currentUserId")
                appendLine("Current Player ID: $currentPlayerId")
                appendLine("Notification System: $notificationSystemDescription")
                appendLine("Client-side Notifications: $useClientSideNotifications")
                appendLine("Fallback Mechanisms: $enableFallbackMechanisms")
                appendLine("Deep Linking: $enableDeepLinking")
                appendLine("================================")
            }
        }
    }
}