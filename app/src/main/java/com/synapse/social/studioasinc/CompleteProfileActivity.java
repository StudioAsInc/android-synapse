package com.synapse.social.studioasinc;

import android.Manifest;
import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Typeface;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import android.view.Menu;
import android.view.MenuItem;
import androidx.browser.*;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.gridlayout.*;
import com.bumptech.glide.Glide;

import com.synapse.social.studioasinc.FadeEditText;
import com.theartofdev.edmodo.cropper.*;
import com.yalantis.ucrop.*;
import java.io.*;
import java.io.File;
import java.io.InputStream;
import java.text.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;
import com.synapse.social.studioasinc.ImageUploader;
import com.onesignal.OneSignal;
import com.onesignal.user.subscriptions.IPushSubscriptionObserver;
import com.onesignal.user.subscriptions.PushSubscriptionChangedState;
import com.synapse.social.studioasinc.util.ViewUtilsKt;
import com.synapse.social.studioasinc.backend.AuthenticationService;
import com.synapse.social.studioasinc.backend.DatabaseService;
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService;
import com.synapse.social.studioasinc.backend.interfaces.ICompletionListener;
import com.synapse.social.studioasinc.backend.interfaces.IUser;


public class CompleteProfileActivity extends AppCompatActivity {

	public final int REQ_CD_SELECTAVATAR = 101;

	private IAuthenticationService authService;
	private IDatabaseService dbService;

	private boolean userNameErr = false;
	private String avatarUri = "";
	private String path = "";
	private String thedpurl = "";

	private ScrollView scroll;
	private androidx.constraintlayout.widget.ConstraintLayout body;
	private com.google.android.material.appbar.MaterialToolbar toolbar;
	private TextView title;
	private TextView subtitle;
	private CardView profile_image_card;
	private FadeEditText username_input;
	private FadeEditText nickname_input;
	private FadeEditText biography_input;
	private androidx.constraintlayout.widget.ConstraintLayout email_verification;
	private androidx.constraintlayout.widget.ConstraintLayout buttons;
	private ImageView profile_image;
	private TextView email_verification_title;
	private TextView email_verification_subtitle;
	private TextView email_verification_send;
	private ImageView email_verification_error_ic;
	private ImageView email_verification_verified_ic;
	private TextView email_verification_status;
	private ImageView email_verification_status_refresh;
	private com.google.android.material.button.MaterialButton skip_button;
	private com.google.android.material.button.MaterialButton complete_button;

	private Vibrator vbr;
	private Intent intent = new Intent();

