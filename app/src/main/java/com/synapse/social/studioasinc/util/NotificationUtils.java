package com.synapse.social.studioasinc.util;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
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
}
