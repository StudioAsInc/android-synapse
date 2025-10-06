package com.synapse.social.studioasinc;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.*;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.gridlayout.*;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButtonGroup;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayout.OnTabSelectedListener;
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService;
import com.synapse.social.studioasinc.backend.interfaces.IDataListener;
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError;
import com.synapse.social.studioasinc.backend.interfaces.IQuery;
import com.synapse.social.studioasinc.backend.interfaces.IUser;
import com.theartofdev.edmodo.cropper.*;
import com.yalantis.ucrop.*;
import java.io.*;
import java.io.InputStream;
import java.text.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import org.json.*;
import androidx.core.widget.NestedScrollView;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import androidx.browser.customtabs.CustomTabsIntent;

public class ProfileActivity extends AppCompatActivity {

	private Timer _timer = new Timer();

	private IAuthenticationService authService;
	private IDatabaseService dbService;

	private HashMap<String, Object> UserInfoCacheMap = new HashMap<>();
	private HashMap<String, Object> postLikeCountCache = new HashMap<>();
	private String UserAvatarUri = "";
	private HashMap<String, Object> mSendHistoryMap = new HashMap<>();
	private String handle = "";
	private String object_clicked = "";
	private String nickname = "";
	private String AndroidDevelopersBlogURL = "";

	private ArrayList<HashMap<String, Object>> UserPostsList = new ArrayList<>();

	private LinearLayout ProfilePageBody;
	private LinearLayout ProfilePageTopBar;
	private LinearLayout ProfilePageMiddleLayout;
	private ImageView ProfilePageTopBarBack;
	private LinearLayout ProfilePageTopBarSpace;
	private ImageView myqr_btn;
	private ImageView ProfilePageTopBarMenu;
	private TabLayout ProfilePageTabLayout;
	private SwipeRefreshLayout ProfilePageSwipeLayout;
	private LinearLayout ProfilePageNoInternetBody;
	private LinearLayout ProfilePageLoadingBody;
	private LinearLayout ProfilePageSwipeLayoutBody;
	private NestedScrollView ProfilePageTabUserInfo;
	private LinearLayout ProfilePageTabUserPosts;
	private LinearLayout ProfilePageTabUserInfoBody;
	private LinearLayout ProfilePageTabUserInfoCoverLayout;
	private LinearLayout prfLay;
	private LinearLayout ProfilePageTabUserInfoLayoutMarg;
	private ImageView ProfilePageTabUserInfoCoverImage;
	private LinearLayout ProfilePageTabUserInfoCardLayout;
	private LinearLayout prfLaySpc;
	private CardView ProfilePageTabUserInfoProfileCard;
	private ImageView ProfilePageTabUserInfoProfileImage;
	private LinearLayout likeUserProfileButton;
	private ImageView likeUserProfileButtonIc;
	private TextView likeUserProfileButtonLikeCount;
	private LinearLayout ProfilePageTabUserInfoNameLayout;
	private TextView ProfilePageTabUserInfoBioLayoutText;
	private LinearLayout ProfilePageTabUserInfoStateDetails;
	private LinearLayout ProfilePageTabUserInfoFollowsDetails;
	private MaterialButtonGroup ProfilePageTabUserInfoSecondaryButtons;
	private Button btnEditProfile;
	private LinearLayout join_date_layout;
	private LinearLayout user_uid_layout;
	private TextView ProfilePageTabUserInfoNickname;
	private TextView ProfilePageTabUserInfoUsername;
	private TextView ProfilePageTabUserInfoStateSpc;
	private TextView ProfilePageTabUserInfoStatus;
	private TextView ProfilePageTabUserInfoFollowersCount;
	private TextView ProfilePageTabUserInfoSpc;
	private TextView ProfilePageTabUserInfoFollowingCount;
	private Button btnFollow;
	private Button btnMessage;
	private TextView join_date_layout_title;
	private TextView join_date_layout_text;
	private TextView user_uid_layout_title;
	private TextView user_uid_layout_text;
	private RecyclerView ProfilePageTabUserPostsRecyclerView;
	private TextView ProfilePageTabUserPostsNoPostsSubtitle;
	private ImageView ProfilePageNoInternetBodyIc;
	private TextView ProfilePageNoInternetBodyTitle;
	private TextView ProfilePageNoInternetBodySubtitle;
	private TextView ProfilePageNoInternetBodyRetry;
	private ProgressBar ProfilePageLoadingBodyBar;

