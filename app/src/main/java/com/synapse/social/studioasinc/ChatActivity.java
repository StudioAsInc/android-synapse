package com.synapse.social.studioasinc;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.vanniktech.emoji.EmojiEditText;
import com.vanniktech.emoji.EmojiPopup;

import com.synapse.social.studioasinc.util.ChatMessageManager; // Import the migrated ChatMessageManager

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;

import io.github.jan_tennert.supabase.SupabaseClient;
import io.github.jan_tennert.supabase.createSupabaseClient;
import io.github.jan_tennert.supabase.postgrest.Postgrest;
import io.github.jan_tennert.supabase.gotrue.Auth;
import io.github.jan_tennert.supabase.realtime.Realtime;
import io.github.jan_tennert.supabase.realtime.PostgresAction;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.withContext;

public class ChatActivity extends AppCompatActivity {

    private SupabaseClient supabase;
    private ChatMessageManager chatMessageManager;

    private ArrayList<HashMap<String, Object>> chat_list_map = new ArrayList<>();
    private String chatKey = "";
    private String chat_id = ""; // This is the combined UID chat_id (e.g., uid1uid2)
    private String chatPartnerUid = ""; // The UID of the other person in the chat
    private String username = ""; // Partner's username
    private String userProfile = ""; // Partner's profile URL
    private String currentUid = "";
    private String currentUserUsername = "";
    private String currentUserProfile = "";
    private String message_type = "";
    private String _message_text = "";
    private boolean isVisible = false;
    private String chatType = "";
    private String oneSignalUserId = "";

    private LinearLayout linear1;
    private RelativeLayout toolbar;
    private ImageView back;
    private TextView username_tv;
    private ImageView profile_image;
    private ImageView emoji_button;
    private EmojiEditText edittext_message;
    private ImageView send_button;
    private LinearLayout message_container;
    private ListView chat_listview;
    private LinearLayout linear4;
    private ProgressBar progressbar1;
    private ImageView options;
    private LinearLayout message_view;

