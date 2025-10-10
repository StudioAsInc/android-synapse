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
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
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
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener;
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

// TODO: Migrate to Supabase
// This activity is responsible for playing line videos.
// The following needs to be done:
// 1. Replace all Firebase database calls with calls to the `DatabaseService` interface.
// 2. Replace all Firebase auth calls with calls to the `AuthenticationService` interface.
public class LineVideoPlayerActivity extends AppCompatActivity {

	public LineVideosRecyclerViewAdapter mLineVideosRecyclerViewAdapter;

	private ArrayList<HashMap<String, Object>> lineVideosListMap = new ArrayList<>();

	private LinearLayout body;
	private RelativeLayout middleRelative;
	private LinearLayout bottomBar;
	private LinearLayout middleRelativeTop;
	private LinearLayout middleRelativeBottom;
	private SwipeRefreshLayout middleRelativeTopSwipe;
	private LinearLayout middleRelativeTopSwipeBody;
	private LinearLayout loadedBody;
	private LinearLayout noInternetBody;
	private RecyclerView videosRecyclerView;
	private ImageView noInternetBodyIc;
	private TextView noInternetBodyTitle;
	private TextView noInternetBodySubtitle;
	private TextView noInternetBodyRetry;
	private LinearLayout middleRelativeBottomTop;
	private ImageView middleRelativeBottomTopBack;
	private ImageView middleRelativeBottomTopCoverIc;
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

	private RequestNetwork request;
	private RequestNetwork.RequestListener _request_request_listener;
	private Intent intent = new Intent();

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_line_video_player);
		initialize(_savedInstanceState);
		// TODO: Initialize Supabase client in SynapseApp.java instead of here.
		// FirebaseApp.initializeApp(this);
		initializeLogic();
	}

	private void initialize(Bundle _savedInstanceState) {
		body = findViewById(R.id.body);
		middleRelative = findViewById(R.id.middleRelative);
		bottomBar = findViewById(R.id.bottomBar);
		middleRelativeTop = findViewById(R.id.middleRelativeTop);
		middleRelativeBottom = findViewById(R.id.middleRelativeBottom);
		middleRelativeTopSwipe = findViewById(R.id.middleRelativeTopSwipe);
		middleRelativeTopSwipeBody = findViewById(R.id.middleRelativeTopSwipeBody);
		loadedBody = findViewById(R.id.loadedBody);
		noInternetBody = findViewById(R.id.noInternetBody);
		videosRecyclerView = findViewById(R.id.videosRecyclerView);
		noInternetBodyIc = findViewById(R.id.noInternetBodyIc);
		noInternetBodyTitle = findViewById(R.id.noInternetBodyTitle);
		noInternetBodySubtitle = findViewById(R.id.noInternetBodySubtitle);
		noInternetBodyRetry = findViewById(R.id.noInternetBodyRetry);
		middleRelativeBottomTop = findViewById(R.id.middleRelativeBottomTop);
		middleRelativeBottomTopBack = findViewById(R.id.middleRelativeBottomTopBack);
		middleRelativeBottomTopCoverIc = findViewById(R.id.middleRelativeBottomTopCoverIc);
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
		request = new RequestNetwork(this);

		middleRelativeTopSwipe.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
			@Override
			public void onRefresh() {
				_getReference();
			}
		});

		noInternetBodyRetry.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				_getReference();
			}
		});

		middleRelativeBottomTopBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				onBackPressed();
			}
		});

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
				intent.setClass(getApplicationContext(), SearchActivity.class);
				startActivity(intent);
			}
		});


		bottom_profile.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(getApplicationContext(), ProfileActivity.class);
				// TODO: Replace with Supabase Auth
				// intent.putExtra("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
				startActivity(intent);
			}
		});

		_request_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _tag = _param1;
				final String _response = _param2;
				final HashMap<String, Object> _responseHeaders = _param3;
				loadedBody.setVisibility(View.VISIBLE);
				noInternetBody.setVisibility(View.GONE);
				// TODO: Get line videos from Supabase
				middleRelativeTopSwipe.setRefreshing(false);
			}

			@Override
			public void onErrorResponse(String _param1, String _param2) {
				final String _tag = _param1;
				final String _message = _param2;
				loadedBody.setVisibility(View.GONE);
				noInternetBody.setVisibility(View.VISIBLE);
				middleRelativeTopSwipe.setRefreshing(false);
			}
		};
	}

	private void initializeLogic() {
		_stateColor(0xFF000000, 0xFF000000);
		noInternetBodySubtitle.setText(getResources().getString(R.string.reasons_may_be).concat("\n\n".concat(getResources().getString(R.string.err_no_internet).concat("\n".concat(getResources().getString(R.string.err_app_maintenance).concat("\n".concat(getResources().getString(R.string.err_problem_on_our_side))))))));
		loadedBody.setVisibility(View.GONE);
		noInternetBody.setVisibility(View.GONE);
		_ImageColor(middleRelativeBottomTopBack, 0xFFFFFFFF);
		_ImageColor(bottom_home_ic, 0xFF424242);
		_ImageColor(bottom_search_ic, 0xFF424242);
		_ImageColor(bottom_videos_ic, 0xFFFFFFFF);
		_ImageColor(bottom_chats_ic, 0xFF424242);
		_ImageColor(bottom_profile_ic, 0xFF424242);
		_viewGraphics(noInternetBodyRetry, 0xFF212121, 0xFF424242, 24, 3, 0xFF424242);
		videosRecyclerView.setLayoutManager(new LinearLayoutManager(this));
		PagerSnapHelper lineVideoViewSnapHelper = new PagerSnapHelper();

		lineVideoViewSnapHelper.attachToRecyclerView(videosRecyclerView);
		_getReference();
	}


	@Override
	public void onBackPressed() {
		intent.setClass(getApplicationContext(), HomeActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
		startActivity(intent);
		finish();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();

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


	public void _getReference() {
		request.startRequestNetwork(RequestNetworkController.POST, "https://google.com", "google", _request_request_listener);
	}

}