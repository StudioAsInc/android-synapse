package com.synapse.social.studioasinc;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.media.*;
import android.net.*;
import android.net.Uri;
import android.os.*;
import android.os.Bundle;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.*;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.gridlayout.*;
import androidx.recyclerview.widget.*;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import androidx.recyclerview.widget.RecyclerView.ViewHolder;
import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.theartofdev.edmodo.cropper.*;
import com.yalantis.ucrop.*;
import java.io.*;
import java.io.InputStream;
import java.text.*;
import java.util.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.*;
import org.json.*;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.postgrest.PostgrestClient;
import io.github.jan.supabase.postgrest.PostgrestQuery;
import io.github.jan.supabase.postgrest.rpc.PostgrestRpc;
import io.github.jan.supabase.postgrest.result.PostgrestResult;
import io.github.jan.supabase.postgrest.Postgrest;
import io.github.jan.supabase.postgrest.PostgrestCallback;
import io.github.jan.supabase.postgrest.PostgrestResponse;
import io.github.jan.supabase.postgrest.PostgrestError;
import java.util.Map;
import java.util.List;
import com.synapse.social.studioasinc.Supabase;

public class SelectRegionActivity extends AppCompatActivity {
	
	private SupabaseClient supabaseClient;
	
	private String CurrentRegionCode = "";
	
	private ArrayList<HashMap<String, Object>> regionsList = new ArrayList<>();
	
	private LinearLayout body;
	private LinearLayout top;
	private RecyclerView mRegionList;
	private LinearLayout mLoadingBody;
	private ImageView mBack;
	private TextView mTitle;
	private ImageView spc;
	private ProgressBar mLoadingBar;
	
	private Intent intent = new Intent();
	private IAuthenticationService authService;
	private IDatabaseService dbService;
	private PostgrestClient main = supabaseClient.from("skyline");
	
