package com.synapse.social.studioasinc;

import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupWindow;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.synapse.social.studioasinc.adapter.SearchUserAdapter;
import com.synapse.social.studioasinc.model.User;

import java.util.ArrayList;
import java.util.List;

public class UserMention implements TextWatcher, SearchUserAdapter.OnUserClickListener {

    private final EditText editText;
    private final Context context;
    private final DatabaseReference usersRef;
    private PopupWindow popupWindow;
    private SearchUserAdapter searchUserAdapter;
    private final List<User> userList = new ArrayList<>();

    private final View sendButton;

    public UserMention(EditText editText, View sendButton) {
        this.editText = editText;
        this.context = editText.getContext();
        this.usersRef = FirebaseDatabase.getInstance().getReference("skyline/users");
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

        popupWindow = new PopupWindow(recyclerView,
                RecyclerView.LayoutParams.WRAP_CONTENT,
                RecyclerView.LayoutParams.WRAP_CONTENT);
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
        Query searchQuery = usersRef.orderByChild("username")
                .startAt(query)
                .endAt(query + "\uf8ff")
                .limitToFirst(10);

        searchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null) {
                        user.setUid(snapshot.getKey());
                        userList.add(user);
                    }
                }
                searchUserAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Handle error
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
