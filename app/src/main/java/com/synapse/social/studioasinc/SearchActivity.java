package com.synapse.social.studioasinc;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Context;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.os.Bundle;
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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
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
import com.bumptech.glide.Glide;
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService;
import com.synapse.social.studioasinc.backend.interfaces.IDataListener;
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError;
import com.theartofdev.edmodo.cropper.*;
import com.yalantis.ucrop.*;
import java.io.*;
import java.io.InputStream;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;
import androidx.core.widget.NestedScrollView;


import androidx.annotation.NonNull;
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService;
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService;
import java.util.Map;

public class SearchActivity extends AppCompatActivity {

	private IAuthenticationService authService;
    private IDatabaseService dbService;

	private ArrayList<HashMap<String, Object>> searchedUsersList = new ArrayList<>();

	private LinearLayout body;
	private LinearLayout middleLayout;
	private LinearLayout bottomSpc;
	private LinearLayout topLayout;
	private LinearLayout SearchUserLayout;
	private LinearLayout topLayoutBar;
	private LinearLayout topLayoutBarMiddle;
	private LinearLayout bottomBar;
	private TextView topLayoutBarTitle;
	private LinearLayout bottom_home;
	private LinearLayout bottom_search;
	private LinearLayout bottom_videos;
	private LinearLayout bottom_chats;
	private LinearLayout bottom_profile;
	private ImageView bottom_home_ic;
	private ImageView bottom_search_ic;
	private ImageView bottom_videos_ic;
	private ImageView bottom_chats_ic;
	private ImageView bottom_profile_ic;
	private ImageView topLayoutBarMiddleSearchLayoutCancel;
	private LinearLayout topLayoutBarMiddleSearchLayout;
	private EditText topLayoutBarMiddleSearchInput;
	private ImageView topLayoutBarMiddleSearchLayoutIc;
	private RecyclerView SearchUserLayoutRecyclerView;
	private TextView SearchUserLayoutNoUserFound;

