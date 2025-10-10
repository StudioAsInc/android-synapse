package com.synapse.social.studioasinc.util;

import androidx.annotation.NonNull;
// TODO(supabase): Add Supabase imports for GoTrueClient and PostgrestClient
// import io.supabase.gotrue.GoTrueClient;
// import io.supabase.postgrest.PostgrestClient;
import com.synapse.social.studioasinc.NotificationConfig;
import com.synapse.social.studioasinc.NotificationHelper;
import java.util.HashMap;

public class NotificationUtils {

    public static void sendPostLikeNotification(String postKey, String postAuthorUid, GoTrueClient goTrueClient, PostgrestClient postgrestClient) {
        // TODO(supabase): Get current user from Supabase Auth
        /*
        goTrueClient.getCurrentUser().thenAccept(currentUser -> {
            if (currentUser == null) {
                return;
            }
            String currentUid = currentUser.getId();

            // Get sender's name from Supabase
            postgrestClient.from("users").select("username").eq("uid", currentUid).single().thenAccept(response -> {
                if (response.getStatus() == 200) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.getData());
                        String senderName = jsonObject.optString("username");
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
                    } catch (JSONException e) {
                        android.util.Log.e("NotificationUtils", "Error parsing sender data for post like notification", e);
                    }
                } else {
                    android.util.Log.e("NotificationUtils", "Failed to get sender name for post like notification: " + response.getStatus());
                }
            }).exceptionally(e -> {
                android.util.Log.e("NotificationUtils", "Supabase query failed for post like notification", e);
                return null;
            });
        }).exceptionally(e -> {
            android.util.Log.e("NotificationUtils", "Supabase Auth failed for post like notification", e);
            return null;
        });
        */
        // Placeholder for now
        String currentUid = "TODO_SUPABASE_CURRENT_UID";
        String senderName = "TODO_SUPABASE_SENDER_NAME";
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

    public static void sendMentionNotification(String mentionedUid, String postKey, String commentKey, String contentType, GoTrueClient goTrueClient, PostgrestClient postgrestClient) {
        // TODO(supabase): Get current user from Supabase Auth
        /*
        goTrueClient.getCurrentUser().thenAccept(currentUser -> {
            if (currentUser == null || currentUser.getId().equals(mentionedUid)) {
                return;
            }
            String currentUid = currentUser.getId();

            // Get sender's name from Supabase
            postgrestClient.from("users").select("username").eq("uid", currentUid).single().thenAccept(response -> {
                if (response.getStatus() == 200) {
                    try {
                        JSONObject jsonObject = new JSONObject(response.getData());
                        String senderName = jsonObject.optString("username");
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
                    } catch (JSONException e) {
                        android.util.Log.e("NotificationUtils", "Error parsing sender data for mention notification", e);
                    }
                } else {
                    android.util.Log.e("NotificationUtils", "Failed to get sender name for mention notification: " + response.getStatus());
                }
            }).exceptionally(e -> {
                android.util.Log.e("NotificationUtils", "Supabase query failed for mention notification", e);
                return null;
            });
        }).exceptionally(e -> {
            android.util.Log.e("NotificationUtils", "Supabase Auth failed for mention notification", e);
            return null;
        });
        */
        // Placeholder for now
        String currentUid = "TODO_SUPABASE_CURRENT_UID";
        String senderName = "TODO_SUPABASE_SENDER_NAME";
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
}