	private Intent intent = new Intent();
	private Vibrator vbr;
	private Calendar cc = Calendar.getInstance();

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
	private RequestNetwork req;
	private RequestNetwork.RequestListener _req_request_listener;
	private Calendar JoinDateCC = Calendar.getInstance();
	private TimerTask after;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_profile);
		initialize(_savedInstanceState);
		initializeLogic();
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (authService.getCurrentUser() != null) {
			PresenceManager.setActivity(authService.getCurrentUser().getUid(), "In Profile");
		}
	}

	@Override
	protected void onStop() {
		super.onStop();
	}

	private void initialize(Bundle _savedInstanceState) {
		ProfilePageBody = findViewById(R.id.ProfilePageBody);
		ProfilePageTopBar = findViewById(R.id.ProfilePageTopBar);
		ProfilePageMiddleLayout = findViewById(R.id.ProfilePageMiddleLayout);
		ProfilePageTopBarBack = findViewById(R.id.ProfilePageTopBarBack);
		ProfilePageTopBarSpace = findViewById(R.id.ProfilePageTopBarSpace);
		myqr_btn = findViewById(R.id.myqr_btn);
		ProfilePageTopBarMenu = findViewById(R.id.ProfilePageTopBarMenu);
		ProfilePageTabLayout = findViewById(R.id.ProfilePageTabLayout);
		ProfilePageSwipeLayout = findViewById(R.id.ProfilePageSwipeLayout);
		ProfilePageNoInternetBody = findViewById(R.id.ProfilePageNoInternetBody);
		ProfilePageLoadingBody = findViewById(R.id.ProfilePageLoadingBody);
		ProfilePageSwipeLayoutBody = findViewById(R.id.ProfilePageSwipeLayoutBody);
		ProfilePageTabUserInfo = findViewById(R.id.ProfilePageTabUserInfo);
		ProfilePageTabUserPosts = findViewById(R.id.ProfilePageTabUserPosts);
		ProfilePageTabUserInfoBody = findViewById(R.id.ProfilePageTabUserInfoBody);
		ProfilePageTabUserInfoCoverLayout = findViewById(R.id.ProfilePageTabUserInfoCoverLayout);
		prfLay = findViewById(R.id.prfLay);
		ProfilePageTabUserInfoLayoutMarg = findViewById(R.id.ProfilePageTabUserInfoLayoutMarg);
		ProfilePageTabUserInfoCoverImage = findViewById(R.id.ProfilePageTabUserInfoCoverImage);
		ProfilePageTabUserInfoCardLayout = findViewById(R.id.ProfilePageTabUserInfoCardLayout);
		prfLaySpc = findViewById(R.id.prfLaySpc);
		ProfilePageTabUserInfoProfileCard = findViewById(R.id.ProfilePageTabUserInfoProfileCard);
		ProfilePageTabUserInfoProfileImage = findViewById(R.id.ProfilePageTabUserInfoProfileImage);
		likeUserProfileButton = findViewById(R.id.likeUserProfileButton);
		likeUserProfileButtonIc = findViewById(R.id.likeUserProfileButtonIc);
		likeUserProfileButtonLikeCount = findViewById(R.id.likeUserProfileButtonLikeCount);
		ProfilePageTabUserInfoNameLayout = findViewById(R.id.ProfilePageTabUserInfoNameLayout);
		ProfilePageTabUserInfoBioLayoutText = findViewById(R.id.ProfilePageTabUserInfoBioLayoutText);
		ProfilePageTabUserInfoStateDetails = findViewById(R.id.ProfilePageTabUserInfoStateDetails);
		ProfilePageTabUserInfoFollowsDetails = findViewById(R.id.ProfilePageTabUserInfoFollowsDetails);
		ProfilePageTabUserInfoSecondaryButtons = findViewById(R.id.ProfilePageTabUserInfoSecondaryButtons);
		btnEditProfile = findViewById(R.id.btnEditProfile);
		join_date_layout = findViewById(R.id.join_date_layout);
		user_uid_layout = findViewById(R.id.user_uid_layout);
		ProfilePageTabUserInfoNickname = findViewById(R.id.ProfilePageTabUserInfoNickname);
		ProfilePageTabUserInfoUsername = findViewById(R.id.ProfilePageTabUserInfoUsername);
		ProfilePageTabUserInfoStateSpc = findViewById(R.id.ProfilePageTabUserInfoStateSpc);
		ProfilePageTabUserInfoStatus = findViewById(R.id.ProfilePageTabUserInfoStatus);
		ProfilePageTabUserInfoFollowersCount = findViewById(R.id.ProfilePageTabUserInfoFollowersCount);
		ProfilePageTabUserInfoSpc = findViewById(R.id.ProfilePageTabUserInfoSpc);
		ProfilePageTabUserInfoFollowingCount = findViewById(R.id.ProfilePageTabUserInfoFollowingCount);
		btnFollow = findViewById(R.id.btnFollow);
		btnMessage = findViewById(R.id.btnMessage);
		join_date_layout_title = findViewById(R.id.join_date_layout_title);
		join_date_layout_text = findViewById(R.id.join_date_layout_text);
		user_uid_layout_title = findViewById(R.id.user_uid_layout_title);
		user_uid_layout_text = findViewById(R.id.user_uid_layout_text);
		ProfilePageTabUserPostsRecyclerView = findViewById(R.id.ProfilePageTabUserPostsRecyclerView);
		ProfilePageTabUserPostsNoPostsSubtitle = findViewById(R.id.ProfilePageTabUserPostsNoPostsSubtitle);
		ProfilePageNoInternetBodyIc = findViewById(R.id.ProfilePageNoInternetBodyIc);
		ProfilePageNoInternetBodyTitle = findViewById(R.id.ProfilePageNoInternetBodyTitle);
		ProfilePageNoInternetBodySubtitle = findViewById(R.id.ProfilePageNoInternetBodySubtitle);
		ProfilePageNoInternetBodyRetry = findViewById(R.id.ProfilePageNoInternetBodyRetry);
		ProfilePageLoadingBodyBar = findViewById(R.id.ProfilePageLoadingBodyBar);
		authService = ((SynapseApp) getApplication()).getAuthenticationService();
		dbService = ((SynapseApp) getApplication()).getDatabaseService();
		vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		req = new RequestNetwork(this);

		ProfilePageTopBarBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				onBackPressed();
			}
		});

		ProfilePageTopBarMenu.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), ChatsettingsActivity.class);
				startActivity(intent);
			}
		});

		ProfilePageTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				final int _position = tab.getPosition();
				if (_position == 0) {
					ProfilePageTabUserInfo.setVisibility(View.VISIBLE);
					ProfilePageTabUserPosts.setVisibility(View.GONE);
				}
				if (_position == 1) {
					ProfilePageTabUserInfo.setVisibility(View.GONE);
					ProfilePageTabUserPosts.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {
				final int _position = tab.getPosition();

			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {
				final int _position = tab.getPosition();

			}
		});

		ProfilePageSwipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				_loadRequest();
			}
		});

		ProfilePageTabUserInfoProfileImage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (getIntent().hasExtra("uid")) {
					_ProfileImagePreview(getIntent().getStringExtra("uid"));
				}
			}
		});

		likeUserProfileButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (authService.getCurrentUser() == null) return;
				dbService.getReference("profile-likes").child(getIntent().getStringExtra("uid")).child(authService.getCurrentUser().getUid()).getData(new IDataListener() {
					@Override
					public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
						if(dataSnapshot.exists()) {
							dbService.getReference("profile-likes").child(getIntent().getStringExtra("uid")).child(authService.getCurrentUser().getUid()).setValue(null, (result, error) -> {});
							UserInfoCacheMap.put("profile_like_count".concat(getIntent().getStringExtra("uid")), String.valueOf((long)(Double.parseDouble(UserInfoCacheMap.get("profile_like_count".concat(getIntent().getStringExtra("uid"))).toString()) - 1)));
							likeUserProfileButtonLikeCount.setText(_getStyledNumber(Double.parseDouble(UserInfoCacheMap.get("profile_like_count".concat(getIntent().getStringExtra("uid"))).toString())));
							likeUserProfileButtonIc.setImageResource(R.drawable.post_icons_1_1);
							_viewGraphics(likeUserProfileButton, 0xFFFFFFFF, 0xFFEEEEEE, 300, 0, 0xFF9E9E9E);
							_ImageColor(likeUserProfileButtonIc, 0xFF000000);
							likeUserProfileButtonLikeCount.setTextColor(0xFF616161);
						} else {
							dbService.getReference("profile-likes").child(getIntent().getStringExtra("uid")).child(authService.getCurrentUser().getUid()).setValue(authService.getCurrentUser().getUid(), (result, error) -> {});
							UserInfoCacheMap.put("profile_like_count".concat(getIntent().getStringExtra("uid")), String.valueOf((long)(Double.parseDouble(UserInfoCacheMap.get("profile_like_count".concat(getIntent().getStringExtra("uid"))).toString()) + 1)));
							likeUserProfileButtonLikeCount.setText(_getStyledNumber(Double.parseDouble(UserInfoCacheMap.get("profile_like_count".concat(getIntent().getStringExtra("uid"))).toString())));
							likeUserProfileButtonIc.setImageResource(R.drawable.post_icons_1_2);
							_viewGraphics(likeUserProfileButton, 0xFFF50057, 0xFFC51162, 300, 0, 0xFF9E9E9E);
							_ImageColor(likeUserProfileButtonIc, 0xFFFFFFFF);
							likeUserProfileButtonLikeCount.setTextColor(0xFFFFFFFF);
						}
					}

					@Override
					public void onCancelled(@NonNull IDatabaseError databaseError) {

					}
				});
				vbr.vibrate((long)(28));
			}
		});

		ProfilePageTabUserInfoFollowsDetails.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), UserFollowsListActivity.class);
				intent.putExtra("uid", getIntent().getStringExtra("uid"));
				startActivity(intent);
			}
		});

		btnEditProfile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), ProfileEditActivity.class);
				startActivity(intent);
			}
		});

		btnFollow.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (authService.getCurrentUser() == null) return;
				dbService.getReference("followers").child(getIntent().getStringExtra("uid")).child(authService.getCurrentUser().getUid()).getData(new IDataListener() {
					@Override
					public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
						if(dataSnapshot.exists()) {
							dbService.getReference("followers").child(getIntent().getStringExtra("uid")).child(authService.getCurrentUser().getUid()).setValue(null, (result, error) -> {});
							dbService.getReference("following").child(authService.getCurrentUser().getUid()).child(getIntent().getStringExtra("uid")).setValue(null, (result, error) -> {});
							UserInfoCacheMap.put("followers_count".concat(getIntent().getStringExtra("uid")), String.valueOf((long)(Double.parseDouble(UserInfoCacheMap.get("followers_count".concat(getIntent().getStringExtra("uid"))).toString()) - 1)));
							ProfilePageTabUserInfoFollowersCount.setText(_getStyledNumber(Double.parseDouble(UserInfoCacheMap.get("followers_count".concat(getIntent().getStringExtra("uid"))).toString())).concat(" ".concat(getResources().getString(R.string.followers))));
							btnFollow.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
							btnFollow.setText(getResources().getString(R.string.follow));
							btnFollow.setTextColor(0xFFFFFFFF);
						} else {
							dbService.getReference("followers").child(getIntent().getStringExtra("uid")).child(authService.getCurrentUser().getUid()).setValue(authService.getCurrentUser().getUid(), (result, error) -> {});
							dbService.getReference("following").child(authService.getCurrentUser().getUid()).child(getIntent().getStringExtra("uid")).setValue(getIntent().getStringExtra("uid"), (result, error) -> {});
							UserInfoCacheMap.put("followers_count".concat(getIntent().getStringExtra("uid")), String.valueOf((long)(Double.parseDouble(UserInfoCacheMap.get("followers_count".concat(getIntent().getStringExtra("uid"))).toString()) + 1)));
							ProfilePageTabUserInfoFollowersCount.setText(_getStyledNumber(Double.parseDouble(UserInfoCacheMap.get("followers_count".concat(getIntent().getStringExtra("uid"))).toString())).concat(" ".concat(getResources().getString(R.string.followers))));
							btnFollow.setBackgroundColor(getResources().getColor(R.color.bars_colors));
							btnFollow.setText(getResources().getString(R.string.unfollow));
							btnFollow.setTextColor(0xFF000000);
						}
					}

					@Override
					public void onCancelled(@NonNull IDatabaseError databaseError) {

					}
				});
			}
		});

		btnMessage.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), ChatActivity.class);
				intent.putExtra("uid", getIntent().getStringExtra("uid"));
				intent.putExtra("origin", "ProfileActivity");
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
			}
		});

		user_uid_layout_text.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View _view) {
				((ClipboardManager) getSystemService(getApplicationContext().CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("clipboard", user_uid_layout_text.getText().toString()));
				return true;
			}
		});

		ProfilePageNoInternetBodyRetry.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_loadRequest();
			}
		});

		_req_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _tag = _param1;
				final String _response = _param2;
				final HashMap<String, Object> _responseHeaders = _param3;
				_getUserReference();
			}

			@Override
			public void onErrorResponse(String _param1, String _param2) {
				final String _tag = _param1;
				final String _message = _param2;
				ProfilePageSwipeLayout.setVisibility(View.GONE);
				ProfilePageNoInternetBody.setVisibility(View.VISIBLE);
				ProfilePageLoadingBody.setVisibility(View.GONE);
			}
		};
	}

	private void initializeLogic() {
		ProfilePageTabLayout.addTab(ProfilePageTabLayout.newTab().setText(getResources().getString(R.string.profile_tab)));
		ProfilePageTabLayout.addTab(ProfilePageTabLayout.newTab().setText(getResources().getString(R.string.posts_tab)));
		ProfilePageTabLayout.setTabTextColors(0xFF9E9E9E, 0xFF445E91);
		ProfilePageTabLayout.setTabRippleColor(new android.content.res.ColorStateList(new int[][]{new int[]{android.R.attr.state_pressed}},

		new int[] {0xFFEEEEEE}));
		ProfilePageTabLayout.setSelectedTabIndicatorColor(0xFF445E91);
		ProfilePageTabUserPostsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		_loadRequest();
		ProfilePageTabUserInfoNickname.setTypeface(Typeface.DEFAULT, 1);
		ProfilePageNoInternetBodyRetry.setBackgroundResource(R.drawable.shape_rounded_20dp);
		String intentUid = getIntent().getStringExtra("uid");
		IUser currentUser = authService.getCurrentUser();
		if (intentUid != null && currentUser != null && intentUid.equals(currentUser.getUid())) {
			ProfilePageTabUserInfoSecondaryButtons.setVisibility(View.GONE);
			btnEditProfile.setVisibility(View.VISIBLE);
		} else {
			ProfilePageTabUserInfoSecondaryButtons.setVisibility(View.VISIBLE);
			btnEditProfile.setVisibility(View.GONE);
		}
		_viewGraphics(likeUserProfileButton, 0xFFFFFFFF, 0xFFEEEEEE, 300, 0, 0xFF9E9E9E);
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


	public void _getUserReference() {
		dbService.getUserById(getIntent().getStringExtra("uid"), new IDataListener() {
			@Override
			public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
				if(dataSnapshot.exists()) {
					ProfilePageSwipeLayout.setVisibility(View.VISIBLE);
					ProfilePageNoInternetBody.setVisibility(View.GONE);
					ProfilePageLoadingBody.setVisibility(View.GONE);
					user_uid_layout_text.setText(dataSnapshot.getChild("uid").getValue(String.class));
					JoinDateCC.setTimeInMillis((long)(Double.parseDouble(dataSnapshot.getChild("join_date").getValue(String.class))));
					if (dataSnapshot.getChild("banned").getValue(String.class).equals("true")) {
						UserAvatarUri = "null";
						ProfilePageTabUserInfoProfileImage.setImageResource(R.drawable.banned_avatar);
						ProfilePageTabUserInfoCoverImage.setImageResource(R.drawable.banned_cover_photo);
					} else {
						_getUserPostsReference();
						_getUserCountReference();
						UserAvatarUri = dataSnapshot.getChild("avatar").getValue(String.class);
						if (dataSnapshot.getChild("profile_cover_image").getValue(String.class).equals("null")) {
							ProfilePageTabUserInfoCoverImage.setImageResource(R.drawable.user_null_cover_photo);
						} else {
							Glide.with(getApplicationContext()).load(Uri.parse(dataSnapshot.getChild("profile_cover_image").getValue(String.class))).into(ProfilePageTabUserInfoCoverImage);
						}
						if (dataSnapshot.getChild("avatar").getValue(String.class).equals("null")) {
							ProfilePageTabUserInfoProfileImage.setImageResource(R.drawable.avatar);
						} else {
							Glide.with(getApplicationContext()).load(Uri.parse(dataSnapshot.getChild("avatar").getValue(String.class))).into(ProfilePageTabUserInfoProfileImage);
						}
					}
					// Check user status
					if (dataSnapshot.getChild("status").getValue(String.class).equals("online")) {
						ProfilePageTabUserInfoStatus.setText(getResources().getString(R.string.online));
						ProfilePageTabUserInfoStatus.setTextColor(0xFF2196F3);
					} else {
						if (dataSnapshot.child("status").getValue(String.class).equals("offline")) {
							ProfilePageTabUserInfoStatus.setText(getResources().getString(R.string.offline));
						} else {
							_setUserLastSeen(Double.parseDouble(dataSnapshot.child("status").getValue(String.class)), ProfilePageTabUserInfoStatus);
						}
						ProfilePageTabUserInfoStatus.setTextColor(0xFF757575);
					}
					ProfilePageTabUserInfoUsername.setText("@" + dataSnapshot.getChild("username").getValue(String.class));
					if (dataSnapshot.child("nickname").getValue(String.class).equals("null")) {
						ProfilePageTabUserInfoNickname.setText("@" + dataSnapshot.child("username").getValue(String.class));
					} else {
						ProfilePageTabUserInfoNickname.setText(dataSnapshot.child("nickname").getValue(String.class));
						nickname = dataSnapshot.child("nickname").getValue(String.class);
					}
					if (dataSnapshot.child("biography").getValue(String.class).equals("null")) {

					} else {
						ProfilePageTabUserInfoBioLayoutText.setText(dataSnapshot.child("biography").getValue(String.class));
					}
					/*
if (dataSnapshot.child("gender").getValue(String.class).equals("hidden")) {

} else {
if (dataSnapshot.child("gender").getValue(String.class).equals("male")) {

} else {
if (dataSnapshot.child("gender").getValue(String.class).equals("female")) {

}
}
}
if (dataSnapshot.child("user_region").getValue(String.class) != null) {

} else {

}
if (dataSnapshot.child("account_type").getValue(String.class).equals("admin")) {

} else {
if (dataSnapshot.child("account_type").getValue(String.class).equals("moderator")) {

} else {
if (dataSnapshot.child("account_type").getValue(String.class).equals("support")) {

} else {

}
}
}
if (dataSnapshot.child("account_premium").getValue(String.class).equals("true")) {

} else {

}
if (dataSnapshot.child("verify").getValue(String.class).equals("true")) {

} else {

}
join_date_layout_text.setText(new SimpleDateFormat("dd MMMM yyyy").format(JoinDateCC.getTime()));
if ( || ( || )) {

} else {

}
*/
				} else {
				}
			}

			@Override
			public void onCancelled(@NonNull IDatabaseError databaseError) {

			}
		});
		ProfilePageSwipeLayout.setRefreshing(false);
	}


	public void _getUserPostsReference() {
		IQuery getUserPostsRef = dbService.getReference("posts").orderBy("uid", IQuery.Order.ASCENDING).equalTo(getIntent().getStringExtra("uid"));
		getUserPostsRef.getData(new IDataListener() {
			@Override
			public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
				if(dataSnapshot.exists()) {
					ProfilePageTabUserPostsRecyclerView.setVisibility(View.VISIBLE);
					ProfilePageTabUserPostsNoPostsSubtitle.setVisibility(View.GONE);
					UserPostsList.clear();
					try {
						for (IDataSnapshot _data : dataSnapshot.getChildren()) {
							HashMap<String, Object> _map = (HashMap<String, Object>)_data.getValue();
							UserPostsList.add(_map);
						}
					} catch (Exception _e) {
						_e.printStackTrace();
					}
					SketchwareUtil.sortListMap(UserPostsList, "publish_date", false, false);
					ProfilePageTabUserPostsRecyclerView.setAdapter(new ProfilePageTabUserPostsRecyclerViewAdapter(UserPostsList));
				} else {
					ProfilePageTabUserPostsRecyclerView.setVisibility(View.GONE);
					ProfilePageTabUserPostsNoPostsSubtitle.setVisibility(View.VISIBLE);
				}
			}

			@Override
			public void onCancelled(@NonNull IDatabaseError databaseError) {

			}
		});
	}


	public void _setCount(final TextView _txt, final double _number) {
		if (_number < 10000) {
			_txt.setText(String.valueOf((long) _number));
		} else {
			DecimalFormat decimalFormat = new DecimalFormat("0.0");
			String numberFormat;
			double formattedNumber;
			if (_number < 1000000) {
				numberFormat = "K";
				formattedNumber = _number / 1000;
			} else if (_number < 1000000000) {
				numberFormat = "M";
				formattedNumber = _number / 1000000;
			} else if (_number < 1000000000000L) {
				numberFormat = "B";
				formattedNumber = _number / 1000000000;
			} else {
				numberFormat = "T";
				formattedNumber = _number / 1000000000000L;
			}
			_txt.setText(decimalFormat.format(formattedNumber) + numberFormat);
		}

	}


	public void _setTime(final double _currentTime, final TextView _txt) {
		Calendar c1 = Calendar.getInstance();
		Calendar c2 = Calendar.getInstance();
		double time_diff = c1.getTimeInMillis() - _currentTime;
		if (time_diff < 60000) {
			if ((time_diff / 1000) < 2) {
				_txt.setText("1" + " " + getResources().getString(R.string.seconds_ago));
			} else {
				_txt.setText(String.valueOf((long)(time_diff / 1000)).concat(" " + getResources().getString(R.string.seconds_ago)));
			}
		} else {
			if (time_diff < (60 * 60000)) {
				if ((time_diff / 60000) < 2) {
					_txt.setText("1" + " " + getResources().getString(R.string.minutes_ago));
				} else {
					_txt.setText(String.valueOf((long)(time_diff / 60000)).concat(" " + getResources().getString(R.string.minutes_ago)));
				}
			} else {
				if (time_diff < (24 * (60 * 60000))) {
					if ((time_diff / (60 * 60000)) < 2) {
						_txt.setText(String.valueOf((long)(time_diff / (60 * 60000))).concat(" " + getResources().getString(R.string.hours_ago)));
					} else {
						_txt.setText(String.valueOf((long)(time_diff / (60 * 60000))).concat(" " + getResources().getString(R.string.hours_ago)));
					}
				} else {
					if (time_diff < (7 * (24 * (60 * 60000)))) {
						if ((time_diff / (24 * (60 * 60000))) < 2) {
							_txt.setText(String.valueOf((long)(time_diff / (24 * (60 * 60000)))).concat(" " + getResources().getString(R.string.days_ago)));
						} else {
							_txt.setText(String.valueOf((long)(time_diff / (24 * (60 * 60000)))).concat(" " + getResources().getString(R.string.days_ago)));
						}
					} else {
						c2.setTimeInMillis((long)(_currentTime));
						_txt.setText(new SimpleDateFormat("dd-MM-yyyy").format(c2.getTime()));
					}
				}
			}
		}
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
		} else if (p instanceof RelativeLayout.LayoutParams) {
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)p;
			lp.setMargins(left, top, right, bottom);
			_view.setLayoutParams(lp);
		} else if (p instanceof TableRow.LayoutParams) {
			TableRow.LayoutParams lp = (TableRow.LayoutParams)p;
			lp.setMargins(left, top, right, bottom);
			_view.setLayoutParams(lp);
		}

	}


	public void _getUserCountReference() {
		if (authService.getCurrentUser() == null) return;
		dbService.getReference("followers").child(getIntent().getStringExtra("uid")).getData(new IDataListener() {
			@Override
			public void onDataChange(IDataSnapshot dataSnapshot) {
				long count = dataSnapshot.getChildrenCount();
				ProfilePageTabUserInfoFollowersCount.setText(_getStyledNumber(count).concat(" ".concat(getResources().getString(R.string.followers))));
				UserInfoCacheMap.put("followers_count".concat(getIntent().getStringExtra("uid")), String.valueOf((long)(count)));
			}

			@Override
			public void onCancelled(IDatabaseError databaseError) {

			}
		});
		dbService.getReference("following").child(getIntent().getStringExtra("uid")).getData(new IDataListener() {
			@Override
			public void onDataChange(IDataSnapshot dataSnapshot) {
				long count = dataSnapshot.getChildrenCount();
				ProfilePageTabUserInfoFollowingCount.setText(_getStyledNumber(count).concat(" ".concat(getResources().getString(R.string.following))));
				UserInfoCacheMap.put("following_count".concat(getIntent().getStringExtra("uid")), String.valueOf((long)(count)));
			}

			@Override
			public void onCancelled(IDatabaseError databaseError) {

			}
		});
		dbService.getReference("followers").child(getIntent().getStringExtra("uid")).child(authService.getCurrentUser().getUid()).getData(new IDataListener() {
			@Override
			public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
				if(dataSnapshot.exists()) {
					btnFollow.setText(getResources().getString(R.string.unfollow));
					btnFollow.setBackgroundColor(getResources().getColor(R.color.bars_colors));
					btnFollow.setTextColor(0xFF000000);
				} else {
					btnFollow.setText(getResources().getString(R.string.follow));
					btnFollow.setBackgroundColor(getResources().getColor(R.color.colorPrimary));
					btnFollow.setTextColor(0xFFFFFFFF);
				}
			}

			@Override
			public void onCancelled(@NonNull IDatabaseError databaseError) {

			}
		});
		dbService.getReference("profile-likes").child(getIntent().getStringExtra("uid")).child(authService.getCurrentUser().getUid()).getData(new IDataListener() {
			@Override
			public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
				if(dataSnapshot.exists()) {
					likeUserProfileButtonIc.setImageResource(R.drawable.post_icons_1_2);
					_viewGraphics(likeUserProfileButton, 0xFFF50057, 0xFFC51162, 300, 0, 0xFF9E9E9E);
					_ImageColor(likeUserProfileButtonIc, 0xFFFFFFFF);
					likeUserProfileButtonLikeCount.setTextColor(0xFFFFFFFF);
				} else {
					likeUserProfileButtonIc.setImageResource(R.drawable.post_icons_1_1);
					_viewGraphics(likeUserProfileButton, 0xFFFFFFFF, 0xFFEEEEEE, 300, 0, 0xFF9E9E9E);
					_ImageColor(likeUserProfileButtonIc, 0xFF000000);
					likeUserProfileButtonLikeCount.setTextColor(0xFF616161);
				}
			}

			@Override
			public void onCancelled(@NonNull IDatabaseError databaseError) {

			}
		});
		dbService.getReference("profile-likes").child(getIntent().getStringExtra("uid")).getData(new IDataListener() {
			@Override
			public void onDataChange(IDataSnapshot dataSnapshot) {
				long count = dataSnapshot.getChildrenCount();
				likeUserProfileButtonLikeCount.setText(_getStyledNumber(count));
				UserInfoCacheMap.put("profile_like_count".concat(getIntent().getStringExtra("uid")), String.valueOf((long)(count)));
			}

			@Override
			public void onCancelled(IDatabaseError databaseError) {

			}
		});
	}


	public String _getStyledNumber(final double _number) {
		if (_number < 10000) {
			return String.valueOf((long) _number);
		} else {
			DecimalFormat decimalFormat = new DecimalFormat("0.0");
			String numberFormat;
			double formattedNumber;
			if (_number < 1000000) {
				numberFormat = "K";
				formattedNumber = _number / 1000;
			} else if (_number < 1000000000) {
				numberFormat = "M";
				formattedNumber = _number / 1000000;
			} else if (_number < 1000000000000L) {
				numberFormat = "B";
				formattedNumber = _number / 1000000000;
			} else {
				numberFormat = "T";
				formattedNumber = _number / 1000000000000L;
			}
			return decimalFormat.format(formattedNumber) + numberFormat;
		}

	}


	public void _loadRequest() {
		ProfilePageSwipeLayout.setVisibility(View.GONE);
		ProfilePageNoInternetBody.setVisibility(View.GONE);
		ProfilePageLoadingBody.setVisibility(View.VISIBLE);
		req.startRequestNetwork(RequestNetworkController.POST, "https://google.com", "google", _req_request_listener);
	}


	public double _convertXpToLevel(final double _xp_point) {
		double convertedLevel = 0;
		if ((_xp_point == 0) || (_xp_point < 1000)) {
			convertedLevel = 1;
		} else if ((_xp_point == 1000) || (_xp_point < 2000)) {
			convertedLevel = 2;
		} else if ((_xp_point == 2000) || (_xp_point < 3000)) {
			convertedLevel = 3;
		} else if ((_xp_point == 3000) || (_xp_point < 4000)) {
			convertedLevel = 4;
		} else if ((_xp_point == 4000) || (_xp_point < 5000)) {
			convertedLevel = 5;
		} else if ((_xp_point == 5000) || (_xp_point < 6000)) {
			convertedLevel = 6;
		} else if ((_xp_point == 6000) || (_xp_point < 7000)) {
			convertedLevel = 7;
		} else if ((_xp_point == 7000) || (_xp_point < 8000)) {
			convertedLevel = 8;
		} else if ((_xp_point == 8000) || (_xp_point < 9000)) {
			convertedLevel = 9;
		} else if ((_xp_point == 9000) || (_xp_point < 10000)) {
			convertedLevel = 10;
		}
		return convertedLevel;
	}


	public void _ScrollingText(final TextView _view) {
		_view.setSingleLine(true);
		_view.setEllipsize(TextUtils.TruncateAt.MARQUEE);
		_view.setSelected(true);
	}


	public void _ProfileImagePreview(final String _uid) {
		{
			final AlertDialog mProfileImageViewDialog = new AlertDialog.Builder(ProfileActivity.this).create();
			View mProfileImageViewDialogView = (View)getLayoutInflater().inflate(R.layout.dp_preview, null);
			mProfileImageViewDialog.setView(mProfileImageViewDialogView);
			mProfileImageViewDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

			final LinearLayout body = mProfileImageViewDialogView.findViewById(R.id.body);
			final CardView avatarCard = mProfileImageViewDialogView.findViewById(R.id.avatarCard);
			final ImageView avatar = mProfileImageViewDialogView.findViewById(R.id.avatar);
			final TextView save_to_history = mProfileImageViewDialogView.findViewById(R.id.save_to_history);

			body.setVisibility(View.GONE);
			_viewGraphics(save_to_history, 0xFFFFFFFF, 0xFFEEEEEE, 300, 0, Color.TRANSPARENT);
			avatarCard.setBackgroundResource(R.drawable.shape_circular);
			dbService.getUserById(_uid, new IDataListener() {
				@Override
				public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
					if(dataSnapshot.exists()) {
						if (dataSnapshot.getChild("banned").getValue(String.class).equals("true")) {
							avatar.setImageResource(R.drawable.avatar);
							save_to_history.setVisibility(View.GONE);
						} else {
							if (dataSnapshot.getChild("avatar").getValue(String.class).equals("null")) {
								avatar.setImageResource(R.drawable.avatar);
								save_to_history.setVisibility(View.GONE);
							} else {
								Glide.with(getApplicationContext()).load(Uri.parse(dataSnapshot.getChild("avatar").getValue(String.class))).into(avatar);
								if (authService.getCurrentUser() != null && !dataSnapshot.getChild("uid").getValue(String.class).equals(authService.getCurrentUser().getUid())) {
									if (dataSnapshot.getChild("avatar_history_type").getValue(String.class).equals("url")) {
										save_to_history.setOnClickListener(new View.OnClickListener() {
											@Override
											public void onClick(View _view) {
												String profileHistoryKey = dbService.getReference("profile-history").child(authService.getCurrentUser().getUid()).push().getKey();
												mSendHistoryMap = new HashMap<>();
												mSendHistoryMap.put("key", profileHistoryKey);
												mSendHistoryMap.put("image_url", dataSnapshot.getChild("avatar").getValue(String.class));
												mSendHistoryMap.put("upload_date", String.valueOf((long)(cc.getTimeInMillis())));
												mSendHistoryMap.put("type", "url");
												dbService.getReference("profile-history").child(authService.getCurrentUser().getUid()).child(profileHistoryKey).setValue(mSendHistoryMap, (result, error) -> {});
												SketchwareUtil.showMessage(getApplicationContext(), "Saved to History");
												mProfileImageViewDialog.dismiss();
											}
										});
										save_to_history.setVisibility(View.VISIBLE);
									} else {
										save_to_history.setVisibility(View.GONE);
									}
								} else {
									save_to_history.setVisibility(View.GONE);
								}
							}
						}
						body.setVisibility(View.VISIBLE);
					} else {
						mProfileImageViewDialog.dismiss();
					}
				}

				@Override
				public void onCancelled(@NonNull IDatabaseError databaseError) {

				}
			});

			mProfileImageViewDialog.setCancelable(true);
			mProfileImageViewDialog.show();
		}
	}


	public void _setUserLastSeen(final double _currentTime, final TextView _txt) {
		Calendar c1 = Calendar.getInstance();
		Calendar c2 = Calendar.getInstance();
		c2.setTimeInMillis((long)_currentTime);

		long time_diff = c1.getTimeInMillis() - c2.getTimeInMillis();

		long seconds = time_diff / 1000;
		long minutes = seconds / 60;
		long hours = minutes / 60;
		long days = hours / 24;
		long weeks = days / 7;
		long months = days / 30;
		long years = days / 365;

		if (seconds < 60) {
			if (seconds < 2) {
				_txt.setText("1 " + getResources().getString(R.string.status_text_seconds));
			} else {
				_txt.setText(String.valueOf(seconds) + " " + getResources().getString(R.string.status_text_seconds));
			}
		} else if (minutes < 60) {
			if (minutes < 2) {
				_txt.setText("1 " + getResources().getString(R.string.status_text_minutes));
			} else {
				_txt.setText(String.valueOf(minutes) + " " + getResources().getString(R.string.status_text_minutes));
			}
		} else if (hours < 24) {
			if (hours < 2) {
				_txt.setText("1 " + getResources().getString(R.string.status_text_hours));
			} else {
				_txt.setText(String.valueOf(hours) + " " + getResources().getString(R.string.status_text_hours));
			}
		} else if (days < 7) {
			if (days < 2) {
				_txt.setText("1 " + getResources().getString(R.string.status_text_days));
			} else {
				_txt.setText(String.valueOf(days) + " " + getResources().getString(R.string.status_text_days));
			}
		} else if (weeks < 4) {
			if (weeks < 2) {
				_txt.setText("1 " + getResources().getString(R.string.status_text_week));
			} else {
				_txt.setText(String.valueOf(weeks) + " " + getResources().getString(R.string.status_text_week));
			}
		} else if (months < 12) {
			if (months < 2) {
				_txt.setText("1 " + getResources().getString(R.string.status_text_month));
			} else {
				_txt.setText(String.valueOf(months) + " " + getResources().getString(R.string.status_text_month));
			}
		} else {
			if (years < 2) {
				_txt.setText("1 " + getResources().getString(R.string.status_text_years));
			} else {
				_txt.setText(String.valueOf(years) + " " + getResources().getString(R.string.status_text_years));
			}
		}
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
					dbService.getReference("username").child(handle).getData(new IDataListener() {
						@Override
						public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
							if(dataSnapshot.exists()) {
								if (!dataSnapshot.getChild("uid").getValue(String.class).equals("null")) {
									intent.setClass(getApplicationContext(), ProfileActivity.class);
									intent.putExtra("uid", dataSnapshot.getChild("uid").getValue(String.class));
									startActivity(intent);
								} else {

								}
							} else {
							}
						}
						@Override
						public void onCancelled(@NonNull IDatabaseError databaseError) {
							//	swipeLayout.setVisibility(View.GONE);
							//noInternetBody.setVisibility(View.VISIBLE);
							//	loadingBody.setVisibility(View.GONE);
						}
					});
				}
			}

		}
		@Override
		public void updateDrawState(TextPaint ds) {
			ds.setUnderlineText(false);
			ds.setColor(getResources().getColor(R.color.md_theme_primary, null));
			ds.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
		}
	}


	public void _ImgRound(final ImageView _imageview, final double _value) {
		android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable ();
		gd.setColor(android.R.color.transparent);
		gd.setCornerRadius((int)_value);
		_imageview.setClipToOutline(true);
		_imageview.setBackground(gd);
	}


	public void _OpenWebView(final String _URL) {
		AndroidDevelopersBlogURL = _URL;
		CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
		builder.setToolbarColor(getResources().getColor(R.color.md_theme_surface, null));
		CustomTabsIntent customtabsintent = builder.build();
		customtabsintent.launchUrl(this, Uri.parse(AndroidDevelopersBlogURL));
	}

	public class ProfilePageTabUserPostsRecyclerViewAdapter extends RecyclerView.Adapter<ProfilePageTabUserPostsRecyclerViewAdapter.ViewHolder> {

		ArrayList<HashMap<String, Object>> _data;

		public ProfilePageTabUserPostsRecyclerViewAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.synapse_post_cv, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}

		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;

			final LinearLayout body = _view.findViewById(R.id.body);
			final LinearLayout top = _view.findViewById(R.id.top);
			final LinearLayout linear1 = _view.findViewById(R.id.linear1);
			final LinearLayout middle = _view.findViewById(R.id.middle);
			final LinearLayout bottom = _view.findViewById(R.id.bottom);
			final LinearLayout userInfo = _view.findViewById(R.id.userInfo);
			final LinearLayout topSpace = _view.findViewById(R.id.topSpace);
			final ImageView topMoreButton = _view.findViewById(R.id.topMoreButton);
			final androidx.cardview.widget.CardView userInfoProfileCard = _view.findViewById(R.id.userInfoProfileCard);
			final LinearLayout userInfoRight = _view.findViewById(R.id.userInfoRight);
			final ImageView userInfoProfileImage = _view.findViewById(R.id.userInfoProfileImage);
			final LinearLayout userInfoUsernameRightTop = _view.findViewById(R.id.userInfoUsernameRightTop);
			final LinearLayout btmPost = _view.findViewById(R.id.btmPost);
			final TextView userInfoUsername = _view.findViewById(R.id.userInfoUsername);
			final ImageView userInfoGenderBadge = _view.findViewById(R.id.userInfoGenderBadge);
			final ImageView userInfoUsernameVerifiedBadge = _view.findViewById(R.id.userInfoUsernameVerifiedBadge);
			final TextView postPublishDate = _view.findViewById(R.id.postPublishDate);
			final ImageView postPrivateStateIcon = _view.findViewById(R.id.postPrivateStateIcon);
			final TextView postMessageTextMiddle = _view.findViewById(R.id.postMessageTextMiddle);
			final ImageView postImage = _view.findViewById(R.id.postImage);
			final LinearLayout likeButton = _view.findViewById(R.id.likeButton);
			final LinearLayout commentsButton = _view.findViewById(R.id.commentsButton);
			final LinearLayout shareButton = _view.findViewById(R.id.shareButton);
			final LinearLayout bottomSpc = _view.findViewById(R.id.bottomSpc);
			final ImageView favoritePostButton = _view.findViewById(R.id.favoritePostButton);
			final ImageView likeButtonIc = _view.findViewById(R.id.likeButtonIc);
			final TextView likeButtonCount = _view.findViewById(R.id.likeButtonCount);
			final TextView textview1 = _view.findViewById(R.id.textview1);
			final TextView tv_1 = _view.findViewById(R.id.tv_1);
			final ImageView commentsButtonIc = _view.findViewById(R.id.commentsButtonIc);
			final TextView commentsButtonCount = _view.findViewById(R.id.commentsButtonCount);
			final TextView textview2 = _view.findViewById(R.id.textview2);
			final TextView tv_2 = _view.findViewById(R.id.tv_2);
			final ImageView shareButtonIc = _view.findViewById(R.id.shareButtonIc);
			final TextView shareButtonCount = _view.findViewById(R.id.shareButtonCount);

			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_view.setLayoutParams(_lp);
			body.setVisibility(View.GONE);
			if (_data.get((int)_position).get("post_type").toString().equals("TEXT") || (_data.get((int)_position).get("post_type").toString().equals("IMAGE") || _data.get((int)_position).get("post_type").toString().equals("VIDEO"))) {
				if (_data.get((int)_position).containsKey("post_text")) {
					//		postMessageTextMiddle.setText(_data.get((int)_position).get("post_text").toString());
					//postMessageTextMiddle.setText(_data.get((int)_position).get("post_text").toString());
					_textview_mh(postMessageTextMiddle, _data.get((int)_position).get("post_text").toString());

					postMessageTextMiddle.setVisibility(View.VISIBLE);
				} else {
					postMessageTextMiddle.setVisibility(View.GONE);
				}
				if (_data.get((int)_position).containsKey("post_image")) {
					Glide.with(getApplicationContext()).load(Uri.parse(_data.get((int)_position).get("post_image").toString())).into(postImage);
					postImage.setVisibility(View.VISIBLE);
				} else {
					postImage.setVisibility(View.GONE);
				}
			} else {
				postImage.setVisibility(View.GONE);
				postMessageTextMiddle.setVisibility(View.GONE);
			}

			if (_data.get((int)_position).get("post_hide_like_count").toString().equals("true")) {
				likeButtonCount.setVisibility(View.GONE);
			} else {
				likeButtonCount.setVisibility(View.VISIBLE);
			}
			if (_data.get((int)_position).get("post_hide_comments_count").toString().equals("true")) {
				commentsButtonCount.setVisibility(View.GONE);
			} else {
				commentsButtonCount.setVisibility(View.VISIBLE);
			}
			if (_data.get((int)_position).get("post_disable_comments").toString().equals("true")) {
				commentsButton.setVisibility(View.GONE);
			} else {
				commentsButton.setVisibility(View.VISIBLE);
			}
			_setTime(Double.parseDouble(_data.get((int)_position).get("publish_date").toString()), postPublishDate);
			if (UserInfoCacheMap.containsKey("uid-".concat(_data.get((int)_position).get("uid").toString()))) {
				if (_data.get((int)_position).get("post_visibility").toString().equals("private")) {
					if (authService.getCurrentUser() != null && _data.get((int)_position).get("uid").toString().equals(authService.getCurrentUser().getUid())) {
						postPrivateStateIcon.setVisibility(View.VISIBLE);
						body.setVisibility(View.VISIBLE);
					} else {
						body.setVisibility(View.GONE);
					}
				} else {
					body.setVisibility(View.VISIBLE);
					postPrivateStateIcon.setVisibility(View.GONE);
				}
				if (UserInfoCacheMap.get("banned-".concat(_data.get((int)_position).get("uid").toString())).toString().equals("true")) {
					userInfoProfileImage.setImageResource(R.drawable.avatar);
				} else {
					if (UserInfoCacheMap.get("avatar-".concat(_data.get((int)_position).get("uid").toString())).toString().equals("null")) {
						userInfoProfileImage.setImageResource(R.drawable.avatar);
					} else {
						Glide.with(getApplicationContext()).load(Uri.parse(UserInfoCacheMap.get("avatar-".concat(_data.get((int)_position).get("uid").toString())).toString())).into(userInfoProfileImage);
					}
				}
				if (UserInfoCacheMap.get("nickname-".concat(_data.get((int)_position).get("uid").toString())).toString().equals("null")) {
					userInfoUsername.setText("@" + UserInfoCacheMap.get("username-".concat(_data.get((int)_position).get("uid").toString())).toString());
				} else {
					userInfoUsername.setText(UserInfoCacheMap.get("nickname-".concat(_data.get((int)_position).get("uid").toString())).toString());
				}
				if (UserInfoCacheMap.get("gender-".concat(_data.get((int)_position).get("uid").toString())).toString().equals("hidden")) {
					userInfoGenderBadge.setVisibility(View.GONE);
				} else {
					if (UserInfoCacheMap.get("gender-".concat(_data.get((int)_position).get("uid").toString())).toString().equals("male")) {
						userInfoGenderBadge.setImageResource(R.drawable.male_badge);
						userInfoGenderBadge.setVisibility(View.VISIBLE);
					} else {
						if (UserInfoCacheMap.get("gender-".concat(_data.get((int)_position).get("uid").toString())).toString().equals("female")) {
							userInfoGenderBadge.setImageResource(R.drawable.female_badge);
							userInfoGenderBadge.setVisibility(View.VISIBLE);
						}
					}
				}
				if (UserInfoCacheMap.get("acc_type-".concat(_data.get((int)_position).get("uid").toString())).toString().equals("admin")) {
					userInfoUsernameVerifiedBadge.setImageResource(R.drawable.admin_badge);
					userInfoUsernameVerifiedBadge.setVisibility(View.VISIBLE);
				} else {
					if (UserInfoCacheMap.get("acc_type-".concat(_data.get((int)_position).get("uid").toString())).toString().equals("moderator")) {
						userInfoUsernameVerifiedBadge.setImageResource(R.drawable.moderator_badge);
						userInfoUsernameVerifiedBadge.setVisibility(View.VISIBLE);
					} else {
						if (UserInfoCacheMap.get("acc_type-".concat(_data.get((int)_position).get("uid").toString())).toString().equals("support")) {
							userInfoUsernameVerifiedBadge.setImageResource(R.drawable.support_badge);
							userInfoUsernameVerifiedBadge.setVisibility(View.VISIBLE);
						} else {
							if (UserInfoCacheMap.get("acc_type-".concat(_data.get((int)_position).get("uid").toString())).toString().equals("user")) {
								if (UserInfoCacheMap.get("verify-".concat(_data.get((int)_position).get("uid").toString())).toString().equals("true")) {
									userInfoUsernameVerifiedBadge.setVisibility(View.VISIBLE);
								} else {
									userInfoUsernameVerifiedBadge.setVisibility(View.GONE);
								}
							}
						}
					}
				}

			} else {
				dbService.getUserById(_data.get((int)_position).get("uid").toString(), new IDataListener() {
					@Override
					public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
						if(dataSnapshot.exists()) {
							UserInfoCacheMap.put("uid-".concat(_data.get((int)_position).get("uid").toString()), _data.get((int)_position).get("uid").toString());
							UserInfoCacheMap.put("banned-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.getChild("banned").getValue(String.class));
							UserInfoCacheMap.put("nickname-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.getChild("nickname").getValue(String.class));
							UserInfoCacheMap.put("username-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.getChild("username").getValue(String.class));
							UserInfoCacheMap.put("gender-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.getChild("gender").getValue(String.class));
							UserInfoCacheMap.put("verify-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.getChild("verify").getValue(String.class));
							UserInfoCacheMap.put("acc_type-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.getChild("account_type").getValue(String.class));
							if (_data.get((int)_position).get("post_visibility").toString().equals("private")) {
								if (authService.getCurrentUser() != null && _data.get((int)_position).get("uid").toString().equals(authService.getCurrentUser().getUid())) {
									postPrivateStateIcon.setVisibility(View.VISIBLE);
									body.setVisibility(View.VISIBLE);
								} else {
									body.setVisibility(View.GONE);
								}
							} else {
								body.setVisibility(View.VISIBLE);
								postPrivateStateIcon.setVisibility(View.GONE);
							}
							if (dataSnapshot.getChild("banned").getValue(String.class).equals("true")) {
								userInfoProfileImage.setImageResource(R.drawable.avatar);
								UserInfoCacheMap.put("avatar-".concat(_data.get((int)_position).get("uid").toString()), "null");
							} else {
								UserInfoCacheMap.put("avatar-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.getChild("avatar").getValue(String.class));
								if (dataSnapshot.getChild("avatar").getValue(String.class).equals("null")) {
									userInfoProfileImage.setImageResource(R.drawable.avatar);
								} else {
									Glide.with(getApplicationContext()).load(Uri.parse(dataSnapshot.getChild("avatar").getValue(String.class))).into(userInfoProfileImage);
								}
							}
							if (dataSnapshot.getChild("nickname").getValue(String.class).equals("null")) {
								userInfoUsername.setText("@" + dataSnapshot.getChild("username").getValue(String.class));
							} else {
								userInfoUsername.setText(dataSnapshot.getChild("nickname").getValue(String.class));
							}
							if (dataSnapshot.getChild("gender").getValue(String.class).equals("hidden")) {
								userInfoGenderBadge.setVisibility(View.GONE);
							} else {
								if (dataSnapshot.getChild("gender").getValue(String.class).equals("male")) {
									userInfoGenderBadge.setImageResource(R.drawable.male_badge);
									userInfoGenderBadge.setVisibility(View.VISIBLE);
								} else {
									if (dataSnapshot.getChild("gender").getValue(String.class).equals("female")) {
										userInfoGenderBadge.setImageResource(R.drawable.female_badge);
										userInfoGenderBadge.setVisibility(View.VISIBLE);
									}
								}
							}
							if (dataSnapshot.getChild("account_type").getValue(String.class).equals("admin")) {
								userInfoUsernameVerifiedBadge.setImageResource(R.drawable.admin_badge);
								userInfoUsernameVerifiedBadge.setVisibility(View.VISIBLE);
							} else {
								if (dataSnapshot.getChild("account_type").getValue(String.class).equals("moderator")) {
									userInfoUsernameVerifiedBadge.setImageResource(R.drawable.moderator_badge);
									userInfoUsernameVerifiedBadge.setVisibility(View.VISIBLE);
								} else {
									if (dataSnapshot.getChild("account_type").getValue(String.class).equals("support")) {
										userInfoUsernameVerifiedBadge.setImageResource(R.drawable.support_badge);
										userInfoUsernameVerifiedBadge.setVisibility(View.VISIBLE);
									} else {
										if (dataSnapshot.getChild("account_type").getValue(String.class).equals("user")) {
											if (dataSnapshot.getChild("verify").getValue(String.class).equals("true")) {
												userInfoUsernameVerifiedBadge.setImageResource(R.drawable.verified_badge);
												userInfoUsernameVerifiedBadge.setVisibility(View.VISIBLE);
											} else {
												userInfoUsernameVerifiedBadge.setVisibility(View.GONE);
											}
										}
									}
								}
							}
						} else {
						}
					}
					@Override
					public void onCancelled(@NonNull IDatabaseError databaseError) {

					}
				});

			}
			if (authService.getCurrentUser() != null) {
				dbService.getReference("posts-likes").child(_data.get((int)_position).get("key").toString()).child(authService.getCurrentUser().getUid()).getData(new IDataListener() {
					@Override
					public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
						if(dataSnapshot.exists()) {
							likeButtonIc.setImageResource(R.drawable.post_icons_1_2);
						} else {
							likeButtonIc.setImageResource(R.drawable.post_icons_1_1);
						}
					}

					@Override
					public void onCancelled(@NonNull IDatabaseError databaseError) {

					}
				});
			}
			dbService.getReference("posts-comments").child(_data.get((int)_position).get("key").toString()).getData(new IDataListener() {
				@Override
				public void onDataChange(IDataSnapshot dataSnapshot) {
					long count = dataSnapshot.getChildrenCount();
					_setCount(commentsButtonCount, count);
				}

				@Override
				public void onCancelled(IDatabaseError databaseError) {

				}
			});
			dbService.getReference("posts-likes").child(_data.get((int)_position).get("key").toString()).getData(new IDataListener() {
				@Override
				public void onDataChange(IDataSnapshot dataSnapshot) {
					long count = dataSnapshot.getChildrenCount();
					_setCount(likeButtonCount, count);
					postLikeCountCache.put(_data.get((int)_position).get("key").toString(), String.valueOf((long)(count)));
				}

				@Override
				public void onCancelled(IDatabaseError databaseError) {

				}
			});
			if (authService.getCurrentUser() != null) {
				dbService.getReference("favorite-posts").child(authService.getCurrentUser().getUid()).child(_data.get((int)_position).get("key").toString()).getData(new IDataListener() {
					@Override
					public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
						if(dataSnapshot.exists()) {
							favoritePostButton.setImageResource(R.drawable.delete_favorite_post_ic);
						} else {
							favoritePostButton.setImageResource(R.drawable.add_favorite_post_ic);
						}
					}

					@Override
					public void onCancelled(@NonNull IDatabaseError databaseError) {

					}
				});
			}

			likeButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					if (authService.getCurrentUser() == null) return;
					dbService.getReference("posts-likes").child(_data.get((int)_position).get("key").toString()).child(authService.getCurrentUser().getUid()).getData(new IDataListener() {
						@Override
						public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
							if(dataSnapshot.exists()) {
								dbService.getReference("posts-likes").child(_data.get((int)_position).get("key").toString()).child(authService.getCurrentUser().getUid()).setValue(null, (result, error) -> {});
								postLikeCountCache.put(_data.get((int)_position).get("key").toString(), String.valueOf((long)(Double.parseDouble(postLikeCountCache.get(_data.get((int)_position).get("key").toString()).toString()) - 1)));
								_setCount(likeButtonCount, Double.parseDouble(postLikeCountCache.get(_data.get((int)_position).get("key").toString()).toString()));
								likeButtonIc.setImageResource(R.drawable.post_icons_1_1);
							} else {
								dbService.getReference("posts-likes").child(_data.get((int)_position).get("key").toString()).child(authService.getCurrentUser().getUid()).setValue(authService.getCurrentUser().getUid(), (result, error) -> {});
								com.synapse.social.studioasinc.util.NotificationUtils.sendPostLikeNotification(_data.get((int)_position).get("key").toString(), _data.get((int)_position).get("uid").toString());
								postLikeCountCache.put(_data.get((int)_position).get("key").toString(), String.valueOf((long)(Double.parseDouble(postLikeCountCache.get(_data.get((int)_position).get("key").toString()).toString()) + 1)));
								_setCount(likeButtonCount, Double.parseDouble(postLikeCountCache.get(_data.get((int)_position).get("key").toString()).toString()));
								likeButtonIc.setImageResource(R.drawable.post_icons_1_2);
							}
						}

						@Override
						public void onCancelled(@NonNull IDatabaseError databaseError) {

						}
					});
					vbr.vibrate((long)(24));
				}
			});
			commentsButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					Bundle sendPostKey = new Bundle();
					sendPostKey.putString("postKey", _data.get((int)_position).get("key").toString());
					sendPostKey.putString("postPublisherUID", _data.get((int)_position).get("uid").toString());
					sendPostKey.putString("postPublisherAvatar", UserInfoCacheMap.get("avatar-".concat(_data.get((int)_position).get("uid").toString())).toString());
					PostCommentsBottomSheetDialog postCommentsBottomSheet = new PostCommentsBottomSheetDialog();
					postCommentsBottomSheet.setArguments(sendPostKey);
					postCommentsBottomSheet.show(getSupportFragmentManager(), postCommentsBottomSheet.getTag());
				}
			});
			userInfo.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					intent.setClass(getApplicationContext(), ProfileActivity.class);
					intent.putExtra("uid", _data.get((int)_position).get("uid").toString());
					startActivity(intent);
				}
			});
			favoritePostButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					if (authService.getCurrentUser() == null) return;
					dbService.getReference("favorite-posts").child(authService.getCurrentUser().getUid()).child(_data.get((int)_position).get("key").toString()).getData(new IDataListener() {
						@Override
						public void onDataChange(@NonNull IDataSnapshot dataSnapshot) {
							if(dataSnapshot.exists()) {
								dbService.getReference("favorite-posts").child(authService.getCurrentUser().getUid()).child(_data.get((int)_position).get("key").toString()).setValue(null, (result, error) -> {});
								favoritePostButton.setImageResource(R.drawable.add_favorite_post_ic);
							} else {
								dbService.getReference("favorite-posts").child(authService.getCurrentUser().getUid()).child(_data.get((int)_position).get("key").toString()).setValue(_data.get((int)_position).get("key").toString(), (result, error) -> {});
								favoritePostButton.setImageResource(R.drawable.delete_favorite_post_ic);
							}
						}

						@Override
						public void onCancelled(@NonNull IDatabaseError databaseError) {

						}
					});
					vbr.vibrate((long)(24));
				}
			});
			topMoreButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					Bundle sendPostKey = new Bundle();
					sendPostKey.putString("postKey", _data.get((int)_position).get("key").toString());
					sendPostKey.putString("postPublisherUID", _data.get((int)_position).get("uid").toString());
					sendPostKey.putString("postType", _data.get((int)_position).get("post_type").toString());
					PostMoreBottomSheetDialog postMoreBottomSheetDialog = new PostMoreBottomSheetDialog();
					postMoreBottomSheetDialog.setArguments(sendPostKey);
					postMoreBottomSheetDialog.show(getSupportFragmentManager(), postMoreBottomSheetDialog.getTag());
				}
			});
			likeButton.setBackgroundResource(R.drawable.shape_rounded_16dp_stroke);
			commentsButton.setBackgroundResource(R.drawable.shape_rounded_16dp_stroke);
			postImage.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					_OpenWebView(_data.get((int)_position).get("post_image").toString());
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