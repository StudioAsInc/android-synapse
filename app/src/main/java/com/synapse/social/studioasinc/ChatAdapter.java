package com.synapse.social.studioasinc;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
// import com.synapse.social.studioasinc.styling.TextStylingUtil;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import androidx.gridlayout.widget.GridLayout;
import android.widget.RelativeLayout;
import com.google.firebase.database.GenericTypeIndicator;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.content.Intent;
import android.app.Activity;
import android.view.Gravity;
import com.synapse.social.studioasinc.config.CloudinaryConfig;
import com.synapse.social.studioasinc.model.Attachment;
import com.synapse.social.studioasinc.crypto.E2EEHelper;
import com.synapse.social.studioasinc.util.AttachmentUtils;
import com.synapse.social.studioasinc.R;
import com.synapse.social.studioasinc.util.UIUtils;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatAdapter";
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_MEDIA_GRID = 2;
    private static final int VIEW_TYPE_TYPING = 3;
    private static final int VIEW_TYPE_VIDEO = 4;
    private static final int VIEW_TYPE_LINK_PREVIEW = 5;
    private static final int VIEW_TYPE_LOADING_MORE = 99;

    private ArrayList<HashMap<String, Object>> _data;
    private HashMap<String, HashMap<String, Object>> repliedMessagesCache;
    private Context _context;
    private String secondUserAvatarUrl = "";
    private String firstUserName = "";
    private String secondUserName = "";
    private SharedPreferences appSettings;
    // private TextStylingUtil textStylingUtil;
    private ChatActivity chatActivity;
    private E2EEHelper e2eeHelper;

    public ChatAdapter(ArrayList<HashMap<String, Object>> _arr, HashMap<String, HashMap<String, Object>> repliedCache) {
        _data = _arr;
        this.repliedMessagesCache = repliedCache;
    }
    public void setChatActivity(ChatActivity activity) { this.chatActivity = activity; }
    public void setE2EEHelper(E2EEHelper helper) { this.e2eeHelper = helper; }
    public void setSecondUserAvatar(String url) { this.secondUserAvatarUrl = url; }
    public void setFirstUserName(String name) { this.firstUserName = name; }
    public void setSecondUserName(String name) { this.secondUserName = name; }
    private String secondUserUid;
    public void setSecondUserUid(String uid) { this.secondUserUid = uid; }

    @Override
    public int getItemViewType(int position) {
        if (_data.get(position).containsKey("isLoadingMore")) return VIEW_TYPE_LOADING_MORE;
        if (_data.get(position).containsKey("typingMessageStatus")) return VIEW_TYPE_TYPING;

        String type = _data.get(position).getOrDefault("TYPE", "MESSAGE").toString();
        
        if ("ATTACHMENT_MESSAGE".equals(type)) {
            ArrayList<HashMap<String, Object>> attachments = getAttachmentsFromMessage(_data.get(position));
            if (attachments != null && attachments.size() == 1 && String.valueOf(attachments.get(0).getOrDefault("publicId", "")).contains("|video")) {
                return VIEW_TYPE_VIDEO;
            }
            return VIEW_TYPE_MEDIA_GRID;
        }

        if ("LINK_PREVIEW_MESSAGE".equals(type)) {
            return VIEW_TYPE_LINK_PREVIEW;
        }
        
        return VIEW_TYPE_TEXT;
    }

    @Override
    public long getItemId(int position) {
        try {
            // CRITICAL FIX: Use the correct key constant that matches ChatActivity
            Object keyObj = _data.get(position).getOrDefault("key", _data.get(position).getOrDefault("KEY_KEY", position));
            return String.valueOf(keyObj).hashCode();
        } catch (Exception e) {
            return position;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        _context = parent.getContext();
        appSettings = _context.getSharedPreferences("appSettings", Context.MODE_PRIVATE);
        // textStylingUtil = new TextStylingUtil(_context);
        LayoutInflater inflater = LayoutInflater.from(_context);
        switch (viewType) {
            case VIEW_TYPE_MEDIA_GRID: return new MediaViewHolder(inflater.inflate(R.layout.chat_bubble_media, parent, false));
            case VIEW_TYPE_VIDEO: return new VideoViewHolder(inflater.inflate(R.layout.chat_bubble_video, parent, false));
            case VIEW_TYPE_TYPING: return new TypingViewHolder(inflater.inflate(R.layout.chat_bubble_typing, parent, false));
            case VIEW_TYPE_LINK_PREVIEW: return new LinkPreviewViewHolder(inflater.inflate(R.layout.chat_bubble_link_preview, parent, false));
            case VIEW_TYPE_LOADING_MORE: return new LoadingViewHolder(inflater.inflate(R.layout.chat_bubble_loading_more, parent, false));
            default: return new TextViewHolder(inflater.inflate(R.layout.chat_bubble_text, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case VIEW_TYPE_TEXT: bindTextViewHolder((TextViewHolder) holder, position); break;
            case VIEW_TYPE_MEDIA_GRID: bindMediaViewHolder((MediaViewHolder) holder, position); break;
            case VIEW_TYPE_VIDEO: bindVideoViewHolder((VideoViewHolder) holder, position); break;
            case VIEW_TYPE_TYPING: bindTypingViewHolder((TypingViewHolder) holder, position); break;
            case VIEW_TYPE_LINK_PREVIEW: bindLinkPreviewViewHolder((LinkPreviewViewHolder) holder, position); break;
            case VIEW_TYPE_LOADING_MORE: bindLoadingViewHolder((LoadingViewHolder) holder, position); break;
        }
    }

    @Override
    public int getItemCount() { return _data.size(); }

    private void bindCommonMessageProperties(BaseMessageViewHolder holder, int position) {
        HashMap<String, Object> data = _data.get(position);
        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        String msgUid = data != null && data.get("uid") != null ? String.valueOf(data.get("uid")) : "";
        boolean isMyMessage = msgUid.equals(myUid);
        
        if (holder.message_layout != null) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) holder.message_layout.getLayoutParams();
            int sideMarginPx = (int) _context.getResources().getDimension(R.dimen.chat_side_margin);
            int topBottomPaddingPx = (int) _context.getResources().getDimension(R.dimen.chat_padding_vertical);
            int innerPaddingPx = (int) _context.getResources().getDimension(R.dimen.chat_padding_inner);

            if (isMyMessage) {
                params.setMargins(sideMarginPx, topBottomPaddingPx, innerPaddingPx, topBottomPaddingPx);
                holder.body.setGravity(Gravity.TOP | Gravity.RIGHT);
                holder.message_layout.setGravity(Gravity.RIGHT);
            } else {
                params.setMargins(innerPaddingPx, topBottomPaddingPx, sideMarginPx, topBottomPaddingPx);
                holder.body.setGravity(Gravity.TOP | Gravity.LEFT);
                holder.message_layout.setGravity(Gravity.LEFT);
            }
            holder.message_layout.setLayoutParams(params);
        }
        holder.body.setGravity(isMyMessage ? (Gravity.TOP | Gravity.RIGHT) : (Gravity.TOP | Gravity.LEFT));
        
        if (holder.my_message_info != null && holder.date != null && holder.message_state != null) {
            boolean showMessageInfo = false;
            if (position == _data.size() - 1) {
                showMessageInfo = true;
            } else if (position + 1 < _data.size()) {
                HashMap<String, Object> currentMsg = _data.get(position);
                HashMap<String, Object> nextMsg = _data.get(position + 1);

                String nextUid = nextMsg != null && nextMsg.get("uid") != null ? String.valueOf(nextMsg.get("uid")) : "";
                String currUid = currentMsg != null && currentMsg.get("uid") != null ? String.valueOf(currentMsg.get("uid")) : "";
                boolean nextIsDifferentUser = !nextUid.equals(currUid);
                boolean nextIsTyping = nextMsg.containsKey("typingMessageStatus");

                boolean timeIsSignificant = false;
                if (currentMsg.containsKey("push_date") && nextMsg.containsKey("push_date") && currentMsg.get("push_date") != null && nextMsg.get("push_date") != null) {
                    try {
                        long currentTime = (long) Double.parseDouble(String.valueOf(currentMsg.get("push_date")));
                        long nextTime = (long) Double.parseDouble(String.valueOf(nextMsg.get("push_date")));
                        if ((nextTime - currentTime) > (5 * 60 * 1000)) { // 5 minutes
                            timeIsSignificant = true;
                        }
                    } catch (NumberFormatException e) {
                        // Handle case where push_date is not a valid double
                        timeIsSignificant = false;
                    }
                }

                if (nextIsDifferentUser || nextIsTyping || timeIsSignificant) {
                    showMessageInfo = true;
                }
            }
            holder.my_message_info.setVisibility(showMessageInfo ? View.VISIBLE : View.GONE);
            
            if (showMessageInfo) {
                Calendar push = Calendar.getInstance();
                try {
                    double pushVal = Double.parseDouble(String.valueOf(data.get("push_date")));
                    push.setTimeInMillis((long) pushVal);
                } catch (Exception e) {
                    push.setTimeInMillis(System.currentTimeMillis());
                }
                // CRITICAL FIX: Smart timestamp visibility logic
                boolean shouldShowTime = _shouldShowTimestamp(position, data);
                holder.date.setVisibility(shouldShowTime ? View.VISIBLE : View.GONE);
                if (shouldShowTime) {
                    holder.date.setText(new SimpleDateFormat("hh:mm a").format(push.getTime()));
                }
                
                holder.message_state.setVisibility(isMyMessage ? View.VISIBLE : View.GONE);
                if (isMyMessage) {
                    String state = data.get("message_state") != null ? String.valueOf(data.get("message_state")) : "";
                    holder.message_state.setImageResource("seen".equals(state) ? R.drawable.icon_done_all_round : R.drawable.icon_done_round);
                    holder.message_state.setColorFilter("seen".equals(state) ? com.synapse.social.studioasinc.util.ThemeUtils.getThemeColor(_context, com.google.android.material.R.attr.colorPrimaryContainer) : 0xFF424242, PorterDuff.Mode.SRC_ATOP);
                }
            }
        }

        if (holder.mProfileCard != null && holder.mProfileImage != null) {
            if (!isMyMessage) {
                String currUid = data != null && data.get("uid") != null ? String.valueOf(data.get("uid")) : "";
                String prevUid = (position - 1 >= 0 && _data.get(position - 1) != null && _data.get(position - 1).get("uid") != null) ? String.valueOf(_data.get(position - 1).get("uid")) : "";
                boolean isFirstOfGroup = (position == 0) || (position - 1 >= 0 && !prevUid.equals(currUid));
                holder.mProfileCard.setVisibility(isFirstOfGroup ? View.VISIBLE : View.GONE);
                if (isFirstOfGroup) {
                    if (secondUserAvatarUrl != null && !secondUserAvatarUrl.isEmpty() && !secondUserAvatarUrl.equals("null_banned")) {
                         Glide.with(_context).load(Uri.parse(secondUserAvatarUrl)).into(holder.mProfileImage);
                    } else if ("null_banned".equals(secondUserAvatarUrl)) {
                         holder.mProfileImage.setImageResource(R.drawable.banned_avatar);
                    } else {
                         holder.mProfileImage.setImageResource(R.drawable.avatar);
                    }
                }
            } else {
                holder.mProfileCard.setVisibility(View.GONE);
            }
        }
        
        if (holder.mRepliedMessageLayout != null) {
            holder.mRepliedMessageLayout.setVisibility(View.GONE);
            Log.d(TAG, "Checking for reply data at position " + position + " - Reply layout holder exists");
            
            if (data.containsKey("replied_message_id")) {
                String repliedId = data.get("replied_message_id").toString();
                Log.d(TAG, "Found replied_message_id: " + repliedId + " for position: " + position);
                
                if (repliedId != null && !repliedId.isEmpty() && !repliedId.equals("null")) {
                    Log.d(TAG, "Processing reply for message ID: " + repliedId);
                    
                    if (repliedMessagesCache != null && repliedMessagesCache.containsKey(repliedId)) {
                        Log.d(TAG, "Reply cache HIT for ID: " + repliedId);
                        HashMap<String, Object> snapshot = repliedMessagesCache.get(repliedId);
                        if (snapshot != null && !snapshot.isEmpty()) {
                            Log.d(TAG, "Reply snapshot is valid, showing layout for ID: " + repliedId);
                            holder.mRepliedMessageLayout.setVisibility(View.VISIBLE);

                            if (isMyMessage) {
                                holder.mRepliedMessageLayout.setCardBackgroundColor(Color.parseColor("#80FFFFFF"));
                            } else {
                                holder.mRepliedMessageLayout.setCardBackgroundColor(Color.parseColor("#F5F5F5"));
                            }

                            String repliedUid = snapshot.get("uid") instanceof String ? (String) snapshot.get("uid") : null;

                            String repliedText = "";
                            boolean isRepliedMsgEncrypted = Boolean.TRUE.equals(snapshot.get("isEncrypted"));
                            String originalRepliedText = (String) snapshot.get("message_text");

                            if (isRepliedMsgEncrypted) {
                                try {
                                    repliedText = e2eeHelper.decrypt(secondUserUid, originalRepliedText);
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to decrypt replied message text", e);
                                    repliedText = "⚠️ This message could not be decrypted.";
                                }
                            } else {
                                repliedText = originalRepliedText;
                            }

                            if (holder.mRepliedMessageLayoutImage != null) {
                                if (snapshot.containsKey(ChatActivity.ATTACHMENTS_KEY)) {
                                    ArrayList<HashMap<String, Object>> attachments = getDecryptedAttachments(snapshot);
                                    if (attachments != null && !attachments.isEmpty()) {
                                        holder.mRepliedMessageLayoutImage.setVisibility(View.VISIBLE);
                                        String publicId = (String) attachments.get(0).get("publicId");
                                        if (publicId != null && !publicId.isEmpty() && _context != null) {
                                            String imageUrl = CloudinaryConfig.buildReplyPreviewUrl(publicId);
                                            int cornerRadius = (int) _context.getResources().getDimension(R.dimen.reply_preview_corner_radius);
                                            Glide.with(_context).load(imageUrl).placeholder(R.drawable.ph_imgbluredsqure).error(R.drawable.ph_imgbluredsqure).transform(new RoundedCorners(cornerRadius)).into(holder.mRepliedMessageLayoutImage);
                                        } else {
                                            holder.mRepliedMessageLayoutImage.setImageResource(R.drawable.ph_imgbluredsqure);
                                        }
                                    } else {
                                        holder.mRepliedMessageLayoutImage.setVisibility(View.GONE);
                                    }
                                } else {
                                    holder.mRepliedMessageLayoutImage.setVisibility(View.GONE);
                                }
                            }

                            if (holder.mRepliedMessageLayoutUsername != null) {
                                String username = repliedUid != null && repliedUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid()) ? firstUserName : secondUserName;
                                holder.mRepliedMessageLayoutUsername.setText(username);
                                holder.mRepliedMessageLayoutUsername.setTextColor(isMyMessage ? Color.WHITE : Color.parseColor("#1A1A1A"));
                            }

                            if (holder.mRepliedMessageLayoutMessage != null) {
                                String messageText = repliedText != null ? repliedText : "";
                                holder.mRepliedMessageLayoutMessage.setText(messageText);
                                holder.mRepliedMessageLayoutMessage.setTextColor(isMyMessage ? Color.parseColor("#E0E0E0") : Color.parseColor("#424242"));
                                holder.mRepliedMessageLayoutMessage.setVisibility(messageText.isEmpty() ? View.GONE : View.VISIBLE);
                            }

                            if (holder.mRepliedMessageLayoutLeftBar != null) {
                                android.graphics.drawable.GradientDrawable leftBarDrawable = new android.graphics.drawable.GradientDrawable();
                                leftBarDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                                leftBarDrawable.setColor(com.synapse.social.studioasinc.util.ThemeUtils.getThemeColor(_context, com.google.android.material.R.attr.colorPrimaryContainer));
                                int leftBarRadius = (int) _context.getResources().getDimension(R.dimen.left_bar_corner_radius);
                                leftBarDrawable.setCornerRadius(leftBarRadius);
                                holder.mRepliedMessageLayoutLeftBar.setBackground(leftBarDrawable);
                            }

                            View.OnClickListener clickListener = v -> {
                                if (chatActivity != null) {
                                    chatActivity.scrollToMessage(repliedId);
                                }
                            };
                            holder.mRepliedMessageLayout.setOnClickListener(clickListener);
                            if (holder.mRepliedMessageLayoutMessage != null) holder.mRepliedMessageLayoutMessage.setOnClickListener(clickListener);
                            if (holder.mRepliedMessageLayoutImage != null) holder.mRepliedMessageLayoutImage.setOnClickListener(clickListener);
                        } else {
                            // Show a lightweight loading state while the replied message loads
                            holder.mRepliedMessageLayout.setVisibility(View.VISIBLE);

                            if (holder.mRepliedMessageLayoutImage != null) {
                                holder.mRepliedMessageLayoutImage.setVisibility(View.GONE);
                            }
                            if (holder.mRepliedMessageLayoutUsername != null) {
                                holder.mRepliedMessageLayoutUsername.setText("Loading…");
                                holder.mRepliedMessageLayoutUsername.setTextColor(isMyMessage ? Color.WHITE : Color.parseColor("#1A1A1A"));
                            }
                            if (holder.mRepliedMessageLayoutMessage != null) {
                                holder.mRepliedMessageLayoutMessage.setText("");
                                holder.mRepliedMessageLayoutMessage.setVisibility(View.GONE);
                            }
                        }
                    } else {
                        holder.mRepliedMessageLayout.setVisibility(View.GONE);
                    }
                } else {
                    Log.d(TAG, "Reply message ID is null or empty for position: " + position);
                }
            } else {
                Log.d(TAG, "No replied_message_id found for position: " + position + ". Available keys: " + data.keySet());
            }
        } else {
            Log.w(TAG, "Reply layout holder is null for position: " + position);
        }
        
        if (holder.messageBG != null) {
            int cornerRadius = 27;
            try { cornerRadius = (int) Double.parseDouble(appSettings.getString("ChatCornerRadius", "27")); } catch (Exception e) {}
            
            android.graphics.drawable.GradientDrawable bubbleDrawable = new android.graphics.drawable.GradientDrawable();
            float density = _context.getResources().getDisplayMetrics().density;
            bubbleDrawable.setCornerRadius(density * cornerRadius);

            if (isMyMessage) {
                bubbleDrawable.setColor(0xFF6B4CFF);
                if(holder.message_text != null) holder.message_text.setTextColor(Color.WHITE);
            } else {
                bubbleDrawable.setColor(Color.WHITE);
                bubbleDrawable.setStroke((int)(2 * density), 0xFFDFDFDF);
                if(holder.message_text != null) holder.message_text.setTextColor(Color.BLACK);
            }
            holder.messageBG.setBackground(bubbleDrawable);

            // Rounded ripple foreground to match bubble corners
            int rippleColor = isMyMessage ? 0x33FFFFFF : 0x22000000;
            android.graphics.drawable.GradientDrawable rippleMask = new android.graphics.drawable.GradientDrawable();
            rippleMask.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            rippleMask.setCornerRadius(density * cornerRadius);
            rippleMask.setColor(Color.WHITE);
            android.content.res.ColorStateList rippleColors = new android.content.res.ColorStateList(new int[][]{ new int[]{} }, new int[]{ rippleColor });
            android.graphics.drawable.RippleDrawable ripple = new android.graphics.drawable.RippleDrawable(rippleColors, null, rippleMask);
            holder.messageBG.setForeground(ripple);
            holder.messageBG.setClickable(true);
            holder.messageBG.setLongClickable(true);
        }

        int textSize = 16;
        try { textSize = (int) Double.parseDouble(appSettings.getString("ChatTextSize", "16")); } catch (Exception e) {}
        
        if(holder.message_text != null) holder.message_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        if (holder.mRepliedMessageLayoutUsername != null) holder.mRepliedMessageLayoutUsername.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 2);
        if (holder.mRepliedMessageLayoutMessage != null) holder.mRepliedMessageLayoutMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

        if (!isMyMessage && data.containsKey("message_state") && "sended".equals(String.valueOf(data.get("message_state")))) {
            String otherUserUid = chatActivity.getIntent().getStringExtra("uid");
            
            String messageKey = String.valueOf(data.get("key"));
            FirebaseDatabase.getInstance().getReference("skyline/chats").child(otherUserUid).child(myUid).child(messageKey).child("message_state").setValue("seen");
            FirebaseDatabase.getInstance().getReference("skyline/chats").child(myUid).child(otherUserUid).child(messageKey).child("message_state").setValue("seen");
            FirebaseDatabase.getInstance().getReference("skyline/inbox").child(otherUserUid).child(myUid).child("last_message_state").setValue("seen");
        }
        
        // Consolidated long click listener for the message context menu.
        View.OnLongClickListener longClickListener = v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) {
                return false;
            }
            Log.d(TAG, "Long click detected on view: " + v.getClass().getSimpleName() + " at position: " + currentPosition);
            if (chatActivity != null) {
                chatActivity.performHapticFeedbackLight();
            }
            // Use the message bubble (messageBG) as the anchor for the popup if it exists.
            View anchor = holder.messageBG != null ? holder.messageBG : holder.itemView;
            chatActivity._messageOverviewPopup(anchor, currentPosition, _data);
            return true;
        };

        // Set the listener on the message bubble itself.
        if (holder.messageBG != null) {
            holder.messageBG.setOnLongClickListener(longClickListener);
        }

        // Also set it on the TextView as a fallback, as it may consume touch events
        // due to the Markdown/link handling.
        if (holder.message_text != null) {
            holder.message_text.setOnLongClickListener(longClickListener);
        }
        
        // Keep only one definitive long click handler to avoid conflicts
    }

    private void bindTextViewHolder(TextViewHolder holder, int position) {
        bindCommonMessageProperties(holder, position);
        HashMap<String, Object> messageData = _data.get(position);
        boolean isEncrypted = Boolean.TRUE.equals(messageData.get("isEncrypted"));
        String messageContent = String.valueOf(messageData.getOrDefault("message_text", ""));

        if (isEncrypted) {
            try {
                String decryptedText = e2eeHelper.decrypt(secondUserUid, messageContent);
                holder.message_text.setText(decryptedText);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt message", e);
                holder.message_text.setText("⚠️ Could not decrypt message");
            }
        } else {
            holder.message_text.setText(messageContent);
        }

        holder.message_text.setVisibility(View.VISIBLE);
        // The original markdown rendering can be applied here if needed, but for now, we'll just show the text.
        // com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, holder.message_text.getText().toString());
    }

    private void bindMediaViewHolder(MediaViewHolder holder, int position) {
        Log.d(TAG, "bindMediaViewHolder called for position " + position);
        bindCommonMessageProperties(holder, position);
        HashMap<String, Object> data = _data.get(position);
        String msgText = String.valueOf(data.getOrDefault("message_text", ""));
        boolean isEncrypted = Boolean.TRUE.equals(data.get("isEncrypted"));

        if (isEncrypted) {
            try {
                msgText = e2eeHelper.decrypt(secondUserUid, msgText);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt message text in media view holder", e);
                msgText = "⚠️ Could not decrypt message";
            }
        }
        
        // Ensure message text is always visible and has content if available
        if (holder.message_text != null) {
            holder.message_text.setVisibility(msgText.isEmpty() ? View.GONE : View.VISIBLE);
            if (!msgText.isEmpty()) {
                com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, msgText);
            }
        }

        ArrayList<HashMap<String, Object>> attachments = getDecryptedAttachments(data);
        
        // CRITICAL FIX: Always ensure at least one layout is visible
        if (attachments == null || attachments.isEmpty()) {
            Log.w(TAG, "No attachments found, showing only message text");
            if (holder.mediaGridLayout != null) holder.mediaGridLayout.setVisibility(View.GONE);
            if (holder.mediaCarouselContainer != null) holder.mediaCarouselContainer.setVisibility(View.GONE);
            
            // If no message text either, show a minimal placeholder to prevent thin line
            if (msgText.isEmpty() && holder.message_text != null) {
                holder.message_text.setVisibility(View.VISIBLE);
                holder.message_text.setText("Media message");
            }
            return;
        }

        int count = attachments.size();
        Log.d(TAG, "Processing " + count + " attachments");

        // TEMPORARY FIX: Always use grid layout for now to ensure messages show properly
        // TODO: Re-enable carousel after debugging
        boolean useCarousel = false; // count >= 3;
        
        Log.d(TAG, "useCarousel: " + useCarousel + ", count: " + count);
        
        if (useCarousel && holder.mediaCarouselContainer != null && holder.mediaCarouselRecyclerView != null) {
            // Hide grid layout and show carousel
            Log.d(TAG, "Using carousel layout");
            if (holder.mediaGridLayout != null) holder.mediaGridLayout.setVisibility(View.GONE);
            holder.mediaCarouselContainer.setVisibility(View.VISIBLE);
            
            try {
                setupCarouselLayout(holder, attachments);
            } catch (Exception e) {
                Log.e(TAG, "Error setting up carousel, falling back to grid: " + e.getMessage());
                // Fallback to grid layout if carousel fails
                if (holder.mediaCarouselContainer != null) holder.mediaCarouselContainer.setVisibility(View.GONE);
                if (holder.mediaGridLayout != null) {
                    holder.mediaGridLayout.setVisibility(View.VISIBLE);
                    setupGridLayout(holder, attachments);
                }
            }
        } else {
            // Use traditional grid layout
            Log.d(TAG, "Using grid layout");
            if (holder.mediaCarouselContainer != null) holder.mediaCarouselContainer.setVisibility(View.GONE);
            if (holder.mediaGridLayout != null) {
                holder.mediaGridLayout.setVisibility(View.VISIBLE);
                try {
                    setupGridLayout(holder, attachments);
                    Log.d(TAG, "Grid layout setup completed successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Error setting up grid layout: " + e.getMessage());
                    // Fallback: show message text instead
                    if (holder.message_text != null) {
                        holder.message_text.setVisibility(View.VISIBLE);
                        holder.message_text.setText("Media message (" + attachments.size() + " images)");
                    }
                }
            } else {
                Log.e(TAG, "mediaGridLayout is null!");
                // Fallback: show message text instead
                if (holder.message_text != null) {
                    holder.message_text.setVisibility(View.VISIBLE);
                    holder.message_text.setText("Media message (" + attachments.size() + " images)");
                }
            }
        }
    }
    
    private void setupCarouselLayout(MediaViewHolder holder, ArrayList<HashMap<String, Object>> attachments) {
        Log.d(TAG, "Setting up carousel layout for " + attachments.size() + " images");
        
        // Setup horizontal RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(_context, LinearLayoutManager.HORIZONTAL, false);
        holder.mediaCarouselRecyclerView.setLayoutManager(layoutManager);
        
        // Add item decoration for proper spacing
        if (holder.mediaCarouselRecyclerView.getItemDecorationCount() == 0) {
            holder.mediaCarouselRecyclerView.addItemDecoration(
                CarouselItemDecoration.createWithStandardSpacing(holder.mediaCarouselRecyclerView));
        }
        
        // Convert to typed attachments once and create adapter with click listener to open gallery
        ArrayList<Attachment> typedAttachments = AttachmentUtils.fromHashMapList(attachments);
        if (typedAttachments == null || typedAttachments.isEmpty()) {
            return;
        }
        MessageImageCarouselAdapter adapter = new MessageImageCarouselAdapter(_context, typedAttachments, 
            (position, attachmentList) -> openImageGalleryTyped(attachmentList, position));
        holder.mediaCarouselRecyclerView.setAdapter(adapter);
        
        // Setup "View All" button - shows when there are more than 3 images
        // This provides easy access to the full-screen gallery experience
        if (holder.viewAllImagesButton != null) {
            if (attachments.size() > 3) {
                holder.viewAllImagesButton.setVisibility(View.VISIBLE);
                holder.viewAllImagesButton.setText("View all " + attachments.size() + " images");
                holder.viewAllImagesButton.setOnClickListener(v -> openImageGalleryTyped(typedAttachments, 0));
            } else {
                holder.viewAllImagesButton.setVisibility(View.GONE);
            }
        }
        
        // Set optimal card width for carousel
        ViewGroup.LayoutParams cardParams = holder.mediaContainerCard.getLayoutParams();
        cardParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        holder.mediaContainerCard.setLayoutParams(cardParams);
    }
    
    private void setupGridLayout(MediaViewHolder holder, ArrayList<HashMap<String, Object>> attachments) {
        Log.d(TAG, "Setting up grid layout for " + attachments.size() + " images");
        
        GridLayout gridLayout = holder.mediaGridLayout;
        gridLayout.removeAllViews();
        
        int count = attachments.size();
        int colCount = 2;
        int maxImages = 4;
        int totalGridWidth = (int) _context.getResources().getDimension(R.dimen.chat_grid_width);
        int imageSize = totalGridWidth / 2;

        ViewGroup.LayoutParams cardParams = holder.mediaContainerCard.getLayoutParams();
        cardParams.width = totalGridWidth;
        holder.mediaContainerCard.setLayoutParams(cardParams);

        gridLayout.setColumnCount(colCount);
        
        // Ensure grid layout has minimum dimensions to prevent thin line
        ViewGroup.LayoutParams gridParams = gridLayout.getLayoutParams();
        if (gridParams == null) {
            gridParams = new ViewGroup.LayoutParams(totalGridWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        gridParams.width = totalGridWidth;
        gridLayout.setLayoutParams(gridParams);

        if (count == 1) {
            gridLayout.setColumnCount(1);
            HashMap<String, Object> attachment = attachments.get(0);
            ImageView iv = createImageView(attachment, totalGridWidth, true, 0, attachments);
            gridLayout.addView(iv);

        } else if (count == 3) {
            int portraitIndex = -1;
            for(int i=0; i < attachments.size(); i++){
                HashMap<String, Object> attachment = attachments.get(i);
                Object widthObj = attachment.get("width");
                Object heightObj = attachment.get("height");

                if (widthObj != null && heightObj != null) {
                    double width = ((Number) widthObj).doubleValue();
                    double height = ((Number) heightObj).doubleValue();
                    if(height > width){
                        portraitIndex = i;
                        break;
                    }
                }
            }

            if(portraitIndex != -1){
                // Tall layout
                ImageView iv1 = createImageView(attachments.get(portraitIndex), imageSize, false, portraitIndex, attachments);
                GridLayout.LayoutParams params1 = new GridLayout.LayoutParams(GridLayout.spec(0, 2, 1f), GridLayout.spec(0, 1, 1f));
                iv1.setLayoutParams(params1);
                gridLayout.addView(iv1);

                int attachmentIndex = 0;
                for(int i=0; i<2; i++){
                    if(attachmentIndex == portraitIndex) attachmentIndex++;
                    ImageView iv = createImageView(attachments.get(attachmentIndex), imageSize, false, attachmentIndex, attachments);
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(i, 1, 1f), GridLayout.spec(1, 1, 1f));
                    iv.setLayoutParams(params);
                    gridLayout.addView(iv);
                    attachmentIndex++;
                }

            } else {
                // Wide layout
                ImageView iv1 = createImageView(attachments.get(0), totalGridWidth, false, 0, attachments);
                GridLayout.LayoutParams params1 = new GridLayout.LayoutParams(GridLayout.spec(0, 1, 1f), GridLayout.spec(0, 2, 1f));
                iv1.setLayoutParams(params1);
                gridLayout.addView(iv1);

                for (int i = 1; i < 3; i++) {
                    ImageView iv = createImageView(attachments.get(i), imageSize, false, i, attachments);
                    GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(1, 1, 1f), GridLayout.spec(i - 1, 1, 1f));
                    iv.setLayoutParams(params);
                    gridLayout.addView(iv);
                }
            }
        } else { // 2, 4, or >4 images
            int limit = Math.min(count, maxImages);
            for (int i = 0; i < limit; i++) {
                View viewToAdd;
                ImageView iv = createImageView(attachments.get(i), imageSize, false, i, attachments);

                if (i == maxImages - 1 && count > maxImages) {
                    RelativeLayout overlayContainer = new RelativeLayout(_context);
                    overlayContainer.setLayoutParams(new ViewGroup.LayoutParams(imageSize, imageSize));
                    overlayContainer.addView(iv);

                    View overlay = new View(_context);
                    overlay.setBackgroundColor(0x40000000);
                    overlayContainer.addView(overlay, new ViewGroup.LayoutParams(imageSize, imageSize));

                    TextView moreText = new TextView(_context);
                    moreText.setText("+" + (count - maxImages));
                    moreText.setTextColor(Color.WHITE);
                    moreText.setTextSize(24);
                    moreText.setGravity(Gravity.CENTER);
                    overlayContainer.addView(moreText, new ViewGroup.LayoutParams(imageSize, imageSize));
                    viewToAdd = overlayContainer;
                    viewToAdd.setOnClickListener(v -> openImageGallery(attachments, 3));
                } else {
                    viewToAdd = iv;
                }
                gridLayout.addView(viewToAdd);
            }
        }
    }
    
    private void openImageGallery(ArrayList<HashMap<String, Object>> attachments, int position) {
        if (_context != null && chatActivity != null) {
            ArrayList<Attachment> typed = AttachmentUtils.fromHashMapList(attachments);
            openImageGalleryTyped(typed, position);
        }
    }

    private void openImageGalleryTyped(ArrayList<Attachment> attachments, int position) {
        if (_context != null && chatActivity != null) {
            Intent intent = new Intent(_context, ImageGalleryActivity.class);
            intent.putParcelableArrayListExtra("attachments_parcelable", attachments);
            intent.putExtra("position", position);
            _context.startActivity(intent);
            if (_context instanceof Activity) {
                ((Activity) _context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
            }
        }
    }

    private ImageView createImageView(HashMap<String, Object> attachment, int width, boolean adjustBounds, int position, ArrayList<HashMap<String, Object>> attachments) {
        String url = String.valueOf(attachment.get("url"));
        ImageView imageView = new ImageView(_context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (adjustBounds) {
            imageView.setAdjustViewBounds(true);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            Glide.with(_context).load(url).into(imageView);
        } else {
            int height;
            Object widthObj = attachment.get("width");
            Object heightObj = attachment.get("height");

            if (widthObj != null && heightObj != null) {
                double imageWidth = ((Number) widthObj).doubleValue();
                double imageHeight = ((Number) heightObj).doubleValue();
                if (imageWidth > 0) {
                    double ratio = imageHeight / imageWidth;
                    height = (int) (width * ratio);
                } else {
                    height = width; // Fallback to square
                }
            } else {
                // Fallback for old messages without dimensions
                height = width;
            }

            imageView.setLayoutParams(new ViewGroup.LayoutParams(width, height));
            Glide.with(_context).load(url).override(width, height).into(imageView);
        }

        // Enhanced click listener to open gallery
        imageView.setOnClickListener(v -> openImageGallery(attachments, position));
        return imageView;
    }

    private void bindVideoViewHolder(VideoViewHolder holder, int position) {
        bindCommonMessageProperties(holder, position);
        HashMap<String, Object> data = _data.get(position);
        String msgText = String.valueOf(data.getOrDefault("message_text", ""));
        boolean isEncrypted = Boolean.TRUE.equals(data.get("isEncrypted"));

        if (isEncrypted) {
            try {
                msgText = e2eeHelper.decrypt(secondUserUid, msgText);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt message text in video view holder", e);
                msgText = "⚠️ Could not decrypt message";
            }
        }

        holder.message_text.setVisibility(msgText.isEmpty() ? View.GONE : View.VISIBLE);
        if (!msgText.isEmpty()) com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, msgText);
        ArrayList<HashMap<String, Object>> attachments = getDecryptedAttachments(data);
        if (attachments != null && !attachments.isEmpty()) {
            String videoUrl = String.valueOf(attachments.get(0).get("url"));
            if(holder.videoThumbnail != null) Glide.with(_context).load(videoUrl).into(holder.videoThumbnail);
            
            // --- CRITICAL FIX: Attach click listener to videoContainerCard ---
            if(holder.videoContainerCard != null) {
                holder.videoContainerCard.setOnClickListener(v -> chatActivity._OpenWebView(videoUrl));
            } else {
                // Fallback if card isn't found (though it should be)
                holder.itemView.setOnClickListener(v -> chatActivity._OpenWebView(videoUrl));
            }
        }
    }

    private void bindTypingViewHolder(TypingViewHolder holder, int position) {
        android.graphics.drawable.GradientDrawable bubbleDrawable = new android.graphics.drawable.GradientDrawable();
        bubbleDrawable.setColor(Color.TRANSPARENT);
        if(holder.messageBG != null) holder.messageBG.setBackground(bubbleDrawable);

        if (holder.mProfileImage != null) {
            if (secondUserAvatarUrl != null && !secondUserAvatarUrl.isEmpty() && !secondUserAvatarUrl.equals("null_banned")) {
                Glide.with(_context).load(Uri.parse(secondUserAvatarUrl)).into(holder.mProfileImage);
            } else if ("null_banned".equals(secondUserAvatarUrl)) {
                holder.mProfileImage.setImageResource(R.drawable.banned_avatar);
            } else {
                holder.mProfileImage.setImageResource(R.drawable.avatar);
            }
        }
        if(holder.mProfileCard != null) holder.mProfileCard.setVisibility(View.VISIBLE);
    }
    
    private void bindLinkPreviewViewHolder(LinkPreviewViewHolder holder, int position) {
        bindCommonMessageProperties(holder, position);
        HashMap<String, Object> data = _data.get(position);

        boolean isEncrypted = Boolean.TRUE.equals(data.get("isEncrypted"));
        String messageContent = String.valueOf(data.getOrDefault("message_text", ""));
        String decryptedText = messageContent;

        if (isEncrypted) {
            try {
                decryptedText = e2eeHelper.decrypt(secondUserUid, messageContent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to decrypt message for link preview", e);
                decryptedText = "⚠️ Could not decrypt message";
            }
        }

        holder.message_text.setVisibility(View.VISIBLE);
        com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, decryptedText);

        String urlToPreview = LinkPreviewUtil.extractUrl(decryptedText);
        if (urlToPreview != null) {
            if (holder.linkPreviewContainer != null) {
                holder.linkPreviewContainer.setVisibility(View.VISIBLE);
            }
            if (holder.linkPreviewImage != null) holder.linkPreviewImage.setVisibility(View.GONE);
            if (holder.linkPreviewTitle != null) holder.linkPreviewTitle.setText("Loading Preview...");
            if (holder.linkPreviewDescription != null) holder.linkPreviewDescription.setText("");
            if (holder.linkPreviewDomain != null) holder.linkPreviewDomain.setText(urlToPreview);

            LinkPreviewUtil.fetchPreview(urlToPreview, new LinkPreviewUtil.LinkPreviewCallback() {
                @Override
                public void onPreviewDataFetched(LinkPreviewUtil.LinkData linkData) {
                    if (linkData != null) {
                        if (holder.linkPreviewTitle != null) holder.linkPreviewTitle.setText(linkData.title);
                        if (holder.linkPreviewDescription != null) holder.linkPreviewDescription.setText(linkData.description);
                        if (holder.linkPreviewDomain != null) holder.linkPreviewDomain.setText(linkData.domain);
                        if (linkData.imageUrl != null && !linkData.imageUrl.isEmpty() && holder.linkPreviewImage != null) {
                            if (chatActivity != null && !chatActivity.isDestroyed()) {
                                Glide.with(chatActivity).load(linkData.imageUrl).into(holder.linkPreviewImage);
                                holder.linkPreviewImage.setVisibility(View.VISIBLE);
                            }
                        }
                        if (holder.linkPreviewContainer != null) {
                            holder.linkPreviewContainer.setOnClickListener(v -> {
                                if (chatActivity != null) {
                                    chatActivity._OpenWebView(linkData.url);
                                }
                            });
                        }
                    }
                }
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Link preview error: " + e.getMessage());
                    if (holder.linkPreviewContainer != null) {
                        holder.linkPreviewContainer.setVisibility(View.GONE);
                    }
                }
            });
        } else {
            if (holder.linkPreviewContainer != null) {
                holder.linkPreviewContainer.setVisibility(View.GONE);
            }
        }
    }
    
    private void bindLoadingViewHolder(LoadingViewHolder holder, int position) {
        // The progress bar is displayed, and the loading is handled by the
        // scroll listener in ChatActivity. No action needed here.
    }

    
    // CRITICAL FIX: Smart timestamp visibility logic
    private boolean _shouldShowTimestamp(int position, HashMap<String, Object> currentMessage) {
        // Always show timestamp for the last message
        if (position == _data.size() - 1) {
            return true;
        }
        
        // Show timestamp if there's a significant time gap (more than 5 minutes)
        if (position < _data.size() - 1) {
            try {
                HashMap<String, Object> nextMessage = _data.get(position + 1);
                long currentTime = _getMessageTimestamp(currentMessage);
                long nextTime = _getMessageTimestamp(nextMessage);
                
                // Show timestamp if gap is more than 5 minutes (300000 ms)
                return Math.abs(currentTime - nextTime) > 300000;
            } catch (Exception e) {
                Log.e(TAG, "Error calculating timestamp visibility: " + e.getMessage());
                return false;
            }
        }
        
        return false;
    }
    
    // Helper method to get message timestamp
    private long _getMessageTimestamp(HashMap<String, Object> message) {
        try {
            Object pushDateObj = message.get("push_date");
			if (pushDateObj instanceof Long) {
				return (Long) pushDateObj;
			} else if (pushDateObj instanceof Double) {
				return ((Double) pushDateObj).longValue();
			} else if (pushDateObj instanceof String) {
				return Long.parseLong((String) pushDateObj);
			}
        } catch (Exception e) {
            Log.w(TAG, "Error parsing message timestamp: " + e.getMessage());
        }
        return System.currentTimeMillis();
    }

    private ArrayList<HashMap<String, Object>> getAttachmentsFromMessage(HashMap<String, Object> messageData) {
        Object attachmentsObj = messageData.get(ChatActivity.ATTACHMENTS_KEY);
        if (attachmentsObj instanceof ArrayList) {
            try {
                return (ArrayList<HashMap<String, Object>>) attachmentsObj;
            } catch (ClassCastException e) {
                Log.e(TAG, "Failed to cast attachments to ArrayList<HashMap>", e);
                return new ArrayList<>();
            }
        } else if (attachmentsObj instanceof String) {
            ArrayList<HashMap<String, Object>> attachmentsList = new ArrayList<>();
            HashMap<String, Object> attachmentMap = new HashMap<>();
            attachmentMap.put("url", attachmentsObj.toString());
            attachmentMap.put("publicId", "");
            attachmentMap.put("width", 200);
            attachmentMap.put("height", 200);
            attachmentsList.add(attachmentMap);
            return attachmentsList;
        } else if (attachmentsObj != null) {
            Log.w(TAG, "Unexpected type for attachments: " + attachmentsObj.getClass().getName());
            return new ArrayList<>();
        }
        return new ArrayList<>();
    }

    private ArrayList<HashMap<String, Object>> getDecryptedAttachments(HashMap<String, Object> messageData) {
        ArrayList<HashMap<String, Object>> attachments = getAttachmentsFromMessage(messageData);
        boolean isEncrypted = Boolean.TRUE.equals(messageData.get("isEncrypted"));

        if (isEncrypted && attachments != null && !attachments.isEmpty()) {
            ArrayList<HashMap<String, Object>> decryptedAttachments = new ArrayList<>();
            for (HashMap<String, Object> attachment : attachments) {
                HashMap<String, Object> decryptedAttachment = new HashMap<>(attachment);
                try {
                    String url = (String) decryptedAttachment.get("url");
                    String publicId = (String) decryptedAttachment.get("publicId");

                    if (url != null) {
                        decryptedAttachment.put("url", e2eeHelper.decrypt(secondUserUid, url));
                    }
                    if (publicId != null) {
                        decryptedAttachment.put("publicId", e2eeHelper.decrypt(secondUserUid, publicId));
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Failed to decrypt attachment", e);
                    // Replace with placeholder or error indicator
                    decryptedAttachment.put("url", "https://via.placeholder.com/150/FF0000/FFFFFF?text=Error");
                    decryptedAttachment.put("publicId", "error");
                }
                decryptedAttachments.add(decryptedAttachment);
            }
            return decryptedAttachments;
        }
        return attachments;
    }
}