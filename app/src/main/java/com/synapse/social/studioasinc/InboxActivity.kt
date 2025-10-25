package com.synapse.social.studioasinc

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import androidx.viewpager.widget.ViewPager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService

class InboxActivity : AppCompatActivity() {

    // Supabase services
    private val authService = SupabaseAuthenticationService()
    private val databaseService = SupabaseDatabaseService()

    // UI Components
    private lateinit var parentLayout: LinearLayout
    private lateinit var appBar: LinearLayout
    private lateinit var contentHolderLayout: LinearLayout
    private lateinit var textview1: TextView
    private lateinit var linear1: LinearLayout
    private lateinit var imageview3: ImageView
    private lateinit var imageview1: ImageView
    private lateinit var imageview2: ImageView
    private lateinit var viewpager1: ViewPager
    private lateinit var bottomnavigation1: BottomNavigationView

    private lateinit var fg: FgFragmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_inbox)
        initialize(savedInstanceState)
        initializeLogic()
    }

    private fun initialize(savedInstanceState: Bundle?) {
        // Initialize UI components
        parentLayout = findViewById(R.id.parentLayout)
        appBar = findViewById(R.id.appBar)
        contentHolderLayout = findViewById(R.id.contentHolderLayout)
        textview1 = findViewById(R.id.textview1)
        linear1 = findViewById(R.id.linear1)
        imageview3 = findViewById(R.id.imageview3)
        imageview1 = findViewById(R.id.imageview1)
        imageview2 = findViewById(R.id.imageview2)
        viewpager1 = findViewById(R.id.viewpager1)
        bottomnavigation1 = findViewById(R.id.bottomnavigation1)
        
        fg = FgFragmentAdapter(applicationContext, supportFragmentManager)

        setupViewPager()
        setupBottomNavigation()
        setupClickListeners()
    }

    private fun setupViewPager() {
        viewpager1.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}

            override fun onPageSelected(position: Int) {
                bottomnavigation1.menu.getItem(position).isChecked = true
            }

            override fun onPageScrollStateChanged(scrollState: Int) {}
        })
        
        viewpager1.adapter = fg
    }

    private fun setupBottomNavigation() {
        // TODO: Implement bottom navigation when proper menu items are available
        bottomnavigation1.setOnNavigationItemSelectedListener { item ->
            // Handle navigation when menu items are properly defined
            true
        }
    }

    private fun setupClickListeners() {
        imageview1.setOnClickListener {
            // Search functionality
            val intent = Intent(applicationContext, SearchActivity::class.java)
            startActivity(intent)
        }

        imageview2.setOnClickListener {
            // Settings or more options
            showMoreOptions()
        }

        imageview3.setOnClickListener {
            // New chat/compose
            startNewChat()
        }
    }

    private fun initializeLogic() {
        stateColor(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
        imageColor(imageview1, 0xFF616161.toInt())
        imageColor(imageview2, 0xFF616161.toInt())
        imageColor(imageview3, 0xFF2196F3.toInt())
        
        appBar.elevation = 4f
        textview1.text = "Messages"
    }

    private fun showMoreOptions() {
        // TODO: Implement more options menu
        SketchwareUtil.showMessage(applicationContext, "More options coming soon")
    }

    private fun startNewChat() {
        // Navigate to search activity to find users to chat with
        val intent = Intent(applicationContext, SearchActivity::class.java)
        intent.putExtra("mode", "chat")
        startActivity(intent)
    }

    override fun onBackPressed() {
        val intent = Intent(applicationContext, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
        finish()
    }

    // Utility functions
    private fun stateColor(statusColor: Int, navigationColor: Int) {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        window.statusBarColor = statusColor
        window.navigationBarColor = navigationColor
    }

    private fun imageColor(image: ImageView, color: Int) {
        image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    /**
     * Fragment Adapter for ViewPager
     */
    inner class FgFragmentAdapter(
        private val context: android.content.Context,
        fm: FragmentManager
    ) : FragmentStatePagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> com.synapse.social.studioasinc.fragments.InboxChatsFragment() // Chat list fragment
                1 -> InboxCallsFragment() // Calls fragment (placeholder)
                2 -> InboxContactsFragment() // Contacts fragment (placeholder)
                else -> com.synapse.social.studioasinc.fragments.InboxChatsFragment()
            }
        }

        override fun getCount(): Int = 3

        override fun getPageTitle(position: Int): CharSequence? {
            return when (position) {
                0 -> "Chats"
                1 -> "Calls"
                2 -> "Contacts"
                else -> null
            }
        }
    }
}

/**
 * Placeholder fragments for the inbox tabs
 * These would be implemented separately with their own functionality
 */
class InboxChatsFragmentSimple : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = TextView(context)
        view.text = "Chat list coming soon\nTap on a user in Search to start chatting"
        view.gravity = android.view.Gravity.CENTER
        view.setTextColor(Color.GRAY)
        view.setPadding(32, 32, 32, 32)
        return view
    }
}

class InboxCallsFragment : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = TextView(context)
        view.text = "Calls feature coming soon"
        view.gravity = android.view.Gravity.CENTER
        view.setTextColor(Color.GRAY)
        return view
    }
}

class InboxContactsFragment : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = TextView(context)
        view.text = "Contacts feature coming soon"
        view.gravity = android.view.Gravity.CENTER
        view.setTextColor(Color.GRAY)
        return view
    }
}