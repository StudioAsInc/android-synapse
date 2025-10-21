package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.util.Log
import com.onesignal.notifications.INotificationClickListener
import com.onesignal.notifications.INotificationClickEvent

/**
 * Handles notification clicks and routes users to the appropriate screens within the app.
 * This implements deep linking functionality for different types of notifications.
 */
class NotificationClickHandler : INotificationClickListener {
    
    companion object {
        private const val TAG = "NotificationClickHandler"
    }
    
    override fun onClick(event: INotificationClickEvent) {
        val context = SynapseApp.getContext()
        val notification = event.notification
        val additionalData = notification.additionalData
        
        Log.d(TAG, "Notification clicked with data: $additionalData")
        
        // Parse notification data
        val notificationType = additionalData?.optString("type") ?: ""
        val senderUid = additionalData?.optString("sender_uid")
        val chatId = additionalData?.optString("chat_id")
        val postId = additionalData?.optString("postId")
        val commentId = additionalData?.optString("commentId")
        
        // Handle different notification types
        when (notificationType) {
            "chat_message" -> handleChatNotification(context, senderUid, chatId)
            NotificationConfig.NOTIFICATION_TYPE_NEW_POST -> handlePostNotification(context, senderUid, postId)
            NotificationConfig.NOTIFICATION_TYPE_NEW_COMMENT -> handleCommentNotification(context, postId, commentId)
            NotificationConfig.NOTIFICATION_TYPE_NEW_REPLY -> handleReplyNotification(context, postId, commentId)
            NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_POST -> handleLikePostNotification(context, postId)
            NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_COMMENT -> handleLikeCommentNotification(context, postId, commentId)
            else -> handleDefaultNotification(context)
        }
    }
    
    /**
     * Handle chat message notifications - open the specific chat
     */
    private fun handleChatNotification(context: Context, senderUid: String?, chatId: String?) {
        if (senderUid.isNullOrBlank()) {
            Log.w(TAG, "Chat notification missing sender UID")
            handleDefaultNotification(context)
            return
        }
        
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra("recipientUid", senderUid)
            if (!chatId.isNullOrBlank()) {
                putExtra("chatId", chatId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        Log.d(TAG, "Opening chat with user: $senderUid")
        context.startActivity(intent)
    }
    
    /**
     * Handle new post notifications - open the user's profile or the specific post
     */
    private fun handlePostNotification(context: Context, senderUid: String?, postId: String?) {
        if (senderUid.isNullOrBlank()) {
            Log.w(TAG, "Post notification missing sender UID")
            handleDefaultNotification(context)
            return
        }
        
        val intent = Intent(context, ProfileActivity::class.java).apply {
            putExtra("uid", senderUid)
            if (!postId.isNullOrBlank()) {
                putExtra("postId", postId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        Log.d(TAG, "Opening profile for user: $senderUid")
        context.startActivity(intent)
    }
    
    /**
     * Handle comment notifications - open the post with comments
     */
    private fun handleCommentNotification(context: Context, postId: String?, commentId: String?) {
        if (postId.isNullOrBlank()) {
            Log.w(TAG, "Comment notification missing post ID")
            handleDefaultNotification(context)
            return
        }
        
        // For now, open the home activity and let the user navigate to the post
        // In a future update, you could implement a specific post viewer activity
        val intent = Intent(context, HomeActivity::class.java).apply {
            putExtra("openPost", postId)
            if (!commentId.isNullOrBlank()) {
                putExtra("highlightComment", commentId)
            }
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        Log.d(TAG, "Opening post: $postId with comment: $commentId")
        context.startActivity(intent)
    }
    
    /**
     * Handle reply notifications - open the post with the specific comment thread
     */
    private fun handleReplyNotification(context: Context, postId: String?, commentId: String?) {
        // Similar to comment notification but with reply context
        handleCommentNotification(context, postId, commentId)
    }
    
    /**
     * Handle post like notifications - open the specific post
     */
    private fun handleLikePostNotification(context: Context, postId: String?) {
        handleCommentNotification(context, postId, null)
    }
    
    /**
     * Handle comment like notifications - open the post with the specific comment
     */
    private fun handleLikeCommentNotification(context: Context, postId: String?, commentId: String?) {
        handleCommentNotification(context, postId, commentId)
    }
    
    /**
     * Default handler - open the home activity
     */
    private fun handleDefaultNotification(context: Context) {
        val intent = Intent(context, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        
        Log.d(TAG, "Opening default home activity")
        context.startActivity(intent)
    }
}