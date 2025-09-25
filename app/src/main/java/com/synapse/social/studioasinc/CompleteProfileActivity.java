package com.synapse.social.studioasinc;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.synapse.social.studioasinc.backend.AuthenticationService;
import com.synapse.social.studioasinc.backend.DatabaseService;
import com.synapse.social.studioasinc.backend.QueryService;
import com.synapse.social.studioasinc.util.ViewUtilsKt;

public class CompleteProfileActivity extends AppCompatActivity {

	public final int REQ_CD_SELECTAVATAR = 101;

	private AuthenticationService authService;
	private QueryService queryService;

	private boolean userNameErr = false;
	private String thedpurl = "";

	private FadeEditText username_input;
	private FadeEditText nickname_input;
	private FadeEditText biography_input;
	private ImageView profile_image;
	private com.google.android.material.button.MaterialButton complete_button;
	private Vibrator vbr;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_complete_profile);
		initialize(_savedInstanceState);
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
				|| ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
		} else {
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
		authService = new AuthenticationService();
		DatabaseService dbService = new DatabaseService();
		queryService = new QueryService(dbService);

		com.google.android.material.appbar.MaterialToolbar toolbar = findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);
		if (getSupportActionBar() != null) {
			getSupportActionBar().setDisplayHomeAsUpEnabled(true);
			getSupportActionBar().setDisplayShowHomeEnabled(true);
			getSupportActionBar().setTitle("");
		}
		toolbar.setNavigationOnClickListener(v -> onBackPressed());

		username_input = findViewById(R.id.username_input);
		nickname_input = findViewById(R.id.nickname_input);
		biography_input = findViewById(R.id.biography_input);
		profile_image = findViewById(R.id.profile_image);
		complete_button = findViewById(R.id.complete_button);
		CardView profile_image_card = findViewById(R.id.profile_image_card);
		vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

		profile_image_card.setOnLongClickListener(view -> {
			thedpurl = "null";
			profile_image.setImageResource(R.drawable.avatar);
			vbr.vibrate(48);
			return true;
		});

		profile_image_card.setOnClickListener(view -> {
			Intent selectAvatarIntent = new Intent(Intent.ACTION_GET_CONTENT);
			selectAvatarIntent.setType("image/*");
			startActivityForResult(selectAvatarIntent, REQ_CD_SELECTAVATAR);
		});

		username_input.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				final String charSeq = s.toString();
				if (charSeq.trim().isEmpty()) {
					username_input.setError(getResources().getString(R.string.enter_username));
					userNameErr = true;
				} else if (!charSeq.matches("[a-z0-9_.]+")) {
					username_input.setError(getResources().getString(R.string.username_err_invalid_characters));
					userNameErr = true;
				} else if (charSeq.length() < 3) {
					username_input.setError(getResources().getString(R.string.username_err_3_characters));
					userNameErr = true;
				} else if (charSeq.length() > 25) {
					username_input.setError(getResources().getString(R.string.username_err_25_characters));
					userNameErr = true;
				} else {
					queryService.fetchWithOrder("skyline/users", "username", charSeq.trim(), new DatabaseService.DataListener() {
						@Override
						public void onDataChange(DataSnapshot dataSnapshot) {
							if (dataSnapshot.exists()) {
								username_input.setError(getResources().getString(R.string.username_err_already_taken));
								userNameErr = true;
							} else {
								username_input.setError(null);
								userNameErr = false;
							}
						}
						@Override
						public void onCancelled(DatabaseError databaseError) {
							// Handle error
						}
					});
				}
			}
			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void afterTextChanged(Editable s) {}
		});


		complete_button.setOnClickListener(view -> {
			if (userNameErr) {
				SketchwareUtil.showMessage(getApplicationContext(), getResources().getString(R.string.username_err_invalid));
				vbr.vibrate(48);
			} else {
				complete_button.setEnabled(false);
				complete_button.setText("Loading...");
				username_input.setEnabled(false);
				UserDataPusher pusher = new UserDataPusher();
				FirebaseUser currentUser = authService.getCurrentUser();
				if (currentUser == null) return;

				pusher.pushData(
						username_input.getText().toString().trim(),
						nickname_input.getText().toString().trim(),
						biography_input.getText().toString().trim(),
						thedpurl,
						getIntent().getStringExtra("googleLoginAvatarUri"),
						currentUser.getEmail(),
						currentUser.getUid(),
						(success, errorMessage) -> {
							if (success) {
								Intent homeIntent = new Intent(getApplicationContext(), HomeActivity.class);
								startActivity(homeIntent);
								finish();
							} else {
								username_input.setEnabled(true);
								complete_button.setEnabled(true);
								complete_button.setText(R.string.continue_button);
								if ("Permission denied".equals(errorMessage)) {
									SketchwareUtil.showMessage(getApplicationContext(), "Email is not verified");
								} else {
									SketchwareUtil.showMessage(getApplicationContext(), errorMessage);
								}
							}
							return null;
						}
				);
			}
		});
	}

	private void initializeLogic() {
		TextView title = findViewById(R.id.title);
		title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
		ViewUtilsKt.setStateColor(this, 0xFFFFFFFF, 0xFFFFFFFF);
		thedpurl = "null";
		userNameErr = true;

		if (getIntent().hasExtra("findedUsername")) {
			username_input.setText(getIntent().getStringExtra("findedUsername"));
		}
		if (getIntent().hasExtra("googleLoginName") && getIntent().hasExtra("googleLoginAvatarUri")) {
			Glide.with(getApplicationContext()).load(Uri.parse(getIntent().getStringExtra("googleLoginAvatarUri"))).into(profile_image);
			nickname_input.setText(getIntent().getStringExtra("googleLoginName"));
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQ_CD_SELECTAVATAR && resultCode == Activity.RESULT_OK && data != null) {
			Uri uri = data.getData();
			if (uri != null) {
				profile_image.setImageURI(uri);
				String path = StorageUtil.getPathFromUri(getApplicationContext(), uri);
				if (path != null) {
					UploadFiles.uploadFile(path, new UploadFiles.UploadCallback() {
						@Override
						public void onUploadComplete(String imageUrl) {
							thedpurl = imageUrl;
						}
						@Override
						public void onUploadError(String errorMessage) {
							SketchwareUtil.showMessage(getApplicationContext(), "Something went wrong");
						}
					});
				} else {
					SketchwareUtil.showMessage(getApplicationContext(), "Could not get path from image");
				}
			}
		}
	}

	@Override
	public void onBackPressed() {
		showConfirmationDialog(
				getResources().getString(R.string.cancel_complete_profile_warn) + "\n\n" + getResources().getString(R.string.cancel_complete_profile_warn2),
				() -> {
					authService.signOut();
					finish();
				});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.complete_profile_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.cancel) {
			showConfirmationDialog(
					getResources().getString(R.string.cancel_create_account_warn) + "\n\n" + getResources().getString(R.string.cancel_create_account_warn2),
					() -> {
						item.setEnabled(false);
						authService.deleteUser(task -> {
                            if(task.isSuccessful()){
                                Intent mainIntent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(mainIntent);
                                finish();
                            } else {
                                SketchwareUtil.showMessage(getApplicationContext(), task.getException().getMessage());
                                invalidateOptionsMenu();
                            }
                        });
					});
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void showConfirmationDialog(String message, Runnable onConfirm) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		LayoutInflater inflater = getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.dialog_synapse_bg_view, null);
		builder.setView(dialogView);
		final AlertDialog dialog = builder.create();

		TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
		TextView dialogMessage = dialogView.findViewById(R.id.dialog_message);
		TextView noButton = dialogView.findViewById(R.id.dialog_no_button);
		TextView yesButton = dialogView.findViewById(R.id.dialog_yes_button);

		dialogTitle.setText(R.string.info);
		dialogMessage.setText(message);
		yesButton.setText(R.string.yes);
		noButton.setText(R.string.no);

		yesButton.setOnClickListener(v -> {
			onConfirm.run();
			dialog.dismiss();
		});
		noButton.setOnClickListener(v -> dialog.dismiss());

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		}
		dialog.show();
	}
}