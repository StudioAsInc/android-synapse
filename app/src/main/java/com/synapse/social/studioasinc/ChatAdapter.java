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

    private List<ChatMessage> _data = new ArrayList<>();
    private HashMap<String, ChatMessage> repliedMessagesCache;
    private Context _context;
    private String secondUserAvatarUrl = "";
    private String firstUserName = "";
    private String secondUserName = "";
    private SharedPreferences appSettings;
    private ChatAdapterListener listener;
    private boolean isGroupChat = false;
    private HashMap<String, String> userNamesMap = new HashMap<>();

    public ChatAdapter(ChatAdapterListener listener) {
        this.listener = listener;
    }

    public void setData(List<ChatMessage> data) {
        this._data = data;
        notifyDataSetChanged();
    }
    public void setSecondUserAvatar(String url) { this.secondUserAvatarUrl = url; }
    public void setFirstUserName(String name) { this.firstUserName = name; }
    public void setSecondUserName(String name) { this.secondUserName = name; }
    public void setGroupChat(boolean isGroup) { this.isGroupChat = isGroup; }
    public void setUserNamesMap(HashMap<String, String> map) { this.userNamesMap = map; }

    public void addTypingIndicator() {
        _data.add(new ChatMessage("", "", "", 0, "", "", null, null, null, false, true));
        notifyItemInserted(_data.size() - 1);
    }

    public void removeTypingIndicator() {
        if (!_data.isEmpty() && _data.get(_data.size() - 1).isTyping()) {
            _data.remove(_data.size() - 1);
            notifyItemRemoved(_data.size());
        }
    }

    @Override
    public int getItemViewType(int position) {
        ChatMessage message = _data.get(position);
        if (message.isLoadingMore()) return VIEW_TYPE_LOADING_MORE;
        if (message.isTyping()) return VIEW_TYPE_TYPING;

        switch (message.getType()) {
            case "VOICE_MESSAGE":
                return VIEW_TYPE_VOICE_MESSAGE;
            case "ATTACHMENT_MESSAGE":
                if (message.getAttachments() != null && message.getAttachments().size() == 1 && message.getAttachments().get(0).getResourceType().equals("video")) {
                    return VIEW_TYPE_VIDEO;
                }
                return VIEW_TYPE_MEDIA_GRID;
            default:
                if (LinkPreviewUtil.extractUrl(message.getMessageText()) != null) {
                    return VIEW_TYPE_LINK_PREVIEW;
                }
                return VIEW_TYPE_TEXT;
        }
    }

    @Override
    public long getItemId(int position) {
        return _data.get(position).getKey().hashCode();
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
        ChatMessage message = _data.get(position);
        String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        boolean isMyMessage = message.getUid().equals(myUid);

        // Handle username display for group chats
        if (holder.senderUsername != null) {
            if (isGroupChat && !isMyMessage && userNamesMap != null && userNamesMap.containsKey(message.getUid())) {
                holder.senderUsername.setVisibility(View.VISIBLE);
                holder.senderUsername.setText(userNamesMap.get(message.getUid()));
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
            boolean showMessageInfo = _shouldShowTimestamp(position);
            holder.my_message_info.setVisibility(showMessageInfo ? View.VISIBLE : View.GONE);

            if (showMessageInfo) {
                Calendar push = Calendar.getInstance();
                push.setTimeInMillis(message.getPushDate());
                holder.date.setText(new SimpleDateFormat("hh:mm a").format(push.getTime()));

                holder.message_state.setVisibility(isMyMessage ? View.VISIBLE : View.GONE);
                if (isMyMessage) {
                    holder.message_state.setImageResource("seen".equals(message.getMessageState()) ? R.drawable.icon_done_all_round : R.drawable.icon_done_round);
                    holder.message_state.setColorFilter("seen".equals(message.getMessageState()) ? _context.getResources().getColor(R.color.colorPrimary) : 0xFF424242, PorterDuff.Mode.SRC_ATOP);
                }
            }
        }

        if (holder.mProfileCard != null) {
            holder.mProfileCard.setVisibility(View.GONE);
        }

        if (holder.mRepliedMessageLayout != null) {
            if (message.getRepliedMessage() != null) {
                holder.mRepliedMessageLayout.setVisibility(View.VISIBLE);
                ChatMessage repliedMessage = message.getRepliedMessage();

                if (isMyMessage) {
                    holder.mRepliedMessageLayout.setCardBackgroundColor(_context.getResources().getColor(R.color.md_theme_primaryContainer, null));
                } else {
                    holder.mRepliedMessageLayout.setCardBackgroundColor(_context.getResources().getColor(R.color.md_theme_surfaceContainerHigh, null));
                }

                if (repliedMessage.getAttachments() != null && !repliedMessage.getAttachments().isEmpty()) {
                    holder.mRepliedMessageLayoutImage.setVisibility(View.VISIBLE);
                    String imageUrl = CloudinaryConfig.buildReplyPreviewUrl(repliedMessage.getAttachments().get(0).getPublicId());
                    int cornerRadius = (int) _context.getResources().getDimension(R.dimen.reply_preview_corner_radius);
                    Glide.with(_context).load(imageUrl).placeholder(R.drawable.ph_imgbluredsqure).error(R.drawable.ph_imgbluredsqure).transform(new RoundedCorners(cornerRadius)).into(holder.mRepliedMessageLayoutImage);
                } else {
                    holder.mRepliedMessageLayoutImage.setVisibility(View.GONE);
                }

                String username = repliedMessage.getUid().equals(myUid) ? firstUserName : secondUserName;
                holder.mRepliedMessageLayoutUsername.setText(username);
                holder.mRepliedMessageLayoutUsername.setTextColor(isMyMessage ? _context.getResources().getColor(R.color.md_theme_onPrimaryContainer, null) : _context.getResources().getColor(R.color.md_theme_onSurface, null));

                holder.mRepliedMessageLayoutMessage.setText(repliedMessage.getMessageText());
                holder.mRepliedMessageLayoutMessage.setTextColor(isMyMessage ? _context.getResources().getColor(R.color.md_theme_onPrimaryContainer, null) : _context.getResources().getColor(R.color.md_theme_onSurfaceVariant, null));
                holder.mRepliedMessageLayoutMessage.setVisibility(repliedMessage.getMessageText().isEmpty() ? View.GONE : View.VISIBLE);

                android.graphics.drawable.GradientDrawable leftBarDrawable = new android.graphics.drawable.GradientDrawable();
                leftBarDrawable.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                leftBarDrawable.setColor(_context.getResources().getColor(R.color.md_theme_primary, null));
                int leftBarRadius = (int) _context.getResources().getDimension(R.dimen.left_bar_corner_radius);
                leftBarDrawable.setCornerRadius(leftBarRadius);
                holder.mRepliedMessageLayoutLeftBar.setBackground(leftBarDrawable);

                View.OnClickListener clickListener = v -> listener.scrollToMessage(repliedMessage.getKey());
                holder.mRepliedMessageLayout.setOnClickListener(clickListener);
                holder.mRepliedMessageLayoutMessage.setOnClickListener(clickListener);
                holder.mRepliedMessageLayoutImage.setOnClickListener(clickListener);
            } else {
                holder.mRepliedMessageLayout.setVisibility(View.GONE);
            }
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

            // Rounded ripple foreground to match bubble corners
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

        if (!isMyMessage && "sended".equals(message.getMessageState())) {
            listener.markMessageAsSeen(message);
        }
        
        // Consolidated long click listener for the message context menu.
        View.OnLongClickListener longClickListener = v -> {
            Log.d(TAG, "Long click detected on view: " + v.getClass().getSimpleName() + " at position: " + position);
            if (listener != null) {
                listener.performHapticFeedback();
            }
            // Use the message bubble (messageBG) as the anchor for the popup if it exists.
            View anchor = holder.messageBG != null ? holder.messageBG : holder.itemView;
            listener.showMessageOverviewPopup(anchor, _data.get(position));
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
        ChatMessage message = _data.get(position);
        holder.message_text.setVisibility(View.VISIBLE);
        com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, message.getMessageText());
    }

    private void bindMediaViewHolder(MediaViewHolder holder, int position) {
        bindCommonMessageProperties(holder, position);
        ChatMessage message = _data.get(position);
        String msgText = message.getMessageText();

        if (holder.message_text != null) {
            holder.message_text.setVisibility(msgText.isEmpty() ? View.GONE : View.VISIBLE);
            if (!msgText.isEmpty()) {
                com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, msgText);
            }
        }

        if (message.getAttachments() == null || message.getAttachments().isEmpty()) {
            if (holder.mediaGridLayout != null) holder.mediaGridLayout.setVisibility(View.GONE);
            if (holder.mediaCarouselContainer != null) holder.mediaCarouselContainer.setVisibility(View.GONE);
            if (msgText.isEmpty() && holder.message_text != null) {
                holder.message_text.setVisibility(View.VISIBLE);
                holder.message_text.setText("Media message");
            }
            return;
        }

        boolean useCarousel = message.getAttachments().size() >= 3;

        if (useCarousel && holder.mediaCarouselContainer != null && holder.mediaCarouselRecyclerView != null) {
            holder.mediaGridLayout.setVisibility(View.GONE);
            holder.mediaCarouselContainer.setVisibility(View.VISIBLE);
            setupCarouselLayout(holder, message.getAttachments());
        } else if (holder.mediaGridLayout != null) {
            holder.mediaCarouselContainer.setVisibility(View.GONE);
            holder.mediaGridLayout.setVisibility(View.VISIBLE);
            setupGridLayout(holder, message.getAttachments());
        }
    }

    private void setupCarouselLayout(MediaViewHolder holder, List<Attachment> attachments) {
        // ... (implementation remains the same, but with List<Attachment>)
    }

    private void setupGridLayout(MediaViewHolder holder, List<Attachment> attachments) {
        // ... (implementation remains the same, but with List<Attachment>)
    }

    private void openImageGallery(List<Attachment> attachments, int position) {
        if (_context != null) {
            Intent intent = new Intent(_context, ImageGalleryActivity.class);
            intent.putParcelableArrayListExtra("attachments_parcelable", new ArrayList<>(attachments));
            intent.putExtra("position", position);
            _context.startActivity(intent);
            if (_context instanceof Activity) {
                ((Activity) _context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left);
            }
        }
    }

    private void openImageGalleryTyped(ArrayList<Attachment> attachments, int position) {
        // This method can be removed, as openImageGallery now handles typed attachments
    }

    private ImageView createImageView(Attachment attachment, int width, boolean adjustBounds, int position, List<Attachment> attachments) {
        // ... (implementation remains the same, but with Attachment)
    }

    private void bindVideoViewHolder(VideoViewHolder holder, int position) {
        bindCommonMessageProperties(holder, position);
        ChatMessage message = _data.get(position);
        String msgText = message.getMessageText();
        holder.message_text.setVisibility(msgText.isEmpty() ? View.GONE : View.VISIBLE);
        if (!msgText.isEmpty()) com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, msgText);

        if (message.getAttachments() != null && !message.getAttachments().isEmpty()) {
            String videoUrl = message.getAttachments().get(0).getUrl();
            if (holder.videoThumbnail != null) Glide.with(_context).load(videoUrl).into(holder.videoThumbnail);

            if (holder.videoContainerCard != null) {
                holder.videoContainerCard.setOnClickListener(v -> listener.openUrl(videoUrl));
            } else {
                holder.itemView.setOnClickListener(v -> listener.openUrl(videoUrl));
            }
        }
    }

    private void bindTypingViewHolder(TypingViewHolder holder, int position) {
        android.graphics.drawable.GradientDrawable bubbleDrawable = new android.graphics.drawable.GradientDrawable();
        bubbleDrawable.setColor(Color.TRANSPARENT);
        if (holder.messageBG != null) holder.messageBG.setBackground(bubbleDrawable);

        if (holder.mProfileImage != null) {
            if (secondUserAvatarUrl != null && !secondUserAvatarUrl.isEmpty() && !secondUserAvatarUrl.equals("null_banned")) {
                Glide.with(_context).load(Uri.parse(secondUserAvatarUrl)).into(holder.mProfileImage);
            } else if ("null_banned".equals(secondUserAvatarUrl)) {
                holder.mProfileImage.setImageResource(R.drawable.banned_avatar);
            } else {
                holder.mProfileImage.setImageResource(R.drawable.avatar);
            }
        }
        if (holder.mProfileCard != null) holder.mProfileCard.setVisibility(View.VISIBLE);
    }

    private void bindLinkPreviewViewHolder(LinkPreviewViewHolder holder, int position) {
        bindCommonMessageProperties(holder, position);
        ChatMessage message = _data.get(position);
        holder.message_text.setVisibility(View.VISIBLE);
        com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, message.getMessageText());

        String urlToPreview = LinkPreviewUtil.extractUrl(message.getMessageText());
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
        // The progress bar is displayed, and the loading is handled by the
        // scroll listener in ChatActivity. No action needed here.
    }

    private void bindVoiceMessageViewHolder(VoiceMessageViewHolder holder, int position) {
        bindCommonMessageProperties(holder, position);
        ChatMessage message = _data.get(position);
        String audioUrl = message.getAttachments().get(0).getUrl();
        long duration = message.getAttachments().get(0).getDuration();

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
            holder.duration.setText(_getDurationString(mp.getDuration()));
            holder.seekBar.setMax(mp.getDuration());
            holder.handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (holder.mediaPlayer != null) {
                        try {
                            holder.seekBar.setProgress(holder.mediaPlayer.getCurrentPosition());
                            holder.handler.postDelayed(this, 1000);
                        } catch (IllegalStateException e) {
                            // mediaplayer is not ready yet, just ignore
                        }
                    }
                }
            }, 1000);
        });

        holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
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
        if (position == 0) return true;
        return !_data.get(position).getUid().equals(_data.get(position - 1).getUid());
    }

    private boolean isLastMessage(int position) {
        if (position == _data.size() - 1) return true;
        return !_data.get(position).getUid().equals(_data.get(position + 1).getUid());
    }

    private String _getDurationString(long durationInMillis) {
        long seconds = durationInMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
    }

    private boolean _shouldShowTimestamp(int position) {
        if (position == _data.size() - 1) return true;
        if (position < _data.size() - 1) {
            long currentTime = _data.get(position).getPushDate();
            long nextTime = _data.get(position + 1).getPushDate();
            return Math.abs(currentTime - nextTime) > 300000;
        }
        return false;
    }
}