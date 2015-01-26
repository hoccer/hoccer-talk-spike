package com.hoccer.xo.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import com.hoccer.talk.model.TalkDelivery;
import com.artcom.hoccer.R;

public abstract class ColorSchemeManager{

    private static final SparseArray<Drawable> mRepaintedIncomingDrawable = new SparseArray<Drawable>();
    private static final SparseArray<Drawable> mRepaintedOutgoingDrawable = new SparseArray<Drawable>();

    public static Drawable getRepaintedDrawable(Resources resources, int bgId, boolean primaryColor) {
        int custom_color = (primaryColor) ? resources.getColor(R.color.primary) : resources.getColor(R.color.message_incoming_background);

        Drawable myBG = resources.getDrawable(bgId);
        myBG.setColorFilter(custom_color, PorterDuff.Mode.MULTIPLY);
        return myBG;
    }

    public static Drawable getRepaintedOutgoingMessageDrawable(Context activity, int bgId, String currentState){
        int custom_color = activity.getResources().getColor(R.color.primary);

        if(currentState == null) {
            custom_color = activity.getResources().getColor(R.color.compose_message_no_state);
        }
        else {
            if (currentState.equals(TalkDelivery.STATE_DELIVERING)) {
                custom_color = activity.getResources().getColor(R.color.compose_message_no_state);
            } else if (currentState.equals(TalkDelivery.STATE_ABORTED)
                    || currentState.equals(TalkDelivery.STATE_ABORTED_ACKNOWLEDGED)
                    || currentState.equals(TalkDelivery.STATE_FAILED))
            {
                custom_color = activity.getResources().getColor(R.color.compose_message_bad_state);
            }
        }

        Drawable myBG = activity.getResources().getDrawable(bgId);
        myBG.setColorFilter(custom_color, PorterDuff.Mode.MULTIPLY);
        return myBG;
    }

    public static Drawable getRepaintedAttachmentDrawable(Context activity, int bgId, boolean isIncoming) {
        Drawable result;
        if(isIncoming) {
            result = mRepaintedIncomingDrawable.get(bgId);
            if(result == null) {
                result = activity.getResources().getDrawable(bgId).mutate();

                // set color filter
                int custom_color = activity.getResources().getColor(R.color.attachment_incoming);
                result.setColorFilter(custom_color, PorterDuff.Mode.MULTIPLY);
                mRepaintedIncomingDrawable.put(bgId, result);
            }
        } else {
            result = mRepaintedOutgoingDrawable.get(bgId);
            if(result == null) {
                result = activity.getResources().getDrawable(bgId).mutate();

                // set color filter
                int custom_color = activity.getResources().getColor(R.color.attachment_outgoing);
                result.setColorFilter(custom_color, PorterDuff.Mode.MULTIPLY);
                mRepaintedOutgoingDrawable.put(bgId, result);
            }
        }

        return result;
    }
}
