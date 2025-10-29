package com.synapse.social.studioasinc

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatEditText

/**
 * Modified by StudioAs Inc. 2024
 */
class FadeEditText @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = android.R.attr.editTextStyle
) : AppCompatEditText(context, attrs, defStyleAttr) {

    private var horiz: Int = 0
    private var fadeTextColor: Int = 0xFF000000.toInt()

    override fun onScrollChanged(horiz: Int, vert: Int, oldHoriz: Int, oldVert: Int) {
        super.onScrollChanged(horiz, vert, oldHoriz, oldVert)
        this.horiz = horiz
        requestLayout()
    }

    override fun onDraw(canvas: Canvas) {
        val width = measuredWidth
        fadeTextColor = currentTextColor

        val text = text
        if (text != null && text.length > 1 && layout.getLineWidth(0) > width) {
            val widthRight = measuredWidth + horiz
            val percent = measuredWidth * 20 / 100

            val widthPreLeft = horiz
            val widthLeft = horiz

            val stopPreLeft = widthPreLeft.toFloat() / widthRight.toFloat()

            val stopLeft = if (widthPreLeft > 0) {
                (widthLeft + percent).toFloat() / widthRight.toFloat()
            } else {
                0f
            }

            val stopRight = if (layout.getLineWidth(0) > widthRight) {
                (widthRight - percent).toFloat() / widthRight.toFloat()
            } else {
                widthRight.toFloat() / widthRight.toFloat()
            }

            val gradient = LinearGradient(
                0f, 0f, widthRight.toFloat(), 0f,
                intArrayOf(fadeTextColor, Color.TRANSPARENT, fadeTextColor, fadeTextColor, Color.TRANSPARENT),
                floatArrayOf(0f, stopPreLeft, stopLeft, stopRight, 1.0f),
                Shader.TileMode.CLAMP
            )

            paint.shader = gradient
        } else {
            paint.shader = null
        }

        super.onDraw(canvas)
    }
}
