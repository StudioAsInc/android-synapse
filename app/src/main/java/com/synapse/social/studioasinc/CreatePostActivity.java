package com.synapse.social.studioasinc;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.os.Vibrator;
import android.text.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.browser.*;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.gridlayout.*;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.synapse.social.studioasinc.FadeEditText;
import com.theartofdev.edmodo.cropper.*;
import com.yalantis.ucrop.*;
import java.io.*;
import java.io.File;
import java.text.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.*;
import org.json.*;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.appbar.MaterialToolbar;
import com.synapse.social.studioasinc.ImageUploader;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.content.ClipData;
import com.synapse.social.studioasinc.database.DatabaseStructure;
import com.synapse.social.studioasinc.database.ContentManager;
import com.synapse.social.studioasinc.util.AuthUtil;


public class CreatePostActivity extends AppCompatActivity {
	
	public final int REQ_CD_IMAGE_PICKER = 101;
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	private FirebaseStorage _firebase_storage = FirebaseStorage.getInstance();
	
	private ProgressDialog SynapseLoadingDialog;
	private String UniquePostKey = "";
	private HashMap<String, Object> PostSendMap = new HashMap<>();
	private String selectedImagePath = "";
	private boolean hasImage = false;
	
	private MaterialToolbar toolbar;
	private Button postButton;
	private Button settingsButton;
	private ScrollView scrollView;
	private LinearLayout scrollBodyLayout;
	private FadeEditText postDescriptionEditText;
	private CardView imageCardView;
	private LinearLayout imagePlaceholderLayout;
	private ImageView postImageView;

	private DatabaseReference maindb = _firebase.getReference("skyline");
	private Calendar cc = Calendar.getInstance();
	private SharedPreferences appSavedData;
	private DatabaseReference fdb = _firebase.getReference("notify");
	
