package com.synapse.social.studioasinc.util.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.databinding.SynapsePostCvBinding
import com.synapse.social.studioasinc.model.Post
import com.synapse.social.studioasinc.model.User

/**
 * Adapter for the list of posts in the profile screen.
 */
class PostAdapter : ListAdapter<Post, PostAdapter.PostViewHolder>(PostDiffCallback()) {

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

        /**
         * Binds the post data to the views.
         */
        fun bind(post: Post) {
            binding.postMessageTextMiddle.text = post.postText

            // Load post image
            if (post.postImage != "null") {
                Glide.with(binding.root.context)
                    .load(Uri.parse(post.postImage))
                    .into(binding.postImage)
            }

            // Fetch and display user data
            val userRef = FirebaseDatabase.getInstance().getReference("skyline/users").child(post.uid)
            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val user = snapshot.getValue(User::class.java)
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
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
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
