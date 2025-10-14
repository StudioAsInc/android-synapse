// To-do: Migrate Firebase to Supabase
// This activity is one of the entry points of the app and currently initializes Firebase.
// 1. **Remove Firebase Initialization**:
//    - The `FirebaseApp.initializeApp(this)` call should be removed from the `onCreate` method.
//
// 2. **Centralize Supabase Initialization**:
//    - The Supabase client should be initialized in a central location, preferably in a custom `Application` class (e.g., `SynapseApp.java`).
//    - This ensures that the Supabase client is initialized only once and can be accessed as a singleton throughout the application's lifecycle.
//    - If the `SynapseApp.java` class does not yet exist, it should be created for this purpose.

package com.synapse.social.studioasinc;

import android.animation.*;
import android.app.*;
import android.content.*;
import android.content.Intent;
import android.content.res.*;
import android.graphics.*;
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
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.LinearLayout;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.gridlayout.*;
import com.google.firebase.FirebaseApp;
import com.theartofdev.edmodo.cropper.*;
import com.yalantis.ucrop.*;
import java.io.*;
import java.io.InputStream;
import java.text.*;
import java.util.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.*;
import org.json.*;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import java.util.Timer;
import java.util.TimerTask;


//Permission
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import java.util.Timer;
import java.util.TimerTask;
import com.synapse.social.studioasinc.permissionreq.AskPermission;

public class CheckpermissionActivity extends AppCompatActivity {

	private Timer _timer = new Timer();

	private LinearLayout linear1;

	private Intent i = new Intent();
	private TimerTask t;

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_checkpermission);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}

	private void initialize(Bundle _savedInstanceState) {
		linear1 = findViewById(R.id.linear1);
	}

	private void initializeLogic() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) { Window w = getWindow();  w.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS); };
		// In your Activity's onCreate:
		AskPermission askPermission = new AskPermission(this);
		askPermission.checkAndRequestPermissions();
	}

	@Override
	public void onBackPressed() {

	}

}