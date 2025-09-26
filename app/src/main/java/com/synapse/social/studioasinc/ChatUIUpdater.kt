package com.synapse.social.studioasinc

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.synapse.social.studioasinc.R

class ChatUIUpdater(
    private val activity: ChatActivity,
    private val noChatText: TextView,
    private val chatMessagesListRecycler: RecyclerView,
    private val topProfileLayoutProfileImage: ImageView,
    private val topProfileLayoutUsername: TextView,
    private val topProfileLayoutStatus: TextView,
    private val topProfileLayoutGenderBadge: ImageView,
    private val topProfileLayoutVerifiedBadge: ImageView,
    private val mMessageReplyLayout: LinearLayout,
    private val mMessageReplyLayoutBodyRightUsername: TextView,
    private val mMessageReplyLayoutBodyRightMessage: TextView,
    private val auth: FirebaseAuth
) {

    fun updateNoChatVisibility(isEmpty: Boolean) {
        if (isEmpty) {
            noChatText.visibility = View.VISIBLE
            chatMessagesListRecycler.visibility = View.GONE
        } else {
            noChatText.visibility = View.GONE
            chatMessagesListRecycler.visibility = View.VISIBLE
        }
    }

    fun updateUserProfile(dataSnapshot: DataSnapshot) {
        val nickname = dataSnapshot.child("nickname").getValue(String::class.java)
        val username = dataSnapshot.child("username").getValue(String::class.java)
        val status = dataSnapshot.child("status").getValue(String::class.java)
        val gender = dataSnapshot.child("gender").getValue(String::class.java)
        val verified = dataSnapshot.child("verified").getValue(Boolean::class.java)
        val avatarUrl = dataSnapshot.child("avatar_url").getValue(String::class.java)

        if (nickname != null && nickname != "null") {
            topProfileLayoutUsername.text = nickname
        } else if (username != null && username != "null") {
            topProfileLayoutUsername.text = "@$username"
        } else {
            topProfileLayoutUsername.text = "Unknown User"
        }

        topProfileLayoutStatus.text = status ?: "Offline"

        if (gender != null && gender == "male") {
            topProfileLayoutGenderBadge.setImageResource(R.drawable.ic_male)
            topProfileLayoutGenderBadge.visibility = View.VISIBLE
        } else {
            // NOTE: ic_female.xml does not exist in the drawables. Hiding the badge for now.
            topProfileLayoutGenderBadge.visibility = View.GONE
        }

        if (verified == true) {
            topProfileLayoutVerifiedBadge.visibility = View.VISIBLE
        } else {
            topProfileLayoutVerifiedBadge.visibility = View.GONE
        }

        Glide.with(activity).load(avatarUrl).into(topProfileLayoutProfileImage)
    }

    fun showReplyUI(firstUserName: String, secondUserName: String, messageData: HashMap<String, Any>) {
        val isMyMessage = auth.currentUser!!.uid == messageData[ChatConstants.UID_KEY].toString()
        mMessageReplyLayoutBodyRightUsername.text = if (isMyMessage) firstUserName else secondUserName
        mMessageReplyLayoutBodyRightMessage.text = messageData[ChatConstants.MESSAGE_TEXT_KEY].toString()
        mMessageReplyLayout.visibility = View.VISIBLE
    }

    fun hideReplyUI() {
        mMessageReplyLayout.visibility = View.GONE
    }
}