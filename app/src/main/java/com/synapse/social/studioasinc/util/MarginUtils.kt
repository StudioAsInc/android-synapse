package com.synapse.social.studioasinc.util

import android.app.Activity
import android.app.DialogFragment
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TableRow
import androidx.fragment.app.Fragment

object MarginUtils {

    fun setMargin(view: View, r: Double, l: Double, t: Double, b: Double) {
        val dpRatio = view.context.resources.displayMetrics.density
        val right = (r * dpRatio).toInt()
        val left = (l * dpRatio).toInt()
        val top = (t * dpRatio).toInt()
        val bottom = (b * dpRatio).toInt()

        val p = view.layoutParams
        when (p) {
            is LinearLayout.LayoutParams -> {
                p.setMargins(left, top, right, bottom)
                view.layoutParams = p
            }
            is RelativeLayout.LayoutParams -> {
                p.setMargins(left, top, right, bottom)
                view.layoutParams = p
            }
            is TableRow.LayoutParams -> {
                p.setMargins(left, top, right, bottom)
                view.layoutParams = p
            }
        }
    }
}