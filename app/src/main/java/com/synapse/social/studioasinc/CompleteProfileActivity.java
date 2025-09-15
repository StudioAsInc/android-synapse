package com.synapse.social.studioasinc;

// AuthStateManager removed - using Firebase Auth only
import com.synapse.social.studioasinc.database.DatabaseStructure;
import com.synapse.social.studioasinc.database.ChatManager;
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
import androidx.browser.*;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.gridlayout.*;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
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
import com.google.firebase.database.Query;
import com.synapse.social.studioasinc.ImageUploader;
import com.synapse.social.studioasinc.OneSignalManager;


// E2EE disabled - import removed

public class CompleteProfileActivity extends AppCompatActivity {

	public final int REQ_CD_SELECTAVATAR = 101;

	private FirebaseDatabase _firebase = FirebaseDatabase.getInstance();
	private FirebaseStorage _firebase_storage = FirebaseStorage.getInstance();

	private boolean userNameErr = false;
	private String avatarUri = ""; // This variable seems unused after refactoring, can be removed if confirmed.
	private String thedpurl = "null"; // Stores the URL of the uploaded profile image
	
	private HashMap<String, Object> createUserMap = new HashMap<>();
	private boolean emailVerify = false; // This variable seems unused after refactoring, can be removed if confirmed.
	private HashMap<String, Object> map = new HashMap<>(); // Used for pushusername

	private String path = ""; // Path of the selected image file

	private ScrollView scroll;
	private LinearLayout body;
	private LinearLayout top;
	private TextView title;
	private TextView subtitle;
	private CardView profile_image_card;
	private FadeEditText username_input;
	private FadeEditText nickname_input;
	private FadeEditText biography_input;
	private LinearLayout email_verification;
	private LinearLayout buttons;
	private ImageView back;
	private LinearLayout topMiddle;
	private ImageView cancelCreateAccount;
	private ProgressBar cancel_create_account_progress;
	private ImageView profile_image;
	private TextView email_verification_title;
	private TextView email_verification_subtitle;
	private LinearLayout email_verification_middle;
	private TextView email_verification_send;
	private ImageView email_verification_error_ic;
	private ImageView email_verification_verified_ic;
	private TextView email_verification_status;
	private ImageView email_verification_status_refresh;
	private TextView skip_button;
	private LinearLayout complete_button;
	private TextView complete_button_title;
	private ProgressBar complete_button_loader_bar;

