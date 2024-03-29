package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.*;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.artcom.hoccer.R;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.adapter.ContactListAdapter;
import com.hoccer.xo.android.base.SearchablePagerListFragment;
import com.hoccer.xo.android.view.NotificationBadgeTextView;

public abstract class ContactListFragment extends SearchablePagerListFragment implements IXoContactListener {

    private ContactListAdapter mContactListAdapter;

    private final int mTabNameId;

    private MenuItem mMenuItemPairing;
    private MenuItem mMenuItemNewGroup;

    private View mTabView;
    private NotificationBadgeTextView mNotificationBadgeTextView;

    protected ContactListFragment(int tabNameId) {
        mTabNameId = tabNameId;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contacts, container, false);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        mMenuItemPairing = menu.findItem(R.id.menu_pair);
        mMenuItemNewGroup = menu.findItem(R.id.menu_new_group);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNotificationBadgeTextView = (NotificationBadgeTextView) getCustomTabView(getActivity()).findViewById(R.id.tv_contact_invite_notification_badge);

        mContactListAdapter = createAdapter();
        setListAdapter(mContactListAdapter);

        XoApplication.get().getClient().registerContactListener(mContactListAdapter);
        XoApplication.get().getClient().registerContactListener(this);
    }

    protected abstract ContactListAdapter createAdapter();

    @Override
    public void onResume() {
        super.onResume();
        updateAdapter();
        updateNotificationBadge();
    }

    private void updateAdapter() {
        mContactListAdapter.updateContactsAndView();
    }

    protected void updateNotificationBadge() {

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                final int count = getInvitedMeCount();
                mNotificationBadgeTextView.update(count);
            }
        });
    }

    protected abstract int getInvitedMeCount();

    @Override
    protected ListAdapter searchInAdapter(String query) {
        mContactListAdapter.setQuery(query);
        return mContactListAdapter;
    }

    @Override
    protected void onSearchModeEnabled() {
        mMenuItemPairing.setVisible(false);
        mMenuItemNewGroup.setVisible(false);
    }

    @Override
    protected void onSearchModeDisabled() {
        mContactListAdapter.setQuery(null);

        if (mMenuItemPairing != null) {
            mMenuItemPairing.setVisible(true);
        }

        if (mMenuItemNewGroup != null) {
            mMenuItemNewGroup.setVisible(true);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        XoApplication.get().getClient().unregisterContactListener(mContactListAdapter);
        XoApplication.get().getClient().unregisterContactListener(this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        TalkClientContact contact = (TalkClientContact) getListAdapter().getItem(position);
        startProfileActivity(contact);
    }

    private void startProfileActivity(TalkClientContact contact) {
        startActivity(getProfileActivityIntent(contact));
    }

    protected abstract Intent getProfileActivityIntent(TalkClientContact contact);

    @Override
    public void onPageScrollStateChanged(int state) {
    }

    @Override
    public View getCustomTabView(Context context) {
        if (mTabView == null) {
            mTabView = LayoutInflater.from(context).inflate(R.layout.view_contacts_tab_with_badge, null);
            TextView title = (TextView) mTabView.findViewById(android.R.id.title);
            title.setText(mTabNameId);
        }

        return mTabView;
    }

    @Override
    public String getTabName(Resources resources) {
        return resources.getString(mTabNameId);
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
    }

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
    }

    @Override
    public void onPageSelected() {}

    @Override
    public void onPageUnselected() {
        leaveSearchMode();
    }
}
