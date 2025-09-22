package com.synapse.social.studioasinc;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.synapse.social.studioasinc.model.User;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewGroupActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private ImageView groupIcon;
    private EditText groupName;
    private LinearLayout disappearingMessagesLayout;
    private LinearLayout groupPermissionsLayout;
    private TextView membersCount;
    private RecyclerView membersRecyclerView;
    private FloatingActionButton fabCreateGroup;

    private ArrayList<User> selectedMembers = new ArrayList<>();
    private MembersAdapter membersAdapter;

    private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
    private DatabaseReference groupsRef = _firebase.getReference("groups");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_group);

        toolbar = findViewById(R.id.toolbar);
        groupIcon = findViewById(R.id.group_icon);
        groupName = findViewById(R.id.group_name);
        disappearingMessagesLayout = findViewById(R.id.disappearing_messages_layout);
        groupPermissionsLayout = findViewById(R.id.group_permissions_layout);
        membersCount = findViewById(R.id.members_count);
        membersRecyclerView = findViewById(R.id.members_recyclerview);
        fabCreateGroup = findViewById(R.id.fab_create_group);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        selectedMembers = (ArrayList<User>) getIntent().getSerializableExtra("selected_members");
        if (selectedMembers == null) {
            selectedMembers = new ArrayList<>();
        }

        setupRecyclerView();

        membersCount.setText("Members: " + selectedMembers.size());

        groupPermissionsLayout.setOnClickListener(v -> {
            Intent intent = new Intent(NewGroupActivity.this, GroupPermissionsActivity.class);
            startActivity(intent);
        });

        fabCreateGroup.setOnClickListener(v -> createGroup());
    }

    private void setupRecyclerView() {
        membersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        membersAdapter = new MembersAdapter(selectedMembers);
        membersRecyclerView.setAdapter(membersAdapter);
    }

    private void createGroup() {
        String name = groupName.getText().toString().trim();
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, "Group name cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        String groupId = groupsRef.push().getKey();
        if (groupId == null) {
            Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> groupData = new HashMap<>();
        groupData.put("name", name);
        groupData.put("creatorId", FirebaseAuth.getInstance().getUid());
        groupData.put("createdAt", System.currentTimeMillis());

        Map<String, Boolean> members = new HashMap<>();
        members.put(FirebaseAuth.getInstance().getUid(), true);
        for (User user : selectedMembers) {
            members.put(user.getUid(), true);
        }
        groupData.put("members", members);

        groupsRef.child(groupId).setValue(groupData).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(NewGroupActivity.this, ChatActivity.class);
                intent.putExtra("uid", groupId);
                intent.putExtra("isGroup", true);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show();
            }
        });
    }

    class MembersAdapter extends RecyclerView.Adapter<MembersAdapter.MemberViewHolder> {
        private List<User> members;

        public MembersAdapter(List<User> members) {
            this.members = members;
        }

        @NonNull
        @Override
        public MemberViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.synapse_users_list_cv, parent, false);
            return new MemberViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MemberViewHolder holder, int position) {
            User user = members.get(position);
            holder.username.setText(user.getNickname() != null ? user.getNickname() : user.getUsername());
            Glide.with(holder.itemView.getContext()).load(user.getAvatar()).placeholder(R.drawable.avatar).into(holder.avatar);
        }

        @Override
        public int getItemCount() {
            return members.size();
        }

        class MemberViewHolder extends RecyclerView.ViewHolder {
            ImageView avatar;
            TextView username;

            public MemberViewHolder(@NonNull View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.profileCardImage);
                username = itemView.findViewById(R.id.username);
            }
        }
    }
}
