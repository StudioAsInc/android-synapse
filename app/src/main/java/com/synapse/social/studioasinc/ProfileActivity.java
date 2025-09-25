package com.synapse.social.studioasinc;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButtonGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.synapse.social.studioasinc.backend.AuthenticationService;
import com.synapse.social.studioasinc.backend.DatabaseService;
import com.synapse.social.studioasinc.backend.QueryService;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class ProfileActivity extends AppCompatActivity {

    private AuthenticationService authService;
    private DatabaseService dbService;
    private QueryService queryService;

    private HashMap<String, Object> UserInfoCacheMap = new HashMap<>();
    private String UserAvatarUri = "";
    private ArrayList<HashMap<String, Object>> UserPostsList = new ArrayList<>();

    private LinearLayout ProfilePageLoadingBody, ProfilePageNoInternetBody;
    private SwipeRefreshLayout ProfilePageSwipeLayout;
    private NestedScrollView ProfilePageTabUserInfo;
    private LinearLayout ProfilePageTabUserPosts;
    private TabLayout ProfilePageTabLayout;
    private RecyclerView ProfilePageTabUserPostsRecyclerView;
    private TextView ProfilePageTabUserPostsNoPostsSubtitle;
    private ImageView ProfilePageTabUserInfoProfileImage, ProfilePageTabUserInfoCoverImage;
    private TextView ProfilePageTabUserInfoNickname, ProfilePageTabUserInfoUsername, ProfilePageTabUserInfoStatus,
            ProfilePageTabUserInfoBioLayoutText, ProfilePageTabUserInfoFollowersCount, ProfilePageTabUserInfoFollowingCount,
            join_date_layout_text, user_uid_layout_text, likeUserProfileButtonLikeCount;
    private Button btnEditProfile, btnFollow, btnMessage;
    private LinearLayout likeUserProfileButton;
    private ImageView likeUserProfileButtonIc;
    private Vibrator vbr;


    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.activity_profile);
        initialize();
        initializeLogic();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authService.getCurrentUser() != null) {
            PresenceManager.setActivity(authService.getCurrentUser().getUid(), "In Profile");
        }
    }

    private void initialize() {
        authService = new AuthenticationService();
        dbService = new DatabaseService();
        queryService = new QueryService(dbService);
        vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Initialize views
        ProfilePageLoadingBody = findViewById(R.id.ProfilePageLoadingBody);
        ProfilePageNoInternetBody = findViewById(R.id.ProfilePageNoInternetBody);
        ProfilePageSwipeLayout = findViewById(R.id.ProfilePageSwipeLayout);
        ProfilePageTabUserInfo = findViewById(R.id.ProfilePageTabUserInfo);
        ProfilePageTabUserPosts = findViewById(R.id.ProfilePageTabUserPosts);
        ProfilePageTabLayout = findViewById(R.id.ProfilePageTabLayout);
        ProfilePageTabUserPostsRecyclerView = findViewById(R.id.ProfilePageTabUserPostsRecyclerView);
        ProfilePageTabUserPostsNoPostsSubtitle = findViewById(R.id.ProfilePageTabUserPostsNoPostsSubtitle);
        ProfilePageTabUserInfoProfileImage = findViewById(R.id.ProfilePageTabUserInfoProfileImage);
        ProfilePageTabUserInfoCoverImage = findViewById(R.id.ProfilePageTabUserInfoCoverImage);
        ProfilePageTabUserInfoNickname = findViewById(R.id.ProfilePageTabUserInfoNickname);
        ProfilePageTabUserInfoUsername = findViewById(R.id.ProfilePageTabUserInfoUsername);
        ProfilePageTabUserInfoStatus = findViewById(R.id.ProfilePageTabUserInfoStatus);
        ProfilePageTabUserInfoBioLayoutText = findViewById(R.id.ProfilePageTabUserInfoBioLayoutText);
        ProfilePageTabUserInfoFollowersCount = findViewById(R.id.ProfilePageTabUserInfoFollowersCount);
        ProfilePageTabUserInfoFollowingCount = findViewById(R.id.ProfilePageTabUserInfoFollowingCount);
        join_date_layout_text = findViewById(R.id.join_date_layout_text);
        user_uid_layout_text = findViewById(R.id.user_uid_layout_text);
        likeUserProfileButtonLikeCount = findViewById(R.id.likeUserProfileButtonLikeCount);
        btnEditProfile = findViewById(R.id.btnEditProfile);
        btnFollow = findViewById(R.id.btnFollow);
        btnMessage = findViewById(R.id.btnMessage);
        likeUserProfileButton = findViewById(R.id.likeUserProfileButton);
        likeUserProfileButtonIc = findViewById(R.id.likeUserProfileButtonIc);

        findViewById(R.id.ProfilePageTopBarBack).setOnClickListener(v -> onBackPressed());
        ProfilePageSwipeLayout.setOnRefreshListener(this::_loadRequest);
    }

    private void initializeLogic() {
        ProfilePageTabLayout.addTab(ProfilePageTabLayout.newTab().setText(getResources().getString(R.string.profile_tab)));
        ProfilePageTabLayout.addTab(ProfilePageTabLayout.newTab().setText(getResources().getString(R.string.posts_tab)));
        ProfilePageTabUserPostsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        _loadRequest();

        String intentUid = getIntent().getStringExtra("uid");
        if (authService.getCurrentUser() != null && intentUid.equals(authService.getCurrentUser().getUid())) {
            findViewById(R.id.ProfilePageTabUserInfoSecondaryButtons).setVisibility(View.GONE);
            btnEditProfile.setVisibility(View.VISIBLE);
        } else {
            findViewById(R.id.ProfilePageTabUserInfoSecondaryButtons).setVisibility(View.VISIBLE);
            btnEditProfile.setVisibility(View.GONE);
        }

        btnFollow.setOnClickListener(v -> handleFollow());
        btnMessage.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
            intent.putExtra("uid", getIntent().getStringExtra("uid"));
            intent.putExtra("origin", "ProfileActivity");
            startActivity(intent);
        });
    }

    private void _loadRequest() {
        ProfilePageSwipeLayout.setVisibility(View.GONE);
        ProfilePageNoInternetBody.setVisibility(View.GONE);
        ProfilePageLoadingBody.setVisibility(View.VISIBLE);
        _getUserReference();
    }

    private void _getUserReference() {
        String uid = getIntent().getStringExtra("uid");
        dbService.getData("skyline/users/" + uid, new DatabaseService.DataListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ProfilePageSwipeLayout.setVisibility(View.VISIBLE);
                    ProfilePageLoadingBody.setVisibility(View.GONE);
                    user_uid_layout_text.setText(dataSnapshot.child("uid").getValue(String.class));
                    // ... (populate other fields from snapshot)
                    _getUserPostsReference();
                    _getUserCountReference();
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                ProfilePageSwipeLayout.setVisibility(View.GONE);
                ProfilePageNoInternetBody.setVisibility(View.VISIBLE);
                ProfilePageLoadingBody.setVisibility(View.GONE);
            }
        });
    }

    private void _getUserPostsReference() {
        queryService.fetchWithOrder("skyline/posts", "uid", getIntent().getStringExtra("uid"), new DatabaseService.DataListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    ProfilePageTabUserPostsRecyclerView.setVisibility(View.VISIBLE);
                    ProfilePageTabUserPostsNoPostsSubtitle.setVisibility(View.GONE);
                    UserPostsList.clear();
                    GenericTypeIndicator<HashMap<String, Object>> ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
                    for (DataSnapshot data : dataSnapshot.getChildren()) {
                        UserPostsList.add(data.getValue(ind));
                    }
                    SketchwareUtil.sortListMap(UserPostsList, "publish_date", false, false);
                    ProfilePageTabUserPostsRecyclerView.setAdapter(new ProfilePageTabUserPostsRecyclerViewAdapter(UserPostsList));
                } else {
                    ProfilePageTabUserPostsRecyclerView.setVisibility(View.GONE);
                    ProfilePageTabUserPostsNoPostsSubtitle.setVisibility(View.VISIBLE);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {}
        });
    }

    private void _getUserCountReference() {
        String uid = getIntent().getStringExtra("uid");
        String myUid = authService.getCurrentUser().getUid();

        dbService.getData("skyline/followers/" + uid, createCountListener(ProfilePageTabUserInfoFollowersCount, R.string.followers));
        dbService.getData("skyline/following/" + uid, createCountListener(ProfilePageTabUserInfoFollowingCount, R.string.following));
        dbService.getData("skyline/profile-likes/" + uid, createCountListener(likeUserProfileButtonLikeCount, 0));

        dbService.addValueEventListener("skyline/followers/" + uid + "/" + myUid, new DatabaseService.DataListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    btnFollow.setText(R.string.unfollow);
                } else {
                    btnFollow.setText(R.string.follow);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private DatabaseService.DataListener createCountListener(TextView textView, int stringResId) {
        return new DatabaseService.DataListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                long count = dataSnapshot.getChildrenCount();
                if (stringResId != 0) {
                    textView.setText(_getStyledNumber(count) + " " + getResources().getString(stringResId));
                } else {
                    textView.setText(_getStyledNumber(count));
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        };
    }

    private void handleFollow() {
        String uid = getIntent().getStringExtra("uid");
        String myUid = authService.getCurrentUser().getUid();
        dbService.getData("skyline/followers/" + uid + "/" + myUid, new DatabaseService.DataListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    dbService.getReference("skyline/followers/" + uid + "/" + myUid).removeValue();
                    dbService.getReference("skyline/following/" + myUid + "/" + uid).removeValue();
                } else {
                    dbService.getReference("skyline/followers/" + uid + "/" + myUid).setValue(myUid);
                    dbService.getReference("skyline/following/" + myUid + "/" + uid).setValue(uid);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    public String _getStyledNumber(final double _number) {
        if (_number < 10000) {
            return String.valueOf((long) _number);
        } else {
            DecimalFormat decimalFormat = new DecimalFormat("0.0");
            String numberFormat;
            double formattedNumber;
            if (_number < 1000000) {
                numberFormat = "K";
                formattedNumber = _number / 1000;
            } else {
                numberFormat = "M";
                formattedNumber = _number / 1000000;
            }
            return decimalFormat.format(formattedNumber) + numberFormat;
        }
    }

    // Adapter class would also be refactored to accept services
    public class ProfilePageTabUserPostsRecyclerViewAdapter extends RecyclerView.Adapter<ProfilePageTabUserPostsRecyclerViewAdapter.ViewHolder> {
        ArrayList<HashMap<String, Object>> _data;
        public ProfilePageTabUserPostsRecyclerViewAdapter(ArrayList<HashMap<String, Object>> _arr) { _data = _arr; }
        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View _v = LayoutInflater.from(parent.getContext()).inflate(R.layout.synapse_post_cv, parent, false);
            return new ViewHolder(_v);
        }
        @Override public void onBindViewHolder(ViewHolder _holder, final int _position) { /* ... Refactored logic ... */ }
        @Override public int getItemCount() { return _data.size(); }
        public class ViewHolder extends RecyclerView.ViewHolder { public ViewHolder(View v) { super(v); } }
    }
}