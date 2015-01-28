package com.hoccer.xo.android.base;

import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.content.SelectedAttachment;

/**
 * Base interface for our fragments
 *
 * We need to use several base classes for our fragments,
 * but since we want a common interface we have this.
 */
public interface IXoFragment {

    public XoActivity getXoActivity();

    public XoClientDatabase getXoDatabase();

    public void onServiceConnected();
    public void onServiceDisconnected();

    public void onAvatarSelected(SelectedAttachment co);
}
