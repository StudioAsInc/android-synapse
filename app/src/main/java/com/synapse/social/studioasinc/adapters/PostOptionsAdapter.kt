package com.synapse.social.studioasinc.adapters

import android.content.Context
import android.content.res.ColorStateList
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.PostActionItem

class PostOptionsAdapter(
    private val items: List<PostActionItem>,
    private val onItemClick: () -> Unit
) : RecyclerView.Adapter<PostOptionsAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.icon)
        val label: TextView = view.findViewById(R.id.label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_post_option, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.label.text = item.label
        holder.icon.setImageResource(item.icon)

        val context = holder.itemView.context
        val color = if (item.isDestructive) {
            resolveColor(context, com.google.android.material.R.attr.colorError)
        } else {
            resolveColor(context, com.google.android.material.R.attr.colorOnSurface)
        }

        holder.label.setTextColor(color)
        ImageViewCompat.setImageTintList(holder.icon, ColorStateList.valueOf(color))

        holder.itemView.setOnClickListener {
            item.action.invoke()
            onItemClick.invoke()
        }
    }

    override fun getItemCount() = items.size

    @ColorInt
    private fun resolveColor(context: Context, @AttrRes attr: Int): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(attr, typedValue, true)
        return typedValue.data
    }
}
