package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import kotlinx.coroutines.*

class SearchActivity : AppCompatActivity() {

    // Supabase services
    private val authService = SupabaseAuthenticationService()
    private val databaseService = SupabaseDatabaseService()

    private val searchedUsersList = mutableListOf<Map<String, Any?>>()

    // UI Components
    private lateinit var body: LinearLayout
    private lateinit var middleLayout: LinearLayout
    private lateinit var bottomSpc: LinearLayout
    private lateinit var topLayout: LinearLayout
    private lateinit var searchUserLayout: LinearLayout
    private lateinit var topLayoutBar: LinearLayout
    private lateinit var topLayoutBarMiddle: LinearLayout
    private lateinit var bottomBar: LinearLayout
    private lateinit var topLayoutBarTitle: TextView
    private lateinit var bottomHome: LinearLayout
    private lateinit var bottomSearch: LinearLayout
    private lateinit var bottomVideos: LinearLayout
    private lateinit var bottomChats: LinearLayout
    private lateinit var bottomProfile: LinearLayout
    private lateinit var bottomHomeIc: ImageView
    private lateinit var bottomSearchIc: ImageView
    private lateinit var bottomVideosIc: ImageView
    private lateinit var bottomChatsIc: ImageView
    private lateinit var bottomProfileIc: ImageView
    private lateinit var topLayoutBarMiddleSearchLayoutCancel: ImageView
    private lateinit var topLayoutBarMiddleSearchLayout: LinearLayout
    private lateinit var topLayoutBarMiddleSearchInput: EditText
    private lateinit var topLayoutBarMiddleSearchLayoutIc: ImageView
    private lateinit var searchUserLayoutRecyclerView: RecyclerView
    private lateinit var searchUserLayoutNoUserFound: TextView

