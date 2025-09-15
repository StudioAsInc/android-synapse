package com.synapse.social.studioasinc.database;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Manages notifications for user activities
 */
public class NotificationManager {
    private static final String TAG = "NotificationManager";
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    
    // Notification types
    public static final String TYPE_NEW_FOLLOWER = "new_follower";
    public static final String TYPE_FOLLOW_REQUEST = "follow_request";
    public static final String TYPE_FOLLOW_ACCEPTED = "follow_accepted";
    public static final String TYPE_LIKE = "like";
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_COMMENT_REPLY = "comment_reply";
    public static final String TYPE_POST_SHARE = "post_share";
    public static final String TYPE_TAG = "tag";
    public static final String TYPE_MENTION = "mention";
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_STORY_VIEW = "story_view";
    
    /**
     * Send a notification
     */
    public static Task<String> sendNotification(String toUid, String type, String fromUid,
                                               String message, String targetContentId,
                                               String targetCommentId) {
        
        if (toUid.equals(fromUid)) {
            // Don't send notifications to self
            return Tasks.forResult(null);
        }
        
        String notificationId = DatabaseStructure.getNotificationsRef(toUid).push().getKey();
        
        Map<String, Object> notification = DatabaseStructure.createNotification(type, fromUid, message, targetContentId);
        
        if (targetCommentId != null) {
            notification.put("target_comment_id", targetCommentId);
        }
        
        return DatabaseStructure.getNotificationsRef(toUid)
            .child(notificationId)
            .setValue(notification)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return notificationId;
                } else {
                    throw task.getException();
                }
            });
    }
    
    /**
     * Send follow notification
     */
    public static Task<String> sendFollowNotification(String followedUid, String followerUid, String followerName) {
        String message = followerName + " started following you.";
        return sendNotification(followedUid, TYPE_NEW_FOLLOWER, followerUid, message, null, null);
    }
    
    /**
     * Send follow request notification
     */
    public static Task<String> sendFollowRequestNotification(String requestedUid, String requesterUid, String requesterName) {
        String message = requesterName + " requested to follow you.";
        return sendNotification(requestedUid, TYPE_FOLLOW_REQUEST, requesterUid, message, null, null);
    }
    
    /**
     * Send follow accepted notification
     */
    public static Task<String> sendFollowAcceptedNotification(String requesterUid, String accepterUid, String accepterName) {
        String message = accepterName + " accepted your follow request.";
        return sendNotification(requesterUid, TYPE_FOLLOW_ACCEPTED, accepterUid, message, null, null);
    }
    
    /**
     * Send like notification
     */
    public static Task<String> sendLikeNotification(String contentOwnerId, String likerUid, 
                                                   String likerName, String contentId, String contentType) {
        String message = likerName + " liked your " + contentType + ".";
        return sendNotification(contentOwnerId, TYPE_LIKE, likerUid, message, contentId, null);
    }
    
    /**
     * Send comment notification
     */
    public static Task<String> sendCommentNotification(String contentOwnerId, String commenterUid,
                                                      String commenterName, String contentId, 
                                                      String commentId, String commentPreview) {
        String message = commenterName + " commented: " + truncateText(commentPreview, 50);
        return sendNotification(contentOwnerId, TYPE_COMMENT, commenterUid, message, contentId, commentId);
    }
    
    /**
     * Send reply notification
     */
    public static Task<String> sendReplyNotification(String commentOwnerId, String replierUid,
                                                    String replierName, String contentId,
                                                    String commentId, String replyPreview) {
        String message = replierName + " replied: " + truncateText(replyPreview, 50);
        return sendNotification(commentOwnerId, TYPE_COMMENT_REPLY, replierUid, message, contentId, commentId);
    }
    
    /**
     * Send share notification
     */
    public static Task<String> sendShareNotification(String contentOwnerId, String sharerUid,
                                                    String sharerName, String contentId) {
        String message = sharerName + " shared your post.";
        return sendNotification(contentOwnerId, TYPE_POST_SHARE, sharerUid, message, contentId, null);
    }
    
    /**
     * Send tag notification
     */
    public static Task<String> sendTagNotification(String taggedUid, String taggerUid,
                                                  String taggerName, String contentId, String contentType) {
        String message = taggerName + " tagged you in a " + contentType + ".";
        return sendNotification(taggedUid, TYPE_TAG, taggerUid, message, contentId, null);
    }
    
    /**
     * Send mention notification
     */
    public static Task<String> sendMentionNotification(String mentionedUid, String mentionerUid,
                                                      String mentionerName, String contentId,
                                                      String commentId, String context) {
        String message = mentionerName + " mentioned you in a " + context + ".";
        return sendNotification(mentionedUid, TYPE_MENTION, mentionerUid, message, contentId, commentId);
    }
    
    /**
     * Send message notification
     */
    public static Task<String> sendMessageNotification(String toUid, String fromUid,
                                                      String fromName, String messagePreview) {
        String message = fromName + ": " + truncateText(messagePreview, 50);
        return sendNotification(toUid, TYPE_MESSAGE, fromUid, message, null, null);
    }
    
    /**
     * Mark notification as read
     */
    public static Task<Void> markAsRead(String uid, String notificationId) {
        return DatabaseStructure.getNotificationsRef(uid)
            .child(notificationId)
            .child("is_read")
            .setValue(true);
    }
    
    /**
     * Mark all notifications as read
     */
    public static Task<Void> markAllAsRead(String uid) {
        return DatabaseStructure.getNotificationsRef(uid)
            .orderByChild("is_read")
            .equalTo(false)
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                Map<String, Object> updates = new HashMap<>();
                DataSnapshot snapshot = task.getResult();
                
                for (DataSnapshot notifSnap : snapshot.getChildren()) {
                    updates.put(notifSnap.getKey() + "/is_read", true);
                }
                
                if (updates.isEmpty()) {
                    return Tasks.forResult(null);
                }
                
                return DatabaseStructure.getNotificationsRef(uid).updateChildren(updates);
            });
    }
    
    /**
     * Delete a notification
     */
    public static Task<Void> deleteNotification(String uid, String notificationId) {
        return DatabaseStructure.getNotificationsRef(uid)
            .child(notificationId)
            .removeValue();
    }
    
    /**
     * Delete old notifications (older than 30 days)
     */
    public static Task<Void> deleteOldNotifications(String uid) {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        
        return DatabaseStructure.getNotificationsRef(uid)
            .orderByChild("timestamp")
            .endAt(thirtyDaysAgo)
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                List<Task<Void>> deleteTasks = new ArrayList<>();
                DataSnapshot snapshot = task.getResult();
                
                for (DataSnapshot notifSnap : snapshot.getChildren()) {
                    deleteTasks.add(notifSnap.getRef().removeValue());
                }
                
                if (deleteTasks.isEmpty()) {
                    return Tasks.forResult(null);
                }
                
                return Tasks.whenAll(deleteTasks);
            });
    }
    
    /**
     * Get notifications for a user
     */
    public static DatabaseReference getNotifications(String uid) {
        return DatabaseStructure.getNotificationsRef(uid);
    }
    
    /**
     * Get unread notification count
     */
    public static Task<Integer> getUnreadCount(String uid) {
        return DatabaseStructure.getNotificationsRef(uid)
            .orderByChild("is_read")
            .equalTo(false)
            .get()
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return (int) task.getResult().getChildrenCount();
                } else {
                    return 0;
                }
            });
    }
    
    /**
     * Send batch notifications (for multiple users)
     */
    public static Task<List<String>> sendBatchNotifications(List<String> toUids, String type,
                                                           String fromUid, String message,
                                                           String targetContentId) {
        List<Task<String>> tasks = new ArrayList<>();
        
        for (String toUid : toUids) {
            tasks.add(sendNotification(toUid, type, fromUid, message, targetContentId, null));
        }
        
        return Tasks.whenAllSuccess(tasks);
    }
    
    /**
     * Helper method to truncate text
     */
    private static String truncateText(String text, int maxLength) {
        if (text == null) return "";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Clear all notifications for a user
     */
    public static Task<Void> clearAllNotifications(String uid) {
        return DatabaseStructure.getNotificationsRef(uid).removeValue();
    }
    
    /**
     * Get notification settings for a user
     */
    public static Task<Boolean> areNotificationsEnabled(String uid) {
        return DatabaseStructure.getUserRef(uid)
            .child("settings")
            .child("notifications_enabled")
            .get()
            .continueWith(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    return task.getResult().getValue(Boolean.class);
                }
                return true; // Default to enabled
            });
    }
    
    /**
     * Update notification settings
     */
    public static Task<Void> updateNotificationSettings(String uid, boolean enabled) {
        return DatabaseStructure.getUserRef(uid)
            .child("settings")
            .child("notifications_enabled")
            .setValue(enabled);
    }
}