package com.synapse.social.studioasinc;

import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.card.MaterialCardView;
import com.synapse.social.studioasinc.animations.ShimmerFrameLayout;

public class BaseMessageViewHolder extends RecyclerView.ViewHolder {
    LinearLayout body, message_layout, messageBG, my_message_info, mRepliedMessageLayoutLeftBar;
    CardView mProfileCard;
    ImageView mProfileImage, message_state;
    TextView date, message_text, mRepliedMessageLayoutUsername, mRepliedMessageLayoutMessage;
    TextView senderUsername; // Username TextView for group chats
    MaterialCardView mRepliedMessageLayout;
    ShimmerFrameLayout shimmer_container;
    // CRITICAL FIX: Add ImageView for reply image previews
    ImageView mRepliedMessageLayoutImage;

    public BaseMessageViewHolder(View view) {
        super(view);
        body = view.findViewById(R.id.body);
        message_layout = view.findViewById(R.id.message_layout);
        messageBG = view.findViewById(R.id.messageBG);
        my_message_info = view.findViewById(R.id.my_message_info);
        mProfileCard = view.findViewById(R.id.mProfileCard);
        mProfileImage = view.findViewById(R.id.mProfileImage);
        date = view.findViewById(R.id.date);
        message_state = view.findViewById(R.id.message_state);
        message_text = view.findViewById(R.id.message_text);
        senderUsername = view.findViewById(R.id.senderUsername); // Initialize username TextView
        shimmer_container = view.findViewById(R.id.shimmer_container);

        mRepliedMessageLayout = view.findViewById(R.id.mRepliedMessageLayout);
        if (mRepliedMessageLayout != null) {
            mRepliedMessageLayoutUsername = mRepliedMessageLayout.findViewById(R.id.mRepliedMessageLayoutUsername);
            mRepliedMessageLayoutMessage = mRepliedMessageLayout.findViewById(R.id.mRepliedMessageLayoutMessage);
            mRepliedMessageLayoutLeftBar = mRepliedMessageLayout.findViewById(R.id.mRepliedMessageLayoutLeftBar);
            // CRITICAL FIX: Initialize reply image view
            mRepliedMessageLayoutImage = mRepliedMessageLayout.findViewById(R.id.mRepliedMessageLayoutImage);
        }
    }

    public void startShimmer() {
        if (shimmer_container != null) {
            shimmer_container.startShimmer();
        }
    }

    public void stopShimmer() {
        if (shimmer_container != null) {
            shimmer_container.stopShimmer();
        }
    }
}