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
import com.synapse.social.studioasinc.chat.common.ui.ChatNavigator;
import com.synapse.social.studioasinc.chat.common.ui.SwipeToReplyHandler;
import com.synapse.social.studioasinc.chat.common.service.UserBlockService;
import com.synapse.social.studioasinc.chat.group.service.GroupDetailsLoader;
import com.synapse.social.studioasinc.util.ActivityResultHandler;
import com.synapse.social.studioasinc.util.ChatMessageManager;
import com.synapse.social.studioasinc.util.ChatHelper;
import com.synapse.social.studioasinc.util.DatabaseHelper;
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
	private ChildEventListener _chat_child_listener;
	private DatabaseReference chatMessagesRef;
	private ValueEventListener _userStatusListener;
	private DatabaseReference userRef;

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
	private DatabaseReference main = _firebase.getReference(SKYLINE_REF);
	private FirebaseAuth auth;
	private TimerTask loadTimer;
	private Calendar cc = Calendar.getInstance();
	private Vibrator vbr;
	private DatabaseReference blocklist = _firebase.getReference(SKYLINE_REF).child(BLOCKLIST_REF);
	private ChildEventListener _blocklist_child_listener;
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

		_blocklist_child_listener = new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot _param1, String _param2) {
				handleBlocklistUpdate(_param1);
			}

			@Override
			public void onChildChanged(DataSnapshot _param1, String _param2) {
				handleBlocklistUpdate(_param1);
			}

			@Override
			public void onChildMoved(DataSnapshot _param1, String _param2) {

			}

			@Override
			public void onChildRemoved(DataSnapshot _param1) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);

			}

			@Override
			public void onCancelled(DatabaseError _param1) {
				final int _errorCode = _param1.getCode();
				final String _errorMessage = _param1.getMessage();

			}
		};
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
                auth
        );

		chatHelper = new ChatHelper(this);
		databaseHelper = new DatabaseHelper(
				this,
				_firebase,
				chatAdapter,
				FirstUserName,
				chatUIUpdater,
				(ArrayList)ChatMessagesList,
				messageKeys,
				oldestMessageKey,
				chatMessagesRef,
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

		databaseHelper.attachChatListener();
		_attachUserStatusListener();
        chatNavigator = new ChatNavigator(this, ChatMessagesListRecycler, ChatMessagesList);
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
		if (auth.getCurrentUser() != null) {
			String chatID = ChatMessageManager.INSTANCE.getChatId(auth.getCurrentUser().getUid(), getIntent().getStringExtra(UID_KEY));
			_firebase.getReference(CHATS_REF).child(chatID).child(TYPING_MESSAGE_REF).removeValue();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		blocklist.addChildEventListener(_blocklist_child_listener);

		if (chatMessagesRef != null && ChatMessagesList != null && chatAdapter != null) {
			_attachChatListener();
		}
		
		if (userRef != null) {
			_attachUserStatusListener();
		}

		if (auth.getCurrentUser() != null) {
			String recipientUid = getIntent().getStringExtra("uid");
			PresenceManager.setChattingWith(auth.getCurrentUser().getUid(), recipientUid);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		databaseHelper.detachChatListener();
		_detachUserStatusListener();
		blocklist.removeEventListener(_blocklist_child_listener);
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
		
		databaseHelper.detachChatListener();
		_detachUserStatusListener();
		
		if (_blocklist_child_listener != null) {
			try {
				blocklist.removeEventListener(_blocklist_child_listener);
			} catch (Exception e) {
				Log.e("ChatActivity", "Error removing blocklist listener: " + e.getMessage());
			}
		}
		
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
		databaseHelper.getUserReference();
	}

	public void _getChatMessagesRef() {
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

	private void _attachUserStatusListener() {
		if (userRef == null) {
			Log.w("ChatActivity", "Cannot attach user status listener - userRef is null");
			return;
		}
		
		if (_userStatusListener != null) {
			try {
				userRef.removeEventListener(_userStatusListener);
			} catch (Exception e) {
				Log.w("ChatActivity", "Error removing existing user status listener: " + e.getMessage());
			}
			_userStatusListener = null;
		}
		
		_userStatusListener = new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				if (dataSnapshot.exists()) {
					chatUIUpdater.updateUserProfile(dataSnapshot);

					String nickname = dataSnapshot.child("nickname").getValue(String.class);
					String username = dataSnapshot.child("username").getValue(String.class);
					if (nickname != null && !"null".equals(nickname)) {
						SecondUserName = nickname;
					} else if (username != null && !"null".equals(username)) {
						SecondUserName = "@" + username;
					} else {
						SecondUserName = "Unknown User";
					}
                    SecondUserAvatar = dataSnapshot.child("avatar_url").getValue(String.class);

					if (chatAdapter != null) {
						chatAdapter.setSecondUserName(SecondUserName);
						chatAdapter.setSecondUserAvatar(SecondUserAvatar);
					}
                    if (messageInteractionHandler != null) {
                        messageInteractionHandler.setSecondUserName(SecondUserName);
                    }
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
				Log.e("ChatActivity", "Failed to get user reference: " + databaseError.getMessage());
			}
		};
		userRef.addValueEventListener(_userStatusListener);
	}

	private void _detachUserStatusListener() {
		if (_userStatusListener != null) {
			userRef.removeEventListener(_userStatusListener);
			_userStatusListener = null;
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

	private void handleBlocklistUpdate(DataSnapshot dataSnapshot) {
		GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
		final String _childKey = dataSnapshot.getKey();
		final HashMap<String, Object> _childValue = dataSnapshot.getValue(_ind);

		if (_childValue == null) {
			return;
		}

		String myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
		String otherUid = getIntent().getStringExtra(UID_KEY);

		if (_childKey.equals(otherUid)) {
			if (_childValue.containsKey(myUid)) {
				message_input_overall_container.setVisibility(View.GONE);
				blocked_txt.setVisibility(View.VISIBLE);
			} else {
				message_input_overall_container.setVisibility(View.VISIBLE);
				blocked_txt.setVisibility(View.GONE);
			}
		}

		if (_childKey.equals(myUid)) {
			if (_childValue.containsKey(otherUid)) {
				message_input_overall_container.setVisibility(View.GONE);
			} else {
				message_input_overall_container.setVisibility(View.VISIBLE);
			}
		}
	}

	private String getSenderNameForMessage(HashMap<String, Object> message) {
		if (message == null || message.get(UID_KEY) == null || auth.getCurrentUser() == null) {
			return "Unknown";
		}
		boolean isMyMessage = message.get(UID_KEY).toString().equals(auth.getCurrentUser().getUid());
		return isMyMessage ? FirstUserName : SecondUserName;
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