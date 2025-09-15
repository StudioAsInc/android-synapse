package com.synapse.social.studioasinc;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.os.*;
import android.os.Vibrator;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.*;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.color.MaterialColors;
import com.google.firebase.FirebaseApp;
import com.theartofdev.edmodo.cropper.*;
import com.yalantis.ucrop.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;
import java.io.*;

public class DebugActivity extends AppCompatActivity {
	
	private LinearLayout body;
	private ImageView ic_bug;
	private TextView title;
	private TextView subtitle;
	private CardView cardview1;
	private Button clearData_btn;
	private ScrollView scroll;
	private TextView error_text;
	
	private Vibrator vbr;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.debug);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		body = findViewById(R.id.body);
		ic_bug = findViewById(R.id.ic_bug);
		title = findViewById(R.id.title);
		subtitle = findViewById(R.id.subtitle);
		cardview1 = findViewById(R.id.cardview1);
		clearData_btn = findViewById(R.id.clearData_btn);
		scroll = findViewById(R.id.scroll);
		error_text = findViewById(R.id.error_text);
		vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		
		clearData_btn.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				//clearData();
				finishAffinity();
			}
		});
		
		error_text.setOnLongClickListener(new View.OnLongClickListener() {
			@Override
			public boolean onLongClick(View _view) {
				((ClipboardManager) getSystemService(getApplicationContext().CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("clipboard", error_text.getText().toString()));
				vbr.vibrate((long)(48));
				return true;
			}
		});
	}
	
	private void initializeLogic() {
		if (getIntent().hasExtra("error")) {
			error_text.setText(getIntent().getStringExtra("error"));
		}
	}
	
	@Override
	public void onBackPressed() {
		finish();
	}
	
	public void _stateColor(final int _statusColor, final int _navigationColor) {
		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
		getWindow().setStatusBarColor(_statusColor);
		getWindow().setNavigationBarColor(_navigationColor);
	}
	
	
	public void _viewGraphics(final View _view, final int _onFocus, final int _onRipple, final double _radius, final double _stroke, final int _strokeColor) {
		android.graphics.drawable.GradientDrawable GG = new android.graphics.drawable.GradientDrawable();
		GG.setColor(_onFocus);
		GG.setCornerRadius((float)_radius);
		GG.setStroke((int) _stroke, _strokeColor);
		android.graphics.drawable.RippleDrawable RE = new android.graphics.drawable.RippleDrawable(new android.content.res.ColorStateList(new int[][]{new int[]{}}, new int[]{ _onRipple}), GG, null);
		_view.setBackground(RE);
	}
	
	
	public void _extra() {
	}
	private void clearData() {
		try {
			if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
				((ActivityManager)getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
			} else {
				Runtime.getRuntime().exec("pm clear " + getApplicationContext().getPackageName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	{
	}
	
}