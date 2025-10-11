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

import com.service.studioasinc.AI.Gemini;
import com.synapse.social.studioasinc.chat.common.ui.ChatNavigator;
import com.synapse.social.studioasinc.chat.common.ui.SwipeToReplyHandler;
import com.synapse.social.studioasinc.chat.common.service.UserBlockService;
import com.synapse.social.studioasinc.chat.group.service.GroupDetailsLoader;
import com.synapse.social.studioasinc.util.ActivityResultHandler;
import com.synapse.social.studioasinc.util.ChatMessageManager;
import com.synapse.social.studioasinc.util.ChatHelper;
import com.synapse.social.studioasinc.util.DatabaseHelper;
import com.synapse.social.studioasinc.util.ItemUploadHandler;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimerTask;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;

import static com.synapse.social.studioasinc.ChatConstants.*;



	private Handler recordHandler = new Handler();
	private Runnable recordRunnable;
	

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
	private String oldestMessageKey = null;
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
	private boolean isLoading = false;
	

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
	private LinearLayout message_input_overall_container;
	private TextView blocked_txt;
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
	private FadeEditText message_et;
	private LinearLayout toolContainer;
	private ImageView btn_voice_message;
	private ImageView close_attachments_btn;
	private View divider_mic_camera;
	private ImageView galleryBtn;

	private Intent intent = new Intent();


	private SharedPreferences blocked;
	private SharedPreferences theme;
	private Intent i = new Intent();
	private SharedPreferences appSettings;
	private Gemini gemini;
    private AiFeatureHandler aiFeatureHandler;
    private ActivityResultHandler activityResultHandler;
    private ChatKeyboardHandler chatKeyboardHandler;
    private VoiceMessageHandler voiceMessageHandler;
    private ChatUIUpdater chatUIUpdater;
    private ChatScrollListener chatScrollListener;
    private AttachmentHandler attachmentHandler;
	private ChatHelper chatHelper;
	private DatabaseHelper databaseHelper;
    private ChatNavigator chatNavigator;
    private GroupDetailsLoader groupDetailsLoader;
    private UserBlockService userBlockService;
    private ItemUploadHandler itemUploadHandler;


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
		

		String otherUserUid = getIntent().getStringExtra(UID_KEY);

        // TODO: Replace MessageSendingHandler with a Supabase-specific implementation.
        // 1. Create a `SupabaseMessageSendingHandler` that sends messages via Supabase.
        // 2. This handler should manage message state, attachments, and replies.
        // 3. Update all calls to `messageSendingHandler` to use the new `SupabaseMessageSendingHandler`.
        messageSendingHandler = new MessageSendingHandler(
                this,
                null, // TODO(supabase): Pass auth service
                null, //TODO: pass database service
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

                null, // TODO(supabase): Pass auth service

        // TODO: Replace MessageInteractionHandler with a Supabase-specific implementation.
        // 1. Create a `SupabaseMessageInteractionHandler` for handling message-related UI interactions.
        // 2. This handler should manage message selection, context menus, and other UI feedback.
        // 3. Update all calls to `messageInteractionHandler` to use the new `SupabaseMessageInteractionHandler`.
        messageInteractionHandler = new MessageInteractionHandler(
                this,
                this,
                null, // TODO(supabase): Pass auth service
                null, // TODO(supabase): Pass database service
                ChatMessagesList,
                ChatMessagesListRecycler,
                vbr,
                aiFeatureHandler,
                FirstUserName,
                SecondUserName
        );

        activityResultHandler = new ActivityResultHandler(this);
        SwipeToReplyHandler swipeToReplyHandler = new SwipeToReplyHandler(this, ChatMessagesList, new Function1<Integer, Unit>() {
            @Override
            public Unit invoke(Integer position) {
                _showReplyUI((double) position);
                return Unit.INSTANCE;
            }
        });
        swipeToReplyHandler.attachToRecyclerView(ChatMessagesListRecycler);

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
                null // TODO(supabase): Pass auth service
        );

		chatHelper = new ChatHelper(this);
		// TODO: Replace DatabaseHelper with a Supabase-specific implementation.
		// 1. Create a `SupabaseDatabaseHelper` that interacts with Supabase's real-time database.
		// 2. This new helper should handle fetching messages, users, and chat metadata.
		// 3. Update all calls to `databaseHelper` to use the new `SupabaseDatabaseHelper`.
		databaseHelper = new DatabaseHelper(
				this,
				null, //TODO: pass database service
				chatAdapter,
				FirstUserName,
				chatUIUpdater,
				(ArrayList)ChatMessagesList,
				messageKeys,
				oldestMessageKey,
				null, //TODO: pass chatMessagesRef
				ChatMessagesListRecycler,
				(HashMap)repliedMessagesCache,
				() -> {
					ChatMessagesListRecycler.scrollToPosition(ChatMessagesList.size() - 1);
					return kotlin.Unit.INSTANCE;
				}
		);

		if (is_group) {
            groupDetailsLoader = new GroupDetailsLoader(
                    this,
                    getIntent().getStringExtra("uid"),
                    topProfileLayoutUsername,
                    topProfileLayoutProfileImage,
                    topProfileLayoutGenderBadge,
                    topProfileLayoutVerifiedBadge,
                    topProfileLayoutStatus
            );
            groupDetailsLoader.loadGroupDetails();
            _getChatMessagesRef();
		} else {
			_getUserReference();
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
                null // TODO(supabase): Pass auth service
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
                null // TODO(supabase): Pass auth service
        );
        attachmentHandler.setup();

        // TODO: Replace ItemUploadHandler with Supabase Storage.
        // 1. Create a `SupabaseItemUploadHandler` that uploads files to Supabase Storage.
        // 2. This handler should manage the upload lifecycle, including progress and completion.
        // 3. Update all calls to `itemUploadHandler` to use the new `SupabaseItemUploadHandler`.
        itemUploadHandler = new ItemUploadHandler(
                this,
                null, // TODO(supabase): Pass auth service
                (ArrayList) attactmentmap,
                rv_attacmentList,
                (url) -> {
                    path = url;
                    return Unit.INSTANCE;
                }
        );

		databaseHelper.attachChatListener();

        chatNavigator = new ChatNavigator(this, ChatMessagesListRecycler, ChatMessagesList);
        // TODO: Replace UserBlockService with a Supabase-backed implementation.
        // 1. Create a `SupabaseUserBlockService` that uses Supabase to manage blocked users.
        // 2. This service should handle blocking, unblocking, and checking block status.
        // 3. Update all calls to `userBlockService` to use the new `SupabaseUserBlockService`.
        userBlockService = new UserBlockService(this);
	}

	@Override
	protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);
		activityResultHandler.handleResult(_requestCode, _resultCode, _data);
	}


	    @Override
		public void onPause() {
			super.onPause();
		}
	    @Override
		protected void onStart() {
			super.onStart();
		}
	    @Override
		public void onStop() {
			super.onStop();
		}
	    @Override
		public void onDestroy() {
			super.onDestroy();
			
			
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
		chatHelper.onBackPressed();
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


    private MessageInteractionHandler messageInteractionHandler;
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


	public void _getUserReference() {
		// TODO: Replace this with a direct call to the Supabase database.
		// 1. Use the `Supabase.client` to query the `users` table for the recipient's data.
		// 2. Update the UI with the retrieved user information.
		databaseHelper.getUserReference();
	}

	public void _getChatMessagesRef() {
		// TODO: Replace this with a direct call to the Supabase database.
		// 1. Use the `Supabase.client` to subscribe to the `messages` table for real-time updates.
		// 2. Filter the messages based on the current chat conversation.
		// 3. Update the chat UI with new and updated messages.
		databaseHelper.getChatMessagesRef();
	}

	private int _findCorrectInsertPosition(HashMap<String, Object> newMessage) {
		if (ChatMessagesList.isEmpty()) {
			return 0;
		}
		
		long newMessageTime = _getMessageTimestamp(newMessage);
		
		for (int i = 0; i < ChatMessagesList.size(); i++) {
			long existingMessageTime = _getMessageTimestamp(ChatMessagesList.get(i));
			
			if (newMessageTime <= existingMessageTime) {
				return i;
			}
		}
		
		return ChatMessagesList.size();
	}
	
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
			Log.w("ChatActivity", "Error parsing message timestamp: " + e.getMessage());
		}
		return System.currentTimeMillis();
	}
	
	private void _forceRefreshRecyclerView() {
		if (chatAdapter != null && ChatMessagesListRecycler != null) {
			ChatMessagesListRecycler.post(() -> {
				chatAdapter.notifyDataSetChanged();
			});
		}
	}
	
	private void _safeUpdateRecyclerView() {
		try {
			if (chatAdapter != null && ChatMessagesListRecycler != null) {
				ChatMessagesListRecycler.post(() -> {
					try {
						chatUIUpdater.updateNoChatVisibility(ChatMessagesList.isEmpty());
					} catch (Exception e) {
						Log.e("ChatActivity", "Error updating RecyclerView visibility: " + e.getMessage());
					}
				});
			}
		} catch (Exception e) {
			Log.e("ChatActivity", "Error in safe update: " + e.getMessage());
		}
	}
	
	private void _reorderMessagesIfNeeded() {
		try {
			if (ChatMessagesList.size() > 1) {
				boolean needsReorder = false;
				for (int i = 0; i < ChatMessagesList.size() - 1; i++) {
					long currentTime = _getMessageTimestamp(ChatMessagesList.get(i));
					long nextTime = _getMessageTimestamp(ChatMessagesList.get(i + 1));
					if (currentTime > nextTime) {
						needsReorder = true;
						break;
					}
				}
				
				if (needsReorder) {
					Log.d("ChatActivity", "Messages are out of order, reordering...");
					ChatMessagesList.sort((msg1, msg2) -> {
						long time1 = _getMessageTimestamp(msg1);
						long time2 = _getMessageTimestamp(msg2);
						return Long.compare(time1, time2);
					});
					
					if (chatAdapter != null) {
						chatAdapter.notifyDataSetChanged();
					}
					Log.d("ChatActivity", "Messages reordered successfully");
				}
			}
		} catch (Exception e) {
			Log.e("ChatActivity", "Error reordering messages: " + e.getMessage());
		}
	}

	private void _attachChatListener() {
		if (databaseHelper != null) {
			databaseHelper.attachChatListener();
		}
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
		databaseHelper.getOldChatMessagesRef();
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

    private MessageSendingHandler messageSendingHandler;


	public void _TransitionManager(final View _view, final double _duration) {
		LinearLayout viewgroup =(LinearLayout) _view;

		android.transition.AutoTransition autoTransition = new android.transition.AutoTransition(); autoTransition.setDuration((long)_duration); android.transition.TransitionManager.beginDelayedTransition(viewgroup, autoTransition);
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
		itemUploadHandler.startUpload((int) _position);
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
		chatUIUpdater.showReplyUI(FirstUserName, SecondUserName, messageData);
		vbr.vibrate((long)(48));
	}




	@Override
	public void performHapticFeedback() {
		if (vbr != null) {
			vbr.vibrate((long)(24));
		}
	}

	@Override
	public void scrollToMessage(final String _messageKey) {
		chatNavigator.scrollToMessage(_messageKey);
	}


	private void startActivityWithUid(Class<?> activityClass) {
		Intent intent = new Intent(getApplicationContext(), activityClass);
		intent.putExtra(UID_KEY, getIntent().getStringExtra(UID_KEY));
		startActivity(intent);
	}


	private void appendMessageToContext(StringBuilder contextBuilder, HashMap<String, Object> message) {
		Object messageTextObj = message.get(MESSAGE_TEXT_KEY);
		String messageText = (messageTextObj != null) ? messageTextObj.toString() : "";
		contextBuilder.append("TODO(supabase): Get sender name")
				.append(": ")
				.append(messageText)
				.append("\n");
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
		return isLoading;
	}

	public String getOldestMessageKey() {
		return oldestMessageKey;
	}
}