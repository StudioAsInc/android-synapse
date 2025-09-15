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
 * Manages content moderation and reporting
 */
public class ModerationManager {
    private static final String TAG = "ModerationManager";
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    
    // Report types
    public static final String TYPE_POST = "post";
    public static final String TYPE_COMMENT = "comment";
    public static final String TYPE_USER = "user";
    public static final String TYPE_GROUP = "group";
    public static final String TYPE_MESSAGE = "message";
    public static final String TYPE_REEL = "reel";
    public static final String TYPE_STORY = "story";
    
    // Report reasons
    public static final String REASON_SPAM = "spam";
    public static final String REASON_HARASSMENT = "harassment";
    public static final String REASON_HATE_SPEECH = "hate_speech";
    public static final String REASON_NUDITY = "nudity";
    public static final String REASON_VIOLENCE = "violence";
    public static final String REASON_FALSE_INFO = "false_information";
    public static final String REASON_COPYRIGHT = "copyright";
    public static final String REASON_IMPERSONATION = "impersonation";
    public static final String REASON_OTHER = "other";
    
    // Report status
    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_REVIEWING = "reviewing";
    public static final String STATUS_RESOLVED = "resolved";
    public static final String STATUS_DISMISSED = "dismissed";
    
    // Admin actions
    public static final String ACTION_CONTENT_REMOVED = "content_removed";
    public static final String ACTION_USER_WARNED = "user_warned";
    public static final String ACTION_USER_SUSPENDED = "user_suspended";
    public static final String ACTION_USER_BANNED = "user_banned";
    public static final String ACTION_NO_VIOLATION = "no_violation";
    
