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

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.gridlayout.widget.GridLayout;
import androidx.recyclerview.widget.RecyclerView;
import com.airbnb.lottie.LottieAnimationView;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.button.MaterialButton;

import com.synapse.social.studioasinc.animations.ShimmerFrameLayout;


class TextViewHolder extends BaseMessageViewHolder { public TextViewHolder(View view) { super(view); } }

class MediaViewHolder extends BaseMessageViewHolder {
    GridLayout mediaGridLayout;
    CardView mediaContainerCard;
    LinearLayout mediaCarouselContainer;
    RecyclerView mediaCarouselRecyclerView;
    MaterialButton viewAllImagesButton;
    
    public MediaViewHolder(View view) {
        super(view);
        mediaGridLayout = view.findViewById(R.id.mediaGridLayout);
        mediaContainerCard = view.findViewById(R.id.mediaContainerCard);
        
        // These might be null if layout doesn't contain them, handle gracefully
        try {
            mediaCarouselContainer = view.findViewById(R.id.mediaCarouselContainer);
            mediaCarouselRecyclerView = view.findViewById(R.id.mediaCarouselRecyclerView);
            viewAllImagesButton = view.findViewById(R.id.viewAllImagesButton);
        } catch (Exception e) {
            // Layout might not contain these newer elements
            mediaCarouselContainer = null;
            mediaCarouselRecyclerView = null;
            viewAllImagesButton = null;
        }
    }
}

class VideoViewHolder extends BaseMessageViewHolder {
    ImageView videoThumbnail, playButton;
    MaterialCardView videoContainerCard;
    public VideoViewHolder(View view) {
        super(view);
        videoThumbnail = view.findViewById(R.id.videoThumbnail);
        playButton = view.findViewById(R.id.playButton);
        videoContainerCard = view.findViewById(R.id.videoContainerCard); // CRITICAL FIX: Initialized here
    }
}

class TypingViewHolder extends RecyclerView.ViewHolder {
    LottieAnimationView lottie_typing;
    CardView mProfileCard;
    ImageView mProfileImage;
    LinearLayout messageBG;
    public TypingViewHolder(View view) {
        super(view);
        lottie_typing = view.findViewById(R.id.lottie_typing);
        mProfileCard = view.findViewById(R.id.mProfileCard);
        mProfileImage = view.findViewById(R.id.mProfileImage);
        messageBG = view.findViewById(R.id.messageBG);
    }
}

class LinkPreviewViewHolder extends BaseMessageViewHolder {
    MaterialCardView linkPreviewContainer;
    ImageView linkPreviewImage;
    TextView linkPreviewTitle, linkPreviewDescription, linkPreviewDomain;

    public LinkPreviewViewHolder(View view) {
        super(view);
        linkPreviewContainer = view.findViewById(R.id.linkPreviewContainer);
        linkPreviewImage = view.findViewById(R.id.linkPreviewImage);
        linkPreviewTitle = view.findViewById(R.id.linkPreviewTitle);
        linkPreviewDescription = view.findViewById(R.id.linkPreviewDescription);
        linkPreviewDomain = view.findViewById(R.id.linkPreviewDomain);
    }
}

class LoadingViewHolder extends RecyclerView.ViewHolder {
    ProgressBar loadingMoreProgressBar;
    public LoadingViewHolder(View view) {
        super(view);
        loadingMoreProgressBar = view.findViewById(R.id.loadingMoreProgressBar);
    }
}

class VoiceMessageViewHolder extends BaseMessageViewHolder {
    ImageView playPauseButton;
    SeekBar seekBar;
    TextView duration;
    MediaPlayer mediaPlayer;
    Handler handler = new Handler(Looper.getMainLooper());

    public VoiceMessageViewHolder(View itemView) {
        super(itemView);
        playPauseButton = itemView.findViewById(R.id.play_pause_button);
        seekBar = itemView.findViewById(R.id.voice_seekbar);
        duration = itemView.findViewById(R.id.voice_duration);
        mediaPlayer = new MediaPlayer();
    }
}