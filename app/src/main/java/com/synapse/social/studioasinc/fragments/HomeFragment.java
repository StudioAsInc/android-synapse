package com.synapse.social.studioasinc.fragments;

import com.synapse.social.studioasinc.R;
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
import com.google.android.material.carousel.CarouselLayoutManager;
import com.google.android.material.carousel.CarouselSnapHelper;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import androidx.browser.customtabs.CustomTabsIntent;
import com.google.firebase.database.Query;
import com.synapse.social.studioasinc.CreateLineVideoActivity;
import com.synapse.social.studioasinc.CreatePostActivity;
import com.synapse.social.studioasinc.HomeActivity;
import com.synapse.social.studioasinc.PostCommentsBottomSheetDialog;
import com.synapse.social.studioasinc.PostMoreBottomSheetDialog;
import com.synapse.social.studioasinc.ProfileActivity;
import com.synapse.social.studioasinc.SynapseApp;
import com.synapse.social.studioasinc.database.DatabaseStructure;
import com.synapse.social.studioasinc.database.ContentManager;
import com.synapse.social.studioasinc.util.AuthUtil;

import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

public class HomeFragment extends Fragment {

    private static final int SHIMMER_ITEM_COUNT = 5;
    private static final int PICK_STORY_MEDIA_REQUEST = 101;
    private FirebaseDatabase _firebase;
    private DatabaseReference udb;
    private DatabaseReference postsRef;
    private DatabaseReference storiesDbRef;

    private HashMap<String, Object> createPostMap = new HashMap<>();
    private HashMap<String, Object> postLikeCountCache = new HashMap<>();
    private HashMap<String, Object> UserInfoCacheMap = new HashMap<>();
    private HashMap<String, Object> postFavoriteCountCache = new HashMap<>();

    private ArrayList<com.synapse.social.studioasinc.models.Story> storiesList = new ArrayList<>();
    private ArrayList<HashMap<String, Object>> PostsList = new ArrayList<>();

    private LinearLayout loadingBody;
    private SwipeRefreshLayout swipeLayout;
    private RecyclerView PublicPostsList;
	private ProgressBar loading_bar;
    private LinearLayout shimmer_container;

    private Intent intent = new Intent();
    private Vibrator vbr;
    private FirebaseAuth auth;
    private Calendar cc = Calendar.getInstance();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        _firebase = FirebaseDatabase.getInstance();
        udb = _firebase.getReference("skyline/users");
        postsRef = _firebase.getReference("skyline/posts");
        storiesDbRef = _firebase.getReference("story");
        postsRef.keepSynced(true);
        storiesDbRef.keepSynced(true);

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
        auth = FirebaseAuth.getInstance();

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

    private void _loadStories(final RecyclerView storiesView, final ArrayList<com.synapse.social.studioasinc.models.Story> storiesList) {
        storiesDbRef.orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!isAdded()) {
                            return;
                        }
                        storiesList.clear();

