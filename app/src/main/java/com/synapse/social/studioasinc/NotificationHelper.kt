package com.synapse.social.studioasinc

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import org.json.JSONArray
import java.io.IOException

/**
 * Enhanced notification system supporting both server-side and client-side OneSignal notifications.
 * 
 * Features:
 * - Toggle between server-side (Cloudflare Workers) and client-side (OneSignal REST API) notification sending
 * - Smart notification suppression when both users are actively chatting
 * - Fallback mechanisms for reliability
 * - Configurable notification settings
 */
object NotificationHelper {

    private const val TAG = "NotificationHelper"
    
    private val JSON = "application/json; charset=utf-8".toMediaType()
    private const val ONESIGNAL_API_URL = "https://onesignal.com/api/v1/notifications"
    
    /**
     * Sends a notification to a user.
     *
     * @param recipientUid The UID of the user to send the notification to.
     * @param senderUid The UID of the user sending the notification.
     * @param message The message to send in the notification.
     * @param notificationType The type of notification to send.
     * @param data A map of additional data to send with the notification.
     */
    @JvmStatic
    fun sendNotification(
        recipientUid: String,
        senderUid: String,
        message: String,
        notificationType: String,
        data: Map<String, String>? = null
    ) {
        if (recipientUid == senderUid) {
            // Don't send notification to self
            Log.d(TAG, "Skipping notification to self: $recipientUid")
            return
        }

        if (recipientUid.isBlank() || senderUid.isBlank() || message.isBlank()) {
            Log.w(TAG, "Invalid notification parameters: recipientUid=$recipientUid, senderUid=$senderUid, message=$message")
            return
        }

        Log.d(TAG, "Attempting to send notification to $recipientUid from $senderUid")

        val userDb = FirebaseDatabase.getInstance().getReference("skyline/users")
        
        // First, try to get the Player ID with retry mechanism
        getPlayerIdWithRetry(userDb, recipientUid, 0) { playerId ->
            if (playerId.isNullOrBlank()) {
                Log.e(TAG, "Failed to get valid Player ID for recipient: $recipientUid after retries")
                // Still save to database for later processing
                saveNotificationToDatabase(recipientUid, senderUid, message, notificationType, data)
                return@getPlayerIdWithRetry
            }

            Log.d(TAG, "Got Player ID for $recipientUid: $playerId")

            // Check recipient status (optional - can proceed without this)
            val recipientStatusRef = FirebaseDatabase.getInstance().getReference("/skyline/users/$recipientUid/status")

            recipientStatusRef.get().addOnSuccessListener { dataSnapshot ->
                // Smart suppression logic removed as per user request.
                sendNotificationWithPlayerId(playerId, message, senderUid, notificationType, data)
                saveNotificationToDatabase(recipientUid, senderUid, message, notificationType, data)
            }.addOnFailureListener { e ->
                Log.e(TAG, "Status check failed. Defaulting to send notification.", e)
                // Still send notification even if status check fails
                sendNotificationWithPlayerId(playerId, message, senderUid, notificationType, data)
                saveNotificationToDatabase(recipientUid, senderUid, message, notificationType, data)
            }
        }
    }

