package com.synapse.social.studioasinc;

import android.Manifest;
import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListPopupWindow;
import android.widget.PopupWindow;
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
import androidx.appcompat.widget.PopupMenu;
import android.view.Menu;
import android.view.MenuItem;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.textfield.TextInputLayout;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import com.service.studioasinc.AI.Gemini;
import com.synapse.social.studioasinc.FadeEditText;
import com.synapse.social.studioasinc.FileUtil;
import com.synapse.social.studioasinc.SketchwareUtil;
import com.synapse.social.studioasinc.StorageUtil;
import com.synapse.social.studioasinc.UploadFiles;
import com.synapse.social.studioasinc.AsyncUploadService;
import com.synapse.social.studioasinc.attachments.Rv_attacmentListAdapter;
import com.synapse.social.studioasinc.util.ChatMessageManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;

public class ChatActivity extends AppCompatActivity implements ChatAdapterListener {

	// Constants
	private static final String SKYLINE_REF = "skyline";
	private static final String USERS_REF = "users";
	private static final String CHATS_REF = "chats";
	private static final String USER_CHATS_REF = "user-chats";
	private static final String INBOX_REF = "inbox";
	private static final String BLOCKLIST_REF = "blocklist";
	private static final String TYPING_MESSAGE_REF = "typing-message";
	private static final String USERNAME_REF = "username";

	private static final String UID_KEY = "uid";
	private static final String ORIGIN_KEY = "origin";
	private static final String KEY_KEY = "key";
	private static final String MESSAGE_TEXT_KEY = "message_text";
	private static final String TYPE_KEY = "TYPE";
	private static final String MESSAGE_STATE_KEY = "message_state";
	private static final String PUSH_DATE_KEY = "push_date";
	private static final String REPLIED_MESSAGE_ID_KEY = "replied_message_id";
	private static final String ATTACHMENTS_KEY = "attachments";
	private static final String LAST_MESSAGE_UID_KEY = "last_message_uid";
	private static final String LAST_MESSAGE_TEXT_KEY = "last_message_text";
	private static final String LAST_MESSAGE_STATE_KEY = "last_message_state";
	private static final String CHAT_ID_KEY = "chatID";

	private static final String MESSAGE_TYPE = "MESSAGE";
	private static final String ATTACHMENT_MESSAGE_TYPE = "ATTACHMENT_MESSAGE";

	private static final String GEMINI_MODEL = "gemini-2.5-flash-lite";
	private static final String GEMINI_EXPLANATION_MODEL = "gemini-2.5-flash";
	private static final int EXPLAIN_CONTEXT_MESSAGES_BEFORE = 5;
	private static final int EXPLAIN_CONTEXT_MESSAGES_AFTER = 2;
	private static final String TAG = "ChatActivity";

	private Handler recordHandler = new Handler();
	private Runnable recordRunnable;

	private ProgressDialog SynapseLoadingDialog;
	private MediaRecorder AudioMessageRecorder;
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
	private ArrayList<HashMap<String, Object>> attactmentmap = new ArrayList<>();

	private androidx.constraintlayout.widget.ConstraintLayout relativelayout1;
	private ImageView ivBGimage;
	private LinearLayout body;
	private LinearLayout appBar;
	private LinearLayout middle;
	private RelativeLayout attachmentLayoutListHolder;
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
	private RecyclerView rv_attacmentList;
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

	private String audioFilePath = "";
	private boolean isRecording = false;

	private Intent intent = new Intent();
	private TimerTask loadTimer;
	private Calendar cc = Calendar.getInstance();
	private Vibrator vbr;
	private SharedPreferences blocked;
	private SharedPreferences theme;
	private Intent i = new Intent();
	private SharedPreferences appSettings;
	private Gemini gemini;

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

		close_attachments_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				attachmentLayoutListHolder.setVisibility(View.GONE);
				int oldSize = attactmentmap.size();
				if (oldSize > 0) {
					attactmentmap.clear();
					rv_attacmentList.getAdapter().notifyItemRangeRemoved(0, oldSize);
				}