	private Vibrator vbr;
	private FirebaseAuth auth;
	private OnCompleteListener<Void> auth_emailVerificationSentListener;
	private OnCompleteListener<Void> auth_deleteUserListener;
	private Intent intent = new Intent();
	private DatabaseReference main = _firebase.getReference("skyline");
	private DatabaseReference pushusername = _firebase.getReference("synapse/username");
	private Calendar getJoinTime = Calendar.getInstance();
	private Intent SelectAvatar = new Intent(Intent.ACTION_GET_CONTENT);

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.complete_profile);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);

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
		top = findViewById(R.id.top);
		title = findViewById(R.id.title);
		subtitle = findViewById(R.id.subtitle);
		profile_image_card = findViewById(R.id.profile_image_card);
		username_input = findViewById(R.id.username_input);
		nickname_input = findViewById(R.id.nickname_input);
		biography_input = findViewById(R.id.biography_input);
		email_verification = findViewById(R.id.email_verification);
		buttons = findViewById(R.id.buttons);
		back = findViewById(R.id.back);
		topMiddle = findViewById(R.id.topMiddle);
		cancelCreateAccount = findViewById(R.id.cancelCreateAccount);
		cancel_create_account_progress = findViewById(R.id.cancel_create_account_progress);
		profile_image = findViewById(R.id.profile_image);
		email_verification_title = findViewById(R.id.email_verification_title);
		email_verification_subtitle = findViewById(R.id.email_verification_subtitle);
		email_verification_middle = findViewById(R.id.email_verification_middle);
		email_verification_send = findViewById(R.id.email_verification_send);
		email_verification_error_ic = findViewById(R.id.email_verification_error_ic);
		email_verification_verified_ic = findViewById(R.id.email_verification_verified_ic);
		email_verification_status = findViewById(R.id.email_verification_status);
		email_verification_status_refresh = findViewById(R.id.email_verification_status_refresh);
		skip_button = findViewById(R.id.skip_button);
		complete_button = findViewById(R.id.complete_button);
		complete_button_title = findViewById(R.id.complete_button_title);
		complete_button_loader_bar = findViewById(R.id.complete_button_loader_bar);
		vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		auth = FirebaseAuth.getInstance();
		SelectAvatar.setType("image/*");
		SelectAvatar.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);

		profile_image_card.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View _view) {
				thedpurl = "null";
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
				final String _charSeq = _param1.toString().trim();
				if (_charSeq.isEmpty()) {
					_setUsernameError(getResources().getString(R.string.enter_username));
				} else {
					if (!_charSeq.matches("[a-z0-9_.]+")) {
						_setUsernameError(getResources().getString(R.string.username_err_invalid_characters));
					} else if (!_charSeq.matches(".*[a-z]+.*")) { // Check for at least one letter
						_setUsernameError(getResources().getString(R.string.username_err_one_letter));
					} else if (_charSeq.length() < 3) {
						_setUsernameError(getResources().getString(R.string.username_err_3_characters));
					} else if (_charSeq.length() > 25) {
						_setUsernameError(getResources().getString(R.string.username_err_25_characters));
					} else {
						username_input.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFEEEEEE, 0xFFFFFFFF));
						((EditText)username_input).setError(null);
						userNameErr = false;

						DatabaseReference checkUsernameRef = FirebaseDatabase.getInstance().getReference().child("skyline/users");
						Query checkUsernameQuery = checkUsernameRef.orderByChild("username").equalTo(_charSeq);
						checkUsernameQuery.addListenerForSingleValueEvent(new ValueEventListener() {
							@Override
							public void onDataChange(DataSnapshot dataSnapshot) {
								if (dataSnapshot.exists()) {
									_setUsernameError(getResources().getString(R.string.username_err_already_taken));
								} else {
									username_input.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFEEEEEE, 0xFFFFFFFF));
									((EditText)username_input).setError(null);
									userNameErr = false;
								}
							}

							@Override
							public void onCancelled(DatabaseError databaseError) {
								Log.e("CompleteProfileActivity", "Firebase username check cancelled: " + databaseError.getMessage(), databaseError.toException());
								SketchwareUtil.showMessage(getApplicationContext(), "Error checking username: " + databaseError.getMessage());
								_setUsernameError(getResources().getString(R.string.something_went_wrong));
							}
						});
					}
				}
			}

			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) { }

			@Override
			public void afterTextChanged(Editable _param1) { }
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
					((EditText)nickname_input).setError(null);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) { }

			@Override
			public void afterTextChanged(Editable _param1) { }
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
					((EditText)biography_input).setError(null);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence _param1, int _param2, int _param3, int _param4) { }

			@Override
			public void afterTextChanged(Editable _param1) { }
		});

		back.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				onBackPressed();
			}
		});

		cancelCreateAccount.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				showConfirmationDialog(
					getResources().getString(R.string.info),
					getResources().getString(R.string.cancel_create_account_warn).concat("\n\n".concat(getResources().getString(R.string.cancel_create_account_warn2))),
					getResources().getString(R.string.yes),
					getResources().getString(R.string.no),
					() -> {
						cancelCreateAccount.setVisibility(View.GONE);
						cancel_create_account_progress.setVisibility(View.VISIBLE);
						FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
						if (currentUser != null) {
							currentUser.delete().addOnCompleteListener(auth_deleteUserListener);
						} else {
							Log.e("CompleteProfileActivity", "Current user is null when trying to delete account.");
							SketchwareUtil.showMessage(getApplicationContext(), "No user logged in to delete.");
							cancelCreateAccount.setVisibility(View.VISIBLE);
							cancel_create_account_progress.setVisibility(View.GONE);
						}
					},
					() -> {}
				);
			}
		});

		email_verification_send.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
				if (currentUser != null) {
					currentUser.sendEmailVerification().addOnCompleteListener(auth_emailVerificationSentListener);
				} else {
					Log.e("CompleteProfileActivity", "Current user is null when trying to send email verification.");
					SketchwareUtil.showMessage(getApplicationContext(), "No user logged in to send verification email.");
				}
			}
		});

		email_verification_status_refresh.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
				if (currentUser != null) {
					currentUser.reload().addOnCompleteListener(task -> {
						if (task.isSuccessful()) {
							updateEmailVerificationUI(FirebaseAuth.getInstance().getCurrentUser());
						} else {
							Log.e("CompleteProfileActivity", "Failed to reload user for email verification status: " + task.getException().getMessage(), task.getException());
							SketchwareUtil.showMessage(getApplicationContext(), "Failed to refresh email status: " + task.getException().getMessage());
						}
					});
				} else {
					Log.e("CompleteProfileActivity", "Current user is null when trying to refresh email verification status.");
					SketchwareUtil.showMessage(getApplicationContext(), "No user logged in to refresh email status.");
				}
			}
		});

		complete_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (userNameErr) {
					SketchwareUtil.showMessage(getApplicationContext(), getResources().getString(R.string.username_err_invalid));
					vbr.vibrate((long)(48));
				} else {
					_pushdata();
				}
			}
		});

		auth_emailVerificationSentListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
				if (_success) {
					showInfoDialog(
						getResources().getString(R.string.info),
						getResources().getString(R.string.email_verification_success_text),
						getResources().getString(R.string.okay),
						() -> {
							FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
							if (currentUser != null) {
								currentUser.reload().addOnCompleteListener(task -> {
									if (task.isSuccessful()) {
										updateEmailVerificationUI(FirebaseAuth.getInstance().getCurrentUser());
									} else {
										Log.e("CompleteProfileActivity", "Failed to reload user after sending verification email: " + task.getException().getMessage(), task.getException());
									}
								});
							}
						}
					);
				} else {
					Log.e("CompleteProfileActivity", "Failed to send email verification: " + _errorMessage, _param1.getException());
					SketchwareUtil.showMessage(getApplicationContext(), _errorMessage);
				}
			}
		};

		auth_deleteUserListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";
				if (_success) {
					intent.setClass(getApplicationContext(), MainActivity.class);
					startActivity(intent);
					finish();
				} else {
					Log.e("CompleteProfileActivity", "Failed to delete user: " + _errorMessage, _param1.getException());
					SketchwareUtil.showMessage(getApplicationContext(), _errorMessage);
				}
				cancelCreateAccount.setVisibility(View.VISIBLE);
				cancel_create_account_progress.setVisibility(View.GONE);
			}
		};
	}

	private void initializeLogic() {
		email_verification_title.setTypeface(Typeface.DEFAULT, 1);
		subtitle.setTypeface(Typeface.DEFAULT, 0);
		title.setTypeface(Typeface.DEFAULT, 1);
		_stateColor(0xFFFFFFFF, 0xFFFFFFFF);
		thedpurl = "null";
		userNameErr = true;
		_viewGraphics(back, 0xFFFFFFFF, 0xFFEEEEEE, 300, 0, Color.TRANSPARENT);
		_viewGraphics(cancelCreateAccount, 0xFFFFFFFF, 0xFFFFCDD2, 300, 0, Color.TRANSPARENT);
		_ImageColor(email_verification_error_ic, 0xFFF44336);
		_ImageColor(email_verification_verified_ic, 0xFF4CAF50);
		profile_image_card.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b) { this.setCornerRadius(a); this.setColor(b); return this; } }.getIns((int)300, Color.TRANSPARENT));
		email_verification.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFEEEEEE, 0xFFFFFFFF));
		complete_button_loader_bar.setVisibility(View.GONE);
		cancel_create_account_progress.setVisibility(View.GONE);
		username_input.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFEEEEEE, 0xFFFFFFFF));
		nickname_input.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFEEEEEE, 0xFFFFFFFF));
		biography_input.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFEEEEEE, 0xFFFFFFFF));
		_progressBarColor(complete_button_loader_bar, 0xFFFFFFFF);
		_progressBarColor(cancel_create_account_progress, 0xFF000000);
		_viewGraphics(email_verification_send, 0xFF445E91, 0xFF445E91, 300, 0, Color.TRANSPARENT);
		_viewGraphics(skip_button, 0xFFFFFFFF, 0xFFEEEEEE, 300, 3, 0xFFEEEEEE);
		_viewGraphics(complete_button, 0xFF445E91, 0xFF445E91, 300, 0, Color.TRANSPARENT);
		if (getIntent().hasExtra("findedUsername")) {
			username_input.setText(getIntent().getStringExtra("findedUsername"));
		} else {
			username_input.setText("");
		}
		if (getIntent().hasExtra("googleLoginName") && (getIntent().hasExtra("googleLoginEmail") && getIntent().hasExtra("googleLoginAvatarUri"))) {
			Glide.with(getApplicationContext()).load(Uri.parse(getIntent().getStringExtra("googleLoginAvatarUri"))).into(profile_image);
			nickname_input.setText(getIntent().getStringExtra("googleLoginName"));
		}
		
		try {
			FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
			if (currentUser != null) {
				currentUser.reload().addOnCompleteListener(task -> {
					if (task.isSuccessful()) {
						updateEmailVerificationUI(FirebaseAuth.getInstance().getCurrentUser());
					} else {
						Log.e("CompleteProfileActivity", "Failed to reload user in initializeLogic: " + task.getException().getMessage(), task.getException());
						SketchwareUtil.showMessage(getApplicationContext(), getResources().getString(R.string.something_went_wrong));
					}
				});
			} else {
				Log.w("CompleteProfileActivity", "Current user is null in initializeLogic. Cannot update email verification UI.");
				email_verification.setVisibility(View.GONE);
			}
		} catch (Exception e) {
			Log.e("CompleteProfileActivity", "Error during initial email verification check: " + e.getMessage(), e);
			SketchwareUtil.showMessage(getApplicationContext(), getResources().getString(R.string.something_went_wrong));
		}

		_font();
	}

	@Override
	protected void onActivityResult(int _requestCode, int _resultCode, Intent _data) {
		super.onActivityResult(_requestCode, _resultCode, _data);

		if (_requestCode == REQ_CD_SELECTAVATAR && _resultCode == Activity.RESULT_OK) {
			Uri selectedImageUri = null;
			if (_data != null) {
				if (_data.getClipData() != null) {
					if (_data.getClipData().getItemCount() > 0) {
						selectedImageUri = _data.getClipData().getItemAt(0).getUri();
					}
				} else {
					selectedImageUri = _data.getData();
				}
			}

			if (selectedImageUri != null) {
				path = FileUtil.convertUriToFilePath(getApplicationContext(), selectedImageUri);
				Glide.with(getApplicationContext()).load(selectedImageUri).into(profile_image);

				ImageUploader.uploadImage(path, new ImageUploader.UploadCallback() {
					@Override
					public void onUploadComplete(String uploadedImageUrl) {
						thedpurl = uploadedImageUrl;
						FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
						if (currentUser != null) {
							HashMap<String, Object> avatarUpdateMap = new HashMap<>();
							avatarUpdateMap.put("avatar", uploadedImageUrl);
							avatarUpdateMap.put("avatar_history_type", "local");

							main.child("users").child(currentUser.getUid()).updateChildren(avatarUpdateMap, new DatabaseReference.CompletionListener() {
								@Override
								public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
									if (databaseError == null) {
										Log.d("CompleteProfileActivity", "Avatar URL updated in Firebase successfully.");
									} else {
										Log.e("CompleteProfileActivity", "Failed to update avatar URL in Firebase: " + databaseError.getMessage(), databaseError.toException());
										SketchwareUtil.showMessage(getApplicationContext(), "Failed to save avatar: " + databaseError.getMessage());
									}
								}
							});
						} else {
							Log.e("CompleteProfileActivity", "Current user is null after image upload. Cannot update avatar in Firebase.");
							SketchwareUtil.showMessage(getApplicationContext(), "User not logged in. Cannot save avatar.");
						}
					}

					@Override
					public void onUploadError(String errorMessage) {
						Log.e("CompleteProfileActivity", "Image upload failed: " + errorMessage);
						SketchwareUtil.showMessage(getApplicationContext(), "Something went wrong during image upload: " + errorMessage);
						thedpurl = "null";
						profile_image.setImageResource(R.drawable.avatar);
					}
				});
			} else {
				SketchwareUtil.showMessage(getApplicationContext(), "No image selected.");
			}
		}
	}

	@Override
	public void onBackPressed() {
		showConfirmationDialog(
			getResources().getString(R.string.info),
			getResources().getString(R.string.cancel_complete_profile_warn).concat("\n\n".concat(getResources().getString(R.string.cancel_complete_profile_warn2))),
			getResources().getString(R.string.yes),
			getResources().getString(R.string.no),
			() -> {
				FirebaseAuth.getInstance().signOut();
				finish();
			},
			() -> {}
		);
	}


	@Override
	public void onStart() {
		super.onStart();
		try {
			FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
			if (currentUser != null) {
				currentUser.reload().addOnCompleteListener(task -> {
					if (task.isSuccessful()) {
						updateEmailVerificationUI(FirebaseAuth.getInstance().getCurrentUser());
					} else {
						Log.e("CompleteProfileActivity", "Failed to reload user in onStart: " + task.getException().getMessage(), task.getException());
						SketchwareUtil.showMessage(getApplicationContext(), getResources().getString(R.string.something_went_wrong));
					}
				});
			} else {
				Log.w("CompleteProfileActivity", "Current user is null in onStart. Cannot update email verification UI.");
				email_verification.setVisibility(View.GONE);
			}
		} catch (Exception e) {
			Log.e("CompleteProfileActivity", "Error during onStart email verification check: " + e.getMessage(), e);
			SketchwareUtil.showMessage(getApplicationContext(), getResources().getString(R.string.something_went_wrong));
		}
	}

	// Helper method to set username error state
	private void _setUsernameError(String errorMessage) {
		username_input.setBackground(new GradientDrawable() { public GradientDrawable getIns(int a, int b, int c, int d) { this.setCornerRadius(a); this.setStroke(b, c); this.setColor(d); return this; } }.getIns((int)28, (int)3, 0xFFF44336, 0xFFFFFFFF));
		((EditText)username_input).setError(errorMessage);
		userNameErr = true;
	}

	// Helper method for email verification UI update
	private void updateEmailVerificationUI(FirebaseUser user) {
		if (user != null) {
			if (user.isEmailVerified()) {
				email_verification_status_refresh.setVisibility(View.GONE);
				email_verification_error_ic.setVisibility(View.GONE);
				email_verification_verified_ic.setVisibility(View.VISIBLE);
				email_verification_status.setTextColor(0xFF4CAF50);
				email_verification_status.setText(getResources().getString(R.string.email_verified));
				email_verification_send.setVisibility(View.GONE);
			} else {
				email_verification_status_refresh.setVisibility(View.VISIBLE);
				email_verification_error_ic.setVisibility(View.VISIBLE);
				email_verification_verified_ic.setVisibility(View.GONE);
				email_verification_status.setTextColor(0xFFF44336);
				email_verification_status.setText(getResources().getString(R.string.email_not_verified));
				email_verification_send.setVisibility(View.VISIBLE);
			}
		} else {
			email_verification_status_refresh.setVisibility(View.GONE);
			email_verification_error_ic.setVisibility(View.VISIBLE);
			email_verification_verified_ic.setVisibility(View.GONE);
			email_verification_status.setTextColor(0xFFF44336);
			email_verification_status.setText("User not available or logged out.");
			email_verification_send.setVisibility(View.GONE);
		}
	}

	// Helper method for custom AlertDialog with Yes/No
	private void showConfirmationDialog(String title, String message, String positiveText, String negativeText, Runnable onPositive, Runnable onNegative) {
		final AlertDialog NewCustomDialog = new AlertDialog.Builder(CompleteProfileActivity.this).create();
		LayoutInflater NewCustomDialogLI = getLayoutInflater();
		View NewCustomDialogCV = NewCustomDialogLI.inflate(R.layout.synapse_dialog_bg_view, null);
		NewCustomDialog.setView(NewCustomDialogCV);
		NewCustomDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		final TextView dialog_title = NewCustomDialogCV.findViewById(R.id.dialog_title);
		final TextView dialog_message = NewCustomDialogCV.findViewById(R.id.dialog_message);
		final TextView dialog_no_button = NewCustomDialogCV.findViewById(R.id.dialog_no_button);
		final TextView dialog_yes_button = NewCustomDialogCV.findViewById(R.id.dialog_yes_button);

		dialog_yes_button.setTextColor(0xFFF44336);
		_viewGraphics(dialog_yes_button, 0xFFFFFFFF, 0xFFFFCDD2, 28, 0, Color.TRANSPARENT);
		dialog_no_button.setTextColor(0xFF2196F3);
		_viewGraphics(dialog_no_button, 0xFFFFFFFF, 0xFFBBDEFB, 28, 0, Color.TRANSPARENT);

		dialog_title.setText(title);
		dialog_message.setText(message);
		dialog_yes_button.setText(positiveText);
		dialog_no_button.setText(negativeText);

		dialog_yes_button.setOnClickListener(_view -> {
			onPositive.run();
			NewCustomDialog.dismiss();
		});
		dialog_no_button.setOnClickListener(_view -> {
			onNegative.run();
			NewCustomDialog.dismiss();
		});
		NewCustomDialog.setCancelable(true);
		NewCustomDialog.show();
	}

	// Helper method for custom AlertDialog with only OK button
	private void showInfoDialog(String title, String message, String positiveText, Runnable onPositive) {
		final AlertDialog NewCustomDialog = new AlertDialog.Builder(CompleteProfileActivity.this).create();
		LayoutInflater NewCustomDialogLI = getLayoutInflater();
		View NewCustomDialogCV = NewCustomDialogLI.inflate(R.layout.synapse_dialog_bg_view, null);
		NewCustomDialog.setView(NewCustomDialogCV);
		NewCustomDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

		final TextView dialog_title = NewCustomDialogCV.findViewById(R.id.dialog_title);
		final TextView dialog_message = NewCustomDialogCV.findViewById(R.id.dialog_message);
		final TextView dialog_no_button = NewCustomDialogCV.findViewById(R.id.dialog_no_button);
		final TextView dialog_yes_button = NewCustomDialogCV.findViewById(R.id.dialog_yes_button);

		dialog_no_button.setVisibility(View.GONE);
		dialog_yes_button.setTextColor(0xFF2196F3);
		_viewGraphics(dialog_yes_button, 0xFFFFFFFF, 0xFFBBDEFB, 28, 0, Color.TRANSPARENT);

		dialog_title.setText(title);
		dialog_message.setText(message);
		dialog_yes_button.setText(positiveText);

		dialog_yes_button.setOnClickListener(_view -> {
			onPositive.run();
			NewCustomDialog.dismiss();
		});
		NewCustomDialog.setCancelable(true);
		NewCustomDialog.show();
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


	public void _progressBarColor(final ProgressBar _progressbar, final int _color) {
		int color = _color;
		_progressbar.setIndeterminateTintList(ColorStateList.valueOf(color));
		_progressbar.setProgressTintList(ColorStateList.valueOf(color));
	}


	public void _font() {
		title.setTypeface(Typeface.DEFAULT, 1);
	}


	public void _pushdata() {
		if (userNameErr) {
			SketchwareUtil.showMessage(getApplicationContext(), getResources().getString(R.string.username_err_invalid));
			vbr.vibrate((long)(48));
			return;
		}

		complete_button_title.setVisibility(View.GONE);
		complete_button_loader_bar.setVisibility(View.VISIBLE);
		username_input.setEnabled(false);

		FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
		if (currentUser == null) {
			Log.e("CompleteProfileActivity", "Current user is null when trying to push data.");
			SketchwareUtil.showMessage(getApplicationContext(), "User not logged in. Please log in again.");
			complete_button_title.setVisibility(View.VISIBLE);
			complete_button_loader_bar.setVisibility(View.GONE);
			username_input.setEnabled(true);
			return;
		}

		getJoinTime = Calendar.getInstance();
		createUserMap.clear();

		// Core user data
		createUserMap.put("uid", currentUser.getUid());
		createUserMap.put("username", username_input.getText().toString().trim());
		String nickname = nickname_input.getText().toString().trim();
		createUserMap.put("nickname", nickname.isEmpty() ? username_input.getText().toString().trim() : nickname);
		createUserMap.put("email", currentUser.getEmail());
		
		// Account status
		if ("mashikahamed0@gmail.com".equals(currentUser.getEmail())) {
			createUserMap.put("account_type", "admin");
			createUserMap.put("is_premium", true);
			createUserMap.put("is_verified", true);
		} else {
			createUserMap.put("account_type", "user");
			createUserMap.put("is_premium", false);
			createUserMap.put("is_verified", currentUser.isEmailVerified());
		}
		createUserMap.put("account_status", "active");
		createUserMap.put("premium_expiry", null);
		
		// Profile data
		String biography = biography_input.getText().toString().trim();
		createUserMap.put("biography", biography.isEmpty() ? "" : biography);
		createUserMap.put("gender", "hidden");
		createUserMap.put("location", "");
		createUserMap.put("website_url", "");
		createUserMap.put("join_date", com.google.firebase.database.ServerValue.TIMESTAMP);
		
		// Avatar and cover
		String finalAvatarUrl = "";
		if (!thedpurl.equals("null")) {
			finalAvatarUrl = thedpurl;
		} else if (getIntent().hasExtra("googleLoginAvatarUri")) {
			finalAvatarUrl = getIntent().getStringExtra("googleLoginAvatarUri");
		}
		createUserMap.put("avatar_url", finalAvatarUrl.isEmpty() || finalAvatarUrl.equals("null") ? "" : finalAvatarUrl);
		createUserMap.put("cover_url", "");

		// Initialize sub-objects
		Map<String, Object> presence = new HashMap<>();
		presence.put("status", "online");
		presence.put("last_seen", com.google.firebase.database.ServerValue.TIMESTAMP);
		presence.put("activity", "Setting up profile");
		createUserMap.put("presence", presence);
		
		Map<String, Object> settings = new HashMap<>();
		settings.put("privacy_profile", "public");
		settings.put("privacy_activity", "public");
		settings.put("language", "en");
		settings.put("theme", "light");
		settings.put("notifications_enabled", true);
		createUserMap.put("settings", settings);
		
		Map<String, Object> counters = new HashMap<>();
		counters.put("posts", 0);
		counters.put("reels", 0);
		counters.put("followers", 0);
		counters.put("following", 0);
		counters.put("groups", 0);
		counters.put("reel_views", 0);
		createUserMap.put("counters", counters);
		
		main.child("users").child(currentUser.getUid()).updateChildren(createUserMap, new DatabaseReference.CompletionListener() {
			@Override
			public void onComplete(DatabaseError databaseError, DatabaseReference databaseReference) {
				if (databaseError == null) {
					// Update username index (new structure just stores uid)
					pushusername.child(username_input.getText().toString().trim().toLowerCase()).setValue(currentUser.getUid());

					// Login to OneSignal
					OneSignalManager.loginUser(currentUser.getUid());

					// Authentication handled by Firebase Auth only - no local storage

					// E2EE disabled - proceed directly to HomeActivity
					intent.setClass(getApplicationContext(), HomeActivity.class);
					startActivity(intent);
					finish();
				} else {
					Log.e("CompleteProfileActivity", "Failed to push user data to Firebase: " + databaseError.getMessage(), databaseError.toException());
					complete_button_title.setVisibility(View.VISIBLE);
					complete_button_loader_bar.setVisibility(View.GONE);
					username_input.setEnabled(true);

					if ("Permission denied".equals(databaseError.getMessage())) {
						SketchwareUtil.showMessage(getApplicationContext(), "Permission denied. Please ensure your email is verified.");
					} else {
						SketchwareUtil.showMessage(getApplicationContext(), databaseError.getMessage());
					}
				}
			}
		});
	}
}