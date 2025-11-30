package com.synapse.social.studioasinc.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.PostActionItem

class PostOptionsAdapter(
    private val items: List<PostActionItem>
) : RecyclerView.Adapter<PostOptionsAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.optionIcon)
        val label: TextView = view.findViewById(R.id.optionLabel)
        
        fun bind(item: PostActionItem) {
            icon.setImageResource(item.icon)
            label.text = item.label
            
            if (item.isDestructive) {
                val errorColor = ContextCompat.getColor(itemView.context, R.color.error_red)
                icon.setColorFilter(errorColor)
                label.setTextColor(errorColor)
            } else {
                icon.clearColorFilter()
                label.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_primary))
            }
            
            itemView.setOnClickListener { item.action() }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size
}
