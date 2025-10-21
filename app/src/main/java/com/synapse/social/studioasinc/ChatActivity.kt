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
        // Initialize UI components
        topLayout = findViewById(R.id.topLayout)
        backButton = findViewById(R.id.backButton)
        userAvatarImageView = findViewById(R.id.userAvatarImageView)
        userNameTextView = findViewById(R.id.userNameTextView)
        userStatusTextView = findViewById(R.id.userStatusTextView)
        moreButton = findViewById(R.id.moreButton)
        messagesRecyclerView = findViewById(R.id.messagesRecyclerView)
        messageInputLayout = findViewById(R.id.messageInputLayout)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)
        attachButton = findViewById(R.id.attachButton)
        replyLayout = findViewById(R.id.replyLayout)
        replyTextView = findViewById(R.id.replyTextView)
        replyCloseButton = findViewById(R.id.replyCloseButton)

        setupClickListeners()
        setupRecyclerView()
        setupMessageInput()
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

        moreButton.setOnClickListener {
            showMoreOptions()
        }

        sendButton.setOnClickListener {
            sendMessage()
        }

        attachButton.setOnClickListener {
            showAttachmentOptions()
        }

        replyCloseButton.setOnClickListener {
            hideReplyLayout()
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(this)
        layoutManager.reverseLayout = true
        layoutManager.stackFromEnd = true
        messagesRecyclerView.layoutManager = layoutManager
        messagesRecyclerView.adapter = ChatMessagesAdapter(messagesList, this)
    }

    private fun setupMessageInput() {
        messageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val hasText = !s.isNullOrEmpty()
                
                if (hasText && !isTyping) {
                    startTyping()
                } else if (!hasText && isTyping) {
                    stopTyping()
                }
                
                // Update send button state
                sendButton.isEnabled = hasText
                sendButton.alpha = if (hasText) 1.0f else 0.5f
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun initializeLogic() {
        stateColor(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
        topLayout.elevation = 4f
        
        imageColor(backButton, 0xFF616161.toInt())
        imageColor(moreButton, 0xFF616161.toInt())
        imageColor(sendButton, 0xFF2196F3.toInt())
        imageColor(attachButton, 0xFF616161.toInt())
        
        messageInputLayout.background = createStrokeDrawable(25, 2, 0xFFEEEEEE.toInt(), 0xFFFFFFFF.toInt())
        
        replyLayout.visibility = View.GONE
        sendButton.isEnabled = false
        sendButton.alpha = 0.5f
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
                messagesRecyclerView.adapter?.notifyDataSetChanged()
                
                if (messages.isNotEmpty()) {
                    messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                }
                
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
        val messageText = messageEditText.text.toString().trim()
        if (messageText.isEmpty() || chatId == null || currentUserId == null) return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val result = chatService.sendMessage(
                    chatId = chatId!!,
                    senderId = currentUserId!!,
                    receiverId = otherUserId,
                    messageText = messageText,
                    messageType = MessageType.TEXT,
                    repliedMessageId = replyMessageId
                )
                
                result.fold(
                    onSuccess = { messageId ->
                        messageEditText.setText("")
                        hideReplyLayout()
                        stopTyping()
                        
                        // Add message to local list immediately for better UX
                        val newMessage = ChatMessageImpl(
                            id = messageId,
                            chatId = chatId!!,
                            senderId = currentUserId!!,
                            receiverId = otherUserId,
                            messageText = messageText,
                            messageType = MessageType.TEXT,
                            messageState = MessageState.SENT,
                            pushDate = System.currentTimeMillis(),
                            repliedMessageId = replyMessageId
                        )
                        
                        messagesList.add(newMessage)
                        messagesRecyclerView.adapter?.notifyItemInserted(messagesList.size - 1)
                        messagesRecyclerView.scrollToPosition(messagesList.size - 1)
                        
                        replyMessageId = null
                    },
                    onFailure = { error ->
                        showError("Failed to send message: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                showError("Error sending message: ${e.message}")
            }
        }
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
        replyLayout.visibility = View.GONE
        replyMessageId = null
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
        replyTextView.text = "Replying to $senderName: $messageText"
        replyLayout.visibility = View.VISIBLE
        messageEditText.requestFocus()
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