	private Calendar getJoinTime = Calendar.getInstance();
	private Intent SelectAvatar = new Intent(Intent.ACTION_GET_CONTENT);

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_complete_profile);
		initialize(_savedInstanceState);


		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
		|| ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);} else {
			initializeLogic();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1000) {
			initializeLogic();
		}
	}

	private void initialize(Bundle _savedInstanceState) {
		scroll = findViewById(R.id.scroll);
		body = findViewById(R.id.body);
		toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setDisplayShowHomeEnabled(true);
			getSupportActionBar().setTitle("");
		}
		toolbar.setNavigationOnClickListener(v -> {
			onBackPressed();
		});
		title = findViewById(R.id.title);
		subtitle = findViewById(R.id.subtitle);
		profile_image_card = findViewById(R.id.profile_image_card);
		username_input = findViewById(R.id.username_input);
		nickname_input = findViewById(R.id.nickname_input);
		biography_input = findViewById(R.id.biography_input);
		email_verification = findViewById(R.id.email_verification);
		buttons = findViewById(R.id.buttons);
		profile_image = findViewById(R.id.profile_image);
		email_verification_title = findViewById(R.id.email_verification_title);
		email_verification_subtitle = findViewById(R.id.email_verification_subtitle);
		email_verification_send = findViewById(R.id.email_verification_send);
		email_verification_error_ic = findViewById(R.id.email_verification_error_ic);
		email_verification_verified_ic = findViewById(R.id.email_verification_verified_ic);
		email_verification_status = findViewById(R.id.email_verification_status);
		email_verification_status_refresh = findViewById(R.id.email_verification_status_refresh);
		skip_button = findViewById(R.id.skip_button);
		complete_button = findViewById(R.id.complete_button);
		vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		SelectAvatar.setType("image/*");
		SelectAvatar.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

		authService = new AuthenticationService(SynapseApp.supabaseClient);
		dbService = new DatabaseService(SynapseApp.supabaseClient);

		profile_image_card.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View _view) {
				avatarUri = "null";
				profile_image.setImageResource(R.drawable.avatar);
				vbr.vibrate((long)(48));
				return true;
			}
		});

		profile_image_card.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				startActivityForResult(SelectAvatar, REQ_CD_SELECTAVATAR);
			}
		});

		username_input.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				if (_charSeq.trim().equals("")) {
					username_input.setActivated(true);
					((EditText)username_input).setError(getResources().getString(R.string.enter_username));
					userNameErr = true;
				} else {
					if (_charSeq.matches("[a-z0-9_.]+")) {
						if (_charSeq.contains("q") || (_charSeq.contains("w") || (_charSeq.contains("e") || (_charSeq.contains("r") || (_charSeq.contains("t") || (_charSeq.contains("y") || (_charSeq.contains("u") || (_charSeq.contains("i") || (_charSeq.contains("o") || (_charSeq.contains("p") || (_charSeq.contains("a") || (_charSeq.contains("s") || (_charSeq.contains("d") || (_charSeq.contains("f") || (_charSeq.contains("g") || (_charSeq.contains("h") || (_charSeq.contains("j") || (_charSeq.contains("k") || (_charSeq.contains("l") || (_charSeq.contains("z") || (_charSeq.contains("x") || (_charSeq.contains("c") || (_charSeq.contains("v") || (_charSeq.contains("b") || (_charSeq.contains("n") || _charSeq.contains("m")))))))))))))))))))))))))) {
							if (username_input.getText().toString().length() < 3) {
								username_input.setActivated(true);
								((EditText)username_input).setError(getResources().getString(R.string.username_err_3_characters));
								userNameErr = true;
							} else {
								if (username_input.getText().toString().length() > 25) {
									username_input.setActivated(true);
									((EditText)username_input).setError(getResources().getString(R.string.username_err_25_characters));
									userNameErr = true;
								} else {
									username_input.setActivated(false);

								}
							}
						} else {
							username_input.setActivated(true);
							((EditText)username_input).setError(getResources().getString(R.string.username_err_one_letter));
							userNameErr = true;
						}
					} else {
						username_input.setActivated(true);
						((EditText)username_input).setError(getResources().getString(R.string.username_err_invalid_characters));
						userNameErr = true;
					}
				}
			}

			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

			}

			@Override
			public void afterTextChanged(Editable _param1) {

			}
		});

		nickname_input.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				if (_charSeq.length() > 30) {
					nickname_input.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFF44336, 0xFFFFFFFF));
					((EditText)nickname_input).setError(getResources().getString(R.string.nickname_err_30_characters));
				} else {
					nickname_input.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFEEEEEE, 0xFFFFFFFF));
				}
			}

			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

			}

			@Override
			public void afterTextChanged(Editable _param1) {

			}
		});

		biography_input.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {
				final String _charSeq = _param1.toString();
				if (_charSeq.length() > 250) {
					biography_input.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFF44336, 0xFFFFFFFF));
					((EditText)biography_input).setError(getResources().getString(R.string.biography_err_250_characters));
				} else {
					biography_input.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFEEEEEE, 0xFFFFFFFF));
				}
			}

			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) {

			}

			@Override
			public void afterTextChanged(Editable _param1) {

			}
		});

		skip_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				SketchwareUtil.showMessage(getApplicationContext(), "Not possible");

			}
		});

		complete_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (userNameErr) {
					SketchwareUtil.showMessage(getApplicationContext(), getResources().getString(R.string.username_err_invalid));
					vbr.vibrate((long)(48));
				} else {
					complete_button.setEnabled(false);
					complete_button.setText("Loading...");
					username_input.setEnabled(false);
					saveUserProfile();
				}
			}
		});
	}

	private void initializeLogic() {
		email_verification_title.setTypeface(Typeface.DEFAULT, 1);
		subtitle.setTypeface(Typeface.DEFAULT, 0);
		title.setTypeface(Typeface.DEFAULT, 1);
		ViewUtilsKt.setStateColor(this, 0xFFFFFFFF, 0xFFFFFFFF);
		avatarUri = "null";
		thedpurl = "null";
		userNameErr = true;
		ViewUtilsKt.setImageColor(email_verification_error_ic, 0xFFF44336);
		ViewUtilsKt.setImageColor(email_verification_verified_ic, 0xFF4CAF50);
		ViewUtilsKt.setGradientDrawable(profile_image_card, Color.TRANSPARENT, 300f, 0, Color.TRANSPARENT);
		ViewUtilsKt.setGradientDrawable(email_verification, 0xFFFFFFFF, 28f, 3, 0xFFEEEEEE);
		ViewUtilsKt.setViewGraphics(email_verification_send, 0xFF445E91, 0xFF445E91, 300, 0, Color.TRANSPARENT);
		if (getIntent().hasExtra("findedUsername")) {
			username_input.setText(getIntent().getStringExtra("findedUsername"));
		} else {
			username_input.setText("");
		}
		if (getIntent().hasExtra("googleLoginName") && (getIntent().hasExtra("googleLoginEmail") && getIntent().hasExtra("googleLoginAvatarUri"))) {
			Glide.with(getApplicationContext()).load(Uri.parse(getIntent().getStringExtra("googleLoginAvatarUri"))).into(profile_image);
			nickname_input.setText(getIntent().getStringExtra("googleLoginName"));
		}


		_font();
	}

	@Override
	protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);

		switch (_requestCode) {
			case REQ_CD_SELECTAVATAR:
			if (_resultCode == Activity.RESULT_OK) {
				ArrayList<String> _filePath = new ArrayList<>();
				if (_data != null) {
					if (_data.getClipData() != null) {
						for (int _index = 0; _index < _data.getClipData().getItemCount(); _index++) {
							ClipData.Item _item = _data.getClipData().getItemAt(_index);
							_filePath.add(FileUtil.convertUriToFilePath(getApplicationContext(), _item.getUri()));
						}
					}
					else {
						_filePath.add(FileUtil.convertUriToFilePath(getApplicationContext(), _data.getData()));
					}
				}
				profile_image.setImageBitmap(FileUtil.decodeSampleBitmapFromPath(_filePath.get((int)(0)), 1024, 1024));
				path = _filePath.get((int)(0));
				ImageUploader.uploadImage(path, new ImageUploader.UploadCallback() {
					@Override
					public void onUploadComplete(String imageUrl) {
						thedpurl = imageUrl;
					}

					@Override
					public void onUploadError(String errorMessage) {
						SketchwareUtil.showMessage(getApplicationContext(), "Something went wrong");
					}
				});

			}
			else {

			}
			break;
			default:
			break;
		}
	}

	@Override
	public void onBackPressed() {
		{
			final AlertDialog NewCustomDialog = new AlertDialog.Builder(CompleteProfileActivity.this).create();
			LayoutInflater NewCustomDialogLI = getLayoutInflater();
			View NewCustomDialogCV = (View) NewCustomDialogLI.inflate(R.layout.dialog_synapse_bg_view, null);
			NewCustomDialog.setView(NewCustomDialogCV);
			NewCustomDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

			final TextView dialog_title = (TextView) NewCustomDialogCV.findViewById(R.id.dialog_title);
			final TextView dialog_message = (TextView) NewCustomDialogCV.findViewById(R.id.dialog_message);
			final TextView dialog_no_button = (TextView) NewCustomDialogCV.findViewById(R.id.dialog_no_button);
			final TextView dialog_yes_button = (TextView) NewCustomDialogCV.findViewById(R.id.dialog_yes_button);
			dialog_yes_button.setTextColor(0xFFF44336);
			ViewUtilsKt.setViewGraphics(dialog_yes_button, 0xFFFFFFFF, 0xFFFFCDD2, 28, 0, Color.TRANSPARENT);
			dialog_no_button.setTextColor(0xFF2196F3);
			ViewUtilsKt.setViewGraphics(dialog_no_button, 0xFFFFFFFF, 0xFFBBDEFB, 28, 0, Color.TRANSPARENT);
			dialog_title.setText(getResources().getString(R.string.info));
			dialog_message.setText(getResources().getString(R.string.cancel_complete_profile_warn).concat("\n\n".concat(getResources().getString(R.string.cancel_complete_profile_warn2))));
			dialog_yes_button.setText(getResources().getString(R.string.yes));
			dialog_no_button.setText(getResources().getString(R.string.no));
			dialog_yes_button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {

					finish();
				}
			});
			dialog_no_button.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					NewCustomDialog.dismiss();
				}
			});
			NewCustomDialog.setCancelable(true);
			NewCustomDialog.show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.complete_profile_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.cancel) {
			{
				final AlertDialog NewCustomDialog = new AlertDialog.Builder(CompleteProfileActivity.this).create();
				LayoutInflater NewCustomDialogLI = getLayoutInflater();
				View NewCustomDialogCV = (View) NewCustomDialogLI.inflate(R.layout.dialog_synapse_bg_view, null);
				NewCustomDialog.setView(NewCustomDialogCV);
				NewCustomDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

				final TextView dialog_title = (TextView) NewCustomDialogCV.findViewById(R.id.dialog_title);
				final TextView dialog_message = (TextView) NewCustomDialogCV.findViewById(R.id.dialog_message);
				final TextView dialog_no_button = (TextView) NewCustomDialogCV.findViewById(R.id.dialog_no_button);
				final TextView dialog_yes_button = (TextView) NewCustomDialogCV.findViewById(R.id.dialog_yes_button);
				dialog_yes_button.setTextColor(0xFFF44336);
				ViewUtilsKt.setViewGraphics(dialog_yes_button, 0xFFFFFFFF, 0xFFFFCDD2, 28, 0, Color.TRANSPARENT);
				dialog_no_button.setTextColor(0xFF2196F3);
				ViewUtilsKt.setViewGraphics(dialog_no_button, 0xFFFFFFFF, 0xFFBBDEFB, 28, 0, Color.TRANSPARENT);
				dialog_title.setText(getResources().getString(R.string.info));
				dialog_message.setText(getResources().getString(R.string.cancel_create_account_warn).concat("\n\n".concat(getResources().getString(R.string.cancel_create_account_warn2))));
				dialog_yes_button.setText(getResources().getString(R.string.yes));
				dialog_no_button.setText(getResources().getString(R.string.no));
				dialog_yes_button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View _view) {
						item.setEnabled(false);

						NewCustomDialog.dismiss();
					}
				});
				dialog_no_button.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View _view) {
						NewCustomDialog.dismiss();
					}
				});
				NewCustomDialog.setCancelable(true);
				NewCustomDialog.show();
			}
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	public void _font() {
		title.setTypeface(Typeface.DEFAULT, 1);
	}

	private void saveUserProfile() {
		IUser currentUser = authService.getCurrentUser();
		if (currentUser == null) {
			// Handle error: user is not logged in
			SketchwareUtil.showMessage(getApplicationContext(), "Error: Not logged in.");
			return;
		}

		String userId = currentUser.getUid();
		String username = username_input.getText().toString();
		String nickname = nickname_input.getText().toString();
		String biography = biography_input.getText().toString();

		HashMap<String, Object> userProfile = new HashMap<>();
		userProfile.put("id", userId); // Assuming "id" is the primary key column in your Supabase table
		userProfile.put("username", username);
		userProfile.put("nickname", nickname);
		userProfile.put("biography", biography);
		userProfile.put("avatar_url", thedpurl); // Use thedpurl from image upload

		// Add OneSignal Player ID
		addOneSignalPlayerIdToMap(userProfile);

		dbService.setValue(dbService.getReference("users/" + userId), userProfile, (result, e) -> {
			if (e == null) {
				// Navigate to HomeActivity on success
				Intent intent = new Intent(CompleteProfileActivity.this, HomeActivity.class);
				startActivity(intent);
				finish();
			} else {
				// Handle error
				SketchwareUtil.showMessage(getApplicationContext(), "Error saving profile: " + e.getMessage());
				complete_button.setEnabled(true);
				complete_button.setText("Complete Profile");
				username_input.setEnabled(true);
			}
		});
	}

	private void addOneSignalPlayerIdToMap(HashMap<String, Object> userMap) {
		// Get current OneSignal Player ID if available
		if (OneSignal.getUser().getPushSubscription().getOptedIn()) {
			String playerId = OneSignal.getUser().getPushSubscription().getId();
			if (playerId != null && !playerId.isEmpty()) {
				userMap.put("oneSignalPlayerId", playerId);
			}
		}
	}
}