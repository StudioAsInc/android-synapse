package com.synapse.social.studioasinc;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.os.Bundle;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.gridlayout.*;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.bumptech.glide.Glide;
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService;
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService;
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService;
import com.theartofdev.edmodo.cropper.*;
import com.yalantis.ucrop.*;
import java.io.*;
import java.io.File;
import java.io.InputStream;
import java.text.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;



public class CreateLineVideoNextStepActivity extends AppCompatActivity {

	private ProgressDialog SynapseLoadingDialog;
	private HashMap<String, Object> PostSendMap = new HashMap<>();
	private String UniquePostKey = "";
	private HashMap<String, Object> m = new HashMap<>();
	private String filePath = "";
	private String extension = "";
	private String fileSize = "";
	private String fileName = "";

	private androidx.constraintlayout.widget.ConstraintLayout top;
	private View topSpace;
	private ScrollView scroll;
	private ImageView back;
	private TextView continueButton;
	private TextView title;
	private TextView subtitle;
	private androidx.constraintlayout.widget.ConstraintLayout scrollBody;
	private View topSpace2;
	private EditText postDescription;
	private RecyclerView recyclerview1;

	private IAuthenticationService authService;
	private IDatabaseService dbService;
	private Intent intent = new Intent();
	private Calendar cc = Calendar.getInstance();
	private SharedPreferences appSavedData;
	private SharedPreferences theme;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_create_line_video_next_step);
		initialize(_savedInstanceState);

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
		authService = ((SynapseApp) getApplication()).getAuthService();
		dbService = ((SynapseApp) getApplication()).getDbService();
		top = findViewById(R.id.top);
		topSpace = findViewById(R.id.topSpace);
		scroll = findViewById(R.id.scroll);
		back = findViewById(R.id.back);
		continueButton = findViewById(R.id.continueButton);
		title = findViewById(R.id.title);
		subtitle = findViewById(R.id.subtitle);
		scrollBody = findViewById(R.id.scrollBody);
		topSpace2 = findViewById(R.id.topSpace2);
		postDescription = findViewById(R.id.postDescription);
		recyclerview1 = findViewById(R.id.recyclerview1);
		appSavedData = getSharedPreferences("data", Activity.MODE_PRIVATE);
		theme = getSharedPreferences("theme", Activity.MODE_PRIVATE);

		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				onBackPressed();
			}
		});

		continueButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (getIntent().hasExtra("type")) {
					if (getIntent().getStringExtra("type").equals("local")) {
						_uploadLineVideo(getIntent().getStringExtra("path"), false);
					} else {
						_uploadLineVideo(getIntent().getStringExtra("path"), true);
					}
				} else {
					if (getIntent().hasExtra("path")) {
						_uploadLineVideo(getIntent().getStringExtra("path"), false);
					}
				}
			}
		});

		postDescription.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				if (_charSeq.length() == 0) {
					_TransitionManager(scrollBody, 200);
				} else {
					_TransitionManager(scrollBody, 200);
				}
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
		if (getIntent().getStringExtra("path") == null || getIntent().getStringExtra("path").isEmpty()) {
			continueButton.setEnabled(false);
			Toast.makeText(this, "No video selected", Toast.LENGTH_SHORT).show();
		}
	}

	@Override
	public void onBackPressed() {
		finish();
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


	public void _viewGraphics(final View _view, final int _onFocus, final int _onRipple, final double _radius, final double _stroke, final int _strokeColor) {
		android.graphics.drawable.GradientDrawable GG = new android.graphics.drawable.GradientDrawable();
		GG.setColor(_onFocus);
		GG.setCornerRadius((float)_radius);
		GG.setStroke((int) _stroke, _strokeColor);
		android.graphics.drawable.RippleDrawable RE = new android.graphics.drawable.RippleDrawable(new android.content.res.ColorStateList(new int[][]{new int[]{}}, new int[]{ _onRipple}), GG, null);
		_view.setBackground(RE);
	}


	public void _stateColor(final int _statusColor, final int _navigationColor) {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(_statusColor);
		getWindow().setNavigationBarColor(_navigationColor);
	}


	public void _ImageColor(final ImageView _image, final int _color) {
		_image.setColorFilter(_color,PorterDuff.Mode.SRC_ATOP);
	}


	public void _uploadLineVideo(final String _path, final boolean _isUrl) {
		if (_isUrl) {
			_LoadingDialog(true);
			Log.d("CreateLineVideo", "_uploadLineVideo(url) - NOT IMPLEMENTED");
			cc = Calendar.getInstance();
			PostSendMap = new HashMap<>();
			PostSendMap.put("key", UniquePostKey);
			if (authService.getCurrentUser() != null) {
				PostSendMap.put("uid", authService.getCurrentUser().getUid());
			}
			PostSendMap.put("post_type", "LINE_VIDEO");
			if (!postDescription.getText().toString().trim().equals("")) {
				PostSendMap.put("post_text", postDescription.getText().toString().trim());
			}
			PostSendMap.put("videoUri", _path);
			if (!appSavedData.contains("user_region_data") && appSavedData.getString("user_region_data", "").equals("none")) {
				PostSendMap.put("post_region", "none");
			} else {
				PostSendMap.put("post_region", appSavedData.getString("user_region_data", ""));
			}
			PostSendMap.put("publish_date", String.valueOf((long)(cc.getTimeInMillis())));
			Log.d("CreateLineVideo", "Submitting post - NOT IMPLEMENTED");

		} else {
			Log.d("CreateLineVideo", "_uploadLineVideo(local) - NOT IMPLEMENTED");
		}
	}


	public void _TransitionManager(final View _view, final double _duration) {
		android.view.ViewGroup viewgroup = (android.view.ViewGroup) _view;
		
		android.transition.AutoTransition autoTransition = new android.transition.AutoTransition(); autoTransition.setDuration((long)_duration); android.transition.TransitionManager.beginDelayedTransition(viewgroup, autoTransition);
	}

}