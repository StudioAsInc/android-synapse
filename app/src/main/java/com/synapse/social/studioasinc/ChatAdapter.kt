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
        private const val VIEW_TYPE_LOADING_MORE = 99
    }

    private var context: Context? = null
    private var secondUserAvatarUrl = ""
    private var firstUserName = ""
    private var secondUserName = ""
    private var appSettings: SharedPreferences? = null
    private var isGroupChat = false
    private var userNamesMap = HashMap<String, String>()
    
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
        val messageTime: TextView? = itemView.findViewById(R.id.date)
        val messageStatus: ImageView? = itemView.findViewById(R.id.message_state)
        val replyLayout: LinearLayout? = itemView.findViewById(R.id.mRepliedMessageLayout)
        val replyText: TextView? = itemView.findViewById(R.id.mRepliedMessageLayoutMessage)
        val messageBubble: LinearLayout? = itemView.findViewById(R.id.messageBG)
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

    // Loading More ViewHolder
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val loadingProgress: ProgressBar = itemView.findViewById(R.id.loadingMoreProgressBar)
    }

    // Binding methods for each view type
    private fun bindCommonMessageProperties(holder: BaseMessageViewHolder, position: Int) {
        val messageData = data[position]
        val currentUser = authService.getCurrentUser()
        val myUid = currentUser?.id ?: ""
        val msgUid = messageData["uid"]?.toString() ?: ""
        val isMyMessage = msgUid == myUid
        
        // Handle username display for group chats
        holder.senderUsername?.let { usernameView ->
            if (isGroupChat && !isMyMessage && userNamesMap.containsKey(msgUid)) {
                usernameView.visibility = View.VISIBLE
                usernameView.text = userNamesMap[msgUid]
            } else {
                usernameView.visibility = View.GONE
            }
        }
        
        // Set message time
        holder.messageTime?.let { timeView ->
            val pushDate = messageData["push_date"]?.toString()?.toLongOrNull() ?: 0L
            timeView.text = formatMessageTime(pushDate)
        }
        
        // Set message status for sent messages
        holder.messageStatus?.let { statusView ->
            if (isMyMessage) {
                val messageState = messageData["message_state"]?.toString() ?: "sent"
                when (messageState) {
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
        
        // Set message bubble alignment and styling
        holder.messageBubble?.let { bubble ->
            val layoutParams = bubble.layoutParams as LinearLayout.LayoutParams
            if (isMyMessage) {
                layoutParams.gravity = Gravity.END
                bubble.setBackgroundResource(R.drawable.shape_outgoing_message_single)
            } else {
                layoutParams.gravity = Gravity.START
                bubble.setBackgroundResource(R.drawable.shape_incoming_message_single)
            }
            bubble.layoutParams = layoutParams
        }
        
        // Set click listeners
        holder.itemView.setOnClickListener {
            val messageId = messageData["key"]?.toString() ?: ""
            listener.onMessageClick(messageId, position)
        }
        
        holder.itemView.setOnLongClickListener {
            val messageId = messageData["key"]?.toString() ?: ""
            listener.onMessageLongClick(messageId, position)
        }
    }

    private fun bindTextViewHolder(holder: TextViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
        holder.messageText.text = messageData["message_text"]?.toString() ?: ""
    }

    private fun bindMediaViewHolder(holder: MediaViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
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
        
        holder.mediaCaption?.text = messageData["message_text"]?.toString() ?: ""
    }

    private fun bindVideoViewHolder(holder: VideoViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
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
        
        holder.videoCaption?.text = messageData["message_text"]?.toString() ?: ""
    }

    private fun bindTypingViewHolder(holder: TypingViewHolder, position: Int) {
        // Typing animation is handled by Lottie animation
    }

    private fun bindLinkPreviewViewHolder(holder: LinkPreviewViewHolder, position: Int) {
        bindCommonMessageProperties(holder, position)
        val messageData = data[position]
        val messageText = messageData["message_text"]?.toString() ?: ""
        
        holder.messageText.text = messageText
        
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
}