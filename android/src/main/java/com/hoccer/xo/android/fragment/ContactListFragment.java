package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.ContactListAdapter;
import com.hoccer.xo.release.R;

public abstract class ContactListFragment extends SearchableListFragment implements IPagerFragment, IXoContactListener {

    private ContactListAdapter mContactListAdapter;
    private Class<?> mProfileActivityClass;

    private int mTabNameId;

    private MenuItem mMenuItemPairing;
    private MenuItem mMenuItemNewGroup;

    private View mTabView;
    private TextView mNotificationBadgeTextView;

    public ContactListFragment(int tabNameId, Class<?> profileActivityClass) {
        mTabNameId = tabNameId;
        mProfileActivityClass = profileActivityClass;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        mNotificationBadgeTextView = (TextView) getCustomTabView(getActivity()).findViewById(R.id.tv_contact_invite_notification_badge);

        mContactListAdapter = createAdapter();
        setListAdapter(mContactListAdapter);

        XoApplication.getXoClient().registerContactListener(mContactListAdapter);
        XoApplication.getXoClient().registerContactListener(this);
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
        final int count = getInvitedMeCount();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (count > 0) {
                    mNotificationBadgeTextView.setVisibility(View.VISIBLE);

                    if (count < 10) {
                        mNotificationBadgeTextView.setTextSize(13);
                    } else if (count < 100) {
                        mNotificationBadgeTextView.setTextSize(11);
                    } else if (count < 1000) {
                        mNotificationBadgeTextView.setTextSize(9);
                    }

                    mNotificationBadgeTextView.setText(Integer.toString(count));
                } else {
                    mNotificationBadgeTextView.setText("");
                    mNotificationBadgeTextView.setVisibility(View.GONE);
                }
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
        XoApplication.getXoClient().unregisterContactListener(mContactListAdapter);
        XoApplication.getXoClient().unregisterContactListener(this);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        TalkClientContact contact = (TalkClientContact) getListAdapter().getItem(position);
        startProfileActivity(contact);
    }

    private void startProfileActivity(TalkClientContact contact) {
        Intent intent = new Intent(getActivity(), mProfileActivityClass);
        intent.putExtra(SingleProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
        startActivity(intent);
    }

    @Override
    public void onPageResume() {}

    @Override
    public void onPageSelected() {}

    @Override
    public void onPageUnselected() {}

    @Override
    public void onPagePause() {}

    @Override
    public void onPageScrollStateChanged(int state) {}

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
    public void onClientRelationshipChanged(TalkClientContact contact) {}

    @Override
    public void onContactAdded(TalkClientContact contact) {}

    @Override
    public void onContactRemoved(TalkClientContact contact) {}

    @Override
    public void onClientPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onGroupPresenceChanged(TalkClientContact contact) {}

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {}
}
