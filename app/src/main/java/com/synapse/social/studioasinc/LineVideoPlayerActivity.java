package com.synapse.social.studioasinc;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
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

import io.github.jan_tennert.supabase.SupabaseClient; // Supabase import
import io.github.jan_tennert.supabase.createSupabaseClient;
import io.github.jan_tennert.supabase.postgrest.Postgrest;
import io.github.jan_tennert.supabase.gotrue.Auth;

public class LineVideoPlayerActivity extends AppCompatActivity {

    // Supabase
    private SupabaseClient supabase; // Supabase client instance
    // TODO: Add Supabase database references as needed (e.g., Postgrest client)

    public String videoUri = "";
    public HashMap<String, Object> video = new HashMap<>();
    public String currentPostUid = "";
    public boolean isPlaying = false;
    public double current_pos = 0;
    public String current_uid = "";
    public String video_status = "";
    public HashMap<String, Object> likedMap = new HashMap<>();
    public String currentUserName = "";
    public String currentUserProfile = "";
    public String currentUserId = "";
    public String currentUserEmail = "";
    public String currentPostCommentId = "";
    public String replyCommentId = "";
    public HashMap<String, Object> likeCommentMap = new HashMap<>();
    public String likeCommentId = "";

    private ImageView profile;
    private TextView username;
    private ImageView like;
    private TextView likes;
    private TextView comments;
    private ImageView share;
    private VideoView videoview;
    private ImageView pause;
    private ImageView volume;
    private ImageView back;
    private EditText comment_edit;
    private ImageView comment_send;
    private RelativeLayout comment_sheet;
    private RecyclerView comment_list;
    private ImageView comment_close;
    private ImageView options;
    private LinearLayout loadedBody;
    private SwipeRefreshLayout comment_swipe;
    private ProgressBar loading;
    private SeekBar seekbar;
    private TextView play_time;
    private TextView video_duration;
    private ImageView video_fullscreen;
    private TextView post_caption;

