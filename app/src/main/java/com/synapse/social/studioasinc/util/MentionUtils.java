package com.synapse.social.studioasinc.util;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;

// TODO(supabase): Add Supabase imports for PostgREST and Realtime
// import io.supabase.postgrest.PostgrestClient;
// import io.supabase.realtime.RealtimeClient;
import com.synapse.social.studioasinc.ProfileActivity;
import com.synapse.social.studioasinc.R;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class MentionUtils {

    public static void handleMentions(Context context, TextView textView, String text, SupabaseClient supabaseClient) {
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
                        // TODO(supabase): Implement with Supabase PostgREST
                        /*
                        supabaseClient.postgrest["users"]
                                .select("uid")
                                .eq("username", username)
                                .single()
                                .thenAccept(response -> {
                                    if (response.getStatus() == 200) {
                                        try {
                                            JSONObject jsonObject = new JSONObject(response.getData());
                                            String uid = jsonObject.optString("uid");
                                            if (uid != null) {
                                                Intent intent = new Intent(context, ProfileActivity.class);
                                                intent.putExtra("uid", uid);
                                                context.startActivity(intent);
                                            }
                                        } catch (JSONException e) {
                                            // Handle error
                                        }
                                    } else {
                                        // Handle error
                                    }
                                })
                                .exceptionally(e -> {
                                    // Handle error
                                    return null;
                                });
                        */
                        // Placeholder for now
                        Intent intent = new Intent(context, ProfileActivity.class);
                        intent.putExtra("uid", "TODO_SUPABASE_UID");
                        context.startActivity(intent);
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

    public static void sendMentionNotifications(String text, String postKey, String commentKey, String contentType, SupabaseClient supabaseClient) {
        if (text == null) return;

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

        // TODO(supabase): Implement with Supabase PostgREST
        /*
        supabaseClient.postgrest["users"]
                .select("uid,username")
                .in("username", mentionedUsernames.toArray(new String[0]))
                .thenAccept(response -> {
                    if (response.getStatus() == 200) {
                        try {
                            JSONArray jsonArray = new JSONArray(response.getData());
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject userObject = jsonArray.getJSONObject(i);
                                String uid = userObject.optString("uid");
                                String username = userObject.optString("username");
                                if (uid != null && mentionedUsernames.contains(username)) {
                                    NotificationUtils.sendMentionNotification(uid, postKey, commentKey, contentType);
                                }
                            }
                        } catch (JSONException e) {
                            // Handle error
                        }
                    } else {
                        // Handle error
                    }
                })
                .exceptionally(e -> {
                    // Handle error
                    return null;
                });
        */
        // Placeholder for now
        for (String username : mentionedUsernames) {
            NotificationUtils.sendMentionNotification("TODO_SUPABASE_UID_FOR_" + username, postKey, commentKey, contentType);
        }
    }
}
