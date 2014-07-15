package com.hoccer.xo.android.util;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import com.hoccer.talk.model.TalkDelivery;
import com.hoccer.xo.release.R;

public abstract class ColorSchemeManager{

    public static Drawable getRepaintedDrawable(Context activity, int bgId, boolean primaryColor) {

        int custom_color = (primaryColor) ? activity.getResources().getColor(R.color.xo_app_main_color) : activity.getResources().getColor(R.color.xo_app_incoming_message_color);

        Drawable myBG = activity.getResources().getDrawable(bgId);

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

        int custom_color = (isIncoming) ? activity.getResources().getColor(R.color.xo_app_attachment_incoming_color) : activity.getResources().getColor(R.color.xo_app_attachment_outgoing_color);

        Drawable myBG = activity.getResources().getDrawable(bgId);

        myBG.setColorFilter(custom_color, PorterDuff.Mode.MULTIPLY);

        return myBG;
    }
}
