package com.synapse.social.studioasinc.util;

import androidx.annotation.NonNull;

import com.synapse.social.studioasinc.NotificationConfig;
import com.synapse.social.studioasinc.NotificationHelper;

import java.util.HashMap;
import java.util.Map;

import io.github.jan.supabase.gotrue.auth;
import kotlinx.coroutines.GlobalScope;
import kotlinx.coroutines.launch;

public class NotificationUtils {

    public static void sendPostLikeNotification(String postKey, String postAuthorUid) {
        String currentUid = SupabaseManager.INSTANCE.getClient().getAuth().getCurrentUserOrNull().getId();
        if (currentUid == null) {
            return;
        }

        GlobalScope.launch(() -> {
            try {
                Map<String, Object> sender = SupabaseManager.INSTANCE.getUser(currentUid);
                if (sender != null) {
                    String senderName = (String) sender.get("username");
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
            } catch (Exception e) {
                android.util.Log.e("NotificationUtils", "Failed to get sender name for post like notification", e);
            }
        });
    }

    public static void sendMentionNotification(String mentionedUid, String postKey, String commentKey, String contentType) {
        String currentUid = SupabaseManager.INSTANCE.getClient().getAuth().getCurrentUserOrNull().getId();
        if (currentUid == null || currentUid.equals(mentionedUid)) {
            return;
        }

        GlobalScope.launch(() -> {
            try {
                Map<String, Object> sender = SupabaseManager.INSTANCE.getUser(currentUid);
                if (sender != null) {
                    String senderName = (String) sender.get("username");
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
            } catch (Exception e) {
                android.util.Log.e("NotificationUtils", "Failed to get sender name for mention notification", e);
            }
        });
    }
}