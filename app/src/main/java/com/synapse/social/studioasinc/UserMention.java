package com.synapse.social.studioasinc;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

// TODO(supabase): Add Supabase imports for PostgREST and Realtime
// import io.supabase.postgrest.PostgrestClient;
// import io.supabase.realtime.RealtimeClient;
import com.synapse.social.studioasinc.adapter.SearchUserAdapter;
import com.synapse.social.studioasinc.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserMention implements TextWatcher, SearchUserAdapter.OnUserClickListener {

    private final EditText editText;
    private final Context context;
    private final SupabaseClient supabaseClient; // TODO(supabase): Initialize Supabase client
    private final String USERS_TABLE = "users"; // Assuming 'users' is your table name

    private PopupWindow popupWindow;
    private SearchUserAdapter searchUserAdapter;
    private final List<User> userList = new ArrayList<>();

    private final View sendButton;

    public UserMention(EditText editText, View sendButton, SupabaseClient supabaseClient) {
        this.editText = editText;
        this.context = editText.getContext();
        this.supabaseClient = supabaseClient; // Initialize Supabase client
        this.sendButton = sendButton;
        setupPopupWindow();
    }

    public UserMention(EditText editText) {
        this(editText, null);
    }

    private void setupPopupWindow() {
        RecyclerView recyclerView = new RecyclerView(context);
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        searchUserAdapter = new SearchUserAdapter(context, userList, this);
        recyclerView.setAdapter(searchUserAdapter);
        recyclerView.setBackgroundResource(R.drawable.rounded_background);

        int height = (int) (200 * context.getResources().getDisplayMetrics().density);
        popupWindow = new PopupWindow(recyclerView,
                RecyclerView.LayoutParams.MATCH_PARENT,
                height);
        popupWindow.setFocusable(false);
        popupWindow.setOutsideTouchable(true);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.BitmapDrawable());
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        String text = s.toString();
        int cursorPosition = editText.getSelectionStart();

        if (sendButton != null) {
            if (!text.trim().isEmpty()) {
                sendButton.setVisibility(View.VISIBLE);
            } else {
                sendButton.setVisibility(View.GONE);
            }
        }

        if (cursorPosition > 0 && text.charAt(cursorPosition - 1) == '@') {
            searchUsers("");
            showPopup();
        } else {
            String[] words = text.substring(0, cursorPosition).split("\\s");
            String lastWord = words.length > 0 ? words[words.length - 1] : "";

            if (lastWord.startsWith("@")) {
                String query = lastWord.substring(1);
                searchUsers(query);
                showPopup();
            } else {
                hidePopup();
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private void searchUsers(String query) {
        // TODO(supabase): Implement with Supabase PostgREST
        /*
        supabaseClient.postgrest[USERS_TABLE]
                .select("uid,username,nickname,avatar") // Adjust columns as needed
                .ilike("username", "%" + query + "%") // Case-insensitive search
                .limit(10)
                .execute()
                .thenAccept(response -> {
                    userList.clear();
                    if (response.getStatus() == 200) {
                        try {
                            JSONArray jsonArray = new JSONArray(response.getData());
                            for (int i = 0; i < jsonArray.length(); i++) {
                                JSONObject jsonObject = jsonArray.getJSONObject(i);
                                User user = new User();
                                user.setUid(jsonObject.optString("uid"));
                                user.setUsername(jsonObject.optString("username"));
                                user.setNickname(jsonObject.optString("nickname"));
                                user.setAvatar(jsonObject.optString("avatar"));
                                userList.add(user);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    searchUserAdapter.notifyDataSetChanged();
                })
                .exceptionally(e -> {
                    e.printStackTrace();
                    return null;
                });
        */
        // Placeholder for now
        userList.clear();
        searchUserAdapter.notifyDataSetChanged();
    }

    private void showPopup() {
        if (!popupWindow.isShowing()) {
            popupWindow.showAsDropDown(editText);
        }
    }

    private void hidePopup() {
        if (popupWindow.isShowing()) {
            popupWindow.dismiss();
        }
    }

    @Override
    public void onUserClick(User user) {
        String username = user.getUsername();
        String text = editText.getText().toString();
        int cursorPosition = editText.getSelectionStart();

        String textBeforeCursor = text.substring(0, cursorPosition);
        int atIndex = textBeforeCursor.lastIndexOf('@');

        if (atIndex != -1) {
            String newText = text.substring(0, atIndex + 1) + username + " " + text.substring(cursorPosition);
            editText.setText(newText);
            editText.setSelection(atIndex + username.length() + 2);
        }

        hidePopup();
    }
}
