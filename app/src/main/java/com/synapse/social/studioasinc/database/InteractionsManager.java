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

/**
 * Manages user interactions with content (likes, saves, views, shares)
 */
public class InteractionsManager {
    private static final String TAG = "InteractionsManager";
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    
    /**
     * Like content
     */
    public static Task<Boolean> likeContent(String contentId, String uid) {
        Map<String, Object> updates = new HashMap<>();
        
        // Add like
        updates.put("/" + DatabaseStructure.INTERACTIONS + "/likes/" + contentId + "/" + uid, ServerValue.TIMESTAMP);
        
        // Increment like counter
        return database.getReference().updateChildren(updates)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                // Update counter
                incrementLikeCount(contentId);
                return Tasks.forResult(true);
            });
    }
    
    /**
     * Unlike content
     */
    public static Task<Boolean> unlikeContent(String contentId, String uid) {
        Map<String, Object> updates = new HashMap<>();
        
        // Remove like
        updates.put("/" + DatabaseStructure.INTERACTIONS + "/likes/" + contentId + "/" + uid, null);
        
        return database.getReference().updateChildren(updates)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                // Update counter
                decrementLikeCount(contentId);
                return Tasks.forResult(false);
            });
    }
    
    /**
     * Check if user has liked content
     */
    public static Task<Boolean> hasLiked(String contentId, String uid) {
        return DatabaseStructure.getLikesRef(contentId)
            .child(uid)
            .get()
            .continueWith(task -> task.isSuccessful() && task.getResult().exists());
    }
    
    /**
     * Save content (bookmark)
     */
    public static Task<Boolean> saveContent(String contentId, String uid) {
        Map<String, Object> updates = new HashMap<>();
        
        // Add save
        updates.put("/" + DatabaseStructure.INTERACTIONS + "/saves/" + contentId + "/" + uid, ServerValue.TIMESTAMP);
        
        return database.getReference().updateChildren(updates)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                // Update counter
                incrementSaveCount(contentId);
                return Tasks.forResult(true);
            });
    }
    
    /**
     * Unsave content
     */
    public static Task<Boolean> unsaveContent(String contentId, String uid) {
        Map<String, Object> updates = new HashMap<>();
        
        // Remove save
        updates.put("/" + DatabaseStructure.INTERACTIONS + "/saves/" + contentId + "/" + uid, null);
        
        return database.getReference().updateChildren(updates)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                // Update counter
                decrementSaveCount(contentId);
                return Tasks.forResult(false);
            });
    }
    
    /**
     * Check if user has saved content
     */
    public static Task<Boolean> hasSaved(String contentId, String uid) {
        return DatabaseStructure.getInteractionsRef()
            .child("saves")
            .child(contentId)
            .child(uid)
            .get()
            .continueWith(task -> task.isSuccessful() && task.getResult().exists());
    }
    
    /**
     * Record view for content (mainly for reels/videos)
     */
    public static Task<Void> recordView(String contentId, String uid) {
        // Check if already viewed
        return DatabaseStructure.getInteractionsRef()
            .child("views")
            .child(contentId)
            .child(uid)
            .get()
            .continueWithTask(task -> {
                if (task.isSuccessful() && !task.getResult().exists()) {
                    // First time viewing
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("/" + DatabaseStructure.INTERACTIONS + "/views/" + contentId + "/" + uid, ServerValue.TIMESTAMP);
                    
                    return database.getReference().updateChildren(updates)
                        .continueWithTask(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                incrementViewCount(contentId);
                            }
                            return Tasks.forResult(null);
                        });
                } else {
                    // Already viewed
                    return Tasks.forResult(null);
                }
            });
    }
    
    /**
     * Share content
     */
    public static Task<Void> shareContent(String contentId, String uid, String platform) {
        Map<String, Object> shareData = new HashMap<>();
        shareData.put("timestamp", ServerValue.TIMESTAMP);
        shareData.put("platform", platform); // "internal", "whatsapp", "instagram", etc.
        
        return DatabaseStructure.getInteractionsRef()
            .child("shares")
            .child(contentId)
            .child(uid)
            .setValue(shareData)
            .continueWithTask(task -> {
                if (task.isSuccessful()) {
                    incrementShareCount(contentId);
                }
                return task;
            });
    }
    
    /**
     * Get likes for content
     */
    public static DatabaseReference getLikes(String contentId) {
        return DatabaseStructure.getLikesRef(contentId);
    }
    
    /**
     * Get user's saved content
     */
    public static Task<Map<String, Object>> getUserSavedContent(String uid) {
        return DatabaseStructure.getInteractionsRef()
            .child("saves")
            .orderByChild(uid)
            .equalTo(true)
            .get()
            .continueWith(task -> {
                Map<String, Object> savedContent = new HashMap<>();
                if (task.isSuccessful()) {
                    DataSnapshot snapshot = task.getResult();
                    for (DataSnapshot contentSnap : snapshot.getChildren()) {
                        String contentId = contentSnap.getKey();
                        DataSnapshot userSave = contentSnap.child(uid);
                        if (userSave.exists()) {
                            savedContent.put(contentId, userSave.getValue());
                        }
                    }
                }
                return savedContent;
            });
    }
    
    /**
     * Like a comment
     */
    public static Task<Void> likeComment(String commentId, String uid) {
        return database.getReference()
            .child(DatabaseStructure.COMMENTS + "_likes")
            .child(commentId)
            .child(uid)
            .setValue(ServerValue.TIMESTAMP)
            .continueWithTask(task -> {
                if (task.isSuccessful()) {
                    incrementCommentLikeCount(commentId);
                }
                return task;
            });
    }
    
    /**
     * Unlike a comment
     */
    public static Task<Void> unlikeComment(String commentId, String uid) {
        return database.getReference()
            .child(DatabaseStructure.COMMENTS + "_likes")
            .child(commentId)
            .child(uid)
            .removeValue()
            .continueWithTask(task -> {
                if (task.isSuccessful()) {
                    decrementCommentLikeCount(commentId);
                }
                return task;
            });
    }
    
    // Counter update methods
    private static void incrementLikeCount(String contentId) {
        DatabaseStructure.getContentRef()
            .child(contentId)
            .child("counters")
            .child("likes")
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
                        Log.e(TAG, "Failed to increment like count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void decrementLikeCount(String contentId) {
        DatabaseStructure.getContentRef()
            .child(contentId)
            .child("counters")
            .child("likes")
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
                        Log.e(TAG, "Failed to decrement like count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void incrementSaveCount(String contentId) {
        DatabaseStructure.getContentRef()
            .child(contentId)
            .child("counters")
            .child("saves")
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
                        Log.e(TAG, "Failed to increment save count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void decrementSaveCount(String contentId) {
        DatabaseStructure.getContentRef()
            .child(contentId)
            .child("counters")
            .child("saves")
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
                        Log.e(TAG, "Failed to decrement save count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void incrementViewCount(String contentId) {
        DatabaseStructure.getContentRef()
            .child(contentId)
            .child("counters")
            .child("views")
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
                        Log.e(TAG, "Failed to increment view count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void incrementShareCount(String contentId) {
        DatabaseStructure.getContentRef()
            .child(contentId)
            .child("counters")
            .child("shares")
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
                        Log.e(TAG, "Failed to increment share count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void incrementCommentLikeCount(String commentId) {
        // Find which content this comment belongs to
        database.getReference()
            .child(DatabaseStructure.COMMENTS)
            .orderByChild(commentId)
            .limitToFirst(1)
            .get()
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    DataSnapshot snapshot = task.getResult();
                    for (DataSnapshot contentSnap : snapshot.getChildren()) {
                        String contentId = contentSnap.getKey();
                        contentSnap.getRef()
                            .child(commentId)
                            .child("likes")
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
                                        Log.e(TAG, "Failed to increment comment like count", databaseError.toException());
                                    }
                                }
                            });
                        break;
                    }
                }
                return null;
            });
    }
    
    private static void decrementCommentLikeCount(String commentId) {
        // Similar to increment but decrement
        database.getReference()
            .child(DatabaseStructure.COMMENTS)
            .orderByChild(commentId)
            .limitToFirst(1)
            .get()
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    DataSnapshot snapshot = task.getResult();
                    for (DataSnapshot contentSnap : snapshot.getChildren()) {
                        String contentId = contentSnap.getKey();
                        contentSnap.getRef()
                            .child(commentId)
                            .child("likes")
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
                                        Log.e(TAG, "Failed to decrement comment like count", databaseError.toException());
                                    }
                                }
                            });
                        break;
                    }
                }
                return null;
            });
    }
}