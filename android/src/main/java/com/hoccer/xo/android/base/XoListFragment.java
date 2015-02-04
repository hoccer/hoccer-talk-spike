package com.hoccer.xo.android.base;

import android.app.Activity;
import android.support.v4.app.ListFragment;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.content.SelectedContent;
import org.apache.log4j.Logger;

import java.io.File;

public abstract class XoListFragment extends ListFragment implements IXoFragment {

    private static final Logger LOG = Logger.getLogger(XoListFragment.class);

    protected XoActivity mActivity;

    public File getAvatarDirectory() {
        return new File(mActivity.getFilesDir(), "avatars");
    }

    public XoActivity getXoActivity() {
        return mActivity;
    }

    public XoClientDatabase getXoDatabase() {
        return mActivity.getXoDatabase();
    }

    public void runOnUiThread(Runnable runnable) {
        mActivity.runOnUiThread(runnable);
    }

    @Override
    public void onAttach(Activity activity) {
        LOG.debug("onAttach()");
        super.onAttach(activity);

        if (activity instanceof XoActivity) {
            mActivity = (XoActivity) activity;
        } else {
            throw new RuntimeException("talk fragments need to be in a talk activity");
        }

        mActivity.registerXoFragment(this);
    }

    @Override
    public void onDetach() {
        LOG.debug("onDetach()");
        super.onDetach();

        if (mActivity != null) {
            mActivity.unregisterXoFragment(this);
            mActivity = null;
        }
    }

    @Override
    public void onServiceConnected() {
    }

    @Override
    public void onServiceDisconnected() {
    }

    @Override
    public void onAvatarSelected(SelectedContent co) {
    }
}
