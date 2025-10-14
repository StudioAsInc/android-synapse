package com.synapse.social.studioasinc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.synapse.social.studioasinc.adapter.ViewPagerAdapter
import com.synapse.social.studioasinc.viewmodel.HomeViewModel

// To-do: Use view binding instead of findViewById.
class HomeActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var settings_button: ImageView
    private lateinit var nav_search_ic: ImageView
    private lateinit var nav_inbox_ic: ImageView
    private lateinit var nav_profile_ic: ImageView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var topBar: View

    companion object {
        private const val REELS_TAB_POSITION = 1
    }

    override fun onCreate(_savedInstanceState: Bundle?) {
        super.onCreate(_savedInstanceState)
        setContentView(R.layout.activity_home)
        FirebaseApp.initializeApp(this)
        initialize()
        initializeLogic()
    }

    override fun onStart() {
        super.onStart()
        // To-do: Replace with Supabase presence.
        if (FirebaseAuth.getInstance().currentUser != null) {
            PresenceManager.setActivity(FirebaseAuth.getInstance().currentUser!!.uid, "In Home")
        }
    }

    private fun initialize() {
        auth = FirebaseAuth.getInstance()
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        settings_button = findViewById(R.id.settings_button)
        nav_search_ic = findViewById(R.id.nav_search_ic)
        nav_inbox_ic = findViewById(R.id.nav_inbox_ic)
        nav_profile_ic = findViewById(R.id.nav_profile_ic)
        topBar = findViewById(R.id.topBar)
    }

    private fun initializeLogic() {
        viewPager.adapter = ViewPagerAdapter(this)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            when (position) {
                0 -> tab.setIcon(R.drawable.home_24px)
                1 -> tab.setIcon(R.drawable.ic_video_library_48px)
                2 -> tab.setIcon(R.drawable.icon_notifications_round)
            }
        }.attach()

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                val toolbarLayoutParams = topBar.layoutParams as AppBarLayout.LayoutParams
                val tabLayoutParams = tabLayout.layoutParams as AppBarLayout.LayoutParams
                if (tab.position == REELS_TAB_POSITION) {
                    toolbarLayoutParams.scrollFlags = 0
                    tabLayoutParams.scrollFlags = 0
                    topBar.visibility = View.GONE // Hide the top bar
                } else {
                    toolbarLayoutParams.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    tabLayoutParams.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    topBar.visibility = View.VISIBLE // Show the top bar
                }
                topBar.layoutParams = toolbarLayoutParams
                tabLayout.layoutParams = tabLayoutParams
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        nav_search_ic.setOnClickListener {
            val intent = Intent(applicationContext, SearchActivity::class.java)
            startActivity(intent)
        }

        nav_inbox_ic.setOnClickListener {
            val intent = Intent(applicationContext, InboxActivity::class.java)
            startActivity(intent)
        }

        nav_profile_ic.setOnClickListener {
            val intent = Intent(applicationContext, ProfileActivity::class.java)
            intent.putExtra("uid", FirebaseAuth.getInstance().currentUser!!.uid)
            startActivity(intent)
        }

        settings_button.setOnClickListener {
            val intent = Intent(this@HomeActivity, CreatePostActivity::class.java)
            startActivity(intent)
        }

        // To-do: Move UI update logic into a separate method.
        homeViewModel.avatarUrl.observe(this) { avatarUrl ->
            if (avatarUrl != null && !avatarUrl.equals("null", ignoreCase = true) && avatarUrl.isNotEmpty()) {
                Glide.with(applicationContext).load(Uri.parse(avatarUrl)).into(nav_profile_ic)
            } else {
                nav_profile_ic.setImageResource(R.drawable.ic_account_circle_48px)
            }
        }

        if (FirebaseAuth.getInstance().currentUser != null) {
            homeViewModel.fetchUserAvatar(FirebaseAuth.getInstance().currentUser!!.uid)
        }
    }

    override fun onBackPressed() {
        MaterialAlertDialogBuilder(this@HomeActivity)
            .setTitle("Exit Synapse")
            .setMessage("Are you certain you wish to terminate the Synapse session? Please confirm your decision.")
            .setIcon(R.drawable.baseline_logout_black_48dp)
            .setPositiveButton("Exit") { _, _ -> finishAffinity() }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }
}
