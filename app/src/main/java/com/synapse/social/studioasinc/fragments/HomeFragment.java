package com.synapse.social.studioasinc.fragments;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
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
import android.widget.*;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.*;
import androidx.cardview.widget.CardView;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.*;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.*;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bumptech.glide.*;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.synapse.social.studioasinc.backend.IAuthenticationService;
import com.synapse.social.studioasinc.backend.IDatabaseService;
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService;
import com.synapse.social.studioasinc.backend.SupabaseDatabaseService;
import com.synapse.social.studioasinc.backend.interfaces.IDataListener;
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError;
import com.synapse.social.studioasinc.backend.interfaces.IQuery;
import com.synapse.social.studioasinc.util.NotificationUtils;
import io.github.jan.supabase.postgrest.query.PostgrestResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import kotlinx.coroutines.BuildersKt;
import kotlinx.coroutines.CoroutineStart;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.GlobalScope;
import kotlinx.coroutines.withContext;
import com.synapse.social.studioasinc.CreateLineVideoActivity;
import com.synapse.social.studioasinc.CreatePostActivity;
import com.synapse.social.studioasinc.PostCommentsBottomSheetDialog;
import com.synapse.social.studioasinc.PostMoreBottomSheetDialog;
import com.synapse.social.studioasinc.ProfileActivity;
import com.synapse.social.studioasinc.R;
import com.synapse.social.studioasinc.SynapseApp;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class HomeFragment extends Fragment {

    private static final int SHIMMER_ITEM_COUNT = 5;
    private IAuthenticationService authService;
    private IDatabaseService dbService;

    private HashMap<String, Object> createPostMap = new HashMap<>();
    private HashMap<String, Object> postLikeCountCache = new HashMap<>();
    private HashMap<String, Object> UserInfoCacheMap = new HashMap<>();
    private HashMap<String, Object> postFavoriteCountCache = new HashMap<>();

    private ArrayList<HashMap<String, Object>> storiesList = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> PostsList = new ArrayList<>();

    private LinearLayout loadingBody;
    private SwipeRefreshLayout swipeLayout;
    private RecyclerView PublicPostsList;
	private ProgressBar loading_bar;
    private LinearLayout shimmer_container;

    private Intent intent = new Intent();
    private Vibrator vbr;
    private Calendar cc = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authService = new SupabaseAuthenticationService(getContext());
        dbService = new SupabaseDatabaseService();

        initialize(view);
        initializeLogic();
    }

    private void initialize(View view) {
        loadingBody = view.findViewById(R.id.loadingBody);
        swipeLayout = view.findViewById(R.id.swipeLayout);
        PublicPostsList = view.findViewById(R.id.PublicPostsList);
        loading_bar = view.findViewById(R.id.loading_bar);
        shimmer_container = view.findViewById(R.id.shimmer_container);

        vbr = (Vibrator) getActivity().getSystemService(Context.VIBRATOR_SERVICE);

        swipeLayout.setOnRefreshListener(() -> {
            _loadPosts();
        });
    }

    private void initializeLogic() {
        _loadPosts();


        PublicPostsList.setLayoutManager(new LinearLayoutManager(getContext()));

        HeaderAdapter headerAdapter = new HeaderAdapter();
        PublicPostsListAdapter postsAdapter = new PublicPostsListAdapter(PostsList);
        ConcatAdapter concatAdapter = new ConcatAdapter(headerAdapter, postsAdapter);
        PublicPostsList.setAdapter(concatAdapter);
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

    private void _loadStories(final RecyclerView storiesView, final ArrayList<HashMap<String, Object>> storiesList) {
        dbService.getReference("stories").orderByChild("publish_date").getData(new IDataListener() {
            @Override
            public void onDataChange(IDataSnapshot dataSnapshot) {
                if (!isAdded()) {
                    return;
                }
                storiesList.clear();
                HashMap<String, Object> myStoryPlaceholder = new HashMap<>();
                myStoryPlaceholder.put("uid", authService.getCurrentUserId());
                storiesList.add(myStoryPlaceholder);

                if (dataSnapshot.exists()) {
                    for (IDataSnapshot storySnap : dataSnapshot.getChildren()) {
                        HashMap<String, Object> storyMap = storySnap.getValue(HashMap.class);
                        if (storyMap != null) {
                            if (!storyMap.containsKey("uid") || !storyMap.get("uid").equals(authService.getCurrentUserId())) {
                                storiesList.add(storyMap);
                            }
                        }
                    }
                }
                if (storiesView != null && storiesView.getAdapter() != null) {
                    storiesView.getAdapter().notifyDataSetChanged();
                } else if (storiesView != null) {
                    storiesView.setAdapter(new StoriesViewAdapter(storiesList));
                }
            }

            @Override
            public void onCancelled(IDatabaseError databaseError) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getContext(), "Error loading stories: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                if (storiesView != null && storiesView.getAdapter() != null) {
                    storiesView.getAdapter().notifyDataSetChanged();
                } else if (storiesView != null) {
                    storiesView.setAdapter(new StoriesViewAdapter(storiesList));
                }
            }
        });
    }

    public void _loadPosts() {
        _showShimmer();
        swipeLayout.setRefreshing(true);
        IQuery query = dbService.getReference("posts").orderByChild("publish_date");
        String notFoundMessage = "There are no public posts available at the moment.";
        _fetchAndDisplayPosts(query, notFoundMessage);
    }

    private void _fetchAndDisplayPosts(IQuery query, final String notFoundMessage) {
        if (query == null) {
            _finalizePostDisplay(notFoundMessage, false);
            return;
        }

        dbService.getData(query, new IDataListener() {
            @Override
            public void onDataChange(IDataSnapshot dataSnapshot) {
                if (!isAdded()) {
                    return;
                }
                PostsList.clear();
                if (dataSnapshot.exists()) {
                    for (IDataSnapshot snapshot : dataSnapshot.getChildren()) {
                        HashMap<String, Object> map = snapshot.getValue(HashMap.class);
                        if (map != null) {
                            PostsList.add(map);
                        }
                    }
                    _finalizePostDisplay(notFoundMessage, true);
                } else {
                    _finalizePostDisplay(notFoundMessage, false);
                }
            }

            @Override
            public void onCancelled(IDatabaseError databaseError) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getContext(), "Failed to fetch latest posts, showing cached data. Error: " + databaseError.getMessage(), Toast.LENGTH_LONG).show();
                _finalizePostDisplay(notFoundMessage, false);
            }
        });
    }

    private void _showShimmer() {
        if (isAdded() && shimmer_container != null) {
            shimmer_container.removeAllViews();
            shimmer_container.setVisibility(View.VISIBLE);
            LayoutInflater inflater = LayoutInflater.from(getContext());
            for (int i = 0; i < SHIMMER_ITEM_COUNT; i++) {
                View shimmerView = inflater.inflate(R.layout.post_placeholder_layout, shimmer_container, false);
                shimmer_container.addView(shimmerView);
            }
        }
    }

    private void _hideShimmer() {
        if (shimmer_container != null) {
            shimmer_container.setVisibility(View.GONE);
        }
    }

    private void _finalizePostDisplay(String notFoundMessage, boolean sortAndNotify) {
        if (sortAndNotify) {
            Collections.sort(PostsList, (o1, o2) -> {
                long date1 = Long.parseLong(o1.get("publish_date").toString());
                long date2 = Long.parseLong(o2.get("publish_date").toString());
                return Long.compare(date2, date1);
            });
        }

        if (PublicPostsList.getAdapter() instanceof ConcatAdapter) {
            ConcatAdapter concatAdapter = (ConcatAdapter) PublicPostsList.getAdapter();
            for (RecyclerView.Adapter adapter : concatAdapter.getAdapters()) {
                if (adapter instanceof PublicPostsListAdapter) {
                    adapter.notifyDataSetChanged();
                }
            }
        } else if (PublicPostsList.getAdapter() instanceof PublicPostsListAdapter) {
             ((PublicPostsListAdapter)PublicPostsList.getAdapter()).notifyDataSetChanged();
        } else {
            HeaderAdapter headerAdapter = new HeaderAdapter();
            PublicPostsListAdapter postsAdapter = new PublicPostsListAdapter(PostsList);
            ConcatAdapter concatAdapter = new ConcatAdapter(headerAdapter, postsAdapter);
            PublicPostsList.setAdapter(concatAdapter);
        }

        if (PostsList.isEmpty()) {
            // If there are no posts, we keep the shimmer effect visible as a placeholder.
            // The shimmer is started when _loadPosts is called.
            PublicPostsList.setVisibility(View.GONE);
        } else {
            _hideShimmer();
            PublicPostsList.setVisibility(View.VISIBLE);
        }
        loadingBody.setVisibility(View.GONE);
        swipeLayout.setRefreshing(false);
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

    public void _ImgRound(final ImageView _imageview, final double _value) {
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable ();
        gd.setColor(android.R.color.transparent);
        gd.setCornerRadius((int)_value);
        _imageview.setClipToOutline(true);
        _imageview.setBackground(gd);
    }

    public void _OpenWebView(final String _URL) {
        String AndroidDevelopersBlogURL = _URL;
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        builder.setToolbarColor(getResources().getColor(R.color.md_theme_surface, null));
        CustomTabsIntent customtabsintent = builder.build();
        customtabsintent.launchUrl(getContext(), Uri.parse(AndroidDevelopersBlogURL));
    }

    public class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.ViewHolder> {

        public class ViewHolder extends RecyclerView.ViewHolder {
            final RecyclerView storiesView;
            final CardView miniPostLayoutProfileCard;
            final EditText miniPostLayoutTextPostInput;
            final ImageView miniPostLayoutProfileImage;
            final ImageView miniPostLayoutImagePost;
            final ImageView miniPostLayoutVideoPost;
            final ImageView miniPostLayoutTextPost;
            final ImageView miniPostLayoutMoreButton;
            final TextView miniPostLayoutTextPostPublish;

            public ViewHolder(View view) {
                super(view);
                storiesView = view.findViewById(R.id.storiesView);
                miniPostLayoutProfileCard = view.findViewById(R.id.miniPostLayoutProfileCard);
                miniPostLayoutTextPostInput = view.findViewById(R.id.miniPostLayoutTextPostInput);
                miniPostLayoutProfileImage = view.findViewById(R.id.miniPostLayoutProfileImage);
                miniPostLayoutImagePost = view.findViewById(R.id.miniPostLayoutImagePost);
                miniPostLayoutVideoPost = view.findViewById(R.id.miniPostLayoutVideoPost);
                miniPostLayoutTextPost = view.findViewById(R.id.miniPostLayoutTextPost);
                miniPostLayoutMoreButton = view.findViewById(R.id.miniPostLayoutMoreButton);
                miniPostLayoutTextPostPublish = view.findViewById(R.id.miniPostLayoutTextPostPublish);
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            View _v = _inflater.inflate(R.layout.feed_header, parent, false);
            return new ViewHolder(_v);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int _position) {
            holder.storiesView.setAdapter(new StoriesViewAdapter(storiesList));
            holder.storiesView.setLayoutManager(new LinearLayoutManager(getContext(),LinearLayoutManager.HORIZONTAL, false));
            _viewGraphics(holder.miniPostLayoutTextPostPublish, Color.TRANSPARENT, Color.TRANSPARENT, 300, 2, 0xFF616161);
            _loadStories(holder.storiesView, storiesList);

            dbService.getReference("users").child(authService.getCurrentUserId()).getData(new IDataListener() {
                @Override
                public void onDataChange(IDataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        HashMap<String, Object> user = dataSnapshot.getValue(HashMap.class);
                        String avatarUrl = (String) user.get("avatar");
                        if (avatarUrl != null && !"null".equals(avatarUrl)) {
                            Glide.with(getContext()).load(Uri.parse(avatarUrl)).into(holder.miniPostLayoutProfileImage);
                        } else {
                            holder.miniPostLayoutProfileImage.setImageResource(R.drawable.avatar);
                        }
                    } else {
                        holder.miniPostLayoutProfileImage.setImageResource(R.drawable.avatar);
                    }
                }

                @Override
                public void onCancelled(IDatabaseError databaseError) {
                    Toast.makeText(getContext(), "Error fetching user profile: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    holder.miniPostLayoutProfileImage.setImageResource(R.drawable.avatar);
                }
            });

	        holder.miniPostLayoutTextPostPublish.setVisibility(View.GONE);
	        _ImageColor(holder.miniPostLayoutImagePost, 0xFF445E91);
	        _ImageColor(holder.miniPostLayoutVideoPost, 0xFF445E91);
	        _ImageColor(holder.miniPostLayoutTextPost, 0xFF445E91);
	        _ImageColor(holder.miniPostLayoutMoreButton, 0xFF445E91);
	        _viewGraphics(holder.miniPostLayoutImagePost, 0xFFFFFFFF, 0xFFEEEEEE, 300, 1, 0xFFEEEEEE);
	        _viewGraphics(holder.miniPostLayoutVideoPost, 0xFFFFFFFF, 0xFFEEEEEE, 300, 1, 0xFFEEEEEE);
	        _viewGraphics(holder.miniPostLayoutTextPost, 0xFFFFFFFF, 0xFFEEEEEE, 300, 1, 0xFFEEEEEE);
	        _viewGraphics(holder.miniPostLayoutMoreButton, 0xFFFFFFFF, 0xFFEEEEEE, 300, 1, 0xFFEEEEEE);

            holder.miniPostLayoutTextPostInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
                    final String _charSeq = _param1.toString();
                    if (_charSeq.length() == 0) {
                        holder.miniPostLayoutTextPostPublish.setVisibility(View.GONE);
                    } else {
                        _viewGraphics(holder.miniPostLayoutTextPostPublish, getResources().getColor(R.color.colorPrimary), 0xFFC5CAE9, 300, 0, Color.TRANSPARENT);
                        holder.miniPostLayoutTextPostPublish.setVisibility(View.VISIBLE);
                    }
                }

                @Override
                public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {}

                @Override
                public void afterTextChanged(Editable _param1) {}
            });

            holder.miniPostLayoutImagePost.setOnClickListener(v -> {
                intent.setClass(getContext(), CreatePostActivity.class);
                startActivity(intent);
            });

            holder.miniPostLayoutVideoPost.setOnClickListener(v -> {
                intent.setClass(getContext(), CreateLineVideoActivity.class);
                startActivity(intent);
            });

            holder.miniPostLayoutTextPostPublish.setOnClickListener(v -> {
                if (holder.miniPostLayoutTextPostInput.getText().toString().trim().isEmpty()) {
                    Toast.makeText(getContext(), getResources().getString(R.string.please_enter_text), Toast.LENGTH_SHORT).show();
                } else {
                    if (holder.miniPostLayoutTextPostInput.getText().toString().length() <= 1500) {
                        String uniqueKey = UUID.randomUUID().toString();
                        cc = Calendar.getInstance();
                        createPostMap = new HashMap<>();
                        createPostMap.put("key", uniqueKey);
                        createPostMap.put("uid", authService.getCurrentUserId());
                        createPostMap.put("post_text", holder.miniPostLayoutTextPostInput.getText().toString().trim());
                        createPostMap.put("post_type", "TEXT");
                        createPostMap.put("post_hide_views_count", false);
                        createPostMap.put("post_region", "none");
                        createPostMap.put("post_hide_like_count", false);
                        createPostMap.put("post_hide_comments_count", false);
                        createPostMap.put("post_visibility", "public");
                        createPostMap.put("post_disable_favorite", false);
                        createPostMap.put("post_disable_comments", false);
                        createPostMap.put("publish_date", String.valueOf(cc.getTimeInMillis()));
                        dbService.getReference("posts").child(uniqueKey).setValue(createPostMap, (result, error) -> {
                            if (error == null) {
                                Toast.makeText(getContext(), getResources().getString(R.string.post_publish_success), Toast.LENGTH_SHORT).show();
                                _loadPosts();
                            } else {
                                Toast.makeText(getContext(), error.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        holder.miniPostLayoutTextPostInput.setText("");
                    }
                }
                vbr.vibrate(48);
            });
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            // No-op for now, since we are not using persistent listeners in the same way as Firebase.
        }

        @Override
        public int getItemCount() {
            return 1;
        }
    }

    public class StoriesViewAdapter extends RecyclerView.Adapter<StoriesViewAdapter.ViewHolder> {
        ArrayList<HashMap<String, Object>> _data;
        public StoriesViewAdapter(ArrayList<HashMap<String, Object>> _arr) {
            _data = _arr;
        }
        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            View _v = _inflater.inflate(R.layout.synapse_story_cv, null);
            RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            _v.setLayoutParams(_lp);
            return new ViewHolder(_v);
        }
        @Override
        public void onBindViewHolder(ViewHolder _holder, final int _position) {
            View _view = _holder.itemView;
            final LinearLayout storiesMyStory = _view.findViewById(R.id.storiesMyStory);
            final LinearLayout storiesSecondStory = _view.findViewById(R.id.storiesSecondStory);
            final androidx.cardview.widget.CardView storiesMyStoryProfileCard = _view.findViewById(R.id.storiesMyStoryProfileCard);
            final TextView storiesMyStoryTitle = _view.findViewById(R.id.storiesMyStoryTitle);
            final ImageView storiesMyStoryProfileImage = _view.findViewById(R.id.storiesMyStoryProfileImage);
            final LinearLayout storiesMyStoryRelativeAddBody = _view.findViewById(R.id.storiesMyStoryRelativeAddBody);
            final ImageView storiesMyStoryRelativeAdd = _view.findViewById(R.id.storiesMyStoryRelativeAdd);
            final androidx.cardview.widget.CardView storiesSecondStoryProfileCard = _view.findViewById(R.id.storiesSecondStoryProfileCard);
            final TextView storiesSecondStoryTitle = _view.findViewById(R.id.storiesSecondStoryTitle);
            final ImageView storiesSecondStoryProfileImage = _view.findViewById(R.id.storiesSecondStoryProfileImage);

            LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
            (int) (getResources().getDisplayMetrics().density * 80),
            ViewGroup.LayoutParams.WRAP_CONTENT
            );
            layoutParams.setMargins(
            (int) (getResources().getDisplayMetrics().density * 4),
            (int) (getResources().getDisplayMetrics().density * 8),
            (int) (getResources().getDisplayMetrics().density * 4),
            (int) (getResources().getDisplayMetrics().density * 8)
            );
            _view.setLayoutParams(layoutParams);

            _ImageColor(storiesMyStoryRelativeAdd, 0xFFFFFFFF);
            _viewGraphics(storiesMyStory, 0xFFFFFFFF, 0xFFEEEEEE, 18, 0, Color.TRANSPARENT);
            _viewGraphics(storiesSecondStory, 0xFFFFFFFF, 0xFFEEEEEE, 18, 0, Color.TRANSPARENT);
            storiesMyStoryRelativeAddBody.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)0, 0x7B000000));
            storiesMyStoryProfileCard.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)300, Color.TRANSPARENT));
            storiesSecondStoryProfileCard.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)300, Color.TRANSPARENT));

            if (_position == 0) {
                storiesMyStoryTitle.setText(getResources().getString(R.string.add_story));
                dbService.getReference("users").child(authService.getCurrentUserId()).getData(new IDataListener() {
                    @Override
                    public void onDataChange(IDataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            HashMap<String, Object> user = dataSnapshot.getValue(HashMap.class);
                            String avatarUrl = (String) user.get("avatar");
                            if (avatarUrl != null && !"null".equals(avatarUrl)) {
                                Glide.with(getContext()).load(Uri.parse(avatarUrl)).into(storiesMyStoryProfileImage);
                            } else {
                                storiesMyStoryProfileImage.setImageResource(R.drawable.avatar);
                            }
                        } else {
                            storiesMyStoryProfileImage.setImageResource(R.drawable.avatar);
                        }
                    }

                    @Override
                    public void onCancelled(IDatabaseError databaseError) {
                        Log.e("StoriesAdapter", "Failed to load user avatar for My Story: " + databaseError.getMessage());
                        storiesMyStoryProfileImage.setImageResource(R.drawable.avatar);
                    }
                });
                storiesMyStory.setVisibility(View.VISIBLE);
                storiesSecondStory.setVisibility(View.GONE);
            } else {
                storiesMyStory.setVisibility(View.GONE);
                storiesSecondStory.setVisibility(View.VISIBLE);
                HashMap<String, Object> storyMap = _data.get(_position);
                String storyUid = (String) storyMap.get("uid");
                if (UserInfoCacheMap.containsKey("uid-" + storyUid)) {
                    _displayUserInfoForStory(storyUid, storiesSecondStoryProfileImage, storiesSecondStoryTitle);
                } else {
                    dbService.getReference("users").child(storyUid).getData(new IDataListener() {
                        @Override
                        public void onDataChange(IDataSnapshot dataSnapshot) {
                            if (dataSnapshot.exists()) {
                                HashMap<String, Object> user = dataSnapshot.getValue(HashMap.class);
                                UserInfoCacheMap.put("uid-" + storyUid, storyUid);
                                UserInfoCacheMap.put("nickname-" + storyUid, user.get("nickname"));
                                UserInfoCacheMap.put("username-" + storyUid, user.get("username"));
                                UserInfoCacheMap.put("avatar-" + storyUid, user.get("avatar"));
                                _displayUserInfoForStory(storyUid, storiesSecondStoryProfileImage, storiesSecondStoryTitle);
                            } else {
                                storiesSecondStoryProfileImage.setImageResource(R.drawable.avatar);
                                storiesSecondStoryTitle.setText("Unknown User");
                            }
                        }

                        @Override
                        public void onCancelled(IDatabaseError databaseError) {
                            Log.e("StoriesAdapter", "Failed to load user info for story: " + databaseError.getMessage());
                            storiesSecondStoryProfileImage.setImageResource(R.drawable.avatar);
                            storiesSecondStoryTitle.setText("Error User");
                        }
                    });
                }
            }
            storiesMyStory.setOnClickListener(_view1 -> Toast.makeText(getContext(), "Add story clicked", Toast.LENGTH_SHORT).show());
            storiesSecondStory.setOnClickListener(_view1 -> Toast.makeText(getContext(), "Story ".concat(String.valueOf(_position)).concat(" clicked"), Toast.LENGTH_SHORT).show());
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
        private void _displayUserInfoForStory(String uid, ImageView profileImage, TextView titleTextView) {
            String avatarUrl = (String) UserInfoCacheMap.get("avatar-" + uid);
            String nickname = (String) UserInfoCacheMap.get("nickname-" + uid);
            String username = (String) UserInfoCacheMap.get("username-" + uid);

            if (avatarUrl != null && !avatarUrl.equals("null")) {
                Glide.with(getContext()).load(Uri.parse(avatarUrl)).into(profileImage);
            } else {
                profileImage.setImageResource(R.drawable.avatar);
            }

            if (nickname != null && !nickname.equals("null") && !nickname.isEmpty()) {
                titleTextView.setText(nickname);
            } else if (username != null && !username.equals("null") && !username.isEmpty()) {
                titleTextView.setText("@" + username);
            } else {
                titleTextView.setText("User Story");
            }
        }
    }

    public class PublicPostsListAdapter extends RecyclerView.Adapter<PublicPostsListAdapter.ViewHolder> {
        ArrayList<HashMap<String, Object>> _data;
        public PublicPostsListAdapter(ArrayList<HashMap<String, Object>> _arr) {
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
            final ImageView topMoreButton = _view.findViewById(R.id.topMoreButton);
            final androidx.cardview.widget.CardView userInfoProfileCard = _view.findViewById(R.id.userInfoProfileCard);
            final ImageView userInfoProfileImage = _view.findViewById(R.id.userInfoProfileImage);
            final TextView userInfoUsername = _view.findViewById(R.id.userInfoUsername);
            final ImageView userInfoGenderBadge = _view.findViewById(R.id.userInfoGenderBadge);
            final ImageView userInfoUsernameVerifiedBadge = _view.findViewById(R.id.userInfoUsernameVerifiedBadge);
            final TextView postPublishDate = _view.findViewById(R.id.postPublishDate);
            final ImageView postPrivateStateIcon = _view.findViewById(R.id.postPrivateStateIcon);
            final TextView postMessageTextMiddle = _view.findViewById(R.id.postMessageTextMiddle);
            final ImageView postImage = _view.findViewById(R.id.postImage);
            final LinearLayout likeButton = _view.findViewById(R.id.likeButton);
            final LinearLayout commentsButton = _view.findViewById(R.id.commentsButton);
            final ImageView favoritePostButton = _view.findViewById(R.id.favoritePostButton);
            final ImageView likeButtonIc = _view.findViewById(R.id.likeButtonIc);
            final TextView likeButtonCount = _view.findViewById(R.id.likeButtonCount);
            final TextView commentsButtonCount = _view.findViewById(R.id.commentsButtonCount);

            body.setVisibility(View.GONE);
            userInfoProfileCard.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)300, Color.TRANSPARENT));
            _ImageColor(postPrivateStateIcon, 0xFF616161);
            _viewGraphics(topMoreButton, 0xFFFFFFFF, 0xFFEEEEEE, 300, 0, Color.TRANSPARENT);

            postImage.setVisibility(View.GONE);

            if (_data.get(_position).containsKey("post_text")) {
                String postText = _data.get(_position).get("post_text").toString();
                com.synapse.social.studioasinc.styling.MarkdownRenderer.get(postMessageTextMiddle.getContext()).render(postMessageTextMiddle, postText);
                postMessageTextMiddle.setVisibility(View.VISIBLE);
            } else {
                postMessageTextMiddle.setVisibility(View.GONE);
            }

            if (_data.get(_position).containsKey("post_image")) {
                Glide.with(getContext()).load(Uri.parse(_data.get(_position).get("post_image").toString())).into(postImage);
                postImage.setOnClickListener(_view1 -> _OpenWebView(_data.get(_position).get("post_image").toString()));
                postImage.setVisibility(View.VISIBLE);
            } else {
                postImage.setVisibility(View.GONE);
            }

            if (_data.get(_position).get("post_hide_like_count").toString().equals("true")) {
                likeButtonCount.setVisibility(View.GONE);
            } else {
                likeButtonCount.setVisibility(View.VISIBLE);
            }
            if (_data.get(_position).get("post_hide_comments_count").toString().equals("true")) {
                commentsButtonCount.setVisibility(View.GONE);
            } else {
                commentsButtonCount.setVisibility(View.VISIBLE);
            }
            if (_data.get(_position).get("post_disable_comments").toString().equals("true")) {
                commentsButton.setVisibility(View.GONE);
            } else {
                commentsButton.setVisibility(View.VISIBLE);
            }
            _setTime(Double.parseDouble(_data.get(_position).get("publish_date").toString()), postPublishDate);

            final String postUid = _data.get(_position).get("uid").toString();
            if (UserInfoCacheMap.containsKey("uid-".concat(postUid))) {
                _updatePostViewVisibility(body, postPrivateStateIcon, postUid, _data.get(_position).get("post_visibility").toString());
                _displayUserInfoFromCache(postUid, userInfoProfileImage, userInfoUsername, userInfoGenderBadge, userInfoUsernameVerifiedBadge);
            } else {
                dbService.getReference("users").child(postUid).getData(new IDataListener() {
                    @Override
                    public void onDataChange(IDataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            HashMap<String, Object> user = dataSnapshot.getValue(HashMap.class);
                            UserInfoCacheMap.put("uid-".concat(postUid), postUid);
                            UserInfoCacheMap.put("banned-".concat(postUid), user.get("banned"));
                            UserInfoCacheMap.put("nickname-".concat(postUid), user.get("nickname"));
                            UserInfoCacheMap.put("username-".concat(postUid), user.get("username"));
                            UserInfoCacheMap.put("avatar-".concat(postUid), user.get("avatar"));
                            UserInfoCacheMap.put("gender-".concat(postUid), user.get("gender"));
                            UserInfoCacheMap.put("verify-".concat(postUid), user.get("verify"));
                            UserInfoCacheMap.put("acc_type-".concat(postUid), user.get("account_type"));
                            _updatePostViewVisibility(body, postPrivateStateIcon, postUid, _data.get(_position).get("post_visibility").toString());
                            _displayUserInfoFromCache(postUid, userInfoProfileImage, userInfoUsername, userInfoGenderBadge, userInfoUsernameVerifiedBadge);
                        } else {
                            userInfoProfileImage.setImageResource(R.drawable.avatar);
                            userInfoUsername.setText("Unknown User");
                            userInfoGenderBadge.setVisibility(View.GONE);
                            userInfoUsernameVerifiedBadge.setVisibility(View.GONE);
                            body.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(IDatabaseError databaseError) {
                        userInfoProfileImage.setImageResource(R.drawable.avatar);
                        userInfoUsername.setText("Error User");
                        userInfoGenderBadge.setVisibility(View.GONE);
                        userInfoUsernameVerifiedBadge.setVisibility(View.GONE);
                        body.setVisibility(View.GONE);
                    }
                });
            }

            dbService.getReference("posts-likes").child(_data.get(_position).get("key").toString()).child(authService.getCurrentUserId()).getData(new IDataListener() {
                @Override
                public void onDataChange(IDataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        likeButtonIc.setImageResource(R.drawable.post_icons_1_2);
                    } else {
                        likeButtonIc.setImageResource(R.drawable.post_icons_1_1);
                    }
                }
                @Override
                public void onCancelled(IDatabaseError databaseError) {}
            });
            dbService.getReference("posts-comments").child(_data.get(_position).get("key").toString()).getData(new IDataListener() {
                @Override
                public void onDataChange(IDataSnapshot dataSnapshot) {
                    _setCount(commentsButtonCount, dataSnapshot.getChildrenCount());
                }
                @Override
                public void onCancelled(IDatabaseError databaseError) {}
            });
            dbService.getReference("posts-likes").child(_data.get(_position).get("key").toString()).getData(new IDataListener() {
                @Override
                public void onDataChange(IDataSnapshot dataSnapshot) {
                    long count = dataSnapshot.getChildrenCount();
                    _setCount(likeButtonCount, count);
                    postLikeCountCache.put(_data.get(_position).get("key").toString(), String.valueOf(count));
                }
                @Override
                public void onCancelled(IDatabaseError databaseError) {}
            });
            dbService.getReference("favorite-posts").child(authService.getCurrentUserId()).child(_data.get(_position).get("key").toString()).getData(new IDataListener() {
                @Override
                public void onDataChange(IDataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        favoritePostButton.setImageResource(R.drawable.delete_favorite_post_ic);
                    } else {
                        favoritePostButton.setImageResource(R.drawable.add_favorite_post_ic);
                    }
                }
                @Override
                public void onCancelled(IDatabaseError databaseError) {}
            });

            likeButton.setOnClickListener(_view1 -> {
                dbService.getReference("posts-likes").child(_data.get(_position).get("key").toString()).child(authService.getCurrentUserId()).getData(new IDataListener() {
                    @Override
                    public void onDataChange(IDataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            dbService.getReference("posts-likes").child(_data.get(_position).get("key").toString()).child(authService.getCurrentUserId()).setValue(null, (result, error) -> {});
                            double currentLikes = Double.parseDouble(postLikeCountCache.get(_data.get(_position).get("key").toString()).toString());
                            postLikeCountCache.put(_data.get(_position).get("key").toString(), String.valueOf((long)(currentLikes - 1)));
                            _setCount(likeButtonCount, currentLikes - 1);
                            likeButtonIc.setImageResource(R.drawable.post_icons_1_1);
                        } else {
                            dbService.getReference("posts-likes").child(_data.get(_position).get("key").toString()).child(authService.getCurrentUserId()).setValue(authService.getCurrentUserId(), (result, error) -> {});
                            NotificationUtils.sendPostLikeNotification(_data.get(_position).get("key").toString(), _data.get(_position).get("uid").toString());
                            double currentLikes = Double.parseDouble(postLikeCountCache.get(_data.get(_position).get("key").toString()).toString());
                            postLikeCountCache.put(_data.get(_position).get("key").toString(), String.valueOf((long)(currentLikes + 1)));
                            _setCount(likeButtonCount, currentLikes + 1);
                            likeButtonIc.setImageResource(R.drawable.post_icons_1_2);
                        }
                    }
                    @Override
                    public void onCancelled(IDatabaseError databaseError) {}
                });
                vbr.vibrate((long)(24));
            });
            commentsButton.setOnClickListener(_view1 -> {
                Bundle sendPostKey = new Bundle();
                sendPostKey.putString("postKey", _data.get(_position).get("key").toString());
                sendPostKey.putString("postPublisherUID", _data.get(_position).get("uid").toString());
                sendPostKey.putString("postPublisherAvatar", UserInfoCacheMap.get("avatar-".concat(_data.get(_position).get("uid").toString())).toString());
                PostCommentsBottomSheetDialog postCommentsBottomSheet = new PostCommentsBottomSheetDialog();
                postCommentsBottomSheet.setArguments(sendPostKey);
                postCommentsBottomSheet.show(getParentFragmentManager(), postCommentsBottomSheet.getTag());
            });
            userInfoProfileImage.setOnClickListener(_view1 -> {
                intent.setClass(getContext(), ProfileActivity.class);
                intent.putExtra("uid", _data.get(_position).get("uid").toString());
                startActivity(intent);
            });
            favoritePostButton.setOnClickListener(_view1 -> {
                dbService.getReference("favorite-posts").child(authService.getCurrentUserId()).child(_data.get(_position).get("key").toString()).getData(new IDataListener() {
                    @Override
                    public void onDataChange(IDataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            dbService.getReference("favorite-posts").child(authService.getCurrentUserId()).child(_data.get(_position).get("key").toString()).setValue(null, (result, error) -> {});
                            favoritePostButton.setImageResource(R.drawable.add_favorite_post_ic);
                        } else {
                            dbService.getReference("favorite-posts").child(authService.getCurrentUserId()).child(_data.get(_position).get("key").toString()).setValue(_data.get(_position).get("key").toString(), (result, error) -> {});
                            favoritePostButton.setImageResource(R.drawable.delete_favorite_post_ic);
                        }
                    }
                    @Override
                    public void onCancelled(IDatabaseError databaseError) {}
                });
                vbr.vibrate((long)(24));
            });
            topMoreButton.setOnClickListener(_view1 -> {
                Bundle sendPostKey = new Bundle();
                sendPostKey.putString("postKey", _data.get(_position).get("key").toString());
                sendPostKey.putString("postPublisherUID", _data.get(_position).get("uid").toString());
                sendPostKey.putString("postType", _data.get(_position).get("post_type").toString());
                if (_data.get(_position).containsKey("post_text") && _data.get(_position).get("post_text") != null) {
                    sendPostKey.putString("postText", _data.get(_position).get("post_text").toString());
                } else {
                    sendPostKey.putString("postText", "");
                }
                if (_data.get(_position).containsKey("post_image") && _data.get(_position).get("post_image") != null && !_data.get(_position).get("post_image").toString().isEmpty()) {
                    sendPostKey.putString("postImg", _data.get(_position).get("post_image").toString());
                }
                PostMoreBottomSheetDialog postMoreBottomSheetDialog = new PostMoreBottomSheetDialog();
                postMoreBottomSheetDialog.setArguments(sendPostKey);
                postMoreBottomSheetDialog.show(getParentFragmentManager(), postMoreBottomSheetDialog.getTag());
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

    private void _updatePostViewVisibility(LinearLayout body, ImageView postPrivateStateIcon, String postUid, String postVisibility) {
        if ("private".equals(postVisibility)) {
            if (postUid.equals(authService.getCurrentUserId())) {
                postPrivateStateIcon.setVisibility(View.VISIBLE);
                body.setVisibility(View.VISIBLE);
            } else {
                body.setVisibility(View.GONE);
            }
        } else {
            body.setVisibility(View.VISIBLE);
            postPrivateStateIcon.setVisibility(View.GONE);
        }
    }

    private void _displayUserInfoFromCache(String postUid, ImageView userInfoProfileImage, TextView userInfoUsername, ImageView userInfoGenderBadge, ImageView userInfoUsernameVerifiedBadge) {
        if (UserInfoCacheMap.get("banned-".concat(postUid)).toString().equals("true")) {
            userInfoProfileImage.setImageResource(R.drawable.banned_avatar);
        } else {
            if (UserInfoCacheMap.get("avatar-".concat(postUid)).toString().equals("null")) {
                userInfoProfileImage.setImageResource(R.drawable.avatar);
            } else {
                Glide.with(getContext()).load(Uri.parse(UserInfoCacheMap.get("avatar-".concat(postUid)).toString())).into(userInfoProfileImage);
            }
        }

        if (UserInfoCacheMap.get("nickname-".concat(postUid)).toString().equals("null")) {
            userInfoUsername.setText("@" + UserInfoCacheMap.get("username-".concat(postUid)).toString());
        } else {
            userInfoUsername.setText(UserInfoCacheMap.get("nickname-".concat(postUid)).toString());
        }

        if (UserInfoCacheMap.get("gender-".concat(postUid)).toString().equals("hidden")) {
            userInfoGenderBadge.setVisibility(View.GONE);
        } else {
            if (UserInfoCacheMap.get("gender-".concat(postUid)).toString().equals("male")) {
                userInfoGenderBadge.setImageResource(R.drawable.male_badge);
                userInfoGenderBadge.setVisibility(View.VISIBLE);
            } else if (UserInfoCacheMap.get("gender-".concat(postUid)).toString().equals("female")) {
                userInfoGenderBadge.setImageResource(R.drawable.female_badge);
                userInfoGenderBadge.setVisibility(View.VISIBLE);
            }
        }

        String accountType = UserInfoCacheMap.get("acc_type-".concat(postUid)).toString();
        if ("admin".equals(accountType)) {
            userInfoUsernameVerifiedBadge.setImageResource(R.drawable.admin_badge);
            userInfoUsernameVerifiedBadge.setVisibility(View.VISIBLE);
        } else if ("moderator".equals(accountType)) {
            userInfoUsernameVerifiedBadge.setImageResource(R.drawable.moderator_badge);
            userInfoUsernameVerifiedBadge.setVisibility(View.VISIBLE);
        } else if ("support".equals(accountType)) {
            userInfoUsernameVerifiedBadge.setImageResource(R.drawable.support_badge);
            userInfoUsernameVerifiedBadge.setVisibility(View.VISIBLE);
        } else if ("user".equals(accountType)) {
            if (UserInfoCacheMap.get("verify-".concat(postUid)).toString().equals("true")) {
                userInfoUsernameVerifiedBadge.setVisibility(View.VISIBLE);
            } else {
                userInfoUsernameVerifiedBadge.setVisibility(View.GONE);
            }
        }
    }
}