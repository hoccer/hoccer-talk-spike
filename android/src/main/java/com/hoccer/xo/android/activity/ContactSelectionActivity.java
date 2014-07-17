package com.hoccer.xo.android.activity;

import android.app.ActionBar;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseBooleanArray;
import android.view.Menu;
import android.view.MenuItem;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.base.XoActionbarActivity;
import com.hoccer.xo.android.fragment.ContactSelectionFragment;
import com.hoccer.xo.release.R;

import java.util.ArrayList;

public class ContactSelectionActivity extends XoActionbarActivity implements ContactSelectionFragment.IContactSelectionListener {

    public static final String SELECTED_CONTACT_IDS_EXTRA = "com.hoccer.xo.android.activity.SELECTED_CONTACT_IDS_EXTRA";

    public static final int CLIENT_CONTACT_MODE = 0;
    public static final int GROUP_CONTACT_MODE = 1;

    private ViewPager mViewPager;

    private ContactSelectionFragment mClientContactSelectionFragment;
    private ContactSelectionFragment mGroupContactSelectionFragment;

    private Menu mMenu;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_contacts;
    }

    @Override
    protected int getMenuResource() {
        return R.menu.menu_activity_contact_selection;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        createFragments();
        registerContactSelectionListenerOnFragments();
        setupViewPager();
        setupActionBarTabs();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        setMenu(menu);
        setCommonMenuItemsInvisible(menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_collections_ok:
                createResultAndFinish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onContactSelectionChanged() {
        toggleAcceptIconVisibility();
    }

    private class RecipientSelectionPagerAdapter extends FragmentPagerAdapter {

        public RecipientSelectionPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return mClientContactSelectionFragment;
                case 1:
                    return mGroupContactSelectionFragment;
                default:
                    return null;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    private class RecipientTabListener implements ActionBar.TabListener {

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

    private class ContactSelectionPageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

        }

        @Override
        public void onPageSelected(int position) {

        }

        @Override
        public void onPageScrollStateChanged(int state) {
            if (state == ViewPager.SCROLL_STATE_IDLE) {
                getActionBar().setSelectedNavigationItem(mViewPager.getCurrentItem());
            }
        }
    }

    private void toggleAcceptIconVisibility() {
        if (mClientContactSelectionFragment.getListView().getCheckedItemCount() == 0
                && mGroupContactSelectionFragment.getListView().getCheckedItemCount() == 0) {
            mMenu.findItem(R.id.menu_collections_ok).setVisible(false);
        } else {
            mMenu.findItem(R.id.menu_collections_ok).setVisible(true);
        }
    }

    private void setupActionBarTabs() {
        final String[] tabNames = getResources().getStringArray(R.array.recipient_selection_tab_names);
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        for (String tabName : tabNames) {
            actionBar.addTab(actionBar.newTab().setText(tabName).setTabListener(new RecipientTabListener()));
        }
    }

    private void registerContactSelectionListenerOnFragments() {
        mClientContactSelectionFragment.addContactSelectionListener(this);
        mGroupContactSelectionFragment.addContactSelectionListener(this);
    }

    private void createFragments() {
        mClientContactSelectionFragment = createContactSelectionFragment(CLIENT_CONTACT_MODE);
        mGroupContactSelectionFragment = createContactSelectionFragment(GROUP_CONTACT_MODE);
    }

    private void setupViewPager() {
        FragmentPagerAdapter pagerAdapter = new RecipientSelectionPagerAdapter(getSupportFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(pagerAdapter);
        mViewPager.setOnPageChangeListener(new ContactSelectionPageChangeListener());
    }

    private void setCommonMenuItemsInvisible(Menu menu) {
        menu.findItem(R.id.menu_my_profile).setVisible(false);
        menu.findItem(R.id.menu_settings).setVisible(false);
    }

    private void setMenu(Menu menu) {
        mMenu = menu;
    }

    private void createResultAndFinish() {
        Intent resultIntent = new Intent();
        resultIntent.putIntegerArrayListExtra(SELECTED_CONTACT_IDS_EXTRA, collectSelectedContactIds());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    private ContactSelectionFragment createContactSelectionFragment(int mode) {
        Bundle bundle = new Bundle();
        bundle.putInt(ContactSelectionFragment.ARG_CLIENT_OR_GROUP_MODE, mode);
        ContactSelectionFragment fragment = new ContactSelectionFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    private ArrayList<Integer> collectSelectedContactIds() {
        ArrayList<Integer> selectedContactIds = getSelectedContactIdsFromFragment(mClientContactSelectionFragment);
        selectedContactIds.addAll(getSelectedContactIdsFromFragment(mGroupContactSelectionFragment));
        LOG.info("#SEPIIDA: " + selectedContactIds);
        return selectedContactIds;
    }

    private ArrayList<Integer> getSelectedContactIdsFromFragment(ContactSelectionFragment contactSelectionFragment) {
        ArrayList<Integer> selectedContactIds = new ArrayList<Integer>();
        SparseBooleanArray checkedItems = contactSelectionFragment.getListView().getCheckedItemPositions();
        for (int i = 0; i < checkedItems.size(); i++) {
            int pos = checkedItems.keyAt(i);
            if (checkedItems.get(pos)) {
                TalkClientContact contact = (TalkClientContact) contactSelectionFragment.getListView().getAdapter().getItem(pos);
                selectedContactIds.add(contact.getClientContactId());
            }
        }
        return selectedContactIds;
    }
}
