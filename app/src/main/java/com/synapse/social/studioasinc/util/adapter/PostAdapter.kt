package com.synapse.social.studioasinc.util.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.SynapsePostCvBinding
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.User
import com.synapse.social.studioasinc.repository.UserRepository
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
    private val onFavoriteClicked: (Post) -> Unit
) : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = SynapsePostCvBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: PostViewHolder) {
        super.onViewRecycled(holder)
        holder.cleanup()
    }

    /**
     * ViewHolder for a post item.
     */
    inner class PostViewHolder(private val binding: SynapsePostCvBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val listeners = mutableListOf<Pair<DatabaseReference, ValueEventListener>>()
        private var isLiked = false
        private var isFavorited = false

        /**
         * Binds the post data to the views.
         */
        fun bind(post: Post) {
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
            } else {
                binding.postImage.visibility = View.GONE
            }

            // Fetch and display user data using the repository
            UserRepository.getUser(post.uid) { user ->
                user?.let {
                    binding.userInfoUsername.text = if (it.nickname == "null") "@${it.username}" else it.nickname
                    if (it.avatar == "null") {
                        binding.userInfoProfileImage.setImageResource(R.drawable.avatar)
                    } else {
                        Glide.with(binding.root.context)
                            .load(Uri.parse(it.avatar))
                            .into(binding.userInfoProfileImage)
                    }
                    // Implement badge logic here based on user properties
                }
            }

            // Fetch and display post stats
            fetchPostStats(post)

            // Set up click listeners
            binding.likeButton.setOnClickListener {
                onLikeClicked(post)
                isLiked = !isLiked
                updateLikeButton()
            }
            binding.commentsButton.setOnClickListener { onCommentClicked(post) }
            binding.shareButton.setOnClickListener { onShareClicked(post) }
            binding.topMoreButton.setOnClickListener { onMoreOptionsClicked(post) }
            binding.favoritePostButton.setOnClickListener {
                onFavoriteClicked(post)
                isFavorited = !isFavorited
                updateFavoriteButton()
            }

            binding.commentsButton.visibility = if (post.postDisableComments == "true") View.GONE else View.VISIBLE
        }

        private fun fetchPostStats(post: Post) {
            val db = FirebaseDatabase.getInstance()

            // Likes
            val likesRef = db.getReference("skyline/posts-likes").child(post.key)
            val likesListener = likesRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val likeCount = snapshot.childrenCount
                    binding.likeButtonCount.text = NumberFormatter.format(likeCount.toDouble())
                    binding.likeButtonCount.visibility = if (post.postHideLikeCount == "true") View.GONE else View.VISIBLE
                    // Update like icon state
                }

                override fun onCancelled(error: DatabaseError) {}
            })
            listeners.add(likesRef to likesListener)


            // Comments
            val commentsRef = db.getReference("skyline/posts-comments").child(post.key)
            val commentsListener = commentsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val commentCount = snapshot.childrenCount
                    binding.commentsButtonCount.text = NumberFormatter.format(commentCount.toDouble())
                    binding.commentsButtonCount.visibility = if (post.postHideCommentsCount == "true") View.GONE else View.VISIBLE
                }

                override fun onCancelled(error: DatabaseError) {}
            })
            listeners.add(commentsRef to commentsListener)


            // Publish Date
            binding.postPublishDate.text = DateFormatter.format(binding.root.context, post.publishDate.toLong())
        }

        private fun updateLikeButton() {
            val icon = if (isLiked) R.drawable.post_icons_1_2 else R.drawable.post_icons_1_1
            val color = if (isLiked) R.color.md_theme_primary else R.color.md_theme_onSurface
            binding.likeButtonIc.setImageResource(icon)
            binding.likeButtonIc.setColorFilter(ContextCompat.getColor(binding.root.context, color))
        }

        private fun updateFavoriteButton() {
            val favIcon = if (isFavorited) R.drawable.delete_favorite_post_ic else R.drawable.add_favorite_post_ic
            binding.favoritePostButton.setImageResource(favIcon)
        }


        fun cleanup() {
            listeners.forEach { (ref, listener) ->
                ref.removeEventListener(listener)
            }
            listeners.clear()
        }
    }

    /**
     * DiffUtil callback for the list of posts.
     */
    private class PostDiffCallback : DiffUtil.ItemCallback<Post>() {
        override fun areItemsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem.key == newItem.key
        }

        override fun areContentsTheSame(oldItem: Post, newItem: Post): Boolean {
            return oldItem == newItem
        }
    }
}