    private EmojiPopup emojiPopup;
    private Calendar cal = Calendar.getInstance();
    private ObjectAnimator animator = new ObjectAnimator();
    private RequestNetwork requestNetwork;
    private RequestNetwork.RequestListener _request_network_listener;
    private HashMap<String, Object> map = new HashMap<>();

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.chat);
        initialize(_savedInstanceState);
        initializeSupabase();
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        linear1 = findViewById(R.id.linear1);
        toolbar = findViewById(R.id.toolbar);
        back = findViewById(R.id.back);
        username_tv = findViewById(R.id.username_tv);
        profile_image = findViewById(R.id.profile_image);
        emoji_button = findViewById(R.id.emoji_button);
        edittext_message = findViewById(R.id.edittext_message);
        send_button = findViewById(R.id.send_button);
        message_container = findViewById(R.id.message_container);
        chat_listview = findViewById(R.id.chat_listview);
        linear4 = findViewById(R.id.linear4);
        progressbar1 = findViewById(R.id.progressbar1);
        options = findViewById(R.id.options);
        message_view = findViewById(R.id.message_view);

        requestNetwork = new RequestNetwork(this);

        back.setOnClickListener(v -> finish());

        send_button.setOnClickListener(v -> sendMessage());

        emoji_button.setOnClickListener(v -> toggleEmojiPopup());

        options.setOnClickListener(v -> showOptions());

        edittext_message.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before: int count) {
                if (edittext_message.getText().toString().length() > 0) {
                    send_button.setColorFilter(Color.parseColor("#FFC107"));
                } else {
                    send_button.setColorFilter(Color.parseColor("#BDBDBD"));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        _request_network_listener = new RequestNetwork.RequestListener() {
            @Override
            public void onResponse(String tag, String response, HashMap<String, Object> responseHeaders) {
            }

            @Override
            public void onErrorResponse(String tag, String message) {
            }
        };
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
        chatMessageManager = new ChatMessageManager(supabase); // Initialize ChatMessageManager

        // Asynchronously get current user ID and username from Supabase Auth
        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                io.github.jan_tennert.supabase.gotrue.User user = supabase.getAuth().getCurrentUser();
                if (user != null) {
                    currentUid = user.getId();
                    currentUserUsername = (String) user.getUserMetadata().get("username");
                    currentUserProfile = (String) user.getUserMetadata().get("profile_url");

                    // Get chat_id from intent
                    chat_id = getIntent().getStringExtra("chat_id");
                    if (chat_id == null || chat_id.isEmpty()) {
                        Log.e("ChatActivity", "Chat ID is null or empty in intent.");
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(ChatActivity.this, "Chat ID missing. Cannot start chat.", Toast.LENGTH_LONG).show();
                            finish();
                        });
                        return;
                    }

                    // Determine chatPartnerUid from chat_id (which is a combination of two UIDs)
                    if (chat_id.startsWith(currentUid)) {
                        chatPartnerUid = chat_id.substring(currentUid.length());
                    } else if (chat_id.endsWith(currentUid)) {
                        chatPartnerUid = chat_id.substring(0, chat_id.length() - currentUid.length());
                    } else {
                        Log.e("ChatActivity", "Cannot parse chat_id to find partner UID. Chat ID: " + chat_id + ", Current UID: " + currentUid);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            Toast.makeText(ChatActivity.this, "Invalid chat ID format.", Toast.LENGTH_LONG).show();
                            finish();
                        });
                        return;
                    }

                    // Now that currentUid is available, set the adapter on the main thread
                    new Handler(Looper.getMainLooper()).post(() -> {
                        chat_listview.setAdapter(new ChatAdapter(ChatActivity.this, chat_list_map, currentUid));
                    });

                    // Setup Supabase Realtime listener for chat messages
                    supabase.getRealtime().from("chats:chat_id=eq." + chat_id).on(PostgresAction.Insert.class, action -> {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            try {
                                HashMap<String, Object> newMessage = action.decode(HashMap.class);
                                chat_list_map.add(newMessage);
                                ((ChatAdapter) chat_list_view.getAdapter()).notifyDataSetChanged();
                                chat_list_view.smoothScrollToPosition(chat_list_map.size() - 1);
                            } catch (Exception e) {
                                Log.e("ChatActivity", "Error decoding realtime message: " + e.getMessage());
                            }
                        });
                    }).subscribe();


                    // Fetch initial chat messages from Supabase Postgrest
                    supabase.getPostgrest().from("chats")
                            .select("*")
                            .eq("chat_id", chat_id)
                            .order("timestamp", true) // Order by timestamp ascending
                            .limit(50)
                            .execute()
                            .thenAccept(response -> {
                                List<HashMap<String, Object>> messages = response.decodeList(HashMap.class);
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    chat_list_map.clear();
                                    chat_list_map.addAll(messages);
                                    ((ChatAdapter) chat_list_view.getAdapter()).notifyDataSetChanged();
                                    chat_list_view.smoothScrollToPosition(chat_list_map.size() - 1);
                                    progressbar1.setVisibility(View.GONE);
                                });
                            })
                            .exceptionally(e -> {
                                Log.e("Supabase", "Error fetching messages: " + e.getMessage());
                                new Handler(Looper.getMainLooper()).post(() -> {
                                    Toast.makeText(ChatActivity.this, "Failed to load messages.", Toast.LENGTH_LONG).show();
                                    progressbar1.setVisibility(View.GONE);
                                });
                                return null;
                            });

                } else {
                    Log.e("ChatActivity", "User not authenticated for chat.");
                    new Handler(Looper.getMainLooper()).post(() -> {
                        Toast.makeText(ChatActivity.this, "User not authenticated. Please log in again.", Toast.LENGTH_LONG).show();
                        finish();
                    });
                }
            } catch (Exception e) {
                Log.e("ChatActivity", "Error getting current user for chat: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    Toast.makeText(ChatActivity.this, "Error fetching user data.", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        });

        // Other intent extras
        chatKey = getIntent().getStringExtra("chatKey"); // Not directly used with Supabase chat_id logic for direct messages
        username = getIntent().getStringExtra("username");
        userProfile = getIntent().getStringExtra("profile");
        chatType = getIntent().getStringExtra("chatType");
        oneSignalUserId = getIntent().getStringExtra("onesignal_user_id");

        username_tv.setText(username);
        Glide.with(getApplicationContext()).load(userProfile).into(profile_image);
    }

    private void initializeLogic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window w = this.getWindow();
            w.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            w.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            w.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        }

        emojiPopup = EmojiPopup.Builder.fromRootView(linear1).build(edittext_message);

        // TODO: Implement user presence updates using Supabase Realtime
        // PresenceManager.goOnline(currentUid);
    }

    private void sendMessage() {
        if (!edittext_message.getText().toString().isEmpty()) {
            final String messageText = edittext_message.getText().toString().trim();
            edittext_message.setText(""); // Clear the input field immediately

            HashMap<String, Object> messageMap = new HashMap<>();
            messageMap.put("sender_id", currentUid);
            messageMap.put("sender_username", currentUserUsername);
            messageMap.put("message_text", messageText);
            messageMap.put("timestamp", Calendar.getInstance().getTimeInMillis());
            messageMap.put("type", "text"); // Or other types like "image", "video"

            chatMessageManager.sendMessageToDb(messageMap, currentUid, chatPartnerUid, false);
            chatMessageManager.updateInbox(messageText, chatPartnerUid, false);

            // The UI update for new messages is primarily handled by the Supabase Realtime listener.
            // No need for local add if Realtime is robust.
            // If local add is desired for snappier feel, ensure it's removed if Realtime delivers the same message.

            // TODO: Optionally send push notification via Supabase Functions/Edge Functions
            // sendPushNotification(oneSignalUserId, currentUserUsername, messageText);
        }
    }

    private void toggleEmojiPopup() {
        emojiPopup.toggle();
    }

    private void showOptions() {
        // TODO: Implement chat options (e.g., delete chat, block user)
        Toast.makeText(this, "Chat options not implemented yet", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBackPressed() {
        if (emojiPopup != null && emojiPopup.isShowing()) {
            emojiPopup.dismiss();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // TODO: Implement user presence update (go offline)
        // PresenceManager.goOffline(currentUid);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // TODO: Implement user presence update (go online)
        // PresenceManager.goOnline(currentUid);
    }
}