                        if (dataSnapshot.exists()) {
                            for (DataSnapshot storySnap : dataSnapshot.getChildren()) {
                                com.synapse.social.studioasinc.models.Story story = storySnap.getValue(com.synapse.social.studioasinc.models.Story.class);
                                if (story != null) {
                                    storiesList.add(story);
                                }
                            }
                        }
                        if (storiesView != null && storiesView.getAdapter() != null) {
                            storiesView.getAdapter().notifyDataSetChanged();
                        } else if (storiesView != null) {
                            storiesView.setAdapter(new CarouselAdapter(storiesList));
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(getContext(), "Error loading stories: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        if (storiesView != null && storiesView.getAdapter() != null) {
                            storiesView.getAdapter().notifyDataSetChanged();
                        } else if (storiesView != null) {
                            storiesView.setAdapter(new CarouselAdapter(storiesList));
                        }
                    }
                });
    }

    public void _loadPosts() {
        _showShimmer();
        swipeLayout.setRefreshing(true);
        Query query = postsRef.orderByChild("publish_date");
        String notFoundMessage = "There are no public posts available at the moment.";
        _fetchAndDisplayPosts(query, notFoundMessage);
    }

    private void _fetchAndDisplayPosts(Query query, final String notFoundMessage) {
        if (query == null) {
            _finalizePostDisplay(notFoundMessage, false);
            return;
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot _dataSnapshot) {
                if (!isAdded()) {
                    return;
                }
                PostsList.clear();
                if (_dataSnapshot.exists()) {
                    try {
                        GenericTypeIndicator<HashMap<String, Object>> _ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
                        for (DataSnapshot _data : _dataSnapshot.getChildren()) {
                            HashMap<String, Object> _map = _data.getValue(_ind);
                            PostsList.add(_map);
                        }
                    } catch (Exception _e) {
                        _e.printStackTrace();
                    }
                    _finalizePostDisplay(notFoundMessage, true);
                } else {
                    _finalizePostDisplay(notFoundMessage, false);
                }
            }

            @Override
            public void onCancelled(DatabaseError _databaseError) {
                if (!isAdded()) {
                    return;
                }
                Toast.makeText(getContext(), "Failed to fetch latest posts, showing cached data. Error: " + _databaseError.getMessage(), Toast.LENGTH_LONG).show();
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
        builder.setToolbarColor(Color.parseColor("#242D39"));
        CustomTabsIntent customtabsintent = builder.build();
        customtabsintent.launchUrl(getContext(), Uri.parse(AndroidDevelopersBlogURL));
    }

    public class HeaderAdapter extends RecyclerView.Adapter<HeaderAdapter.ViewHolder> {
        private ViewHolder mViewHolder;

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

            ValueEventListener profileListener;
            DatabaseReference profileRef;

            public ViewHolder(View view) {
                super(view);
                storiesView = view.findViewById(R.id.storiesView);
                // Set up the CarouselLayoutManager and SnapHelper for the stories RecyclerView
                CarouselLayoutManager layoutManager = new com.google.android.material.carousel.CarouselLayoutManager(new com.google.android.material.carousel.HeroCarouselStrategy());
                storiesView.setLayoutManager(layoutManager);
                CarouselSnapHelper snapHelper = new com.google.android.material.carousel.CarouselSnapHelper();
                snapHelper.attachToRecyclerView(storiesView);

                // Resolve gesture conflict with ViewPager2
                if (getActivity() instanceof HomeActivity) {
                    androidx.viewpager2.widget.ViewPager2 viewPager = ((HomeActivity) getActivity()).getViewPager();
                    if (viewPager != null) {
                        storiesView.addOnItemTouchListener(new RecyclerView.OnItemTouchListener() {
                            private int touchSlop = ViewConfiguration.get(storiesView.getContext()).getScaledTouchSlop();
                            private float startX, startY;

                            @Override
                            public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
                                switch (e.getAction()) {
                                    case MotionEvent.ACTION_DOWN:
                                        startX = e.getX();
                                        startY = e.getY();
                                        rv.getParent().requestDisallowInterceptTouchEvent(true);
                                        break;
                                    case MotionEvent.ACTION_MOVE:
                                        float dx = Math.abs(e.getX() - startX);
                                        float dy = Math.abs(e.getY() - startY);
                                        if (dx > touchSlop && dx * 0.5f > dy) {
                                            rv.getParent().requestDisallowInterceptTouchEvent(true);
                                        } else {
                                            rv.getParent().requestDisallowInterceptTouchEvent(false);
                                        }
                                        break;
                                    case MotionEvent.ACTION_UP:
                                    case MotionEvent.ACTION_CANCEL:
                                        rv.getParent().requestDisallowInterceptTouchEvent(false);
                                        break;
                                }
                                return false;
                            }

                            @Override
                            public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {}

                            @Override
                            public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {}
                        });
                    }
                }

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
            mViewHolder = new ViewHolder(_v);
            return mViewHolder;
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, final int _position) {
            holder.storiesView.setAdapter(new CarouselAdapter(storiesList));
            _viewGraphics(holder.miniPostLayoutTextPostPublish, Color.TRANSPARENT, Color.TRANSPARENT, 300, 2, 0xFF616161);
            _loadStories(holder.storiesView, storiesList);

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                holder.profileRef = udb.child(currentUser.getUid());
                holder.profileListener = new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (!isAdded()) {
                            return;
                        }
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
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        if (!isAdded()) {
                            return;
                        }
                        Toast.makeText(getContext(), "Error fetching user profile: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        holder.miniPostLayoutProfileImage.setImageResource(R.drawable.avatar);
                    }
                };
                holder.profileRef.addValueEventListener(holder.profileListener);
            } else {
                holder.miniPostLayoutProfileImage.setImageResource(R.drawable.avatar);
                holder.miniPostLayoutTextPostInput.setEnabled(false);
                holder.miniPostLayoutTextPostInput.setHint("Please log in to post.");
                holder.miniPostLayoutImagePost.setEnabled(false);
                holder.miniPostLayoutVideoPost.setEnabled(false);
                holder.miniPostLayoutTextPost.setEnabled(false);
                holder.miniPostLayoutMoreButton.setVisibility(View.GONE);
            }

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
                        _viewGraphics(holder.miniPostLayoutTextPostPublish, com.synapse.social.studioasinc.util.ThemeUtils.getThemeColor(getContext(), com.google.android.material.R.attr.colorPrimaryContainer), 0xFFC5CAE9, 300, 0, Color.TRANSPARENT);
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
                if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                    Toast.makeText(getContext(), "Please log in to post.", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (holder.miniPostLayoutTextPostInput.getText().toString().trim().equals("")) {
                    Toast.makeText(getContext(), getResources().getString(R.string.please_enter_text), Toast.LENGTH_SHORT).show();
                } else {
                    if (!(holder.miniPostLayoutTextPostInput.getText().toString().length() > 1500)) {
                        String uniqueKey = udb.push().getKey();
                        cc = Calendar.getInstance();
                        createPostMap = new HashMap<>();
                        createPostMap.put("key", uniqueKey);
                        createPostMap.put("uid", FirebaseAuth.getInstance().getCurrentUser().getUid());
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
                        FirebaseDatabase.getInstance().getReference("skyline/posts").child(uniqueKey).updateChildren(createPostMap, (databaseError, databaseReference) -> {
                            if (databaseError == null) {
                                Toast.makeText(getContext(), getResources().getString(R.string.post_publish_success), Toast.LENGTH_SHORT).show();
                                _loadPosts();
                            } else {
                                Toast.makeText(getContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
                        holder.miniPostLayoutTextPostInput.setText("");
                    }
                }
                vbr.vibrate((long)(48));
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

        public void refreshStories() {
            if (mViewHolder != null) {
                _loadStories(mViewHolder.storiesView, storiesList);
            }
        }
    }

    public class CarouselAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int VIEW_TYPE_ADD = 0;
        private static final int VIEW_TYPE_STORY = 1;

        ArrayList<com.synapse.social.studioasinc.models.Story> _data;

        public CarouselAdapter(ArrayList<com.synapse.social.studioasinc.models.Story> _arr) {
            _data = _arr;
        }

        @Override
        public int getItemViewType(int position) {
            return (position == 0) ? VIEW_TYPE_ADD : VIEW_TYPE_STORY;
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater _inflater = getLayoutInflater();
            if (viewType == VIEW_TYPE_ADD) {
                View _v = _inflater.inflate(R.layout.synapse_story_add_cv, parent, false);
                return new AddStoryViewHolder(_v);
            } else {
                View _v = _inflater.inflate(R.layout.synapse_story_cv, parent, false);
                return new StoryViewHolder(_v);
            }
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, final int position) {
            if (holder.getItemViewType() == VIEW_TYPE_ADD) {
                AddStoryViewHolder addHolder = (AddStoryViewHolder) holder;
                addHolder.itemView.setOnClickListener(_view -> pickStoryMedia());
            } else {
                StoryViewHolder storyHolder = (StoryViewHolder) holder;
                final int storyPosition = position - 1;
                com.synapse.social.studioasinc.models.Story story = _data.get(storyPosition);

                if (story == null || story.getUid() == null || story.getUid().isEmpty()) {
                    // Hide the item or show a placeholder
                    holder.itemView.setVisibility(View.GONE);
                    return;
                }
                holder.itemView.setVisibility(View.VISIBLE);

                // Set seen/unseen border
                String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                if (story.getSeenBy() != null && story.getSeenBy().containsKey(currentUid)) {
                    storyHolder.storyBorder.setBackgroundResource(R.drawable.story_border_seen);
                } else {
                    storyHolder.storyBorder.setBackgroundResource(R.drawable.story_border_unseen);
                }

                // Load story preview image
                if (story.getUrl() != null && !story.getUrl().isEmpty()) {
                    Glide.with(getContext()).load(Uri.parse(story.getUrl())).into(storyHolder.storyPreviewImage);
                } else {
                    storyHolder.storyPreviewImage.setImageResource(R.drawable.image_placeholder_background);
                }

                // Fetch and display user info
                udb.child(story.getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (isAdded() && dataSnapshot.exists()) {
                            String nickname = dataSnapshot.child("nickname").getValue(String.class);
                            String username = dataSnapshot.child("username").getValue(String.class);
                            String avatarUrl = dataSnapshot.child("avatar").getValue(String.class);

                            if (nickname != null && !nickname.isEmpty()) {
                                storyHolder.userName.setText(nickname);
                            } else if (username != null && !username.isEmpty()) {
                                storyHolder.userName.setText("@" + username);
                            } else {
                                storyHolder.userName.setText("User");
                            }

                            if (avatarUrl != null && !avatarUrl.equals("null")) {
                                Glide.with(getContext()).load(Uri.parse(avatarUrl)).into(storyHolder.userProfilePicture);
                            } else {
                                storyHolder.userProfilePicture.setImageResource(R.drawable.avatar);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle error, maybe set default user info
                        if(isAdded()) {
                            storyHolder.userName.setText("User");
                            storyHolder.userProfilePicture.setImageResource(R.drawable.avatar);
                        }
                    }
                });

                storyHolder.itemView.setOnClickListener(_view -> {
                    // Add a null check for the story's UID to prevent a crash when marking as seen.
                    if (story.getUid() != null && !story.getUid().isEmpty()) {
                        storiesDbRef.child(story.getUid()).child("seenBy").child(currentUid).setValue(true);
                    }

                    // Create a new list for the story viewer to avoid issues with the "add story" item
                    ArrayList<com.synapse.social.studioasinc.models.Story> storiesForViewer = new ArrayList<>(_data);

                    Intent intent = new Intent(getContext(), com.synapse.social.studioasinc.StoryViewerActivity.class);
                    intent.putExtra("stories", storiesForViewer);
                    // The position passed to the viewer should be the index in the data list, not the adapter
                    intent.putExtra("position", storyPosition);
                    startActivity(intent);
                });
            }
        }

        @Override
        public int getItemCount() {
            return _data.size() + 1; // +1 for the "Add Story" button
        }

        // ViewHolder for the "Add Story" item
        public class AddStoryViewHolder extends RecyclerView.ViewHolder {
            public AddStoryViewHolder(View v) {
                super(v);
            }
        }

        // ViewHolder for a regular story item
        public class StoryViewHolder extends RecyclerView.ViewHolder {
            View storyBorder;
            ImageView storyPreviewImage;
            de.hdodenhof.circleimageview.CircleImageView userProfilePicture;
            TextView userName;

            public StoryViewHolder(View v) {
                super(v);
                storyBorder = v.findViewById(R.id.story_border);
                storyPreviewImage = v.findViewById(R.id.story_preview_image);
                userProfilePicture = v.findViewById(R.id.user_profile_picture);
                userName = v.findViewById(R.id.user_name);
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
                ExecutorService mExecutorService = Executors.newSingleThreadExecutor();
                Handler mMainHandler = new Handler(Looper.getMainLooper());
                mExecutorService.execute(() -> {
                    DatabaseReference userRef = _firebase.getReference().child("skyline/users").child(postUid);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()) {
                                UserInfoCacheMap.put("uid-".concat(postUid), postUid);
                                UserInfoCacheMap.put("banned-".concat(postUid), dataSnapshot.child("banned").getValue(String.class));
                                UserInfoCacheMap.put("nickname-".concat(postUid), dataSnapshot.child("nickname").getValue(String.class));
                                UserInfoCacheMap.put("username-".concat(postUid), dataSnapshot.child("username").getValue(String.class));
                                UserInfoCacheMap.put("avatar-".concat(postUid), dataSnapshot.child("avatar").getValue(String.class));
                                UserInfoCacheMap.put("gender-".concat(postUid), dataSnapshot.child("gender").getValue(String.class));
                                UserInfoCacheMap.put("verify-".concat(postUid), dataSnapshot.child("verify").getValue(String.class));
                                UserInfoCacheMap.put("acc_type-".concat(postUid), dataSnapshot.child("account_type").getValue(String.class));
                                mMainHandler.post(() -> {
                                    _updatePostViewVisibility(body, postPrivateStateIcon, postUid, _data.get(_position).get("post_visibility").toString());
                                    _displayUserInfoFromCache(postUid, userInfoProfileImage, userInfoUsername, userInfoGenderBadge, userInfoUsernameVerifiedBadge);
                                });
                            } else {
                                mMainHandler.post(() -> {
                                    userInfoProfileImage.setImageResource(R.drawable.avatar);
                                    userInfoUsername.setText("Unknown User");
                                    userInfoGenderBadge.setVisibility(View.GONE);
                                    userInfoUsernameVerifiedBadge.setVisibility(View.GONE);
                                    body.setVisibility(View.GONE);
                                });
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            mMainHandler.post(() -> {
                                userInfoProfileImage.setImageResource(R.drawable.avatar);
                                userInfoUsername.setText("Error User");
                                userInfoGenderBadge.setVisibility(View.GONE);
                                userInfoUsernameVerifiedBadge.setVisibility(View.GONE);
                                body.setVisibility(View.GONE);
                            });
                        }
                    });
                });
            }

            DatabaseReference getCommentsCount = _firebase.getReference("skyline/posts-comments").child(_data.get(_position).get("key").toString());
            DatabaseReference getLikesCount = _firebase.getReference("skyline/posts-likes").child(_data.get(_position).get("key").toString());

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null) {
                DatabaseReference getLikeCheck = _firebase.getReference("skyline/posts-likes").child(_data.get(_position).get("key").toString()).child(currentUser.getUid());
                getLikeCheck.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()) {
                            likeButtonIc.setImageResource(R.drawable.post_icons_1_2);
                        } else {
                            likeButtonIc.setImageResource(R.drawable.post_icons_1_1);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });

                DatabaseReference getFavoriteCheck = _firebase.getReference("skyline/favorite-posts").child(currentUser.getUid()).child(_data.get(_position).get("key").toString());
                getFavoriteCheck.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if(dataSnapshot.exists()) {
                            favoritePostButton.setImageResource(R.drawable.delete_favorite_post_ic);
                        } else {
                            favoritePostButton.setImageResource(R.drawable.add_favorite_post_ic);
                        }
                    }
                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {}
                });
            } else {
                likeButtonIc.setImageResource(R.drawable.post_icons_1_1);
                favoritePostButton.setImageResource(R.drawable.add_favorite_post_ic);
            }

            getCommentsCount.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    _setCount(commentsButtonCount, dataSnapshot.getChildrenCount());
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });
            getLikesCount.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    long count = dataSnapshot.getChildrenCount();
                    _setCount(likeButtonCount, count);
                    postLikeCountCache.put(_data.get(_position).get("key").toString(), String.valueOf(count));
                }
                @Override
                public void onCancelled(DatabaseError databaseError) {}
            });

            likeButton.setOnClickListener(_view1 -> {
                FirebaseUser latestUser = FirebaseAuth.getInstance().getCurrentUser();
                if (latestUser != null) {
                    DatabaseReference likeRef = _firebase.getReference("skyline/posts-likes").child(_data.get(_position).get("key").toString()).child(latestUser.getUid());
                    likeRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()) {
                                likeRef.removeValue();
                                double currentLikes = Double.parseDouble(postLikeCountCache.get(_data.get(_position).get("key").toString()).toString());
                                postLikeCountCache.put(_data.get(_position).get("key").toString(), String.valueOf((long)(currentLikes - 1)));
                                _setCount(likeButtonCount, currentLikes - 1);
                                likeButtonIc.setImageResource(R.drawable.post_icons_1_1);
                            } else {
                                likeRef.setValue(latestUser.getUid());
                                com.synapse.social.studioasinc.util.NotificationUtils.sendPostLikeNotification(_data.get(_position).get("key").toString(), _data.get(_position).get("uid").toString());
                                double currentLikes = Double.parseDouble(postLikeCountCache.get(_data.get(_position).get("key").toString()).toString());
                                postLikeCountCache.put(_data.get(_position).get("key").toString(), String.valueOf((long)(currentLikes + 1)));
                                _setCount(likeButtonCount, currentLikes + 1);
                                likeButtonIc.setImageResource(R.drawable.post_icons_1_2);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {}
                    });
                    vbr.vibrate((long)(24));
                } else {
                    Toast.makeText(getContext(), "Please log in to like posts", Toast.LENGTH_SHORT).show();
                }
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
                FirebaseUser latestUser = FirebaseAuth.getInstance().getCurrentUser();
                if (latestUser != null) {
                    DatabaseReference favoriteRef = _firebase.getReference("skyline/favorite-posts").child(latestUser.getUid()).child(_data.get(_position).get("key").toString());
                    favoriteRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                            if(dataSnapshot.exists()) {
                                favoriteRef.removeValue();
                                favoritePostButton.setImageResource(R.drawable.add_favorite_post_ic);
                            } else {
                                favoriteRef.setValue(_data.get(_position).get("key").toString());
                                favoritePostButton.setImageResource(R.drawable.delete_favorite_post_ic);
                            }
                        }
                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {}
                    });
                    vbr.vibrate((long)(24));
                } else {
                    Toast.makeText(getContext(), "Please log in to save posts", Toast.LENGTH_SHORT).show();
                }
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

    private void pickStoryMedia() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/* video/*");
        startActivityForResult(intent, PICK_STORY_MEDIA_REQUEST);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_STORY_MEDIA_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            Uri mediaUri = data.getData();
            uploadStory(mediaUri);
        }
    }

    private void uploadStory(Uri mediaUri) {
        if (mediaUri == null) {
            return;
        }

        String filePath = com.synapse.social.studioasinc.StorageUtil.getPathFromUri(getContext(), mediaUri);
        String fileName = com.synapse.social.studioasinc.StorageUtil.getFileName(getContext(), mediaUri);
        String fileType = getContext().getContentResolver().getType(mediaUri);

        if (filePath == null || fileName == null) {
            Toast.makeText(getContext(), "Failed to get file path", Toast.LENGTH_SHORT).show();
            return;
        }

        com.synapse.social.studioasinc.UploadFiles.uploadFile(filePath, fileName, new com.synapse.social.studioasinc.UploadFiles.UploadCallback() {
            @Override
            public void onProgress(int percent) {
                // You can add a progress bar here if you want
            }

            @Override
            public void onSuccess(String url, String publicId) {
                saveStoryToDatabase(url, fileType != null && fileType.contains("image") ? "image" : "video");
            }

            @Override
            public void onFailure(String error) {
                Toast.makeText(getContext(), "Upload failed: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveStoryToDatabase(String url, String type) {
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference storyRef = _firebase.getReference("story").push();
        com.synapse.social.studioasinc.models.Story story = new com.synapse.social.studioasinc.models.Story(uid, url, System.currentTimeMillis(), type);
        storyRef.setValue(story).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(getContext(), "Story uploaded successfully", Toast.LENGTH_SHORT).show();
                // Refresh stories
                if (PublicPostsList.getAdapter() instanceof ConcatAdapter) {
                    ConcatAdapter concatAdapter = (ConcatAdapter) PublicPostsList.getAdapter();
                    for (RecyclerView.Adapter adapter : concatAdapter.getAdapters()) {
                        if (adapter instanceof HeaderAdapter) {
                            ((HeaderAdapter) adapter).refreshStories();
                        }
                    }
                }
            } else {
                Toast.makeText(getContext(), "Failed to save story", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void _updatePostViewVisibility(LinearLayout body, ImageView postPrivateStateIcon, String postUid, String postVisibility) {
        if ("private".equals(postVisibility)) {
            if (postUid.equals(FirebaseAuth.getInstance().getCurrentUser().getUid())) {
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
        // Add null checks to prevent NullPointerException
        Object bannedValue = UserInfoCacheMap.get("banned-".concat(postUid));
        if (bannedValue != null && bannedValue.toString().equals("true")) {
            userInfoProfileImage.setImageResource(R.drawable.banned_avatar);
        } else {
            Object avatarValue = UserInfoCacheMap.get("avatar-".concat(postUid));
            if (avatarValue == null || avatarValue.toString().equals("null") || avatarValue.toString().isEmpty()) {
                userInfoProfileImage.setImageResource(R.drawable.avatar);
            } else {
                Glide.with(getContext()).load(Uri.parse(avatarValue.toString())).into(userInfoProfileImage);
            }
        }

        Object nicknameValue = UserInfoCacheMap.get("nickname-".concat(postUid));
        Object usernameValue = UserInfoCacheMap.get("username-".concat(postUid));
        
        if (nicknameValue == null || nicknameValue.toString().equals("null") || nicknameValue.toString().isEmpty()) {
            if (usernameValue != null) {
                userInfoUsername.setText("@" + usernameValue.toString());
            } else {
                userInfoUsername.setText("@user");
            }
        } else {
            userInfoUsername.setText(nicknameValue.toString());
        }

        Object genderValue = UserInfoCacheMap.get("gender-".concat(postUid));
        if (genderValue == null || genderValue.toString().equals("hidden")) {
            userInfoGenderBadge.setVisibility(View.GONE);
        } else {
            if (genderValue.toString().equals("male")) {
                userInfoGenderBadge.setImageResource(R.drawable.male_badge);
                userInfoGenderBadge.setVisibility(View.VISIBLE);
            } else if (genderValue.toString().equals("female")) {
                userInfoGenderBadge.setImageResource(R.drawable.female_badge);
                userInfoGenderBadge.setVisibility(View.VISIBLE);
            }
        }

        Object accountTypeValue = UserInfoCacheMap.get("acc_type-".concat(postUid));
        String accountType = accountTypeValue != null ? accountTypeValue.toString() : "user";
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
