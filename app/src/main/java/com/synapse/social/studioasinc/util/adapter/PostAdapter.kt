package com.synapse.social.studioasinc.util.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.synapse.social.studioasinc.ProfileViewModel
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.SynapsePostCvBinding
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.User
import com.synapse.social.studioasinc.util.DateFormatter
import com.synapse.social.studioasinc.util.NumberFormatter
import io.noties.markwon.Markwon

/**
 * Adapter for the list of posts in the profile screen.
 */
class PostAdapter(
    private val markwon: Markwon,
    private val onLikeClicked: (Post) -> Unit,
    private val onCommentClicked: (Post) -> Unit,
    private val onShareClicked: (Post) -> Unit,
    private val onMoreOptionsClicked: (Post) -> Unit,
    private val onFavoriteClicked: (Post) -> Unit,
    private val onUserClicked: (String) -> Unit
) : ListAdapter<ProfileViewModel.PostUiState, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = SynapsePostCvBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    /**
     * ViewHolder for a post item.
     */
    inner class PostViewHolder(private val binding: SynapsePostCvBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.likeButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onLikeClicked(getItem(adapterPosition).post)
                }
            }
            binding.commentsButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onCommentClicked(getItem(adapterPosition).post)
                }
            }
            binding.shareButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onShareClicked(getItem(adapterPosition).post)
                }
            }
            binding.topMoreButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onMoreOptionsClicked(getItem(adapterPosition).post)
                }
            }
            binding.favoritePostButton.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onFavoriteClicked(getItem(adapterPosition).post)
                }
            }
            binding.userInfo.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onUserClicked(getItem(adapterPosition).post.uid)
                }
            }
        }

        /**
         * Binds the post data to the views.
         */
        fun bind(postState: ProfileViewModel.PostUiState) {
            val post = postState.post
            val user = postState.user
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid

            // Handle visibility of private posts
            if (post.postVisibility == "private" && post.uid != currentUid) {
                binding.root.visibility = View.GONE
                binding.root.layoutParams = RecyclerView.LayoutParams(0, 0)
                return
            } else {
                binding.root.visibility = View.VISIBLE
                binding.root.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            markwon.setMarkdown(binding.postMessageTextMiddle, post.postText ?: "")

            // Load post image
            if (post.postImage != "null") {
                binding.postImage.visibility = View.VISIBLE
                Glide.with(binding.root.context)
                    .load(Uri.parse(post.postImage))
                    .into(binding.postImage)
                binding.postImage.setOnClickListener {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(post.postImage))
                    binding.root.context.startActivity(intent)
                }
            } else {
                binding.postImage.visibility = View.GONE
            }

            // User Info
            user?.let {
                binding.userInfoUsername.text = if (it.nickname == "null") "@${it.username}" else it.nickname
                if (it.avatar == "null") {
                    binding.userInfoProfileImage.setImageResource(R.drawable.avatar)
                } else {
                    Glide.with(binding.root.context)
                        .load(Uri.parse(it.avatar))
                        .into(binding.userInfoProfileImage)
                }
            }

            // Post Stats
            binding.likeButtonCount.text = NumberFormatter.format(postState.likeCount.toDouble())
            binding.commentsButtonCount.text = NumberFormatter.format(postState.commentCount.toDouble())
            binding.postPublishDate.text = DateFormatter.format(binding.root.context, post.publishDate.toLong())

            // Post State
            binding.likeButtonCount.visibility = if (post.postHideLikeCount == "true") View.GONE else View.VISIBLE
            binding.commentsButton.visibility = if (post.postDisableComments == "true") View.GONE else View.VISIBLE
            binding.commentsButtonCount.visibility = if (post.postHideCommentsCount == "true") View.GONE else View.VISIBLE
            binding.postPrivateStateIcon.visibility = if (post.postVisibility == "private") View.VISIBLE else View.GONE

            // Like and Favorite Buttons
            updateLikeButton(postState.isLiked)
            updateFavoriteButton(postState.isFavorited)
        }

        private fun updateLikeButton(isLiked: Boolean) {
            val icon = if (isLiked) R.drawable.post_icons_1_2 else R.drawable.post_icons_1_1
            val color = if (isLiked) R.color.md_theme_primary else R.color.md_theme_onSurface
            binding.likeButtonIc.setImageResource(icon)
            binding.likeButtonIc.setColorFilter(ContextCompat.getColor(binding.root.context, color))
        }

        private fun updateFavoriteButton(isFavorited: Boolean) {
            val favIcon = if (isFavorited) R.drawable.delete_favorite_post_ic else R.drawable.add_favorite_post_ic
            binding.favoritePostButton.setImageResource(favIcon)
        }
    }

    /**
     * DiffUtil callback for the list of posts.
     */
    private class PostDiffCallback : DiffUtil.ItemCallback<ProfileViewModel.PostUiState>() {
        override fun areItemsTheSame(oldItem: ProfileViewModel.PostUiState, newItem: ProfileViewModel.PostUiState): Boolean {
            return oldItem.post.key == newItem.post.key
        }

        override fun areContentsTheSame(oldItem: ProfileViewModel.PostUiState, newItem: ProfileViewModel.PostUiState): Boolean {
            return oldItem == newItem
        }
    }
}
