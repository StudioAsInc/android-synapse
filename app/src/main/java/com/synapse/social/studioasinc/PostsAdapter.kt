package com.synapse.social.studioasinc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.synapse.social.studioasinc.model.Post
import io.noties.markwon.Markwon

/**
 * Adapter for displaying posts in a RecyclerView.
 * Supports markdown rendering, user interactions, and efficient list updates via DiffUtil.
 */
class PostsAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val markwon: Markwon? = null,
    private val onLikeClicked: ((Post) -> Unit)? = null,
    private val onCommentClicked: ((Post) -> Unit)? = null,
    private val onShareClicked: ((Post) -> Unit)? = null,
    private val onMoreOptionsClicked: ((Post) -> Unit)? = null,
    private val onFavoriteClicked: ((Post) -> Unit)? = null,
    private val onUserClicked: ((String) -> Unit)? = null
) : ListAdapter<Post, PostsAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentText: TextView = itemView.findViewById(R.id.postContent)
        private val authorText: TextView = itemView.findViewById(R.id.authorName)
        private val likeButton: View = itemView.findViewById(R.id.likeButton)
        private val commentButton: View = itemView.findViewById(R.id.commentButton)
        private val shareButton: ImageView = itemView.findViewById(R.id.shareButton)
        private val moreButton: ImageView = itemView.findViewById(R.id.postOptions)
        private val likesCountText: TextView = itemView.findViewById(R.id.likeCount)
        private val commentsCountText: TextView = itemView.findViewById(R.id.commentCount)

        fun bind(post: Post) {
            // Set content with markdown support if available
            val postContent = post.postText ?: ""
            if (markwon != null && postContent.isNotEmpty()) {
                markwon.setMarkdown(contentText, postContent)
            } else {
                contentText.text = postContent
            }

            // Set author info (TODO: Load actual username from users table)
            authorText.text = "@${post.authorUid}"
            
            // Set counts with proper formatting
            likesCountText.text = formatCount(post.likesCount)
            commentsCountText.text = formatCount(post.commentsCount)

            // Set click listeners with null safety
            likeButton.setOnClickListener { onLikeClicked?.invoke(post) }
            commentButton.setOnClickListener { onCommentClicked?.invoke(post) }
            shareButton.setOnClickListener { onShareClicked?.invoke(post) }
            moreButton.setOnClickListener { onMoreOptionsClicked?.invoke(post) }
            authorText.setOnClickListener { onUserClicked?.invoke(post.authorUid) }
        }
        
        /**
         * Format large numbers for display (e.g., 1.2K, 3.5M)
         */
        private fun formatCount(count: Int): String {
            return when {
                count < 1000 -> count.toString()
                count < 1_000_000 -> String.format("%.1fK", count / 1000.0)
                else -> String.format("%.1fM", count / 1_000_000.0)
            }
        }

        /**
         * Format timestamp to relative time string (e.g., "2h ago", "3d ago")
         */
        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 0 -> "Just now" // Handle future timestamps
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m ago"
                diff < 86400_000 -> "${diff / 3600_000}h ago"
                diff < 604800_000 -> "${diff / 86400_000}d ago"
                else -> "${diff / 604800_000}w ago"
            }
        }
    }

    class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}