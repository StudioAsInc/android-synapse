package com.synapse.social.studioasinc.chat.group.service

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.backend.interfaces.IDataListener
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseError
import com.synapse.social.studioasinc.backend.interfaces.IDatabaseService

class GroupDetailsLoader(
    private val context: Context,
    private val groupId: String,
    private val topProfileLayoutUsername: TextView,
    private val topProfileLayoutProfileImage: ImageView,
    private val topProfileLayoutGenderBadge: ImageView,
    private val topProfileLayoutVerifiedBadge: ImageView,
    private val topProfileLayoutStatus: TextView,
    private val dbService: IDatabaseService
) {



    fun loadGroupDetails() {
        val groupRef = dbService.getReference("groups").child(groupId)
        dbService.getData(groupRef, object : IDataListener {
            @Suppress("UNCHECKED_CAST")
            override fun onDataChange(dataSnapshot: IDataSnapshot) {
                if (dataSnapshot.exists()) {
                    val group = dataSnapshot.getValue(Map::class.java) as Map<String, Any?>
                    topProfileLayoutUsername.text = group["name"] as? String
                    val iconUrl = group["icon"] as? String
                    if (iconUrl != null) {
                        Glide.with(context).load(Uri.parse(iconUrl)).into(topProfileLayoutProfileImage)
                    }
                    topProfileLayoutGenderBadge.visibility = View.GONE
                    topProfileLayoutVerifiedBadge.visibility = View.GONE
                    topProfileLayoutStatus.text = "Group"
                }
            }

            override fun onCancelled(databaseError: IDatabaseError) {
                // Handle error
            }
        })
    }
}