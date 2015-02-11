package com.hoccer.xo.android.base;

import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.content.SelectedContent;

/**
 * Base interface for our fragments
 *
 * We need to use several base classes for our fragments,
 * but since we want a common interface we have this.
 */
public interface IXoFragment {

    public XoActivity getXoActivity();

    public XoClientDatabase getXoDatabase();

    public void onAvatarSelected(SelectedContent co);
}
