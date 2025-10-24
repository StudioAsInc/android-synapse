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
 * Adapter for displaying posts in a RecyclerView
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
            if (markwon != null) {
                markwon.setMarkdown(contentText, post.postText ?: "")
            } else {
                contentText.text = post.postText ?: ""
            }

            // Set author info
            authorText.text = post.authorUid // This should be replaced with actual username
            
            // Set counts
            likesCountText.text = post.likesCount.toString()
            commentsCountText.text = post.commentsCount.toString()

            // Set click listeners
            likeButton.setOnClickListener { onLikeClicked?.invoke(post) }
            commentButton.setOnClickListener { onCommentClicked?.invoke(post) }
            shareButton.setOnClickListener { onShareClicked?.invoke(post) }
            moreButton.setOnClickListener { onMoreOptionsClicked?.invoke(post) }
            authorText.setOnClickListener { onUserClicked?.invoke(post.authorUid) }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp
            
            return when {
                diff < 60_000 -> "Just now"
                diff < 3600_000 -> "${diff / 60_000}m"
                diff < 86400_000 -> "${diff / 3600_000}h"
                diff < 604800_000 -> "${diff / 86400_000}d"
                else -> "${diff / 604800_000}w"
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