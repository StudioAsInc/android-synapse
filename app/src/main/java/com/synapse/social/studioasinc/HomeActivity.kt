package com.synapse.social.studioasinc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.synapse.social.studioasinc.adapter.ViewPagerAdapter
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.launch

class HomeActivity : AppCompatActivity() {

    companion object {
        private const val REELS_TAB_POSITION = 1
    }

    // Using Supabase client directly
    private lateinit var settingsButton: ImageView
    private lateinit var navSearchIc: ImageView
    private lateinit var navInboxIc: ImageView
    private lateinit var navProfileIc: ImageView
    private lateinit var tabLayout: TabLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var appBarLayout: AppBarLayout
    private lateinit var topBar: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)
        initialize()
        initializeLogic()
    }

    override fun onStart() {
        super.onStart()
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
        if (currentUser != null) {
            PresenceManager.setActivity(currentUser.id, "In Home")
        }
    }

    private fun initialize() {
        // Using Supabase client directly

        tabLayout = findViewById(R.id.tab_layout)
        viewPager = findViewById(R.id.view_pager)
        settingsButton = findViewById(R.id.settings_button)
        navSearchIc = findViewById(R.id.nav_search_ic)
        navInboxIc = findViewById(R.id.nav_inbox_ic)
        navProfileIc = findViewById(R.id.nav_profile_ic)
        appBarLayout = findViewById(R.id.app_bar_layout)
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
                    topBar.visibility = View.GONE
                } else {
                    toolbarLayoutParams.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    tabLayoutParams.scrollFlags = AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
                    topBar.visibility = View.VISIBLE
                }
                
                topBar.layoutParams = toolbarLayoutParams
                tabLayout.layoutParams = tabLayoutParams
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        navSearchIc.setOnClickListener {
            // TODO: Enable when SearchActivity is migrated
            // val intent = Intent(applicationContext, SearchActivity::class.java)
            // startActivity(intent)
        }

        navInboxIc.setOnClickListener {
            // TODO: Enable when InboxActivity is migrated
            // val intent = Intent(applicationContext, InboxActivity::class.java)
            // startActivity(intent)
        }

        navProfileIc.setOnClickListener {
            val currentUser = SupabaseClient.client.auth.currentUserOrNull()
            if (currentUser != null) {
                val intent = Intent(applicationContext, ProfileActivity::class.java)
                intent.putExtra("uid", currentUser.id)
                startActivity(intent)
            }
        }

        settingsButton.setOnClickListener {
            val intent = Intent(this@HomeActivity, CreatePostActivity::class.java)
            startActivity(intent)
        }

        loadUserProfileImage()
    }

    private fun loadUserProfileImage() {
        val currentUser = SupabaseClient.client.auth.currentUserOrNull()
        if (currentUser != null) {
            lifecycleScope.launch {
                try {
                    val userData = SupabaseClient.client.from("users")
                        .select(columns = Columns.raw("avatar,profile_image_url")) {
                            filter { eq("id", currentUser.id) }
                        }.decodeSingleOrNull<JsonObject>()
                    
                    if (userData != null) {
                        val avatar = userData["avatar"]?.toString()?.removeSurrounding("\"")
                        val profileImageUrl = userData["profile_image_url"]?.toString()?.removeSurrounding("\"")
                        val imageUrl = avatar ?: profileImageUrl
                        
                        if (!imageUrl.isNullOrEmpty() && imageUrl != "null") {
                            Glide.with(applicationContext)
                                .load(Uri.parse(imageUrl))
                                .circleCrop()
                                .into(navProfileIc)
                        } else {
                            navProfileIc.setImageResource(R.drawable.ic_account_circle_48px)
                        }
                    } else {
                        navProfileIc.setImageResource(R.drawable.ic_account_circle_48px)
                    }
                } catch (e: Exception) {
                    navProfileIc.setImageResource(R.drawable.ic_account_circle_48px)
                }
            }
        } else {
            navProfileIc.setImageResource(R.drawable.ic_account_circle_48px)
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