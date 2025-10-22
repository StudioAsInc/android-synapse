package com.synapse.social.studioasinc.util;

import androidx.annotation.NonNull;
// Using direct Supabase services - NO Firebase
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService;
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService;
import com.synapse.social.studioasinc.backend.User;
import com.synapse.social.studioasinc.NotificationConfig;
import com.synapse.social.studioasinc.NotificationHelper;
import java.util.HashMap;

public class NotificationUtils {

    public static void sendPostLikeNotification(String postKey, String postAuthorUid) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null) {
            return;
        }
        String currentUid = currentUser.getUid();

        // Get sender's name
        DatabaseReference senderRef = FirebaseDatabase.getInstance().getReference("skyline/users").child(currentUid);
        senderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot senderSnapshot) {
                String senderName = senderSnapshot.child("username").getValue(String.class);
                String message = senderName + " liked your post";

                HashMap<String, String> data = new HashMap<>();
                data.put("postId", postKey);

                NotificationHelper.sendNotification(
                    postAuthorUid,
                    currentUid,
                    message,
                    NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_POST,
                    data
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                android.util.Log.e("NotificationUtils", "Failed to get sender name for post like notification", databaseError.toException());
            }
        });
    }

    public static void sendMentionNotification(String mentionedUid, String postKey, String commentKey, String contentType) {
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser == null || currentUser.getUid().equals(mentionedUid)) {
            return;
        }
        String currentUid = currentUser.getUid();

        DatabaseReference senderRef = FirebaseDatabase.getInstance().getReference("skyline/users").child(currentUid);
        senderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot senderSnapshot) {
                String senderName = senderSnapshot.child("username").getValue(String.class);
                String message = senderName + " mentioned you in a " + contentType;

                HashMap<String, String> data = new HashMap<>();
                data.put("postId", postKey);
                if (commentKey != null) {
                    data.put("commentId", commentKey);
                }

                NotificationHelper.sendNotification(
                    mentionedUid,
                    currentUid,
                    message,
                    NotificationConfig.NOTIFICATION_TYPE_MENTION,
                    data
                );
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                android.util.Log.e("NotificationUtils", "Failed to get sender name for mention notification", databaseError.toException());
            }
        });
    }
}
