package com.synapse.social.studioasinc;

import android.animation.ObjectAnimator;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.synapse.social.studioasinc.util.UserUtils; // Assuming UserUtils provides user data

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.jan_tennert.supabase.SupabaseClient; // Supabase import
import io.github.jan_tennert.supabase.createSupabaseClient;
import io.github.jan_tennert.supabase.postgrest.Postgrest;
import io.github.jan_tennert.supabase.gotrue.Auth;
import io.github.jan_tennert.supabase.realtime.Realtime;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.withContext;

public class ChatsettingsActivity extends AppCompatActivity {

    private SupabaseClient supabase;
    private String currentUserId = "";
    private String chatPartnerUid = "";
    private String chatId = ""; // For single chats, this would be a combination of two UIDs
    private String chatType = ""; // "single" or "group"
    private String chatPartnerUsername = "";
    private String chatPartnerProfile = "";

    private ImageView back;
    private TextView block_text;
    private RelativeLayout block_btn;
    private RelativeLayout mute_btn;
    private SwitchCompat mute_switch;
    private RelativeLayout clear_chat_btn;
    private RelativeLayout delete_chat_btn;
    private TextView partner_username_tv;
    private TextView partner_profile_tv;
    private ImageView partner_profile_image;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.chatsettings);
        initialize();
        initializeSupabase();
        initializeLogic();
    }

    private void initialize() {
        back = findViewById(R.id.back);
        block_text = findViewById(R.id.block_text);
        block_btn = findViewById(R.id.block_btn);
        mute_btn = findViewById(R.id.mute_btn);
        mute_switch = findViewById(R.id.mute_switch);
        clear_chat_btn = findViewById(R.id.clear_chat_btn);
        delete_chat_btn = findViewById(R.id.delete_chat_btn);
        partner_username_tv = findViewById(R.id.partner_username_tv);
        partner_profile_tv = findViewById(R.id.partner_profile_tv);
        partner_profile_image = findViewById(R.id.partner_profile_image);

        back.setOnClickListener(v -> finish());
        block_btn.setOnClickListener(v -> handleBlockClick());
        mute_switch.setOnCheckedChangeListener((buttonView, isChecked) -> toggleChatNotifications(isChecked));
        clear_chat_btn.setOnClickListener(v -> confirmClearChat());
        delete_chat_btn.setOnClickListener(v -> confirmDeleteChat());
    }

    private void initializeSupabase() {
        supabase = createSupabaseClient(
                "YOUR_SUPABASE_URL", // Replace with your Supabase URL
                "YOUR_SUPABASE_ANON_KEY", // Replace with your Supabase anon key
                builder -> {
                    builder.install(new Auth());
                    builder.install(new Postgrest());
                    builder.install(new Realtime());
                    return null;
                }
        );

        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                io.github.jan_tennert.supabase.gotrue.User user = supabase.getAuth().getCurrentUser();
                if (user != null) {
                    currentUserId = user.getId();
                } else {
                    withContext(Dispatchers.getMain(), () -> {
                        Toast.makeText(this, "User not authenticated. Please log in again.", Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            } catch (Exception e) {
                Log.e("ChatsettingsActivity", "Error getting current user: " + e.getMessage());
                withContext(Dispatchers.getMain(), () -> {
                    Toast.makeText(this, "Error fetching user data.", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });

        // Get chat details from intent
        chatPartnerUid = getIntent().getStringExtra("uid"); // For single chat, this is the other user's UID
        chatId = getIntent().getStringExtra("chatId"); // Combined UID string or Group ID
        chatType = getIntent().getStringExtra("chatType"); // "single" or "group"
        chatPartnerUsername = getIntent().getStringExtra("username");
        chatPartnerProfile = getIntent().getStringExtra("profile_url");
    }

    private void initializeLogic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = this.getWindow();
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        partner_username_tv.setText(chatPartnerUsername);
        // partner_profile_tv.setText("View Profile"); // TODO: Link to user profile if available
        if (chatPartnerProfile != null && !chatPartnerProfile.isEmpty()) {
            Glide.with(getApplicationContext()).load(chatPartnerProfile).into(partner_profile_image);
        } else {
            partner_profile_image.setImageResource(R.drawable.default_profile_pic); // Placeholder
        }

        updateBlockButtonState();
        fetchNotificationSetting();
    }

    private void updateBlockButtonState() {
        // TODO: Check if currentUserId has blocked chatPartnerUid using Supabase
        // Example:
        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                // Assuming a 'blocks' table with 'blocker_id' and 'blocked_id'
                Postgrest.FilterBuilder filter = supabase.getPostgrest().from("blocks")
                        .select("id")
                        .eq("blocker_id", currentUserId)
                        .eq("blocked_id", chatPartnerUid);
                List<Map<String, Object>> result = filter.execute().decodeList(Map.class);

                withContext(Dispatchers.getMain(), () -> {
                    if (!result.isEmpty()) {
                        block_text.setText("Unblock " + chatPartnerUsername);
                        block_btn.setBackgroundColor(Color.parseColor("#FFC107")); // Example color for unblock
                    } else {
                        block_text.setText("Block " + chatPartnerUsername);
                        block_btn.setBackgroundColor(Color.parseColor("#FF0000")); // Example color for block
                    }
                });
            } catch (Exception e) {
                Log.e("ChatsettingsActivity", "Error checking block status: " + e.getMessage());
                withContext(Dispatchers.getMain(), () -> {
                    Toast.makeText(this, "Error checking block status.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void fetchNotificationSetting() {
        // TODO: Fetch chat notification setting for this chat from Supabase (e.g., from 'user_chat_settings' table)
        // Example:
        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                Postgrest.FilterBuilder filter = supabase.getPostgrest().from("user_chat_settings")
                        .select("notifications_muted")
                        .eq("user_id", currentUserId)
                        .eq("chat_id", chatId);
                List<Map<String, Object>> result = filter.execute().decodeList(Map.class);

                withContext(Dispatchers.getMain(), () -> {
                    if (!result.isEmpty()) {
                        boolean isMuted = (Boolean) result.get(0).get("notifications_muted");
                        mute_switch.setChecked(isMuted);
                    } else {
                        mute_switch.setChecked(false); // Default to not muted
                    }
                });
            } catch (Exception e) {
                Log.e("ChatsettingsActivity", "Error fetching notification setting: " + e.getMessage());
                withContext(Dispatchers.getMain(), () -> {
                    Toast.makeText(this, "Error fetching notification setting.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    private void handleBlockClick() {
        // Determine if user is currently blocked or not
        if (block_text.getText().toString().startsWith("Unblock")) {
            unblockUser();
        } else {
            blockUser();
        }
    }

    private void blockUser() {
        new AlertDialog.Builder(this)
                .setTitle("Block User")
                .setMessage("Are you sure you want to block " + chatPartnerUsername + "? You will no longer receive messages or calls from them.")
                .setPositiveButton("Block", (dialog, which) -> {
                    // TODO: Implement blocking user in Supabase (e.g., insert into 'blocks' table)
                    CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
                    scope.launch(() -> {
                        try {
                            Map<String, Object> blockEntry = new HashMap<>();
                            blockEntry.put("blocker_id", currentUserId);
                            blockEntry.put("blocked_id", chatPartnerUid);
                            supabase.getPostgrest().from("blocks").insert(blockEntry).execute();

                            withContext(Dispatchers.getMain(), () -> {
                                Toast.makeText(this, chatPartnerUsername + " blocked.", Toast.LENGTH_SHORT).show();
                                updateBlockButtonState();
                            });
                        } catch (Exception e) {
                            Log.e("ChatsettingsActivity", "Error blocking user: " + e.getMessage());
                            withContext(Dispatchers.getMain(), () -> {
                                Toast.makeText(this, "Failed to block user.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void unblockUser() {
        new AlertDialog.Builder(this)
                .setTitle("Unblock User")
                .setMessage("Are you sure you want to unblock " + chatPartnerUsername + "?")
                .setPositiveButton("Unblock", (dialog, which) -> {
                    // TODO: Implement unblocking user in Supabase (e.g., delete from 'blocks' table)
                    CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
                    scope.launch(() -> {
                        try {
                            supabase.getPostgrest().from("blocks")
                                    .delete()
                                    .eq("blocker_id", currentUserId)
                                    .eq("blocked_id", chatPartnerUid)
                                    .execute();

                            withContext(Dispatchers.getMain(), () -> {
                                Toast.makeText(this, chatPartnerUsername + " unblocked.", Toast.LENGTH_SHORT).show();
                                updateBlockButtonState();
                            });
                        } catch (Exception e) {
                            Log.e("ChatsettingsActivity", "Error unblocking user: " + e.getMessage());
                            withContext(Dispatchers.getMain(), () -> {
                                Toast.makeText(this, "Failed to unblock user.", Toast.LENGTH_SHORT).show();
                            });
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void toggleChatNotifications(boolean isChecked) {
        // TODO: Update chat notification preference in Supabase (e.g., update 'user_chat_settings' table)
        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                Map<String, Object> update = new HashMap<>();
                update.put("notifications_muted", isChecked);

                supabase.getPostgrest().from("user_chat_settings")
                        .update(update)
                        .eq("user_id", currentUserId)
                        .eq("chat_id", chatId)
                        .execute(); // Upsert might be better here if the entry might not exist

                withContext(Dispatchers.getMain(), () -> {
                    Toast.makeText(this, "Notifications " + (isChecked ? "muted" : "unmuted"), Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("ChatsettingsActivity", "Error updating notification settings: " + e.getMessage());
                withContext(Dispatchers.getMain(), () -> {
                    Toast.makeText(this, "Failed to update notification settings.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void confirmClearChat() {
        new AlertDialog.Builder(this)
                .setTitle("Clear Chat")
                .setMessage("Are you sure you want to clear all messages in this chat? This cannot be undone.")
                .setPositiveButton("Clear", (dialog, which) -> clearChatMessages())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearChatMessages() {
        // TODO: Delete chat messages for this chat_id in Supabase
        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                supabase.getPostgrest().from("chats")
                        .delete()
                        .eq("chat_id", chatId)
                        .execute();

                withContext(Dispatchers.getMain(), () -> {
                    Toast.makeText(this, "Chat cleared.", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                Log.e("ChatsettingsActivity", "Error clearing chat: " + e.getMessage());
                withContext(Dispatchers.getMain(), () -> {
                    Toast.makeText(this, "Failed to clear chat.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void confirmDeleteChat() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Chat")
                .setMessage("Are you sure you want to delete this chat? This will remove it from your inbox and cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteChat())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteChat() {
        // TODO: Delete chat from current user's inbox in Supabase.
        // For single chat, this might mean deleting the 'user_chats' entry for current user and the 'inbox' entry.
        // For group chat, it might mean removing the user from the group's member list or deleting 'user_groups' entry.
        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                // Delete the inbox entry for the current user for this chat
                supabase.getPostgrest().from("inbox")
                        .delete()
                        .eq("user_id", currentUserId)
                        .eq("chat_id", chatId)
                        .execute();

                // If it's a single chat, also delete the user_chats entry
                if (chatType.equals("single")) {
                    supabase.getPostgrest().from("user_chats")
                            .delete()
                            .eq("user_id", currentUserId)
                            .eq("chat_id", chatId)
                            .execute();
                }
                // If it's a group chat, consider removing the user from the group members list
                // This might be more complex depending on your group management logic.

                withContext(Dispatchers.getMain(), () -> {
                    Toast.makeText(this, "Chat deleted.", Toast.LENGTH_SHORT).show();
                    finish(); // Go back after deleting the chat
                });
            } catch (Exception e) {
                Log.e("ChatsettingsActivity", "Error deleting chat: " + e.getMessage());
                withContext(Dispatchers.getMain(), () -> {
                    Toast.makeText(this, "Failed to delete chat.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
}
