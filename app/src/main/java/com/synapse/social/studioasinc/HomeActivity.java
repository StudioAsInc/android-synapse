package com.synapse.social.studioasinc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.synapse.social.studioasinc.adapter.ViewPagerAdapter;
import com.synapse.social.studioasinc.util.AuthUtil;

public class HomeActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final int REELS_TAB_POSITION = 1;
    private FirebaseAuth auth;
    private FirebaseDatabase _firebase;
    private DatabaseReference udb;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AppBarLayout app_bar_layout;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ImageView menuButton;
    private ImageView addPostButton;
    private ImageView navSearchIc;
    private ImageView navInboxIc;
    private View topBar;
    
    // Cache user UID to avoid redundant lookups
    private String cachedUserUid;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.home);
        FirebaseApp.initializeApp(this);
        
        // Cache user UID once at startup
        cachedUserUid = AuthUtil.getCurrentUserUid();
        
        initialize();
        initializeLogic();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (cachedUserUid != null) {
            PresenceManager.setActivity(cachedUserUid, "In Home");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initialize() {
        auth = FirebaseAuth.getInstance();
        _firebase = FirebaseDatabase.getInstance();
        udb = _firebase.getReference("skyline/users");

        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        app_bar_layout = findViewById(R.id.app_bar_layout);
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);
        menuButton = findViewById(R.id.menu_button);
        addPostButton = findViewById(R.id.add_post_button);
        navSearchIc = findViewById(R.id.nav_search_ic);
        navInboxIc = findViewById(R.id.nav_inbox_ic);
        topBar = findViewById(R.id.topBar);
    }

    private void initializeLogic() {
        if (!AuthUtil.isLoggedIn()) {
            // User is not signed in and no backup authentication, redirect to AuthActivity
            Intent intent = new Intent(HomeActivity.this, AuthActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        navigationView.setNavigationItemSelectedListener(this);

        menuButton.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.END));

        addPostButton.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), CreatePostActivity.class);
            startActivity(intent);
        });

        navSearchIc.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
            startActivity(intent);
        });

        navInboxIc.setOnClickListener(v -> {
            Intent intent = new Intent(getApplicationContext(), InboxActivity.class);
            startActivity(intent);
        });

        viewPager.setAdapter(new ViewPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            switch (position) {
                case 0:
                    tab.setIcon(R.drawable.home_24px);
                    break;
                case 1:
                    tab.setIcon(R.drawable.ic_video_library_48px);
                    break;
                case 2:
                    tab.setIcon(R.drawable.icon_notifications_round);
                    break;
            }
        }).attach();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                AppBarLayout.LayoutParams toolbarLayoutParams = (AppBarLayout.LayoutParams) topBar.getLayoutParams();
                AppBarLayout.LayoutParams tabLayoutParams = (AppBarLayout.LayoutParams) tabLayout.getLayoutParams();
                if (tab.getPosition() == REELS_TAB_POSITION) {
                    toolbarLayoutParams.setScrollFlags(0);
                    tabLayoutParams.setScrollFlags(0);
                    topBar.setVisibility(View.GONE); // Hide the top bar
                } else {
                    toolbarLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
                    tabLayoutParams.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
                    topBar.setVisibility(View.VISIBLE); // Show the top bar
                }
                topBar.setLayoutParams(toolbarLayoutParams);
                tabLayout.setLayoutParams(tabLayoutParams);
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });

        // Setup drawer header
        View headerView = navigationView.getHeaderView(0);
        ImageView profileImage = headerView.findViewById(R.id.profile_image);
        ImageView coverImage = headerView.findViewById(R.id.cover_image);
        android.widget.TextView userName = headerView.findViewById(R.id.user_name);
        android.widget.TextView userEmail = headerView.findViewById(R.id.user_email);

        // Get user UID from cache
        String userUid = cachedUserUid;

        if (userUid != null) {
            DatabaseReference getReference = udb.child(userUid);
            getReference.addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()) {
                        if (dataSnapshot.child("avatar").getValue(String.class) != null && !dataSnapshot.child("avatar").getValue(String.class).equals("null")) {
                            Glide.with(getApplicationContext()).load(Uri.parse(dataSnapshot.child("avatar").getValue(String.class))).into(profileImage);
                        } else {
                            profileImage.setImageResource(R.drawable.ic_account_circle_48px);
                        }
                        if (dataSnapshot.child("cover").getValue(String.class) != null && !dataSnapshot.child("cover").getValue(String.class).equals("null")) {
                            Glide.with(getApplicationContext()).load(Uri.parse(dataSnapshot.child("cover").getValue(String.class))).into(coverImage);
                        }
                        if (dataSnapshot.child("username").getValue(String.class) != null) {
                            userName.setText(dataSnapshot.child("username").getValue(String.class));
                        }
                        if (dataSnapshot.child("email").getValue(String.class) != null) {
                            userEmail.setText(dataSnapshot.child("email").getValue(String.class));
                        }
                    }
                }
                @Override
                public void onCancelled(@NonNull DatabaseError databaseError) {
                    // Handle error
                }
            });
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_my_profile) {
            Intent profileIntent = new Intent(getApplicationContext(), ProfileActivity.class);
            // Use cached user UID
            if (cachedUserUid != null) {
                profileIntent.putExtra("uid", cachedUserUid);
                startActivity(profileIntent);
            }
        } else if (id == R.id.nav_settings) {
            Intent settingsIntent = new Intent(getApplicationContext(), SettingsActivity.class);
            startActivity(settingsIntent);
        } else if (id == R.id.nav_calls) {
            Intent callsIntent = new Intent(getApplicationContext(), CallActivity.class);
            startActivity(callsIntent);
        } else if (id == R.id.nav_logout) {
            FirebaseAuth.getInstance().signOut();
            Intent logoutIntent = new Intent(HomeActivity.this, AuthActivity.class);
            logoutIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(logoutIntent);
            finish();
        } else {
            Toast.makeText(this, "This is a mock item", Toast.LENGTH_SHORT).show();
        }

        drawerLayout.closeDrawer(GravityCompat.END);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            new MaterialAlertDialogBuilder(HomeActivity.this)
                    .setTitle("Exit Synapse")
                    .setMessage("Are you certain you wish to terminate the Synapse session? Please confirm your decision.")
                    .setIcon(R.drawable.baseline_logout_black_48dp)
                    .setPositiveButton("Exit", (_dialog, _which) -> finishAffinity())
                    .setNegativeButton("Cancel", null)
                    .create()
                    .show();
        }
    }

    public ViewPager2 getViewPager() {
        return viewPager;
    }
}