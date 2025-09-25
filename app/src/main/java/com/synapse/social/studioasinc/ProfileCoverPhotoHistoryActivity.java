package com.synapse.social.studioasinc;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.synapse.social.studioasinc.backend.AuthenticationService;
import com.synapse.social.studioasinc.backend.DatabaseService;
import com.synapse.social.studioasinc.services.FileUploaderService;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class ProfileCoverPhotoHistoryActivity extends AppCompatActivity {

	private DatabaseService dbService;
	private AuthenticationService authService;
	private FileUploaderService fileUploaderService;

	private ProgressDialog SynapseLoadingDialog;
	private FloatingActionButton _fab;
	private String CurrentAvatarUri = "";
	private HashMap<String, Object> mAddProfilePhotoMap = new HashMap<>();
	private HashMap<String, Object> mSendMap = new HashMap<>();

	private ArrayList<HashMap<String, Object>> ProfileHistoryList = new ArrayList<>();

	private LinearLayout main;
	private LinearLayout top;
	private LinearLayout body;
	private ImageView back;
	private TextView title;
	private SwipeRefreshLayout mSwipeLayout;
	private LinearLayout mLoadingBody;
	private LinearLayout mLoadedBody;
	private LinearLayout isDataExistsLayout;
	private LinearLayout isDataNotExistsLayout;
	private RecyclerView ProfilePhotosHistoryList;
	private TextView isDataNotExistsLayoutTitle;
	private TextView isDataNotExistsLayoutSubTitle;
	private ProgressBar mLoadingBar;

	private Calendar cc = Calendar.getInstance();

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_profile_cover_photo_history);
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
	    dbService = new DatabaseService();
	    authService = new AuthenticationService();
		fileUploaderService = new FileUploaderService();
		_fab = findViewById(R.id._fab);
		main = findViewById(R.id.main);
		top = findViewById(R.id.top);
		body = findViewById(R.id.body);
		back = findViewById(R.id.back);
		title = findViewById(R.id.title);
		mSwipeLayout = findViewById(R.id.mSwipeLayout);
		mLoadingBody = findViewById(R.id.mLoadingBody);
		mLoadedBody = findViewById(R.id.mLoadedBody);
		isDataExistsLayout = findViewById(R.id.isDataExistsLayout);
		isDataNotExistsLayout = findViewById(R.id.isDataNotExistsLayout);
		ProfilePhotosHistoryList = findViewById(R.id.ProfilePhotosHistoryList);
		isDataNotExistsLayoutTitle = findViewById(R.id.isDataNotExistsLayoutTitle);
		isDataNotExistsLayoutSubTitle = findViewById(R.id.isDataNotExistsLayoutSubTitle);
		mLoadingBar = findViewById(R.id.mLoadingBar);

		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				onBackPressed();
			}
		});

		mSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				_getReference();
			}
		});

		_fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_addProfilePhotoUrlDialog();
			}
		});
	}

	private void initializeLogic() {
		_stateColor(0xFFFFFFFF, 0xFFFFFFFF);
		_viewGraphics(back, 0xFFFFFFFF, 0xFFE0E0E0, 300, 0, Color.TRANSPARENT);
		top.setElevation((float)4);
		ProfilePhotosHistoryList.setLayoutManager(new LinearLayoutManager(this));
		ProfilePhotosHistoryList.setAdapter(new ProfilePhotosHistoryListAdapter(ProfileHistoryList));
		_getReference();
	}

	@Override
	public void onBackPressed() {
		finish();
	}

	public void _stateColor(final int _statusColor, final int _navigationColor) {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(_statusColor);
		getWindow().setNavigationBarColor(_navigationColor);
	}


	public void _ImageColor(final ImageView _image, final int _color) {
		_image.setColorFilter(_color,PorterDuff.Mode.SRC_ATOP);
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
			
		} else {
			if (SynapseLoadingDialog != null){
				SynapseLoadingDialog.dismiss();
			}
		}

	}


	public void _getReference() {
		isDataExistsLayout.setVisibility(View.GONE);
		isDataNotExistsLayout.setVisibility(View.GONE);
		mSwipeLayout.setVisibility(View.GONE);
		mLoadingBody.setVisibility(View.VISIBLE);
		dbService.addValueEventListener("skyline/users/" + authService.getCurrentUser().getUid(), new DatabaseService.DataListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				if(dataSnapshot.exists()) {
					CurrentAvatarUri = dataSnapshot.child("avatar").getValue(String.class);
				} else {
				}
			}
			
			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
				
			}
		});
		Query getProfileHistoryRef = dbService.getReference("skyline/cover-image-history").child(authService.getCurrentUser().getUid());
		dbService.getData(getProfileHistoryRef, new DatabaseService.DataListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				if(dataSnapshot.exists()) {
					isDataExistsLayout.setVisibility(View.VISIBLE);
					isDataNotExistsLayout.setVisibility(View.GONE);
					mSwipeLayout.setVisibility(View.VISIBLE);
					mLoadingBody.setVisibility(View.GONE);
					ProfileHistoryList.clear();
					try {
						GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
						for (DataSnapshot _data : dataSnapshot.getChildren()) {
							HashMap<String, Object> _map = _data.getValue(_ind);
							ProfileHistoryList.add(_map);
						}
					} catch (Exception _e) {
						_e.printStackTrace();
					}
					
					SketchwareUtil.sortListMap(ProfileHistoryList, "upload_date", false, false);
					ProfilePhotosHistoryList.getAdapter().notifyDataSetChanged();
				} else {
					isDataExistsLayout.setVisibility(View.GONE);
					isDataNotExistsLayout.setVisibility(View.VISIBLE);
					mSwipeLayout.setVisibility(View.VISIBLE);
					mLoadingBody.setVisibility(View.GONE);
				}
			}
			
			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
				
			}
		});
		mSwipeLayout.setRefreshing(false);
	}


	public boolean _checkValidUrl(final String _url) {
		try {
			new URL(_url);
			return true;
		} catch (MalformedURLException e) {
			return false;
		}
	}


	public void _addProfilePhotoUrlDialog() {
		{
			final AlertDialog NewCustomDialog = new AlertDialog.Builder(ProfileCoverPhotoHistoryActivity.this).create();
			LayoutInflater NewCustomDialogLI = getLayoutInflater();
			View NewCustomDialogCV = (View) NewCustomDialogLI.inflate(R.layout.dialog_profile_cover_image_history_add, null);
			NewCustomDialog.setView(NewCustomDialogCV);
			NewCustomDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			
			final androidx.cardview.widget.CardView dialog_card = NewCustomDialogCV.findViewById(R.id.dialog_card);
			final androidx.cardview.widget.CardView user_avatar_card = NewCustomDialogCV.findViewById(R.id.user_avatar_card);
			final EditText user_avatar_url_input = NewCustomDialogCV.findViewById(R.id.user_avatar_url_input);
			final ImageView user_avatar_image = NewCustomDialogCV.findViewById(R.id.user_avatar_image);
			final TextView add_button = NewCustomDialogCV.findViewById(R.id.add_button);
			final TextView cancel_button = NewCustomDialogCV.findViewById(R.id.cancel_button);

			dialog_card.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)28, 0xFFFFFFFF));
			user_avatar_card.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)28, Color.TRANSPARENT));
			user_avatar_url_input.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFEEEEEE, Color.TRANSPARENT));
			_viewGraphics(add_button, 0xFF2196F3, 0xFF1976D2, 300, 0, Color.TRANSPARENT);
			_viewGraphics(cancel_button, 0xFFF5F5F5, 0xFFE0E0E0, 300, 0, Color.TRANSPARENT);
			user_avatar_url_input.addTextChangedListener(new TextWatcher() {
				@Override
				public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
					final String _charSeq = _param1.toString();
					
					if (!_charSeq.trim().equals("")) {
						if (_checkValidUrl(_charSeq.trim())) {
							Glide.with(getApplicationContext()).load(Uri.parse(_charSeq.trim())).into(user_avatar_image);
						} else {
							((EditText)user_avatar_url_input).setError("Invalid URL");
						}
					} else {
						((EditText)user_avatar_url_input).setError("Enter Profile Image URL");
					}
				}
				
				@Override
				public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
					
				}
				
				@Override
				public void afterTextChanged(Editable _param1) {
					
				}
			});

			add_button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					if (!user_avatar_url_input.getText().toString().trim().equals("")) {
						if (_checkValidUrl(user_avatar_url_input.getText().toString().trim())) {
							String ProfileHistoryKey = dbService.getReference("skyline/cover-image-history").push().getKey();
							mAddProfilePhotoMap = new HashMap<>();
							mAddProfilePhotoMap.put("key", ProfileHistoryKey);
							mAddProfilePhotoMap.put("image_url", user_avatar_url_input.getText().toString().trim());
							mAddProfilePhotoMap.put("upload_date", String.valueOf((long)(cc.getTimeInMillis())));
							mAddProfilePhotoMap.put("type", "url");
							dbService.getReference("skyline/cover-image-history/".concat(authService.getCurrentUser().getUid().concat("/".concat(ProfileHistoryKey)))).updateChildren(mAddProfilePhotoMap);
							SketchwareUtil.showMessage(getApplicationContext(), "Cover Image Added");
							_getReference();
							NewCustomDialog.dismiss();
						}
					}
				}
			});
			cancel_button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					NewCustomDialog.dismiss();
				}
			});
			
			NewCustomDialog.setCancelable(true);
			NewCustomDialog.show();
		}
	}


	public void _deleteProfileImage(final String _key, final String _type, final String _uri) {
		{
			final AlertDialog NewCustomDialog = new AlertDialog.Builder(ProfileCoverPhotoHistoryActivity.this).create();
			LayoutInflater NewCustomDialogLI = getLayoutInflater();
			View NewCustomDialogCV = (View) NewCustomDialogLI.inflate(R.layout.dialog_synapse_bg_view, null);
			NewCustomDialog.setView(NewCustomDialogCV);
			NewCustomDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
			
			final TextView dialog_title = (TextView) NewCustomDialogCV.findViewById(R.id.dialog_title);
			final TextView dialog_message = (TextView) NewCustomDialogCV.findViewById(R.id.dialog_message);
			final TextView dialog_no_button = (TextView) NewCustomDialogCV.findViewById(R.id.dialog_no_button);
			final TextView dialog_yes_button = (TextView) NewCustomDialogCV.findViewById(R.id.dialog_yes_button);
			dialog_yes_button.setTextColor(0xFFF44336);
			_viewGraphics(dialog_yes_button, 0xFFFFFFFF, 0xFFFFCDD2, 28, 0, Color.TRANSPARENT);
			dialog_no_button.setTextColor(0xFF2196F3);
			_viewGraphics(dialog_no_button, 0xFFFFFFFF, 0xFFBBDEFB, 28, 0, Color.TRANSPARENT);
			dialog_title.setText(getResources().getString(R.string.info));
			dialog_message.setText("Are you sure you want to delete this profile photo completely? ");
			dialog_no_button.setText(getResources().getString(R.string.no));
			dialog_yes_button.setText(getResources().getString(R.string.yes));
			dialog_no_button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					NewCustomDialog.dismiss();
				}
			});
			dialog_yes_button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					// Re-introduce file deletion logic
					fileUploaderService.deleteFile(_uri, new FileUploaderService.DeleteListener() {
						@Override
						public void onSuccess() {
							// Deletion from storage was successful, now update the database
							if (_uri.equals(CurrentAvatarUri)) {
								mSendMap = new HashMap<>();
								mSendMap.put("profile_cover_image", "null");
								mSendMap.put("cover_image_history_type", "local");
								dbService.getReference("skyline/users/".concat(authService.getCurrentUser().getUid())).updateChildren(mSendMap);
								CurrentAvatarUri = "null";
								mSendMap.clear();
							}
							dbService.getReference("skyline/cover-image-history/".concat(authService.getCurrentUser().getUid().concat("/".concat(_key)))).removeValue();
							_getReference();
							NewCustomDialog.dismiss();
						}
						@Override
						public void onFailure(@NonNull String error) {
							// Even if storage deletion fails, proceed with DB removal
							// but log the error.
							if (_uri.equals(CurrentAvatarUri)) {
								mSendMap = new HashMap<>();
								mSendMap.put("profile_cover_image", "null");
								mSendMap.put("cover_image_history_type", "local");
								dbService.getReference("skyline/users/".concat(authService.getCurrentUser().getUid())).updateChildren(mSendMap);
								CurrentAvatarUri = "null";
								mSendMap.clear();
							}
							dbService.getReference("skyline/cover-image-history/".concat(authService.getCurrentUser().getUid().concat("/".concat(_key)))).removeValue();
							_getReference();
							NewCustomDialog.dismiss();
							Toast.makeText(ProfileCoverPhotoHistoryActivity.this, "Storage delete failed: " + error, Toast.LENGTH_SHORT).show();
						}
					});
				}
			});
			NewCustomDialog.setCancelable(true);
			NewCustomDialog.show();
		}
	}

	public class ProfilePhotosHistoryListAdapter extends RecyclerView.Adapter<ProfilePhotosHistoryListAdapter.ViewHolder> {

		ArrayList<HashMap<String, Object>> _data;

		public ProfilePhotosHistoryListAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.profile_cover_image_history_list, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}

		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;

			final LinearLayout body = _view.findViewById(R.id.body);
			final androidx.cardview.widget.CardView card = _view.findViewById(R.id.card);
			final RelativeLayout relative = _view.findViewById(R.id.relative);
			final ImageView profile = _view.findViewById(R.id.profile);
			final LinearLayout checked = _view.findViewById(R.id.checked);
			final ImageView checked_ic = _view.findViewById(R.id.checked_ic);

			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_view.setLayoutParams(_lp);
			_viewGraphics(body, 0xFFFFFFFF, 0xFFEEEEEE, 28, 0, Color.TRANSPARENT);
			card.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)28, Color.TRANSPARENT));
			checked.setBackgroundColor(0x50000000);
			_ImageColor(checked_ic, 0xFFFFFFFF);
			Glide.with(getApplicationContext()).load(Uri.parse(_data.get((int)_position).get("image_url").toString())).into(profile);
			if (_data.get((int)_position).get("image_url").toString().equals(CurrentAvatarUri)) {
				checked.setVisibility(View.VISIBLE);
			} else {
				checked.setVisibility(View.GONE);
			}
			body.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					if (_data.get((int)_position).get("image_url").toString().equals(CurrentAvatarUri)) {
						mSendMap = new HashMap<>();
						mSendMap.put("profile_cover_image", "null");
						mSendMap.put("cover_image_history_type", "local");
						dbService.getReference("skyline/users/".concat(authService.getCurrentUser().getUid())).updateChildren(mSendMap);
						CurrentAvatarUri = "null";
						mSendMap.clear();
						notifyDataSetChanged();
					} else {
						mSendMap = new HashMap<>();
						mSendMap.put("profile_cover_image", _data.get((int)_position).get("image_url").toString());
						mSendMap.put("cover_image_history_type", _data.get((int)_position).get("type").toString());
						dbService.getReference("skyline/users/".concat(authService.getCurrentUser().getUid())).updateChildren(mSendMap);
						CurrentAvatarUri = _data.get((int)_position).get("image_url").toString();
						mSendMap.clear();
						notifyDataSetChanged();
					}
				}
			});
			body.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View _view) {
					_deleteProfileImage(_data.get((int)_position).get("key").toString(), _data.get((int)_position).get("type").toString(), _data.get((int)_position).get("image_url").toString());
					return true;
				}
			});
		}

		@Override
		public int getItemCount() {
			return _data.size();
		}

		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}
}