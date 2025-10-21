package com.synapse.social.studioasinc

import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.chat.interfaces.*
import com.synapse.social.studioasinc.chat.models.*
import com.synapse.social.studioasinc.chat.service.SupabaseChatService
import com.synapse.social.studioasinc.util.ChatHelper
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    // Supabase services
    private val authService = SupabaseAuthenticationService()
    private val databaseService = SupabaseDatabaseService()
    private val chatService = SupabaseChatService(databaseService)
    private val chatHelper = ChatHelper(this)

    private var synapseLoadingDialog: ProgressDialog? = null
    private var chatId: String? = null
    private var otherUserId: String? = null
    private var isGroup: Boolean = false
    private var replyMessageId: String? = null
    
    private val messagesList = mutableListOf<ChatMessage>()
    private var otherUserData: Map<String, Any?>? = null
    private var currentUserId: String? = null

    // UI Components
    private lateinit var topLayout: LinearLayout
    private lateinit var backButton: ImageView
    private lateinit var userAvatarImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userStatusTextView: TextView
    private lateinit var moreButton: ImageView
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageInputLayout: LinearLayout
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageView
    private lateinit var attachButton: ImageView
    private lateinit var replyLayout: LinearLayout
    private lateinit var replyTextView: TextView
    private lateinit var replyCloseButton: ImageView

    // Typing indicator
    private val typingHandler = Handler(Looper.getMainLooper())
    private var typingRunnable: Runnable? = null
    private var isTyping = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        // Get intent data
        chatId = intent.getStringExtra("chatId")
        otherUserId = intent.getStringExtra("uid")
        isGroup = intent.getBooleanExtra("isGroup", false)
        currentUserId = authService.getCurrentUserId()
        
        if (currentUserId == null) {
            finish()
            return
        }

        initialize()
        initializeLogic()
        loadChatData()
    }

    private fun initialize() {
        // Initialize UI components using existing layout IDs
        backButton = findViewById(R.id.back)
        userAvatarImageView = findViewById(R.id.topProfileLayoutProfileImage)
        userNameTextView = findViewById(R.id.topProfileLayoutUsername)
        userStatusTextView = findViewById(R.id.topProfileLayoutStatus)
        
        // For now, create a simple message display
        // TODO: Add proper RecyclerView and input layout to the existing layout
        
        setupClickListeners()
        initializeSimpleUI()
    }
    
    private fun initializeSimpleUI() {
        // Simple initialization until proper UI is implemented
        userNameTextView.text = "Loading..."
        userStatusTextView.text = "Connecting..."
    }

    private fun setupClickListeners() {
        backButton.setOnClickListener {
            chatHelper.onBackPressed()
        }

        userAvatarImageView.setOnClickListener {
            if (!isGroup && otherUserId != null) {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("uid", otherUserId)
                startActivity(intent)
            }
        }

        userNameTextView.setOnClickListener {
            if (!isGroup && otherUserId != null) {
                val intent = Intent(this, ProfileActivity::class.java)
                intent.putExtra("uid", otherUserId)
                startActivity(intent)
            }
        }
    }

    // TODO: Implement proper RecyclerView and message input when UI is ready

    private fun initializeLogic() {
        stateColor(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
        imageColor(backButton, 0xFF616161.toInt())
    }

    private fun loadChatData() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                loadingDialog(true)
                
                // Create or get chat if needed
                if (chatId == null && otherUserId != null && currentUserId != null) {
                    val chatResult = chatService.getOrCreateChatRoom(currentUserId!!, otherUserId!!)
                    chatResult.fold(
                        onSuccess = { newChatId ->
                            chatId = newChatId
                            loadMessages()
                            loadUserData()
                        },
                        onFailure = { error ->
                            showError("Failed to create chat: ${error.message}")
                            loadingDialog(false)
                        }
                    )
                } else if (chatId != null) {
                    loadMessages()
                    loadUserData()
                } else {
                    showError("Invalid chat parameters")
                    loadingDialog(false)
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
                loadingDialog(false)
            }
        }
    }

    private suspend fun loadMessages() {
        if (chatId == null) return
        
        val result = chatService.getMessages(chatId!!, 50, 0)
        
        result.fold(
            onSuccess = { messages ->
                messagesList.clear()
                messagesList.addAll(messages.reversed()) // Reverse to show newest at bottom
                // TODO: Update RecyclerView when implemented
                
                // TODO: Update UI when RecyclerView is implemented
                
                // Mark messages as read
                val unreadMessageIds = messages
                    .filter { it.senderId != currentUserId && it.messageState != MessageState.READ }
                    .map { it.id }
                
                if (unreadMessageIds.isNotEmpty()) {
                    chatService.markMessagesAsRead(chatId!!, currentUserId!!, unreadMessageIds)
                }
                
                loadingDialog(false)
            },
            onFailure = { error ->
                showError("Failed to load messages: ${error.message}")
                loadingDialog(false)
            }
        )
    }

    private suspend fun loadUserData() {
        if (isGroup || otherUserId == null) {
            loadingDialog(false)
            return
        }
        
        val result = databaseService.selectWhere("users", "*", "uid", otherUserId!!)
        
        result.fold(
            onSuccess = { users ->
                val user = users.firstOrNull()
                if (user != null) {
                    otherUserData = user
                    updateUserUI(user)
                }
                loadingDialog(false)
            },
            onFailure = { error ->
                showError("Failed to load user data: ${error.message}")
                loadingDialog(false)
            }
        )
    }

    private fun updateUserUI(user: Map<String, Any?>) {
        val nickname = user["nickname"]?.toString()
        val username = user["username"]?.toString()
        val avatar = user["avatar"]?.toString()
        val status = user["status"]?.toString()
        
        // Set name
        userNameTextView.text = if (nickname != "null" && !nickname.isNullOrEmpty()) {
            nickname
        } else {
            "@$username"
        }
        
        // Set status
        userStatusTextView.text = when (status) {
            "online" -> "Online"
            "offline" -> "Last seen recently"
            else -> "Offline"
        }
        
        // Set avatar
        if (avatar != "null" && !avatar.isNullOrEmpty()) {
            Glide.with(this)
                .load(Uri.parse(avatar))
                .placeholder(R.drawable.avatar)
                .into(userAvatarImageView)
        } else {
            userAvatarImageView.setImageResource(R.drawable.avatar)
        }
    }

    private fun sendMessage() {
        // TODO: Implement message sending when UI is ready
        showError("Message sending will be implemented when UI is complete")
    }

    private fun startTyping() {
        if (isTyping || chatId == null || currentUserId == null) return
        
        isTyping = true
        
        CoroutineScope(Dispatchers.IO).launch {
            chatService.updateTypingStatus(chatId!!, currentUserId!!, true)
        }
        
        // Stop typing after 3 seconds of inactivity
        typingRunnable?.let { typingHandler.removeCallbacks(it) }
        typingRunnable = Runnable { stopTyping() }
        typingHandler.postDelayed(typingRunnable!!, 3000)
    }

    private fun stopTyping() {
        if (!isTyping || chatId == null || currentUserId == null) return
        
        isTyping = false
        
        CoroutineScope(Dispatchers.IO).launch {
            chatService.updateTypingStatus(chatId!!, currentUserId!!, false)
        }
        
        typingRunnable?.let { typingHandler.removeCallbacks(it) }
    }

    private fun showMoreOptions() {
        // TODO: Implement more options (block user, delete chat, etc.)
        SketchwareUtil.showMessage(this, "More options coming soon")
    }

    private fun showAttachmentOptions() {
        // TODO: Implement attachment options (camera, gallery, files, etc.)
        SketchwareUtil.showMessage(this, "Attachments coming soon")
    }

    private fun hideReplyLayout() {
        replyMessageId = null
        // TODO: Hide reply UI when layout is ready
    }

    private fun showError(message: String) {
        SketchwareUtil.showMessage(this, message)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTyping()
    }

    // Message interaction handlers
    private fun onMessageClick(messageId: String, position: Int) {
        // Handle message click (e.g., show message details)
    }

    private fun onMessageLongClick(messageId: String, position: Int): Boolean {
        // Handle message long click (e.g., show context menu)
        return true
    }

    private fun onReplyClick(messageId: String, messageText: String, senderName: String) {
        replyMessageId = messageId
        // TODO: Implement reply UI when layout is ready
    }

    private fun onAttachmentClick(attachmentUrl: String, attachmentType: String) {
        // Handle attachment click (e.g., open image viewer, download file)
    }

    private fun onUserProfileClick(userId: String) {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("uid", userId)
        startActivity(intent)
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

    private fun createStrokeDrawable(radius: Int, stroke: Int, strokeColor: Int, fillColor: Int): GradientDrawable {
        return GradientDrawable().apply {
            cornerRadius = radius.toFloat()
            setStroke(stroke, strokeColor)
            setColor(fillColor)
        }
    }

    private fun loadingDialog(visibility: Boolean) {
        if (visibility) {
            if (synapseLoadingDialog == null) {
                synapseLoadingDialog = ProgressDialog(this).apply {
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    requestWindowFeature(Window.FEATURE_NO_TITLE)
                    window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                }
            }
            synapseLoadingDialog?.show()
            synapseLoadingDialog?.setContentView(R.layout.loading_synapse)
        } else {
            synapseLoadingDialog?.dismiss()
        }
    }
}