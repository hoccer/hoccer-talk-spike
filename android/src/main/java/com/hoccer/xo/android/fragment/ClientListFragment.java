package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.ClientListAdapter;
import com.hoccer.xo.android.adapter.ContactListAdapter;
import com.hoccer.xo.release.R;

import java.sql.SQLException;

public class ClientListFragment extends ContactListFragment {

    public ClientListFragment() {
        mTabLayoutId = R.layout.view_contacts_tab_friends;
        mTabNameId = R.string.contacts_tab_friends;

        mProfileActivityClass = SingleProfileActivity.class;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_client_contacts, container, false);
    }

    @Override
    protected ContactListAdapter createAdapter() {
        return new ClientListAdapter(getActivity());
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        updateNotificationBadge();
    }

    @Override
    protected void updateNotificationBadge() {
        try {
            mInvitedMeCount = (int) XoApplication.getXoClient().getDatabase().getCountOfInvitedMeClients();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.updateNotificationBadge();
    }
}
