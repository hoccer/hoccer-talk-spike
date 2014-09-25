package com.hoccer.xo.android.activity;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import com.hoccer.xo.android.base.XoActionbarActivity;
import com.hoccer.xo.android.fragment.ClientListFragment;
import com.hoccer.xo.android.fragment.GroupListFragment;
import com.hoccer.xo.release.R;

public class ContactsActivity extends XoActionbarActivity {

    private ViewPager mViewPager;
    private Fragment[] mFragments;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_contacts;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.menu_activity_contacts;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final String[] tabs = getResources().getStringArray(R.array.contacts_tab_names);
        mFragments = new Fragment[]{
                new ClientListFragment(),
                new GroupListFragment()
        };

        mViewPager = (ViewPager)findViewById(R.id.pager);
        mViewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                return mFragments[position];
            }

            @Override
            public int getCount() {
                return mFragments.length;
            }
        });
        mViewPager.setOnPageChangeListener(new ContactsOnPageChangeListener());

        ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (String tabName : tabs) {
            ActionBar.Tab tab = actionBar.newTab()
                    .setText(tabName)
                    .setTabListener(new ContactsTabListener());
            actionBar.addTab(tab);
        }
    }

    private class ContactsOnPageChangeListener implements ViewPager.OnPageChangeListener {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            getActionBar().setSelectedNavigationItem(mViewPager.getCurrentItem());
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }
    }

    private class ContactsTabListener implements ActionBar.TabListener {
        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            mViewPager.setCurrentItem(tab.getPosition());
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }
    }
}
