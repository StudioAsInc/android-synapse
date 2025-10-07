package com.synapse.social.studioasinc;

import android.animation.*;
import android.app.*;
import android.app.AlertDialog;
import android.content.*;
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
import java.util.List;
import java.util.Map;
import java.util.regex.*;
import org.json.*;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import android.content.pm.PackageManager;

// Synapse Imports
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService;
import com.synapse.social.studioasinc.backend.interfaces.IUser;
import com.synapse.social.studioasinc.backend.interfaces.IDataListener;
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError;
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseReference;


public class MainActivity extends AppCompatActivity {

	private ArrayList<HashMap<String, Object>> commentsListMap = new ArrayList<>();

	private CenterCropLinearLayoutNoEffect body;
	private LinearLayout top_layout;
	private LinearLayout middle_layout;
	private LinearLayout bottom_layout;
	private ImageView app_logo;
	private ImageView trademark_img;

	private IAuthenticationService authService;
    private IDatabaseService dbService;

	private RequestNetwork network;
	private RequestNetwork.RequestListener _network_request_listener;
	private AlertDialog.Builder updateDialogBuilder;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_main);
		initialize(_savedInstanceState);
		createNotificationChannels();
		initializeLogic();
	}

	private void createNotificationChannels() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			NotificationManager notificationManager = getSystemService(NotificationManager.class);
			
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
			
			NotificationChannel generalChannel = new NotificationChannel(
				"general",
				"General",
				NotificationManager.IMPORTANCE_DEFAULT
			);
			generalChannel.setDescription("General app notifications");
			generalChannel.enableLights(false);
			generalChannel.enableVibration(false);
			
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

		authService = ((SynapseApp) getApplication()).getAuthenticationService();
        dbService = ((SynapseApp) getApplication()).getDatabaseService();

		network = new RequestNetwork(this);
		updateDialogBuilder = new AlertDialog.Builder(this);

		app_logo.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View _view) {
				IUser currentUser = authService.getCurrentUser();
                if (currentUser != null && currentUser.getEmail() != null && currentUser.getEmail().equals("mashikahamed0@gmail.com")) {
                    finish();
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
	}

	private void initializeLogic() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
			getWindow().setFlags(
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
			WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
			);
		}

		final int currentVersionCode;
		try {
			currentVersionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (PackageManager.NameNotFoundException e) {
			_showErrorDialog("Version check failed: " + e.getMessage());
			e.printStackTrace();
			proceedToAuthCheck();
			return;
		}

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
						} else {
							proceedToAuthCheck();
						}
					} catch (Exception e) {
						_showErrorDialog("Update parsing error: " + e.getMessage());
						e.printStackTrace();
						proceedToAuthCheck();
					}
				}

				@Override
				public void onErrorResponse(String tag, String message) {
					proceedToAuthCheck();
				}
			}
			);
		} else {
			proceedToAuthCheck();
		}
	}

    private void proceedToAuthCheck() {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            IUser currentUser = authService.getCurrentUser();
            if (currentUser != null) {
                IDatabaseReference userRef = dbService.getReference("skyline/users").child(currentUser.getUid());

                dbService.getData(userRef, new IDataListener() {
                    @Override
                    public void onDataChange(IDataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            try {
                                List<Map<String, Object>> userList = (List<Map<String, Object>>) dataSnapshot.getValue(List.class);
                                if (userList != null && !userList.isEmpty()) {
                                    Map<String, Object> userMap = userList.get(0);
                                    String banned = (String) userMap.get("banned");
                                    if ("false".equals(banned)) {
                                        startActivity(new Intent(MainActivity.this, HomeActivity.class));
                                        finish();
                                    } else {
                                        Toast.makeText(MainActivity.this, "You are banned & Signed Out.", Toast.LENGTH_LONG).show();
                                        authService.signOut();
                                        finish();
                                    }
                                } else {
                                    startActivity(new Intent(MainActivity.this, CompleteProfileActivity.class));
                                    finish();
                                }
                            } catch (Exception e) {
                                startActivity(new Intent(MainActivity.this, CompleteProfileActivity.class));
                                finish();
                            }
                        } else {
                            startActivity(new Intent(MainActivity.this, CompleteProfileActivity.class));
                            finish();
                        }
                    }

                    @Override
                    public void onCancelled(IDatabaseError databaseError) {
                        Toast.makeText(MainActivity.this, "Database error: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(MainActivity.this, AuthActivity.class));
                        finish();
                    }
                });
            } else {
                startActivity(new Intent(MainActivity.this, AuthActivity.class));
                finish();
            }
        }, 500);
    }

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
		updateDialogBuilder.setCancelable(_isCancelable);

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
				dialog.dismiss();
				finish();
			}
		});

		btnLater.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				dialog.dismiss();
                if (_isCancelable) {
                    proceedToAuthCheck();
                }
			}
		});

		if (!_isCancelable) {
			btnLater.setVisibility(View.GONE);
            dialog.setCanceledOnTouchOutside(false);
		} else {
            btnLater.setVisibility(View.VISIBLE);
            dialog.setCanceledOnTouchOutside(true);
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
                proceedToAuthCheck();
			}
		});

		if (dialog.getWindow() != null) {
			dialog.getWindow().setBackgroundDrawable(new ColorDrawable(0));
		}

		dialog.show();
	}

}