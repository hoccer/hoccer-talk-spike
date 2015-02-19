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
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.ContactsActivity;

import java.sql.SQLException;


public class ContactsMenuItemActionProvider extends ActionProvider implements IXoContactListener {

    private NotificationBadgeTextView mNotificationBadge;
    private final Context mContext;
    private Integer mNotificationCount = 0;
    private View mMenuItemView;

    /**
     * Creates a new instance. ActionProvider classes should always implement a
     * constructor that takes a single Context parameter for inflating from menu XML.
     *
     * @param context Context for accessing resources.
     */
    public ContactsMenuItemActionProvider(Context context) {
        super(context);
        mContext = context;
        initView();
    }

    @Override
    public View onCreateActionView() {
        return initView();
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    public View onCreateActionView(MenuItem forItem) {
        return initView();
    }

    @Override
    public boolean hasSubMenu() {
        return false;
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        updateNotificationBadge();
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        updateNotificationBadge();
    }

    public void updateNotificationBadge() {
        runOnMainThread(new Runnable() {
            @Override
            public void run() {
                try {
                    mNotificationCount = XoApplication.get().getXoClient().getDatabase().getTotalCountOfInvitations();
                    mNotificationBadge.update(mNotificationCount);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private View initView() {
        if (mMenuItemView == null) {
            mMenuItemView = LayoutInflater.from(mContext).inflate(R.layout.view_contacts_menu_item, null);
            mMenuItemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startContactsActivity();
                }
            });
        }

        mNotificationBadge = (NotificationBadgeTextView) mMenuItemView.findViewById(R.id.tv_invite_notification_badge);
        updateNotificationBadge();

        return mMenuItemView;
    }

    private void startContactsActivity() {
        Intent intent = new Intent(mContext, ContactsActivity.class);
        mContext.startActivity(intent);
    }

    private void runOnMainThread(Runnable r) {
        Handler mainHandler = new Handler(mContext.getMainLooper());
        mainHandler.post(r);
    }
}
