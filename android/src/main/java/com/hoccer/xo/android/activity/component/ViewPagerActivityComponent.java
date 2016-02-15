package com.hoccer.xo.android.activity.component;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import com.hoccer.xo.android.fragment.IPagerFragment;

import java.util.*;

/**
 * Adds and manages an activity ViewPager.
 */
public class ViewPagerActivityComponent extends ActivityComponent {

    private ViewPager mViewPager;
    private final List<Fragment> mFragments = new ArrayList<Fragment>();
    private final int mViewPagerId;
    private IPagerFragment mCurrentFragment;

    public ViewPagerActivityComponent(FragmentActivity activity, int viewPagerId, Fragment... fragments) {
        super(activity);

        Collections.addAll(mFragments, fragments);
        mViewPagerId = viewPagerId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // create view pager
        mViewPager = (ViewPager) getActivity().findViewById(mViewPagerId);
        mViewPager.setAdapter(new FragmentPagerAdapter(getActivity().getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return mFragments.get(position);
            }

            @Override
            public int getCount() {
                return mFragments.size();
            }

            @Override
            public int getItemPosition(Object object) {
                int position = mFragments.indexOf(object);
                return position == -1 ? POSITION_NONE : position;
            }
        });
        mViewPager.setOnPageChangeListener(new PageChangeListener());

        ActionBar actionBar = getActivity().getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        for (Fragment fragment : mFragments) {
            addTab((IPagerFragment) fragment);
        }
    }

    public void add(Fragment fragment) {
        addTab((IPagerFragment) fragment);
        mFragments.add(fragment);
        mViewPager.getAdapter().notifyDataSetChanged();
    }

    private void addTab(IPagerFragment fragment) {
        ActionBar.Tab tab = getActivity().getActionBar().newTab();

        View tabView = fragment.getCustomTabView(getActivity());
        if (tabView != null) {
            tab.setCustomView(tabView);
        } else {
            tab.setText(fragment.getTabName(getActivity().getResources()));
        }
        tab.setTag(fragment);
        tab.setTabListener(new TabListener());
        getActivity().getActionBar().addTab(tab);
    }

    public void remove(Fragment fragment) {
        removeTab((IPagerFragment) fragment);
        mFragments.remove(fragment);
        mViewPager.getAdapter().notifyDataSetChanged();
    }

    public int getTabCount(){
        return getActivity().getActionBar().getTabCount();
    }

    private void removeTab(IPagerFragment fragment) {
        ActionBar actionBar = getActivity().getActionBar();
        for (int i = 0; i < actionBar.getTabCount(); i++){
            if (actionBar.getTabAt(i).getTag() == fragment){
                actionBar.removeTabAt(i);
                break;
            }
        }
    }

    private class PageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            getActivity().getActionBar().setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            ((IPagerFragment) mFragments.get(mViewPager.getCurrentItem())).onPageScrollStateChanged(state);
        }
    }

    private class TabListener implements ActionBar.TabListener {
        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            int position = tab.getPosition();
            mViewPager.setCurrentItem(position);

            mCurrentFragment = (IPagerFragment) mFragments.get(position);
            mCurrentFragment.onTabSelected();
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
            mCurrentFragment.onTabUnselected();
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }
    }
}
