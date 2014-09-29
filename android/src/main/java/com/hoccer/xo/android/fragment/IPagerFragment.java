package com.hoccer.xo.android.fragment;

/**
 * Defines additional lifecycle methods for Fragments managed by a ViewPager.
 * @see com.hoccer.xo.android.activity.component.ViewPagerActivityComponent
 */
public interface IPagerFragment {

    void onPageSelected();
    void onPageUnselected();
    void onPageScrollStateChanged(int state);
}
