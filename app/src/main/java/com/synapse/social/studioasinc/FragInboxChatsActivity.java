package com.synapse.social.studioasinc;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.os.Bundle;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.gridlayout.*;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.chip.*;
import com.google.android.material.chip.ChipGroup;
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
import com.theartofdev.edmodo.cropper.*;
import com.yalantis.ucrop.*;
import java.io.*;
import java.io.InputStream;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import com.synapse.social.studioasinc.R;
import org.json.*;
import androidx.core.widget.NestedScrollView;
import com.google.firebase.database.Query;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.appcompat.app.AppCompatDelegate;
import com.bumptech.glide.Glide;
import com.synapse.social.studioasinc.animations.ShimmerFrameLayout;
import com.synapse.social.studioasinc.util.AuthUtil;

public class FragInboxChatsActivity extends Fragment {

	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();

	private HashMap<String, Object> UserInfoCacheMap = new HashMap<>();

	private ArrayList<HashMap<String, Object>> ChatInboxList = new ArrayList<>();

	private LinearLayout linear2;
	private HorizontalScrollView hscroll1;
	private RecyclerView inboxListRecyclerView;
	private ChipGroup linear9;
	private Chip linear10;
	private Chip linear29;
	private Chip linear30;
	private Chip linear31;
	private Chip linear32;
	private Chip linear33;
    private ShimmerFrameLayout shimmer_view_container;

