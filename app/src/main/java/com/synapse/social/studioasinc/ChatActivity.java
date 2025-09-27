package com.synapse.social.studioasinc;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.service.studioasinc.AI.Gemini;
import com.synapse.social.studioasinc.util.ActivityResultHandler;
import com.synapse.social.studioasinc.util.ChatMessageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimerTask;
import java.util.stream.Collectors;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;

import static com.synapse.social.studioasinc.ChatConstants.*;

public class ChatActivity extends AppCompatActivity implements ChatAdapterListener, ChatInteractionListener, VoiceMessageHandler.VoiceMessageListener, ChatDataListener {

	private Handler recordHandler = new Handler();
	private Runnable recordRunnable;
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();

	private ProgressDialog SynapseLoadingDialog;
	private String SecondUserAvatar = "";
	String ReplyMessageID = "";
	private String FirstUserName = "";
	private String SecondUserName = "";
	private boolean is_group = false;
	private String path = "";
	private String AndroidDevelopersBlogURL = "";
	public final int REQ_CD_IMAGE_PICKER = 101;
	private ChatAdapter chatAdapter;
	private DatabaseReference chatMessagesRef;
	private DatabaseReference userRef;

	private HashMap<String, HashMap<String, Object>> repliedMessagesCache = new HashMap<>();
	private java.util.Set<String> messageKeys = new java.util.HashSet<>();
	private java.util.Set<String> locallyDeletedMessages = new java.util.HashSet<>();
	private ArrayList<HashMap<String, Object>> ChatMessagesList = new ArrayList<>();
	public ArrayList<HashMap<String, Object>> attactmentmap = new ArrayList<>();

	// UI Components
	private LinearLayout mMessageReplyLayout;
	private LinearLayout message_input_overall_container;
	private TextView blocked_txt;
	private ImageView topProfileLayoutProfileImage;
	private TextView topProfileLayoutStatus;
	private TextView topProfileLayoutUsername;
	private ImageView topProfileLayoutGenderBadge;
	private ImageView topProfileLayoutVerifiedBadge;
	private TextView noChatText;
	private RecyclerView ChatMessagesListRecycler;
	public RecyclerView rv_attacmentList;
	private TextView mMessageReplyLayoutBodyRightUsername;
	private TextView mMessageReplyLayoutBodyRightMessage;
	private FadeEditText message_et;

