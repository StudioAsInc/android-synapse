package com.synapse.social.studioasinc;

import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.carousel.CarouselLayoutManager;
import com.google.android.material.carousel.CarouselSnapHelper;
import com.google.android.material.carousel.FullScreenCarouselStrategy;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.synapse.social.studioasinc.models.Story;
import com.synapse.social.studioasinc.models.User;

import java.util.ArrayList;

import de.hdodenhof.circleimageview.CircleImageView;

public class StoryViewerActivity extends AppCompatActivity {

    private RecyclerView storyCarouselView;
    private ArrayList<Story> stories;
    private int currentPosition;
    private CountDownTimer countDownTimer;
    private StoryCarouselAdapter adapter;
    private CarouselSnapHelper snapHelper;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_story_viewer);

        storyCarouselView = findViewById(R.id.story_carousel_view);

        stories = (ArrayList<Story>) getIntent().getSerializableExtra("stories");
        currentPosition = getIntent().getIntExtra("position", 0);

        adapter = new StoryCarouselAdapter(stories);
        storyCarouselView.setLayoutManager(new CarouselLayoutManager(new FullScreenCarouselStrategy()));
        snapHelper = new CarouselSnapHelper();
        snapHelper.attachToRecyclerView(storyCarouselView);

        storyCarouselView.setAdapter(adapter);
        storyCarouselView.scrollToPosition(currentPosition);

        storyCarouselView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    View snapView = snapHelper.findSnapView(storyCarouselView.getLayoutManager());
                    if (snapView != null) {
                        currentPosition = storyCarouselView.getChildAdapterPosition(snapView);
                        startStoryProgress();
                    }
                }
            }
        });

        startStoryProgress();
    }

    private void startStoryProgress() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }

        RecyclerView.ViewHolder currentViewHolder = storyCarouselView.findViewHolderForAdapterPosition(currentPosition);
        if (currentViewHolder != null) {
            ((StoryCarouselAdapter.StoryViewHolder) currentViewHolder).storyProgressBar.setProgress(0);
        }


        countDownTimer = new CountDownTimer(5000, 50) {
            @Override
            public void onTick(long millisUntilFinished) {
                RecyclerView.ViewHolder currentViewHolderOnTick = storyCarouselView.findViewHolderForAdapterPosition(currentPosition);
                if (currentViewHolderOnTick != null) {
                    int progress = (int) (100 * (5000 - millisUntilFinished) / 5000);
                    ((StoryCarouselAdapter.StoryViewHolder) currentViewHolderOnTick).storyProgressBar.setProgress(progress);
                }
            }

            @Override
            public void onFinish() {
                RecyclerView.ViewHolder currentViewHolderOnFinish = storyCarouselView.findViewHolderForAdapterPosition(currentPosition);
                if (currentViewHolderOnFinish != null) {
                    ((StoryCarouselAdapter.StoryViewHolder) currentViewHolderOnFinish).storyProgressBar.setProgress(100);
                }
                moveToNextStory();
            }
        }.start();
    }

    private void moveToNextStory() {
        if (currentPosition < stories.size() - 1) {
            currentPosition++;
            storyCarouselView.scrollToPosition(currentPosition);
        } else {
            finish();
        }
    }

    private void moveToPreviousStory() {
        if (currentPosition > 0) {
            currentPosition--;
            storyCarouselView.scrollToPosition(currentPosition);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
    }

    private class StoryCarouselAdapter extends RecyclerView.Adapter<StoryCarouselAdapter.StoryViewHolder> {

        private final ArrayList<Story> stories;

        public StoryCarouselAdapter(ArrayList<Story> stories) {
            this.stories = stories;
        }

        @NonNull
        @Override
        public StoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.story_item, parent, false);
            return new StoryViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull StoryViewHolder holder, int position) {
            Story story = stories.get(position);
            if (story.getUrl() != null && !story.getUrl().isEmpty()) {
                Glide.with(holder.itemView.getContext())
                        .load(Uri.parse(story.getUrl()))
                        .into(holder.storyImageView);
            } else {
                // Optionally, hide the view or set a placeholder
                holder.storyImageView.setVisibility(View.GONE);
            }

            fetchUserInfo(story.getUid(), holder);

            holder.itemView.setOnTouchListener((v, event) -> {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    if (event.getX() < v.getWidth() / 2) {
                        moveToPreviousStory();
                    } else {
                        moveToNextStory();
                    }
                }
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return stories.size();
        }

        private void fetchUserInfo(String uid, StoryViewHolder holder) {
            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("Users").child(uid);
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        User user = snapshot.getValue(User.class);
                        if (user != null) {
                            holder.storyUsername.setText(user.getUsername());
                            Glide.with(holder.itemView.getContext())
                                    .load(user.getImageurl())
                                    .into(holder.storyProfileImage);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // Handle error
                }
            });
        }


        class StoryViewHolder extends RecyclerView.ViewHolder {
            ImageView storyImageView;
            CircleImageView storyProfileImage;
            TextView storyUsername;
            ProgressBar storyProgressBar;


            public StoryViewHolder(@NonNull View itemView) {
                super(itemView);
                storyImageView = itemView.findViewById(R.id.story_image_view);
                storyProfileImage = itemView.findViewById(R.id.story_profile_image);
                storyUsername = itemView.findViewById(R.id.story_username);
                storyProgressBar = itemView.findViewById(R.id.story_progress_bar);
            }
        }
    }
}
