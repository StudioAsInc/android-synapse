package com.synapse.social.studioasinc.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.MediaItem
import com.synapse.social.studioasinc.model.MediaType

class SelectedMediaAdapter(
    private val mediaItems: List<MediaItem>,
    private val onRemoveClick: (Int) -> Unit
) : RecyclerView.Adapter<SelectedMediaAdapter.MediaViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_selected_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        holder.bind(mediaItems[position])
    }

    override fun getItemCount(): Int = mediaItems.size

    inner class MediaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val mediaImage: ImageView = itemView.findViewById(R.id.mediaImage)
        private val removeButton: ImageView = itemView.findViewById(R.id.removeButton)
        private val videoIndicator: ImageView = itemView.findViewById(R.id.videoIndicator)

        init {
            removeButton.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveClick(position)
                }
            }
        }

        fun bind(mediaItem: MediaItem) {
            // Load thumbnail
            Glide.with(itemView.context)
                .load(mediaItem.url)
                .centerCrop()
                .placeholder(R.drawable.default_image)
                .into(mediaImage)

            // Show video indicator for videos
            videoIndicator.visibility = if (mediaItem.type == MediaType.VIDEO) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }
}