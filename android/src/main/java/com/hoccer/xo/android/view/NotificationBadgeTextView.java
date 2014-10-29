package com.hoccer.xo.android.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

public class NotificationBadgeTextView extends TextView {

    public NotificationBadgeTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void update(Integer count) {

        if (count > 0) {
            setNotificationBadgeTextSize(count);
            setText(count.toString());
            setVisibility(View.VISIBLE);
        } else {
            setText("");
            setVisibility(View.GONE);
        }
    }

    private void setNotificationBadgeTextSize(Integer notificationCount) {
        if (notificationCount < 10) {
            setTextSize(13);
        } else if (notificationCount < 100) {
            setTextSize(11);
        } else if (notificationCount < 1000) {
            setTextSize(9);
        }
    }

}
