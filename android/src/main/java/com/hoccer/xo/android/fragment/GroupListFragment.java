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

import java.sql.SQLException;

public class GroupListFragment extends ContactListFragment {

    public GroupListFragment() {
        mTabLayoutId = R.layout.view_contacts_tab_groups;
        mTabNameId = R.string.contacts_tab_groups;

        mItemActivityClass = GroupProfileActivity.class;
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
    protected void updateNotificationBadge() {
        try {
            mInvitedMeCount = XoApplication.getXoClient().getDatabase().getCountOfInvitedMeGroups();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        super.updateNotificationBadge();
    }
}
