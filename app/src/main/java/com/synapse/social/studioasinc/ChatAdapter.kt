package com.synapse.social.studioasinc

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService
import com.synapse.social.studioasinc.chat.interfaces.ChatAdapterListener
import com.synapse.social.studioasinc.util.LinkPreviewUtil
import com.synapse.social.studioasinc.util.MessageAnimations
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val data: ArrayList<HashMap<String, Any?>>,
    private val repliedMessagesCache: HashMap<String, HashMap<String, Any?>>,
    private val listener: ChatAdapterListener
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TAG = "ChatAdapter"
        private const val VIEW_TYPE_TEXT = 1
        private const val VIEW_TYPE_MEDIA_GRID = 2
        private const val VIEW_TYPE_TYPING = 3
        private const val VIEW_TYPE_VIDEO = 4
        private const val VIEW_TYPE_LINK_PREVIEW = 5
        private const val VIEW_TYPE_VOICE_MESSAGE = 6
        private const val VIEW_TYPE_ERROR = 7
        private const val VIEW_TYPE_LOADING_MORE = 99
    }

    private var context: Context? = null
    private var secondUserAvatarUrl = ""
    private var firstUserName = ""
    private var secondUserName = ""
    private var appSettings: SharedPreferences? = null
    private var isGroupChat = false
    private var userNamesMap = HashMap<String, String>()
    private var previousSenderId: String? = null
    
    // Supabase services
    private val authService = SupabaseAuthenticationService()
    private val databaseService = SupabaseDatabaseService()

    // Setter methods for configuration
    fun setSecondUserAvatar(url: String) { secondUserAvatarUrl = url }
    fun setFirstUserName(name: String) { firstUserName = name }
    fun setSecondUserName(name: String) { secondUserName = name }
    fun setGroupChat(isGroup: Boolean) { isGroupChat = isGroup }
    fun setUserNamesMap(map: HashMap<String, String>) { userNamesMap = map }

    override fun getItemViewType(position: Int): Int {
        val item = data[position]
        
        if (item.containsKey("isLoadingMore")) return VIEW_TYPE_LOADING_MORE
        if (item.containsKey("typingMessageStatus")) return VIEW_TYPE_TYPING
        
        // Check for error/failed messages - support both field names
        val deliveryStatus = item["delivery_status"]?.toString() 
            ?: item["message_state"]?.toString() 
            ?: ""
        if (deliveryStatus == "failed" || deliveryStatus == "error") {
            Log.d(TAG, "Error message detected at position $position")
            return VIEW_TYPE_ERROR
        }
        
        val type = item["TYPE"]?.toString() ?: "MESSAGE"
        Log.d(TAG, "Message at position $position has type: $type")

        return when (type) {
            "VOICE_MESSAGE" -> VIEW_TYPE_VOICE_MESSAGE
            "ATTACHMENT_MESSAGE" -> {
                val attachments = item["attachments"] as? ArrayList<HashMap<String, Any?>>
                Log.d(TAG, "ATTACHMENT_MESSAGE detected with ${attachments?.size ?: 0} attachments")

                if (attachments?.size == 1 && 
                    attachments[0]["publicId"]?.toString()?.contains("|video") == true) {
                    Log.d(TAG, "Video message detected, returning VIEW_TYPE_VIDEO")
                    VIEW_TYPE_VIDEO
                } else {
                    Log.d(TAG, "Media message detected, returning VIEW_TYPE_MEDIA_GRID")
                    VIEW_TYPE_MEDIA_GRID
                }
            }
            else -> {
                val messageText = item["message_text"]?.toString() ?: ""
                if (LinkPreviewUtil.extractUrl(messageText) != null) {
                    Log.d(TAG, "Link preview message detected, returning VIEW_TYPE_LINK_PREVIEW")
                    VIEW_TYPE_LINK_PREVIEW
                } else {
                    Log.d(TAG, "Text message detected, returning VIEW_TYPE_TEXT")
                    VIEW_TYPE_TEXT
                }
            }
        }
    }

    override fun getItemId(position: Int): Long {
        return try {
            val keyObj = data[position]["key"] ?: data[position]["KEY_KEY"] ?: position
            keyObj.toString().hashCode().toLong()
        } catch (e: Exception) {
            position.toLong()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        context = parent.context
        appSettings = context?.getSharedPreferences("appSettings", Context.MODE_PRIVATE)
        val inflater = LayoutInflater.from(context)
        
        return when (viewType) {
            VIEW_TYPE_MEDIA_GRID -> MediaViewHolder(inflater.inflate(R.layout.chat_bubble_media, parent, false))
            VIEW_TYPE_VIDEO -> VideoViewHolder(inflater.inflate(R.layout.chat_bubble_video, parent, false))
            VIEW_TYPE_TYPING -> TypingViewHolder(inflater.inflate(R.layout.chat_bubble_typing, parent, false))
            VIEW_TYPE_LINK_PREVIEW -> LinkPreviewViewHolder(inflater.inflate(R.layout.chat_bubble_link_preview, parent, false))
            VIEW_TYPE_VOICE_MESSAGE -> VoiceMessageViewHolder(inflater.inflate(R.layout.chat_bubble_voice, parent, false))
            VIEW_TYPE_ERROR -> ErrorViewHolder(inflater.inflate(R.layout.chat_bubble_error, parent, false))
            VIEW_TYPE_LOADING_MORE -> LoadingViewHolder(inflater.inflate(R.layout.chat_bubble_loading_more, parent, false))
            else -> TextViewHolder(inflater.inflate(R.layout.chat_bubble_text, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder.itemViewType) {
            VIEW_TYPE_TEXT -> bindTextViewHolder(holder as TextViewHolder, position)
            VIEW_TYPE_MEDIA_GRID -> bindMediaViewHolder(holder as MediaViewHolder, position)
            VIEW_TYPE_VIDEO -> bindVideoViewHolder(holder as VideoViewHolder, position)
            VIEW_TYPE_TYPING -> bindTypingViewHolder(holder as TypingViewHolder, position)
            VIEW_TYPE_LINK_PREVIEW -> bindLinkPreviewViewHolder(holder as LinkPreviewViewHolder, position)
            VIEW_TYPE_VOICE_MESSAGE -> bindVoiceMessageViewHolder(holder as VoiceMessageViewHolder, position)
            VIEW_TYPE_ERROR -> bindErrorViewHolder(holder as ErrorViewHolder, position)
            VIEW_TYPE_LOADING_MORE -> bindLoadingViewHolder(holder as LoadingViewHolder, position)
        }
    }

    override fun getItemCount(): Int = data.size

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is VoiceMessageViewHolder) {
            holder.mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.release()
                holder.mediaPlayer = null
            }
            holder.handler?.removeCallbacksAndMessages(null)
        }
    }

    // Base ViewHolder class
    abstract class BaseMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val senderUsername: TextView? = itemView.findViewById(R.id.senderUsername)
        val forwardedIndicator: LinearLayout? = itemView.findViewById(R.id.forwardedIndicator)
        val editedIndicator: TextView? = itemView.findViewById(R.id.editedIndicator)
        val messageTime: TextView? = itemView.findViewById(R.id.date)
        val messageStatus: ImageView? = itemView.findViewById(R.id.message_state)
        val replyLayout: LinearLayout? = itemView.findViewById(R.id.mRepliedMessageLayout)
        val replyText: TextView? = itemView.findViewById(R.id.mRepliedMessageLayoutMessage)
        val messageBubble: LinearLayout? = itemView.findViewById(R.id.messageBG)
        val messageLayout: LinearLayout? = itemView.findViewById(R.id.message_layout)
        val bodyLayout: LinearLayout? = itemView.findViewById(R.id.body)
        val deletedMessagePlaceholder: LinearLayout? = itemView.findViewById(R.id.deletedMessagePlaceholder)
        val messageContentContainer: LinearLayout? = itemView.findViewById(R.id.messageContentContainer)
    }

    // Text Message ViewHolder
    class TextViewHolder(itemView: View) : BaseMessageViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
    }

    // Media Message ViewHolder
    class MediaViewHolder(itemView: View) : BaseMessageViewHolder(itemView) {
        val mediaGrid: androidx.gridlayout.widget.GridLayout = itemView.findViewById(R.id.mediaGridLayout)
        val mediaCaption: TextView? = itemView.findViewById(R.id.message_text)
    }

    // Video Message ViewHolder
    class VideoViewHolder(itemView: View) : BaseMessageViewHolder(itemView) {
        val videoThumbnail: ImageView = itemView.findViewById(R.id.videoThumbnail)
        val playButton: ImageView = itemView.findViewById(R.id.playButton)
        val videoDuration: TextView? = itemView.findViewById(R.id.date) // Use date field for duration
        val videoCaption: TextView? = itemView.findViewById(R.id.message_text)
    }

    // Typing Indicator ViewHolder
    class TypingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val typingAnimation: com.airbnb.lottie.LottieAnimationView = itemView.findViewById(R.id.lottie_typing)
    }

    // Link Preview ViewHolder
    class LinkPreviewViewHolder(itemView: View) : BaseMessageViewHolder(itemView) {
        val messageText: TextView = itemView.findViewById(R.id.message_text)
        val linkPreviewCard: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.linkPreviewContainer)
        val linkImage: ImageView = itemView.findViewById(R.id.linkPreviewImage)
        val linkTitle: TextView = itemView.findViewById(R.id.linkPreviewTitle)
        val linkDescription: TextView = itemView.findViewById(R.id.linkPreviewDescription)
        val linkUrl: TextView = itemView.findViewById(R.id.linkPreviewDomain)
    }

    // Voice Message ViewHolder
    class VoiceMessageViewHolder(itemView: View) : BaseMessageViewHolder(itemView) {
        val playPauseButton: ImageView = itemView.findViewById(R.id.play_pause_button)
        val waveform: SeekBar = itemView.findViewById(R.id.voice_seekbar)
        val duration: TextView = itemView.findViewById(R.id.voice_duration)
        var mediaPlayer: MediaPlayer? = null
        var handler: Handler? = null
    }

    // Error Message ViewHolder
    class ErrorViewHolder(itemView: View) : BaseMessageViewHolder(itemView) {
        val errorMessageText: TextView = itemView.findViewById(R.id.error_message_text)
        val retryText: TextView = itemView.findViewById(R.id.retry_text)
        val errorIcon: ImageView = itemView.findViewById(R.id.error_icon)
    }

    // Loading More ViewHolder
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val loadingProgress: ProgressBar = itemView.findViewById(R.id.loadingMoreProgressBar)
    }

    // Binding methods for each view type
    private fun bindCommonMessageProperties(holder: BaseMessageViewHolder, position: Int) {
        val messageData = data[position]
        val currentUser = authService.getCurrentUser()
        val myUid = currentUser?.id ?: ""
        // Support both old (uid) and new (sender_id) field names
        val msgUid = messageData["sender_id"]?.toString() 
            ?: messageData["uid"]?.toString() 
            ?: ""
        val isMyMessage = msgUid == myUid
        
        // Check if message is deleted
        val isDeleted = messageData["is_deleted"]?.toString()?.toBooleanStrictOrNull() ?: false
        val deleteForEveryone = messageData["delete_for_everyone"]?.toString()?.toBooleanStrictOrNull() ?: false
        
        // Handle deleted message display
        if (isDeleted || deleteForEveryone) {
            // Show deleted placeholder, hide content
            holder.deletedMessagePlaceholder?.visibility = View.VISIBLE
            holder.messageContentContainer?.visibility = View.GONE
            
            // Keep timestamp and sender info visible but hide message status
            holder.messageStatus?.visibility = View.GONE
            
            // Disable long-press for deleted messages
            holder.itemView.setOnLongClickListener(null)
            holder.itemView.isLongClickable = false
            
            // Still allow regular click for navigation
            holder.itemView.setOnClickListener {
                val messageId = messageData["id"]?.toString() 
                    ?: messageData["key"]?.toString() 
                    ?: ""
                listener.onMessageClick(messageId, position)
            }
            
            // Set message layout alignment
            holder.messageLayout?.let { layout ->
                val layoutParams = layout.layoutParams as? LinearLayout.LayoutParams
                layoutParams?.let { params ->
                    params.gravity = if (isMyMessage) Gravity.END else Gravity.START
                    layout.layoutParams = params
                }
            }
            
            // Set message bubble background for deleted messages
            holder.messageBubble?.let { bubble ->
                if (isMyMessage) {
                    bubble.setBackgroundResource(R.drawable.shape_outgoing_message_single)
                } else {
                    bubble.setBackgroundResource(R.drawable.shape_incoming_message_single)
                }
            }
            
            // Set message time
            holder.messageTime?.let { timeView ->
                val timestamp = messageData["created_at"]?.toString()?.toLongOrNull()
                    ?: messageData["push_date"]?.toString()?.toLongOrNull() 
                    ?: System.currentTimeMillis()
                timeView.text = formatMessageTime(timestamp)
            }
            
            // Handle username display for group chats
            holder.senderUsername?.let { usernameView ->
                if (isGroupChat && !isMyMessage && userNamesMap.containsKey(msgUid)) {
                    usernameView.visibility = View.VISIBLE
                    usernameView.text = userNamesMap[msgUid]
                } else {
                    usernameView.visibility = View.GONE
                }
            }
            
            // Apply dynamic vertical spacing
            holder.bodyLayout?.let { bodyLayout ->
                val layoutParams = bodyLayout.layoutParams as? ViewGroup.MarginLayoutParams
                layoutParams?.let { params ->
                    val context = holder.itemView.context
                    val spacing = if (position > 0 && previousSenderId != null && previousSenderId != msgUid) {
                        context.resources.getDimensionPixelSize(R.dimen.message_vertical_spacing_sender_change)
                    } else {
                        context.resources.getDimensionPixelSize(R.dimen.message_vertical_spacing)
                    }
                    params.topMargin = spacing
                    bodyLayout.layoutParams = params
                }
            }
            
            // Update previous sender ID
            previousSenderId = msgUid
            
            // Early return - don't process normal message content
            return
        }
        
        // Message is not deleted - show content, hide placeholder
        holder.deletedMessagePlaceholder?.visibility = View.GONE
        holder.messageContentContainer?.visibility = View.VISIBLE
        
        // Apply dynamic vertical spacing based on sender changes
        holder.bodyLayout?.let { bodyLayout ->
            val layoutParams = bodyLayout.layoutParams as? ViewGroup.MarginLayoutParams
            layoutParams?.let { params ->
                val context = holder.itemView.context
                val spacing = if (position > 0 && previousSenderId != null && previousSenderId != msgUid) {
                    // Sender changed - use larger spacing
                    context.resources.getDimensionPixelSize(R.dimen.message_vertical_spacing_sender_change)
                } else {
                    // Same sender - use normal spacing
                    context.resources.getDimensionPixelSize(R.dimen.message_vertical_spacing)
                }
                params.topMargin = spacing
                bodyLayout.layoutParams = params
            }
        }
        
        // Update previous sender ID for next message
        previousSenderId = msgUid
        
        // Handle username display for group chats
        holder.senderUsername?.let { usernameView ->
            if (isGroupChat && !isMyMessage && userNamesMap.containsKey(msgUid)) {
                usernameView.visibility = View.VISIBLE
                usernameView.text = userNamesMap[msgUid]
            } else {
                usernameView.visibility = View.GONE
            }
        }
        
        // Handle forwarded indicator display
        holder.forwardedIndicator?.let { forwardedView ->
            val forwardedFromMessageId = messageData["forwarded_from_message_id"]?.toString()
            if (!forwardedFromMessageId.isNullOrEmpty()) {
                forwardedView.visibility = View.VISIBLE
            } else {
                forwardedView.visibility = View.GONE
            }
        }
        
        // Set message time - support both old and new field names
        holder.messageTime?.let { timeView ->
            val timestamp = messageData["created_at"]?.toString()?.toLongOrNull()
                ?: messageData["push_date"]?.toString()?.toLongOrNull() 
                ?: System.currentTimeMillis()
            timeView.text = formatMessageTime(timestamp)
        }
        
        // Handle edited indicator display
        holder.editedIndicator?.let { editedView ->
            val isEdited = messageData["is_edited"]?.toString()?.toBooleanStrictOrNull() ?: false
            if (isEdited) {
                editedView.visibility = View.VISIBLE
                // Add click listener to show edit history dialog
                editedView.setOnClickListener {
                    val messageId = messageData["id"]?.toString() 
                        ?: messageData["key"]?.toString() 
                        ?: ""
                    listener.onEditHistoryClick(messageId)
                }
            } else {
                editedView.visibility = View.GONE
                editedView.setOnClickListener(null)
            }
        }
        
        // Set message status for sent messages - support both field names
        holder.messageStatus?.let { statusView ->
            if (isMyMessage) {
                val deliveryStatus = messageData["delivery_status"]?.toString() 
                    ?: messageData["message_state"]?.toString() 
                    ?: "sent"
                
                when (deliveryStatus) {
                    "sending" -> {
                        statusView.setImageResource(R.drawable.ic_upload)
                        statusView.visibility = View.VISIBLE
                    }
                    "sent" -> {
                        statusView.setImageResource(R.drawable.ic_check_circle)
                        statusView.visibility = View.VISIBLE
                    }
                    "delivered" -> {
                        statusView.setImageResource(R.drawable.ic_check_circle)
                        statusView.visibility = View.VISIBLE
                    }
                    "read" -> {
                        statusView.setImageResource(R.drawable.ic_check_circle)
                        statusView.visibility = View.VISIBLE
                    }
                    else -> statusView.visibility = View.GONE
                }
            } else {
                statusView.visibility = View.GONE
            }
        }
        
        // Handle reply layout
        holder.replyLayout?.let { replyLayout ->
            val repliedMessageId = messageData["replied_message_id"]?.toString()
            if (!repliedMessageId.isNullOrEmpty() && repliedMessagesCache.containsKey(repliedMessageId)) {
                val repliedMessage = repliedMessagesCache[repliedMessageId]
                holder.replyText?.text = repliedMessage?.get("message_text")?.toString() ?: "Message"
                replyLayout.visibility = View.VISIBLE
            } else {
                replyLayout.visibility = View.GONE
            }
        }
        
        // Set message layout alignment
        holder.messageLayout?.let { layout ->
            val layoutParams = layout.layoutParams as? LinearLayout.LayoutParams
            layoutParams?.let { params ->
                params.gravity = if (isMyMessage) Gravity.END else Gravity.START
                layout.layoutParams = params
            }
        }
        
        // Set message bubble background and styling
        holder.messageBubble?.let { bubble ->
            if (isMyMessage) {
                bubble.setBackgroundResource(R.drawable.shape_outgoing_message_single)
            } else {
                bubble.setBackgroundResource(R.drawable.shape_incoming_message_single)
            }
        }
        
        // Set click listeners - support both id field names
        holder.itemView.setOnClickListener {
            val messageId = messageData["id"]?.toString() 
                ?: messageData["key"]?.toString() 
                ?: ""
            listener.onMessageClick(messageId, position)
        }
        
        holder.itemView.setOnLongClickListener {
            val messageId = messageData["id"]?.toString() 
                ?: messageData["key"]?.toString() 
                ?: ""
            
            // Trigger haptic feedback on long-press
            holder.itemView.performHapticFeedback(
                android.view.HapticFeedbackConstants.LONG_PRESS,
                android.view.HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING
            )
            
            // Call listener and return true to consume the event
            listener.onMessageLongClick(messageId, position)
        }
    }

    private fun bindTextViewHolder(holder: TextViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
        val currentUser = authService.getCurrentUser()
        val myUid = currentUser?.id ?: ""
        val msgUid = messageData["sender_id"]?.toString() 
            ?: messageData["uid"]?.toString() 
            ?: ""
        val isMyMessage = msgUid == myUid
        
        // Support both content (Supabase) and message_text (legacy) field names
        val messageText = messageData["content"]?.toString() 
            ?: messageData["message_text"]?.toString() 
            ?: ""
        holder.messageText.text = messageText
        
        // Apply text color based on message type
        val context = holder.itemView.context
        holder.messageText.setTextColor(
            if (isMyMessage) {
                context.getColor(R.color.md_theme_onPrimaryContainer)
            } else {
                context.getColor(R.color.md_theme_onSecondaryContainer)
            }
        )
    }

    private fun bindMediaViewHolder(holder: MediaViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
        val currentUser = authService.getCurrentUser()
        val myUid = currentUser?.id ?: ""
        val msgUid = messageData["sender_id"]?.toString() 
            ?: messageData["uid"]?.toString() 
            ?: ""
        val isMyMessage = msgUid == myUid
        val attachments = messageData["attachments"] as? ArrayList<HashMap<String, Any?>>
        
        holder.mediaGrid.removeAllViews()
        
        attachments?.forEach { attachment ->
            val imageView = ImageView(context)
            val imageUrl = attachment["url"]?.toString()
            
            if (!imageUrl.isNullOrEmpty()) {
                Glide.with(context!!)
                    .load(imageUrl)
                    .transform(RoundedCorners(16))
                    .into(imageView)
                
                imageView.setOnClickListener {
                    listener.onAttachmentClick(imageUrl, attachment["type"]?.toString() ?: "image")
                }
            }
            
            holder.mediaGrid.addView(imageView)
        }
        
        // Support both content and message_text for caption
        val caption = messageData["content"]?.toString() 
            ?: messageData["message_text"]?.toString() 
            ?: ""
        holder.mediaCaption?.text = caption
        
        // Apply text color to caption based on message type
        holder.mediaCaption?.let { captionView ->
            val context = holder.itemView.context
            captionView.setTextColor(
                if (isMyMessage) {
                    context.getColor(R.color.md_theme_onPrimaryContainer)
                } else {
                    context.getColor(R.color.md_theme_onSecondaryContainer)
                }
            )
        }
    }

    private fun bindVideoViewHolder(holder: VideoViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
        val currentUser = authService.getCurrentUser()
        val myUid = currentUser?.id ?: ""
        val msgUid = messageData["sender_id"]?.toString() 
            ?: messageData["uid"]?.toString() 
            ?: ""
        val isMyMessage = msgUid == myUid
        val attachments = messageData["attachments"] as? ArrayList<HashMap<String, Any?>>
        val videoAttachment = attachments?.firstOrNull()
        
        val thumbnailUrl = videoAttachment?.get("thumbnailUrl")?.toString()
        val videoUrl = videoAttachment?.get("url")?.toString()
        
        if (!thumbnailUrl.isNullOrEmpty()) {
            Glide.with(context!!)
                .load(thumbnailUrl)
                .transform(RoundedCorners(16))
                .into(holder.videoThumbnail)
        }
        
        holder.playButton.setOnClickListener {
            if (!videoUrl.isNullOrEmpty()) {
                listener.onAttachmentClick(videoUrl, "video")
            }
        }
        
        // Support both content and message_text for caption
        val caption = messageData["content"]?.toString() 
            ?: messageData["message_text"]?.toString() 
            ?: ""
        holder.videoCaption?.text = caption
        
        // Apply text color to caption based on message type
        holder.videoCaption?.let { captionView ->
            val context = holder.itemView.context
            captionView.setTextColor(
                if (isMyMessage) {
                    context.getColor(R.color.md_theme_onPrimaryContainer)
                } else {
                    context.getColor(R.color.md_theme_onSecondaryContainer)
                }
            )
        }
    }

    private fun bindTypingViewHolder(holder: TypingViewHolder, position: Int) {
        // Typing animation is handled by Lottie animation
    }

    private fun bindLinkPreviewViewHolder(holder: LinkPreviewViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
        val currentUser = authService.getCurrentUser()
        val myUid = currentUser?.id ?: ""
        val msgUid = messageData["sender_id"]?.toString() 
            ?: messageData["uid"]?.toString() 
            ?: ""
        val isMyMessage = msgUid == myUid
        
        // Support both content and message_text field names
        val messageText = messageData["content"]?.toString() 
            ?: messageData["message_text"]?.toString() 
            ?: ""
        
        holder.messageText.text = messageText
        
        // Apply text color based on message type
        val context = holder.itemView.context
        holder.messageText.setTextColor(
            if (isMyMessage) {
                context.getColor(R.color.md_theme_onPrimaryContainer)
            } else {
                context.getColor(R.color.md_theme_onSecondaryContainer)
            }
        )
        
        val url = LinkPreviewUtil.extractUrl(messageText)
        if (url != null) {
            // TODO: Implement link preview loading
            holder.linkPreviewCard.visibility = View.VISIBLE
            holder.linkUrl.text = url
        } else {
            holder.linkPreviewCard.visibility = View.GONE
        }
    }

    private fun bindVoiceMessageViewHolder(holder: VoiceMessageViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
        val attachments = messageData["attachments"] as? ArrayList<HashMap<String, Any?>>
        val audioAttachment = attachments?.firstOrNull()
        val audioUrl = audioAttachment?.get("url")?.toString()
        
        holder.playPauseButton.setOnClickListener {
            if (!audioUrl.isNullOrEmpty()) {
                toggleVoicePlayback(holder, audioUrl)
            }
        }
        
        val duration = audioAttachment?.get("duration")?.toString()?.toLongOrNull() ?: 0L
        holder.duration.text = formatDuration(duration)
    }

    private fun bindErrorViewHolder(holder: ErrorViewHolder, position: Int) {
        val messageData = data[position]
        
        // Display user-friendly error message
        holder.errorMessageText.text = context?.getString(R.string.failed_to_send) ?: "Failed to send"
        holder.retryText.text = context?.getString(R.string.tap_to_retry) ?: "Tap to retry"
        
        // Log full error details for debugging (never display in UI)
        val errorDetails = messageData["error"]?.toString()
        val exception = messageData["exception"] as? Exception
        val messageId = messageData["id"]?.toString() 
            ?: messageData["key"]?.toString() 
            ?: "unknown"
        
        Log.e(TAG, "Message send failed for message ID: $messageId. Error: $errorDetails", exception)
        
        // Set message time
        holder.messageTime?.let { timeView ->
            val timestamp = messageData["created_at"]?.toString()?.toLongOrNull()
                ?: messageData["push_date"]?.toString()?.toLongOrNull() 
                ?: System.currentTimeMillis()
            timeView.text = formatMessageTime(timestamp)
        }
        
        // Set message layout alignment (error messages are always from current user)
        holder.messageLayout?.let { layout ->
            val layoutParams = layout.layoutParams as? LinearLayout.LayoutParams
            layoutParams?.let { params ->
                params.gravity = Gravity.END
                layout.layoutParams = params
            }
        }
        
        // Implement retry click listener
        holder.itemView.setOnClickListener {
            listener.onMessageRetry(messageId, position)
        }
        
        // Also allow long click for message options
        holder.itemView.setOnLongClickListener {
            listener.onMessageLongClick(messageId, position)
        }
    }

    private fun bindLoadingViewHolder(holder: LoadingViewHolder, position: Int) {
        // Loading is handled by progress bar only
    }

    private fun toggleVoicePlayback(holder: VoiceMessageViewHolder, audioUrl: String) {
        try {
            if (holder.mediaPlayer == null) {
                holder.mediaPlayer = MediaPlayer().apply {
                    setDataSource(audioUrl)
                    prepareAsync()
                    setOnPreparedListener { player ->
                        player.start()
                        holder.playPauseButton.setImageResource(R.drawable.ic_close)
                        startProgressUpdate(holder)
                    }
                    setOnCompletionListener {
                        holder.playPauseButton.setImageResource(R.drawable.ic_play_circle_filled)
                        holder.waveform.progress = 0
                    }
                }
            } else {
                if (holder.mediaPlayer!!.isPlaying) {
                    holder.mediaPlayer!!.pause()
                    holder.playPauseButton.setImageResource(R.drawable.ic_play_circle_filled)
                } else {
                    holder.mediaPlayer!!.start()
                    holder.playPauseButton.setImageResource(R.drawable.ic_close)
                    startProgressUpdate(holder)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing voice message", e)
        }
    }

    private fun startProgressUpdate(holder: VoiceMessageViewHolder) {
        holder.handler = Handler(Looper.getMainLooper())
        val updateProgress = object : Runnable {
            override fun run() {
                holder.mediaPlayer?.let { player ->
                    if (player.isPlaying) {
                        val progress = (player.currentPosition * 100) / player.duration
                        holder.waveform.progress = progress
                        holder.handler?.postDelayed(this, 100)
                    }
                }
            }
        }
        holder.handler?.post(updateProgress)
    }

    private fun formatMessageTime(timestamp: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp
        
        val now = Calendar.getInstance()
        
        return if (isSameDay(calendar, now)) {
            SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
        } else if (isYesterday(calendar, now)) {
            "Yesterday ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))}"
        } else {
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(timestamp))
        }
    }

    private fun formatDuration(durationMs: Long): String {
        val seconds = (durationMs / 1000) % 60
        val minutes = (durationMs / (1000 * 60)) % 60
        return String.format("%d:%02d", minutes, seconds)
    }

    private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(messageTime: Calendar, now: Calendar): Boolean {
        val yesterday = Calendar.getInstance()
        yesterday.timeInMillis = now.timeInMillis
        yesterday.add(Calendar.DAY_OF_YEAR, -1)
        
        return isSameDay(messageTime, yesterday)
    }

    /**
     * Update a message with fade animation (for edited messages)
     */
    fun updateMessageWithAnimation(position: Int, newMessageData: HashMap<String, Any?>) {
        if (position >= 0 && position < data.size) {
            data[position] = newMessageData
            notifyItemChanged(position)
        }
    }

    /**
     * Remove a message with slide-out animation (for deleted messages)
     */
    fun removeMessageWithAnimation(position: Int, recyclerView: RecyclerView) {
        if (position >= 0 && position < data.size) {
            val viewHolder = recyclerView.findViewHolderForAdapterPosition(position)
            viewHolder?.itemView?.let { view ->
                val currentUser = authService.getCurrentUser()
                val myUid = currentUser?.id ?: ""
                val messageData = data[position]
                val msgUid = messageData["sender_id"]?.toString() 
                    ?: messageData["uid"]?.toString() 
                    ?: ""
                val isMyMessage = msgUid == myUid
                
                MessageAnimations.applyDeletedMessageAnimation(view, isMyMessage) {
                    data.removeAt(position)
                    notifyItemRemoved(position)
                }
            } ?: run {
                // If view is not visible, remove without animation
                data.removeAt(position)
                notifyItemRemoved(position)
            }
        }
    }

    /**
     * Scroll to a message and highlight it
     */
    fun scrollToMessageWithHighlight(recyclerView: RecyclerView, position: Int) {
        MessageAnimations.scrollToMessageWithHighlight(recyclerView, position)
    }
}