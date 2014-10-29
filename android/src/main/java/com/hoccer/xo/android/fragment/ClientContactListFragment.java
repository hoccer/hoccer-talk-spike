package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.SingleProfileActivity;
import com.hoccer.xo.android.adapter.ClientContactListAdapter;
import com.hoccer.xo.android.adapter.ContactListAdapter;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class ClientContactListFragment extends ContactListFragment {

    public static final Logger LOG = Logger.getLogger(ContactListFragment.class);

    public ClientContactListFragment() {
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
        return new ClientContactListAdapter(getActivity());
    }

    @Override
    public void onClientRelationshipChanged(TalkClientContact contact) {
        updateNotificationBadge();
    }

    @Override
    protected int getInvitedMeCount() {
        try {
            return (int) XoApplication.getXoClient().getDatabase().getCountOfInvitedMeClients();
        } catch (SQLException e) {
            LOG.error("Error getting invitation count", e);
        }

        return 0;
    }
}
