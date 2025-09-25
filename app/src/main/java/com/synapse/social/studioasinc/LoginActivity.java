package com.synapse.social.studioasinc;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.os.Bundle;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.synapse.social.studioasinc.util.ViewUtilsKt;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import org.json.*;
import com.synapse.social.studioasinc.util.SupabaseManager;
import io.github.jan.supabase.gotrue.handleDeeplinks
import io.github.jan.supabase.gotrue.providers.Google
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.jvm.functions.Function2;
import kotlinx.coroutines.CoroutineScope;
import kotlinx.coroutines.Dispatchers;
import kotlinx.coroutines.launch;

public class LoginActivity extends AppCompatActivity {

	private Timer _timer = new Timer();

	private HashMap<String, Object> m = new HashMap<>();
	private String full_name = "";
	private String email = "";
	private String password = "";
	private String confirm_password = "";
	private boolean isLogin = false;
	private String SplashScreen = "";
	GoogleSignInOptions gso;
	GoogleSignInClient gsc;
	private String getType = "";
	private double n = 0;
	private double count = 0;
	private String email_support = "";
	private String terms_of_service = "";
	private String privacy_policy = "";
	private String discord_server = "";

	private LinearLayout bg;
	private LinearLayout main_body;
	private ImageView imageview2;
	private TextView textview2;
	private TextView textview1;
	private LinearLayout body;
	private LinearLayout login_body;
	private LinearLayout register_body;
	private TextView login_subtitle;
	private EditText login_email;
	private EditText login_password;
	private TextView login_forgot_password;
	private TextView login_button;
	private LinearLayout linear7;
	private LinearLayout linear8;
	private TextView textview8;
	private TextView login_signup_button;
	private TextView textview10;
	private ImageView imageview3;
	private TextView register_subtitle;
	private EditText register_email;
	private EditText register_password;
	private EditText register_confirm_password;
	private TextView register_button;
	private LinearLayout linear10;
	private TextView textview11;
	private TextView register_login_button;

	private Intent intent = new Intent();
	private TimerTask timer;