	private Intent intent = new Intent();
	private Vibrator vbr;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_search);
		initialize(_savedInstanceState);
		initializeLogic();
	}

	private void initialize(Bundle _savedInstanceState) {
		body = findViewById(R.id.body);
		middleLayout = findViewById(R.id.middleLayout);
		bottomSpc = findViewById(R.id.bottomSpc);
		topLayout = findViewById(R.id.topLayout);
		SearchUserLayout = findViewById(R.id.SearchUserLayout);
		topLayoutBar = findViewById(R.id.topLayoutBar);
		topLayoutBarMiddle = findViewById(R.id.topLayoutBarMiddle);
		bottomBar = findViewById(R.id.bottomBar);
		topLayoutBarTitle = findViewById(R.id.topLayoutBarTitle);
		bottom_home = findViewById(R.id.bottom_home);
		bottom_search = findViewById(R.id.bottom_search);
		bottom_videos = findViewById(R.id.bottom_videos);
		bottom_chats = findViewById(R.id.bottom_chats);
		bottom_profile = findViewById(R.id.bottom_profile);
		bottom_home_ic = findViewById(R.id.bottom_home_ic);
		bottom_search_ic = findViewById(R.id.bottom_search_ic);
		bottom_videos_ic = findViewById(R.id.bottom_videos_ic);
		bottom_chats_ic = findViewById(R.id.bottom_chats_ic);
		bottom_profile_ic = findViewById(R.id.bottom_profile_ic);
		topLayoutBarMiddleSearchLayoutCancel = findViewById(R.id.topLayoutBarMiddleSearchLayoutCancel);
		topLayoutBarMiddleSearchLayout = findViewById(R.id.topLayoutBarMiddleSearchLayout);
		topLayoutBarMiddleSearchInput = findViewById(R.id.topLayoutBarMiddleSearchInput);
		topLayoutBarMiddleSearchLayoutIc = findViewById(R.id.topLayoutBarMiddleSearchLayoutIc);
		SearchUserLayoutRecyclerView = findViewById(R.id.SearchUserLayoutRecyclerView);
		SearchUserLayoutNoUserFound = findViewById(R.id.SearchUserLayoutNoUserFound);
		vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		authService = ((SynapseApp) getApplication()).getAuthService();
        dbService = ((SynapseApp) getApplication()).getDbService();

		bottom_home.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), HomeActivity.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
				startActivity(intent);
				finish();
			}
		});

		bottom_search.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {

			}
		});

		bottom_videos.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), LineVideoPlayerActivity.class);
				startActivity(intent);
			}
		});


		bottom_profile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), ProfileActivity.class);
				if (authService.getCurrentUser() != null) {
					intent.putExtra("uid", authService.getCurrentUser().getUid());
				}
				startActivity(intent);
			}
		});

		topLayoutBarMiddleSearchLayoutCancel.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				SearchUserLayout.setVisibility(View.GONE);
				topLayoutBarMiddleSearchLayoutCancel.setVisibility(View.GONE);
			}
		});

		topLayoutBarMiddleSearchInput.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				if (_charSeq.trim().equals("")) {
					_showAllUser();
					_TransitionManager(topLayoutBarMiddleSearchLayout, 100);
					topLayoutBarMiddleSearchLayoutCancel.setVisibility(View.GONE);
					_ImageColor(topLayoutBarMiddleSearchLayoutIc, 0xFF757575);
				} else {
					_search();
					topLayoutBarMiddleSearchLayoutCancel.setVisibility(View.VISIBLE);
					_TransitionManager(topLayoutBarMiddleSearchLayout, 100);
					_ImageColor(topLayoutBarMiddleSearchLayoutIc, 0xFF2962FF);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

			}

			@Override
			public void afterTextChanged(Editable _param1) {

			}
		});

		topLayoutBarMiddleSearchLayoutIc.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (topLayoutBarMiddleSearchInput.getText().toString().trim().equals("")) {
					_showAllUser();
				} else {
					_getSearchedUserReference();
				}
			}
		});

		SearchUserLayoutRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(RecyclerView recyclerView, int _scrollState) {
				super.onScrollStateChanged(recyclerView, _scrollState);

			}

			@Override
			public void onScrolled(RecyclerView recyclerView, int _offsetX, int _offsetY) {
				super.onScrolled(recyclerView, _offsetX, _offsetY);

			}
		});
	}

	private void initializeLogic() {
		topLayoutBarMiddleSearchLayout.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)0, Color.TRANSPARENT, 0xFFF5F5F5));
		SearchUserLayout.setVisibility(View.VISIBLE);
		topLayoutBarMiddleSearchLayoutCancel.setVisibility(View.GONE);
		_ImageColor(topLayoutBarMiddleSearchLayoutIc, 0xFF757575);
		_ImageColor(topLayoutBarMiddleSearchLayoutCancel, 0xFF757575);
		_ImageColor(bottom_home_ic, 0xFFBDBDBD);
		_ImageColor(bottom_search_ic, 0xFF000000);
		_ImageColor(bottom_videos_ic, 0xFFBDBDBD);
		_ImageColor(bottom_chats_ic, 0xFFBDBDBD);
		_ImageColor(bottom_profile_ic, 0xFFBDBDBD);
		SearchUserLayoutRecyclerView.setAdapter(new SearchUserLayoutRecyclerViewAdapter(searchedUsersList));
		SearchUserLayoutRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		topLayoutBarMiddleSearchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if(actionId== 3) {
					if (!topLayoutBarMiddleSearchInput.getText().toString().trim().equals("")) {
						_getSearchedUserReference();
					}
				}
				return false;
			}
		});
		_showAllUser();
	}


	@Override
	public void onBackPressed() {
		intent.setClass(getApplicationContext(), HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
		finish();
	}

	@Override
	public void onResume() {
		super.onResume();
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
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


	public void _getSearchedUserReference() {
		_performSearch(topLayoutBarMiddleSearchInput.getText().toString());
		SearchUserLayout.setVisibility(View.VISIBLE);
		topLayoutBarMiddleSearchLayoutCancel.setVisibility(View.VISIBLE);
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


	public void _search() {
		_performSearch(topLayoutBarMiddleSearchInput.getText().toString());
		SearchUserLayout.setVisibility(View.VISIBLE);
		topLayoutBarMiddleSearchLayoutCancel.setVisibility(View.VISIBLE);
	}


	public void _showAllUser() {
		_performSearch("");
	}

    private void _performSearch(String query) {
        dbService.searchUsers(query, new IDataListener() {
            @Override
            public void onDataReceived(IDataSnapshot dataSnapshot) {
                searchedUsersList.clear();
                if (dataSnapshot.exists() && dataSnapshot.hasChildren()) {
                    SearchUserLayoutNoUserFound.setVisibility(View.GONE);
                    SearchUserLayoutRecyclerView.setVisibility(View.VISIBLE);
                    for (IDataSnapshot userSnapshot : dataSnapshot.getChildren()) {
                        searchedUsersList.add((HashMap<String, Object>) userSnapshot.getValue());
                    }
                } else {
                    SearchUserLayoutNoUserFound.setVisibility(View.VISIBLE);
                    SearchUserLayoutRecyclerView.setVisibility(View.GONE);
                }
                SearchUserLayoutRecyclerView.getAdapter().notifyDataSetChanged();
            }

            @Override
            public void onCancelled(IDatabaseError databaseError) {
                // Handle error
                searchedUsersList.clear();
                SearchUserLayoutRecyclerView.getAdapter().notifyDataSetChanged();
                SearchUserLayoutNoUserFound.setVisibility(View.VISIBLE);
                SearchUserLayoutRecyclerView.setVisibility(View.GONE);
            }
        });
    }


	public void _TransitionManager(final View _view, final double _duration) {
		LinearLayout viewgroup =(LinearLayout) _view;

		android.transition.AutoTransition autoTransition = new android.transition.AutoTransition(); autoTransition.setDuration((long)_duration); android.transition.TransitionManager.beginDelayedTransition(viewgroup, autoTransition);
	}

	public class SearchUserLayoutRecyclerViewAdapter extends RecyclerView.Adapter<SearchUserLayoutRecyclerViewAdapter.ViewHolder> {

		ArrayList<HashMap<String, Object>> _data;

		public SearchUserLayoutRecyclerViewAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}

		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.synapse_users_list_cv, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}

		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;

			final androidx.cardview.widget.CardView cardview1 = _view.findViewById(R.id.cardview1);
			final LinearLayout body = _view.findViewById(R.id.body);
			final RelativeLayout profileCardRelative = _view.findViewById(R.id.profileCardRelative);
			final LinearLayout lin = _view.findViewById(R.id.lin);
			final androidx.cardview.widget.CardView profileCard = _view.findViewById(R.id.profileCard);
			final LinearLayout ProfileRelativeUp = _view.findViewById(R.id.ProfileRelativeUp);
			final ImageView profileAvatar = _view.findViewById(R.id.profileAvatar);
			final LinearLayout userStatusCircleBG = _view.findViewById(R.id.userStatusCircleBG);
			final LinearLayout userStatusCircleIN = _view.findViewById(R.id.userStatusCircleIN);
			final LinearLayout usr = _view.findViewById(R.id.usr);
			final TextView name = _view.findViewById(R.id.name);
			final TextView username = _view.findViewById(R.id.username);
			final ImageView genderBadge = _view.findViewById(R.id.genderBadge);
			final ImageView badge = _view.findViewById(R.id.badge);

			try{
				RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
				_view.setLayoutParams(_lp);
				profileCard.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)300, Color.TRANSPARENT));
				userStatusCircleBG.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)300, 0xFFFFFFFF));
				userStatusCircleIN.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)300, 0xFF2196F3));
				name.setText("@" + _data.get((int)_position).get("username").toString());
				if (_data.get((int)_position).get("banned").toString().equals("true")) {
					profileAvatar.setImageResource(R.drawable.banned_avatar);
				} else {
					if (_data.get((int)_position).get("avatar").toString().equals("null")) {
						profileAvatar.setImageResource(R.drawable.avatar);
					} else {
						Glide.with(getApplicationContext()).load(Uri.parse(_data.get((int)_position).get("avatar").toString())).into(profileAvatar);
					}
				}
				if (_data.get((int)_position).get("nickname").toString().equals("null")) {
					username.setText("@" + _data.get((int)_position).get("username").toString());
				} else {
					username.setText(_data.get((int)_position).get("nickname").toString());
				}
				if (_data.get((int)_position).get("gender").toString().equals("hidden")) {
					genderBadge.setVisibility(View.GONE);
				} else {
					if (_data.get((int)_position).get("gender").toString().equals("male")) {
						genderBadge.setImageResource(R.drawable.male_badge);
						genderBadge.setVisibility(View.VISIBLE);
					} else {
						if (_data.get((int)_position).get("gender").toString().equals("female")) {
							genderBadge.setImageResource(R.drawable.female_badge);
							genderBadge.setVisibility(View.VISIBLE);
						}
					}
				}
				if (_data.get((int)_position).get("account_type").toString().equals("admin")) {
					badge.setImageResource(R.drawable.admin_badge);
					badge.setVisibility(View.VISIBLE);
				} else {
					if (_data.get((int)_position).get("account_type").toString().equals("moderator")) {
						badge.setImageResource(R.drawable.moderator_badge);
						badge.setVisibility(View.VISIBLE);
					} else {
						if (_data.get((int)_position).get("account_type").toString().equals("support")) {
							badge.setImageResource(R.drawable.support_badge);
							badge.setVisibility(View.VISIBLE);
						} else {
							if (_data.get((int)_position).get("account_type").toString().equals("user")) {
								if (_data.get((int)_position).get("account_premium").toString().equals("true")) {
									badge.setImageResource(R.drawable.premium_badge);
									badge.setVisibility(View.VISIBLE);
								} else {
									if (_data.get((int)_position).get("verify").toString().equals("true")) {
										badge.setImageResource(R.drawable.verified_badge);
										badge.setVisibility(View.VISIBLE);
									} else {
										badge.setVisibility(View.GONE);
									}
								}
							}
						}
					}
				}
				if (_data.get((int)_position).get("status").toString().equals("online")) {
					userStatusCircleBG.setVisibility(View.VISIBLE);
				} else {
					userStatusCircleBG.setVisibility(View.GONE);
				}
				if (_position == 0) {
					_setMargin(body, 18, 18, 10, 10);
				} else {
					_setMargin(body, 18, 18, 0, 10);
				}
				body.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View _view) {
						intent.setClass(getApplicationContext(), ProfileActivity.class);
						intent.putExtra("uid", _data.get((int)_position).get("uid").toString());
						intent.putExtra("origin", "SearchActivity");
						startActivity(intent);
						finish();
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