package com.hoccer.xo.android.base;

import android.support.v4.app.ListFragment;
import com.hoccer.xo.android.fragment.IPagerFragment;

public abstract class PagerListFragment extends ListFragment implements IPagerFragment {

    private boolean mIsSelected;

    @Override
    public void onResume() {
        super.onResume();
        if (mIsSelected) {
            onPageSelected();
        }
    }

    @Override
    public void onTabSelected() {
        mIsSelected = true;
        if (isResumed()) {
            onPageSelected();
        }
    }

    @Override
    public void onTabUnselected() {
        mIsSelected = false;
        onPageUnselected();
    }
}
