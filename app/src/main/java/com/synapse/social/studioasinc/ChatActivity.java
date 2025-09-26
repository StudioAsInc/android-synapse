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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.service.studioasinc.AI.Gemini;
import com.synapse.social.studioasinc.util.ActivityResultHandler;
import com.synapse.social.studioasinc.util.ChatMessageManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimerTask;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;

import static com.synapse.social.studioasinc.ChatConstants.*;

public class ChatActivity extends AppCompatActivity implements ChatAdapterListener, ChatInteractionListener, VoiceMessageHandler.VoiceMessageListener {

	private Handler recordHandler = new Handler();
	private Runnable recordRunnable;
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();

	private ProgressDialog SynapseLoadingDialog;
	private HashMap<String, Object> ChatSendMap = new HashMap<>();
	private HashMap<String, Object> ChatInboxSend = new HashMap<>();
	private double recordMs = 0;
	private HashMap<String, Object> ChatInboxSend2 = new HashMap<>();
	private String SecondUserAvatar = "";
	private HashMap<String, Object> typingSnd = new HashMap<>();
	private String ReplyMessageID = "";
	private String SecondUserName = "";
	private String FirstUserName = "";
	private static final int CHAT_PAGE_SIZE = 80;
	private boolean is_group = false;
	private String object_clicked = "";
	private String handle = "";
	private HashMap<String, Object> block = new HashMap<>();
	private double block_switch = 0;
	private String path = "";
	private String AndroidDevelopersBlogURL = "";
	public final int REQ_CD_IMAGE_PICKER = 101;
	private ChatAdapter chatAdapter;
	DatabaseReference chatMessagesRef;
	DatabaseReference userRef;

	private HashMap<String, HashMap<String, Object>> repliedMessagesCache = new HashMap<>();
	private java.util.Set<String> messageKeys = new java.util.HashSet<>();
	private java.util.Set<String> locallyDeletedMessages = new java.util.HashSet<>();
	private ArrayList<HashMap<String, Object>> ChatMessagesList = new ArrayList<>();
	public ArrayList<HashMap<String, Object>> attactmentmap = new ArrayList<>();

	private androidx.constraintlayout.widget.ConstraintLayout relativelayout1;
	private ImageView ivBGimage;
	private LinearLayout body;
	private LinearLayout appBar;
	private LinearLayout middle;
	public RelativeLayout attachmentLayoutListHolder;
	private LinearLayout mMessageReplyLayout;
	LinearLayout message_input_overall_container;
	TextView blocked_txt;
	private ImageView back;
	private LinearLayout topProfileLayout;
	private LinearLayout topProfileLayoutSpace;
	private ImageView ic_video_call;
	private ImageView ic_audio_call;
	private ImageView ic_more;
	private CardView topProfileCard;
	private LinearLayout topProfileLayoutRight;
	private ImageView topProfileLayoutProfileImage;
	private LinearLayout topProfileLayoutRightTop;
	private TextView topProfileLayoutStatus;
	private TextView topProfileLayoutUsername;
	private ImageView topProfileLayoutGenderBadge;
	private ImageView topProfileLayoutVerifiedBadge;
	private TextView noChatText;
	private RecyclerView ChatMessagesListRecycler;
	private CardView card_attactmentListRVHolder;
	public RecyclerView rv_attacmentList;
	private LinearLayout mMessageReplyLayoutBody;
	private LinearLayout mMessageReplyLayoutSpace;
	private ImageView mMessageReplyLayoutBodyIc;
	private LinearLayout mMessageReplyLayoutBodyRight;
	private ImageView mMessageReplyLayoutBodyCancel;
	private TextView mMessageReplyLayoutBodyRightUsername;
	private TextView mMessageReplyLayoutBodyRightMessage;
	private LinearLayout message_input_outlined_round;
	private MaterialButton btn_sendMessage;
	FadeEditText message_et;
	private LinearLayout toolContainer;
	private ImageView btn_voice_message;
	private ImageView close_attachments_btn;
	private View divider_mic_camera;
	private ImageView galleryBtn;