	// Post settings variables
	private boolean hideViewsCount = false;
	private boolean hideLikesCount = false;
	private boolean hideCommentsCount = false;
	private boolean hidePostFromEveryone = false;
	private boolean disableSaveToFavorites = false;
	private boolean disableComments = false;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_create_post);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
		|| ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);} else {
			initializeLogic();
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (FirebaseAuth.getInstance().getCurrentUser() != null) {
			String currentUid = AuthUtil.getCurrentUserUid();
			if (currentUid != null) {
				PresenceManager.setActivity(currentUid, "Creating a post");
			}
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1000) {
			initializeLogic();
		}
	}
	
	private void initialize(Bundle _savedInstanceState) {
		toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		toolbar.setNavigationOnClickListener(v -> onBackPressed());

		scrollView = findViewById(R.id.scrollView);
		scrollBodyLayout = findViewById(R.id.scrollBodyLayout);
		postDescriptionEditText = findViewById(R.id.postDescriptionEditText);
		imageCardView = findViewById(R.id.imageCardView);
		imagePlaceholderLayout = findViewById(R.id.imagePlaceholderLayout);
		postImageView = findViewById(R.id.postImageView);
		postButton = findViewById(R.id.postButton);
		settingsButton = findViewById(R.id.settingsButton);
		appSavedData = getSharedPreferences("data", Activity.MODE_PRIVATE);
		
		postButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_createPost();
			}
		});

		settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_showPostSettingsBottomSheet();
			}
		});

		imagePlaceholderLayout.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_openImagePicker();
			}
		});
		
		postImageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				final CharSequence[] items = { "Change Image", "Remove Image", "Cancel" };
				AlertDialog.Builder builder = new AlertDialog.Builder(CreatePostActivity.this);
				builder.setTitle("Image Options");
				builder.setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int item) {
						if (items[item].equals("Change Image")) {
							_openImagePicker();
						} else if (items[item].equals("Remove Image")) {
							hasImage = false;
							selectedImagePath = "";
							postImageView.setVisibility(View.GONE);
							imagePlaceholderLayout.setVisibility(View.VISIBLE);
							postImageView.setImageDrawable(null);
						} else if (items[item].equals("Cancel")) {
							dialog.dismiss();
						}
					}
				});
				builder.show();
			}
		});
	}
	
	private void initializeLogic() {
		_setStatusBarColor();
		// Check if we have an image from intent
		if (getIntent().hasExtra("type") && getIntent().hasExtra("path")) {
			selectedImagePath = getIntent().getStringExtra("path");
			hasImage = true;
			_loadSelectedImage();
		}
	}

	private void _setStatusBarColor() {
		TypedValue typedValue = new TypedValue();
		getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
		int color = typedValue.data;
		getWindow().setStatusBarColor(color);

		int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
		if (currentNightMode == Configuration.UI_MODE_NIGHT_NO) {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		}
	}
	
	@Override
	protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);
		
		switch (_requestCode) {
			case REQ_CD_IMAGE_PICKER:
			if (_resultCode == Activity.RESULT_OK && _data != null) {
				String imagePath = null;
				
				if (_data.getClipData() != null) {
					// Multiple images selected, use only the first one
					ClipData.Item item = _data.getClipData().getItemAt(0);
					imagePath = FileUtil.convertUriToFilePath(getApplicationContext(), item.getUri());
				} else if (_data.getData() != null) {
					// Single image selected
					imagePath = FileUtil.convertUriToFilePath(getApplicationContext(), _data.getData());
				}
				
				if (imagePath != null) {
					selectedImagePath = imagePath;
					hasImage = true;
					_loadSelectedImage();
				}
			}
			break;
			default:
			break;
		}
	}
	
	@Override
	public void onBackPressed() {
		finish();
	}
	
	private void _openImagePicker() {
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_DENIED || checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) == android.content.pm.PackageManager.PERMISSION_DENIED) {
				requestPermissions(new String[] {android.Manifest.permission.READ_EXTERNAL_STORAGE,android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
			} else {
				Intent sendImgInt = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
				sendImgInt.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
				startActivityForResult(sendImgInt, REQ_CD_IMAGE_PICKER);
			}
		} else {
			Intent sendImgInt = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			sendImgInt.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
			startActivityForResult(sendImgInt, REQ_CD_IMAGE_PICKER);
		}
	}
	
	private void _loadSelectedImage() {
		if (hasImage && selectedImagePath != null) {
			imagePlaceholderLayout.setVisibility(View.GONE);
			postImageView.setVisibility(View.VISIBLE);
			Glide.with(getApplicationContext()).load(selectedImagePath).into(postImageView);
		}
	}
	
	private void _createPost() {
		if (postDescriptionEditText.getText().toString().trim().equals("") && !hasImage) {
			Toast.makeText(getApplicationContext(), "Please add some text or an image to your post", Toast.LENGTH_SHORT).show();
			return;
		}
		
		UniquePostKey = maindb.push().getKey();
		_LoadingDialog(true);
		
		if (hasImage) {
			// Upload image first, then create post
			ImageUploader.uploadImage(selectedImagePath, new ImageUploader.UploadCallback() {
				@Override
				public void onUploadComplete(String imageUrl) {
					_savePostToDatabase(imageUrl);
				}
				
				@Override
				public void onUploadError(String errorMessage) {
					_LoadingDialog(false);
					Toast.makeText(getApplicationContext(), "Failed to upload image: " + errorMessage, Toast.LENGTH_SHORT).show();
				}
			});
		} else {
			// Create text-only post
			_savePostToDatabase(null);
		}
	}
	
	private void _savePostToDatabase(String imageUrl) {
		// Check if user is logged in
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			Toast.makeText(getApplicationContext(), "User not logged in", Toast.LENGTH_SHORT).show();
			_LoadingDialog(false);
			return;
		}
		
		String caption = postDescriptionEditText.getText().toString().trim();
		String visibility = hidePostFromEveryone ? DatabaseStructure.VISIBILITY_PRIVATE : DatabaseStructure.VISIBILITY_PUBLIC;
		
		// Prepare media items if image exists
		List<Map<String, Object>> mediaItems = null;
		if (hasImage && imageUrl != null) {
			mediaItems = new ArrayList<>();
			Map<String, Object> mediaItem = new HashMap<>();
			mediaItem.put("type", "image");
			mediaItem.put("url", imageUrl);
			mediaItems.add(mediaItem);
		}
		
		// Extract hashtags from caption
		Map<String, Boolean> topicTags = null;
		if (!caption.isEmpty()) {
			topicTags = new HashMap<>();
			String[] words = caption.split("\\s+");
			for (String word : words) {
				if (word.startsWith("#") && word.length() > 1) {
					String tag = word.substring(1).toLowerCase();
					topicTags.put(tag, true);
				}
			}
		}
		
		// Create post using ContentManager
		ContentManager.createPost(
			currentUser.getUid(),
			caption,
			visibility,
			mediaItems,
			null, // tagged users
			topicTags,
			null  // location
		).addOnCompleteListener(task -> {
			if (task.isSuccessful()) {
				String postId = task.getResult();
				// Update post config for additional settings
				Map<String, Object> configUpdates = new HashMap<>();
				configUpdates.put("config/comments_disabled", disableComments);
				configUpdates.put("config/likes_hidden", hideLikesCount);
				
				DatabaseStructure.getContentRef().child(postId).updateChildren(configUpdates)
					.addOnCompleteListener(updateTask -> {
						_sendNotificationsToFollowers(postId, caption);
						Toast.makeText(getApplicationContext(), getResources().getString(R.string.post_publish_success), Toast.LENGTH_SHORT).show();
						_LoadingDialog(false);
						finish();
					});
			} else {
				String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
				Toast.makeText(getApplicationContext(), error, Toast.LENGTH_SHORT).show();
				_LoadingDialog(false);
			}
		});
	}
	
	private void _sendNotificationsToFollowers(String postKey, final String postText) {
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			return;
		}
		String currentUid = currentUser.getUid();
		com.synapse.social.studioasinc.util.UserUtils.getUserDisplayName(currentUid, new com.synapse.social.studioasinc.util.UserUtils.Callback<String>() {
			public void onResult(String senderName) {
				// Send notification to followers
				DatabaseReference followersRef = FirebaseDatabase.getInstance().getReference("skyline/users").child(currentUid).child("followers");
				followersRef.addListenerForSingleValueEvent(new ValueEventListener() {
					@Override
					public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
						for (DataSnapshot followerSnapshot : dataSnapshot.getChildren()) {
							String followerUid = followerSnapshot.getKey();
							if (followerUid != null) {
								String message = senderName + " has a new post";
								HashMap<String, String> data = new HashMap<>();
								data.put("postId", postKey);
								NotificationHelper.sendNotification(
									followerUid,
									currentUid,
									message,
									NotificationConfig.NOTIFICATION_TYPE_NEW_POST,
									data
								);
							}
						}
					}

					@Override
					public void onCancelled(@NonNull DatabaseError databaseError) {
						// Handle error
					}
				});

				// Send notification to mentioned users
				Pattern pattern = Pattern.compile("@(\\w+)");
				Matcher matcher = pattern.matcher(postText);
				while (matcher.find()) {
					String username = matcher.group(1);
					DatabaseReference usernameRef = FirebaseDatabase.getInstance().getReference("skyline/usernames").child(username);
					usernameRef.addListenerForSingleValueEvent(new ValueEventListener() {
						@Override
						public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
							if (dataSnapshot.exists()) {
								String recipientUid = dataSnapshot.child("uid").getValue(String.class);
								if (recipientUid != null && !recipientUid.equals(currentUid)) {
									String message = senderName + " mentioned you in a post";
									HashMap<String, String> data = new HashMap<>();
									data.put("postId", postKey);
									NotificationHelper.sendNotification(
										recipientUid,
										currentUid,
										message,
										"mention_post",
										data
									);
								}
							}
						}

						@Override
						public void onCancelled(@NonNull DatabaseError databaseError) {
						}
					});
				}
			}

			public void onCancelled(@NonNull DatabaseError databaseError) {
				// Handle error
			}
		});
	}

	private void _showPostSettingsBottomSheet() {
		BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
		View bottomSheetView = getLayoutInflater().inflate(R.layout.bottomsheet_post_settings, null);
		bottomSheetDialog.setContentView(bottomSheetView);
		
		// Initialize switches
		MaterialSwitch hideViewsSwitch = bottomSheetView.findViewById(R.id.hideViewsSwitch);
		MaterialSwitch hideLikesSwitch = bottomSheetView.findViewById(R.id.hideLikesSwitch);
		MaterialSwitch hideCommentsSwitch = bottomSheetView.findViewById(R.id.hideCommentsSwitch);
		MaterialSwitch hidePostSwitch = bottomSheetView.findViewById(R.id.hidePostSwitch);
		MaterialSwitch disableSaveSwitch = bottomSheetView.findViewById(R.id.disableSaveSwitch);
		MaterialSwitch disableCommentsSwitch = bottomSheetView.findViewById(R.id.disableCommentsSwitch);
		Button doneButton = bottomSheetView.findViewById(R.id.doneButton);
		
		// Set current values
		hideViewsSwitch.setChecked(hideViewsCount);
		hideLikesSwitch.setChecked(hideLikesCount);
		hideCommentsSwitch.setChecked(hideCommentsCount);
		hidePostSwitch.setChecked(hidePostFromEveryone);
		disableSaveSwitch.setChecked(disableSaveToFavorites);
		disableCommentsSwitch.setChecked(disableComments);
		
		// Set listeners
		hideViewsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				hideViewsCount = isChecked;
			}
		});
		
		hideLikesSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				hideLikesCount = isChecked;
			}
		});
		
		hideCommentsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				hideCommentsCount = isChecked;
			}
		});
		
		hidePostSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				hidePostFromEveryone = isChecked;
				if (isChecked) {
					disableCommentsSwitch.setChecked(true);
					disableCommentsSwitch.setEnabled(false);
				} else {
					disableCommentsSwitch.setChecked(false);
					disableCommentsSwitch.setEnabled(true);
				}
			}
		});
		
		disableSaveSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				disableSaveToFavorites = isChecked;
			}
		});
		
		disableCommentsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				disableComments = isChecked;
			}
		});

		doneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				bottomSheetDialog.dismiss();
			}
		});
		
		bottomSheetDialog.show();
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
}