    private LineVideoCommentAdapter lineVideoCommentAdapter;
    private ArrayList<HashMap<String, Object>> commentsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        setContentView(R.layout.line_video_player);
        initialize(R.layout.line_video_player);
        initializeSupabase(); // Initialize Supabase
        initializeLogic();
    }

    private void initialize(final int _contentLayoutRes) {
        profile = findViewById(R.id.profile);
        username = findViewById(R.id.username);
        like = findViewById(R.id.like);
        likes = findViewById(R.id.likes);
        comments = findViewById(R.id.comments);
        share = findViewById(R.id.share);
        videoview = findViewById(R.id.videoview);
        pause = findViewById(R.id.pause);
        volume = findViewById(R.id.volume);
        back = findViewById(R.id.back);
        comment_edit = findViewById(R.id.comment_edit);
        comment_send = findViewById(R.id.comment_send);
        comment_sheet = findViewById(R.id.comment_sheet);
        comment_list = findViewById(R.id.comment_list);
        comment_close = findViewById(R.id.comment_close);
        options = findViewById(R.id.options);
        loadedBody = findViewById(R.id.loadedBody);
        comment_swipe = findViewById(R.id.comment_swipe);
        loading = findViewById(R.id.loading);
        seekbar = findViewById(R.id.seekbar);
        play_time = findViewById(R.id.play_time);
        video_duration = findViewById(R.id.video_duration);
        video_fullscreen = findViewById(R.id.video_fullscreen);
        post_caption = findViewById(R.id.post_caption);
    }

    private void initializeSupabase() {
        // Initialize Supabase client
        supabase = createSupabaseClient(
                "YOUR_SUPABASE_URL", // Replace with your Supabase URL
                "YOUR_SUPABASE_ANON_KEY", // Replace with your Supabase anon key
                builder -> {
                    builder.install(new Auth());
                    builder.install(new Postgrest());
                    return null;
                }
        );

        // TODO: Get current user from Supabase Auth
        // User currentUser = supabase.auth.currentUser();
        // if (currentUser != null) {
        //     currentUserId = currentUser.id();
        //     currentUserEmail = currentUser.email();
        //     fetchCurrentUserData(); // Call a Supabase-specific method to fetch user data
        // }
    }

    private void initializeLogic() {
        // TODO: Get current Supabase user and their data.
        // For example:
        // val user = supabase.auth.currentUser
        // if (user != null) {
        //     currentUserId = user.id
        //     fetchCurrentUserData()
        // }

        videoUri = getIntent().getStringExtra("video_uri");
        currentPostUid = getIntent().getStringExtra("post_uid");

        setupVideoView();
        setupClickListeners();
        // setupDatabaseListeners(); // This will be replaced with Supabase calls
        // TODO: Implement Supabase data fetching for post details, likes, and comments
        // Example:
        // fetchPostDetails(currentPostUid);
        // fetchLikes(currentPostUid);
        // fetchComments(currentPostUid);
    }

    private void setupVideoView() {
        videoview.setVideoURI(Uri.parse(videoUri));
        videoview.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            videoview.start();
            isPlaying = true;
            pause.setImageResource(R.drawable.ic_pause_black);
            loading.setVisibility(View.GONE);
            video_duration.setText(formatTime(mp.getDuration()));
            updateSeekBar();
        });

        videoview.setOnErrorListener((mp, what, extra) -> {
            Toast.makeText(this, "Video playback error: " + what + ", " + extra, Toast.LENGTH_LONG).show();
            return false;
        });

        videoview.setOnCompletionListener(mp -> {
            videoview.start();
        });

        videoview.setOnClickListener(v -> togglePlayPause());
    }

    private void togglePlayPause() {
        if (isPlaying) {
            videoview.pause();
            pause.setImageResource(R.drawable.ic_play_arrow_black);
        } else {
            videoview.start();
            pause.setImageResource(R.drawable.ic_pause_black);
        }
        isPlaying = !isPlaying;
    }

    private void setupClickListeners() {
        pause.setOnClickListener(v -> togglePlayPause());
        volume.setOnClickListener(v -> toggleVolume());
        back.setOnClickListener(v -> finish());
        like.setOnClickListener(v -> handleLikeClick());
        comments.setOnClickListener(v -> showCommentSheet());
        share.setOnClickListener(v -> handleShareClick());
        comment_send.setOnClickListener(v -> handleCommentSend());
        comment_close.setOnClickListener(v -> hideCommentSheet());
        options.setOnClickListener(v -> showOptions());
        video_fullscreen.setOnClickListener(v -> toggleFullScreen());

        comment_edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().isEmpty()) {
                    comment_send.setColorFilter(Color.parseColor("#BDBDBD"));
                } else {
                    comment_send.setColorFilter(Color.parseColor("#FFC107"));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    videoview.seekTo(progress);
                    play_time.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        comment_swipe.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                // TODO: Refresh comments from Supabase
                // fetchComments(currentPostUid);
                comment_swipe.setRefreshing(false);
            }
        });
    }

    private void toggleVolume() {
        if (videoview.getVolume() == 0) {
            videoview.setVolume(1, 1); // Set to full volume
            volume.setImageResource(R.drawable.ic_volume_up_black);
        } else {
            videoview.setVolume(0, 0); // Mute
            volume.setImageResource(R.drawable.ic_volume_off_black);
        }
    }

    private void updateSeekBar() {
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if (videoview.isPlaying()) {
                    int currentPosition = videoview.getCurrentPosition();
                    seekbar.setProgress(currentPosition);
                    play_time.setText(formatTime(currentPosition));
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(runnable, 0);
    }

    private String formatTime(int milliseconds) {
        int minutes = (milliseconds / 1000) / 60;
        int seconds = (milliseconds / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void toggleFullScreen() {
        // TODO: Implement fullscreen toggle for video player
        Toast.makeText(this, "Fullscreen not implemented yet", Toast.LENGTH_SHORT).show();
    }

    private void handleLikeClick() {
        // TODO: Implement like functionality with Supabase
        Toast.makeText(this, "Like functionality not implemented yet", Toast.LENGTH_SHORT).show();
    }

    private void showCommentSheet() {
        comment_sheet.setVisibility(View.VISIBLE);
        ObjectAnimator.ofFloat(comment_sheet, "translationY", comment_sheet.getHeight(), 0).setDuration(300).start();
        // TODO: Fetch comments from Supabase
    }

    private void hideCommentSheet() {
        ObjectAnimator.ofFloat(comment_sheet, "translationY", 0, comment_sheet.getHeight()).setDuration(300).start();
        new Handler(Looper.getMainLooper()).postDelayed(() -> comment_sheet.setVisibility(View.GONE), 300);
    }

    private void handleCommentSend() {
        String commentText = comment_edit.getText().toString().trim();
        if (!commentText.isEmpty()) {
            // TODO: Send comment to Supabase
            Toast.makeText(this, "Comment sent: " + commentText, Toast.LENGTH_SHORT).show();
            comment_edit.setText("");
        }
    }

    private void handleShareClick() {
        // TODO: Implement share functionality
        Toast.makeText(this, "Share functionality not implemented yet", Toast.LENGTH_SHORT).show();
    }

    private void showOptions() {
        // TODO: Implement options menu
        Toast.makeText(this, "Options not implemented yet", Toast.LENGTH_SHORT).show();
    }

    // TODO: Implement Supabase methods for data fetching and manipulation
    // Example Supabase methods:
    /*
    private void fetchPostDetails(String postId) {
        supabase.from("posts").select("*").eq("id", postId).execute()
            .thenAccept(response -> {
                // Parse response and update UI
            })
            .exceptionally(e -> {
                Log.e("Supabase", "Error fetching post details: " + e.getMessage());
                return null;
            });
    }

    private void fetchLikes(String postId) {
        supabase.from("likes").select("*").eq("post_id", postId).execute()
            .thenAccept(response -> {
                // Parse response and update likes count
            })
            .exceptionally(e -> {
                Log.e("Supabase", "Error fetching likes: " + e.getMessage());
                return null;
            });
    }

    private void fetchComments(String postId) {
        supabase.from("comments").select("*").eq("post_id", postId).order("created_at", true).execute()
            .thenAccept(response -> {
                // Parse response and update commentsList, then notify adapter
            })
            .exceptionally(e -> {
                Log.e("Supabase", "Error fetching comments: " + e.getMessage());
                return null;
            });
    }

    private void submitComment(String postId, String userId, String commentText) {
        Map<String, Object> newComment = new HashMap<>();
        newComment.put("post_id", postId);
        newComment.put("user_id", userId);
        newComment.put("comment_text", commentText);
        supabase.from("comments").insert(newComment).execute()
            .thenAccept(response -> {
                // Handle success, refresh comments
            })
            .exceptionally(e -> {
                Log.e("Supabase", "Error submitting comment: " + e.getMessage());
                return null;
            });
    }
    */
}
