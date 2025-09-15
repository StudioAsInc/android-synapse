package com.synapse.social.studioasinc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.search.SearchBar;
import com.google.android.material.search.SearchView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {

    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();

    private ArrayList<HashMap<String, Object>> searchedUsersList = new ArrayList<>();

    private SearchBar search_bar;
    private SearchView search_view;
    private RecyclerView SearchUserLayoutRecyclerView;
    private TextView SearchUserLayoutNoUserFound;

    private Intent intent = new Intent();

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.search);
        initialize(_savedInstanceState);
        FirebaseApp.initializeApp(this);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        search_bar = findViewById(R.id.search_bar);
        search_view = findViewById(R.id.search_view);
        SearchUserLayoutRecyclerView = findViewById(R.id.SearchUserLayoutRecyclerView);
        SearchUserLayoutNoUserFound = findViewById(R.id.SearchUserLayoutNoUserFound);
    }

    private void initializeLogic() {
        SearchUserLayoutRecyclerView.setAdapter(new SearchUserLayoutRecyclerViewAdapter(searchedUsersList));
        SearchUserLayoutRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        search_view.setupWithSearchBar(search_bar);
        search_view.getEditText().addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                performSearch(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        _showAllUser();
    }

    public void _showAllUser() {
        DatabaseReference searchRef = FirebaseDatabase.getInstance().getReference("skyline/users");
        Query searchQuery = searchRef.limitToLast(50);

        searchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    SearchUserLayoutRecyclerView.setVisibility(View.VISIBLE);
                    SearchUserLayoutNoUserFound.setVisibility(View.GONE);
                    searchedUsersList.clear();

                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        HashMap<String, Object> searchMap = new HashMap<String, Object>((Map<String, Object>) dataSnapshot.getValue());
                        searchedUsersList.add(searchMap);
                    }
                    SearchUserLayoutRecyclerView.getAdapter().notifyDataSetChanged();
                } else {
                    SearchUserLayoutRecyclerView.setVisibility(View.GONE);
                    SearchUserLayoutNoUserFound.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    public void performSearch(final String query) {
        if (query.trim().isEmpty()) {
            searchedUsersList.clear();
            SearchUserLayoutRecyclerView.getAdapter().notifyDataSetChanged();
            SearchUserLayoutRecyclerView.setVisibility(View.GONE);
            SearchUserLayoutNoUserFound.setVisibility(View.VISIBLE);
        } else {
            _searchUsers(query);
        }
    }

    private void _searchUsers(String searchText) {
        DatabaseReference searchRef = FirebaseDatabase.getInstance().getReference("skyline/users");
        Query searchQuery = searchRef.orderByChild("username").startAt(searchText).endAt(searchText + "\uf8ff").limitToLast(50);

        searchQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    SearchUserLayoutRecyclerView.setVisibility(View.VISIBLE);
                    SearchUserLayoutNoUserFound.setVisibility(View.GONE);
                    searchedUsersList.clear();

                    for (DataSnapshot dataSnapshot : snapshot.getChildren()) {
                        HashMap<String, Object> searchMap = new HashMap<>((Map<String, Object>) dataSnapshot.getValue());
                        searchedUsersList.add(searchMap);
                    }
                    SearchUserLayoutRecyclerView.getAdapter().notifyDataSetChanged();
                } else {
                    SearchUserLayoutRecyclerView.setVisibility(View.GONE);
                    SearchUserLayoutNoUserFound.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    public class SearchUserLayoutRecyclerViewAdapter extends RecyclerView.Adapter<SearchUserLayoutRecyclerViewAdapter.ViewHolder> {

        ArrayList<HashMap<String, Object>> _data;

        public SearchUserLayoutRecyclerViewAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            View _v = _inflater.inflate(R.layout.synapse_users_list_cv, parent, false);
            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            View _view = _holder.itemView;

            final ImageView profileAvatar = _view.findViewById(R.id.profileAvatar);
            final TextView username = _view.findViewById(R.id.username);
            final TextView name = _view.findViewById(R.id.name);
            final ImageView badge = _view.findViewById(R.id.badge);
            final ImageView genderBadge = _view.findViewById(R.id.genderBadge);
            final LinearLayout userStatusCircleBG = _view.findViewById(R.id.userStatusCircleBG);

            try {
                HashMap<String, Object> currentItem = _data.get(_position);
                if (currentItem == null) return;

                // Username
                Object usernameObj = currentItem.get("username");
                if (usernameObj != null) {
                    name.setText("@" + usernameObj.toString());
                }

                // Avatar
                Object bannedObj = currentItem.get("banned");
                if (bannedObj != null && bannedObj.toString().equals("true")) {
                    profileAvatar.setImageResource(R.drawable.banned_avatar);
                } else {
                    Object avatarObj = currentItem.get("avatar");
                    if (avatarObj != null && !avatarObj.toString().equals("null")) {
                        Glide.with(getApplicationContext()).load(Uri.parse(avatarObj.toString())).circleCrop().into(profileAvatar);
                    } else {
                        profileAvatar.setImageResource(R.drawable.avatar);
                    }
                }

                // Nickname
                Object nicknameObj = currentItem.get("nickname");
                if (nicknameObj != null && !nicknameObj.toString().equals("null")) {
                    username.setText(nicknameObj.toString());
                } else if (usernameObj != null) {
                    username.setText("@" + usernameObj.toString());
                }

                // Badges
                badge.setVisibility(View.GONE);
                genderBadge.setVisibility(View.GONE);

                Object genderObj = currentItem.get("gender");
                if (genderObj != null && !genderObj.toString().equals("hidden")) {
                    genderBadge.setVisibility(View.VISIBLE);
                    if (genderObj.toString().equals("male")) {
                        genderBadge.setImageResource(R.drawable.male_badge);
                    } else if (genderObj.toString().equals("female")) {
                        genderBadge.setImageResource(R.drawable.female_badge);
                    }
                }

                Object accountTypeObj = currentItem.get("account_type");
                if (accountTypeObj != null) {
                    badge.setVisibility(View.VISIBLE);
                    switch (accountTypeObj.toString()) {
                        case "admin":
                            badge.setImageResource(R.drawable.admin_badge);
                            break;
                        case "moderator":
                            badge.setImageResource(R.drawable.moderator_badge);
                            break;
                        case "support":
                            badge.setImageResource(R.drawable.support_badge);
                            break;
                        default:
                            Object premiumObj = currentItem.get("account_premium");
                            Object verifyObj = currentItem.get("verify");
                            if (premiumObj != null && premiumObj.toString().equals("true")) {
                                badge.setImageResource(R.drawable.premium_badge);
                            } else if (verifyObj != null && verifyObj.toString().equals("true")) {
                                badge.setImageResource(R.drawable.verified_badge);
                            } else {
                                badge.setVisibility(View.GONE);
                            }
                            break;
                    }
                }

                // Status
                Object statusObj = currentItem.get("status");
                if (statusObj != null && statusObj.toString().equals("online")) {
                    userStatusCircleBG.setVisibility(View.VISIBLE);
                } else {
                    userStatusCircleBG.setVisibility(View.GONE);
                }

                // Click listener
                _view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View _view) {
                        Object uidObj = _data.get(_position).get("uid");
                        if (uidObj != null) {
                            intent.setClass(getApplicationContext(), ProfileActivity.class);
                            intent.putExtra("uid", uidObj.toString());
                            intent.putExtra("origin", "SearchActivity");
                            startActivity(intent);
                        }
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public int getItemCount() {
            return _data.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View v) {
                super(v);
            }
        }
    }
}
