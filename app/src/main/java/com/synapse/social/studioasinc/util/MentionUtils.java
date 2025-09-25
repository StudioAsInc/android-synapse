package com.synapse.social.studioasinc.util;

import android.content.Context;
import android.content.Intent;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.synapse.social.studioasinc.ProfileActivity;
import com.synapse.social.studioasinc.R;
import com.synapse.social.studioasinc.backend.DatabaseService;
import com.synapse.social.studioasinc.backend.QueryService;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.ArrayList;

public class MentionUtils {

    public static void handleMentions(Context context, TextView textView, String text) {
        DatabaseService dbService = new DatabaseService();
        QueryService queryService = new QueryService(dbService);
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
                    public void onClick(@NonNull View widget) {
                        queryService.fetchWithOrder("skyline/users", "username", username, new DatabaseService.DataListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                                if (dataSnapshot.exists()) {
                                    for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                                        String uid = userSnapshot.getKey();
                                        if (uid != null) {
                                            Intent intent = new Intent(context, ProfileActivity.class);
                                            intent.putExtra("uid", uid);
                                            context.startActivity(intent);
                                            break;
                                        }
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                // Handle error
                            }
                        });
                    }

                    @Override
                    public void updateDrawState(@NonNull android.text.TextPaint ds) {
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
        if (text == null) return;
        DatabaseService dbService = new DatabaseService();
        QueryService queryService = new QueryService(dbService);

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

        for (String username : mentionedUsernames) {
            queryService.fetchWithOrder("skyline/users", "username", username, new DatabaseService.DataListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        for (DataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                            String uid = userSnapshot.getKey();
                            if (uid != null) {
                                NotificationUtils.sendMentionNotification(uid, postKey, commentKey, contentType);
                                break;
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                }
            });
        }
    }
}