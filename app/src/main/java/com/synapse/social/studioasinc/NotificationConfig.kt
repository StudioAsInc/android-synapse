package com.synapse.social.studioasinc

/**
 * Configuration class for notification system settings.
 * 
 * This class centralizes all notification-related configuration to make it easy
 * to switch between different notification systems and manage credentials.
 */
object NotificationConfig {
    
    // ===== NOTIFICATION SYSTEM TOGGLE =====
    // Set to true to use client-side OneSignal notifications
    // Set to false to use server-side Cloudflare Worker notifications
    const val USE_CLIENT_SIDE_NOTIFICATIONS = true
    
    // ===== ONESIGNAL CONFIGURATION =====
    // IMPORTANT: Replace these placeholder values with your actual OneSignal credentials
    const val ONESIGNAL_APP_ID = "044e1911-6911-4871-95f9-d60003002fe2"
    const val ONESIGNAL_REST_API_KEY = "os_v2_app_arhbseljcfehdfpz2yaagabp4kjpeii7jqvuervkkbv3awuu5eunc7kekglutiqlwdwatnswfqvti7repqjoephn7scdvu6bwgtrlsy"
    
    // ===== NOTIFICATION SETTINGS =====
    const val NOTIFICATION_TITLE = "New Message"
    const val NOTIFICATION_SUBTITLE = "Synapse Social"
    const val NOTIFICATION_CHANNEL_ID = "messages"
    const val NOTIFICATION_PRIORITY = 10 // High priority

    // ===== NOTIFICATION TYPES =====
    const val NOTIFICATION_TYPE_NEW_POST = "NEW_POST"
    const val NOTIFICATION_TYPE_NEW_COMMENT = "NEW_COMMENT"
    const val NOTIFICATION_TYPE_NEW_REPLY = "NEW_REPLY"
    const val NOTIFICATION_TYPE_NEW_LIKE_POST = "NEW_LIKE_POST"
    const val NOTIFICATION_TYPE_NEW_LIKE_COMMENT = "NEW_LIKE_COMMENT"
    const val NOTIFICATION_TYPE_NEW_FOLLOWER = "new_follower"
    const val NOTIFICATION_TYPE_PROFILE_LIKE = "profile_like"
    const val NOTIFICATION_TYPE_MENTION_POST = "mention_post"

    // ===== NOTIFICATION TITLES =====
    const val NOTIFICATION_TITLE_NEW_POST = "New Post"
    const val NOTIFICATION_TITLE_NEW_COMMENT = "New Comment"
    const val NOTIFICATION_TITLE_NEW_REPLY = "New Reply"
    const val NOTIFICATION_TITLE_NEW_LIKE_POST = "New Like"
    const val NOTIFICATION_TITLE_NEW_LIKE_COMMENT = "New Like"
    const val NOTIFICATION_TITLE_NEW_FOLLOWER = "New Follower"
    const val NOTIFICATION_TITLE_PROFILE_LIKE = "Profile Liked"
    const val NOTIFICATION_TITLE_MENTION_POST = "You were mentioned"
    
    // ===== SERVER-SIDE CONFIGURATION =====
    const val WORKER_URL = "https://my-app-notification-sender.mashikahamed0.workers.dev"
    
    // ===== PRESENCE SETTINGS =====
    // Time threshold (in milliseconds) to consider a user "recently active"
    // Users who were active within this time will not receive notifications
    const val RECENT_ACTIVITY_THRESHOLD = 5 * 60 * 1000L // 5 minutes
    
    // ===== FEATURE FLAGS =====
    // Enable/disable smart notification suppression
    const val ENABLE_SMART_SUPPRESSION = true
    
    // Enable/disable fallback mechanisms
    const val ENABLE_FALLBACK_MECHANISMS = true
    
    // Enable/disable deep linking in notifications
    const val ENABLE_DEEP_LINKING = true
    
    // ===== DEBUG SETTINGS =====
    // Enable detailed logging for notification system
    const val ENABLE_DEBUG_LOGGING = true
    
    /**
     * Validates the current notification configuration.
     * @return true if configuration is valid, false otherwise
     */
    fun isConfigurationValid(): Boolean {
        return if (USE_CLIENT_SIDE_NOTIFICATIONS) {
            ONESIGNAL_APP_ID != "YOUR_ONESIGNAL_APP_ID_HERE" && 
            ONESIGNAL_REST_API_KEY != "YOUR_ONESIGNAL_REST_API_KEY_HERE"
        } else {
            true // Server-side is always considered valid
        }
    }
    
    /**
     * Gets title for a given notification type.
     * @return Title for the given notification type
     */
    fun getTitleForNotificationType(type: String): String {
        return when (type) {
            NOTIFICATION_TYPE_NEW_POST -> NOTIFICATION_TITLE_NEW_POST
            NOTIFICATION_TYPE_NEW_COMMENT -> NOTIFICATION_TITLE_NEW_COMMENT
            NOTIFICATION_TYPE_NEW_REPLY -> NOTIFICATION_TITLE_NEW_REPLY
            NOTIFICATION_TYPE_NEW_LIKE_POST -> NOTIFICATION_TITLE_NEW_LIKE_POST
            NOTIFICATION_TYPE_NEW_LIKE_COMMENT -> NOTIFICATION_TITLE_NEW_LIKE_COMMENT
            NOTIFICATION_TYPE_NEW_FOLLOWER -> NOTIFICATION_TITLE_NEW_FOLLOWER
            NOTIFICATION_TYPE_PROFILE_LIKE -> NOTIFICATION_TITLE_PROFILE_LIKE
            NOTIFICATION_TYPE_MENTION_POST -> NOTIFICATION_TITLE_MENTION_POST
            else -> NOTIFICATION_TITLE
        }
    }

    /**
     * Gets a human-readable description of the current notification system.
     * @return Description of the active notification system
     */
    fun getNotificationSystemDescription(): String {
        return if (USE_CLIENT_SIDE_NOTIFICATIONS) {
            "Client-side OneSignal REST API"
        } else {
            "Server-side Cloudflare Worker"
        }
    }
    
    /**
     * Gets configuration status for debugging purposes.
     * @return Map containing configuration status
     */
    fun getConfigurationStatus(): Map<String, Any> {
        return mapOf(
            "useClientSideNotifications" to USE_CLIENT_SIDE_NOTIFICATIONS,
            "systemDescription" to getNotificationSystemDescription(),
            "isConfigurationValid" to isConfigurationValid(),
            "enableSmartSuppression" to ENABLE_SMART_SUPPRESSION,
            "enableFallbackMechanisms" to ENABLE_FALLBACK_MECHANISMS,
            "enableDeepLinking" to ENABLE_DEEP_LINKING,
            "enableDebugLogging" to ENABLE_DEBUG_LOGGING,
            "recentActivityThreshold" to RECENT_ACTIVITY_THRESHOLD
        )
    }
}