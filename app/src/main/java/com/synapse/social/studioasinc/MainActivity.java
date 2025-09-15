package com.synapse.social.studioasinc;

import android.animation.*;
import android.app.*;
import android.app.AlertDialog;
import android.content.*;
import android.content.DialogInterface;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.bumptech.glide.*;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.color.MaterialColors;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.synapse.social.studioasinc.CenterCropLinearLayoutNoEffect;
import com.synapse.social.studioasinc.util.UpdateManager;
import com.synapse.social.studioasinc.util.AuthUtil;
import com.theartofdev.edmodo.cropper.*;
import com.yalantis.ucrop.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;
// Required imports
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.widget.Toast;
import android.graphics.drawable.ColorDrawable;
import com.google.gson.Gson; // Import Gson
import com.google.gson.reflect.TypeToken; // Import TypeToken
import android.content.pm.PackageManager; // Import PackageManager


public class MainActivity extends AppCompatActivity {

	private ArrayList<HashMap<String, Object>> commentsListMap = new ArrayList<>();

	private CenterCropLinearLayoutNoEffect body;
	private LinearLayout top_layout;
	private LinearLayout middle_layout;
	private LinearLayout bottom_layout;
	private ImageView app_logo;
	private ImageView trademark_img;

	private FirebaseAuth auth;
	private OnCompleteListener<AuthResult> _auth_create_user_listener;
	private OnCompleteListener<AuthResult> _auth_sign_in_listener;
	private OnCompleteListener<Void> _auth_reset_password_listener;
	private OnCompleteListener<Void> auth_updateEmailListener;
	private OnCompleteListener<Void> auth_updatePasswordListener;
	private OnCompleteListener<Void> auth_emailVerificationSentListener;
	private OnCompleteListener<Void> auth_deleteUserListener;
	private OnCompleteListener<Void> auth_updateProfileListener;
	private OnCompleteListener<AuthResult> auth_phoneAuthListener;
	private OnCompleteListener<AuthResult> auth_googleSignInListener;
	private RequestNetwork network;
	private RequestNetwork.RequestListener _network_request_listener;
	private AlertDialog.Builder updateDialogBuilder; // Renamed to avoid confusion with Dialog object

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.main);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		createNotificationChannels();
		initializeLogic();
	}

	private void createNotificationChannels() {
		// Create notification channels for Android O and above
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			
			// Messages channel
			NotificationChannel messagesChannel = new NotificationChannel(
				"messages",
				"Messages",
				NotificationManager.IMPORTANCE_HIGH
			);
			messagesChannel.setDescription("Chat message notifications");
			messagesChannel.enableLights(true);
			messagesChannel.setLightColor(Color.RED);
			messagesChannel.enableVibration(true);
			messagesChannel.setShowBadge(true);
			messagesChannel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
			
			// General notifications channel
			NotificationChannel generalChannel = new NotificationChannel(
				"general",
				"General",
				NotificationManager.IMPORTANCE_DEFAULT
			);
			generalChannel.setDescription("General app notifications");
			generalChannel.enableLights(false);
			generalChannel.enableVibration(false);
			
			// Create the channels
			notificationManager.createNotificationChannel(messagesChannel);
			notificationManager.createNotificationChannel(generalChannel);
		}
	}

	private void initialize(Bundle _savedInstanceState) {
		body = findViewById(R.id.body);
		top_layout = findViewById(R.id.top_layout);
		middle_layout = findViewById(R.id.middle_layout);
		bottom_layout = findViewById(R.id.bottom_layout);
		app_logo = findViewById(R.id.app_logo);
		trademark_img = findViewById(R.id.trademark_img);
		auth = FirebaseAuth.getInstance();
		network = new RequestNetwork(this);
		updateDialogBuilder = new AlertDialog.Builder(this); // Use the renamed variable

		app_logo.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View _view) {
				if (FirebaseAuth.getInstance().getCurrentUser() != null && FirebaseAuth.getInstance().getCurrentUser().getEmail() != null && FirebaseAuth.getInstance().getCurrentUser().getEmail().equals("mashikahamed0@gmail.com")) {
					finish(); // This seems to be the intended action for the long click
				} else {
					// Optionally, do something else or nothing
				}
				return true;
			}
		});

		_network_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _tag = _param1;
				final String _response = _param2;
				final HashMap<String, Object> _responseHeaders = _param3;

			}

			@Override
			public void onErrorResponse(String _param1, String _param2) {
				final String _tag = _param1;
				final String _message = _param2;

			}
		};

		auth_updateEmailListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_updatePasswordListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_emailVerificationSentListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_deleteUserListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_phoneAuthListener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> task) {
				final boolean _success = task.isSuccessful();
				final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";

			}
		};

		auth_updateProfileListener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		auth_googleSignInListener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> task) {
				final boolean _success = task.isSuccessful();
				final String _errorMessage = task.getException() != null ? task.getException().getMessage() : "";

			}
		};

		_auth_create_user_listener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		_auth_sign_in_listener = new OnCompleteListener<AuthResult>() {
			@Override
			public void onComplete(Task<AuthResult> _param1) {
				final boolean _success = _param1.isSuccessful();
				final String _errorMessage = _param1.getException() != null ? _param1.getException().getMessage() : "";

			}
		};

		_auth_reset_password_listener = new OnCompleteListener<Void>() {
			@Override
			public void onComplete(Task<Void> _param1) {
				final boolean _success = _param1.isSuccessful();

			}
		};
	}

	private void initializeLogic() {
		// 1. Set Fullscreen Immersive Mode
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getWindow().setFlags(
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
			);
		}

		// The update check is now handled by the UpdateManager
		UpdateManager updateManager = new UpdateManager(this, new Runnable() {
            @Override
            public void run() {
                proceedToAuthCheck();
            }
        });
        updateManager.checkForUpdate();
	}

	// Helper method to encapsulate the delayed auth check logic
	public void proceedToAuthCheck() {
		new Handler(Looper.getMainLooper()).postDelayed(() -> {
			if (AuthUtil.isLoggedIn()) {
				// User is logged in (either via Firebase or backup), verify the user is still valid
				FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
				if (currentUser != null) {
					currentUser.reload().addOnCompleteListener(task -> {
						if (task.isSuccessful()) {
							// User is still valid, check ban status
							checkUserBanStatusAndProceed(currentUser.getUid());
						} else {
							// Reload failed (e.g., network). Keep session and proceed to Home to avoid false sign-outs
							startActivity(new Intent(MainActivity.this, HomeActivity.class));
							finish();
						}
					});
				} else {
					// Firebase user is null but we have backup authentication
					// Try to get the stored UID and proceed
					String storedUid = AuthUtil.getCurrentUserUid();
					if (storedUid != null) {
						checkUserBanStatusAndProceed(storedUid);
					} else {
						// No stored UID, clear authentication and go to auth
						// No local auth state to clear - handled by Firebase
						startActivity(new Intent(MainActivity.this, AuthActivity.class));
						finish();
					}
				}
			} else {
				// User not logged in, redirect to AuthActivity
				startActivity(new Intent(MainActivity.this, AuthActivity.class));
				finish();
			}
		}, 500); // 500ms delay
	}
	
	private void checkUserBanStatusAndProceed(String uid) {
		DatabaseReference userRef = FirebaseDatabase.getInstance()
				.getReference("skyline/users")
				.child(uid);

		userRef.addListenerForSingleValueEvent(new ValueEventListener() {
			@Override
			public void onDataChange(@NonNull DataSnapshot snapshot) {
				if (snapshot.exists()) {
					Object bannedObj = snapshot.child("banned").getValue();
					boolean isBanned = false;

					if (bannedObj instanceof Boolean) {
						isBanned = (Boolean) bannedObj;
					} else if (bannedObj instanceof String) {
						String bannedStr = (String) bannedObj;
						isBanned = bannedStr.equalsIgnoreCase("true") || bannedStr.equals("1");
					}

					if (!isBanned) {
						// Not banned, redirect to HomeActivity
						startActivity(new Intent(MainActivity.this, HomeActivity.class));
						finish();
					} else {
						// Banned, show toast and sign out
						Toast.makeText(MainActivity.this, "You are banned & Signed Out.", Toast.LENGTH_LONG).show();
						FirebaseAuth.getInstance().signOut();
						startActivity(new Intent(MainActivity.this, AuthActivity.class));
						finish();
					}
				} else {
					// User data not found (maybe first login, or incomplete profile)
					// This path leads to CompleteProfileActivity
					startActivity(new Intent(MainActivity.this, CompleteProfileActivity.class));
					finish();
				}
			}

			@Override
			public void onCancelled(@NonNull DatabaseError error) {
				Toast.makeText(MainActivity.this, "Database error: " + error.getMessage(), Toast.LENGTH_SHORT).show();
				// If database check fails, do not sign the user out. Proceed to Home for offline experience
				startActivity(new Intent(MainActivity.this, HomeActivity.class));
				finish();
			}
		});
	}

	@Override
	public void onBackPressed() {
		finishAffinity();
	}

}