    /**
     * Gets Player ID with retry mechanism to handle cases where it's not immediately available.
     */
    private fun getPlayerIdWithRetry(
        userDb: com.google.firebase.database.DatabaseReference,
        recipientUid: String,
        retryCount: Int,
        callback: (String?) -> Unit
    ) {
        userDb.child(recipientUid).child("oneSignalPlayerId").get()
            .addOnSuccessListener { snapshot ->
                val playerId = snapshot.getValue(String::class.java)
                if (!playerId.isNullOrBlank()) {
                    callback(playerId)
                } else if (retryCount < 2) {
                    Log.w(TAG, "Player ID is blank for $recipientUid, retrying... (attempt ${retryCount + 1})")
                    // Retry after delay
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        getPlayerIdWithRetry(userDb, recipientUid, retryCount + 1, callback)
                    }, (retryCount + 1) * 1500L)
                } else {
                    Log.e(TAG, "Player ID is still blank after retries for $recipientUid")
                    callback(null)
                }
            }
            .addOnFailureListener { e ->
                if (retryCount < 2) {
                    Log.w(TAG, "Failed to get Player ID for $recipientUid, retrying... (attempt ${retryCount + 1})", e)
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        getPlayerIdWithRetry(userDb, recipientUid, retryCount + 1, callback)
                    }, (retryCount + 1) * 1500L)
                } else {
                    Log.e(TAG, "Failed to get Player ID after retries for $recipientUid", e)
                    callback(null)
                }
            }
    }

    /**
     * Sends notification using the provided Player ID.
     */
    private fun sendNotificationWithPlayerId(
        playerId: String,
        message: String,
        senderUid: String?,
        notificationType: String,
        data: Map<String, String>?
    ) {
        if (NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS) {
            sendClientSideNotification(playerId, message, senderUid, notificationType, data)
        } else {
            sendServerSideNotification(playerId, message, notificationType, data)
        }
    }

    /**
     * Enhanced notification sending with smart presence checking and dual system support.
     *
     * @param senderUid The UID of the message sender
     * @param recipientUid The UID of the message recipient
     * @param recipientOneSignalPlayerId The OneSignal Player ID of the recipient
     * @param message The message content to send in the notification
     * @param chatId Optional chat ID for deep linking (can be null)
     * @deprecated Use sendNotification instead.
     */
    @JvmStatic
    @Deprecated("Use sendNotification instead.")
    fun sendMessageAndNotifyIfNeeded(
        senderUid: String,
        recipientUid: String,
        recipientOneSignalPlayerId: String,
        message: String,
        chatId: String? = null
    ) {
        sendNotification(
            recipientUid,
            senderUid,
            message,
            "chat_message",
            if (chatId != null) mapOf("chat_id" to chatId) else null
        )
    }

    /**
     * Sends notification via the existing Cloudflare Worker (server-side).
     */
    @JvmStatic
    fun sendServerSideNotification(
        recipientId: String,
        message: String,
        notificationType: String,
        data: Map<String, String>? = null
    ) {
        if (notificationType != "chat_message") {
            Log.w(TAG, "Server-side notification for type $notificationType is not yet implemented. Sending a generic message.")
        }

        val client = OkHttpClient()
        val jsonBody = JSONObject()
        try {
            jsonBody.put("recipientUserId", recipientId)
            jsonBody.put("notificationMessage", message)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create JSON for server-side notification", e)
            return
        }

        val body = jsonBody.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url(NotificationConfig.WORKER_URL)
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to send server-side notification", e)
                if (NotificationConfig.ENABLE_FALLBACK_MECHANISMS && !NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS) {
                    Log.i(TAG, "Falling back to client-side notification due to server failure")
                    sendClientSideNotification(recipientId, message, null, notificationType, data)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (it.isSuccessful) {
                        Log.i(TAG, "Server-side notification sent successfully.")
                    } else {
                        Log.e(TAG, "Failed to send server-side notification: ${it.code}")
                        if (NotificationConfig.ENABLE_FALLBACK_MECHANISMS && !NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS) {
                            Log.i(TAG, "Falling back to client-side notification due to server error")
                            sendClientSideNotification(recipientId, message, null, notificationType, data)
                        }
                    }
                }
            }
        })
    }

    /**
     * Sends notification directly via OneSignal REST API (client-side).
     */
    @JvmStatic
    fun sendClientSideNotification(
        recipientPlayerId: String,
        message: String,
        senderUid: String? = null,
        notificationType: String,
        data: Map<String, String>? = null
    ) {
        if (recipientPlayerId.isBlank()) {
            Log.e(TAG, "Cannot send client-side notification: Player ID is blank")
            return
        }

        if (!NotificationConfig.isConfigurationValid()) {
            Log.e(TAG, "OneSignal configuration is invalid. Cannot send client-side notification.")
            return
        }

        Log.d(TAG, "Sending client-side notification to Player ID: $recipientPlayerId")
        Log.d(TAG, "Notification type: $notificationType, Message: $message")

        val client = OkHttpClient.Builder()
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
        
        val jsonBody = JSONObject()
        
        try {
            jsonBody.put("app_id", NotificationConfig.ONESIGNAL_APP_ID)
            // OneSignal expects include_player_ids to be a JSON array of player IDs
            jsonBody.put("include_player_ids", JSONArray().put(recipientPlayerId))
            jsonBody.put("contents", JSONObject().put("en", message))
            jsonBody.put("headings", JSONObject().put("en", NotificationConfig.getTitleForNotificationType(notificationType)))
            
            // Add subtitle only if it's not empty
            val subtitle = NotificationConfig.NOTIFICATION_SUBTITLE
            if (subtitle.isNotBlank()) {
                jsonBody.put("subtitle", JSONObject().put("en", subtitle))
            }
            
            // Add custom data for deep linking and notification handling
            if (NotificationConfig.ENABLE_DEEP_LINKING) {
                val dataJson = JSONObject()
                if (senderUid != null) {
                    dataJson.put("sender_uid", senderUid)
                }
                dataJson.put("type", notificationType)
                data?.forEach { (key, value) ->
                    dataJson.put(key, value)
                }
                jsonBody.put("data", dataJson)
            }
            
            // Set notification priority
            jsonBody.put("priority", NotificationConfig.NOTIFICATION_PRIORITY)
            
            // Add Android-specific settings
            val androidSettings = JSONObject()
            androidSettings.put("sound", "default")
            androidSettings.put("accent_color", "FF0000FF") // Blue accent
            
            // Only include android_channel_id if it looks like a valid OneSignal channel ID (UUID)
            val channelId = NotificationConfig.NOTIFICATION_CHANNEL_ID
            val looksLikeUuid = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
            if (looksLikeUuid.matches(channelId)) {
                androidSettings.put("channel_for_external_user_ids", channelId)
            } else {
                // Use the standard Android channel ID for messages
                androidSettings.put("existing_android_channel_id", "messages")
            }
            
            jsonBody.put("android_accent_color", "FF0000FF")
            jsonBody.put("android_sound", "default")
            jsonBody.put("android_visibility", 1) // Public visibility
            
            Log.d(TAG, "OneSignal payload: ${jsonBody.toString()}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create JSON for client-side notification", e)
            return
        }

        val body = jsonBody.toString().toRequestBody(JSON)
        val request = Request.Builder()
            .url(ONESIGNAL_API_URL)
            .addHeader("Authorization", "Basic ${NotificationConfig.ONESIGNAL_REST_API_KEY}")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Network failure sending client-side notification to $recipientPlayerId", e)
                if (NotificationConfig.ENABLE_FALLBACK_MECHANISMS && NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS) {
                    Log.i(TAG, "Falling back to server-side notification due to client-side network failure")
                    sendServerSideNotification(recipientPlayerId, message, notificationType, data)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    val responseBody = it.body?.string() ?: ""
                    if (it.isSuccessful) {
                        Log.i(TAG, "Client-side notification sent successfully to $recipientPlayerId")
                        Log.d(TAG, "OneSignal response: $responseBody")
                        
                        // Parse response to check if notification was actually sent
                        try {
                            val responseJson = JSONObject(responseBody)
                            val recipients = responseJson.optInt("recipients", 0)
                            if (recipients == 0) {
                                Log.w(TAG, "Notification sent but no recipients reached. Player ID might be invalid.")
                            } else {
                                Log.i(TAG, "Notification delivered to $recipients recipient(s)")
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Could not parse OneSignal response JSON: $e")
                        }
                    } else {
                        Log.e(TAG, "Failed to send client-side notification: ${it.code}")
                        Log.e(TAG, "Response body: $responseBody")
                        
                        // Try to extract error details
                        try {
                            val errorJson = JSONObject(responseBody)
                            val errors = errorJson.optJSONArray("errors")
                            if (errors != null) {
                                for (i in 0 until errors.length()) {
                                    Log.e(TAG, "OneSignal error: ${errors.getString(i)}")
                                }
                            }
                        } catch (e: Exception) {
                            Log.d(TAG, "Could not parse error response: $e")
                        }
                        
                        if (NotificationConfig.ENABLE_FALLBACK_MECHANISMS && NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS) {
                            Log.i(TAG, "Falling back to server-side notification due to client-side error")
                            sendServerSideNotification(recipientPlayerId, message, notificationType, data)
                        }
                    }
                }
            }
        })
    }

    /**
     * Legacy method for backward compatibility.
     * @deprecated Use sendMessageAndNotifyIfNeeded with chatId parameter instead
     */
    @JvmStatic
    @Deprecated("Use sendMessageAndNotifyIfNeeded with chatId parameter for better deep linking")
    fun triggerPushNotification(recipientId: String, message: String) {
        @Suppress("DEPRECATION")
        sendMessageAndNotifyIfNeeded("", "", recipientId, message)
    }

    /**
     * Gets the current notification system being used.
     * @return true if using client-side notifications, false if using server-side
     */
    @JvmStatic
    fun isUsingClientSideNotifications(): Boolean {
        return NotificationConfig.USE_CLIENT_SIDE_NOTIFICATIONS
    }

    /**
     * Checks if the notification system is properly configured.
     * @return true if configuration is valid, false otherwise
     */
    @JvmStatic
    fun isNotificationSystemConfigured(): Boolean {
        return NotificationConfig.isConfigurationValid()
    }

    private fun saveNotificationToDatabase(
        recipientUid: String,
        senderUid: String,
        message: String,
        notificationType: String,
        data: Map<String, String>?
    ) {
        val notificationsRef = FirebaseDatabase.getInstance().getReference("skyline/notifications").child(recipientUid)
        val notificationId = notificationsRef.push().key
        if (notificationId != null) {
            val notification = com.synapse.social.studioasinc.model.Notification(
                senderUid,
                message,
                notificationType,
                data?.get("postId"),
                data?.get("commentId"),
                System.currentTimeMillis()
            )
            notificationsRef.child(notificationId).setValue(notification)
        }
    }
}
