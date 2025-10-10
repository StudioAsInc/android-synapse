package com.synapse.social.studioasinc;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.synapse.social.studioasinc.adapter.ViewPagerAdapter;

// TODO: Migrate to Supabase
// This activity is the main screen of the app.
// The following needs to be done:
// 1. Replace all Firebase database calls with calls to the `DatabaseService` interface.
// 2. Replace all Firebase auth calls with calls to the `AuthenticationService` interface.
public class HomeActivity extends AppCompatActivity {

    private static final int REELS_TAB_POSITION = 1;
    private ImageView settings_button;
    private ImageView nav_search_ic;
    private ImageView nav_inbox_ic;
    private ImageView nav_profile_ic;
    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private AppBarLayout app_bar_layout;
    private View topBar;

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.activity_home);
        // TODO: Initialize Supabase client in SynapseApp.java instead of here.
        // FirebaseApp.initializeApp(this);
        initialize();
        initializeLogic();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // TODO: Implement presence with Supabase Realtime
        // if (FirebaseAuth.getInstance().getCurrentUser() != null) {
        //     PresenceManager.setActivity(FirebaseAuth.getInstance().getCurrentUser().getUid(), "In Home");
        // }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initialize() {
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        settings_button = findViewById(R.id.settings_button);
        nav_search_ic = findViewById(R.id.nav_search_ic);
        nav_inbox_ic = findViewById(R.id.nav_inbox_ic);
        nav_profile_ic = findViewById(R.id.nav_profile_ic);
        app_bar_layout = findViewById(R.id.app_bar_layout);
        topBar = findViewById(R.id.topBar);
    }

    private void initializeLogic() {
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

        nav_search_ic.setOnClickListener(_view -> {
            Intent intent = new Intent(getApplicationContext(), SearchActivity.class);
            startActivity(intent);
        });

        nav_inbox_ic.setOnClickListener(_view -> {
            Intent intent = new Intent(getApplicationContext(), InboxActivity.class);
            startActivity(intent);
        });

        nav_profile_ic.setOnClickListener(_view -> {
            Intent intent = new Intent(getApplicationContext(), ProfileActivity.class);
            // TODO: Replace with Supabase Auth
            // intent.putExtra("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
            startActivity(intent);
        });

        settings_button.setOnClickListener(_view -> {
            Intent intent = new Intent(HomeActivity.this, CreatePostActivity.class);
            startActivity(intent);
        });

        // TODO: Get user profile from Supabase
    }

    @Override
    public void onBackPressed() {
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