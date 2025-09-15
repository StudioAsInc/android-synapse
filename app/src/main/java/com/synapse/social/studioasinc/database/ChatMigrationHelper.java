package com.synapse.social.studioasinc.database;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.ChildEventListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to migrate from old chat structure to new SSoT structure
 * This allows gradual migration without breaking existing functionality
 */
public class ChatMigrationHelper {
    private static final String TAG = "ChatMigrationHelper";
    private static final String SKYLINE_REF = "skyline";
    private static final String CHATS_REF = "chats";
    private static final String USERS_REF = "users";
    
    private final FirebaseDatabase database;
    private final FirebaseAuth auth;
    private String currentRoomId = null;
    private boolean useLegacyMode = true; // Start with legacy mode
    
    public ChatMigrationHelper() {
        this.database = FirebaseDatabase.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }
    
    /**
     * Initialize chat - determines whether to use legacy or new structure
     */
    public Task<String> initializeChat(String myUid, String otherUid, String myName, String otherName) {
        // First check if a room exists in the new structure
        String roomId = DatabaseStructure.generateDirectRoomId(myUid, otherUid);
        
        return DatabaseStructure.getRoomRef(roomId).child("metadata").get()
            .continueWithTask(task -> {
                if (task.isSuccessful() && task.getResult().exists()) {
                    // Room exists in new structure
                    useLegacyMode = false;
                    currentRoomId = roomId;
                    Log.d(TAG, "Using new chat structure for room: " + roomId);
                    return Tasks.forResult(roomId);
                } else {
                    // Check if legacy chat exists
                    return checkLegacyChat(myUid, otherUid).continueWithTask(legacyTask -> {
                        if (legacyTask.getResult()) {
                            // Legacy chat exists, continue using it for now
                            useLegacyMode = true;
                            Log.d(TAG, "Using legacy chat structure");
                            return Tasks.forResult(null);
                        } else {
                            // No chat exists, create new room
                            useLegacyMode = false;
                            Log.d(TAG, "Creating new chat room: " + roomId);
                            return ChatManager.getOrCreateDirectRoom(myUid, otherUid, myName, otherName)
                                .continueWith(createTask -> {
                                    currentRoomId = createTask.getResult();
                                    return currentRoomId;
                                });
                        }
                    });
                }
            });
    }
    
    /**
     * Check if legacy chat exists
     */
    private Task<Boolean> checkLegacyChat(String myUid, String otherUid) {
        DatabaseReference legacyRef = database.getReference(SKYLINE_REF)
            .child(CHATS_REF).child(myUid).child(otherUid);
            
        return legacyRef.get().continueWith(task -> {
            return task.isSuccessful() && task.getResult().exists() && 
                   task.getResult().hasChildren();
        });
    }
    
    /**
     * Get the appropriate database reference for messages
     */
    public DatabaseReference getMessagesRef(String myUid, String otherUid) {
        if (useLegacyMode) {
            return database.getReference(SKYLINE_REF)
                .child(CHATS_REF).child(myUid).child(otherUid);
        } else {
            return ChatManager.getRoomMessages(currentRoomId);
        }
    }
    
    /**
     * Send a message using appropriate structure
     */
    public Task<String> sendMessage(String myUid, String otherUid, String messageType, 
                                   String content, Map<String, Object> additionalData) {
        if (useLegacyMode) {
            // Use legacy dual-write approach
            return sendLegacyMessage(myUid, otherUid, messageType, content, additionalData);
        } else {
            // Use new SSoT approach
            return ChatManager.sendMessage(currentRoomId, myUid, otherUid, 
                                         messageType, content, additionalData);
        }
    }
    
    /**
     * Send message using legacy structure (dual write)
     */
    private Task<String> sendLegacyMessage(String myUid, String otherUid, String messageType,
                                          String content, Map<String, Object> additionalData) {
        DatabaseReference ref1 = database.getReference(SKYLINE_REF)
            .child(CHATS_REF).child(myUid).child(otherUid);
        DatabaseReference ref2 = database.getReference(SKYLINE_REF)
            .child(CHATS_REF).child(otherUid).child(myUid);
            
        String messageKey = ref1.push().getKey();
        
        Map<String, Object> message = new HashMap<>();
        message.put("key", messageKey);
        message.put("uid", myUid);
        message.put("messageText", content);
        message.put("pushDate", com.google.firebase.database.ServerValue.TIMESTAMP);
        
        if (additionalData != null) {
            message.putAll(additionalData);
        }
        
        Map<String, Object> updates = new HashMap<>();
        updates.put("/" + SKYLINE_REF + "/" + CHATS_REF + "/" + myUid + "/" + otherUid + "/" + messageKey, message);
        updates.put("/" + SKYLINE_REF + "/" + CHATS_REF + "/" + otherUid + "/" + myUid + "/" + messageKey, message);
        
        return database.getReference().updateChildren(updates)
            .continueWith(task -> {
                if (task.isSuccessful()) {
                    return messageKey;
                } else {
                    throw task.getException();
                }
            });
    }
    
