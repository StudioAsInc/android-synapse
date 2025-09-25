package com.synapse.social.studioasinc;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.GenericTypeIndicator;
import com.synapse.social.studioasinc.backend.AuthenticationService;
import com.synapse.social.studioasinc.backend.DatabaseService;
import com.synapse.social.studioasinc.backend.QueryService;
import java.util.ArrayList;
import java.util.HashMap;

public class LineVideoPlayerActivity extends AppCompatActivity {

    public LineVideosRecyclerViewAdapter mLineVideosRecyclerViewAdapter;
    private ArrayList<HashMap<String, Object>> lineVideosListMap = new ArrayList<>();

    private LinearLayout loadedBody;
    private LinearLayout noInternetBody;
    private RecyclerView videosRecyclerView;
    private SwipeRefreshLayout middleRelativeTopSwipe;
    private AuthenticationService authService;
    private QueryService queryService;
    private Intent intent = new Intent();


    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.activity_line_video_player);
        initialize(_savedInstanceState);
        initializeLogic();
    }

    private void initialize(Bundle _savedInstanceState) {
        authService = new AuthenticationService();
        DatabaseService dbService = new DatabaseService();
        queryService = new QueryService(dbService);

        loadedBody = findViewById(R.id.loadedBody);
        noInternetBody = findViewById(R.id.noInternetBody);
        videosRecyclerView = findViewById(R.id.videosRecyclerView);
        middleRelativeTopSwipe = findViewById(R.id.middleRelativeTopSwipe);
        TextView noInternetBodyRetry = findViewById(R.id.noInternetBodyRetry);
        ImageView middleRelativeBottomTopBack = findViewById(R.id.middleRelativeBottomTopBack);
        LinearLayout bottom_home = findViewById(R.id.bottom_home);
        LinearLayout bottom_search = findViewById(R.id.bottom_search);
        LinearLayout bottom_profile = findViewById(R.id.bottom_profile);

        middleRelativeTopSwipe.setOnRefreshListener(this::_getReference);
        noInternetBodyRetry.setOnClickListener(v -> _getReference());
        middleRelativeBottomTopBack.setOnClickListener(v -> onBackPressed());

        bottom_home.setOnClickListener(v -> {
            intent.setClass(getApplicationContext(), HomeActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
            finish();
        });

        bottom_search.setOnClickListener(v -> {
            intent.setClass(getApplicationContext(), SearchActivity.class);
            startActivity(intent);
        });

        bottom_profile.setOnClickListener(v -> {
            if (authService.getCurrentUser() != null) {
                intent.setClass(getApplicationContext(), ProfileActivity.class);
                intent.putExtra("uid", authService.getCurrentUser().getUid());
                startActivity(intent);
            }
        });
    }

    private void initializeLogic() {
        _stateColor(0xFF000000, 0xFF000000);
        TextView noInternetBodySubtitle = findViewById(R.id.noInternetBodySubtitle);
        noInternetBodySubtitle.setText(getResources().getString(R.string.reasons_may_be).concat("\n\n".concat(getResources().getString(R.string.err_no_internet).concat("\n".concat(getResources().getString(R.string.err_app_maintenance).concat("\n".concat(getResources().getString(R.string.err_problem_on_our_side))))))));
        loadedBody.setVisibility(View.GONE);
        noInternetBody.setVisibility(View.GONE);
        ImageView middleRelativeBottomTopBack = findViewById(R.id.middleRelativeBottomTopBack);
        _ImageColor(middleRelativeBottomTopBack, 0xFFFFFFFF);
        _ImageColor(findViewById(R.id.bottom_home_ic), 0xFF424242);
        _ImageColor(findViewById(R.id.bottom_search_ic), 0xFF424242);
        _ImageColor(findViewById(R.id.bottom_videos_ic), 0xFFFFFFFF);
        _ImageColor(findViewById(R.id.bottom_chats_ic), 0xFF424242);
        _ImageColor(findViewById(R.id.bottom_profile_ic), 0xFF424242);
        _viewGraphics(findViewById(R.id.noInternetBodyRetry), 0xFF212121, 0xFF424242, 24, 3, 0xFF424242);
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

    public void _stateColor(final int _statusColor, final int _navigationColor) {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        getWindow().setStatusBarColor(_statusColor);
        getWindow().setNavigationBarColor(_navigationColor);
    }


    public void _ImageColor(final ImageView _image, final int _color) {
        _image.setColorFilter(_color, PorterDuff.Mode.SRC_ATOP);
    }


    public void _viewGraphics(final View _view, final int _onFocus, final int _onRipple, final double _radius, final double _stroke, final int _strokeColor) {
        android.graphics.drawable.GradientDrawable GG = new android.graphics.drawable.GradientDrawable();
        GG.setColor(_onFocus);
        GG.setCornerRadius((float) _radius);
        GG.setStroke((int) _stroke, _strokeColor);
        android.graphics.drawable.RippleDrawable RE = new android.graphics.drawable.RippleDrawable(new android.content.res.ColorStateList(new int[][]{new int[]{}}, new int[]{_onRipple}), GG, null);
        _view.setBackground(RE);
    }


    public void _getReference() {
        loadedBody.setVisibility(View.VISIBLE);
        noInternetBody.setVisibility(View.GONE);

        queryService.fetchWithLimit("skyline/line-posts", "post_type", 50, "LINE_VIDEO", new DatabaseService.DataListener() {
            @Override
            public void onDataChange(DataSnapshot _dataSnapshot) {
                lineVideosListMap.clear();
                try {
                    GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
                    for (DataSnapshot _data : _dataSnapshot.getChildren()) {
                        HashMap<String, Object> _map = _data.getValue(_ind);
                        lineVideosListMap.add(_map);
                    }
                    mLineVideosRecyclerViewAdapter = new LineVideosRecyclerViewAdapter(getApplicationContext(), getSupportFragmentManager(), lineVideosListMap);
                    videosRecyclerView.setAdapter(mLineVideosRecyclerViewAdapter);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                middleRelativeTopSwipe.setRefreshing(false);
            }

            @Override
            public void onCancelled(DatabaseError _databaseError) {
                loadedBody.setVisibility(View.GONE);
                noInternetBody.setVisibility(View.VISIBLE);
                middleRelativeTopSwipe.setRefreshing(false);
            }
        });
    }
}