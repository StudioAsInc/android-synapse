/**
 * CONFIDENTIAL AND PROPRIETARY
 * 
 * This source code is the sole property of StudioAs Inc. Synapse. (Ashik).
 * Any reproduction, modification, distribution, or exploitation in any form
 * without explicit written permission from the owner is strictly prohibited.
 * 
 * Copyright (c) 2025 StudioAs Inc. Synapse. (Ashik)
 * All rights reserved.
 */

package com.synapse.social.studioasinc;

import android.content.Intent;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.synapse.social.studioasinc.model.Attachment;
import com.synapse.social.studioasinc.util.AttachmentUtils;
import com.synapse.social.studioasinc.util.MediaStorageUtils;
import com.synapse.social.studioasinc.util.SystemUIUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class ImageGalleryActivity extends AppCompatActivity {
    private static final String TAG = "ImageGalleryActivity";
    
    private ViewPager2 viewPager;
    private TextView counterText;
    private ImageView closeButton;
    private FloatingActionButton downloadFab;
    private ArrayList<Attachment> attachments;
    private ImageGalleryPagerAdapter adapter;
    private int initialPosition = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Setup immersive mode using modern API
        SystemUIUtils.setupImmersiveActivity(this);
        
        setContentView(R.layout.activity_image_gallery);
        
        initViews();
        handleIntent();
        setupViewPager();
        setupClickListeners();
    }
    
    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        counterText = findViewById(R.id.counterText);
        closeButton = findViewById(R.id.closeButton);
        downloadFab = findViewById(R.id.downloadFab);
    }
    
    private void handleIntent() {
        Intent intent = getIntent();
        if (intent != null) {
            initialPosition = intent.getIntExtra("position", 0);
            
            // Try to get Parcelable attachments first (new format)
            if (intent.hasExtra("attachments_parcelable")) {
                attachments = intent.getParcelableArrayListExtra("attachments_parcelable");
            }
            // Fallback to Serializable format (legacy)
            else if (intent.hasExtra("attachments")) {
                @SuppressWarnings("unchecked")
                ArrayList<HashMap<String, Object>> hashMapAttachments = 
                    (ArrayList<HashMap<String, Object>>) intent.getSerializableExtra("attachments");
                attachments = AttachmentUtils.fromHashMapList(hashMapAttachments);
            }
        }
        
        if (attachments == null || attachments.isEmpty()) {
            finish();
            return;
        }
    }
    
    private void setupViewPager() {
        adapter = new ImageGalleryPagerAdapter(this, attachments);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(initialPosition, false);
        
        updateCounter(initialPosition);
        
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateCounter(position);
            }
        });
    }
    
    private void setupClickListeners() {
        closeButton.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
        });
        
        downloadFab.setOnClickListener(v -> downloadCurrentImage());
        
        // Toggle UI visibility on tap
        findViewById(R.id.rootLayout).setOnClickListener(v -> toggleUIVisibility());
    }
    
    private void updateCounter(int position) {
        String counterFormat = String.format("%d of %d", position + 1, attachments.size());
        counterText.setText(counterFormat);
    }
    
    private void downloadCurrentImage() {
        if (attachments == null || attachments.isEmpty()) return;
        
        int currentPosition = viewPager.getCurrentItem();
        if (currentPosition >= 0 && currentPosition < attachments.size()) {
            Attachment attachment = attachments.get(currentPosition);
            String imageUrl = attachment.getUrl();
            
            if (imageUrl != null && !imageUrl.isEmpty()) {
                downloadImage(imageUrl, attachment);
            } else {
                Toast.makeText(this, "Unable to download image", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    private void downloadImage(String imageUrl, Attachment attachment) {
        // Use modern MediaStore API for image download
        try {
            String fileName = "synapse_image_" + System.currentTimeMillis();
            
            if (attachment.isVideo()) {
                MediaStorageUtils.downloadVideo(this, imageUrl, fileName, new MediaStorageUtils.DownloadCallback() {
                    @Override
                    public void onSuccess(Uri savedUri, String fileName) {
                        runOnUiThread(() -> 
                            Toast.makeText(ImageGalleryActivity.this, "Video saved to gallery", Toast.LENGTH_SHORT).show()
                        );
                    }
                    
                    @Override
                    public void onProgress(int progress) {
                        // Could show progress if needed
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> 
                            Toast.makeText(ImageGalleryActivity.this, "Download failed: " + error, Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            } else {
                MediaStorageUtils.downloadImage(this, imageUrl, fileName, new MediaStorageUtils.DownloadCallback() {
                    @Override
                    public void onSuccess(Uri savedUri, String fileName) {
                        runOnUiThread(() -> 
                            Toast.makeText(ImageGalleryActivity.this, "Image saved to gallery", Toast.LENGTH_SHORT).show()
                        );
                    }
                    
                    @Override
                    public void onProgress(int progress) {
                        // Could show progress if needed
                    }
                    
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> 
                            Toast.makeText(ImageGalleryActivity.this, "Download failed: " + error, Toast.LENGTH_SHORT).show()
                        );
                    }
                });
            }
            
            Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, "Download failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void toggleUIVisibility() {
        boolean isVisible = counterText.getVisibility() == View.VISIBLE;
        int visibility = isVisible ? View.GONE : View.VISIBLE;
        
        counterText.setVisibility(visibility);
        closeButton.setVisibility(visibility);
        downloadFab.setVisibility(visibility);
        
        // Use modern SystemUI utility
        SystemUIUtils.toggleImmersiveMode(this, isVisible);
    }
    
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (viewPager != null && viewPager.getAdapter() != null) {
            viewPager.setAdapter(null);
        }
    }
}