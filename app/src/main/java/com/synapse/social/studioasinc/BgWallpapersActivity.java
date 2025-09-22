package com.synapse.social.studioasinc;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BgWallpapersActivity extends AppCompatActivity {
    
    private GridView gridview1;
    
    private WallpaperAdapter wallpaperAdapter;
    private ArrayList<HashMap<String, Object>> wallpapersList = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bg_wallpapers);
        initializeViews();
        setupGridView();
        wallpapersList.addAll(getWallpapersList());
        wallpaperAdapter.notifyDataSetChanged();

        // Set back button click listener
        findViewById(R.id.back).setOnClickListener(v -> finish());
    }
    
    private void initializeViews() {
        gridview1 = findViewById(R.id.gridview1);
        
        // Set title
        ((TextView)findViewById(R.id.title)).setText("Chat Backgrounds");
    }
    
    private void setupGridView() {
        wallpaperAdapter = new WallpaperAdapter(this, wallpapersList);
        gridview1.setAdapter(wallpaperAdapter);
        
        // Set click listener for GridView items
        gridview1.setOnItemClickListener((parent, view, position, id) -> {
            HashMap<String, Object> wallpaper = wallpapersList.get(position);
            String selectedUrl = wallpaper.get("imageUrl").toString();

            // Save to preferences
            SharedPreferences theme = getSharedPreferences("theme", MODE_PRIVATE);
            SharedPreferences.Editor editor = theme.edit();
            editor.putString("chat_background_url", selectedUrl);
            editor.apply();

            Toast.makeText(BgWallpapersActivity.this, 
                "Wallpaper set to: " + wallpaper.get("name"),
                Toast.LENGTH_SHORT).show();

            // Finish activity and go back
            finish();
        });
    }
    
    private ArrayList<HashMap<String, Object>> getWallpapersList() {
        ArrayList<HashMap<String, Object>> wallpapers = new ArrayList<>();

        // Patterns
        String patternBaseUrl = "https://raw.githubusercontent.com/labStudioAsInc/host/refs/heads/main/images/synapse/chatbg/";
        for (int i = 1; i <= 5; i++) {
            HashMap<String, Object> map = new HashMap<>();
            map.put("name", "Pattern " + i);
            String url = patternBaseUrl + "pattern%20(" + i + ").png";
            map.put("imageUrl", url);
            map.put("thumbnailUrl", url);
            wallpapers.add(map);
        }
        HashMap<String, Object> map = new HashMap<>();
        map.put("name", "Pattern");
        String url = patternBaseUrl + "pattern.png";
        map.put("imageUrl", url);
        map.put("thumbnailUrl", url);
        wallpapers.add(map);

        // Wallpapers
        String wallpaperBaseUrl = "https://raw.githubusercontent.com/labStudioAsInc/host/refs/heads/main/images/synapse/chatbg/";
        for (int i = 1; i <= 31; i++) {
            HashMap<String, Object> wallpaperMap = new HashMap<>();
            wallpaperMap.put("name", "Wallpaper " + i);
            String wallpaperUrl = wallpaperBaseUrl + "wallpaper%20(" + i + ").jpg";
            wallpaperMap.put("imageUrl", wallpaperUrl);
            wallpaperMap.put("thumbnailUrl", wallpaperUrl);
            wallpapers.add(wallpaperMap);
        }
        HashMap<String, Object> wallpaperMap = new HashMap<>();
        wallpaperMap.put("name", "Wallpaper");
        String wallpaperUrl = wallpaperBaseUrl + "wallpaper.jpg";
        wallpaperMap.put("imageUrl", wallpaperUrl);
        wallpaperMap.put("thumbnailUrl", wallpaperUrl);
        wallpapers.add(wallpaperMap);

        return wallpapers;
    }
    
    // BaseAdapter for GridView
    private class WallpaperAdapter extends BaseAdapter {
        
        private Context context;
        private final ArrayList<HashMap<String, Object>> wallpapers;
        
        public WallpaperAdapter(Context context, ArrayList<HashMap<String, Object>> wallpapers) {
            this.context = context;
            this.wallpapers = wallpapers;
        }
        
        @Override
        public int getCount() {
            return wallpapers.size();
        }
        
        @Override
        public Object getItem(int position) {
            return wallpapers.get(position);
        }
        
        @Override
        public long getItemId(int position) {
            return position;
        }
        
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            
            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.cv_item_wallpapers, parent, false);
                holder = new ViewHolder();
                holder.imageview1 = convertView.findViewById(R.id.imageview1);
                holder.textview3 = convertView.findViewById(R.id.textview3);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            
            HashMap<String, Object> wallpaper = wallpapers.get(position);
            
            holder.textview3.setText(wallpaper.get("name").toString());
            
            Glide.with(context)
                .load(wallpaper.get("thumbnailUrl").toString())
                .placeholder(R.drawable.default_image)
                .error(R.drawable.default_image)
                .into(holder.imageview1);
            
            return convertView;
        }
        
        private class ViewHolder {
            ImageView imageview1;
            TextView textview3;
        }
    }
}