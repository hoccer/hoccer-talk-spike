package com.hoccer.xo.android.util;

import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import com.artcom.hoccer.R;
import com.hoccer.xo.android.XoApplication;

public abstract class ColorSchemeManager {

    private static final SparseArray<Drawable> mRepaintedIncomingDrawable = new SparseArray<Drawable>();
    private static final SparseArray<Drawable> mRepaintedOutgoingDrawable = new SparseArray<Drawable>();

    public static Drawable getInkedDrawable(int drawableId, int inkColorId) {
        int inkColor = getResources().getColor(inkColorId);
        Drawable drawable = getResources().getDrawable(drawableId);
        drawable.setColorFilter(inkColor, PorterDuff.Mode.MULTIPLY);

        return drawable;
    }

    public static Drawable getInkedAttachmentDrawable(int drawableId, boolean isIncoming) {
        Drawable result;
        if (isIncoming) {
            result = mRepaintedIncomingDrawable.get(drawableId);
            if (result == null) {
                result = getResources().getDrawable(drawableId).mutate();

                // set color filter
                int inkColor = getResources().getColor(R.color.attachment_incoming);
                result.setColorFilter(inkColor, PorterDuff.Mode.MULTIPLY);
                mRepaintedIncomingDrawable.put(drawableId, result);
            }
        } else {
            result = mRepaintedOutgoingDrawable.get(drawableId);
            if (result == null) {
                result = getResources().getDrawable(drawableId).mutate();

                // set color filter
                int inkColor = getResources().getColor(R.color.attachment_outgoing);
                result.setColorFilter(inkColor, PorterDuff.Mode.MULTIPLY);
                mRepaintedOutgoingDrawable.put(drawableId, result);
            }
        }

        return result;
    }

    private static Resources getResources() {
        return XoApplication.getContext().getResources();
    }
}
