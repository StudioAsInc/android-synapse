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
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.*;
import com.google.android.material.appbar.AppBarLayout;
import com.synapse.social.studioasinc.backend.AuthenticationService;
import com.synapse.social.studioasinc.backend.DatabaseService;
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService;
import com.synapse.social.studioasinc.backend.interfaces.IDataListener;
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError;
import com.synapse.social.studioasinc.backend.interfaces.IQuery;
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener;
import io.supabase.postgrest.PostgrestCallback;
import io.supabase.postgrest.PostgrestResponse;
import io.supabase.postgrest.PostgrestError;
import io.supabase.postgrest.PostgrestQuery;
import java.util.Map;
import java.util.List;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.browser.customtabs.CustomTabsIntent;
import com.google.firebase.database.Query;
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
    private Calendar cc = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authService = new AuthenticationService(SynapseApp.supabaseClient);
        dbService = new DatabaseService(SynapseApp.supabaseClient);
        
        initialize(view);
        initializeLogic();
    }

    private void initialize(View view) {
        loadingBody = view.findViewById(R.id.loadingBody);
        swipeLayout = view.findViewById(R.id.swipeLayout);
        PublicPostsList = view.findViewById(R.id.PublicPostsList);
        loading_bar = view.findViewById(R.id.loading_bar);
        shimmer_container = view.findViewById(R.id.shimmer_container);

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
        IQuery query = dbService.getReference("stories").orderByChild("publish_date");
        dbService.getData(query, new IDataListener() {
            @Override
            public void onDataChange(IDataSnapshot dataSnapshot) {
                storiesList.clear();
                storiesList.add(new HashMap<>()); // For the "Add Story" item
                if (dataSnapshot.exists()) {
                    for (IDataSnapshot snapshot : dataSnapshot.getChildren()) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("key", snapshot.getKey());
                        if (snapshot.getValue() instanceof HashMap) {
                            map.putAll((HashMap<String, Object>) snapshot.getValue());
                        }
                        storiesList.add(map);
                    }
                }
                if(storiesView.getAdapter() != null) {
                    storiesView.getAdapter().notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(IDatabaseError databaseError) {
                Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
                PostsList.clear();
                if (dataSnapshot.exists()) {
                    for (IDataSnapshot snapshot : dataSnapshot.getChildren()) {
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("key", snapshot.getKey());
                        map.put("uid", snapshot.child("uid").getValue(String.class));
                        map.put("post_text", snapshot.child("post_text").getValue(String.class));
                        map.put("post_type", snapshot.child("post_type").getValue(String.class));
                        map.put("post_hide_views_count", snapshot.child("post_hide_views_count").getValue(String.class));
                        map.put("post_region", snapshot.child("post_region").getValue(String.class));
                        map.put("post_hide_like_count", snapshot.child("post_hide_like_count").getValue(String.class));
                        map.put("post_hide_comments_count", snapshot.child("post_hide_comments_count").getValue(String.class));
                        map.put("post_visibility", snapshot.child("post_visibility").getValue(String.class));
                        map.put("post_disable_favorite", snapshot.child("post_disable_favorite").getValue(String.class));
                        map.put("post_disable_comments", snapshot.child("post_disable_comments").getValue(String.class));
                        map.put("publish_date", snapshot.child("publish_date").getValue(String.class));
                        if (snapshot.child("post_image") != null) {
                            map.put("post_image", snapshot.child("post_image").getValue(String.class));
                        }
                        PostsList.add(map);
                    }
                }
                _finalizePostDisplay(notFoundMessage, true);
            }

            @Override
            public void onCancelled(IDatabaseError databaseError) {
                _finalizePostDisplay(databaseError.getMessage(), false);
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

            dbService.getData(dbService.getReference("users/" + authService.getCurrentUser().getUid()), new IDataListener() {
                @Override
                public void onDataChange(IDataSnapshot dataSnapshot) {
                    if(dataSnapshot.exists()) {
                        if (dataSnapshot.child("avatar").getValue(String.class) != null && !dataSnapshot.child("avatar").getValue(String.class).equals("null")) {
                            Glide.with(getContext()).load(Uri.parse(dataSnapshot.child("avatar").getValue(String.class))).into(holder.miniPostLayoutProfileImage);
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
                if (holder.miniPostLayoutTextPostInput.getText().toString().trim().equals("")) {
                    Toast.makeText(getContext(), getResources().getString(R.string.please_enter_text), Toast.LENGTH_SHORT).show();
                } else {
                    if (!(holder.miniPostLayoutTextPostInput.getText().toString().length() > 1500)) {
                        String uniqueKey = dbService.getReference("posts").push().getKey();
                        cc = Calendar.getInstance();
                        createPostMap = new HashMap<>();
                        createPostMap.put("key", uniqueKey);
                        createPostMap.put("uid", authService.getCurrentUser().getUid());
                        createPostMap.put("post_text", holder.miniPostLayoutTextPostInput.getText().toString().trim());
                        createPostMap.put("post_type", "TEXT");
                        createPostMap.put("post_hide_views_count", "false");
                        createPostMap.put("post_region", "none");
                        createPostMap.put("post_hide_like_count", "false");
                        createPostMap.put("post_hide_comments_count", "false");
                        createPostMap.put("post_visibility", "public");
                        createPostMap.put("post_disable_favorite", "false");
                        createPostMap.put("post_disable_comments", "false");
                        createPostMap.put("publish_date", String.valueOf((long)(cc.getTimeInMillis())));
                        dbService.getReference("posts/" + uniqueKey).setValue(createPostMap, new ICompletionListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Toast.makeText(getContext(), getResources().getString(R.string.post_publish_success), Toast.LENGTH_SHORT).show();
                                _loadPosts();
                            }

                            @Override
                            public void onFailure(Exception e) {
                                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        holder.miniPostLayoutTextPostInput.setText("");
                    }
                }
            });
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            if (holder.profileRef != null && holder.profileListener != null) {
                holder.profileRef.removeEventListener(holder.profileListener);
            }
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
                dbService.getData(dbService.getReference("users/" + authService.getCurrentUser().getUid()), new IDataListener() {
                    @Override
                    public void onDataChange(IDataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            if (dataSnapshot.child("avatar").getValue(String.class) != null && !dataSnapshot.child("avatar").getValue(String.class).equals("null")) {
                                Glide.with(getContext()).load(Uri.parse(dataSnapshot.child("avatar").getValue(String.class))).into(storiesMyStoryProfileImage);
                            } else {
                                storiesMyStoryProfileImage.setImageResource(R.drawable.avatar);
                            }
                        } else {
                            storiesMyStoryProfileImage.setImageResource(R.drawable.avatar);
                        }
                    }

                    @Override
                    public void onCancelled(IDatabaseError databaseError) {
                        Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
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
                dbService.getData(dbService.getReference("users/" + postUid), new IDataListener() {
                    @Override
                    public void onDataChange(IDataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            UserInfoCacheMap.put("uid-".concat(postUid), dataSnapshot.child("uid").getValue(String.class));
                            UserInfoCacheMap.put("username-".concat(postUid), dataSnapshot.child("username").getValue(String.class));
                            UserInfoCacheMap.put("nickname-".concat(postUid), dataSnapshot.child("nickname").getValue(String.class));
                            UserInfoCacheMap.put("avatar-".concat(postUid), dataSnapshot.child("avatar").getValue(String.class));
                            UserInfoCacheMap.put("gender-".concat(postUid), dataSnapshot.child("gender").getValue(String.class));
                            UserInfoCacheMap.put("acc_type-".concat(postUid), dataSnapshot.child("account_type").getValue(String.class));
                            UserInfoCacheMap.put("verify-".concat(postUid), dataSnapshot.child("verify").getValue(String.class));
                            UserInfoCacheMap.put("banned-".concat(postUid), dataSnapshot.child("banned").getValue(String.class));
                            _updatePostViewVisibility(body, postPrivateStateIcon, postUid, _data.get(_position).get("post_visibility").toString());
                            _displayUserInfoFromCache(postUid, userInfoProfileImage, userInfoUsername, userInfoGenderBadge, userInfoUsernameVerifiedBadge);
                        }
                    }

                    @Override
                    public void onCancelled(IDatabaseError databaseError) {
                        Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            dbService.getData(dbService.getReference("posts-likes/" + _data.get(_position).get("key").toString() + "/" + authService.getCurrentUser().getUid()), new IDataListener() {
                @Override
                public void onDataChange(IDataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        likeButtonIc.setImageResource(R.drawable.like_2_filled);
                    } else {
                        likeButtonIc.setImageResource(R.drawable.like_2);
                    }
                }

                @Override
                public void onCancelled(IDatabaseError databaseError) {
                    Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            dbService.getData(dbService.getReference("posts-comments/" + _data.get(_position).get("key").toString()), new IDataListener() {
                @Override
                public void onDataChange(IDataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        commentsButtonCount.setText(String.valueOf(dataSnapshot.getChildrenCount()));
                    } else {
                        commentsButtonCount.setText("0");
                    }
                }

                @Override
                public void onCancelled(IDatabaseError databaseError) {
                    Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            dbService.getData(dbService.getReference("posts-likes/" + _data.get(_position).get("key").toString()), new IDataListener() {
                @Override
                public void onDataChange(IDataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        likeButtonCount.setText(String.valueOf(dataSnapshot.getChildrenCount()));
                    } else {
                        likeButtonCount.setText("0");
                    }
                }

                @Override
                public void onCancelled(IDatabaseError databaseError) {
                    Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

            dbService.getData(dbService.getReference("favorite-posts/" + authService.getCurrentUser().getUid() + "/" + _data.get(_position).get("key").toString()), new IDataListener() {
                @Override
                public void onDataChange(IDataSnapshot dataSnapshot) {
                    if (dataSnapshot.exists()) {
                        favoritePostButton.setImageResource(R.drawable.favorite_filled);
                    } else {
                        favoritePostButton.setImageResource(R.drawable.favorite);
                    }
                }

                @Override
                public void onCancelled(IDatabaseError databaseError) {
                    // Handle error
                }
            });






            likeButton.setOnClickListener(_view1 -> {
                dbService.getData(dbService.getReference("posts-likes/" + _data.get(_position).get("key").toString() + "/" + authService.getCurrentUser().getUid()), new IDataListener() {
                    @Override
                    public void onDataChange(IDataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            dbService.getReference("posts-likes/" + _data.get(_position).get("key").toString() + "/" + authService.getCurrentUser().getUid()).setValue(null, new ICompletionListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    likeButtonIc.setImageResource(R.drawable.like_2);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Map<String, Object> likeMap = new HashMap<>();
                            likeMap.put("uid", authService.getCurrentUser().getUid());
                            dbService.getReference("posts-likes/" + _data.get(_position).get("key").toString() + "/" + authService.getCurrentUser().getUid()).setValue(likeMap, new ICompletionListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    likeButtonIc.setImageResource(R.drawable.like_2_filled);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(IDatabaseError databaseError) {
                        Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
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
                dbService.getData(dbService.getReference("favorite-posts/" + authService.getCurrentUser().getUid() + "/" + _data.get(_position).get("key").toString()), new IDataListener() {
                    @Override
                    public void onDataChange(IDataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            dbService.getReference("favorite-posts/" + authService.getCurrentUser().getUid() + "/" + _data.get(_position).get("key").toString()).setValue(null, new ICompletionListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    favoritePostButton.setImageResource(R.drawable.favorite);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            Map<String, Object> favoriteMap = new HashMap<>();
                            favoriteMap.put("uid", authService.getCurrentUser().getUid());
                            dbService.getReference("favorite-posts/" + authService.getCurrentUser().getUid() + "/" + _data.get(_position).get("key").toString()).setValue(favoriteMap, new ICompletionListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    favoritePostButton.setImageResource(R.drawable.favorite_filled);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }

                    @Override
                    public void onCancelled(IDatabaseError databaseError) {
                        Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
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
            if (postUid.equals(authService.getCurrentUser().getUid())) {
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
