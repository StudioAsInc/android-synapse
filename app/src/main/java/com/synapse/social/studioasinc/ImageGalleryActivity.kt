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

package com.synapse.social.studioasinc

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.synapse.social.studioasinc.model.Attachment
import com.synapse.social.studioasinc.util.AttachmentUtils
import com.synapse.social.studioasinc.util.MediaStorageUtils
import com.synapse.social.studioasinc.util.SystemUIUtils

class ImageGalleryActivity : AppCompatActivity() {
    
    companion object {
        private const val TAG = "ImageGalleryActivity"
    }
    
    private lateinit var viewPager: ViewPager2
    private lateinit var counterText: TextView
    private lateinit var closeButton: ImageView
    private lateinit var downloadFab: FloatingActionButton
    private lateinit var rootLayout: View
    
    private var attachments: ArrayList<Attachment>? = null
    private var adapter: ImageGalleryPagerAdapter? = null
    private var initialPosition: Int = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Setup immersive mode using modern API
        SystemUIUtils.setupImmersiveActivity(this)
        
        setContentView(R.layout.activity_image_gallery)
        
        initViews()
        handleIntent()
        setupViewPager()
        setupClickListeners()
    }
    
    private fun initViews() {
        viewPager = findViewById(R.id.viewPager)
        counterText = findViewById(R.id.counterText)
        closeButton = findViewById(R.id.closeButton)
        downloadFab = findViewById(R.id.downloadFab)
        rootLayout = findViewById(R.id.rootLayout)
    }
    
    private fun handleIntent() {
        intent?.let { intent ->
            initialPosition = intent.getIntExtra("position", 0)
            
            // Try to get Parcelable attachments first (new format)
            attachments = if (intent.hasExtra("attachments_parcelable")) {
                intent.getParcelableArrayListExtra("attachments_parcelable")
            }
            // Fallback to Serializable format (legacy)
            else if (intent.hasExtra("attachments")) {
                @Suppress("UNCHECKED_CAST", "DEPRECATION")
                val hashMapAttachments = intent.getSerializableExtra("attachments") 
                    as? ArrayList<HashMap<String, Any>>
                hashMapAttachments?.let { AttachmentUtils.fromHashMapList(it) }
            } else {
                null
            }
        }
        
        if (attachments.isNullOrEmpty()) {
            finish()
            return
        }
    }
    
    private fun setupViewPager() {
        val attachmentList = attachments ?: return
        
        adapter = ImageGalleryPagerAdapter(this, attachmentList)
        viewPager.adapter = adapter
        viewPager.setCurrentItem(initialPosition, false)
        
        updateCounter(initialPosition)
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateCounter(position)
            }
        })
    }
    
    private fun setupClickListeners() {
        closeButton.setOnClickListener {
            finish()
            overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
        }
        
        downloadFab.setOnClickListener {
            downloadCurrentImage()
        }
        
        // Toggle UI visibility on tap
        rootLayout.setOnClickListener {
            toggleUIVisibility()
        }
    }
    
    private fun updateCounter(position: Int) {
        val attachmentList = attachments ?: return
        counterText.text = String.format("%d of %d", position + 1, attachmentList.size)
    }
    
    private fun downloadCurrentImage() {
        val attachmentList = attachments ?: return
        if (attachmentList.isEmpty()) return
        
        val currentPosition = viewPager.currentItem
        if (currentPosition in attachmentList.indices) {
            val attachment = attachmentList[currentPosition]
            val imageUrl = attachment.url
            
            if (!imageUrl.isNullOrEmpty()) {
                downloadImage(imageUrl, attachment)
            } else {
                Toast.makeText(this, "Unable to download image", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    private fun downloadImage(imageUrl: String, attachment: Attachment) {
        try {
            val fileName = "synapse_image_${System.currentTimeMillis()}"
            
            val callback = object : MediaStorageUtils.DownloadCallback {
                override fun onSuccess(savedUri: Uri, fileName: String) {
                    runOnUiThread {
                        val message = if (attachment.isVideo) {
                            "Video saved to gallery"
                        } else {
                            "Image saved to gallery"
                        }
                        Toast.makeText(this@ImageGalleryActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
                
                override fun onProgress(progress: Int) {
                    // Could show progress if needed
                }
                
                override fun onError(error: String) {
                    runOnUiThread {
                        Toast.makeText(
                            this@ImageGalleryActivity,
                            "Download failed: $error",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            
            if (attachment.isVideo) {
                MediaStorageUtils.downloadVideo(this, imageUrl, fileName, callback)
            } else {
                MediaStorageUtils.downloadImage(this, imageUrl, fileName, callback)
            }
            
            Toast.makeText(this, "Download started...", Toast.LENGTH_SHORT).show()
            
        } catch (e: Exception) {
            Toast.makeText(this, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun toggleUIVisibility() {
        val isVisible = counterText.visibility == View.VISIBLE
        val visibility = if (isVisible) View.GONE else View.VISIBLE
        
        counterText.visibility = visibility
        closeButton.visibility = visibility
        downloadFab.visibility = visibility
        
        // Use modern SystemUI utility
        SystemUIUtils.toggleImmersiveMode(this, isVisible)
    }
    
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        finish()
        overridePendingTransition(R.anim.slide_in_from_left, R.anim.slide_out_to_right)
    }
    
    override fun onDestroy() {
        super.onDestroy()
        viewPager.adapter = null
        adapter = null
    }
}