	private Intent intent = new Intent();
	private DatabaseReference main = _firebase.getReference(SKYLINE_REF);
	private FirebaseAuth auth;
	private TimerTask loadTimer;
	private Calendar cc = Calendar.getInstance();
	private Vibrator vbr;
	private DatabaseReference blocklist = _firebase.getReference(SKYLINE_REF).child(BLOCKLIST_REF);
	private SharedPreferences blocked;
	private SharedPreferences theme;
	private Intent i = new Intent();
	private SharedPreferences appSettings;
	private Gemini gemini;
	AiFeatureHandler aiFeatureHandler;
	private ActivityResultHandler activityResultHandler;
	private ChatKeyboardHandler chatKeyboardHandler;
	VoiceMessageHandler voiceMessageHandler;
	ChatUIUpdater chatUIUpdater;
	private ChatScrollListener chatScrollListener;
	private AttachmentHandler attachmentHandler;
	private ChatDataHandler chatDataHandler;
	MessageSendingHandler messageSendingHandler;
	MessageInteractionHandler messageInteractionHandler;


	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		is_group = getIntent().getBooleanExtra("isGroup", false);
		if (is_group) {
			setContentView(R.layout.activity_chat_group);
		} else {
			setContentView(R.layout.activity_chat);
		}
		initialize(_savedInstanceState);
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