				// Clear the attachment draft from SharedPreferences
				SharedPreferences drafts = getSharedPreferences("chat_drafts", Context.MODE_PRIVATE);
				// TODO: Migrate to Supabase
			}
		});

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
				mMessageReplyLayout.setVisibility(View.GONE);
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
				if (!message_et.getText().toString().isEmpty()) {
					String prompt = "Fix grammar, punctuation, and clarity without changing meaning. " +
					"Preserve original formatting (line breaks, lists, markdown). " +
					"Censor profanity by replacing letters with asterisks. " +
					"Return ONLY the corrected RAW text.\n```"
					.concat(message_et.getText().toString())
					.concat("```");
					callGemini(prompt, true);
				} else {
					if (ReplyMessageID != null && !ReplyMessageID.equals("null")) {
						int repliedMessageIndex = -1;
						for (int i = 0; i < ChatMessagesList.size(); i++) {
							if (ChatMessagesList.get(i).get(KEY_KEY).toString().equals(ReplyMessageID)) {
								repliedMessageIndex = i;
								break;
							}
						}

						if (repliedMessageIndex != -1) {
							StringBuilder contextBuilder = new StringBuilder();
							contextBuilder.append("You are helping 'Me' to write a reply in a conversation with '").append(SecondUserName).append("'.\n");
							contextBuilder.append("Here is the recent chat history:\n---\n");

							int startIndex = Math.max(0, repliedMessageIndex - 10);
							int endIndex = Math.min(ChatMessagesList.size() - 1, repliedMessageIndex + 10);

							for (int i = startIndex; i <= endIndex; i++) {
								HashMap<String, Object> message = ChatMessagesList.get(i);
								// TODO: Migrate to Supabase
							}

							contextBuilder.append("---\n");

							String repliedMessageSender = mMessageReplyLayoutBodyRightUsername.getText().toString();
							String repliedMessageText = mMessageReplyLayoutBodyRightMessage.getText().toString();

							contextBuilder.append("I need to reply to this message from '").append(repliedMessageSender).append("': \"").append(repliedMessageText).append("\"\n");
							contextBuilder.append("Based on the conversation history, please suggest a short, relevant reply from 'Me'.");

							String prompt = contextBuilder.toString();
							callGemini(prompt, false);
						}
					} else {
						// Fallback for non-reply long-press
						String prompt = "Suggest a generic, friendly greeting.";
						callGemini(prompt, false);
					}
				}
				return true;
			}
		});

		btn_sendMessage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_send_btn();
			}
		});

		message_et.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				// TODO: Migrate to Supabase
				if (_charSeq.length() == 0) {
					_TransitionManager(message_input_overall_container, 150);
					toolContainer.setVisibility(View.VISIBLE);
					message_input_outlined_round.setOrientation(LinearLayout.HORIZONTAL);
				} else {
					typingSnd = new HashMap<>();
					typingSnd.put("typingMessageStatus", "true");
					_TransitionManager(message_input_overall_container, 150);
					toolContainer.setVisibility(View.GONE);
					message_input_outlined_round.setOrientation(LinearLayout.VERTICAL);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

			}

			@Override
			public void afterTextChanged(Editable _param1) {
				if (message_et.getLineCount() > 1) {
					message_input_outlined_round.setBackgroundResource(R.drawable.bg_message_input_expanded);
				} else {
					message_input_outlined_round.setBackgroundResource(R.drawable.bg_message_input);
				}
			}
		});

		btn_voice_message.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, android.view.MotionEvent event) {
				switch (event.getAction()) {
					case android.view.MotionEvent.ACTION_DOWN:
						if (ContextCompat.checkSelfPermission(ChatActivity.this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
							_AudioRecorderStart();
							Toast.makeText(getApplicationContext(), "Recording...", Toast.LENGTH_SHORT).show();
						} else {
							ActivityCompat.requestPermissions(ChatActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1000);
						}
						return true;
					case android.view.MotionEvent.ACTION_UP:
						// Slide to cancel not implemented as per user's request to avoid major UI changes if it was too complex.
						_AudioRecorderStop();
						uploadAudioFile();
						return true;
				}
				return false;
			}
		});

		galleryBtn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				StorageUtil.pickMultipleFiles(ChatActivity.this, "*/*", REQ_CD_IMAGE_PICKER);
			}
		});

	}

	private void initializeLogic() {
		is_group = getIntent().getBooleanExtra("isGroup", false);
		// Load and apply chat background
		SharedPreferences themePrefs = getSharedPreferences("theme", MODE_PRIVATE);
		String backgroundUrl = themePrefs.getString("chat_background_url", null);
		if (backgroundUrl != null && !backgroundUrl.isEmpty()) {
			Glide.with(this).load(backgroundUrl).into(ivBGimage);
		}

		SecondUserAvatar = "null";
		ReplyMessageID = "null";
		path = "";
		block_switch = 0;
		// Set the Layout Manager
		LinearLayoutManager ChatRecyclerLayoutManager = new LinearLayoutManager(this);
		ChatRecyclerLayoutManager.setReverseLayout(false);
		ChatRecyclerLayoutManager.setStackFromEnd(true);
		ChatMessagesListRecycler.setLayoutManager(ChatRecyclerLayoutManager);

		// CRITICAL FIX: Configure RecyclerView to allow long press events
		ChatMessagesListRecycler.setLongClickable(true);
		ChatMessagesListRecycler.setClickable(true);
		
		// Create, configure, and set the new ChatAdapter
		chatAdapter = new ChatAdapter(ChatMessagesList, repliedMessagesCache, this);
		chatAdapter.setHasStableIds(true);
		ChatMessagesListRecycler.setAdapter(chatAdapter);
		
		// CRITICAL FIX: Ensure RecyclerView is properly configured for smooth updates
		ChatMessagesListRecycler.setItemViewCacheSize(50);
		ChatMessagesListRecycler.setDrawingCacheEnabled(true);
		ChatMessagesListRecycler.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
		
		// TODO: Migrate to Supabase
		
		userProfileUpdater = new com.synapse.social.studioasinc.util.UserProfileUpdater(
				this,
				topProfileLayoutProfileImage,
				topProfileLayoutUsername,
				topProfileLayoutStatus,
				topProfileLayoutGenderBadge,
				topProfileLayoutVerifiedBadge
		);
		// Initialize with custom settings
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
		_setupSwipeToReply();
		// --- START: Critical Initialization for Attachment RecyclerView ---

        // 1. Create the adapter for the attachment list, passing it our empty list.
        Rv_attacmentListAdapter attachmentAdapter = new Rv_attacmentListAdapter(this, attactmentmap, attachmentLayoutListHolder);
		rv_attacmentList.setAdapter(attachmentAdapter);

		// 2. A RecyclerView must have a LayoutManager to function.
		//    We set it to a horizontal layout.
		rv_attacmentList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
		
		// Add spacing between attachment items
		int attachmentSpacing = (int) getResources().getDimension(R.dimen.spacing_small);
		rv_attacmentList.addItemDecoration(new RecyclerView.ItemDecoration() {
			@Override
			public void getItemOffsets(@NonNull Rect outRect, @NonNull View view, 
			                          @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
				int position = parent.getChildAdapterPosition(view);
				if (position == 0) {
					outRect.left = attachmentSpacing;
				}
				outRect.right = attachmentSpacing;
			}
		});

		// --- END: Critical Initialization ---
		if (is_group) {
			_getGroupReference();
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
		ChatMessagesListRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
				super.onScrolled(recyclerView, dx, dy);
				LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
				if (dy < 0) { //check for scroll up
					if (layoutManager != null && layoutManager.findFirstVisibleItemPosition() <= 2) {
						// CRITICAL FIX: Only load more if we have an oldest message key and not already loading
						// Also check if we've reached the end to prevent unnecessary work
						if (!isLoading && oldestMessageKey != null && !oldestMessageKey.isEmpty() && !oldestMessageKey.equals("null")) {
							_getOldChatMessagesRef();
						}
					}
				}
			}
		});

		// Attach listeners after all references are safely initialized.
		_attachChatListener();
		_attachUserStatusListener();
	}

	@Override
	protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);
		if (_requestCode == REQ_CD_IMAGE_PICKER && _resultCode == Activity.RESULT_OK) {
			if (_data != null) {
				ArrayList<String> resolvedFilePaths = new ArrayList<>();
				
				try {
					if (_data.getClipData() != null) {
						for (int i = 0; i < _data.getClipData().getItemCount(); i++) {
							Uri fileUri = _data.getClipData().getItemAt(i).getUri();
							String path = StorageUtil.getPathFromUri(getApplicationContext(), fileUri);
							if (path != null && !path.isEmpty()) {
								resolvedFilePaths.add(path);
							} else {
								Log.w("ChatActivity", "Failed to resolve file path for clip data item " + i);
							}
						}
					} else if (_data.getData() != null) {
						Uri fileUri = _data.getData();
						String path = StorageUtil.getPathFromUri(getApplicationContext(), fileUri);
						if (path != null && !path.isEmpty()) {
							resolvedFilePaths.add(path);
						} else {
							Log.w("ChatActivity", "Failed to resolve file path for single data");
						}
					}
				} catch (Exception e) {
					Log.e("ChatActivity", "Error processing file picker result: " + e.getMessage());
					Toast.makeText(this, "Error processing selected files", Toast.LENGTH_SHORT).show();
					return;
				}

				if (!resolvedFilePaths.isEmpty()) {
					attachmentLayoutListHolder.setVisibility(View.VISIBLE);

					int startingPosition = attactmentmap.size();

					for (String filePath : resolvedFilePaths) {
						try {
							HashMap<String, Object> itemMap = new HashMap<>();
							itemMap.put("localPath", filePath);
							itemMap.put("uploadState", "pending");

							// Get image dimensions safely
							BitmapFactory.Options options = new BitmapFactory.Options();
							options.inJustDecodeBounds = true;
							try {
								BitmapFactory.decodeFile(filePath, options);
								itemMap.put("width", options.outWidth > 0 ? options.outWidth : 100);
								itemMap.put("height", options.outHeight > 0 ? options.outHeight : 100);
							} catch (Exception e) {
								Log.w("ChatActivity", "Could not decode image dimensions for: " + filePath);
								itemMap.put("width", 100);
								itemMap.put("height", 100);
							}

							attactmentmap.add(itemMap);
						} catch (Exception e) {
							Log.e("ChatActivity", "Error processing file: " + filePath + ", Error: " + e.getMessage());
						}
					}

					// Notify adapter of changes
					if (rv_attacmentList.getAdapter() != null) {
						rv_attacmentList.getAdapter().notifyItemRangeInserted(startingPosition, resolvedFilePaths.size());
					}

					// Start upload for each item
					for (int i = 0; i < resolvedFilePaths.size(); i++) {
						try {
							_startUploadForItem(startingPosition + i);
						} catch (Exception e) {
							Log.e("ChatActivity", "Error starting upload for item " + i + ": " + e.getMessage());
						}
					}
				} else {
					Log.w("ChatActivity", "No valid file paths resolved from file picker");
					Toast.makeText(this, "No valid files selected", Toast.LENGTH_SHORT).show();
				}
			}
		}
		switch (_requestCode) {
			default:
				break;
		}
	}


	@Override
	public void onPause() {
		super.onPause();
		// TODO: Migrate to Supabase
	}

	@Override
	protected void onStart() {
		super.onStart();
		// TODO: Migrate to Supabase
	}

	@Override
	public void onStop() {
		super.onStop();
		_detachChatListener();
		_detachUserStatusListener();
		// TODO: Migrate to Supabase
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		
		// TODO: Migrate to Supabase
		
		// Clean up Firebase listeners
		_detachChatListener();
		_detachUserStatusListener();
		
		// TODO: Migrate to Supabase
		
		// Clean up timers
		if (recordHandler != null && recordRunnable != null) {
			recordHandler.removeCallbacks(recordRunnable);
		}
		
		// Clean up media recorder
		if (AudioMessageRecorder != null) {
			try {
				if (isRecording) {
					AudioMessageRecorder.stop();
				}
				AudioMessageRecorder.release();
			} catch (Exception e) {
				Log.e("ChatActivity", "Error cleaning up media recorder in onDestroy: " + e.getMessage());
			} finally {
				AudioMessageRecorder = null;
				isRecording = false;
			}
		}
		
		// Clear lists to prevent memory leaks
		if (ChatMessagesList != null) {
			ChatMessagesList.clear();
		}
		if (attactmentmap != null) {
			attactmentmap.clear();
		}
		
		// Clean up progress dialog
		if (SynapseLoadingDialog != null && SynapseLoadingDialog.isShowing()) {
			try {
				SynapseLoadingDialog.dismiss();
			} catch (Exception e) {
				Log.e("ChatActivity", "Error dismissing progress dialog: " + e.getMessage());
			}
			SynapseLoadingDialog = null;
		}
		
		// Clean up adapters
		if (chatAdapter != null) {
			chatAdapter = null;
		}
		
		// Clean up RecyclerViews
		if (ChatMessagesListRecycler != null) {
			ChatMessagesListRecycler.setAdapter(null);
		}
		if (rv_attacmentList != null) {
			rv_attacmentList.setAdapter(null);
		}
		
		// Cancel all active uploads and clear notifications
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

	private void _fetchRepliedMessages(ArrayList<HashMap<String, Object>> messages) {
		java.util.HashSet<String> repliedIdsToFetch = new java.util.HashSet<>();
		for (HashMap<String, Object> message : messages) {
			if (message.containsKey("replied_message_id")) {
                String repliedId = String.valueOf(message.get("replied_message_id"));
				if (repliedId != null && !repliedId.equals("null") && !repliedMessagesCache.containsKey(repliedId)) {
					repliedIdsToFetch.add(repliedId);
				}
			}
		}

		if (repliedIdsToFetch.isEmpty()) {
			return;
		}

		// TODO: Migrate to Supabase
	}

	private void _updateMessageInRecyclerView(String repliedMessageKey) {
		if (chatAdapter == null || isFinishing() || isDestroyed()) return;
		for (int i = 0; i < ChatMessagesList.size(); i++) {
			HashMap<String, Object> message = ChatMessagesList.get(i);
			if (message != null && message.containsKey("replied_message_id") && repliedMessageKey.equals(message.get("replied_message_id").toString())) {
				final int positionToUpdate = i;
				runOnUiThread(() -> {
					if (chatAdapter != null && positionToUpdate < chatAdapter.getItemCount()) {
						chatAdapter.notifyItemChanged(positionToUpdate);
					}
				});
			}
		}
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
		if (_data == null || _position >= _data.size() || _position < 0) {
			return;
		}

		final HashMap<String, Object> messageData = _data.get(_position);
		// TODO: Migrate to Supabase
		String senderUid = messageData.get(UID_KEY) != null ? String.valueOf(messageData.get(UID_KEY)) : null;
		final boolean isMine = true;
		final String messageText = messageData.get(MESSAGE_TEXT_KEY) != null ? messageData.get(MESSAGE_TEXT_KEY).toString() : "";

		// Inflate the custom popup layout
		LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View popupView = inflater.inflate(R.layout.chat_msg_options_popup_cv_synapse, null);

		// Create the PopupWindow
		final PopupWindow popupWindow = new PopupWindow(popupView, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true);
		popupWindow.setElevation(24);

		// Find views in the popup layout
		LinearLayout editLayout = popupView.findViewById(R.id.edit);
		LinearLayout replyLayout = popupView.findViewById(R.id.reply);
		LinearLayout summaryLayout = popupView.findViewById(R.id.summary);
		LinearLayout explainLayout = popupView.findViewById(R.id.explain);
		LinearLayout copyLayout = popupView.findViewById(R.id.copy);
		LinearLayout deleteLayout = popupView.findViewById(R.id.delete);

		// Configure visibility based on message owner and content
		editLayout.setVisibility(isMine ? View.VISIBLE : View.GONE);
		deleteLayout.setVisibility(isMine ? View.VISIBLE : View.GONE);
		summaryLayout.setVisibility(messageText.length() > 200 ? View.VISIBLE : View.GONE);

		// Set click listeners
		replyLayout.setOnClickListener(v -> {
			ReplyMessageID = messageData.get(KEY_KEY).toString();
			mMessageReplyLayoutBodyRightUsername.setText(isMine ? FirstUserName : SecondUserName);
			mMessageReplyLayoutBodyRightMessage.setText(messageText);
			mMessageReplyLayout.setVisibility(View.VISIBLE);
			vbr.vibrate(48);
			popupWindow.dismiss();
		});

		copyLayout.setOnClickListener(v -> {
			((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("clipboard", messageText));
			vbr.vibrate(48);
			popupWindow.dismiss();
		});

		deleteLayout.setOnClickListener(v -> {
			_DeleteMessageDialog(messageData);
			popupWindow.dismiss();
		});

		editLayout.setOnClickListener(v -> {
			MaterialAlertDialogBuilder dialog = new MaterialAlertDialogBuilder(ChatActivity.this);
			dialog.setTitle("Edit message");
			View dialogView = LayoutInflater.from(ChatActivity.this).inflate(R.layout.single_et, null);
			dialog.setView(dialogView);
			final EditText editText = dialogView.findViewById(R.id.edittext1);
			editText.setText(messageText);
			dialog.setPositiveButton("Save", (d, w) -> {
				String newText = editText.getText().toString();
				// TODO: Migrate to Supabase
			});
			dialog.setNegativeButton("Cancel", null);
			AlertDialog shownDialog = dialog.show();

			// Request focus and show keyboard
			editText.requestFocus();
			if (shownDialog.getWindow() != null) {
				shownDialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
			}

			popupWindow.dismiss();
		});
		
		summaryLayout.setOnClickListener(v -> {
			String prompt = "Summarize the following text in a few sentences:\n\n" + messageText;
			RecyclerView.ViewHolder vh = ChatMessagesListRecycler.findViewHolderForAdapterPosition((int)_position);
			if (vh instanceof BaseMessageViewHolder) {
				callGeminiForSummary(prompt, (BaseMessageViewHolder) vh);
			}
			popupWindow.dismiss();
		});

		explainLayout.setOnClickListener(v -> {
			int position = (int)_position;
			String prompt = buildExplanationPrompt(position, messageText, messageData);
			RecyclerView.ViewHolder vh = ChatMessagesListRecycler.findViewHolderForAdapterPosition(position);
			if (vh instanceof BaseMessageViewHolder) {
				callGeminiForExplanation(prompt, (BaseMessageViewHolder) vh);
			}
			popupWindow.dismiss();
		});


		// CRITICAL FIX: Improved positioning for compact popup
		popupView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
		int popupWidth = popupView.getMeasuredWidth();
		int popupHeight = popupView.getMeasuredHeight();

		int[] location = new int[2];
		_view.getLocationOnScreen(location);

		// Compute initial centered-above coordinates with better positioning
		int x = location[0] + (_view.getWidth() / 2) - (popupWidth / 2);
		int aboveY = location[1] - popupHeight - 8; // Add small gap
		int belowY = location[1] + _view.getHeight() + 8; // Add small gap

		// Constrain within the visible window and flip below if there's no room above
		Rect visibleFrame = new Rect();
		_view.getWindowVisibleDisplayFrame(visibleFrame);

		// Horizontal clamp with better margins
		x = Math.max(visibleFrame.left + 16, Math.min(x, visibleFrame.right - popupWidth - 16));

		// Vertical position: prefer above, otherwise below, and clamp
		int y = (aboveY >= visibleFrame.top + 16) ? aboveY : Math.min(belowY, visibleFrame.bottom - popupHeight - 16);
		y = Math.max(visibleFrame.top + 16, Math.min(y, visibleFrame.bottom - popupHeight - 16));

		// Enable outside touch dismissal and proper shadow rendering
		popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		popupWindow.setOutsideTouchable(true);

		popupWindow.showAtLocation(_view, Gravity.NO_GRAVITY, x, y);
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
		// The user profile data is now fetched via a persistent listener attached in onStart,
		// so the addListenerForSingleValueEvent call is no longer needed here.

		// TODO: Migrate to Supabase

		_getChatMessagesRef();
	}


	public void _getChatMessagesRef() {
		// Initial load
		// TODO: Migrate to Supabase
	}

	private void _attachChatListener() {
		// Extra safety: ensure all required dependencies are available
		// TODO: Migrate to Supabase
	}

	/**
	 * CRITICAL FIX: Find the correct position to insert a new message based on timestamp
	 * This ensures messages are always in chronological order
	 */
	private int _findCorrectInsertPosition(HashMap<String, Object> newMessage) {
		if (ChatMessagesList.isEmpty()) {
			return 0;
		}
		
		// Get the timestamp of the new message
		long newMessageTime = _getMessageTimestamp(newMessage);
		
		// Find the correct position by comparing timestamps
		for (int i = 0; i < ChatMessagesList.size(); i++) {
			long existingMessageTime = _getMessageTimestamp(ChatMessagesList.get(i));
			
			// If new message is older than or equal to existing message, insert before it
			if (newMessageTime <= existingMessageTime) {
				return i;
			}
		}
		
		// If new message is the newest, add to the end
		return ChatMessagesList.size();
	}
	
	/**
	 * Helper method to extract timestamp from message
	 */
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
	
	/**
	 * CRITICAL FIX: Force refresh the RecyclerView when needed
	 */
	private void _forceRefreshRecyclerView() {
		if (chatAdapter != null && ChatMessagesListRecycler != null) {
			ChatMessagesListRecycler.post(() -> {
				chatAdapter.notifyDataSetChanged();
			});
		}
	}
	
	/**
	 * CRITICAL FIX: Safely update the RecyclerView with proper error handling
	 */
	private void _safeUpdateRecyclerView() {
		try {
			if (chatAdapter != null && ChatMessagesListRecycler != null) {
				ChatMessagesListRecycler.post(() -> {
					try {
						if (ChatMessagesList.isEmpty()) {
							ChatMessagesListRecycler.setVisibility(View.GONE);
							noChatText.setVisibility(View.VISIBLE);
						} else {
							ChatMessagesListRecycler.setVisibility(View.VISIBLE);
							noChatText.setVisibility(View.GONE);
						}
					} catch (Exception e) {
						Log.e("ChatActivity", "Error updating RecyclerView visibility: " + e.getMessage());
					}
				});
			}
		} catch (Exception e) {
			Log.e("ChatActivity", "Error in safe update: " + e.getMessage());
		}
	}
	
	/**
	 * CRITICAL FIX: Reorder messages if they are out of chronological order
	 */
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

	/**
	 * Scrolls the RecyclerView to the bottom smoothly
	 */
	private void scrollToBottom() {
		if (ChatMessagesListRecycler != null && !ChatMessagesList.isEmpty()) {
			ChatMessagesListRecycler.smoothScrollToPosition(ChatMessagesList.size() - 1);
		}
	}

	/**
	 * Scrolls the RecyclerView to the bottom immediately
	 */
	private void scrollToBottomImmediate() {
		if (ChatMessagesListRecycler != null && !ChatMessagesList.isEmpty()) {
			ChatMessagesListRecycler.scrollToPosition(ChatMessagesList.size() - 1);
		}
	}

	private void _detachChatListener() {
		// TODO: Migrate to Supabase
	}

	private void _attachUserStatusListener() {
		// TODO: Migrate to Supabase
	}

	private void _detachUserStatusListener() {
		// TODO: Migrate to Supabase
	}

	public void _AudioRecorderStart() {
		cc = Calendar.getInstance();
		recordMs = 0;
		AudioMessageRecorder = new MediaRecorder();

		File getCacheDir = getExternalCacheDir();
		String getCacheDirName = "audio_records";
		File getCacheFolder = new File(getCacheDir, getCacheDirName);
		getCacheFolder.mkdirs();
		File getRecordFile = new File(getCacheFolder, cc.getTimeInMillis() + ".mp3");
		audioFilePath = getRecordFile.getAbsolutePath();

		AudioMessageRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		AudioMessageRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		AudioMessageRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
		AudioMessageRecorder.setAudioEncodingBitRate(320000);
		AudioMessageRecorder.setOutputFile(audioFilePath);

		try {
			AudioMessageRecorder.prepare();
			AudioMessageRecorder.start();
			isRecording = true;
		} catch (IOException e) {
			e.printStackTrace();
		}

		vbr.vibrate((long)(48));
		recordRunnable = new Runnable() {
			@Override
			public void run() {
				recordMs += 500;
				recordHandler.postDelayed(this, 500);
			}
		};
		recordHandler.postDelayed(recordRunnable, 500);

	}


	public void _AudioRecorderStop() {
		if (isRecording) {
			if (AudioMessageRecorder != null) {
				try {
					AudioMessageRecorder.stop();
					AudioMessageRecorder.release();
				} catch (RuntimeException e) {
					Log.e("ChatActivity", "Error stopping media recorder: " + e.getMessage());
				}
				AudioMessageRecorder = null;
			}
			isRecording = false;
			vbr.vibrate((long)(48));
			if (recordHandler != null && recordRunnable != null) {
				recordHandler.removeCallbacks(recordRunnable);
			}
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
		// CRITICAL FIX: Robust pagination check - prevent loading when no more messages
		if (isLoading || oldestMessageKey == null || oldestMessageKey.isEmpty() || oldestMessageKey.equals("null")) {
			return;
		}
		isLoading = true;
		_showLoadMoreIndicator();

		// TODO: Migrate to Supabase
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

			// TODO: Migrate to Supabase

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




	public void _textview_mh(final TextView _txt, final String _value) {
		_txt.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
		//_txt.setTextIsSelectable(true);
		updateSpan(_value, _txt);
	}
	private void updateSpan(String str, TextView _txt){
		SpannableStringBuilder ssb = new SpannableStringBuilder(str);
		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?<![^\\s])(([@]{1}|[#]{1})([A-Za-z0-9_-]\\.?)+)(?![^\\s,])|\\*\\*(.*?)\\*\\*|__(.*?)__|~~(.*?)~~|_(.*?)_|\\*(.*?)\\*|///(.*?)///");
		java.util.regex.Matcher matcher = pattern.matcher(str);
		int offset = 0;

		while (matcher.find()) {
			int start = matcher.start() + offset;
			int end = matcher.end() + offset;

			if (matcher.group(3) != null) {
				// For mentions or hashtags
				ProfileSpan span = new ProfileSpan();
				ssb.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			} else if (matcher.group(4) != null) {
				// For bold text (**bold**)
				String boldText = matcher.group(4); // Extract text inside **
				ssb.replace(start, end, boldText);
				ssb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), start, start + boldText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				offset -= 4; // Update offset for bold text replacement
			} else if (matcher.group(5) != null) {
				// For italic text (__italic__)
				String italicText = matcher.group(5);
				ssb.replace(start, end, italicText);
				ssb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.ITALIC), start, start + italicText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				offset -= 4; // Update offset for italic text replacement
			} else if (matcher.group(6) != null) {
				// For strikethrough text (~~strikethrough~~)
				String strikethroughText = matcher.group(6);
				ssb.replace(start, end, strikethroughText);
				ssb.setSpan(new android.text.style.StrikethroughSpan(), start, start + strikethroughText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				offset -= 4; // Update offset for strikethrough text replacement
			} else if (matcher.group(7) != null) {
				// For underline text (_underline_)
				String underlineText = matcher.group(7);
				ssb.replace(start, end, underlineText);
				ssb.setSpan(new android.text.style.UnderlineSpan(), start, start + underlineText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				offset -= 2; // Update offset for underline text replacement
			} else if (matcher.group(8) != null) {
				// For italic text (*italic*)
				String italicText = matcher.group(8);
				ssb.replace(start, end, italicText);
				ssb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.ITALIC), start, start + italicText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				offset -= 2; // Update offset for italic text replacement
			} else if (matcher.group(9) != null) {
				// For bold-italic text (///bold-italic///)
				String boldItalicText = matcher.group(9);
				ssb.replace(start, end, boldItalicText);
				ssb.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD_ITALIC), start, start + boldItalicText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
				offset -= 6; // Update offset for bold-italic text replacement
			}
		}
		_txt.setText(ssb);
	}
	private class ProfileSpan extends android.text.style.ClickableSpan{


		@Override
		public void onClick(View view){

			if(view instanceof TextView){
				TextView tv = (TextView)view;

				if(tv.getText() instanceof Spannable){
					Spannable sp = (Spannable)tv.getText();

					int start = sp.getSpanStart(this);
					int end = sp.getSpanEnd(this);
					object_clicked = sp.subSequence(start,end).toString();
					handle = object_clicked.replace("@", "");
					// TODO: Migrate to Supabase
				}
			}

		}
		@Override
		public void updateDrawState(TextPaint ds) {
			ds.setUnderlineText(false);
			ds.setColor(Color.parseColor("#FFFF00"));
			ds.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
		}
	}


	public void _send_btn() {
		// TODO: Migrate to Supabase
	}

	private void proceedWithMessageSending(String messageText, String senderUid, String recipientUid) {
		// TODO: Migrate to Supabase
		
		if (attactmentmap.isEmpty() && messageText.isEmpty()) {
			Log.w("ChatActivity", "No message text and no attachments - nothing to send");
			return;
		}

		// TODO: Migrate to Supabase
		final String uniqueMessageKey = "";
		final HashMap<String, Object> messageToSend = new HashMap<>();
		String lastMessageForInbox;

		if (!attactmentmap.isEmpty()) {
			ArrayList<HashMap<String, Object>> successfulAttachments = new ArrayList<>();
			boolean allUploadsSuccessful = true;
			for (HashMap<String, Object> item : attactmentmap) {
				if ("success".equals(item.get("uploadState"))) {
					HashMap<String, Object> attachmentData = new HashMap<>();
					attachmentData.put("url", item.get("cloudinaryUrl"));
					attachmentData.put("publicId", item.get("publicId"));
					attachmentData.put("width", item.get("width"));
					attachmentData.put("height", item.get("height"));
					successfulAttachments.add(attachmentData);
				} else {
					allUploadsSuccessful = false;
				}
			}

			if (!allUploadsSuccessful) {
				Toast.makeText(getApplicationContext(), "Waiting for uploads to complete...", Toast.LENGTH_SHORT).show();
				return;
			}
			
			messageToSend.put(TYPE_KEY, ATTACHMENT_MESSAGE_TYPE);
			messageToSend.put(ATTACHMENTS_KEY, successfulAttachments);
			lastMessageForInbox = messageText.isEmpty() ? successfulAttachments.size() + " attachment(s)" : messageText;

		} else { // Text-only message
			messageToSend.put(TYPE_KEY, MESSAGE_TYPE);
			lastMessageForInbox = messageText;
		}

		messageToSend.put(UID_KEY, senderUid);
		messageToSend.put(MESSAGE_TEXT_KEY, messageText);
		messageToSend.put(MESSAGE_STATE_KEY, "sended");
		if (!ReplyMessageID.equals("null")) messageToSend.put(REPLIED_MESSAGE_ID_KEY, ReplyMessageID);
		messageToSend.put(KEY_KEY, uniqueMessageKey);
		// TODO: Migrate to Supabase

		// --- Immediate Actions: Update UI and send to DB ---
		ChatMessageManager.INSTANCE.sendMessageToDb(
				(HashMap<String, Object>) messageToSend,
				senderUid,
				recipientUid,
				uniqueMessageKey,
				is_group
		);

		// Create a copy for local UI to avoid modification by reference
		HashMap<String, Object> localMessage = new HashMap<>(messageToSend);
		localMessage.put("isLocalMessage", true);
		messageKeys.add(uniqueMessageKey);
		ChatMessagesList.add(localMessage);

		if (ChatMessagesList.size() == 1) {
			noChatText.setVisibility(View.GONE);
			ChatMessagesListRecycler.setVisibility(View.VISIBLE);
		}

		int newPosition = ChatMessagesList.size() - 1;
		chatAdapter.notifyItemInserted(newPosition);
		if (newPosition > 0) chatAdapter.notifyItemChanged(newPosition - 1);

		ChatMessagesListRecycler.post(this::scrollToBottom);

		if (localMessage.containsKey(REPLIED_MESSAGE_ID_KEY)) {
			ArrayList<HashMap<String, Object>> singleMessageList = new ArrayList<>();
			singleMessageList.add(localMessage);
			_fetchRepliedMessages(singleMessageList);
		}

		ChatMessageManager.INSTANCE.updateInbox(lastMessageForInbox, recipientUid, is_group, null);

		// Clear UI fields
		message_et.setText("");
		ReplyMessageID = "null";
		mMessageReplyLayout.setVisibility(View.GONE);
		if (!attactmentmap.isEmpty()) {
			resetAttachmentState();
		}

		// --- Background Action: Fetch recipient's notification ID and send notification ---
		final String chatId = ChatMessageManager.INSTANCE.getChatId(senderUid, recipientUid);
		final String senderDisplayName = TextUtils.isEmpty(FirstUserName) ? "Someone" : FirstUserName;
		final String notificationMessage = senderDisplayName + ": " + lastMessageForInbox;

		// TODO: Migrate to Supabase
	}


	public void _Block(final String _uid) {
		block = new HashMap<>();
		block.put(_uid, "true");
		// TODO: Migrate to Supabase
		block.clear();
	}


	public void _TransitionManager(final View _view, final double _duration) {
		LinearLayout viewgroup =(LinearLayout) _view;

		android.transition.AutoTransition autoTransition = new android.transition.AutoTransition(); autoTransition.setDuration((long)_duration); android.transition.TransitionManager.beginDelayedTransition(viewgroup, autoTransition);
	}


	public void _Unblock_this_user() {
		// TODO: Migrate to Supabase
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


			//loading_bar_layout.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)100, 0xFFFFFFFF));
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
		// TODO: Migrate to Supabase
		// Use the correct parameter name '_position' as defined by your More Block
		final int itemPosition = (int) _position;

		// Safety check for position bounds
		if (itemPosition < 0 || itemPosition >= attactmentmap.size()) {
			Log.e("ChatActivity", "Invalid position for upload: " + itemPosition + ", size: " + attactmentmap.size());
			return;
		}

		// Check for internet connection first.
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
							mapToUpdate.put("cloudinaryUrl", url); // Keep this key for consistency in _send_btn
							mapToUpdate.put("publicId", publicId);
							if (rv_attacmentList.getAdapter() != null) {
								rv_attacmentList.getAdapter().notifyItemChanged(itemPosition);
							}

							// Set the URL to the 'path' variable instead of message_et
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


	/**
	 * CRITICAL FIX: Reset attachment state completely to prevent issues with subsequent messages
	 */
	private void resetAttachmentState() {
		Log.d("ChatActivity", "=== RESETTING ATTACHMENT STATE ===");
		
		// Hide the attachment layout
		if (attachmentLayoutListHolder != null) {
			attachmentLayoutListHolder.setVisibility(View.GONE);
		}
		
		// Update the attachment list adapter
		if (rv_attacmentList.getAdapter() != null) {
			int oldSize = attactmentmap.size();
			if (oldSize > 0) {
				attactmentmap.clear();
				rv_attacmentList.getAdapter().notifyItemRangeRemoved(0, oldSize);
			}
		}
		
		// Reset the path variable
		path = "";
		
		Log.d("ChatActivity", "Attachment state reset complete - Map size: " + attactmentmap.size() + ", Path: '" + path + "'");
		Log.d("ChatActivity", "=== ATTACHMENT STATE RESET COMPLETE ===");
	}



	public void _showLoadMoreIndicator() {
		if (!ChatMessagesList.isEmpty() && !ChatMessagesList.get(0).containsKey("isLoadingMore")) {
			HashMap<String, Object> loadingMap = new HashMap<>();
			loadingMap.put("isLoadingMore", true);
			ChatMessagesList.add(0, loadingMap);
			if (chatAdapter != null) {
				chatAdapter.notifyItemInserted(0);
				// Notify the next item as well, as its view might need to change (e.g. remove avatar)
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
		// This is where you trigger your reply UI.
		HashMap<String, Object> messageData = ChatMessagesList.get((int)_position);
		ReplyMessageID = messageData.get(KEY_KEY).toString();

		// TODO: Migrate to Supabase

		mMessageReplyLayoutBodyRightMessage.setText(messageData.get(MESSAGE_TEXT_KEY).toString());
		mMessageReplyLayout.setVisibility(View.VISIBLE);
		vbr.vibrate((long)(48));
	}



	public void _setupSwipeToReply() {
		// This helper class handles drawing the swipe background and icon.
		ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
			@Override
			public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
				return false; // We don't want to handle drag & drop
			}

			@Override
			public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
				int position = viewHolder.getAdapterPosition();
				if (position < 0 || position >= ChatMessagesList.size()) {
					return; // Invalid position, do nothing.
				}

				// Get the item and check if it's a real message with a key.
				HashMap<String, Object> messageData = ChatMessagesList.get(position);
				if (messageData == null || !messageData.containsKey("key") || messageData.get("key") == null) {
					// This is not a real message (e.g., typing indicator, loading view).
					// We just notify the adapter to redraw the item back to its original state.
					chatAdapter.notifyItemChanged(position);
					return;
				}

				// If it's a real message, proceed with the reply UI.
				_showReplyUI(position);
				// Smoothly reset the swiped item back into place
				viewHolder.itemView.animate().translationX(0).setDuration(150).start();
				chatAdapter.notifyItemChanged(position);
			}

			@Override
			public boolean isItemViewSwipeEnabled() {
				return true; // Enable swipe
			}

			@Override
			public boolean isLongPressDragEnabled() {
				return false; // Disable long press drag to allow our custom long press
			}

			@Override
			public void onChildDraw(Canvas c, RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
				if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
					View itemView = viewHolder.itemView;
					Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
					Drawable icon = ContextCompat.getDrawable(getApplicationContext(), R.drawable.ic_reply);
					if (icon != null) {
						// Neutral icon color, no background rectangle
						icon.setColorFilter(0xFF616161, PorterDuff.Mode.SRC_IN);

						int iconMargin = (itemView.getHeight() - icon.getIntrinsicHeight()) / 2;
						int iconTop = itemView.getTop() + iconMargin;
						int iconBottom = iconTop + icon.getIntrinsicHeight();

						float width = (float) itemView.getWidth();
						float threshold = width * 0.25f;
						float progress = Math.min(1f, Math.abs(dX) / threshold);
						icon.setAlpha((int) (Math.max(0.25f, progress) * 255));

						if (dX > 0) { // Swiping to the right
							int iconLeft = itemView.getLeft() + iconMargin;
							int iconRight = itemView.getLeft() + iconMargin + icon.getIntrinsicWidth();
							icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
							icon.draw(c);
						} else { // Swiping to the left
							int iconRight = itemView.getRight() - iconMargin;
							int iconLeft = itemView.getRight() - iconMargin - icon.getIntrinsicWidth();
							icon.setBounds(iconLeft, iconTop, iconRight, iconBottom);
							icon.draw(c);
						}
					}

					// Damped translation for a smoother feel (no alpha fade)
					float dampedDx = dX * 0.75f;
					itemView.setTranslationX(dampedDx);
					itemView.setAlpha(1.0f);
				} else {
					super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
				}
			}

			@Override
			public float getSwipeThreshold(RecyclerView.ViewHolder viewHolder) {
				return 0.25f; // require ~25% swipe to trigger
			}

			@Override
			public float getSwipeEscapeVelocity(float defaultValue) {
				return defaultValue * 1.5f; // slightly higher to avoid accidental triggers
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
			// CRITICAL FIX: Scroll to message with animation and highlight effect
			ChatMessagesListRecycler.smoothScrollToPosition(position);
			
			// Add highlight animation after scroll completes
			new Handler().postDelayed(() -> {
				// CRITICAL FIX: Check if activity is still valid before highlighting
				if (!isFinishing() && !isDestroyed() && ChatMessagesListRecycler != null) {
					RecyclerView.ViewHolder viewHolder = ChatMessagesListRecycler.findViewHolderForAdapterPosition(position);
					if (viewHolder != null) {
						_highlightMessage(viewHolder.itemView);
					}
				}
			}, 500); // Wait for scroll to complete
		} else {
			Toast.makeText(getApplicationContext(), "Original message not found", Toast.LENGTH_SHORT).show();
		}
	}
	
	// CRITICAL FIX: Helper method to find message position
	private int _findMessagePosition(String messageKey) {
		for (int i = 0; i < ChatMessagesList.size(); i++) {
			if (ChatMessagesList.get(i).get(KEY_KEY).toString().equals(messageKey)) {
				return i;
			}
		}
		return -1;
	}

	public void _getGroupReference() {
		// TODO: Migrate to Supabase

		_getChatMessagesRef();
	}

	// CRITICAL FIX: Add highlight animation for replied messages with NPE protection
	private void _highlightMessage(View messageView) {
		// CRITICAL FIX: Check if activity is finishing to prevent crashes
		if (isFinishing() || isDestroyed()) {
			return;
		}
		
		// Store original background safely
		Drawable originalBackground = messageView.getBackground();
		
		// Create highlight animation
		ValueAnimator highlightAnimator = ValueAnimator.ofFloat(0f, 1f);
		highlightAnimator.setDuration(800);
		highlightAnimator.addUpdateListener(animation -> {
			// CRITICAL FIX: Check if activity is still valid during animation
			if (isFinishing() || isDestroyed() || messageView == null) {
				animation.cancel();
				return;
			}
			
			float progress = (Float) animation.getAnimatedValue();
			
			// Create a pulsing highlight effect
			int alpha = (int) (100 * (1 - progress));
			int color = Color.argb(alpha, 107, 76, 255); // Purple with fading alpha
			
			GradientDrawable highlightDrawable = new GradientDrawable();
			highlightDrawable.setColor(color);
			highlightDrawable.setCornerRadius(dpToPx(27)); // Match message bubble corner radius
			
			// CRITICAL FIX: Use setBackgroundDrawable for better compatibility
			messageView.setBackgroundDrawable(highlightDrawable);
		});
		
		highlightAnimator.addListener(new AnimatorListenerAdapter() {
			@Override
			public void onAnimationEnd(Animator animation) {
				// CRITICAL FIX: Safely restore original background
				if (!isFinishing() && !isDestroyed() && messageView != null) {
					messageView.setBackgroundDrawable(originalBackground);
				}
			}
		});
		
		highlightAnimator.start();
	}
	
	private int dpToPx(int dp) {
		// CRITICAL FIX: Safe dp to px conversion with null checks
		try {
			if (getResources() != null && getResources().getDisplayMetrics() != null) {
				return (int) (dp * getResources().getDisplayMetrics().density);
			}
		} catch (Exception e) {
			Log.e("ChatActivity", "Error converting dp to px: " + e.getMessage());
		}
		return dp; // Fallback to dp value
	}

	private void startActivityWithUid(Class<?> activityClass) {
		Intent intent = new Intent(getApplicationContext(), activityClass);
		intent.putExtra(UID_KEY, getIntent().getStringExtra("uid"));
		startActivity(intent);
	}

	private void callGemini(String prompt, boolean showThinking) {
		gemini.setModel(GEMINI_MODEL);
		gemini.setShowThinking(showThinking);
		gemini.setSystemInstruction(
		"You are a concise text assistant. Always return ONLY the transformed text (no explanation, no labels). " +
		"Preserve original formatting. Censor profanity by replacing letters with asterisks (e.g., s***t). " +
		"Keep the language and tone of the input unless asked to change it."
		);
		gemini.sendPrompt(prompt, new Gemini.GeminiCallback() {
			@Override
			public void onSuccess(String response) {
				runOnUiThread(() -> message_et.setText(response));
			}

			@Override
			public void onError(String error) {
				runOnUiThread(() -> message_et.setText("Error: " + error));
			}

			@Override
			public void onThinking() {
				if (showThinking) {
					runOnUiThread(() -> message_et.setText(gemini.getThinkingText()));
				}
			}
		});
	}

	private void callGeminiForSummary(String prompt, final BaseMessageViewHolder viewHolder) {
		AiFeatureParams params = new AiFeatureParams(
				prompt,
				getString(R.string.gemini_system_instruction_summary),
				GEMINI_MODEL,
				getString(R.string.gemini_summary_title),
				"GeminiSummary",
				getString(R.string.gemini_error_summary),
				viewHolder,
				null
		);
		callGeminiForAiFeature(params);
	}

	private void callGeminiForExplanation(String prompt, final BaseMessageViewHolder viewHolder) {
		AiFeatureParams params = new AiFeatureParams(
				prompt,
				getString(R.string.gemini_system_instruction_explanation),
				GEMINI_EXPLANATION_MODEL,
				getString(R.string.gemini_explanation_title),
				"GeminiExplanation",
				getString(R.string.gemini_error_explanation),
				viewHolder,
				1000
		);
		callGeminiForAiFeature(params);
	}

	private static class AiFeatureParams {
		String prompt;
		String systemInstruction;
		String model;
		String bottomSheetTitle;
		String logTag;
		String errorMessage;
		BaseMessageViewHolder viewHolder;
		Integer maxTokens;

		AiFeatureParams(String prompt, String systemInstruction, String model, String bottomSheetTitle, String logTag, String errorMessage, BaseMessageViewHolder viewHolder, Integer maxTokens) {
			this.prompt = prompt;
			this.systemInstruction = systemInstruction;
			this.model = model;
			this.bottomSheetTitle = bottomSheetTitle;
			this.logTag = logTag;
			this.errorMessage = errorMessage;
			this.viewHolder = viewHolder;
			this.maxTokens = maxTokens;
		}
	}


	private void callGeminiForAiFeature(AiFeatureParams params) {
		Gemini.Builder builder = new Gemini.Builder(this)
				.model(params.model)
				.showThinking(true)
				.systemInstruction(params.systemInstruction);

		if (params.maxTokens != null) {
			builder.maxTokens(params.maxTokens);
		}

		Gemini gemini = builder.build();

		gemini.sendPrompt(params.prompt, new Gemini.GeminiCallback() {
			@Override
			public void onSuccess(String response) {
				runOnUiThread(() -> {
					if (params.viewHolder != null) {
						params.viewHolder.stopShimmer();
					}
					ContentDisplayBottomSheetDialogFragment bottomSheet = ContentDisplayBottomSheetDialogFragment.newInstance(response, params.bottomSheetTitle);
					bottomSheet.show(getSupportFragmentManager(), bottomSheet.getTag());
				});
			}

			@Override
			public void onError(String error) {
				runOnUiThread(() -> {
					if (params.viewHolder != null) {
						params.viewHolder.stopShimmer();
					}
					Log.e(TAG, params.logTag + " Error: " + error);
					Toast.makeText(getApplicationContext(), params.errorMessage + error, Toast.LENGTH_SHORT).show();
				});
			}

			@Override
			public void onThinking() {
				runOnUiThread(() -> {
					if (params.viewHolder != null) {
						params.viewHolder.startShimmer();
					}
				});
			}
		});
	}

	private void handleBlocklistUpdate(Object dataSnapshot) {
		// TODO: Migrate to Supabase
	}

	private String getSenderNameForMessage(HashMap<String, Object> message) {
		// TODO: Migrate to Supabase
		return "";
	}

	private void appendMessageToContext(StringBuilder contextBuilder, HashMap<String, Object> message) {
		Object messageTextObj = message.get(MESSAGE_TEXT_KEY);
		String messageText = (messageTextObj != null) ? messageTextObj.toString() : "";
		contextBuilder.append(getSenderNameForMessage(message))
				.append(": ")
				.append(messageText)
				.append("\n");
	}

	private String buildExplanationPrompt(int position, String messageText, HashMap<String, Object> messageData) {
		// Build context strings
		StringBuilder beforeContext = new StringBuilder();
		int startIndex = Math.max(0, position - EXPLAIN_CONTEXT_MESSAGES_BEFORE);
		for (int i = startIndex; i < position; i++) {
			appendMessageToContext(beforeContext, ChatMessagesList.get(i));
		}

		StringBuilder afterContext = new StringBuilder();
		int endIndex = Math.min(ChatMessagesList.size(), position + EXPLAIN_CONTEXT_MESSAGES_AFTER + 1);
		for (int i = position + 1; i < endIndex; i++) {
			appendMessageToContext(afterContext, ChatMessagesList.get(i));
		}

		String senderOfMessageToExplain = getSenderNameForMessage(messageData);

		return getString(R.string.gemini_explanation_prompt,
				SecondUserName,
				beforeContext.toString(),
				senderOfMessageToExplain,
				messageText,
				afterContext.toString());
	}

	private com.synapse.social.studioasinc.util.UserProfileUpdater userProfileUpdater;


//	public class Rv_attacmentListAdapter extends RecyclerView.Adapter<Rv_attacmentListAdapter.ViewHolder> { MOVED to attachments package }

	private void uploadAudioFile() {
		if (audioFilePath != null && !audioFilePath.isEmpty()) {
			File file = new File(audioFilePath);
			if (file.exists()) {
				AsyncUploadService.uploadWithNotification(this, audioFilePath, file.getName(), new AsyncUploadService.UploadProgressListener() {
					@Override
					public void onProgress(String filePath, int percent) {
						// You can optionally show progress here
					}

					@Override
					public void onSuccess(String filePath, String url, String publicId) {
						_sendVoiceMessage(url, (long) recordMs);
					}

					@Override
					public void onFailure(String filePath, String error) {
						Toast.makeText(getApplicationContext(), "Failed to upload audio.", Toast.LENGTH_SHORT).show();
					}
				});
			}
		}
	}

	private void _sendVoiceMessage(String audioUrl, long duration) {
		// TODO: Migrate to Supabase

		ReplyMessageID = "null";
		mMessageReplyLayout.setVisibility(View.GONE);
	}

	@Override
	public String getRecipientUid() {
		return getIntent().getStringExtra("uid");
	}

	public static class ChatMessagesListRecyclerAdapter extends RecyclerView.Adapter<ChatMessagesListRecyclerAdapter.ViewHolder> {

		private final ArrayList<HashMap<String, Object>> data;
		private final Context context;
		private final boolean isGroup;
		// TODO: Migrate to Supabase

		public ChatMessagesListRecyclerAdapter(Context context, ArrayList<HashMap<String, Object>> arr, boolean isGroup) {
			this.data = arr;
			this.context = context;
			this.isGroup = isGroup;
			// TODO: Migrate to Supabase
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

			// TODO: Migrate to Supabase
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