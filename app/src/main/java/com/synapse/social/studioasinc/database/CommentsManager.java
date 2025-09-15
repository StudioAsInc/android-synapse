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
 * Manages comments and replies on content
 */
public class CommentsManager {
    private static final String TAG = "CommentsManager";
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    
    /**
     * Add a comment to content
     */
    public static Task<String> addComment(String contentId, String uid, String text,
                                         Map<String, Boolean> taggedUsers) {
        
        DatabaseReference commentsRef = DatabaseStructure.getCommentsRef(contentId);
        String commentId = commentsRef.push().getKey();
        
        Map<String, Object> comment = new HashMap<>();
        comment.put("id", commentId);
        comment.put("uid", uid);
        comment.put("text", text);
        comment.put("created_at", ServerValue.TIMESTAMP);
        comment.put("likes", 0);
        comment.put("replies", 0);
        
        if (taggedUsers != null && !taggedUsers.isEmpty()) {
            comment.put("tagged_users", taggedUsers);
        }
        
        Map<String, Object> updates = new HashMap<>();
        
        // Add comment
        updates.put("/" + DatabaseStructure.COMMENTS + "/" + contentId + "/" + commentId, comment);
        
        // Update comment count on content
        return database.getReference().updateChildren(updates)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                incrementCommentCount(contentId);
                return Tasks.forResult(commentId);
            });
    }
    
    /**
     * Add a reply to a comment
     */
    public static Task<String> addReply(String contentId, String commentId, String uid, 
                                        String text, Map<String, Boolean> taggedUsers) {
        
        DatabaseReference repliesRef = database.getReference()
            .child("comment_replies")
            .child(commentId);
        String replyId = repliesRef.push().getKey();
        
        Map<String, Object> reply = new HashMap<>();
        reply.put("id", replyId);
        reply.put("uid", uid);
        reply.put("text", text);
        reply.put("created_at", ServerValue.TIMESTAMP);
        reply.put("likes", 0);
        
        if (taggedUsers != null && !taggedUsers.isEmpty()) {
            reply.put("tagged_users", taggedUsers);
        }
        
        Map<String, Object> updates = new HashMap<>();
        
        // Add reply
        updates.put("/comment_replies/" + commentId + "/" + replyId, reply);
        
        // Update reply count on original comment
        return database.getReference().updateChildren(updates)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                incrementReplyCount(contentId, commentId);
                return Tasks.forResult(replyId);
            });
    }
    
    /**
     * Delete a comment
     */
    public static Task<Void> deleteComment(String contentId, String commentId, String uid) {
        // First verify ownership
        return DatabaseStructure.getCommentsRef(contentId)
            .child(commentId)
            .child("uid")
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                String ownerId = task.getResult().getValue(String.class);
                if (!uid.equals(ownerId)) {
                    throw new SecurityException("User does not own this comment");
                }
                
                Map<String, Object> updates = new HashMap<>();
                
                // Remove comment
                updates.put("/" + DatabaseStructure.COMMENTS + "/" + contentId + "/" + commentId, null);
                
                // Remove all replies
                updates.put("/comment_replies/" + commentId, null);
                
                // Remove comment likes
                updates.put("/comment_likes/" + commentId, null);
                
                return database.getReference().updateChildren(updates)
                    .continueWithTask(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            decrementCommentCount(contentId);
                        }
                        return deleteTask;
                    });
            });
    }
    
    /**
     * Delete a reply
     */
    public static Task<Void> deleteReply(String contentId, String commentId, String replyId, String uid) {
        // First verify ownership
        return database.getReference()
            .child("comment_replies")
            .child(commentId)
            .child(replyId)
            .child("uid")
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                String ownerId = task.getResult().getValue(String.class);
                if (!uid.equals(ownerId)) {
                    throw new SecurityException("User does not own this reply");
                }
                
                return database.getReference()
                    .child("comment_replies")
                    .child(commentId)
                    .child(replyId)
                    .removeValue()
                    .continueWithTask(deleteTask -> {
                        if (deleteTask.isSuccessful()) {
                            decrementReplyCount(contentId, commentId);
                        }
                        return deleteTask;
                    });
            });
    }
    
    /**
     * Edit a comment
     */
    public static Task<Void> editComment(String contentId, String commentId, String uid, String newText) {
        return DatabaseStructure.getCommentsRef(contentId)
            .child(commentId)
            .child("uid")
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                String ownerId = task.getResult().getValue(String.class);
                if (!uid.equals(ownerId)) {
                    throw new SecurityException("User does not own this comment");
                }
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("text", newText);
                updates.put("edited_at", ServerValue.TIMESTAMP);
                
                return DatabaseStructure.getCommentsRef(contentId)
                    .child(commentId)
                    .updateChildren(updates);
            });
    }
    
    /**
     * Get comments for content
     */
    public static DatabaseReference getComments(String contentId) {
        return DatabaseStructure.getCommentsRef(contentId);
    }
    
    /**
     * Get replies for a comment
     */
    public static DatabaseReference getReplies(String commentId) {
        return database.getReference()
            .child("comment_replies")
            .child(commentId);
    }
    
    /**
     * Check if user has liked a comment
     */
    public static Task<Boolean> hasLikedComment(String commentId, String uid) {
        return database.getReference()
            .child("comment_likes")
            .child(commentId)
            .child(uid)
            .get()
            .continueWith(task -> task.isSuccessful() && task.getResult().exists());
    }
    
    /**
     * Report a comment
     */
    public static Task<String> reportComment(String contentId, String commentId, String reporterUid,
                                            String reason, String description) {
        String reportId = database.getReference().push().getKey();
        
        Map<String, Object> report = new HashMap<>();
        report.put("reporter_uid", reporterUid);
        report.put("type", "comment");
        report.put("reported_id", commentId);
        report.put("content_id", contentId);
        report.put("reason", reason);
        report.put("description", description);
        report.put("timestamp", ServerValue.TIMESTAMP);
        report.put("status", "pending");
        
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
    
    // Counter update methods
    private static void incrementCommentCount(String contentId) {
        DatabaseStructure.getContentRef()
            .child(contentId)
            .child("counters")
            .child("comments")
            .runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(
                    com.google.firebase.database.MutableData mutableData) {
                    Integer currentValue = mutableData.getValue(Integer.class);
                    if (currentValue == null) {
                        mutableData.setValue(1);
                    } else {
                        mutableData.setValue(currentValue + 1);
                    }
                    return com.google.firebase.database.Transaction.success(mutableData);
                }
                
                @Override
                public void onComplete(DatabaseError databaseError, boolean committed,
                                     DataSnapshot dataSnapshot) {
                    if (databaseError != null) {
                        Log.e(TAG, "Failed to increment comment count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void decrementCommentCount(String contentId) {
        DatabaseStructure.getContentRef()
            .child(contentId)
            .child("counters")
            .child("comments")
            .runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(
                    com.google.firebase.database.MutableData mutableData) {
                    Integer currentValue = mutableData.getValue(Integer.class);
                    if (currentValue == null || currentValue <= 0) {
                        mutableData.setValue(0);
                    } else {
                        mutableData.setValue(currentValue - 1);
                    }
                    return com.google.firebase.database.Transaction.success(mutableData);
                }
                
                @Override
                public void onComplete(DatabaseError databaseError, boolean committed,
                                     DataSnapshot dataSnapshot) {
                    if (databaseError != null) {
                        Log.e(TAG, "Failed to decrement comment count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void incrementReplyCount(String contentId, String commentId) {
        DatabaseStructure.getCommentsRef(contentId)
            .child(commentId)
            .child("replies")
            .runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(
                    com.google.firebase.database.MutableData mutableData) {
                    Integer currentValue = mutableData.getValue(Integer.class);
                    if (currentValue == null) {
                        mutableData.setValue(1);
                    } else {
                        mutableData.setValue(currentValue + 1);
                    }
                    return com.google.firebase.database.Transaction.success(mutableData);
                }
                
                @Override
                public void onComplete(DatabaseError databaseError, boolean committed,
                                     DataSnapshot dataSnapshot) {
                    if (databaseError != null) {
                        Log.e(TAG, "Failed to increment reply count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void decrementReplyCount(String contentId, String commentId) {
        DatabaseStructure.getCommentsRef(contentId)
            .child(commentId)
            .child("replies")
            .runTransaction(new com.google.firebase.database.Transaction.Handler() {
                @Override
                public com.google.firebase.database.Transaction.Result doTransaction(
                    com.google.firebase.database.MutableData mutableData) {
                    Integer currentValue = mutableData.getValue(Integer.class);
                    if (currentValue == null || currentValue <= 0) {
                        mutableData.setValue(0);
                    } else {
                        mutableData.setValue(currentValue - 1);
                    }
                    return com.google.firebase.database.Transaction.success(mutableData);
                }
                
                @Override
                public void onComplete(DatabaseError databaseError, boolean committed,
                                     DataSnapshot dataSnapshot) {
                    if (databaseError != null) {
                        Log.e(TAG, "Failed to decrement reply count", databaseError.toException());
                    }
                }
            });
    }
}