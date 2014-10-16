package com.hoccer.xo.android.view;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.view.ActionProvider;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.ContactsActivity;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class ContactsMenuItemActionProvider extends ActionProvider implements IXoContactListener {

    static final Logger LOG = Logger.getLogger(ContactsMenuItemActionProvider.class);

    private TextView mNotificationBadge;
    private Context mContext;
    private Integer mNotificationCount = 0;

    private Runnable mShowNotificationBadge = new Runnable() {
        @Override
        public void run() {
            setNotificationBadgeTextSize();
            mNotificationBadge.setText(mNotificationCount.toString());
            mNotificationBadge.setVisibility(View.VISIBLE);
        }
    };

    private Runnable mHideNotificationBadge = new Runnable() {
        @Override
        public void run() {
            mNotificationBadge.setVisibility(View.GONE);
        }
    };

    /**
     * Creates a new instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */
    public ContactsMenuItemActionProvider(Context context) {
        super(context);
        mContext = context;
    }

    public void evaluateNotifications() {
        try {
            mNotificationCount = XoApplication.getXoClient().getDatabase().getTotalCountOfInvitations();
            if (mNotificationBadge != null) {
                updateNotificationBadge();
            }
        } catch (SQLException e) {
            LOG.error("SQL Exception while getting amount of invitation", e);
        }
    }

    @Override
    public View onCreateActionView() {
        return initView(mContext);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public View onCreateActionView(MenuItem forItem) {
        return initView(mContext);
    }

    @Override
    public boolean hasSubMenu() {
        return false;
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {

    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {

    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {

    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        evaluateNotifications();
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {

    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        evaluateNotifications();
    }

    private View initView(Context context) {
        View v = LayoutInflater.from(context).inflate(R.layout.view_contacts_menu_item, null);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startContactsActivity();
            }
        });

        mNotificationBadge = (TextView) v.findViewById(R.id.tv_invite_notification_badge);
        updateNotificationBadge();

        return v;
    }

    private void startContactsActivity() {
        Intent intent = new Intent(mContext, ContactsActivity.class);
        mContext.startActivity(intent);
    }

    private void updateNotificationBadge() {
        if (mNotificationCount > 0) {
            runOnMainThread(mShowNotificationBadge);
        } else if (mNotificationBadge.getVisibility() == View.VISIBLE) {
            runOnMainThread(mHideNotificationBadge);
        }
    }

    private void setNotificationBadgeTextSize() {
        if (mNotificationCount < 10) {
            mNotificationBadge.setTextSize(13);
        } else if (mNotificationCount < 100) {
            mNotificationBadge.setText(11);
        } else if (mNotificationCount < 1000) {
            mNotificationBadge.setText(9);
        }
    }

    private void runOnMainThread(Runnable r) {
        Handler mainHandler = new Handler(mContext.getMainLooper());
        mainHandler.post(r);
    }
}
