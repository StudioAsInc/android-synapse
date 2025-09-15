package com.synapse.social.studioasinc.util;

import android.content.Context;
import android.util.TypedValue;

public class ThemeUtils {

    public static int getThemeColor(Context context, int colorAttr) {
        TypedValue typedValue = new TypedValue();
        context.getTheme().resolveAttribute(colorAttr, typedValue, true);
        return typedValue.data;
    }
}
