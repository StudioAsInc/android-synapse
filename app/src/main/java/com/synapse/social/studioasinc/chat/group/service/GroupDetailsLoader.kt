package com.synapse.social.studioasinc.chat.group.service

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.compatibility.FirebaseDatabase
import com.synapse.social.studioasinc.compatibility.DatabaseReference
import com.synapse.social.studioasinc.compatibility.ValueEventListener
import com.synapse.social.studioasinc.compatibility.DataSnapshot
import com.synapse.social.studioasinc.compatibility.DatabaseError

class GroupDetailsLoader(
    private val context: Context,
    private val groupId: String,
    private val topProfileLayoutUsername: TextView,
    private val topProfileLayoutProfileImage: ImageView,
    private val topProfileLayoutGenderBadge: ImageView,
    private val topProfileLayoutVerifiedBadge: ImageView,
    private val topProfileLayoutStatus: TextView
) {

    private val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()

    fun loadGroupDetails() {
        val groupRef: DatabaseReference = firebaseDatabase.getReference("groups").child(groupId)
        groupRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    topProfileLayoutUsername.text = dataSnapshot.child("name").getValue(String::class.java)
                    val iconUrl = dataSnapshot.child("icon").getValue(String::class.java)
                    if (iconUrl != null) {
                        Glide.with(context).load(Uri.parse(iconUrl)).into(topProfileLayoutProfileImage)
                    }
                    topProfileLayoutGenderBadge.visibility = View.GONE
                    topProfileLayoutVerifiedBadge.visibility = View.GONE
                    topProfileLayoutStatus.text = "Group"
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }
}