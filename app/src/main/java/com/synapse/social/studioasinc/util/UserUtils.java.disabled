package com.synapse.social.studioasinc.util;

// Using direct Supabase services - NO Firebase
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService;

public class UserUtils {
    
    public interface Callback<T> {
        void onSuccess(T result);
        void onError(String error);
    }
    
    public static void getUserDisplayName(String userId, final Callback<String> callback) {
        if (userId == null || userId.isEmpty()) {
            callback.onError("Invalid user ID");
            return;
        }
        
        FirebaseDatabase.getInstance()
                .getReference("skyline/users")
                .child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            String nickname = dataSnapshot.child("nickname").getValue(String.class);
                            String username = dataSnapshot.child("username").getValue(String.class);
                            
                            // Return nickname if available, otherwise username
                            String displayName = (nickname != null && !nickname.isEmpty()) ? nickname : username;
                            
                            if (displayName != null) {
                                callback.onSuccess(displayName);
                            } else {
                                callback.onError("User not found");
                            }
                        } else {
                            callback.onError("User not found");
                        }
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        callback.onError(databaseError.getMessage());
                    }
                });
    }
}