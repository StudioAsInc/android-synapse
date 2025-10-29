/**
 * CONFIDENTIAL AND PROPRIETARY
 * 
 * This source code is the sole property of StudioAs Inc. Synapse. (Ashik).
 * Any reproduction, modification, distribution, or exploitation in any form
 * without explicit written permission from the owner is strictly prohibited.
 * 
 * Copyright (c) 2025 StudioAs Inc. Synapse. (Ashik)
 * All rights reserved.
 */

package com.synapse.social.studioasinc

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.github.chrisbanes.photoview.PhotoView
import com.synapse.social.studioasinc.model.Attachment

class ImageGalleryPagerAdapter(
    private val context: Context,
    private val attachments: List<Attachment>
) : RecyclerView.Adapter<ImageGalleryPagerAdapter.ImageViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_gallery_image, parent, false)
        return ImageViewHolder(view)
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val attachment = attachments[position]
        val imageUrl = attachment.url
        
        if (!imageUrl.isNullOrEmpty()) {
            holder.progressBar.visibility = View.VISIBLE
            
            val crossfadeDuration = context.resources.getInteger(R.integer.crossfade_duration)
            
            Glide.with(context)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade(crossfadeDuration))
                .listener(object : RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<android.graphics.drawable.Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.progressBar.visibility = View.GONE
                        holder.photoView.setImageResource(R.drawable.ph_imgbluredsqure)
                        return false // Allow Glide to handle the error drawable
                    }
                    
                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable,
                        model: Any,
                        target: Target<android.graphics.drawable.Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.progressBar.visibility = View.GONE
                        return false // Allow Glide to set the image
                    }
                })
                .into(holder.photoView)
        } else {
            holder.progressBar.visibility = View.GONE
            holder.photoView.setImageResource(R.drawable.ph_imgbluredsqure)
        }
    }
    
    override fun getItemCount(): Int = attachments.size
    
    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView.findViewById(R.id.photoView)
        val progressBar: ProgressBar = itemView.findViewById(R.id.progressBar)
        
        init {
            // Configure PhotoView for zoom and pan
            photoView.apply {
                mediumScale = 2.0f
                maximumScale = 4.0f
                minimumScale = 0.5f
            }
        }
    }
}