	private Intent google_sign_in = new Intent();
	private Intent i = new Intent();

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_login);
		initialize(_savedInstanceState);
		initializeLogic();
	}

	private void initialize(Bundle _savedInstanceState) {
		bg = findViewById(R.id.bg);
		main_body = findViewById(R.id.main_body);
		imageview2 = findViewById(R.id.imageview2);
		textview2 = findViewById(R.id.textview2);
		textview1 = findViewById(R.id.textview1);
		body = findViewById(R.id.body);
		login_body = findViewById(R.id.login_body);
		register_body = findViewById(R.id.register_body);
		login_subtitle = findViewById(R.id.login_subtitle);
		login_email = findViewById(R.id.login_email);
		login_password = findViewById(R.id.login_password);
		login_forgot_password = findViewById(R.id.login_forgot_password);
		login_button = findViewById(R.id.login_button);
		linear7 = findViewById(R.id.linear7);
		linear8 = findViewById(R.id.linear8);
		textview8 = findViewById(R.id.textview8);
		login_signup_button = findViewById(R.id.login_signup_button);
		textview10 = findViewById(R.id.textview10);
		imageview3 = findViewById(R.id.imageview3);
		register_subtitle = findViewById(R.id.register_subtitle);
		register_email = findViewById(R.id.register_email);
		register_password = findViewById(R.id.register_password);
		register_confirm_password = findViewById(R.id.register_confirm_password);
		register_button = findViewById(R.id.register_button);
		linear10 = findViewById(R.id.linear10);
		textview11 = findViewById(R.id.textview11);
		register_login_button = findViewById(R.id.register_login_button);

		login_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (isLogin) {

				} else {
					if (login_email.getText().toString().trim().equals("") || login_password.getText().toString().trim().equals("")) {
						SketchwareUtil.showMessage(getApplicationContext(), "Please fill in all fields.");
					} else {
						isLogin = true;
						login_button.setText("Loading...");
						// SupabaseManager.INSTANCE.signIn(
						// 	login_email.getText().toString().trim(),
						// 	login_password.getText().toString().trim(),
						// 	(success, errorMessage) -> {
						// 		if (success) {
						// 			intent.setClass(getApplicationContext(), HomeActivity.class);
						// 			startActivity(intent);
						// 			finish();
						// 		} else {
						// 			isLogin = false;
						// 			login_button.setText("Login");
						// 			SketchwareUtil.showMessage(getApplicationContext(), errorMessage);
						// 		}
						// 		return null;
						// 	}
						// );
					}
				}
			}
		});

		login_signup_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				body.setVisibility(View.GONE);
				register_body.setVisibility(View.VISIBLE);
			}
		});

		imageview3.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				// CoroutineScope scope = new CoroutineScope(Dispatchers.getMain());
				// scope.launch(new Function2<CoroutineScope, Continuation<? super Unit>, Object>() {
				// 	@Override
				// 	public Object invoke(CoroutineScope coroutineScope, Continuation<? super Unit> continuation) {
				// 		try {
				// 			SupabaseManager.INSTANCE.getClient().getAuth().signInWith(Google.INSTANCE, null, null, null, null, continuation);
				// 		} catch (Exception e) {
				// 			e.printStackTrace();
				// 		}
				// 		return null;
				// 	}
				// });
			}
		});

		register_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				if (isLogin) {

				} else {
					if (register_email.getText().toString().trim().equals("") || (register_password.getText().toString().trim().equals("") || register_confirm_password.getText().toString().trim().equals(""))) {
						SketchwareUtil.showMessage(getApplicationContext(), "Please fill in all fields.");
					} else {
						if (register_password.getText().toString().trim().equals(register_confirm_password.getText().toString().trim())) {
							isLogin = true;
							register_button.setText("Loading...");
							// SupabaseManager.INSTANCE.signUp(
							// 	register_email.getText().toString().trim(),
							// 	register_password.getText().toString().trim(),
							// 	(success, errorMessage) -> {
							// 		if (success) {
							// 			intent.setClass(getApplicationContext(), CompleteProfileActivity.class);
							// 			startActivity(intent);
							// 			finish();
							// 		} else {
							// 			isLogin = false;
							// 			register_button.setText("Register");
							// 			SketchwareUtil.showMessage(getApplicationContext(), errorMessage);
							// 		}
							// 		return null;
							// 	}
							// );
						} else {
							SketchwareUtil.showMessage(getApplicationContext(), "Passwords do not match.");
						}
					}
				}
			}
		});

		register_login_button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				body.setVisibility(View.VISIBLE);
				register_body.setVisibility(View.GONE);
			}
		});

	}

	private void initializeLogic() {
		this.getIntent().setData(Uri.parse("io.supabase.androiddemo://login-callback/"));
		isLogin = false;
		ViewUtilsKt.setGradientDrawable(login_button, 0xFF445E91, 300, 0, Color.TRANSPARENT);
		ViewUtilsKt.setGradientDrawable(register_button, 0xFF445E91, 300, 0, Color.TRANSPARENT);
		ViewUtilsKt.setGradientDrawable(login_email, 0xFFFFFFFF, 28, 3, 0xFFEEEEEE);
		ViewUtilsKt.setGradientDrawable(login_password, 0xFFFFFFFF, 28, 3, 0xFFEEEEEE);
		ViewUtilsKt.setGradientDrawable(register_email, 0xFFFFFFFF, 28, 3, 0xFFEEEEEE);
		ViewUtilsKt.setGradientDrawable(register_password, 0xFFFFFFFF, 28, 3, 0xFFEEEEEE);
		ViewUtilsKt.setGradientDrawable(register_confirm_password, 0xFFFFFFFF, 28, 3, 0xFFEEEEEE);
		register_body.setVisibility(View.GONE);
		gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken("80383182931-8593l62n3n4n4g7s8hq0g8g2h6g8g2h6.apps.googleusercontent.com").requestEmail().build();
		gsc = GoogleSignIn.getClient(this, gso);
		// try {
		// 	SupabaseManager.INSTANCE.getClient().handleDeeplinks(getIntent(), (session) -> {
		// 		if (session != null) {
		// 			i.setClass(getApplicationContext(), HomeActivity.class);
		// 			startActivity(i);
		// 			finish();
		// 		}
		// 	});
		// } catch (Exception e) {
		// 	e.printStackTrace();
		// }
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == 1000) {
			Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
			try {
				task.getResult(ApiException.class);
				// homeActivity();
			} catch (ApiException e) {
				Toast.makeText(getApplicationContext(), "Something went wrong", Toast.LENGTH_SHORT).show();
			}
		}
	}


	@Override
	public void onStart() {
		super.onStart();
		// if (SupabaseManager.INSTANCE.getClient().getAuth().getCurrentSession() != null) {
		// 	intent.setClass(getApplicationContext(), HomeActivity.class);
		// 	startActivity(intent);
		// 	finish();
		// }
	}

	@Deprecated
	public void showMessage(String _s) {
		Toast.makeText(getApplicationContext(), _s, Toast.LENGTH_SHORT).show();
	}

	@Deprecated
	public int getLocationX(View _v) {
		int _location[] = new int[2];
		_v.getLocationInWindow(_location);
		return _location[0];
	}

	@Deprecated
	public int getLocationY(View _v) {
		int _location[] = new int[2];
		_v.getLocationInWindow(_location);
		return _location[1];
	}

	@Deprecated
	public int getRandom(int _min, int _max) {
		Random random = new Random();
		return random.nextInt(_max - _min + 1) + _min;
	}

	@Deprecated
	public ArrayList<Double> getCheckedItemPositionsToArray(ListView _list) {
		ArrayList<Double> _result = new ArrayList<Double>();
		SparseBooleanArray _arr = _list.getCheckedItemPositions();
		for (int _iIdx = 0; _iIdx < _arr.size(); _iIdx++) {
			if (_arr.valueAt(_iIdx))
			_result.add((double)_arr.keyAt(_iIdx));
		}
		return _result;
	}

	@Deprecated
	public float getDip(int _input) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, _input, getResources().getDisplayMetrics());
	}

	@Deprecated
	public int getDisplayWidthPixels() {
		return getResources().getDisplayMetrics().widthPixels;
	}

	@Deprecated
	public int getDisplayHeightPixels() {
		return getResources().getDisplayMetrics().heightPixels;
	}
}