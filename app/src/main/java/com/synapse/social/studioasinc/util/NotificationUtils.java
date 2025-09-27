package com.synapse.social.studioasinc.util;

import android.util.Log;

import com.synapse.social.studioasinc.NotificationConfig;
import com.synapse.social.studioasinc.NotificationHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.jan_tennert.supabase.SupabaseClient;
import io.github.jan_tennert.supabase.postgrest.Postgrest;
import io.github.jan_tennert.supabase.gotrue.Auth;
import io.github.jan_tennert.supabase.gotrue.User;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.withContext;

public class NotificationUtils {

    // The SupabaseClient should be provided, typically from a singleton or DI framework.
    // We will assume it is set via the same mechanism as UserUtils.
    private static SupabaseClient _supabaseClient;

    public static void setSupabaseClient(SupabaseClient client) {
        _supabaseClient = client;
    }

    public static void sendPostLikeNotification(String postKey, String postAuthorUid) {
        if (_supabaseClient == null) {
            Log.e("NotificationUtils", "Supabase client is not initialized.");
            return;
        }

        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                User currentUser = _supabaseClient.getAuth().getCurrentUser();
                if (currentUser == null) {
                    return;
                }
                String currentUid = currentUser.getId();

                // Fetch sender's name (username) from Supabase 'users' table
                _supabaseClient.getPostgrest().from("users")
                        .select("username")
                        .eq("id", currentUid)
                        .limit(1)
                        .execute()
                        .thenAccept(response -> {
                            List<Map<String, Object>> userList = response.decodeList(Map.class);
                            if (!userList.isEmpty()) {
                                String senderName = (String) userList.get(0).get("username");
                                String message = senderName + " liked your post";

                                HashMap<String, String> data = new HashMap<>();
                                data.put("postId", postKey);

                                // Assuming NotificationHelper is migrated and handles sending logic
                                NotificationHelper.sendNotification(
                                        postAuthorUid,
                                        currentUid,
                                        message,
                                        NotificationConfig.NOTIFICATION_TYPE_NEW_LIKE_POST,
                                        data
                                );
                            }
                        })
                        .exceptionally(e -> {
                            Log.e("NotificationUtils", "Failed to get sender name for post like notification", e);
                            return null;
                        });

            } catch (Exception e) {
                Log.e("NotificationUtils", "Error in sendPostLikeNotification: " + e.getMessage());
            }
        });
    }

    public static void sendMentionNotification(String mentionedUid, String postKey, String commentKey, String contentType) {
        if (_supabaseClient == null) {
            Log.e("NotificationUtils", "Supabase client is not initialized.");
            return;
        }

        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                User currentUser = _supabaseClient.getAuth().getCurrentUser();
                if (currentUser == null || currentUser.getId().equals(mentionedUid)) {
                    return;
                }
                String currentUid = currentUser.getId();

                // Fetch sender's name (username) from Supabase 'users' table
                _supabaseClient.getPostgrest().from("users")
                        .select("username")
                        .eq("id", currentUid)
                        .limit(1)
                        .execute()
                        .thenAccept(response -> {
                            List<Map<String, Object>> userList = response.decodeList(Map.class);
                            if (!userList.isEmpty()) {
                                String senderName = (String) userList.get(0).get("username");
                                String message = senderName + " mentioned you in a " + contentType;

                                HashMap<String, String> data = new HashMap<>();
                                data.put("postId", postKey);
                                if (commentKey != null) {
                                    data.put("commentId", commentKey);
                                }

                                // Assuming NotificationHelper is migrated and handles sending logic
                                NotificationHelper.sendNotification(
                                        mentionedUid,
                                        currentUid,
                                        message,
                                        NotificationConfig.NOTIFICATION_TYPE_MENTION,
                                        data
                                );
                            }
                        })
                        .exceptionally(e -> {
                            Log.e("NotificationUtils", "Failed to get sender name for mention notification", e);
                            return null;
                        });

            } catch (Exception e) {
                Log.e("NotificationUtils", "Error in sendMentionNotification: " + e.getMessage());
            }
        });
    }
}
