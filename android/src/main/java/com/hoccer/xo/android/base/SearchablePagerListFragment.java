package com.hoccer.xo.android.base;

import com.hoccer.xo.android.fragment.IPagerFragment;
import com.hoccer.xo.android.fragment.SearchableListFragment;


public abstract class SearchablePagerListFragment extends SearchableListFragment implements IPagerFragment {

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