    private lateinit var vbr: Vibrator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        initialize(savedInstanceState)
        initializeLogic()
    }

    private fun initialize(savedInstanceState: Bundle?) {
        // Initialize UI components
        body = findViewById(R.id.body)
        middleLayout = findViewById(R.id.middleLayout)
        bottomSpc = findViewById(R.id.bottomSpc)
        topLayout = findViewById(R.id.topLayout)
        searchUserLayout = findViewById(R.id.SearchUserLayout)
        topLayoutBar = findViewById(R.id.topLayoutBar)
        topLayoutBarMiddle = findViewById(R.id.topLayoutBarMiddle)
        bottomBar = findViewById(R.id.bottomBar)
        topLayoutBarTitle = findViewById(R.id.topLayoutBarTitle)
        bottomHome = findViewById(R.id.bottom_home)
        bottomSearch = findViewById(R.id.bottom_search)
        bottomVideos = findViewById(R.id.bottom_videos)
        bottomChats = findViewById(R.id.bottom_chats)
        bottomProfile = findViewById(R.id.bottom_profile)
        bottomHomeIc = findViewById(R.id.bottom_home_ic)
        bottomSearchIc = findViewById(R.id.bottom_search_ic)
        bottomVideosIc = findViewById(R.id.bottom_videos_ic)
        bottomChatsIc = findViewById(R.id.bottom_chats_ic)
        bottomProfileIc = findViewById(R.id.bottom_profile_ic)
        topLayoutBarMiddleSearchLayoutCancel = findViewById(R.id.topLayoutBarMiddleSearchLayoutCancel)
        topLayoutBarMiddleSearchLayout = findViewById(R.id.topLayoutBarMiddleSearchLayout)
        topLayoutBarMiddleSearchInput = findViewById(R.id.topLayoutBarMiddleSearchInput)
        topLayoutBarMiddleSearchLayoutIc = findViewById(R.id.topLayoutBarMiddleSearchLayoutIc)
        searchUserLayoutRecyclerView = findViewById(R.id.SearchUserLayoutRecyclerView)
        searchUserLayoutNoUserFound = findViewById(R.id.SearchUserLayoutNoUserFound)

        vbr = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupClickListeners() {
        bottomHome.setOnClickListener {
            val intent = Intent(applicationContext, HomeActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }

        bottomSearch.setOnClickListener {
            // Already on search activity
        }

        bottomVideos.setOnClickListener {
            val intent = Intent(applicationContext, LineVideoPlayerActivity::class.java)
            startActivity(intent)
        }

        bottomChats.setOnClickListener {
            val intent = Intent(applicationContext, InboxActivity::class.java)
            startActivity(intent)
        }

        bottomProfile.setOnClickListener {
            val currentUserId = authService.getCurrentUserId()
            if (currentUserId != null) {
                val intent = Intent(applicationContext, ProfileActivity::class.java)
                intent.putExtra("uid", currentUserId)
                startActivity(intent)
            }
        }

        topLayoutBarMiddleSearchLayoutCancel.setOnClickListener {
            searchUserLayout.visibility = View.GONE
            topLayoutBarMiddleSearchLayoutCancel.visibility = View.GONE
        }

        topLayoutBarMiddleSearchLayoutIc.setOnClickListener {
            if (topLayoutBarMiddleSearchInput.text.toString().trim().isEmpty()) {
                showAllUsers()
            } else {
                getSearchedUserReference()
            }
        }
    }

    private fun setupTextWatchers() {
        topLayoutBarMiddleSearchInput.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                if (s.toString().trim().isEmpty()) {
                    showAllUsers()
                    transitionManager(topLayoutBarMiddleSearchLayout, 100)
                    topLayoutBarMiddleSearchLayoutCancel.visibility = View.GONE
                    imageColor(topLayoutBarMiddleSearchLayoutIc, 0xFF757575.toInt())
                } else {
                    search()
                    topLayoutBarMiddleSearchLayoutCancel.visibility = View.VISIBLE
                    transitionManager(topLayoutBarMiddleSearchLayout, 100)
                    imageColor(topLayoutBarMiddleSearchLayoutIc, 0xFF2962FF.toInt())
                }
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable) {}
        })

        topLayoutBarMiddleSearchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == 3) {
                if (topLayoutBarMiddleSearchInput.text.toString().trim().isNotEmpty()) {
                    getSearchedUserReference()
                }
            }
            false
        }
    }

    private fun initializeLogic() {
        topLayoutBarMiddleSearchLayout.background = createStrokeDrawable(28, 0, Color.TRANSPARENT, 0xFFF5F5F5.toInt())
        searchUserLayout.visibility = View.VISIBLE
        topLayoutBarMiddleSearchLayoutCancel.visibility = View.GONE
        
        imageColor(topLayoutBarMiddleSearchLayoutIc, 0xFF757575.toInt())
        imageColor(topLayoutBarMiddleSearchLayoutCancel, 0xFF757575.toInt())
        imageColor(bottomHomeIc, 0xFFBDBDBD.toInt())
        imageColor(bottomSearchIc, 0xFF000000.toInt())
        imageColor(bottomVideosIc, 0xFFBDBDBD.toInt())
        imageColor(bottomChatsIc, 0xFFBDBDBD.toInt())
        imageColor(bottomProfileIc, 0xFFBDBDBD.toInt())
        
        searchUserLayoutRecyclerView.adapter = SearchUserLayoutRecyclerViewAdapter(searchedUsersList)
        searchUserLayoutRecyclerView.layoutManager = LinearLayoutManager(this)
        
        showAllUsers()
    }

    private fun search() {
        getSearchedUserReference()
        searchUserLayout.visibility = View.VISIBLE
        topLayoutBarMiddleSearchLayoutCancel.visibility = View.VISIBLE
    }

    private fun getSearchedUserReference() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val searchText = topLayoutBarMiddleSearchInput.text.toString().trim()
                
                // Search users by username using LIKE query
                val result = databaseService.select("users", "*")
                
                result.fold(
                    onSuccess = { users ->
                        val filteredUsers = users.filter { user ->
                            val username = user["username"]?.toString() ?: ""
                            username.contains(searchText, ignoreCase = true)
                        }.take(50)
                        
                        if (filteredUsers.isNotEmpty()) {
                            searchUserLayoutRecyclerView.visibility = View.VISIBLE
                            searchUserLayoutNoUserFound.visibility = View.GONE
                            
                            searchedUsersList.clear()
                            searchedUsersList.addAll(filteredUsers)
                            searchUserLayoutRecyclerView.adapter?.notifyDataSetChanged()
                        } else {
                            searchUserLayoutRecyclerView.visibility = View.GONE
                            searchUserLayoutNoUserFound.visibility = View.VISIBLE
                        }
                    },
                    onFailure = { error ->
                        searchUserLayoutRecyclerView.visibility = View.GONE
                        searchUserLayoutNoUserFound.visibility = View.VISIBLE
                        SketchwareUtil.showMessage(applicationContext, "Search error: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                searchUserLayoutRecyclerView.visibility = View.GONE
                searchUserLayoutNoUserFound.visibility = View.VISIBLE
                SketchwareUtil.showMessage(applicationContext, "Error: ${e.message}")
            }
        }
    }

    private fun showAllUsers() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val searchText = topLayoutBarMiddleSearchInput.text.toString().trim()
                
                val result = if (searchText.isEmpty()) {
                    // Show all users (limited to 50)
                    databaseService.select("users", "*")
                } else {
                    // Perform search
                    databaseService.select("users", "*")
                }
                
                result.fold(
                    onSuccess = { users ->
                        val filteredUsers = if (searchText.isEmpty()) {
                            users.take(50)
                        } else {
                            users.filter { user ->
                                val username = user["username"]?.toString() ?: ""
                                username.contains(searchText, ignoreCase = true)
                            }.take(50)
                        }
                        
                        if (filteredUsers.isNotEmpty()) {
                            searchUserLayoutRecyclerView.visibility = View.VISIBLE
                            searchUserLayoutNoUserFound.visibility = View.GONE
                            
                            searchedUsersList.clear()
                            searchedUsersList.addAll(filteredUsers)
                            searchUserLayoutRecyclerView.adapter?.notifyDataSetChanged()
                        } else {
                            searchUserLayoutRecyclerView.visibility = View.GONE
                            searchUserLayoutNoUserFound.visibility = View.VISIBLE
                        }
                    },
                    onFailure = { error ->
                        searchUserLayoutRecyclerView.visibility = View.GONE
                        searchUserLayoutNoUserFound.visibility = View.VISIBLE
                        SketchwareUtil.showMessage(applicationContext, "Error loading users: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                searchUserLayoutRecyclerView.visibility = View.GONE
                searchUserLayoutNoUserFound.visibility = View.VISIBLE
                SketchwareUtil.showMessage(applicationContext, "Error: ${e.message}")
            }
        }
    }

    override fun onBackPressed() {
        val intent = Intent(applicationContext, HomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN)
    }

    // Utility functions
    private fun imageColor(image: ImageView, color: Int) {
        image.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
    }

    private fun createStrokeDrawable(radius: Int, stroke: Int, strokeColor: Int, fillColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = radius.toFloat()
            setStroke(stroke, strokeColor)
            setColor(fillColor)
        }
    }

    private fun transitionManager(view: View, duration: Int) {
        // Simple transition implementation
        view.animate().setDuration(duration.toLong()).start()
    }

    private fun setMargin(view: View, right: Int, left: Int, top: Int, bottom: Int) {
        val params = view.layoutParams as? ViewGroup.MarginLayoutParams
        params?.setMargins(left, top, right, bottom)
        view.layoutParams = params
    }

    // RecyclerView Adapter
    inner class SearchUserLayoutRecyclerViewAdapter(
        private val data: List<Map<String, Any?>>
    ) : RecyclerView.Adapter<SearchUserLayoutRecyclerViewAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val inflater = layoutInflater
            val view = inflater.inflate(R.layout.synapse_users_list_cv, null)
            val layoutParams = RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            view.layoutParams = layoutParams
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            try {
                val item = data[position]
                
                // Set layout params
                val layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                holder.itemView.layoutParams = layoutParams
                
                // Style components
                holder.profileCard.background = createGradientDrawable(300, Color.TRANSPARENT)
                holder.userStatusCircleBG.background = createGradientDrawable(300, 0xFFFFFFFF.toInt())
                holder.userStatusCircleIN.background = createGradientDrawable(300, 0xFF2196F3.toInt())
                
                // Set user data
                holder.name.text = "@${item["username"]}"
                
                // Set avatar
                val banned = item["banned"]?.toString() == "true"
                val avatar = item["avatar"]?.toString()
                
                if (banned) {
                    holder.profileAvatar.setImageResource(R.drawable.banned_avatar)
                } else if (avatar == "null" || avatar.isNullOrEmpty()) {
                    holder.profileAvatar.setImageResource(R.drawable.avatar)
                } else {
                    Glide.with(applicationContext).load(Uri.parse(avatar)).into(holder.profileAvatar)
                }
                
                // Set nickname or username
                val nickname = item["nickname"]?.toString()
                holder.username.text = if (nickname == "null" || nickname.isNullOrEmpty()) {
                    "@${item["username"]}"
                } else {
                    nickname
                }
                
                // Set gender badge
                val gender = item["gender"]?.toString()
                when (gender) {
                    "hidden" -> holder.genderBadge.visibility = View.GONE
                    "male" -> {
                        holder.genderBadge.setImageResource(R.drawable.male_badge)
                        holder.genderBadge.visibility = View.VISIBLE
                    }
                    "female" -> {
                        holder.genderBadge.setImageResource(R.drawable.female_badge)
                        holder.genderBadge.visibility = View.VISIBLE
                    }
                    else -> holder.genderBadge.visibility = View.GONE
                }
                
                // Set account badge
                val accountType = item["account_type"]?.toString()
                when (accountType) {
                    "admin" -> {
                        holder.badge.setImageResource(R.drawable.admin_badge)
                        holder.badge.visibility = View.VISIBLE
                    }
                    "moderator" -> {
                        holder.badge.setImageResource(R.drawable.moderator_badge)
                        holder.badge.visibility = View.VISIBLE
                    }
                    "support" -> {
                        holder.badge.setImageResource(R.drawable.support_badge)
                        holder.badge.visibility = View.VISIBLE
                    }
                    "user" -> {
                        val isPremium = item["account_premium"]?.toString() == "true"
                        val isVerified = item["verify"]?.toString() == "true"
                        
                        when {
                            isPremium -> {
                                holder.badge.setImageResource(R.drawable.premium_badge)
                                holder.badge.visibility = View.VISIBLE
                            }
                            isVerified -> {
                                holder.badge.setImageResource(R.drawable.verified_badge)
                                holder.badge.visibility = View.VISIBLE
                            }
                            else -> holder.badge.visibility = View.GONE
                        }
                    }
                    else -> holder.badge.visibility = View.GONE
                }
                
                // Set online status
                val status = item["status"]?.toString()
                holder.userStatusCircleBG.visibility = if (status == "online") View.VISIBLE else View.GONE
                
                // Set margins
                if (position == 0) {
                    setMargin(holder.body, 18, 18, 10, 10)
                } else {
                    setMargin(holder.body, 18, 18, 0, 10)
                }
                
                // Set click listener
                holder.body.setOnClickListener {
                    val intent = Intent(applicationContext, ProfileActivity::class.java)
                    intent.putExtra("uid", item["uid"]?.toString())
                    intent.putExtra("origin", "SearchActivity")
                    startActivity(intent)
                    finish()
                }
                
            } catch (e: Exception) {
                // Handle error silently
            }
        }

        override fun getItemCount(): Int = data.size

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val cardview1: androidx.cardview.widget.CardView = itemView.findViewById(R.id.cardview1)
            val body: LinearLayout = itemView.findViewById(R.id.body)
            val profileCardRelative: RelativeLayout = itemView.findViewById(R.id.profileCardRelative)
            val lin: LinearLayout = itemView.findViewById(R.id.lin)
            val profileCard: androidx.cardview.widget.CardView = itemView.findViewById(R.id.profileCard)
            val profileRelativeUp: LinearLayout = itemView.findViewById(R.id.ProfileRelativeUp)
            val profileAvatar: ImageView = itemView.findViewById(R.id.profileAvatar)
            val userStatusCircleBG: LinearLayout = itemView.findViewById(R.id.userStatusCircleBG)
            val userStatusCircleIN: LinearLayout = itemView.findViewById(R.id.userStatusCircleIN)
            val usr: LinearLayout = itemView.findViewById(R.id.usr)
            val name: TextView = itemView.findViewById(R.id.name)
            val username: TextView = itemView.findViewById(R.id.username)
            val genderBadge: ImageView = itemView.findViewById(R.id.genderBadge)
            val badge: ImageView = itemView.findViewById(R.id.badge)
        }
    }

    private fun createGradientDrawable(radius: Int, color: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = radius.toFloat()
            setColor(color)
        }
    }
}