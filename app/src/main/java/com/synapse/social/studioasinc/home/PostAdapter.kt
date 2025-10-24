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
import com.synapse.social.studioasinc.model.Post
import kotlinx.coroutines.launch

class PostAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val authRepository: AuthRepository = AuthRepository(),
    private val postRepository: PostRepository = PostRepository(),
    private val userRepository: UserRepository = UserRepository()
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
        }
    }
}