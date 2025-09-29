package com.synapse.social.studioasinc.util

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.synapse.social.studioasinc.R
import com.synapse.social.studioasinc.backend.interfaces.IDataSnapshot
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

    fun updateAll(dataSnapshot: IDataSnapshot) {
        if (!dataSnapshot.exists()) {
            Log.w("UserProfileUpdater", "User profile data snapshot is null or doesn't exist")
            return
        }
        val userMap = dataSnapshot.getValue(Map::class.java) as? Map<String, Any> ?: return

        updateUserProfile(userMap)
        updateUserBadges(userMap)
    }

    private fun updateUserProfile(userMap: Map<String, Any>) {
        if (userMap["banned"] as? String == "true") {
            topProfileLayoutProfileImage.setImageResource(R.drawable.banned_avatar)
            secondUserAvatar = "null_banned"
            topProfileLayoutStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.status_banned_text))
            topProfileLayoutStatus.text = context.getString(R.string.offline)
        } else {
            val avatarUrl = userMap["avatar"] as? String
            if (avatarUrl.isNullOrEmpty() || avatarUrl == "null") {
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

        val nickname = userMap["nickname"] as? String
        secondUserName = if (nickname.isNullOrEmpty() || nickname == "null") {
            val username = userMap["username"] as? String
            if (!username.isNullOrEmpty()) "@$username" else "Unknown User"
        } else {
            nickname
        }
        topProfileLayoutUsername.text = secondUserName

        val status = userMap["status"] as? String
        if (status == "online") {
            topProfileLayoutStatus.text = context.getString(R.string.online)
            topProfileLayoutStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.status_online_text))
        } else {
            if (status == "offline") {
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
            topProfileLayoutStatus.setTextColor(androidx.core.content.ContextCompat.getColor(context, R.color.status_offline_text))
        }
    }

    private fun updateUserBadges(userMap: Map<String, Any>) {
        val gender = userMap["gender"] as? String
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

        val accountType = userMap["account_type"] as? String
        topProfileLayoutVerifiedBadge.visibility = View.VISIBLE
        val badgeRes = when (accountType) {
            "admin" -> R.drawable.admin_badge
            "moderator" -> R.drawable.moderator_badge
            "support" -> R.drawable.support_badge
            else -> {
                when {
                    userMap["account_premium"] as? String == "true" -> R.drawable.premium_badge
                    userMap["verify"] as? String == "true" -> R.drawable.verified_badge
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
        val c2 = Calendar.getInstance()
        c2.timeInMillis = currentTime.toLong()

        val timeDiff = c1.timeInMillis - c2.timeInMillis

        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeDiff)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeDiff)
        val hours = TimeUnit.MILLISECONDS.toHours(timeDiff)
        val days = TimeUnit.MILLISECONDS.toDays(timeDiff)
        val weeks = days / 7
        val months = days / 30
        val years = days / 365

        fun getString(resId: Int): String {
            return context.resources.getString(resId)
        }

        val text = when {
            seconds < 60 -> if (seconds < 2) "1 " + getString(R.string.status_text_seconds) else "$seconds " + getString(R.string.status_text_seconds)
            minutes < 60 -> if (minutes < 2) "1 " + getString(R.string.status_text_minutes) else "$minutes " + getString(R.string.status_text_minutes)
            hours < 24 -> if (hours < 2) "1 " + getString(R.string.status_text_hours) else "$hours " + getString(R.string.status_text_hours)
            days < 7 -> if (days < 2) "1 " + getString(R.string.status_text_days) else "$days " + getString(R.string.status_text_days)
            weeks < 4 -> if (weeks < 2) "1 " + getString(R.string.status_text_week) else "$weeks " + getString(R.string.status_text_week)
            months < 12 -> if (months < 2) "1 " + getString(R.string.status_text_month) else "$months " + getString(R.string.status_text_month)
            else -> if (years < 2) "1 " + getString(R.string.status_text_years) else "$years " + getString(R.string.status_text_years)
        }
        txt.text = text
    }
}