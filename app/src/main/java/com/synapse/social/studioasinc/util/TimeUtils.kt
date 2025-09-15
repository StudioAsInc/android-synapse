package com.synapse.social.studioasinc.util

import android.content.Context
import android.widget.TextView
import com.synapse.social.studioasinc.R
import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {
    fun setTime(currentTime: Double, textView: TextView, context: Context) {
        val c1 = Calendar.getInstance()
        val c2 = Calendar.getInstance()
        val timeDiff = c1.timeInMillis - currentTime
        
        when {
            timeDiff < 60000 -> {
                val seconds = (timeDiff / 1000).toLong()
                if (seconds < 2) {
                    textView.text = "1 ${context.getString(R.string.seconds_ago)}"
                } else {
                    textView.text = "$seconds ${context.getString(R.string.seconds_ago)}"
                }
            }
            timeDiff < (60 * 60000) -> {
                val minutes = (timeDiff / 60000).toLong()
                if (minutes < 2) {
                    textView.text = "1 ${context.getString(R.string.minutes_ago)}"
                } else {
                    textView.text = "$minutes ${context.getString(R.string.minutes_ago)}"
                }
            }
            timeDiff < (24 * 60 * 60000) -> {
                val hours = (timeDiff / (60 * 60000)).toLong()
                textView.text = "$hours ${context.getString(R.string.hours_ago)}"
            }
            timeDiff < (7 * 24 * 60 * 60000) -> {
                val days = (timeDiff / (24 * 60 * 60000)).toLong()
                textView.text = "$days ${context.getString(R.string.days_ago)}"
            }
            else -> {
                c2.timeInMillis = currentTime.toLong()
                textView.text = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(c2.time)
            }
        }
    }
}