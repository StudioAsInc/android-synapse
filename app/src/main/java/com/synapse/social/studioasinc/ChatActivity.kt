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
    
    private val messagesList = ArrayList<HashMap<String, Any?>>()
    private var otherUserData: Map<String, Any?>? = null
    private var currentUserId: String? = null

    // UI Components
    private lateinit var backButton: ImageView
    private lateinit var userAvatarImageView: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var userStatusTextView: TextView
    private lateinit var moreButton: ImageView
    private lateinit var messagesRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageView
    private lateinit var attachButton: ImageView
    private lateinit var voiceButton: ImageView
    private lateinit var replyLayout: LinearLayout
    private lateinit var replyTextView: TextView
    private lateinit var replyCloseButton: ImageView

    
    // Chat adapter and UI handlers
    private lateinit var chatAdapter: RecyclerView.Adapter<*>
    private lateinit var chatUIUpdater: ChatUIUpdater
    private lateinit var keyboardHandler: ChatKeyboardHandler
    private val repliedMessagesCache = HashMap<String, HashMap<String, Any?>>()

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
        moreButton = findViewById(R.id.ic_more)
        
        // Message components
        messagesRecyclerView = findViewById(R.id.ChatMessagesListRecycler)
        messageEditText = findViewById(R.id.message_et)
        sendButton = findViewById(R.id.btn_sendMessage)
        attachButton = findViewById(R.id.galleryBtn)
        voiceButton = findViewById(R.id.btn_voice_message)
        
        // Reply components
        replyLayout = findViewById(R.id.mMessageReplyLayout)
        replyTextView = findViewById(R.id.mMessageReplyLayoutBodyRightMessage)
        replyCloseButton = findViewById(R.id.mMessageReplyLayoutBodyCancel)
        
        setupClickListeners()
        setupRecyclerView()
        setupMessageInput()
        setupUIHandlers()
        initializeUI()
    }
    
    private fun initializeUI() {
        userNameTextView.text = "Loading..."
        userStatusTextView.text = "Connecting..."
        replyLayout.visibility = View.GONE
        sendButton.isEnabled = false
        sendButton.alpha = 0.5f
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
        
        voiceButton.setOnClickListener {
            startVoiceRecording()
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
        
        // Create a simple adapter without listener for now
        chatAdapter = SimpleChatAdapter(messagesList)
        
        messagesRecyclerView.adapter = chatAdapter

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
                
                // Show/hide voice button
                voiceButton.visibility = if (hasText) View.GONE else View.VISIBLE
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }
    
    private fun setupUIHandlers() {
        // Initialize ChatUIUpdater
        chatUIUpdater = ChatUIUpdater(
            context = this,
            chatId = chatId ?: "",
            recyclerView = messagesRecyclerView,
            adapter = chatAdapter,
            messagesList = messagesList
        )
        chatUIUpdater.setStatusTextView(userStatusTextView)
        
        // Initialize ChatKeyboardHandler
        keyboardHandler = ChatKeyboardHandler(
            activity = this,
            rootView = findViewById(android.R.id.content),
            messageInput = messageEditText,
            recyclerView = messagesRecyclerView
        )
        
        // Setup keyboard callbacks
        keyboardHandler.setOnKeyboardOpenListener { height ->
            // Keyboard opened - scroll to bottom
            messagesRecyclerView.scrollToPosition(0)
        }
        
        keyboardHandler.setOnKeyboardCloseListener {
            // Keyboard closed
        }
        
        // Handle input focus
        keyboardHandler.handleInputFocus()
    }

    private fun initializeLogic() {
        stateColor(0xFFFFFFFF.toInt(), 0xFFFFFFFF.toInt())
        imageColor(backButton, 0xFF616161.toInt())
        imageColor(moreButton, 0xFF616161.toInt())
        imageColor(sendButton, 0xFF2196F3.toInt())
        imageColor(attachButton, 0xFF616161.toInt())
        imageColor(voiceButton, 0xFF616161.toInt())

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
                // Convert ChatMessage objects to HashMap format for adapter
                val messagesData = messages.map { message ->
                    HashMap<String, Any?>().apply {
                        put("key", message.id)
                        put("uid", message.senderId)
                        put("message_text", message.messageText)
                        put("TYPE", message.messageType)
                        put("message_state", message.messageState)
                        put("push_date", message.pushDate)
                        if (message.repliedMessageId != null) {
                            put("replied_message_id", message.repliedMessageId)
                        }
                        if (message.attachments != null) {
                            put("attachments", message.attachments)
                        }
                    }
                }.reversed() // Reverse for RecyclerView display
                
                // Initialize UI updater with messages
                chatUIUpdater.initializeWithMessages(messagesData)
                
                // Start real-time updates
                chatUIUpdater.startUpdates()
                
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
                    messageType = "MESSAGE",
                    repliedMessageId = replyMessageId
                )
                
                result.fold(
                    onSuccess = { messageId ->
                        messageEditText.setText("")
                        hideReplyLayout()
                        stopTyping()
                        
                        // Add message to local list immediately for better UX
                        val newMessage = HashMap<String, Any?>().apply {
                            put("key", messageId)
                            put("uid", currentUserId!!)
                            put("message_text", messageText)
                            put("TYPE", "MESSAGE")
                            put("message_state", "sent")
                            put("push_date", System.currentTimeMillis())
                            if (replyMessageId != null) {
                                put("replied_message_id", replyMessageId!!)
                            }
                        }
                        
                        // Add message through UI updater for proper handling
                        chatUIUpdater.addMessageImmediately(newMessage)
                        
                        // Handle keyboard after sending
                        keyboardHandler.handleMessageSent()
                        
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
        val options = arrayOf("View Profile", "Clear Chat", "Block User", "Report User")
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Chat Options")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> { // View Profile
                    if (otherUserId != null) {
                        val intent = Intent(this, ProfileActivity::class.java)
                        intent.putExtra("uid", otherUserId)
                        startActivity(intent)
                    }
                }
                1 -> { // Clear Chat
                    showClearChatConfirmation()
                }
                2 -> { // Block User
                    showBlockUserConfirmation()
                }
                3 -> { // Report User
                    showReportUserDialog()
                }
            }
        }
        builder.show()
    }

    private fun showClearChatConfirmation() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Clear Chat")
        builder.setMessage("Are you sure you want to clear this chat? This action cannot be undone.")
        builder.setPositiveButton("Clear") { _, _ ->
            clearChat()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun clearChat() {
        if (chatId == null || currentUserId == null) return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                loadingDialog(true)
                val result = chatService.clearChatForUser(chatId!!, currentUserId!!)
                result.fold(
                    onSuccess = {
                        messagesList.clear()
                        chatAdapter.notifyDataSetChanged()
                        Toast.makeText(this@ChatActivity, "Chat cleared", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        showError("Failed to clear chat: ${error.message}")
                    }
                )
                loadingDialog(false)
            } catch (e: Exception) {
                showError("Error clearing chat: ${e.message}")
                loadingDialog(false)
            }
        }
    }

    private fun showBlockUserConfirmation() {
        if (otherUserId == null) return
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Block User")
        builder.setMessage("Are you sure you want to block this user? They won't be able to message you.")
        builder.setPositiveButton("Block") { _, _ ->
            blockUser()
        }
        builder.setNegativeButton("Cancel", null)
        builder.show()
    }

    private fun blockUser() {
        if (otherUserId == null || currentUserId == null) return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                loadingDialog(true)
                val blockData = mapOf(
                    "blocker_id" to currentUserId!!,
                    "blocked_id" to otherUserId!!,
                    "reason" to "Blocked from chat"
                )
                
                val result = databaseService.insert("blocked_users", blockData)
                result.fold(
                    onSuccess = {
                        Toast.makeText(this@ChatActivity, "User blocked", Toast.LENGTH_SHORT).show()
                        finish()
                    },
                    onFailure = { error ->
                        showError("Failed to block user: ${error.message}")
                    }
                )
                loadingDialog(false)
            } catch (e: Exception) {
                showError("Error blocking user: ${e.message}")
                loadingDialog(false)
            }
        }
    }

    private fun showReportUserDialog() {
        if (otherUserId == null) return
        
        val reasons = arrayOf("Spam", "Harassment", "Inappropriate Content", "Fake Account", "Other")
        
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Report User")
        builder.setItems(reasons) { _, which ->
            reportUser(reasons[which])
        }
        builder.show()
    }

    private fun reportUser(reason: String) {
        if (otherUserId == null || currentUserId == null) return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                loadingDialog(true)
                val reportData = mapOf(
                    "reporter_id" to currentUserId!!,
                    "reported_user_id" to otherUserId!!,
                    "target_type" to "user",
                    "reason" to reason,
                    "description" to "Reported from chat"
                )
                
                val result = databaseService.insert("reports", reportData)
                result.fold(
                    onSuccess = {
                        Toast.makeText(this@ChatActivity, "User reported", Toast.LENGTH_SHORT).show()
                    },
                    onFailure = { error ->
                        showError("Failed to report user: ${error.message}")
                    }
                )
                loadingDialog(false)
            } catch (e: Exception) {
                showError("Error reporting user: ${e.message}")
                loadingDialog(false)
            }
        }
    }

    private fun showAttachmentOptions() {
        // Handle keyboard when showing attachments
        keyboardHandler.handleAttachmentPress()
        
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
        
        // Clean up UI handlers
        if (::chatUIUpdater.isInitialized) {
            chatUIUpdater.stopUpdates()
        }
        if (::keyboardHandler.isInitialized) {
            keyboardHandler.cleanup()
        }
    }
    
    override fun onBackPressed() {
        // Let keyboard handler handle back press first
        if (::keyboardHandler.isInitialized && keyboardHandler.handleBackPress()) {
            return
        }
        
        // Otherwise, use normal back press handling
        chatHelper.onBackPressed()
    }
    
    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        if (::keyboardHandler.isInitialized) {
            keyboardHandler.handleConfigurationChange()
        }
    }

    // Message interaction handlers
    private fun onMessageClick(messageId: String, position: Int) {
        // Handle message click (e.g., show message details)
    }

    private fun onMessageLongClick(messageId: String, position: Int): Boolean {
        // Handle message long click (e.g., show context menu)
        return true
    }

    private fun showReplyLayout(messageId: String, messageText: String, senderName: String) {
        replyMessageId = messageId
        replyTextView.text = "Replying to $senderName: $messageText"
        replyLayout.visibility = View.VISIBLE
        messageEditText.requestFocus()
    }

    private fun openAttachment(attachmentUrl: String, attachmentType: String) {
        when (attachmentType) {
            "image" -> {
                // Open image viewer
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(attachmentUrl), "image/*")
                startActivity(intent)
            }
            "video" -> {
                // Open video player
                val intent = Intent(Intent.ACTION_VIEW)
                intent.setDataAndType(Uri.parse(attachmentUrl), "video/*")
                startActivity(intent)
            }
            else -> {
                // Open in browser or appropriate app
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(attachmentUrl))
                startActivity(intent)
            }
        }
    }
    
    private fun showMessageOptions(messageId: String, position: Int) {
        // TODO: Implement message options (copy, delete, reply, etc.)
        SketchwareUtil.showMessage(this, "Message options coming soon")
    }
    
    private fun startVoiceRecording() {
        // Handle keyboard when recording voice
        keyboardHandler.handleVoiceRecording(true)
        
        // TODO: Implement voice recording
        SketchwareUtil.showMessage(this, "Voice recording coming soon")
    }
    
    private fun toggleEmojiKeyboard() {
        // TODO: Implement emoji keyboard
        SketchwareUtil.showMessage(this, "Emoji keyboard coming soon")
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