    /**
     * Report content
     */
    public static Task<String> reportContent(String reporterUid, String type, String reportedId,
                                            String reason, String description,
                                            Map<String, Object> additionalInfo) {
        
        String reportId = database.getReference().push().getKey();
        
        Map<String, Object> report = new HashMap<>();
        report.put("reporter_uid", reporterUid);
        report.put("type", type);
        report.put("reported_id", reportedId);
        report.put("reason", reason);
        report.put("description", description);
        report.put("timestamp", ServerValue.TIMESTAMP);
        report.put("status", STATUS_PENDING);
        
        if (additionalInfo != null) {
            report.putAll(additionalInfo);
        }
        
        return database.getReference()
            .child(DatabaseStructure.MODERATION)
            .child("reports")
            .child(reportId)
            .setValue(report)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return reportId;
                } else {
                    throw task.getException();
                }
            });
    }
    
    /**
     * Report a user
     */
    public static Task<String> reportUser(String reporterUid, String reportedUid,
                                         String reason, String description) {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("reported_user_uid", reportedUid);
        
        return reportContent(reporterUid, TYPE_USER, reportedUid, reason, description, additionalInfo);
    }
    
    /**
     * Report a post
     */
    public static Task<String> reportPost(String reporterUid, String postId, String postOwnerId,
                                         String reason, String description) {
        Map<String, Object> additionalInfo = new HashMap<>();
        additionalInfo.put("content_owner_uid", postOwnerId);
        
        return reportContent(reporterUid, TYPE_POST, postId, reason, description, additionalInfo);
    }
    
    /**
     * Update report status (for admins)
     */
    public static Task<Void> updateReportStatus(String reportId, String newStatus, String adminUid) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("reviewed_by", adminUid);
        updates.put("reviewed_at", ServerValue.TIMESTAMP);
        
        return database.getReference()
            .child(DatabaseStructure.MODERATION)
            .child("reports")
            .child(reportId)
            .updateChildren(updates);
    }
    
    /**
     * Take action on a report (for admins)
     */
    public static Task<String> takeAction(String reportId, String adminUid, String action,
                                         String targetUid, String notes) {
        
        // First update the report
        Task<Void> updateReportTask = updateReportStatus(reportId, STATUS_RESOLVED, adminUid);
        
        // Then log the admin action
        String actionId = database.getReference().push().getKey();
        
        Map<String, Object> adminAction = new HashMap<>();
        adminAction.put("admin_uid", adminUid);
        adminAction.put("action", action);
        adminAction.put("target_id", targetUid);
        adminAction.put("report_id", reportId);
        adminAction.put("notes", notes);
        adminAction.put("timestamp", ServerValue.TIMESTAMP);
        
        Task<Void> logActionTask = database.getReference()
            .child(DatabaseStructure.MODERATION)
            .child("admin_logs")
            .child(actionId)
            .setValue(adminAction);
        
        // Execute the action
        Task<Void> executeActionTask = executeModeration(action, targetUid, adminUid);
        
        return Tasks.whenAll(updateReportTask, logActionTask, executeActionTask)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return actionId;
                } else {
                    throw task.getException();
                }
            });
    }
    
    /**
     * Execute moderation action
     */
    private static Task<Void> executeModeration(String action, String targetUid, String adminUid) {
        Map<String, Object> updates = new HashMap<>();
        
        switch (action) {
            case ACTION_USER_SUSPENDED:
                updates.put("/" + DatabaseStructure.USERS + "/" + targetUid + "/account_status", "suspended");
                updates.put("/" + DatabaseStructure.USERS + "/" + targetUid + "/suspended_at", ServerValue.TIMESTAMP);
                updates.put("/" + DatabaseStructure.USERS + "/" + targetUid + "/suspended_by", adminUid);
                break;
                
            case ACTION_USER_BANNED:
                updates.put("/" + DatabaseStructure.USERS + "/" + targetUid + "/account_status", "banned");
                updates.put("/" + DatabaseStructure.USERS + "/" + targetUid + "/banned_at", ServerValue.TIMESTAMP);
                updates.put("/" + DatabaseStructure.USERS + "/" + targetUid + "/banned_by", adminUid);
                break;
                
            case ACTION_USER_WARNED:
                // Just send a warning notification
                return NotificationManager.sendNotification(
                    targetUid,
                    "warning",
                    "system",
                    "Your account has received a warning for violating community guidelines.",
                    null,
                    null
                ).continueWith(task -> null);
                
            case ACTION_CONTENT_REMOVED:
                // Content removal would be handled separately based on content type
                return Tasks.forResult(null);
                
            case ACTION_NO_VIOLATION:
                // No action needed
                return Tasks.forResult(null);
                
            default:
                return Tasks.forException(new IllegalArgumentException("Unknown action: " + action));
        }
        
        if (updates.isEmpty()) {
            return Tasks.forResult(null);
        }
        
        return database.getReference().updateChildren(updates);
    }
    
    /**
     * Suspend a user
     */
    public static Task<Void> suspendUser(String uid, String adminUid, long durationInMillis, String reason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("account_status", "suspended");
        updates.put("suspended_at", ServerValue.TIMESTAMP);
        updates.put("suspended_by", adminUid);
        updates.put("suspended_until", System.currentTimeMillis() + durationInMillis);
        updates.put("suspension_reason", reason);
        
        return DatabaseStructure.getUserRef(uid).updateChildren(updates);
    }
    
    /**
     * Ban a user permanently
     */
    public static Task<Void> banUser(String uid, String adminUid, String reason) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("account_status", "banned");
        updates.put("banned_at", ServerValue.TIMESTAMP);
        updates.put("banned_by", adminUid);
        updates.put("ban_reason", reason);
        
        return DatabaseStructure.getUserRef(uid).updateChildren(updates);
    }
    
    /**
     * Unban/unsuspend a user
     */
    public static Task<Void> reinstateUser(String uid, String adminUid) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("account_status", "active");
        updates.put("reinstated_at", ServerValue.TIMESTAMP);
        updates.put("reinstated_by", adminUid);
        
        // Remove suspension/ban fields
        updates.put("suspended_at", null);
        updates.put("suspended_by", null);
        updates.put("suspended_until", null);
        updates.put("suspension_reason", null);
        updates.put("banned_at", null);
        updates.put("banned_by", null);
        updates.put("ban_reason", null);
        
        return DatabaseStructure.getUserRef(uid).updateChildren(updates);
    }
    
    /**
     * Get pending reports (for admins)
     */
    public static DatabaseReference getPendingReports() {
        return database.getReference()
            .child(DatabaseStructure.MODERATION)
            .child("reports")
            .orderByChild("status")
            .equalTo(STATUS_PENDING)
            .getRef();
    }
    
    /**
     * Get reports by type (for admins)
     */
    public static DatabaseReference getReportsByType(String type) {
        return database.getReference()
            .child(DatabaseStructure.MODERATION)
            .child("reports")
            .orderByChild("type")
            .equalTo(type)
            .getRef();
    }
    
    /**
     * Get user's report history
     */
    public static DatabaseReference getUserReports(String reporterUid) {
        return database.getReference()
            .child(DatabaseStructure.MODERATION)
            .child("reports")
            .orderByChild("reporter_uid")
            .equalTo(reporterUid)
            .getRef();
    }
    
    /**
     * Check if content has been reported
     */
    public static Task<Boolean> isContentReported(String contentId) {
        return database.getReference()
            .child(DatabaseStructure.MODERATION)
            .child("reports")
            .orderByChild("reported_id")
            .equalTo(contentId)
            .limitToFirst(1)
            .get()
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return task.getResult().exists();
                }
                return false;
            });
    }
    
    /**
     * Get report count for content
     */
    public static Task<Integer> getReportCount(String contentId) {
        return database.getReference()
            .child(DatabaseStructure.MODERATION)
            .child("reports")
            .orderByChild("reported_id")
            .equalTo(contentId)
            .get()
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return (int) task.getResult().getChildrenCount();
                }
                return 0;
            });
    }
    
    /**
     * Auto-hide content after threshold reports
     */
    public static Task<Void> checkAndHideContent(String contentId, int reportThreshold) {
        return getReportCount(contentId)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                int reportCount = task.getResult();
                if (reportCount >= reportThreshold) {
                    // Hide the content
                    return DatabaseStructure.getContentRef()
                        .child(contentId)
                        .child("visibility")
                        .setValue("hidden_pending_review");
                }
                
                return Tasks.forResult(null);
            });
    }
}