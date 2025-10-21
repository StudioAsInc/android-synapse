package com.synapse.social.studioasinc

import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.chat.interfaces.ChatRoom
import com.synapse.social.studioasinc.chat.service.SupabaseChatService
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class InboxChatsFragment : Fragment() {

    // Supabase services
    private val authService = SupabaseAuthenticationService()
    private val databaseService = SupabaseDatabaseService()
    private val chatService = SupabaseChatService(databaseService)

    private val chatsList = mutableListOf<ChatRoom>()
    private val usersCache = mutableMapOf<String, Map<String, Any?>>()

    // UI Components
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var chatsRecyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout
    private lateinit var emptyStateText: TextView
    private lateinit var loadingLayout: LinearLayout
    private lateinit var loadingProgressBar: ProgressBar

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_inbox_chats, container, false)
        initializeViews(view)
        setupRecyclerView()
        loadChats()
        return view
    }

    private fun initializeViews(view: View) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        chatsRecyclerView = view.findViewById(R.id.chatsRecyclerView)
        emptyStateLayout = view.findViewById(R.id.emptyStateLayout)
        emptyStateText = view.findViewById(R.id.emptyStateText)
        loadingLayout = view.findViewById(R.id.loadingLayout)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)

        swipeRefreshLayout.setOnRefreshListener {
            loadChats()
        }

        emptyStateText.text = "No conversations yet\nStart a new chat to begin messaging"
    }

    private fun setupRecyclerView() {
        chatsRecyclerView.layoutManager = LinearLayoutManager(context)
        chatsRecyclerView.adapter = ChatsAdapter(chatsList) { chatRoom ->
            openChat(chatRoom)
        }
    }

    private fun loadChats() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                showLoading(true)
                
                val currentUserId = authService.getCurrentUserId()
                if (currentUserId == null) {
                    showError("User not authenticated")
                    return@launch
                }

                val result = chatService.getUserChats(currentUserId)
                
                result.fold(
                    onSuccess = { chats ->
                        chatsList.clear()
                        chatsList.addAll(chats)
                        
                        // Load user data for chat participants
                        loadUsersData(chats)
                        
                        updateUI()
                    },
                    onFailure = { error ->
                        showError("Failed to load chats: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                showLoading(false)
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private suspend fun loadUsersData(chats: List<ChatRoom>) {
        val currentUserId = authService.getCurrentUserId() ?: return
        
        val userIds = chats.flatMap { chat ->
            chat.participants.filter { it != currentUserId }
        }.distinct()

        userIds.forEach { userId ->
            if (!usersCache.containsKey(userId)) {
                val userResult = databaseService.selectWhere("users", "*", "uid", userId)
                userResult.fold(
                    onSuccess = { users ->
                        val user = users.firstOrNull()
                        if (user != null) {
                            usersCache[userId] = user
                        }
                    },
                    onFailure = { }
                )
            }
        }
    }

    private fun openChat(chatRoom: ChatRoom) {
        val intent = Intent(context, ChatActivity::class.java)
        intent.putExtra("chatId", chatRoom.id)
        intent.putExtra("isGroup", chatRoom.isGroup)
        
        if (!chatRoom.isGroup && chatRoom.participants.size == 2) {
            val currentUserId = authService.getCurrentUserId()
            val otherUserId = chatRoom.participants.find { it != currentUserId }
            intent.putExtra("uid", otherUserId)
        }
        
        startActivity(intent)
    }

    private fun showLoading(show: Boolean) {
        if (show) {
            loadingLayout.visibility = View.VISIBLE
            chatsRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.GONE
        } else {
            loadingLayout.visibility = View.GONE
        }
    }

    private fun updateUI() {
        if (chatsList.isEmpty()) {
            chatsRecyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
        } else {
            chatsRecyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            chatsRecyclerView.adapter?.notifyDataSetChanged()
        }
    }

    private fun showError(message: String) {
        context?.let {
            SketchwareUtil.showMessage(it, message)
        }
    }

    // RecyclerView Adapter for Chats
    inner class ChatsAdapter(
        private val chats: List<ChatRoom>,
        private val onChatClick: (ChatRoom) -> Unit
    ) : RecyclerView.Adapter<ChatsAdapter.ChatViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_conversation, parent, false)
            return ChatViewHolder(view)
        }

        override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
            val chat = chats[position]
            holder.bind(chat)
        }

        override fun getItemCount(): Int = chats.size

        inner class ChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val avatarImageView: ImageView = itemView.findViewById(R.id.avatarImageView)
            private val nameTextView: TextView = itemView.findViewById(R.id.nameTextView)
            private val lastMessageTextView: TextView = itemView.findViewById(R.id.lastMessageTextView)
            private val timeTextView: TextView = itemView.findViewById(R.id.timeTextView)
            private val unreadBadge: TextView = itemView.findViewById(R.id.unreadBadge)
            private val onlineIndicator: View = itemView.findViewById(R.id.onlineIndicator)

            fun bind(chat: ChatRoom) {
                val currentUserId = authService.getCurrentUserId()
                
                if (chat.isGroup) {
                    // Group chat
                    nameTextView.text = chat.groupName ?: "Group Chat"
                    
                    if (chat.groupAvatar != null) {
                        Glide.with(itemView.context)
                            .load(Uri.parse(chat.groupAvatar))
                            .placeholder(R.drawable.group_avatar_placeholder)
                            .into(avatarImageView)
                    } else {
                        avatarImageView.setImageResource(R.drawable.group_avatar_placeholder)
                    }
                    
                    onlineIndicator.visibility = View.GONE
                } else {
                    // Direct chat
                    val otherUserId = chat.participants.find { it != currentUserId }
                    val otherUser = otherUserId?.let { usersCache[it] }
                    
                    if (otherUser != null) {
                        val nickname = otherUser["nickname"]?.toString()
                        val username = otherUser["username"]?.toString()
                        
                        nameTextView.text = if (nickname != "null" && !nickname.isNullOrEmpty()) {
                            nickname
                        } else {
                            "@$username"
                        }
                        
                        val avatar = otherUser["avatar"]?.toString()
                        if (avatar != "null" && !avatar.isNullOrEmpty()) {
                            Glide.with(itemView.context)
                                .load(Uri.parse(avatar))
                                .placeholder(R.drawable.avatar)
                                .into(avatarImageView)
                        } else {
                            avatarImageView.setImageResource(R.drawable.avatar)
                        }
                        
                        // Show online status
                        val status = otherUser["status"]?.toString()
                        onlineIndicator.visibility = if (status == "online") View.VISIBLE else View.GONE
                    } else {
                        nameTextView.text = "Unknown User"
                        avatarImageView.setImageResource(R.drawable.avatar)
                        onlineIndicator.visibility = View.GONE
                    }
                }
                
                // Last message
                lastMessageTextView.text = chat.lastMessageText ?: "No messages yet"
                
                // Time
                if (chat.lastMessageTime != null) {
                    timeTextView.text = formatTime(chat.lastMessageTime)
                } else {
                    timeTextView.text = ""
                }
                
                // Unread count
                if (chat.unreadCount > 0) {
                    unreadBadge.visibility = View.VISIBLE
                    unreadBadge.text = if (chat.unreadCount > 99) "99+" else chat.unreadCount.toString()
                } else {
                    unreadBadge.visibility = View.GONE
                }
                
                // Click listener
                itemView.setOnClickListener {
                    onChatClick(chat)
                }
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        
        return when {
            diff < 60 * 1000 -> "now" // Less than 1 minute
            diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m" // Less than 1 hour
            diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h" // Less than 1 day
            diff < 7 * 24 * 60 * 60 * 1000 -> {
                val days = diff / (24 * 60 * 60 * 1000)
                "${days}d"
            }
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}