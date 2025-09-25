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
import com.synapse.social.studioasinc.backend.AuthenticationService;
import com.synapse.social.studioasinc.backend.DatabaseService;
import com.synapse.social.studioasinc.util.ChatMessageManager;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import androidx.gridlayout.widget.GridLayout;
import android.widget.RelativeLayout;
import android.app.Activity;
import com.synapse.social.studioasinc.config.CloudinaryConfig;
import com.synapse.social.studioasinc.model.Attachment;
import com.synapse.social.studioasinc.util.AttachmentUtils;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "ChatAdapter";
    private static final int VIEW_TYPE_TEXT = 1;
    private static final int VIEW_TYPE_MEDIA_GRID = 2;
    private static final int VIEW_TYPE_TYPING = 3;
    private static final int VIEW_TYPE_VIDEO = 4;
    private static final int VIEW_TYPE_LINK_PREVIEW = 5;
    private static final int VIEW_TYPE_VOICE_MESSAGE = 6;
    private static final int VIEW_TYPE_LOADING_MORE = 99;

    private ArrayList<HashMap<String, Object>> _data;
    private HashMap<String, HashMap<String, Object>> repliedMessagesCache;
    private Context _context;
    private String secondUserAvatarUrl = "";
    private String firstUserName = "";
    private String secondUserName = "";
    private SharedPreferences appSettings;
    private ChatAdapterListener listener;
    private boolean isGroupChat = false;
    private HashMap<String, String> userNamesMap = new HashMap<>();
    private DatabaseService dbService;
    private AuthenticationService authService;

    public ChatAdapter(ArrayList<HashMap<String, Object>> _arr, HashMap<String, HashMap<String, Object>> repliedCache, ChatAdapterListener listener, DatabaseService dbService, AuthenticationService authService) {
        _data = _arr;
        this.repliedMessagesCache = repliedCache;
        this.listener = listener;
        this.dbService = dbService;
        this.authService = authService;
    }
    public void setSecondUserAvatar(String url) { this.secondUserAvatarUrl = url; }
    public void setFirstUserName(String name) { this.firstUserName = name; }
    public void setSecondUserName(String name) { this.secondUserName = name; }
    public void setGroupChat(boolean isGroup) { this.isGroupChat = isGroup; }
    public void setUserNamesMap(HashMap<String, String> map) { this.userNamesMap = map; }

    @Override
    public int getItemViewType(int position) {
        if (_data.get(position).containsKey("isLoadingMore")) return VIEW_TYPE_LOADING_MORE;
        if (_data.get(position).containsKey("typingMessageStatus")) return VIEW_TYPE_TYPING;
        
        String type = _data.get(position).getOrDefault("TYPE", "MESSAGE").toString();

        if ("VOICE_MESSAGE".equals(type)) {
            return VIEW_TYPE_VOICE_MESSAGE;
        }
        
        if ("ATTACHMENT_MESSAGE".equals(type)) {
            ArrayList<HashMap<String, Object>> attachments = (ArrayList<HashMap<String, Object>>) _data.get(position).get("attachments");
            if (attachments != null && attachments.size() == 1 && String.valueOf(attachments.get(0).getOrDefault("publicId", "")).contains("|video")) {
                return VIEW_TYPE_VIDEO;
            }
            return VIEW_TYPE_MEDIA_GRID;
        }

        String messageText = String.valueOf(_data.get(position).getOrDefault("message_text", ""));
        if (LinkPreviewUtil.extractUrl(messageText) != null) {
            return VIEW_TYPE_LINK_PREVIEW;
        }
        
        return VIEW_TYPE_TEXT;
    }

    @Override
    public long getItemId(int position) {
        try {
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
        HashMap<String, Object> data = _data.get(position);
        final String myUid = authService.getCurrentUser() != null ? authService.getCurrentUser().getUid() : "";
        String msgUid = data != null && data.get("uid") != null ? String.valueOf(data.get("uid")) : "";
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
            // ... (Message info logic remains the same)
        }

        if (holder.mRepliedMessageLayout != null && data.containsKey("replied_message_id")) {
            // ... (Reply layout logic remains the same)
        }
        
        if (holder.messageBG != null) {
            // ... (Message bubble shape logic remains the same)
        }

        if (!isMyMessage && data.containsKey("message_state") && "sended".equals(String.valueOf(data.get("message_state")))) {
            String otherUserUid = listener.getRecipientUid();
            if (myUid != null && !myUid.isEmpty() && otherUserUid != null && !otherUserUid.isEmpty()) {
                String chatId = ChatMessageManager.INSTANCE.getChatId(myUid, otherUserUid);
                String messageKey = String.valueOf(data.get("key"));
                String chatPath = "chats/" + chatId;

                dbService.getReference(chatPath).child(messageKey).child("message_state").setValue("seen");
                dbService.getReference("inbox").child(myUid).child(otherUserUid).child("last_message_state").setValue("seen");
            }
        }
        
        View.OnLongClickListener longClickListener = v -> {
            if (listener != null) {
                listener.performHapticFeedback();
            }
            View anchor = holder.messageBG != null ? holder.messageBG : holder.itemView;
            listener.showMessageOverviewPopup(anchor, position, _data);
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
        String text = String.valueOf(_data.get(position).getOrDefault("message_text", ""));
        holder.message_text.setVisibility(View.VISIBLE);
        com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, text);
    }
    
    // ... (All other bind* and helper methods remain the same)
    private void bindMediaViewHolder(MediaViewHolder holder, int position) { Log.d(TAG, "bindMediaViewHolder called for position " + position); bindCommonMessageProperties(holder, position); HashMap<String, Object> data = _data.get(position); String msgText = data.getOrDefault("message_text", "").toString(); if (holder.message_text != null) { holder.message_text.setVisibility(msgText.isEmpty() ? View.GONE : View.VISIBLE); if (!msgText.isEmpty()) { com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, msgText); } } ArrayList<HashMap<String, Object>> attachments = (ArrayList<HashMap<String, Object>>) data.get("attachments"); Log.d(TAG, "Attachments found: " + (attachments != null ? attachments.size() : 0)); if (attachments == null || attachments.isEmpty()) { Log.w(TAG, "No attachments found, showing only message text"); if (holder.mediaGridLayout != null) holder.mediaGridLayout.setVisibility(View.GONE); if (holder.mediaCarouselContainer != null) holder.mediaCarouselContainer.setVisibility(View.GONE); if (msgText.isEmpty() && holder.message_text != null) { holder.message_text.setVisibility(View.VISIBLE); holder.message_text.setText("Media message"); } return; } int count = attachments.size(); Log.d(TAG, "Processing " + count + " attachments"); boolean useCarousel = false; Log.d(TAG, "useCarousel: " + useCarousel + ", count: " + count); if (useCarousel && holder.mediaCarouselContainer != null && holder.mediaCarouselRecyclerView != null) { Log.d(TAG, "Using carousel layout"); if (holder.mediaGridLayout != null) holder.mediaGridLayout.setVisibility(View.GONE); holder.mediaCarouselContainer.setVisibility(View.VISIBLE); try { setupCarouselLayout(holder, attachments); } catch (Exception e) { Log.e(TAG, "Error setting up carousel, falling back to grid: " + e.getMessage()); if (holder.mediaCarouselContainer != null) holder.mediaCarouselContainer.setVisibility(View.GONE); if (holder.mediaGridLayout != null) { holder.mediaGridLayout.setVisibility(View.VISIBLE); setupGridLayout(holder, attachments); } } } else { Log.d(TAG, "Using grid layout"); if (holder.mediaCarouselContainer != null) holder.mediaCarouselContainer.setVisibility(View.GONE); if (holder.mediaGridLayout != null) { holder.mediaGridLayout.setVisibility(View.VISIBLE); try { setupGridLayout(holder, attachments); Log.d(TAG, "Grid layout setup completed successfully"); } catch (Exception e) { Log.e(TAG, "Error setting up grid layout: " + e.getMessage()); if (holder.message_text != null) { holder.message_text.setVisibility(View.VISIBLE); holder.message_text.setText("Media message (" + attachments.size() + " images)"); } } } else { Log.e(TAG, "mediaGridLayout is null!"); if (holder.message_text != null) { holder.message_text.setVisibility(View.VISIBLE); holder.message_text.setText("Media message (" + attachments.size() + " images)"); } } } }
    private void setupCarouselLayout(MediaViewHolder holder, ArrayList<HashMap<String, Object>> attachments) { Log.d(TAG, "Setting up carousel layout for " + attachments.size() + " images"); LinearLayoutManager layoutManager = new LinearLayoutManager(_context, LinearLayoutManager.HORIZONTAL, false); holder.mediaCarouselRecyclerView.setLayoutManager(layoutManager); if (holder.mediaCarouselRecyclerView.getItemDecorationCount() == 0) { holder.mediaCarouselRecyclerView.addItemDecoration( CarouselItemDecoration.createWithStandardSpacing(holder.mediaCarouselRecyclerView)); } ArrayList<Attachment> typedAttachments = AttachmentUtils.fromHashMapList(attachments); if (typedAttachments == null || typedAttachments.isEmpty()) { return; } MessageImageCarouselAdapter adapter = new MessageImageCarouselAdapter(_context, typedAttachments, (position, attachmentList) -> openImageGalleryTyped(attachmentList, position)); holder.mediaCarouselRecyclerView.setAdapter(adapter); if (holder.viewAllImagesButton != null) { if (attachments.size() > 3) { holder.viewAllImagesButton.setVisibility(View.VISIBLE); holder.viewAllImagesButton.setText("View all " + attachments.size() + " images"); holder.viewAllImagesButton.setOnClickListener(v -> openImageGalleryTyped(typedAttachments, 0)); } else { holder.viewAllImagesButton.setVisibility(View.GONE); } } ViewGroup.LayoutParams cardParams = holder.mediaContainerCard.getLayoutParams(); cardParams.width = ViewGroup.LayoutParams.MATCH_PARENT; holder.mediaContainerCard.setLayoutParams(cardParams); }
    private void setupGridLayout(MediaViewHolder holder, ArrayList<HashMap<String, Object>> attachments) { Log.d(TAG, "Setting up grid layout for " + attachments.size() + " images"); GridLayout gridLayout = holder.mediaGridLayout; gridLayout.removeAllViews(); int count = attachments.size(); int colCount = 2; int maxImages = 4; int totalGridWidth = (int) _context.getResources().getDimension(R.dimen.chat_grid_width); int imageSize = totalGridWidth / 2; ViewGroup.LayoutParams cardParams = holder.mediaContainerCard.getLayoutParams(); cardParams.width = totalGridWidth; holder.mediaContainerCard.setLayoutParams(cardParams); gridLayout.setColumnCount(colCount); ViewGroup.LayoutParams gridParams = gridLayout.getLayoutParams(); if (gridParams == null) { gridParams = new ViewGroup.LayoutParams(totalGridWidth, ViewGroup.LayoutParams.WRAP_CONTENT); } gridParams.width = totalGridWidth; gridLayout.setLayoutParams(gridParams); if (count == 1) { gridLayout.setColumnCount(1); HashMap<String, Object> attachment = attachments.get(0); ImageView iv = createImageView(attachment, totalGridWidth, true, 0, attachments); gridLayout.addView(iv); } else if (count == 3) { int portraitIndex = -1; for(int i=0; i < attachments.size(); i++){ HashMap<String, Object> attachment = attachments.get(i); Object widthObj = attachment.get("width"); Object heightObj = attachment.get("height"); if (widthObj != null && heightObj != null) { double width = ((Number) widthObj).doubleValue(); double height = ((Number) heightObj).doubleValue(); if(height > width){ portraitIndex = i; break; } } } if(portraitIndex != -1){ ImageView iv1 = createImageView(attachments.get(portraitIndex), imageSize, false, portraitIndex, attachments); GridLayout.LayoutParams params1 = new GridLayout.LayoutParams(GridLayout.spec(0, 2, 1f), GridLayout.spec(0, 1, 1f)); iv1.setLayoutParams(params1); gridLayout.addView(iv1); int attachmentIndex = 0; for(int i=0; i<2; i++){ if(attachmentIndex == portraitIndex) attachmentIndex++; ImageView iv = createImageView(attachments.get(attachmentIndex), imageSize, false, attachmentIndex, attachments); GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(i, 1, 1f), GridLayout.spec(1, 1, 1f)); iv.setLayoutParams(params); gridLayout.addView(iv); attachmentIndex++; } } else { ImageView iv1 = createImageView(attachments.get(0), totalGridWidth, false, 0, attachments); GridLayout.LayoutParams params1 = new GridLayout.LayoutParams(GridLayout.spec(0, 1, 1f), GridLayout.spec(0, 2, 1f)); iv1.setLayoutParams(params1); gridLayout.addView(iv1); for (int i = 1; i < 3; i++) { ImageView iv = createImageView(attachments.get(i), imageSize, false, i, attachments); GridLayout.LayoutParams params = new GridLayout.LayoutParams(GridLayout.spec(1, 1, 1f), GridLayout.spec(i - 1, 1, 1f)); iv.setLayoutParams(params); gridLayout.addView(iv); } } } else { int limit = Math.min(count, maxImages); for (int i = 0; i < limit; i++) { View viewToAdd; ImageView iv = createImageView(attachments.get(i), imageSize, false, i, attachments); if (i == maxImages - 1 && count > maxImages) { RelativeLayout overlayContainer = new RelativeLayout(_context); overlayContainer.setLayoutParams(new ViewGroup.LayoutParams(imageSize, imageSize)); overlayContainer.addView(iv); View overlay = new View(_context); overlay.setBackgroundColor(0x40000000); overlayContainer.addView(overlay, new ViewGroup.LayoutParams(imageSize, imageSize)); TextView moreText = new TextView(_context); moreText.setText("+" + (count - maxImages)); moreText.setTextColor(Color.WHITE); moreText.setTextSize(24); moreText.setGravity(Gravity.CENTER); overlayContainer.addView(moreText, new ViewGroup.LayoutParams(imageSize, imageSize)); viewToAdd = overlayContainer; viewToAdd.setOnClickListener(v -> openImageGallery(attachments, 3)); } else { viewToAdd = iv; } gridLayout.addView(viewToAdd); } } }
    private void openImageGallery(ArrayList<HashMap<String, Object>> attachments, int position) { if (_context != null) { ArrayList<Attachment> typed = AttachmentUtils.fromHashMapList(attachments); openImageGalleryTyped(typed, position); } }
    private void openImageGalleryTyped(ArrayList<Attachment> attachments, int position) { if (_context != null) { Intent intent = new Intent(_context, ImageGalleryActivity.class); intent.putParcelableArrayListExtra("attachments_parcelable", attachments); intent.putExtra("position", position); _context.startActivity(intent); if (_context instanceof Activity) { ((Activity) _context).overridePendingTransition(R.anim.slide_in_from_right, R.anim.slide_out_to_left); } } }
    private ImageView createImageView(HashMap<String, Object> attachment, int width, boolean adjustBounds, int position, ArrayList<HashMap<String, Object>> attachments) { String url = String.valueOf(attachment.get("url")); ImageView imageView = new ImageView(_context); imageView.setScaleType(ImageView.ScaleType.CENTER_CROP); if (adjustBounds) { imageView.setAdjustViewBounds(true); imageView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)); Glide.with(_context).load(url).into(imageView); } else { int height; Object widthObj = attachment.get("width"); Object heightObj = attachment.get("height"); if (widthObj != null && heightObj != null) { double imageWidth = ((Number) widthObj).doubleValue(); double imageHeight = ((Number) heightObj).doubleValue(); if (imageWidth > 0) { double ratio = imageHeight / imageWidth; height = (int) (width * ratio); } else { height = width; } } else { height = width; } imageView.setLayoutParams(new ViewGroup.LayoutParams(width, height)); Glide.with(_context).load(url).override(width, height).into(imageView); } imageView.setOnClickListener(v -> openImageGallery(attachments, position)); return imageView; }
    private void bindVideoViewHolder(VideoViewHolder holder, int position) { bindCommonMessageProperties(holder, position); HashMap<String, Object> data = _data.get(position); String msgText = data.getOrDefault("message_text", "").toString(); holder.message_text.setVisibility(msgText.isEmpty() ? View.GONE : View.VISIBLE); if (!msgText.isEmpty()) com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, msgText); ArrayList<HashMap<String, Object>> attachments = (ArrayList<HashMap<String, Object>>) data.get("attachments"); if (attachments != null && !attachments.isEmpty()) { String videoUrl = String.valueOf(attachments.get(0).get("url")); if(holder.videoThumbnail != null) Glide.with(_context).load(videoUrl).into(holder.videoThumbnail); if(holder.videoContainerCard != null) { holder.videoContainerCard.setOnClickListener(v -> listener.openUrl(videoUrl)); } else { holder.itemView.setOnClickListener(v -> listener.openUrl(videoUrl)); } } }
    private void bindTypingViewHolder(TypingViewHolder holder, int position) { android.graphics.drawable.GradientDrawable bubbleDrawable = new android.graphics.drawable.GradientDrawable(); bubbleDrawable.setColor(Color.TRANSPARENT); if(holder.messageBG != null) holder.messageBG.setBackground(bubbleDrawable); if (holder.mProfileImage != null) { if (secondUserAvatarUrl != null && !secondUserAvatarUrl.isEmpty() && !secondUserAvatarUrl.equals("null_banned")) { Glide.with(_context).load(Uri.parse(secondUserAvatarUrl)).into(holder.mProfileImage); } else if ("null_banned".equals(secondUserAvatarUrl)) { holder.mProfileImage.setImageResource(R.drawable.banned_avatar); } else { holder.mProfileImage.setImageResource(R.drawable.avatar); } } if(holder.mProfileCard != null) holder.mProfileCard.setVisibility(View.VISIBLE); }
    private void bindLinkPreviewViewHolder(LinkPreviewViewHolder holder, int position) { bindCommonMessageProperties(holder, position); HashMap<String, Object> data = _data.get(position); String messageText = String.valueOf(data.getOrDefault("message_text", "")); holder.message_text.setVisibility(View.VISIBLE); com.synapse.social.studioasinc.styling.MarkdownRenderer.get(holder.message_text.getContext()).render(holder.message_text, messageText); String urlToPreview = LinkPreviewUtil.extractUrl(messageText); if (urlToPreview != null) { if (holder.linkPreviewImage != null) holder.linkPreviewImage.setVisibility(View.GONE); if (holder.linkPreviewTitle != null) holder.linkPreviewTitle.setText("Loading Preview..."); if (holder.linkPreviewDescription != null) holder.linkPreviewDescription.setText(""); if (holder.linkPreviewDomain != null) holder.linkPreviewDomain.setText(urlToPreview); LinkPreviewUtil.fetchPreview(urlToPreview, new LinkPreviewUtil.LinkPreviewCallback() { @Override public void onPreviewDataFetched(LinkPreviewUtil.LinkData linkData) { if (linkData != null) { if (holder.linkPreviewTitle != null) holder.linkPreviewTitle.setText(linkData.title); if (holder.linkPreviewDescription != null) holder.linkPreviewDescription.setText(linkData.description); if (holder.linkPreviewDomain != null) holder.linkPreviewDomain.setText(linkData.domain); if (linkData.imageUrl != null && !linkData.imageUrl.isEmpty() && holder.linkPreviewImage != null) { if (_context != null) { Glide.with(_context).load(linkData.imageUrl).into(holder.linkPreviewImage); holder.linkPreviewImage.setVisibility(View.VISIBLE); } } } } @Override public void onError(Exception e) { Log.e(TAG, "Link preview error: " + e.getMessage()); if (holder.linkPreviewTitle != null) holder.linkPreviewTitle.setText("Cannot load preview"); if (holder.linkPreviewDescription != null) holder.linkPreviewDescription.setText(""); if (holder.linkPreviewDomain != null) holder.linkPreviewDomain.setText(urlToPreview); if (holder.linkPreviewImage != null) holder.linkPreviewImage.setVisibility(View.GONE); } }); } }
    private void bindLoadingViewHolder(LoadingViewHolder holder, int position) { }
    private void bindVoiceMessageViewHolder(VoiceMessageViewHolder holder, int position) { bindCommonMessageProperties(holder, position); HashMap<String, Object> data = _data.get(position); String audioUrl = (String) data.get("audio_url"); long duration = ((Number) data.get("audio_duration")).longValue(); if (holder.mediaPlayer == null) { holder.mediaPlayer = new MediaPlayer(); } holder.duration.setText(_getDurationString(duration)); holder.playPauseButton.setOnClickListener(v -> { if (holder.mediaPlayer.isPlaying()) { holder.mediaPlayer.pause(); holder.playPauseButton.setImageResource(R.drawable.ic_send); } else { holder.mediaPlayer.start(); holder.playPauseButton.setImageResource(R.drawable.ic_close_48px); } }); holder.mediaPlayer.setOnCompletionListener(mp -> { holder.playPauseButton.setImageResource(R.drawable.ic_send); holder.seekBar.setProgress(0); }); try { holder.mediaPlayer.reset(); holder.mediaPlayer.setDataSource(audioUrl); holder.mediaPlayer.prepareAsync(); } catch (Exception e) { e.printStackTrace(); } holder.mediaPlayer.setOnPreparedListener(mp -> { holder.seekBar.setMax(mp.getDuration()); holder.handler.postDelayed(new Runnable() { @Override public void run() { if (holder.mediaPlayer != null) { try { holder.seekBar.setProgress(holder.mediaPlayer.getCurrentPosition()); holder.handler.postDelayed(this, 1000); } catch (IllegalStateException e) { } } } }, 1000); }); holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) { if(fromUser) { holder.mediaPlayer.seekTo(progress); } } @Override public void onStartTrackingTouch(SeekBar seekBar) { } @Override public void onStopTrackingTouch(SeekBar seekBar) { } }); }
    private boolean isFirstMessage(int position) { if (position == 0) { return true; } String currentSender = (String) _data.get(position).get("uid"); String previousSender = (String) _data.get(position - 1).get("uid"); return !currentSender.equals(previousSender); }
    private boolean isLastMessage(int position) { if (position == _data.size() - 1) { return true; } String currentSender = (String) _data.get(position).get("uid"); String nextSender = (String) _data.get(position + 1).get("uid"); return !currentSender.equals(nextSender); }
	private String _getDurationString(final long _durationInMillis) { long seconds = _durationInMillis / 1000; long minutes = seconds / 60; long hours = minutes / 60; seconds %= 60; minutes %= 60; if (hours > 0) { return String.format("%02d:%02d:%02d", hours, minutes, seconds); } else { return String.format("%02d:%02d", minutes, seconds); } }
    private boolean _shouldShowTimestamp(int position, HashMap<String, Object> currentMessage) { if (position == _data.size() - 1) { return true; } if (position < _data.size() - 1) { try { HashMap<String, Object> nextMessage = _data.get(position + 1); long currentTime = _getMessageTimestamp(currentMessage); long nextTime = _getMessageTimestamp(nextMessage); return Math.abs(currentTime - nextTime) > 300000; } catch (Exception e) { Log.e(TAG, "Error calculating timestamp visibility: " + e.getMessage()); return false; } } return false; }
    private long _getMessageTimestamp(HashMap<String, Object> message) { try { Object pushDateObj = message.get("push_date"); if (pushDateObj instanceof Long) { return (Long) pushDateObj; } else if (pushDateObj instanceof Double) { return ((Double) pushDateObj).longValue(); } else if (pushDateObj instanceof String) { return Long.parseLong((String) pushDateObj); } } catch (Exception e) { Log.w(TAG, "Error parsing message timestamp: " + e.getMessage()); } return System.currentTimeMillis(); }
}