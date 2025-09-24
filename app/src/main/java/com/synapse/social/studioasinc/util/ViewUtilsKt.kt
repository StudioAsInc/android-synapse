package com.synapse.social.studioasinc.util

import android.app.Activity
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.widget.ImageView

object ViewUtilsKt {
    fun setStateColor(activity: Activity, color1: Int, color2: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity.window.statusBarColor = color1
            activity.window.navigationBarColor = color2
        }
    }

    fun setImageColor(imageView: ImageView, color: Int) {
        imageView.setColorFilter(color)
    }

    fun setGradientDrawable(view: View, color: Int, radius: Float, stroke: Int, strokeColor: Int) {
        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.cornerRadius = radius
        drawable.setStroke(stroke, strokeColor)
        view.background = drawable
    }

    fun setViewGraphics(view: View, color1: Int, color2: Int, radius: Int, stroke: Int, strokeColor: Int) {
        val drawable = GradientDrawable()
        drawable.setColor(color1)
        drawable.cornerRadius = radius.toFloat()
        drawable.setStroke(stroke, strokeColor)
        view.background = drawable
    }
}
