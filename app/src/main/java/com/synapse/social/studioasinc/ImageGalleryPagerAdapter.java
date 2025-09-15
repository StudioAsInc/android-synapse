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

package com.synapse.social.studioasinc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.github.chrisbanes.photoview.PhotoView;
import com.synapse.social.studioasinc.model.Attachment;

import java.util.ArrayList;

public class ImageGalleryPagerAdapter extends RecyclerView.Adapter<ImageGalleryPagerAdapter.ImageViewHolder> {
    private final Context context;
    private final ArrayList<Attachment> attachments;
    
    public ImageGalleryPagerAdapter(Context context, ArrayList<Attachment> attachments) {
        this.context = context;
        this.attachments = attachments;
    }
    
    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_gallery_image, parent, false);
        return new ImageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Attachment attachment = attachments.get(position);
        String imageUrl = attachment.getUrl();
        
        if (imageUrl != null && !imageUrl.isEmpty()) {
            holder.progressBar.setVisibility(View.VISIBLE);
            
            int crossfadeDuration = context.getResources().getInteger(R.integer.crossfade_duration);
            
            Glide.with(context)
                .load(imageUrl)
                .transition(DrawableTransitionOptions.withCrossFade(crossfadeDuration))
                .listener(new com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(@androidx.annotation.Nullable com.bumptech.glide.load.engine.GlideException e, Object model, 
                                              com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        holder.photoView.setImageResource(R.drawable.ph_imgbluredsqure);
                        return false; // Allow Glide to handle the error drawable
                    }
                    
                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, Object model, 
                                                 com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable> target, 
                                                 com.bumptech.glide.load.DataSource dataSource, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        return false; // Allow Glide to set the image
                    }
                })
                .into(holder.photoView);
        } else {
            holder.progressBar.setVisibility(View.GONE);
            holder.photoView.setImageResource(R.drawable.ph_imgbluredsqure);
        }
    }
    
    @Override
    public int getItemCount() {
        return attachments != null ? attachments.size() : 0;
    }
    
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;
        ProgressBar progressBar;
        
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
            progressBar = itemView.findViewById(R.id.progressBar);
            
            // Configure PhotoView for zoom and pan
            photoView.setMediumScale(2.0f);
            photoView.setMaximumScale(4.0f);
            photoView.setMinimumScale(0.5f);
        }
    }
}