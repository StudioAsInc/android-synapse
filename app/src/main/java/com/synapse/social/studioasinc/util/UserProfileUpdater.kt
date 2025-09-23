package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.synapse.social.studioasinc.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

class UserProfileUpdater(
    private val context: Context,
    private val topProfileLayoutProfileImage: ImageView,
    private val topProfileLayoutUsername: TextView,
    private val topProfileLayoutStatus: TextView,
    private val topProfileLayoutGenderBadge: ImageView,
    private val topProfileLayoutVerifiedBadge: ImageView
) {

    var secondUserName: String = "Unknown User"
    var secondUserAvatar: String = "null"

    fun updateAll(dataSnapshot: DataSnapshot) {
        updateUserProfile(dataSnapshot)
        updateUserBadges(dataSnapshot)
    }

    private fun updateUserProfile(dataSnapshot: DataSnapshot) {
        if (!dataSnapshot.exists()) {
            Log.w("UserProfileUpdater", "User profile data snapshot is null or doesn't exist")
            return
        }

        if ("true" == dataSnapshot.child("banned").getValue(String::class.java)) {
            topProfileLayoutProfileImage.setImageResource(R.drawable.banned_avatar)
            secondUserAvatar = "null_banned"
            topProfileLayoutStatus.setTextColor(0xFF9E9E9E)
            topProfileLayoutStatus.text = context.getString(R.string.offline)
        } else {
            val avatarUrl = dataSnapshot.child("avatar").getValue(String::class.java)
            if (avatarUrl == null || avatarUrl == "null") {
                topProfileLayoutProfileImage.setImageResource(R.drawable.avatar)
                secondUserAvatar = "null"
            } else {
                try {
                    Glide.with(context).load(Uri.parse(avatarUrl)).into(topProfileLayoutProfileImage)
                    secondUserAvatar = avatarUrl
                } catch (e: Exception) {
                    Log.e("UserProfileUpdater", "Error loading avatar: " + e.message)
                    topProfileLayoutProfileImage.setImageResource(R.drawable.avatar)
                    secondUserAvatar = "null"
                }
            }
        }

        val nickname = dataSnapshot.child("nickname").getValue(String::class.java)
        secondUserName = if (nickname == null || nickname == "null") {
            val username = dataSnapshot.child("username").getValue(String::class.java)
            if (username != null) "@$username" else "Unknown User"
        } else {
            nickname
        }
        topProfileLayoutUsername.text = secondUserName

        val status = dataSnapshot.child("status").getValue(String::class.java)
        if ("online" == status) {
            topProfileLayoutStatus.text = context.getString(R.string.online)
            topProfileLayoutStatus.setTextColor(0xFF2196F3)
        } else {
            if ("offline" == status) {
                topProfileLayoutStatus.text = context.getString(R.string.offline)
            } else {
                try {
                    val statusTimestamp = status?.toDouble()
                    if (statusTimestamp != null) {
                        setUserLastSeen(statusTimestamp, topProfileLayoutStatus)
                    } else {
                        topProfileLayoutStatus.text = context.getString(R.string.offline)
                    }
                } catch (e: NumberFormatException) {
                    Log.e("UserProfileUpdater", "Invalid status timestamp: $status")
                    topProfileLayoutStatus.text = context.getString(R.string.offline)
                }
            }
            topProfileLayoutStatus.setTextColor(0xFF757575)
        }
    }

    private fun updateUserBadges(dataSnapshot: DataSnapshot) {
        if (!dataSnapshot.exists()) {
            Log.w("UserProfileUpdater", "User badge data snapshot is null or doesn't exist")
            return
        }

        val gender = dataSnapshot.child("gender").getValue(String::class.java)
        when (gender) {
            "male" -> {
                topProfileLayoutGenderBadge.setImageResource(R.drawable.male_badge)
                topProfileLayoutGenderBadge.visibility = View.VISIBLE
            }
            "female" -> {
                topProfileLayoutGenderBadge.setImageResource(R.drawable.female_badge)
                topProfileLayoutGenderBadge.visibility = View.VISIBLE
            }
            else -> topProfileLayoutGenderBadge.visibility = View.GONE
        }

        val accountType = dataSnapshot.child("account_type").getValue(String::class.java)
        topProfileLayoutVerifiedBadge.visibility = View.VISIBLE
        val badgeRes = when (accountType) {
            "admin" -> R.drawable.admin_badge
            "moderator" -> R.drawable.moderator_badge
            "support" -> R.drawable.support_badge
            else -> {
                when {
                    "true" == dataSnapshot.child("account_premium").getValue(String::class.java) -> R.drawable.premium_badge
                    "true" == dataSnapshot.child("verify").getValue(String::class.java) -> R.drawable.verified_badge
                    else -> 0
                }
            }
        }

        if (badgeRes != 0) {
            topProfileLayoutVerifiedBadge.setImageResource(badgeRes)
        } else {
            topProfileLayoutVerifiedBadge.visibility = View.GONE
        }
    }

    private fun setUserLastSeen(currentTime: Double, txt: TextView) {
        val c1 = Calendar.getInstance()
        val c2 = Calendar.getInstance().apply {
            timeInMillis = currentTime.toLong()
        }

        val diff = c1.timeInMillis - c2.timeInMillis

        val seconds = TimeUnit.MILLISECONDS.toSeconds(diff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        val res = context.resources
        txt.text = when {
            seconds < 60 -> res.getQuantityString(R.plurals.seconds_ago, seconds.toInt(), seconds.toInt())
            minutes < 60 -> res.getQuantityString(R.plurals.minutes_ago, minutes.toInt(), minutes.toInt())
            hours < 24 -> res.getQuantityString(R.plurals.hours_ago, hours.toInt(), hours.toInt())
            days < 7 -> res.getQuantityString(R.plurals.days_ago, days.toInt(), days.toInt())
            else -> {
                val weeks = days / 7
                res.getQuantityString(R.plurals.weeks_ago, weeks.toInt(), weeks.toInt())
            }
        }
    }
}
