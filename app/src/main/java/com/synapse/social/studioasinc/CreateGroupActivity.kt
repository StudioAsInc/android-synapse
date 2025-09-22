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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView

class CreateGroupActivity : AppCompatActivity() {

    private lateinit var groupIcon: ImageView
    private lateinit var groupName: EditText
    private lateinit var fabCreateGroup: FloatingActionButton

    private var selectedUsers: ArrayList<String>? = null
    private var imageUri: Uri? = null

    private val database = FirebaseDatabase.getInstance().reference
    private val storage = FirebaseStorage.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_group)

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

        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid == null) {
            Toast.makeText(this, "Authentication error", Toast.LENGTH_SHORT).show()
            return
        }

        val groupId = database.child("groups").push().key ?: ""
        if (groupId.isEmpty()) {
            Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show()
            return
        }

        if (imageUri != null) {
            val fileRef = storage.child("group_icons/$groupId.jpg")
            fileRef.putFile(imageUri!!)
                .addOnSuccessListener {
                    fileRef.downloadUrl.addOnSuccessListener { uri ->
                        saveGroupInfo(groupId, name, uri.toString(), currentUserUid)
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Failed to upload group icon", Toast.LENGTH_SHORT).show()
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

        val group = hashMapOf(
            "groupId" to groupId,
            "name" to name,
            "icon" to iconUrl,
            "admin" to adminUid,
            "members" to members.associateWith { true }
        )

        database.child("groups").child(groupId).setValue(group)
            .addOnSuccessListener {
                Toast.makeText(this, "Group created successfully", Toast.LENGTH_SHORT).show()
                // Navigate to the group chat activity
                val intent = Intent(this, ChatActivity::class.java)
                intent.putExtra("uid", groupId)
                intent.putExtra("isGroup", true)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to create group", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
