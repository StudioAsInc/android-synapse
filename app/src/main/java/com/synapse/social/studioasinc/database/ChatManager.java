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
 * Manages chat operations using the new SSoT (Single Source of Truth) structure
 */
public class ChatManager {
    private static final String TAG = "ChatManager";
    private static final FirebaseDatabase database = FirebaseDatabase.getInstance();
    private static final FirebaseAuth auth = FirebaseAuth.getInstance();
    
    /**
     * Get or create a chat room between two users
     * @return The room ID
     */
    public static Task<String> getOrCreateDirectRoom(String uid1, String uid2, String user1Name, String user2Name) {
        String roomId = DatabaseStructure.generateDirectRoomId(uid1, uid2);
        DatabaseReference roomRef = DatabaseStructure.getRoomRef(roomId);
        
        return roomRef.child("metadata").get().continueWithTask(task -> {
            if (task.isSuccessful() && task.getResult().exists()) {
                // Room already exists
                return Tasks.forResult(roomId);
            } else {
                // Create new room
                Map<String, Object> roomData = new HashMap<>();
                
                // Create metadata
                Map<String, Boolean> members = new HashMap<>();
                members.put(uid1, true);
                members.put(uid2, true);
                Map<String, Object> metadata = DatabaseStructure.createRoomMetadata("direct", members);
                roomData.put("metadata", metadata);
                
                // Create user room entries
                Map<String, Object> updates = new HashMap<>();
                updates.put("/" + DatabaseStructure.CHAT + "/" + DatabaseStructure.ROOMS + "/" + roomId, roomData);
                
                // Add to user_rooms for both users
                Map<String, Object> userRoom1 = new HashMap<>();
                userRoom1.put("last_message/text", "Chat started");
                userRoom1.put("last_message/timestamp", ServerValue.TIMESTAMP);
                userRoom1.put("unread_count", 0);
                userRoom1.put("is_muted", false);
                userRoom1.put("is_pinned", false);
                userRoom1.put("other_user_uid", uid2);
                userRoom1.put("other_user_name", user2Name);
                updates.put("/" + DatabaseStructure.CHAT + "/" + DatabaseStructure.USER_ROOMS + "/" + uid1 + "/" + roomId, userRoom1);
                
                Map<String, Object> userRoom2 = new HashMap<>();
                userRoom2.put("last_message/text", "Chat started");
                userRoom2.put("last_message/timestamp", ServerValue.TIMESTAMP);
                userRoom2.put("unread_count", 0);
                userRoom2.put("is_muted", false);
                userRoom2.put("is_pinned", false);
                userRoom2.put("other_user_uid", uid1);
                userRoom2.put("other_user_name", user1Name);
                updates.put("/" + DatabaseStructure.CHAT + "/" + DatabaseStructure.USER_ROOMS + "/" + uid2 + "/" + roomId, userRoom2);
                
                return database.getReference().updateChildren(updates)
                    .continueWith(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            return roomId;
                        } else {
                            throw updateTask.getException();
                        }
                    });
            }
        });
    }
    
    /**
     * Send a message in a chat room
     */
    public static Task<String> sendMessage(String roomId, String senderUid, String recipientUid, 
                                          String messageType, String content, Map<String, Object> additionalData) {
        DatabaseReference messagesRef = DatabaseStructure.getRoomRef(roomId).child(DatabaseStructure.MESSAGES);
        String messageId = messagesRef.push().getKey();
        
        // Create message
        Map<String, Object> message = DatabaseStructure.createMessage(senderUid, messageType, content);
        message.put("id", messageId);
        
        // Add additional data if provided (for attachments, etc.)
        if (additionalData != null) {
            message.putAll(additionalData);
        }
        
        // Prepare updates
        Map<String, Object> updates = new HashMap<>();
        
        // Add message to room
        updates.put("/" + DatabaseStructure.CHAT + "/" + DatabaseStructure.ROOMS + "/" + roomId + "/" + DatabaseStructure.MESSAGES + "/" + messageId, message);
        
        // Update last message for both users
        Map<String, Object> lastMessage = new HashMap<>();
        lastMessage.put("text", messageType.equals("text") ? content : "[" + messageType + "]");
        lastMessage.put("type", messageType);
        lastMessage.put("timestamp", ServerValue.TIMESTAMP);
        lastMessage.put("sender_uid", senderUid);
        
        updates.put("/" + DatabaseStructure.CHAT + "/" + DatabaseStructure.USER_ROOMS + "/" + senderUid + "/" + roomId + "/last_message", lastMessage);
        updates.put("/" + DatabaseStructure.CHAT + "/" + DatabaseStructure.USER_ROOMS + "/" + recipientUid + "/" + roomId + "/last_message", lastMessage);
        
        // Increment unread count for recipient
        DatabaseReference recipientUnreadRef = DatabaseStructure.getUserRoomsRef(recipientUid)
            .child(roomId).child("unread_count");
        
        return recipientUnreadRef.get().continueWithTask(task -> {
            int currentUnread = 0;
            if (task.isSuccessful() && task.getResult().exists()) {
                currentUnread = task.getResult().getValue(Integer.class);
            }
            updates.put("/" + DatabaseStructure.CHAT + "/" + DatabaseStructure.USER_ROOMS + "/" + recipientUid + "/" + roomId + "/unread_count", currentUnread + 1);
            
            return database.getReference().updateChildren(updates)
                .continueWith(updateTask -> {
                    if (updateTask.isSuccessful()) {
                        return messageId;
                    } else {
                        throw updateTask.getException();
                    }
                });
        });
    }
    
    /**
     * Mark messages as read
     */
    public static Task<Void> markMessagesAsRead(String roomId, String uid) {
        Map<String, Object> updates = new HashMap<>();
        
        // Reset unread count
        updates.put("/" + DatabaseStructure.CHAT + "/" + DatabaseStructure.USER_ROOMS + "/" + uid + "/" + roomId + "/unread_count", 0);
        
        // Mark messages as read (this would need to iterate through unread messages)
        // For now, we'll just reset the count
        
        return database.getReference().updateChildren(updates);
    }
    
    /**
     * Delete a message
     */
    public static Task<Void> deleteMessage(String roomId, String messageId) {
        return DatabaseStructure.getRoomRef(roomId)
            .child(DatabaseStructure.MESSAGES)
            .child(messageId)
            .removeValue();
    }
    
    /**
     * Update a message (edit)
     */
    public static Task<Void> updateMessage(String roomId, String messageId, String newContent) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("content", newContent);
        updates.put("edited_at", ServerValue.TIMESTAMP);
        
        return DatabaseStructure.getRoomRef(roomId)
            .child(DatabaseStructure.MESSAGES)
            .child(messageId)
            .updateChildren(updates);
    }
    
    /**
     * Get user's chat rooms
     */
    public static DatabaseReference getUserRooms(String uid) {
        return DatabaseStructure.getUserRoomsRef(uid);
    }
    
    /**
     * Get messages for a room
     */
    public static DatabaseReference getRoomMessages(String roomId) {
        return DatabaseStructure.getRoomRef(roomId).child(DatabaseStructure.MESSAGES);
    }
    
    /**
     * Update typing indicator
     */
    public static Task<Void> updateTypingStatus(String roomId, String uid, boolean isTyping) {
        String path = "/" + DatabaseStructure.CHAT + "/" + DatabaseStructure.USER_ROOMS + "/" + uid + "/" + roomId + "/typing_indicators/" + uid;
        
        if (isTyping) {
            return database.getReference(path).setValue(true);
        } else {
            return database.getReference(path).removeValue();
        }
    }
    
    /**
     * Block a user - updates social graph and removes chat access
     */
    public static Task<Void> blockUser(String blockerUid, String blockedUid) {
        Map<String, Object> updates = new HashMap<>();
        
        // Add to blocks
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.BLOCKS + "/" + DatabaseStructure.USER_BLOCKS + "/" + blockerUid + "/" + blockedUid, true);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.BLOCKS + "/" + DatabaseStructure.BLOCKED_BY + "/" + blockedUid + "/" + blockerUid, true);
        
        // Remove from followers/following if exists
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWERS + "/" + blockerUid + "/" + blockedUid, null);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWERS + "/" + blockedUid + "/" + blockerUid, null);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWING + "/" + blockerUid + "/" + blockedUid, null);
        updates.put("/" + DatabaseStructure.SOCIAL_GRAPH + "/" + DatabaseStructure.FOLLOWING + "/" + blockedUid + "/" + blockerUid, null);
        
        return database.getReference().updateChildren(updates);
    }
    
    /**
     * Check if user is blocked
     */
    public static Task<Boolean> isUserBlocked(String uid1, String uid2) {
        List<Task<DataSnapshot>> tasks = new ArrayList<>();
        
        // Check if uid1 blocked uid2
        tasks.add(database.getReference()
            .child(DatabaseStructure.SOCIAL_GRAPH)
            .child(DatabaseStructure.BLOCKS)
            .child(DatabaseStructure.USER_BLOCKS)
            .child(uid1)
            .child(uid2)
            .get());
            
        // Check if uid2 blocked uid1
        tasks.add(database.getReference()
            .child(DatabaseStructure.SOCIAL_GRAPH)
            .child(DatabaseStructure.BLOCKS)
            .child(DatabaseStructure.USER_BLOCKS)
            .child(uid2)
            .child(uid1)
            .get());
            
        return Tasks.whenAllSuccess(tasks).continueWith(task -> {
            List<Object> resultObjects = task.getResult();
            List<DataSnapshot> results = new ArrayList<>();
            for (Object obj : resultObjects) {
                if (obj instanceof DataSnapshot) {
                    results.add((DataSnapshot) obj);
                }
            }
            return results.get(0).exists() || results.get(1).exists();
        });
    }
}