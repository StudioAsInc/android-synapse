package com.synapse.social.studioasinc

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.synapse.social.studioasinc.backend.AuthenticationService
import com.synapse.social.studioasinc.backend.DatabaseService
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView
import java.io.File

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var groupIcon: ImageView
    private lateinit var groupName: EditText
    private lateinit var fabCreateGroup: FloatingActionButton

    private var selectedUsers: ArrayList<String>? = null
    private var imageUri: Uri? = null

    private lateinit var dbService: IDatabaseService
    private lateinit var authService: IAuthenticationService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

        dbService = DatabaseService()
        authService = AuthenticationService()

        groupIcon = findViewById(R.id.group_icon)
        groupName = findViewById(R.id.group_name)
        fabCreateGroup = findViewById(R.id.fab_create_group)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "New Group"

        selectedUsers = intent.getStringArrayListExtra("selected_users")

        groupIcon.setOnClickListener {
            CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(this)
        }

        fabCreateGroup.setOnClickListener {
            createGroup()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)
            if (resultCode == Activity.RESULT_OK) {
                imageUri = result.uri
                Glide.with(this).load(imageUri).into(groupIcon)
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Toast.makeText(this, "Failed to select image: ${result.error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun createGroup() {
        val name = groupName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Group name cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserUid = authService.getCurrentUser()?.getUid()
        if (currentUserUid == null) {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
            return
        }

        val groupRef = dbService.getReference("groups")
        val groupId = groupRef.push().key ?: ""
        if (groupId.isEmpty()) {
            Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri != null) {
            val imagePath = StorageUtil.getPathFromUri(this, imageUri)
            if (imagePath != null) {
                val file = File(imagePath)
                UploadFiles.uploadFile(imagePath, file.name, object : UploadFiles.UploadCallback {
                    override fun onProgress(percent: Int) {
                        // Handle progress if needed
                    }

                    override fun onSuccess(url: String, publicId: String) {
                        saveGroupInfo(groupId, name, url, currentUserUid)
                    }

                    override fun onFailure(error: String) {
                        Toast.makeText(this@CreateGroupActivity, "Failed to upload group icon: $error", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this, "Failed to get image path", Toast.LENGTH_SHORT).show()
            }
        } else {
            saveGroupInfo(groupId, name, "", currentUserUid)
        }
    }

    private fun saveGroupInfo(groupId: String, name: String, iconUrl: String, adminUid: String) {
        val members = selectedUsers?.toMutableList() ?: mutableListOf()
        if (!members.contains(adminUid)) {
            members.add(adminUid)
        }

        val group = hashMapOf<String, Any>(
            "groupId" to groupId,
            "name" to name,
            "icon" to iconUrl,
            "admin" to adminUid,
            "members" to members.associateWith { true }
        )

        val groupRef = dbService.getReference("groups").child(groupId)
        dbService.setValue(groupRef, group, object : com.synapse.social.studioasinc.backend.interfaces.ICompletionListener<Unit> {
            override fun onComplete(result: Unit?, error: Exception?) {
                if (error == null) {
                    Toast.makeText(this@CreateGroupActivity, "Group created successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this@CreateGroupActivity, ChatGroupActivity::class.java)
                    intent.putExtra("uid", groupId)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this@CreateGroupActivity, "Failed to create group: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}