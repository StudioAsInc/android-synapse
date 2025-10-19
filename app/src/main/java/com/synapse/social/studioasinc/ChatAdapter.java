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
import android.widget.SeekBar;
import android.media.MediaPlayer;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import androidx.gridlayout.widget.GridLayout;
import android.widget.RelativeLayout;
import com.google.firebase.database.GenericTypeIndicator;
import android.view.MotionEvent;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import android.app.Activity;
import android.view.Gravity;
import com.synapse.social.studioasinc.chat.model.ChatMessage;
import com.synapse.social.studioasinc.config.CloudinaryConfig;
import com.synapse.social.studioasinc.model.Attachment;
import com.synapse.social.studioasinc.util.AttachmentUtils;
import com.synapse.social.studioasinc.util.UIUtils;


public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatAdapter";
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_MEDIA_GRID = 2;
    private static final int VIEW_TYPE_TYPING = 3;
    private static final int VIEW_TYPE_VIDEO = 4;
    private static final int VIEW_TYPE_LINK_PREVIEW = 5;
    private static final int VIEW_TYPE_VOICE_MESSAGE = 6;
    private static final int VIEW_TYPE_LOADING_MORE = 99;

    private List<ChatMessage> _data;
    private HashMap<String, ChatMessage> repliedMessagesCache;
    private Context _context;
    private String secondUserAvatarUrl = "";
    private String firstUserName = "";
    private String secondUserName = "";
    private SharedPreferences appSettings;
    private ChatAdapterListener listener;
    private boolean isGroupChat = false;
    private HashMap<String, String> userNamesMap = new HashMap<>();

    public ChatAdapter(List<ChatMessage> _arr, HashMap<String, ChatMessage> repliedCache, ChatAdapterListener listener) {
        _data = _arr;
        this.repliedMessagesCache = repliedCache;
        this.listener = listener;
    }
    public void setSecondUserAvatar(String url) { this.secondUserAvatarUrl = url; }
    public void setFirstUserName(String name) { this.firstUserName = name; }
    public void setSecondUserName(String name) { this.secondUserName = name; }
    public void setGroupChat(boolean isGroup) { this.isGroupChat = isGroup; }
    public void setUserNamesMap(HashMap<String, String> map) { this.userNamesMap = map; }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = _data.get(position);
        
        String type = message.getType();
        Log.d(TAG, "Message at position " + position + " has type: " + type);

        if ("VOICE_MESSAGE".equals(type)) {
            return VIEW_TYPE_VOICE_MESSAGE;
        }
        
        if ("ATTACHMENT_MESSAGE".equals(type)) {
            List<Attachment> attachments = message.getAttachments();
            Log.d(TAG, "ATTACHMENT_MESSAGE detected with " + (attachments != null ? attachments.size() : 0) + " attachments");

            if (attachments != null && attachments.size() == 1 && attachments.get(0).getPublicId().contains("|video")) {
                Log.d(TAG, "Video message detected, returning VIEW_TYPE_VIDEO");
                return VIEW_TYPE_VIDEO;
            }
            Log.d(TAG, "Media message detected, returning VIEW_TYPE_MEDIA_GRID");
            return VIEW_TYPE_MEDIA_GRID;
        }

        String messageText = message.getMessageText();
        if (LinkPreviewUtil.extractUrl(messageText) != null) {
            Log.d(TAG, "Link preview message detected, returning VIEW_TYPE_LINK_PREVIEW");
            return VIEW_TYPE_LINK_PREVIEW;
        }
        
        Log.d(TAG, "Text message detected, returning VIEW_TYPE_TEXT");
        return VIEW_TYPE_TEXT;
    }

    @Override
    public long getItemId(int position) {
        try {
            return _data.get(position).getKey().hashCode();
        } catch (Exception e) {
            return position;
        }
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        _context = parent.getContext();
        appSettings = _context.getSharedPreferences("appSettings", Context.MODE_PRIVATE);
        LayoutInflater inflater = LayoutInflater.from(_context);
        switch (viewType) {
            case VIEW_TYPE_MEDIA_GRID: return new MediaViewHolder(inflater.inflate(R.layout.chat_bubble_media, parent, false));
            case VIEW_TYPE_VIDEO: return new VideoViewHolder(inflater.inflate(R.layout.chat_bubble_video, parent, false));
            case VIEW_TYPE_TYPING: return new TypingViewHolder(inflater.inflate(R.layout.chat_bubble_typing, parent, false));
            case VIEW_TYPE_LINK_PREVIEW: return new LinkPreviewViewHolder(inflater.inflate(R.layout.chat_bubble_link_preview, parent, false));
            case VIEW_TYPE_VOICE_MESSAGE: return new VoiceMessageViewHolder(inflater.inflate(R.layout.chat_bubble_voice, parent, false));
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
            case VIEW_TYPE_VOICE_MESSAGE: bindVoiceMessageViewHolder((VoiceMessageViewHolder) holder, position); break;
            case VIEW_TYPE_LOADING_MORE: bindLoadingViewHolder((LoadingViewHolder) holder, position); break;
        }
    }

    @Override
    public int getItemCount() { return _data.size(); }

    @Override
    public void onViewRecycled(@NonNull RecyclerView.ViewHolder holder) {
        super.onViewRecycled(holder);
        if (holder instanceof VoiceMessageViewHolder) {
            VoiceMessageViewHolder vh = (VoiceMessageViewHolder) holder;
            if (vh.mediaPlayer != null) {
                if (vh.mediaPlayer.isPlaying()) {
                    vh.mediaPlayer.stop();
                }
                vh.mediaPlayer.release();
                vh.mediaPlayer = null;
            }
            if (vh.handler != null) {
                vh.handler.removeCallbacksAndMessages(null);
            }
        }
    }

    private void bindCommonMessageProperties(BaseMessageViewHolder holder, int position) {
        ChatMessage data = _data.get(position);
        String myUid = FirebaseAuth.getInstance().getCurrentUser() != null ? FirebaseAuth.getInstance().getCurrentUser().getUid() : "";
        String msgUid = data.getUid();
        boolean isMyMessage = msgUid.equals(myUid);
        
        if (holder.senderUsername != null) {
            if (isGroupChat && !isMyMessage && userNamesMap != null && userNamesMap.containsKey(msgUid)) {
                holder.senderUsername.setVisibility(View.VISIBLE);
                holder.senderUsername.setText(userNamesMap.get(msgUid));
            } else {
                holder.senderUsername.setVisibility(View.GONE);
            }
        }
        
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
                ChatMessage currentMsg = _data.get(position);
                ChatMessage nextMsg = _data.get(position + 1);

                String nextUid = nextMsg.getUid();
                String currUid = currentMsg.getUid();
                boolean nextIsDifferentUser = !nextUid.equals(currUid);

                boolean timeIsSignificant = false;
                long currentTime = currentMsg.getPushDate();
                long nextTime = nextMsg.getPushDate();
                if ((nextTime - currentTime) > (5 * 60 * 1000)) { // 5 minutes
                    timeIsSignificant = true;
                }

                if (nextIsDifferentUser || timeIsSignificant) {
                    showMessageInfo = true;
                }
            }
            holder.my_message_info.setVisibility(showMessageInfo ? View.VISIBLE : View.GONE);

            if (showMessageInfo) {
                Calendar push = Calendar.getInstance();
                push.setTimeInMillis(data.getPushDate());
                boolean shouldShowTime = _shouldShowTimestamp(position, data);
                holder.date.setVisibility(shouldShowTime ? View.VISIBLE : View.GONE);
                if (shouldShowTime) {
                    holder.date.setText(new SimpleDateFormat("hh:mm a").format(push.getTime()));
                }

                holder.message_state.setVisibility(isMyMessage ? View.VISIBLE : View.GONE);
                if (isMyMessage) {
                    String state = data.getMessageState();
                    holder.message_state.setImageResource("seen".equals(state) ? R.drawable.icon_done_all_round : R.drawable.icon_done_round);
                    holder.message_state.setColorFilter("seen".equals(state) ? _context.getResources().getColor(R.color.colorPrimary) : 0xFF424242, PorterDuff.Mode.SRC_ATOP);
                }
            }
        }

        if (holder.mProfileCard != null) {
            holder.mProfileCard.setVisibility(View.GONE);
        }

        if (holder.mRepliedMessageLayout != null) {
            holder.mRepliedMessageLayout.setVisibility(View.GONE);
            Log.d(TAG, "Checking for reply data at position " + position + " - Reply layout holder exists");

            if (data.getRepliedMessageId() != null) {
                String repliedId = data.getRepliedMessageId();
                Log.d(TAG, "Found replied_message_id: " + repliedId + " for position: " + position);

                if (repliedId != null && !repliedId.isEmpty() && !repliedId.equals("null")) {
                    Log.d(TAG, "Processing reply for message ID: " + repliedId);

                    if (repliedMessagesCache != null && repliedMessagesCache.containsKey(repliedId)) {
                        ChatMessage snapshot = repliedMessagesCache.get(repliedId);
                        if (snapshot != null) {
                            holder.mRepliedMessageLayout.setVisibility(View.VISIBLE);

                            if (isMyMessage) {
                                holder.mRepliedMessageLayout.setCardBackgroundColor(_context.getResources().getColor(R.color.md_theme_primaryContainer, null));
                            } else {
                                holder.mRepliedMessageLayout.setCardBackgroundColor(_context.getResources().getColor(R.color.md_theme_surfaceContainerHigh, null));
                            }

                            String repliedUid = snapshot.getUid();
                            String repliedText = snapshot.getMessageText();

                            if (holder.mRepliedMessageLayoutImage != null) {
                                if (snapshot.getAttachments() != null) {
                                    List<Attachment> attachments = snapshot.getAttachments();
                                    if (attachments != null && !attachments.isEmpty()) {
                                        holder.mRepliedMessageLayoutImage.setVisibility(View.VISIBLE);
                                        String publicId = attachments.get(0).getPublicId();
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
                                holder.mRepliedMessageLayoutUsername.setTextColor(isMyMessage ? _context.getResources().getColor(R.color.md_theme_onPrimaryContainer, null) : _context.getResources().getColor(R.color.md_theme_onSurface, null));
                            }

                            if (holder.mRepliedMessageLayoutMessage != null) {
                                String messageText = repliedText != null ? repliedText : "";
                                holder.mRepliedMessageLayoutMessage.setText(messageText);
                                holder.mRepliedMessageLayoutMessage.setTextColor(isMyMessage ? _context.getResources().getColor(R.color.md_theme_onPrimaryContainer, null) : _context.getResources().getColor(R.color.md_theme_onSurfaceVariant, null));
                                holder.mRepliedMessageLayoutMessage.setVisibility(messageText.isEmpty() ? View.GONE : View.VISIBLE);
                            }

                            if (holder.mRepliedMessageLayoutLeftBar != null) {
                                android.graphics.drawable.GradientDrawable leftBarDrawable = new android.graphics.drawable.GradientDrawable();
                                leftBarDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                                leftBarDrawable.setColor(_context.getResources().getColor(R.color.md_theme_primary, null));
                                int leftBarRadius = (int) _context.getResources().getDimension(R.dimen.left_bar_corner_radius);
                                leftBarDrawable.setCornerRadius(leftBarRadius);
                                holder.mRepliedMessageLayoutLeftBar.setBackground(leftBarDrawable);
                            }

                            View.OnClickListener clickListener = v -> {
                                if (listener != null) {
                                    listener.scrollToMessage(repliedId);
                                }
                            };
                            holder.mRepliedMessageLayout.setOnClickListener(clickListener);
                            if (holder.mRepliedMessageLayoutMessage != null) holder.mRepliedMessageLayoutMessage.setOnClickListener(clickListener);
                            if (holder.mRepliedMessageLayoutImage != null) holder.mRepliedMessageLayoutImage.setOnClickListener(clickListener);
                        } else {
                            holder.mRepliedMessageLayout.setVisibility(View.VISIBLE);

                            if (holder.mRepliedMessageLayoutImage != null) {
                                holder.mRepliedMessageLayoutImage.setVisibility(View.GONE);
                            }
                            if (holder.mRepliedMessageLayoutUsername != null) {
                                holder.mRepliedMessageLayoutUsername.setText("Loading…");
                                holder.mRepliedMessageLayoutUsername.setTextColor(isMyMessage ? _context.getResources().getColor(R.color.md_theme_onPrimaryContainer, null) : _context.getResources().getColor(R.color.md_theme_onSurface, null));
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
                Log.d(TAG, "No replied_message_id found for position: " + position);
            }
        } else {
            Log.w(TAG, "Reply layout holder is null for position: " + position);
        }
        
        if (holder.messageBG != null) {
            boolean isFirst = isFirstMessage(position);
            boolean isLast = isLastMessage(position);

            int drawableResId;
            if (isMyMessage) {
                if (isFirst && isLast) {
                    drawableResId = R.drawable.shape_outgoing_message_single;
                } else if (isFirst) {
                    drawableResId = R.drawable.shape_outgoing_message_first;
                } else if (isLast) {
                    drawableResId = R.drawable.shape_outgoing_message_last;
                } else {
                    drawableResId = R.drawable.shape_outgoing_message_middle;
                }
            } else {
                if (isFirst && isLast) {
                    drawableResId = R.drawable.shape_incoming_message_single;
                } else if (isFirst) {
                    drawableResId = R.drawable.shape_incoming_message_first;
                } else if (isLast) {
                    drawableResId = R.drawable.shape_incoming_message_last;
                } else {
                    drawableResId = R.drawable.shape_incoming_message_middle;
                }
            }
            holder.messageBG.setBackgroundResource(drawableResId);

            if (isMyMessage) {
                if(holder.message_text != null) holder.message_text.setTextColor(_context.getResources().getColor(R.color.md_theme_onPrimaryContainer, null));
            } else {
                if(holder.message_text != null) holder.message_text.setTextColor(_context.getResources().getColor(R.color.md_theme_onSurface, null));
            }

            int rippleColor = isMyMessage ? 0x33FFFFFF : 0x22000000;
            android.graphics.drawable.Drawable background = holder.messageBG.getBackground();
            if (background instanceof android.graphics.drawable.RippleDrawable) {
                android.graphics.drawable.RippleDrawable ripple = (android.graphics.drawable.RippleDrawable) background;
                android.content.res.ColorStateList rippleColors = new android.content.res.ColorStateList(new int[][]{ new int[]{} }, new int[]{ rippleColor });
                ripple.setColor(rippleColors);
            }
            holder.messageBG.setClickable(true);
            holder.messageBG.setLongClickable(true);
        }

        int textSize = 16;
        try { textSize = (int) Double.parseDouble(appSettings.getString("ChatTextSize", "16")); } catch (Exception e) {}

        if(holder.message_text != null) holder.message_text.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
        if (holder.mRepliedMessageLayoutUsername != null) holder.mRepliedMessageLayoutUsername.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize - 2);
        if (holder.mRepliedMessageLayoutMessage != null) holder.mRepliedMessageLayoutMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);

        if (!isMyMessage && "sended".equals(data.getMessageState())) {
            String otherUserUid = listener.getRecipientUid();

            String messageKey = data.getKey();
            FirebaseDatabase.getInstance().getReference("skyline/chats").child(otherUserUid).child(myUid).child(messageKey).child("message_state").setValue("seen");
            FirebaseDatabase.getInstance().getReference("skyline/chats").child(myUid).child(otherUserUid).child(messageKey).child("message_state").setValue("seen");
            FirebaseDatabase.getInstance().getReference("skyline/inbox").child(otherUserUid).child(myUid).child("last_message_state").setValue("seen");
        }
        
        View.OnLongClickListener longClickListener = v -> {
            Log.d(TAG, "Long click detected on view: " + v.getClass().getSimpleName() + " at position: " + position);
            if (listener != null) {
                listener.performHapticFeedback();
            }
            View anchor = holder.messageBG != null ? holder.messageBG : holder.itemView;
            listener.showMessageOverviewPopup(anchor, position, (ArrayList) _data);
            return true;
        };

        if (holder.messageBG != null) {
            holder.messageBG.setOnLongClickListener(longClickListener);
        }

        if (holder.message_text != null) {
            holder.message_text.setOnLongClickListener(longClickListener);
        }
    }

    private void bindTextViewHolder(TextViewHolder holder, int position) {
        bindCommonMessageProperties(holder, position);
        String text = _data.get(position).getMessageText();
        holder.message_text.setVisibility(View.VISIBLE);
        com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, text);
    }

    private void bindMediaViewHolder(MediaViewHolder holder, int position) {
        Log.d(TAG, "bindMediaViewHolder called for position " + position);
        bindCommonMessageProperties(holder, position);
        ChatMessage data = _data.get(position);
        String msgText = data.getMessageText();

        if (holder.message_text != null) {
            holder.message_text.setVisibility(msgText.isEmpty() ? View.GONE : View.VISIBLE);
            if (!msgText.isEmpty()) {
                com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, msgText);
            }
        }

        List<Attachment> attachments = data.getAttachments();
        Log.d(TAG, "Attachments found: " + (attachments != null ? attachments.size() : 0));

        if (attachments == null || attachments.isEmpty()) {
            Log.w(TAG, "No attachments found, showing only message text");
            if (holder.mediaGridLayout != null) holder.mediaGridLayout.setVisibility(View.GONE);
            if (holder.mediaCarouselContainer != null) holder.mediaCarouselContainer.setVisibility(View.GONE);

            if (msgText.isEmpty() && holder.message_text != null) {
                holder.message_text.setVisibility(View.VISIBLE);
                holder.message_text.setText("Media message");
            }
            return;
        }

        int count = attachments.size();
        Log.d(TAG, "Processing " + count + " attachments");

        boolean useCarousel = false;

        Log.d(TAG, "useCarousel: " + useCarousel + ", count: " + count);

        if (useCarousel && holder.mediaCarouselContainer != null && holder.mediaCarouselRecyclerView != null) {
            Log.d(TAG, "Using carousel layout");
            if (holder.mediaGridLayout != null) holder.mediaGridLayout.setVisibility(View.GONE);
            holder.mediaCarouselContainer.setVisibility(View.VISIBLE);

            try {
                setupCarouselLayout(holder, (ArrayList) attachments);
            } catch (Exception e) {
                Log.e(TAG, "Error setting up carousel, falling back to grid: " + e.getMessage());
                if (holder.mediaCarouselContainer != null) holder.mediaCarouselContainer.setVisibility(View.GONE);
                if (holder.mediaGridLayout != null) {
                    holder.mediaGridLayout.setVisibility(View.VISIBLE);
                    setupGridLayout(holder, (ArrayList) attachments);
                }
            }
        } else {
            Log.d(TAG, "Using grid layout");
            if (holder.mediaCarouselContainer != null) holder.mediaCarouselContainer.setVisibility(View.GONE);
            if (holder.mediaGridLayout != null) {
                holder.mediaGridLayout.setVisibility(View.VISIBLE);
                try {
                    setupGridLayout(holder, (ArrayList) attachments);
                    Log.d(TAG, "Grid layout setup completed successfully");
                } catch (Exception e) {
                    Log.e(TAG, "Error setting up grid layout: " + e.getMessage());
                    if (holder.message_text != null) {
                        holder.message_text.setVisibility(View.VISIBLE);
                        holder.message_text.setText("Media message (" + attachments.size() + " images)");
                    }
                }
            } else {
                Log.e(TAG, "mediaGridLayout is null!");
                if (holder.message_text != null) {
                    holder.message_text.setVisibility(View.VISIBLE);
                    holder.message_text.setText("Media message (" + attachments.size() + " images)");
                }
            }
        }
    }

    private void setupCarouselLayout(MediaViewHolder holder, ArrayList<Attachment> attachments) {
        Log.d(TAG, "Setting up carousel layout for " + attachments.size() + " images");

        LinearLayoutManager layoutManager = new LinearLayoutManager(_context, LinearLayoutManager.HORIZONTAL, false);
        holder.mediaCarouselRecyclerView.setLayoutManager(layoutManager);

        if (holder.mediaCarouselRecyclerView.getItemDecorationCount() == 0) {
            holder.mediaCarouselRecyclerView.addItemDecoration(
                CarouselItemDecoration.createWithStandardSpacing(holder.mediaCarouselRecyclerView));
        }

        ArrayList<Attachment> typedAttachments = attachments;
        if (typedAttachments == null || typedAttachments.isEmpty()) {
            return;
        }
        MessageImageCarouselAdapter adapter = new MessageImageCarouselAdapter(_context, typedAttachments,
            (position, attachmentList) -> openImageGalleryTyped(attachmentList, position));
        holder.mediaCarouselRecyclerView.setAdapter(adapter);

        if (holder.viewAllImagesButton != null) {
            if (attachments.size() > 3) {
                holder.viewAllImagesButton.setVisibility(View.VISIBLE);
                holder.viewAllImagesButton.setText("View all " + attachments.size() + " images");
                holder.viewAllImagesButton.setOnClickListener(v -> openImageGalleryTyped(typedAttachments, 0));
            } else {
                holder.viewAllImagesButton.setVisibility(View.GONE);
            }
        }

        ViewGroup.LayoutParams cardParams = holder.mediaContainerCard.getLayoutParams();
        cardParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        holder.mediaContainerCard.setLayoutParams(cardParams);
    }

    private void setupGridLayout(MediaViewHolder holder, ArrayList<Attachment> attachments) {
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

        ViewGroup.LayoutParams gridParams = gridLayout.getLayoutParams();
        if (gridParams == null) {
            gridParams = new ViewGroup.LayoutParams(totalGridWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        gridParams.width = totalGridWidth;
        gridLayout.setLayoutParams(gridParams);

        if (count == 1) {
            gridLayout.setColumnCount(1);
            Attachment attachment = attachments.get(0);
            ImageView iv = createImageView(attachment, totalGridWidth, true, 0, attachments);
            gridLayout.addView(iv);

        } else if (count == 3) {
            int portraitIndex = -1;
            for(int i=0; i < attachments.size(); i++){
                Attachment attachment = attachments.get(i);
                double width = attachment.getWidth();
                double height = attachment.getHeight();

                if (width > 0 && height > 0) {
                    if(height > width){
                        portraitIndex = i;
                        break;
                    }
                }
            }

            if(portraitIndex != -1){
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
        } else {
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
                    viewToAdd.setOnClickListener(v -> openImageGalleryTyped(attachments, 3));
                } else {
                    viewToAdd = iv;
                }
                gridLayout.addView(viewToAdd);
            }
        }
    }

    private void openImageGalleryTyped(ArrayList<Attachment> attachments, int position) {
        if (_context != null) {
            Intent intent = new Intent(_context, ImageGalleryActivity.class);
            intent.putParcelableArrayListExtra("attachments_parcelable", attachments);
            intent.putExtra("position", position);
            _context.startActivity(intent);
            if (_context instanceof Activity) {
                ((Activity) _context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
            }
        }
    }

    private ImageView createImageView(Attachment attachment, int width, boolean adjustBounds, int position, ArrayList<Attachment> attachments) {
        String url = attachment.getUrl();
        ImageView imageView = new ImageView(_context);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

        if (adjustBounds) {
            imageView.setAdjustViewBounds(true);
            imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            Glide.with(_context).load(url).into(imageView);
        } else {
            int height;
            double imageWidth = attachment.getWidth();
            double imageHeight = attachment.getHeight();

            if (imageWidth > 0) {
                double ratio = imageHeight / imageWidth;
                height = (int) (width * ratio);
            } else {
                height = width;
            }

            imageView.setLayoutParams(new ViewGroup.LayoutParams(width, height));
            Glide.with(_context).load(url).override(width, height).into(imageView);
        }

        imageView.setOnClickListener(v -> openImageGalleryTyped(attachments, position));
        return imageView;
    }

    private void bindVideoViewHolder(VideoViewHolder holder, int position) {
        bindCommonMessageProperties(holder, position);
        ChatMessage data = _data.get(position);
        String msgText = data.getMessageText();
        holder.message_text.setVisibility(msgText.isEmpty() ? View.GONE : View.VISIBLE);
        if (!msgText.isEmpty()) com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, msgText);
        List<Attachment> attachments = data.getAttachments();
        if (attachments != null && !attachments.isEmpty()) {
            String videoUrl = attachments.get(0).getUrl();
            if(holder.videoThumbnail != null) Glide.with(_context).load(videoUrl).into(holder.videoThumbnail);

            if(holder.videoContainerCard != null) {
                holder.videoContainerCard.setOnClickListener(v -> listener.openUrl(videoUrl));
            } else {
                holder.itemView.setOnClickListener(v -> listener.openUrl(videoUrl));
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
        ChatMessage data = _data.get(position);
        String messageText = data.getMessageText();
        holder.message_text.setVisibility(View.VISIBLE);
        com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, messageText);

        String urlToPreview = LinkPreviewUtil.extractUrl(messageText);
        if (urlToPreview != null) {
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
                            if (_context != null) {
                                Glide.with(_context).load(linkData.imageUrl).into(holder.linkPreviewImage);
                                holder.linkPreviewImage.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Link preview error: " + e.getMessage());
                    if (holder.linkPreviewTitle != null) holder.linkPreviewTitle.setText("Cannot load preview");
                    if (holder.linkPreviewDescription != null) holder.linkPreviewDescription.setText("");
                    if (holder.linkPreviewDomain != null) holder.linkPreviewDomain.setText(urlToPreview);
                    if (holder.linkPreviewImage != null) holder.linkPreviewImage.setVisibility(View.GONE);
                }
            });
        }
    }

    private void bindLoadingViewHolder(LoadingViewHolder holder, int position) {
    }

    private void bindVoiceMessageViewHolder(VoiceMessageViewHolder holder, int position) {
        bindCommonMessageProperties(holder, position);
        ChatMessage data = _data.get(position);
        String audioUrl = data.getAudioUrl();
        long duration = data.getAudioDuration();

        if (holder.mediaPlayer == null) {
            holder.mediaPlayer = new MediaPlayer();
        }

        holder.duration.setText(_getDurationString(duration));
        holder.playPauseButton.setOnClickListener(v -> {
            if (holder.mediaPlayer.isPlaying()) {
                holder.mediaPlayer.pause();
                holder.playPauseButton.setImageResource(R.drawable.ic_send);
            } else {
                holder.mediaPlayer.start();
                holder.playPauseButton.setImageResource(R.drawable.ic_close_48px);
            }
        });

        holder.mediaPlayer.setOnCompletionListener(mp -> {
            holder.playPauseButton.setImageResource(R.drawable.ic_send);
            holder.seekBar.setProgress(0);
        });

        try {
            holder.mediaPlayer.reset();
            holder.mediaPlayer.setDataSource(audioUrl);
            holder.mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }

        holder.mediaPlayer.setOnPreparedListener(mp -> {
            holder.seekBar.setMax(mp.getDuration());
            holder.handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (holder.mediaPlayer != null) {
                        try {
                            holder.seekBar.setProgress(holder.mediaPlayer.getCurrentPosition());
                            holder.handler.postDelayed(this, 1000);
                        } catch (IllegalStateException e) {
                        }
                    }
                }
            }, 1000);
        });

        holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    holder.mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    private boolean isFirstMessage(int position) {
        if (position == 0) {
            return true;
        }
        String currentSender = _data.get(position).getUid();
        String previousSender = _data.get(position - 1).getUid();
        return !currentSender.equals(previousSender);
    }

    private boolean isLastMessage(int position) {
        if (position == _data.size() - 1) {
            return true;
        }
        String currentSender = _data.get(position).getUid();
        String nextSender = _data.get(position + 1).getUid();
        return !currentSender.equals(nextSender);
    }

	private String _getDurationString(final long _durationInMillis) {
		long seconds = _durationInMillis / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		seconds %= 60;
		minutes %= 60;

		if (hours > 0) {
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		} else {
			return String.format("%02d:%02d", minutes, seconds);
		}
	}
    private boolean _shouldShowTimestamp(int position, ChatMessage currentMessage) {
        if (position == _data.size() - 1) {
            return true;
        }

        if (position < _data.size() - 1) {
            try {
                ChatMessage nextMessage = _data.get(position + 1);
                long currentTime = currentMessage.getPushDate();
                long nextTime = nextMessage.getPushDate();

                return Math.abs(currentTime - nextTime) > 300000;
            } catch (Exception e) {
                Log.e(TAG, "Error calculating timestamp visibility: " + e.getMessage());
                return false;
            }
        }

        return false;
    }
}