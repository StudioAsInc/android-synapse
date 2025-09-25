package com.synapse.social.studioasinc

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.synapse.social.studioasinc.model.User
import com.synapse.social.studioasinc.util.SupabaseManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class NewGroupActivity : AppCompatActivity() {

    private lateinit var usersRecyclerView: RecyclerView
    private lateinit var searchView: SearchView
    private lateinit var fabNext: FloatingActionButton

    private lateinit var usersAdapter: UsersAdapter
    private val usersList = mutableListOf<User>()
    private val selectedUsers = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_group)

        usersRecyclerView = findViewById(R.id.users_recycler_view)
        searchView = findViewById(R.id.search_view)
        fabNext = findViewById(R.id.fab_next)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Add Members"

        usersAdapter = UsersAdapter(usersList) { user, isChecked ->
            if (isChecked) {
                selectedUsers.add(user.uid)
            } else {
                selectedUsers.remove(user.uid)
            }
        }

        usersRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@NewGroupActivity)
            adapter = usersAdapter
        }

        fabNext.setOnClickListener {
            if (selectedUsers.isNotEmpty()) {
                val intent = Intent(this, CreateGroupActivity::class.java)
                intent.putStringArrayListExtra("selected_users", ArrayList(selectedUsers))
                startActivity(intent)
            } else {
                Toast.makeText(this, "Select at least one member", Toast.LENGTH_SHORT).show()
            }
        }

        setupSearch()
        fetchUsers()
    }

    private fun setupSearch() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                usersAdapter.filter(newText)
                return true
            }
        })
    }

    private fun fetchUsers() {
        GlobalScope.launch {
            try {
                val users = SupabaseManager.getUsers()
                if (users != null) {
                    usersList.clear()
                    for (userMap in users) {
                        val user = User(
                            userMap["id"] as? String ?: "",
                            userMap["username"] as? String ?: "",
                            userMap["nickname"] as? String ?: "",
                            userMap["avatar"] as? String ?: ""
                        )
                        usersList.add(user)
                    }
                    runOnUiThread {
                        usersAdapter.notifyDataSetChanged()
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@NewGroupActivity, "Failed to load users.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}

class UsersAdapter(
    private var users: List<User>,
    private val onUserSelected: (User, Boolean) -> Unit
) : RecyclerView.Adapter<UsersAdapter.UserViewHolder>() {

    private var filteredUsers: MutableList<User> = users.toMutableList()

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): UserViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user_selectable, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(filteredUsers[position])
    }

    override fun getItemCount(): Int = filteredUsers.size

    fun filter(query: String?) {
        filteredUsers = if (query.isNullOrBlank()) {
            users.toMutableList()
        } else {
            users.filter {
                it.username.contains(query, ignoreCase = true) ||
                it.nickname.contains(query, ignoreCase = true)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    inner class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val userName: TextView = itemView.findViewById(R.id.user_name)
        private val userAvatar: ImageView = itemView.findViewById(R.id.user_avatar)
        private val userCheckbox: CheckBox = itemView.findViewById(R.id.user_checkbox)

        fun bind(user: User) {
            userName.text = if (user.nickname.isNotEmpty()) user.nickname else user.username
            Glide.with(itemView.context).load(user.avatar).placeholder(R.drawable.avatar).into(userAvatar)

            itemView.setOnClickListener {
                userCheckbox.isChecked = !userCheckbox.isChecked
            }

            userCheckbox.setOnCheckedChangeListener { _, isChecked ->
                onUserSelected(user, isChecked)
            }
        }
    }
}