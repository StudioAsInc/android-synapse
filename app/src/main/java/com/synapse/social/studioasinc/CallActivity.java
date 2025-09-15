package com.synapse.social.studioasinc;

import android.animation.*;
import android.app.*;
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
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.*;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.gridlayout.*;
import com.google.firebase.FirebaseApp;
import com.theartofdev.edmodo.cropper.*;
import com.yalantis.ucrop.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class CallActivity extends AppCompatActivity {
	
	private LinearLayout parent_layout;
	private LinearLayout info_container;
	private LinearLayout actions_container;
	private CardView dp_main_cv;
	private TextView tv_status;
	private TextView tv_username;
	private LinearLayout dp_main_container;
	private ImageView dp_iv;
	private LinearLayout linear3;
	private LinearLayout linear4;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.call);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		parent_layout = findViewById(R.id.parent_layout);
		info_container = findViewById(R.id.info_container);
		actions_container = findViewById(R.id.actions_container);
		dp_main_cv = findViewById(R.id.dp_main_cv);
		tv_status = findViewById(R.id.tv_status);
		tv_username = findViewById(R.id.tv_username);
		dp_main_container = findViewById(R.id.dp_main_container);
		dp_iv = findViewById(R.id.dp_iv);
		linear3 = findViewById(R.id.linear3);
		linear4 = findViewById(R.id.linear4);
	}
	
	private void initializeLogic() {
	}
	
}