	private RequestNetwork getRegionsRef;
	private RequestNetwork.RequestListener _getRegionsRef_request_listener;
	private Vibrator vbr;
	private SharedPreferences appSavedData;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.activity_select_region);
		initialize(_savedInstanceState);
		authService = new AuthenticationService(SynapseApp.supabaseClient);
		dbService = new DatabaseService(SynapseApp.supabaseClient);
		initializeLogic();
	}
	
	private void initialize(Bundle _savedInstanceState) {
		body = findViewById(R.id.body);
		top = findViewById(R.id.top);
		mRegionList = findViewById(R.id.mRegionList);
		mLoadingBody = findViewById(R.id.mLoadingBody);
		mBack = findViewById(R.id.mBack);
		mTitle = findViewById(R.id.mTitle);
		spc = findViewById(R.id.spc);
		mLoadingBar = findViewById(R.id.mLoadingBar);
        supabaseClient = Supabase.client;
		getRegionsRef = new RequestNetwork(this);
		vbr = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
		appSavedData = getSharedPreferences("data", Activity.MODE_PRIVATE);
		
		mBack.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View _view) {
				onBackPressed();
			}
		});
		

		
		_getRegionsRef_request_listener = new RequestNetwork.RequestListener() {
			@Override
			public void onResponse(String _param1, String _param2, HashMap<String, Object> _param3) {
				final String _tag = _param1;
				final String _response = _param2;
				final HashMap<String, Object> _responseHeaders = _param3;
				regionsList = new Gson().fromJson(_response, new TypeToken<ArrayList<HashMap<String, Object>>>(){}.getType());
				mRegionList.setAdapter(new MRegionListAdapter(regionsList));
				_getCurrentRegionRef();
				mRegionList.setVisibility(View.VISIBLE);
				mLoadingBody.setVisibility(View.GONE);
			}
			
			@Override
			public void onErrorResponse(String _param1, String _param2) {
				final String _tag = _param1;
				final String _message = _param2;
				
			}
		};
	}
	
	private void initializeLogic() {
		_stateColor(0xFFFFFFFF, 0xFFF5F5F5);
		_viewGraphics(mBack, 0xFFFFFFFF, 0xFFEEEEEE, 300, 0, Color.TRANSPARENT);
		top.setElevation((float)4);
		mRegionList.setVisibility(View.GONE);
		mLoadingBody.setVisibility(View.VISIBLE);
		mRegionList.setLayoutManager(new LinearLayoutManager(this));
		_getRegionsStart();
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
	
	
	public void _ImageColor(final ImageView _image, final int _color) {
		_image.setColorFilter(_color,PorterDuff.Mode.SRC_ATOP);
	}
	
	
	public void _viewGraphics(final View _view, final int _onFocus, final int _onRipple, final double _radius, final double _stroke, final int _strokeColor) {
		android.graphics.drawable.GradientDrawable GG = new android.graphics.drawable.GradientDrawable();
		GG.setColor(_onFocus);
		GG.setCornerRadius((float)_radius);
		GG.setStroke((int) _stroke, _strokeColor);
		android.graphics.drawable.RippleDrawable RE = new android.graphics.drawable.RippleDrawable(new android.content.res.ColorStateList(new int[][]{new int[]{}}, new int[]{ _onRipple}), GG, null);
		_view.setBackground(RE);
	}
	
	
	public void _setMargin(final View _view, final double _r, final double _l, final double _t, final double _b) {
		float dpRatio = new c(this).getContext().getResources().getDisplayMetrics().density;
		int right = (int)(_r * dpRatio);
		int left = (int)(_l * dpRatio);
		int top = (int)(_t * dpRatio);
		int bottom = (int)(_b * dpRatio);
		
		boolean _default = false;
		
		ViewGroup.LayoutParams p = _view.getLayoutParams();
		if (p instanceof LinearLayout.LayoutParams) {
			LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams)p;
			lp.setMargins(left, top, right, bottom);
			_view.setLayoutParams(lp);
		}
		else if (p instanceof RelativeLayout.LayoutParams) {
			RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams)p;
			lp.setMargins(left, top, right, bottom);
			_view.setLayoutParams(lp);
		}
		else if (p instanceof TableRow.LayoutParams) {
			TableRow.LayoutParams lp = (TableRow.LayoutParams)p;
			lp.setMargins(left, top, right, bottom);
			_view.setLayoutParams(lp);
		}
		
		
	}
	
	class c {
		Context co;
		public <T extends Activity> c(T a) {
			co = a;
		}
		public <T extends Fragment> c(T a) {
			co = a.getActivity();
		}
		public <T extends DialogFragment> c(T a) {
			co = a.getActivity();
		}
		
		public Context getContext() {
			return co;
		}
		
	}
	
	
	{
		
	}
	
	
	public void _getRegionsStart() {
		if (getResources().getString(R.string.lang).equals("en")) {
			getRegionsRef.startRequestNetwork(RequestNetworkController.GET, "https://unepix.github.io/regions/countries-en.json", "regions", _getRegionsRef_request_listener);
		} else {
			if (getResources().getString(R.string.lang).equals("tr")) {
				getRegionsRef.startRequestNetwork(RequestNetworkController.GET, "https://unepix.github.io/regions/countries-tr.json", "regions", _getRegionsRef_request_listener);
			} else {
				getRegionsRef.startRequestNetwork(RequestNetworkController.GET, "https://unepix.github.io/regions/countries-en.json", "regions", _getRegionsRef_request_listener);
			}
		}
	}
	
	
	public void _getCurrentRegionRef() {
        dbService.getData(dbService.getReference("users/" + authService.getCurrentUser().getUid()), new IDataListener() {
            @Override
            public void onDataChange(IDataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    String region = dataSnapshot.child("user_region").getValue(String.class);
                    if (region != null) {
                        CurrentRegionCode = region;
                        appSavedData.edit().putString("user_region_data", CurrentRegionCode).commit();
                    } else {
                        CurrentRegionCode = "none";
                        appSavedData.edit().putString("user_region_data", "none").commit();
                    }
                } else {
                    CurrentRegionCode = "none";
                    appSavedData.edit().putString("user_region_data", "none").commit();
                }
                if(mRegionList.getAdapter() != null) {
                    mRegionList.getAdapter().notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(IDatabaseError databaseError) {
                Toast.makeText(getApplicationContext(), databaseError.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
	}
	
	public class MRegionListAdapter extends RecyclerView.Adapter<MRegionListAdapter.ViewHolder> {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public MRegionListAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
			LayoutInflater _inflater = getLayoutInflater();
			View _v = _inflater.inflate(R.layout.region_list_synapse, null);
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_v.setLayoutParams(_lp);
			return new ViewHolder(_v);
		}
		
		@Override
		public void onBindViewHolder(ViewHolder _holder, final int _position) {
			View _view = _holder.itemView;
			
			final LinearLayout body = _view.findViewById(R.id.body);
			final androidx.cardview.widget.CardView flagCard = _view.findViewById(R.id.flagCard);
			final TextView name = _view.findViewById(R.id.name);
			final ImageView checkbox = _view.findViewById(R.id.checkbox);
			final ImageView flag = _view.findViewById(R.id.flag);
			
			RecyclerView.LayoutParams _lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
			_view.setLayoutParams(_lp);
			_viewGraphics(body, 0xFFFFFFFF, 0xFFEEEEEE, 28, 0, Color.TRANSPARENT);
			if (_position == 0) {
				_setMargin(body, 14, 14, 14, 14);
			} else {
				_setMargin(body, 14, 14, 0, 14);
			}
			Glide.with(getApplicationContext()).load(Uri.parse("https://flagcdn.com/w640/".concat(_data.get((int)_position).get("code").toString().concat(".png")))).into(flag);
			name.setText(_data.get((int)_position).get("name").toString());
			if (CurrentRegionCode.equals(_data.get((int)_position).get("code").toString())) {
				checkbox.setImageResource(R.drawable.icon_check_circle_round);
			} else {
				checkbox.setImageResource(R.drawable.icon_radio_button_unchecked_round);
			}
			body.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					if (!CurrentRegionCode.equals(_data.get((int)_position).get("code").toString())) {
						dbService.getReference("users/" + authService.getCurrentUser().getUid() + "/user_region").setValue(_data.get((int)_position).get("code").toString(), null);
					}
					vbr.vibrate((long)(28));
				}
			});
		}
		
		@Override
		public int getItemCount() {
			return _data.size();
		}
		
		public class ViewHolder extends RecyclerView.ViewHolder {
			public ViewHolder(View v) {
				super(v);
			}
		}
	}
}
