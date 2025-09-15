package com.synapse.social.studioasinc;
//Modified by StudioAs Inc. 2024
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Vibrator;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import com.bumptech.glide.Glide;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.VideoView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.os.Handler;
import android.os.Looper;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
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
import com.google.firebase.database.Query;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.HashMap;
import java.io.File;

public class LineVideosRecyclerViewAdapter extends RecyclerView.Adapter<LineVideosRecyclerViewAdapter.ViewHolder> {

	private FragmentManager postCommentsFragMng;
	private FirebaseAuth auth;
	private DatabaseReference main = FirebaseDatabase.getInstance().getReference("skyline");
	private Vibrator vbr;
	private Intent intent = new Intent();

	private Context context;
	private Handler refreshVideoProgressHandler;
	private Runnable refreshVideoProgressRunnable;
	private View viewP;

	private HashMap<String, Object> UserInfoCacheMap = new HashMap<>();
	private HashMap<String, Object> postLikeCountCache = new HashMap<>();

	private ArrayList<HashMap<String, Object>> _data;

	public LineVideosRecyclerViewAdapter(Context context, FragmentManager fragment, ArrayList<HashMap<String, Object>> _arr) {
		this.context = context;
		this.postCommentsFragMng = fragment;
		FirebaseApp.initializeApp(context);
		this.auth = FirebaseAuth.getInstance();
		this.vbr = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
		_data = _arr;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View view = inflater.inflate(R.layout.synapse_reels_cv, parent, false);
		RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
		view.setLayoutParams(lp);
		return new ViewHolder(view);
	}


