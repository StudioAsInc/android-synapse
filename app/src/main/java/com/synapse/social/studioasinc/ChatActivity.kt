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
import com.google.android.material.button.MaterialButton
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.serialization.json.JsonObject
import com.synapse.social.studioasinc.util.ChatHelper
import com.synapse.social.studioasinc.chat.presentation.MessageActionsViewModel
import com.synapse.social.studioasinc.chat.MessageActionsBottomSheet
import com.synapse.social.studioasinc.chat.DeleteConfirmationDialog
import com.synapse.social.studioasinc.chat.EditMessageDialog
import com.synapse.social.studioasinc.chat.EditHistoryDialog
import com.synapse.social.studioasinc.chat.SwipeToReplyCallback
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.synapse.social.studioasinc.presentation.viewmodel.ChatViewModel
import com.synapse.social.studioasinc.chat.service.RealtimeState
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity(), DefaultLifecycleObserver {

    // Supabase services
    private val chatService = com.synapse.social.studioasinc.backend.SupabaseChatService()
    private val databaseService = com.synapse.social.studioasinc.backend.SupabaseDatabaseService()
    
    private var synapseLoadingDialog: ProgressDialog? = null
    private var chatId: String? = null
    private var otherUserId: String? = null
    private var isGroup: Boolean = false
    private var replyMessageId: String? = null
    
    private val messagesList = ArrayList<HashMap<String, Any?>>()
    private var otherUserData: Map<String, Any?>? = null
    private var currentUserId: String? = null
    private var chatAdapter: ChatAdapter? = null
    
    // ViewModels
    private lateinit var viewModel: MessageActionsViewModel
    private lateinit var chatViewModel: ChatViewModel
    
    // Realtime channel
    private var realtimeChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    // UI Components
    private var recyclerView: RecyclerView? = null
    private var messageInput: EditText? = null
    private var sendButton: MaterialButton? = null
    private var backButton: ImageView? = null
    private var chatNameText: TextView? = null
    private var chatAvatarImage: ImageView? = null
    private var toolContainer: LinearLayout? = null
    
    // Reply preview UI components
    private var replyLayout: LinearLayout? = null
    private var replyUsername: TextView? = null
    private var replyMessage: TextView? = null
    private var replyMediaPreview: ImageView? = null
    private var replyCancelButton: ImageView? = null
    
    // Typing indicator UI components
    private var typingIndicatorView: View? = null
    private var typingText: TextView? = null
    private var typingAnimation: com.synapse.social.studioasinc.widget.TypingAnimationView? = null
    private var typingAvatar: ImageView? = null
    
    // Connection status UI components
    private var connectionStatusBanner: LinearLayout? = null
    private var connectionProgress: ProgressBar? = null
    private var connectionIcon: ImageView? = null
    private var connectionText: TextView? = null
    private var connectionRetryButton: com.google.android.material.button.MaterialButton? = null
    
    // App lifecycle state tracking
    private var isAppInBackground = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super<AppCompatActivity>.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)
        
        // Get intent data
        chatId = intent.getStringExtra("chatId")
        otherUserId = intent.getStringExtra("uid")
        isGroup = intent.getBooleanExtra("isGroup", false)
        currentUserId = SupabaseClient.client.auth.currentUserOrNull()?.id
        
        if (currentUserId == null) {
            finish()
            return
        }

        initialize()
        initializeLogic()
        loadChatData()
        setupRealtimeSubscription()
        
        // Register lifecycle observer for app backgrounding
        lifecycle.addObserver(this)
    }

    private fun initialize() {
        // Initialize UI components from layout
        try {
            recyclerView = findViewById(R.id.ChatMessagesListRecycler)
            messageInput = findViewById(R.id.message_et)
            sendButton = findViewById(R.id.btn_sendMessage)
            backButton = findViewById(R.id.back)
            chatNameText = findViewById(R.id.topProfileLayoutUsername)
            chatAvatarImage = findViewById(R.id.topProfileLayoutProfileImage)
            toolContainer = findViewById(R.id.toolContainer)
            
            // Initialize reply preview components
            replyLayout = findViewById(R.id.mMessageReplyLayout)
            replyUsername = findViewById(R.id.mMessageReplyLayoutBodyRightUsername)
            replyMessage = findViewById(R.id.mMessageReplyLayoutBodyRightMessage)
            replyMediaPreview = findViewById(R.id.mMessageReplyLayoutMediaPreview)
            replyCancelButton = findViewById(R.id.mMessageReplyLayoutBodyCancel)
            
            // Setup RecyclerView with proper configuration
            recyclerView?.apply {
                layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                    stackFromEnd = true
                }
                setHasFixedSize(true)
                
                // Setup swipe-to-reply gesture
                setupSwipeToReply(this)
            }
            
            // Initialize ChatAdapter with full listener implementation
            val chatAdapter = ChatAdapter(
                data = messagesList,
                repliedMessagesCache = HashMap(),
                listener = object : com.synapse.social.studioasinc.chat.interfaces.ChatAdapterListener {
                    override fun onMessageClick(messageId: String, position: Int) {
                        // Handle message click if needed
                    }
                    
                    override fun onMessageLongClick(messageId: String, position: Int): Boolean {
                        showMessageActionsBottomSheet(messageId, position)
                        return true
                    }
                    
                    override fun onReplyClick(messageId: String, messageText: String, senderName: String) {
                        prepareReply(messageId, messageText, senderName)
                    }
                    
                    override fun onAttachmentClick(attachmentUrl: String, attachmentType: String) {
                        when (attachmentType) {
                            "link" -> openUrl(attachmentUrl)
                            "image", "video" -> openMediaViewer(attachmentUrl, attachmentType)
                            else -> openUrl(attachmentUrl)
                        }
                    }
                    
                    override fun onUserProfileClick(userId: String) {
                        openUserProfile(userId)
                    }
                    
                    override fun onMessageRetry(messageId: String, position: Int) {
                        retryFailedMessage(messageId, position)
                    }
                    
                    override fun onReplyAction(messageId: String, messageText: String, senderName: String) {
                        prepareReply(messageId, messageText, senderName)
                    }
                    
                    override fun onForwardAction(messageId: String, messageData: Map<String, Any?>) {
                        showForwardDialog(messageId, messageData)
                    }
                    
                    override fun onEditAction(messageId: String, currentText: String) {
                        showEditDialog(messageId, currentText)
                    }
                    
                    override fun onDeleteAction(messageId: String, deleteForEveryone: Boolean) {
                        showDeleteConfirmation(messageId, deleteForEveryone)
                    }
                    
                    override fun onAISummaryAction(messageId: String, messageText: String) {
                        showAISummary(messageId, messageText)
                    }
                    
                    override fun onEditHistoryClick(messageId: String) {
                        showEditHistory(messageId)
                    }
                }
            )
            recyclerView?.adapter = chatAdapter
            this.chatAdapter = chatAdapter
            
            // Set ChatAdapter reference in ChatViewModel for real-time updates
            chatViewModel.setChatAdapter(chatAdapter)
            
            // Initialize ViewModels
            viewModel = MessageActionsViewModel(this)
            chatViewModel = ViewModelProvider(this)[ChatViewModel::class.java]
            
            // Initialize managers in ChatViewModel
            chatViewModel.initializeManagers(this)
            
            // Setup real-time message state updates for read receipts
            setupMessageStateUpdates()
            
            // Initialize typing indicator
            initializeTypingIndicator()
            
            // Initialize connection status banner
            initializeConnectionStatusBanner()
            
        } catch (e: Exception) {
            android.util.Log.e("ChatActivity", "UI initialization error: ${e.message}", e)
            Toast.makeText(this, "Failed to initialize chat interface", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initializeTypingIndicator() {
        // Find the parent container where we can add the typing indicator
        val bottomContainer = findViewById<LinearLayout>(R.id.message_input_overall_container)?.parent as? LinearLayout
        
        if (bottomContainer != null) {
            // Create typing indicator view programmatically
            val inflater = layoutInflater
            typingIndicatorView = inflater.inflate(R.layout.typing_indicator_item, bottomContainer, false)
            
            // Initialize typing indicator components
            typingText = typingIndicatorView?.findViewById(R.id.typing_text)
            typingAnimation = typingIndicatorView?.findViewById(R.id.typing_animation)
            typingAvatar = typingIndicatorView?.findViewById(R.id.typing_avatar)
            
            // Initially hide the typing indicator
            typingIndicatorView?.visibility = View.GONE
            
            // Add typing indicator above the message input (before the last child which is message input)
            val messageInputIndex = bottomContainer.childCount - 1
            bottomContainer.addView(typingIndicatorView, messageInputIndex)
            
            // Set up typing indicator observer
            setupTypingIndicatorObserver()
        }
    }

    private fun initializeConnectionStatusBanner() {
        // Initialize connection status banner components
        connectionStatusBanner = findViewById(R.id.connection_status_banner)
        connectionProgress = findViewById(R.id.connection_progress)
        connectionIcon = findViewById(R.id.connection_icon)
        connectionText = findViewById(R.id.connection_text)
        connectionRetryButton = findViewById(R.id.connection_retry_button)
        
        // Initially hide the banner
        connectionStatusBanner?.visibility = View.GONE
        
        // Setup retry button click listener
        connectionRetryButton?.setOnClickListener {
            retryConnection()
        }
        
        // Setup connection state observer
        setupConnectionStateObserver()
    }

    private fun initializeLogic() {
        // Setup basic functionality
        sendButton?.apply {
            isEnabled = false
            setOnClickListener { sendMessage() }
        }
        
        backButton?.setOnClickListener {
            // Clean up subscriptions when leaving chat
            chatViewModel.onChatClosed()
            onBackPressedDispatcher.onBackPressed()
        }
        
        // Setup typing indicator
        setupTypingIndicator()
        
        // Start polling for typing indicators from other users
        startTypingIndicatorPolling()
        
        // Setup reply preview observers
        setupReplyPreview()
    }
    
    private fun setupTypingIndicatorObserver() {
        // Observe typing users from ChatViewModel
        lifecycleScope.launch {
            chatViewModel.typingUsers.collect { typingUsers ->
                runOnUiThread {
                    updateTypingIndicator(typingUsers)
                }
            }
        }
    }

    private fun setupConnectionStateObserver() {
        // Observe connection state from realtime service
        lifecycleScope.launch {
            chatViewModel.getRealtimeService()?.connectionState?.collect { state ->
                runOnUiThread {
                    updateConnectionStatusBanner(state)
                }
            }
        }
    }

    private fun loadChatData() {
        lifecycleScope.launch {
            try {
                loadingDialog(true)
                
                // Create or get chat if needed
                if (chatId == null && otherUserId != null && currentUserId != null) {
                    // Create or get direct chat
                    val result = chatService.getOrCreateDirectChat(currentUserId!!, otherUserId!!)
                    result.fold(
                        onSuccess = { createdChatId ->
                            chatId = createdChatId
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
                    showError("Invalid chat configuration")
                    finish()
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
                loadingDialog(false)
            }
        }
    }

    private fun loadMessages() {
        lifecycleScope.launch {
            try {
                if (chatId == null) return@launch
                
                val result = chatService.getMessages(chatId!!)
                result.fold(
                    onSuccess = { messages ->
                        messagesList.clear()
                        // Convert messages to HashMap format for adapter
                        messages.forEach { message ->
                            val messageMap = HashMap<String, Any?>()
                            messageMap["id"] = message["id"]
                            messageMap["chat_id"] = message["chat_id"]
                            messageMap["sender_id"] = message["sender_id"]
                            messageMap["uid"] = message["sender_id"] // For compatibility
                            messageMap["content"] = message["content"]
                            messageMap["message_text"] = message["content"] // For compatibility
                            messageMap["message_type"] = message["message_type"]
                            messageMap["created_at"] = message["created_at"]
                            messageMap["push_date"] = message["created_at"] // For compatibility
                            messageMap["is_deleted"] = message["is_deleted"]
                            messageMap["is_edited"] = message["is_edited"]
                            messagesList.add(messageMap)
                        }
                        
                        chatAdapter?.notifyDataSetChanged()
                        if (messagesList.isNotEmpty()) {
                            recyclerView?.scrollToPosition(messagesList.size - 1)
                        }
                        
                        // Mark messages as read
                        if (currentUserId != null) {
                            chatService.markMessagesAsRead(chatId!!, currentUserId!!)
                        }
                        
                        loadingDialog(false)
                    },
                    onFailure = { error ->
                        showError("Failed to load messages: ${error.message}")
                        loadingDialog(false)
                    }
                )
            } catch (e: Exception) {
                showError("Error loading messages: ${e.message}")
                loadingDialog(false)
            }
        }
    }
    
    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                if (otherUserId == null) return@launch
                
                val result = databaseService.selectWhere("users", "*", "uid", otherUserId!!)
                result.fold(
                    onSuccess = { users ->
                        otherUserData = users.firstOrNull()
                        updateChatHeader()
                    },
                    onFailure = { error ->
                        android.util.Log.e("ChatActivity", "Failed to load user data: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error loading user data: ${e.message}")
            }
        }
    }
    
    private fun updateChatHeader() {
        val userData = otherUserData ?: return
        
        chatNameText?.text = userData["username"]?.toString() ?: "User"
        
        val avatarUrl = userData["avatar"]?.toString()
            ?.takeIf { it.isNotEmpty() && it != "null" }
        
        if (avatarUrl != null) {
            chatAvatarImage?.let { imageView ->
                Glide.with(this)
                    .load(Uri.parse(avatarUrl))
                    .circleCrop()
                    .placeholder(R.drawable.ic_account_circle_48px)
                    .error(R.drawable.ic_account_circle_48px)
                    .into(imageView)
            }
        }
    }
    
    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun loadingDialog(show: Boolean) {
        runOnUiThread {
            if (show) {
                if (synapseLoadingDialog == null) {
                    synapseLoadingDialog = ProgressDialog(this).apply {
                        setMessage("Loading...")
                        setCancelable(false)
                    }
                }
                synapseLoadingDialog?.show()
            } else {
                synapseLoadingDialog?.dismiss()
            }
        }
    }

    private fun sendMessage() {
        val messageText = messageInput?.text?.toString()?.trim()
        if (messageText.isNullOrEmpty()) return
        
        if (chatId == null || currentUserId == null) {
            showError("Chat not initialized")
            return
        }
        
        // Disable send button to prevent double-sending
        sendButton?.isEnabled = false
        
        lifecycleScope.launch {
            try {
                val result = chatService.sendMessage(
                    chatId = chatId!!,
                    senderId = currentUserId!!,
                    content = messageText,
                    messageType = "text",
                    replyToId = replyMessageId
                )
                
                result.fold(
                    onSuccess = { messageId ->
                        // Clear input immediately for better UX
                        runOnUiThread {
                            messageInput?.text?.clear()
                            
                            // Clear reply preview after message sent
                            viewModel.clearReply()
                            replyMessageId = null
                        }
                        
                        // Add message optimistically to UI
                        val newMessage = HashMap<String, Any?>()
                        newMessage["id"] = messageId
                        newMessage["chat_id"] = chatId
                        newMessage["sender_id"] = currentUserId
                        newMessage["uid"] = currentUserId
                        newMessage["content"] = messageText
                        newMessage["message_text"] = messageText
                        newMessage["message_type"] = "text"
                        newMessage["created_at"] = System.currentTimeMillis()
                        newMessage["push_date"] = System.currentTimeMillis()
                        newMessage["is_deleted"] = false
                        newMessage["is_edited"] = false
                        newMessage["delivery_status"] = "sent"
                        
                        // Include reply reference if present
                        if (replyMessageId != null) {
                            newMessage["replied_message_id"] = replyMessageId
                        }
                        
                        runOnUiThread {
                            messagesList.add(newMessage)
                            chatAdapter?.notifyItemInserted(messagesList.size - 1)
                            recyclerView?.scrollToPosition(messagesList.size - 1)
                        }
                    },
                    onFailure = { error ->
                        showError("Failed to send message: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                showError("Error sending message: ${e.message}")
            } finally {
                // Re-enable send button
                runOnUiThread {
                    sendButton?.isEnabled = true
                }
            }
        }
    }
    
    /**
     * Setup typing indicator and toolContainer animation
     */
    private fun setupTypingIndicator() {
        var typingJob: kotlinx.coroutines.Job? = null
        
        messageInput?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // Cancel previous typing job
                typingJob?.cancel()
                
                // Animate toolContainer visibility based on text input
                toolContainer?.let { container ->
                    if (!s.isNullOrEmpty() && container.visibility == View.VISIBLE) {
                        // Hide toolContainer with animation when user starts typing
                        container.animate()
                            .alpha(0f)
                            .scaleX(0.8f)
                            .scaleY(0.8f)
                            .setDuration(200)
                            .withEndAction {
                                container.visibility = View.GONE
                            }
                            .start()
                    } else if (s.isNullOrEmpty() && container.visibility == View.GONE) {
                        // Show toolContainer with animation when input is empty
                        container.visibility = View.VISIBLE
                        container.alpha = 0f
                        container.scaleX = 0.8f
                        container.scaleY = 0.8f
                        container.animate()
                            .alpha(1f)
                            .scaleX(1f)
                            .scaleY(1f)
                            .setDuration(200)
                            .start()
                    }
                }
                
                // Send typing status through ChatViewModel (only if app is not in background)
                if (!isAppInBackground) {
                    chatViewModel.onUserTyping(s?.toString() ?: "")
                }
            }
            override fun afterTextChanged(s: Editable?) {
                sendButton?.isEnabled = !s.isNullOrEmpty() && s.isNotBlank()
                
                // Stop typing when input is cleared (handled by ChatViewModel)
            }
        })
    }
    
    /**
     * Start polling for typing indicators
     */
    private fun startTypingIndicatorPolling() {
        // This method is now replaced by setupTypingIndicatorObserver()
        // Keep for backward compatibility but it's no longer used
    }
    
    /**
     * Update typing indicator UI based on typing users list
     * 
     * @param typingUsers List of user IDs who are currently typing
     */
    private fun updateTypingIndicator(typingUsers: List<String>) {
        if (typingUsers.isEmpty()) {
            hideTypingIndicator()
        } else {
            showTypingIndicator(typingUsers)
        }
    }
    
    // Auto-hide job for typing indicator
    private var typingIndicatorAutoHideJob: kotlinx.coroutines.Job? = null
    
    /**
     * Show typing indicator with appropriate text and animation
     * 
     * @param typingUsers List of user IDs who are currently typing
     */
    private fun showTypingIndicator(typingUsers: List<String>) {
        typingIndicatorView?.let { view ->
            // Generate typing text based on number of users
            val typingMessage = generateTypingMessage(typingUsers)
            typingText?.text = typingMessage
            
            // Load avatar for single user typing
            if (typingUsers.size == 1 && otherUserData != null) {
                val avatarUrl = otherUserData?.get("avatar")?.toString()
                    ?.takeIf { it.isNotEmpty() && it != "null" }
                
                if (avatarUrl != null) {
                    typingAvatar?.let { imageView ->
                        Glide.with(this)
                            .load(Uri.parse(avatarUrl))
                            .circleCrop()
                            .placeholder(R.drawable.ic_account_circle_48px)
                            .error(R.drawable.ic_account_circle_48px)
                            .into(imageView)
                    }
                }
            }
            
            // Show with fade-in animation if currently hidden
            if (view.visibility != View.VISIBLE) {
                view.visibility = View.VISIBLE
                view.alpha = 0f
                view.animate()
                    .alpha(1f)
                    .setDuration(200) // 200ms fade-in as required
                    .withStartAction {
                        // Start typing animation when fade-in begins
                        typingAnimation?.startAnimation()
                    }
                    .start()
            } else {
                // If already visible, just update the text and ensure animation is running
                typingAnimation?.startAnimation()
            }
            
            // Cancel previous auto-hide job and start new one
            typingIndicatorAutoHideJob?.cancel()
            typingIndicatorAutoHideJob = lifecycleScope.launch {
                kotlinx.coroutines.delay(5000) // Auto-hide after 5 seconds
                runOnUiThread {
                    hideTypingIndicator()
                }
            }
        }
    }
    
    /**
     * Hide typing indicator with fade-out animation
     */
    private fun hideTypingIndicator() {
        typingIndicatorView?.let { view ->
            if (view.visibility == View.VISIBLE) {
                view.animate()
                    .alpha(0f)
                    .setDuration(200) // 200ms fade-out as required
                    .withStartAction {
                        // Stop typing animation when fade-out begins
                        typingAnimation?.stopAnimation()
                    }
                    .withEndAction {
                        view.visibility = View.GONE
                    }
                    .start()
            }
        }
    }
    
    /**
     * Generate appropriate typing message based on number of users
     * 
     * @param typingUsers List of user IDs who are currently typing
     * @return Formatted typing message string
     */
    private fun generateTypingMessage(typingUsers: List<String>): String {
        return when (typingUsers.size) {
            0 -> ""
            1 -> {
                val username = otherUserData?.get("username")?.toString() ?: "User"
                "$username is typing..."
            }
            2 -> {
                // For now, we'll use generic text since we only have one other user's data
                // In a group chat, this would need to fetch multiple user names
                "2 people are typing..."
            }
            else -> {
                "${typingUsers.size} people are typing..."
            }
        }
    }

    /**
     * Update connection status banner based on realtime connection state.
     * Shows "Connecting..." when establishing connection, "Connection lost" when disconnected,
     * and hides banner when connected.
     * 
     * Requirements: 6.2
     * 
     * @param state The current realtime connection state
     */
    private fun updateConnectionStatusBanner(state: com.synapse.social.studioasinc.chat.service.RealtimeState) {
        when (state) {
            is com.synapse.social.studioasinc.chat.service.RealtimeState.Connected -> {
                hideConnectionStatusBanner()
            }
            is com.synapse.social.studioasinc.chat.service.RealtimeState.Connecting -> {
                showConnectionStatusBanner(
                    message = "Connecting...",
                    showProgress = true,
                    showRetry = false,
                    backgroundColor = R.color.md_theme_secondaryContainer,
                    textColor = R.color.md_theme_onSecondaryContainer
                )
            }
            is com.synapse.social.studioasinc.chat.service.RealtimeState.Disconnected -> {
                showConnectionStatusBanner(
                    message = "Connection lost",
                    showProgress = false,
                    showRetry = true,
                    backgroundColor = R.color.md_theme_errorContainer,
                    textColor = R.color.md_theme_onErrorContainer
                )
            }
            is com.synapse.social.studioasinc.chat.service.RealtimeState.Error -> {
                val errorMessage = when {
                    state.message.contains("timeout", ignoreCase = true) -> "Connection timeout"
                    state.message.contains("network", ignoreCase = true) -> "Network error"
                    state.message.contains("polling", ignoreCase = true) -> "Using backup connection"
                    else -> "Connection error"
                }
                
                val isPollingFallback = state.message.contains("polling", ignoreCase = true)
                showConnectionStatusBanner(
                    message = errorMessage,
                    showProgress = false,
                    showRetry = !isPollingFallback,
                    backgroundColor = if (isPollingFallback) R.color.md_theme_tertiaryContainer else R.color.md_theme_errorContainer,
                    textColor = if (isPollingFallback) R.color.md_theme_onTertiaryContainer else R.color.md_theme_onErrorContainer
                )
            }
        }
    }

    /**
     * Show connection status banner with specified configuration.
     * 
     * @param message The message to display
     * @param showProgress Whether to show progress indicator
     * @param showRetry Whether to show retry button
     * @param backgroundColor Background color resource ID
     * @param textColor Text color resource ID
     */
    private fun showConnectionStatusBanner(
        message: String,
        showProgress: Boolean,
        showRetry: Boolean,
        backgroundColor: Int,
        textColor: Int
    ) {
        connectionStatusBanner?.let { banner ->
            // Update banner appearance
            banner.setBackgroundColor(getColor(backgroundColor))
            
            // Update text and color
            connectionText?.text = message
            connectionText?.setTextColor(getColor(textColor))
            
            // Show/hide progress indicator
            connectionProgress?.visibility = if (showProgress) View.VISIBLE else View.GONE
            
            // Show/hide retry button
            connectionRetryButton?.visibility = if (showRetry) View.VISIBLE else View.GONE
            connectionRetryButton?.setTextColor(getColor(textColor))
            
            // Update icon based on state
            if (showProgress) {
                connectionIcon?.visibility = View.GONE
            } else {
                connectionIcon?.visibility = View.VISIBLE
                connectionIcon?.setImageResource(
                    if (showRetry) R.drawable.ic_wifi_off_24 else R.drawable.ic_wifi_24
                )
                connectionIcon?.setColorFilter(getColor(textColor))
            }
            
            // Show banner with animation if currently hidden
            if (banner.visibility != View.VISIBLE) {
                banner.visibility = View.VISIBLE
                banner.alpha = 0f
                banner.translationY = -banner.height.toFloat()
                banner.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .start()
            }
        }
    }

    /**
     * Hide connection status banner with animation.
     */
    private fun hideConnectionStatusBanner() {
        connectionStatusBanner?.let { banner ->
            if (banner.visibility == View.VISIBLE) {
                banner.animate()
                    .alpha(0f)
                    .translationY(-banner.height.toFloat())
                    .setDuration(300)
                    .withEndAction {
                        banner.visibility = View.GONE
                    }
                    .start()
            }
        }
    }

    /**
     * Retry connection when user taps retry button.
     * Attempts to reconnect the realtime service for the current chat.
     */
    private fun retryConnection() {
        val chatId = this.chatId ?: return
        
        lifecycleScope.launch {
            try {
                // Show connecting state
                showConnectionStatusBanner(
                    message = "Reconnecting...",
                    showProgress = true,
                    showRetry = false,
                    backgroundColor = R.color.md_theme_secondaryContainer,
                    textColor = R.color.md_theme_onSecondaryContainer
                )
                
                // Attempt to reconnect
                chatViewModel.getRealtimeService()?.reconnect(chatId)
                
                Toast.makeText(this@ChatActivity, "Reconnecting...", Toast.LENGTH_SHORT).show()
                
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Failed to retry connection", e)
                Toast.makeText(this@ChatActivity, "Reconnection failed", Toast.LENGTH_SHORT).show()
                
                // Show error state again
                showConnectionStatusBanner(
                    message = "Connection failed",
                    showProgress = false,
                    showRetry = true,
                    backgroundColor = R.color.md_theme_errorContainer,
                    textColor = R.color.md_theme_onErrorContainer
                )
            }
        }
    }

    /**
     * Setup swipe-to-reply gesture on RecyclerView
     */
    private fun setupSwipeToReply(recyclerView: RecyclerView) {
        val swipeToReplyCallback = SwipeToReplyCallback(this) { position ->
            // Get message data at position
            val messageData = messagesList.getOrNull(position) ?: return@SwipeToReplyCallback
            
            // Extract message details
            val messageId = messageData["id"]?.toString() 
                ?: messageData["key"]?.toString() 
                ?: return@SwipeToReplyCallback
            val messageText = messageData["content"]?.toString() 
                ?: messageData["message_text"]?.toString() 
                ?: ""
            
            // Get sender name
            val senderId = messageData["sender_id"]?.toString() 
                ?: messageData["uid"]?.toString()
            val senderName = if (senderId == currentUserId) {
                "You"
            } else {
                otherUserData?.get("username")?.toString() ?: "User"
            }
            
            // Prepare reply
            prepareReply(messageId, messageText, senderName)
        }
        
        val itemTouchHelper = androidx.recyclerview.widget.ItemTouchHelper(swipeToReplyCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    /**
     * Setup reply preview observers and listeners
     */
    private fun setupReplyPreview() {
        // Observe reply state from ViewModel
        lifecycleScope.launch {
            viewModel.replyState.collect { state ->
                when (state) {
                    is MessageActionsViewModel.ReplyState.Idle -> {
                        hideReplyPreview()
                    }
                    is MessageActionsViewModel.ReplyState.Active -> {
                        showReplyPreview(
                            senderName = state.senderName,
                            messageText = state.previewText,
                            messageData = null // We'll add media support later if needed
                        )
                        replyMessageId = state.messageId
                    }
                }
            }
        }
        
        // Setup cancel button listener
        replyCancelButton?.setOnClickListener {
            viewModel.clearReply()
        }
    }
    
    /**
     * Show reply preview above message input
     * 
     * @param senderName The name of the message sender
     * @param messageText The message text (already truncated to 3 lines)
     * @param messageData Optional message data for media preview
     */
    private fun showReplyPreview(
        senderName: String,
        messageText: String,
        messageData: Map<String, Any?>?
    ) {
        replyLayout?.visibility = View.VISIBLE
        replyUsername?.text = senderName
        replyMessage?.text = messageText
        
        // Show media preview if message has attachments
        val attachmentUrl = messageData?.get("attachment_url")?.toString()
        val messageType = messageData?.get("message_type")?.toString()
        
        if (!attachmentUrl.isNullOrEmpty() && messageType == "image") {
            replyMediaPreview?.visibility = View.VISIBLE
            Glide.with(this)
                .load(Uri.parse(attachmentUrl))
                .centerCrop()
                .placeholder(R.drawable.ph_imgbluredsqure)
                .error(R.drawable.ph_imgbluredsqure)
                .into(replyMediaPreview!!)
        } else {
            replyMediaPreview?.visibility = View.GONE
        }
        
        // Animate the reply preview appearance
        replyLayout?.apply {
            alpha = 0f
            translationY = -50f
            animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(200)
                .start()
        }
    }
    
    /**
     * Hide reply preview
     */
    private fun hideReplyPreview() {
        replyLayout?.animate()
            ?.alpha(0f)
            ?.translationY(-50f)
            ?.setDuration(200)
            ?.withEndAction {
                replyLayout?.visibility = View.GONE
                replyMessageId = null
            }
            ?.start()
    }
    
    /**
     * Prepare reply to a message (called from message actions)
     * 
     * @param messageId The ID of the message to reply to
     * @param messageText The text content of the message
     * @param senderName The name of the message sender
     */
    fun prepareReply(messageId: String, messageText: String, senderName: String) {
        viewModel.prepareReply(messageId, messageText, senderName)
        
        // Focus message input
        messageInput?.requestFocus()
        
        // Show keyboard
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.showSoftInput(messageInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
    }
    
    /**
     * Scroll to a specific message and highlight it
     * 
     * @param messageId The ID of the message to scroll to
     */
    private fun scrollToMessage(messageId: String) {
        // Find the position of the message in the list
        val position = messagesList.indexOfFirst { 
            it["id"]?.toString() == messageId 
        }
        
        if (position != -1) {
            // Scroll to the message with smooth animation
            recyclerView?.smoothScrollToPosition(position)
            
            // Highlight the message briefly after scrolling
            recyclerView?.postDelayed({
                highlightMessage(position)
            }, 300) // Wait for scroll animation to complete
        } else {
            // Message not found in current list
            // In a real implementation, we would load more messages
            Toast.makeText(
                this,
                "Original message not found. It may have been deleted or not loaded yet.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    /**
     * Highlight a message with a flash animation
     * 
     * @param position The position of the message in the list
     */
    private fun highlightMessage(position: Int) {
        val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
        val messageView = viewHolder?.itemView?.findViewById<LinearLayout>(R.id.messageBG)
        
        messageView?.let { view ->
            // Store original background
            val originalBackground = view.background
            
            // Create highlight animation
            val highlightColor = getColor(R.color.md_theme_primaryContainer)
            view.setBackgroundColor(highlightColor)
            
            // Fade back to original background
            view.animate()
                .alpha(0.5f)
                .setDuration(200)
                .withEndAction {
                    view.animate()
                        .alpha(1f)
                        .setDuration(200)
                        .withEndAction {
                            view.background = originalBackground
                        }
                        .start()
                }
                .start()
        }
    }

    /**
     * Set up Supabase Realtime channel for messages
     * Subscribes to UPDATE and INSERT events on messages table filtered by chat_id
     */
    private fun setupRealtimeSubscription() {
        if (chatId == null) {
            android.util.Log.w("ChatActivity", "Cannot setup realtime: chatId is null")
            return
        }
        
        // TODO: Implement realtime subscription for live message updates
        // The Supabase Realtime API needs to be properly configured
        // For now, messages will be loaded on activity start and when sending
        android.util.Log.d("ChatActivity", "Realtime subscription not yet implemented for chat: $chatId")
    }
    
    /**
     * Handle real-time message updates (edits and deletions)
     */
    private fun handleMessageUpdate(record: JsonObject) {
        lifecycleScope.launch {
            try {
                val messageId = record["id"]?.toString()?.removeSurrounding("\"") ?: return@launch
                val isEdited = record["is_edited"]?.toString()?.removeSurrounding("\"")?.toBooleanStrictOrNull() ?: false
                val isDeleted = record["is_deleted"]?.toString()?.removeSurrounding("\"")?.toBooleanStrictOrNull() ?: false
                val deleteForEveryone = record["delete_for_everyone"]?.toString()?.removeSurrounding("\"")?.toBooleanStrictOrNull() ?: false
                
                // Find the message in the list
                val position = messagesList.indexOfFirst { it["id"]?.toString() == messageId }
                
                if (position != -1) {
                    if (isDeleted || deleteForEveryone) {
                        // Handle deletion
                        handleRealtimeMessageDeletion(messageId, position)
                    } else if (isEdited) {
                        // Handle edit
                        handleRealtimeMessageEdit(messageId, position, record)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error handling message update", e)
            }
        }
    }
    
    /**
     * Handle real-time message inserts (new messages including forwarded)
     */
    private fun handleMessageInsert(record: JsonObject) {
        lifecycleScope.launch {
            try {
                val chatIdFromRecord = record["chat_id"]?.toString()?.removeSurrounding("\"")
                
                // Only process if message is for current chat
                if (chatIdFromRecord != chatId) {
                    return@launch
                }
                
                val senderId = record["sender_id"]?.toString()?.removeSurrounding("\"")
                
                // Don't add if it's from current user (already added optimistically)
                if (senderId == currentUserId) {
                    return@launch
                }
                
                handleRealtimeForwardedMessage(record)
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error handling message insert", e)
            }
        }
    }
    
    /**
     * Handle real-time message edits
     * Updates message in RecyclerView adapter data and refreshes the view
     */
    private fun handleRealtimeMessageEdit(messageId: String, position: Int, record: JsonObject) {
        runOnUiThread {
            try {
                // Update message data
                val message = messagesList[position]
                val newContent = record["content"]?.toString()?.removeSurrounding("\"") ?: ""
                val editedAt = record["edited_at"]?.toString()?.removeSurrounding("\"")?.toLongOrNull() ?: System.currentTimeMillis()
                
                message["content"] = newContent
                message["message_text"] = newContent
                message["is_edited"] = true
                message["edited_at"] = editedAt
                
                // Refresh the specific message view with animation
                chatAdapter?.notifyItemChanged(position)
                
                // Show brief animation to indicate update
                recyclerView?.postDelayed({
                    val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                    viewHolder?.itemView?.let { view ->
                        view.alpha = 0.5f
                        view.animate()
                            .alpha(1f)
                            .setDuration(300)
                            .start()
                    }
                }, 100)
                
                android.util.Log.d("ChatActivity", "Message edited in real-time: $messageId")
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error updating edited message in UI", e)
            }
        }
    }
    
    /**
     * Handle real-time message deletions
     * Replaces message content with deleted placeholder
     */
    private fun handleRealtimeMessageDeletion(messageId: String, position: Int) {
        runOnUiThread {
            try {
                // Update message data
                val message = messagesList[position]
                message["is_deleted"] = true
                message["delete_for_everyone"] = true
                
                // Refresh the specific message view with animation
                chatAdapter?.notifyItemChanged(position)
                
                // Show brief animation to indicate deletion
                recyclerView?.postDelayed({
                    val viewHolder = recyclerView?.findViewHolderForAdapterPosition(position)
                    viewHolder?.itemView?.let { view ->
                        view.alpha = 1f
                        view.animate()
                            .alpha(0.5f)
                            .setDuration(200)
                            .withEndAction {
                                view.animate()
                                    .alpha(1f)
                                    .setDuration(200)
                                    .start()
                            }
                            .start()
                    }
                }, 100)
                
                android.util.Log.d("ChatActivity", "Message deleted in real-time: $messageId")
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error updating deleted message in UI", e)
            }
        }
    }
    
    /**
     * Handle real-time forwarded messages
     * Adds new message to RecyclerView and scrolls if user is at bottom
     */
    private fun handleRealtimeForwardedMessage(record: JsonObject) {
        runOnUiThread {
            try {
                // Check if user is at bottom of list
                val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager
                val lastVisiblePosition = layoutManager?.findLastCompletelyVisibleItemPosition() ?: -1
                val isAtBottom = lastVisiblePosition >= messagesList.size - 1
                
                // Create message map from record
                val newMessage = HashMap<String, Any?>()
                newMessage["id"] = record["id"]?.toString()?.removeSurrounding("\"")
                newMessage["chat_id"] = record["chat_id"]?.toString()?.removeSurrounding("\"")
                newMessage["sender_id"] = record["sender_id"]?.toString()?.removeSurrounding("\"")
                newMessage["uid"] = record["sender_id"]?.toString()?.removeSurrounding("\"")
                newMessage["content"] = record["content"]?.toString()?.removeSurrounding("\"")
                newMessage["message_text"] = record["content"]?.toString()?.removeSurrounding("\"")
                newMessage["message_type"] = record["message_type"]?.toString()?.removeSurrounding("\"")
                newMessage["created_at"] = record["created_at"]?.toString()?.removeSurrounding("\"")?.toLongOrNull() ?: System.currentTimeMillis()
                newMessage["push_date"] = record["created_at"]?.toString()?.removeSurrounding("\"")?.toLongOrNull() ?: System.currentTimeMillis()
                newMessage["is_deleted"] = false
                newMessage["is_edited"] = false
                newMessage["delivery_status"] = "delivered"
                
                // Check if message is forwarded
                val forwardedFromMessageId = record["forwarded_from_message_id"]?.toString()?.removeSurrounding("\"")
                if (!forwardedFromMessageId.isNullOrEmpty() && forwardedFromMessageId != "null") {
                    newMessage["forwarded_from_message_id"] = forwardedFromMessageId
                    newMessage["forwarded_from_chat_id"] = record["forwarded_from_chat_id"]?.toString()?.removeSurrounding("\"")
                }
                
                // Check if message is a reply
                val replyToId = record["reply_to_id"]?.toString()?.removeSurrounding("\"")
                if (!replyToId.isNullOrEmpty() && replyToId != "null") {
                    newMessage["replied_message_id"] = replyToId
                }
                
                // Add message to list
                messagesList.add(newMessage)
                chatAdapter?.notifyItemInserted(messagesList.size - 1)
                
                if (isAtBottom) {
                    // Scroll to new message if user is at bottom
                    recyclerView?.smoothScrollToPosition(messagesList.size - 1)
                } else {
                    // Show notification if user is scrolled up
                    Toast.makeText(
                        this,
                        "New message received",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                
                // Mark message as read if user is viewing
                if (currentUserId != null) {
                    lifecycleScope.launch {
                        chatService.markMessagesAsRead(chatId!!, currentUserId!!)
                    }
                }
                
                android.util.Log.d("ChatActivity", "New message received in real-time")
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error adding new message to UI", e)
            }
        }
    }

    /**
     * Show message actions bottom sheet on long press
     */
    private fun showMessageActionsBottomSheet(messageId: String, position: Int) {
        val messageData = messagesList.getOrNull(position) ?: return
        
        MessageActionsBottomSheet.show(
            fragmentManager = supportFragmentManager,
            messageData = messageData,
            currentUserId = currentUserId ?: return,
            listener = object : MessageActionsBottomSheet.MessageActionListener {
                override fun onReplyAction(messageId: String, messageText: String, senderName: String) {
                    prepareReply(messageId, messageText, senderName)
                }
                
                override fun onForwardAction(messageId: String, messageData: Map<String, Any?>) {
                    showForwardDialog(messageId, messageData)
                }
                
                override fun onEditAction(messageId: String, currentText: String) {
                    showEditDialog(messageId, currentText)
                }
                
                override fun onDeleteAction(messageId: String) {
                    showDeleteConfirmation(messageId, false)
                }
                
                override fun onAISummaryAction(messageId: String, messageText: String) {
                    showAISummary(messageId, messageText)
                }
            }
        )
    }
    
    /**
     * Open URL in browser
     */
    private fun openUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Cannot open link", Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Open media viewer for images/videos
     */
    private fun openMediaViewer(url: String, type: String) {
        // TODO: Implement media viewer
        Toast.makeText(this, "Media viewer coming soon", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Open user profile
     */
    private fun openUserProfile(userId: String) {
        val intent = Intent(this, ProfileActivity::class.java)
        intent.putExtra("uid", userId)
        startActivity(intent)
    }
    
    /**
     * Retry failed message
     */
    private fun retryFailedMessage(messageId: String, position: Int) {
        // TODO: Implement message retry logic
        Toast.makeText(this, "Retry feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show forward message dialog
     */
    private fun showForwardDialog(messageId: String, messageData: Map<String, Any?>) {
        // TODO: Implement forward dialog
        Toast.makeText(this, "Forward feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show edit message dialog
     */
    private fun showEditDialog(messageId: String, currentText: String) {
        val dialog = EditMessageDialog.newInstance(
            messageId = messageId,
            currentText = currentText,
            listener = object : EditMessageDialog.EditMessageListener {
                override fun onMessageEdited(messageId: String, newText: String) {
                    editMessage(messageId, newText)
                }
            }
        )
        dialog.show(supportFragmentManager, "EditMessageDialog")
    }
    
    /**
     * Edit a message
     */
    private fun editMessage(messageId: String, newText: String) {
        lifecycleScope.launch {
            try {
                val result = chatService.editMessage(messageId, newText)
                result.fold(
                    onSuccess = {
                        Toast.makeText(this@ChatActivity, "Message edited", Toast.LENGTH_SHORT).show()
                        // Update will come through realtime
                    },
                    onFailure = { error ->
                        showError("Failed to edit message: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                showError("Error editing message: ${e.message}")
            }
        }
    }
    
    /**
     * Show delete confirmation dialog
     */
    private fun showDeleteConfirmation(messageId: String, deleteForEveryone: Boolean) {
        // Determine if this is the user's own message
        val messageData = messagesList.find { it["id"]?.toString() == messageId }
        val senderId = messageData?.get("sender_id")?.toString() 
            ?: messageData?.get("uid")?.toString()
        val isOwnMessage = senderId == currentUserId
        
        // Show delete confirmation dialog
        val dialog = DeleteConfirmationDialog.newInstance(messageId, isOwnMessage)
        dialog.show(supportFragmentManager, "DeleteConfirmationDialog")
    }
    
    /**
     * Delete a message
     */
    private fun deleteMessage(messageId: String, deleteForEveryone: Boolean) {
        lifecycleScope.launch {
            try {
                val result = chatService.deleteMessage(messageId)
                result.fold(
                    onSuccess = {
                        Toast.makeText(this@ChatActivity, "Message deleted", Toast.LENGTH_SHORT).show()
                        // Update will come through realtime
                    },
                    onFailure = { error ->
                        showError("Failed to delete message: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                showError("Error deleting message: ${e.message}")
            }
        }
    }
    
    /**
     * Show AI summary dialog
     */
    private fun showAISummary(messageId: String, messageText: String) {
        // TODO: Implement AI summary dialog
        Toast.makeText(this, "AI Summary feature coming soon", Toast.LENGTH_SHORT).show()
    }
    
    /**
     * Show edit history dialog
     */
    private fun showEditHistory(messageId: String) {
        val dialog = EditHistoryDialog.newInstance(messageId)
        dialog.show(supportFragmentManager, "EditHistoryDialog")
    }

    /**
     * Setup real-time message state updates for read receipts
     * This method handles updating message states when read receipts are received
     * 
     * Requirements: 4.3
     */
    private fun setupMessageStateUpdates() {
        // This method would integrate with the ReadReceiptManager when it's fully implemented
        // For now, we provide a placeholder that demonstrates the integration pattern
        
        // Example of how message states would be updated:
        // readReceiptManager?.subscribeToReadReceipts(chatId) { readReceiptEvent ->
        //     // Update message states in the adapter
        //     val messageStates = readReceiptEvent.messageIds.associateWith { "read" }
        //     chatAdapter?.updateMessageStates(messageStates)
        // }
        
        android.util.Log.d("ChatActivity", "Message state updates setup completed")
    }

    /**
     * Update message state for a specific message
     * Called when read receipts are received from the backend
     * 
     * Requirements: 4.3
     * 
     * @param messageId The message ID to update
     * @param newState The new message state (sent, delivered, read, failed)
     */
    fun updateMessageState(messageId: String, newState: String) {
        runOnUiThread {
            chatAdapter?.updateMessageState(messageId, newState)
        }
    }

    /**
     * Update multiple message states efficiently
     * Used for batch read receipt updates
     * 
     * Requirements: 4.3
     * 
     * @param messageStates Map of message ID to new state
     */
    fun updateMessageStates(messageStates: Map<String, String>) {
        runOnUiThread {
            chatAdapter?.updateMessageStates(messageStates)
        }
    }

    override fun onResume() {
        super<AppCompatActivity>.onResume()
        
        // App is returning to foreground
        isAppInBackground = false
        
        // Subscribe to typing events and read receipts when chat screen opens
        if (chatId != null) {
            chatViewModel.onChatOpened(chatId!!)
            
            // Mark visible messages as read when chat opens (only if not backgrounded)
            if (!isAppInBackground) {
                markVisibleMessagesAsRead()
            }
        }
        
        // Resume operations when app returns to foreground
        chatViewModel.setChatVisibility(true)
    }
    
    override fun onPause() {
        super<AppCompatActivity>.onPause()
        
        // App is going to background
        isAppInBackground = true
        
        // Defer read receipt updates when app is backgrounded
        chatViewModel.setChatVisibility(false)
        
        // Unsubscribe when chat screen closes
        chatViewModel.onChatClosed()
        
        // Stop typing indicator when leaving chat
        if (chatId != null && currentUserId != null) {
            lifecycleScope.launch {
                chatService.updateTypingStatus(chatId!!, currentUserId!!, false)
            }
        }
    }
    
    // DefaultLifecycleObserver methods for app backgrounding
    override fun onStart(owner: LifecycleOwner) {
        // App is coming to foreground
        isAppInBackground = false
        
        // Resume operations when app returns to foreground
        if (chatId != null) {
            chatViewModel.setChatVisibility(true)
            // Re-subscribe to events if needed
            chatViewModel.onChatOpened(chatId!!)
        }
    }
    
    override fun onStop(owner: LifecycleOwner) {
        // App is going to background
        isAppInBackground = true
        
        // Defer read receipt updates when app is backgrounded
        chatViewModel.setChatVisibility(false)
        
        // Stop sending typing events when app is backgrounded
        if (chatId != null && currentUserId != null) {
            lifecycleScope.launch {
                chatService.updateTypingStatus(chatId!!, currentUserId!!, false)
            }
        }
    }

    /**
     * Mark all visible messages as read when chat opens.
     * This implements requirement 4.1 - mark messages as read within 1 second when chat opens.
     * Only marks messages as read if app is not in background (requirement 4.5).
     */
    private fun markVisibleMessagesAsRead() {
        // Don't mark messages as read if app is in background
        if (isAppInBackground) {
            return
        }
        
        val layoutManager = recyclerView?.layoutManager as? LinearLayoutManager ?: return
        val firstVisible = layoutManager.findFirstVisibleItemPosition()
        val lastVisible = layoutManager.findLastVisibleItemPosition()
        
        if (firstVisible == RecyclerView.NO_POSITION || lastVisible == RecyclerView.NO_POSITION) {
            return
        }
        
        // Get message IDs for visible messages that aren't sent by current user
        val visibleMessageIds = mutableListOf<String>()
        for (position in firstVisible..lastVisible) {
            val messageData = messagesList.getOrNull(position) ?: continue
            val messageId = messageData["id"]?.toString() ?: continue
            val senderId = messageData["sender_id"]?.toString() 
                ?: messageData["uid"]?.toString()
            
            // Only mark messages from other users as read
            if (senderId != currentUserId) {
                visibleMessageIds.add(messageId)
            }
        }
        
        if (visibleMessageIds.isNotEmpty()) {
            chatViewModel.markVisibleMessagesAsRead(visibleMessageIds)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
    
    override fun onDestroy() {
        super<AppCompatActivity>.onDestroy()
        
        // Remove lifecycle observer
        lifecycle.removeObserver(this)
        
        // Unsubscribe from realtime channel
        lifecycleScope.launch {
            try {
                realtimeChannel?.unsubscribe()
                android.util.Log.d("ChatActivity", "Realtime channel unsubscribed")
            } catch (e: Exception) {
                android.util.Log.e("ChatActivity", "Error unsubscribing from realtime channel", e)
            }
        }
        
        // Clean up typing indicator coroutine jobs
        typingIndicatorAutoHideJob?.cancel()
        
        // Stop typing indicator animations
        typingAnimation?.stopAnimation()
        
        // Clean up ChatViewModel resources
        chatViewModel.onChatClosed()
        
        synapseLoadingDialog?.dismiss()
        synapseLoadingDialog = null
    }
}