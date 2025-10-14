// To-do: Migrate Firebase to Supabase
// 1. No direct Firebase dependencies in this file.
// 2. This is a custom UI component for displaying a center-cropped background.
// 3. While this view itself doesn't have backend logic, review how it's used in the application.
//    - If the background drawable is loaded from a URL (e.g., using Glide), ensure the URL source is updated from Firebase Storage to Supabase Storage.

package com.synapse.social.studioasinc;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.LinearLayout;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

public class CenterCropLinearLayoutNoEffect extends LinearLayout {
    private Drawable backgroundDrawable;

    public CenterCropLinearLayoutNoEffect(Context context) {
        super(context);
        init(context, null);
    }

    public CenterCropLinearLayoutNoEffect(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public CenterCropLinearLayoutNoEffect(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, @Nullable AttributeSet attrs) {
        if (attrs != null) {
            int[] attrsArray = new int[]{android.R.attr.background};
            TypedArray ta = context.obtainStyledAttributes(attrs, attrsArray);
            backgroundDrawable = ta.getDrawable(0);
            ta.recycle();
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        if (backgroundDrawable != null) {
            int viewWidth = getWidth();
            int viewHeight = getHeight();
            int drawableWidth = backgroundDrawable.getIntrinsicWidth();
            int drawableHeight = backgroundDrawable.getIntrinsicHeight();

            float scale = Math.max((float) viewWidth / drawableWidth, (float) viewHeight / drawableHeight);

            int scaledWidth = Math.round(scale * drawableWidth);
            int scaledHeight = Math.round(scale * drawableHeight);

            int dx = (viewWidth - scaledWidth) / 2;
            int dy = (viewHeight - scaledHeight) / 2;

            backgroundDrawable.setBounds(dx, dy, dx + scaledWidth, dy + scaledHeight);
            backgroundDrawable.draw(canvas);
        }

        super.dispatchDraw(canvas);
    }

    @Override
    public void setBackground(Drawable background) {
        backgroundDrawable = background;
        invalidate();
    }

    @Override
    public void setBackgroundResource(int resid) {
        backgroundDrawable = ContextCompat.getDrawable(getContext(), resid);
        invalidate();
    }

    @Override
    public void setBackgroundDrawable(Drawable background) {
        backgroundDrawable = background;
        invalidate();
    }
}

// codes by @studioasinc
// https://t.me/studioasinc