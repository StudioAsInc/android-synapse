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
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.synapse.social.studioasinc.config.CloudinaryConfig;
import com.synapse.social.studioasinc.model.Attachment;
import com.synapse.social.studioasinc.util.UIUtils;

import java.util.ArrayList;

public class MessageImageCarouselAdapter extends RecyclerView.Adapter<MessageImageCarouselAdapter.ImageCarouselViewHolder> {
    
    private static final String TAG = "MessageImageCarouselAdapter";
    
    private final Context context;
    private final ArrayList<Attachment> attachments;
    private final OnImageClickListener onImageClickListener;
    private final int imageSize;
    
    public interface OnImageClickListener {
        void onImageClick(int position, ArrayList<Attachment> attachments);
    }
    
    public MessageImageCarouselAdapter(Context context, ArrayList<Attachment> attachments, OnImageClickListener listener) {
        this.context = context;
        this.attachments = attachments;
        this.onImageClickListener = listener;
        this.imageSize = (int) context.getResources().getDimension(R.dimen.chat_carousel_image_size);
    }
    
    @NonNull
    @Override
    public ImageCarouselViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_message_carousel_image, parent, false);
        return new ImageCarouselViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImageCarouselViewHolder holder, int position) {
        if (attachments == null || position < 0 || position >= attachments.size()) {
            resetViewHolder(holder);
            return;
        }
        Attachment attachment = attachments.get(position);

        // Handle case where attachment is null to prevent crashes
        if (attachment == null) {
            resetViewHolder(holder);
            return;
        }

        String publicId = attachment.getPublicId();
        
        // Set consistent size for all carousel images
        ViewGroup.LayoutParams layoutParams = holder.cardView.getLayoutParams();
        layoutParams.width = imageSize;
        layoutParams.height = imageSize;
        holder.cardView.setLayoutParams(layoutParams);
        
        if (publicId != null && !publicId.isEmpty()) {
            // Use optimized image URL based on actual view size for better performance
            int densityDpi = context.getResources().getDisplayMetrics().densityDpi;
            String imageUrl = CloudinaryConfig.buildCarouselImageUrl(publicId, densityDpi);
            
            int cornerRadius = (int) context.getResources().getDimension(R.dimen.gallery_image_corner_radius);
            
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ph_imgbluredsqure)
                .error(R.drawable.ph_imgbluredsqure)
                .transform(new RoundedCorners(cornerRadius))
                .into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.ph_imgbluredsqure);
        }
        
        // Set click listener to open gallery
        holder.itemView.setOnClickListener(v -> {
            if (onImageClickListener != null && position >= 0 && position < attachments.size()) {
                onImageClickListener.onImageClick(position, attachments);
            }
        });
        
        // Add ripple effect with hover animation
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        holder.cardView.setForeground(context.getDrawable(outValue.resourceId));
        
        // Add subtle scale animation on touch
        int animationDuration = context.getResources().getInteger(R.integer.touch_feedback_duration);
        float scaleDown = 0.95f; // Scale down value
        float scaleNormal = 1.0f; // Normal scale value
        
        holder.itemView.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(scaleDown).scaleY(scaleDown).setDuration(animationDuration).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(scaleNormal).scaleY(scaleNormal).setDuration(animationDuration).start();
                    break;
            }
            return false; // Allow the click listener to handle the click
        });
    }
    
    @Override
    public int getItemCount() {
        return attachments != null ? attachments.size() : 0;
    }

    private void resetViewHolder(ImageCarouselViewHolder holder) {
        holder.imageView.setImageResource(R.drawable.ph_imgbluredsqure);
        holder.itemView.setOnClickListener(null);
        holder.itemView.setOnTouchListener(null);
    }
    
    static class ImageCarouselViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView imageView;
        
        public ImageCarouselViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.carouselImageCard);
            imageView = itemView.findViewById(R.id.carouselImageView);
        }
    }
}