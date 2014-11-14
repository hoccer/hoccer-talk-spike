package com.hoccer.xo.android.fragment;

import com.hoccer.talk.client.model.TalkClientContact;
import com.hoccer.xo.android.XoApplication;
import com.hoccer.xo.android.activity.GroupProfileActivity;
import com.hoccer.xo.android.adapter.ContactListAdapter;
import com.hoccer.xo.android.adapter.GroupContactListAdapter;
import com.hoccer.xo.android.view.Placeholder;
import com.artcom.hoccer.R;
import org.apache.log4j.Logger;

import java.sql.SQLException;

public class GroupContactListFragment extends ContactListFragment {

    private static final Logger LOG = Logger.getLogger(GroupContactListFragment.class);
    private static final Placeholder PLACEHOLDER = new Placeholder(
            R.drawable.placeholder_group,
            R.drawable.placeholder_group_head,
            R.string.placeholder_groups_text);

    public GroupContactListFragment() {
        super(R.string.contacts_tab_groups, GroupProfileActivity.class, PLACEHOLDER);
    }

    @Override
    protected ContactListAdapter createAdapter() {
        return new GroupContactListAdapter(getActivity());
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
