package com.synapse.social.studioasinc.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.Comment
import com.synapse.social.studioasinc.model.Reply
import com.synapse.social.studioasinc.model.User
import de.hdodenhof.circleimageview.CircleImageView

class CommentsAdapter(
    private val onCommentLiked: (Comment) -> Unit,
    private val onReplyClicked: (Comment) -> Unit,
    private val onCommentLongClicked: (Comment) -> Unit,
    private val onShowReplies: (String) -> Unit,
    private val onReplyLiked: (Reply) -> Unit,
    private val onReplyLongClicked: (Reply) -> Unit
) : ListAdapter<Comment, CommentsAdapter.CommentViewHolder>(CommentDiffCallback()) {

    private var userMap: Map<String, User> = emptyMap()
    private var repliesMap: Map<String, List<Reply>> = emptyMap()

    fun setUserData(userMap: Map<String, User>) {
        this.userMap = userMap
        notifyDataSetChanged()
    }

    fun setReplies(repliesMap: Map<String, List<Reply>>) {
        this.repliesMap = repliesMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.synapse_comments_list_cv, parent, false)
        return CommentViewHolder(view, onReplyLiked, onReplyLongClicked)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = getItem(position)
        val user = userMap[comment.uid]
        val replies = repliesMap[comment.key] ?: emptyList()
        holder.bind(comment, user, replies, onCommentLiked, onReplyClicked, onCommentLongClicked, onShowReplies, userMap)
    }

    class CommentViewHolder(
        itemView: View,
        onReplyLiked: (Reply) -> Unit,
        onReplyLongClicked: (Reply) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: CircleImageView = itemView.findViewById(R.id.profileImage)
        private val username: TextView = itemView.findViewById(R.id.username)
        private val commentText: TextView = itemView.findViewById(R.id.comment_text)
        private val likeCount: TextView = itemView.findViewById(R.id.like_count)
        private val likeButton: ImageView = itemView.findViewById(R.id.like_unlike_ic)
        private val replyButton: LinearLayout = itemView.findViewById(R.id.body)
        private val badge: ImageView = itemView.findViewById(R.id.badge)
        private val repliesRecyclerView: RecyclerView = itemView.findViewById(R.id.other_replies_list)
        private val showRepliesButton: TextView = itemView.findViewById(R.id.show_other_replies_button)
        private val hideRepliesButton: TextView = itemView.findViewById(R.id.hide_replies_list_button)

        private val repliesAdapter = RepliesAdapter(onReplyLiked, onReplyLongClicked)

        init {
            repliesRecyclerView.adapter = repliesAdapter
            repliesRecyclerView.layoutManager = LinearLayoutManager(itemView.context)
        }

        fun bind(
            comment: Comment,
            user: User?,
            replies: List<Reply>,
            onCommentLiked: (Comment) -> Unit,
            onReplyClicked: (Comment) -> Unit,
            onCommentLongClicked: (Comment) -> Unit,
            onShowReplies: (String) -> Unit,
            userMap: Map<String, User>
        ) {
            commentText.text = comment.comment
            likeCount.text = comment.like.toString()

            user?.let {
                username.text = it.nickname ?: "@${it.username}"

                if (it.avatar != null && it.avatar != "null") {
                    Glide.with(itemView.context).load(Uri.parse(it.avatar)).into(profileImage)
                } else {
                    profileImage.setImageResource(R.drawable.avatar)
                }

                when (it.accountType) {
                    "admin" -> badge.setImageResource(R.drawable.admin_badge)
                    "moderator" -> badge.setImageResource(R.drawable.moderator_badge)
                    "support" -> badge.setImageResource(R.drawable.support_badge)
                    else -> if (it.verify == true) badge.setImageResource(R.drawable.verified_badge)
                }
            }

            likeButton.setOnClickListener { onCommentLiked(comment) }
            replyButton.setOnClickListener { onReplyClicked(comment) }
            replyButton.setOnLongClickListener {
                onCommentLongClicked(comment)
                true
            }

            repliesAdapter.setUserData(userMap)
            repliesAdapter.submitList(replies)

            showRepliesButton.setOnClickListener {
                onShowReplies(comment.key)
                repliesRecyclerView.visibility = View.VISIBLE
                showRepliesButton.visibility = View.GONE
                hideRepliesButton.visibility = View.VISIBLE
            }

            hideRepliesButton.setOnClickListener {
                repliesRecyclerView.visibility = View.GONE
                showRepliesButton.visibility = View.VISIBLE
                hideRepliesButton.visibility = View.GONE
            }
        }
    }

    class CommentDiffCallback : DiffUtil.ItemCallback<Comment>() {
        override fun areItemsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: Comment, newItem: Comment): Boolean {
            return oldItem == newItem
        }
    }
}
