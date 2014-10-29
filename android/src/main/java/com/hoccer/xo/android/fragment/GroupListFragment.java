package com.hoccer.xo.android.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.GroupProfileActivity;
import com.hoccer.xo.android.adapter.ContactListAdapter;
import com.hoccer.xo.android.adapter.GroupListAdapter;
import com.hoccer.xo.release.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class GroupListFragment extends ContactListFragment {

    public static final Logger LOG = Logger.getLogger(GroupListFragment.class);

    public GroupListFragment() {
        mTabLayoutId = R.layout.view_contacts_tab_groups;
        mTabNameId = R.string.contacts_tab_groups;

        mProfileActivityClass = GroupProfileActivity.class;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_contacts, container, false);
    }

    @Override
    protected ContactListAdapter createAdapter() {
        return new GroupListAdapter(getActivity());
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        updateNotificationBadge();
    }

    @Override
    protected int getInvitedMeCount() {
        try {
            return XoApplication.getXoClient().getDatabase().getCountOfInvitedMeGroups();
        } catch (SQLException e) {
            LOG.error("Error getting group invitation count", e);
        }

        return 0;
    }
}
