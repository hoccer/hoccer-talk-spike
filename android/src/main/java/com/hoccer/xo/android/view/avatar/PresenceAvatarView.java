package com.hoccer.xo.android.view.avatar;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.talk.model.TalkPresence;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.view.avatar.AvatarView;

public class PresenceAvatarView extends AvatarView implements IXoContactListener {

    private View mPresenceIndicatorActive;
    private View mPresenceIndicatorInactive;

    private boolean mIsAttachedToWindow;

    public PresenceAvatarView(Context context, AttributeSet attrs) {
        super(context, attrs, R.layout.view_avatar_presence);

        mPresenceIndicatorActive = this.findViewById(R.id.presence_indicator_view_active);
        mPresenceIndicatorInactive = this.findViewById(R.id.presence_indicator_view_inactive);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttachedToWindow = true;
        updatePresence();
        if (mContact != null) {
            XoApplication.get().getXoClient().registerContactListener(this);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttachedToWindow = false;
        if (mContact != null) {
            XoApplication.get().getXoClient().unregisterContactListener(this);
        }
    }

    public void setContact(TalkClientContact contact) {
        super.setContact(contact);
        if (mIsAttachedToWindow) {
            if (mContact == null) {
                if (contact != null) {
                    XoApplication.get().getXoClient().registerContactListener(this);
                }
            } else {
                if (contact == null) {
                    XoApplication.get().getXoClient().unregisterContactListener(this);
                }
            }
        }
        updatePresence();
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
        updatePresence();
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {}

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {}

    private void updatePresence() {
        post(new Runnable() {
            @Override
            public void run() {
                if (mContact != null && mContact.isClient()) {
                    TalkPresence presence = mContact.getClientPresence();
                    if (presence != null) {
                        if (presence.isConnected()) {
                            if (presence.isPresent()) {
                                showPresenceIndicatorActive();
                            } else {
                                showPresenceIndicatorInactive();
                            }
                            return;
                        }
                    }
                }
                hidePresenceIndicator();
            }
        });
    }

    private void showPresenceIndicatorActive() {
        mPresenceIndicatorActive.setVisibility(View.VISIBLE);
        mPresenceIndicatorInactive.setVisibility(View.INVISIBLE);
    }

    private void showPresenceIndicatorInactive() {
        mPresenceIndicatorActive.setVisibility(View.INVISIBLE);
        mPresenceIndicatorInactive.setVisibility(View.VISIBLE);
    }

    private void hidePresenceIndicator() {
        mPresenceIndicatorActive.setVisibility(View.INVISIBLE);
        mPresenceIndicatorInactive.setVisibility(View.INVISIBLE);
    }
}
