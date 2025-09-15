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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.gridlayout.*;
import androidx.viewpager.widget.PagerAdapter;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager.widget.ViewPager.OnAdapterChangeListener;
import androidx.viewpager.widget.ViewPager.OnPageChangeListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomnavigation.BottomNavigationView.OnNavigationItemSelectedListener;
import com.google.firebase.FirebaseApp;
import com.theartofdev.edmodo.cropper.*;
import com.yalantis.ucrop.*;
import java.io.*;
import java.io.InputStream;
import java.text.*;
import java.util.*;
import java.util.regex.*;
import org.json.*;

public class InboxActivity extends AppCompatActivity {

	private LinearLayout parentLayout;
	private LinearLayout appBar;
	private LinearLayout contentHolderLayout;
	private TextView textview1;
	private LinearLayout linear1;
	private ImageView imageview3;
	private ImageView imageview1;
	private ImageView imageview2;
	private ViewPager viewpager1;
	private BottomNavigationView bottomnavigation1;

	private FgFragmentAdapter fg;
	private Intent intent = new Intent();

	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.inbox);
		initialize(_savedInstanceState);
		FirebaseApp.initializeApp(this);
		initializeLogic();
	}

	private void initialize(Bundle _savedInstanceState) {
		parentLayout = findViewById(R.id.parentLayout);
		appBar = findViewById(R.id.appBar);
		contentHolderLayout = findViewById(R.id.contentHolderLayout);
		textview1 = findViewById(R.id.textview1);
		linear1 = findViewById(R.id.linear1);
		imageview3 = findViewById(R.id.imageview3);
		imageview1 = findViewById(R.id.imageview1);
		imageview2 = findViewById(R.id.imageview2);
		viewpager1 = findViewById(R.id.viewpager1);
		bottomnavigation1 = findViewById(R.id.bottomnavigation1);
		fg = new FgFragmentAdapter(getApplicationContext(), getSupportFragmentManager());

		viewpager1.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
			@Override
			public void onPageScrolled(int _position, float _positionOffset, int _positionOffsetPixels) {

			}

			@Override
			public void onPageSelected(int _position) {
				bottomnavigation1.getMenu().getItem(_position).setChecked(true);
			}

			@Override
			public void onPageScrollStateChanged(int _scrollState) {

			}
		});

		bottomnavigation1.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
			@Override
			public boolean onNavigationItemSelected(MenuItem item) {
				final int _itemId = item.getItemId();
				viewpager1.setCurrentItem((int)_itemId);
				return true;
			}
		});
	}

	private void initializeLogic() {
		bottomnavigation1.getMenu().add(0, 0, 0, "Chats").setIcon(R.drawable.icon_message_round);
		bottomnavigation1.getMenu().add(0, 1, 0, "Calls").setIcon(R.drawable.ic_call_48px);
		bottomnavigation1.getMenu().add(0, 2, 0, "Story").setIcon(R.drawable.ic_bookmark);
		fg.setTabCount(3);
		viewpager1.setAdapter(fg);
		viewpager1.setCurrentItem((int)0);
	}

	public class FgFragmentAdapter extends FragmentStatePagerAdapter {
		// This class is deprecated, you should migrate to ViewPager2:
		// https://developer.android.com/reference/androidx/viewpager2/widget/ViewPager2
		Context context;
		int tabCount;

		public FgFragmentAdapter(Context context, FragmentManager manager) {
			super(manager);
			this.context = context;
		}

		public void setTabCount(int tabCount) {
			this.tabCount = tabCount;
		}

		@Override
		public int getCount() {
			return tabCount;
		}

		@Override
		public CharSequence getPageTitle(int _position) {
			return "";
		}


		@Override
		public Fragment getItem(int _position) {
			switch (_position) {
				case 0:
				return new FragInboxChatsActivity();
				case 1:
				return new FragInboxCallsActivity();
				case 2:
				return new FragInboxStoriesActivity();
				// case 3:
				//  return new UserprofileFragmentActivity();
				default:
				return new FragInboxChatsActivity(); // default case
			}
			/*
if (_position == 0) {
return new HomeFragmentActivity();
} else {

}
if (_position == 1) {
return new NotificationsFragmentActivity();
} else {

}
if (_position == 2) {
return new ToolsFragmentActivity();
} else {

}
if (_position == 3) {
return new UserprofileFragmentActivity();
} else {

}
*/
		}

	}

	@Override
	public void onBackPressed() {
		intent.setClass(getApplicationContext(), HomeActivity.class);
		startActivity(intent);
		finish();
	}
}