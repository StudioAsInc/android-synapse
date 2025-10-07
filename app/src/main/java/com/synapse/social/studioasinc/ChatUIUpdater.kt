package com.synapse.social.studioasinc

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.backend.interfaces.IAuthenticationService
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot

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
    private val authService: IAuthenticationService
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

    fun updateUserProfile(dataSnapshot: IDataSnapshot) {
        @Suppress("UNCHECKED_CAST")
        val user = dataSnapshot.getValue(Map::class.java) as Map<String, Any?>
        val nickname = user["nickname"] as? String
        val username = user["username"] as? String
        val status = user["status"] as? String
        val gender = user["gender"] as? String
        val verified = user["verified"] as? Boolean
        val avatarUrl = user["avatar_url"] as? String

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
        authService.getCurrentUser()?.let {
            val isMyMessage = it.getUid() == messageData[ChatConstants.UID_KEY].toString()
            mMessageReplyLayoutBodyRightUsername.text = if (isMyMessage) firstUserName else secondUserName
            mMessageReplyLayoutBodyRightMessage.text = messageData[ChatConstants.MESSAGE_TEXT_KEY].toString()
            mMessageReplyLayout.visibility = View.VISIBLE
        }
    }

    fun hideReplyUI() {
        mMessageReplyLayout.visibility = View.GONE
    }

    fun showLoadMoreIndicator() {
        activity._showLoadMoreIndicator()
    }

    fun hideLoadMoreIndicator() {
        activity._hideLoadMoreIndicator()
    }
}