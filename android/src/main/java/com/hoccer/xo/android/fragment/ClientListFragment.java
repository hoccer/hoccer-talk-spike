package com.hoccer.xo.android.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import com.hoccer.talk.client.IXoContactListener;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.ClientContactsAdapter;
import com.hoccer.xo.release.R;

import java.sql.SQLException;

public class ClientListFragment extends ListFragment implements IPagerFragment, IXoContactListener {

    ClientContactsAdapter mClientContactsAdapter;

    private View tabView;
    private TextView notificationBadgeTextView;

    private int mInvitedMeCount = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mClientContactsAdapter = new ClientContactsAdapter(getActivity());
        notificationBadgeTextView = (TextView) tabView.findViewById(R.id.tv_contact_invite_notification_badge);
        setListAdapter(mClientContactsAdapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            mInvitedMeCount = (int) XoApplication.getXoClient().getDatabase().getInvitedMeRequestsCount();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        if (mInvitedMeCount > 0) {
            notificationBadgeTextView.setVisibility(View.VISIBLE);
        }
        notificationBadgeTextView.setText(Integer.toString(mInvitedMeCount));
        XoApplication.getXoClient().registerContactListener(mClientContactsAdapter);
        XoApplication.getXoClient().registerContactListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        XoApplication.getXoClient().unregisterContactListener(mClientContactsAdapter);
        XoApplication.getXoClient().unregisterContactListener(this);
    }

    @Override
    public void onPageSelected() {

    }

    @Override
    public void onPageUnselected() {

    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
        TalkClientContact contact = (TalkClientContact) getListAdapter().getItem(position);
        Intent intent = new Intent(getActivity(), SingleProfileActivity.class);
        intent.putExtra(SingleProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
        startActivity(intent);
    }

    @Override
    public View getCustomTabView(Context context) {
        tabView = LayoutInflater.from(context).inflate(R.layout.view_contacts_tab_friends, null);
        return tabView;
    }

    @Override
    public String getTabName(Resources resources) {
        return resources.getString(R.string.contacts_tab_friends);
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        if (contact.getClientRelationship().invitedMe()) {
            try {
                mInvitedMeCount = (int) XoApplication.getXoClient().getDatabase().getInvitedMeRequestsCount();
            } catch (SQLException e) {
                e.printStackTrace();
            }
            if (mInvitedMeCount > 0) {
                notificationBadgeTextView.setVisibility(View.VISIBLE);
                notificationBadgeTextView.setText(Integer.toString(mInvitedMeCount));
            } else {
                notificationBadgeTextView.setVisibility(View.GONE);
                notificationBadgeTextView.setText("");
            }
        }
    }

    @Override
    public void onContactAdded(TalkClientContact contact) {

    }

    @Override
    public void onContactRemoved(TalkClientContact contact) {

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
}
