// To-do: Migrate Firebase to Supabase
// This class is responsible for updating the UI and is tightly coupled to Firebase data structures.
// 1. **Remove Firebase Dependencies**:
//    - Remove the `FirebaseAuth` and `DataSnapshot` imports.
//
// 2. **Refactor `updateUserProfile`**:
//    - The `updateUserProfile` method currently accepts a Firebase `DataSnapshot`.
//    - This method should be updated to accept a type-safe data class (e.g., `UserProfile.kt`) that represents a user's profile data from Supabase.
//    - The logic for extracting data (e.g., `dataSnapshot.child("nickname").getValue(String::class.java)`) will be replaced with property access on the new data class (e.g., `userProfile.nickname`).
//
// 3. **Refactor `showReplyUI`**:
//    - This method uses `auth.currentUser!!.uid` to determine if a message is from the current user.
//    - The `auth` dependency should be removed. The current user's ID should be passed into this method or the class's constructor from the `ChatActivity` or a ViewModel.
//    - The `messageData` parameter, which is a `HashMap`, should be replaced with a `Message` data class.
//
// 4. **Decouple from `ChatActivity`**:
//    - The calls to `activity._showLoadMoreIndicator()` and `activity._hideLoadMoreIndicator()` create a tight coupling with `ChatActivity`.
//    - Consider using an interface to communicate these events back to the activity or, preferably, manage this state in a shared ViewModel.

package com.synapse.social.studioasinc

import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot

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

    fun showLoadMoreIndicator() {
        activity._showLoadMoreIndicator()
    }

    fun hideLoadMoreIndicator() {
        activity._hideLoadMoreIndicator()
    }
}