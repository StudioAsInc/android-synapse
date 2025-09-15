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
 * Manages social graph operations (followers, following, blocks, etc.)
 */
public class SocialGraphManager {
    private static final String TAG = "SocialGraphManager";
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    
    /**
     * Follow a user
     */
    public static Task<Void> followUser(String followerUid, String followedUid, boolean isPrivateAccount) {
        if (followerUid.equals(followedUid)) {
            return Tasks.forException(new IllegalArgumentException("Cannot follow yourself"));
        }
        
        Map<String, Object> updates = new HashMap<>();
        
        if (isPrivateAccount) {
            // Add to pending follow requests
            updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.PENDING_FOLLOW_REQUESTS + 
                       "/" + followedUid + "/" + followerUid, ServerValue.TIMESTAMP);
        } else {
            // Direct follow for public accounts
            updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWERS + 
                       "/" + followedUid + "/" + followerUid, ServerValue.TIMESTAMP);
            updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWING + 
                       "/" + followerUid + "/" + followedUid, ServerValue.TIMESTAMP);
            
            // Update counters
            incrementFollowerCount(followedUid);
            incrementFollowingCount(followerUid);
        }
        
        return database.getReference().updateChildren(updates);
    }
    
    /**
     * Unfollow a user
     */
    public static Task<Void> unfollowUser(String followerUid, String followedUid) {
        Map<String, Object> updates = new HashMap<>();
        
        // Remove from followers/following
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWERS + 
                   "/" + followedUid + "/" + followerUid, null);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWING + 
                   "/" + followerUid + "/" + followedUid, null);
        
        // Remove from close friends if exists
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.CLOSE_FRIENDS + 
                   "/" + followerUid + "/" + followedUid, null);
        
        // Update counters
        decrementFollowerCount(followedUid);
        decrementFollowingCount(followerUid);
        
        return database.getReference().updateChildren(updates);
    }
    
    /**
     * Accept a follow request
     */
    public static Task<Void> acceptFollowRequest(String requestedUid, String requesterUid) {
        Map<String, Object> updates = new HashMap<>();
        
        // Remove from pending
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.PENDING_FOLLOW_REQUESTS + 
                   "/" + requestedUid + "/" + requesterUid, null);
        
        // Add to followers/following
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWERS + 
                   "/" + requestedUid + "/" + requesterUid, ServerValue.TIMESTAMP);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWING + 
                   "/" + requesterUid + "/" + requestedUid, ServerValue.TIMESTAMP);
        
        // Update counters
        incrementFollowerCount(requestedUid);
        incrementFollowingCount(requesterUid);
        
        return database.getReference().updateChildren(updates);
    }
    
    /**
     * Reject a follow request
     */
    public static Task<Void> rejectFollowRequest(String requestedUid, String requesterUid) {
        return DatabaseStructure.getSocialGraphRef()
            .child(DatabaseStructure.PENDING_FOLLOW_REQUESTS)
            .child(requestedUid)
            .child(requesterUid)
            .removeValue();
    }
    
    /**
     * Add to close friends
     */
    public static Task<Void> addToCloseFriends(String uid, String friendUid) {
        return DatabaseStructure.getSocialGraphRef()
            .child(DatabaseStructure.CLOSE_FRIENDS)
            .child(uid)
            .child(friendUid)
            .setValue(true);
    }
    
    /**
     * Remove from close friends
     */
    public static Task<Void> removeFromCloseFriends(String uid, String friendUid) {
        return DatabaseStructure.getSocialGraphRef()
            .child(DatabaseStructure.CLOSE_FRIENDS)
            .child(uid)
            .child(friendUid)
            .removeValue();
    }
    
    /**
     * Block a user
     */
    public static Task<Void> blockUser(String blockerUid, String blockedUid) {
        Map<String, Object> updates = new HashMap<>();
        
        // Add to blocks
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.BLOCKS + 
                   "/" + DatabaseStructure.USER_BLOCKS + "/" + blockerUid + "/" + blockedUid, true);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.BLOCKS + 
                   "/" + DatabaseStructure.BLOCKED_BY + "/" + blockedUid + "/" + blockerUid, true);
        
        // Remove from followers/following if exists
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWERS + 
                   "/" + blockerUid + "/" + blockedUid, null);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWERS + 
                   "/" + blockedUid + "/" + blockerUid, null);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWING + 
                   "/" + blockerUid + "/" + blockedUid, null);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWING + 
                   "/" + blockedUid + "/" + blockerUid, null);
        
        // Remove from close friends
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.CLOSE_FRIENDS + 
                   "/" + blockerUid + "/" + blockedUid, null);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.CLOSE_FRIENDS + 
                   "/" + blockedUid + "/" + blockerUid, null);
        
        // Remove pending follow requests
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.PENDING_FOLLOW_REQUESTS + 
                   "/" + blockerUid + "/" + blockedUid, null);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.PENDING_FOLLOW_REQUESTS + 
                   "/" + blockedUid + "/" + blockerUid, null);
        
        return database.getReference().updateChildren(updates);
    }
    
    /**
     * Unblock a user
     */
    public static Task<Void> unblockUser(String blockerUid, String blockedUid) {
        Map<String, Object> updates = new HashMap<>();
        
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.BLOCKS + 
                   "/" + DatabaseStructure.USER_BLOCKS + "/" + blockerUid + "/" + blockedUid, null);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.BLOCKS + 
                   "/" + DatabaseStructure.BLOCKED_BY + "/" + blockedUid + "/" + blockerUid, null);
        
        return database.getReference().updateChildren(updates);
    }
    
    /**
     * Check if user is following another user
     */
    public static Task<Boolean> isFollowing(String followerUid, String followedUid) {
        return DatabaseStructure.getFollowingRef(followerUid)
            .child(followedUid)
            .get()
            .continueWith(task -> task.isSuccessful() && task.getResult().exists());
    }
    
    /**
     * Check if user has blocked another user
     */
    public static Task<Boolean> hasBlocked(String blockerUid, String blockedUid) {
        return DatabaseStructure.getSocialGraphRef()
            .child(DatabaseStructure.BLOCKS)
            .child(DatabaseStructure.USER_BLOCKS)
            .child(blockerUid)
            .child(blockedUid)
            .get()
            .continueWith(task -> task.isSuccessful() && task.getResult().exists());
    }
    
    /**
     * Check if user is blocked by another user
     */
    public static Task<Boolean> isBlockedBy(String uid, String potentialBlockerUid) {
        return DatabaseStructure.getSocialGraphRef()
            .child(DatabaseStructure.BLOCKS)
            .child(DatabaseStructure.BLOCKED_BY)
            .child(uid)
            .child(potentialBlockerUid)
            .get()
            .continueWith(task -> task.isSuccessful() && task.getResult().exists());
    }
    
    /**
     * Get followers reference
     */
    public static DatabaseReference getFollowers(String uid) {
        return DatabaseStructure.getFollowersRef(uid);
    }
    
    /**
     * Get following reference
     */
    public static DatabaseReference getFollowing(String uid) {
        return DatabaseStructure.getFollowingRef(uid);
    }
    
    /**
     * Get blocked users reference
     */
    public static DatabaseReference getBlockedUsers(String uid) {
        return DatabaseStructure.getSocialGraphRef()
            .child(DatabaseStructure.BLOCKS)
            .child(DatabaseStructure.USER_BLOCKS)
            .child(uid);
    }
    
    /**
     * Get pending follow requests
     */
    public static DatabaseReference getPendingRequests(String uid) {
        return DatabaseStructure.getSocialGraphRef()
            .child(DatabaseStructure.PENDING_FOLLOW_REQUESTS)
            .child(uid);
    }
    
    // Counter update methods
    private static void incrementFollowerCount(String uid) {
        DatabaseStructure.getUserRef(uid)
            .child("counters")
            .child("followers")
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
                        Log.e(TAG, "Failed to increment follower count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void decrementFollowerCount(String uid) {
        DatabaseStructure.getUserRef(uid)
            .child("counters")
            .child("followers")
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
                        Log.e(TAG, "Failed to decrement follower count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void incrementFollowingCount(String uid) {
        DatabaseStructure.getUserRef(uid)
            .child("counters")
            .child("following")
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
                        Log.e(TAG, "Failed to increment following count", databaseError.toException());
                    }
                }
            });
    }
    
    private static void decrementFollowingCount(String uid) {
        DatabaseStructure.getUserRef(uid)
            .child("counters")
            .child("following")
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
                        Log.e(TAG, "Failed to decrement following count", databaseError.toException());
                    }
                }
            });
    }
}