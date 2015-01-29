package com.hoccer.xo.android.base;

import android.app.Activity;
import android.support.v4.app.Fragment;
import com.hoccer.talk.client.XoClientDatabase;
import com.hoccer.talk.content.SelectedContent;
import com.hoccer.xo.android.XoAndroidClient;
import com.hoccer.xo.android.XoApplication;
import org.apache.log4j.Logger;

/**
 * Base class for fragments working with the talk client
 *
 * This encapsulated commonalities:
 *  - access to activity for db and services
 */
public abstract class XoFragment extends Fragment implements IXoFragment {

    private static final Logger LOG = Logger.getLogger(XoFragment.class);

    private XoActivity mActivity;

    public XoActivity getXoActivity() {
        return mActivity;
    }

    public XoAndroidClient getXoClient() {
        return XoApplication.getXoClient();
    }

    public XoClientDatabase getXoDatabase() {
        return mActivity.getXoDatabase();
    }

    @Override
    public void onAttach(Activity activity) {
        LOG.debug("onAttach()");
        super.onAttach(activity);

        if(activity instanceof XoActivity) {
            mActivity = (XoActivity)activity;
        } else {
            throw new RuntimeException("talk fragments need to be in a talk activity");
        }

        mActivity.registerXoFragment(this);
    }

    @Override
    public void onDetach() {
        LOG.debug("onDetach()");
        super.onDetach();

        if(mActivity != null) {
            mActivity.unregisterXoFragment(this);
            mActivity = null;
        }
    }

    public void onServiceConnected() {
    }

    public void onServiceDisconnected() {
    }

    public void onAvatarSelected(SelectedContent content) {
    }
}
