package com.hoccer.xo.android.util;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.SparseArray;
import com.hoccer.talk.model.TalkDelivery;
import com.artcom.hoccer.R;

import java.util.HashMap;

public abstract class ColorSchemeManager{

    private static final SparseArray<Drawable> mRepaintedIncomingDrawable = new SparseArray<Drawable>();
    private static final SparseArray<Drawable> mRepaintedOutgoingDrawable = new SparseArray<Drawable>();

    public static Drawable getRepaintedDrawable(Resources resources, int bgId, boolean primaryColor) {
        int custom_color = (primaryColor) ? resources.getColor(R.color.xo_app_main_color) : resources.getColor(R.color.xo_app_incoming_message_color);

        Drawable myBG = resources.getDrawable(bgId);
        myBG.setColorFilter(custom_color, PorterDuff.Mode.MULTIPLY);
        return myBG;
    }

    public static Drawable getRepaintedOutgoingMessageDrawable(Context activity, int bgId, String currentState){
        int custom_color = activity.getResources().getColor(R.color.xo_app_main_color);

        if(currentState == null) {
            custom_color = activity.getResources().getColor(R.color.xo_compose_message_no_state_color);
        }
        else {
            if (currentState.equals(TalkDelivery.STATE_DELIVERING)) {
                custom_color = activity.getResources().getColor(R.color.xo_compose_message_no_state_color);
            } else if (currentState.equals(TalkDelivery.STATE_ABORTED)
                    || currentState.equals(TalkDelivery.STATE_ABORTED_ACKNOWLEDGED)
                    || currentState.equals(TalkDelivery.STATE_FAILED))
            {
                custom_color = activity.getResources().getColor(R.color.xo_compose_message_bad_state_color);
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
                int custom_color = activity.getResources().getColor(R.color.xo_app_attachment_incoming_color);
                result.setColorFilter(custom_color, PorterDuff.Mode.MULTIPLY);
                mRepaintedIncomingDrawable.put(bgId, result);
            }
        } else {
            result = mRepaintedOutgoingDrawable.get(bgId);
            if(result == null) {
                result = activity.getResources().getDrawable(bgId).mutate();

                // set color filter
                int custom_color = activity.getResources().getColor(R.color.xo_app_attachment_outgoing_color);
                result.setColorFilter(custom_color, PorterDuff.Mode.MULTIPLY);
                mRepaintedOutgoingDrawable.put(bgId, result);
            }
        }

        return result;
    }
}
