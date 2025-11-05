package com.synapse.social.studioasinc.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.SeekBar
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.gridlayout.widget.GridLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.model.ChatAttachmentImpl
import com.synapse.social.studioasinc.model.Message
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter for displaying chat messages
 */
class ChatAdapter(
    private val context: Context,
    private val onMessageLongClick: (Message) -> Unit = {},
    private val onImageClick: (List<String>, Int) -> Unit = { _, _ -> },
    private val onVideoClick: (String) -> Unit = {},
    private val onAudioClick: (String) -> Unit = {},
    private val onDocumentClick: (ChatAttachmentImpl) -> Unit = {}
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    private val authService = SupabaseAuthenticationService()
    private val currentUserId = authService.getCurrentUserId()

    companion object {
        private const val VIEW_TYPE_MESSAGE_SENT = 1
        private const val VIEW_TYPE_MESSAGE_RECEIVED = 2
        private const val VIEW_TYPE_IMAGE_SENT = 3
        private const val VIEW_TYPE_IMAGE_RECEIVED = 4
        private const val VIEW_TYPE_VIDEO_SENT = 5
        private const val VIEW_TYPE_VIDEO_RECEIVED = 6
        private const val VIEW_TYPE_AUDIO_SENT = 7
        private const val VIEW_TYPE_AUDIO_RECEIVED = 8
        private const val VIEW_TYPE_DOCUMENT_SENT = 9
        private const val VIEW_TYPE_DOCUMENT_RECEIVED = 10
    }

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        val isSent = message.senderId == currentUserId
        
        // Check if message has attachments
        val attachments = message.attachments
        if (!attachments.isNullOrEmpty()) {
            val firstAttachment = attachments.first()
            return when (firstAttachment.type) {
                "image" -> if (isSent) VIEW_TYPE_IMAGE_SENT else VIEW_TYPE_IMAGE_RECEIVED
                "video" -> if (isSent) VIEW_TYPE_VIDEO_SENT else VIEW_TYPE_VIDEO_RECEIVED
                "audio" -> if (isSent) VIEW_TYPE_AUDIO_SENT else VIEW_TYPE_AUDIO_RECEIVED
                "document" -> if (isSent) VIEW_TYPE_DOCUMENT_SENT else VIEW_TYPE_DOCUMENT_RECEIVED
                else -> if (isSent) VIEW_TYPE_MESSAGE_SENT else VIEW_TYPE_MESSAGE_RECEIVED
            }
        }
        
        return if (isSent) VIEW_TYPE_MESSAGE_SENT else VIEW_TYPE_MESSAGE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_MESSAGE_SENT -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false)
                SentMessageViewHolder(view)
            }
            VIEW_TYPE_MESSAGE_RECEIVED -> {
                val view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false)
                ReceivedMessageViewHolder(view)
            }
            VIEW_TYPE_IMAGE_SENT, VIEW_TYPE_IMAGE_RECEIVED -> {
                val view = inflater.inflate(R.layout.chat_bubble_image, parent, false)
                ImageAttachmentViewHolder(view, viewType == VIEW_TYPE_IMAGE_SENT)
            }
            VIEW_TYPE_VIDEO_SENT, VIEW_TYPE_VIDEO_RECEIVED -> {
                val view = inflater.inflate(R.layout.chat_bubble_video, parent, false)
                VideoAttachmentViewHolder(view, viewType == VIEW_TYPE_VIDEO_SENT)
            }
            VIEW_TYPE_AUDIO_SENT, VIEW_TYPE_AUDIO_RECEIVED -> {
                val view = inflater.inflate(R.layout.chat_bubble_audio, parent, false)
                AudioAttachmentViewHolder(view, viewType == VIEW_TYPE_AUDIO_SENT)
            }
            VIEW_TYPE_DOCUMENT_SENT, VIEW_TYPE_DOCUMENT_RECEIVED -> {
                val view = inflater.inflate(R.layout.chat_bubble_document, parent, false)
                DocumentAttachmentViewHolder(view, viewType == VIEW_TYPE_DOCUMENT_SENT)
            }
            else -> throw IllegalArgumentException("Invalid view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
            is ImageAttachmentViewHolder -> holder.bind(message)
            is VideoAttachmentViewHolder -> holder.bind(message)
            is AudioAttachmentViewHolder -> holder.bind(message)
            is DocumentAttachmentViewHolder -> holder.bind(message)
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }
        
        val payload = payloads[0]
        if (payload is UploadProgressPayload) {
            when (holder) {
                is ImageAttachmentViewHolder -> holder.bind(getItem(position), payload)
                is VideoAttachmentViewHolder -> holder.bind(getItem(position), payload)
                is AudioAttachmentViewHolder -> holder.bind(getItem(position), payload)
                is DocumentAttachmentViewHolder -> holder.bind(getItem(position), payload)
                else -> super.onBindViewHolder(holder, position, payloads)
            }
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    inner class SentMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(android.R.id.text1)
        private val timeText: TextView = itemView.findViewById(android.R.id.text2)

        fun bind(message: Message) {
            messageText.text = message.getDisplayContent()
            timeText.text = formatTime(message.createdAt)
            
            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    inner class ReceivedMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(android.R.id.text1)
        private val timeText: TextView = itemView.findViewById(android.R.id.text2)
        private val senderName: TextView = itemView.findViewById(android.R.id.text1)

        fun bind(message: Message) {
            messageText.text = "${message.senderName ?: "Unknown"}: ${message.getDisplayContent()}"
            timeText.text = formatTime(message.createdAt)
            
            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
        }
    }
    
    private fun formatDuration(milliseconds: Long): String {
        val seconds = milliseconds / 1000
        val minutes = seconds / 60
        val remainingSeconds = seconds % 60
        return String.format("%d:%02d", minutes, remainingSeconds)
    }
    
    /**
     * Updates upload progress for a message
     */
    fun updateUploadProgress(messageId: String, progress: Float, state: String, error: String? = null) {
        val position = currentList.indexOfFirst { it.id == messageId }
        if (position != -1) {
            notifyItemChanged(position, UploadProgressPayload(progress, state, error))
        }
    }
    
    /**
     * Payload for upload progress updates
     */
    data class UploadProgressPayload(
        val progress: Float,
        val state: String, // UPLOADING, COMPLETED, FAILED
        val error: String? = null
    )

    /**
     * ViewHolder for image attachments
     */
    inner class ImageAttachmentViewHolder(itemView: View, private val isSent: Boolean) : RecyclerView.ViewHolder(itemView) {
        private val imageGridLayout: GridLayout = itemView.findViewById(R.id.imageGridLayout)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val shimmerContainer: View = itemView.findViewById(R.id.shimmer_container)
        private val timeText: TextView = itemView.findViewById(R.id.date)
        private val senderUsername: TextView? = itemView.findViewById(R.id.senderUsername)
        private val messageBG: View = itemView.findViewById(R.id.messageBG)
        private val deletedMessagePlaceholder: View = itemView.findViewById(R.id.deletedMessagePlaceholder)
        private val messageContentContainer: View = itemView.findViewById(R.id.messageContentContainer)
        
        // Upload progress views
        private val uploadProgressOverlay: View = itemView.findViewById(R.id.uploadProgressOverlay)
        private val uploadProgressBar: ProgressBar = itemView.findViewById(R.id.uploadProgressBar)
        private val uploadProgressText: TextView = itemView.findViewById(R.id.uploadProgressText)
        private val uploadEstimatedTime: TextView = itemView.findViewById(R.id.uploadEstimatedTime)
        private val uploadErrorLayout: View = itemView.findViewById(R.id.uploadErrorLayout)
        private val uploadErrorText: TextView = itemView.findViewById(R.id.uploadErrorText)
        private val retryButton: View = itemView.findViewById(R.id.retryButton)
        private val uploadSuccessIcon: View = itemView.findViewById(R.id.uploadSuccessIcon)

        fun bind(message: Message, payload: UploadProgressPayload? = null) {
            // Handle payload updates for progress
            if (payload != null) {
                updateProgress(payload)
                return
            }
            
            bind(message)
        }
        
        fun bind(message: Message) {
            // Handle deleted messages
            if (message.isDeleted) {
                deletedMessagePlaceholder.visibility = View.VISIBLE
                messageContentContainer.visibility = View.GONE
                timeText.text = formatTime(message.createdAt)
                return
            } else {
                deletedMessagePlaceholder.visibility = View.GONE
                messageContentContainer.visibility = View.VISIBLE
            }

            // Show sender name for received messages
            if (!isSent && message.senderName != null) {
                senderUsername?.visibility = View.VISIBLE
                senderUsername?.text = message.senderName
            } else {
                senderUsername?.visibility = View.GONE
            }

            // Clear previous images
            imageGridLayout.removeAllViews()

            val attachments = message.attachments?.filter { it.type == "image" } ?: emptyList()
            
            // Add images to grid (2 columns)
            attachments.forEachIndexed { index, attachment ->
                val imageView = ImageView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        if (attachments.size == 1) 600 else 280,
                        if (attachments.size == 1) 600 else 280
                    )
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setPadding(4, 4, 4, 4)
                    
                    // Load thumbnail with Glide
                    Glide.with(context)
                        .load(attachment.thumbnailUrl ?: attachment.url)
                        .placeholder(R.drawable.ph_imgbluredsqure)
                        .error(R.drawable.ph_imgbluredsqure)
                        .thumbnail(0.1f) // Load low-res first
                        .centerCrop()
                        .into(this)
                    
                    // Click to open gallery
                    setOnClickListener {
                        val imageUrls = attachments.map { it.url }
                        onImageClick(imageUrls, index)
                    }
                }
                imageGridLayout.addView(imageView)
            }

            // Show caption if present
            if (!message.content.isNullOrEmpty()) {
                shimmerContainer.visibility = View.VISIBLE
                messageText.text = message.content
            } else {
                shimmerContainer.visibility = View.GONE
            }

            timeText.text = formatTime(message.createdAt)
            
            // Hide upload progress overlay by default
            uploadProgressOverlay.visibility = View.GONE
            
            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
        
        private fun updateProgress(payload: UploadProgressPayload) {
            when (payload.state) {
                "UPLOADING", "COMPRESSING" -> {
                    uploadProgressOverlay.visibility = View.VISIBLE
                    uploadProgressBar.visibility = View.VISIBLE
                    uploadProgressText.visibility = View.VISIBLE
                    uploadErrorLayout.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    uploadSuccessIcon.visibility = View.GONE
                    
                    val progressPercent = (payload.progress * 100).toInt()
                    uploadProgressBar.progress = progressPercent
                    uploadProgressText.text = if (payload.state == "COMPRESSING") {
                        "Compressing... $progressPercent%"
                    } else {
                        "Uploading... $progressPercent%"
                    }
                    
                    // Show estimated time for large uploads
                    if (progressPercent > 0 && progressPercent < 100) {
                        uploadEstimatedTime.visibility = View.VISIBLE
                        val estimatedSeconds = ((100 - progressPercent) * 0.5).toInt()
                        uploadEstimatedTime.text = "Estimated: ${estimatedSeconds}s"
                    } else {
                        uploadEstimatedTime.visibility = View.GONE
                    }
                }
                "COMPLETED" -> {
                    uploadProgressBar.visibility = View.GONE
                    uploadProgressText.visibility = View.GONE
                    uploadEstimatedTime.visibility = View.GONE
                    uploadErrorLayout.visibility = View.GONE
                    retryButton.visibility = View.GONE
                    uploadSuccessIcon.visibility = View.VISIBLE
                    
                    // Hide overlay after 1 second
                    itemView.postDelayed({
                        uploadProgressOverlay.visibility = View.GONE
                    }, 1000)
                }
                "FAILED" -> {
                    uploadProgressBar.visibility = View.GONE
                    uploadProgressText.visibility = View.GONE
                    uploadEstimatedTime.visibility = View.GONE
                    uploadSuccessIcon.visibility = View.GONE
                    uploadErrorLayout.visibility = View.VISIBLE
                    retryButton.visibility = View.VISIBLE
                    
                    uploadErrorText.text = payload.error ?: "Upload failed"
                }
                else -> {
                    uploadProgressOverlay.visibility = View.GONE
                }
            }
        }
    }

    /**
     * ViewHolder for video attachments
     */
    inner class VideoAttachmentViewHolder(itemView: View, private val isSent: Boolean) : RecyclerView.ViewHolder(itemView) {
        private val videoThumbnail: ImageView = itemView.findViewById(R.id.videoThumbnail)
        private val playButton: ImageView = itemView.findViewById(R.id.playButton)
        private val videoDuration: TextView? = itemView.findViewById(R.id.videoDuration)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val shimmerContainer: View = itemView.findViewById(R.id.shimmer_container)
        private val timeText: TextView = itemView.findViewById(R.id.date)
        private val senderUsername: TextView? = itemView.findViewById(R.id.senderUsername)
        private val deletedMessagePlaceholder: View = itemView.findViewById(R.id.deletedMessagePlaceholder)
        private val messageContentContainer: View = itemView.findViewById(R.id.messageContentContainer)

        fun bind(message: Message) {
            // Handle deleted messages
            if (message.isDeleted) {
                deletedMessagePlaceholder.visibility = View.VISIBLE
                messageContentContainer.visibility = View.GONE
                timeText.text = formatTime(message.createdAt)
                return
            } else {
                deletedMessagePlaceholder.visibility = View.GONE
                messageContentContainer.visibility = View.VISIBLE
            }

            // Show sender name for received messages
            if (!isSent && message.senderName != null) {
                senderUsername?.visibility = View.VISIBLE
                senderUsername?.text = message.senderName
            } else {
                senderUsername?.visibility = View.GONE
            }

            val attachment = message.attachments?.firstOrNull { it.type == "video" }
            
            if (attachment != null) {
                // Load video thumbnail with Glide
                Glide.with(context)
                    .load(attachment.thumbnailUrl ?: attachment.url)
                    .placeholder(R.drawable.ph_imgbluredsqure)
                    .error(R.drawable.ph_imgbluredsqure)
                    .thumbnail(0.1f) // Load low-res first
                    .centerCrop()
                    .into(videoThumbnail)
                
                // Show duration overlay
                attachment.duration?.let { duration ->
                    videoDuration?.visibility = View.VISIBLE
                    videoDuration?.text = formatDuration(duration)
                } ?: run {
                    videoDuration?.visibility = View.GONE
                }
                
                // Click to play video
                videoThumbnail.setOnClickListener {
                    onVideoClick(attachment.url)
                }
                playButton.setOnClickListener {
                    onVideoClick(attachment.url)
                }
            }

            // Show caption if present
            if (!message.content.isNullOrEmpty()) {
                shimmerContainer.visibility = View.VISIBLE
                messageText.text = message.content
            } else {
                shimmerContainer.visibility = View.GONE
            }

            timeText.text = formatTime(message.createdAt)
            
            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    /**
     * ViewHolder for audio attachments
     */
    inner class AudioAttachmentViewHolder(itemView: View, private val isSent: Boolean) : RecyclerView.ViewHolder(itemView) {
        private val audioFileName: TextView = itemView.findViewById(R.id.audioFileName)
        private val playPauseButton: ImageButton = itemView.findViewById(R.id.playPauseButton)
        private val seekBar: SeekBar = itemView.findViewById(R.id.seekBar)
        private val currentTime: TextView = itemView.findViewById(R.id.currentTime)
        private val totalDuration: TextView = itemView.findViewById(R.id.totalDuration)
        private val loadingIndicator: ProgressBar = itemView.findViewById(R.id.loadingIndicator)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val shimmerContainer: View = itemView.findViewById(R.id.shimmer_container)
        private val timeText: TextView = itemView.findViewById(R.id.date)
        private val senderUsername: TextView? = itemView.findViewById(R.id.senderUsername)
        private val deletedMessagePlaceholder: View = itemView.findViewById(R.id.deletedMessagePlaceholder)
        private val messageContentContainer: View = itemView.findViewById(R.id.messageContentContainer)

        fun bind(message: Message) {
            // Handle deleted messages
            if (message.isDeleted) {
                deletedMessagePlaceholder.visibility = View.VISIBLE
                messageContentContainer.visibility = View.GONE
                timeText.text = formatTime(message.createdAt)
                return
            } else {
                deletedMessagePlaceholder.visibility = View.GONE
                messageContentContainer.visibility = View.VISIBLE
            }

            // Show sender name for received messages
            if (!isSent && message.senderName != null) {
                senderUsername?.visibility = View.VISIBLE
                senderUsername?.text = message.senderName
            } else {
                senderUsername?.visibility = View.GONE
            }

            val attachment = message.attachments?.firstOrNull { it.type == "audio" }
            
            if (attachment != null) {
                // Set file name
                audioFileName.text = attachment.fileName ?: "Audio"
                
                // Set duration
                attachment.duration?.let { duration ->
                    totalDuration.text = formatDuration(duration)
                    currentTime.text = "0:00"
                    seekBar.max = (duration / 1000).toInt()
                    seekBar.progress = 0
                } ?: run {
                    totalDuration.text = "0:00"
                    currentTime.text = "0:00"
                }
                
                // Hide loading indicator (will be shown during playback)
                loadingIndicator.visibility = View.GONE
                
                // Click to play audio
                playPauseButton.setOnClickListener {
                    onAudioClick(attachment.url)
                }
            }

            // Show caption if present
            if (!message.content.isNullOrEmpty()) {
                shimmerContainer.visibility = View.VISIBLE
                messageText.text = message.content
            } else {
                shimmerContainer.visibility = View.GONE
            }

            timeText.text = formatTime(message.createdAt)
            
            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    /**
     * ViewHolder for document attachments
     */
    inner class DocumentAttachmentViewHolder(itemView: View, private val isSent: Boolean) : RecyclerView.ViewHolder(itemView) {
        private val documentIcon: ImageView = itemView.findViewById(R.id.documentIcon)
        private val documentFileName: TextView = itemView.findViewById(R.id.documentFileName)
        private val documentFileInfo: TextView = itemView.findViewById(R.id.documentFileInfo)
        private val downloadButton: ImageButton = itemView.findViewById(R.id.downloadButton)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)
        private val shimmerContainer: View = itemView.findViewById(R.id.shimmer_container)
        private val timeText: TextView = itemView.findViewById(R.id.date)
        private val senderUsername: TextView? = itemView.findViewById(R.id.senderUsername)
        private val deletedMessagePlaceholder: View = itemView.findViewById(R.id.deletedMessagePlaceholder)
        private val messageContentContainer: View = itemView.findViewById(R.id.messageContentContainer)

        fun bind(message: Message) {
            // Handle deleted messages
            if (message.isDeleted) {
                deletedMessagePlaceholder.visibility = View.VISIBLE
                messageContentContainer.visibility = View.GONE
                timeText.text = formatTime(message.createdAt)
                return
            } else {
                deletedMessagePlaceholder.visibility = View.GONE
                messageContentContainer.visibility = View.VISIBLE
            }

            // Show sender name for received messages
            if (!isSent && message.senderName != null) {
                senderUsername?.visibility = View.VISIBLE
                senderUsername?.text = message.senderName
            } else {
                senderUsername?.visibility = View.GONE
            }

            val attachment = message.attachments?.firstOrNull { it.type == "document" }
            
            if (attachment != null) {
                // Set file name
                documentFileName.text = attachment.fileName ?: "Document"
                
                // Set file info (type and size)
                val fileType = attachment.mimeType?.substringAfterLast("/")?.uppercase() ?: "FILE"
                val fileSize = attachment.fileSize?.let { formatFileSize(it) } ?: ""
                documentFileInfo.text = if (fileSize.isNotEmpty()) {
                    "$fileType • $fileSize"
                } else {
                    fileType
                }
                
                // Click to open document
                downloadButton.setOnClickListener {
                    onDocumentClick(attachment)
                }
                itemView.setOnClickListener {
                    onDocumentClick(attachment)
                }
            }

            // Show caption if present
            if (!message.content.isNullOrEmpty()) {
                shimmerContainer.visibility = View.VISIBLE
                messageText.text = message.content
            } else {
                shimmerContainer.visibility = View.GONE
            }

            timeText.text = formatTime(message.createdAt)
            
            itemView.setOnLongClickListener {
                onMessageLongClick(message)
                true
            }
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Message, newItem: Message): Boolean {
            return oldItem == newItem
        }
    }
}