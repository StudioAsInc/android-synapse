package com.synapse.social.studioasinc.database;

import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Manages feed operations for efficient content discovery
 */
public class FeedManager {
    private static final String TAG = "FeedManager";
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    
    /**
     * Add content to user's profile feed
     */
    public static Task<Void> addToUserFeed(String uid, String contentId) {
        return DatabaseStructure.getUserFeedRef(uid)
            .child(contentId)
            .setValue(ServerValue.TIMESTAMP);
    }
    
    /**
     * Remove content from user's profile feed
     */
    public static Task<Void> removeFromUserFeed(String uid, String contentId) {
        return DatabaseStructure.getUserFeedRef(uid)
            .child(contentId)
            .removeValue();
    }
    
    /**
     * Add content to followers' home feeds
     * This should ideally be done server-side for scalability
     */
    public static Task<Void> distributeToFollowerFeeds(String authorUid, String contentId) {
        // Get all followers
        return DatabaseStructure.getFollowersRef(authorUid)
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                DataSnapshot snapshot = task.getResult();
                Map<String, Object> updates = new HashMap<>();
                
                // Add to each follower's home feed
                for (DataSnapshot followerSnap : snapshot.getChildren()) {
                    String followerUid = followerSnap.getKey();
                    updates.put("/" + DatabaseStructure.FEEDS + "/home_feed/" + followerUid + "/" + contentId, 
                               ServerValue.TIMESTAMP);
                }
                
                if (updates.isEmpty()) {
                    return Tasks.forResult(null);
                }
                
                // Batch update
                return database.getReference().updateChildren(updates);
            });
    }
    
    /**
     * Remove content from followers' home feeds
     */
    public static Task<Void> removeFromFollowerFeeds(String authorUid, String contentId) {
        // Get all followers
        return DatabaseStructure.getFollowersRef(authorUid)
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                DataSnapshot snapshot = task.getResult();
                Map<String, Object> updates = new HashMap<>();
                
                // Remove from each follower's home feed
                for (DataSnapshot followerSnap : snapshot.getChildren()) {
                    String followerUid = followerSnap.getKey();
                    updates.put("/" + DatabaseStructure.FEEDS + "/home_feed/" + followerUid + "/" + contentId, null);
                }
                
                if (updates.isEmpty()) {
                    return Tasks.forResult(null);
                }
                
                // Batch update
                return database.getReference().updateChildren(updates);
            });
    }
    
    /**
     * Add content to explore feed with a score
     */
    public static Task<Void> addToExploreFeed(String contentId, double score) {
        return DatabaseStructure.getFeedsRef()
            .child("explore_feed")
            .child(contentId)
            .setValue(score);
    }
    
    /**
     * Update explore feed score
     */
    public static Task<Void> updateExploreScore(String contentId, double newScore) {
        return DatabaseStructure.getFeedsRef()
            .child("explore_feed")
            .child(contentId)
            .setValue(newScore);
    }
    
    /**
     * Remove from explore feed
     */
    public static Task<Void> removeFromExploreFeed(String contentId) {
        return DatabaseStructure.getFeedsRef()
            .child("explore_feed")
            .child(contentId)
            .removeValue();
    }
    
    /**
     * Add reel to reels feed
     */
    public static Task<Void> addToReelsFeed(String uid, String reelId) {
        return DatabaseStructure.getFeedsRef()
            .child("reels_feed")
            .child(uid)
            .child(reelId)
            .setValue(ServerValue.TIMESTAMP);
    }
    
    /**
     * Get user's profile feed
     */
    public static Query getUserFeed(String uid, int limit) {
        return DatabaseStructure.getUserFeedRef(uid)
            .orderByValue()
            .limitToLast(limit);
    }
    
    /**
     * Get user's home feed (following feed)
     */
    public static Query getHomeFeed(String uid, int limit) {
        return DatabaseStructure.getHomeFeedRef(uid)
            .orderByValue()
            .limitToLast(limit);
    }
    
    /**
     * Get explore feed
     */
    public static Query getExploreFeed(int limit) {
        return DatabaseStructure.getFeedsRef()
            .child("explore_feed")
            .orderByValue()
            .limitToLast(limit);
    }
    
    /**
     * Get reels feed
     */
    public static Query getReelsFeed(String uid, int limit) {
        return DatabaseStructure.getFeedsRef()
            .child("reels_feed")
            .child(uid)
            .orderByValue()
            .limitToLast(limit);
    }
    
    /**
     * Refresh user's home feed based on current following
     */
    public static Task<Void> refreshHomeFeed(String uid) {
        // Clear existing home feed
        return DatabaseStructure.getHomeFeedRef(uid)
            .removeValue()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                // Get all users this user is following
                return DatabaseStructure.getFollowingRef(uid).get();
            })
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                DataSnapshot followingSnapshot = task.getResult();
                List<Task<DataSnapshot>> feedTasks = new ArrayList<>();
                
                // Get recent posts from each followed user
                for (DataSnapshot followedUser : followingSnapshot.getChildren()) {
                    String followedUid = followedUser.getKey();
                    Task<DataSnapshot> userFeedTask = DatabaseStructure.getUserFeedRef(followedUid)
                        .orderByValue()
                        .limitToLast(10) // Get last 10 posts per user
                        .get();
                    feedTasks.add(userFeedTask);
                }
                
                return Tasks.whenAllSuccess(feedTasks);
            })
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                List<Object> resultObjects = task.getResult();
                List<DataSnapshot> results = new ArrayList<>();
                for (Object obj : resultObjects) {
                    if (obj instanceof DataSnapshot) {
                        results.add((DataSnapshot) obj);
                    }
                }
                Map<String, Object> updates = new HashMap<>();
                
                // Aggregate all posts
                for (DataSnapshot userFeed : results) {
                    for (DataSnapshot post : userFeed.getChildren()) {
                        String contentId = post.getKey();
                        Object timestamp = post.getValue();
                        updates.put(contentId, timestamp);
                    }
                }
                
                if (updates.isEmpty()) {
                    return Tasks.forResult(null);
                }
                
                // Update home feed
                return DatabaseStructure.getHomeFeedRef(uid).updateChildren(updates);
            });
    }
    
    /**
     * Calculate content score for explore feed
     * This is a simple algorithm - should be more sophisticated in production
     */
    public static double calculateContentScore(int likes, int comments, int shares, int views, long ageInHours) {
        // Engagement score
        double engagementScore = (likes * 1.0) + (comments * 2.0) + (shares * 3.0);
        
        // View rate
        double viewRate = views > 0 ? engagementScore / views : 0;
        
        // Time decay factor (content gets less score as it ages)
        double timeFactor = Math.max(0.1, 1.0 - (ageInHours / 168.0)); // 168 hours = 1 week
        
        // Final score
        return (engagementScore * viewRate * timeFactor);
    }
    
    /**
     * Update feed when user follows someone
     */
    public static Task<Void> onUserFollowed(String followerUid, String followedUid) {
        // Get recent posts from followed user
        return DatabaseStructure.getUserFeedRef(followedUid)
            .orderByValue()
            .limitToLast(20) // Get last 20 posts
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                DataSnapshot snapshot = task.getResult();
                Map<String, Object> updates = new HashMap<>();
                
                // Add to follower's home feed
                for (DataSnapshot post : snapshot.getChildren()) {
                    updates.put("/" + DatabaseStructure.FEEDS + "/home_feed/" + followerUid + "/" + post.getKey(), 
                               post.getValue());
                }
                
                if (updates.isEmpty()) {
                    return Tasks.forResult(null);
                }
                
                return database.getReference().updateChildren(updates);
            });
    }
    
    /**
     * Update feed when user unfollows someone
     */
    public static Task<Void> onUserUnfollowed(String followerUid, String unfollowedUid) {
        // Get posts from unfollowed user in follower's home feed
        return DatabaseStructure.getContentRef()
            .orderByChild("uid")
            .equalTo(unfollowedUid)
            .get()
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                DataSnapshot snapshot = task.getResult();
                Map<String, Object> updates = new HashMap<>();
                
                // Remove from follower's home feed
                for (DataSnapshot content : snapshot.getChildren()) {
                    updates.put("/" + DatabaseStructure.FEEDS + "/home_feed/" + followerUid + "/" + content.getKey(), null);
                }
                
                if (updates.isEmpty()) {
                    return Tasks.forResult(null);
                }
                
                return database.getReference().updateChildren(updates);
            });
    }
}