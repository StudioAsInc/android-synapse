package com.synapse.social.studioasinc.util;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.synapse.social.studioasinc.ProfileActivity;
import com.synapse.social.studioasinc.R;
import com.synapse.social.studioasinc.util.NotificationUtils; // Already migrated

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.github.jan_tennert.supabase.SupabaseClient;
import io.github.jan_tennert.supabase.postgrest.Postgrest;
import io.github.jan_tennert.supabase.postgrest.PostgrestResult;
import io.github.jan_tennert.supabase.postgrest.query.Columns;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.withContext;

public class MentionUtils {

    private static SupabaseClient _supabaseClient;

    public static void setSupabaseClient(SupabaseClient client) {
        _supabaseClient = client;
    }

    public static void handleMentions(Context context, TextView textView, String text) {
        SpannableString spannableString = new SpannableString(text);
        Pattern pattern = Pattern.compile("@(\\w+)");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            String username = matcher.group(1);
            if (username != null) {
                int start = matcher.start();
                int end = matcher.end();

                ClickableSpan clickableSpan = new ClickableSpan() {
                    @Override
                    public void onClick(View widget) {
                        if (_supabaseClient == null) {
                            Log.e("MentionUtils", "Supabase client not initialized.");
                            return;
                        }

                        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
                        scope.launch(() -> {
                            try {
                                PostgrestResult response = _supabaseClient.getPostgrest().from("users")
                                        .select(Columns.list("id")) // Select only the ID
                                        .eq("username", username)
                                        .limit(1)
                                        .execute();

                                List<HashMap<String, Object>> users = response.decodeList(HashMap.class);

                                if (!users.isEmpty()) {
                                    String uid = (String) users.get(0).get("id"); // Get UID from the map
                                    if (uid != null) {
                                        withContext(Dispatchers.getMain(), () -> {
                                            Intent intent = new Intent(context, ProfileActivity.class);
                                            intent.putExtra("uid", uid);
                                            context.startActivity(intent);
                                        });
                                    }
                                } else {
                                    Log.w("MentionUtils", "User not found for mention: " + username);
                                }
                            } catch (Exception e) {
                                Log.e("MentionUtils", "Error fetching user for mention: " + e.getMessage());
                            }
                        });
                    }

                    @Override
                    public void updateDrawState(android.text.TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setUnderlineText(false);
                        ds.setColor(context.getColor(R.color.md_theme_primary));
                    }
                };
                spannableString.setSpan(clickableSpan, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        textView.setText(spannableString);
        textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
    }

    public static void sendMentionNotifications(String text, String postKey, String commentKey, String contentType) {
        if (text == null || _supabaseClient == null) {
            if (_supabaseClient == null) Log.e("MentionUtils", "Supabase client not initialized for notifications.");
            return;
        }

        Pattern pattern = Pattern.compile("@(\\w+)");
        Matcher matcher = pattern.matcher(text);

        ArrayList<String> mentionedUsernames = new ArrayList<>();
        while (matcher.find()) {
            String username = matcher.group(1);
            if (username != null && !mentionedUsernames.contains(username)) {
                mentionedUsernames.add(username);
            }
        }

        if (mentionedUsernames.isEmpty()) {
            return;
        }

        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                // Fetch UIDs for all mentioned usernames in one query
                PostgrestResult response = _supabaseClient.getPostgrest().from("users")
                        .select(Columns.list("id", "username"))
                        .filter("username.in", mentionedUsernames) // Supabase 'in' filter
                        .execute();

                List<HashMap<String, Object>> usersData = response.decodeList(HashMap.class);

                for (HashMap<String, Object> userData : usersData) {
                    String uid = (String) userData.get("id");
                    String username = (String) userData.get("username");
                    if (uid != null) {
                        // Use the already migrated NotificationUtils to send notification
                        NotificationUtils.sendMentionNotification(uid, postKey, commentKey, contentType);
                    }
                }
            } catch (Exception e) {
                Log.e("MentionUtils", "Error fetching users for mention notifications: " + e.getMessage());
            }
        });
    }
}
