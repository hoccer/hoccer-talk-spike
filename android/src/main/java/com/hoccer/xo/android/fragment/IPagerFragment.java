package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.res.Resources;
import android.view.View;

/**
 * Defines additional lifecycle methods for Fragments managed by a ViewPager.
 * @see com.hoccer.xo.android.activity.component.ViewPagerActivityComponent
 */
public interface IPagerFragment {

    /*
     * This method is called initially by the ViewPager.
     * @see getTabName()
     * @param context This is provided because the fragment might not have been initialized yet.
     * @return Return custom tab view or null if no custom tab view is wanted/needed.
     */
    View getCustomTabView(Context context);

    /*
     * This method is called initially by the ViewPager if getCustomTabView() returned null.
     * @param resources This is provided because the fragment might not have been initialized yet.
     * @return The name used for the corresponding tab.
     */
    String getTabName(Resources resources);

    /*
     * @see android.support.v4.view.ViewPager.OnPageChangeListener.onPageScrollStateChanged
     */
    void onPageScrollStateChanged(int state);

    void onTabSelected();

    void onTabUnselected();

    void onPageSelected();

    void onPageUnselected();
}
