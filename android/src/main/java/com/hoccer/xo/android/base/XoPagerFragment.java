package com.hoccer.xo.android.base;

import android.support.v4.app.Fragment;
import com.hoccer.xo.android.fragment.IPagerFragment;


public abstract class XoPagerFragment extends Fragment implements IPagerFragment {

    private boolean mIsSelected;

    @Override
    public void onResume() {
        super.onResume();
        if (mIsSelected) {
            onPageResume();
        }
    }

    @Override
    public void onTabSelected() {
        mIsSelected = true;
        if (isResumed()) {
            onPageResume();
        }
    }

    @Override
    public void onTabUnselected() {
        mIsSelected = false;
        onPagePause();
    }

    protected abstract void onPageResume();

    protected abstract void onPagePause();
}
