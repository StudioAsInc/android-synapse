package com.synapse.social.studioasinc;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupWindow;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.synapse.social.studioasinc.adapter.SearchUserAdapter;
import com.synapse.social.studioasinc.backend.IDatabaseService;
import com.synapse.social.studioasinc.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserMention implements TextWatcher, SearchUserAdapter.OnUserClickListener {

    private final EditText editText;
    private final Context context;
    private final IDatabaseService dbService;
    private PopupWindow popupWindow;
    private SearchUserAdapter searchUserAdapter;
    private final List<User> userList = new ArrayList<>();

    private final View sendButton;

    public UserMention(EditText editText, View sendButton, @NonNull IDatabaseService dbService) {
        this.editText = editText;
        this.context = editText.getContext();
        this.dbService = dbService;
        this.sendButton = sendButton;
        setupPopupWindow();
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
        dbService.searchUsers(query, new IDataListener() {
            @Override
            public void onDataReceived(IDataSnapshot dataSnapshot) {
                userList.clear();
                if (dataSnapshot.exists()) {
                    for (IDataSnapshot snapshot : dataSnapshot.getChildren()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            userList.add(user);
                        }
                    }
                }
                searchUserAdapter.notifyDataSetChanged();

                if (userList.isEmpty()) {
                    hidePopup();
                } else {
                    showPopup();
                }
            }

            @Override
            public void onCancelled(IDatabaseError databaseError) {
                hidePopup();
            }
        });
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
