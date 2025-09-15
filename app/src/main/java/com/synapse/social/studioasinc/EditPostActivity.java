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
import androidx.appcompat.widget.SwitchCompat;
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
import java.util.regex.*;
import org.json.*;
import androidx.appcompat.widget.SwitchCompat;
import com.synapse.social.studioasinc.ImageUploader;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import android.content.ClipData;


public class EditPostActivity extends AppCompatActivity {
	
	public final int REQ_CD_IMAGE_PICKER = 101;
	
	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	
	private ProgressDialog SynapseLoadingDialog;
	private String postKey = "";
	private HashMap<String, Object> PostUpdateMap = new HashMap<>();
	private String selectedImagePath = "";
	private boolean hasImage = false;
	private boolean imageChanged = false;
	
	private LinearLayout main;
	private LinearLayout top;
	private LinearLayout topSpace;
	private ScrollView scroll;
	private ImageView back;
	private LinearLayout topSpc;
	private Button updateButton;
	private TextView title;
	private LinearLayout scrollBody;
	private LinearLayout PostInfoTop1;
	private LinearLayout topSpace2;
	private LinearLayout spc2;
	private CardView imageCard;
	private FadeEditText postDescription;
	private ImageView postImageView;
	private LinearLayout imagePlaceholder;
	private LinearLayout settingsButton;
	
	private DatabaseReference maindb = _firebase.getReference("skyline");
	private Calendar cc = Calendar.getInstance();
	private SharedPreferences appSavedData;
	private DatabaseReference fdb = _firebase.getReference("notify");
	