	@Override
	public void onBindViewHolder(ViewHolder _holder, final int _position) {
		View _view = _holder.itemView;

		HashMap<String, Object> videoData = _data.get(_position);
		if (videoData == null) {
			return;
		}

		Object uidObj = videoData.get("uid");
		Object keyObj = videoData.get("key");
		Object videoUriObj = videoData.get("videoUri");

		if (uidObj == null || keyObj == null || videoUriObj == null) {
			return;
		}

		String uid = uidObj.toString();
		String key = keyObj.toString();
		String videoUri = videoUriObj.toString();

		DatabaseReference getUserDetails = FirebaseDatabase.getInstance().getReference("skyline/users").child(uid);
		DatabaseReference checkLike = FirebaseDatabase.getInstance().getReference("skyline/posts-likes").child(key).child(FirebaseAuth.getInstance().getCurrentUser().getUid());
		DatabaseReference getLikesCount = FirebaseDatabase.getInstance().getReference("skyline/posts-likes").child(key);
		DatabaseReference getCommentsCount = FirebaseDatabase.getInstance().getReference("skyline/posts-comments").child(key);
		DatabaseReference getShareCount = FirebaseDatabase.getInstance().getReference("skyline/posts-share").child(key);

		final RelativeLayout body = _view.findViewById(R.id.body);
		final FrameLayout middle = _view.findViewById(R.id.middle);
		final LinearLayout mediaPlayerControllers = _view.findViewById(R.id.mediaPlayerControllers);
		final LinearLayout buttonsRelative = _view.findViewById(R.id.buttonsRelative);
		final VideoView videoView = _view.findViewById(R.id.videoView);
		final ImageView playPauseButton = _view.findViewById(R.id.playPauseButton);
		final LinearLayout buttonsRelativeTop = _view.findViewById(R.id.buttonsRelativeTop);
		final LinearLayout buttonsRelativeBottom = _view.findViewById(R.id.buttonsRelativeBottom);
		final SeekBar videoSeek = _view.findViewById(R.id.videoSeek);
		final LinearLayout likeButton = _view.findViewById(R.id.likeButton);
		final LinearLayout commentsButton = _view.findViewById(R.id.commentsButton);
		final LinearLayout shareButton = _view.findViewById(R.id.shareButton);
		final LinearLayout addFavoriteButton = _view.findViewById(R.id.addFavoriteButton);
		final ImageView likeButtonIc = _view.findViewById(R.id.likeButtonIc);
		final TextView likeButtonCount = _view.findViewById(R.id.likeButtonCount);
		final ImageView commentsButtonIc = _view.findViewById(R.id.commentsButtonIc);
		final TextView commentsButtonCount = _view.findViewById(R.id.commentsButtonCount);
		final ImageView shareButtonIc = _view.findViewById(R.id.shareButtonIc);
		final TextView shareButtonCount = _view.findViewById(R.id.shareButtonCount);
		final ImageView addFavoriteButtonIc = _view.findViewById(R.id.addFavoriteButtonIc);
		final TextView addFavoriteButtonCount = _view.findViewById(R.id.addFavoriteButtonCount);
		final LinearLayout buttonsRelativeBottomUserInf = _view.findViewById(R.id.buttonsRelativeBottomUserInf);
		final TextView postDescription = _view.findViewById(R.id.postDescription);
		final androidx.cardview.widget.CardView profileCard = _view.findViewById(R.id.profileCard);
		final LinearLayout buttonsRelativeBottomUserInfMiniRight = _view.findViewById(R.id.buttonsRelativeBottomUserInfMiniRight);
		final ImageView profileCardImage = _view.findViewById(R.id.profileCardImage);
		final TextView buttonsRelativeBottomUserInfUsername = _view.findViewById(R.id.buttonsRelativeBottomUserInfUsername);
		final ImageView buttonsRelativeBottomUserInfGenderBadge = _view.findViewById(R.id.buttonsRelativeBottomUserInfGenderBadge);
		final ImageView buttonsRelativeBottomUserInfVerifiedBadge = _view.findViewById(R.id.buttonsRelativeBottomUserInfVerifiedBadge);
		final LinearLayout videoMusicInfo = _view.findViewById(R.id.videoMusicInfo);
		final ImageView videoMusicInfoIc = _view.findViewById(R.id.videoMusicInfoIc);
		final TextView videoMusicInfoTitle = _view.findViewById(R.id.videoMusicInfoTitle);

		buttonsRelativeBottomUserInf.setVisibility(View.INVISIBLE);
		mediaPlayerControllers.setVisibility(View.GONE);
		profileCard.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)300, Color.TRANSPARENT));
		playPauseButton.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)300, 0x7B000000));
		videoMusicInfo.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)0, 0x7B000000));
		_ImageColor(videoMusicInfoIc, 0xFF9E9E9E);
		_ImageColor(playPauseButton, 0xFFFFFFFF);
		_ImageColor(likeButtonIc, 0xFFFFFFFF);
		_ImageColor(commentsButtonIc, 0xFFFFFFFF);
		_ImageColor(shareButtonIc, 0xFFFFFFFF);
		_ImageColor(addFavoriteButtonIc, 0xFFFFFFFF);
		setSeekBarColor(videoSeek, 0xFFFFFFFF, 0xFFFFFFFF);

		videoView.setVideoURI(Uri.parse(videoUri));

		if (videoData.containsKey("post_text") && videoData.get("post_text") != null) {
			postDescription.setText(videoData.get("post_text").toString());
			postDescription.setVisibility(View.VISIBLE);
		} else {
			postDescription.setVisibility(View.GONE);
		}

		if (UserInfoCacheMap.containsKey("uid-".concat(uid))) {
			buttonsRelativeBottomUserInf.setVisibility(View.VISIBLE);
			if (String.valueOf(UserInfoCacheMap.get("banned-".concat(uid))).equals("true")) {
				profileCardImage.setImageResource(R.drawable.avatar);
			} else {
				if (String.valueOf(UserInfoCacheMap.get("avatar-".concat(uid))).equals("null")) {
					profileCardImage.setImageResource(R.drawable.avatar);
				} else {
					Glide.with(context).load(Uri.parse(String.valueOf(UserInfoCacheMap.get("avatar-".concat(uid))))).into(profileCardImage);
				}
			}
			if (String.valueOf(UserInfoCacheMap.get("nickname-".concat(uid))).equals("null")) {
				buttonsRelativeBottomUserInfUsername.setText("@" + String.valueOf(UserInfoCacheMap.get("username-".concat(uid))));
			} else {
				buttonsRelativeBottomUserInfUsername.setText(String.valueOf(UserInfoCacheMap.get("nickname-".concat(uid))));
			}
			if (String.valueOf(UserInfoCacheMap.get("gender-".concat(uid))).equals("hidden")) {
				buttonsRelativeBottomUserInfGenderBadge.setVisibility(View.GONE);
			} else {
				if (String.valueOf(UserInfoCacheMap.get("gender-".concat(uid))).equals("male")) {
					buttonsRelativeBottomUserInfGenderBadge.setImageResource(R.drawable.male_badge);
					buttonsRelativeBottomUserInfGenderBadge.setVisibility(View.VISIBLE);
				} else {
					if (String.valueOf(UserInfoCacheMap.get("gender-".concat(uid))).equals("female")) {
						buttonsRelativeBottomUserInfGenderBadge.setImageResource(R.drawable.female_badge);
						buttonsRelativeBottomUserInfGenderBadge.setVisibility(View.VISIBLE);
					}
				}
			}
			if (String.valueOf(UserInfoCacheMap.get("acc_type-".concat(uid))).equals("admin")) {
				buttonsRelativeBottomUserInfVerifiedBadge.setImageResource(R.drawable.admin_badge);
				buttonsRelativeBottomUserInfVerifiedBadge.setVisibility(View.VISIBLE);
			} else {
				if (String.valueOf(UserInfoCacheMap.get("acc_type-".concat(uid))).equals("moderator")) {
					buttonsRelativeBottomUserInfVerifiedBadge.setImageResource(R.drawable.moderator_badge);
					buttonsRelativeBottomUserInfVerifiedBadge.setVisibility(View.VISIBLE);
				} else {
					if (String.valueOf(UserInfoCacheMap.get("acc_type-".concat(uid))).equals("support")) {
						buttonsRelativeBottomUserInfVerifiedBadge.setImageResource(R.drawable.support_badge);
						buttonsRelativeBottomUserInfVerifiedBadge.setVisibility(View.VISIBLE);
					} else {
						if (String.valueOf(UserInfoCacheMap.get("acc_type-".concat(uid))).equals("user")) {
							if (String.valueOf(UserInfoCacheMap.get("verify-".concat(uid))).equals("true")) {
								buttonsRelativeBottomUserInfVerifiedBadge.setVisibility(View.VISIBLE);
							} else {
								buttonsRelativeBottomUserInfVerifiedBadge.setVisibility(View.GONE);
							}
						}
					}
				}
			}
		} else {
			getUserDetails.addListenerForSingleValueEvent(new ValueEventListener() {
				@Override
				public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
					if(dataSnapshot.exists()) {
						buttonsRelativeBottomUserInf.setVisibility(View.VISIBLE);
						UserInfoCacheMap.put("uid-".concat(uid), uid);
						UserInfoCacheMap.put("banned-".concat(uid), dataSnapshot.child("banned").getValue(String.class));
						UserInfoCacheMap.put("nickname-".concat(uid), dataSnapshot.child("nickname").getValue(String.class));
						UserInfoCacheMap.put("username-".concat(uid), dataSnapshot.child("username").getValue(String.class));
						UserInfoCacheMap.put("gender-".concat(uid), dataSnapshot.child("gender").getValue(String.class));
						UserInfoCacheMap.put("verify-".concat(uid), dataSnapshot.child("verify").getValue(String.class));
						UserInfoCacheMap.put("acc_type-".concat(uid), dataSnapshot.child("account_type").getValue(String.class));
						if (String.valueOf(dataSnapshot.child("banned").getValue()).equals("true")) {
							profileCardImage.setImageResource(R.drawable.avatar);
							UserInfoCacheMap.put("avatar-".concat(uid), "null");
						} else {
							UserInfoCacheMap.put("avatar-".concat(uid), dataSnapshot.child("avatar").getValue(String.class));
							if (String.valueOf(dataSnapshot.child("avatar").getValue()).equals("null")) {
								profileCardImage.setImageResource(R.drawable.avatar);
							} else {
								Glide.with(context).load(Uri.parse(String.valueOf(dataSnapshot.child("avatar").getValue()))).into(profileCardImage);
							}
						}
						if (String.valueOf(dataSnapshot.child("nickname").getValue()).equals("null")) {
							buttonsRelativeBottomUserInfUsername.setText("@" + String.valueOf(dataSnapshot.child("username").getValue()));
						} else {
							buttonsRelativeBottomUserInfUsername.setText(String.valueOf(dataSnapshot.child("nickname").getValue()));
						}
						if (String.valueOf(dataSnapshot.child("gender").getValue()).equals("hidden")) {
							buttonsRelativeBottomUserInfGenderBadge.setVisibility(View.GONE);
						} else {
							if (String.valueOf(dataSnapshot.child("gender").getValue()).equals("male")) {
								buttonsRelativeBottomUserInfGenderBadge.setImageResource(R.drawable.male_badge);
								buttonsRelativeBottomUserInfGenderBadge.setVisibility(View.VISIBLE);
							} else {
								if (String.valueOf(dataSnapshot.child("gender").getValue()).equals("female")) {
									buttonsRelativeBottomUserInfGenderBadge.setImageResource(R.drawable.female_badge);
									buttonsRelativeBottomUserInfGenderBadge.setVisibility(View.VISIBLE);
								}
							}
						}
						if (String.valueOf(dataSnapshot.child("account_type").getValue()).equals("admin")) {
							buttonsRelativeBottomUserInfVerifiedBadge.setImageResource(R.drawable.admin_badge);
							buttonsRelativeBottomUserInfVerifiedBadge.setVisibility(View.VISIBLE);
						} else {
							if (String.valueOf(dataSnapshot.child("account_type").getValue()).equals("moderator")) {
								buttonsRelativeBottomUserInfVerifiedBadge.setImageResource(R.drawable.moderator_badge);
								buttonsRelativeBottomUserInfVerifiedBadge.setVisibility(View.VISIBLE);
							} else {
								if (String.valueOf(dataSnapshot.child("account_type").getValue()).equals("support")) {
									buttonsRelativeBottomUserInfVerifiedBadge.setImageResource(R.drawable.support_badge);
									buttonsRelativeBottomUserInfVerifiedBadge.setVisibility(View.VISIBLE);
								} else {
									if (String.valueOf(dataSnapshot.child("account_type").getValue()).equals("user")) {
										if (String.valueOf(dataSnapshot.child("verify").getValue()).equals("true")) {
											buttonsRelativeBottomUserInfVerifiedBadge.setImageResource(R.drawable.verified_badge);
											buttonsRelativeBottomUserInfVerifiedBadge.setVisibility(View.VISIBLE);
										} else {
											buttonsRelativeBottomUserInfVerifiedBadge.setVisibility(View.GONE);
										}
									}
								}
							}
						}
					} else {
					}
				}
				@Override
				public void onCancelled(@NonNull DatabaseError databaseError) {

				}
			});
		}

		likeButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				checkLike.addListenerForSingleValueEvent(new ValueEventListener() {
					@Override
					public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
						try {
							if(dataSnapshot.exists()) {
								checkLike.removeValue();
								likeButtonIc.setImageResource(R.drawable.line_video_player_icons_2);
								_ImageColor(likeButtonIc, 0xFFFFFFFF);
								double currentLikes = Double.parseDouble(String.valueOf(postLikeCountCache.get(key)));
								currentLikes--;
								postLikeCountCache.put(key, String.valueOf(currentLikes));
								likeButtonCount.setText(String.valueOf((long)currentLikes));
							} else {
								checkLike.setValue(FirebaseAuth.getInstance().getCurrentUser().getUid());
								likeButtonIc.setImageResource(R.drawable.line_video_player_icons_1);
								_ImageColor(likeButtonIc, 0xFFE91E63);
								double currentLikes = Double.parseDouble(String.valueOf(postLikeCountCache.get(key)));
								currentLikes++;
								postLikeCountCache.put(key, String.valueOf(currentLikes));
								likeButtonCount.setText(String.valueOf((long)currentLikes));
							}
						} catch (Exception e) {
							// ignore
						}
					}

					@Override
					public void onCancelled(@NonNull DatabaseError databaseError) {

					}
				});
				vbr.vibrate((long)(24));
			}
		});
		commentsButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				try {
					Bundle sendPostKey = new Bundle();
					sendPostKey.putString("postKey", key);
					sendPostKey.putString("postPublisherUID", uid);
					sendPostKey.putString("postPublisherAvatar", String.valueOf(UserInfoCacheMap.get("avatar-".concat(uid))));
					PostCommentsBottomSheetDialog postCommentsBottomSheet = new PostCommentsBottomSheetDialog();
					postCommentsBottomSheet.setArguments(sendPostKey);
					postCommentsBottomSheet.show(postCommentsFragMng, postCommentsBottomSheet.getTag());
				} catch (Exception e) {
					// ignore
				}
			}
		});

		buttonsRelativeBottomUserInf.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				intent.setClass(context, ProfileActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra("uid", uid);
				context.startActivity(intent);
			}
		});

		checkLike.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
				if(dataSnapshot.exists()) {
					likeButtonIc.setImageResource(R.drawable.line_video_player_icons_1);
					_ImageColor(likeButtonIc, 0xFFE91E63);
				} else {
					likeButtonIc.setImageResource(R.drawable.line_video_player_icons_2);
					_ImageColor(likeButtonIc, 0xFFFFFFFF);
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError databaseError) {

			}
		});
		getLikesCount.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				long count = dataSnapshot.getChildrenCount();
				postLikeCountCache.put(key, String.valueOf((long)(count)));
				likeButtonCount.setText(String.valueOf((long)(count)));
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {

			}
		});
		getCommentsCount.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				long count = dataSnapshot.getChildrenCount();
				commentsButtonCount.setText(String.valueOf((long)(count)));
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {

			}
		});
		getShareCount.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(DataSnapshot dataSnapshot) {
				long count = dataSnapshot.getChildrenCount();
				shareButtonCount.setText(String.valueOf((long)(count)));
			}

			@Override
			public void onCancelled(DatabaseError databaseError) {

			}
		});
	}

	@Override
	public int getItemCount() {
		return _data.size();
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		public ViewHolder(View _view) {
			super(_view);

			final FrameLayout middle = _view.findViewById(R.id.middle);
			final LinearLayout mediaPlayerControllers = _view.findViewById(R.id.mediaPlayerControllers);
			final VideoView videoView = _view.findViewById(R.id.videoView);
			final SeekBar videoSeek = _view.findViewById(R.id.videoSeek);

			videoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mp) {
					mp.setLooping(true);
					if (mp.isPlaying()) {
						mp.pause();
						mediaPlayerControllers.setVisibility(View.VISIBLE);
					} else {
						mp.start();
						mediaPlayerControllers.setVisibility(View.GONE);
					}

					videoSeek.setMax((int)videoView.getDuration());
					videoSeek.setProgress((int)0);

					videoSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

						private int progress;

						@Override
						public void onProgressChanged(SeekBar _param1, int _param2, boolean _param3) {
							final int _progressValue = _param2;
							progress = _progressValue;
						}

						@Override
						public void onStartTrackingTouch(SeekBar _param1) {

						}

						@Override
						public void onStopTrackingTouch(SeekBar _param2) {
							videoView.seekTo(progress);
						}
					});

					if (refreshVideoProgressHandler != null && refreshVideoProgressRunnable != null) {
						refreshVideoProgressHandler.removeCallbacks(refreshVideoProgressRunnable);
						refreshVideoProgressHandler = null;
						refreshVideoProgressRunnable = null;
					}

					refreshVideoProgressHandler = new Handler(Looper.getMainLooper());
					refreshVideoProgressRunnable = new Runnable() {
						@Override
						public void run() {
							videoSeek.setProgress((int)videoView.getCurrentPosition());
							refreshVideoProgressHandler.postDelayed(refreshVideoProgressRunnable, 500);
						}
					};
					refreshVideoProgressHandler.post(refreshVideoProgressRunnable);
				}
			});

			videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
				@Override
				public boolean onError(MediaPlayer mp, int what, int extra) {
					return true;
				}
			});


			middle.setOnClickListener(new View.OnClickListener(){
				@Override
				public void onClick(View _clickedView){
					if (videoView.isPlaying()) {
						videoView.pause();
						mediaPlayerControllers.setVisibility(View.VISIBLE);
					} else {
						videoView.start();
						mediaPlayerControllers.setVisibility(View.GONE);
					}
				}
			});
		}
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

	public void setSeekBarColor(@NonNull SeekBar mSeekBar, int mProgressColor, int mThumbColor) {
		mSeekBar.getProgressDrawable().setColorFilter(mProgressColor, PorterDuff.Mode.SRC_IN);
		mSeekBar.getThumb().setColorFilter(mThumbColor, PorterDuff.Mode.SRC_IN);
	}
}
