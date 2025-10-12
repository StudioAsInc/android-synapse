package com.synapse.social.studioasinc.util.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.databinding.SynapsePostCvBinding
import com.synapse.social.studioasinc.model.Post

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
            // Bind post data to the views in synapse_post_cv.xml
            binding.postMessageTextMiddle.text = post.post_text
            // ... bind other views
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