	// Post data from intent
	private String originalPostText = "";
	private String originalPostImage = "";
	private String originalPostType = "";
	
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
		setContentView(R.layout.edit_post);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
		|| ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);} else {
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
		main = findViewById(R.id.main);
		top = findViewById(R.id.top);
		topSpace = findViewById(R.id.topSpace);
		scroll = findViewById(R.id.scroll);
		back = findViewById(R.id.back);
		topSpc = findViewById(R.id.topSpc);
		updateButton = findViewById(R.id.updateButton);
		title = findViewById(R.id.title);
		scrollBody = findViewById(R.id.scrollBody);
		PostInfoTop1 = findViewById(R.id.PostInfoTop1);
		topSpace2 = findViewById(R.id.topSpace2);
		spc2 = findViewById(R.id.spc2);
		imageCard = findViewById(R.id.imageCard);
		postDescription = findViewById(R.id.postDescription);
		postImageView = findViewById(R.id.postImageView);
		imagePlaceholder = findViewById(R.id.imagePlaceholder);
		settingsButton = findViewById(R.id.settingsButton);
		appSavedData = getSharedPreferences("data", Activity.MODE_PRIVATE);
		
		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				onBackPressed();
			}
		});
		
		updateButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_updatePost();
			}
		});
		
		imagePlaceholder.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_openImagePicker();
			}
		});
		
		settingsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_showPostSettingsBottomSheet();
			}
		});
		
		postDescription.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				_TransitionManager(PostInfoTop1, 130);
			}
			
			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				
			}
			
			@Override
			public void afterTextChanged(Editable _param1) {
				
			}
		});
	}
	
	private void initializeLogic() {
		_setStatusBarColor(true, 0xFFFFFFFF, 0xFFFFFFFF);
		_viewGraphics(back, 0xFFFFFFFF, 0xFFE0E0E0, 300, 0, Color.TRANSPARENT);
		_viewGraphics(settingsButton, 0xFFFFFFFF, 0xFFE0E0E0, 300, 0, Color.TRANSPARENT);
		imageCard.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)22, (int)2, 0xFFEEEEEE, 0xFFFFFFFF));
		postDescription.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFEEEEEE, 0xFFFFFFFF));
		_viewGraphics(imagePlaceholder, 0xFFFFFFFF, 0xFFEEEEEE, 0, 0, Color.TRANSPARENT);
		
		// Get post data from intent
		if (getIntent().hasExtra("postKey")) {
			postKey = getIntent().getStringExtra("postKey");
		}
		if (getIntent().hasExtra("postText")) {
			originalPostText = getIntent().getStringExtra("postText");
			postDescription.setText(originalPostText);
		}
		if (getIntent().hasExtra("postImage")) {
			originalPostImage = getIntent().getStringExtra("postImage");
			if (originalPostImage != null && !originalPostImage.isEmpty()) {
				hasImage = true;
				_loadOriginalImage();
			}
		}
		if (getIntent().hasExtra("postType")) {
			originalPostType = getIntent().getStringExtra("postType");
		}
		
		// Load post settings from intent
		if (getIntent().hasExtra("hideViewsCount")) {
			hideViewsCount = getIntent().getBooleanExtra("hideViewsCount", false);
		}
		if (getIntent().hasExtra("hideLikesCount")) {
			hideLikesCount = getIntent().getBooleanExtra("hideLikesCount", false);
		}
		if (getIntent().hasExtra("hideCommentsCount")) {
			hideCommentsCount = getIntent().getBooleanExtra("hideCommentsCount", false);
		}
		if (getIntent().hasExtra("hidePostFromEveryone")) {
			hidePostFromEveryone = getIntent().getBooleanExtra("hidePostFromEveryone", false);
		}
		if (getIntent().hasExtra("disableSaveToFavorites")) {
			disableSaveToFavorites = getIntent().getBooleanExtra("disableSaveToFavorites", false);
		}
		if (getIntent().hasExtra("disableComments")) {
			disableComments = getIntent().getBooleanExtra("disableComments", false);
		}
	}
	
	@Override
	protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);
		
		switch (_requestCode) {
			case REQ_CD_IMAGE_PICKER:
			if (_resultCode == Activity.RESULT_OK) {
				ArrayList<String> _filePath = new ArrayList<>();
				if (_data != null) {
					if (_data.getClipData() != null) {
						for (int _index = 0; _index < _data.getClipData().getItemCount(); _index++) {
							ClipData.Item _item = _data.getClipData().getItemAt(_index);
							_filePath.add(FileUtil.convertUriToFilePath(getApplicationContext(), _item.getUri()));
						}
					}
					else {
						_filePath.add(FileUtil.convertUriToFilePath(getApplicationContext(), _data.getData()));
					}
				}
				selectedImagePath = _filePath.get((int)(0));
				hasImage = true;
				imageChanged = true;
				_loadSelectedImage();
			}
			else {
				
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
				startActivityForResult(sendImgInt, REQ_CD_IMAGE_PICKER);
			}
		} else {
			Intent sendImgInt = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
			startActivityForResult(sendImgInt, REQ_CD_IMAGE_PICKER);
		}
	}
	
	private void _loadOriginalImage() {
		if (hasImage && originalPostImage != null) {
			imagePlaceholder.setVisibility(View.GONE);
			postImageView.setVisibility(View.VISIBLE);
			Glide.with(getApplicationContext()).load(originalPostImage).into(postImageView);
		}
	}
	
	private void _loadSelectedImage() {
		if (hasImage && selectedImagePath != null) {
			imagePlaceholder.setVisibility(View.GONE);
			postImageView.setVisibility(View.VISIBLE);
			Glide.with(getApplicationContext()).load(selectedImagePath).into(postImageView);
		}
	}
	
	private void _updatePost() {
		if (postDescription.getText().toString().trim().equals("") && !hasImage) {
			Toast.makeText(getApplicationContext(), "Please add some text or an image to your post", Toast.LENGTH_SHORT).show();
			return;
		}
		
		_LoadingDialog(true);
		
		if (imageChanged && hasImage) {
			// Upload new image first, then update post
			ImageUploader.uploadImage(selectedImagePath, new ImageUploader.UploadCallback() {
				@Override
				public void onUploadComplete(String imageUrl) {
					_updatePostInDatabase(imageUrl);
				}
				
				@Override
				public void onUploadError(String errorMessage) {
					_LoadingDialog(false);
					Toast.makeText(getApplicationContext(), "Failed to upload image: " + errorMessage, Toast.LENGTH_SHORT).show();
				}
			});
		} else {
			// Update post without changing image
			_updatePostInDatabase(originalPostImage);
		}
	}
	
	private void _updatePostInDatabase(String imageUrl) {
		cc = Calendar.getInstance();
		PostUpdateMap = new HashMap<>();
		
		// Check if user is logged in
		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			Toast.makeText(getApplicationContext(), "User not logged in", Toast.LENGTH_SHORT).show();
			_LoadingDialog(false);
			return;
		}
		
		if (hasImage) {
			PostUpdateMap.put("post_type", "IMAGE");
			PostUpdateMap.put("post_image", imageUrl);
		} else {
			PostUpdateMap.put("post_type", "TEXT");
			PostUpdateMap.put("post_image", "");
		}
		
		if (!postDescription.getText().toString().trim().equals("")) {
			PostUpdateMap.put("post_text", postDescription.getText().toString().trim());
		} else {
			PostUpdateMap.put("post_text", "");
		}
		
		// Apply post settings
		PostUpdateMap.put("post_hide_views_count", hideViewsCount);
		PostUpdateMap.put("post_hide_like_count", hideLikesCount);
		PostUpdateMap.put("post_hide_comments_count", hideCommentsCount);
		
		if (hidePostFromEveryone) {
			PostUpdateMap.put("post_visibility", "private");
		} else {
			PostUpdateMap.put("post_visibility", "public");
		}
		
		PostUpdateMap.put("post_disable_favorite", disableSaveToFavorites);
		PostUpdateMap.put("post_disable_comments", disableComments);
		PostUpdateMap.put("last_edited", String.valueOf((long)(cc.getTimeInMillis())));
		
		FirebaseDatabase.getInstance().getReference("skyline/posts").child(postKey).updateChildren(PostUpdateMap, new DatabaseReference.CompletionListener() {
			@Override
			public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
				if (databaseError == null) {
					Toast.makeText(getApplicationContext(), "Post updated successfully", Toast.LENGTH_SHORT).show();
					_LoadingDialog(false);
					finish();
				} else {
					Toast.makeText(getApplicationContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
					_LoadingDialog(false);
				}
			}
		});
	}
	
	private void _showPostSettingsBottomSheet() {
		BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
		View bottomSheetView = getLayoutInflater().inflate(R.layout.bottomsheet_post_settings, null);
		bottomSheetDialog.setContentView(bottomSheetView);
		
		// Initialize switches
		com.google.android.material.materialswitch.MaterialSwitch hideViewsSwitch = bottomSheetView.findViewById(R.id.hideViewsSwitch);
		com.google.android.material.materialswitch.MaterialSwitch hideLikesSwitch = bottomSheetView.findViewById(R.id.hideLikesSwitch);
		com.google.android.material.materialswitch.MaterialSwitch hideCommentsSwitch = bottomSheetView.findViewById(R.id.hideCommentsSwitch);
		com.google.android.material.materialswitch.MaterialSwitch hidePostSwitch = bottomSheetView.findViewById(R.id.hidePostSwitch);
		com.google.android.material.materialswitch.MaterialSwitch disableSaveSwitch = bottomSheetView.findViewById(R.id.disableSaveSwitch);
		com.google.android.material.materialswitch.MaterialSwitch disableCommentsSwitch = bottomSheetView.findViewById(R.id.disableCommentsSwitch);
		
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
		
		bottomSheetDialog.show();
	}

	public void _setStatusBarColor(final boolean _isLight, final int _stateColor, final int _navigationColor) {
		if (_isLight) {
			getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		}
		getWindow().setStatusBarColor(_stateColor);
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
	
	
	public void _TransitionManager(final View _view, final double _duration) {
		LinearLayout viewgroup =(LinearLayout) _view;
		
		android.transition.AutoTransition autoTransition = new android.transition.AutoTransition(); autoTransition.setDuration((long)_duration); android.transition.TransitionManager.beginDelayedTransition(viewgroup, autoTransition);
	}
	
}