	private void initialize(Bundle _savedInstanceState) {
		relativelayout1 = findViewById(R.id.relativelayout1);
		ivBGimage = findViewById(R.id.ivBGimage);
		body = findViewById(R.id.body);
		appBar = findViewById(R.id.appBar);
		middle = findViewById(R.id.middle);
		attachmentLayoutListHolder = findViewById(R.id.attachmentLayoutListHolder);
		mMessageReplyLayout = findViewById(R.id.mMessageReplyLayout);
		message_input_overall_container = findViewById(R.id.message_input_overall_container);
		blocked_txt = findViewById(R.id.blocked_txt);
		back = findViewById(R.id.back);
		topProfileLayout = findViewById(R.id.topProfileLayout);
		topProfileLayoutSpace = findViewById(R.id.topProfileLayoutSpace);
		ic_video_call = findViewById(R.id.ic_video_call);
		ic_audio_call = findViewById(R.id.ic_audio_call);
		ic_more = findViewById(R.id.ic_more);
		topProfileCard = findViewById(R.id.topProfileCard);
		topProfileLayoutRight = findViewById(R.id.topProfileLayoutRight);
		topProfileLayoutProfileImage = findViewById(R.id.topProfileLayoutProfileImage);
		topProfileLayoutRightTop = findViewById(R.id.topProfileLayoutRightTop);
		topProfileLayoutStatus = findViewById(R.id.topProfileLayoutStatus);
		topProfileLayoutUsername = findViewById(R.id.topProfileLayoutUsername);
		topProfileLayoutGenderBadge = findViewById(R.id.topProfileLayoutGenderBadge);
		topProfileLayoutVerifiedBadge = findViewById(R.id.topProfileLayoutVerifiedBadge);
		noChatText = findViewById(R.id.noChatText);
		ChatMessagesListRecycler = findViewById(R.id.ChatMessagesListRecycler);
		card_attactmentListRVHolder = findViewById(R.id.card_attactmentListRVHolder);
		rv_attacmentList = findViewById(R.id.rv_attacmentList);
		mMessageReplyLayoutBody = findViewById(R.id.mMessageReplyLayoutBody);
		mMessageReplyLayoutSpace = findViewById(R.id.mMessageReplyLayoutSpace);
		mMessageReplyLayoutBodyIc = findViewById(R.id.mMessageReplyLayoutBodyIc);
		mMessageReplyLayoutBodyRight = findViewById(R.id.mMessageReplyLayoutBodyRight);
		mMessageReplyLayoutBodyCancel = findViewById(R.id.mMessageReplyLayoutBodyCancel);
		mMessageReplyLayoutBodyRightUsername = findViewById(R.id.mMessageReplyLayoutBodyRightUsername);
		mMessageReplyLayoutBodyRightMessage = findViewById(R.id.mMessageReplyLayoutBodyRightMessage);
		message_input_outlined_round = findViewById(R.id.message_input_outlined_round);
		btn_sendMessage = findViewById(R.id.btn_sendMessage);
		message_et = findViewById(R.id.message_et);
		toolContainer = findViewById(R.id.toolContainer);
		btn_voice_message = findViewById(R.id.btn_voice_message);
		divider_mic_camera = findViewById(R.id.divider_mic_camera);
		galleryBtn = findViewById(R.id.galleryBtn);
		close_attachments_btn = findViewById(R.id.close_attachments_btn);
		auth = FirebaseAuth.getInstance();
		vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		blocked = getSharedPreferences("block", Activity.MODE_PRIVATE);
		theme = getSharedPreferences("theme", Activity.MODE_PRIVATE);
		appSettings = getSharedPreferences("appSettings", Activity.MODE_PRIVATE);

		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				onBackPressed();
			}
		});

		View.OnClickListener profileClickListener = v -> startActivityWithUid(ConversationSettingsActivity.class);
		topProfileLayout.setOnClickListener(profileClickListener);
		ic_more.setOnClickListener(profileClickListener);

		ic_video_call.setVisibility(View.GONE);
		ic_audio_call.setVisibility(View.GONE);

		mMessageReplyLayoutBodyCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				ReplyMessageID = "null";
				chatUIUpdater.hideReplyUI();
				vbr.vibrate((long)(48));
			}
		});

		message_input_outlined_round.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				message_et.requestFocus();
			}
		});

		btn_sendMessage.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View _view) {
				return aiFeatureHandler.handleSendButtonLongClick(ReplyMessageID);
			}
		});

		btn_sendMessage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				messageSendingHandler.sendButtonAction(message_et, ReplyMessageID, mMessageReplyLayout);
			}
		});
	}

	private void initializeLogic() {
		is_group = getIntent().getBooleanExtra("isGroup", false);
		SharedPreferences themePrefs = getSharedPreferences("theme", MODE_PRIVATE);
		String backgroundUrl = themePrefs.getString("chat_background_url", null);
		if (backgroundUrl != null && !backgroundUrl.isEmpty()) {
			Glide.with(this).load(backgroundUrl).into(ivBGimage);
		}

		SecondUserAvatar = "null";
		ReplyMessageID = "null";
		path = "";
		block_switch = 0;
		LinearLayoutManager ChatRecyclerLayoutManager = new LinearLayoutManager(this);
		ChatRecyclerLayoutManager.setReverseLayout(false);
		ChatRecyclerLayoutManager.setStackFromEnd(true);
		ChatMessagesListRecycler.setLayoutManager(ChatRecyclerLayoutManager);

		ChatMessagesListRecycler.setLongClickable(true);
		ChatMessagesListRecycler.setClickable(true);

		chatAdapter = new ChatAdapter(ChatMessagesList, repliedMessagesCache, this);
		chatAdapter.setHasStableIds(true);
		ChatMessagesListRecycler.setAdapter(chatAdapter);

		ChatMessagesListRecycler.setItemViewCacheSize(50);
		ChatMessagesListRecycler.setDrawingCacheEnabled(true);
		ChatMessagesListRecycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);

		String currentUserUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
		String otherUserUid = getIntent().getStringExtra(UID_KEY);
		if (is_group) {
			chatMessagesRef = _firebase.getReference("skyline/group-chats").child(otherUserUid);
		} else {
			String chatID = ChatMessageManager.INSTANCE.getChatId(currentUserUid, otherUserUid);
			chatMessagesRef = _firebase.getReference(CHATS_REF).child(chatID);
		}
		userRef = _firebase.getReference(SKYLINE_REF).child(USERS_REF).child(otherUserUid);

		chatUIUpdater = new ChatUIUpdater(
				this,
				noChatText,
				ChatMessagesListRecycler,
				topProfileLayoutProfileImage,
				topProfileLayoutUsername,
				topProfileLayoutStatus,
				topProfileLayoutGenderBadge,
				topProfileLayoutVerifiedBadge,
				mMessageReplyLayout,
				mMessageReplyLayoutBodyRightUsername,
				mMessageReplyLayoutBodyRightMessage,
				auth
		);

		chatDataHandler = new ChatDataHandler(
				this,
				_firebase,
				auth,
				chatMessagesRef,
				userRef,
				blocklist,
				ChatMessagesList,
				messageKeys,
				locallyDeletedMessages,
				repliedMessagesCache,
				chatAdapter,
				chatUIUpdater,
				ChatMessagesListRecycler,
				is_group,
				getIntent().getStringExtra(UID_KEY),
				message_input_overall_container,
				blocked_txt
		);

		messageSendingHandler = new MessageSendingHandler(
				this,
				auth,
				_firebase,
				ChatMessagesList,
				attactmentmap,
				chatAdapter,
				ChatMessagesListRecycler,
				rv_attacmentList,
				attachmentLayoutListHolder,
				messageKeys,
				otherUserUid,
				FirstUserName,
				is_group
		);

		gemini = new Gemini.Builder(this)
				.model("gemini-1.5-flash")
				.responseType("text")
				.tone("friendly")
				.size("medium")
				.maxTokens(2000)
				.temperature(0.8)
				.showThinking(true)
				.thinkingText("Analyzing your request...")
				.systemInstruction("Your name is ChatBot, help users with their questions")
				.responseTextView(message_et)
				.build();

		aiFeatureHandler = new AiFeatureHandler(
				this,
				gemini,
				message_et,
				ChatMessagesList,
				auth,
				SecondUserName,
				mMessageReplyLayoutBodyRightUsername,
				mMessageReplyLayoutBodyRightMessage
		);

		messageInteractionHandler = new MessageInteractionHandler(
				this,
				this,
				auth,
				_firebase,
				ChatMessagesList,
				ChatMessagesListRecycler,
				vbr,
				aiFeatureHandler,
				FirstUserName,
				SecondUserName
		);

		activityResultHandler = new ActivityResultHandler(this);
		_setupSwipeToReply();

		if (is_group) {
			chatDataHandler.getGroupReference();
		} else {
			chatDataHandler.getUserReference();
		}
		message_input_outlined_round.setOrientation(LinearLayout.HORIZONTAL);
		if (message_et.getText().toString().trim().equals("")) {
			_TransitionManager(message_input_overall_container, 100);
			message_input_outlined_round.setOrientation(LinearLayout.HORIZONTAL);

		} else {
			_TransitionManager(message_input_overall_container, 100);
			message_input_outlined_round.setOrientation(LinearLayout.VERTICAL);

		}

		chatKeyboardHandler = new ChatKeyboardHandler(
				this,
				message_et,
				toolContainer,
				btn_sendMessage,
				message_input_outlined_round,
				message_input_overall_container,
				auth
		);
		chatKeyboardHandler.setup();

		voiceMessageHandler = new VoiceMessageHandler(this, this);
		voiceMessageHandler.setupVoiceButton(btn_voice_message);

		chatScrollListener = new ChatScrollListener(this, ChatRecyclerLayoutManager);
		ChatMessagesListRecycler.addOnScrollListener(chatScrollListener);

		attachmentHandler = new AttachmentHandler(
				this,
				attachmentLayoutListHolder,
				rv_attacmentList,
				attactmentmap,
				close_attachments_btn,
				galleryBtn,
				auth
		);
		attachmentHandler.setup();
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

		// Listeners are detached in onStop by the ChatDataHandler

		if (recordHandler != null && recordRunnable != null) {
			recordHandler.removeCallbacks(recordRunnable);
		}

		if (ChatMessagesList != null) {
			ChatMessagesList.clear();
		}
		if (attactmentmap != null) {
			attactmentmap.clear();
		}

		if (SynapseLoadingDialog != null && SynapseLoadingDialog.isShowing()) {
			try {
				SynapseLoadingDialog.dismiss();
			} catch (Exception e) {
				Log.e("ChatActivity", "Error dismissing progress dialog: " + e.getMessage());
			}
			SynapseLoadingDialog = null;
		}

		if (chatAdapter != null) {
			chatAdapter = null;
		}

		if (ChatMessagesListRecycler != null) {
			ChatMessagesListRecycler.setAdapter(null);
		}
		if (rv_attacmentList != null) {
			rv_attacmentList.setAdapter(null);
		}

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

	public void _stateColor(final int _statusColor, final int _navigationColor) {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(_statusColor);
		getWindow().setNavigationBarColor(_navigationColor);
	}


	public void _viewGraphics(final View _view, final int _onFocus, final int _onRipple, final double _radius, final double _stroke, final int _strokeColor) {
		android.graphics.drawable.GradientDrawable GG = new android.graphics.drawable.GradientDrawable();
		GG.setColor(_onFocus);
		GG.setCornerRadius((float)_radius);
		GG.setStroke((int) _stroke, _strokeColor);
		android.graphics.drawable.RippleDrawable RE = new android.graphics.drawable.RippleDrawable(new android.content.res.ColorStateList(new int[][]{new int[]{}}, new int[]{ _onRipple}), GG, null);
		_view.setBackground(RE);
	}


	public void _ImageColor(final ImageView _image, final int _color) {
		_image.setColorFilter(_color,PorterDuff.Mode.SRC_ATOP);
	}


	@Override
	public void showMessageOverviewPopup(View _view, int _position, ArrayList<HashMap<String, Object>> _data) {
		messageInteractionHandler.showMessageOverviewPopup(_view, _position);
	}


	public void _setMargin(final View _view, final double _r, final double _l, final double _t, final double _b) {
		float dpRatio = new c(this).getContext().getResources().getDisplayMetrics().density;
		int right = (int)(_r * dpRatio);
		int left = (int)(_l * dpRatio);
		int top = (int)(_t * dpRatio);
		int bottom = (int)(_b * dpRatio);

		boolean _default = false;

		ViewGroup.LayoutParams p = _view.getLayoutParams();
		if (p instanceof LinearLayout.LayoutParams) {
			LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)p;
			lp.setMargins(left, top, right, bottom);
			_view.setLayoutParams(lp);
		}
		else if (p instanceof RelativeLayout.LayoutParams) {
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)p;
			lp.setMargins(left, top, right, bottom);
			_view.setLayoutParams(lp);
		}
		else if (p instanceof TableRow.LayoutParams) {
			TableRow.LayoutParams lp = (TableRow.LayoutParams)p;
			lp.setMargins(left, top, right, bottom);
			_view.setLayoutParams(lp);
		}


	}

	class c {
		Context co;
		public <T extends Activity> c(T a) {
			co = a;
		}
		public <T extends Fragment> c(T a) {
			co = a.getActivity();
		}
		public <T extends DialogFragment> c(T a) {
			co = a.getActivity();
		}

		public Context getContext() {
			return co;
		}

	}


	{

	}


	private void scrollToBottom() {
		if (ChatMessagesListRecycler != null && !ChatMessagesList.isEmpty()) {
			ChatMessagesListRecycler.smoothScrollToPosition(ChatMessagesList.size() - 1);
		}
	}

	private void scrollToBottomImmediate() {
		if (ChatMessagesListRecycler != null && !ChatMessagesList.isEmpty()) {
			ChatMessagesListRecycler.scrollToPosition(ChatMessagesList.size() - 1);
		}
	}

	public String _getDurationString(final long _durationInMillis) {
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


	public void _getOldChatMessagesRef() {
		chatDataHandler.getOldChatMessagesRef();
	}


	public void _DeleteMessageDialog(final HashMap<String, Object> messageData) {
		if (messageData == null || messageData.get(KEY_KEY) == null) {
			return;
		}
		final String messageKey = messageData.get(KEY_KEY).toString();

		MaterialAlertDialogBuilder zorry = new MaterialAlertDialogBuilder(ChatActivity.this);
		zorry.setTitle("Delete Message");
		zorry.setMessage("Are you sure you want to delete this message? This action cannot be undone.");
		zorry.setIcon(R.drawable.popup_ic_3);
		zorry.setPositiveButton("Delete", (dialog, which) -> {
			locallyDeletedMessages.add(messageKey);

			final String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
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

			for (int i = 0; i < ChatMessagesList.size(); i++) {
				Object k = ChatMessagesList.get(i).get(KEY_KEY);
				if (k != null && messageKey.equals(String.valueOf(k))) {
					ChatMessagesList.remove(i);
					messageKeys.remove(messageKey);
					chatAdapter.notifyItemRemoved(i);
					if (i > 0) {
						chatAdapter.notifyItemChanged(i - 1);
					}
					break;
				}
			}
		});
		zorry.setNegativeButton("Cancel", null);
		zorry.create().show();
	}


	public void _ScrollingText(final TextView _view) {
		_view.setSingleLine(true);
		_view.setEllipsize(TextUtils.TruncateAt.MARQUEE);
		_view.setSelected(true);
	}




	public void _Block(final String _uid) {
		block = new HashMap<>();
		block.put(_uid, "true");
		blocklist.child(FirebaseAuth.getInstance().getCurrentUser().getUid()).updateChildren(block);
		block.clear();
	}


	public void _TransitionManager(final View _view, final double _duration) {
		LinearLayout viewgroup =(LinearLayout) _view;

		android.transition.AutoTransition autoTransition = new android.transition.AutoTransition(); autoTransition.setDuration((long)_duration); android.transition.TransitionManager.beginDelayedTransition(viewgroup, autoTransition);
	}


	public void _Unblock_this_user() {
		DatabaseReference blocklistRef = FirebaseDatabase.getInstance().getReference("skyline/blocklist");
		String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
		String uidToRemove = getIntent().getStringExtra("uid");

		blocklistRef.child(myUid).child(uidToRemove).removeValue()
				.addOnSuccessListener(aVoid -> {
					Intent intent = getIntent();
					intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
					finish();
					startActivity(intent);
				})
				.addOnFailureListener(e -> {
					Log.e("UnblockUser", "Failed to unblock user", e);
				});
	}


	public void _LoadingDialog(final boolean _visibility) {
		if (_visibility) {
			if (SynapseLoadingDialog== null){
				SynapseLoadingDialog = new ProgressDialog(this);
				SynapseLoadingDialog.setCancelable(false);
				SynapseLoadingDialog.setCanceledOnTouchOutside(false);

				SynapseLoadingDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
				SynapseLoadingDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

			}
			SynapseLoadingDialog.show();
			SynapseLoadingDialog.setContentView(R.layout.loading_synapse);

			LinearLayout loading_bar_layout = (LinearLayout)SynapseLoadingDialog.findViewById(R.id.loading_bar_layout);

		} else {
			if (SynapseLoadingDialog != null){
				SynapseLoadingDialog.dismiss();
			}
		}

	}


	public void _ImgRound(final ImageView _imageview, final double _value) {
		android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable ();
		gd.setColor(android.R.color.transparent);
		gd.setCornerRadius((int)_value);
		_imageview.setClipToOutline(true);
		_imageview.setBackground(gd);
	}


	@Override
	public void openUrl(final String _URL) {
		AndroidDevelopersBlogURL = _URL;
		CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
		builder.setToolbarColor(Color.parseColor("#242D39"));
		CustomTabsIntent customtabsintent = builder.build();
		customtabsintent.launchUrl(this, Uri.parse(AndroidDevelopersBlogURL));
	}


	public void _startUploadForItem(final double _position) {
		if (auth.getCurrentUser() != null) {
			PresenceManager.setActivity(auth.getCurrentUser().getUid(), "Sending an attachment");
		}
		final int itemPosition = (int) _position;

		if (itemPosition < 0 || itemPosition >= attactmentmap.size()) {
			Log.e("ChatActivity", "Invalid position for upload: " + itemPosition + ", size: " + attactmentmap.size());
			return;
		}

		if (!SketchwareUtil.isConnected(getApplicationContext())) {
			try {
				HashMap<String, Object> itemMap = attactmentmap.get(itemPosition);
				if (itemMap != null) {
					itemMap.put("uploadState", "failed");
					if (rv_attacmentList.getAdapter() != null) {
						rv_attacmentList.getAdapter().notifyItemChanged(itemPosition);
					}
				}
			} catch (Exception e) {
				Log.e("ChatActivity", "Error updating upload state: " + e.getMessage());
			}
			return;
		}

		HashMap<String, Object> itemMap = attactmentmap.get(itemPosition);
		if (itemMap == null || !"pending".equals(itemMap.get("uploadState"))) {
			return;
		}

		itemMap.put("uploadState", "uploading");
		itemMap.put("uploadProgress", 0.0);

		if (rv_attacmentList.getAdapter() != null) {
			rv_attacmentList.getAdapter().notifyItemChanged(itemPosition);
		}

		String filePath = itemMap.get("localPath").toString();
		if (filePath == null || filePath.isEmpty()) {
			Log.e("ChatActivity", "Invalid file path for upload");
			itemMap.put("uploadState", "failed");
			if (rv_attacmentList.getAdapter() != null) {
				rv_attacmentList.getAdapter().notifyItemChanged(itemPosition);
			}
			return;
		}

		File file = new File(filePath);
		if (!file.exists()) {
			Log.e("ChatActivity", "File does not exist: " + filePath);
			itemMap.put("uploadState", "failed");
			if (rv_attacmentList.getAdapter() != null) {
				rv_attacmentList.getAdapter().notifyItemChanged(itemPosition);
			}
			return;
		}

		AsyncUploadService.uploadWithNotification(this, filePath, file.getName(), new AsyncUploadService.UploadProgressListener() {
			@Override
			public void onProgress(String filePath, int percent) {
				try {
					if (itemPosition >= 0 && itemPosition < attactmentmap.size()) {
						HashMap<String, Object> currentItem = attactmentmap.get(itemPosition);
						if (currentItem != null && filePath.equals(currentItem.get("localPath"))) {
							currentItem.put("uploadProgress", (double) percent);
							if (rv_attacmentList.getAdapter() != null) {
								rv_attacmentList.getAdapter().notifyItemChanged(itemPosition);
							}
						}
					}
				} catch (Exception e) {
					Log.e("ChatActivity", "Error updating upload progress: " + e.getMessage());
				}
			}

			@Override
			public void onSuccess(String filePath, String url, String publicId) {
				try {
					if (itemPosition >= 0 && itemPosition < attactmentmap.size()) {
						HashMap<String, Object> mapToUpdate = attactmentmap.get(itemPosition);
						if (mapToUpdate != null && filePath.equals(mapToUpdate.get("localPath"))) {
							mapToUpdate.put("uploadState", "success");
							mapToUpdate.put("cloudinaryUrl", url);
							mapToUpdate.put("publicId", publicId);
							if (rv_attacmentList.getAdapter() != null) {
								rv_attacmentList.getAdapter().notifyItemChanged(itemPosition);
							}

							path = url;
						}
					}
				} catch (Exception e) {
					Log.e("ChatActivity", "Error updating upload success: " + e.getMessage());
				}
			}

			@Override
			public void onFailure(String filePath, String error) {
				try {
					if (itemPosition >= 0 && itemPosition < attactmentmap.size()) {
						HashMap<String, Object> currentItem = attactmentmap.get(itemPosition);
						if (currentItem != null && filePath.equals(currentItem.get("localPath"))) {
							currentItem.put("uploadState", "failed");
							if (rv_attacmentList.getAdapter() != null) {
								rv_attacmentList.getAdapter().notifyItemChanged(itemPosition);
							}
						}
					}
					Log.e("ChatActivity", "Upload failed: " + error);
				} catch (Exception e) {
					Log.e("ChatActivity", "Error updating upload failure: " + e.getMessage());
				}
			}
		});
	}

	public void resetAttachmentState() {
		attachmentHandler.resetAttachmentState();
		path = "";
	}



	public void _showLoadMoreIndicator() {
		if (!ChatMessagesList.isEmpty() && !ChatMessagesList.get(0).containsKey("isLoadingMore")) {
			HashMap<String, Object> loadingMap = new HashMap<>();
			loadingMap.put("isLoadingMore", true);
			ChatMessagesList.add(0, loadingMap);
			if (chatAdapter != null) {
				chatAdapter.notifyItemInserted(0);
				if (ChatMessagesList.size() > 1) {
					chatAdapter.notifyItemChanged(1);
				}
			}
		}
	}


	public void _hideLoadMoreIndicator() {
		if (!ChatMessagesList.isEmpty() && ChatMessagesList.get(0).containsKey("isLoadingMore")) {
			ChatMessagesList.remove(0);
			((ChatAdapter)chatAdapter).notifyItemRemoved(0);
		}
	}


	public void _showReplyUI(final double _position) {
		HashMap<String, Object> messageData = ChatMessagesList.get((int)_position);
		ReplyMessageID = messageData.get(KEY_KEY).toString();
		chatUIUpdater.showReplyUI(chatDataHandler.getFirstUserName(), chatDataHandler.getSecondUserName(), messageData);
		vbr.vibrate((long)(48));
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
				if (position < 0 || position >= ChatMessagesList.size()) {
					return;
				}

				HashMap<String, Object> messageData = ChatMessagesList.get(position);
				if (messageData == null || !messageData.containsKey("key") || messageData.get("key") == null) {
					chatAdapter.notifyItemChanged(position);
					return;
				}

				_showReplyUI(position);
				viewHolder.itemView.animate().translationX(0).setDuration(150).start();
				chatAdapter.notifyItemChanged(position);
			}

			@Override
			public boolean isItemViewSwipeEnabled() {
				return true;
			}

			@Override
			public boolean isLongPressDragEnabled() {
				return false;
			}

			@Override
			public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
				if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
					View itemView = viewHolder.itemView;
					Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
					Drawable icon = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_reply);
					if (icon != null) {
						icon.setColorFilter(0xFF616161, PorterDuff.Mode.SRC_IN);

						int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
						int iconTop = itemView.getTop() + iconMargin;
						int iconBottom = iconTop + icon.getIntrinsicHeight();

						float width = (float) itemView.getWidth();
						float threshold = width * 0.25f;
						float progress = Math.min(1f, Math.abs(dX) / threshold);
						icon.setAlpha((int) (Math.max(0.25f, progress) * 255));

						if (dX > 0) {
							int iconLeft = itemView.getLeft() + iconMargin;
							int iconRight = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
							icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
							icon.draw(c);
						} else {
							int iconRight = itemView.getRight() - iconMargin;
							int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
							icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
							icon.draw(c);
						}
					}

					float dampedDx = dX * 0.75f;
					itemView.setTranslationX(dampedDx);
					itemView.setAlpha(1.0f);
				} else {
					super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
				}
			}

			@Override
			public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
				return 0.25f;
			}

			@Override
			public float getSwipeEscapeVelocity(float defaultValue) {
				return defaultValue * 1.5f;
			}

			@Override
			public float getSwipeVelocityThreshold(float defaultValue) {
				return defaultValue * 1.2f;
			}
		};
		ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
		itemTouchHelper.attachToRecyclerView(ChatMessagesListRecycler);
	}

	@Override
	public void performHapticFeedback() {
		if (vbr != null) {
			vbr.vibrate((long)(24));
		}
	}

	@Override
	public void scrollToMessage(final String _messageKey) {
		final int position = _findMessagePosition(_messageKey);
		if (position != -1) {
			ChatMessagesListRecycler.smoothScrollToPosition(position);

			new Handler().postDelayed(() -> {
				if (!isFinishing() && !isDestroyed() && ChatMessagesListRecycler != null) {
					RecyclerView.ViewHolder viewHolder = ChatMessagesListRecycler.findViewHolderForAdapterPosition(position);
					if (viewHolder != null) {
						_highlightMessage(viewHolder.itemView);
					}
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

	public void _getGroupReference() {
		chatDataHandler.getGroupReference();
	}

	private void _highlightMessage(View messageView) {
		if (isFinishing() || isDestroyed()) {
			return;
		}

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

			messageView.setBackgroundDrawable(highlightDrawable);
		});

		highlightAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				if (!isFinishing() && !isDestroyed() && messageView != null) {
					messageView.setBackgroundDrawable(originalBackground);
				}
			}
		});

		highlightAnimator.start();
	}

	private int dpToPx(int dp) {
		try {
			if (getResources() != null && getResources().getDisplayMetrics() != null) {
				return (int) (dp * getResources().getDisplayMetrics().density);
			}
		} catch (Exception e) {
			Log.e("ChatActivity", "Error converting dp to px: " + e.getMessage());
		}
		return dp;
	}

	private void startActivityWithUid(Class<?> activityClass) {
		Intent intent = new Intent(getApplicationContext(), activityClass);
		intent.putExtra(UID_KEY, getIntent().getStringExtra(UID_KEY));
		startActivity(intent);
	}


	private String getSenderNameForMessage(HashMap<String, Object> message) {
		if (message == null || message.get(UID_KEY) == null || auth.getCurrentUser() == null) {
			return "Unknown";
		}
		boolean isMyMessage = message.get(UID_KEY).toString().equals(auth.getCurrentUser().getUid());
		return isMyMessage ? chatDataHandler.getFirstUserName() : chatDataHandler.getSecondUserName();
	}

	private void appendMessageToContext(StringBuilder contextBuilder, HashMap<String, Object> message) {
		Object messageTextObj = message.get(MESSAGE_TEXT_KEY);
		String messageText = (messageTextObj != null) ? messageTextObj.toString() : "";
		contextBuilder.append(getSenderNameForMessage(message))
				.append(": ")
				.append(messageText)
				.append("\n");
	}

	@Override
	public void onReplySelected(String messageId) {
		ReplyMessageID = messageId;
		for (HashMap<String, Object> messageData : ChatMessagesList) {
			if (messageId.equals(messageData.get(KEY_KEY))) {
				chatUIUpdater.showReplyUI(chatDataHandler.getFirstUserName(), chatDataHandler.getSecondUserName(), messageData);
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

	public String getOldestMessageKey() {
		return chatDataHandler.getOldestMessageKey();
	}

	public static class ChatMessagesListRecyclerAdapter extends RecyclerView.Adapter<ChatMessagesListRecyclerAdapter.ViewHolder> {

		private final ArrayList<HashMap<String, Object>> data;
		private final Context context;
		private final boolean isGroup;
		private final FirebaseDatabase firebase;

		public ChatMessagesListRecyclerAdapter(Context context, ArrayList<HashMap<String, Object>> arr, boolean isGroup, FirebaseDatabase firebase) {
			this.data = arr;
			this.context = context;
			this.isGroup = isGroup;
			this.firebase = firebase;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater inflater = LayoutInflater.from(context);
			View v = inflater.inflate(R.layout.chat_msg_cv_synapse, null);
			RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			v.setLayoutParams(lp);
			return new ViewHolder(v);
		}

		@Override
		public void onBindViewHolder(ViewHolder holder, final int position) {
			View view = holder.itemView;
			final TextView sender_name = view.findViewById(R.id.sender_name);

			if (isGroup && !data.get(position).get("uid").toString().equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
				sender_name.setVisibility(View.VISIBLE);
				final String senderUid = data.get(position).get("uid").toString();
				DatabaseReference userRef = firebase.getReference("skyline/users").child(senderUid);
				userRef.addListenerForSingleValueEvent(new ValueEventListener() {
					@Override
					public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
						if (dataSnapshot.exists()) {
							String nickname = dataSnapshot.child("nickname").getValue(String.class);
							String username = dataSnapshot.child("username").getValue(String.class);
							if (nickname != null && !"null".equals(nickname)) {
								sender_name.setText(nickname);
							} else if (username != null && !"null".equals(username)) {
								sender_name.setText("@" + username);
							} else {
								sender_name.setText("Unknown User");
							}
						} else {
							sender_name.setText("Unknown User");
						}
					}

					@Override
					public void onCancelled(@NonNull DatabaseError databaseError) {
						sender_name.setText("Unknown User");
					}
				});
			} else {
				sender_name.setVisibility(View.GONE);
			}
		}

		@Override
		public int getItemCount() {
			return data.size();
		}

		public static class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}
}