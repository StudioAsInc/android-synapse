package com.synapse.social.studioasinc;

import com.synapse.social.studioasinc.CheckpermissionActivity;
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
import com.google.android.material.color.MaterialColors;
import com.synapse.social.studioasinc.CenterCropLinearLayoutNoEffect;
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

import android.widget.Toast;
import android.graphics.drawable.ColorDrawable;
import com.google.gson.Gson; // Import Gson
import com.google.gson.reflect.TypeToken; // Import TypeToken
import android.content.pm.PackageManager; // Import PackageManager


// TODO: Migrate to Supabase
// This is the main entry point of the app.
// The following needs to be done:
// 1. Replace all Firebase database calls with calls to the `DatabaseService` interface.
// 2. Replace all Firebase auth calls with calls to the `AuthenticationService` interface.
public class MainActivity extends AppCompatActivity {

	private ArrayList<HashMap<String, Object>> commentsListMap = new ArrayList<>();

	private CenterCropLinearLayoutNoEffect body;
	private LinearLayout top_layout;
	private LinearLayout middle_layout;
	private LinearLayout bottom_layout;
	private ImageView app_logo;
	private ImageView trademark_img;

	private RequestNetwork network;
	private RequestNetwork.RequestListener _network_request_listener;
	private AlertDialog.Builder updateDialogBuilder; // Renamed to avoid confusion with Dialog object

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_main);
		initialize(_savedInstanceState);
		// TODO: Initialize Supabase client in SynapseApp.java instead of here.
		// FirebaseApp.initializeApp(this);
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
		network = new RequestNetwork(this);
		updateDialogBuilder = new AlertDialog.Builder(this); // Use the renamed variable

		app_logo.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View _view) {
				// TODO: Replace with Supabase Auth
				// if (FirebaseAuth.getInstance().getCurrentUser() != null && FirebaseAuth.getInstance().getCurrentUser().getEmail() != null && FirebaseAuth.getInstance().getCurrentUser().getEmail().equals("mashikahamed0@gmail.com")) {
				// 	finish(); // This seems to be the intended action for the long click
				// } else {
				// 	// Optionally, do something else or nothing
				// }
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
	}

	private void initializeLogic() {
		// 1. Set Fullscreen Immersive Mode
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getWindow().setFlags(
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
			);
		}

		// 2. Get current app version
		final int currentVersionCode; // Made final to be accessible in anonymous inner class
		try {
			currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			_showErrorDialog("Version check failed: " + e.getMessage());
			e.printStackTrace();
			// If version code cannot be obtained, proceed with normal flow after delay
			proceedToAuthCheck();
			return; // Exit method to prevent further execution in initializeLogic
		}

		// 3. Check for updates (only if online)
		if (isNetworkAvailable()) {
			network.startRequestNetwork(
			RequestNetworkController.GET,
			"https://pastebin.com/raw/sQuaciVv",
			"update",
			new RequestNetwork.RequestListener() {
				@Override
				public void onResponse(String tag, String response, HashMap<String, Object> _responseHeaders) {
					try {
						HashMap<String, Object> updateMap = new Gson().fromJson(
						response,
						new TypeToken<HashMap<String, Object>>(){}.getType()
						);

						double latestVersionCode = 0;
						if (updateMap.containsKey("versionCode")) {
							Object vc = updateMap.get("versionCode");
							if (vc instanceof Double) {
								latestVersionCode = (Double) vc;
							} else if (vc instanceof String) {
								latestVersionCode = Double.parseDouble((String) vc);
							} else if (vc instanceof Number) {
								latestVersionCode = ((Number) vc).doubleValue();
							}
						}

						if (latestVersionCode > currentVersionCode) {
							// New update available, show dialog
							String title = updateMap.get("title").toString();
							String versionName = updateMap.get("versionName").toString();
							String changelog = updateMap.get("whatsNew").toString().replace("\\n", "\n");
							String updateLink = updateMap.get("updateLink").toString();
							boolean isCancelable = false;
							if (updateMap.containsKey("isCancelable")) {
								Object ic = updateMap.get("isCancelable");
								if (ic instanceof Boolean) {
									isCancelable = (Boolean) ic;
								} else if (ic instanceof String) {
									isCancelable = Boolean.parseBoolean((String) ic);
								}
							}

							_showUpdateDialog(title, versionName, changelog, updateLink, isCancelable);
							// IMPORTANT: DO NOT call proceedToAuthCheck() here.
                            // It will be called by the dialog's 'Later' button if cancelable.
                            // If not cancelable, the app will effectively pause until the user updates.
						} else {
							// Already on latest version, proceed to auth check after delay
						//	SketchwareUtil.showMessage(getApplicationContext(), "You have the latest version.");
							proceedToAuthCheck();
						}
					} catch (Exception e) {
						_showErrorDialog("Update parsing error: " + e.getMessage());
						e.printStackTrace();
						// On parsing error, proceed to auth check after delay
						proceedToAuthCheck();
					}
				}

				@Override
				public void onErrorResponse(String tag, String message) {
					// Error fetching update, proceed to auth check after delay
					proceedToAuthCheck();
				}
			}
			);
		} else {
			// No internet, proceed to auth check after delay
			proceedToAuthCheck();
		}
	}

    // Helper method to encapsulate the delayed auth check logic
    private void proceedToAuthCheck() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // TODO: Implement with Supabase
        }, 500); // 500ms delay
    }

	// Helper method to check internet connectivity
	private boolean isNetworkAvailable() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null) return false;
		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
	}

	@Override
	public void onBackPressed() {
		finishAffinity();
	}

	public void _showUpdateDialog(final String _title, final String _versionName, final String _changelog, final String _updateLink, final boolean _isCancelable) {

		LayoutInflater inflater = getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.dialog_update, null);

		updateDialogBuilder.setView(dialogView);
		updateDialogBuilder.setCancelable(_isCancelable); // Set cancelable property

		TextView tvTitle = dialogView.findViewById(R.id.update_title);
		TextView tvVersion = dialogView.findViewById(R.id.update_version);
		TextView tvChangelog = dialogView.findViewById(R.id.update_changelog);
		MaterialButton btnUpdate = dialogView.findViewById(R.id.button_update);
		MaterialButton btnLater = dialogView.findViewById(R.id.button_later);

		tvTitle.setText(_title);
		tvVersion.setText("Version " + _versionName);
		tvChangelog.setText(_changelog);

		final AlertDialog dialog = updateDialogBuilder.create();

		btnUpdate.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(_updateLink));
				startActivity(intent);
				dialog.dismiss(); // Dismiss dialog when update button is clicked
				finish(); // Finish MainActivity, as user is likely leaving the app to update
			}
		});

		btnLater.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				dialog.dismiss();
				// ONLY call proceedToAuthCheck if the dialog was cancelable and user chose "Later"
                // If not cancelable, this button is hidden.
                if (_isCancelable) {
                    proceedToAuthCheck();
                }
			}
		});

		if (!_isCancelable) {
			btnLater.setVisibility(View.GONE);
            // If not cancelable, make sure user cannot dismiss the dialog by back press or tapping outside
            dialog.setCanceledOnTouchOutside(false);
		} else {
            btnLater.setVisibility(View.VISIBLE); // Ensure it's visible if cancelable
            dialog.setCanceledOnTouchOutside(true); // Allow dismiss by touch if cancelable
        }

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
		}

		dialog.show();
	}


	public void _showErrorDialog(final String _errorMessage) {
		LayoutInflater inflater = getLayoutInflater();
		View dialogView = inflater.inflate(R.layout.dialog_error, null);

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(dialogView);
		builder.setCancelable(true);


		TextView tvErrorMessage = dialogView.findViewById(R.id.error_message_textview);
		MaterialButton btnOk = dialogView.findViewById(R.id.ok_button);

		tvErrorMessage.setText(_errorMessage);

		final AlertDialog dialog = builder.create();

		btnOk.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				dialog.dismiss();
                // If this error dialog implies that the app cannot proceed normally (e.g., version check failed critically)
                // you might want to call proceedToAuthCheck() here, or even finish the app.
                // For now, let's assume it's a minor error and the app should try to proceed.
                proceedToAuthCheck(); // Assuming _showErrorDialog doesn't block the main flow.
			}
		});

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
		}

		dialog.show();
	}

}