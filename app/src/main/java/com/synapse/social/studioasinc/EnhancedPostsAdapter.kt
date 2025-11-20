package com.synapse.social.studioasinc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import com.synapse.social.studioasinc.animations.ReactionAnimations
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.ReactionType

/**
 * Enhanced adapter for displaying posts with reactions and multi-media support
 */
class EnhancedPostsAdapter(
    private val context: Context,
    private val currentUserId: String,
    private val onPostClicked: ((Post) -> Unit)? = null,
    private val onLikeClicked: ((Post) -> Unit)? = null,
    private val onCommentClicked: ((Post) -> Unit)? = null,
    private val onShareClicked: ((Post) -> Unit)? = null,
    private val onUserClicked: ((String) -> Unit)? = null,
    private val onReactionSelected: ((Post, ReactionType) -> Unit)? = null,
    private val onReactionSummaryClicked: ((Post) -> Unit)? = null,
    private val onReactionPickerRequested: ((Post, View) -> Unit)? = null
) : ListAdapter<Post, EnhancedPostsAdapter.PostViewHolder>(PostDiffCallback()) {

    fun setLoadingMore(isLoading: Boolean) {
        // TODO: Implement footer loading view
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_enhanced, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postCard: MaterialCardView = itemView.findViewById(R.id.postCard)
        private val authorAvatar: ImageView = itemView.findViewById(R.id.authorAvatar)
        private val authorName: TextView = itemView.findViewById(R.id.authorName)
        private val postTimestamp: TextView = itemView.findViewById(R.id.postTimestamp)
        private val postOptions: ImageView = itemView.findViewById(R.id.postOptions)
        private val postContent: TextView = itemView.findViewById(R.id.postContent)
        private val readMoreButton: TextView = itemView.findViewById(R.id.readMoreButton)
        private val postImage: ImageView = itemView.findViewById(R.id.postImage)
        private val mediaContainer: ViewGroup = itemView.findViewById(R.id.mediaContainer)
        private val mediaViewPager: ViewPager2 = itemView.findViewById(R.id.mediaViewPager)
        private val mediaCountBadge: TextView = itemView.findViewById(R.id.mediaCountBadge)
        private val pageIndicatorContainer: LinearLayout = itemView.findViewById(R.id.pageIndicatorContainer)
        private val reactionSummaryContainer: ViewGroup = itemView.findViewById(R.id.reactionSummaryContainer)
        private val reactionSummary: View = itemView.findViewById(R.id.reactionSummary)
        private val reactionEmojis: TextView = itemView.findViewById(R.id.reactionEmojis)
        private val reactionCount: TextView = itemView.findViewById(R.id.reactionCount)
        private val commentsCountText: TextView = itemView.findViewById(R.id.commentsCountText)
        private val divider: View = itemView.findViewById(R.id.divider)
        private val likeButton: View = itemView.findViewById(R.id.likeButton)
        private val likeIcon: ImageView = itemView.findViewById(R.id.likeIcon)
        private val likeText: TextView = itemView.findViewById(R.id.likeText)
        private val commentButton: View = itemView.findViewById(R.id.commentButton)
        private val shareButton: View = itemView.findViewById(R.id.shareButton)

        private var currentPost: Post? = null
        private var isContentExpanded = false

        fun bind(post: Post) {
            currentPost = post

            // Bind author info
            authorName.text = post.authorUid // TODO: Load actual username
            postTimestamp.text = formatTimestamp(post.timestamp)

            // Load avatar (placeholder for now)
            Glide.with(context)
                .load(R.drawable.avatar)
                .circleCrop()
                .into(authorAvatar)

            // Bind post content
            bindContent(post)

            // Bind media (single image or carousel)
            bindMedia(post)

            // Bind reactions
            bindReactions(post)

            // Setup click listeners
            setupClickListeners(post)
        }

        private fun bindContent(post: Post) {
            val content = post.postText ?: ""
            postContent.text = content

            // Show/hide read more button
            if (content.length > 200 && !isContentExpanded) {
                postContent.maxLines = 5
                readMoreButton.visibility = View.VISIBLE
                readMoreButton.setOnClickListener {
                    isContentExpanded = true
                    postContent.maxLines = Int.MAX_VALUE
                    readMoreButton.visibility = View.GONE
                }
            } else {
                postContent.maxLines = Int.MAX_VALUE
                readMoreButton.visibility = View.GONE
            }
        }

        private fun bindMedia(post: Post) {
            val mediaItems = post.mediaItems

            if (mediaItems != null && mediaItems.size > 1) {
                // Multiple media items - use carousel
                postImage.visibility = View.GONE
                mediaContainer.visibility = View.VISIBLE

                val adapter = MediaCarouselAdapter(context, mediaItems)
                mediaViewPager.adapter = adapter

                // Setup page indicators
                setupPageIndicators(mediaItems.size)

                // Show media count badge
                mediaCountBadge.visibility = View.VISIBLE
                mediaCountBadge.text = "1/${mediaItems.size}"

                // Update count badge when page changes
                mediaViewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        mediaCountBadge.text = "${position + 1}/${mediaItems.size}"
                        updatePageIndicators(position)
                    }
                })

            } else if (!post.postImage.isNullOrEmpty()) {
                // Single image
                mediaContainer.visibility = View.GONE
                postImage.visibility = View.VISIBLE

                Glide.with(context)
                    .load(post.postImage)
                    .placeholder(R.drawable.default_image)
                    .into(postImage)
            } else {
                // No media
                postImage.visibility = View.GONE
                mediaContainer.visibility = View.GONE
            }
        }

        private fun setupPageIndicators(count: Int) {
            if (count <= 1) {
                pageIndicatorContainer.visibility = View.GONE
                return
            }

            pageIndicatorContainer.visibility = View.VISIBLE
            pageIndicatorContainer.removeAllViews()

            for (i in 0 until count) {
                val dot = View(context).apply {
                    layoutParams = LinearLayout.LayoutParams(16, 16).apply {
                        marginEnd = 8
                    }
                    setBackgroundResource(if (i == 0) R.drawable.page_indicator_dot_selected else R.drawable.page_indicator_dot)
                    tag = i
                }
                pageIndicatorContainer.addView(dot)
            }
        }

        private fun updatePageIndicators(position: Int) {
            for (i in 0 until pageIndicatorContainer.childCount) {
                val dot = pageIndicatorContainer.getChildAt(i)
                dot.setBackgroundResource(
                    if (i == position) R.drawable.page_indicator_dot_selected else R.drawable.page_indicator_dot
                )
            }
        }

        private fun bindReactions(post: Post) {
            val totalReactions = post.getTotalReactionsCount()
            val hasReactions = totalReactions > 0
            val hasComments = post.commentsCount > 0

            if (hasReactions || hasComments) {
                reactionSummaryContainer.visibility = View.VISIBLE
                divider.visibility = View.VISIBLE

                // Show reaction summary
                if (hasReactions) {
                    val summary = post.getReactionSummary()
                    val topReactions = post.getTopReactions()

                    if (topReactions.isNotEmpty()) {
                        val emojis = topReactions.take(3).joinToString(" ") { it.first.emoji }
                        reactionEmojis.text = emojis
                        reactionCount.text = totalReactions.toString()
                        reactionSummary.visibility = View.VISIBLE
                    } else {
                        reactionSummary.visibility = View.GONE
                    }
                } else {
                    reactionSummary.visibility = View.GONE
                }

                // Show comments count
                if (hasComments) {
                    commentsCountText.visibility = View.VISIBLE
                    commentsCountText.text = "${post.commentsCount} ${if (post.commentsCount == 1) "comment" else "comments"}"
                } else {
                    commentsCountText.visibility = View.GONE
                }
            } else {
                reactionSummaryContainer.visibility = View.GONE
                divider.visibility = View.GONE
            }

            // Update like button based on user's reaction
            if (post.userReaction != null) {
                likeIcon.setImageResource(post.userReaction!!.iconRes)
                likeText.text = post.userReaction!!.displayName
                likeText.setTextColor(context.getColor(R.color.colorPrimary))
            } else {
                likeIcon.setImageResource(R.drawable.ic_reaction_like)
                likeText.text = "Like"
                likeText.setTextColor(context.getColor(R.color.text_secondary))
            }
        }

        private fun setupClickListeners(post: Post) {
            // Author click
            authorAvatar.setOnClickListener { onUserClicked?.invoke(post.authorUid) }
            authorName.setOnClickListener { onUserClicked?.invoke(post.authorUid) }

            // Post click
            postCard.setOnClickListener { onPostClicked?.invoke(post) }

            // Like button - single tap
            likeButton.setOnClickListener {
                onLikeClicked?.invoke(post)
            }

            // Like button - long press for reaction picker
            likeButton.setOnLongClickListener {
                onReactionPickerRequested?.invoke(post, likeButton)
                true
            }

            // Reaction summary click - show who reacted
            reactionSummary.setOnClickListener {
                onReactionSummaryClicked?.invoke(post)
            }

            // Comment button
            commentButton.setOnClickListener {
                onCommentClicked?.invoke(post)
            }

            // Share button
            shareButton.setOnClickListener {
                onShareClicked?.invoke(post)
            }

            // Options button
            postOptions.setOnClickListener {
                // Show post options menu
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 0 -> "Just now"
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
