package com.hoccer.xo.android.base;

import android.support.v4.app.Fragment;
import com.hoccer.xo.android.fragment.IPagerFragment;

public class PagerLifecycle {

    private boolean mIsSelected;

    public void onResume(IPagerFragment fragment) {
        if (mIsSelected) {
            fragment.onPageResume();
        }
    }

    public void onTabSelected(IPagerFragment fragment) {
        mIsSelected = true;

        if (((Fragment) fragment).isResumed()) {
            fragment.onPageResume();
        }
    }

    public void onTabUnselected(IPagerFragment fragment) {
        mIsSelected = false;
        fragment.onPagePause();
    }
}
