package com.synapse.social.studioasinc.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ui.PlayerView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.MediaType

class MediaPagerAdapter(
    private val context: Context,
    private val mediaItems: List<com.synapse.social.studioasinc.model.MediaItem>,
    private val onMediaClick: ((com.synapse.social.studioasinc.model.MediaItem, Int) -> Unit)? = null
) : RecyclerView.Adapter<MediaPagerAdapter.MediaViewHolder>() {

    private val players = mutableMapOf<Int, ExoPlayer>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        return when (viewType) {
            TYPE_IMAGE -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_media_image, parent, false)
                ImageViewHolder(view)
            }
            TYPE_VIDEO -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_media_video, parent, false)
                VideoViewHolder(view)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val mediaItem = mediaItems[position]
        
        when (holder) {
            is ImageViewHolder -> {
                holder.bind(mediaItem)
                holder.itemView.setOnClickListener {
                    onMediaClick?.invoke(mediaItem, position)
                }
            }
            is VideoViewHolder -> {
                holder.bind(mediaItem, position)
                holder.itemView.setOnClickListener {
                    onMediaClick?.invoke(mediaItem, position)
                }
            }
        }
    }

    override fun getItemCount(): Int = mediaItems.size

    override fun getItemViewType(position: Int): Int {
        return when (mediaItems[position].type) {
            MediaType.IMAGE -> TYPE_IMAGE
            MediaType.VIDEO -> TYPE_VIDEO
        }
    }

    override fun onViewRecycled(holder: MediaViewHolder) {
        super.onViewRecycled(holder)
        if (holder is VideoViewHolder) {
            holder.releasePlayer()
        }
    }

    fun releaseAllPlayers() {
        players.values.forEach { it.release() }
        players.clear()
    }

    // ViewHolder classes
    abstract class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bind(mediaItem: com.synapse.social.studioasinc.model.MediaItem)
    }

    inner class ImageViewHolder(view: View) : MediaViewHolder(view) {
        private val imageView: ImageView = view.findViewById(R.id.mediaImageView)

        override fun bind(mediaItem: com.synapse.social.studioasinc.model.MediaItem) {
            Glide.with(context)
                .load(mediaItem.url)
                .placeholder(R.drawable.default_image)
                .error(R.drawable.default_image)
                .into(imageView)
        }
    }

    inner class VideoViewHolder(view: View) : MediaViewHolder(view) {
        private val playerView: PlayerView = view.findViewById(R.id.mediaPlayerView)
        private val thumbnailView: ImageView = view.findViewById(R.id.videoThumbnail)
        private val playButton: ImageView = view.findViewById(R.id.playButton)
        private var player: ExoPlayer? = null
        private var currentPosition: Int = -1

        override fun bind(mediaItem: com.synapse.social.studioasinc.model.MediaItem) {
            bind(mediaItem, bindingAdapterPosition)
        }

        fun bind(mediaItem: com.synapse.social.studioasinc.model.MediaItem, position: Int) {
            currentPosition = position
            
            // Show thumbnail initially
            if (!mediaItem.thumbnailUrl.isNullOrEmpty()) {
                Glide.with(context)
                    .load(mediaItem.thumbnailUrl)
                    .into(thumbnailView)
            } else {
                // Generate thumbnail from video URL if needed
                Glide.with(context)
                    .load(mediaItem.url)
                    .frame(1000000) // 1 second
                    .into(thumbnailView)
            }
            
            thumbnailView.visibility = View.VISIBLE
            playButton.visibility = View.VISIBLE
            playerView.visibility = View.GONE
            
            // Setup click to play
            playButton.setOnClickListener {
                setupPlayer(mediaItem)
            }
        }

        private fun setupPlayer(mediaItem: com.synapse.social.studioasinc.model.MediaItem) {
            // Release existing player if any
            releasePlayer()
            
            // Create new player
            player = ExoPlayer.Builder(context).build().also {
                players[currentPosition] = it
                playerView.player = it
                
                // Prepare media source
                val exoMediaItem = MediaItem.fromUri(Uri.parse(mediaItem.url))
                it.setMediaItem(exoMediaItem)
                it.prepare()
                it.playWhenReady = true
                
                // Hide thumbnail and show player
                thumbnailView.visibility = View.GONE
                playButton.visibility = View.GONE
                playerView.visibility = View.VISIBLE
            }
        }

        fun releasePlayer() {
            players.remove(currentPosition)?.release()
            player = null
            playerView.player = null
        }
    }

    companion object {
        private const val TYPE_IMAGE = 0
        private const val TYPE_VIDEO = 1
    }
}