package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.Uri
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.model.User
import io.noties.markwon.Markwon
import java.text.SimpleDateFormat
import java.util.Calendar

/**
 * Manages the user profile screen, responsible for fetching user data
 * and populating the UI components.
 *
 * @param context The context.
 * @param markwon An instance of Markwon for rendering Markdown text.
 */
class UserProfileManager(
    private val context: Context,
    private val markwon: Markwon
) {

    /**
     * Loads the user profile data from Firebase and populates the UI.
     *
     * @param uid The user ID of the profile to load.
     * @param currentUid The user ID of the current user.
     * @param views A data class containing the views to populate.
     */
    fun loadUserProfile(uid: String, currentUid: String, views: ProfileViews) {
        val userRef = FirebaseDatabase.getInstance().getReference("skyline/users").child(uid)

        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val user = snapshot.getValue(User::class.java)
                    user?.let {
                        populateUI(it, currentUid, views)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle database error
            }
        })
    }

    /**
     * Populates the UI components with the user's data.
     *
     * @param user The user data.
     * @param currentUid The user ID of the current user.
     * @param views A data class containing the views to populate.
     */
    private fun populateUI(user: User, currentUid: String, views: ProfileViews) {
        if (user.banned == "true") {
            handleBannedUser(views)
            return
        }

        loadImages(user, views)
        setProfileDetails(user, views)
        setButtonVisibility(user.uid, currentUid, views)
    }

    /**
     * Handles the UI state for a banned user.
     *
     * @param views A data class containing the views to populate.
     */
    private fun handleBannedUser(views: ProfileViews) {
        views.profileImage.setImageResource(R.drawable.banned_avatar)
        views.coverImage.setImageResource(R.drawable.banned_cover_photo)
        // Optionally, disable buttons or show a "banned" message
    }

    /**
     * Loads the user's profile and cover images using Glide.
     *
     * @param user The user data.
     * @param views A data class containing the views to populate.
     */
    private fun loadImages(user: User, views: ProfileViews) {
        if (user.avatar == "null") {
            views.profileImage.setImageResource(R.drawable.avatar)
        } else {
            Glide.with(context).load(Uri.parse(user.avatar)).into(views.profileImage)
        }

        if (user.profile_cover_image == "null") {
            views.coverImage.setImageResource(R.drawable.user_null_cover_photo)
        } else {
            Glide.with(context).load(Uri.parse(user.profile_cover_image)).into(views.coverImage)
        }
    }

    /**
     * Sets the user's profile details, such as name, bio, and stats.
     *
     * @param user The user data.
     * @param views A data class containing the views to populate.
     */
    private fun setProfileDetails(user: User, views: ProfileViews) {
        views.nickname.text = if (user.nickname == "null") "@${user.username}" else user.nickname
        views.username.text = "@${user.username}"

        if (user.biography != "null") {
            markwon.setMarkdown(views.bio, user.biography)
        }

        // Additional details like join date, followers, etc. can be set here
    }

    /**
     * Sets the visibility of the "Edit Profile" or "Follow/Message" buttons.
     *
     * @param profileUid The user ID of the profile being viewed.
     * @param currentUid The user ID of the current user.
     * @param views A data class containing the views to populate.
     */
    private fun setButtonVisibility(profileUid: String, currentUid: String, views: ProfileViews) {
        if (profileUid == currentUid) {
            views.btnEditProfile.visibility = View.VISIBLE
            views.secondaryButtons.visibility = View.GONE
        } else {
            views.btnEditProfile.visibility = View.GONE
            views.secondaryButtons.visibility = View.VISIBLE
        }
    }

    /**
     * Data class to hold the views that need to be populated.
     */
    data class ProfileViews(
        val profileImage: ImageView,
        val coverImage: ImageView,
        val nickname: TextView,
        val username: TextView,
        val bio: TextView,
        val btnEditProfile: Button,
        val secondaryButtons: View
    )
}
