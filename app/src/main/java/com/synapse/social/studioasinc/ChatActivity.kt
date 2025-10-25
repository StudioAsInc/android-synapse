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
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObject
import com.synapse.social.studioasinc.util.ChatHelper
import kotlinx.coroutines.*
import androidx.lifecycle.lifecycleScope
import java.text.SimpleDateFormat
import java.util.*

class ChatActivity : AppCompatActivity() {

    // Supabase services
    private val chatService = com.synapse.social.studioasinc.backend.SupabaseChatService()
    private val databaseService = com.synapse.social.studioasinc.backend.SupabaseDatabaseService()
    
    private var synapseLoadingDialog: ProgressDialog? = null
    private var chatId: String? = null
    private var otherUserId: String? = null
    private var isGroup: Boolean = false
    private var replyMessageId: String? = null
    
    private val messagesList = ArrayList<Map<String, Any?>>()
    private var otherUserData: Map<String, Any?>? = null
    private var currentUserId: String? = null
    private var messagesAdapter: ChatAdapter? = null

    // UI Components
    private var recyclerView: RecyclerView? = null
    private var messageInput: EditText? = null
    private var sendButton: ImageButton? = null
    private var backButton: ImageView? = null
    private var chatNameText: TextView? = null
    private var chatAvatarImage: ImageView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
    }

    private fun initialize() {
        // Initialize UI components - create minimal UI if layout is missing
        try {
            recyclerView = RecyclerView(this)
            messageInput = EditText(this)
            sendButton = ImageButton(this)
        } catch (e: Exception) {
            // Handle missing UI components gracefully
            Toast.makeText(this, "UI initialization error", Toast.LENGTH_SHORT).show()
        }
    }

    private fun initializeLogic() {
        // Setup basic functionality
        sendButton?.setOnClickListener {
            sendMessage()
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
                        messagesList.addAll(messages)
                        messagesAdapter?.notifyDataSetChanged()
                        recyclerView?.scrollToPosition(messagesList.size - 1)
                        
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
        otherUserData?.let { userData ->
            chatNameText?.text = userData["username"]?.toString() ?: "User"
            
            val avatarUrl = userData["avatar"]?.toString()
            if (!avatarUrl.isNullOrEmpty() && avatarUrl != "null") {
                chatAvatarImage?.let { imageView ->
                    Glide.with(this)
                        .load(Uri.parse(avatarUrl))
                        .circleCrop()
                        .into(imageView)
                }
            }
        }
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    
    private fun loadingDialog(show: Boolean) {
        if (show) {
            synapseLoadingDialog = ProgressDialog(this).apply {
                setMessage("Loading...")
                setCancelable(false)
                show()
            }
        } else {
            synapseLoadingDialog?.dismiss()
            synapseLoadingDialog = null
        }
    }

    private fun sendMessage() {
        val messageText = messageInput?.text?.toString()?.trim()
        if (messageText.isNullOrEmpty()) return
        
        if (chatId == null || currentUserId == null) {
            showError("Chat not initialized")
            return
        }
        
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
                    onSuccess = {
                        messageInput?.text?.clear()
                        replyMessageId = null
                        loadMessages() // Reload to show new message
                    },
                    onFailure = { error ->
                        showError("Failed to send message: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                showError("Error sending message: ${e.message}")
                messageInput?.setText("")
                Toast.makeText(this@ChatActivity, "Message sending not implemented yet", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                showError("Failed to send message: ${e.message}")
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }
}