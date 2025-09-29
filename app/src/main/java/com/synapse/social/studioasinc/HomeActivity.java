package com.synapse.social.studioasinc;

import android.content.Intent;
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
import com.synapse.social.studioasinc.backend.IAuthenticationService;
import com.synapse.social.studioasinc.backend.IDatabaseService;
import com.synapse.social.studioasinc.backend.SupabaseAuthService;
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService;
import com.synapse.social.studioasinc.backend.interfaces.IDataListener;
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseReference;

public class HomeActivity extends AppCompatActivity {

    private static final int REELS_TAB_POSITION = 1;
    private IAuthenticationService authService;
    private IDatabaseService dbService;
    private IDatabaseReference udb;
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
        initialize();
        initializeLogic();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (authService.getCurrentUser() != null) {
            PresenceManager.setActivity(authService.getCurrentUser().getUid(), "In Home");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    private void initialize() {
        authService = new SupabaseAuthService();
        dbService = new SupabaseDatabaseService();
        udb = dbService.getReference("skyline/users");

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
            intent.putExtra("uid", authService.getCurrentUser().getUid());
            startActivity(intent);
        });

        settings_button.setOnClickListener(_view -> {
            Intent intent = new Intent(HomeActivity.this, CreatePostActivity.class);
            startActivity(intent);
        });

        IDatabaseReference getReference = udb.child(authService.getCurrentUser().getUid());
        dbService.getData(getReference, new IDataListener() {
            @Override
            public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()) {
                    if (dataSnapshot.child("avatar").getValue(String.class) != null && !dataSnapshot.child("avatar").getValue(String.class).equals("null")) {
                        Glide.with(getApplicationContext()).load(Uri.parse(dataSnapshot.child("avatar").getValue(String.class))).into(nav_profile_ic);
                    } else {
                        nav_profile_ic.setImageResource(R.drawable.ic_account_circle_48px);
                    }
                } else {
                    nav_profile_ic.setImageResource(R.drawable.ic_account_circle_48px);
                }
            }
            @Override
            public void onCancelled(@NonNull IDatabaseError databaseError) {
                nav_profile_ic.setImageResource(R.drawable.ic_account_circle_48px);
            }
        });
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