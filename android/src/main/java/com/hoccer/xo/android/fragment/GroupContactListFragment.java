package com.hoccer.xo.android.fragment;

import android.content.Intent;
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
        super(R.string.contacts_tab_groups, PLACEHOLDER);
    }

    @Override
    protected ContactListAdapter createAdapter() {
        return new GroupContactListAdapter(getActivity());
    }

    @Override
    protected Intent getProfileActivityIntent(TalkClientContact contact) {
        return new Intent(getActivity(), GroupProfileActivity.class)
                .setAction(GroupProfileActivity.ACTION_SHOW)
                .putExtra(GroupProfileActivity.EXTRA_CLIENT_CONTACT_ID, contact.getClientContactId());
    }

    @Override
    public void onGroupMembershipChanged(TalkClientContact contact) {
        updateNotificationBadge();
    }

    @Override
    protected int getInvitedMeCount() {
        try {
            return XoApplication.get().getXoClient().getDatabase().getCountOfInvitedMeGroupContacts();
        } catch (SQLException e) {
            LOG.error("Error getting group invitation count", e);
        }

        return 0;
    }
}
