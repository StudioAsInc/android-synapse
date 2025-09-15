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
import android.widget.ScrollView;
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

public class DisappearingMessageSettingsActivity extends AppCompatActivity {
	
	private LinearLayout parent_layout;
	private LinearLayout linear2;
	private ScrollView vscroll1;
	private ImageView imageview1;
	private TextView textview1;
	private LinearLayout linear3;
	private CardView cardview1;
	private LinearLayout linear4;
	private LinearLayout linear5;
	private ImageView imageview3;
	private TextView textview2;
	private TextView textview3;
	private TextView textview4;
	private RadioGroup radiogroup1;
	private TextView textview5;
	private RadioButton radiobutton1;
	private RadioButton radiobutton4;
	private RadioButton radiobutton5;
	private RadioButton radiobutton2;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.disappearing_message_settings);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		parent_layout = findViewById(R.id.parent_layout);
		linear2 = findViewById(R.id.linear2);
		vscroll1 = findViewById(R.id.vscroll1);
		imageview1 = findViewById(R.id.imageview1);
		textview1 = findViewById(R.id.textview1);
		linear3 = findViewById(R.id.linear3);
		cardview1 = findViewById(R.id.cardview1);
		linear4 = findViewById(R.id.linear4);
		linear5 = findViewById(R.id.linear5);
		imageview3 = findViewById(R.id.imageview3);
		textview2 = findViewById(R.id.textview2);
		textview3 = findViewById(R.id.textview3);
		textview4 = findViewById(R.id.textview4);
		radiogroup1 = findViewById(R.id.radiogroup1);
		textview5 = findViewById(R.id.textview5);
		radiobutton1 = findViewById(R.id.radiobutton1);
		radiobutton4 = findViewById(R.id.radiobutton4);
		radiobutton5 = findViewById(R.id.radiobutton5);
		radiobutton2 = findViewById(R.id.radiobutton2);
	}
	
	private void initializeLogic() {
	}
	
}