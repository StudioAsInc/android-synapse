package com.synapse.social.studioasinc;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.synapse.social.studioasinc.adapter.SearchUserAdapter;
import com.synapse.social.studioasinc.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

import io.github.jan_tennert.supabase.SupabaseClient;
import io.github.jan_tennert.supabase.postgrest.Postgrest;
import io.github.jan_tennert.supabase.postgrest.PostgrestResult;
import io.github.jan_tennert.supabase.postgrest.query.Columns;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;
import kotlinx.coroutines.withContext;

public class UserMention implements TextWatcher, SearchUserAdapter.OnUserClickListener {

    private final EditText editText;
    private final Context context;
    private final SupabaseClient supabase;
    private final Postgrest postgrest; // Convenience field for Postgrest client
    private PopupWindow popupWindow;
    private SearchUserAdapter searchUserAdapter;
    private final List<User> userList = new ArrayList<>();

    private final View sendButton;

    public UserMention(EditText editText, View sendButton, SupabaseClient supabase) {
        this.editText = editText;
        this.context = editText.getContext();
        this.supabase = supabase;
        this.postgrest = supabase.postgrest;
        this.sendButton = sendButton;
        setupPopupWindow();
    }

    public UserMention(EditText editText, SupabaseClient supabase) {
        this(editText, null, supabase);
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
            // The popup will be shown in searchUsers after data is fetched
        } else {
            String[] words = text.substring(0, cursorPosition).split("\\s");
            String lastWord = words.length > 0 ? words[words.length - 1] : "";

            if (lastWord.startsWith("@")) {
                String query = lastWord.substring(1);
                searchUsers(query);
            } else {
                hidePopup();
            }
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    private void searchUsers(String query) {
        CoroutineScope scope = new CoroutineScope(Dispatchers.getIO());
        scope.launch(() -> {
            try {
                // Supabase query to search for users by username
                // Assuming your 'users' table has 'id', 'username', and 'profile_url'
                PostgrestResult response = postgrest.from("users")
                        .select(Columns.list("id", "username", "profile_url")) // Select relevant user columns
                        .filter("username.ilike", "%" + query + "%") // Case-insensitive partial match
                        .limit(10) // Limit to 10 results
                        .execute();

                // Decode the response to a list of User objects
                List<User> fetchedUsers = response.decodeList(User.class);

                // Update UI on the main thread
                new Handler(Looper.getMainLooper()).post(() -> {
                    userList.clear();
                    userList.addAll(fetchedUsers);
                    searchUserAdapter.notifyDataSetChanged();
                    if (userList.isEmpty()) {
                        hidePopup();
                    } else {
                        showPopup();
                    }
                });
            } catch (Exception e) {
                Log.e("UserMention", "Error searching users: " + e.getMessage());
                new Handler(Looper.getMainLooper()).post(() -> {
                    hidePopup(); // Hide popup on error
                });
            }
        });
    }

    private void showPopup() {
        if (!popupWindow.isShowing() && !userList.isEmpty()) { // Only show if there are results
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
        String username = user.getUsername(); // Assuming User.getUsername() exists
        String text = editText.getText().toString();
        int cursorPosition = editText.getSelectionStart();

        String textBeforeCursor = text.substring(0, cursorPosition);
        int atIndex = textBeforeCursor.lastIndexOf('@');

        if (atIndex != -1) {
            String newText = text.substring(0, atIndex + 1) + username + " " + text.substring(cursorPosition);
            editText.setText(newText);
            editText.setSelection(atIndex + username.length() + 2); // Position cursor after the mentioned username and a space
        }

        hidePopup();
    }
}