	private FirebaseAuth auth;
	private OnCompleteListener<AuthResult> _auth_create_user_listener;
	private OnCompleteListener<AuthResult> _auth_sign_in_listener;
	private OnCompleteListener<Void> _auth_reset_password_listener;
	private OnCompleteListener<Void> auth_updateEmailListener;
	private OnCompleteListener<Void> auth_updatePasswordListener;
	private OnCompleteListener<Void> auth_emailVerificationSentListener;
	private OnCompleteListener<Void> auth_deleteUserListener;
	private OnCompleteListener<Void> auth_updateProfileListener;
	private OnCompleteListener<AuthResult> auth_phoneAuthListener;
	private OnCompleteListener<AuthResult> auth_googleSignInListener;
	private DatabaseReference main = _firebase.getReference("skyline");
	private ChildEventListener _main_child_listener;
	private Intent intent = new Intent();

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater _inflater, @Nullable ViewGroup _container, @Nullable Bundle _savedInstanceState) {
		View _view = _inflater.inflate(R.layout.frag_inbox_chats, _container, false);
		initialize(_savedInstanceState, _view);
		FirebaseApp.initializeApp(getContext());
		initializeLogic();
		return _view;
	}

	private void initialize(Bundle _savedInstanceState, View _view) {
		linear2 = _view.findViewById(R.id.linear2);
		hscroll1 = _view.findViewById(R.id.hscroll1);
		inboxListRecyclerView = _view.findViewById(R.id.inboxListRecyclerView);
		linear9 = _view.findViewById(R.id.linear9);
		linear10 = _view.findViewById(R.id.linear10);
		linear29 = _view.findViewById(R.id.linear29);
		linear30 = _view.findViewById(R.id.linear30);
		linear31 = _view.findViewById(R.id.linear31);
		linear32 = _view.findViewById(R.id.linear32);
		linear33 = _view.findViewById(R.id.linear33);
        shimmer_view_container = _view.findViewById(R.id.shimmer_view_container);
		auth = FirebaseAuth.getInstance();

		_main_child_listener = new ChildEventListener() {
			@Override
			public void onChildAdded(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);

			}

			@Override
			public void onChildChanged(DataSnapshot _param1, String _param2) {
				GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
				final String _childKey = _param1.getKey();
				final HashMap<String, Object> _childValue = _param1.getValue(_ind);

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
		main.addChildEventListener(_main_child_listener);

		auth_updateEmailListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_updatePasswordListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_emailVerificationSentListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_deleteUserListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_phoneAuthListener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> task) {
				final boolean _success = task.isSuccessful();
				final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";

			}
		};

		auth_updateProfileListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_googleSignInListener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> task) {
				final boolean _success = task.isSuccessful();
				final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";

			}
		};

		_auth_create_user_listener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		_auth_sign_in_listener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		_auth_reset_password_listener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();

			}
		};
	}

	private void initializeLogic() {
		inboxListRecyclerView.setAdapter(new InboxListRecyclerViewAdapter(ChatInboxList));
		inboxListRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
		_getInboxReference();
	}

	public void _ImgRound(final ImageView _imageview, final double _value) {
		android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable ();
		gd.setColor(android.R.color.transparent);
		gd.setCornerRadius((int)_value);
		_imageview.setClipToOutline(true);
		_imageview.setBackground(gd);
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


	public void _getInboxReference() {
		shimmer_view_container.startShimmer();
		shimmer_view_container.setVisibility(View.VISIBLE);
		inboxListRecyclerView.setVisibility(View.GONE);
		
		// Get current user UID safely
		String userUid = AuthUtil.getCurrentUserUid();
		if (userUid == null) {
			// No authentication available, hide shimmer and show empty state
			shimmer_view_container.stopShimmer();
			shimmer_view_container.setVisibility(View.GONE);
			inboxListRecyclerView.setVisibility(View.GONE);
			return;
		}
		
		Query getInboxRef = FirebaseDatabase.getInstance().getReference("skyline/inbox").child(userUid);
		getInboxRef.addValueEventListener(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				shimmer_view_container.stopShimmer();
				shimmer_view_container.setVisibility(View.GONE);
				inboxListRecyclerView.setVisibility(View.VISIBLE);
				if(dataSnapshot.exists()) {
					inboxListRecyclerView.setVisibility(View.VISIBLE);
					ChatInboxList.clear();
					try {
						GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
						for (DataSnapshot _data : dataSnapshot.getChildren()) {
							HashMap<String, Object> _map = _data.getValue(_ind);
							ChatInboxList.add(_map);
						}
					} catch (Exception _e) {
						_e.printStackTrace();
					}

					SketchwareUtil.sortListMap(ChatInboxList, "push_date", false, false);
					inboxListRecyclerView.getAdapter().notifyDataSetChanged();
				} else {
					inboxListRecyclerView.setVisibility(View.GONE);
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {
                shimmer_view_container.stopShimmer();
                shimmer_view_container.setVisibility(View.GONE);
                inboxListRecyclerView.setVisibility(View.VISIBLE);
			}
		});
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

	private boolean isNullOrEmpty(String str) {
		return str == null || str.trim().isEmpty() || "null".equalsIgnoreCase(str);
	}
	public class InboxListRecyclerViewAdapter extends RecyclerView.Adapter<InboxListRecyclerViewAdapter.ViewHolder> {

		ArrayList<HashMap<String, Object>> _data;

		public InboxListRecyclerViewAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getActivity().getLayoutInflater();
			View _v = _inflater.inflate(R.layout.item_inbox_message, parent, false);
			return new ViewHolder(_v);
		}

		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;

			final RelativeLayout main = _view.findViewById(R.id.main);
			final ImageView profileCardImage = _view.findViewById(R.id.profileCardImage);
			final LinearLayout userStatusCircleBG = _view.findViewById(R.id.userStatusCircleBG);
			final TextView push = _view.findViewById(R.id.push);
			final TextView username = _view.findViewById(R.id.username);
			final ImageView genderBadge = _view.findViewById(R.id.genderBadge);
			final ImageView verifiedBadge = _view.findViewById(R.id.verifiedBadge);
			final TextView last_message = _view.findViewById(R.id.last_message);
			final ImageView message_state = _view.findViewById(R.id.message_state);
			final TextView unread_messages_count_badge = _view.findViewById(R.id.unread_messages_count_badge);

			try{
				userStatusCircleBG.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)300, 0xFFFFFFFF));
				unread_messages_count_badge.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)300, com.synapse.social.studioasinc.util.ThemeUtils.getThemeColor(getContext(), com.google.android.material.R.attr.colorPrimaryContainer)));
				unread_messages_count_badge.setVisibility(View.GONE);
				main.setVisibility(View.GONE);
				if (_data.get((int)_position).get("last_message_text").toString().equals("null")) {
					last_message.setText(getResources().getString(R.string.m_no_chats));
				} else {
					last_message.setText(_data.get((int)_position).get("last_message_text").toString());
				}
				// Get current user UID safely
				String currentUserUid = AuthUtil.getCurrentUserUid();
				
				if (currentUserUid != null && _data.get((int)_position).get("last_message_uid").toString().equals(currentUserUid)) {
					if (_data.get((int)_position).get("last_message_state").toString().equals("sended")) {
						message_state.setImageResource(R.drawable.icon_done_round);
                        message_state.setContentDescription("Message sent");
					} else {
						message_state.setImageResource(R.drawable.icon_done_all_round);
                        message_state.setContentDescription("Message read");
					}
					last_message.setTextColor(0xFF616161);
					push.setTextColor(0xFF616161);
					message_state.setVisibility(View.VISIBLE);
					unread_messages_count_badge.setVisibility(View.GONE);
				} else {
					message_state.setVisibility(View.GONE);
					{
						ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
						Handler mMainHandler = new Handler(Looper.getMainLooper());
						
						mExecutorService.execute(new Runnable() {
							@Override
							public void run() {
								// Get current user UID safely
								String currentUserUid = AuthUtil.getCurrentUserUid();
								
								if (currentUserUid != null) {
									Query getUnreadMessagesCount = FirebaseDatabase.getInstance().getReference("skyline/chats").child(currentUserUid).child(_data.get((int)_position).get("uid").toString()).orderByChild("message_state").equalTo("sended");
									getUnreadMessagesCount.addValueEventListener(new ValueEventListener() {
									@Override
									public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
										mMainHandler.post(new Runnable() {
											@Override
											public void run() {
												long unReadMessageCount = dataSnapshot.getChildrenCount();
												if(dataSnapshot.exists()) {
													last_message.setTextColor(0xFF000000);
													push.setTextColor(0xFF000000);
													//	last_message.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/appfont.ttf"), 1);
													//	push.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/appfont.ttf"), 1);
													unread_messages_count_badge.setText(String.valueOf((long)(unReadMessageCount)));
													unread_messages_count_badge.setVisibility(View.VISIBLE);
												} else {
													last_message.setTextColor(0xFF616161);
													push.setTextColor(0xFF616161);
													//	last_message.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/appfont.ttf"), 0);
													//	push.setTypeface(Typeface.createFromAsset(getContext().getAssets(),"fonts/appfont.ttf"), 0);
													unread_messages_count_badge.setVisibility(View.GONE);
												}
											}
										});
									}
									
									@Override
									public void onCancelled(@NonNull DatabaseError databaseError) {
										
									}
								});
								}
							}
						});
					}
				}
				_setTime(Double.parseDouble(_data.get((int)_position).get("push_date").toString()), push);
				if (UserInfoCacheMap.containsKey("uid-".concat(_data.get((int)_position).get("uid").toString()))) {
					main.setVisibility(View.VISIBLE);
					
					// Get uid once to avoid repeated calls
					String uid = _data.get((int)_position).get("uid").toString();
					
					// Handle banned status with null check
					Object bannedObj = UserInfoCacheMap.get("banned-".concat(uid));
					if (bannedObj != null && bannedObj.toString().equals("true")) {
						profileCardImage.setImageResource(R.drawable.banned_avatar);
					} else {
						// Handle avatar with null check
						Object avatarObj = UserInfoCacheMap.get("avatar-".concat(uid));
						if (isNullOrEmpty(avatarObj != null ? avatarObj.toString() : null)) {
							profileCardImage.setImageResource(R.drawable.avatar);
						} else {
							Glide.with(getContext())
							.load(Uri.parse(avatarObj.toString()))
							.into(profileCardImage);
						}
					}
					
					// Handle nickname with null check
					Object nicknameObj = UserInfoCacheMap.get("nickname-".concat(uid));
					if (isNullOrEmpty(nicknameObj != null ? nicknameObj.toString() : null)) {
						Object usernameObj = UserInfoCacheMap.get("username-".concat(uid));
						username.setText("@" + (usernameObj != null ? usernameObj.toString() : "unknown"));
                        profileCardImage.setContentDescription("Profile picture of " + username.getText().toString());
					} else {
						username.setText(nicknameObj.toString());
                        profileCardImage.setContentDescription("Profile picture of " + username.getText().toString());
					}
					
					// Handle status with null check
					Object statusObj = UserInfoCacheMap.get("status-".concat(uid));
					userStatusCircleBG.setVisibility(statusObj != null && statusObj.toString().equals("online") 
					? View.VISIBLE : View.GONE);
					
					// Handle gender with null check
					Object genderObj = UserInfoCacheMap.get("gender-".concat(uid));
					if (isNullOrEmpty(genderObj != null ? genderObj.toString() : null) || "hidden".equals(genderObj != null ? genderObj.toString() : null)) {
						genderBadge.setVisibility(View.GONE);
					} else {
						genderBadge.setVisibility(View.VISIBLE);
						String gender = genderObj.toString();
						if (gender.equals("male")) {
							genderBadge.setImageResource(R.drawable.male_badge);
                            genderBadge.setContentDescription("Male");
						} else if (gender.equals("female")) {
							genderBadge.setImageResource(R.drawable.female_badge);
                            genderBadge.setContentDescription("Female");
						}
					}
					
					// Handle account type and badges with null checks
					Object accountTypeObj = UserInfoCacheMap.get("account_type-".concat(uid));
					Object premiumObj = UserInfoCacheMap.get("account_premium-".concat(uid));
					Object verifyObj = UserInfoCacheMap.get("verify-".concat(uid));
					
					if (accountTypeObj != null) {
						String accountType = accountTypeObj.toString();
						if (accountType.equals("admin")) {
							verifiedBadge.setImageResource(R.drawable.admin_badge);
							verifiedBadge.setVisibility(View.VISIBLE);
                            verifiedBadge.setContentDescription("Admin badge");
						} else if (accountType.equals("moderator")) {
							verifiedBadge.setImageResource(R.drawable.moderator_badge);
							verifiedBadge.setVisibility(View.VISIBLE);
                            verifiedBadge.setContentDescription("Moderator badge");
						} else if (accountType.equals("support")) {
							verifiedBadge.setImageResource(R.drawable.support_badge);
							verifiedBadge.setVisibility(View.VISIBLE);
                            verifiedBadge.setContentDescription("Support badge");
						} else if (premiumObj != null && premiumObj.toString().equals("true")) {
							verifiedBadge.setImageResource(R.drawable.premium_badge);
							verifiedBadge.setVisibility(View.VISIBLE);
                            verifiedBadge.setContentDescription("Premium badge");
						} else if (verifyObj != null && verifyObj.toString().equals("true")) {
							verifiedBadge.setImageResource(R.drawable.verified_badge);
							verifiedBadge.setVisibility(View.VISIBLE);
                            verifiedBadge.setContentDescription("Verified badge");
						} else {
							verifiedBadge.setVisibility(View.GONE);
						}
					} else if (premiumObj != null && premiumObj.toString().equals("true")) {
						verifiedBadge.setImageResource(R.drawable.premium_badge);
						verifiedBadge.setVisibility(View.VISIBLE);
                        verifiedBadge.setContentDescription("Premium badge");
					} else if (verifyObj != null && verifyObj.toString().equals("true")) {
						verifiedBadge.setImageResource(R.drawable.verified_badge);
						verifiedBadge.setVisibility(View.VISIBLE);
                        verifiedBadge.setContentDescription("Verified badge");
					} else {
						verifiedBadge.setVisibility(View.GONE);
					}
				} else {
					{
						ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
						Handler mMainHandler = new Handler(Looper.getMainLooper());
						
						mExecutorService.execute(new Runnable() {
							@Override
							public void run() {
								DatabaseReference getUserReference = FirebaseDatabase.getInstance().getReference("skyline/users").child(_data.get((int)_position).get("uid").toString());
								getUserReference.addListenerForSingleValueEvent(new ValueEventListener() {
									@Override
									public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
										mMainHandler.post(new Runnable() {
											@Override
											public void run() {
												if(dataSnapshot.exists()) {
													UserInfoCacheMap.put("uid-".concat(_data.get((int)_position).get("uid").toString()), _data.get((int)_position).get("uid").toString());
													UserInfoCacheMap.put("avatar-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.child("avatar").getValue(String.class));
													UserInfoCacheMap.put("banned-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.child("banned").getValue(String.class));
													UserInfoCacheMap.put("username-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.child("username").getValue(String.class));
													UserInfoCacheMap.put("nickname-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.child("nickname").getValue(String.class));
													UserInfoCacheMap.put("status-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.child("status").getValue(String.class));
													UserInfoCacheMap.put("gender-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.child("gender").getValue(String.class));
													UserInfoCacheMap.put("account_type-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.child("account_type").getValue(String.class));
													UserInfoCacheMap.put("account_premium-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.child("account_premium").getValue(String.class));
													UserInfoCacheMap.put("verify-".concat(_data.get((int)_position).get("uid").toString()), dataSnapshot.child("verify").getValue(String.class));
													main.setVisibility(View.VISIBLE);
													String banned = dataSnapshot.child("banned").getValue(String.class);
													String avatar = dataSnapshot.child("avatar").getValue(String.class);
													String nickname = dataSnapshot.child("nickname").getValue(String.class);
													String usernameValue = dataSnapshot.child("username").getValue(String.class);
													String status = dataSnapshot.child("status").getValue(String.class);
													String gender = dataSnapshot.child("gender").getValue(String.class);
													String accountType = dataSnapshot.child("account_type").getValue(String.class);
													String accountPremium = dataSnapshot.child("account_premium").getValue(String.class);
													String verify = dataSnapshot.child("verify").getValue(String.class);

													if ("true".equals(banned)) {
														profileCardImage.setImageResource(R.drawable.banned_avatar);
													} else {
														if (isNullOrEmpty(avatar)) {
															profileCardImage.setImageResource(R.drawable.avatar);
														} else {
															Glide.with(getContext()).load(Uri.parse(avatar)).into(profileCardImage);
														}
													}

													if (isNullOrEmpty(nickname)) {
														username.setText("@" + (usernameValue != null ? usernameValue : ""));
                                                        profileCardImage.setContentDescription("Profile picture of " + username.getText().toString());
													} else {
														username.setText(nickname);
                                                        profileCardImage.setContentDescription("Profile picture of " + username.getText().toString());
													}

													if ("online".equals(status)) {
														userStatusCircleBG.setVisibility(View.VISIBLE);
													} else {
														userStatusCircleBG.setVisibility(View.GONE);
													}

													if (isNullOrEmpty(gender) || "hidden".equals(gender)) {
														genderBadge.setVisibility(View.GONE);
													} else {
														if ("male".equals(gender)) {
															genderBadge.setImageResource(R.drawable.male_badge);
															genderBadge.setVisibility(View.VISIBLE);
                                                            genderBadge.setContentDescription("Male");
														} else if ("female".equals(gender)) {
															genderBadge.setImageResource(R.drawable.female_badge);
															genderBadge.setVisibility(View.VISIBLE);
                                                            genderBadge.setContentDescription("Female");
														}
													}

													verifiedBadge.setVisibility(View.GONE);
													if ("admin".equals(accountType)) {
														verifiedBadge.setImageResource(R.drawable.admin_badge);
														verifiedBadge.setVisibility(View.VISIBLE);
                                                        verifiedBadge.setContentDescription("Admin badge");
													} else if ("moderator".equals(accountType)) {
														verifiedBadge.setImageResource(R.drawable.moderator_badge);
														verifiedBadge.setVisibility(View.VISIBLE);
                                                        verifiedBadge.setContentDescription("Moderator badge");
													} else if ("support".equals(accountType)) {
														verifiedBadge.setImageResource(R.drawable.support_badge);
														verifiedBadge.setVisibility(View.VISIBLE);
                                                        verifiedBadge.setContentDescription("Support badge");
													} else if ("true".equals(accountPremium)) {
														verifiedBadge.setImageResource(R.drawable.premium_badge);
														verifiedBadge.setVisibility(View.VISIBLE);
                                                        verifiedBadge.setContentDescription("Premium badge");
													} else if ("true".equals(verify)) {
														verifiedBadge.setImageResource(R.drawable.verified_badge);
														verifiedBadge.setVisibility(View.VISIBLE);
                                                        verifiedBadge.setContentDescription("Verified badge");
													}
												} else {
												}
											}
										});
									}
									
									@Override
									public void onCancelled(@NonNull DatabaseError databaseError) {
										
									}
								});
							}
						});
					}

				}
				main.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View _view) {
						intent.setClass(getContext().getApplicationContext(), ChatActivity.class);
						intent.putExtra("uid", _data.get((int)_position).get("uid").toString());
						intent.putExtra("origin", "InboxActivity");
						startActivity(intent);
					}
				});
			}catch(Exception e){
				
			}
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