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
 * Manages content operations (posts, reels, stories) using unified content structure
 */
public class ContentManager {
    private static final String TAG = "ContentManager";
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    
    /**
     * Create a new post
     */
    public static Task<String> createPost(String uid, String caption, String visibility,
                                         List<Map<String, Object>> mediaItems,
                                         Map<String, Boolean> taggedUsers,
                                         Map<String, Boolean> topicTags,
                                         Map<String, Object> location) {
        
        String contentId = DatabaseStructure.getContentRef().push().getKey();
        Map<String, Object> content = DatabaseStructure.createContentMap(uid, DatabaseStructure.TYPE_POST, caption, visibility);
        content.put("id", contentId);
        
        // Add media if provided
        if (mediaItems != null && !mediaItems.isEmpty()) {
            Map<String, Object> media = new HashMap<>();
            for (int i = 0; i < mediaItems.size(); i++) {
                media.put(String.valueOf(i), mediaItems.get(i));
            }
            content.put("media", media);
        }
        
        // Add tags
        if (taggedUsers != null && !taggedUsers.isEmpty()) {
            content.put("tagged_users", taggedUsers);
        }
        if (topicTags != null && !topicTags.isEmpty()) {
            content.put("topic_tags", topicTags);
        }
        
        // Add location
        if (location != null) {
            content.put("location", location);
        }
        
        // Prepare multi-path updates
        Map<String, Object> updates = new HashMap<>();
        
        // Add to content node
        updates.put("/" + DatabaseStructure.CONTENT + "/" + contentId, content);
        
        // Add to user's feed
        updates.put("/" + DatabaseStructure.FEEDS + "/user_feed/" + uid + "/" + contentId, ServerValue.TIMESTAMP);
        
        // Add to followers' home feeds (this should ideally be done server-side)
        // For now, we'll just add to user's feed
        
        // Update user's post count
        incrementPostCount(uid);
        
        // Update tags reverse index
        if (taggedUsers != null) {
            for (String taggedUid : taggedUsers.keySet()) {
                updates.put("/" + DatabaseStructure.INTERACTIONS + "/tags/" + taggedUid + "/" + contentId, ServerValue.TIMESTAMP);
            }
        }
        
        return database.getReference().updateChildren(updates)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return contentId;
                } else {
                    throw task.getException();
                }
            });
    }
    
    /**
     * Create a new reel
     */
    public static Task<String> createReel(String uid, String caption, String visibility,
                                         Map<String, Object> videoData,
                                         Map<String, Object> musicData,
                                         Map<String, Boolean> taggedUsers,
                                         Map<String, Boolean> topicTags,
                                         boolean duetAllowed, boolean stitchAllowed) {
        
        String contentId = DatabaseStructure.getContentRef().push().getKey();
        Map<String, Object> content = DatabaseStructure.createContentMap(uid, DatabaseStructure.TYPE_REEL, caption, visibility);
        content.put("id", contentId);
        
        // Add video data
        Map<String, Object> media = new HashMap<>();
        media.put("0", videoData);
        content.put("media", media);
        
        // Add music data
        if (musicData != null) {
            content.put("music", musicData);
        }
        
        // Update reel-specific config
        Map<String, Object> config = (Map<String, Object>) content.get("config");
        config.put("duet_allowed", duetAllowed);
        config.put("stitch_allowed", stitchAllowed);
        
        // Add tags
        if (taggedUsers != null && !taggedUsers.isEmpty()) {
            content.put("tagged_users", taggedUsers);
        }
        if (topicTags != null && !topicTags.isEmpty()) {
            content.put("topic_tags", topicTags);
        }
        
        // Prepare multi-path updates
        Map<String, Object> updates = new HashMap<>();
        
        // Add to content node
        updates.put("/" + DatabaseStructure.CONTENT + "/" + contentId, content);
        
        // Add to user's feed
        updates.put("/" + DatabaseStructure.FEEDS + "/user_feed/" + uid + "/" + contentId, ServerValue.TIMESTAMP);
        
        // Add to reels feed
        updates.put("/" + DatabaseStructure.FEEDS + "/reels_feed/" + uid + "/" + contentId, ServerValue.TIMESTAMP);
        
        // Update user's reel count
        incrementReelCount(uid);
        
        return database.getReference().updateChildren(updates)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return contentId;
                } else {
                    throw task.getException();
                }
            });
    }
    
    /**
     * Create a story (24-hour ephemeral content)
     */
    public static Task<String> createStory(String uid, String mediaType, String mediaUrl,
                                          String caption, String visibility) {
        
        String storyId = database.getReference().push().getKey();
        
        Map<String, Object> storyData = new HashMap<>();
        storyData.put("uid", uid);
        storyData.put("created_at", ServerValue.TIMESTAMP);
        storyData.put("expires_at", System.currentTimeMillis() + (24 * 60 * 60 * 1000)); // 24 hours
        storyData.put("media_type", mediaType);
        storyData.put("media_url", mediaUrl);
        storyData.put("caption", caption);
        storyData.put("visibility", visibility);
        
        Map<String, Object> updates = new HashMap<>();
        
        // Add to stories metadata
        updates.put("/" + DatabaseStructure.STORIES + "/metadata/" + storyId, storyData);
        
        // Add to user's active stories
        updates.put("/" + DatabaseStructure.STORIES + "/user_stories/" + uid + "/" + storyId, ServerValue.TIMESTAMP);
        
        return database.getReference().updateChildren(updates)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return storyId;
                } else {
                    throw task.getException();
                }
            });
    }
    
    /**
     * Delete content
     */
    public static Task<Void> deleteContent(String contentId, String uid) {
        // First verify ownership
        return DatabaseStructure.getContentRef().child(contentId).child("uid").get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                String ownerId = task.getResult().getValue(String.class);
                if (!uid.equals(ownerId)) {
                    throw new SecurityException("User does not own this content");
                }
                
                Map<String, Object> updates = new HashMap<>();
                
                // Remove from content
                updates.put("/" + DatabaseStructure.CONTENT + "/" + contentId, null);
                
                // Remove from feeds
                updates.put("/" + DatabaseStructure.FEEDS + "/user_feed/" + uid + "/" + contentId, null);
                
                // Remove interactions
                updates.put("/" + DatabaseStructure.INTERACTIONS + "/likes/" + contentId, null);
                updates.put("/" + DatabaseStructure.INTERACTIONS + "/saves/" + contentId, null);
                updates.put("/" + DatabaseStructure.INTERACTIONS + "/views/" + contentId, null);
                updates.put("/" + DatabaseStructure.INTERACTIONS + "/shares/" + contentId, null);
                
                // Remove comments
                updates.put("/" + DatabaseStructure.COMMENTS + "/" + contentId, null);
                
                // Decrement counters
                return DatabaseStructure.getContentRef().child(contentId).child("type").get()
                    .continueWithTask(typeTask -> {
                        if (typeTask.isSuccessful()) {
                            String type = typeTask.getResult().getValue(String.class);
                            if (DatabaseStructure.TYPE_POST.equals(type)) {
                                decrementPostCount(uid);
                            } else if (DatabaseStructure.TYPE_REEL.equals(type)) {
                                decrementReelCount(uid);
                            }
                        }
                        
                        return database.getReference().updateChildren(updates);
                    });
            });
    }
    
    /**
     * Update content visibility
     */
    public static Task<Void> updateContentVisibility(String contentId, String uid, String newVisibility) {
        return DatabaseStructure.getContentRef().child(contentId).child("uid").get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                String ownerId = task.getResult().getValue(String.class);
                if (!uid.equals(ownerId)) {
                    throw new SecurityException("User does not own this content");
                }
                
                return DatabaseStructure.getContentRef().child(contentId).child("visibility").setValue(newVisibility);
            });
    }
    
    /**
     * Update content caption
     */
    public static Task<Void> updateContentCaption(String contentId, String uid, String newCaption) {
        return DatabaseStructure.getContentRef().child(contentId).child("uid").get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                String ownerId = task.getResult().getValue(String.class);
                if (!uid.equals(ownerId)) {
                    throw new SecurityException("User does not own this content");
                }
                
                Map<String, Object> updates = new HashMap<>();
                updates.put("caption", newCaption);
                updates.put("edited_at", ServerValue.TIMESTAMP);
                
                return DatabaseStructure.getContentRef().child(contentId).updateChildren(updates);
            });
    }
    
    /**
     * Toggle comments on content
     */
    public static Task<Void> toggleComments(String contentId, String uid, boolean enabled) {
        return DatabaseStructure.getContentRef().child(contentId).child("uid").get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                String ownerId = task.getResult().getValue(String.class);
                if (!uid.equals(ownerId)) {
                    throw new SecurityException("User does not own this content");
                }
                
                return DatabaseStructure.getContentRef()
                    .child(contentId)
                    .child("config")
                    .child("comments_disabled")
                    .setValue(!enabled);
            });
    }
    
    /**
     * Get content by ID
     */
    public static DatabaseReference getContent(String contentId) {
        return DatabaseStructure.getContentRef().child(contentId);
    }
    
    /**
     * Get user's content feed
     */
    public static DatabaseReference getUserFeed(String uid) {
        return DatabaseStructure.getUserFeedRef(uid);
    }
    
    /**
     * View a story
     */
    public static Task<Void> viewStory(String storyId, String viewerUid) {
        return database.getReference()
            .child(DatabaseStructure.STORIES)
            .child("metadata")
            .child(storyId)
            .child("viewers")
            .child(viewerUid)
            .setValue(ServerValue.TIMESTAMP);
    }
    
    /**
     * Delete expired stories (should be run periodically)
     */
    public static Task<Void> deleteExpiredStories() {
        long now = System.currentTimeMillis();
        
        return database.getReference()
            .child(DatabaseStructure.STORIES)
            .child("metadata")
            .orderByChild("expires_at")
            .endAt(now)
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                Map<String, Object> updates = new HashMap<>();
                DataSnapshot snapshot = task.getResult();
                
                for (DataSnapshot storySnap : snapshot.getChildren()) {
                    String storyId = storySnap.getKey();
                    String uid = storySnap.child("uid").getValue(String.class);
                    
                    // Remove from metadata
                    updates.put("/" + DatabaseStructure.STORIES + "/metadata/" + storyId, null);
                    
                    // Remove from user stories
                    if (uid != null) {
                        updates.put("/" + DatabaseStructure.STORIES + "/user_stories/" + uid + "/" + storyId, null);
                    }
                }
                
                if (updates.isEmpty()) {
                    return Tasks.forResult(null);
                }
                
                return database.getReference().updateChildren(updates);
            });
    }
    
    // Counter update methods
    private static void incrementPostCount(String uid) {
        DatabaseStructure.getUserRef(uid)
            .child("counters")
            .child("posts")
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
                        Log.e(TAG, "Failed to increment post count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void decrementPostCount(String uid) {
        DatabaseStructure.getUserRef(uid)
            .child("counters")
            .child("posts")
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
                        Log.e(TAG, "Failed to decrement post count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void incrementReelCount(String uid) {
        DatabaseStructure.getUserRef(uid)
            .child("counters")
            .child("reels")
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
                        Log.e(TAG, "Failed to increment reel count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void decrementReelCount(String uid) {
        DatabaseStructure.getUserRef(uid)
            .child("counters")
            .child("reels")
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
                        Log.e(TAG, "Failed to decrement reel count", databaseError.toException());
                    }
                }
            });
    }
}