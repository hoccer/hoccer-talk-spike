package com.hoccer.xo.android.base;

import com.hoccer.xo.android.fragment.IPagerFragment;

public abstract class XoPagerListFragment extends XoListFragment implements IPagerFragment {

    private PagerLifecycle mPagerLifecycle = new PagerLifecycle();

    @Override
    public void onResume() {
        super.onResume();
        mPagerLifecycle.onResume(this);
    }

    @Override
    public void onTabSelected() {
        mPagerLifecycle.onTabSelected(this);
    }

    @Override
    public void onTabUnselected() {
        mPagerLifecycle.onTabUnselected(this);
    }
}
