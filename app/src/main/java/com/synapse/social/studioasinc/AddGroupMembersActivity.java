package com.synapse.social.studioasinc;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.synapse.social.studioasinc.model.User;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class AddGroupMembersActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView selectedMembersRecyclerView;
    private RecyclerView contactsRecyclerView;
    private FloatingActionButton fabNext;

    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference usersRef = _firebase.getReference("skyline/users");

    private List<User> userList = new ArrayList<>();
    private List<User> selectedUserList = new ArrayList<>();
    private UserAdapter userAdapter;
    private SelectedUserAdapter selectedUserAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_group_members);

        toolbar = findViewById(R.id.toolbar);
        selectedMembersRecyclerView = findViewById(R.id.selected_members_recyclerview);
        contactsRecyclerView = findViewById(R.id.contacts_recyclerview);
        fabNext = findViewById(R.id.fab_next);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        setupRecyclerViews();

        fabNext.setOnClickListener(v -> {
            if (selectedUserList.isEmpty()) {
                Toast.makeText(AddGroupMembersActivity.this, "Select at least one member", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(AddGroupMembersActivity.this, NewGroupActivity.class);
            intent.putExtra("selected_members", (Serializable) selectedUserList);
            startActivity(intent);
        });

        loadUsers();
    }

    private void setupRecyclerViews() {
        selectedMembersRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        selectedUserAdapter = new SelectedUserAdapter(selectedUserList);
        selectedMembersRecyclerView.setAdapter(selectedUserAdapter);

        contactsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(userList, selectedUserList);
        contactsRecyclerView.setAdapter(userAdapter);
    }

    private void loadUsers() {
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                userList.clear();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    User user = snapshot.getValue(User.class);
                    if (user != null && !snapshot.getKey().equals(FirebaseAuth.getInstance().getUid())) {
                        user.setUid(snapshot.getKey());
                        userList.add(user);
                    }
                }
                userAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Toast.makeText(AddGroupMembersActivity.this, "Failed to load users.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
        private List<User> users;
        private List<User> selectedUsers;

        public UserAdapter(List<User> users, List<User> selectedUsers) {
            this.users = users;
            this.selectedUsers = selectedUsers;
        }

        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.synapse_users_list_cv, parent, false);
            return new UserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            User user = users.get(position);
            holder.username.setText(user.getNickname() != null ? user.getNickname() : user.getUsername());
            Glide.with(holder.itemView.getContext()).load(user.getAvatar()).placeholder(R.drawable.avatar).into(holder.avatar);

            if (selectedUsers.contains(user)) {
                holder.itemView.setBackgroundColor(getResources().getColor(android.R.color.holo_green_light));
            } else {
                holder.itemView.setBackgroundColor(getResources().getColor(android.R.color.transparent));
            }

            holder.itemView.setOnClickListener(v -> {
                if (selectedUsers.contains(user)) {
                    selectedUsers.remove(user);
                } else {
                    selectedUsers.add(user);
                }
                notifyDataSetChanged();
                selectedUserAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return users.size();
        }

        class UserViewHolder extends RecyclerView.ViewHolder {
            ImageView avatar;
            TextView username;

            public UserViewHolder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.profileCardImage);
                username = itemView.findViewById(R.id.username);
            }
        }
    }

    class SelectedUserAdapter extends RecyclerView.Adapter<SelectedUserAdapter.SelectedUserViewHolder> {
        private List<User> selectedUsers;

        public SelectedUserAdapter(List<User> selectedUsers) {
            this.selectedUsers = selectedUsers;
        }

        @NonNull
        @Override
        public SelectedUserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_media, parent, false);
            return new SelectedUserViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SelectedUserViewHolder holder, int position) {
            User user = selectedUsers.get(position);
            holder.username.setText(user.getNickname() != null ? user.getNickname() : user.getUsername());
            Glide.with(holder.itemView.getContext()).load(user.getAvatar()).placeholder(R.drawable.avatar).into(holder.avatar);
            holder.removeButton.setOnClickListener(v -> {
                selectedUsers.remove(user);
                notifyDataSetChanged();
                userAdapter.notifyDataSetChanged();
            });
        }

        @Override
        public int getItemCount() {
            return selectedUsers.size();
        }

        class SelectedUserViewHolder extends RecyclerView.ViewHolder {
            ImageView avatar;
            TextView username;
            ImageView removeButton;

            public SelectedUserViewHolder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.media_image);
                username = itemView.findViewById(R.id.media_name);
                removeButton = itemView.findViewById(R.id.remove_button);
            }
        }
    }
}
