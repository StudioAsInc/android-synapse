package com.synapse.social.studioasinc.util;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class UserUtils {

    public interface Callback<T> {
        void onResult(T result);
    }

    public static void getUserDisplayName(String uid, final Callback<String> callback) {
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("skyline/users").child(uid);
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String nickname = dataSnapshot.child("nickname").getValue(String.class);
                    if (nickname != null && !nickname.equals("null")) {
                        callback.onResult(nickname);
                    } else {
                        callback.onResult("@" + dataSnapshot.child("username").getValue(String.class));
                    }
                } else {
                    callback.onResult("Someone");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                callback.onResult("Someone");
            }
        });
    }

    public static void getCurrentUserDisplayName(final Callback<String> callback) {
        String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        getUserDisplayName(currentUserUid, callback);
    }
}
