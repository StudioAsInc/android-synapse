package com.synapse.social.studioasinc;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.synapse.social.studioasinc.backend.AuthenticationService;
import com.synapse.social.studioasinc.backend.DatabaseService;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class ProfilePhotoHistoryActivity extends AppCompatActivity {

	private DatabaseService dbService;
	private AuthenticationService authService;

	private ProgressDialog SynapseLoadingDialog;
	private FloatingActionButton _fab;
	private String CurrentAvatarUri = "";
	private HashMap<String, Object> mSendMap = new HashMap<>();
	private HashMap<String, Object> mAddProfilePhotoMap = new HashMap<>();

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
		setContentView(R.layout.activity_profile_photo_history);
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
		GridLayoutManager profileImagesHistoryListGrid = new GridLayoutManager(this, 3);
		ProfilePhotosHistoryList.setLayoutManager(profileImagesHistoryListGrid);
		ProfilePhotosHistoryList.setAdapter(new ProfilePhotosHistoryListAdapter(ProfileHistoryList));
		_getReference();
	}

	@Override
	public void onBackPressed() {
		finish();
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


	public void _stateColor(final int _statusColor, final int _navigationColor) {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(_statusColor);
		getWindow().setNavigationBarColor(_navigationColor);
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
		Query getProfileHistoryRef = dbService.getReference("skyline/profile-history").child(authService.getCurrentUser().getUid());
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


	public void _deleteProfileImage(final String _key, final String _type, final String _uri) {
		{
			final AlertDialog NewCustomDialog = new AlertDialog.Builder(ProfilePhotoHistoryActivity.this).create();
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
					if (_uri.equals(CurrentAvatarUri)) {
						mSendMap = new HashMap<>();
						mSendMap.put("avatar", "null");
						mSendMap.put("avatar_history_type", "local");
						dbService.getReference("skyline/users/".concat(authService.getCurrentUser().getUid())).updateChildren(mSendMap);
						CurrentAvatarUri = "null";
						mSendMap.clear();
					}
					dbService.getReference("skyline/profile-history/".concat(authService.getCurrentUser().getUid().concat("/".concat(_key)))).removeValue();
					_getReference();
					NewCustomDialog.dismiss();
				}
			});
			NewCustomDialog.setCancelable(true);
			NewCustomDialog.show();
		}
	}


	public void _addProfilePhotoUrlDialog() {
		MaterialAlertDialogBuilder Dialogs = new MaterialAlertDialogBuilder(ProfilePhotoHistoryActivity.this);
		Dialogs.setTitle("Add image with link");
		View EdittextDesign = LayoutInflater.from(ProfilePhotoHistoryActivity.this).inflate(R.layout.single_et, null);
		Dialogs.setView(EdittextDesign);
		final EditText edittext1 = EdittextDesign.findViewById(R.id.edittext1);
		edittext1.setFocusableInTouchMode(true);
		Dialogs.setPositiveButton("Add", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface _dialog, int _which) {
				if (!edittext1.getText().toString().trim().equals("")) {
					if (_checkValidUrl(edittext1.getText().toString().trim())) {
						String ProfileHistoryKey = dbService.getReference("skyline/profile-history").push().getKey();
						mAddProfilePhotoMap = new HashMap<>();
						mAddProfilePhotoMap.put("key", ProfileHistoryKey);
						mAddProfilePhotoMap.put("image_url", edittext1.getText().toString().trim());
						mAddProfilePhotoMap.put("upload_date", String.valueOf((long)(cc.getTimeInMillis())));
						mAddProfilePhotoMap.put("type", "url");
						dbService.getReference("skyline/profile-history/".concat(authService.getCurrentUser().getUid().concat("/".concat(ProfileHistoryKey)))).updateChildren(mAddProfilePhotoMap);
						_getReference();
					}
				}
			}
		});
		Dialogs.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface _dialog, int _which) {
				
			}
		});
		androidx.appcompat.app.AlertDialog edittextDialog = Dialogs.create();
		
		edittextDialog.setCancelable(true);
		edittextDialog.show();
	}


	public boolean _checkValidUrl(final String _url) {
		try {
			new URL(_url);
			return true;
		} catch (MalformedURLException e) {
			return false;
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
			View _v = _inflater.inflate(R.layout.dp_history_cv, null);
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
						mSendMap.put("avatar", "null");
						mSendMap.put("avatar_history_type", "local");
						dbService.getReference("skyline/users/".concat(authService.getCurrentUser().getUid())).updateChildren(mSendMap);
						CurrentAvatarUri = "null";
						mSendMap.clear();
						notifyDataSetChanged();
					} else {
						mSendMap = new HashMap<>();
						mSendMap.put("avatar", _data.get((int)_position).get("image_url").toString());
						mSendMap.put("avatar_history_type", _data.get((int)_position).get("type").toString());
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