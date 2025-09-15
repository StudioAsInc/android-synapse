package com.synapse.social.studioasinc.database;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import java.util.HashMap;
import java.util.Map;

/**
 * Central class for managing Firebase database structure and paths
 * Implements the new SSoT (Single Source of Truth) architecture
 */
public class DatabaseStructure {
    
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    
    // Root nodes
    public static final String USERS = "users";
    public static final String USERNAMES = "usernames";
    public static final String SOCIAL_GRAPH = "social_graph";
    public static final String CONTENT = "content";
    public static final String FEEDS = "feeds";
    public static final String INTERACTIONS = "interactions";
    public static final String COMMENTS = "comments";
    public static final String CHAT = "chat";
    public static final String STORIES = "stories";
    public static final String NOTIFICATIONS = "notifications";
    public static final String MODERATION = "moderation";
    public static final String SYSTEM = "system";
    
    // Social graph sub-nodes
    public static final String FOLLOWERS = "followers";
    public static final String FOLLOWING = "following";
    public static final String CLOSE_FRIENDS = "close_friends";
    public static final String BLOCKS = "blocks";
    public static final String USER_BLOCKS = "user_blocks";
    public static final String BLOCKED_BY = "blocked_by";
    public static final String PENDING_FOLLOW_REQUESTS = "pending_follow_requests";
    
    // Chat sub-nodes
    public static final String ROOMS = "rooms";
    public static final String USER_ROOMS = "user_rooms";
    public static final String METADATA = "metadata";
    public static final String MESSAGES = "messages";
    public static final String READ_BY = "read_by";
    
    // Content types
    public static final String TYPE_POST = "post";
    public static final String TYPE_REEL = "reel";
    public static final String TYPE_STORY = "story";
    
    // Visibility levels
    public static final String VISIBILITY_PUBLIC = "public";
    public static final String VISIBILITY_FOLLOWERS = "followers";
    public static final String VISIBILITY_CLOSE_FRIENDS = "close_friends";
    public static final String VISIBILITY_PRIVATE = "private";
    
    // References
    public static DatabaseReference getUsersRef() {
        return database.getReference(USERS);
    }
    
    public static DatabaseReference getUserRef(String uid) {
        return getUsersRef().child(uid);
    }
    
    public static DatabaseReference getUsernamesRef() {
        return database.getReference(USERNAMES);
    }
    
    public static DatabaseReference getSocialGraphRef() {
        return database.getReference(SOCIAL_GRAPH);
    }
    
    public static DatabaseReference getFollowersRef(String uid) {
        return getSocialGraphRef().child(FOLLOWERS).child(uid);
    }
    
    public static DatabaseReference getFollowingRef(String uid) {
        return getSocialGraphRef().child(FOLLOWING).child(uid);
    }
    
    public static DatabaseReference getContentRef() {
        return database.getReference(CONTENT);
    }
    
    public static DatabaseReference getFeedsRef() {
        return database.getReference(FEEDS);
    }
    
    public static DatabaseReference getUserFeedRef(String uid) {
        return getFeedsRef().child("user_feed").child(uid);
    }
    
    public static DatabaseReference getHomeFeedRef(String uid) {
        return getFeedsRef().child("home_feed").child(uid);
    }
    
    public static DatabaseReference getInteractionsRef() {
        return database.getReference(INTERACTIONS);
    }
    
    public static DatabaseReference getLikesRef(String contentId) {
        return getInteractionsRef().child("likes").child(contentId);
    }
    
    public static DatabaseReference getCommentsRef(String contentId) {
        return database.getReference(COMMENTS).child(contentId);
    }
    
    public static DatabaseReference getChatRef() {
        return database.getReference(CHAT);
    }
    
    public static DatabaseReference getRoomsRef() {
        return getChatRef().child(ROOMS);
    }
    
    public static DatabaseReference getRoomRef(String roomId) {
        return getRoomsRef().child(roomId);
    }
    
    public static DatabaseReference getUserRoomsRef(String uid) {
        return getChatRef().child(USER_ROOMS).child(uid);
    }
    
    public static DatabaseReference getNotificationsRef(String uid) {
        return database.getReference(NOTIFICATIONS).child(uid);
    }
    
    // Helper methods
    
    /**
     * Generate a room ID for direct messages between two users
     * Always returns the same ID regardless of user order
     */
    public static String generateDirectRoomId(String uid1, String uid2) {
        if (uid1.compareTo(uid2) < 0) {
            return "direct_" + uid1 + "_" + uid2;
        } else {
            return "direct_" + uid2 + "_" + uid1;
        }
    }
    
    /**
     * Create a new content item with proper structure
     */
    public static Map<String, Object> createContentMap(String uid, String type, String caption, String visibility) {
        Map<String, Object> content = new HashMap<>();
        content.put("id", getContentRef().push().getKey());
        content.put("type", type);
        content.put("uid", uid);
        content.put("created_at", ServerValue.TIMESTAMP);
        content.put("edited_at", null);
        content.put("caption", caption);
        content.put("visibility", visibility);
        
        // Initialize counters
        Map<String, Object> counters = new HashMap<>();
        counters.put("likes", 0);
        counters.put("comments", 0);
        counters.put("shares", 0);
        counters.put("saves", 0);
        counters.put("views", 0);
        content.put("counters", counters);
        
        // Initialize config
        Map<String, Object> config = new HashMap<>();
        config.put("comments_disabled", false);
        config.put("likes_hidden", false);
        if (TYPE_REEL.equals(type)) {
            config.put("duet_allowed", true);
            config.put("stitch_allowed", true);
        }
        content.put("config", config);
        
        return content;
    }
    
    /**
     * Create a chat room metadata structure
     */
    public static Map<String, Object> createRoomMetadata(String type, Map<String, Boolean> members) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("type", type);
        metadata.put("created_at", ServerValue.TIMESTAMP);
        metadata.put("members", members);
        return metadata;
    }
    
    /**
     * Create a message structure
     */
    public static Map<String, Object> createMessage(String uid, String type, String content) {
        Map<String, Object> message = new HashMap<>();
        message.put("id", getRoomsRef().push().getKey());
        message.put("uid", uid);
        message.put("type", type);
        message.put("content", content);
        message.put("created_at", ServerValue.TIMESTAMP);
        return message;
    }
    
    /**
     * Create a notification structure
     */
    public static Map<String, Object> createNotification(String type, String fromUid, String message, String targetContentId) {
        Map<String, Object> notification = new HashMap<>();
        notification.put("type", type);
        notification.put("from_uid", fromUid);
        notification.put("message", message);
        notification.put("target_content_id", targetContentId);
        notification.put("timestamp", ServerValue.TIMESTAMP);
        notification.put("is_read", false);
        return notification;
    }
}