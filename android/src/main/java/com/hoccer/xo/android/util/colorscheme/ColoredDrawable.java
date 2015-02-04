package com.hoccer.xo.android.util.colorscheme;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import com.hoccer.xo.android.XoApplication;

import java.util.HashMap;
import java.util.Map;

public abstract class ColoredDrawable {

    private static final Map<ColoredDrawableId, Drawable> mColoredDrawables = new HashMap<ColoredDrawableId, Drawable>();

    public static Drawable getFromCache(int drawableId, int colorId) {
        ColoredDrawableId coloredDrawableId = new ColoredDrawableId(drawableId, colorId);

        if (!mColoredDrawables.containsKey(coloredDrawableId)) {
            Drawable drawable = create(drawableId, colorId);
            mColoredDrawables.put(coloredDrawableId, drawable);
        }

        return mColoredDrawables.get(coloredDrawableId);
    }

    public static Drawable create(int drawableId, int inkColorId) {
        int inkColor = getResources().getColor(inkColorId);
        Drawable drawable = getResources().getDrawable(drawableId).mutate();
        drawable.setColorFilter(inkColor, PorterDuff.Mode.MULTIPLY);

        return drawable;
    }

    private static Resources getResources() {
        return XoApplication.getContext().getResources();
    }
}
