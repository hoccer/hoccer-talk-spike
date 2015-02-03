package com.hoccer.xo.android.util;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.XoApplication;

public abstract class ColorSchemeManager {

    private static final SparseArray<SparseArray<Drawable>> mInkedDrawables = new SparseArray<SparseArray<Drawable>>();

    public static Drawable getInkedDrawableCached(int drawableId, int inkColorId) {
        Drawable drawable;

        SparseArray<Drawable> inkColorIdToInkedDrawables = mInkedDrawables.get(drawableId);
        if (inkColorIdToInkedDrawables != null && inkColorIdToInkedDrawables.size() != 0) {
            drawable = inkColorIdToInkedDrawables.get(inkColorId);
            if (drawable == null) {
                drawable = createInkedDrawable(drawableId, inkColorId);
                inkColorIdToInkedDrawables.put(inkColorId, drawable);
            }
        } else {
            inkColorIdToInkedDrawables = new SparseArray<Drawable>();
            drawable = createInkedDrawable(drawableId, inkColorId);
            inkColorIdToInkedDrawables.put(inkColorId, drawable);

            mInkedDrawables.put(drawableId, inkColorIdToInkedDrawables);
        }

        return drawable;
    }

    public static Drawable createInkedDrawable(int drawableId, int inkColorId) {
        int inkColor = getResources().getColor(inkColorId);
        Drawable drawable = getResources().getDrawable(drawableId).mutate();
        drawable.setColorFilter(inkColor, PorterDuff.Mode.MULTIPLY);

        return drawable;
    }

    private static Resources getResources() {
        return XoApplication.getContext().getResources();
    }
}
