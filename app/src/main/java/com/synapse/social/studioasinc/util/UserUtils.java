package com.synapse.social.studioasinc.util;

import java.util.Map;
import kotlinx.coroutines.GlobalScope;
import kotlinx.coroutines.launch;

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

        GlobalScope.launch(() -> {
            try {
                Map<String, Object> user = SupabaseManager.INSTANCE.getUser(userId);
                if (user != null) {
                    String nickname = (String) user.get("nickname");
                    String username = (String) user.get("username");

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
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        });
    }
}