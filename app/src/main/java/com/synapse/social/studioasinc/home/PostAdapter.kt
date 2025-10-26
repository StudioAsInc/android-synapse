package com.synapse.social.studioasinc.home

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.data.repository.AuthRepository
import com.synapse.social.studioasinc.data.repository.PostRepository
import com.synapse.social.studioasinc.data.repository.UserRepository
import com.synapse.social.studioasinc.data.repository.LikeRepository
import com.synapse.social.studioasinc.model.Post
import kotlinx.coroutines.launch
import android.widget.LinearLayout

class PostAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val authRepository: AuthRepository = AuthRepository(),
    private val postRepository: PostRepository = PostRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val likeRepository: LikeRepository = LikeRepository()
) : RecyclerView.Adapter<PostAdapter.PostViewHolder>() {

    private var posts = mutableListOf<Post>()

    fun updatePosts(newPosts: List<Post>) {
        posts.clear()
        posts.addAll(newPosts)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false)
        return PostViewHolder(view)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = posts[position]
        holder.bind(post)
    }

    override fun getItemCount(): Int = posts.size

    inner class PostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val postContent: TextView = itemView.findViewById(R.id.postContent)
        private val postImage: ImageView = itemView.findViewById(R.id.postImage)
        private val authorName: TextView = itemView.findViewById(R.id.authorName)
        private val likeButton: LinearLayout = itemView.findViewById(R.id.likeButton)
        private val likeIcon: ImageView = itemView.findViewById(R.id.likeIcon)
        private val likeCount: TextView = itemView.findViewById(R.id.likeCount)

        fun bind(post: Post) {
            // Set post content
            postContent.text = post.postText ?: ""
            
            // Load post image if available
            post.postImage?.let { imageUrl ->
                postImage.visibility = View.VISIBLE
                Glide.with(context)
                    .load(imageUrl)
                    .into(postImage)
            } ?: run {
                postImage.visibility = View.GONE
            }

            // Load author information
            lifecycleOwner.lifecycleScope.launch {
                userRepository.getUserById(post.authorUid)
                    .onSuccess { user ->
                        authorName.text = user?.username ?: "Unknown User"
                    }
                    .onFailure {
                        authorName.text = "Unknown User"
                    }
            }
            
            // Load like status and count
            loadLikeStatus(post)
            
            // Set up like button click listener
            likeButton.setOnClickListener {
                handleLikeClick(post)
            }
        }
        
        private fun loadLikeStatus(post: Post) {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    val currentUserId = authRepository.getCurrentUserId()
                    if (currentUserId != null) {
                        // Check if user has liked this post
                        likeRepository.isLiked(currentUserId, post.id, "post")
                            .onSuccess { isLiked ->
                                updateLikeIcon(isLiked)
                            }
                    }
                    
                    // Get like count
                    likeRepository.getLikeCount(post.id, "post")
                        .onSuccess { count ->
                            likeCount.text = count.toString()
                        }
                } catch (e: Exception) {
                    android.util.Log.e("PostAdapter", "Failed to load like status", e)
                }
            }
        }
        
        private fun handleLikeClick(post: Post) {
            lifecycleOwner.lifecycleScope.launch {
                try {
                    val currentUserId = authRepository.getCurrentUserId()
                    if (currentUserId == null) {
                        android.widget.Toast.makeText(context, "Please login to like posts", android.widget.Toast.LENGTH_SHORT).show()
                        return@launch
                    }
                    
                    // Toggle like
                    likeRepository.toggleLike(currentUserId, post.id, "post")
                        .onSuccess { isLiked ->
                            updateLikeIcon(isLiked)
                            
                            // Update like count
                            likeRepository.getLikeCount(post.id, "post")
                                .onSuccess { count ->
                                    likeCount.text = count.toString()
                                }
                        }
                        .onFailure { error ->
                            android.util.Log.e("PostAdapter", "Failed to toggle like", error)
                            android.widget.Toast.makeText(context, "Failed to like post: ${error.message}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                } catch (e: Exception) {
                    android.util.Log.e("PostAdapter", "Error handling like click", e)
                }
            }
        }
        
        private fun updateLikeIcon(isLiked: Boolean) {
            if (isLiked) {
                likeIcon.setImageResource(R.drawable.post_icons_1_2) // Filled heart
                likeIcon.setColorFilter(context.getColor(android.R.color.holo_red_light))
            } else {
                likeIcon.setImageResource(R.drawable.post_icons_1_1) // Outline heart
                likeIcon.clearColorFilter()
            }
        }
    }
}