	// Services and Handlers
	private FirebaseAuth auth;
	private Vibrator vbr;
	private DatabaseReference blocklist = _firebase.getReference(SKYLINE_REF).child(BLOCKLIST_REF);
	private Gemini gemini;
	private AiFeatureHandler aiFeatureHandler;
	private ActivityResultHandler activityResultHandler;
	private ChatKeyboardHandler chatKeyboardHandler;
	private VoiceMessageHandler voiceMessageHandler;
	private ChatUIUpdater chatUIUpdater;
	private ChatScrollListener chatScrollListener;
	private AttachmentHandler attachmentHandler;
	private ChatDataHandler chatDataHandler;
	private MessageSendingHandler messageSendingHandler;
	private MessageInteractionHandler messageInteractionHandler;


	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		is_group = getIntent().getBooleanExtra("isGroup", false);
		if (is_group) {
			setContentView(R.layout.activity_chat_group);
		} else {
			setContentView(R.layout.activity_chat);
		}
		initializeViews();
		FirebaseApp.initializeApp(this);

		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
				|| ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
				|| ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO}, 1000);} else {
			initializeLogic();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1000) {
			initializeLogic();
		}
	}

	private void initializeViews() {
		mMessageReplyLayout = findViewById(R.id.mMessageReplyLayout);
		message_input_overall_container = findViewById(R.id.message_input_overall_container);
		blocked_txt = findViewById(R.id.blocked_txt);
		topProfileLayoutProfileImage = findViewById(R.id.topProfileLayoutProfileImage);
		topProfileLayoutStatus = findViewById(R.id.topProfileLayoutStatus);
		topProfileLayoutUsername = findViewById(R.id.topProfileLayoutUsername);
		topProfileLayoutGenderBadge = findViewById(R.id.topProfileLayoutGenderBadge);
		topProfileLayoutVerifiedBadge = findViewById(R.id.topProfileLayoutVerifiedBadge);
		noChatText = findViewById(R.id.noChatText);
		ChatMessagesListRecycler = findViewById(R.id.ChatMessagesListRecycler);
		rv_attacmentList = findViewById(R.id.rv_attacmentList);
		mMessageReplyLayoutBodyRightUsername = findViewById(R.id.mMessageReplyLayoutBodyRightUsername);
		mMessageReplyLayoutBodyRightMessage = findViewById(R.id.mMessageReplyLayoutBodyRightMessage);
		message_et = findViewById(R.id.message_et);

		ImageView back = findViewById(R.id.back);
		LinearLayout topProfileLayout = findViewById(R.id.topProfileLayout);
		ImageView ic_more = findViewById(R.id.ic_more);
		ImageView mMessageReplyLayoutBodyCancel = findViewById(R.id.mMessageReplyLayoutBodyCancel);
		LinearLayout message_input_outlined_round = findViewById(R.id.message_input_outlined_round);
		MaterialButton btn_sendMessage = findViewById(R.id.btn_sendMessage);

		auth = FirebaseAuth.getInstance();
		vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		back.setOnClickListener(_view -> onBackPressed());
		View.OnClickListener profileClickListener = v -> startActivityWithUid(ConversationSettingsActivity.class);
		topProfileLayout.setOnClickListener(profileClickListener);
		ic_more.setOnClickListener(profileClickListener);

		mMessageReplyLayoutBodyCancel.setOnClickListener(_view -> {
			ReplyMessageID = "null";
			chatUIUpdater.hideReplyUI();
			vbr.vibrate(48);
		});

		message_input_outlined_round.setOnClickListener(_view -> message_et.requestFocus());
		btn_sendMessage.setOnClickListener(_view -> {
			if (messageSendingHandler != null) {
				messageSendingHandler.sendButtonAction(message_et, ReplyMessageID, mMessageReplyLayout);
			}
		});
		btn_sendMessage.setOnLongClickListener(_view -> {
			if (aiFeatureHandler != null) {
				return aiFeatureHandler.handleSendButtonLongClick(ReplyMessageID);
			}
			return false;
		});
	}

	private void initializeLogic() {
		is_group = getIntent().getBooleanExtra("isGroup", false);
		SharedPreferences themePrefs = getSharedPreferences("theme", MODE_PRIVATE);
		String backgroundUrl = themePrefs.getString("chat_background_url", null);
		if (backgroundUrl != null && !backgroundUrl.isEmpty()) {
			Glide.with(this).load(backgroundUrl).into((ImageView) findViewById(R.id.ivBGimage));
		}

		ReplyMessageID = "null";
		path = "";
		LinearLayoutManager ChatRecyclerLayoutManager = new LinearLayoutManager(this);
		ChatRecyclerLayoutManager.setReverseLayout(false);
		ChatRecyclerLayoutManager.setStackFromEnd(true);
		ChatMessagesListRecycler.setLayoutManager(ChatRecyclerLayoutManager);

		chatAdapter = new ChatAdapter(ChatMessagesList, repliedMessagesCache, this);
		chatAdapter.setHasStableIds(true);
		ChatMessagesListRecycler.setAdapter(chatAdapter);
		ChatMessagesListRecycler.setItemViewCacheSize(50);

		String currentUserUid = auth.getCurrentUser().getUid();
		String otherUserUid = getIntent().getStringExtra(UID_KEY);
		if (is_group) {
			chatMessagesRef = _firebase.getReference("skyline/group-chats").child(otherUserUid);
		} else {
			String chatID = ChatMessageManager.INSTANCE.getChatId(currentUserUid, otherUserUid);
			chatMessagesRef = _firebase.getReference(CHATS_REF).child(chatID);
		}
		userRef = _firebase.getReference(SKYLINE_REF).child(USERS_REF).child(otherUserUid);

		chatDataHandler = new ChatDataHandler(this, _firebase, auth, chatMessagesRef, userRef, blocklist, is_group, otherUserUid);

		chatUIUpdater = new ChatUIUpdater(
				this, noChatText, ChatMessagesListRecycler, topProfileLayoutProfileImage,
				topProfileLayoutUsername, topProfileLayoutStatus, topProfileLayoutGenderBadge,
				topProfileLayoutVerifiedBadge, mMessageReplyLayout, mMessageReplyLayoutBodyRightUsername,
				mMessageReplyLayoutBodyRightMessage, auth
		);

		messageSendingHandler = new MessageSendingHandler(
				this, auth, _firebase, ChatMessagesList, attactmentmap, chatAdapter,
				ChatMessagesListRecycler, rv_attacmentList, (RelativeLayout) findViewById(R.id.attachmentLayoutListHolder),
				messageKeys, otherUserUid, FirstUserName, is_group
		);

		gemini = new Gemini.Builder(this)
				.model("gemini-1.5-flash").responseType("text").tone("friendly").size("medium")
				.maxTokens(2000).temperature(0.8).showThinking(true).thinkingText("Analyzing your request...")
				.systemInstruction("Your name is ChatBot, help users with their questions")
				.responseTextView(message_et).build();

		aiFeatureHandler = new AiFeatureHandler(
				this, gemini, message_et, ChatMessagesList, auth, SecondUserName,
				mMessageReplyLayoutBodyRightUsername, mMessageReplyLayoutBodyRightMessage
		);

		messageInteractionHandler = new MessageInteractionHandler(
				this, this, auth, _firebase, ChatMessagesList, ChatMessagesListRecycler,
				vbr, aiFeatureHandler, FirstUserName, SecondUserName
		);

		activityResultHandler = new ActivityResultHandler(this);
		_setupSwipeToReply();

		LinearLayout message_input_outlined_round = findViewById(R.id.message_input_outlined_round);
		chatKeyboardHandler = new ChatKeyboardHandler(
				this, message_et, (LinearLayout) findViewById(R.id.toolContainer),
				(MaterialButton) findViewById(R.id.btn_sendMessage), message_input_outlined_round,
				message_input_overall_container, auth
		);
		chatKeyboardHandler.setup();

		voiceMessageHandler = new VoiceMessageHandler(this, this);
		voiceMessageHandler.setupVoiceButton((ImageView) findViewById(R.id.btn_voice_message));

		chatScrollListener = new ChatScrollListener(this, ChatRecyclerLayoutManager);
		ChatMessagesListRecycler.addOnScrollListener(chatScrollListener);

		attachmentHandler = new AttachmentHandler(
				this, (RelativeLayout) findViewById(R.id.attachmentLayoutListHolder),
				rv_attacmentList, attactmentmap, (ImageView) findViewById(R.id.close_attachments_btn),
				(ImageView) findViewById(R.id.galleryBtn), auth
		);
		attachmentHandler.setup();

		chatDataHandler.loadInitialData();
	}

	@Override
	protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);
		activityResultHandler.handleResult(_requestCode, _resultCode, _data);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (auth.getCurrentUser() != null) {
			String chatID = ChatMessageManager.INSTANCE.getChatId(auth.getCurrentUser().getUid(), getIntent().getStringExtra(UID_KEY));
			_firebase.getReference(CHATS_REF).child(chatID).child(TYPING_MESSAGE_REF).removeValue();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (chatDataHandler != null) {
			chatDataHandler.attachListeners();
		}
		if (auth.getCurrentUser() != null) {
			String recipientUid = getIntent().getStringExtra("uid");
			PresenceManager.setChattingWith(auth.getCurrentUser().getUid(), recipientUid);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (chatDataHandler != null) {
			chatDataHandler.detachListeners();
		}
		if (auth.getCurrentUser() != null) {
			String chatID = ChatMessageManager.INSTANCE.getChatId(auth.getCurrentUser().getUid(), getIntent().getStringExtra(UID_KEY));
			_firebase.getReference(CHATS_REF).child(chatID).child(TYPING_MESSAGE_REF).removeValue();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (auth.getCurrentUser() != null) {
			try {
				String chatID = ChatMessageManager.INSTANCE.getChatId(auth.getCurrentUser().getUid(), getIntent().getStringExtra(UID_KEY));
				_firebase.getReference("chats").child(chatID).child(TYPING_MESSAGE_REF).removeValue();
			} catch (Exception e) {
				Log.e("ChatActivity", "Error cleaning up typing indicator: " + e.getMessage());
			}
		}
		if (recordHandler != null && recordRunnable != null) {
			recordHandler.removeCallbacks(recordRunnable);
		}
		if (ChatMessagesList != null) ChatMessagesList.clear();
		if (attactmentmap != null) attactmentmap.clear();
		if (SynapseLoadingDialog != null && SynapseLoadingDialog.isShowing()) {
			try {
				SynapseLoadingDialog.dismiss();
			} catch (Exception e) {
				Log.e("ChatActivity", "Error dismissing progress dialog: " + e.getMessage());
			}
			SynapseLoadingDialog = null;
		}
		if (chatAdapter != null) chatAdapter = null;
		if (ChatMessagesListRecycler != null) ChatMessagesListRecycler.setAdapter(null);
		if (rv_attacmentList != null) rv_attacmentList.setAdapter(null);
		AsyncUploadService.cancelAllUploads(this);
	}

	@Override
	public void onBackPressed() {
		if (getIntent().hasExtra(ORIGIN_KEY)) {
			String originSimpleName = getIntent().getStringExtra(ORIGIN_KEY);
			if (originSimpleName != null && !originSimpleName.equals("null") && !originSimpleName.trim().isEmpty()) {
				try {
					String packageName = "com.synapse.social.studioasinc";
					String fullClassName = packageName + "." + originSimpleName.trim();
					Class<?> clazz = Class.forName(fullClassName);

					Intent intent = new Intent(this, clazz);
					if ("ProfileActivity".equals(originSimpleName.trim())) {
						if (getIntent().hasExtra(UID_KEY)) {
							intent.putExtra(UID_KEY, getIntent().getStringExtra(UID_KEY));
						} else {
							Toast.makeText(this, "Error: UID is required for ProfileActivity", Toast.LENGTH_SHORT).show();
							finish();
							return;
						}
					}
					startActivity(intent);
					finish();
					return;
				} catch (ClassNotFoundException e) {
					Log.e("ChatActivity", "Activity class not found: " + originSimpleName, e);
					Toast.makeText(this, "Error: Activity not found", Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Log.e("ChatActivity", "Failed to start activity: " + originSimpleName, e);
					Toast.makeText(this, "Error: Failed to start activity", Toast.LENGTH_SHORT).show();
				}
			}
		}
		finish();
	}

	@Override
	public void showMessageOverviewPopup(View _view, int _position, ArrayList<HashMap<String, Object>> _data) {
		messageInteractionHandler.showMessageOverviewPopup(_view, _position);
	}
	
	public void scrollToBottom() {
		if (ChatMessagesListRecycler != null && !ChatMessagesList.isEmpty()) {
			ChatMessagesListRecycler.smoothScrollToPosition(ChatMessagesList.size() - 1);
		}
	}

	private void scrollToBottomImmediate() {
		if (ChatMessagesListRecycler != null && !ChatMessagesList.isEmpty()) {
			ChatMessagesListRecycler.scrollToPosition(ChatMessagesList.size() - 1);
		}
	}

	public void _getOldChatMessagesRef() {
		chatDataHandler.getOldChatMessagesRef();
	}

	public void _DeleteMessageDialog(final HashMap<String, Object> messageData) {
		if (messageData == null || messageData.get(KEY_KEY) == null) return;
		final String messageKey = messageData.get(KEY_KEY).toString();

		new MaterialAlertDialogBuilder(ChatActivity.this)
			.setTitle("Delete Message")
			.setMessage("Are you sure you want to delete this message? This action cannot be undone.")
			.setIcon(R.drawable.popup_ic_3)
			.setPositiveButton("Delete", (dialog, which) -> {
				locallyDeletedMessages.add(messageKey);
				final String myUid = auth.getCurrentUser().getUid();
				final String otherUid = getIntent().getStringExtra(UID_KEY);
				final String chatID = ChatMessageManager.INSTANCE.getChatId(myUid, otherUid);
				final DatabaseReference chatRef = _firebase.getReference(CHATS_REF).child(chatID);

				chatRef.child(messageKey).removeValue().addOnCompleteListener(task -> {
					if (task.isSuccessful()) {
						chatRef.limitToFirst(1).addListenerForSingleValueEvent(new ValueEventListener() {
							@Override
							public void onDataChange(@NonNull DataSnapshot snapshot) {
								if (!snapshot.exists()) {
									_firebase.getReference(INBOX_REF).child(myUid).child(otherUid).removeValue();
									_firebase.getReference(INBOX_REF).child(otherUid).child(myUid).removeValue();
									_firebase.getReference(USER_CHATS_REF).child(myUid).child(chatID).removeValue();
									_firebase.getReference(USER_CHATS_REF).child(otherUid).child(chatID).removeValue();
								}
							}
							@Override
							public void onCancelled(@NonNull DatabaseError error) {
								Log.e("ChatActivity", "Error checking for last message: " + error.getMessage());
							}
						});
					}
				});
				onMessageRemoved(messageKey);
			})
			.setNegativeButton("Cancel", null)
			.create().show();
	}

	public void _Block(final String _uid) {
		HashMap<String,Object> blockMap = new HashMap<>();
		blockMap.put(_uid, "true");
		blocklist.child(auth.getCurrentUser().getUid()).updateChildren(blockMap);
	}

	public void _TransitionManager(final View _view, final double _duration) {
		LinearLayout viewgroup =(LinearLayout) _view;
		android.transition.AutoTransition autoTransition = new android.transition.AutoTransition();
		autoTransition.setDuration((long)_duration);
		android.transition.TransitionManager.beginDelayedTransition(viewgroup, autoTransition);
	}

	@Override
	public void openUrl(final String _URL) {
		CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
		builder.setToolbarColor(Color.parseColor("#242D39"));
		CustomTabsIntent customtabsintent = builder.build();
		customtabsintent.launchUrl(this, Uri.parse(_URL));
	}

	@Override
	public void showLoadMoreIndicator() {
		runOnUiThread(() -> {
			if (!ChatMessagesList.isEmpty() && !ChatMessagesList.get(0).containsKey("isLoadingMore")) {
				HashMap<String, Object> loadingMap = new HashMap<>();
				loadingMap.put("isLoadingMore", true);
				ChatMessagesList.add(0, loadingMap);
				if (chatAdapter != null) {
					chatAdapter.notifyItemRangeInserted(0, 1);
				}
			}
		});
	}

	@Override
	public void hideLoadMoreIndicator() {
		runOnUiThread(() -> {
			if (!ChatMessagesList.isEmpty() && ChatMessagesList.get(0).containsKey("isLoadingMore")) {
				ChatMessagesList.remove(0);
				if(chatAdapter != null) chatAdapter.notifyItemRemoved(0);
			}
		});
	}

	public void _showReplyUI(final double _position) {
		HashMap<String, Object> messageData = ChatMessagesList.get((int)_position);
		ReplyMessageID = messageData.get(KEY_KEY).toString();
		chatUIUpdater.showReplyUI(FirstUserName, SecondUserName, messageData);
		vbr.vibrate(48);
	}

	public void _setupSwipeToReply() {
		ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
				return false;
			}

			@Override
			public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
				int position = viewHolder.getAdapterPosition();
				if (position < 0 || position >= ChatMessagesList.size()) return;

				HashMap<String, Object> messageData = ChatMessagesList.get(position);
				if (messageData == null || !messageData.containsKey("key") || messageData.get("key") == null) {
					chatAdapter.notifyItemChanged(position);
					return;
				}
				_showReplyUI(position);
				viewHolder.itemView.animate().translationX(0).setDuration(150).start();
				chatAdapter.notifyItemChanged(position);
			}
		};
		new ItemTouchHelper(simpleItemTouchCallback).attachToRecyclerView(ChatMessagesListRecycler);
	}

	@Override
	public void performHapticFeedback() {
		vbr.vibrate(24);
	}

	@Override
	public void scrollToMessage(final String _messageKey) {
		final int position = _findMessagePosition(_messageKey);
		if (position != -1) {
			ChatMessagesListRecycler.smoothScrollToPosition(position);
			new Handler().postDelayed(() -> {
				if (!isFinishing() && !isDestroyed() && ChatMessagesListRecycler != null) {
					RecyclerView.ViewHolder viewHolder = ChatMessagesListRecycler.findViewHolderForAdapterPosition(position);
					if (viewHolder != null) _highlightMessage(viewHolder.itemView);
				}
			}, 500);
		} else {
			Toast.makeText(getApplicationContext(), "Original message not found", Toast.LENGTH_SHORT).show();
		}
	}
	
	private int _findMessagePosition(String messageKey) {
		for (int i = 0; i < ChatMessagesList.size(); i++) {
			if (ChatMessagesList.get(i).get(KEY_KEY).toString().equals(messageKey)) {
				return i;
			}
		}
		return -1;
	}

	private void _highlightMessage(View messageView) {
		if (isFinishing() || isDestroyed()) return;
		
		Drawable originalBackground = messageView.getBackground();
		ValueAnimator highlightAnimator = ValueAnimator.ofFloat(0f, 1f);
		highlightAnimator.setDuration(800);
		highlightAnimator.addUpdateListener(animation -> {
			if (isFinishing() || isDestroyed() || messageView == null) {
				animation.cancel();
				return;
			}
			float progress = (Float) animation.getAnimatedValue();
			int alpha = (int) (100 * (1 - progress));
			int color = Color.argb(alpha, 107, 76, 255);
			GradientDrawable highlightDrawable = new GradientDrawable();
			highlightDrawable.setColor(color);
			highlightDrawable.setCornerRadius(dpToPx(27));
			messageView.setBackground(highlightDrawable);
		});
		highlightAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				if (!isFinishing() && !isDestroyed() && messageView != null) {
					messageView.setBackground(originalBackground);
				}
			}
		});
		highlightAnimator.start();
	}
	
	private int dpToPx(int dp) {
		return (int) (dp * getResources().getDisplayMetrics().density);
	}

	private void startActivityWithUid(Class<?> activityClass) {
		Intent intent = new Intent(getApplicationContext(), activityClass);
		intent.putExtra(UID_KEY, getIntent().getStringExtra(UID_KEY));
		startActivity(intent);
	}

	private String getSenderNameForMessage(HashMap<String, Object> message) {
		if (message == null || message.get(UID_KEY) == null || auth.getCurrentUser() == null) return "Unknown";
		boolean isMyMessage = message.get(UID_KEY).toString().equals(auth.getCurrentUser().getUid());
		return isMyMessage ? FirstUserName : SecondUserName;
	}

	private void appendMessageToContext(StringBuilder contextBuilder, HashMap<String, Object> message) {
		Object messageTextObj = message.get(MESSAGE_TEXT_KEY);
		String messageText = (messageTextObj != null) ? messageTextObj.toString() : "";
		contextBuilder.append(getSenderNameForMessage(message)).append(": ").append(messageText).append("\n");
	}

	@Override
	public void onReplySelected(String messageId) {
		ReplyMessageID = messageId;
		for (HashMap<String, Object> messageData : ChatMessagesList) {
			if (messageId.equals(messageData.get(KEY_KEY))) {
				chatUIUpdater.showReplyUI(FirstUserName, SecondUserName, messageData);
				vbr.vibrate(48);
				break;
			}
		}
	}

	@Override
	public void onDeleteMessage(HashMap<String, Object> messageData) {
		_DeleteMessageDialog(messageData);
	}

	@Override
	public String getRecipientUid() {
		return getIntent().getStringExtra("uid");
	}

	@Override
	public void onVoiceMessageRecorded(String url, long duration) {
		messageSendingHandler.sendVoiceMessage(url, duration, ReplyMessageID, mMessageReplyLayout);
	}

	public boolean isLoading() {
		return chatDataHandler.isLoading();
	}

	@Override
	public void onInitialMessagesLoaded(ArrayList<HashMap<String, Object>> initialMessages, String oldestMessageKey) {
		runOnUiThread(() -> {
			chatUIUpdater.updateNoChatVisibility(initialMessages.isEmpty());
			ChatMessagesList.clear();
			messageKeys.clear();
			if (!initialMessages.isEmpty()) {
				ChatMessagesList.addAll(initialMessages);
				messageKeys.addAll(initialMessages.stream().map(m -> m.get(KEY_KEY).toString()).collect(Collectors.toSet()));
				chatAdapter.notifyDataSetChanged();
				scrollToBottomImmediate();
			}
		});
	}

	@Override
	public void onNewMessageAdded(HashMap<String, Object> newMessage) {
		runOnUiThread(() -> {
			String messageKey = newMessage.get(KEY_KEY).toString();
			if (!messageKeys.contains(messageKey)) {
				messageKeys.add(messageKey);
				int insertPosition = ChatMessagesList.size();
				ChatMessagesList.add(insertPosition, newMessage);
				chatAdapter.notifyItemInserted(insertPosition);
				scrollToBottom();
			}
		});
	}

	@Override
	public void onMessageChanged(HashMap<String, Object> updatedMessage) {
		runOnUiThread(() -> {
			String key = updatedMessage.get(KEY_KEY).toString();
			for (int i = 0; i < ChatMessagesList.size(); i++) {
				if (ChatMessagesList.get(i).get(KEY_KEY).toString().equals(key)) {
					ChatMessagesList.set(i, updatedMessage);
					chatAdapter.notifyItemChanged(i);
					break;
				}
			}
		});
	}

	@Override
	public void onMessageRemoved(String removedKey) {
		runOnUiThread(() -> {
			if (locallyDeletedMessages.contains(removedKey)) {
				locallyDeletedMessages.remove(removedKey);
				return;
			}
			for (int i = 0; i < ChatMessagesList.size(); i++) {
				if (ChatMessagesList.get(i).get(KEY_KEY).toString().equals(removedKey)) {
					ChatMessagesList.remove(i);
					messageKeys.remove(removedKey);
					chatAdapter.notifyItemRemoved(i);
					if (i > 0 && i <= ChatMessagesList.size()) {
						chatAdapter.notifyItemChanged(i - 1);
					}
					break;
				}
			}
		});
	}

	@Override
	public void onUserProfileUpdated(DataSnapshot dataSnapshot) {
		runOnUiThread(() -> {
			chatUIUpdater.updateUserProfile(dataSnapshot);
			String nickname = dataSnapshot.child("nickname").getValue(String.class);
			String username = dataSnapshot.child("username").getValue(String.class);
			SecondUserName = (nickname != null && !nickname.equals("null")) ? nickname : ((username != null && !username.equals("null")) ? "@" + username : "Unknown User");
			SecondUserAvatar = dataSnapshot.child("avatar_url").getValue(String.class);
			chatAdapter.setSecondUserName(SecondUserName);
			chatAdapter.setSecondUserAvatar(SecondUserAvatar);
			messageInteractionHandler.setSecondUserName(SecondUserName);
		});
	}

	@Override
	public void onGroupProfileUpdated(DataSnapshot dataSnapshot) {
		runOnUiThread(() -> chatUIUpdater.updateGroupProfile(dataSnapshot));
	}

	@Override
	public void onFirstUserNameResolved(String name) {
		runOnUiThread(() -> {
			this.FirstUserName = name;
			chatAdapter.setFirstUserName(name);
			messageInteractionHandler.setFirstUserName(name);
			messageSendingHandler.setFirstUserName(name);
		});
	}

	@Override
	public void onMoreMessagesLoaded(ArrayList<HashMap<String, Object>> newMessages, String oldestKey) {
		runOnUiThread(() -> {
			LinearLayoutManager layoutManager = (LinearLayoutManager) ChatMessagesListRecycler.getLayoutManager();
			int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
			View firstVisibleView = layoutManager.findViewByPosition(firstVisiblePosition);
			int topOffset = (firstVisibleView != null) ? firstVisibleView.getTop() : 0;

			ChatMessagesList.addAll(0, newMessages);
			messageKeys.addAll(newMessages.stream().map(m -> m.get(KEY_KEY).toString()).collect(Collectors.toSet()));
			chatAdapter.notifyItemRangeInserted(0, newMessages.size());

			if (firstVisibleView != null) {
				layoutManager.scrollToPositionWithOffset(firstVisiblePosition + newMessages.size(), topOffset);
			}
		});
	}

	@Override
	public void onNoMoreMessages() {
		runOnUiThread(() -> Toast.makeText(ChatActivity.this, "No more messages", Toast.LENGTH_SHORT).show());
	}

	@Override
	public void onDataLoadError(String error) {
		runOnUiThread(() -> Toast.makeText(ChatActivity.this, error, Toast.LENGTH_SHORT).show());
	}

	@Override
	public void onBlockStatusChanged(boolean isBlocked, boolean isBlockedByMe) {
		runOnUiThread(() -> {
			if (isBlocked || isBlockedByMe) {
				message_input_overall_container.setVisibility(View.GONE);
				if (isBlocked) blocked_txt.setVisibility(View.VISIBLE);
			} else {
				message_input_overall_container.setVisibility(View.VISIBLE);
				blocked_txt.setVisibility(View.GONE);
			}
		});
	}
}