    /**
     * Delete a message
     */
    public Task<Void> deleteMessage(String myUid, String otherUid, String messageId) {
        if (useLegacyMode) {
            // Delete from both locations
            Map<String, Object> updates = new HashMap<>();
            updates.put("/" + SKYLINE_REF + "/" + CHATS_REF + "/" + myUid + "/" + otherUid + "/" + messageId, null);
            updates.put("/" + SKYLINE_REF + "/" + CHATS_REF + "/" + otherUid + "/" + myUid + "/" + messageId, null);
            return database.getReference().updateChildren(updates);
        } else {
            return ChatManager.deleteMessage(currentRoomId, messageId);
        }
    }
    
    /**
     * Update message (edit)
     */
    public Task<Void> updateMessage(String myUid, String otherUid, String messageId, String newContent) {
        if (useLegacyMode) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("/" + SKYLINE_REF + "/" + CHATS_REF + "/" + myUid + "/" + otherUid + "/" + messageId + "/messageText", newContent);
            updates.put("/" + SKYLINE_REF + "/" + CHATS_REF + "/" + otherUid + "/" + myUid + "/" + messageId + "/messageText", newContent);
            updates.put("/" + SKYLINE_REF + "/" + CHATS_REF + "/" + myUid + "/" + otherUid + "/" + messageId + "/isEdited", true);
            updates.put("/" + SKYLINE_REF + "/" + CHATS_REF + "/" + otherUid + "/" + myUid + "/" + messageId + "/isEdited", true);
            return database.getReference().updateChildren(updates);
        } else {
            return ChatManager.updateMessage(currentRoomId, messageId, newContent);
        }
    }
    
    /**
     * Migrate legacy chat to new structure (can be called manually or automatically)
     */
    public Task<String> migrateLegacyChat(String myUid, String otherUid, String myName, String otherName) {
        Log.d(TAG, "Starting migration for chat between " + myUid + " and " + otherUid);
        
        // First create the room
        return ChatManager.getOrCreateDirectRoom(myUid, otherUid, myName, otherName)
            .continueWithTask(task -> {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }
                
                String roomId = task.getResult();
                
                // Then copy messages from legacy structure
                DatabaseReference legacyRef = database.getReference(SKYLINE_REF)
                    .child(CHATS_REF).child(myUid).child(otherUid);
                    
                return legacyRef.get().continueWithTask(dataTask -> {
                    if (!dataTask.isSuccessful()) {
                        throw dataTask.getException();
                    }
                    
                    DataSnapshot snapshot = dataTask.getResult();
                    if (!snapshot.exists()) {
                        return Tasks.forResult(roomId);
                    }
                    
                    Map<String, Object> updates = new HashMap<>();
                    
                    for (DataSnapshot messageSnap : snapshot.getChildren()) {
                        Map<String, Object> legacyMessage = (Map<String, Object>) messageSnap.getValue();
                        if (legacyMessage != null) {
                            // Convert legacy message to new format
                            Map<String, Object> newMessage = new HashMap<>();
                            newMessage.put("id", messageSnap.getKey());
                            newMessage.put("uid", legacyMessage.get("uid"));
                            newMessage.put("type", "text"); // Assume text for legacy
                            newMessage.put("content", legacyMessage.get("messageText"));
                            newMessage.put("created_at", legacyMessage.get("pushDate"));
                            
                            // Copy other fields if they exist
                            if (legacyMessage.containsKey("isEdited")) {
                                newMessage.put("edited_at", legacyMessage.get("pushDate"));
                            }
                            
                            updates.put("/" + DatabaseStructure.CHAT + "/" + DatabaseStructure.ROOMS + "/" + 
                                      roomId + "/" + DatabaseStructure.MESSAGES + "/" + messageSnap.getKey(), newMessage);
                        }
                    }
                    
                    if (updates.isEmpty()) {
                        return Tasks.forResult(roomId);
                    }
                    
                    return database.getReference().updateChildren(updates)
                        .continueWith(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                Log.d(TAG, "Successfully migrated " + updates.size() + " messages");
                                // Optionally delete legacy data
                                // legacyRef.removeValue();
                                return roomId;
                            } else {
                                throw updateTask.getException();
                            }
                        });
                });
            });
    }
    
    public boolean isUsingLegacyMode() {
        return useLegacyMode;
    }
    
    public String getCurrentRoomId() {
        return currentRoomId;
    }
}